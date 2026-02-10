package edu.brown.cs.catan;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.JsonParser;

import edu.brown.cs.api.CatanConverter;
import edu.brown.cs.board.Board;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Building;
import edu.brown.cs.board.City;
import edu.brown.cs.board.Settlement;

public class MetropolisTest {

  @Test
  public void testMetropolisLogic() {
    // Setup C&K game
    JsonObject settingsJson = new JsonObject();
    settingsJson.addProperty("isCitiesAndKnights", true);
    settingsJson.addProperty("numPlayers", 4);
    GameSettings settings = new GameSettings(settingsJson);
    MasterReferee ref = new MasterReferee(settings);

    // Add players
    int p1 = ref.addPlayer("P1", "#FF0000");
    int p2 = ref.addPlayer("P2", "#00FF00");

    Player player1 = ref.getPlayerByID(p1);
    Player player2 = ref.getPlayerByID(p2);

    // Initial state: no owners
    assertNull(ref.getMetropolisOwner(CityImprovement.Track.TRADE));

    // P1 upgrades Trade to Level 4
    for (int i = 0; i < 4; i++) {
      player1.getCityImprovement().advance(CityImprovement.Track.TRADE);
    }
    ref.updateMetropolis(CityImprovement.Track.TRADE, p1);

    // Verify P1 owns Trade Metropolis
    assertEquals(Integer.valueOf(p1), ref.getMetropolisOwner(CityImprovement.Track.TRADE));
    assertEquals(2, ref.getNumPublicPoints(p1)); // 2 VP for metropolis

    // P2 upgrades to Level 4 -> No change (must exceed)
    for (int i = 0; i < 4; i++) {
        player2.getCityImprovement().advance(CityImprovement.Track.TRADE);
    }
    ref.updateMetropolis(CityImprovement.Track.TRADE, p2);
    assertEquals(Integer.valueOf(p1), ref.getMetropolisOwner(CityImprovement.Track.TRADE));
    
    // P2 upgrades to Level 5 -> Steal
    player2.getCityImprovement().advance(CityImprovement.Track.TRADE);
    ref.updateMetropolis(CityImprovement.Track.TRADE, p2);
    
    assertEquals(Integer.valueOf(p2), ref.getMetropolisOwner(CityImprovement.Track.TRADE));
    assertEquals(0, ref.getNumPublicPoints(p1)); // P1 lost VP
    assertEquals(2, ref.getNumPublicPoints(p2)); // P2 gained VP
  }

  @Test
  public void testMetropolisSerialization() {
      // Setup C&K game
      JsonObject settingsJson = new JsonObject();
      settingsJson.addProperty("isCitiesAndKnights", true);
      settingsJson.addProperty("numPlayers", 4);
      GameSettings settings = new GameSettings(settingsJson);
      MasterReferee ref = new MasterReferee(settings);
  
      int p1 = ref.addPlayer("P1", "#FF0000");
      Player player1 = ref.getPlayerByID(p1);
      
      // Place a city for P1 manually
      // Need a valid intersection. 
      Board board = ref.getBoard();
      // Pick first intersection
      Intersection inter = board.getIntersections().values().iterator().next();
      inter.placeSettlement(player1); // Must be settlement first
      inter.placeCity(player1); // Then city
      
      // Give P1 logic ownership of Metropolis
      for (int i = 0; i < 4; i++) {
          player1.getCityImprovement().advance(CityImprovement.Track.POLITICS);
      }
      ref.updateMetropolis(CityImprovement.Track.POLITICS, p1);
      
      // Serialize board
      CatanConverter converter = new CatanConverter();
      JsonObject gameState = converter.getGameState(ref, p1);
      JsonObject json = gameState.get("board").getAsJsonObject();
      
      // Inspect JSON
      // Structure: {intersections: [ {building: {type: "city", metropolis: "politics"}}, ... ]}
      boolean foundMetropolis = false;
      for (com.google.gson.JsonElement el : json.get("intersections").getAsJsonArray()) {
          JsonObject iObj = el.getAsJsonObject();
          if (iObj.has("building")) {
              JsonObject bObj = iObj.get("building").getAsJsonObject();
              if (bObj.get("type").getAsString().equals("city")) {
                  if (bObj.has("metropolis") && bObj.get("metropolis").getAsString().equals("politics")) {
                      foundMetropolis = true;
                  }
              }
          }
      }
      
      assertTrue("Should find a city with politics metropolis", foundMetropolis);
  }
}
