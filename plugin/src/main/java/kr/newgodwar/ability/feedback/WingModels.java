package kr.newgodwar.ability.feedback;

import java.util.ArrayList;
import java.util.List;

/** A shared anatomical rig: shoulder, wrist, overlapping flight feathers and trailing coverts. */
public final class WingModels {
    private WingModels() { }

    public static ObjectModel create(boolean fire, boolean art, boolean still) {
        return ObjectModel.animated((phase, detail) -> pose(still ? 10 : phase, fire, art));
    }

    private static List<ObjectModel.Part> pose(double age, boolean fire, boolean art) {
        List<ObjectModel.Part> out = new ArrayList<ObjectModel.Part>();
        double open = smooth(age / 9);
        // Slow recovery, then a stronger downstroke. The wrist follows the shoulder, not the camera.
        double beat = age * Math.PI / (fire ? 22 : 18);
        double flap = .23 * Math.sin(beat) + .075 * Math.sin(beat * 2);
        double spread = .32 + .68 * open;
        double sweep = .18 + (1 - open) * .8 + .12 * Math.cos(beat);
        for (int side : new int[] {-1, 1}) {
            for (int i = 0; i < 6; i++) {
                double u = i / 5.0;
                double rootX = .29 + u * .99;
                double rootY = 1.48 + Math.sin(u * Math.PI * .72) * .43;
                double tipX = .62 + u * 1.96;
                double tipY = .63 + u * .73;
                double lag = .08 * Math.sin(beat - u * .7) * u;
                double[] root = point(side, rootX, rootY, spread, flap, sweep);
                double[] tip = point(side, tipX, tipY + lag, spread, flap, sweep);
                feather(out, root, tip, .36 + u * .08, fire, art, false);
            }
            // Broad overlapping shoulder plumage closes the gaps at the roots of the primaries.
            feather(out, point(side, .23, 1.47, spread, flap, sweep),
                point(side, 1.49, 1.81, spread, flap, sweep), .40, fire, art, false);
            // Phoenix streamers trail behind the hips; victory wings have short lower coverts.
            double sway = .10 * Math.sin(beat - .8);
            feather(out, new double[] {side * .27, 1.05, -.48},
                new double[] {side * (.56 + sway), fire ? -.42 : .55, fire ? -1.05 : -.57},
                fire ? .23 : .28, fire, art, true);
        }
        return out;
    }

    private static double[] point(int side, double x, double y, double spread, double flap, double sweep) {
        double reach = (x - .23) * spread;
        return new double[] {side * (.23 + reach * Math.cos(sweep)),
            y + reach * flap, -.42 - reach * Math.sin(sweep)};
    }

    private static void feather(List<ObjectModel.Part> out, double[] a, double[] b, double width,
                                boolean fire, boolean art, boolean tail) {
        double dx=b[0]-a[0], dy=b[1]-a[1], dz=b[2]-a[2];
        double planar=Math.hypot(dx,dy), length=Math.sqrt(planar*planar+dz*dz);
        // Rz(roll) Rx(pitch) maps local +Y exactly to the root-to-tip vector.
        double roll=Math.atan2(-dx,dy), pitch=Math.atan2(dz,planar);
        String material=fire?(tail?"RED_CONCRETE":"ORANGE_CONCRETE"):"QUARTZ_BLOCK";
        if(art) {
            out.add(new ObjectModel.Part("PAPER",true,(a[0]+b[0])/2,(a[1]+b[1])/2,(a[2]+b[2])/2,
                width*1.65,length*1.12,.025,roll,0,pitch,
                "art/shared/"+(fire?"fire_wings":"wings")+"/glyph3",false));
        } else {
            // Two tapered solids per feather also give legacy particle outlines a coherent silhouette.
            out.add(new ObjectModel.Part(material,false,a[0]+dx*.36,a[1]+dy*.36,a[2]+dz*.36,
                width*.70,length*.70,.045,roll,0,pitch,null));
            out.add(new ObjectModel.Part(fire?"YELLOW_CONCRETE":"WHITE_CONCRETE",false,
                a[0]+dx*.80,a[1]+dy*.80,a[2]+dz*.80,width*.29,length*.42,.035,roll,0,pitch,null));
        }
    }

    private static double smooth(double x) { x=Math.max(0,Math.min(1,x));return x*x*(3-2*x); }
}
