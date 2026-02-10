package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Inventor progress card. Swaps the number tokens
 * on two land hexes (excluding 2, 6, 8, 12).
 */
public class SwapHexNumbers implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private HexCoordinate _hex1;
    private HexCoordinate _hex2;
    private boolean _isSetup;
    public static final String ID = "swapHexNumbers";

    public SwapHexNumbers(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        try {
            _ref.getBoard().swapRollNumbers(_hex1, _hex2);
        } catch (IllegalArgumentException e) {
            return createResponse(false, e.getMessage(), null);
        }

        _ref.removeFollowUp(this);

        String playerName = _ref.getPlayerByID(_playerID).getName();
        return createResponse(true,
                playerName + " swapped number tokens with Inventor.", null);
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
                "Select two hexes to swap numbers (excluding 2, 6, 8, 12).");
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
            _hex1 = convertToHexCoordinate(json.get("hex1").getAsJsonObject());
            _hex2 = convertToHexCoordinate(json.get("hex2").getAsJsonObject());
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid hex coordinates");
        }
    }

    @Override
    public String getVerb() {
        return "swap number tokens";
    }

    private HexCoordinate convertToHexCoordinate(JsonObject json) {
        int x = json.get("x").getAsInt();
        int y = json.get("y").getAsInt();
        int z = json.get("z").getAsInt();
        return new HexCoordinate(x, y, z);
    }
}
