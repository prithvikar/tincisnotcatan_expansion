package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Merchant progress card. The player places the
 * merchant pawn on a land hex adjacent to one of their buildings, gaining
 * a 2:1 trade ratio for that hex's resource and 1 VP.
 */
public class PlaceMerchant implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private HexCoordinate _hex;
    private boolean _isSetup;
    public static final String ID = "placeMerchant";

    public PlaceMerchant(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        // Validate: the hex must have at least one intersection with a
        // building owned by this player
        boolean hasBuilding = false;
        for (Intersection inter : _ref.getBoard().getIntersections().values()) {
            if (inter.getBuilding() != null
                    && inter.getBuilding().getPlayer().getID() == _playerID) {
                IntersectionCoordinate pos = inter.getPosition();
                if (_hex.equals(pos.getCoord1())
                        || _hex.equals(pos.getCoord2())
                        || _hex.equals(pos.getCoord3())) {
                    hasBuilding = true;
                    break;
                }
            }
        }

        if (!hasBuilding) {
            return createResponse(false,
                    "You must place the merchant on a hex adjacent to one of your buildings.",
                    null);
        }

        _ref.setMerchant(_playerID, _hex);
        _ref.removeFollowUp(this);

        String playerName = _ref.getPlayerByID(_playerID).getName();
        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        "Merchant placed! You gain 2:1 trade for this resource and 1 VP.",
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        playerName + " placed the Merchant.", null));
            }
        }
        return toRet;
    }

    private Map<Integer, ActionResponse> createResponse(boolean success,
            String message, Object data) {
        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            toRet.put(p.getID(), new ActionResponse(success, message, data));
        }
        return toRet;
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
                "Place the Merchant on a land hex adjacent to your building.");
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
        try {
            JsonObject hexJson = json.get("hex").getAsJsonObject();
            int x = hexJson.get("x").getAsInt();
            int y = hexJson.get("y").getAsInt();
            int z = hexJson.get("z").getAsInt();
            _hex = new HexCoordinate(x, y, z);
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex coordinate");
        }
    }

    @Override
    public String getVerb() {
        return "place the merchant";
    }
}
