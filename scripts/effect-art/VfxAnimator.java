import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

/**
 * Frame-by-frame animation for the effect pack.
 *
 * One-shot sequences (burst, slash, emblem tracing) are separate sprites; the server picks the frame from
 * the scene's age, so each cast replays them from the start. Loops (beam flow, spark twinkle, element debris)
 * are vertical strips animated by the client through .mcmeta, so they move smoothly between server updates.
 */
final class VfxAnimator {
    static final int ONE_SHOT = 6, TRACE = 2, LOOP = 8, LOOP_SIZE = 64;
    static final List<String> SEQUENCES = List.of("burst", "slash", "craft", "chart", "crash");
    static final List<String> LOOPS = List.of("beam", "spark", "shard");

    static List<BufferedImage> sequence(String layer, VfxPainter.Family fam) {
        List<BufferedImage> frames = new ArrayList<>();
        for (int f = 0; f < ONE_SHOT; f++) frames.add(VfxPainter.shrink(switch (layer) {
            case "burst" -> burst(fam, f);
            case "slash" -> slash(fam, f);
            case "craft" -> craft(fam, f);
            case "chart" -> chart(fam, f, true);
            case "crash" -> chart(fam, f, false);
            default -> throw new IllegalArgumentException(layer);
        }));
        return frames;
    }

    static List<BufferedImage> loop(String layer, VfxPainter.Family fam) {
        List<BufferedImage> frames = new ArrayList<>();
        for (int f = 0; f < LOOP; f++) {
            BufferedImage full = switch (layer) {
                case "beam" -> beam(fam, f);
                case "spark" -> spark(fam, f);
                case "shard" -> shard(fam, f);
                default -> throw new IllegalArgumentException(layer);
            };
            frames.add(VfxPainter.shrink(full, LOOP_SIZE));
        }
        return frames;
    }

    // ---------------------------------------------------------------- one-shot: explosion burst

    /** Flash → shock ring tears outward → debris streaks → embers. Frame 0 is the instant of impact. */
    static BufferedImage burst(VfxPainter.Family fam, int f) {
        double[] coreR = {46, 70, 52, 30, 16, 6}, coreA = {1, 1, .8, .5, .28, .1};
        double[] rayL = {84, 126, 118, 96, 70, 40}, rayA = {.85, 1, .62, .32, .14, 0};
        double[] ringR = {18, 46, 78, 102, 116, 124}, ringW = {10, 13, 10, 7, 4.5, 2.5}, ringA = {.5, .95, .9, .62, .36, .14};
        BufferedImage light = VfxPainter.canvas();
        Graphics2D g = VfxPainter.graphics(light);
        Random random = new Random(fam.name().hashCode() * 31L);
        // Rays: uneven and slightly rotated per frame, so the burst visibly shudders.
        for (int i = 0; i < 14; i++) {
            double angle = i * Math.PI / 7 + random.nextDouble() * .25 + f * .05;
            double length = rayL[f] * (i % 2 == 0 ? 1 : .62) * (.85 + random.nextDouble() * .3), width = (i % 2 == 0 ? 9 : 6) * (1 - f * .12);
            if (rayA[f] <= 0) break;
            Path2D ray = new Path2D.Double();
            ray.moveTo(128 + Math.cos(angle + Math.PI / 2) * width, 128 + Math.sin(angle + Math.PI / 2) * width);
            ray.lineTo(128 + Math.cos(angle) * length, 128 + Math.sin(angle) * length);
            ray.lineTo(128 + Math.cos(angle - Math.PI / 2) * width, 128 + Math.sin(angle - Math.PI / 2) * width);
            ray.closePath();
            g.setPaint(new RadialGradientPaint(128, 128, (float) Math.max(1, length), new float[]{0, .45f, 1},
                new Color[]{VfxPainter.alpha(Color.WHITE, (float) rayA[f]), VfxPainter.alpha(fam.body(), (float) rayA[f] * .9f), VfxPainter.alpha(fam.halo(), 0)}));
            g.fill(ray);
        }
        // Shock ring.
        g.setStroke(VfxPainter.round((float) ringW[f]));
        g.setColor(VfxPainter.alpha(Color.WHITE, (float) ringA[f]));
        g.draw(new Ellipse2D.Double(128 - ringR[f], 128 - ringR[f], ringR[f] * 2, ringR[f] * 2));
        // Debris: bright streaks thrown outward, then cooling embers.
        if (f >= 1) for (int i = 0; i < 16; i++) {
            double angle = random.nextDouble() * Math.PI * 2, speed = .7 + random.nextDouble() * .5;
            double r = ringR[f] * speed, tail = f < 4 ? 14 - f * 2 : 3, a = Math.max(0, 1 - (f - 1) * .2);
            g.setColor(VfxPainter.alpha(i % 3 == 0 ? Color.WHITE : fam.body(), (float) a));
            g.setStroke(VfxPainter.round(f < 4 ? 3.5f : 4.5f));
            g.draw(new Line2D.Double(128 + Math.cos(angle) * r, 128 + Math.sin(angle) * r,
                128 + Math.cos(angle) * (r - tail), 128 + Math.sin(angle) * (r - tail)));
        }
        g.dispose();
        BufferedImage out = VfxPainter.canvas();
        Graphics2D o = VfxPainter.graphics(out);
        // Hot core and its corona.
        o.setPaint(new RadialGradientPaint(128, 128, (float) (coreR[f] * 1.9), new float[]{0, .35f, .7f, 1},
            new Color[]{VfxPainter.alpha(Color.WHITE, (float) coreA[f]), VfxPainter.alpha(fam.body(), (float) coreA[f] * .9f),
                VfxPainter.alpha(fam.halo(), (float) coreA[f] * .35f), VfxPainter.alpha(fam.halo(), 0)}));
        o.fillRect(0, 0, 256, 256);
        o.drawImage(VfxPainter.tint(VfxPainter.blur(light, 7), fam.halo()), 0, 0, null);
        o.drawImage(light, 0, 0, null);
        o.dispose();
        return out;
    }

    // ---------------------------------------------------------------- one-shot: blade sweep

    /** The blade's edge travels along the crescent, then the trail dissolves from its tail. */
    static BufferedImage slash(VfxPainter.Family fam, int f) {
        double[] head = {-.35, .35, 1, 1, 1, 1}, tail = {-1, -1, -1, -.45, .15, .6}, gain = {1, 1, 1, .78, .5, .26};
        BufferedImage out = VfxPainter.canvas();
        Color base = VfxPainter.mix(fam.glow(), Color.WHITE, .2f);
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
            double dx = x - 20, dy = y - 128, r = Math.hypot(dx, dy), angle = Math.atan2(dy, dx);
            if (angle > head[f] || angle < tail[f] || Math.abs(angle) > 1) { out.setRGB(x, y, 0); continue; }
            double taper = Math.pow(Math.cos(angle * Math.PI / 2), .8);
            // Thicker right behind the travelling edge, like a motion smear.
            double lead = f <= 2 ? Math.exp(-Math.pow((head[f] - angle) / .25, 2)) : 0;
            double width = 8 + 78 * taper * (1 + lead * .4), depth = 196 - r;
            double a = depth < 0 ? Math.exp(-depth * depth / 10) * taper : depth < width ? Math.pow(1 - depth / width, 1.6) * taper : 0;
            double hot = depth > -3 && depth < 5 ? Math.exp(-depth * depth / 8) : 0;
            double heat = Math.min(1, hot + a * .25 + lead * .8);
            out.setRGB(x, y, VfxPainter.blend(base, Color.WHITE, heat, Math.min(1, (a * .95 + hot * taper + lead * a) * gain[f])));
        }
        BufferedImage glow = VfxPainter.blur(out, 5);
        Graphics2D g = VfxPainter.graphics(glow); g.drawImage(out, 0, 0, null);
        if (f <= 2) {
            // A star flare rides the blade's edge.
            double angle = head[f];
            g.drawImage(spark(fam, 0), (int) (20 + Math.cos(angle) * 196 - 128 * .35), (int) (128 + Math.sin(angle) * 196 - 128 * .35), 90, 90, null);
        }
        g.dispose();
        return glow;
    }

    // ---------------------------------------------------------------- one-shot: workshop and market

    /** A 3×3 crafting grid: cells light up as materials arrive, then fuse into one glowing centre. */
    static BufferedImage craft(VfxPainter.Family fam, int f) {
        int[] lit = {1, 3, 6, 9, 9, 9};
        int[] order = {4, 1, 7, 3, 5, 0, 8, 2, 6};
        double fuse = f == 4 ? 1 : f == 5 ? .45 : 0, dim = f == 5 ? .45 : 1;
        BufferedImage lines = VfxPainter.canvas(), fills = VfxPainter.canvas();
        Graphics2D l = VfxPainter.graphics(lines), b = VfxPainter.graphics(fills);
        Set<Integer> on = new HashSet<>();
        for (int i = 0; i < lit[f]; i++) on.add(order[i]);
        for (int cell = 0; cell < 9; cell++) {
            double x = 50 + (cell % 3) * 54, y = 50 + (cell / 3) * 54;
            Shape box = new RoundRectangle2D.Double(x, y, 48, 48, 12, 12);
            boolean active = on.contains(cell);
            l.setColor(VfxPainter.alpha(Color.WHITE, (float) ((active ? 1 : .35) * dim)));
            l.setStroke(VfxPainter.round(active ? 4 : 2.5f)); l.draw(box);
            if (active && fuse < 1) {
                b.setPaint(new GradientPaint((float) x, (float) y, VfxPainter.alpha(fam.body(), (float) (.9 * dim)), (float) x + 48, (float) y + 48, VfxPainter.alpha(fam.glow(), (float) (.6 * dim))));
                b.fill(new RoundRectangle2D.Double(x + 8, y + 8, 32, 32, 8, 8));
            }
        }
        l.dispose(); b.dispose();
        BufferedImage out = VfxPainter.canvas();
        Graphics2D g = VfxPainter.graphics(out);
        g.drawImage(VfxPainter.tint(VfxPainter.blur(lines, 6), fam.halo()), 0, 0, null);
        g.drawImage(VfxPainter.blur(fills, 3), 0, 0, null);
        g.drawImage(fills, 0, 0, null);
        g.drawImage(VfxPainter.tint(lines, fam.body()), 0, 0, null);
        if (fuse > 0) {
            g.setPaint(new RadialGradientPaint(128, 128, 110, new float[]{0, .3f, 1},
                new Color[]{VfxPainter.alpha(Color.WHITE, (float) fuse), VfxPainter.alpha(fam.body(), (float) (.8 * fuse)), VfxPainter.alpha(fam.halo(), 0)}));
            g.fillRect(0, 0, 256, 256);
        }
        g.dispose();
        return out;
    }

    /** A market chart drawn live: the line climbs (or collapses), bars rise beneath it, and the arrow lands. */
    static BufferedImage chart(VfxPainter.Family fam, int f, boolean up) {
        double[][] points = up ? new double[][]{{44, 200}, {80, 168}, {108, 182}, {148, 120}, {178, 134}, {214, 52}}
            : new double[][]{{44, 56}, {80, 88}, {108, 72}, {148, 140}, {178, 126}, {214, 210}};
        double[] drawn = {.25, .5, .75, 1, 1, 1};
        double glow = f == 4 ? 1.25 : f == 5 ? .5 : 1;
        BufferedImage lines = VfxPainter.canvas();
        Graphics2D l = VfxPainter.graphics(lines);
        l.setColor(VfxPainter.alpha(Color.WHITE, .55f)); l.setStroke(VfxPainter.round(3));
        l.draw(new Line2D.Double(30, 30, 30, 226)); l.draw(new Line2D.Double(30, 226, 230, 226));
        for (int i = 0; i < 5; i++) {
            double h = (up ? 24 + i * 26 : 130 - i * 24) * Math.min(1, (f + 1) / 4.0);
            l.fill(new Rectangle2D.Double(48 + i * 36, 222 - h, 18, h));
        }
        Path2D graph = new Path2D.Double(); graph.moveTo(points[0][0], points[0][1]);
        double total = 0;
        for (int i = 1; i < points.length; i++) total += Math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1]);
        double budget = total * drawn[f], tipX = points[0][0], tipY = points[0][1], angle = 0;
        for (int i = 1; i < points.length && budget > 0; i++) {
            double len = Math.hypot(points[i][0] - points[i - 1][0], points[i][1] - points[i - 1][1]), t = Math.min(1, budget / len);
            tipX = points[i - 1][0] + (points[i][0] - points[i - 1][0]) * t; tipY = points[i - 1][1] + (points[i][1] - points[i - 1][1]) * t;
            angle = Math.atan2(points[i][1] - points[i - 1][1], points[i][0] - points[i - 1][0]);
            graph.lineTo(tipX, tipY); budget -= len;
        }
        l.setColor(Color.WHITE); l.setStroke(VfxPainter.round(9)); l.draw(graph);
        if (f >= 3) {
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(tipX + Math.cos(angle) * 22, tipY + Math.sin(angle) * 22);
            arrow.lineTo(tipX + Math.cos(angle + 2.4) * 22, tipY + Math.sin(angle + 2.4) * 22);
            arrow.lineTo(tipX + Math.cos(angle - 2.4) * 22, tipY + Math.sin(angle - 2.4) * 22);
            arrow.closePath(); l.fill(arrow);
        }
        l.dispose();
        BufferedImage out = VfxPainter.canvas();
        Graphics2D g = VfxPainter.graphics(out);
        g.setComposite(AlphaComposite.SrcOver.derive((float) Math.min(1, glow)));
        g.drawImage(VfxPainter.tint(VfxPainter.blur(lines, 8), fam.halo()), 0, 0, null);
        g.drawImage(VfxPainter.tint(lines, fam.body()), 0, 0, null);
        if (f <= 3) g.drawImage(spark(fam, 0), (int) tipX - 40, (int) tipY - 40, 80, 80, null);
        g.dispose();
        return f == 5 ? VfxPainter.faded(out, 1) : out;
    }

    // ---------------------------------------------------------------- loops

    /** Energy streaks run up the column; the core breathes. */
    static BufferedImage beam(VfxPainter.Family fam, int f) {
        BufferedImage out = VfxPainter.canvas();
        double phase = f / (double) LOOP;
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
            double v = 1 - y / 255.0, dx = (x - 127.5) / 128.0;
            double rise = Math.min(1, (1 - v) * 9) * Math.pow(Math.max(0, 1 - v), .35) * Math.min(1, v * 5 + .15);
            double breathe = 1 + .12 * Math.sin(2 * Math.PI * phase);
            double coreI = Math.exp(-Math.pow(dx / (.13 * breathe), 2)), glowI = Math.exp(-Math.pow(dx / .32, 2));
            double flow = 0;
            for (int k = 0; k < 4; k++) {
                double lane = new double[]{-.22, -.08, .1, .24}[k], speed = 1 + k % 2;
                double pulse = Math.pow(.5 + .5 * Math.sin(2 * Math.PI * (v * 2.5 - phase * speed + k * .37)), 6);
                flow += .7 * pulse * Math.exp(-Math.pow((dx - lane) / .02, 2));
            }
            double a = Math.min(1, (coreI * 1.2 + glowI * .7 + flow) * rise);
            out.setRGB(x, y, VfxPainter.blend(fam.halo(), Color.WHITE, Math.min(1, coreI + flow), a));
        }
        return out;
    }

    /** The flare twinkles: its cross lengthens and turns a little each frame. */
    static BufferedImage spark(VfxPainter.Family fam, int f) {
        BufferedImage out = VfxPainter.canvas();
        double phase = f / (double) LOOP, pulse = .75 + .35 * Math.sin(2 * Math.PI * phase), turn = phase * Math.PI / 2; // a quarter turn maps the star onto itself: seamless loop
        double c = Math.cos(turn), s = Math.sin(turn);
        for (int y = 0; y < 256; y++) for (int x = 0; x < 256; x++) {
            double px = x - 127.5, py = y - 127.5;
            double dx = Math.abs(px * c + py * s), dy = Math.abs(-px * s + py * c), d = Math.hypot(dx, dy);
            double core = Math.exp(-d * d / (230 * pulse)), halo = Math.exp(-d * d / 3600) * .65 * pulse;
            double cross = (Math.exp(-dy / 2.6) * Math.exp(-dx / (46 * pulse)) + Math.exp(-dx / 2.6) * Math.exp(-dy / (46 * pulse)));
            double u = Math.abs(dx - dy) / Math.sqrt(2), w = (dx + dy) / Math.sqrt(2);
            double diag = Math.exp(-u / 2.2) * Math.exp(-w / (20 * (1.6 - pulse))) * .6;
            double a = Math.min(1, core + halo + cross + diag);
            out.setRGB(x, y, VfxPainter.blend(fam.halo(), Color.WHITE, Math.min(1, core * 1.4 + cross * .5), a));
        }
        return out;
    }

    /** Element debris that is alive: flames flicker, bolts re-fork, leaves flutter, feathers sway, crystals glint. */
    static BufferedImage shard(VfxPainter.Family fam, int f) {
        double phase = f / (double) LOOP, wave = Math.sin(2 * Math.PI * phase);
        Shape shape;
        AffineTransform motion = new AffineTransform();
        switch (fam.name()) {
            case "fire" -> {
                motion.translate(128, 226); motion.shear(wave * .16, 0); motion.scale(1 + .06 * Math.cos(2 * Math.PI * phase * 2), 1 + .12 * wave); motion.translate(-128, -226);
                shape = VfxPainter.path(128, 30, 196, 120, 170, 226, 128, 226, 86, 226, 52, 150, 98, 100, 106, 150, 118, 120, 128, 30);
            }
            case "storm" -> {
                Random random = new Random(f * 7919L + fam.name().hashCode());
                Path2D bolt = new Path2D.Double(); bolt.moveTo(140 + random.nextInt(30) - 15, 18);
                double x = 140, y = 18;
                while (y < 230) { y += 26 + random.nextInt(18); x = 128 + random.nextInt(70) - 35; bolt.lineTo(x, Math.min(238, y)); }
                shape = new BasicStroke(13, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER).createStrokedShape(bolt);
            }
            case "nature", "love" -> {
                motion.translate(128, 128); motion.rotate(.35 + wave * .25); motion.scale(Math.max(.18, Math.abs(Math.cos(Math.PI * phase))), 1); motion.translate(-128, -128);
                shape = VfxPainter.leaf(128, 128, 92, 0);
            }
            case "wind", "holy" -> {
                motion.translate(128, 236); motion.rotate(wave * .3); motion.translate(-128, -236);
                shape = VfxPainter.path(128, 22, 196, 80, 180, 196, 128, 236, 76, 196, 60, 80, 128, 22);
            }
            default -> {
                motion.translate(128, 128); motion.rotate(wave * .12); motion.translate(-128, -128);
                shape = VfxPainter.poly(128, 20, 178, 110, 128, 236, 78, 110);
            }
        }
        shape = motion.createTransformedShape(shape);
        BufferedImage mask = VfxPainter.canvas();
        Graphics2D m = VfxPainter.graphics(mask); m.setColor(Color.WHITE); m.fill(shape); m.dispose();
        BufferedImage out = VfxPainter.canvas();
        Graphics2D g = VfxPainter.graphics(out);
        g.drawImage(VfxPainter.tint(VfxPainter.blur(mask, 14), fam.halo()), 0, 0, null);
        BufferedImage body = VfxPainter.canvas();
        Graphics2D b = VfxPainter.graphics(body);
        b.setPaint(new GradientPaint(80, 40, Color.WHITE, 170, 230, fam.glow()));
        b.fill(shape);
        // A glint band sweeps across the debris once per loop.
        double sweep = -120 + phase * 500;
        b.setClip(shape);
        b.setPaint(new GradientPaint((float) sweep, 0, VfxPainter.alpha(Color.WHITE, 0), (float) sweep + 40, 40, VfxPainter.alpha(Color.WHITE, .95f), true));
        b.fill(new Rectangle2D.Double(sweep, -40, 60, 400));
        b.setClip(null);
        b.setColor(Color.WHITE); b.setStroke(VfxPainter.round(5)); b.draw(shape); b.dispose();
        g.drawImage(body, 0, 0, null);
        g.dispose();
        return out;
    }

    // ---------------------------------------------------------------- emblem tracing

    /** The emblem's strokes are drawn in by a travelling pen of light, before the completed emblem flashes in. */
    static BufferedImage trace(List<Shape> ink, VfxPainter.Family family, double fraction) {
        BufferedImage strokes = VfxPainter.canvas(), core = VfxPainter.canvas(), pens = VfxPainter.canvas();
        Graphics2D s = VfxPainter.graphics(strokes), c = VfxPainter.graphics(core), p = VfxPainter.graphics(pens);
        for (Shape shape : ink) {
            Point2D[] tip = new Point2D[1];
            Shape partial = partial(shape, fraction, tip);
            s.setColor(family.halo()); s.setStroke(VfxPainter.round(18)); s.draw(partial);
            c.setColor(family.body()); c.setStroke(VfxPainter.round(9)); c.draw(partial);
            c.setColor(Color.WHITE); c.setStroke(VfxPainter.round(4)); c.draw(partial);
            if (tip[0] != null) { p.setColor(Color.WHITE); p.fill(new Ellipse2D.Double(tip[0].getX() - 9, tip[0].getY() - 9, 18, 18)); }
        }
        s.dispose(); c.dispose(); p.dispose();
        BufferedImage out = VfxPainter.canvas();
        Graphics2D g = VfxPainter.graphics(out);
        g.drawImage(VfxPainter.blur(strokes, 12), 0, 0, null);
        g.drawImage(VfxPainter.tint(VfxPainter.blur(pens, 10), family.halo()), 0, 0, null);
        g.drawImage(VfxPainter.blur(pens, 3), 0, 0, null);
        g.drawImage(core, 0, 0, null);
        g.dispose();
        return VfxPainter.shrink(VfxPainter.boost(out, 1.2f));
    }

    /** The first {@code fraction} of a shape's outline, with the pen position at its end. */
    static Shape partial(Shape shape, double fraction, Point2D[] tip) {
        List<double[]> segments = new ArrayList<>();
        double total = 0, sx = 0, sy = 0, lx = 0, ly = 0;
        double[] v = new double[6];
        for (PathIterator it = shape.getPathIterator(null, .8); !it.isDone(); it.next()) {
            int type = it.currentSegment(v);
            if (type == PathIterator.SEG_MOVETO) { sx = lx = v[0]; sy = ly = v[1]; segments.add(new double[]{0, v[0], v[1], v[0], v[1]}); }
            else {
                double x = type == PathIterator.SEG_CLOSE ? sx : v[0], y = type == PathIterator.SEG_CLOSE ? sy : v[1];
                double length = Math.hypot(x - lx, y - ly); total += length;
                segments.add(new double[]{length, lx, ly, x, y}); lx = x; ly = y;
            }
        }
        double budget = total * fraction;
        Path2D out = new Path2D.Double();
        for (double[] seg : segments) {
            if (seg[0] == 0) { out.moveTo(seg[1], seg[2]); continue; }
            if (budget <= 0) break;
            double t = Math.min(1, budget / seg[0]);
            double x = seg[1] + (seg[3] - seg[1]) * t, y = seg[2] + (seg[4] - seg[2]) * t;
            out.lineTo(x, y); budget -= seg[0]; tip[0] = new Point2D.Double(x, y);
        }
        return out;
    }
}
