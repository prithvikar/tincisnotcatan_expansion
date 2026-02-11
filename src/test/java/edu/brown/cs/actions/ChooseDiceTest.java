package edu.brown.cs.actions;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;

public class ChooseDiceTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteWithoutSetup() {
        ChooseDice action = new ChooseDice(0);
        action.execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPlayerSetup() {
        Referee ref = setupGame();
        ChooseDice action = new ChooseDice(0);
        JsonObject params = makeDiceParams(3, 4);
        action.setupAction(ref, 1, params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDiceTooLow() {
        Referee ref = setupGame();
        ChooseDice action = new ChooseDice(0);
        JsonObject params = makeDiceParams(0, 4);
        action.setupAction(ref, 0, params);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidDiceTooHigh() {
        Referee ref = setupGame();
        ChooseDice action = new ChooseDice(0);
        JsonObject params = makeDiceParams(3, 7);
        action.setupAction(ref, 0, params);
    }

    @Test
    public void testValidSetupAndExecute() {
        Referee ref = setupGame();
        int p1 = 0;

        ChooseDice action = new ChooseDice(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = makeDiceParams(3, 5);
        action.setupAction(ref, p1, params);

        Map<Integer, ActionResponse> result = action.execute();
        assertTrue(result.get(p1).getSuccess());
        assertTrue(result.get(p1).getMessage().contains("3 + 5 = 8"));
    }

    @Test
    public void testDiceOverrideSetAndConsumed() {
        Referee ref = setupGame();

        // Initially no override
        assertNull(ref.consumeOverriddenDice());

        // Set override
        ref.setOverriddenDice(2, 6);
        int[] override = ref.consumeOverriddenDice();
        assertArrayEquals(new int[] { 2, 6 }, override);

        // Should be consumed (null after first consume)
        assertNull(ref.consumeOverriddenDice());
    }

    @Test
    public void testExecuteSetsOverride() {
        Referee ref = setupGame();
        int p1 = 0;

        ChooseDice action = new ChooseDice(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = makeDiceParams(4, 2);
        action.setupAction(ref, p1, params);
        action.execute();

        // After execute, the override should be set on the referee
        int[] override = ref.consumeOverriddenDice();
        assertArrayEquals(new int[] { 4, 2 }, override);
    }

    @Test
    public void testGetVerb() {
        ChooseDice action = new ChooseDice(0);
        assertEquals("choose dice values", action.getVerb());
    }

    @Test
    public void testGetDataMessage() {
        ChooseDice action = new ChooseDice(0);
        JsonObject data = action.getData();
        assertTrue(data.has("message"));
        assertTrue(data.get("message").getAsString().contains("1-6"));
    }

    private JsonObject makeDiceParams(int red, int white) {
        JsonObject params = new JsonObject();
        params.addProperty("redDie", red);
        params.addProperty("whiteDie", white);
        return params;
    }
}
