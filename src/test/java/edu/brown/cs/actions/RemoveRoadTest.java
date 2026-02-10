package edu.brown.cs.actions;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;

public class RemoveRoadTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteWithoutSetup() {
        RemoveRoad action = new RemoveRoad(0);
        action.execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPlayerSetup() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        RemoveRoad action = new RemoveRoad(p1);
        JsonObject params = new JsonObject();
        params.add("start", createIntersectionJson(0, 0, 0, 0, 1, 0, 1, 0, 0));
        params.add("end", createIntersectionJson(0, 1, 0, 1, 0, 0, 1, 1, 0));
        action.setupAction(ref, p2, params);
    }

    @Test
    public void testRemoveNonexistentRoad() {
        Referee ref = setupGame();
        int p1 = 0;

        RemoveRoad action = new RemoveRoad(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.add("start", createIntersectionJson(0, 0, 0, 0, 1, 0, 1, 0, 0));
        params.add("end", createIntersectionJson(0, 1, 0, 1, 0, 0, 1, 1, 0));
        action.setupAction(ref, p1, params);
        java.util.Map<Integer, ActionResponse> result = action.execute();
        assertTrue(!result.isEmpty());
    }

    private JsonObject createIntersectionJson(int x1, int y1, int z1,
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
