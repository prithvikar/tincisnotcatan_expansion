package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class ChooseCommodityTest {

        private Referee setupGame() {
                Referee ref = new MasterReferee();
                ref.addPlayer("Alice", "#000000");
                ref.addPlayer("Bob", "#ffffff");
                ref.addPlayer("Charlie", "#ff0000");
                ref.startNextTurn();
                return ref;
        }

        @Test
        public void testTradeMonopolyGivesOneEach() {
                Referee ref = setupGame();
                int p1 = 0, p2 = 1, p3 = 2;

                ref.getPlayerByID(p2).addResource(Resource.BRICK, 5);
                ref.getPlayerByID(p3).addResource(Resource.BRICK, 3);

                double aliceBefore = ref.getPlayerByID(p1).getResources()
                                .getOrDefault(Resource.BRICK, 0.0);

                ChooseCommodity action = new ChooseCommodity(p1);
                ref.addFollowUp(Collections.singletonList(action));
                JsonObject params = new JsonObject();
                params.addProperty("resource", "brick");
                action.setupAction(ref, p1, params);
                Map<Integer, ActionResponse> result = action.execute();

                double aliceAfter = ref.getPlayerByID(p1).getResources()
                                .get(Resource.BRICK);
                assertEquals(aliceBefore + 2, aliceAfter, 0.001);
                assertEquals(4.0, ref.getPlayerByID(p2).getResources()
                                .get(Resource.BRICK), 0.001);
                assertEquals(2.0, ref.getPlayerByID(p3).getResources()
                                .get(Resource.BRICK), 0.001);
                assertTrue(result.get(p1).getSuccess());
        }

        @Test
        public void testTradeMonopolyOpponentHasNone() {
                Referee ref = setupGame();
                int p1 = 0;

                ChooseCommodity action = new ChooseCommodity(p1);
                ref.addFollowUp(Collections.singletonList(action));
                JsonObject params = new JsonObject();
                params.addProperty("resource", "wood");
                action.setupAction(ref, p1, params);
                Map<Integer, ActionResponse> result = action.execute();

                assertTrue(result.get(p1).getSuccess());
        }
}
