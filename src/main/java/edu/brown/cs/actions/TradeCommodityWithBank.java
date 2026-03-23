package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Action for trading commodities with the bank. Requires Trade track level 3
 * (which grants 2:1 commodity trading). The player gives 2 of a commodity and
 * receives 1 of any resource.
 */
public class TradeCommodityWithBank implements Action {

  public static final String ID = "tradeCommodityWithBank";
  private Referee _ref;
  private Player _player;
  private Commodity _toGive;
  private Resource _toGet;

  public TradeCommodityWithBank(Referee ref, int playerID, JsonObject params) {
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException("No player exists with the given ID.");
    }
    String toGive = params.get("toGive").getAsString();
    String toGet = params.get("toGet").getAsString();
    _toGive = Commodity.stringToCommodity(toGive);
    _toGet = Resource.stringToResource(toGet);
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    // Must be current player's turn
    if (!_ref.currentPlayer().equals(_player)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You can only trade with the bank on your turn.", null));
    }

    // Check Trade track level >= 3
    CityImprovement ci = _player.getCityImprovement();
    if (ci == null || ci.getLevel(CityImprovement.Track.TRADE) < 3) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          "You need Trade track level 3 to trade commodities at 2:1.", null));
    }

    // Check player has 2 of the commodity
    if (!_player.hasCommodity(_toGive, 2)) {
      return ImmutableMap.of(_player.getID(), new ActionResponse(false,
          String.format("You need at least 2 %s to trade.", _toGive), null));
    }

    // Execute trade
    _player.removeCommodity(_toGive, 2);
    _player.addResource(_toGet, 1.0, _ref.getBank());

    // Responses
    String playerMsg = String.format("You traded 2 %s for 1 %s with the bank.",
        _toGive, _toGet);
    String publicMsg = String.format("%s traded commodities with the bank.",
        _player.getName());
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.equals(_player)) {
        toReturn.put(p.getID(), new ActionResponse(true, playerMsg, null));
      } else {
        toReturn.put(p.getID(), new ActionResponse(true, publicMsg, null));
      }
    }
    return toReturn;
  }
}
