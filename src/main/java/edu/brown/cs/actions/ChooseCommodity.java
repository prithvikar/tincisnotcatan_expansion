package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Follow-up action for the Trade Monopoly progress card. The player names a
 * resource/commodity, and each opponent must give 1 of it (if they have it).
 */
public class ChooseCommodity implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private Resource _resource;
    private boolean _isSetup;
    public static final String ID = "chooseCommodity";

    public ChooseCommodity(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player monopolist = _ref.getPlayerByID(_playerID);
        int totalGained = 0;

        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                continue;
            }
            // Each opponent gives 1 if they have it
            double has = p.getResources().getOrDefault(_resource, 0.0);
            if (has >= 1.0) {
                p.removeResource(_resource, 1);
                monopolist.addResource(_resource, 1);
                totalGained++;
            }
        }

        _ref.removeFollowUp(this);

        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        String.format("Trade Monopoly: you gained %d %s!",
                                totalGained, _resource),
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        String.format("%s played Trade Monopoly on %s.",
                                monopolist.getName(), _resource),
                        null));
            }
        }
        return toRet;
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
                "Choose a commodity/resource. Each opponent gives you 1.");
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
            String resStr = json.get("resource").getAsString();
            _resource = Resource.stringToResource(resStr);
            _isSetup = true;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid resource selection");
        }
    }

    @Override
    public String getVerb() {
        return "choose a commodity (monopoly)";
    }
}
