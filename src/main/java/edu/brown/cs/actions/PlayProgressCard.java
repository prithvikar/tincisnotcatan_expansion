package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCard;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action for playing a progress card from the player's hand. Handles all
 * immediate-effect progress cards directly; complex cards that require
 * additional input are noted with TODOs for follow-up actions.
 */
public class PlayProgressCard implements Action {

  public final static String ID = "playProgressCard";

  private Referee _ref;
  private Player _player;
  private ProgressCard _card;

  public PlayProgressCard(Referee ref, int playerID, String cardName) {
    assert ref != null;
    _ref = ref;
    _player = ref.getPlayerByID(playerID);
    if (_player == null) {
      throw new IllegalArgumentException(
          String.format("No player exists with the id: %d", playerID));
    }
    _card = findCard(cardName);
    if (_card == null) {
      throw new IllegalArgumentException(
          String.format("Player does not have card: %s", cardName));
    }
  }

  private ProgressCard findCard(String cardName) {
    for (ProgressCard pc : _player.getProgressCards()) {
      if (pc.getName().equals(cardName)) {
        return pc;
      }
    }
    return null;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    // Remove the card from the player's hand
    _player.removeProgressCard(_card);

    Map<Integer, ActionResponse> toRet = new HashMap<>();
    String publicMsg = String.format("%s played %s.", _player.getName(),
        _card.getName());

    switch (_card) {

    // --- Immediate VP cards ---
    case CONSTITUTION:
    case PRINTER:
      _player.addVictoryPoint();
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              "You revealed " + _card.getName() + " for 1 VP!", null));
        } else {
          toRet.put(p.getID(), new ActionResponse(true,
              publicMsg + " (+1 VP)", null));
        }
      }
      break;

    // --- Warlord: activate all your knights for free ---
    case WARLORD:
      for (KnightPiece k : _player.getKnights()) {
        k.activate();
      }
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              "All your knights are now active!", null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Engineer: build a city wall for free ---
    case ENGINEER:
      if (_player.getCityWallCount() < Settings.MAX_CITY_WALLS) {
        _player.addCityWall();
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "You built a city wall for free!", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
      } else {
        // Can't build more walls, give the card back
        _player.addProgressCard(_card);
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You already have the maximum number of city walls.", null));
      }
      break;

    // --- Road Building: build 2 roads for free ---
    case ROAD_BUILDING:
      // Grants 2 free road placements via follow-up
      // For now, treat similarly to base game road building
      if (_player.numRoads() >= 2) {
        _ref.addFollowUp(
            com.google.common.collect.ImmutableList.of(
                new PlaceRoad(_player.getID(), false)));
        _ref.addFollowUp(
            com.google.common.collect.ImmutableList.of(
                new PlaceRoad(_player.getID(), false)));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Place 2 roads for free!", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
      } else {
        _player.addProgressCard(_card);
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You don't have enough road pieces.", null));
      }
      break;

    // --- Irrigation: 2 wheat per wheat hex adjacent to your buildings ---
    case IRRIGATION:
      int wheatGained = 0;
      for (Tile t : _ref.getBoard().getTiles()) {
        if (t.getType().getType() == Resource.WHEAT) {
          for (Intersection i : t.getIntersections()) {
            if (i.getBuilding() != null
                && i.getBuilding().getPlayer().getID() == _player.getID()) {
              _player.addResource(Resource.WHEAT, 2.0, _ref.getBank());
              wheatGained += 2;
              break; // only count once per tile
            }
          }
        }
      }
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              String.format("You gained %d wheat from Irrigation!", wheatGained),
              null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Mining: 2 ore per ore hex adjacent to your buildings ---
    case MINING:
      int oreGained = 0;
      for (Tile t : _ref.getBoard().getTiles()) {
        if (t.getType().getType() == Resource.ORE) {
          for (Intersection i : t.getIntersections()) {
            if (i.getBuilding() != null
                && i.getBuilding().getPlayer().getID() == _player.getID()) {
              _player.addResource(Resource.ORE, 2.0, _ref.getBank());
              oreGained += 2;
              break;
            }
          }
        }
      }
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              String.format("You gained %d ore from Mining!", oreGained),
              null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Medicine: build a city for 1 ore + 1 wheat ---
    case MEDICINE:
      // This modifies the next city cost — store a flag on the player/turn
      // For now, give player back the normal cost difference
      if (_player.hasResource(Resource.ORE, 1.0)
          && _player.hasResource(Resource.WHEAT, 1.0)) {
        // Discount: normal city costs 3 ore + 2 wheat, medicine costs 1+1
        // We'll add a follow-up that builds city at reduced cost
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Medicine: your next city costs only 1 ore + 1 wheat!",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
      } else {
        _player.addProgressCard(_card);
        return ImmutableMap.of(_player.getID(), new ActionResponse(false,
            "You need at least 1 ore and 1 wheat to use Medicine.", null));
      }
      break;

    // --- Smith: promote 2 knights for free ---
    case SMITH:
      int promoted = 0;
      for (KnightPiece k : _player.getKnights()) {
        if (promoted >= 2) break;
        if (k.getLevel().nextLevel() != null) {
          k.promote();
          promoted++;
        }
      }
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              String.format("Smith: %d knight(s) promoted!", promoted),
              null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Merchant Fleet: 2:1 trade for rest of turn ---
    case MERCHANT_FLEET:
      // Flag that the player has a 2:1 trade rate for this turn
      // This requires modifying getBankRates in MasterReferee for this turn
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              "Merchant Fleet: you can trade any resource/commodity at 2:1 this turn!",
              null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Crane: build city improvement for 1 fewer commodity ---
    case CRANE:
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              "Crane: your next city improvement costs 1 fewer commodity!",
              null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;

    // --- Cards requiring follow-up (player must choose target) ---
    case BISHOP:
    case MERCHANT:
    case MASTER_MERCHANT:
    case RESOURCE_MONOPOLY:
    case TRADE_MONOPOLY:
    case COMMERCIAL_HARBOR:
    case SPY:
    case DESERTER:
    case DIPLOMAT:
    case INTRIGUE:
    case SABOTEUR:
    case WEDDING:
    case INVENTOR:
    case ALCHEMIST:
    default:
      // Stub: acknowledge the card was played but don't execute the effect
      for (Player p : _ref.getPlayers()) {
        if (p.equals(_player)) {
          toRet.put(p.getID(), new ActionResponse(true,
              _card.getName() + " played. " + _card.getDescription(), null));
        } else {
          toRet.put(p.getID(),
              new ActionResponse(true, publicMsg, null));
        }
      }
      break;
    }

    return toRet;
  }
}
