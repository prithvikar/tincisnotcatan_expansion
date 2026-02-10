package edu.brown.cs.catan;

/**
 * Tracks the barbarian ship's progress toward Catan in the Cities & Knights
 * expansion. Each time the event die shows a ship icon, the barbarian advances.
 * When the ship reaches the end of the track, an attack is triggered and the
 * track resets.
 */
public class BarbarianTrack {

  public static final int TRACK_LENGTH = 7;

  private int _position;
  private int _attackCount;

  /**
   * Creates a new BarbarianTrack with the ship at position 0.
   */
  public BarbarianTrack() {
    _position = 0;
    _attackCount = 0;
  }

  /**
   * Advances the barbarian ship by one position.
   *
   * @return true if the barbarians have reached the island and an attack is
   *         triggered.
   */
  public boolean advance() {
    _position++;
    if (_position >= TRACK_LENGTH) {
      _position = 0;
      _attackCount++;
      return true; // Attack triggered
    }
    return false;
  }

  /**
   * Returns the current position of the barbarian ship (0 to TRACK_LENGTH-1).
   */
  public int getPosition() {
    return _position;
  }

  /**
   * Returns the number of barbarian attacks that have occurred so far.
   */
  public int getAttackCount() {
    return _attackCount;
  }

  /**
   * Returns how many more advances until the next attack.
   */
  public int getDistanceToAttack() {
    return TRACK_LENGTH - _position;
  }

  /**
   * Resets the track to position 0 (used after an attack, though advance()
   * handles this automatically).
   */
  public void reset() {
    _position = 0;
  }

  @Override
  public String toString() {
    return String.format("BarbarianTrack[position=%d/%d, attacks=%d]",
        _position, TRACK_LENGTH, _attackCount);
  }

}
