import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.List;
import javax.imageio.ImageIO;

/** Original, code-native menu sprites. No font or vanilla texture is baked into these icons. */
final class GuiArtwork {
    static final List<String> IDS=List.of(
        "settings","game","team","world","core","display","combat","gambling",
        "home","help","ability","items","map","rewards","back","next",
        "previous","close","toggle_on","toggle_off","plus","minus","confirm","cancel",
        "refresh","play","stop","coin","info","frame","accent");
    static final Color INK=new Color(10,23,36), NAVY=new Color(28,47,66),
        PALE=new Color(238,244,232), TEAL=new Color(91,218,197), GOLD=new Color(244,195,104),
        MUTED=new Color(112,138,154), RED=new Color(240,137,129);

    private static Path2D polygon(double... points) {
        Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);p.closePath();return p;
    }
    private static void fill(Graphics2D g,Color c,Shape s) {g.setColor(c);g.fill(s);}
    private static void stroke(Graphics2D g,Color c,float width,Shape s) {
        g.setColor(c);g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(s);
    }
    private static void line(Graphics2D g,Color c,float width,double... points) {
        Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);stroke(g,c,width,p);
    }
    private static void circle(Graphics2D g,Color c,double x,double y,double r) {
        fill(g,c,new Ellipse2D.Double(x-r,y-r,r*2,r*2));
    }
    private static void box(Graphics2D g,Color c,double x,double y,double w,double h,double r) {
        fill(g,c,new RoundRectangle2D.Double(x,y,w,h,r,r));
    }
    private static void outlined(Graphics2D g,Color c,Shape shape) {
        stroke(g,INK,4,shape);fill(g,c,shape);
    }
    private static void diamond(Graphics2D g,Color c,double x,double y,double r) {
        fill(g,c,polygon(x,y-r,x+r*.68,y,x,y+r,x-r*.68,y));
    }
    private static void arrow(Graphics2D g,Color c,boolean right) {
        Graphics2D a=(Graphics2D)g.create();if(!right){a.translate(64,0);a.scale(-1,1);}
        fill(a,c,polygon(16,27,34,27,34,17,49,32,34,47,34,37,16,37));a.dispose();
    }
    private static void sword(Graphics2D g) {
        outlined(g,PALE,polygon(28,36,28,16,32,9,36,16,36,36));
        line(g,TEAL,2,32,16,32,32);line(g,GOLD,4,23,36,41,36);
        line(g,GOLD,5,32,40,32,49);circle(g,GOLD,32,51,3);
    }
    private static void person(Graphics2D g,double x,double y,Color c) {
        circle(g,INK,x,y,7);circle(g,c,x,y,5);
        Shape body=new RoundRectangle2D.Double(x-8,y+7,16,15,8,8);outlined(g,c,body);
    }
    private static void die(Graphics2D g,double x,double y,double rotation,Color c,boolean five) {
        Graphics2D d=(Graphics2D)g.create();d.rotate(rotation,x+11,y+11);
        outlined(d,c,new RoundRectangle2D.Double(x,y,22,22,5,5));
        for(int i=0;i<2;i++)for(int j=0;j<2;j++)circle(d,INK,x+6+i*10,y+6+j*10,1.8);
        if(five)circle(d,INK,x+11,y+11,1.8);d.dispose();
    }
    private static void icon(Graphics2D g,String id) {
        switch(id) {
        case "settings": {
            Area gear=new Area(new Ellipse2D.Double(17,17,30,30));
            for(int i=0;i<8;i++)gear.add(new Area(AffineTransform.getRotateInstance(i*Math.PI/4,32,32)
                .createTransformedShape(new RoundRectangle2D.Double(27,10,10,15,2,2))));
            gear.subtract(new Area(new Ellipse2D.Double(25,25,14,14)));fill(g,PALE,gear);circle(g,TEAL,32,32,4);break;
        }
        case "game":
            line(g,GOLD,4,18,12,18,51);fill(g,TEAL,polygon(21,13,48,13,42,24,48,35,21,35));
            fill(g,PALE,polygon(28,18,37,24,28,30));line(g,GOLD,3,12,51,25,51);break;
        case "team": person(g,19,23,MUTED);person(g,45,23,TEAL);person(g,32,19,PALE);break;
        case "world":
            circle(g,TEAL,32,32,20);stroke(g,PALE,2.5f,new Ellipse2D.Double(22,12,20,40));
            line(g,PALE,2.5f,13,32,51,32);line(g,PALE,2,16,22,48,22);line(g,PALE,2,16,42,48,42);break;
        case "core":
            fill(g,GOLD,polygon(32,10,50,21,50,43,32,54,14,43,14,21));
            fill(g,NAVY,polygon(32,16,45,24,45,40,32,48,19,40,19,24));
            diamond(g,TEAL,32,32,15);fill(g,PALE,polygon(32,17,32,32,22,32));
            fill(g,new Color(46,150,150),polygon(32,32,42,32,32,47));break;
        case "display": {
            Path2D eye=new Path2D.Double();eye.moveTo(10,32);eye.curveTo(22,12,42,12,54,32);
            eye.curveTo(42,52,22,52,10,32);eye.closePath();fill(g,PALE,eye);
            circle(g,TEAL,32,32,12);circle(g,INK,32,32,6);circle(g,PALE,29,28,2.5);break;
        }
        case "combat": {
            fill(g,TEAL,polygon(15,15,32,11,49,15,46,38,32,51,18,38));
            Graphics2D s=(Graphics2D)g.create();s.rotate(Math.PI/4,32,32);sword(s);s.dispose();break;
        }
        case "gambling": die(g,12,15,-.18,TEAL,false);die(g,30,29,.17,PALE,true);break;
        case "home":
            fill(g,PALE,polygon(17,29,32,17,47,29,47,50,17,50));
            line(g,GOLD,5,11,30,32,12,53,30);box(g,TEAL,27,34,10,17,2);break;
        case "help":
            circle(g,PALE,32,32,20);for(int i=0;i<4;i++) {
                Graphics2D s=(Graphics2D)g.create();s.rotate(i*Math.PI/2,32,32);box(s,TEAL,26,12,12,12,2);s.dispose();
            }circle(g,NAVY,32,32,11);stroke(g,GOLD,2,new Ellipse2D.Double(11,11,42,42));break;
        case "ability":
            fill(g,GOLD,polygon(35,10,16,35,29,35,26,54,48,27,35,27));
            diamond(g,TEAL,15,17,5);diamond(g,PALE,49,46,5);break;
        case "items":
            box(g,GOLD,12,17,40,33,5);box(g,TEAL,15,33,34,14,2);
            line(g,INK,3,12,31,52,31);line(g,INK,2,22,18,22,48);line(g,INK,2,42,18,42,48);
            outlined(g,PALE,new RoundRectangle2D.Double(28,27,8,12,2,2));break;
        case "map":
            outlined(g,PALE,polygon(11,17,25,13,39,18,53,13,53,46,39,51,25,46,11,51));
            fill(g,TEAL,polygon(25,16,39,21,39,48,25,43));
            line(g,NAVY,2.5f,16,40,23,32,33,36,44,24);circle(g,GOLD,44,24,4);break;
        case "rewards":
            box(g,TEAL,14,28,36,24,3);box(g,PALE,11,23,42,10,2);box(g,GOLD,29,23,6,29,0);
            stroke(g,GOLD,4,new Ellipse2D.Double(17,12,15,12));stroke(g,GOLD,4,new Ellipse2D.Double(32,12,15,12));break;
        case "back":
            line(g,TEAL,6,18,26,40,26,48,34,48,43,42,49,27,49);
            fill(g,PALE,polygon(26,13,11,26,26,39));break;
        case "next": arrow(g,PALE,true);break;
        case "previous": arrow(g,PALE,false);break;
        case "close":
            stroke(g,MUTED,2.5f,new Ellipse2D.Double(12,12,40,40));
            line(g,PALE,5,23,23,41,41);line(g,PALE,5,41,23,23,41);break;
        case "toggle_on": case "toggle_off": {
            boolean on=id.equals("toggle_on");box(g,on?TEAL:MUTED,10,21,44,23,23);
            circle(g,INK,on?42:22,32.5,8.5);circle(g,PALE,on?42:22,31.5,7);
            if(on)line(g,INK,2.5f,16,32,20,36,26,28);else line(g,NAVY,3,37,32.5,46,32.5);break;
        }
        case "plus": line(g,TEAL,7,16,32,48,32);line(g,TEAL,7,32,16,32,48);break;
        case "minus": line(g,GOLD,7,16,32,48,32);break;
        case "confirm": line(g,TEAL,7,14,32,26,44,50,20);break;
        case "cancel": line(g,RED,6,19,19,45,45);line(g,RED,6,45,19,19,45);break;
        case "refresh":
            stroke(g,TEAL,5,new Arc2D.Double(15,15,34,34,48,285,Arc2D.OPEN));
            fill(g,PALE,polygon(33,17,49,12,47,28));break;
        case "play": fill(g,TEAL,polygon(23,13,50,32,23,51));break;
        case "stop": box(g,RED,17,17,30,30,4);break;
        case "coin":
            circle(g,GOLD,32,33,21);circle(g,new Color(187,126,53),32,33,16);
            circle(g,GOLD,32,31,14);diamond(g,PALE,32,31,10);break;
        case "info":
            fill(g,PALE,polygon(13,15,51,15,51,42,29,42,17,51,17,42,13,42));
            for(int i=0;i<3;i++)circle(g,TEAL,23+i*9,29,3);break;
        case "frame": case "accent": break;
        default: throw new IllegalArgumentException("Missing menu icon: "+id);
        }
    }
    static BufferedImage draw(String id) {
        BufferedImage image=new BufferedImage(128,128,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g=image.createGraphics();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(2,2);
        boolean filler=id.equals("frame")||id.equals("accent");
        if(filler) {
            fill(g,new Color(20,35,49),new Rectangle2D.Double(0,0,64,64));
            stroke(g,new Color(27,45,60),1,new Rectangle2D.Double(.5,.5,63,63));
            if(id.equals("accent")) {
                line(g,new Color(49,125,127),2,0,3,64,3);line(g,new Color(49,125,127),2,0,61,64,61);
                diamond(g,GOLD,32,32,3);
            }
            g.dispose();return image;
        }
        box(g,new Color(4,11,22,180),2,5,60,57,12);
        g.setPaint(new GradientPaint(0,3,new Color(40,66,85),0,59,new Color(19,34,50)));
        g.fill(new RoundRectangle2D.Double(2,2,60,57,11,11));
        stroke(g,new Color(104,133,144),1.2f,new RoundRectangle2D.Double(3,3,58,55,10,10));
        line(g,new Color(244,195,104,170),1.5f,18,58,46,58);
        icon(g,id);
        g.dispose();return image;
    }
    static int writePack(Path root,Path contactSheet) throws Exception {
        final int cellWidth=160,cellHeight=194,cols=8;
        BufferedImage sheet=new BufferedImage(cols*cellWidth,((IDS.size()+cols-1)/cols)*cellHeight,BufferedImage.TYPE_INT_RGB);
        Graphics2D g=sheet.createGraphics();g.setColor(new Color(11,20,31));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());
        int index=0;
        for(String id:IDS) {
            BufferedImage icon=draw(id);String path="gui/"+id;
            Path png=root.resolve("assets/newgodwar/textures/"+path+".png");Files.createDirectories(png.getParent());ImageIO.write(icon,"png",png.toFile());
            BuildEffectPack.write(root,"assets/newgodwar/models/"+path+".json","{\"parent\":\"minecraft:item/generated\",\"gui_light\":\"front\",\"textures\":{\"layer0\":\"newgodwar:"+path+"\"}}\n");
            BuildEffectPack.write(root,"assets/newgodwar/items/"+path+".json","{\"model\":{\"type\":\"minecraft:model\",\"model\":\"newgodwar:"+path+"\",\"tints\":[]}}\n");
            int x=index%cols*cellWidth,y=index/cols*cellHeight;
            g.drawImage(icon,x+28,y+6,104,104,null);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(icon,x+39,y+119,32,32,null);g.drawImage(icon,x+96,y+127,16,16,null);
            g.setColor(PALE);g.setFont(new Font("SansSerif",Font.PLAIN,13));FontMetrics fm=g.getFontMetrics();
            g.drawString(id,x+(cellWidth-fm.stringWidth(id))/2,y+177);index++;
        }
        g.dispose();Files.createDirectories(contactSheet.getParent());ImageIO.write(sheet,"png",contactSheet.toFile());return IDS.size();
    }
}
