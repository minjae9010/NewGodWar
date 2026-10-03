import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Glow-first effect painting. Every sprite is composed from a blurred bloom, a tinted body and a
 * white-hot core, so it reads as light rather than as a flat icon. Work happens on a 256px canvas
 * and is downsampled to the 128px pack texture.
 */
final class VfxPainter {
    static final int CANVAS = 256, OUT = 128;
    static final float[] FADES = {.18f, .42f, .72f, 1f};
    /** Gameplay boundaries use the sigil's outer ring core; ArtModels mirrors this radius. */
    static final double SIGIL_RADIUS = 118;
    /** Static layers with four fade steps; animated layers live in VfxAnimator. */
    static final List<String> LAYERS = List.of("flash", "wave", "sigil", "ring");

    record Family(String name, Color glow, Color body) {
        /**
         * Minecraft blends item sprites normally, never additively: a dim saturated halo only darkens what is
         * behind it. Halos therefore use a lightened tint so they still read as emitted light.
         */
        Color halo() { return mix(glow, Color.WHITE, .42f); }
    }
    static final Map<String, Family> FAMILIES = new LinkedHashMap<>();
    static {
        family("gold", 0xffc93a, 0xfff0b0); family("fire", 0xff5a14, 0xffc070);
        family("ice", 0x48c8ff, 0xd8f6ff); family("nature", 0x4fe05a, 0xd2ffb8);
        family("arcane", 0x9a5cff, 0xe2ccff); family("shadow", 0x5a1fc8, 0xb890ff);
        family("blood", 0xff2448, 0xffb0bc); family("holy", 0xffe27a, 0xfffbe6);
        family("water", 0x14c6e6, 0xbdf6ff); family("love", 0xff4fa8, 0xffd0e8);
        family("storm", 0x7f9cff, 0xeef2ff); family("earth", 0xd08a38, 0xffe0b0);
        family("wind", 0x5cf0d0, 0xeafff8); family("toxic", 0x9cff1e, 0xeaffc0);
        family("steel", 0x9ec4e6, 0xf2f8ff);
    }
    static void family(String name, int glow, int body) { FAMILIES.put(name, new Family(name, new Color(glow), new Color(body))); }

    // ---------------------------------------------------------------- emblem

    /** The ability's own silhouette, lit from inside with a wide coloured bloom. */
    static BufferedImage emblem(List<Shape> ink, Color tint, Family family, boolean rainbow) {
        BufferedImage strokes = canvas(), fills = canvas(), core = canvas();
        Graphics2D s = graphics(strokes), f = graphics(fills), c = graphics(core);
        Color body = mix(tint, family.body(), .45f), deep = mix(family.glow(), Color.BLACK, .25f);
        int n = 0;
        for (Shape shape : ink) {
            if (rainbow) { body = Color.getHSBColor(n / 7f, .55f, 1); family = new Family(family.name(), Color.getHSBColor(n++ / 7f, .9f, 1), body); }
            if (closed(shape)) {
                f.setPaint(new GradientPaint(70, 30, alpha(body, .9f), 190, 230, alpha(deep, .78f)));
                f.fill(shape);
            }
            s.setColor(family.halo()); s.setStroke(round(20)); s.draw(shape); if (closed(shape)) s.fill(shape);
            c.setColor(mix(body, family.glow(), .35f)); c.setStroke(round(11f)); c.draw(shape);
            c.setColor(new Color(255, 253, 244)); c.setStroke(round(4.2f)); c.draw(shape);
        }
        s.dispose(); f.dispose(); c.dispose();
        BufferedImage out = canvas();
        Graphics2D g = graphics(out);
        // Wide halo, tight halo, then the lit body; the halo makes it read as emitted light.
        g.setComposite(AlphaComposite.SrcOver.derive(.95f)); g.drawImage(blur(strokes, 16), 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver.derive(.9f)); g.drawImage(blur(strokes, 6), 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver); g.drawImage(fills, 0, 0, null);
        g.drawImage(blur(core, 1), 0, 0, null); g.drawImage(core, 0, 0, null);
        g.dispose();
        return shrink(boost(out, 1.25f));
    }

    // ---------------------------------------------------------------- shared effect layers

    static BufferedImage layer(String layer, Family family) {
        BufferedImage full = switch (layer) {
            case "flash" -> flash(family);
            case "wave" -> wave(family);
            case "sigil" -> sigil(family);
            case "ring" -> ring(family);
            default -> throw new IllegalArgumentException(layer);
        };
        // Boundaries and shockwaves stretch across whole areas (up to 12 blocks), so they keep full resolution.
        return layer.equals("ring") || layer.equals("wave") ? full : shrink(full);
    }

    /** Impact flash: hot core, coloured corona and uneven light rays. */
    static BufferedImage flash(Family fam) {
        BufferedImage rays = canvas();
        Graphics2D r = graphics(rays);
        Random random = new Random(fam.name().hashCode());
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6 + random.nextDouble() * .18, length = (i % 2 == 0 ? 120 : 78) - random.nextDouble() * 14;
            double width = i % 2 == 0 ? 11 : 7;
            Path2D ray = new Path2D.Double();
            ray.moveTo(128 + Math.cos(angle + Math.PI / 2) * width, 128 + Math.sin(angle + Math.PI / 2) * width);
            ray.lineTo(128 + Math.cos(angle) * length, 128 + Math.sin(angle) * length);
            ray.lineTo(128 + Math.cos(angle - Math.PI / 2) * width, 128 + Math.sin(angle - Math.PI / 2) * width);
            ray.closePath();
            r.setPaint(new RadialGradientPaint(128, 128, (float) length, new float[]{0, .4f, 1},
                new Color[]{Color.WHITE, alpha(fam.body(), .95f), alpha(fam.halo(), 0)}));
            r.fill(ray);
        }
        r.dispose();
        BufferedImage out = canvas();
        Graphics2D g = graphics(out);
        g.setPaint(new RadialGradientPaint(128, 128, 126, new float[]{0, .16f, .34f, .6f, 1},
            new Color[]{Color.WHITE, alpha(fam.body(), 1f), alpha(fam.halo(), .72f), alpha(fam.halo(), .22f), alpha(fam.halo(), 0)}));
        g.fillRect(0, 0, CANVAS, CANVAS);
        g.drawImage(blur(rays, 5), 0, 0, null); g.drawImage(rays, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Ground shockwave: a bright leading edge with a soft wake behind it. */
    static BufferedImage wave(Family fam) {
        BufferedImage out = canvas();
        Graphics2D g = graphics(out);
        g.setPaint(new RadialGradientPaint(128, 128, 127, new float[]{0, .55f, .8f, .9f, .935f, .97f, 1},
            new Color[]{alpha(fam.halo(), 0), alpha(fam.halo(), 0), alpha(fam.halo(), .3f), alpha(fam.body(), .85f),
                Color.WHITE, alpha(fam.halo(), .55f), alpha(fam.halo(), 0)}));
        g.fillRect(0, 0, CANVAS, CANVAS);
        g.dispose();
        return out;
    }

    /** Magic circle: outer boundary ring, rune band, hexagram and centre seal. */
    static BufferedImage sigil(Family fam) {
        BufferedImage lines = canvas();
        Graphics2D l = graphics(lines);
        l.setColor(Color.WHITE);
        l.setStroke(round(3.4f)); circle(l, SIGIL_RADIUS);
        l.setStroke(round(2f)); circle(l, 104); circle(l, 60); circle(l, 30);
        for (int i = 0; i < 36; i++) {
            double a = i * Math.PI / 18;
            if (i % 3 == 0) diamond(l, 128 + Math.cos(a) * 111, 128 + Math.sin(a) * 111, 4.5);
            else l.draw(new Line2D.Double(128 + Math.cos(a) * 107, 128 + Math.sin(a) * 107, 128 + Math.cos(a) * 115, 128 + Math.sin(a) * 115));
        }
        l.setStroke(round(2.4f));
        for (int t = 0; t < 2; t++) {
            Path2D tri = new Path2D.Double();
            for (int i = 0; i < 3; i++) {
                double a = -Math.PI / 2 + t * Math.PI + i * Math.PI * 2 / 3;
                if (i == 0) tri.moveTo(128 + Math.cos(a) * 100, 128 + Math.sin(a) * 100);
                else tri.lineTo(128 + Math.cos(a) * 100, 128 + Math.sin(a) * 100);
            }
            tri.closePath(); l.draw(tri);
        }
        for (int i = 0; i < 6; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 3;
            l.draw(new Ellipse2D.Double(128 + Math.cos(a) * 100 - 7, 128 + Math.sin(a) * 100 - 7, 14, 14));
        }
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4, len = i % 2 == 0 ? 26 : 16;
            l.draw(new Line2D.Double(128, 128, 128 + Math.cos(a) * len, 128 + Math.sin(a) * len));
        }
        l.dispose();
        BufferedImage colored = tint(lines, fam.body());
        BufferedImage out = canvas();
        Graphics2D g = graphics(out);
        g.drawImage(tint(blur(lines, 6), fam.halo()), 0, 0, null);
        g.drawImage(tint(blur(lines, 6), fam.halo()), 0, 0, null);
        g.drawImage(colored, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver.derive(.7f)); g.drawImage(lines, 0, 0, null);
        g.dispose();
        return out;
    }

    /** Thin gameplay boundary for large areas: a lit rim and rune ticks, nothing over the ground inside. */
    static BufferedImage ring(Family fam) {
        BufferedImage lines = canvas();
        Graphics2D l = graphics(lines);
        l.setColor(Color.WHITE);
        l.setStroke(round(2.6f)); circle(l, SIGIL_RADIUS);
        l.setStroke(round(1.1f)); circle(l, SIGIL_RADIUS - 7);
        for (int i = 0; i < 48; i++) {
            double a = i * Math.PI / 24;
            if (i % 4 == 0) diamond(l, 128 + Math.cos(a) * (SIGIL_RADIUS - 3.5), 128 + Math.sin(a) * (SIGIL_RADIUS - 3.5), 3.6);
        }
        l.dispose();
        // A comet of light on the rim; ArtModels spins the ring, so it races around the boundary.
        BufferedImage comet = canvas();
        Graphics2D k = graphics(comet);
        for (int i = 0; i < 40; i++) {
            float a = (1 - i / 40f);
            k.setColor(alpha(Color.WHITE, a * a));
            k.setStroke(round(2 + 5 * a));
            k.draw(new Arc2D.Double(128 - SIGIL_RADIUS, 128 - SIGIL_RADIUS, SIGIL_RADIUS * 2, SIGIL_RADIUS * 2, i * 2.2, 2.6, Arc2D.OPEN));
        }
        k.dispose();
        BufferedImage out = canvas();
        Graphics2D g = graphics(out);
        g.drawImage(tint(blur(comet, 6), fam.halo()), 0, 0, null);
        g.drawImage(tint(blur(lines, 3), fam.halo()), 0, 0, null);
        g.drawImage(tint(lines, fam.body()), 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver.derive(.8f)); g.drawImage(lines, 0, 0, null);
        g.setComposite(AlphaComposite.SrcOver); g.drawImage(comet, 0, 0, null);
        g.dispose();
        return out;
    }

    // ---------------------------------------------------------------- helpers

    static BufferedImage faded(BufferedImage source, int fade) {
        BufferedImage out = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
        float alpha = FADES[fade];
        for (int y = 0; y < source.getHeight(); y++) for (int x = 0; x < source.getWidth(); x++) {
            int argb = source.getRGB(x, y);
            int a = Math.round(((argb >>> 24) & 255) * alpha);
            out.setRGB(x, y, (a << 24) | (argb & 0xffffff));
        }
        return out;
    }

    static BufferedImage canvas() { return new BufferedImage(CANVAS, CANVAS, BufferedImage.TYPE_INT_ARGB); }
    static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        return g;
    }
    static BasicStroke round(float width) { return new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND); }
    static void circle(Graphics2D g, double r) { g.draw(new Ellipse2D.Double(128 - r, 128 - r, r * 2, r * 2)); }
    static void diamond(Graphics2D g, double x, double y, double r) { g.fill(poly(x, y - r, x + r * .6, y, x, y + r, x - r * .6, y)); }
    static Path2D poly(double... v) {
        Path2D p = new Path2D.Double(); p.moveTo(v[0], v[1]);
        for (int i = 2; i < v.length; i += 2) p.lineTo(v[i], v[i + 1]);
        p.closePath(); return p;
    }
    static Path2D path(double x, double y, double... v) {
        Path2D p = new Path2D.Double(); p.moveTo(x, y);
        for (int i = 0; i + 5 < v.length; i += 6) p.curveTo(v[i], v[i + 1], v[i + 2], v[i + 3], v[i + 4], v[i + 5]);
        p.closePath(); return p;
    }
    static Shape leaf(double x, double y, double s, double rot) {
        Path2D p = new Path2D.Double(); p.moveTo(0, -s);
        p.curveTo(s * .85, -s * .45, s * .65, s * .35, 0, s); p.curveTo(-s * .65, s * .35, -s * .85, -s * .45, 0, -s); p.closePath();
        AffineTransform a = new AffineTransform(); a.translate(x, y); a.rotate(rot); return a.createTransformedShape(p);
    }
    static boolean closed(Shape shape) {
        if (shape instanceof Ellipse2D || shape instanceof RectangularShape) return true;
        PathIterator it = shape.getPathIterator(null); double[] v = new double[6];
        while (!it.isDone()) { if (it.currentSegment(v) == PathIterator.SEG_CLOSE) return true; it.next(); }
        return false;
    }
    static Color alpha(Color c, float a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(Math.max(0, Math.min(1, a)) * 255)); }
    static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t), Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }
    static int blend(Color base, Color hot, double heat, double alpha) {
        heat = Math.max(0, Math.min(1, heat)); alpha = Math.max(0, Math.min(1, alpha));
        int r = (int) Math.round(base.getRed() + (hot.getRed() - base.getRed()) * heat);
        int g = (int) Math.round(base.getGreen() + (hot.getGreen() - base.getGreen()) * heat);
        int b = (int) Math.round(base.getBlue() + (hot.getBlue() - base.getBlue()) * heat);
        return ((int) Math.round(alpha * 255) << 24) | (r << 16) | (g << 8) | b;
    }
    /** Recolour a white mask while keeping its alpha. */
    static BufferedImage tint(BufferedImage mask, Color color) {
        BufferedImage out = new BufferedImage(mask.getWidth(), mask.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int rgb = color.getRGB() & 0xffffff;
        for (int y = 0; y < mask.getHeight(); y++) for (int x = 0; x < mask.getWidth(); x++)
            out.setRGB(x, y, (mask.getRGB(x, y) & 0xff000000) | rgb);
        return out;
    }
    /** Raise faint bloom alpha so the halo survives downsampling and the darkest fade step. */
    static BufferedImage boost(BufferedImage image, float gain) {
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++) {
            int argb = image.getRGB(x, y), a = Math.min(255, Math.round(((argb >>> 24) & 255) * gain));
            image.setRGB(x, y, (a << 24) | (argb & 0xffffff));
        }
        return image;
    }
    static BufferedImage shrink(BufferedImage source) { return shrink(source, OUT); }
    static BufferedImage shrink(BufferedImage source, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int k = source.getWidth() / size;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
            // Premultiplied box filter: transparent pixels never darken a glowing edge.
            double a = 0, r = 0, g = 0, b = 0;
            for (int j = 0; j < k; j++) for (int i = 0; i < k; i++) {
                int argb = source.getRGB(x * k + i, y * k + j); double alpha = ((argb >>> 24) & 255) / 255.0;
                a += alpha; r += alpha * ((argb >> 16) & 255); g += alpha * ((argb >> 8) & 255); b += alpha * (argb & 255);
            }
            int n = k * k;
            out.setRGB(x, y, a <= 0 ? 0 : ((int) Math.round(a / n * 255) << 24) | ((int) Math.round(r / a) << 16) | ((int) Math.round(g / a) << 8) | (int) Math.round(b / a));
        }
        return out;
    }
    /** Three box passes approximate a Gaussian; colour is averaged premultiplied. */
    static BufferedImage blur(BufferedImage source, int radius) {
        int w = source.getWidth(), h = source.getHeight();
        double[][] ch = new double[4][w * h];
        for (int i = 0; i < w * h; i++) {
            int argb = source.getRGB(i % w, i / w); double a = ((argb >>> 24) & 255) / 255.0;
            ch[0][i] = a; ch[1][i] = a * ((argb >> 16) & 255); ch[2][i] = a * ((argb >> 8) & 255); ch[3][i] = a * (argb & 255);
        }
        for (int pass = 0; pass < 3; pass++) for (double[] c : ch) { box(c, w, h, radius, true); box(c, w, h, radius, false); }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < w * h; i++) {
            double a = ch[0][i];
            out.setRGB(i % w, i / w, a <= 1e-4 ? 0 : ((int) Math.round(Math.min(1, a) * 255) << 24)
                | (clamp(ch[1][i] / a) << 16) | (clamp(ch[2][i] / a) << 8) | clamp(ch[3][i] / a));
        }
        return out;
    }
    static int clamp(double v) { return (int) Math.max(0, Math.min(255, Math.round(v))); }
    static void box(double[] c, int w, int h, int r, boolean horizontal) {
        double[] copy = c.clone(); int lines = horizontal ? h : w, length = horizontal ? w : h;
        for (int line = 0; line < lines; line++) {
            double sum = 0;
            for (int i = -r; i <= r; i++) sum += at(copy, w, horizontal, line, Math.max(0, Math.min(length - 1, i)));
            for (int i = 0; i < length; i++) {
                int index = horizontal ? line * w + i : i * w + line;
                c[index] = sum / (2 * r + 1);
                sum += at(copy, w, horizontal, line, Math.min(length - 1, i + r + 1)) - at(copy, w, horizontal, line, Math.max(0, i - r));
            }
        }
    }
    static double at(double[] c, int w, boolean horizontal, int line, int i) { return horizontal ? c[line * w + i] : c[i * w + line]; }
}
