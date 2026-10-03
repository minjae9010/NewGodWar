import java.nio.file.*;
import java.util.jar.*;
import org.objectweb.asm.*;

/** Replace only the incompatible command adapter, preserving the loopback patch. */
public final class PrepareClientActions {
    public static void main(String[] args) throws Exception {
        String target = "xyz/langyo/minecraft/mcp/common/ReflectionHelper.class";
        int[] changed = {0};
        try (JarFile source = new JarFile(args[0]);
             JarOutputStream output = new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            var entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                byte[] bytes = source.getInputStream(entry).readAllBytes();
                if (entry.getName().equals(target)) {
                    ClassWriter writer = new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9, writer) {
                        @Override public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                            MethodVisitor method = super.visitMethod(access, name, descriptor, signature, exceptions);
                            if (!name.equals("sendCommand") || !descriptor.equals("(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;")) return method;
                            changed[0]++;
                            method.visitCode();
                            method.visitVarInsn(Opcodes.ALOAD, 0);
                            method.visitVarInsn(Opcodes.ALOAD, 1);
                            method.visitMethodInsn(Opcodes.INVOKESTATIC, "kr/newgodwar/visualtest/TestClientActions", "command", descriptor, false);
                            method.visitInsn(Opcodes.ARETURN);
                            method.visitMaxs(2, 2);
                            method.visitEnd();
                            return null;
                        }
                    }, 0);
                    bytes = writer.toByteArray();
                }
                output.putNextEntry(new JarEntry(entry.getName()));
                output.write(bytes);
                output.closeEntry();
            }
            String helper = "kr/newgodwar/visualtest/TestClientActions.class";
            output.putNextEntry(new JarEntry(helper));
            output.write(Files.readAllBytes(Path.of(args[2]).resolve(helper)));
            output.closeEntry();
        }
        if (changed[0] != 1) throw new IllegalStateException("Unexpected upstream command adapter");
        System.out.println("PASS: replaced one command adapter with Minecraft 26.3 internal actions");
    }
}
