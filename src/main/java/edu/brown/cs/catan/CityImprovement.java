package edu.brown.cs.catan;

/**
 * Tracks a player's city improvement levels across the three development
 * tracks in the Cities & Knights expansion: Trade (Paper), Politics (Coin),
 * and Science (Cloth). Each track has levels 0–5, and reaching level 4 on a
 * track allows the player to claim the corresponding Metropolis (worth 2 VP).
 */
public class CityImprovement {

  /**
   * The three development tracks in Cities & Knights.
   */
  public enum Track {
    TRADE("trade", "green"), POLITICS("politics", "blue"), SCIENCE("science",
        "yellow");

    private final String _name;
    private final String _color;

    private Track(String name, String color) {
      _name = name;
      _color = color;
    }

    public String getName() {
      return _name;
    }

    public String getColor() {
      return _color;
    }

    /**
     * Returns the commodity used to advance this track.
     */
    public Commodity getCommodity() {
      switch (this) {
      case TRADE:
        return Commodity.PAPER;
      case POLITICS:
        return Commodity.COIN;
      case SCIENCE:
        return Commodity.CLOTH;
      default:
        throw new IllegalStateException("Unknown track.");
      }
    }

    /**
     * Converts a string to a Track enum.
     */
    public static Track fromString(String str) {
      switch (str.toLowerCase()) {
      case "trade":
        return TRADE;
      case "politics":
        return POLITICS;
      case "science":
        return SCIENCE;
      default:
        throw new IllegalArgumentException(
            String.format("Unknown track: %s", str));
      }
    }
  }

  public static final int MAX_LEVEL = 5;
  public static final int METROPOLIS_THRESHOLD = 4;

  // Commodity costs for each level (index 0 = cost to reach level 1, etc.)
  // Level 1 costs 1, Level 2 costs 2, Level 3 costs 3, Level 4 costs 4, Level 5 costs 5
  public static final int[] LEVEL_COSTS = { 1, 2, 3, 4, 5 };

  private int _tradeLevel;
  private int _politicsLevel;
  private int _scienceLevel;

  /**
   * Creates a new CityImprovement with all tracks at level 0.
   */
  public CityImprovement() {
    _tradeLevel = 0;
    _politicsLevel = 0;
    _scienceLevel = 0;
  }

  /**
   * Returns the current level for the given track.
   */
  public int getLevel(Track track) {
    switch (track) {
    case TRADE:
      return _tradeLevel;
    case POLITICS:
      return _politicsLevel;
    case SCIENCE:
      return _scienceLevel;
    default:
      throw new IllegalArgumentException("Unknown track.");
    }
  }

  /**
   * Advances the given track by one level.
   *
   * @param track
   *          The track to advance.
   * @throws IllegalStateException
   *           if the track is already at max level.
   */
  public void advance(Track track) {
    int current = getLevel(track);
    if (current >= MAX_LEVEL) {
      throw new IllegalStateException(
          String.format("Track %s is already at max level.", track.getName()));
    }
    switch (track) {
    case TRADE:
      _tradeLevel++;
      break;
    case POLITICS:
      _politicsLevel++;
      break;
    case SCIENCE:
      _scienceLevel++;
      break;
    default:
      break;
    }
  }

  /**
   * Returns the commodity cost to advance to the next level on the given track.
   *
   * @param track
   *          The track to check.
   * @return The number of commodities needed.
   */
  public int getCostToAdvance(Track track) {
    int current = getLevel(track);
    if (current >= MAX_LEVEL) {
      return -1; // Cannot advance further
    }
    return LEVEL_COSTS[current];
  }

  /**
   * Returns whether the given track has reached the metropolis threshold.
   */
  public boolean canClaimMetropolis(Track track) {
    return getLevel(track) >= METROPOLIS_THRESHOLD;
  }

  /**
   * Returns whether advancing this track requires having at least one city
   * (only levels above 0 have this requirement by default in C&K).
   */
  public boolean requiresCity(Track track) {
    return true; // All improvements require having at least one city
  }

  @Override
  public String toString() {
    return String.format("CityImprovement[Trade=%d, Politics=%d, Science=%d]",
        _tradeLevel, _politicsLevel, _scienceLevel);
  }

}
