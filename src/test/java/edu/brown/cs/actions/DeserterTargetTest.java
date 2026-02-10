package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

public class DeserterTargetTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test
    public void testDeserterRemovesKnight() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        Player bob = ref.getPlayerByID(p2);
        HexCoordinate h1 = new HexCoordinate(0, 0, 0);
        HexCoordinate h2 = new HexCoordinate(0, 1, 0);
        HexCoordinate h3 = new HexCoordinate(1, 0, 0);
        IntersectionCoordinate coord = new IntersectionCoordinate(h1, h2, h3);
        KnightPiece knight = new KnightPiece(p2, coord);
        bob.addKnight(knight);

        assertEquals(1, bob.getKnights().size());

        DeserterTarget action = new DeserterTarget(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.addProperty("targetPlayer", p2);
        JsonObject coordinateJson = new JsonObject();
        coordinateJson.add("coord1", makeCoord(0, 0, 0));
        coordinateJson.add("coord2", makeCoord(0, 1, 0));
        coordinateJson.add("coord3", makeCoord(1, 0, 0));
        params.add("coordinate", coordinateJson);
        action.setupAction(ref, p1, params);
        Map<Integer, ActionResponse> result = action.execute();

        assertTrue(result.get(p1).getSuccess());
        assertEquals(0, bob.getKnights().size());
    }

    private JsonObject makeCoord(int x, int y, int z) {
        JsonObject c = new JsonObject();
        c.addProperty("x", x);
        c.addProperty("y", y);
        c.addProperty("z", z);
        return c;
    }
}
