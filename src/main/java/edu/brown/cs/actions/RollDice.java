package edu.brown.cs.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PrimitiveIterator;
import java.util.Random;

import com.google.gson.JsonObject;

import edu.brown.cs.board.Intersection;
import edu.brown.cs.board.Tile;
import edu.brown.cs.catan.CityImprovement;
import edu.brown.cs.catan.KnightPiece;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Player;
import edu.brown.cs.catan.ProgressCard;
import edu.brown.cs.catan.Referee;
import edu.brown.cs.catan.Resource;
import edu.brown.cs.catan.Settings;

/**
 * Action responsible for rolling the dice.
 * 
 * @author anselvahle
 *
 */
public class RollDice implements FollowUpAction {

  private Player _player;
  private final int _playerID;
  private Referee _ref;
  private static final String VERB = "start the next turn.";
  private static final String ID = "rollDice";
  private boolean _isSetUp = false;

  public RollDice(Referee ref, int playerID) {
    assert ref != null;
    _ref = ref;
    _playerID = playerID;
    _player = _ref.getPlayerByID(playerID);
    if (_player == null) {
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (!ref.currentPlayer().equals(_player)) {
      throw new IllegalArgumentException();
    }
    _isSetUp = true;
  }

  public RollDice(int playerID) {
    _playerID = playerID;
  }

  @Override
  public Map<Integer, ActionResponse> execute() {
    if (!_isSetUp) {
      throw new UnsupportedOperationException(
          "A FollowUpAction must be setup before executed.");
    }
    Random r = new Random();
    PrimitiveIterator.OfInt rolls = r.ints(1, 7).iterator();
    int redDie = rolls.nextInt();
    int whiteDie = rolls.nextInt();
    int diceRoll = redDie + whiteDie;
    _ref.getGameStats().addRoll(diceRoll);
    Map<Integer, Map<Resource, Integer>> playerResourceCount = new HashMap<>();
    Map<Integer, ActionResponse> toRet = new HashMap<>();

    if (diceRoll != 7) {
      Collection<Tile> tiles = _ref.getBoard().getTiles();
      // Iterate through tiles on the board
      for (Tile t : tiles) {
        // If the tile matches the roll and does not have the robber
        if (t.getRollNumber() == diceRoll && !t.hasRobber()) {
          // Find out who should collect what from the intersections
          Map<Integer, Map<Resource, Integer>> fromTile = t
              .notifyIntersections();
          // Iterate through this and consolidate collections for each person
          for (int playerID : fromTile.keySet()) {
            if (!playerResourceCount.containsKey(playerID)) {
              playerResourceCount.put(playerID,
                  new HashMap<Resource, Integer>());
            }
            Map<Resource, Integer> resourceCount = fromTile.get(playerID);
            Map<Resource, Integer> playerCount = playerResourceCount
                .get(playerID);
            for (Resource res : resourceCount.keySet()) {
              if (playerCount.containsKey(res)) {
                // Update the count
                playerCount.replace(res,
                    playerCount.get(res) + resourceCount.get(res));
              } else {
                playerCount.put(res, resourceCount.get(res));
              }
              // Make sure the player collects the resource
              _ref.getPlayerByID(playerID).addResource(res,
                  resourceCount.get(res), _ref.getBank());
            }
          }
        }
      }
      for (Integer playerID : playerResourceCount.keySet()) {
        StringBuilder message = new StringBuilder();
        message.append(String.format("%d was rolled", diceRoll));
        Map<Resource, Integer> resourceCount = playerResourceCount
            .get(playerID);
        for (Resource res : resourceCount.keySet()) {
          switch (res) {
            case WHEAT:
              message.append(String.format(", you received %d wheat",
                  resourceCount.get(res)));
              break;
            case SHEEP:
              message.append(String.format(", you received %d sheep",
                  resourceCount.get(res)));
              break;
            case ORE:
              message.append(String.format(", you received %d ore",
                  resourceCount.get(res)));
              break;
            case BRICK:
              message.append(String.format(", you received %d brick",
                  resourceCount.get(res)));
              break;
            case WOOD:
              message.append(String.format(", you received %d wood",
                  resourceCount.get(res)));
              break;
            default:
              message.append(".");
              break;
          }
        }
        message.append(".");
        ActionResponse toAdd = new ActionResponse(true, message.toString(),
            resourceCount);
        toRet.put(playerID, toAdd);
      }
      for (Player p : _ref.getPlayers()) {
        if (!toRet.containsKey(p.getID())) {
          ActionResponse toAdd = new ActionResponse(true, String.format(
              "%d was rolled.", diceRoll), new HashMap<Resource, Integer>());
          toRet.put(p.getID(), toAdd);
        }
      }
    } else {
      // 7 is rolled:
      Map<Integer, Double> playersToDrop = new HashMap<>();
      Map<Integer, JsonObject> jsonToSend = new HashMap<>();
      String message = "7 was rolled.";
      for (Player p : _ref.getPlayers()) {
        // C&K: city walls increase the hand limit by 2 each
        double threshold = Settings.DROP_CARDS_THRESH;
        if (_ref.getGameSettings().isCitiesAndKnights) {
          threshold += p.getCityWallCount() * Settings.CITY_WALL_HAND_BONUS;
        }
        if (p.getNumResourceCards() > threshold) {
          double numToDrop = p.getNumResourceCards() / 2.0;
          if (!_ref.getGameSettings().isDecimal) {
            numToDrop = Math.floor(numToDrop);
          }
          playersToDrop.put(p.getID(), numToDrop);
          message += String.format(" %s must discard cards", p.getName());
          JsonObject jsonForPlayer = new JsonObject();
          jsonForPlayer.addProperty("numToDrop", numToDrop);
          jsonToSend.put(p.getID(), jsonForPlayer);
        }
      }
      if (playersToDrop.size() > 0) {
        Collection<FollowUpAction> followUps = new ArrayList<>();
        message += ".";
        for (Player p : _ref.getPlayers()) {
          if (playersToDrop.containsKey(p.getID())) {
            followUps
                .add(new DropCards(p.getID(), playersToDrop.get(p.getID())));
            toRet.put(p.getID(),
                new ActionResponse(true,
                    "7 was rolled. You must drop half of your cards.",
                    jsonToSend.get(p.getID())));
          } else {
            toRet.put(p.getID(), new ActionResponse(true, message, null));
          }
        }
        _ref.addFollowUp(followUps);
      } else {
        ActionResponse respToAll = new ActionResponse(true,
            "7 was rolled. No one has more than 7 cards.", null);
        ActionResponse respToPlayer = new ActionResponse(true,
            "7 was rolled. You get to move the Robber.", null);
        for (Player p : _ref.getPlayers()) {
          if (p.equals(_player)) {
            toRet.put(p.getID(), respToPlayer);
          } else {
            toRet.put(p.getID(), respToAll);
          }
        }
      }
      // Follow up MoveRobber action:
      Collection<FollowUpAction> followUps = new ArrayList<>();
      followUps.add(new MoveRobber(_player.getID()));
      _ref.addFollowUp(followUps);
    }

    // --- Cities & Knights Event Die Logic ---
    if (_ref.getGameSettings().isCitiesAndKnights) {
      MasterReferee mr = (MasterReferee) _ref;
      // Roll the event die (1-3: Ship, 4-6: City Gate)
      int eventRoll = r.nextInt(6) + 1;
      String eventDie = "";
      String gateName = ""; // For message display

      // Barbarian ship moves on 1, 2, 3
      if (eventRoll <= 3) {
        eventDie = "ship";
        boolean attack = mr.getBarbarianTrack().advance();
        String msg = " Event: Barbarian ship advanced!";
        if (attack) {
          // --- Barbarian Attack Resolution ---
          int totalKnightStrength = 0;
          int totalBuiltCities = 0;
          for (Player p : _ref.getPlayers()) {
            totalKnightStrength += p.getActiveKnightStrength();
            int builtCities = Settings.INITIAL_CITIES - p.numCities();
            totalBuiltCities += builtCities;
          }

          if (totalKnightStrength >= totalBuiltCities) {
            // --- Knights Win: Defender of Catan ---
            msg += " Knights defended Catan!";
            int maxStrength = 0;
            for (Player p : _ref.getPlayers()) {
              int str = p.getActiveKnightStrength();
              if (str > maxStrength) {
                maxStrength = str;
              }
            }
            if (maxStrength > 0) {
              for (Player p : _ref.getPlayers()) {
                if (p.getActiveKnightStrength() == maxStrength) {
                  p.addDefenderPoint();
                  msg += String.format(" %s earns Defender of Catan!", p.getName());
                }
              }
            }
          } else {
            // --- Knights Lose: Pillage weakest contributor's city ---
            msg += " Barbarians pillage!";
            // Find lowest knight strength among players with at least 1 built city
            int minStrength = Integer.MAX_VALUE;
            for (Player p : _ref.getPlayers()) {
              int builtCities = Settings.INITIAL_CITIES - p.numCities();
              if (builtCities > 0) {
                int str = p.getActiveKnightStrength();
                if (str < minStrength) {
                  minStrength = str;
                }
              }
            }
            // Pillage one city from each player with the lowest strength
            for (Player p : _ref.getPlayers()) {
              int builtCities = Settings.INITIAL_CITIES - p.numCities();
              if (builtCities > 0 && p.getActiveKnightStrength() == minStrength) {
                // Demote one city on the board to a settlement
                for (Intersection inter : mr.getBoard().getIntersections().values()) {
                  if (inter.demoteToSettlement(p)) {
                    // Return a city piece and consume a settlement piece
                    // (useCity adds back a settlement, so we undo that by
                    // just adjusting the counts directly — but the simplest
                    // approach: the player effectively gets a city piece back
                    // and loses a settlement piece)
                    msg += String.format(" %s's city was pillaged!", p.getName());
                    // Remove a city wall if the player has one
                    if (p.getCityWallCount() > 0) {
                      p.removeCityWall();
                      msg += String.format(" %s lost a city wall.", p.getName());
                    }
                    break; // Only pillage one city per player
                  }
                }
              }
            }
          }

          // Deactivate all knights after the attack
          for (Player p : _ref.getPlayers()) {
            for (KnightPiece k : p.getKnights()) {
              if (k.isActive()) {
                k.deactivate();
              }
            }
          }
        }
        for (Integer pid : toRet.keySet()) {
          ActionResponse orig = toRet.get(pid);
          // Merge data
          Object existingData = orig.getData();
          JsonObject data = new JsonObject();
          if (existingData instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) existingData;
            for (Map.Entry<?, ?> e : m.entrySet()) {
              data.addProperty(e.getKey().toString(), e.getValue().toString());
            }
          } else if (existingData instanceof JsonObject) {
            JsonObject old = (JsonObject) existingData;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : old.entrySet()) {
              data.add(entry.getKey(), entry.getValue());
            }
          }
          data.addProperty("eventDie", "ship");
          data.addProperty("barbarianPosition", mr.getBarbarianTrack().getPosition());
          data.addProperty("redDie", redDie);
          data.addProperty("whiteDie", whiteDie);

          toRet.put(pid, new ActionResponse(orig.getSuccess(),
              orig.getMessage() + msg, data));
        }
      } else {
        // City Gate (Green/Blue/Yellow) on 4, 5, 6
        CityImprovement.Track matchTrack;
        ProgressCard.Category category;
        if (eventRoll == 4) {
          eventDie = "green"; // Trade
          category = ProgressCard.Category.TRADE;
          gateName = "Trade (green)";
          matchTrack = CityImprovement.Track.TRADE;
        } else if (eventRoll == 5) {
          eventDie = "blue"; // Politics
          category = ProgressCard.Category.POLITICS;
          gateName = "Politics (blue)";
          matchTrack = CityImprovement.Track.POLITICS;
        } else {
          eventDie = "yellow"; // Science
          category = ProgressCard.Category.SCIENCE;
          gateName = "Science (yellow)";
          matchTrack = CityImprovement.Track.SCIENCE;
        }

        // Use the red die value (already rolled)
        // If a player's improvement level on the matching track >= redDie,
        // they draw a progress card.
        String gateMsg = String.format(" Event: %s gate (%d).", gateName,
            redDie);
        for (Player p : _ref.getPlayers()) {
          int level = p.getCityImprovement().getLevel(matchTrack);
          if (level >= redDie) {
            ProgressCard drawn = mr.drawProgressCard(category);
            if (drawn != null) {
              p.addProgressCard(drawn);
              // If it's a VP card, reveal immediately
              if (drawn.isVictoryPoint()) {
                p.addVictoryPoint();
              }
            }
          }
        }
        // Append event die info to all players' messages
        for (Integer pid : toRet.keySet()) {
          ActionResponse orig = toRet.get(pid);
          // Merge data
          Object existingData = orig.getData();
          JsonObject data = new JsonObject();
          if (existingData instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) existingData;
            for (Map.Entry<?, ?> e : m.entrySet()) {
              data.addProperty(e.getKey().toString(), e.getValue().toString());
            }
          } else if (existingData instanceof JsonObject) {
            JsonObject old = (JsonObject) existingData;
            for (Map.Entry<String, com.google.gson.JsonElement> entry : old.entrySet()) {
              data.add(entry.getKey(), entry.getValue());
            }
          }
          data.addProperty("eventDie", eventDie);
          data.addProperty("redDie", redDie);
          data.addProperty("whiteDie", whiteDie);

          toRet.put(pid, new ActionResponse(orig.getSuccess(),
              orig.getMessage() + gateMsg, data));
        }
      }
    }

    _ref.removeFollowUp(this);
    return toRet;
  }

  @Override
  public JsonObject getData() {
    JsonObject toRet = new JsonObject();
    toRet.addProperty("message", "Please roll the dice");
    return toRet;
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
      String err = String.format("No player exists with the id: %d", playerID);
      throw new IllegalArgumentException(err);
    }
    if (!ref.currentPlayer().equals(_player)) {
      throw new IllegalArgumentException();
    }
    _isSetUp = true;
  }

  @Override
  public String getVerb() {
    return VERB;
  }
}
