package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;
import edu.brown.cs.board.City;

/**
 * Action for building a city wall under an existing city. City walls increase
 * the player's hand limit by 2 when a 7 is rolled.
 * Cost: 2 Brick. Max 3 per player.
 */
public class BuildCityWall implements Action {

  private final Player _player;
  private final Intersection _intersection;
  private final Referee _ref;
  public static final String ID = "buildCityWall";

  public BuildCityWall(Referee ref, int playerID,
      IntersectionCoordinate location) {
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _intersection = _ref.getBoard().getIntersections().get(location);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    if (_intersection == null) {
      throw new IllegalArgumentException(
          "The intersection could not be found.");
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You cannot build a city wall when it is not your turn.", null));
    }

    // Must have a city here
    if (_intersection.getBuilding() == null
        || !(_intersection.getBuilding() instanceof City)
        || _intersection.getBuilding().getPlayer().getID() != _player
            .getID()) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You can only build a city wall under your own city.", null));
    }

    // Max walls
    if (_player.getCityWallCount() >= Settings.MAX_CITY_WALLS) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              String.format(
                  "You have already built the maximum of %d city walls.",
                  Settings.MAX_CITY_WALLS),
              null));
    }

    // Check cost
    for (Map.Entry<Resource, Double> price : Settings.CITY_WALL_COST
        .entrySet()) {
      if (!_player.hasResource(price.getKey(), price.getValue())) {
        return ImmutableMap.of(_player.getID(),
            new ActionResponse(false,
                "You cannot afford to build a city wall (requires 2 Brick).",
                null));
      }
    }

    // Pay
    for (Map.Entry<Resource, Double> price : Settings.CITY_WALL_COST
        .entrySet()) {
      _player.removeResource(price.getKey(), price.getValue());
    }

    // Build wall
    _player.addCityWall();

    // Response
    ActionResponse respToPlayer = new ActionResponse(true,
        "You built a City Wall! Your hand limit is now "
            + (int) _player.getHandLimit() + ".",
        null);
    String message = String.format("%s built a city wall.",
        _player.getName());
    ActionResponse respToAll = new ActionResponse(true, message, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player player : _ref.getPlayers()) {
      if (player.equals(_player)) {
        toReturn.put(player.getID(), respToPlayer);
      } else {
        toReturn.put(player.getID(), respToAll);
      }
    }
    return toReturn;
  }

}
