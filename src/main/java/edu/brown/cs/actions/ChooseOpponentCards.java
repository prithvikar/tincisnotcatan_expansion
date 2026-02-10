package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Follow-up action for the Master Merchant progress card. The player selects an
 * opponent who has more VPs and takes 2 random resource cards from them.
 */
public class ChooseOpponentCards implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private int _targetPlayerID;
    private boolean _isSetup;
    public static final String ID = "chooseOpponentCards";

    public ChooseOpponentCards(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player thief = _ref.getPlayerByID(_playerID);
        Player target = _ref.getPlayerByID(_targetPlayerID);
        if (target == null) {
            return createResponse(false, "Invalid target player.", null);
        }

        // Validate: target must have more VPs
        if (target.numVictoryPoints() <= thief.numVictoryPoints()) {
            return createResponse(false,
                    "Target must have more victory points than you.", null);
        }

        // Collect all resource cards the target has
        List<Resource> availableCards = new ArrayList<>();
        for (Map.Entry<Resource, Double> entry : target.getResources()
                .entrySet()) {
            if (entry.getKey() != Resource.WILDCARD) {
                int count = (int) Math.floor(entry.getValue());
                for (int i = 0; i < count; i++) {
                    availableCards.add(entry.getKey());
                }
            }
        }

        int toTake = Math.min(2, availableCards.size());
        Random rand = new Random();
        List<Resource> taken = new ArrayList<>();
        for (int i = 0; i < toTake; i++) {
            int idx = rand.nextInt(availableCards.size());
            Resource r = availableCards.remove(idx);
            target.removeResource(r, 1);
            thief.addResource(r, 1);
            taken.add(r);
        }

        _ref.removeFollowUp(this);

        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        "Master Merchant: you took " + taken + " from "
                                + target.getName() + "!",
                        null));
            } else if (p.getID() == _targetPlayerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        thief.getName()
                                + " used Master Merchant to take 2 cards from you!",
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        thief.getName() + " used Master Merchant on "
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
                "Select a player with more VPs to take 2 cards from.");
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
        return "choose an opponent (Master Merchant)";
    }
}
