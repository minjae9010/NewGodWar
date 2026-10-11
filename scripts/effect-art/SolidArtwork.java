import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Small native item meshes: shaded metal, leather grips, raised rims and inset runes. */
final class SolidArtwork {
    static final int MAX_ELEMENTS = 40;
    static final String[] FACES = {"north", "south", "east", "west", "up", "down"};
    record Box(double x0, double y0, double z0, double x1, double y1, double z1, int material) { }
    record Preview(String key, List<Box> boxes, Color[] colors) { }
    final Properties catalogue = new Properties();
    final Set<String> built = new TreeSet<>();
    final List<Preview> previews = new ArrayList<>();

    SolidArtwork(Path repo) throws Exception {
        try (var input = Files.newInputStream(repo.resolve("plugin/src/main/resources/solid-art.properties"))) {
            catalogue.load(input);
        }
    }

    boolean write(Path root, String key, Color tint, String family) throws Exception {
        String kind = catalogue.getProperty(key);
        if (kind == null) return false;
        List<Box> boxes = mesh(kind);
        Color[] colors = palette(tint, family);
        BufferedImage atlas = atlas(colors);
        String base = "art/" + key + "/";
        for (int fade = 0; fade < 4; fade++) {
            String texture = base + "material" + fade;
            Path png = root.resolve("assets/newgodwar/textures/" + texture + ".png");
            Files.createDirectories(png.getParent());
            ImageIO.write(VfxPainter.faded(atlas, fade), "png", png.toFile());
            // Keep the existing item path and legacy numeric ID without baking an unused flat glyph texture.
            BuildEffectPack.write(root, "assets/newgodwar/models/" + base + "glyph" + fade + ".json", json(boxes, texture));
            BuildEffectPack.write(root, "assets/newgodwar/items/" + base + "glyph" + fade + ".json",
                "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"newgodwar:"+base+"glyph"+fade+"\"}}");
        }
        built.add(key);
        previews.add(new Preview(key, boxes, colors));
        return true;
    }

    static List<Box> mesh(String kind) {
        List<Box> b = new ArrayList<>();
        switch (kind) {
            case "hammer":
                box(b, 7.1, 1.4, 7.1, 8.9, 10.7, 8.9, 3); // wrapped grip
                for (double y : new double[]{2, 4, 6, 8}) box(b, 6.95, y, 6.95, 9.05, y+.35, 9.05, 2);
                box(b, 6.6, .8, 6.6, 9.4, 1.8, 9.4, 2);
                box(b, 2.1, 10, 5.5, 13.9, 14.8, 10.5, 0);
                box(b, 1.3, 10.7, 6.1, 2.1, 14.1, 9.9, 1);
                box(b, 13.9, 10.7, 6.1, 14.7, 14.1, 9.9, 1);
                for (double x : new double[]{3, 12.1}) box(b, x, 9.75, 5.25, x+.9, 15.05, 10.75, 2);
                box(b, 5, 10.6, 5.25, 11, 14.2, 10.75, 5);
                for (double z : new double[]{5.02, 10.75}) {
                    box(b, 8.1, 12.6, z, 8.75, 13.8, z+.23, 4);
                    box(b, 7.25, 12.15, z, 8.75, 12.7, z+.23, 4);
                    box(b, 7.25, 11.1, z, 7.9, 12.25, z+.23, 4);
                }
                break;
            case "spear":
                box(b, 7.5, 1, 7.5, 8.5, 11.6, 8.5, 3);
                for (double y : new double[]{1.3, 3, 8.2, 9.3}) box(b, 7.25, y, 7.25, 8.75, y+.45, 8.75, 2);
                box(b, 6.25, 9.8, 7.2, 9.75, 10.5, 8.8, 2);
                box(b, 6.85, 10.5, 7.4, 9.15, 12.9, 8.6, 0);
                box(b, 7.2, 12.9, 7.5, 8.8, 14.2, 8.5, 1);
                box(b, 7.6, 14.2, 7.65, 8.4, 15.3, 8.35, 1);
                box(b, 7.8, 10.7, 7.18, 8.2, 14.5, 7.4, 4);
                box(b, 7.8, 10.7, 8.6, 8.2, 14.5, 8.82, 4);
                box(b, 7.7, .5, 7.7, 8.3, 1, 8.3, 1);
                break;
            case "trident":
                box(b, 7.4, .8, 7.4, 8.6, 10.2, 8.6, 5);
                for (double y : new double[]{1, 3.5, 6, 8.3}) box(b, 7.2, y, 7.2, 8.8, y+.45, 8.8, 2);
                box(b, 3.2, 9.6, 7.15, 12.8, 10.8, 8.85, 0);
                box(b, 2.7, 10.4, 7.3, 4, 13.7, 8.7, 0);
                box(b, 12, 10.4, 7.3, 13.3, 13.7, 8.7, 0);
                box(b, 7.25, 10.8, 7.2, 8.75, 14.5, 8.8, 0);
                for (double x : new double[]{2.95, 12.25}) {
                    box(b, x, 13.7, 7.5, x+.8, 14.65, 8.5, 1);
                    box(b, x+.2, 14.65, 7.7, x+.6, 15.15, 8.3, 1);
                }
                box(b, 7.55, 14.5, 7.5, 8.45, 15.4, 8.5, 1);
                box(b, 6.8, 9.4, 6.9, 9.2, 11.1, 9.1, 2);
                for (double z : new double[]{6.65, 9.1}) box(b, 7.5, 9.7, z, 8.5, 10.8, z+.25, 4);
                break;
            case "dagger":
                box(b, 7.15, 1.2, 7.3, 8.85, 5.8, 8.7, 3);
                for (double y : new double[]{1.5, 3, 4.5}) box(b, 7, y, 7.2, 9, y+.3, 8.8, 2);
                box(b, 6.65, .7, 6.95, 9.35, 1.6, 9.05, 2);
                box(b, 4.8, 5.6, 7, 11.2, 6.5, 9, 2);
                box(b, 6.65, 6.5, 7.35, 9.35, 11.7, 8.65, 0);
                box(b, 7, 11.7, 7.45, 9, 13.2, 8.55, 0);
                box(b, 7.45, 13.2, 7.6, 8.55, 14.7, 8.4, 1);
                for (double x : new double[]{6.45, 9.35}) box(b, x, 6.65, 7.7, x+.2, 11.5, 8.3, 1);
                for (double z : new double[]{7.12, 8.65}) box(b, 7.8, 7, z, 8.2, 12.8, z+.23, 4);
                break;
            case "shield":
                // Stepped rim and domed boss remain visible from side and rear views.
                box(b, 2.1, 6, 7.1, 13.9, 14, 9, 2);
                box(b, 3.3, 3.8, 7.1, 12.7, 6, 9, 2);
                box(b, 5.1, 2.1, 7.1, 10.9, 3.8, 9, 2);
                box(b, 6.6, 1, 7.1, 9.4, 2.1, 9, 2);
                box(b, 3, 6.6, 6.85, 13, 13.25, 7.4, 0);
                box(b, 4.2, 4.4, 6.85, 11.8, 6.6, 7.4, 0);
                box(b, 5.8, 2.8, 6.85, 10.2, 4.4, 7.4, 0);
                box(b, 7.55, 3, 6.55, 8.45, 13, 6.85, 1);
                box(b, 3.4, 9.2, 6.55, 12.6, 9.9, 6.85, 1);
                box(b, 6.2, 7.8, 6.05, 9.8, 11.4, 6.55, 2);
                box(b, 6.85, 8.45, 5.55, 9.15, 10.75, 6.05, 5);
                box(b, 7.6, 8.9, 5.3, 8.4, 10.3, 5.55, 4);
                for (double x : new double[]{3.35, 12.1}) for (double y : new double[]{7, 12.3})
                    box(b, x, y, 6.55, x+.55, y+.55, 6.85, 1);
                for (double x : new double[]{5, 10.2}) box(b, x, 6, 9, x+.8, 11, 9.65, 3);
                // ItemDisplayRenderer rotates item models by Y=180 degrees after the display transform.
                // Keep the decorated face on MODEL -Z so it renders toward the display's local +Z.
                break;
            default: throw new IllegalArgumentException("Unknown solid artwork: " + kind);
        }
        if (b.isEmpty() || b.size() > MAX_ELEMENTS) throw new IllegalStateException(kind + " element budget");
        return b;
    }

    static void box(List<Box> b, double x0, double y0, double z0, double x1, double y1, double z1, int material) {
        if (x0 >= x1 || y0 >= y1 || z0 >= z1 || x0 < 0 || y0 < 0 || z0 < 0 || x1 > 16 || y1 > 16 || z1 > 16)
            throw new IllegalArgumentException("Solid geometry must fit the item bounds");
        b.add(new Box(x0, y0, z0, x1, y1, z1, material));
    }

    static Color[] palette(Color tint, String family) {
        boolean dark = family.equals("shadow");
        Color metal = mix(new Color(dark ? 0x3c335b : 0x647d91), tint, dark ? .20 : .32);
        return new Color[]{metal, mix(tint, Color.WHITE, .55), new Color(dark ? 0x9680bc : 0xc9a35b),
            new Color(dark ? 0x36243f : 0x593c30), mix(tint, Color.WHITE, .8), mix(metal, Color.BLACK, .48),
            tint, new Color(0xeff4f6)};
    }

    static Color mix(Color a, Color b, double t) {
        return new Color((int)(a.getRed()*(1-t)+b.getRed()*t), (int)(a.getGreen()*(1-t)+b.getGreen()*t),
            (int)(a.getBlue()*(1-t)+b.getBlue()*t));
    }

    /** Eight 16px material tiles; deterministic scratches and woven leather need no external artwork. */
    static BufferedImage atlas(Color[] colors) {
        BufferedImage image = new BufferedImage(128, 16, BufferedImage.TYPE_INT_ARGB);
        for (int tile = 0; tile < 8; tile++) for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int noise = Math.floorMod(x*17+y*29+x*y*3+tile*19, 13)-6;
            double shade = noise/120.0 + (7.5-y)/140;
            if (x == 0 || y == 0) shade += .1;
            if (x == 15 || y == 15) shade -= .12;
            if (tile == 3 && (x+y*2)%7 < 2) shade -= .2;
            if (tile == 4) shade = .04;
            Color color = mix(colors[tile], shade > 0 ? Color.WHITE : Color.BLACK, Math.abs(shade));
            image.setRGB(tile*16+x, y, color.getRGB());
        }
        return image;
    }

    static String json(List<Box> boxes, String texture) {
        List<String> elements = new ArrayList<>();
        for (Box b : boxes) {
            List<String> faces = new ArrayList<>();
            // Half-texel inset prevents another material bleeding onto the rim at distance.
            double u0 = b.material*2+.0625, u1 = (b.material+1)*2-.0625;
            for (String face : FACES) faces.add("\""+face+"\":{\"uv\":["+u0+",0.5,"+u1+",15.5],\"texture\":\"#surface\"}");
            elements.add("{\"from\":["+b.x0+","+b.y0+","+b.z0+"],\"to\":["+b.x1+","+b.y1+","+b.z1
                +"],\"shade\":"+(b.material != 4)+",\"faces\":{"+String.join(",", faces)+"}}");
        }
        return "{\"ambientocclusion\":false,\"textures\":{\"particle\":\"#surface\",\"surface\":\"newgodwar:"
            +texture+"\"},\"elements\":["+String.join(",", elements)+"]}";
    }

    void preview(Path output) throws Exception {
        if (!built.equals(catalogue.stringPropertyNames())) throw new IllegalStateException("Solid catalogue references missing effects");
        int columns = 5, cell = 240, rows = (previews.size()+columns-1)/columns;
        BufferedImage image = new BufferedImage(columns*cell, rows*280, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x101b29)); g.fillRect(0,0,image.getWidth(),image.getHeight());
        for (int i = 0; i < previews.size(); i++) {
            Preview p = previews.get(i); int x = (i%columns)*cell, y = (i/columns)*280;
            g.setColor(new Color(0x182638)); g.fillRoundRect(x+8,y+8,224,264,18,18);
            render(g,p,x+120,y+128);
            g.setFont(new Font("SansSerif",Font.BOLD,12)); g.setColor(new Color(0xdfebf6));
            g.drawString(p.key,x+18,y+241);
            g.setFont(new Font("SansSerif",Font.PLAIN,11));g.setColor(new Color(0x93aec9));
            g.drawString(p.boxes.size()+" cuboids / 1 display",x+18,y+260);
        }
        g.dispose(); ImageIO.write(image,"png",output.toFile());
    }

    record Face(double depth, double[][] points, Color color) { }
    static void render(Graphics2D g, Preview p, double cx, double cy) {
        List<Face> faces = new ArrayList<>();
        for (Box model : p.boxes) {
            // Match ItemDisplayRenderer's inner Y=180-degree rotation before previewing the front.
            Box b = new Box(16-model.x1,model.y0,16-model.z1,16-model.x0,model.y1,16-model.z0,model.material);
            Color c = p.colors[b.material];
            face(faces,new double[][]{{b.x0,b.y0,b.z1},{b.x1,b.y0,b.z1},{b.x1,b.y1,b.z1},{b.x0,b.y1,b.z1}},c);
            face(faces,new double[][]{{b.x1,b.y0,b.z0},{b.x1,b.y0,b.z1},{b.x1,b.y1,b.z1},{b.x1,b.y1,b.z0}},mix(c,Color.BLACK,.26));
            face(faces,new double[][]{{b.x0,b.y1,b.z0},{b.x1,b.y1,b.z0},{b.x1,b.y1,b.z1},{b.x0,b.y1,b.z1}},mix(c,Color.WHITE,.24));
        }
        faces.sort(Comparator.comparingDouble(Face::depth).reversed());
        for (Face f : faces) {
            Path2D path = new Path2D.Double(); boolean first=true;
            for (double[] point:f.points) {
                double x=point[0]-8,y=point[1]-8,z=point[2]-8;
                double px=cx+(x*.9-z*.43)*12.5, py=cy+(x*.16-y*.94+z*.33)*12.5;
                if(first){path.moveTo(px,py);first=false;}else path.lineTo(px,py);
            }
            path.closePath();g.setColor(f.color);g.fill(path);
            g.setColor(mix(f.color,Color.BLACK,.16));g.setStroke(new BasicStroke(.5f));g.draw(path);
        }
    }
    static void face(List<Face> faces, double[][] points, Color color) {
        double depth=0;for(double[] p:points)depth+=-p[0]*.4-p[1]*.34-p[2]*.84;
        faces.add(new Face(depth/4,points,color));
    }
}
