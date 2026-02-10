package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BarbarianTrackTest {

    @Test
    public void testInitialPosition() {
        BarbarianTrack track = new BarbarianTrack();
        assertEquals(0, track.getPosition());
        assertEquals(0, track.getAttackCount());
        assertEquals(BarbarianTrack.TRACK_LENGTH, track.getDistanceToAttack());
    }

    @Test
    public void testAdvanceNoAttack() {
        BarbarianTrack track = new BarbarianTrack();
        for (int i = 0; i < BarbarianTrack.TRACK_LENGTH - 1; i++) {
            assertFalse(track.advance());
            assertEquals(i + 1, track.getPosition());
        }
    }

    @Test
    public void testAttackTrigger() {
        BarbarianTrack track = new BarbarianTrack();
        // Advance to just before attack
        for (int i = 0; i < BarbarianTrack.TRACK_LENGTH - 1; i++) {
            track.advance();
        }
        // This advance should trigger an attack
        assertTrue(track.advance());
        assertEquals(1, track.getAttackCount());
        // Position resets to 0 after attack
        assertEquals(0, track.getPosition());
    }

    @Test
    public void testMultipleAttacks() {
        BarbarianTrack track = new BarbarianTrack();
        for (int attack = 1; attack <= 3; attack++) {
            for (int i = 0; i < BarbarianTrack.TRACK_LENGTH - 1; i++) {
                assertFalse(track.advance());
            }
            assertTrue(track.advance());
            assertEquals(attack, track.getAttackCount());
            assertEquals(0, track.getPosition());
        }
    }

    @Test
    public void testDistanceToAttack() {
        BarbarianTrack track = new BarbarianTrack();
        assertEquals(7, track.getDistanceToAttack());
        track.advance();
        assertEquals(6, track.getDistanceToAttack());
        track.advance();
        assertEquals(5, track.getDistanceToAttack());
    }

    @Test
    public void testReset() {
        BarbarianTrack track = new BarbarianTrack();
        track.advance();
        track.advance();
        assertEquals(2, track.getPosition());
        track.reset();
        assertEquals(0, track.getPosition());
    }
}
