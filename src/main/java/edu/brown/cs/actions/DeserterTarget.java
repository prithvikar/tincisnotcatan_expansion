package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.board.IntersectionCoordinate;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Deserter progress card. The player selects an
 * opponent's knight to remove from the board entirely.
 */
public class DeserterTarget implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private int _targetPlayerID;
    private IntersectionCoordinate _knightLocation;
    private boolean _isSetup;
    public static final String ID = "deserterTarget";

    public DeserterTarget(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player target = _ref.getPlayerByID(_targetPlayerID);
        if (target == null) {
            return createResponse(false, "Invalid target player.", null);
        }
        if (target.getID() == _playerID) {
            return createResponse(false,
                    "You cannot target your own knight.", null);
        }

        // Find the knight at the specified location
        KnightPiece toRemove = null;
        for (KnightPiece k : target.getKnights()) {
            if (k.getPosition() != null
                    && k.getPosition().equals(_knightLocation)) {
                toRemove = k;
                break;
            }
        }

        if (toRemove == null) {
            return createResponse(false,
                    "No knight found at that location for the target player.",
                    null);
        }

        target.removeKnight(toRemove);

        _ref.removeFollowUp(this);

        String msg = String.format("%s used Deserter to remove %s's knight!",
                _ref.getPlayerByID(_playerID).getName(), target.getName());
        return createResponse(true, msg, null);
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
                "Select an opponent's knight to remove from the board.");
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
            _targetPlayerID = json.get("targetPlayer").getAsInt();
            _knightLocation = toIntersectionCoordinate(
                    json.get("coordinate").getAsJsonObject());
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid target player or coordinate");
        }
    }

    @Override
    public String getVerb() {
        return "choose a knight to desert";
    }

    private IntersectionCoordinate toIntersectionCoordinate(JsonObject object) {
        JsonObject coord1 = object.get("coord1").getAsJsonObject();
        JsonObject coord2 = object.get("coord2").getAsJsonObject();
        JsonObject coord3 = object.get("coord3").getAsJsonObject();
        HexCoordinate h1 = new HexCoordinate(coord1.get("x").getAsInt(),
                coord1.get("y").getAsInt(), coord1.get("z").getAsInt());
        HexCoordinate h2 = new HexCoordinate(coord2.get("x").getAsInt(),
                coord2.get("y").getAsInt(), coord2.get("z").getAsInt());
        HexCoordinate h3 = new HexCoordinate(coord3.get("x").getAsInt(),
                coord3.get("y").getAsInt(), coord3.get("z").getAsInt());
        return new IntersectionCoordinate(h1, h2, h3);
    }
}
