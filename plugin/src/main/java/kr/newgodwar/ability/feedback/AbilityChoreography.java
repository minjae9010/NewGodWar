package kr.newgodwar.ability.feedback;

/** Subject-first scenes: one readable action, subordinate light, then a directional release. */
final class AbilityChoreography {
    private AbilityChoreography() { }

    static boolean compose(ArtModels.Frame f, String motion) {
        String id=f.key.substring(f.key.lastIndexOf('/')+1);
        if(receivedStatus(f,id)) return true;
        if(id.equals("trident")) { trident(f); return true; }
        if(id.equals("serpent_feathers")) { featheredSerpent(f); return true; }
        if(id.equals("muscle")) { ready(f); return true; }
        if(id.equals("medicine")) { medicine(f); return true; }
        if(id.equals("wind_heal")) { featherHeal(f); return true; }
        if(id.equals("herbs")) { herbs(f); return true; }
        if(id.equals("ink_orchid")) { orchid(f); return true; }
        if(id.equals("voodoo_doll")) { voodoo(f); return true; }
        if(id.equals("sun_mirror")) { sunMirror(f); return true; }
        if(id.equals("solar_disc")) { solarDisc(f); return true; }
        switch(motion) {
            case "shot": shot(f); return true;
            case "ready": ready(f); return true;
            case "weaken": weaken(f); return true;
            case "receive-repair": repairReceived(f); return true;
            case "aim": aim(f); return true;
            case "impact": receivedImpact(f,false); return true;
            case "receive-slash": receivedImpact(f,true); return true;
            case "receive-heal": receivedHeal(f); return true;
            case "splash": splash(f); return true;
            case "pages": pages(f); return true;
            case "roots": roots(f); return true;
            case "lightning": lightning(f); return true;
            case "steam": vessel(f); return true;
            case "flame": flame(f); return true;
            case "bloom": case "fruit": case "leaves": case "seed": case "wheat":
                botanical(f,motion); return true;
            case "bow": bow(f); return true;
            default: return false;
        }
    }

    private static boolean receivedStatus(ArtModels.Frame f,String id) {
        switch(id) {
            case "status_item": repairReceived(f); return true;
            case "status_cleanse": receivedHeal(f); return true;
            case "status_poison": case "venom":
                for(int i=0;i<7;i++) {
                    double q=f.in(i*.4,10+i*.4),a=i*2.399+q*.6,r=.65-q*.32;
                    f.fx("spark",Math.sin(q*Math.PI)*f.out(),Math.cos(a)*r,.45+q*.9,Math.sin(a)*r,.17,.24,0,0,0,true);
                }
                f.emblem(f.in(2,5)*f.out(),0,2.05,0,.38,.38,Math.sin(f.t*.35)*.1,0,0,true);
                return true;
            case "status_slow": case "status_seal": case "counter_lock": case "heart_bind":
                double height=id.equals("status_slow")?.25:.85;
                for(int i=0;i<4;i++) {
                    double a=i*Math.PI/2,r=.68-f.in(0,6)*.26;
                    f.emblem(f.out(),Math.cos(a)*r,height,Math.sin(a)*r,.55,.30,0,-a+Math.PI/2,0,false);
                }
                return true;
            case "status_blind": case "shut_eye":
                f.emblem(f.out(),0,1.64,.36,.7,.3,0,0,0,false);
                for(int i=0;i<4;i++)f.fx("shard",f.out()*.45,(i-1.5)*.14,1.65+Math.sin(f.t*.4+i)*.07,.32,.18,.16,0,0,0,true);
                return true;
            case "status_frost":
                for(int i=0;i<5;i++) {
                    double a=i*2.399;
                    f.emblem(f.out()*f.in(i*.3,4+i*.3),Math.cos(a)*.42,.25+i*.27,Math.sin(a)*.42,.28,.28,f.t*.02,0,0,true);
                }
                return true;
            case "status_hunger":
                f.emblem(f.out(),0,.85,.4,.38,.38,Math.sin(f.t*.5)*.1,0,0,true); return true;
            case "status_music": case "status_sleep": case "dream":
                for(int i=0;i<3;i++)f.emblem(f.out()*f.in(i,3+i),(i-1)*.3,1.8+i*.16+f.in(0,14)*.35,0,.23,.3,.1*Math.sin(f.t*.3+i),0,0,true);
                return true;
            case "status_haste": case "one_punch_ready": case "green_ready":
                for(int side:new int[]{-1,1}) {
                    f.emblem(f.out(),side*.4,.95,.22,.32,.35,side*f.t*.05,0,0,false);
                    f.fx("spark",f.out()*.5,side*.4,.95+Math.sin(f.t*.6)*.06,.22,.12,.12,0,0,0,true);
                }
                return true;
            case "status_weakness": case "nature_recoil":
                for(int i=0;i<4;i++) {
                    double q=f.in(i*.5,13+i*.5),a=i*2.399;
                    f.emblem(f.out()*Math.sin(Math.PI*q),Math.cos(a)*.42,1.25-q*.65,Math.sin(a)*.42,.21,.27,q*.6,0,0,true);
                }
                return true;
            case "status_strength":
                for(int side:new int[]{-1,1})f.emblem(f.out(),side*.4,.95,.25,.32,.35,side*.2,0,0,false);
                return true;
            default:return false;
        }
    }

    private static void ready(ArtModels.Frame f) {
        for(int side:new int[]{-1,1}) {
            f.emblem(f.out(),side*.4,.95,.22,.32,.35,side*f.t*.05,0,0,false);
            f.fx("spark",f.out()*.5,side*.4,.95+Math.sin(f.t*.6)*.06,.22,.12,.12,0,0,0,true);
        }
    }

    private static void weaken(ArtModels.Frame f) {
        for(int i=0;i<4;i++) {
            double q=f.in(i*.5,13+i*.5),a=i*2.399;
            f.emblem(f.out()*Math.sin(Math.PI*q),Math.cos(a)*.42,1.25-q*.65,Math.sin(a)*.42,.21,.27,q*.6,0,0,true);
        }
    }

    private static void repairReceived(ArtModels.Frame f) {
        for(int i=0;i<5;i++)f.fx("spark",f.out()*f.decay(i*.8,3),-.4+Math.sin(i*2.4)*.16,
            .95+Math.cos(i*2.4)*.17,.32,.13,.2,i*.5,0,0,true);
    }

    private static void shot(ArtModels.Frame f) {
        // Only the release at the bow. The real projectile owns its path and confirmed contact.
        f.sequence("burst",0,-.48,1.25,.9,.48,.48,0,0,0,false);
        for(int i=0;i<3;i++) f.fx("spark",f.decay(0,2.5),-.48+(i-1)*.1,1.25,
            .9+f.in(0,5)*.45,.08,.2,0,0,0,false);
    }

    private static void aim(ArtModels.Frame f) {
        // Readiness is a held reticle beside the shooter's bow: no tracer, muzzle flash or distant hit.
        double settle=f.in(0,6);
        f.emblem(f.out(),-.65,1.28,.9,.65+(1-settle)*.3,.65+(1-settle)*.3,0,0,0,false);
        f.fx("spark",f.decay(6,2)*.4,-.65,1.28,.9,.13,.13,0,0,0,false);
    }

    private static void receivedImpact(ArtModels.Frame f,boolean slash) {
        // Local +Z points toward the incoming attack, independent of the victim's facing.
        f.sequence(slash?"slash":"burst",0,0,1.05,.38,slash?1.35:.65,slash?1.35:.65,slash?-.45:0,0,0,false);
        for(int i=0;i<6;i++) {
            double q=f.after(0,9),a=i*Math.PI/3;
            f.fx("spark",f.decay(0,2.8)*f.out(),Math.cos(a)*q*.52,1.05+Math.sin(a)*q*.5,
                .38-q*.16,.10,.21,a,0,0,false);
        }
    }

    private static void receivedHeal(ArtModels.Frame f) {
        for(int i=0;i<6;i++) {
            double q=f.in(i*.5,10+i*.5),a=i*Math.PI/3+q*.8;
            f.fx("spark",Math.sin(Math.PI*q)*f.out(),Math.cos(a)*.46,.15+q*1.8,Math.sin(a)*.46,.15,.23,0,0,0,true);
        }
        f.emblem(f.in(2,5)*f.out(),0,2.12+f.in(4,12)*.2,0,.42,.42,0,0,0,true);
    }

    private static void splash(ArtModels.Frame f) {
        for(int i=0;i<7;i++) {
            double q=f.after(0,12),a=i*2.399;
            f.emblem(f.decay(0,4)*f.out(),Math.cos(a)*q*.72,1.05+Math.sin(a)*q*.7-q*q*.6,
                .36-q*.18,.14,.25,a+q,0,0,false);
        }
    }

    private static void medicine(ArtModels.Frame f) {
        double grow=f.in(0,5), lift=f.in(4,13);
        // A single medical staff by the shoulder, with two ascending coils around the recipient.
        f.emblem(grow*f.out(),-.76,.6+grow*.6,0,.6,1.7*grow+.01,0,0,0,false);
        for(int i=0;i<8;i++) {
            double a=i*Math.PI/2+lift*2.4, y=.18+i*.17+lift*.62;
            f.fx("spark",f.in(i*.3,3+i*.3)*f.out(),Math.cos(a)*.55,y,Math.sin(a)*.55,
                .13,.21,-a,0,0,true);
        }
        f.hero(2.5,.62,0); f.flash(1.1,.85);
    }

    private static void featherHeal(ArtModels.Frame f) {
        for(int i=0;i<3;i++) {
            double q=f.in(i,10+i),a=i*2.094+q*.8;
            f.emblem(f.out()*f.in(i,i+2),Math.cos(a)*(.8-q*.22),.35+q*1.65,Math.sin(a)*.6,
                .32,.85,-.5+q*.9,0,0,false);
        }
        f.rise(4,.45,.15,1.8,f.t,"spark",.16);
    }

    private static void herbs(ArtModels.Frame f) {
        f.prop("bowl",f.out(),-.65,.72,.4,.75,.5,0,0,0,false);
        for(int i=0;i<3;i++) {
            double q=f.in(i,6+i),a=(i-1)*.7;
            f.emblem(f.out(),-.65+Math.sin(a)*.3,.8+q*.45,.4,.26,.65*q+.01,-a,0,0,false);
        }
        for(int i=0;i<5;i++) {
            double q=f.in(4+i*.7,12+i*.7),a=i*2.4;
            f.fx("spark",Math.sin(q*Math.PI)*f.out(),Math.cos(a)*(.7-q*.3),.8+q*1.25,
                Math.sin(a)*.5,.15,.23,a,0,0,true);
        }
    }

    private static void orchid(ArtModels.Frame f) {
        double draw=f.in(0,7);
        // Ink grows from the ground first; the pink blossom opens only once the stem is drawn.
        f.prop("stem",f.out(),-.65,.12+draw*.65,0,.46,draw*1.3+.01,-.15,0,0,false);
        f.emblem(f.in(6,10)*f.out(),-.77,1.48,0,.65*f.in(6,10)+.01,.72*f.in(6,10)+.01,0,0,0,false);
        for(int i=0;i<4;i++) {
            double q=f.in(8+i*.6,16),a=i*1.57;
            f.prop("petal",Math.sin(Math.PI*q),-.77+Math.cos(a)*q*.65,1.48+q*.5-q*q*.6,
                Math.sin(a)*q*.5,.20,.3,a+q*1.4,0,0,false);
        }
    }

    private static void pages(ArtModels.Frame f) {
        double open=f.in(0,5),close=f.in(12,18),spread=open*(1-close*.6);
        for(int side:new int[]{-1,1}) {
            // Separate hinged pages, anchored low by the caster's right hand.
            f.prop("page",f.out(),-.48+side*.25*spread,1.02,.95,.53,.72,
                side*(.32-.23*spread),side*(1-spread)*1.2,-.28,false);
        }
        for(int i=0;i<5;i++) {
            double q=f.in(3+i*.7,11+i*.7);
            f.prop("rune",Math.sin(Math.PI*q)*f.out(),-.48+(i-2)*.18,
                1.22+q*.8,.97+q*.28,.13,.20,Math.sin(q*3+i)*.15,0,0,false);
        }
        f.fx("spark",f.decay(5,4),-.48,1.1,1.0,.45,.45,0,0,0,false);
    }

    private static void roots(ArtModels.Frame f) {
        for(int i=0;i<4;i++) {
            double a=i*Math.PI/2+.3,grow=f.in(i*.5,6+i*.5),r=.62-.17*grow;
            f.emblem(f.out(),Math.cos(a)*r,grow*.47,Math.sin(a)*r,
                .4,.95*grow+.01,Math.sin(a)*.15,-a,0,false);
            f.fx("shard",f.decay(1+i*.5,3),Math.cos(a)*.65,.05,Math.sin(a)*.65,
                .20,.20,a,0,0,true);
        }
        f.fx("wave",f.decay(6,3),0,.08,0,1.4,1.4,0,0,Math.PI/2,false);
    }

    private static void lightning(ArtModels.Frame f) {
        // Jagged bolt segments descend in order; no cylindrical column covering the whole target.
        for(int i=0;i<5;i++) {
            double x=0,on=f.t>=1+i*.5&&f.t<11?(f.t%3<1?1:.58):0;
            f.emblem(on,x,5.8-i*1.1,0,.62,1.55,(i%2==0?.14:-.14),0,0,true);
        }
        f.sequence("burst",4,0,.18,0,1.8,1.8,0,0,0,true);
        for(int i=0;i<5;i++) {
            double a=i*Math.PI*2/5,q=f.after(4,11);
            f.fx("shard",f.decay(4,3),Math.cos(a)*q*1.2,.12+q*.25,Math.sin(a)*q*1.2,
                .16,.5,a,0,Math.PI/2,false);
        }
    }

    private static void vessel(ArtModels.Frame f) {
        double tilt=f.key.endsWith("wine")?f.in(3,8)*.35:Math.sin(f.t*.3)*.035;
        f.emblem(f.out(),-.65,.87,.7,.75*f.grow(),.8*f.grow(),tilt,0,0,false);
        for(int i=0;i<5;i++) {
            double q=f.in(i,12+i),s=.12+q*.15;
            f.fx("spark",Math.sin(Math.PI*q)*.55*f.out(),-.65+Math.sin(q*4+i)*.16,
                1.1+q*.75,.7,s,s*.8,q*1.2,0,0,true);
        }
        f.fx("wave",f.decay(4,3),-.65,1.0,.7,.45,.45,0,0,Math.PI/2,false);
    }

    private static void flame(ArtModels.Frame f) {
        for(int i=0;i<5;i++) {
            double a=i*Math.PI*2/5,q=f.in(i*.5,7+i*.5),fade=f.out();
            double flicker=1+.12*Math.sin(f.t*.8+i*2);
            f.emblem(q*fade,Math.cos(a)*.47,.25+q*.48,Math.sin(a)*.47,
                .36*flicker,(.65+q*.55)*flicker,Math.sin(f.t*.3+i)*.12,-a,0,false);
        }
        f.rise(5,.55,.6,2.0,f.t,"spark",.12);
        f.flash(.4,.75);
    }

    private static void botanical(ArtModels.Frame f,String motion) {
        boolean flower=motion.equals("bloom")||motion.equals("fruit");
        int count=flower?5:3;
        for(int i=0;i<count;i++) {
            double a=i*Math.PI*2/count,grow=f.in(i*.6,6+i*.6),r=.3+grow*.4;
            // Whole plants root beside the feet instead of duplicating a large emblem above the head.
            f.emblem(f.out(),Math.cos(a)*r,.12+grow*(flower?.36:.58),Math.sin(a)*r,
                (flower?.5:.32)*grow+.01,(flower?.65:1.0)*grow+.01,Math.sin(a)*.16,-a,0,false);
        }
        f.rise(5,.6,.3,1.5,f.t,"spark",.12);
        f.flash(.4,.65);
    }

    private static void trident(ArtModels.Frame f) {
        double thrust=f.in(2,6),recoil=f.in(9,16),z=.6+thrust*1.45-recoil*.6;
        f.emblem(f.out(),-.48,1.05,z,.58,1.9,-.6+thrust*.35,0,-1.1,false);
        for(int i=0;i<3;i++) {
            double q=f.after(4+i,12+i);
            f.fx("wave",f.decay(4+i,3),-.48,1.0,1.6+q*1.5,.35+q*1.5,.35+q*1.5,0,0,0,false);
        }
        f.burst(5,.6,"shard",.18);
    }

    private static void featheredSerpent(ArtModels.Frame f) {
        for(int i=0;i<10;i++) {
            double q=i/9.0,a=q*4.4+f.t*.10,r=.65+.2*q;
            f.emblem(f.in(i*.25,3+i*.25)*f.out(),Math.cos(a)*r,.15+q*1.7,
                Math.sin(a)*r,.25,.58,a*.35,0,0,false);
        }
        f.fx("wave",f.decay(4,4),0,.06,0,1+f.after(4,12)*2,1+f.after(4,12)*2,0,0,Math.PI/2,false);
    }

    private static void voodoo(ArtModels.Frame f) {
        f.emblem(f.out(),0,2.5,0,.65,.9,Math.sin(f.t*1.5)*.05*f.in(3,6),0,0,true);
        for(int i=0;i<3;i++) {
            double q=f.in(1+i,5+i),a=i*2.094;
            f.prop("pin",f.out(),Math.cos(a)*(1-q*.7),2.5+Math.sin(a)*(1-q*.7),0,
                .12,.55,a-Math.PI/2,0,0,false);
        }
        f.contract(1.6);
    }

    private static void sunMirror(ArtModels.Frame f) {
        f.emblem(f.out(),-.55,1.25,.9,1.15*f.grow(),1.15*f.grow(),0,-.3+f.in(2,7)*.6,0,false);
        f.sequence("burst",4,-.55,1.25,1.0,1.5,1.5,0,0,0,false);
        for(int i=0;i<4;i++) {
            double q=f.after(4+i*.4,13);
            f.fx("spark",f.decay(4+i*.4,3),-.55+(i-1.5)*q*.4,1.25,1+q*2,.12,.35,0,0,Math.PI/2,false);
        }
    }

    private static void solarDisc(ArtModels.Frame f) {
        f.emblem(f.out(),0,2.45+f.in(0,7)*.35,0,1.1*f.grow(),1.1*f.grow(),f.t*.025,0,0,true);
        for(int i=0;i<6;i++) {
            double a=i*Math.PI/3+f.t*.04;
            f.fx("shard",f.in(2,6)*f.out(),Math.cos(a)*.85,2.8+Math.sin(a)*.85,0,
                .15,.42,a-Math.PI/2,0,0,false);
        }
        f.flash(2.8,1.0);
    }

    private static void bow(ArtModels.Frame f) {
        double draw=f.in(0,4),release=f.in(4,6),travel=f.after(4,11);
        f.emblem(f.out(),-.48,1.2,1.05,.75,.95,-.08*draw,0,0,false);
        // A drawn string and arrow are separate props, with a visible hold before release.
        f.prop("string",f.out(),-.58-draw*.13*(1-release),1.2,1.03,.15+.22*draw*(1-release),.82,0,0,0,false);
        f.prop("arrow",f.t<13?f.out():0,-.48,1.2,1.05-draw*.20+travel*4.2,
            .16,.95,0,0,Math.PI/2,false);
        f.tracer(1.2,1.2+travel*4.2,f.decay(4,3)*.5);
        f.sequence("burst",4,-.48,1.2,1.3,.6,.6,0,0,0,false);
    }
}
