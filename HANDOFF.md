# Cities & Knights — Implementation Handoff

**Date:** 2026-02-25
**Status:** Backend architecture complete. Several backend cards are broken stubs. City improvement level abilities are entirely missing. Frontend C&K UI is partially wired.

---

## Project Context

Java/Spark backend + jQuery frontend. WebSocket-driven game state. All files live under:
- Backend: `src/main/java/edu/brown/cs/`
- Frontend: `src/main/resources/static/js/` and `src/main/resources/spark/template/freemarker/`

Build: `mvn package` → `./run` or `./bin/run`
Entry: `edu.brown.cs.catan.Main`

---

## What Is Already Working

| Feature | Key Files |
|---------|-----------|
| Barbarian track advance + reset | `BarbarianTrack.java`, `RollDice.java:218-326` |
| Barbarian attack resolution (pillage, Defender of Catan VP) | `RollDice.java:228-301` |
| All knights (place, activate, promote, displace, deserter) | `PlaceKnight`, `ActivateKnight`, `PromoteKnight`, `DisplaceKnight`, `DeserterTarget` |
| City improvements + metropolis claim/steal | `ImproveCityTrack.java`, `MasterReferee.java:363` |
| City walls + hand limit on 7-roll | `BuildCityWall.java`, `RollDice.java:165` |
| Merchant (PlaceMerchant follow-up fully wired) | `PlaceMerchant.java`, `MasterReferee.java:129` |
| Event die (ship → barbarian, gates → progress cards) | `RollDice.java:218-386` |
| Progress card draw rule (level >= redDie) | `RollDice.java:355` |
| 16 of 20 progress cards fully working | `PlayProgressCard.java` |
| All 10 follow-up actions (backend + frontend) | `actions/`, `main.js:1321-1640` |
| VP calculation (defender, metropolis, merchant, no Largest Army in C&K) | `MasterReferee.java:325-396` |
| Progress card deck sizes correct | `ProgressCard.java:143-190` |

---

## Gaps — Prioritized Implementation Order

---

### GAP 1 — Medicine (Science card) is a no-op
**Priority: High | Effort: Small**

**Problem:**
`PlayProgressCard.java:199-222` sends a success message but sets no state. `BuildCity.java` uses `HumanPlayer.canBuildCity()` which always checks `Settings.CITY_COST` (3 ore + 2 wheat). Discount is never applied.

**Implementation:**

1. Add to `Turn.java`:
```java
private boolean _medicineDiscountActive = false;
public boolean hasMedicineDiscount() { return _medicineDiscountActive; }
public void setMedicineDiscount(boolean v) { _medicineDiscountActive = v; }
```

2. Add to `Settings.java`:
```java
public static final Map<Resource, Double> MEDICINE_CITY_COST = ImmutableMap.of(
    Resource.ORE, 1.0, Resource.WHEAT, 1.0
);
```

3. In `PlayProgressCard.java` case `MEDICINE`:
   After the existing resource check, add: `_ref.getTurn().setMedicineDiscount(true);`
   Add a follow-up `PlaceCity` (or just gate the next `buildCity` call via flag — see step 4).

4. In `BuildCity.java:execute()`, before `_player.canBuildCity()`:
```java
if (_ref.getTurn().hasMedicineDiscount()) {
    // Check and pay discounted cost
    if (!_player.hasResource(Resource.ORE, 1.0) || !_player.hasResource(Resource.WHEAT, 1.0)) {
        return ImmutableMap.of(_player.getID(), new ActionResponse(false, "Need 1 ore + 1 wheat for Medicine city.", null));
    }
    _player.removeResource(Resource.ORE, 1.0);
    _player.removeResource(Resource.WHEAT, 1.0);
    _player.useCity();
    _intersection.placeCity(_player);
    _ref.getTurn().setMedicineDiscount(false);
    // build response and return
}
```

5. In `EndTurn.java:execute()`: `_ref.getTurn().setMedicineDiscount(false);`

---

### GAP 2 — Crane (Science card) is a no-op
**Priority: High | Effort: Small**

**Problem:**
`PlayProgressCard.java:264-275` sends a message only. `ImproveCityTrack.java:75` calls `getCostToAdvance()` with no discount.

**Implementation:**

1. Add to `Turn.java`:
```java
private boolean _craneDiscountActive = false;
public boolean hasCraneDiscount() { return _craneDiscountActive; }
public void setCraneDiscount(boolean v) { _craneDiscountActive = v; }
```

2. In `PlayProgressCard.java` case `CRANE`:
   Add: `_ref.getTurn().setCraneDiscount(true);`

3. In `ImproveCityTrack.java:execute()`, where cost is calculated:
```java
int cost = improvement.getCostToAdvance(_track);
if (_ref.getTurn().hasCraneDiscount()) {
    cost = Math.max(0, cost - 1);
    _ref.getTurn().setCraneDiscount(false);
}
```

4. In `EndTurn.java:execute()`: `_ref.getTurn().setCraneDiscount(false);`

---

### GAP 3 — Merchant Fleet (Trade card) is a no-op
**Priority: High | Effort: Small**

**Problem:**
`PlayProgressCard.java:248-261` sends a message only. `TradeWithBank.java` never applies a 2:1 override.

**Implementation:**

1. Add to `Turn.java`:
```java
private boolean _merchantFleetActive = false;
public boolean hasMerchantFleet() { return _merchantFleetActive; }
public void setMerchantFleet(boolean v) { _merchantFleetActive = v; }
```

2. In `PlayProgressCard.java` case `MERCHANT_FLEET`:
   Add: `_ref.getTurn().setMerchantFleet(true);`

3. In `TradeWithBank.java:execute()`, before the rate lookup:
```java
if (_ref.getTurn().hasMerchantFleet()) {
    // 2:1 for everything — skip rate check, just require 2 of the offered resource
    if (_offered < 2.0) {
        return ImmutableMap.of(/* error */);
    }
    // execute the trade at 2:1
}
```

4. In `EndTurn.java:execute()`: `_ref.getTurn().setMerchantFleet(false);`

---

### GAP 4 — Progress card hand limit not enforced
**Priority: High | Effort: Small**

**Problem:**
`HumanPlayer.addProgressCard()` has no cap. `RollDice.java` adds cards unconditionally.

**Official rule:** Max 4 progress cards. (Constitution and Printer are revealed immediately so they don't count.)

**Implementation:**

1. Add to `Settings.java`:
```java
public static final int MAX_PROGRESS_CARDS = 4;
```

2. In `HumanPlayer.java:addProgressCard()`:
```java
public void addProgressCard(ProgressCard card) {
    if (progressCards.size() >= Settings.MAX_PROGRESS_CARDS) {
        return; // or throw, or discard oldest
    }
    progressCards.add(card);
}
```

3. In `RollDice.java` where cards are drawn (around line 355), check before adding:
```java
if (player.getProgressCards().size() < Settings.MAX_PROGRESS_CARDS) {
    player.addProgressCard(drawn);
}
```

---

### GAP 5 — Alchemist does not control the event die
**Priority: Medium | Effort: Small**

**Problem:**
`ChooseDice.java` overrides production dice only. `RollDice.java:218` always generates a random event roll regardless.

**Official rule:** Alchemist lets you set all dice including the event die.

**Implementation:**

1. Add `_eventDie` field to `ChooseDice.java`. Update `getData()` to request it from the player. Update `setupAction()` to parse it (1–6).

2. Expand `Referee.setOverriddenDice()` / `consumeOverriddenDice()` to carry a 3rd value:
```java
void setOverriddenDice(int red, int white, int event);
int[] consumeOverriddenDice(); // returns [red, white, event]
```

3. In `RollDice.java:218`, replace:
```java
int eventRoll = r.nextInt(6) + 1;
```
with:
```java
int[] override = _ref.consumeOverriddenDice();
int redDie, whiteDie, eventRoll;
if (override != null) {
    redDie = override[0]; whiteDie = override[1]; eventRoll = override[2];
} else {
    redDie = rolls.nextInt(); whiteDie = rolls.nextInt();
    eventRoll = r.nextInt(6) + 1;
}
```

4. Update frontend `enterChooseDiceModal()` in `main.js` to add a third dropdown for the event die (Ship / Green Gate / Blue Gate / Yellow Gate).

---

### GAP 6 — City Improvement Level Abilities not implemented
**Priority: Medium | Effort: Medium**

None of the track level-specific bonuses are triggered anywhere.

**Official rules:**

| Track | Level | Ability |
|-------|-------|---------|
| Trade | 3 | This player may always trade any **commodity** at 2:1 with the bank |
| Politics | 3 | May promote a knight without paying wool (just ore) |
| Science | 3 | **Aqueduct**: if this player produces 0 resources on a roll, take any 1 resource from bank |

**Implementation:**

**Trade Level 3** — Modify `TradeWithBank.java`:
```java
// If offering a commodity and player has Trade level >= 3, apply 2:1
CityImprovement ci = _player.getCityImprovement();
if (ci != null && ci.getLevel(Track.TRADE) >= 3 && isCommodity(_offered)) {
    requiredAmount = 2.0;
}
```

**Politics Level 3** — Modify `PromoteKnight.java`:
If player's Politics level >= 3, allow promotion without wool (only ore cost).

**Science Level 3 (Aqueduct)** — Add to `RollDice.java` in the normal production path:
```java
// After distributing resources, check if player received 0 resources
if (totalReceived == 0 && _ref.getGameSettings().isCitiesAndKnights) {
    CityImprovement ci = player.getCityImprovement();
    if (ci != null && ci.getLevel(Track.SCIENCE) >= 3) {
        // Queue a follow-up for player to choose 1 free resource
        _ref.addFollowUp(ImmutableList.of(new ChooseResource(player.getID(), true /* aqueduct */)));
    }
}
```
`ChooseResource` will need an `isAqueduct` mode that gives 1 resource instead of taking from opponents.

---

### GAP 7 — Commercial Harbor (Trade card) is a complete stub
**Priority: Low | Effort: Large**

**Problem:**
`PlayProgressCard.java:466-476` sends a message with zero effect. No follow-up class exists. No frontend modal.

**Official rule:**
For each **commodity** a player gives to the bank, they receive the corresponding **resource** (Paper→Wood, Cloth→Sheep, Coin→Ore). OR for 2 of any resource, receive the corresponding commodity. Other players may be forced to participate.

**Implementation:**

1. Create `CommercialHarborAction.java` implementing `FollowUpAction`:
   - `getData()` returns available exchange options for the active player
   - `setupAction()` parses the chosen exchange direction + resource/commodity pair
   - `execute()` performs the bank swap for the active player, then optionally queues follow-ups for opponents

2. In `PlayProgressCard.java` case `COMMERCIAL_HARBOR`:
```java
_ref.addFollowUp(ImmutableList.of(new CommercialHarborAction(_player.getID())));
```

3. In `websocket.js`, add `"commercialHarbor"` case to the follow-up handler.

4. In `main.js`, add `enterCommercialHarborModal()`:
   - Show available commodity→resource swaps
   - Confirm sends `sendCommercialHarborAction(direction, resource)`

---

## In-Progress Frontend Work (uncommitted as of handoff)

The following frontend changes are **committed in this handoff but incomplete**:

| File | What changed | Status |
|------|-------------|--------|
| `board.ftl` | Barbarian track HTML (7 steps + ship token) | Layout done, JS animation incomplete |
| `main.css` | Barbarian track styles | Done |
| `intersection.js` | Knight rendering (SVG icons, active/inactive states) | Done |
| `intersection.js` | Click handlers for knight modes | Wired, needs `inPlaceKnightMode` global in `main.js` |
| `player.js` | Progress card buttons now show category icons | Done |
| `websocket.js` | Barbarian track position update in `handleGetGameState` | Done |
| `websocket.js` | `debugCK()` function for local testing | **Remove before production** |
| `board.ftl` | Red debug button calling `debugCK()` | **Remove before production** |
| `static/images/` | 10 new SVG icons (knights, commodities, progress categories, barbarian ship) | Done |

### Remaining Frontend TODOs

- **`main.js`**: `inPlaceKnightMode`, `inActivateKnightMode`, `inPromoteKnightMode` globals + `enterPlaceKnightMode()`, `enterActivateKnightMode()`, `enterPromoteKnightMode()` functions need to be confirmed wired (check if these already exist and align with `intersection.js` handlers)
- **`main.js`**: Add Merchant Fleet active indicator (e.g. banner or highlight on bank trade UI)
- **`main.js`**: Add Medicine/Crane discount active indicator in the build panel
- **`board.ftl` + `main.js`**: Remove `debugCK()` button before production deploy
- **`websocket.js`**: Barbarian track animation when ship advances (currently it just jumps)
- **`player.js`**: City improvement track levels should be visible in opponent tabs (currently only shown for self)

---

## Key File Map

```
src/main/java/edu/brown/cs/
├── actions/
│   ├── PlayProgressCard.java     ← All progress card logic (Gaps 1-3, 7)
│   ├── BuildCity.java            ← Needs Medicine discount check (Gap 1)
│   ├── ImproveCityTrack.java     ← Needs Crane discount check (Gap 2)
│   ├── TradeWithBank.java        ← Needs Merchant Fleet + Trade-3 rate (Gaps 3, 6)
│   ├── RollDice.java             ← Event die, Aqueduct, card draw (Gaps 5, 6)
│   ├── ChooseDice.java           ← Alchemist: add event die param (Gap 5)
│   ├── ChooseResource.java       ← Reuse for Aqueduct free resource (Gap 6)
│   ├── PromoteKnight.java        ← Needs Politics-3 wool waiver (Gap 6)
│   └── EndTurn.java              ← Clear all per-turn flags (Gaps 1-3)
├── catan/
│   ├── Turn.java                 ← Add medicine/crane/merchantFleet flags (Gaps 1-3)
│   ├── MasterReferee.java        ← C&K state (barbarian, metropolises, merchant, overridden dice)
│   ├── ProgressCard.java         ← Card enum + deck factories
│   ├── CityImprovement.java      ← Level tracking (no ability triggers yet)
│   ├── HumanPlayer.java          ← addProgressCard needs cap (Gap 4)
│   └── Settings.java             ← Add MAX_PROGRESS_CARDS, MEDICINE_CITY_COST (Gaps 1, 4)
src/main/resources/
├── static/js/
│   ├── main.js                   ← Knight mode globals, discount indicators, remove debug
│   ├── websocket.js              ← Barbarian track update, add commercialHarbor handler
│   ├── intersection.js           ← Knight rendering (done)
│   └── player.js                 ← Progress card icons (done)
└── spark/template/freemarker/
    └── board.ftl                 ← Remove debug button
```

---

## Test Coverage Needed

- `MasterRefereeTest.java` — add tests for Medicine/Crane/MerchantFleet flag lifecycle
- `BarbarianTrackTest.java` — already covers track advance; add Alchemist event die override
- `MetropolisTest.java` — already exists; extend for city improvement level abilities
- New: `ProgressCardTest.java` — hand limit enforcement, Commercial Harbor exchanges

---

## Build & Run

```bash
cd src/main/java/edu/brown/cs/tincisnotcatan
mvn package
./run --port=4567
```

Or use the `build.sh` script in the repo root.
