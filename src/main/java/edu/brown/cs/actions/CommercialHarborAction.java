package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Follow-up action for the Commercial Harbor progress card. Each opponent
 * must exchange 1 commodity of their choice for the corresponding resource.
 *
 * Simplified implementation: automatically swaps the first available commodity
 * from each opponent (Paper→Wood, Cloth→Sheep, Coin→Ore).
 */
public class CommercialHarborAction implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private boolean _isSetup;
    public static final String ID = "commercialHarbor";

    public CommercialHarborAction(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player cardPlayer = _ref.getPlayerByID(_playerID);
        StringBuilder summary = new StringBuilder();

        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                continue;
            }
            // Find first commodity the opponent has
            for (Commodity c : Commodity.values()) {
                if (p.hasCommodity(c, 1)) {
                    Resource matchingResource = c.toResource();
                    p.removeCommodity(c, 1);
                    p.addResource(matchingResource, 1);
                    summary.append(String.format(" %s traded 1 %s for 1 %s.",
                        p.getName(), c, matchingResource));
                    break;
                }
            }
        }

        _ref.removeFollowUp(this);

        String result = summary.length() > 0 ? summary.toString()
            : " No opponents had commodities to trade.";

        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                    "Commercial Harbor resolved!" + result, null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                    cardPlayer.getName() + " played Commercial Harbor."
                        + result,
                    null));
            }
        }
        return toRet;
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
            "Commercial Harbor: each opponent trades 1 commodity for the matching resource. Confirm to proceed.");
        return json;
    }

    @Override
    public String getID() {
        return ID;
    }

    @Override
    public int getPlayerID() {
        return _playerID;
    }

    @Override
    public void setupAction(Referee ref, int playerID, JsonObject json) {
        _ref = ref;
        if (_playerID != playerID) {
            throw new IllegalArgumentException("Wrong player ID");
        }
        _isSetup = true;
    }

    @Override
    public String getVerb() {
        return "resolve Commercial Harbor";
    }
}
