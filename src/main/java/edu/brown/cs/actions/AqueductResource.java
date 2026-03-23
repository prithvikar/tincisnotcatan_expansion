package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

/**
 * Follow-up action for the Science Level 3 (Aqueduct) ability. When a player
 * produces 0 resources on a dice roll, they may choose 1 free resource from
 * the bank.
 */
public class AqueductResource implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private Resource _resource;
    private boolean _isSetup;
    public static final String ID = "aqueductResource";

    public AqueductResource(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        Player player = _ref.getPlayerByID(_playerID);
        player.addResource(_resource, 1.0, _ref.getBank());

        _ref.removeFollowUp(this);

        Map<Integer, ActionResponse> toRet = new HashMap<>();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        String.format("Aqueduct: you received 1 %s!",
                                _resource),
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        String.format("%s used Aqueduct to gain a resource.",
                                player.getName()),
                        null));
            }
        }
        return toRet;
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
                "Aqueduct: you produced nothing. Choose 1 free resource.");
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
            if (_resource == Resource.WILDCARD) {
                throw new IllegalArgumentException(
                        "Cannot choose wildcard resource");
            }
            _isSetup = true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid resource selection");
        }
    }

    @Override
    public String getVerb() {
        return "choose a free resource (Aqueduct)";
    }
}
