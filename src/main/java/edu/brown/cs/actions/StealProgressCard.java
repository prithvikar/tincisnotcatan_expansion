package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCard;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Spy progress card. Allows the player to steal a
 * random progress card from a target opponent.
 */
public class StealProgressCard implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private int _targetPlayerID;
    private boolean _isSetup;
    public static final String ID = "stealProgressCard";

    public StealProgressCard(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player target = _ref.getPlayerByID(_targetPlayerID);
        Player thief = _ref.getPlayerByID(_playerID);
        if (target == null) {
            return createResponse(false, "Invalid target player.", null);
        }

        List<ProgressCard> targetCards = target.getProgressCards();
        if (targetCards == null || targetCards.isEmpty()) {
            _ref.removeFollowUp(this);
            return createResponse(true,
                    target.getName() + " has no progress cards to steal.", null);
        }

        // Pick a random card
        Random rand = new Random();
        ProgressCard stolen = targetCards.get(rand.nextInt(targetCards.size()));
        target.removeProgressCard(stolen);
        thief.addProgressCard(stolen);

        _ref.removeFollowUp(this);

        // Different messages per player
        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        "You stole " + stolen.getName() + " from "
                                + target.getName() + "!",
                        null));
            } else if (p.getID() == _targetPlayerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        thief.getName() + " stole a progress card from you!",
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        thief.getName() + " stole a progress card from "
                                + target.getName() + ".",
                        null));
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
                "Select a player to steal a progress card from.");
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
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid target player");
        }
    }

    @Override
    public String getVerb() {
        return "steal a progress card";
    }
}
