package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.CityImprovement.Track;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Referee.GameStatus;
import edu.brown.cs.catan.Settings;

/**
 * Action for advancing a city improvement track. The player pays the required
 * number of commodities to advance one level on a chosen track (Trade,
 * Politics, or Science). Reaching level 4 can grant a Metropolis.
 */
public class ImproveCityTrack implements Action {

  private final Player _player;
  private final Referee _ref;
  private final Track _track;
  public static final String ID = "improveCityTrack";

  public ImproveCityTrack(Referee ref, int playerID, String trackName) {
    _ref = ref;
    _player = _ref.getPlayerByID(playerID);
    _track = Track.fromString(trackName);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (_ref.getGameStatus() == GameStatus.PROGRESS
        && !_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You cannot improve a city track when it is not your turn.",
              null));
    }

    CityImprovement improvement = _player.getCityImprovement();
    if (improvement == null) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "City improvements are not available.", null));
    }

    int currentLevel = improvement.getLevel(_track);
    if (currentLevel >= CityImprovement.MAX_LEVEL) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              String.format("Your %s track is already at max level.",
                  _track.getName()),
              null));
    }

    // Player must have at least one city on the board to improve
    int numCitiesOnBoard = Settings.INITIAL_CITIES - _player.numCities();
    if (numCitiesOnBoard <= 0) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              "You must have at least one city on the board to improve.",
              null));
    }

    // Check commodity cost
    Commodity required = _track.getCommodity();
    int cost = improvement.getCostToAdvance(_track);
    if (!_player.hasCommodity(required, cost)) {
      return ImmutableMap.of(_player.getID(),
          new ActionResponse(false,
              String.format(
                  "You need %d %s to advance your %s track to level %d.",
                  cost, required, _track.getName(), currentLevel + 1),
              null));
    }

    // Pay commodities
    _player.removeCommodity(required, cost);

    // Advance track
    improvement.advance(_track);
    int newLevel = improvement.getLevel(_track);

    // Build response
    String playerMsg = String.format(
        "You advanced your %s track to level %d!", _track.getName(),
        newLevel);

    // Check for Metropolis at level 4
    if (newLevel >= CityImprovement.METROPOLIS_THRESHOLD) {
      playerMsg += " You may be eligible for a Metropolis!";
    }

    ActionResponse respToPlayer = new ActionResponse(true, playerMsg, null);
    String message = String.format("%s advanced their %s track to level %d.",
        _player.getName(), _track.getName(), newLevel);
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
