package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class ChooseResourceTest {

        private Referee setupGame() {
                Referee ref = new MasterReferee();
                ref.addPlayer("Alice", "#000000");
                ref.addPlayer("Bob", "#ffffff");
                ref.addPlayer("Charlie", "#ff0000");
                ref.startNextTurn();
                return ref;
        }

        @Test
        public void testResourceMonopolyCapped() {
                Referee ref = setupGame();
                int p1 = 0, p2 = 1, p3 = 2;

                ref.getPlayerByID(p2).addResource(Resource.WHEAT, 5);
                ref.getPlayerByID(p3).addResource(Resource.WHEAT, 1);

                double aliceWheatBefore = ref.getPlayerByID(p1).getResources()
                                .getOrDefault(Resource.WHEAT, 0.0);

                ChooseResource action = new ChooseResource(p1);
                ref.addFollowUp(Collections.singletonList(action));
                JsonObject params = new JsonObject();
                params.addProperty("resource", "wheat");
                action.setupAction(ref, p1, params);
                Map<Integer, ActionResponse> result = action.execute();

                double aliceWheatAfter = ref.getPlayerByID(p1).getResources()
                                .get(Resource.WHEAT);
                assertEquals(aliceWheatBefore + 3, aliceWheatAfter, 0.001);
                assertEquals(3.0, ref.getPlayerByID(p2).getResources()
                                .get(Resource.WHEAT), 0.001);
                assertEquals(0.0, ref.getPlayerByID(p3).getResources()
                                .get(Resource.WHEAT), 0.001);
                assertTrue(result.get(p1).getSuccess());
        }

        @Test
        public void testResourceMonopolyNoOneHas() {
                Referee ref = setupGame();
                int p1 = 0;

                ChooseResource action = new ChooseResource(p1);
                ref.addFollowUp(Collections.singletonList(action));
                JsonObject params = new JsonObject();
                params.addProperty("resource", "ore");
                action.setupAction(ref, p1, params);
                Map<Integer, ActionResponse> result = action.execute();

                assertTrue(result.get(p1).getSuccess());
        }
}
