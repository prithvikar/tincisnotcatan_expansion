package edu.brown.cs.catan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a Progress Card in the Cities & Knights expansion. Progress cards
 * replace the base game's Development Cards and are organized into three
 * categories: Trade (green), Politics (blue), and Science (yellow).
 */
public enum ProgressCard {

  // --- Trade (Green) cards ---
  COMMERCIAL_HARBOR("Commercial Harbor", Category.TRADE,
      "Force all players to trade a commodity of their choice for a commodity of your choice."),
  MASTER_MERCHANT("Master Merchant", Category.TRADE,
      "Look at the cards of a player with more VPs and take 2 resource or commodity cards."),
  MERCHANT("Merchant", Category.TRADE,
      "Place the Merchant on a land hex adjacent to your building. Trade that resource at 2:1 and gain 1 VP."),
  MERCHANT_FLEET("Merchant Fleet", Category.TRADE,
      "For the rest of the turn, trade 1 resource or commodity of your choice at 2:1 with the bank."),
  RESOURCE_MONOPOLY("Resource Monopoly", Category.TRADE,
      "Name a resource. Each player must give you 2 of that resource (if they have them)."),
  TRADE_MONOPOLY("Trade Monopoly", Category.TRADE,
      "Name a commodity. Each player must give you 1 of that commodity (if they have it)."),

  // --- Politics (Blue) cards ---
  BISHOP("Bishop", Category.POLITICS,
      "Move the robber. Take 1 resource from each player with a building adjacent to the robber's new hex."),
  CONSTITUTION("Constitution", Category.POLITICS,
      "1 Victory Point. Revealed immediately."),
  DESERTER("Deserter", Category.POLITICS,
      "Remove an opponent's knight from the board."),
  DIPLOMAT("Diplomat", Category.POLITICS,
      "Remove any open road from the board (yours or opponent's)."),
  INTRIGUE("Intrigue", Category.POLITICS,
      "Displace an opponent's knight adjacent to one of your buildings."),
  SABOTEUR("Saboteur", Category.POLITICS,
      "All players with equal or more VPs than you must discard half their hand (rounded down)."),
  SPY("Spy", Category.POLITICS,
      "Look at an opponent's progress cards and steal one."),
  WARLORD("Warlord", Category.POLITICS,
      "Activate all your knights for free."),
  WEDDING("Wedding", Category.POLITICS,
      "Each player with more VPs must give you 2 resource or commodity cards of their choice."),

  // --- Science (Yellow) cards ---
  ALCHEMIST("Alchemist", Category.SCIENCE,
      "Choose the result of your production and event dice rolls this turn."),
  CRANE("Crane", Category.SCIENCE,
      "Build a city improvement for 1 fewer commodity."),
  ENGINEER("Engineer", Category.SCIENCE,
      "Build a city wall for free."),
  INVENTOR("Inventor", Category.SCIENCE,
      "Swap the number tokens on two land hexes (excluding 2, 6, 8, 12)."),
  IRRIGATION("Irrigation", Category.SCIENCE,
      "Collect 2 wheat for each wheat hex adjacent to your buildings."),
  MEDICINE("Medicine", Category.SCIENCE,
      "Build a city for 1 ore and 1 wheat instead of the normal cost."),
  MINING("Mining", Category.SCIENCE,
      "Collect 2 ore for each ore hex adjacent to your buildings."),
  PRINTER("Printer", Category.SCIENCE,
      "1 Victory Point. Revealed immediately."),
  ROAD_BUILDING("Road Building", Category.SCIENCE,
      "Build 2 roads for free."),
  SMITH("Smith", Category.SCIENCE,
      "Promote 2 of your knights for free.");

  /**
   * The three categories of progress cards.
   */
  public enum Category {
    TRADE("trade", "green"), POLITICS("politics", "blue"), SCIENCE("science",
        "yellow");

    private final String _name;
    private final String _color;

    private Category(String name, String color) {
      _name = name;
      _color = color;
    }

    public String getName() {
      return _name;
    }

    public String getColor() {
      return _color;
    }
  }

  private final String _name;
  private final Category _category;
  private final String _description;

  private ProgressCard(String name, Category category, String description) {
    _name = name;
    _category = category;
    _description = description;
  }

  public String getName() {
    return _name;
  }

  public Category getCategory() {
    return _category;
  }

  public String getDescription() {
    return _description;
  }

  /**
   * Returns true if this progress card is an immediate VP (Constitution or
   * Printer).
   */
  public boolean isVictoryPoint() {
    return this == CONSTITUTION || this == PRINTER;
  }

  @Override
  public String toString() {
    return _name;
  }

  // --- Deck building ---

  // Official card counts per type:
  // Trade: Commercial Harbor x2, Master Merchant x2, Merchant x6,
  //        Merchant Fleet x2, Resource Monopoly x4, Trade Monopoly x2
  // Politics: Bishop x2, Constitution x1, Deserter x2, Diplomat x2,
  //           Intrigue x2, Saboteur x2, Spy x3, Warlord x2, Wedding x2
  // Science: Alchemist x2, Crane x2, Engineer x1, Inventor x1,
  //          Irrigation x2, Medicine x2, Mining x2, Printer x1,
  //          Road Building x2, Smith x2

  /**
   * Creates a shuffled deck of trade progress cards.
   */
  public static List<ProgressCard> createTradeDeck() {
    List<ProgressCard> deck = new ArrayList<>();
    addCards(deck, COMMERCIAL_HARBOR, 2);
    addCards(deck, MASTER_MERCHANT, 2);
    addCards(deck, MERCHANT, 6);
    addCards(deck, MERCHANT_FLEET, 2);
    addCards(deck, RESOURCE_MONOPOLY, 4);
    addCards(deck, TRADE_MONOPOLY, 2);
    Collections.shuffle(deck);
    return deck;
  }

  /**
   * Creates a shuffled deck of politics progress cards.
   */
  public static List<ProgressCard> createPoliticsDeck() {
    List<ProgressCard> deck = new ArrayList<>();
    addCards(deck, BISHOP, 2);
    addCards(deck, CONSTITUTION, 1);
    addCards(deck, DESERTER, 2);
    addCards(deck, DIPLOMAT, 2);
    addCards(deck, INTRIGUE, 2);
    addCards(deck, SABOTEUR, 2);
    addCards(deck, SPY, 3);
    addCards(deck, WARLORD, 2);
    addCards(deck, WEDDING, 2);
    Collections.shuffle(deck);
    return deck;
  }

  /**
   * Creates a shuffled deck of science progress cards.
   */
  public static List<ProgressCard> createScienceDeck() {
    List<ProgressCard> deck = new ArrayList<>();
    addCards(deck, ALCHEMIST, 2);
    addCards(deck, CRANE, 2);
    addCards(deck, ENGINEER, 1);
    addCards(deck, INVENTOR, 1);
    addCards(deck, IRRIGATION, 2);
    addCards(deck, MEDICINE, 2);
    addCards(deck, MINING, 2);
    addCards(deck, PRINTER, 1);
    addCards(deck, ROAD_BUILDING, 2);
    addCards(deck, SMITH, 2);
    Collections.shuffle(deck);
    return deck;
  }

  private static void addCards(List<ProgressCard> deck, ProgressCard card,
      int count) {
    for (int i = 0; i < count; i++) {
      deck.add(card);
    }
  }

}
