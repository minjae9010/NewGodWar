package kr.newgodwar.ability.builtin;

import org.bukkit.Location;
import org.junit.Test;

import static org.junit.Assert.*;

public class ThorAbilityTest {
    @Test public void flightFollowsItsThreeDimensionalPathAndIgnoresTheTargetsView() {
        Location origin = new Location(null, 0, 2, 0, 35, -60);
        Location target = new Location(null, 0, 8, 12, 170, 85);
        Location outbound = ThorAbility.hammerPoint(origin, target, 0.5);
        assertEquals(0, outbound.getX(), 0.00001);
        assertEquals(5, outbound.getY(), 0.00001);
        assertEquals(6, outbound.getZ(), 0.00001);
        assertEquals(0, outbound.getYaw(), 0.00001);
        assertEquals(1, outbound.getDirection().dot(target.toVector().subtract(origin.toVector()).normalize()), 0.00001);
        target.setYaw(-85); target.setPitch(-70);
        assertEquals(outbound, ThorAbility.hammerPoint(origin, target, 0.5));
        assertEquals(35, origin.getYaw(), 0.00001);
        assertEquals(-60, origin.getPitch(), 0.00001);
    }

    @Test public void returnReachesTheMovingHandWithoutLeavingTheCurrentSegment() {
        Location current = new Location(null, 0, 2, 20, 0, -20);
        org.bukkit.util.Vector thrownDirection = current.getDirection();
        for (int frame = 7; frame <= 11; frame++) {
            Location hand = new Location(null, (frame - 7) * 0.6, 1.3, 0, frame * 40, frame * 5);
            Location next = ThorAbility.hammerReturnPoint(current, hand, 1.0 / (12 - frame));
            org.bukkit.util.Vector catchPoint = hand.toVector().add(thrownDirection.clone().multiply(.65));
            assertTrue(next.getX() >= current.getX() && next.getX() <= hand.getX());
            assertTrue(next.getZ() >= catchPoint.getZ() - 0.000001 && next.getZ() <= current.getZ() + 0.000001);
            assertTrue(Double.isFinite(next.getX()) && Double.isFinite(next.getY()) && Double.isFinite(next.getZ()));
            assertEquals("Recall must preserve the thrown yaw",0,next.getYaw(),0);
            assertEquals("Recall must preserve the thrown pitch",-20,next.getPitch(),0);
            if (frame == 11) assertEquals(0, catchPoint.distanceSquared(next.toVector()), 0.000001);
            current = next;
        }
    }

    @Test public void recallLeadsWithTheGripAndStopsTheHeadAheadOfTheHand() {
        Location hand = new Location(null, 0, 1.3, 0, 135, 70);
        Location thrown = ThorAbility.hammerPoint(new Location(null, 0, 1.3, 0), new Location(null, 0, 1.3, 8), 1);
        Location returning = ThorAbility.hammerReturnPoint(thrown, hand, .5);
        assertEquals(thrown.getYaw(), returning.getYaw(),0);
        assertEquals(thrown.getPitch(), returning.getPitch(),0);
        assertEquals("The grip leads opposite the preserved head direction",-1,
            returning.toVector().subtract(thrown.toVector()).normalize().dot(returning.getDirection()),.000001);
        Location caught = ThorAbility.hammerReturnPoint(returning, hand, 1);
        assertEquals("The head stops in front of the hand",.65,caught.getZ()-hand.getZ(),.000001);
        assertEquals(hand.toVector(),caught.toVector().subtract(caught.getDirection().multiply(.65)));
    }

    @Test public void catchPositionRemainsBesideTheCameraEvenWhenLookingVertically() {
        Location eye = new Location(null, 4, 7, 12, 90, -90);
        Location hand = ThorAbility.handPosition(eye);
        assertEquals(3.65, hand.getX(), 0.00001);
        assertEquals(6.65, hand.getY(), 0.00001);
        assertEquals(11.55, hand.getZ(), 0.00001);
        eye.setPitch(90);
        assertEquals(hand.toVector(), ThorAbility.handPosition(eye).toVector());
        assertEquals(7, eye.getY(), 0.00001);
    }

    @Test public void verticalOrZeroLengthFlightDoesNotProduceAnInvalidRotation() {
        Location origin = new Location(null, 3, 4, 5, 35, -60);
        Location stationary = ThorAbility.hammerPoint(origin, origin, 0.5);
        assertEquals(origin.getDirection(), stationary.getDirection());
        Location upward = ThorAbility.hammerPoint(origin, origin.clone().add(0, 7, 0), 0.5);
        assertEquals(1, upward.getDirection().getY(), 0.00001);
        Location downward = ThorAbility.hammerPoint(origin, origin.clone().add(0, -7, 0), 0.5);
        assertEquals(-1, downward.getDirection().getY(), 0.00001);
    }
}
