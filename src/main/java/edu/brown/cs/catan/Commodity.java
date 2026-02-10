package edu.brown.cs.catan;

/**
 * Represents a Cities & Knights commodity. Commodities are produced by cities
 * adjacent to specific tile types and are used to advance city improvement
 * tracks.
 *
 * PAPER comes from Forest (Wood) tiles.
 * CLOTH comes from Pasture (Sheep) tiles.
 * COIN comes from Mountain (Ore) tiles.
 */
public enum Commodity {

  PAPER("paper", "a paper"), CLOTH("cloth", "a cloth"), COIN("coin", "a coin");

  private final String _description;
  private final String _withArticle;

  private Commodity(String description, String withArticle) {
    _description = description;
    _withArticle = withArticle;
  }

  @Override
  public String toString() {
    return _description;
  }

  public String stringWithArticle() {
    return _withArticle;
  }

  /**
   * Converts a string to a Commodity enum.
   *
   * @param str
   *          The string to convert.
   * @return The corresponding Commodity.
   */
  public static Commodity stringToCommodity(String str) {
    switch (str) {
    case "paper":
      return PAPER;
    case "cloth":
      return CLOTH;
    case "coin":
      return COIN;
    default:
      throw new IllegalArgumentException(
          String.format("The commodity %s does not exist.", str));
    }
  }

  /**
   * Returns the commodity associated with a given resource type. Only Wood,
   * Sheep, and Ore produce commodities.
   *
   * @param res
   *          The resource type.
   * @return The associated Commodity, or null if no commodity is associated.
   */
  public static Commodity fromResource(Resource res) {
    switch (res) {
    case WOOD:
      return PAPER;
    case SHEEP:
      return CLOTH;
    case ORE:
      return COIN;
    default:
      return null;
    }
  }

}
