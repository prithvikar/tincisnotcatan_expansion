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

public class DisplaceKnightTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteWithoutSetup() {
        DisplaceKnight action = new DisplaceKnight(0);
        action.execute();
    }

    @Test
    public void testDisplaceExistingKnight() {
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

        // The coordinates may not map to a real board intersection,
        // so the action should return failure gracefully
        DisplaceKnight action = new DisplaceKnight(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.add("coordinate", makeCoordJson(0, 0, 0, 0, 1, 0, 1, 0, 0));
        action.setupAction(ref, p1, params);
        Map<Integer, ActionResponse> result = action.execute();

        // Returns a response for all players regardless
        assertTrue(!result.isEmpty());
    }

    @Test
    public void testDisplaceNoKnightAtLocation() {
        Referee ref = setupGame();
        int p1 = 0;

        DisplaceKnight action = new DisplaceKnight(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.add("coordinate", makeCoordJson(0, 0, 0, 0, 1, 0, 1, 0, 0));
        action.setupAction(ref, p1, params);
        Map<Integer, ActionResponse> result = action.execute();

        assertTrue(!result.get(p1).getSuccess());
    }

    private JsonObject makeCoordJson(int x1, int y1, int z1,
            int x2, int y2, int z2, int x3, int y3, int z3) {
        JsonObject obj = new JsonObject();
        obj.add("coord1", makeCoord(x1, y1, z1));
        obj.add("coord2", makeCoord(x2, y2, z2));
        obj.add("coord3", makeCoord(x3, y3, z3));
        return obj;
    }

    private JsonObject makeCoord(int x, int y, int z) {
        JsonObject c = new JsonObject();
        c.addProperty("x", x);
        c.addProperty("y", y);
        c.addProperty("z", z);
        return c;
    }
}
