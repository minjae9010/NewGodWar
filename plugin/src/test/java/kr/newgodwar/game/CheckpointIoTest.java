package kr.newgodwar.game;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CheckpointIoTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void replacementPreservesPreviousVanillaBackup() throws Exception {
        Path target = folder.getRoot().toPath().resolve("player.dat");
        CheckpointIo.replace(target, temp -> Files.write(temp, bytes("first")));
        CheckpointIo.replace(target, temp -> Files.write(temp, bytes("second")));
        assertArrayEquals(bytes("second"), Files.readAllBytes(target));
        assertArrayEquals(bytes("first"), Files.readAllBytes(target.resolveSibling("player.dat_old")));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test public void failedWriteRetainsCurrentAndBackupAndRemovesTemporaryFile() throws Exception {
        Path target = folder.getRoot().toPath().resolve("player.dat");
        Files.write(target, bytes("current"));
        Files.write(target.resolveSibling("player.dat_old"), bytes("previous"));
        try {
            CheckpointIo.replace(target, temp -> {
                Files.write(temp, bytes("truncated"));
                throw new IOException("disk failed");
            });
            fail("Write failure ignored");
        } catch (IOException expected) { }
        assertArrayEquals(bytes("current"), Files.readAllBytes(target));
        assertArrayEquals(bytes("previous"), Files.readAllBytes(target.resolveSibling("player.dat_old")));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test public void failedAtomicReplacementRetainsCurrentFile() throws Exception {
        Path target = folder.getRoot().toPath().resolve("player.dat");
        Files.write(target, bytes("current"));
        Path blockedBackup = Files.createDirectory(target.resolveSibling("player.dat_old"));
        Files.write(blockedBackup.resolve("blocker"), bytes("keep"));
        try {
            CheckpointIo.replace(target, temp -> Files.write(temp, bytes("new")));
            fail("Blocked backup replacement accepted");
        } catch (IOException expected) { }
        assertArrayEquals(bytes("current"), Files.readAllBytes(target));
        assertArrayEquals(bytes("keep"), Files.readAllBytes(blockedBackup.resolve("blocker")));
        assertEquals(2, folder.getRoot().list().length);
    }

    @Test public void failedWorkerStillDrainsOtherWriters() throws Exception {
        verifyBarrier(false);
    }

    @Test public void interruptedCallerStillDrainsWritersAndRestoresInterrupt() throws Exception {
        verifyBarrier(true);
    }

    private void verifyBarrier(boolean interrupt) throws Exception {
        CountDownLatch writing = new CountDownLatch(1), release = new CountDownLatch(1), returned = new CountDownLatch(1);
        AtomicBoolean written = new AtomicBoolean(), interrupted = new AtomicBoolean();
        AtomicReference<Throwable> outcome = new AtomicReference<Throwable>();
        try (CheckpointIo io = new CheckpointIo(2)) {
            Thread caller = new Thread(() -> {
                try {
                    io.writeAll(Arrays.asList(() -> { throw new IOException("one disk write failed"); }, () -> {
                        writing.countDown();
                        if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test timed out");
                        written.set(true);
                        return null;
                    }));
                } catch (Throwable ex) { outcome.set(ex); }
                finally { interrupted.set(Thread.currentThread().isInterrupted()); returned.countDown(); }
            });
            caller.start();
            try {
                assertTrue(writing.await(5, TimeUnit.SECONDS));
                if (interrupt) caller.interrupt();
                assertFalse("Returned while a writer was still running", returned.await(100, TimeUnit.MILLISECONDS));
            } finally { release.countDown(); }
            assertTrue(returned.await(5, TimeUnit.SECONDS));
            assertTrue(written.get());
            assertTrue(outcome.get() instanceof IOException);
            assertEquals(interrupt, interrupted.get());
        }
    }

    @Test public void closedWriterRejectsWork() throws Exception {
        CheckpointIo io = new CheckpointIo(2);
        io.close();
        try {
            io.writeAll(Collections.singletonList(() -> { fail("Write after shutdown"); return null; }));
            fail("Submission failure ignored");
        } catch (IOException expected) { }
    }

    @Test public void unchangedJsonSkipsWritesButDoesNotTrustStaleMemory() throws Exception {
        Path target = folder.getRoot().toPath().resolve("advancements/player.json");
        byte[] checkpoint = bytes("{\"done\":true}");
        CheckpointIo.replaceJsonIfChanged(target, checkpoint);
        FileTime old = FileTime.fromMillis(123456000L);
        Files.setLastModifiedTime(target, old);
        CheckpointIo.replaceJsonIfChanged(target, checkpoint);
        assertEquals(old, Files.getLastModifiedTime(target));
        Files.write(target, bytes("{\"done\":false}"));
        CheckpointIo.replaceJsonIfChanged(target, checkpoint);
        assertArrayEquals(checkpoint, Files.readAllBytes(target));
        assertFalse(Files.exists(target.resolveSibling("player.json_old")));
    }

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }
}
