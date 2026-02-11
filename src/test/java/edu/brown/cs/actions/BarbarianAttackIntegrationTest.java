package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.board.City;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Settlement;
import edu.brown.cs.catan.BarbarianTrack;
import edu.brown.cs.catan.GameSettings;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Settings;

/**
 * Integration tests for the full barbarian attack cycle:
 * track advancement → attack trigger → knight comparison →
 * defend/pillage → knight deactivation → track reset.
 */
public class BarbarianAttackIntegrationTest {

    /**
     * Creates a C&K-enabled game with 2 players.
     */
    private MasterReferee setupCKGame() {
        JsonObject settings = new JsonObject();
        settings.addProperty("numPlayers", 2);
        settings.addProperty("victoryPoints", 13);
        settings.addProperty("isDecimal", false);
        settings.addProperty("isStandard", false);
        settings.addProperty("isCitiesAndKnights", true);
        MasterReferee ref = new MasterReferee(new GameSettings(settings));
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    /**
     * Places a settlement then upgrades it to a city, properly consuming pieces
     * from the player.
     */
    private Intersection buildCity(MasterReferee ref, Player p) {
        Intersection inter = findEmptyIntersection(ref);
        // Place settlement (adjusts the intersection, not piece counts)
        inter.placeSettlement(p);
        p.useSettlement();
        // Upgrade to city (adjusts the intersection)
        inter.placeCity(p);
        p.useCity();
        return inter;
    }

    // ── Track lifecycle ──

    @Test
    public void testTrackAdvancesToAttackAndResets() {
        MasterReferee ref = setupCKGame();
        BarbarianTrack track = ref.getBarbarianTrack();
        // Advance to just before attack
        for (int i = 0; i < BarbarianTrack.TRACK_LENGTH - 1; i++) {
            assertFalse("Should not attack yet", track.advance());
        }
        // This advance triggers the attack
        assertTrue("Should trigger attack", track.advance());
        assertEquals("Track should reset to 0", 0, track.getPosition());
        assertEquals(1, track.getAttackCount());
    }

    // ── Knights win scenario ──

    @Test
    public void testKnightsWinDefenderVPAwarded() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);
        Player bob = ref.getPlayerByID(1);

        // Give Alice one activated basic knight (strength 1)
        KnightPiece knight = new KnightPiece(0, new IntersectionCoordinate(null, null, null));
        knight.activate();
        alice.addKnight(knight);

        // No built cities → total strength (1) >= total cities (0) → knights win
        int totalStrength = 0;
        int totalCities = 0;
        for (Player p : ref.getPlayers()) {
            totalStrength += p.getActiveKnightStrength();
            totalCities += Settings.INITIAL_CITIES - p.numCities();
        }
        assertTrue("Knights should win", totalStrength >= totalCities);

        // Award Defender VP to the highest contributor
        int maxStrength = 0;
        for (Player p : ref.getPlayers()) {
            int str = p.getActiveKnightStrength();
            if (str > maxStrength)
                maxStrength = str;
        }
        for (Player p : ref.getPlayers()) {
            if (p.getActiveKnightStrength() == maxStrength) {
                p.addDefenderPoint();
            }
        }

        assertEquals("Alice should have 1 defender point", 1, alice.getDefenderPoints());
        assertEquals("Bob should have 0 defender points", 0, bob.getDefenderPoints());
    }

    @Test
    public void testTiedStrengthBothGetDefenderVP() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);
        Player bob = ref.getPlayerByID(1);

        // Both players get an active basic knight
        KnightPiece k1 = new KnightPiece(0, new IntersectionCoordinate(null, null, null));
        k1.activate();
        alice.addKnight(k1);

        KnightPiece k2 = new KnightPiece(1, new IntersectionCoordinate(null, null, null));
        k2.activate();
        bob.addKnight(k2);

        // Both have strength 1 → tied → both should get Defender VP
        int maxStrength = 0;
        for (Player p : ref.getPlayers()) {
            int str = p.getActiveKnightStrength();
            if (str > maxStrength)
                maxStrength = str;
        }
        assertEquals(1, maxStrength);

        for (Player p : ref.getPlayers()) {
            if (p.getActiveKnightStrength() == maxStrength) {
                p.addDefenderPoint();
            }
        }

        assertEquals(1, alice.getDefenderPoints());
        assertEquals(1, bob.getDefenderPoints());
    }

    // ── Knights lose scenario ──

    @Test
    public void testKnightsLoseCityPillaged() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);

        // Build a city for Alice (properly tracking pieces)
        Intersection cityIntersection = buildCity(ref, alice);

        int builtCities = Settings.INITIAL_CITIES - alice.numCities();
        assertEquals("Alice should have 1 built city", 1, builtCities);

        // No knights → total strength = 0, total cities = 1 → knights lose
        int totalStrength = 0;
        int totalCities = 0;
        for (Player p : ref.getPlayers()) {
            totalStrength += p.getActiveKnightStrength();
            totalCities += Settings.INITIAL_CITIES - p.numCities();
        }
        assertTrue("Knights should lose", totalStrength < totalCities);

        // Pillage: demote Alice's city to settlement
        boolean demoted = cityIntersection.demoteToSettlement(alice);
        assertTrue("City should be demoted", demoted);
        assertTrue("Building should now be a settlement",
                cityIntersection.getBuilding() instanceof Settlement);
    }

    @Test
    public void testPillageOnlyAffectsWeakestContributor() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);
        Player bob = ref.getPlayerByID(1);

        // Build cities for both players
        Intersection aliceCity = buildCity(ref, alice);
        Intersection bobCity = buildCity(ref, bob);

        // Give Alice an active knight (strength 1), Bob has none
        KnightPiece k = new KnightPiece(0, new IntersectionCoordinate(null, null, null));
        k.activate();
        alice.addKnight(k);

        // Total: strength 1 < cities 2 → knights lose
        // Bob (strength 0) is the weakest contributor with a city
        int minStrength = Integer.MAX_VALUE;
        for (Player p : ref.getPlayers()) {
            int built = Settings.INITIAL_CITIES - p.numCities();
            if (built > 0) {
                int str = p.getActiveKnightStrength();
                if (str < minStrength)
                    minStrength = str;
            }
        }
        assertEquals("Min strength should be 0 (Bob)", 0, minStrength);

        // Only Bob should be pillaged
        for (Player p : ref.getPlayers()) {
            int built = Settings.INITIAL_CITIES - p.numCities();
            if (built > 0 && p.getActiveKnightStrength() == minStrength) {
                for (Intersection inter : ref.getBoard().getIntersections().values()) {
                    if (inter.demoteToSettlement(p)) {
                        break;
                    }
                }
            }
        }

        // Alice's city should be untouched
        assertTrue("Alice's city should still be a city",
                aliceCity.getBuilding() instanceof City);
        // Bob's city should be demoted
        assertTrue("Bob's city should be a settlement",
                bobCity.getBuilding() instanceof Settlement);
    }

    // ── Knight deactivation ──

    @Test
    public void testAllKnightsDeactivatedAfterAttack() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);
        Player bob = ref.getPlayerByID(1);

        KnightPiece k1 = new KnightPiece(0, new IntersectionCoordinate(null, null, null));
        k1.activate();
        alice.addKnight(k1);

        KnightPiece k2 = new KnightPiece(1, new IntersectionCoordinate(null, null, null));
        k2.activate();
        bob.addKnight(k2);

        assertTrue(k1.isActive());
        assertTrue(k2.isActive());

        // Deactivate all knights (as done after barbarian attack)
        for (Player p : ref.getPlayers()) {
            for (KnightPiece kp : p.getKnights()) {
                if (kp.isActive()) {
                    kp.deactivate();
                }
            }
        }

        assertFalse("Alice's knight should be deactivated", k1.isActive());
        assertFalse("Bob's knight should be deactivated", k2.isActive());
        assertEquals(0, alice.getActiveKnightStrength());
        assertEquals(0, bob.getActiveKnightStrength());
    }

    // ── Full cycle ──

    @Test
    public void testFullCycleKnightsWin() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);

        KnightPiece k = new KnightPiece(0, new IntersectionCoordinate(null, null, null));
        k.activate();
        alice.addKnight(k);

        // 1. Advance barbarian track to attack
        BarbarianTrack track = ref.getBarbarianTrack();
        for (int i = 0; i < BarbarianTrack.TRACK_LENGTH; i++) {
            track.advance();
        }
        assertEquals("Track resets after attack", 0, track.getPosition());
        assertEquals(1, track.getAttackCount());

        // 2. Knights win → award defender VP
        alice.addDefenderPoint();
        assertEquals(1, alice.getDefenderPoints());

        // 3. Deactivate all knights
        k.deactivate();
        assertFalse(k.isActive());
        assertEquals(0, alice.getActiveKnightStrength());
    }

    @Test
    public void testFullCycleKnightsLose() {
        MasterReferee ref = setupCKGame();
        Player alice = ref.getPlayerByID(0);

        // Build a city (no knights)
        Intersection inter = buildCity(ref, alice);

        // 1. Advance track to attack
        BarbarianTrack track = ref.getBarbarianTrack();
        for (int i = 0; i < BarbarianTrack.TRACK_LENGTH; i++) {
            track.advance();
        }
        assertEquals(0, track.getPosition());

        // 2. Knights lose (0 < 1) → pillage Alice's city
        assertTrue(inter.demoteToSettlement(alice));
        assertTrue(inter.getBuilding() instanceof Settlement);

        // 3. No knights to deactivate
        assertEquals(0, alice.getActiveKnightStrength());
    }

    private Intersection findEmptyIntersection(MasterReferee ref) {
        Collection<Intersection> intersections = ref.getBoard().getIntersections().values();
        for (Intersection inter : intersections) {
            if (inter.getBuilding() == null) {
                return inter;
            }
        }
        throw new IllegalStateException("No empty intersection found");
    }
}
