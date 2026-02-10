package edu.brown.cs.actions;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class ChooseOpponentCardsTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test
    public void testMasterMerchantSuccess() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        // Give Bob resources and VPs so he qualifies
        Player bob = ref.getPlayerByID(p2);
        bob.addResource(Resource.WHEAT, 3);
        bob.addVictoryPoint(); // Give Bob more VPs than Alice

        ChooseOpponentCards action = new ChooseOpponentCards(p1);
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

        ChooseOpponentCards action = new ChooseOpponentCards(p1);
        JsonObject params = new JsonObject();
        params.addProperty("targetPlayer", p2);
        action.setupAction(ref, p2, params);
    }
}
