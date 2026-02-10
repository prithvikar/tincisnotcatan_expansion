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
 * Follow-up action for the Intrigue progress card. Allows the player to
 * displace an opponent's knight that is adjacent to one of the player's
 * buildings.
 */
public class DisplaceKnight implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private IntersectionCoordinate _coord;
    private boolean _isSetup;
    public static final String ID = "displaceKnight";

    public DisplaceKnight(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        // Validate the intersection exists
        if (!_ref.getBoard().getIntersections().containsKey(_coord)) {
            return createResponse(false, "Invalid intersection.", null);
        }

        // Find a knight at this location belonging to an opponent
        KnightPiece targetKnight = null;
        Player targetPlayer = null;
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                continue;
            }
            for (KnightPiece k : p.getKnights()) {
                if (k.getPosition() != null
                        && k.getPosition().equals(_coord)) {
                    targetKnight = k;
                    targetPlayer = p;
                    break;
                }
            }
            if (targetKnight != null) {
                break;
            }
        }

        if (targetKnight == null || targetPlayer == null) {
            return createResponse(false,
                    "No opponent knight found at that location.", null);
        }

        // Remove the knight from the opponent
        targetPlayer.removeKnight(targetKnight);

        _ref.removeFollowUp(this);

        String msg = String.format(
                "%s used Intrigue to displace %s's knight!",
                _ref.getPlayerByID(_playerID).getName(),
                targetPlayer.getName());
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
                "Select an opponent's knight to displace.");
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
            _coord = toIntersectionCoordinate(
                    json.get("coordinate").getAsJsonObject());
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid coordinate");
        }
    }

    @Override
    public String getVerb() {
        return "displace a knight";
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
