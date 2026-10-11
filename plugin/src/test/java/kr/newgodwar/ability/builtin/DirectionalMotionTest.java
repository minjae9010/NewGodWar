package kr.newgodwar.ability.builtin;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.Test;
import kr.newgodwar.ability.feedback.ObjectModel;
import static org.junit.Assert.*;

public class DirectionalMotionTest {
    @Test public void horizontalActionsKeepTheirHeadingAtVerticalCameraAngles() {
        double[][] headings={{0,0,1},{90,-1,0},{180,0,-1},{-90,1,0},{45,-Math.sqrt(.5),Math.sqrt(.5)}};
        for(double[] heading:headings) for(float pitch:new float[]{-90,-89,-85,0,85,89,90}) {
            Location source=new Location(null,3,7,11,(float)heading[0],pitch);
            Vector direction=BaseAbility.horizontalDirection(source);
            assertEquals(heading[1],direction.getX(),.000001);
            assertEquals(0,direction.getY(),0);
            assertEquals(heading[2],direction.getZ(),.000001);
            assertEquals(1,direction.length(),.000001);
            assertEquals("Computing the heading must not turn the player's camera",pitch,source.getPitch(),0);
            assertEquals(3,source.getX(),0);assertEquals(7,source.getY(),0);assertEquals(11,source.getZ(),0);
        }
    }

    @Test public void fallbackSolarArrowTravelsForwardFromItsBowInsteadOfAcrossTheCastersFace() {
        ObjectModel bow=AbilityDesigns.SOLAR_ARROW.model(true);
        ObjectModel.Part drawn=bow.parts(4,1).get(8),released=bow.parts(11,1).get(8);
        assertEquals("Arrow stays at the bow hand",drawn.x,released.x,0);
        assertEquals(drawn.y,released.y,0);
        assertTrue("Arrow releases along the caster's forward Z axis",released.z>drawn.z+4);
        assertEquals("Shaft is parallel to forward Z",0,released.turn,0);
        assertEquals(0,released.roll,0);
        assertTrue(released.sz>released.sx*10);
        assertTrue("Bow and arrow stay beside the first-person view",released.x<0);
    }

    @Test public void trackingSpearAdvancesToTheMovingTargetWithItsTipFirst() {
        Location eye=new Location(null,0,2,0,85,70),target=new Location(null,0,5,12,-170,-80);
        Location current=OdinAbility.spearLaunchPoint(eye,target);
        for(int frame=1;frame<=6;frame++) {
            target.add(.2,.1,0);
            Location next=OdinAbility.spearPoint(current,target,1.0/(7-frame));
            Vector delta=next.toVector().subtract(current.toVector());
            assertTrue("Each frame advances toward the current target",next.toVector().distanceSquared(target.toVector())<current.toVector().distanceSquared(target.toVector()));
            assertEquals("Tip follows the actual 3D segment",1,delta.normalize().dot(next.getDirection()),.000001);
            current=next;
        }
        assertEquals(0,current.toVector().distanceSquared(target.toVector()),.000001);
        assertEquals("Player's camera is unchanged",70,eye.getPitch(),0);
    }

    @Test public void pointBlankProjectilesDoNotLaunchBeyondTheirTarget() {
        Location eye=new Location(null,0,2,0,0,0),target=eye.clone().add(0,0,.1);
        Location hand=BaseAbility.rightHandPosition(eye);
        for(Location launch:new Location[]{ThorAbility.hammerLaunchPoint(eye,target),OdinAbility.spearLaunchPoint(eye,target)}) {
            Vector remaining=target.toVector().subtract(launch.toVector()).normalize();
            assertEquals("Flight direction cannot reverse after spawn",1,launch.getDirection().dot(remaining),.000001);
            assertTrue(launch.toVector().distanceSquared(target.toVector())<hand.toVector().distanceSquared(target.toVector()));
        }
    }
}
