import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import org.objectweb.asm.*;
import org.lwjgl.sdl.SDLInit;
import org.lwjgl.sdl.SDLVideo;

/** Test-only SDL adapter: render a real client without displaying or focusing its window. */
public final class PrepareHiddenClient {
    private static final long HIDDEN_FLAGS = 0x80000008L;
    private static final long REMOVE_FLAGS = 0x1L | 0x100L | 0x200L | 0x400L | 0x1000L | 0x4000L | 0x8000L | 0x10000L | 0x100000L;
    private static final Set<String> VIDEO_NOOP = Set.of("SDL_ShowWindow", "SDL_RaiseWindow", "SDL_RestoreWindow",
        "SDL_MaximizeWindow", "SDL_SetWindowFullscreen", "SDL_SetWindowFocusable", "SDL_SetWindowKeyboardGrab",
        "SDL_SetWindowMouseGrab", "SDL_ShowWindowSystemMenu", "SDL_FlashWindow");
    private static final Set<String> MOUSE_NOOP = Set.of("SDL_WarpMouseInWindow", "SDL_WarpMouseGlobal",
        "SDL_SetWindowRelativeMouseMode", "SDL_CaptureMouse", "SDL_ShowCursor", "SDL_HideCursor");

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && args[0].equals("verify")) {
            if (!SDLInit.SDL_Init(SDLInit.SDL_INIT_VIDEO)) throw new AssertionError("SDL initialization failed");
            long window = 0;
            try {
                window = SDLVideo.SDL_CreateWindow("NewGodWar hidden render test", 64, 64, 0L);
                if (window == 0) throw new AssertionError("Hidden window creation failed");
                SDLVideo.SDL_ShowWindow(window);
                SDLVideo.SDL_RaiseWindow(window);
                long flags = SDLVideo.SDL_GetWindowFlags(window);
                if ((flags & HIDDEN_FLAGS) != HIDDEN_FLAGS || (flags & 0x200L) != 0) throw new AssertionError("Window visibility/focus: " + flags);
                System.out.println("PASS: SDL window stays HIDDEN and NOT_FOCUSABLE after show/raise requests; flags=" + flags);
            } finally {
                if (window != 0) SDLVideo.SDL_DestroyWindow(window);
                SDLInit.SDL_Quit();
            }
            return;
        }
        if (args.length != 2) throw new IllegalArgumentException("input.jar output.jar or verify");
        int changed = 0;
        try (JarFile input = new JarFile(args[0]); JarOutputStream output = new JarOutputStream(Files.newOutputStream(Path.of(args[1])))) {
            for (var entries = input.entries(); entries.hasMoreElements();) {
                var entry = entries.nextElement();
                byte[] data = input.getInputStream(entry).readAllBytes();
                if (Set.of("org/lwjgl/sdl/SDLVideo.class", "org/lwjgl/sdl/SDLMouse.class", "org/lwjgl/sdl/SDLMessageBox.class").contains(entry.getName())) {
                    data = patch(data, entry.getName());
                    changed++;
                }
                var copy = new JarEntry(entry.getName());
                copy.setTime(entry.getTime());
                output.putNextEntry(copy);
                output.write(data);
                output.closeEntry();
            }
        }
        if (changed != 3) throw new AssertionError("Unexpected SDL library version: " + changed);
        System.out.println("Patched 3 SDL classes for isolated hidden rendering.");
    }

    private static byte[] patch(byte[] bytes, String source) {
        var reader = new ClassReader(bytes);
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        int[] creates = {0};
        reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
            @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                var mv = super.visitMethod(access, name, desc, signature, exceptions);
                boolean video = source.endsWith("SDLVideo.class");
                boolean mouse = source.endsWith("SDLMouse.class");
                if (video && name.equals("nSDL_CreateWindow") && desc.equals("(JIIJ)J")) {
                    creates[0]++;
                    return new MethodVisitor(Opcodes.ASM9, mv) {
                        @Override public void visitCode() {
                            super.visitCode();
                            visitVarInsn(Opcodes.LLOAD, 4);
                            visitLdcInsn(~REMOVE_FLAGS);
                            visitInsn(Opcodes.LAND);
                            visitLdcInsn(HIDDEN_FLAGS);
                            visitInsn(Opcodes.LOR);
                            visitVarInsn(Opcodes.LSTORE, 4);
                        }
                    };
                }
                boolean noop = video && VIDEO_NOOP.contains(name) || mouse && MOUSE_NOOP.contains(name)
                    || source.endsWith("SDLMessageBox.class") && name.startsWith("SDL_Show");
                boolean refuse = video && (name.contains("CreateWindowWithProperties") || name.contains("CreatePopupWindow"));
                if (!noop && !refuse) return mv;
                mv.visitCode();
                if (refuse) {
                    mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException");
                    mv.visitInsn(Opcodes.DUP);
                    mv.visitLdcInsn("Unsupported window creation path in hidden visual test");
                    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false);
                    mv.visitInsn(Opcodes.ATHROW);
                } else if (Type.getReturnType(desc).getSort() == Type.BOOLEAN) {
                    mv.visitInsn(Opcodes.ICONST_1);
                    mv.visitInsn(Opcodes.IRETURN);
                } else if (Type.getReturnType(desc).getSort() == Type.VOID) {
                    mv.visitInsn(Opcodes.RETURN);
                } else throw new AssertionError("Unexpected method: " + name + desc);
                mv.visitMaxs(0, 0);
                mv.visitEnd();
                return null;
            }
        }, 0);
        if (source.endsWith("SDLVideo.class") && creates[0] != 1) throw new AssertionError("CreateWindow signature changed");
        return writer.toByteArray();
    }
}
