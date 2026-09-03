package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Resource;

public class CommercialHarborTest {

  private MasterReferee _ref;
  private Player _p1;

  @Before
  public void setUp() {
    _ref = new MasterReferee();
    _ref.addPlayer("P1", "blue");
    _ref.startNextTurn();
    _p1 = _ref.getPlayerByID(0);
  }

  @Test
  public void testGiveCommoditySuccess() {
    _p1.addCommodity(Commodity.PAPER, 1.0);
    CommercialHarbor action = new CommercialHarbor(0);
    JsonObject json = new JsonObject();
    json.addProperty("giveCommodity", true);
    json.addProperty("resource", "wood");
    json.addProperty("commodity", "paper");

    action.setupAction(_ref, 0, json);
    _ref.addFollowUp(Collections.singletonList(action));
    Map<Integer, ActionResponse> response = action.execute();

    assertTrue(response.containsKey(0));
    assertTrue(response.get(0).getMessage().equals("Commercial Harbor exchange complete."));
    assertEquals(0.0, _p1.getCommodities().getOrDefault(Commodity.PAPER, 0.0), 0.001);
    assertEquals(1.0, _p1.getResources().getOrDefault(Resource.WOOD, 0.0), 0.001);
  }

  @Test
  public void testGiveCommodityMissing() {
    CommercialHarbor action = new CommercialHarbor(0);
    JsonObject json = new JsonObject();
    json.addProperty("giveCommodity", true);
    json.addProperty("resource", "wood");
    json.addProperty("commodity", "paper");

    action.setupAction(_ref, 0, json);
    _ref.addFollowUp(Collections.singletonList(action));
    Map<Integer, ActionResponse> response = action.execute();

    assertTrue(response.containsKey(0));
    assertTrue(response.get(0).getMessage().contains("You do not have a paper to trade"));
  }

  @Test
  public void testGetCommoditySuccess() {
    _p1.addResource(Resource.ORE, 2.0, _ref.getBank());
    CommercialHarbor action = new CommercialHarbor(0);
    JsonObject json = new JsonObject();
    json.addProperty("giveCommodity", false);
    json.addProperty("resource", "ore");
    json.addProperty("commodity", "coin");

    action.setupAction(_ref, 0, json);
    _ref.addFollowUp(Collections.singletonList(action));
    Map<Integer, ActionResponse> response = action.execute();

    assertTrue(response.containsKey(0));
    assertTrue(response.get(0).getMessage().equals("Commercial Harbor exchange complete."));
    assertEquals(0.0, _p1.getResources().getOrDefault(Resource.ORE, 0.0), 0.001);
    assertEquals(1.0, _p1.getCommodities().getOrDefault(Commodity.COIN, 0.0), 0.001);
  }

  @Test
  public void testGetCommodityMissing() {
    _p1.addResource(Resource.ORE, 1.0, _ref.getBank());
    CommercialHarbor action = new CommercialHarbor(0);
    JsonObject json = new JsonObject();
    json.addProperty("giveCommodity", false);
    json.addProperty("resource", "ore");
    json.addProperty("commodity", "coin");

    action.setupAction(_ref, 0, json);
    _ref.addFollowUp(Collections.singletonList(action));
    Map<Integer, ActionResponse> response = action.execute();

    assertTrue(response.containsKey(0));
    assertTrue(response.get(0).getMessage().contains("You do not have two ores to trade"));
  }

  @Test
  public void testInvalidPair() {
    _p1.addCommodity(Commodity.PAPER, 1.0);
    CommercialHarbor action = new CommercialHarbor(0);
    JsonObject json = new JsonObject();
    json.addProperty("giveCommodity", true);
    json.addProperty("resource", "ore");
    json.addProperty("commodity", "paper");

    action.setupAction(_ref, 0, json);
    _ref.addFollowUp(Collections.singletonList(action));
    Map<Integer, ActionResponse> response = action.execute();

    assertTrue(response.containsKey(0));
    assertTrue(response.get(0).getMessage().contains("Invalid exchange pair chosen"));
  }
}
