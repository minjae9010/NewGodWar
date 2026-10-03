package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.builtin.AbilityDesigns;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Action-specific world-space choreography. Original geometry remains the no-pack fallback. */
public final class ArtModels {
    private ArtModels() { }
    private static final Map<ObjectModel,ObjectModel> VARIANTS = new IdentityHashMap<ObjectModel,ObjectModel>();
    // BuildEffectPack paints the boundary's bright core at radius 94 on a 256px canvas.
    private static final double BOUNDARY_TEXTURE_RADIUS = 94.0 / 256.0;
    private static boolean initialized;

    public static synchronized ObjectModel variant(ObjectModel source) {
        if (!initialized) {
            initialized = true;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    Objects.requireNonNull(ArtModels.class.getResourceAsStream("/effect-art.tsv")), StandardCharsets.UTF_8))) {
                String row;
                while ((row=reader.readLine()) != null) {
                    if(row.startsWith("#") || row.trim().isEmpty())continue;
                    String[] fields=row.split("\\|");String[] name=fields[0].split("\\.");
                    Class<?> owner;
                    if(name[0].equals("design"))owner=AbilityDesigns.class;
                    else if(name[0].equals("shared"))owner=SharedModels.class;
                    else owner=Class.forName("kr.newgodwar.ability.builtin."+Character.toUpperCase(name[0].charAt(0))+name[0].substring(1)+"Ability");
                    Field field=owner.getDeclaredField(name[1]);field.setAccessible(true);Object value=field.get(null);
                    String key=fields[0].toLowerCase(Locale.ROOT).replace('.', '/');
                    if(value instanceof DesignedEffect) {
                        DesignedEffect effect=(DesignedEffect)value;
                        VARIANTS.put(effect.model(true), create(key,fields[3],true,false));
                        VARIANTS.put(effect.model(false), create(key,fields[3],true,true));
                    } else VARIANTS.put((ObjectModel)value,create(key,fields[3],false,false));
                }
            } catch (Exception error) {throw new IllegalStateException("Invalid bundled effect art catalogue",error);}
        }
        return VARIANTS.get(source);
    }

    public static ObjectModel create(String key,String motion,boolean shortAction,boolean still) {
        return ObjectModel.animated((phase,detail)->frame(key,motion,still?8:phase,detail,shortAction,still));
    }

    private static List<ObjectModel.Part> frame(String key,String motion,double phase,double detail,boolean shortAction,boolean still) {
        Frame f=new Frame(key,phase,shortAction,still);
        double t=f.t, o=f.open, p=f.progress, a=t*.065;
        switch(motion) {
        case "phalanx":
            for(int i=-2;i<=2;i++) {
                double angle=i*Math.PI/5,x=Math.sin(angle)*4.5,z=Math.cos(angle)*4.5;
                f.glyph(x,1.15,z,1.25,1.9,0,-angle,0);
                f.arc(x,1.15,z+.02,1.45,2.0,Math.sin(t*.08+i)*.08,-angle,0);
            }
            break;
        case "wings":
            for(int side:new int[]{-1,1})for(int i=0;i<5;i++) {
                double flap=Math.sin(t*.18+i*.17)*.13;
                f.glyph(side*(.43+i*.24)*f.scale,1.48-i*.065+flap,-.32-i*.045,
                        .78,1.55,side*(.45+i*.10),side*flap,0);
            }
            break;
        case "clock": {
            double r=Math.max(.5,detail);
            // Only genuine area effects draw a floor boundary; its visible bright core marks r.
            f.floor("glyph",0,.055,0,r*2,-a*.12);
            f.floor("ring",0,.065,0,r*2,a*.07);
            for(int i=0;i<4;i++) {
                double angle=i*Math.PI/2;
                f.mote(Math.cos(angle)*r,.12,Math.sin(angle)*r,.17,.17,angle);
            }
            break;
        }
        case "fire-rune": case "frost-rune": {
            double r=Math.max(.5,Math.min(3,detail));
            boolean frost=motion.equals("frost-rune");
            f.floor("glyph",0,.07,0,r*2,frost?0:-a*.08);
            f.floor("ring",0,.055,0,r*2,0);
            int arms=frost?6:3;
            for(int i=0;i<arms;i++) {
                double angle=i*Math.PI*2/arms,travel=frost?(.75+.15*Math.sin(a+i)):(.2+.65*cycle(t*.025+i/(double)arms));
                f.glyph(Math.cos(angle)*r*travel,.14+(frost?0:travel*.5),Math.sin(angle)*r*travel,
                        frost?.3:.23,frost?.3:.52,frost?angle:0,-angle,frost?Math.PI/2:0);
            }
            break;
        }
        case "charge": {
            double r=Math.max(1,detail);
            // Detail is charge growth, not the explosion's damage radius: keep the sigil overhead.
            double orb=.8+detail*.15;
            f.glyph(0,2.45,0,orb,orb,-a*.3,0,0);
            f.glyph(0,2.45,0,orb,orb,a*.3,Math.PI/2,0);
            for(int i=0;i<8;i++) {
                double angle=i*Math.PI/4, travel=cycle(t*.025+i/8.0),distance=r*(.2+.65*(1-travel));
                f.arc(Math.cos(angle)*distance,.25+travel*.8,Math.sin(angle)*distance,.25,.7,angle,-angle,0);
            }
            break;
        }
        case "abyss": {
            double r=Math.max(.1,detail);
            f.floor("ring",0,.06,0,r*2,0);
            for(int i=0;i<6;i++) {
                double angle=i*Math.PI/3,drop=cycle(t*.022+i/6.0);
                f.glyph(Math.cos(angle)*r*.84,.48-drop*.4,Math.sin(angle)*r*.84,.36,.58,-angle,-angle,0);
            }
            break;
        }
        case "vortex": {
            double r=Math.max(.5,Math.min(6,detail));
            f.glyph(0,.65,0,.62,.62,a,0,0);
            f.glyph(0,.65,0,.62,.62,-a,Math.PI/2,0);
            f.floor("ring",0,.05,0,r*2,0);
            // Inward streams communicate pull direction, rather than an oversized decorative disc.
            for(int arm=0;arm<3;arm++)for(int i=0;i<3;i++) {
                double q=cycle(t*.035+i/3.0),angle=arm*Math.PI*2/3+q*1.8+a*.2,distance=r*(.12+.8*(1-q));
                f.arc(Math.cos(angle)*distance,.18+q*.47,Math.sin(angle)*distance,.24,.42,angle,-angle,Math.PI/2);
            }
            break;
        }
        case "projectile":
            f.glyph(0,0,0,.9,1.5,t*.18,0,Math.PI/2);
            f.glyph(0,0,0,.9,1.5,t*.18,Math.PI/2,0);
            for(int i=0;i<3;i++)f.arc(0,0,-.25-i*.22,.55-i*.12,.23-i*.04,a+i,0,0);
            break;
        case "flock": {
            boolean bees=key.endsWith("bees");int count=bees?3:2;
            for(int i=0;i<count;i++) {
                double angle=a+i*Math.PI*2/count, x=Math.cos(angle)*.8,z=Math.sin(angle)*.8;
                double y=(bees?1.3:2.5)+Math.sin(t*.18+i)*.14;
                f.glyph(x,y,z,bees?.52:.88,bees?.42:.62,Math.sin(t*.55+i)*.2,-angle,0);
                f.arc(x,y-.06,z,bees?.32:.65,.14,Math.sin(t*.55+i)*.2,-angle,Math.PI/2);
            }
            break;
        }
        case "pages":
            f.glyph(.88,.98+o*.25,.05,1.18,1.0,-.12,Math.sin(a)*.18,0);
            for(int i=0;i<4;i++) {
                double q=cycle(p*.75+i*.21);
                f.arc(.65+i*.2+q*.22,1.13+q*.94,-.28+Math.sin(i+a)*.26,.35,.46,-.5+q*1.0,i*.5+q,0);
            }
            break;
        case "curse":
            f.glyph(-.9,1.28,.05,.9,1.1,-.13,Math.sin(a)*.16,0);
            for(int i=0;i<4;i++) {
                double angle=i*Math.PI/2+a*.35,r=1.3-o*.62;
                f.arc(Math.cos(angle)*r,.55+i*.3,Math.sin(angle)*r,.8,.27,-angle,-angle,Math.PI/4);
            }
            break;
        case "heal":
            // Three strands travel feet → shoulders, never suggesting an area buff.
            for(int i=0;i<3;i++) {
                double q=cycle(p*.88+i/3.0),angle=i*2.094+q*1.8;
                f.arc(Math.cos(angle)*.68,.25+q*1.9,Math.sin(angle)*.68,.25,.85,-.4,-angle,.15);
                f.mote(Math.cos(angle)*.68,.65+q*1.9,Math.sin(angle)*.68,.18,.18,angle);
            }
            f.glyph(.83,1.22,.1,.63,.78,0,0,0);
            break;
        case "bloom":
            f.glyph(.52,.23+o*.46,0,.95,1.12,(1-o)*.28,0,0);
            for(int i=0;i<4;i++) {
                double angle=i*2.4+.4,q=clamp((p-.15)*1.18+i*.05);
                f.glyph(.5+Math.cos(angle)*(.18+q*.66),.3+o*.35+q*.55,Math.sin(angle)*(.18+q*.65),
                        .22+.08*(i%2),.32,angle+q*.8,angle,0);
            }
            break;
        case "leaves":
            for(int i=0;i<6;i++) {
                double q=cycle(p*.72+i/6.0),angle=i*2.399+q*2.1;
                f.glyph(Math.cos(angle)*(.42+q*.38),.15+q*1.94,Math.sin(angle)*(.42+q*.38),.32,.52,angle+a,angle,0);
            }
            break;
        case "roots":
            for(int i=0;i<5;i++) {
                double angle=i*Math.PI*2/5,r=.58-o*.18;
                f.glyph(Math.cos(angle)*r,.2+o*.22,Math.sin(angle)*r,.38,.85,-Math.sin(angle)*.4,-angle,0);
            }
            break;
        case "seed":
            f.glyph(.48,.14+o*.3,0,.62,.75,0,Math.sin(a)*.12,0);
            for(int i=0;i<3;i++)f.mote(.45+(i-1)*(.13+p*.18),.2+p*(.45+i*.14),-.12+i*.1,.14,.23,-.4+i*.4);
            break;
        case "wheat":
            for(int i=0;i<5;i++)f.glyph((i-2)*.23,.36+o*.32+(i%2)*.1,-.1+Math.abs(i-2)*.08,.28,.98,
                    (i-2)*-.09+Math.sin(a+i)*.05,0,0);
            break;
        case "fruit":
            f.glyph(.82,.76+o*.25,0,.92,1.0,-.08,Math.sin(a)*.12,0);
            for(int i=0;i<4;i++) {
                double q=clamp(p*1.4-i*.12);
                f.mote(.84+(i-1.5)*q*.34,1.03-q*q*.78,-.12+i*.08,.16,.21,a+i);
            }
            break;
        case "steam":
            f.glyph(.83,.6+o*.16,0,1.05,.92,Math.sin(a)*.03,0,0);
            for(int i=0;i<3;i++) {
                double q=cycle(p*.7+i/3.0);
                f.arc(.83+(i-1)*.18+Math.sin(q*5+i)*.06,1.05+q*.9,-.12+i*.09,.2+q*.14,.5,Math.sin(q*4)*.18,i*.6,0);
            }
            break;
        case "flame":
            for(int i=0;i<3;i++) {
                double q=cycle(p*.8+i*.29),x=(i-1)*.45;
                f.glyph(x,.22+q*.9,-.3,.3+q*.2,.9-q*.2,Math.sin(t*.3+i)*.08,0,0);
                f.mote(x+Math.sin(a+i)*.12,1.15+q*.5,-.3,.11,.23,a);
            }
            break;
        case "guard": case "wall": {
            int count=motion.equals("wall")?5:3;
            for(int i=0;i<count;i++) {
                double angle=(i-(count-1)/2.0)*.72,r=.9;
                f.glyph(Math.sin(angle)*r,.98,Math.cos(angle)*r,.62,count==5?1.5:1.28,0,-angle,0);
            }
            break;
        }
        case "bind":
            for(int i=0;i<2;i++) {
                double side=i==0?-1:1;
                f.glyph(side*(1.05-o*.45),.78,0,.52,.62,side*(.25-o*.2),0,0);
                f.arc(side*(.7-o*.24),.55,.08,.92,.38,side*.25,0,0);
            }
            break;
        case "knot":
            f.glyph(.66,.63,0,.76,.7,0,0,0);
            for(int side:new int[]{-1,1})f.arc(.66+side*(.35+(.25*(1-o))),.55,.02,.9,.22,side*.3,0,0);
            break;
        case "mirror":
            for(int i=0;i<3;i++) {
                double angle=i*2.094+a*.3;
                f.glyph(Math.cos(angle)*.94,.83+(i%2)*.35,Math.sin(angle)*.94,.48,1.04,Math.sin(a+i)*.08,-angle,0);
            }
            break;
        case "slash":
            f.glyph(1.2-o*.45,1.8-o*.63,.1,.72,1.45,-.8+o*1.25,0,0);
            for(int i=0;i<3;i++)f.arc(.88+i*.15-p*.4,1.28-i*.19,.13-i*.12,1.2-i*.2,.22,-.5+i*.12,0,0);
            break;
        case "punch":
            f.glyph(.78,1.16,.05+o*.58,1.08,1.02,-.12,0,0);
            for(int i=0;i<3;i++)f.arc(.78,1.16,.15+i*.24+o*.55,.3+o*.8-i*.14,.22+o*.45-i*.09,0,0,0);
            break;
        case "forge":
            f.glyph(.92,1.45-o*.57,0,.82,1.16,-.7+o*.9,0,0);
            for(int i=0;i<4;i++) {
                double q=clamp((p-.2)*1.4),angle=(i-1.5)*.55;
                f.mote(.84+Math.sin(angle)*q,.52+Math.cos(angle)*q*.63-q*q*.25,.04,.12,.3,angle);
            }
            break;
        case "lightning":
            for(int i=0;i<3;i++) {
                double growth=ease(clamp((t-i*1.6)/4));
                f.glyph((i-1)*.43,2.12-growth*.55,-.15+i*.1,.36,1.1+growth*.35,(i-1)*.13,0,0);
            }
            break;
        case "bow":
            f.glyph(.9,1.25,0,.9,1.27,-.15+o*.15,0,0);
            for(int i=0;i<3;i++)f.mote(.93,1.29,.3+o*.35+p*.7-i*.15,.11,.22,Math.PI/2);
            break;
        case "scope":
            f.glyph(.93,1.36,.16,.73,.73,0,0,0);
            for(int i=0;i<2;i++)f.arc(.93,1.36,.18+i*.08,.98-i*.16,.25,Math.PI/2*i,0,0);
            break;
        case "shot":
            f.glyph(.82,1.23,.05+o*.25+p*.6,.57,1.08,-Math.PI/2,0,0);
            for(int i=0;i<3;i++)f.mote(.82,1.23,.1+p*.6-i*.25,.15-i*.025,.32,a);
            break;
        case "hook":
            f.glyph(.8,1.13,.2+Math.sin(p*Math.PI)*.75,.66,1.1,-.18+p*.36,0,0);
            for(int i=0;i<3;i++)f.arc(.82,1.12,.1+i*.2+Math.sin(p*Math.PI)*.22,.24,.17,0,Math.PI/2,0);
            break;
        case "gust":
            for(int i=0;i<3;i++) {
                double x=-.85+p*1.6+i*.24;
                f.arc(x,.4+i*.3,-.2+i*.16,1.4-i*.22,.35,Math.sin(a+i)*.15,0,0);
            }
            f.glyph(.8+p*.2,.88,.05,.76,.72,Math.sin(a)*.1,0,0);
            break;
        case "exhaust":
            for(int i=0;i<3;i++)f.glyph((i-1)*.3,.37,-.5-i*.24-p*.28,.38,.62-i*.1,Math.PI/2,0,Math.PI/2);
            for(int i=0;i<3;i++)f.mote((i-1)*.2,.32,-.9-i*.25-p*.45,.13,.26,a);
            break;
        case "step":
            for(int i=0;i<3;i++)f.glyph((i%2==0?-.24:.24),.1,-.65+i*.45+p*.35,.38,.64,0,.1,Math.PI/2);
            break;
        case "rainbow":
            f.glyph(0,2.44,0,2.45,1.02,0,0,0);
            for(int i=0;i<4;i++)f.mote((i-1.5)*.46,2.42-Math.abs(i-1.5)*.22-p*.25,0,.12,.3,(i-1.5)*-.1);
            break;
        case "portal":
            for(int side:new int[]{-1,1})f.glyph(side*(.85+o*.25),.85,0,.58,1.66,side*.05,side*.28,0);
            for(int i=0;i<3;i++)f.mote(-.65+cycle(p*.9+i/3.0)*1.3,.7+i*.12,0,.13,.23,Math.PI/2);
            break;
        case "veil":
            for(int i=0;i<3;i++) {
                double side=i%2==0?1:-1;
                f.faded("glyph",Math.max(0,2-(int)(p*2)),side*(.58+p*.5+i*.12),.75+i*.38,.08-i*.18,
                        .44,.86,side*(.1+p*.45),side*p*.4,0);
            }
            break;
        case "dream":
            f.glyph(-.72,2.23+Math.sin(a)*.08,0,.72,.82,-.2,0,0);
            for(int i=0;i<3;i++)f.mote(.5+i*.22,1.55-cycle(p*.5+i*.3)*.82,-.18,.12,.12,a+i);
            break;
        case "mask":
            f.faded("glyph",f.fade,.79,1.36,0,.84,1.05,-.08,Math.sin(a)*.15,0);
            f.faded("glyph",Math.max(0,f.fade-1),-.66-p*.16,1.27,-.15,.68,.87,.12,-Math.sin(a)*.18,0);
            break;
        case "eye":
            f.glyph(.82,1.63,0,.94,.14+.51*(1-ease(clamp((p-.3)*1.4))),0,0,0);
            for(int i=0;i<2;i++)f.arc(.82,1.61+(i==0?-.14:.14),0,.8,.16,i==0?0:Math.PI,0,0);
            break;
        case "crown":
            f.glyph(0,2.5,0,1.2,.83,0,0,0);
            f.glyph(0,2.5,0,1.2,.83,0,Math.PI/2,0);
            if(key.equals("design/laurel")) {
                for(int i=0;i<3;i++) {
                    double size=i<detail?.16:.035;
                    f.mote((i-1)*.25,2.86,0,size,size,Math.PI/4);
                }
            }
            break;
        case "sun":
            f.glyph(.85,2.13,0,.88,.88,a*.15,0,0);
            for(int i=0;i<4;i++) {
                double angle=i*Math.PI/2+.3;
                f.arc(.85+Math.cos(angle)*.65,2.13+Math.sin(angle)*.65,0,.17,.41,angle-Math.PI/2,0,0);
            }
            break;
        case "orbit":
            f.glyph(0,2.58,0,.64,.64,0,0,0);
            for(int i=0;i<3;i++)f.faded("ring",Math.max(1,f.fade-1),0,2.58,0,1.22,1.22,0,a*(i%2==0?.4:-.3)+i,.6+i*.55);
            break;
        case "gears":
            for(int i=0;i<3;i++)f.glyph((i-1)*.43,2.4+(i%2)*.27,0,i==1?.73:.52,i==1?.73:.52,(i%2==0?a:-a)*.4,0,0);
            break;
        case "record":
            f.glyph(0,2.5,0,.93,.93,a,0,.3);
            for(int i=0;i<3;i++)f.mote(.55+i*.2,2.25+cycle(p*.8+i*.3)*.7,0,.18,.28,Math.sin(a+i)*.2);
            break;
        case "banner":
            f.glyph(.95,1.3,0,.75,1.66,Math.sin(a)*.04,Math.sin(a*.7)*.17,0);
            for(int i=0;i<3;i++)f.mote(.82+i*.22,1.88+Math.sin(a+i)*.12,-.05,.1,.22,-.4);
            break;
        case "seal":
            f.glyph(.82,1.62-o*.72,0,.82,.95,-.12+o*.12,0,0);
            for(int i=0;i<2;i++)f.arc(.82,.45,.08,1.15-i*.22,.25,0,i*Math.PI/2,Math.PI/2);
            break;
        case "cards":
            for(int i=0;i<5;i++)f.glyph(.83+(i-2)*.18*o,1.16+Math.abs(i-2)*.06,.08-i*.025,.36,.59,(i-2)*-.18*o,0,0);
            break;
        case "coins":
            for(int i=0;i<5;i++) {
                double q=cycle(p*.65+i*.18);
                f.glyph(.82+(i-2)*q*.2,.55+Math.sin(q*Math.PI)*1.02,.05-i*.04,.32,.32,0,t*.14+i,0);
            }
            break;
        case "shatter":
            for(int i=0;i<5;i++) {
                double angle=i*2.399;
                f.glyph(.72+Math.cos(angle)*o*.67,.92+Math.sin(angle)*o*.67,-.05+i*.08,.27,.5,angle+p,angle,0);
            }
            break;
        case "snow":
            for(int i=0;i<5;i++) {
                double q=cycle(p*.63+i*.19),angle=i*2.399;
                f.glyph(Math.cos(angle)*(.5+q*.2),2.12-q*1.75,Math.sin(angle)*.63,.32,.32,a+i,angle,0);
            }
            break;
        case "serpent":
            for(int i=0;i<3;i++) {
                double angle=a+i*2.094;
                f.glyph(Math.cos(angle)*.59,.36+i*.34,Math.sin(angle)*.59,.76,.56,.25,-angle,0);
            }
            break;
        case "ship":
            f.glyph(.75,.37,.3-p*.55,1.18,.88,Math.sin(a)*.07,0,0);
            for(int i=0;i<3;i++)f.arc(.73,.12,.35+i*.17-p*.55,.58+i*.15,.17,0,0,Math.PI/2);
            break;
        case "anchor":
            f.glyph(.65,1.27-o*.84,0,.84,1.05,-.12+o*.12,0,0);
            for(int i=0;i<3;i++)f.arc(.65,1.6-o*.6-i*.18,0,.19,.25,0,i*Math.PI/2,0);
            break;
        case "relic": {
            boolean scales=key.startsWith("anubis/");double y=scales?0:.5;
            double roll=scales?-Math.min(4,detail)*.08:Math.sin(a)*.08;
            f.glyph(0,y,0,.95,.95,roll,0,0);
            f.glyph(0,y,0,.95,.95,roll,Math.PI/2,0);
            if(!scales)for(int i=0;i<2;i++)f.mote(.18+i*.08,y+.52+Math.sin(t*.35+i)*.05,0,.09,.16,a);
            break;
        }
        default: throw new IllegalArgumentException("Unknown art motion: "+motion);
        }
        if(motion.equals("pages")||motion.equals("steam")||motion.equals("forge")) f.inFrontOfHands();
        // A faint profile face keeps flat illustrated props legible to players standing side-on.
        // Large formations and existing crossed faces already provide their own side silhouette.
        if(!motion.equals("phalanx")&&!motion.equals("wings")&&!motion.equals("projectile")
                &&!motion.equals("crown")&&!motion.equals("relic")&&!motion.equals("vortex")&&!motion.equals("charge"))
            f.profileFace();
        return f.parts;
    }

    private static double clamp(double value) {return Math.max(0,Math.min(1,value));}
    private static double ease(double value) {return 1-Math.pow(1-value,3);}
    private static double cycle(double value) {return value-Math.floor(value);}

    private static final class Frame {
        final List<ObjectModel.Part> parts=new ArrayList<ObjectModel.Part>();
        final String key;
        final double t,open,progress,scale;
        final int fade;
        Frame(String key,double phase,boolean shortAction,boolean still) {
            this.key=key;t=Math.max(0,phase);open=ease(clamp(t/6));
            progress=shortAction?clamp(t/18):cycle(t/40);
            double close=shortAction&&!still?clamp((18-t)/6):1;
            scale=shortAction?(.18+.82*open)*(.15+.85*close):1;
            fade=Math.min(3,(int)Math.ceil((shortAction?Math.max(.025,open*close):1)*3));
        }
        void glyph(double x,double y,double z,double sx,double sy,double roll,double turn,double pitch) {
            faded("glyph",fade,x,y,z,sx,sy,roll,turn,pitch);
        }
        void arc(double x,double y,double z,double sx,double sy,double roll,double turn,double pitch) {
            faded("arc",Math.max(0,fade-1),x,y,z,sx,sy,roll,turn,pitch);
        }
        void mote(double x,double y,double z,double sx,double sy,double roll) {
            faded("mote",fade,x,y,z,sx,sy,roll,0,0);
        }
        void floor(String layer,double x,double y,double z,double diameter,double yaw) {
            // Match the painted line, not the transparent PNG edge, to the gameplay radius.
            double size=layer.equals("ring")?diameter*.5/BOUNDARY_TEXTURE_RADIUS:diameter;
            faded(layer,fade,x,y,z,size,size,0,yaw,Math.PI/2);
        }
        void faded(String layer,int opacity,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch) {
            parts.add(new ObjectModel.Part("PAPER",true,x,y,z,sx*scale,sy*scale,1,roll,turn,pitch,
                    "art/"+key+"/"+layer+Math.min(fade,Math.max(0,opacity))));
        }
        void profileFace() {
            for(ObjectModel.Part p:parts)if(p.art.contains("/glyph")&&Math.abs(p.pitch)<.001) {
                // Append, rather than reallocating the scene; identity and entity count stay stable.
                String texture=p.art.substring(0,p.art.length()-1)+Math.max(0,fade-1);
                parts.add(new ObjectModel.Part(p.material,true,p.x,p.y,p.z,p.sx,p.sy,p.sz,p.roll,p.turn+Math.PI/2,p.pitch,texture));
                return;
            }
        }
        void inFrontOfHands() {
            // Hand props must sit inside the owner's forward view, not beside the first-person camera.
            for(int i=0;i<parts.size();i++) {
                ObjectModel.Part p=parts.get(i);
                parts.set(i,new ObjectModel.Part(p.material,p.item,p.x*.6,p.y,p.z+1.25,
                    p.sx,p.sy,p.sz,p.roll,p.turn,p.pitch,p.art));
            }
        }
    }
}
