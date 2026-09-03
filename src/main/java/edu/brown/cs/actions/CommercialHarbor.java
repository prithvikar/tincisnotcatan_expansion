package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonObject;

import edu.brown.cs.catan.Commodity;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;

public class CommercialHarbor implements FollowUpAction {

    public static final String ID = "commercialHarbor";

    private Referee _ref;
    private int _playerID;
    private Player _player;
    private boolean _isSetup;

    private boolean _giveCommodity; // true = give commodity/get resource; false = give 2 resources/get commodity
    private Resource _resource;
    private Commodity _commodity;

    public CommercialHarbor(int playerID) {
        _playerID = playerID;
        _isSetup = false;
    }

    @Override
    public Map<Integer, ActionResponse> execute() {
        if (!_isSetup) {
            throw new UnsupportedOperationException("Action must be setup.");
        }

        if (_giveCommodity) {
            if (_player.getCommodities().getOrDefault(_commodity, 0.0) < 1.0) {
                return java.util.Collections.singletonMap(_playerID, new ActionResponse(false,
                        "You do not have a " + _commodity.toString().toLowerCase() + " to trade.", null));
            }
            Resource expectedResource = getResourceFromCommodity(_commodity);
            if (_resource != expectedResource) {
                return java.util.Collections.singletonMap(_playerID, new ActionResponse(false,
                        "Invalid exchange pair chosen.", null));
            }
            _player.removeCommodity(_commodity, 1.0);
            _player.addResource(_resource, 1.0, _ref.getBank());
        } else {
            if (_player.getResources().getOrDefault(_resource, 0.0) < 2.0) {
                return java.util.Collections.singletonMap(_playerID, new ActionResponse(false,
                        "You do not have two " + _resource.toString().toLowerCase() + "s to trade.", null));
            }
            Commodity expectedCommodity = Commodity.fromResource(_resource);
            if (_commodity != expectedCommodity) {
                return java.util.Collections.singletonMap(_playerID, new ActionResponse(false,
                        "Invalid exchange pair chosen.", null));
            }
            _player.removeResource(_resource, 2.0, _ref.getBank());
            _player.addCommodity(_commodity, 1.0);
        }

        _ref.removeFollowUp(this);

        Map<Integer, ActionResponse> toRet = new HashMap<>();
        String msg = "Commercial Harbor exchange complete.";
        toRet.put(_playerID, new ActionResponse(true, msg, null));
        for (Player p : _ref.getPlayers()) {
            if (p.getID() != _playerID) {
                toRet.put(p.getID(), new ActionResponse(true,
                        _player.getName() + " used Commercial Harbor to trade.", null));
            }
        }
        return toRet;
    }

    private Resource getResourceFromCommodity(Commodity c) {
        switch (c) {
            case PAPER:
                return Resource.WOOD;
            case CLOTH:
                return Resource.SHEEP;
            case COIN:
                return Resource.ORE;
            default:
                return null;
        }
    }

    @Override
    public JsonObject getData() {
        JsonObject json = new JsonObject();
        json.addProperty("message",
                "Choose to trade 1 commodity for its resource or 2 resources for its commodity.");
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
    public void setupAction(Referee ref, int playerID, JsonObject params) {
        if (playerID != _playerID) {
            throw new IllegalArgumentException();
        }
        assert ref != null;
        _ref = ref;
        _player = _ref.getPlayerByID(playerID);
        if (_player == null) {
            throw new IllegalArgumentException(
                    String.format("No player exists with the id: %d", playerID));
        }
        if (ref.currentPlayer() != null && ref.currentPlayer().getID() != playerID) {
            throw new IllegalArgumentException();
        }

        if (params.has("giveCommodity") && params.has("resource") && params.has("commodity")) {
            _giveCommodity = params.get("giveCommodity").getAsBoolean();
            _resource = Resource.stringToResource(params.get("resource").getAsString());
            _commodity = Commodity.valueOf(params.get("commodity").getAsString().toUpperCase());

            // Validate simple mismatch input
            if (_resource == null || _resource == Resource.WILDCARD || _commodity == null) {
                throw new IllegalArgumentException("Invalid resource or commodity specified.");
            }

            _isSetup = true;
        } else {
            throw new IllegalArgumentException("Missing parameters for Commercial Harbor action");
        }
    }

    @Override
    public String getVerb() {
        return "use Commercial Harbor";
    }

}
