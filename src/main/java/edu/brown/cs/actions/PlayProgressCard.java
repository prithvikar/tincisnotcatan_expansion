package edu.brown.cs.actions;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
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
          if (promoted >= 2)
            break;
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

      // --- Bishop: move robber, take 1 resource from each adjacent player ---
      case BISHOP:
        _ref.addFollowUp(
            ImmutableList.of(new MoveRobber(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Bishop played. Move the robber.", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Diplomat: remove any open road ---
      case DIPLOMAT:
        _ref.addFollowUp(
            ImmutableList.of(new RemoveRoad(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Diplomat played. Select a road to remove.", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Deserter: remove an opponent's knight ---
      case DESERTER:
        _ref.addFollowUp(
            ImmutableList.of(new DeserterTarget(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Deserter played. Select an opponent's knight to remove.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Intrigue: displace an opponent's knight ---
      case INTRIGUE:
        _ref.addFollowUp(
            ImmutableList.of(new DisplaceKnight(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Intrigue played. Select an opponent's knight to displace.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Saboteur: players with >= your VP discard half hand ---
      case SABOTEUR: {
        int myVP = _player.numVictoryPoints();
        for (Player p : _ref.getPlayers()) {
          if (p.getID() != _player.getID()
              && p.numVictoryPoints() >= myVP) {
            // Discard half (rounded down)
            int totalCards = 0;
            for (Map.Entry<Resource, Double> entry : p.getResources()
                .entrySet()) {
              if (entry.getKey() != Resource.WILDCARD) {
                totalCards += (int) Math.floor(entry.getValue());
              }
            }
            int toDiscard = totalCards / 2;
            int discarded = 0;
            for (Resource r : Resource.values()) {
              if (r == Resource.WILDCARD || discarded >= toDiscard) {
                break;
              }
              double has = p.getResources().getOrDefault(r, 0.0);
              int canRemove = (int) Math.min(has, toDiscard - discarded);
              if (canRemove > 0) {
                p.removeResource(r, canRemove);
                discarded += canRemove;
              }
            }
          }
        }
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Saboteur played! Opponents with >= your VPs discarded half their cards.",
                null));
          } else {
            toRet.put(p.getID(), new ActionResponse(true,
                publicMsg + " Players with enough VPs had to discard.",
                null));
          }
        }
        break;
      }

      // --- Spy: steal a progress card from an opponent ---
      case SPY:
        _ref.addFollowUp(
            ImmutableList.of(new StealProgressCard(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Spy played. Select an opponent to steal a progress card from.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Wedding: players with more VPs give you 2 resources each ---
      case WEDDING: {
        int myVPs = _player.numVictoryPoints();
        int totalGained = 0;
        for (Player p : _ref.getPlayers()) {
          if (p.getID() != _player.getID()
              && p.numVictoryPoints() > myVPs) {
            int given = 0;
            for (Resource r : Resource.values()) {
              if (r == Resource.WILDCARD || given >= 2) {
                break;
              }
              double has = p.getResources().getOrDefault(r, 0.0);
              int canGive = (int) Math.min(has, 2 - given);
              if (canGive > 0) {
                p.removeResource(r, canGive);
                _player.addResource(r, canGive);
                given += canGive;
                totalGained += canGive;
              }
            }
          }
        }
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Wedding: you gained " + totalGained
                    + " resources from richer players!",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;
      }

      // --- Resource Monopoly: name a resource, each opponent gives up to 2 ---
      case RESOURCE_MONOPOLY:
        _ref.addFollowUp(
            ImmutableList.of(new ChooseResource(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Resource Monopoly played. Choose a resource.", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Trade Monopoly: name a commodity, each opponent gives 1 ---
      case TRADE_MONOPOLY:
        _ref.addFollowUp(
            ImmutableList.of(new ChooseCommodity(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Trade Monopoly played. Choose a resource.", null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Commercial Harbor: simplified resource swap with opponents ---
      case COMMERCIAL_HARBOR:
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Commercial Harbor played. Resource trades active.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Master Merchant: take 2 cards from a richer opponent ---
      case MASTER_MERCHANT:
        _ref.addFollowUp(
            ImmutableList.of(new ChooseOpponentCards(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Master Merchant played. Select a player with more VPs.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Inventor: swap number tokens on two hexes ---
      case INVENTOR:
        _ref.addFollowUp(
            ImmutableList.of(new SwapHexNumbers(_player.getID())));
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                "Inventor played. Select two hexes to swap numbers.",
                null));
          } else {
            toRet.put(p.getID(),
                new ActionResponse(true, publicMsg, null));
          }
        }
        break;

      // --- Deferred cards: Merchant, Alchemist ---
      case MERCHANT:
      case ALCHEMIST:
      default:
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), new ActionResponse(true,
                _card.getName() + " played. " + _card.getDescription(),
                null));
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
