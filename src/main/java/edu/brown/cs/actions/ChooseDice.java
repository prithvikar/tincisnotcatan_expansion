package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;

/**
 * Follow-up action for the Alchemist progress card. The player chooses the
 * values of both dice (1-6 each) before the dice roll occurs.
 */
public class ChooseDice implements FollowUpAction {

    private Referee _ref;
    private int _playerID;
    private int _redDie;
    private int _whiteDie;
    private boolean _isSetup;
    public static final String ID = "chooseDice";

    public ChooseDice(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        _ref.setOverriddenDice(_redDie, _whiteDie);
        _ref.removeFollowUp(this);

        int total = _redDie + _whiteDie;
        Map<Integer, ActionResponse> toRet = new HashMap<>();
        String playerName = _ref.getPlayerByID(_playerID).getName();
        for (Player p : _ref.getPlayers()) {
            if (p.getID() == _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        String.format(
                                "Alchemist: you chose %d + %d = %d for your dice roll!",
                                _redDie, _whiteDie, total),
                        null));
            } else {
                toRet.put(p.getID(), new ActionResponse(true,
                        playerName + " used the Alchemist to choose their dice roll.",
                        null));
            }
        }
        return toRet;
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
                "Choose values for both dice (1-6 each).");
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
            _redDie = json.get("redDie").getAsInt();
            _whiteDie = json.get("whiteDie").getAsInt();
            if (_redDie < 1 || _redDie > 6 || _whiteDie < 1
                    || _whiteDie > 6) {
                throw new IllegalArgumentException("Dice values must be 1-6");
            }
            _isSetup = true;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid dice values");
        }
    }

    @Override
    public String getVerb() {
        return "choose dice values";
    }
}
