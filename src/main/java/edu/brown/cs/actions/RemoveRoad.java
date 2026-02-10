package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.board.Path;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Diplomat progress card. Allows the player to remove
 * any open road from the board (theirs or an opponent's).
 */
public class RemoveRoad implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private IntersectionCoordinate _start;
    private IntersectionCoordinate _end;
    private boolean _isSetup;
    public static final String ID = "removeRoad";

    public RemoveRoad(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException(
                    "Action must be setup before execution.");
        }

        Path path = _ref.getBoard().getPath(_start, _end);
        if (path == null || path.getRoad() == null) {
            return createResponse(false, "There is no road there.", null);
        }

        try {
            _ref.getBoard().removeRoad(_start, _end);
        } catch (IllegalArgumentException e) {
            return createResponse(false, e.getMessage(), null);
        }

        _ref.removeFollowUp(this);

        String playerName = _ref.getPlayerByID(_playerID).getName();
        return createResponse(true,
                playerName + " removed a road with Diplomat.", null);
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
        json.addProperty("message", "Select a road to remove.");
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
            JsonObject startJson = json.get("start").getAsJsonObject();
            JsonObject endJson = json.get("end").getAsJsonObject();
            _start = toIntersectionCoordinate(startJson);
            _end = toIntersectionCoordinate(endJson);
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid road coordinates");
        }
    }

    @Override
    public String getVerb() {
        return "remove a road";
    }

    private IntersectionCoordinate toIntersectionCoordinate(JsonObject object) {
        JsonObject coord1 = object.get("coord1").getAsJsonObject();
        JsonObject coord2 = object.get("coord2").getAsJsonObject();
        JsonObject coord3 = object.get("coord3").getAsJsonObject();
        HexCoordinate h1 = new HexCoordinate(coord1.get("x").getAsInt(), coord1
                .get("y").getAsInt(), coord1.get("z").getAsInt());
        HexCoordinate h2 = new HexCoordinate(coord2.get("x").getAsInt(), coord2
                .get("y").getAsInt(), coord2.get("z").getAsInt());
        HexCoordinate h3 = new HexCoordinate(coord3.get("x").getAsInt(), coord3
                .get("y").getAsInt(), coord3.get("z").getAsInt());
        return new IntersectionCoordinate(h1, h2, h3);
    }

}
