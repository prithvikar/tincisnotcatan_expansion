package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCard;
import edu.brown.cs.catan.Referee;

public class StealProgressCardTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test
    public void testStealCardFromOpponent() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        Player bob = ref.getPlayerByID(p2);
        bob.addProgressCard(ProgressCard.CRANE);

        assertEquals(1, bob.getProgressCards().size());
        int aliceCardsBefore = ref.getPlayerByID(p1).getProgressCards().size();

        StealProgressCard action = new StealProgressCard(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.addProperty("targetPlayer", p2);
        action.setupAction(ref, p1, params);
        Map<Integer, ActionResponse> result = action.execute();

        assertTrue(result.get(p1).getSuccess());
        assertEquals(0, bob.getProgressCards().size());
        assertEquals(aliceCardsBefore + 1,
                ref.getPlayerByID(p1).getProgressCards().size());
    }

    @Test
    public void testStealFromEmptyHand() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        StealProgressCard action = new StealProgressCard(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = new JsonObject();
        params.addProperty("targetPlayer", p2);
        action.setupAction(ref, p1, params);
        Map<Integer, ActionResponse> result = action.execute();

        assertTrue(result.get(p1).getSuccess());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPlayerSetup() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        StealProgressCard action = new StealProgressCard(p1);
        JsonObject params = new JsonObject();
        params.addProperty("targetPlayer", p2);
        action.setupAction(ref, p2, params);
    }
}
