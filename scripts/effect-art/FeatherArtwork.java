import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

/** Axial flight feather: pointed tip at +Y in model space and a narrow shoulder root. */
final class FeatherArtwork {
    static BufferedImage paint(boolean phoenix) {
        BufferedImage out=VfxPainter.canvas();
        Graphics2D g=VfxPainter.graphics(out);
        Path2D vane=new Path2D.Double();
        vane.moveTo(128,238);
        vane.curveTo(118,191,57,171,69,109);
        vane.curveTo(75,72,111,43,128,15);
        vane.curveTo(147,52,193,91,187,128);
        vane.curveTo(181,178,143,199,128,238);
        vane.closePath();
        BufferedImage halo=VfxPainter.canvas(); Graphics2D h=VfxPainter.graphics(halo);
        h.setColor(phoenix?new Color(255,152,63,165):new Color(188,222,255,130));
        h.setStroke(new BasicStroke(10)); h.draw(vane); h.dispose();
        g.drawImage(VfxPainter.blur(halo,7),0,0,null);
        g.setPaint(new LinearGradientPaint(128,18,128,237,new float[]{0,.4f,.76f,1},
            phoenix?new Color[]{new Color(255,94,38),new Color(255,173,63),new Color(255,226,129),new Color(255,247,208)}
                   :new Color[]{new Color(155,195,234),new Color(209,230,251),new Color(243,247,255),new Color(255,233,172)}));
        g.fill(vane);
        g.setClip(vane);
        for(int i=0;i<10;i++) {
            int y=55+i*16;
            g.setColor(phoenix?new Color(175,55,34,95):new Color(80,126,182,85));
            g.setStroke(new BasicStroke(3));
            g.draw(new QuadCurve2D.Double(128,y+26,96,y+3,62,y-2));
            g.draw(new QuadCurve2D.Double(128,y+26,158,y+8,191,y-5));
        }
        g.setClip(null);
        g.setColor(phoenix?new Color(255,241,182,235):new Color(255,255,255,230));
        g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        g.draw(new QuadCurve2D.Double(128,230,130,127,128,33));
        g.dispose(); return VfxPainter.shrink(out);
    }
}
