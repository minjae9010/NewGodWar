package kr.newgodwar.ability.feedback;

import kr.newgodwar.ability.builtin.AbilityDesigns;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Resource-pack choreography. Every short action follows the same readable beat:
 * charge (0-4 ticks) → impact pop with flash and shockwave (4) → follow-through debris → dissolve.
 * Each motion then adds the movement that names the skill: a sweeping blade, a tracer, chains
 * tightening, a beam from the sky. Persistent scenes loop instead and mark their real radius.
 *
 * Billboarded parts sit on the anchor's vertical axis (or in rings around it) because a billboard
 * also turns its translation toward the viewer. Directional parts stay fixed to the anchor's yaw.
 * Original geometry remains the no-pack fallback.
 */
public final class ArtModels {
    private ArtModels() { }
    private static final Map<ObjectModel,ObjectModel> VARIANTS = new IdentityHashMap<ObjectModel,ObjectModel>();
    /** VfxPainter paints the ring's (and sigil's) outer core at radius 118 on a 256px canvas. */
    public static final double BOUNDARY_TEXTURE_RADIUS = 118.0 / 256.0;
    /** The scene's display budget; ObjectEffects still enforces its session and server limits. */
    public static final int MAX_PARTS = 16;
    private static final double HEAD = 2.55;
    /**
     * Held props sit at the main (right) hand, beside rather than in the centre of the first-person view.
     * A display's local +x is the anchor's left side, verified from behind in the 26.3 client.
     */
    private static final double HAND = -.42;
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
                        VARIANTS.put(effect.model(true), create(key,fields[3],fields[4],true,false));
                        VARIANTS.put(effect.model(false), create(key,fields[3],fields[4],true,true));
                    } else VARIANTS.put((ObjectModel)value,create(key,fields[3],fields[4],false,false));
                }
            } catch (Exception error) {throw new IllegalStateException("Invalid bundled effect art catalogue",error);}
        }
        return VARIANTS.get(source);
    }

    public static ObjectModel create(String key,String motion,String palette,boolean shortAction,boolean still) {
        if (motion.equals("wings")) return WingModels.create(palette.equals("fire"), true, still);
        return ObjectModel.animated((phase,detail)->{
            Frame f=new Frame(key,palette,still?8:phase,shortAction,still);
            if (!AbilityChoreography.compose(f, motion)) compose(f,motion,detail);
            if(f.parts.size()>MAX_PARTS)throw new IllegalStateException(key+" exceeds the display budget");
            return f.parts;
        });
    }

    private static void compose(Frame f,String motion,double detail) {
        double t=f.t;
        switch(motion) {
        // ---------------------------------------------------------------- persistent scenes
        case "phalanx":
            for(int i=-2;i<=2;i++) {
                double angle=i*Math.PI/5;
                f.emblem(1,Math.sin(angle)*4.5,1.15,Math.cos(angle)*4.5,1.5,2.1,0,-angle,0,false);
                f.fx("wave",.55+.35*Math.sin(t*.12+i),Math.sin(angle)*4.5,1.15,Math.cos(angle)*4.5-.05,2.6,2.6,0,-angle,0,false);
            }
            f.boundary(4.5,t*.01,.8);
            break;
        case "clock": {
            double r=Math.max(.5,detail);
            f.boundary(r,-t*.03,1);
            // A compact dial marks the centre; only the thin ring spans the real radius.
            double dial=Math.min(2.4,r*1.6);
            f.fx("sigil",.85,0,.07,0,dial,dial,0,t*.06,Math.PI/2,false);
            f.emblem(1,0,HEAD+Math.sin(t*.1)*.08,0,1.1,1.1,-t*.05,0,0,true);
            f.pulse(r,t,20);
            f.orbit(6,r*.92,.4,1.3,t*.03,"spark",.32);
            break;
        }
        case "fire-rune": case "frost-rune": {
            double r=Math.max(.5,Math.min(3,detail));
            boolean frost=motion.equals("frost-rune");
            f.boundary(r,frost?0:t*.04,1);
            f.emblem(1,0,.09,0,Math.min(2.6,r*1.25),Math.min(2.6,r*1.25),0,frost?-t*.02:-t*.06,Math.PI/2,false);
            f.emblem(.9,0,1.1+Math.sin(t*.12)*.12,0,.75,.75,0,0,0,true);
            f.pulse(r,t,24);
            if(frost)f.orbit(6,r*.8,.25,.9,t*.02,"shard",.3);
            else f.rise(6,r*.75,.3,2.2,t,"spark",.34);
            break;
        }
        case "charge": {
            // Detail is charge growth (2→4), not the explosion's damage radius: the circle hangs in the sky.
            double r=Math.max(1,detail);
            f.fx("sigil",1,0,3.4,0,r*1.6,r*1.6,0,t*.09,Math.PI/2,false);
            f.fx("sigil",.8,0,3.6,0,r*.9,r*.9,0,-t*.14,Math.PI/2,false);
            f.emblem(1,0,3.45,0,r*.8,r*.8,0,-t*.05,Math.PI/2,false);
            f.fx("beam",.75,0,1.7,0,.5+r*.2,3.4,0,0,0,true);
            double throb=1+.12*Math.sin(t*.6);
            f.fx("flash",.9,0,1.0,0,(.9+r*.35)*throb,(.9+r*.35)*throb,t*.05,0,0,true);
            f.converge(8,r*1.1,.3,1.2,t,"spark",.4);
            break;
        }
        case "abyss": {
            double r=Math.max(.1,detail);
            f.boundary(r,-t*.02,1);
            f.fx("wave",cycleAlpha(t,18),0,.06,0,r*2*(1-cycle(t/18.0))+.2,r*2*(1-cycle(t/18.0))+.2,0,0,Math.PI/2,false);
            f.emblem(1,0,1.4,0,1.2,1.2,t*.03,0,0,true);
            for(int i=0;i<6;i++) {
                double angle=i*Math.PI/3+t*.01,q=cycle(t/26.0+i/6.0);
                f.fx("shard",Math.sin(Math.PI*q),Math.cos(angle)*r*.85,1.6-q*1.6,Math.sin(angle)*r*.85,.32,.42,Math.PI,0,0,true);
            }
            break;
        }
        case "vortex": {
            double r=Math.max(.5,Math.min(6,detail));
            f.boundary(r,t*.05,1);
            f.emblem(1,0,.95,0,1.05,1.05,t*.12,0,0,true);
            f.fx("flash",.55+.3*Math.sin(t*.3),0,.95,0,1.8,1.8,t*.05,0,0,true);
            f.converge(9,r,.2,.95,t*1.4,"shard",.3);
            break;
        }
        case "projectile":
            f.emblem(1,0,0,0,1.0,1.6,t*.32,0,Math.PI/2,false);
            f.emblem(1,0,0,0,1.0,1.6,t*.32,Math.PI/2,0,false);
            f.fx("flash",.55+.25*Math.sin(t*.7),0,0,0,1.7,1.7,0,0,0,true);
            for(int i=0;i<3;i++)f.fx("spark",.8-i*.22,0,0,-.45-i*.35,.55-i*.12,.55-i*.12,t*.2+i,0,0,true);
            break;
        case "flock": {
            boolean bees=f.key.endsWith("bees");int count=bees?3:2;
            for(int i=0;i<count;i++) {
                double angle=t*.07+i*Math.PI*2/count,x=Math.cos(angle)*.85,z=Math.sin(angle)*.85;
                double y=(bees?1.3:2.5)+Math.sin(t*.22+i)*.15;
                f.emblem(1,x,y,z,bees?.6:.95,bees?.5:.7,Math.sin(t*.6+i)*.2,0,0,true);
                f.fx("shard",.55,Math.cos(angle-.5)*.85,y-.05,Math.sin(angle-.5)*.85,.2,.24,angle,0,0,true);
            }
            break;
        }
        case "relic": {
            boolean scales=f.key.startsWith("anubis/");double y=scales?0:.5;
            double roll=scales?-Math.min(4,detail)*.08:Math.sin(t*.065)*.08;
            f.emblem(1,0,y,0,1.0,1.0,roll,0,0,false);
            f.emblem(1,0,y,0,1.0,1.0,roll,Math.PI/2,0,false);
            f.fx("flash",.35+.2*Math.sin(t*.4),0,y,0,1.7,1.7,0,0,0,true);
            if(!scales)for(int i=0;i<2;i++)f.fx("spark",.6+.4*Math.sin(t*.9+i*2),.2+i*.08,y+.62,0,.32,.32,t*.3,0,0,true);
            break;
        }
        case "crown":
            if(!f.shortAction) {
                f.emblem(1,0,HEAD,0,1.15,.8,0,0,0,true);
                f.fx("flash",.3,0,HEAD,0,1.4,1.0,0,0,0,true);
                for(int i=0;i<3;i++) {
                    double size=i<detail?.42:.06;
                    f.fx("spark",i<detail?1:.2,(i-1)*.32,HEAD+.42,0,size,size,t*.05,0,0,true);
                }
                break;
            }
            f.cast(1.9,true,false);f.hero(HEAD,1.25,0);f.fall(6,1.1,"shard",.3);
            break;
        case "guard":
            if(!f.shortAction) {
                // Held low at the hand: the first-person view centre stays clear while blocking.
                f.emblem(1,HAND*.8,.95,.8,1.0,1.25,0,0,0,false);
                f.fx("wave",.55+.3*Math.sin(t*.25),0,.9,0,2.2,2.2,0,0,Math.PI/2,false);
                f.fx("flash",.3,HAND*.8,.95,.75,1.2,1.2,0,0,0,false);
                break;
            }
            // Plates close in at the body's sides and back, never across the first-person view;
            // the barrier is a waist-level ring around the whole body.
            for(int i=0;i<3;i++) {
                double angle=i==0?1.15:i==1?-1.15:Math.PI,r=.8+(1-f.in(0,4))*.9;
                f.emblem(f.out(),Math.sin(angle)*r,1.0,Math.cos(angle)*r,.8*f.pop(),1.3*f.pop(),0,-angle,0,false);
            }
            f.fx("wave",f.decay(4,4),0,.95,0,1.2+2.4*f.after(4,8),1.2+2.4*f.after(4,8),0,0,Math.PI/2,false);
            f.cast(1.7,false,true);f.hero(HEAD,1.0,0);f.rise(5,.75,.2,1.9,t,"spark",.3);
            break;
        // ---------------------------------------------------------------- short actions
        case "wall":
            for(int i=0;i<5;i++) {
                double angle=(i-2)*.5,drop=1-f.in(i*.6,3+i*.6);
                f.emblem(f.out(),Math.sin(angle)*1.35,.6+drop*1.8,Math.cos(angle)*1.35,.7,1.05,0,-angle,0,false);
            }
            f.fx("wave",f.decay(4,4),0,.7,0,1.6+2.6*f.after(4,9),1.6+2.6*f.after(4,9),0,0,Math.PI/2,false);
            f.wave(5.0);f.flash(HEAD,3.25);f.burst(4,1.2,"shard",.34);
            break;
        case "heal":
            f.cast(1.6,false,false);f.hero(HEAD,1.15,0);f.wave(3.0);f.beam(3.0,.9);
            f.rise(7,.6,.1,2.4,t,"shard",.34);f.flash(HEAD,2.00);
            break;
        case "bloom": case "leaves": case "seed": case "wheat": case "fruit":
            f.cast(1.7,false,false);f.hero(HEAD,1.2,0);f.wave(3.2);f.flash(HEAD,2.25);
            f.bloom(8,motion.equals("fruit")||motion.equals("wheat")?1.0:.8);
            break;
        case "roots": {
            f.cast(1.5,false,true);f.hero(HEAD,1.0,0);f.contract(3.4);
            for(int i=0;i<5;i++) {
                double angle=i*Math.PI*2/5,r=.95-f.in(0,6)*.35,grow=f.in(i*.5,4+i*.5);
                f.emblem(f.out(),Math.cos(angle)*r,.05+grow*.45,Math.sin(angle)*r,.55*grow+.01,1.0*grow+.01,-Math.sin(angle)*.35,-angle,0,false);
            }
            f.flash(.4,1.4);
            break;
        }
        case "serpent":
            f.hero(HEAD,1.05,0);f.contract(3.0);f.flash(HEAD,1.88);
            for(int i=0;i<3;i++) {
                double angle=t*.12+i*2.094,r=.95-f.in(0,6)*.35;
                f.emblem(f.out(),Math.cos(angle)*r,.35+i*.42,Math.sin(angle)*r,.85,.62,.25,-angle,0,false);
            }
            f.converge(5,1.4,.3,1.6,t,"shard",.28);
            break;
        case "bind": case "curse":
            f.cast(1.6,false,true);f.hero(HEAD,1.1,Math.sin(t*1.3)*.08*(1-f.in(4,10)));f.contract(3.6);
            for(int i=0;i<2;i++) {
                double side=i==0?-1:1,closing=f.in(0,5);
                f.emblem(f.out(),side*(1.5-closing*.9),1.05,0,.7,.75,side*(.35-closing*.3),0,0,true);
            }
            f.converge(6,1.6,.4,1.8,t,"spark",.3);f.flash(HEAD,2.12);
            break;
        case "knot":
            f.cast(1.6,false,false);f.hero(HEAD,1.2,0);f.wave(3.0);f.flash(HEAD,2.50);
            for(int side:new int[]{-1,1})f.emblem(f.out()*(1-f.in(10,16)),side*(.55-f.in(0,5)*.55),1.35,0,.9,.85,side*t*.05,0,0,true);
            f.rise(6,.6,.3,2.2,t,"spark",.28);
            break;
        case "mirror":
            f.emblem(f.out(),HAND,1.25,1.1,1.0*f.pop(),1.6*f.pop(),Math.sin(t*.4)*.1,0,0,false);
            f.emblem(f.out(),HAND,1.25,1.1,1.0*f.pop(),1.6*f.pop(),Math.sin(t*.4)*.1,Math.PI/2,0,false);
            f.fx("wave",f.decay(3,4),HAND,1.25,1.15,.6+1.8*f.after(3,9),.6+1.8*f.after(3,9),0,0,0,false);
            f.frontFlash(1.25,1.2,2.2);f.burst(6,1.25,"shard",.3);f.wave(3.6);
            break;
        case "slash": {
            f.weapon(.95,1.3,1.0,-.9+f.in(1,5)*1.9,1.3);
            // Two crossing blade sweeps and a horizontal cut; each sequence draws its own arc and trail.
            for(int i=0;i<2;i++)
                f.sequence("slash",2+i,(i==0?.15:-.15),1.25+i*.15,1.35+i*.1,2.8+i*.4,2.8+i*.4,i==0?-.5:Math.PI+.6,0,0,false);
            f.sequence("slash",4,0,1.05,.4,3.6,3.6,0,.4-f.in(4,10)*.8,Math.PI/2,false);
            f.frontFlash(1.25,1.7,2.0);f.burst(5,1.25,"spark",.3);
            break;
        }
        case "punch": {
            double thrust=f.in(1,4);
            f.weapon(.4+thrust*1.0,1.25,1.1,0,1.25);
            f.fx("wave",f.decay(4,4),HAND*.7,1.15,1.8,.6+2.4*f.after(4,9),.6+2.4*f.after(4,9),0,0,0,false);
            f.fx("wave",f.decay(5,5),HAND*.7,1.15,2.6,.4+1.8*f.after(5,10),.4+1.8*f.after(5,10),0,0,0,false);
            f.frontFlash(1.25,1.9,2.6);f.wave(4.2);f.burst(6,1.25,"shard",.32);
            break;
        }
        // ---------------------------------------------------------------- workshop
        case "forge": {
            // An anvil settles in front; the hammer strikes three times, each blow throwing sparks,
            // then the finished piece rises glowing from the anvil and turns.
            double wx=HAND*.6,wz=1.3;
            f.prop("anvil",f.out(),wx,.42,wz,.95*f.grow(),.8*f.grow(),0,0,0,false);
            f.prop("anvil",f.out(),wx,.42,wz,.95*f.grow(),.8*f.grow(),0,Math.PI/2,0,false);
            double lift=t<7?Math.abs(Math.sin(Math.PI*(t-1)/3)):0,rest=f.in(8,11);
            double hy=.95+.75*lift+rest*.2,swing=-.2-.9*lift;
            f.emblem(f.out()*(1-rest*.7),wx+.25,hy,wz,.75,.95,swing,0,0,false);
            f.emblem(f.out()*(1-rest*.7),wx+.25,hy,wz,.75,.95,swing,Math.PI/2,0,false);
            for(int blow=0;blow<3;blow++)f.sequence("burst",1+blow*3,wx,.85,wz-.05,.9,.9,0,0,0,false);
            double last=t>=7?7:t>=4?4:1,dt=t-last,q=Math.min(1,dt/3);
            for(int i=0;i<4;i++) {
                double side=(i-1.5)*.45;
                f.fx("spark",t<1?0:(1-q)*f.out(),wx+side*q*1.1,.9+q*.7-q*q*.45,wz-.1,.28,.28,t*.3+i,0,0,true);
            }
            double rise=f.in(8,14);
            f.fx("flash",rise*f.out()*.8,wx,.9+rise*1.1,wz+.04,1.3*rise+.01,1.3*rise+.01,t*.1,0,0,false);
            f.prop("product",f.in(8,10)*f.out(),wx,.9+rise*1.1,wz,.7*rise+.01,.7*rise+.01,0,t*.25,0,false);
            f.prop("product",f.in(8,10)*f.out(),wx,.9+rise*1.1,wz,.7*rise+.01,.7*rise+.01,0,t*.25+Math.PI/2,0,false);
            break;
        }
        case "quench": {
            // The glowing blade plunges into water; the surface rings out and steam boils up.
            double dip=f.in(1,5),wx=HAND*.6;
            f.emblem(f.out(),wx,1.35-dip*.55,1.25,.9,1.1,-.3,0,0,false);
            f.emblem(f.out(),wx,1.35-dip*.55,1.25,.9,1.1,-.3,Math.PI/2,0,false);
            f.fx("wave",f.decay(4,5),wx,.08,1.25,.6+2.2*f.after(4,12),.6+2.2*f.after(4,12),0,0,Math.PI/2,false);
            f.sequence("burst",4,wx,.8,1.25,1.4,1.4,0,0,0,false);
            for(int i=0;i<6;i++) {
                double q=cycle(t/10.0+i/6.0);
                f.fx("spark",Math.sin(Math.PI*q)*f.in(4,6)*f.out(),wx+Math.sin(i*2.1)*.35,.8+q*1.6,1.25+Math.cos(i*1.3)*.25,
                    .3+q*.25,.3+q*.25,t*.1,0,0,true);
            }
            break;
        }
        case "craft": {
            // A crafting grid lights cell by cell as materials fly in, fuses, and the product pops out over the head.
            double gx=HAND*.5,gy=1.3,gz=1.55;
            f.sequence("craft",0,gx,gy,gz,1.3,1.3,0,0,0,false);
            for(int i=0;i<6;i++) {
                double a=i*Math.PI/3+.3,q=f.in(i*.6,4+i*.6),r=(1-q)*1.4+.05;
                f.fx("shard",(1-f.in(7,9))*f.in(i*.6,i*.6+1),gx+Math.cos(a)*r,gy+Math.sin(a)*r*.8,gz-.05,.3,.36,a,0,0,false);
            }
            f.sequence("burst",8,gx,gy,gz-.08,1.6,1.6,0,0,0,false);
            double pop=f.after(8,15),px=gx*(1-pop),py=gy+(HEAD-gy)*pop+Math.sin(Math.PI*pop)*.4,pz=gz*(1-pop);
            double shown=t>=8?f.out():0;
            f.prop("product",shown,px,py,pz,.9,.9,0,t*.2,0,false);
            f.prop("product",shown,px,py,pz,.9,.9,0,t*.2+Math.PI/2,0,false);
            for(int i=0;i<3;i++) {
                double a=t*.4+i*2.09;
                f.fx("spark",shown,px+Math.cos(a)*.45,py+Math.sin(a)*.3,pz,.26,.26,a,0,0,true);
            }
            break;
        }
        case "cook": {
            // Ingredients drop into a pan, it sizzles, and the finished dish rises out of the steam.
            double px=HAND*.55,pz=1.2;
            f.prop("pan",f.out(),px,.95,pz,1.1*f.grow(),1.1*f.grow(),0,0,-1.0,false);
            for(int i=0;i<4;i++) {
                double q=f.in(i*1.2,3+i*1.2);
                f.fx("shard",f.in(i*1.2,i*1.2+.5)*(1-f.in(6+i*.3,8+i*.3))*f.out(),px+(i-1.5)*.2,2.3-q*1.3,pz,.3,.34,t*.3+i,0,0,true);
            }
            for(int i=0;i<4;i++) {
                double q=cycle(t/6.0+i/4.0);
                f.fx("spark",Math.sin(Math.PI*q)*f.in(4,6)*f.out(),px+Math.sin(i*2.3)*.35,1.0+Math.sin(Math.PI*q)*.45,pz+Math.cos(i*1.7)*.2,
                    .22,.22,t*.4,0,0,true);
            }
            f.sequence("burst",9,px,1.2,pz,1.3,1.3,0,0,0,false);
            double up=f.after(9,16),dish=t>=9?f.out():0;
            f.emblem(dish,px*(1-up),1.2+up*(HEAD-1.2),pz*(1-up),1.0,1.0,0,0,0,false);
            f.emblem(dish,px*(1-up),1.2+up*(HEAD-1.2),pz*(1-up),1.0,1.0,0,Math.PI/2,0,false);
            break;
        }
        // ---------------------------------------------------------------- Jang Yeong-sil's workshop
        case "assemble1": case "assemble2": case "assemble3": {
            // Gear parts fly in and lock together; three pips count the parts made so far (n/3).
            // The third part completes the set and an iron pickaxe springs out of the assembly.
            int made=motion.charAt(8)-'0';
            double ax=HAND*.5,ay=1.35,az=1.45;
            for(int i=0;i<3;i++) {
                boolean have=i<made;
                double a=i*2.094+.5,q=f.in(i*.8,4+i*.8),r=have?(1-q)*1.3:0;
                double alpha=have?f.out()*f.in(i*.8,i*.8+1):.22*f.out();
                f.prop("part",alpha,ax+Math.cos(a)*(.32+r),ay+Math.sin(a)*(.32+r)*.8,az,.5,.5,have?(i%2==0?1:-1)*t*.25:0,0,0,false);
            }
            for(int i=0;i<3;i++)
                f.fx("spark",i<made?f.in(4+i,6+i)*f.out():.15*f.out(),ax+(i-1)*.32,ay-.68,az,i<made?.32:.16,i<made?.32:.16,t*.1,0,0,false);
            f.sequence("burst",5,ax,ay,az-.05,1.3,1.3,0,0,0,false);
            if(made==3) {
                // The pickaxe appears once the fusing burst has opened, arcs over the head and lands with a flash.
                double pop=f.after(8,13),px=ax*(1-pop),py=ay+(HEAD-ay)*pop+Math.sin(Math.PI*pop)*.4,pz=az*(1-pop);
                double shown=t>=8?f.out():0;
                f.prop("product",shown,px,py,pz,1.25,1.25,0,t*.2,0,false);
                f.prop("product",shown,px,py,pz,1.25,1.25,0,t*.2+Math.PI/2,0,false);
                f.sequence("burst",12,0,HEAD,0,2.4,2.4,0,0,0,true);
            }
            break;
        }
        case "device": {
            // The armillary device unfolds overhead and its signal travels out to the real 10-block radius.
            f.boundary(10,t*.02,f.in(2,6)*f.out());
            double q=f.after(3,15),d=20*(.08+.92*q);
            f.fx("wave",t<3?0:(1-q)*f.out(),0,.07,0,d,d,0,0,Math.PI/2,false);
            f.cast(2.4,true,false);f.hero(HEAD,1.45,t*.03);f.flash(HEAD,2.4);f.beam(3.4,1.0);
            for(int i=0;i<4;i++) {
                double a=t*.08+i*Math.PI/2;
                f.prop("part",f.in(1,5)*f.out(),Math.cos(a)*1.15,HEAD+Math.sin(a*2)*.25,Math.sin(a)*1.15,.45,.45,t*.25*(i%2==0?1:-1),0,0,true);
            }
            break;
        }
        case "mark":
            f.emblem(f.out(),0,HEAD+.1,.08,.75*f.pop(),.75*f.pop(),t*.2,0,0,true);
            f.sequence("burst",4,0,HEAD+.1,0,1.4,1.4,0,0,0,true);
            f.orbit(3,.6,HEAD-.1,HEAD+.3,t*.1,"spark",.22);
            break;
        case "strike": {
            // A pickaxe swings down onto the target; ore chips fly and a slowing grip closes in.
            double swing=f.in(0,4);
            f.emblem(f.out(),HAND*.6,1.9-swing*.8,.9,.9,.9,1.2-swing*2.0,0,0,false);
            f.emblem(f.out(),HAND*.6,1.9-swing*.8,.9,.9,.9,1.2-swing*2.0,Math.PI/2,0,false);
            f.frontFlash(1.0,1.2,1.4);f.burst(6,1.0,"shard",.3);f.contract(2.4);
            break;
        }
        // ---------------------------------------------------------------- area scenes with a real radius
        case "cage": {
            // Ice shards rise along four meridians and close into a dome at the prison's real radius.
            double r=Math.max(1,Math.min(6,detail)),form=clamp(t/10);
            f.boundary(r,t*.01,1);
            for(int rib=0;rib<4;rib++)for(int i=1;i<=2;i++) {
                double e=i*.62*form,a=rib*Math.PI/2+.4;
                f.fx("shard",t<1?0:1,Math.cos(a)*Math.cos(e)*r,.2+Math.sin(e)*r,Math.sin(a)*Math.cos(e)*r,.7,.9,0,0,0,true);
            }
            f.emblem(1,0,1.4,0,1.2,1.2,t*.03,0,0,true);
            f.fx("flash",.4+.2*Math.sin(t*.3),0,1.4,0,2.0,2.0,t*.05,0,0,true);
            f.pulse(r,t,20);
            break;
        }
        case "melody": {
            // Notes circle the flute player at the song's current radius, which widens with every beat.
            double r=Math.max(1,Math.min(8,detail));
            for(int i=0;i<6;i++) {
                double a=i*Math.PI/3+t*.05;
                f.emblem(1,Math.cos(a)*r,1.5+Math.sin(t*.2+i)*.3,Math.sin(a)*r,.6,.6,Math.sin(t*.3+i)*.2,0,0,true);
            }
            f.boundary(r,t*.02,.7);f.pulse(r,t,20);
            break;
        }
        case "huntmark": {
            // A silver crescent over the prey; one star lights per hunting mark (up to three).
            f.emblem(1,0,2.35,.08,.95,.95,Math.sin(t*.1)*.08,0,0,true);
            f.fx("flash",.3+.15*Math.sin(t*.3),0,2.35,0,1.4,1.4,0,0,0,true);
            for(int i=0;i<3;i++) {
                double size=i<detail?.42:.1;
                f.fx("spark",i<detail?1:.25,(i-1)*.34,2.95,0,size,size,t*.1+i,0,0,true);
            }
            break;
        }
        case "harvest": {
            // Wheat ripens around the real 5-block healing radius, a little taller with each of the three stages.
            double stage=Math.max(0,Math.min(3,detail)),height=.7+stage*.45;
            f.boundary(5,t*.01,.85);
            for(int i=0;i<8;i++) {
                double a=i*Math.PI/4;
                f.emblem(1,Math.cos(a)*3.6,height/2+Math.sin(t*.12+i)*.05,Math.sin(a)*3.6,.8+stage*.15,height*1.3,Math.sin(t*.1+i)*.08,0,0,true);
            }
            f.rise(5,1.6,.3,2.6,t,"shard",.3);
            f.pulse(5,t,40);
            break;
        }
        case "levitate": {
            // A spell circle under the target and a spiral of light lifting them.
            f.fx("sigil",1,0,.05,0,1.8,1.8,0,t*.12,Math.PI/2,false);
            f.emblem(.9,0,2.6,.08,.8,.8,Math.sin(t*.2)*.15,0,0,true);
            for(int i=0;i<6;i++) {
                double q=cycle(t/14.0+i/6.0),a=i*Math.PI/3+t*.18;
                f.fx("shard",Math.sin(Math.PI*q),Math.cos(a)*.75,.1+q*2.4,Math.sin(a)*.75,.28,.32,a,0,0,true);
            }
            break;
        }
        case "dome": {
            // Protego: shields stand around the real radius, facing outward, under a shimmering dome.
            double r=Math.max(1,Math.min(6,detail));
            f.boundary(r,t*.02,1);
            for(int i=0;i<6;i++) {
                double a=i*Math.PI/3+.2;
                f.emblem(.85,Math.sin(a)*r,1.0,Math.cos(a)*r,.9,1.2,0,-a,0,false);
            }
            f.fx("flash",.35+.15*Math.sin(t*.25),0,r*.8+.6,0,r*1.2,r*.6,0,0,0,true);
            f.pulse(r,t,24);
            break;
        }
        case "echo": {
            // The recorded strike replays: two crossing blade sweeps, a ground cut and a fading afterimage.
            double r=Math.max(1,Math.min(6,detail));
            f.sequence("slash",0,0,1.1,0,2.4,2.4,-.5,0,0,true);
            f.sequence("slash",1,0,1.1,0,2.4,2.4,Math.PI+.6,0,0,true);
            f.sequence("slash",2,0,.4,0,r*2.2,r*2.2,0,t*.1,Math.PI/2,false);
            f.boundary(r,t*.03,.8*(1-clamp(t/14)));
            f.emblem(.7*(1-clamp(t/12)),0,1.1,.08,1.0,1.0,0,0,0,true);
            break;
        }
        // ---------------------------------------------------------------- market
        case "stock": {
            // A live chart climbs; at its peak the coin above the head splits into two and gold rains down.
            f.sequence("chart",0,HAND*.45,1.55,1.6,1.5,1.5,0,0,0,false);
            double split=f.after(7,12);
            for(int side:new int[]{-1,1})
                f.emblem(t>=5?f.out():0,side*split*.55,HEAD,.08,.85*f.pop(),.85*f.pop(),side*split*.2,0,0,true);
            f.sequence("burst",7,0,HEAD,0,2.4,2.4,0,0,0,true);
            f.fall(6,1.0,"shard",.3);
            f.rise(3,.5,1.4,2.8,t*1.6,"spark",.3);
            break;
        }
        case "crash": {
            // The chart collapses; the coin trembles, shatters, and its pieces drop away.
            f.sequence("crash",0,HAND*.45,1.55,1.6,1.5,1.5,0,0,0,false);
            double shake=t<8?Math.sin(t*2.6)*.1*f.in(2,6):0;
            f.emblem(t<8?f.out():0,shake,HEAD,.08,.9,.9,shake,0,0,true);
            f.sequence("burst",8,0,HEAD,0,1.8,1.8,0,0,0,true);
            for(int i=0;i<6;i++) {
                double a=i*1.047+.4,q=f.after(8,16);
                f.fx("shard",t<8?0:1-q,Math.cos(a)*(.15+q*.6),HEAD-q*q*2.0+q*.3,Math.sin(a)*(.15+q*.6),.32,.4,a+q*5,0,0,true);
            }
            f.contract(3.0);
            break;
        }
        case "goldrain":
            f.cast(2.0,true,false);f.hero(HEAD,1.2,0);f.flash(HEAD,2.4);f.beam(3.2,1.0);
            for(int i=0;i<6;i++) {
                double q=cycle(t/14.0+i/6.0),a=i*2.399,r=.5+.6*(i%3)/2.0;
                f.prop("coin",Math.sin(Math.PI*q)*f.in(2,5)*f.out(),Math.cos(a)*r,3.4-q*3.2,Math.sin(a)*r,.4,.4,0,t*.3+i,0,true);
            }
            f.fall(4,1.0,"spark",.28);
            break;
        case "discount": {
            // The price tag swings in, coins slide into it, then the discount stamp lands.
            double swing=Math.sin(t*.7)*Math.exp(-t/6)*.6;
            f.emblem(f.out(),0,HEAD,.08,1.2*f.grow(),1.2*f.grow(),swing,0,0,true);
            for(int i=0;i<5;i++) {
                double a=i*1.256+.3,q=f.in(i*.6,5+i*.6),r=1.3*(1-q)+.1;
                f.prop("coin",(1-f.in(8,10))*f.in(i*.6,i*.6+1),Math.cos(a)*r,HEAD-.9+q*.9,Math.sin(a)*r,.38,.38,0,t*.3,0,true);
            }
            f.sequence("burst",8,0,HEAD,0,2.2,2.2,0,0,0,true);
            f.wave(3.0);f.cast(1.6,true,false);
            break;
        }
        // ---------------------------------------------------------------- explorers and examinees
        case "explore": {
            // The compass needle swings and settles, footprints appear behind the walker, radar pings spread.
            f.hero(HEAD,1.1,(1-f.in(0,10))*Math.sin(t*1.1)*.6);
            for(int i=0;i<4;i++) {
                double show=f.in(i*1.5,i*1.5+1.5)*(1-f.in(12,17));
                f.prop("foot",show,i%2==0?-.18:.18,.04,-.5-i*.55,.38,.5,0,0,Math.PI/2,false);
            }
            for(int k=0;k<2;k++) {
                double q=cycle((t+k*9)/18.0),d=.5+5*q;
                f.fx("wave",(1-q)*f.out(),0,.06,0,d,d,0,0,Math.PI/2,false);
            }
            f.rise(3,.6,.3,2.0,t,"spark",.25);
            break;
        }
        case "exam": {
            // The paper unrolls, then a question mark pops above it.
            double unroll=f.in(0,5),q=f.in(5,8);
            f.emblem(f.out(),0,HEAD,.08,1.1*f.grow(),1.3*unroll+.08,0,0,0,true);
            f.prop("question",t>=5?f.out():0,0,HEAD+1.0+Math.sin(t*.3)*.08,.12,.75*(q+.25*Math.sin(Math.PI*q))+.01,.75*(q+.25*Math.sin(Math.PI*q))+.01,0,0,0,true);
            f.sequence("burst",5,0,HEAD+1.0,0,1.8,1.8,0,0,0,true);
            f.orbit(4,.9,HEAD-.2,HEAD+.4,t*.08,"spark",.26);
            f.cast(1.5,false,false);
            break;
        }
        case "wrong": {
            // A red X slams down onto the paper and shakes.
            double slam=1-f.in(0,3),shake=t>=3&&t<9?Math.sin(t*3)*.08*(1-f.in(3,9)):0;
            f.emblem(f.out(),shake,HEAD+slam*1.6,.08,1.2*(1+slam*.6),1.2*(1+slam*.6),shake,0,0,true);
            f.sequence("burst",3,0,HEAD,0,2.0,2.0,0,0,0,true);
            f.contract(2.8);
            f.fall(4,.9,"shard",.28);
            break;
        }
        case "balance": {
            // The hunger scale wobbles and comes to rest level: hunger is held at exactly half.
            double tilt=Math.sin(t*.6)*Math.exp(-t/7)*.45;
            f.emblem(f.out(),0,HEAD+.55,.08,.8*f.grow(),.8*f.grow(),0,0,0,true);
            f.prop("scale",f.out(),0,HEAD-.25,.08,1.1*f.grow(),1.1*f.grow(),tilt,0,0,true);
            f.orbit(4,.75,HEAD-.4,HEAD+.2,t*.06,"shard",.24);
            break;
        }
        case "lightning":
            // A column strikes from the sky; it flickers instead of fading smoothly.
            f.fx("beam",(t>=2&&t<12?(t%4<2?1:.55):0),0,4.0,0,1.5,8.0,0,0,0,true);
            f.fx("beam",(t>=3&&t<9?.85:0),.25,4.0,-.15,.7,8.0,0,0,0,true);
            f.hero(HEAD+.6,1.3,0);f.flash(.6,3.4);f.wave(5.2);
            f.burst(6,.4,"shard",.42);f.cast(2.2,false,false);
            break;
        case "bow": case "shot": case "scope": {
            boolean scope=motion.equals("scope");
            f.weapon(scope?1.3:.9,1.35,scope?1.1-f.in(0,4)*.35:.95,scope?f.t*.08:0,scope?1.0:1.3);
            double flight=f.after(3,9);
            f.tracer(1.35,1.2+flight*6.5,f.decay(3,6));
            f.sequence("burst",3,HAND,1.35,1.4,1.5,1.5,0,0,0,false);
            f.sequence("burst",8,0,1.35,7.5,2.8,2.8,0,0,0,false);
            f.fx("wave",f.decay(3,3),0,1.35,1.5,.4+1.8*f.after(3,7),.4+1.8*f.after(3,7),0,0,0,false);
            f.fx("spark",f.decay(3,6),0,1.35,1.2+flight*6.5,.7,.7,t*.2,0,0,false);
            break;
        }
        case "hook": {
            double pull=f.in(4,10);
            f.weapon(.6,1.2,3.4-pull*2.3-(1-f.in(0,4))*.8,Math.sin(t*.5)*.2,1.15);
            f.tracer(1.2,3.4-pull*2.3,f.out()*.8);
            f.fx("wave",f.decay(3,5),0,.1,3.4,1+2.5*f.after(3,10),1+2.5*f.after(3,10),0,0,Math.PI/2,false);
            f.burst(6,1.0,"shard",.3);f.frontFlash(1.2,3.4-pull*2.3,1.6);
            break;
        }
        case "gust": case "step": case "exhaust": {
            boolean back=!motion.equals("gust");
            double dir=back?-1:1;
            for(int i=0;i<3;i++) {
                double q=f.after(1+i,9+i);
                f.sequence("slash",1+i,0,(back?.3:.6)+i*.38,dir*(.4+q*2.6),2.0-i*.3,1.4-i*.2,0,back?0:Math.PI,Math.PI/2,false);
            }
            for(int i=0;i<6;i++) {
                double q=cycle(t/12.0+i/6.0);
                f.fx("shard",Math.sin(Math.PI*q)*f.out(),Math.sin(i*2.4)*.5,.3+(i%3)*.45,dir*(.2+q*3.0),.26,.32,i,0,0,true);
            }
            f.wave(back?3.0:3.6);f.flash(back?.4:1.1,back?1.5:1.8);
            if(!back)f.weapon(.9,1.2,1.0,0,1.0);
            break;
        }
        case "flame":
            f.cast(1.7,false,false);f.hero(HEAD,1.15,0);f.wave(3.2);f.beam(3.4,1.2);
            f.rise(7,.55,.1,2.6,t*1.4,"shard",.38);f.flash(HEAD,2.50);
            break;
        case "snow":
            f.cast(1.8,false,false);f.hero(HEAD,1.2,t*.04);f.wave(3.6);f.flash(HEAD,2.50);
            f.fall(8,1.2,"shard",.3);
            break;
        case "shatter":
            f.weapon(.4+f.in(0,4)*.6,1.1,1.1,0,1.1);f.frontFlash(1.1,1.4,2.4);f.wave(3.4);
            for(int i=0;i<9;i++) {
                double angle=i*2.399,q=f.after(4,12);
                f.fx("shard",f.decay(4,6),Math.cos(angle)*q*1.6,1.1+Math.sin(angle)*q*1.3-q*q*.6,1.4,.34,.42,angle+q*3,0,0,true);
            }
            break;
        case "rainbow":
            f.cast(1.6,false,false);f.flash(HEAD,2.50);f.wave(3.6);
            f.emblem(f.out(),0,2.4,0,3.4*f.grow(),1.45*f.grow(),0,0,0,true);
            f.rise(8,1.4,.2,2.4,t,"spark",.3);
            break;
        case "portal": {
            double open=f.in(0,4),fold=f.in(10,17);
            double size=2.4*open*(1-fold)+.01;
            // The gate stands behind the traveller: visible to everyone else, never over the caster's own view.
            f.fx("sigil",f.out(),0,1.2,-.85,size,size,t*.2,0,0,false);
            f.fx("sigil",f.out()*.8,0,1.2,-.9,size*.6,size*.6,-t*.3,0,0,false);
            f.hero(HEAD,1.1,0);f.flash(HEAD,2.75);f.wave(3.2);
            f.converge(6,1.5,.4,1.9,t,"spark",.3);
            break;
        }
        case "veil":
            f.hero(HEAD,1.1,0);f.contract(3.4);f.flash(HEAD,2.00);
            for(int i=0;i<3;i++) {
                double side=i%2==0?1:-1,q=f.in(0,12);
                f.emblem(f.out()*(1-q*.6),side*(.6+q*.7+i*.12),.75+i*.38,.08-i*.18,.62,1.05,side*(.1+q*.45),side*q*.4,0,false);
            }
            f.converge(6,1.7,.3,2.0,t,"shard",.3);
            break;
        case "dream":
            f.hero(HEAD,1.15,Math.sin(t*.15)*.1);f.flash(HEAD,1.6);f.cast(1.4,false,true);
            for(int i=0;i<5;i++) {
                double q=cycle(t/22.0+i/5.0);
                f.fx("spark",Math.sin(Math.PI*q)*f.out(),.35+i*.12,1.4+q*1.5,-.1,.22+q*.25,.22+q*.25,t*.05,0,0,true);
            }
            f.contract(2.6);
            break;
        case "eye": {
            double shut=f.in(5,10);
            f.emblem(f.out(),0,HEAD,0,1.4*f.pop(),(1.1*(1-shut)+.08)*f.pop(),0,0,0,true);
            f.flash(HEAD,2.2);f.contract(3.0);f.cast(1.5,false,true);
            f.converge(6,1.4,1.2,2.2,t,"shard",.28);
            break;
        }
        case "mask":
            f.hero(HEAD,1.2,Math.sin(t*.3)*.12);f.flash(HEAD,2.50);f.wave(3.0);f.cast(1.5,false,false);
            for(int i=0;i<3;i++) {
                double angle=i*2.094+.5,q=f.after(5,13);
                f.fx("shard",f.decay(5,6),Math.cos(angle)*q*1.3,HEAD+Math.sin(angle)*q*.9,.1,.34,.4,angle,0,0,true);
            }
            f.rise(4,.6,.2,1.8,t,"spark",.26);
            break;
        case "pages":
            f.cast(1.7,false,false);f.hero(HEAD,1.3,0);f.wave(3.0);f.beam(3.2,1.0);f.flash(HEAD,2.38);
            f.rise(7,.6,.3,2.6,t,"shard",.3);
            break;
        case "steam":
            f.cast(1.6,false,false);f.hero(HEAD,1.2,0);f.wave(2.8);f.flash(HEAD,2.25);
            f.rise(7,.45,.6,2.6,t*.8,"spark",.34);
            break;
        case "sun":
            f.cast(2.0,true,false);f.hero(HEAD+.2,1.5,t*.03);f.beam(4.0,1.6);f.wave(4.4);f.flash(HEAD+.2,3.2);
            f.orbit(6,1.0,HEAD+.2,HEAD+.2,t*.06,"spark",.36);
            break;
        case "orbit": case "gears": case "record":
            f.cast(1.9,true,false);f.hero(HEAD,1.25,(motion.equals("orbit")?.03:.12)*t);f.wave(3.6);f.flash(HEAD,2.0);f.beam(3.2,.9);
            f.orbit(6,1.15,HEAD-.3,HEAD+.3,t*.07,motion.equals("record")?"shard":"spark",.3);
            break;
        case "banner": case "seal": case "anchor": {
            // The emblem drops and lands: impact belongs to the ground, not to the air.
            double drop=1-f.in(0,4);
            f.emblem(f.out(),0,1.3+drop*2.4,0,1.3*f.pop(),1.5*f.pop(),0,0,0,true);
            f.sequence("burst",4,0,.4,0,3.6,3.6,0,0,0,true);
            f.wave(5.4);f.cast(2.2,true,false);
            f.fx("wave",f.decay(6,4),0,.08,0,.4+3.2*f.after(6,13),.4+3.2*f.after(6,13),0,0,Math.PI/2,false);
            f.burst(6,.4,"shard",.36);
            break;
        }
        case "cards":
            f.cast(1.6,false,false);f.flash(HEAD,2.25);f.wave(3.0);
            for(int i=0;i<5;i++) {
                double fan=f.in(0,5);
                f.emblem(f.out(),(i-2)*.32*fan,1.4+Math.abs(i-2)*-.05,.9,.5,.82,(i-2)*-.22*fan,0,0,false);
            }
            f.hero(HEAD,.9,0);f.orbit(4,.9,1.4,1.6,t*.09,"spark",.26);
            break;
        case "coins":
            f.cast(1.7,true,false);f.hero(HEAD,1.2,0);f.flash(HEAD,2.0);f.wave(3.0);
            for(int i=0;i<6;i++) {
                double q=cycle(t/16.0+i/6.0),angle=i*1.047;
                f.emblem(Math.sin(Math.PI*q)*f.out(),Math.cos(angle)*.7,.3+Math.sin(q*Math.PI)*1.9,Math.sin(angle)*.7,.38,.38,0,t*.25+i,0,true);
            }
            break;
        case "ship":
            f.weapon(.4,.75,1.0-f.in(0,10)*.4,Math.sin(t*.3)*.06,1.5);
            f.fx("wave",f.decay(2,6),0,.08,1.0,1+3.4*f.after(2,12),1+3.4*f.after(2,12),0,0,Math.PI/2,false);
            f.wave(4.4);f.flash(.6,2.0);f.burst(7,.4,"shard",.3);f.hero(HEAD,.9,0);
            break;
        default: throw new IllegalArgumentException("Unknown art motion: "+motion);
        }
    }

    private static double clamp(double value) {return Math.max(0,Math.min(1,value));}
    private static double ease(double value) {return 1-Math.pow(1-value,3);}
    private static double smooth(double value) {double x=clamp(value);return x*x*(3-2*x);}
    private static double cycle(double value) {return value-Math.floor(value);}
    private static double cycleAlpha(double t,double period) {double q=cycle(t/period);return Math.sin(Math.PI*q);}

    static final class Frame {
        final List<ObjectModel.Part> parts=new ArrayList<ObjectModel.Part>();
        final String key,palette;
        final double t;
        final boolean shortAction,still;
        Frame(String key,String palette,double phase,boolean shortAction,boolean still) {
            this.key=key;this.palette=palette;this.shortAction=shortAction;this.still=still;
            t=Math.max(0,phase);
        }

        // ---- timing
        double in(double from,double to) {return smooth((t-from)/(to-from));}
        /** 0→1 after a moment, eased. */
        double after(double from,double to) {return ease(clamp((t-from)/(to-from)));}
        /** Light that ignites at a moment and dies away exponentially. */
        double decay(double from,double k) {return t<from?0:Math.exp(-(t-from)/k);}
        double out() {return shortAction&&!still?clamp((18-t)/5):1;}
        double grow() {return shortAction?.25+.75*in(0,4):1;}
        /** Overshoot at the impact beat, then settle. */
        double pop() {
            if(!shortAction)return 1;
            return t<4?.25+.9*ease(t/4):1.15-.15*ease(clamp((t-4)/4));
        }

        // ---- primitives
        void emblem(double alpha,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch,boolean billboard) {
            add("art/"+key+"/glyph",alpha,x,y,z,sx,sy,roll,turn,pitch,billboard);
        }
        /** A secondary prop painted for this ability: an anvil, the finished product, a price tag... */
        void prop(String role,double alpha,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch,boolean billboard) {
            add("art/"+key+"/"+role,alpha,x,y,z,sx,sy,roll,turn,pitch,billboard);
        }
        void fx(String layer,double alpha,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch,boolean billboard) {
            add("art/fx/"+palette+"/"+layer,alpha,x,y,z,sx,sy,roll,turn,pitch,billboard);
        }
        /**
         * A one-shot animation frame (no fade suffix). The server advances it every update (10 fps), so each cast
         * replays the sequence from its first frame; frames outside the sequence collapse to a speck.
         */
        void sequence(String layer,double start,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch,boolean billboard) {
            int index=(int)Math.floor((t-start)/2);
            boolean visible=t>=start&&index<=5;
            int frame=Math.max(0,Math.min(5,index));
            parts.add(new ObjectModel.Part("PAPER",true,x,y,z,visible?sx:.01,visible?sy:.01,1,roll,turn,pitch,
                "art/fx/"+palette+"/"+layer+frame,billboard));
        }
        private void add(String texture,double alpha,double x,double y,double z,double sx,double sy,double roll,double turn,double pitch,boolean billboard) {
            double a=Double.isFinite(alpha)?clamp(alpha):0;
            // Invisible moments keep their entity (stable topology) but collapse to a speck.
            boolean hidden=a<.04;
            int fade=a>.8?3:a>.5?2:a>.22?1:0;
            double size=hidden?.01:1;
            parts.add(new ObjectModel.Part("PAPER",true,x,y,z,Math.max(.01,sx*size),Math.max(.01,sy*size),1,roll,turn,pitch,
                texture+fade,billboard));
        }

        // ---- beats shared by the short actions
        /** The skill's emblem pops above the head, readable from every side. */
        void hero(double y,double size,double roll) {
            double s=size*pop();
            // A billboard's local +z faces the viewer: keep the emblem just in front of its own flash.
            double lift=shortAction?in(10,18)*.35:0;
            if(shortAction&&!still&&t<4) {
                // During the charge a pen of light traces the emblem's strokes; the finished emblem pops on impact.
                parts.add(new ObjectModel.Part("PAPER",true,0,y+lift,.08,s,s,1,roll,0,0,"art/"+key+"/draw"+(t<2?0:1),true));
                return;
            }
            emblem(out(),0,y+lift,.08,s,s,roll,0,0,true);
        }
        /** A weapon-shaped emblem held in front of the body, with a profile face for side viewers. */
        void weapon(double z,double y,double size,double roll,double height) {
            double s=size*pop();
            emblem(out(),HAND,y,z,s,s*height,roll,0,0,false);
            emblem(out(),HAND,y,z,s,s*height,roll,Math.PI/2,0,false);
        }
        /** A small personal casting circle; never larger than the body's own footprint area. */
        void cast(double diameter,boolean fast,boolean reverse) {
            double open=in(0,4),spin=(fast?.14:.07)*(reverse?-1:1);
            fx("sigil",open*out(),0,.04,0,diameter*(.4+.6*open),diameter*(.4+.6*open),0,t*spin,Math.PI/2,false);
        }
        void wave(double diameter) {
            double q=after(4,13),d=diameter*(.15+.85*q);
            fx("wave",t<4?0:Math.pow(1-q,1.2),0,.06,0,d,d,0,0,Math.PI/2,false);
        }
        /** Inward shockwave for binds and curses. */
        void contract(double diameter) {
            double q=after(1,9),d=diameter*(1-.8*q);
            fx("wave",Math.sin(Math.PI*clamp((t-1)/9)),0,.06,0,d,d,0,0,Math.PI/2,false);
        }
        /** The impact explosion: a six-frame burst whose shock ring tears outward on its own. */
        void flash(double y,double size) {
            sequence("burst",4,0,y,0,size*.85,size*.85,0,0,0,true);
        }
        /** An impact flash in front of the body: two crossed faces, visible head-on and from the side. */
        void frontFlash(double y,double z,double size) {
            // Beside and slightly below the eye line, so the impact never whites out the crosshair.
            double s=Math.min(1.5,size)*1.4;
            sequence("burst",4,HAND*.7,Math.min(y,1.2),Math.max(z,1.6),s,s,0,0,0,false);
            sequence("burst",4,HAND*.7,Math.min(y,1.2),Math.max(z,1.6),s*.85,s*.85,0,Math.PI/2,0,false);
        }
        void beam(double height,double width) {
            double rise=in(2,6),fade=1-in(8,16);
            // Tall enough to rise past the emblem above the head, so it is not hidden by the body.
            double h=(height+.8)*(.3+.7*rise);
            fx("beam",rise*fade*.55,0,h/2,0,width*.6*(1-.45*in(6,16)),h,0,0,0,true);
        }
        /** Arrow-like light streak along the facing direction. */
        void tracer(double y,double tip,double alpha) {
            double length=Math.max(.2,Math.min(4,tip-.6));
            fx("beam",alpha,HAND*.5,y,tip-length/2,.5,length,Math.PI/2,0,Math.PI/2,false);
            fx("beam",alpha,HAND*.5,y,tip-length/2,.5,length,0,0,Math.PI/2,false);
        }
        void rise(int count,double radius,double from,double to,double clock,String layer,double size) {
            for(int i=0;i<count;i++) {
                double q=cycle(clock/16.0+i/(double)count),angle=i*2.399+clock*.12;
                double r=radius+.25*q,a=Math.sin(Math.PI*q)*(shortAction?in(1,5)*out():1);
                fx(layer,a,Math.cos(angle)*r,from+q*(to-from),Math.sin(angle)*r,size*(1-q*.4),size*(1-q*.4),angle+q*2,0,0,true);
            }
        }
        void fall(int count,double radius,String layer,double size) {
            for(int i=0;i<count;i++) {
                double q=cycle(t/18.0+i/(double)count),angle=i*2.399;
                fx(layer,Math.sin(Math.PI*q)*(shortAction?in(2,6)*out():1),Math.cos(angle)*radius*(.4+.6*(i%3)/2.0),3.0-q*2.8,
                    Math.sin(angle)*radius*(.4+.6*(i%3)/2.0),size,size,t*.15+i,0,0,true);
            }
        }
        void orbit(int count,double radius,double low,double high,double angle,String layer,double size) {
            for(int i=0;i<count;i++) {
                double a=angle+i*Math.PI*2/count;
                fx(layer,(shortAction?in(0,4)*out():.75+.25*Math.sin(t*.2+i)),Math.cos(a)*radius,low+(high-low)*(.5+.5*Math.sin(a*2)),
                    Math.sin(a)*radius,size,size,a,0,0,true);
            }
        }
        void converge(int count,double radius,double low,double high,double clock,String layer,double size) {
            for(int i=0;i<count;i++) {
                double q=cycle(clock/14.0+i/(double)count),angle=i*2.399+q*1.6;
                double r=radius*(1-q)+.12;
                fx(layer,Math.sin(Math.PI*q)*(shortAction?out():1),Math.cos(angle)*r,low+(high-low)*(i%3)/2.0,Math.sin(angle)*r,
                    size*(.6+.4*(1-q)),size*(.6+.4*(1-q)),angle,0,0,true);
            }
        }
        /** Outward debris from the impact moment. */
        void burst(int count,double y,String layer,double size) {
            for(int i=0;i<count;i++) {
                double angle=i*Math.PI*2/count+.3,q=after(4,13);
                double r=.25+q*1.9;
                fx(layer,decay(4,5),Math.cos(angle)*r,y+q*(.5+(i%3)*.35)-q*q*.6,Math.sin(angle)*r,size,size,angle+q*4,0,0,true);
            }
        }
        /** Petals or grain spiral up and open outward. */
        void bloom(int count,double spread) {
            for(int i=0;i<count;i++) {
                double q=cycle(t/20.0+i/(double)count),angle=i*2.399+q*2.2;
                double r=(.35+q*.9)*spread;
                fx("shard",Math.sin(Math.PI*q)*in(1,5)*out(),Math.cos(angle)*r,.25+q*2.3,Math.sin(angle)*r,.34,.4,angle+q*3,0,0,true);
            }
        }
        void trail(int count,double x,double y,double z,String layer,double size) {
            for(int i=0;i<count;i++) {
                double q=cycle(t/10.0+i/(double)count);
                fx(layer,(1-q)*.8,x+Math.sin(i*2.1)*.5,y-q*.6,z-q*1.2,size,size,i+t*.2,0,0,true);
            }
        }
        /** The real radius of a gameplay area, matched to the painted ring core. */
        void boundary(double radius,double spin,double alpha) {
            double size=radius/BOUNDARY_TEXTURE_RADIUS;
            fx("ring",alpha,0,.05,0,size,size,0,spin,Math.PI/2,false);
        }
        /** A shockwave pulse travelling from the centre to the boundary, repeating. */
        void pulse(double radius,double clock,double period) {
            double q=cycle(clock/period),d=radius*2*(.1+.9*ease(q));
            fx("wave",Math.pow(1-q,1.3),0,.07,0,d,d,0,0,Math.PI/2,false);
        }
    }
}
