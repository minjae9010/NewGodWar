package kr.newgodwar.game;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/** Bounded disk work with a mandatory barrier before the server can advance a tick. */
final class CheckpointIo implements AutoCloseable {
    private final ExecutorService executor;

    CheckpointIo(int workers) {
        AtomicInteger sequence = new AtomicInteger();
        executor = Executors.newFixedThreadPool(workers, runnable -> {
            Thread thread = new Thread(runnable, "NewGodWar-checkpoint-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    void writeAll(List<? extends Callable<Void>> writes) throws IOException {
        List<Future<Void>> pending = new ArrayList<Future<Void>>(writes.size());
        IOException failure = null;
        boolean interrupted = false;
        try {
            for (Callable<Void> write : writes) pending.add(executor.submit(write));
        } catch (RuntimeException ex) {
            failure = new IOException("Could not submit player checkpoint writes", ex);
        }
        // Even an interrupted/failed checkpoint must drain every submitted write. Cancelling
        // futures can leave an old writer racing the next checkpoint or a player's logout.
        for (Future<Void> future : pending) {
            boolean finished = false;
            while (!finished) {
                try {
                    future.get();
                    finished = true;
                } catch (InterruptedException ex) {
                    interrupted = true;
                } catch (ExecutionException ex) {
                    IOException error = new IOException("Could not write player checkpoint", ex.getCause());
                    if (failure == null) failure = error;
                    else failure.addSuppressed(error);
                    finished = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
            if (failure == null) failure = new IOException("Interrupted while saving player checkpoint");
        }
        if (failure != null) throw failure;
    }

    interface FileWriter { void write(Path temporary) throws Exception; }

    /** Keep both vanilla .dat and .dat_old valid, including a failed replacement. */
    static void replace(Path target, FileWriter writer) throws Exception {
        replace(target, writer, true);
    }

    static void replace(Path target, FileWriter writer, boolean keepBackup) throws Exception {
        replace(target, writer, keepBackup, true);
    }

    private static void replace(Path target, FileWriter writer, boolean keepBackup, boolean forceData) throws Exception {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName() + "-", ".tmp");
        Path backupTemporary = null;
        try {
            writer.write(temporary);
            if (forceData) force(temporary);
            if (keepBackup && Files.exists(target)) {
                backupTemporary = Files.createTempFile(target.getParent(), target.getFileName() + "-old-", ".tmp");
                Files.copy(target, backupTemporary, StandardCopyOption.REPLACE_EXISTING);
                // The new primary was already forced. Keep vanilla's closed-file backup
                // semantics without a redundant second disk flush for each old snapshot.
                Files.move(backupTemporary, target.resolveSibling(target.getFileName() + "_old"),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            // Do not delete the current file first or downgrade to a non-atomic move.
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
            if (backupTemporary != null) Files.deleteIfExists(backupTemporary);
        }
    }

    static void replaceJsonIfChanged(Path target, byte[] bytes) throws Exception {
        // Compare the actual file, not a cache: native autosaves/logout or another plugin
        // may have saved a newer version since our preceding checkpoint.
        if (Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), bytes)) return;
        // Vanilla stats/advancements use a closed buffered writer, with no fsync. Retain
        // that durability contract and add atomic replacement; NBT/session files are forced.
        replace(target, temporary -> Files.write(temporary, bytes), false, false);
    }

    private static void force(Path file) throws IOException {
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.WRITE)) {
            channel.force(true);
        }
    }

    @Override public void close() {
        // writeAll never returns with an outstanding writer.
        executor.shutdown();
    }
}
