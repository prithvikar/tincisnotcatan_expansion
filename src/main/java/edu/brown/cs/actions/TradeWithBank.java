package edu.brown.cs.actions;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class TradeWithBank implements Action {

  public static final String ID = "tradeWithBank";
  private Referee _ref;
  private Player _player;
  private Resource _toGiveResource;
  private Commodity _toGiveCommodity;
  private Resource _toGetResource;
  private Commodity _toGetCommodity;
  private double _amount;
  private String _toGiveStr;
  private String _toGetStr;
  private boolean _giveIsCommodity;
  private boolean _getIsCommodity;

  public TradeWithBank(Referee ref, int playerID, JsonObject params) {
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException("No player exists with the given ID.");
    }
    if (!params.get("toGive").isJsonNull() && !params.get("toGet").isJsonNull()) {
      _toGiveStr = params.get("toGive").getAsString();
      _toGetStr = params.get("toGet").getAsString();
      _amount = params.get("amount").getAsDouble();
      try {
        _toGiveResource = Resource.stringToResource(_toGiveStr);
        _giveIsCommodity = false;
      } catch (IllegalArgumentException e) {
        _toGiveCommodity = Commodity.stringToCommodity(_toGiveStr);
        _giveIsCommodity = true;
      }
      try {
        _toGetResource = Resource.stringToResource(_toGetStr);
        _getIsCommodity = false;
      } catch (IllegalArgumentException e) {
        _toGetCommodity = Commodity.stringToCommodity(_toGetStr);
        _getIsCommodity = true;
      }
    } else {
      throw new IllegalArgumentException("toGive and toGet cannot be null.");
    }

  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    // Validation:
    Double rate = 4.0;
    if (!_giveIsCommodity) {
      Map<Resource, Double> rates = _ref.getBankRates(_player.getID());
      rate = rates.get(_toGiveResource);
    }

    if (_ref.getTurn().hasMerchantFleet()) {
      rate = Math.min(rate, 2.0);
    }

    if (_giveIsCommodity) {
      edu.brown.cs.catan.CityImprovement ci = _player.getCityImprovement();
      if (ci != null && ci.getLevel(edu.brown.cs.catan.CityImprovement.Track.TRADE) >= 3) {
        rate = Math.min(rate, 2.0);
      }
    }

    if (!_ref.getGameSettings().isDecimal) {
      rate = Math.ceil(rate);
    }

    double amountToGive = rate * _amount;

    if (_giveIsCommodity) {
      if (!_player.hasCommodity(_toGiveCommodity, amountToGive)) {
        String message = String.format("You do not have enough %s to trade with the bank", _toGiveStr);
        return ImmutableMap.of(_player.getID(), new ActionResponse(false, message, null));
      }
    } else {
      if (!_player.hasResource(_toGiveResource, amountToGive)) {
        String message = String.format("You do not have enough %s to trade with the bank", _toGiveStr);
        return ImmutableMap.of(_player.getID(), new ActionResponse(false, message, null));
      }
    }

    if (!_ref.currentPlayer().equals(_player)) {
      String message = String.format("You can only trade with the bank on your turn", _toGiveStr);
      return ImmutableMap.of(_player.getID(), new ActionResponse(false, message, null));
    }

    // Action:
    if (_giveIsCommodity) {
      _player.removeCommodity(_toGiveCommodity, amountToGive);
    } else {
      _player.removeResource(_toGiveResource, amountToGive, _ref.getBank());
    }

    if (_getIsCommodity) {
      _player.addCommodity(_toGetCommodity, _amount * 1);
    } else {
      _player.addResource(_toGetResource, _amount * 1, _ref.getBank());
    }

    // Format responses:
    NumberFormat nf = new DecimalFormat("##.##");
    String messageToPlayer = String.format(
        "You traded with the bank and got %s %s", nf.format(_amount), _toGetStr);
    ActionResponse respToPlayer = new ActionResponse(true, messageToPlayer,
        null);
    String messageToAll = String
        .format("%s traded %s %s for %s %s", _player.getName(), nf.format(amountToGive),
            _toGiveStr, nf.format(_amount), _toGetStr);
    ActionResponse respToAll = new ActionResponse(true, messageToAll, null);
    Map<Integer, ActionResponse> toReturn = new HashMap<>();
    for (Player p : _ref.getPlayers()) {
      if (p.equals(_player)) {
        toReturn.put(p.getID(), respToPlayer);
      } else {
        toReturn.put(p.getID(), respToAll);
      }
    }
    return toReturn;
  }
}
