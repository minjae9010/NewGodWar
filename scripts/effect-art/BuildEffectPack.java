import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.imageio.ImageIO;

/** Editable vector artwork, rasterized deterministically for vanilla Minecraft item models. */
public class BuildEffectPack {
    static final int SIZE = 128, LOOP_COLUMNS = 8;
    static final List<Shape> ink = new ArrayList<>();
    static void line(double... v) { Path2D p=new Path2D.Double(); p.moveTo(v[0],v[1]); for(int i=2;i<v.length;i+=2)p.lineTo(v[i],v[i+1]); if(v[0]==v[v.length-2]&&v[1]==v[v.length-1])p.closePath();ink.add(p); }
    static void curve(double x,double y,double... v) { Path2D p=new Path2D.Double();p.moveTo(x,y);for(int i=0;i<v.length;i+=6)p.curveTo(v[i],v[i+1],v[i+2],v[i+3],v[i+4],v[i+5]);if(x==v[v.length-2]&&y==v[v.length-1])p.closePath();ink.add(p); }
    static void circle(double x,double y,double r) { ink.add(new Ellipse2D.Double(x-r,y-r,r*2,r*2)); }
    static void oval(double x,double y,double w,double h) { ink.add(new Ellipse2D.Double(x,y,w,h)); }
    static void arc(double r,double start,double extent) { ink.add(new Arc2D.Double(128-r,128-r,r*2,r*2,start,extent,Arc2D.OPEN)); }
    static void diamond(double x,double y,double r) {line(x,y-r,x+r*.55,y,x,y+r,x-r*.55,y,x,y-r);}
    static void star(double x,double y,double r) {line(x,y-r,x,y+r);line(x-r*.7,y,x+r*.7,y);diamond(x,y,r*.4);}
    static void rotate(double angle,Runnable body) {
        int first=ink.size();body.run();AffineTransform a=AffineTransform.getRotateInstance(angle,128,128);
        for(int i=first;i<ink.size();i++)ink.set(i,a.createTransformedShape(ink.get(i)));
    }
    static void scale(double k,Runnable body) {
        int first=ink.size();body.run();AffineTransform a=new AffineTransform();a.translate(128,128);a.scale(k,k);a.translate(-128,-128);
        for(int i=first;i<ink.size();i++)ink.set(i,a.createTransformedShape(ink.get(i)));
    }
    static void leaf(double x,double y,double s,double rot) {
        Path2D p=new Path2D.Double();p.moveTo(0,-s);p.curveTo(s*.85,-s*.45,s*.65,s*.35,0,s);p.curveTo(-s*.65,s*.35,-s*.85,-s*.45,0,-s);p.closePath();
        AffineTransform a=new AffineTransform();a.translate(x,y);a.rotate(rot);ink.add(a.createTransformedShape(p));
        ink.add(a.createTransformedShape(new Line2D.Double(0,-s*.8,0,s*.9)));
    }
    static void feather() {
        curve(79,213, 101,174,149,126,171,43);
        curve(83,201, 46,104,135,28,176,35, 204,113,140,184,83,201);
        for(int i=0;i<6;i++) {double y=76+i*20,x=167-i*11;line(x,y,x-37,y-7);line(x,y,x+17,y-20);}
    }
    static void sword() {line(119,170,119,75,128,35,137,75,137,170);line(128,56,128,166);line(97,171,159,171);line(120,179,120,205,136,205,136,179);circle(128,218,7);}
    static void bowl() {curve(66,128, 72,212,184,212,190,128);line(66,128,190,128);oval(67,113,122,27);line(84,195,78,217);line(172,195,178,217);}
    static void glyph(String motif) {
        switch(motif) {
        case "feather": feather();break;
        case "bow": curve(90,38,192,80,192,177,90,218);line(90,38,90,218);line(55,128,210,128);line(190,109,210,128,190,147);circle(90,128,7);break;
        case "swords": rotate(-.45,BuildEffectPack::sword);rotate(.45,BuildEffectPack::sword);break;
        case "spear": case "dagger": sword();break;
        case "hammer": line(112,117,112,215,139,215,139,117);line(65,60,186,60,199,74,199,112,186,124,65,124,53,112,53,74,65,60);line(77,60,77,124);line(174,60,174,124);line(118,76,137,76,122,96,137,96,115,113);break;
        case "shield": curve(128,37,102,58,78,60,55,68,55,153,81,191,128,219,175,191,201,153,201,68,174,60,152,58,128,37);curve(128,58,107,73,86,78,74,82,76,145,91,174,128,198,165,174,180,145,182,82,163,77,145,71,128,58);star(128,123,34);break;
        case "book": curve(128,76,101,50,67,51,38,63);line(38,63,38,181);curve(38,181,75,166,104,174,128,194,153,174,181,166,218,181);line(218,181,218,63);curve(218,63,187,51,155,50,128,76);line(128,76,128,194);for(int i=0;i<4;i++){line(57,85+i*20,106,96+i*20);line(150,96+i*20,197,85+i*20);}break;
        case "sun": circle(128,128,43);circle(128,128,56);for(int i=0;i<12;i++){final int n=i;rotate(i*Math.PI/6,()->{if(n%2==0)line(122,59,128,30,134,59);else line(128,52,128,40);});}star(128,128,23);break;
        case "moon": curve(149,35,26,36,21,203,150,218,101,171,91,93,149,35);star(178,105,17);star(184,159,9);break;
        case "eclipse": circle(128,128,54);arc(73,10,158);arc(73,190,158);oval(26,102,204,52);for(int i=0;i<5;i++){final int n=i;rotate(i*1.256,()->diamond(128,34,7+n%2*3));}break;
        case "clock": circle(128,128,86);circle(128,128,77);for(int i=0;i<12;i++)rotate(i*Math.PI/6,()->line(128,58,128,70));line(128,77,128,128,165,150);circle(128,128,7);break;
        case "orrery": circle(128,128,75);oval(103,41,50,174);rotate(.75,()->oval(48,104,160,48));circle(128,128,16);star(192,84,10);break;
        case "gear": for(int i=0;i<10;i++)rotate(i*Math.PI/5,()->line(111,65,111,44,145,44,145,65));circle(128,128,63);circle(128,128,39);star(128,128,14);break;
        case "sigil": circle(128,128,83);circle(128,128,73);line(128,47,198,169,58,169,128,47);line(128,209,58,87,198,87,128,209);circle(128,128,24);star(128,128,11);break;
        case "snow": for(int i=0;i<6;i++)rotate(i*Math.PI/3,()->{line(128,128,128,39);line(105,55,128,77,151,55);line(108,92,128,111,148,92);diamond(128,38,6);});break;
        case "fruit":
            curve(98,65,31,74,32,194,128,218,224,194,225,74,158,65);line(98,65,94,41,115,54,128,32,141,54,162,41,158,65);curve(128,81,91,113,159,164,128,205);for(int i=0;i<3;i++)for(int j=0;j<3;j++)diamond(85+i*43,104+j*33,7);break;
        case "rose":
            for(int i=0;i<5;i++)rotate(i*Math.PI*.4,()->curve(128,122,91,103,78,48,120,43,165,26,190,86,142,123));
            for(int i=0;i<5;i++)rotate(i*Math.PI*.4,()->curve(128,131,101,101,110,78,130,89,151,99,151,121,128,131));circle(128,128,12);break;
        case "lotus": case "orchid":
            for(int i=0;i<(motif.equals("orchid")?5:8);i++) {final int n=i;rotate(i*Math.PI*2/(motif.equals("orchid")?5:8),()->leaf(128,85,43,n%2*.08));}
            circle(128,128,18);for(int i=0;i<5;i++)rotate(i*Math.PI*.4,()->diamond(128,119,4));break;
        case "leaf":
            curve(118,224,145,159,114,99,140,35);for(int i=0;i<4;i++){leaf(106,183-i*34,21,-.85);leaf(152,165-i*34,21,.85);}break;
        case "wheat":
            // A grain ear with awns, distinct from the herb sprig.
            curve(128,230,124,180,132,120,128,52);curve(126,206,96,192,76,166,64,132);
            for(int i=0;i<6;i++){double y=176-i*22;leaf(114,y,13,-.55);leaf(142,y-8,13,.55);line(108,y-10,92,y-40);line(148,y-18,164,y-48);}
            leaf(128,42,13,0);line(128,30,128,8);break;
        case "vine":
            // Roots reach down and curl, where the gameplay effect grips the ankles.
            curve(128,30,118,62,138,92,128,124);leaf(110,44,17,-.95);leaf(148,38,17,.95);
            for(int i=0;i<5;i++){double dx=(i-2)*42;double curl=i<2?-1:i>2?1:(i==2?0:1);curve(128,124,128+dx*.3,150,128+dx*.9,170,128+dx,206);curve(128+dx,206,128+dx,222,128+dx+curl*16,224,128+dx+curl*12,212);}
            for(int s:new int[]{-1,1})line(128+s*30,160,128+s*52,150);break;
        case "laurel": for(int s:new int[]{-1,1}) {curve(128+s*8,211,128+s*85,188,128+s*86,98,128+s*39,43);for(int i=0;i<5;i++)leaf(128+s*(64-i*i*1.4),174-i*24,18,s*.85);}star(128,74,16);break;
        case "rainbow": for(int i=0;i<7;i++)ink.add(new Arc2D.Double(27+i*10,37+i*10,202-i*20,202-i*20,0,180,Arc2D.OPEN));break;
        case "wind": for(int i=0;i<3;i++){final int n=i;curve(33,102+n*31,84,52+n*34,158,163-n*8,201,70+n*29,226,33+n*33,168,31+n*28,176,60+n*25);}break;
        case "flame": curve(126,221,21,192,59,108,90,98,89,132,106,140,113,142,88,75,146,42,157,24,148,85,208,105,203,157,201,204,163,222,126,221);curve(128,204,90,180,121,132,132,114,128,154,163,163,157,184,152,201,140,210,128,204);break;
        case "bolt": line(145,31,69,137,122,137,102,225,190,107,138,107,145,31);line(58,87,40,104);line(199,166,216,148);break;
        case "cross": line(113,46,143,46,143,97,193,97,193,127,143,127,143,211,113,211,113,127,63,127,63,97,113,97,113,46);star(128,110,14);break;
        case "caduceus": line(128,35,128,222);circle(128,37,11);curve(98,68,46,95,190,105,157,133,68,164,157,168,141,199);curve(158,68,211,95,67,105,99,133,186,164,99,168,115,199);line(93,64,103,73,92,81);line(163,64,153,73,164,81);break;
        case "serpent":
            // Two rails give the coiled body visible width at a distance.
            scale(1.22,()->{curve(85,217,221,180,48,150,144,118,232,72,119,30,76,79);curve(97,209,201,180,64,148,148,108,212,74,122,44,84,86);
                line(76,79,86,56,111,62,105,85,76,79);line(79,81,58,88,45,81);circle(97,68,3);});break;
        case "heart": curve(128,202,91,178,43,139,46,93,49,50,105,44,128,82,151,44,207,50,210,93,213,139,165,178,128,202);break;
        case "chain":
            // Two chain arms lock into a central padlock.
            ink.add(new RoundRectangle2D.Double(90,118,76,80,16,16));curve(102,118,102,64,154,64,154,118);curve(112,118,112,80,144,80,144,118);
            circle(128,150,9);line(128,158,128,180);
            for(int s:new int[]{-1,1}){oval(s<0?8:200,140,48,28);oval(s<0?44:162,146,50,16);}break;
        case "knot": circle(98,132,56);circle(158,132,56);circle(98,132,44);circle(158,132,44);star(128,52,14);break;
        case "cloak":
            curve(128,28,88,30,76,72,86,104,66,150,52,190,44,222,80,210,100,230,128,216,156,230,176,210,212,222,204,190,190,150,170,104,180,72,168,30,128,28);
            curve(128,56,104,58,100,88,110,104,120,114,136,114,146,104,156,88,152,58,128,56);
            line(110,132,96,208);line(146,132,160,208);line(128,124,128,212);break;
        case "quench":
            // Glowing blade dipped in water, with steam leaving the surface.
            rotate(.62,BuildEffectPack::sword);
            curve(30,196,64,178,96,214,128,196,160,178,192,214,226,196);curve(48,222,80,206,104,236,136,222,166,208,190,234,214,222);
            for(int i=0;i<3;i++)curve(54+i*34,172,34+i*34,140,78+i*34,128,58+i*34,92);break;
        case "eye": curve(30,128,91,53,165,53,226,128,166,203,90,203,30,128);circle(128,128,36);circle(128,128,13);line(66,62,48,41);line(190,62,208,41);break;
        case "mask": curve(54,72,92,88,164,88,202,72,213,161,166,204,128,218,90,204,43,161,54,72);line(71,113,110,129,81,142);line(185,113,146,129,175,142);curve(101,174,117,187,139,187,155,174);line(60,79,65,39,91,81);line(196,79,191,39,165,81);break;
        case "crystal": line(128,32,183,84,178,169,128,223,78,169,73,84,128,32);line(128,32,109,84,116,172,128,223,140,172,147,84,128,32);line(73,84,183,84);line(78,169,178,169);break;
        case "fist": line(58,122,66,80,92,80,92,60,121,60,121,55,148,55,148,66,176,66,176,115,191,123,189,181,164,211,90,211,65,177,58,122);line(92,86,92,128,176,128);line(120,69,120,125);line(147,75,147,125);line(76,151,128,151,139,169,174,169);break;
        case "trident": line(119,222,119,111,71,111,56,89,56,46,73,66,73,91,117,91,117,49,128,27,139,49,139,91,183,91,183,66,200,46,200,89,185,111,137,111,137,222);break;
        case "anchor": circle(128,52,20);line(128,72,128,215);line(84,98,172,98);curve(128,214,85,211,50,184,46,151);curve(128,214,171,211,206,184,210,151);line(37,176,46,148,70,165);line(219,176,210,148,186,165);break;
        case "hook": curve(128,44,111,75,138,104,128,141);curve(128,141,115,231,213,220,186,155);line(186,155,171,182);arc(87,208,122);break;
        case "scales": line(128,46,128,212);line(81,214,175,214);line(58,79,198,79);circle(128,53,10);for(int s:new int[]{-1,1}){double x=128+s*67;line(x,81,x-30,147,x+30,147,x,81);curve(x-30,147,x-18,177,x+18,177,x+30,147);}break;
        case "chalice": case "cup": case "cauldron": bowl();if(motif.equals("chalice")){line(128,197,128,232);line(100,232,156,232);}else if(motif.equals("cup"))curve(190,132,231,102,240,172,187,169);for(int i=0;i<3;i++)curve(89+i*38,99,60+i*38,65,112+i*38,60,87+i*38,27);break;
        case "spoon": oval(91,35,74,92);line(117,127,117,214,139,214,139,127);break;
        case "record": circle(128,128,84);circle(128,128,64);circle(128,128,28);circle(128,128,8);arc(72,15,83);arc(52,192,78);break;
        case "coin": circle(128,128,78);circle(128,128,67);line(128,79,128,179);curve(150,93,100,65,93,127,128,128,175,130,156,179,104,160);break;
        case "cards": for(int i=-1;i<=1;i++)rotate(i*.28,()->{line(81,51,175,51,175,207,81,207,81,51);diamond(128,128,29);star(95,73,6);star(161,185,6);});break;
        case "seal": ink.add(new Rectangle2D.Double(63,57,130,142));ink.add(new Rectangle2D.Double(76,70,104,116));line(94,98,161,98,161,155,95,155,95,123,148,123);line(127,85,127,171);break;
        case "banner": line(73,32,73,223);curve(75,45,136,15,150,88,208,57);line(208,57,208,157);curve(208,157,149,187,130,115,75,144);star(140,101,23);break;
        case "ship": line(39,147,67,195,190,195,220,147,39,147);line(51,139,79,102,176,102,205,139);line(88,100,88,68,167,68,167,100);line(128,68,128,35);line(128,35,169,49,128,58);for(int i=0;i<5;i++){line(71+i*27,154,81+i*27,188);diamond(86+i*22,127,8);}curve(35,212,62,194,80,230,107,213,134,195,161,229,187,213,201,205,218,203,228,212);break;
        case "rocket": curve(128,30,73,72,79,151,88,180);curve(128,30,183,72,177,151,168,180);line(88,180,168,180);circle(128,97,22);line(87,128,57,188,89,174);line(169,128,199,188,167,174);line(106,189,111,218);line(128,189,128,237);line(150,189,145,218);break;
        case "bomb": circle(128,143,67);line(111,76,111,60,148,60,148,76);curve(128,60,121,24,165,24,167,49);star(174,38,15);arc(49,105,51);break;
        case "bullet": line(83,82,166,82,166,191,83,191,83,82);curve(83,82,95,57,113,44,128,31,148,51,159,59,166,82);line(85,172,165,172);line(91,209,70,233);line(153,209,174,233);break;
        case "reticle": arc(70,8,74);arc(70,98,74);arc(70,188,74);arc(70,278,74);line(128,25,128,84);line(128,172,128,231);line(25,128,84,128);line(172,128,231,128);circle(128,128,17);break;
        case "rift": oval(58,29,140,198);oval(72,39,112,178);line(117,60,143,104,114,144,143,191);break;
        case "bee": oval(99,97,60,104);oval(52,60,71,86);oval(135,60,71,86);line(106,117,151,117);line(103,147,155,147);line(106,175,151,175);line(114,96,102,71);line(144,96,156,71);break;
        case "raven": curve(128,158,86,141,70,75,27,71,36,110,64,141,92,151);curve(128,158,170,141,186,75,229,71,220,110,192,141,164,151);line(105,157,117,192,128,175,139,192,151,157);circle(129,128,19);line(145,123,168,133,145,138);break;
        case "doll": circle(128,68,32);line(103,99,62,124,73,148,105,136,96,214,120,218,128,172,136,218,160,214,151,136,183,148,194,124,153,99);for(int x:new int[]{115,141}){line(x-5,61,x+5,73);line(x+5,61,x-5,73);}line(116,85,139,85);line(114,139,144,168);line(118,155,137,149);break;
        case "feast": oval(37,139,181,62);oval(56,151,143,32);curve(76,147,70,65,181,61,184,147);line(84,117,178,117);star(128,51,13);break;
        // Workshop and economy props: the objects these abilities actually make, trade or examine.
        case "anvil": line(40,92,196,92,222,104,186,118,176,118,168,140,150,150,150,186,186,196,186,212,70,212,70,196,106,186,106,150,88,140,80,118,52,118,40,92);line(62,102,178,102);break;
        case "ingot": for(double[] b:new double[][]{{22,168},{134,168},{78,124}}){double x=b[0],y=b[1];line(x+16,y,x+100,y,x+116,y+36,x,y+36,x+16,y);line(x+24,y+9,x+60,y+9);}break;
        case "gem": line(78,62,178,62,216,104,128,216,40,104,78,62);line(40,104,216,104);line(78,62,104,104,128,216,152,104,178,62);line(104,104,128,62,152,104);break;
        case "sword": sword();break;
        case "pickaxe": curve(40,92,92,40,164,40,216,92);curve(216,92,170,70,86,70,40,92);line(122,62,122,226,138,226,138,62);line(112,104,148,104);break;
        case "swirl": for(int i=0;i<3;i++){final int n=i;rotate(n*Math.PI*2/3,()->curve(128,128,128,86,182,74,196,110,206,140,180,170,150,160));}circle(128,128,14);break;
        case "drop": curve(128,30,150,80,196,118,196,160,196,200,164,226,128,226,92,226,60,200,60,160,60,118,106,80,128,30);curve(92,160,92,186,108,200,128,202);break;
        case "note": oval(58,168,64,46);oval(150,146,64,46);line(118,190,118,48,210,30,210,168);line(118,82,210,64);break;
        case "arrows": for(int i=-1;i<=1;i++){final int n=i;rotate(-.6+n*.16,()->{line(128,34,128,214);line(112,62,128,30,144,62,112,62);line(128,214,110,232);line(128,214,146,232);line(128,198,110,216);line(128,198,146,216);});}break;
        case "pan": oval(30,98,150,96);oval(48,114,114,64);line(178,138,238,124,242,140,180,156);break;
        case "foot": oval(98,98,62,112);circle(102,74,9);circle(121,62,10);circle(142,63,9);circle(158,73,8);circle(167,90,7);break;
        case "question": curve(82,98,82,30,176,30,176,96,176,140,128,124,128,172);circle(128,208,13);break;
        case "tag": line(56,72,160,72,224,128,160,184,56,184,56,72);circle(184,128,10);circle(88,104,13);circle(128,154,13);line(134,94,82,164);break;
        case "compass": circle(128,128,98);circle(128,128,86);line(128,42,152,128,128,214,104,128,128,42);line(104,128,152,128);for(int i=0;i<4;i++)rotate(i*Math.PI/2,()->diamond(128,20,6));circle(128,128,8);break;
        case "scroll": line(68,58,190,58,190,198,68,198,68,58);oval(54,44,26,28);oval(176,184,26,28);line(88,96,170,96);line(88,124,170,124);line(88,152,142,152);break;
        case "xmark": circle(128,128,98);line(78,78,178,178);line(178,78,78,178);line(92,70,186,164);line(70,92,164,186);break;
        case "hunger": curve(66,58,26,92,34,176,98,178,150,180,176,128,164,92,152,52,108,30,66,58);curve(84,88,70,108,74,138,96,146);line(150,152,198,200);circle(208,194,13);circle(194,210,13);break;
        default: throw new IllegalArgumentException("Unpainted motif: "+motif);
        }
    }
    /** Four alpha steps per sprite; ArtModels chooses one per frame to fade without extra entities. */
    static void sprite(Path root,String base,BufferedImage image) throws Exception {
        for(int fade=0;fade<4;fade++)texture(root,base+fade,VfxPainter.faded(image,fade));
    }
    /** One-shot animation: one sprite per frame; the server selects the frame from the scene's age. */
    static void frames(Path root,String base,List<BufferedImage> frames) throws Exception {
        for(int i=0;i<frames.size();i++)texture(root,base+i,frames.get(i));
    }
    /** Looping animation: a vertical strip per fade step, played smoothly by the client through .mcmeta. */
    static void strip(Path root,String base,List<BufferedImage> frames,boolean interpolate) throws Exception {
        int size=frames.get(0).getWidth();
        for(int fade=0;fade<4;fade++) {
            BufferedImage strip=new BufferedImage(size,size*frames.size(),BufferedImage.TYPE_INT_ARGB);Graphics2D g=strip.createGraphics();
            for(int i=0;i<frames.size();i++)g.drawImage(VfxPainter.faded(frames.get(i),fade),0,i*size,null);
            g.dispose();texture(root,base+fade,strip);
            write(root,"assets/newgodwar/textures/"+base+fade+".png.mcmeta","{\"animation\":{\"frametime\":2,\"interpolate\":"+interpolate+"}}");
        }
    }
    static void texture(Path root,String path,BufferedImage image) throws Exception {
        Path png=root.resolve("assets/newgodwar/textures/"+path+".png");Files.createDirectories(png.getParent());
        ImageIO.write(image,"png",png.toFile());
        write(root,"assets/newgodwar/models/"+path+".json","{\"textures\":{\"particle\":\"#art\",\"art\":\"newgodwar:"+path+"\"},\"elements\":[{\"from\":[0,0,8],\"to\":[16,16,8],\"shade\":false,\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#art\"},\"south\":{\"uv\":[16,0,0,16],\"texture\":\"#art\"}}}]}");
        write(root,"assets/newgodwar/items/"+path+".json","{\"model\":{\"type\":\"minecraft:model\",\"model\":\"newgodwar:"+path+"\"}}");
    }
    static void write(Path root,String path,String text) throws Exception {Path p=root.resolve(path);Files.createDirectories(p.getParent());Files.writeString(p,text,StandardCharsets.UTF_8);}
    static Path2D foodPath(double... points) {
        Path2D p=new Path2D.Double();p.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=6)p.curveTo(points[i],points[i+1],points[i+2],points[i+3],points[i+4],points[i+5]);
        p.closePath();return p;
    }
    static Color hex(int value){return new Color(value);}
    static void foodFill(Graphics2D g,Shape s,int top,int bottom) {
        g.setPaint(new GradientPaint(80,55,hex(top),160,205,hex(bottom)));g.fill(s);
        g.setColor(hex(0x443343));g.setStroke(new BasicStroke(5,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(s);
    }
    static void foodStroke(Graphics2D g,int color,float width,double... pts) {
        Path2D p=new Path2D.Double();p.moveTo(pts[0],pts[1]);for(int i=2;i<pts.length;i+=2)p.lineTo(pts[i],pts[i+1]);
        g.setColor(hex(color));g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(p);
    }
    static void garnish(Graphics2D g,double x,double y,double angle) {
        AffineTransform old=g.getTransform();g.translate(x,y);g.rotate(angle);
        foodFill(g,foodPath(0,20,-34,7,-31,-19,0,-31,24,-9,27,10,0,20),0xb4e683,0x397d60);
        foodStroke(g,0xe0f5b0,2,0,12,0,-19);g.setTransform(old);
    }
    static BufferedImage food(int which) {
        BufferedImage img=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);Graphics2D g=img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.scale(.5,.5);
        switch(which) {
        case 0: // Folded, warm crescent; its blue wind curls distinguish the speed meal.
            foodFill(g,foodPath(35,148,20,101,44,52,93,50,111,34,145,34,166,50,215,53,237,112,219,153,207,182,186,187,174,178,202,138,183,98,154,102,135,81,114,87,100,104,69,102,54,139,78,174,61,187,45,175,35,148),0xffd992,0xb55b36);
            for(int i=0;i<4;i++){double x=78+i*29;foodStroke(g,0x9b5939,5,x,57+Math.abs(i-1.5)*5,x+6,84,x+2,106);foodStroke(g,0xffedb3,5,x+9,61,x+13,82);}
            foodStroke(g,0x80dbea,5,29,192,75,203,117,196);foodStroke(g,0xc0f4f5,3,149,202,192,207,230,186);break;
        case 1: // Roasted drumstick with white bone and fresh medicinal herbs.
            foodFill(g,foodPath(151,153,176,161,187,189,206,195,230,174,242,197,226,211,229,237,205,238,198,219,172,199,154,187,138,177),0xfff4dc,0xd4b68d);
            foodFill(g,foodPath(142,170,95,192,42,159,39,115,29,73,81,38,127,50,167,53,193,99,175,133,164,154,159,164,142,170),0xfbd083,0xa84e35);
            foodStroke(g,0xffe2a0,10, 60,88,79, 70,108,66);foodStroke(g,0x934330,6,65,133,94,153,120,150);
            garnish(g,62,184,-.8);garnish(g,92,185,.4);garnish(g,47,159,-1.4);break;
        case 2: // Red steak, cream fat border, cross-grill and a fiery pepper garnish.
            foodFill(g,foodPath(51,87,82,45,119,69,145,51,189,31,230,79,218,120,203,178,156,207,94,200,45,194,22,144,51,87),0xffd7a5,0xc4805a);
            foodFill(g,foodPath(65,92,91,64,121,90,147,69,177,47,207,89,198,119,185,161,146,186,97,180,60,180,42,132,65,92),0xed8d73,0x92414b);
            for(int i=0;i<3;i++)foodStroke(g,0x673540,7,78+i*34,88, 60+i*34,154);
            for(int i=0;i<3;i++)foodStroke(g,0xffb08a,3,65,107+i*22,173,116+i*19);
            foodFill(g,foodPath(176,215,163,192,192,181,192,158,222,182,230,211,201,223,191,227,182,222,176,215),0xffce60,0xdd5844);garnish(g,50,183,-1);break;
        case 3: // Split golden potato, pale fluffy centre, butter and chives.
            foodFill(g,foodPath( 40,125,47, 70,125,40,184,62,229,84,229,139,200,175,162,222,89,222, 50,181,34,163,32,143,40,125),0xf3c673,0xa66b40);
            foodFill(g,foodPath(60,134,75,98,131,72,180,82,192,114,166,161,114,181,79,180,67,154,60,134),0xfff3c4,0xe9c077);
            foodFill(g,new RoundRectangle2D.Double(109,104, 50,39,8,8),0xffed7d,0xe9b649);
            for(int i=0;i<5;i++)foodStroke(g,0x669d59,4,82+i*21,141+(i%2)*18, 90+i*21,133+(i%2)*18);
            garnish(g, 40, 90,-.6);g.setColor(hex(0xffefb0));g.fillOval(177, 50,10,10);break;
        case 4: // Three glazed ribs, ivory bone tips, honey droplets.
            for(int i=0;i<3;i++){AffineTransform old=g.getTransform();g.rotate(-.32,128,128);int y=68+i* 40;
                foodFill(g,new RoundRectangle2D.Double(49,y+9,161,21,20,20),0xfff2d1,0xd1b08a);
                foodFill(g,new RoundRectangle2D.Double( 70,y,116,40,23,23),0xe9ad63,0xa94e39);
                foodStroke(g,0xffd482,5,85,y+10,113,y+8,157,y+10);foodStroke(g,0x893f32,4,115,y+5,119,y+30,141,y+35);g.setTransform(old);}
            foodFill(g,foodPath(213, 40,187, 70,196, 80,213,81,233,80,239,64,213,40),0xffe486,0xdf8c34);garnish(g,55,199,.8);break;
        case 5: // Whole silver-blue fish with warm roast centre, fins and lemon.
            foodFill(g,foodPath(183,111,210,96,228,81,235,81,222,118,223,148,231,169,207,168,194,147,181,139),0x8ed3d9,0x508ba3);
            foodFill(g,foodPath( 30,128,69,58,161,64,197,120,178,184,78,197,30,128),0xbbe6e4,0x6599a8);
            foodFill(g,foodPath( 70,132,90,95,149,92,177,124,151,163,94,171,70,132),0xffe0a0,0xd59a63);
            for(int i=0;i<3;i++)foodStroke(g,0x9c744e,5,99+i*24,115,89+i*24,143);
            g.setColor(hex(0x30434e));g.fillOval(52,116,11,11);g.setColor(Color.WHITE);g.fillOval(54,117,4,4);
            foodFill(g,new Ellipse2D.Double(153,173, 50, 50),0xffee89,0xd4af49);foodStroke(g,0xfff4bf,3,178,180,178,214,160,194,197,197);garnish(g,97, 60,-.2);break;
        }
        g.dispose();return img;
    }
    static void foods(Path repo,Path root) throws Exception {
        String[] names={"wind_croissant","herb_chicken","flame_steak","golden_potato","honey_pork","cloud_fish"};
        String[] materials={"bread","cooked_chicken","cooked_beef","baked_potato","cooked_porkchop","cooked_cod"};
        String[] labels={"바람결 크루아상","생명의 허브구이","용사의 불꽃 스테이크","수호의 황금 감자","장인의 꿀 바비큐","구름 도약 생선구이"};
        BufferedImage sheet=new BufferedImage(960,260,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
        g.setColor(hex(0x172132));g.fillRect(0,0,960,260);g.setFont(new Font("Malgun Gothic",Font.PLAIN,13));
        for(int i=0;i<6;i++) {
            String path="art/food/"+names[i];BufferedImage img=food(i);Path png=root.resolve("assets/newgodwar/textures/"+path+".png");Files.createDirectories(png.getParent());ImageIO.write(img,"png",png.toFile());
            write(root,"assets/newgodwar/models/"+path+".json","{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"newgodwar:"+path+"\"}}");
            String original="{\"type\":\"minecraft:model\",\"model\":\"minecraft:item/"+materials[i]+"\"}";
            String special="{\"type\":\"minecraft:model\",\"model\":\"newgodwar:"+path+"\"}";
            // The upper threshold restores vanilla for other plugins' larger custom model IDs.
            write(root,"assets/minecraft/items/"+materials[i]+".json","{\"model\":{\"type\":\"minecraft:range_dispatch\",\"property\":\"minecraft:custom_model_data\",\"index\":0,\"fallback\":"+original+",\"entries\":[{\"threshold\":"+(73101+i)+",\"model\":"+special+"},{\"threshold\":"+(73102+i)+",\"model\":"+original+"}]}}");
            g.drawImage(img,i*160+16,24,128,128,null);g.setColor(hex(0xffe9bd));g.drawString(labels[i],i*160+5,186);g.setColor(hex(0xa8c4cd));g.drawString(materials[i],i*160+12,213);
        }
        g.dispose();ImageIO.write(sheet,"png",repo.resolve("build/effect-pack/siksin-foods.png").toFile());
    }
    public static void main(String[] args) throws Exception {
        Path repo=Path.of(args.length==0?".":args[0]).toAbsolutePath().normalize();
        Path root=repo.resolve("build/effect-pack/NewGodWar-Art");
        // Start clean: VersionedPacks zips every staged file, so retired sprites must not linger.
        if(Files.exists(root))try(var stale=Files.walk(root)){for(Path path:stale.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}
        Files.createDirectories(root);
        // VersionedPacks writes pack.mcmeta; textures under item/ need no atlas registration in any release.
        List<String> profiles=Files.readAllLines(repo.resolve("plugin/src/main/resources/effect-art.tsv"));
        List<String[]> rows=new ArrayList<>();
        for(String row:profiles)if(!row.startsWith("#")&&!row.isBlank())rows.add(row.split("\\|"));
        Set<String> families=new TreeSet<>();for(String[] p:rows)families.add(p[4]);
        int columns=8,cell=160,sheetRows=(rows.size()+columns-1)/columns+families.size();
        BufferedImage sheet=new BufferedImage(columns*cell,sheetRows*cell,BufferedImage.TYPE_INT_RGB);Graphics2D g=sheet.createGraphics();
        g.setColor(new Color(15,20,32));g.fillRect(0,0,sheet.getWidth(),sheet.getHeight());g.setFont(new Font("SansSerif",Font.PLAIN,10));
        int index=0;
        for(String[] p:rows) {
            // Each ability owns its emblem; the surrounding light, waves and debris come from its palette.
            String key=p[0].toLowerCase(Locale.ROOT).replace('.','/');Color color=new Color(Integer.parseInt(p[2],16));
            ink.clear();glyph(p[1]);
            BufferedImage emblem=VfxPainter.emblem(new ArrayList<>(ink),color,VfxPainter.FAMILIES.get(p[4]),p[1].equals("rainbow"));
            sprite(root,"art/"+key+"/glyph",emblem);
            List<BufferedImage> traced=new ArrayList<>();
            for(int i=0;i<VfxAnimator.TRACE;i++)traced.add(VfxAnimator.trace(new ArrayList<>(ink),VfxPainter.FAMILIES.get(p[4]),.4+.4*i));
            frames(root,"art/"+key+"/draw",traced);
            if(p.length>5)for(String prop:p[5].split(",")) {
                // Secondary props (an anvil, the finished product, a price tag...) share the ability's light.
                String[] role=prop.split(":");ink.clear();glyph(role[1]);
                sprite(root,"art/"+key+"/"+role[0],VfxPainter.emblem(new ArrayList<>(ink),color,VfxPainter.FAMILIES.get(p[4]),false));
            }
            int x=index%columns*cell,y=index/columns*cell;g.drawImage(emblem,x+16,y+4,128,128,null);
            g.setColor(color);g.drawString(p[0],x+8,y+142);g.drawString(p[3]+" · "+p[4],x+8,y+155);
            index++;
        }
        int row=(index+columns-1)/columns;
        for(String family:families) {
            int column=0;
            for(String layer:VfxPainter.LAYERS) {
                BufferedImage image=VfxPainter.layer(layer,VfxPainter.FAMILIES.get(family));
                sprite(root,"art/fx/"+family+"/"+layer,image);
                g.drawImage(image,column*cell+16,row*cell+4,128,128,null);g.setColor(Color.LIGHT_GRAY);g.drawString(family+"/"+layer,column*cell+8,row*cell+148);
                column++;
            }
            row++;
        }
        g.dispose();ImageIO.write(sheet,"png",repo.resolve("build/effect-pack/contact-sheet.png").toFile());
        // Animated layers, plus a frame-by-frame preview sheet (one row per palette and animation).
        BufferedImage frameSheet=new BufferedImage(LOOP_COLUMNS*96,families.size()*5*104,BufferedImage.TYPE_INT_RGB);Graphics2D a=frameSheet.createGraphics();
        a.setColor(new Color(15,20,32));a.fillRect(0,0,frameSheet.getWidth(),frameSheet.getHeight());a.setFont(new Font("SansSerif",Font.PLAIN,10));
        int line=0;
        for(String family:families) {
            VfxPainter.Family palette=VfxPainter.FAMILIES.get(family);
            Map<String,List<BufferedImage>> animated=new LinkedHashMap<>();
            for(String layer:VfxAnimator.SEQUENCES){List<BufferedImage> f=VfxAnimator.sequence(layer,palette);frames(root,"art/fx/"+family+"/"+layer,f);animated.put(layer,f);}
            for(String layer:VfxAnimator.LOOPS){List<BufferedImage> f=VfxAnimator.loop(layer,palette);// Lightning re-forks with hard cuts; crossfading two bolts would read as a ghost.
                strip(root,"art/fx/"+family+"/"+layer,f,!(family.equals("storm")&&layer.equals("shard")));animated.put(layer,f);}
            for(var entry:animated.entrySet()) {
                for(int i=0;i<entry.getValue().size();i++)a.drawImage(entry.getValue().get(i),i*96+4,line*104+2,88,88,null);
                a.setColor(Color.LIGHT_GRAY);a.drawString(family+"/"+entry.getKey(),4,line*104+100);line++;
            }
        }
        a.dispose();ImageIO.write(frameSheet,"png",repo.resolve("build/effect-pack/animation-frames.png").toFile());
        int menuIcons=GuiArtwork.writePack(root,repo.resolve("build/effect-pack/menu-icons.png"));
        foods(repo,root);
        VersionedPacks.build(repo,root);
        System.out.println(index+" emblems + "+families.size()+" palettes + "+menuIcons+" menu icons; versioned packs complete.");
    }
}
