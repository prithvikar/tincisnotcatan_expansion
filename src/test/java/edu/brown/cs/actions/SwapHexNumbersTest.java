package edu.brown.cs.actions;

import static org.junit.Assert.assertTrue;

import java.util.Collections;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;

public class SwapHexNumbersTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteWithoutSetup() {
        SwapHexNumbers action = new SwapHexNumbers(0);
        action.execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPlayerSetup() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        SwapHexNumbers action = new SwapHexNumbers(p1);
        JsonObject params = new JsonObject();
        params.add("hex1", makeHexJson(0, 0, 0));
        params.add("hex2", makeHexJson(0, 1, 0));
        action.setupAction(ref, p2, params);
    }

    @Test
    public void testSetupValid() {
        Referee ref = setupGame();
        int p1 = 0;

        SwapHexNumbers action = new SwapHexNumbers(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.add("hex1", makeHexJson(0, 0, 0));
        params.add("hex2", makeHexJson(0, 1, 0));
        action.setupAction(ref, p1, params);

        assertTrue(action.getID().equals("swapHexNumbers"));
    }

    private JsonObject makeHexJson(int x, int y, int z) {
        JsonObject hex = new JsonObject();
        hex.addProperty("x", x);
        hex.addProperty("y", y);
        hex.addProperty("z", z);
        return hex;
    }
}
