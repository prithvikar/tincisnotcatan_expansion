# Cities & Knights Expansion - Implementation Status

This document outlines the features implemented for the *Cities & Knights* expansion of *Tinc Is Not Catan*, including backend mechanics, frontend UI components, and remaining work items.

## Completed Features

### 1. Core Data Models & Mechanics
*   **New Resources (Commodities):** Added Paper (Forest/Wood), Coin (Mountain/Ore), and Cloth (Pasture/Sheep) as distinct resource types.
*   **City Improvements:** Implemented 3 improvement tracks (Trade, Politics, Science) with 5 levels each.
    *   Upgrade logic requires corresponding commodities.
    *   Level 3 unlocks special abilities (Trade: 2:1 Commodity Trade, Politics: Promote Strong Knights, Science: Promote Mighty Knights).
*   **Knights:**
    *   **Types:** Basic (1 strength), Strong (2 strength), Mighty (3 strength).
    *   **Actions:** Place Knight, Activate Knight (costs 1 grain), Promote Knight (costs 1 wool + 1 ore/improvement ability), Move/Displace Knight.
    *   **Status:** Knights can be active or inactive. Active knights contribute to barbarian defense.
*   **Barbarian Invasion:** 
    *   Implemented a 7-step invasion track.
    *   Event Die (Ship result) advances the barbarian ship.
    *   Attack trigger logic exists (ship reaches end of track).
*   **City Walls:**
    *   Implemented backend logic to build walls (costs 2 brick).
    *   Increases hand limit by 2 cards per wall (up to +6 cards).
*   **Metropolises:**
    *   **Claiming:** First player to reach Level 4 in a track claims the Metropolis (2 VPs).
    *   **Stealing:** Players can steal ownership by reaching Level 5 before the current owner does.
    *   **Visuals:** Frontend renders colored markers (Green/Blue/Yellow) on cities with metropolises.
*   **Merchant:**
    *   Implemented logic for the Merchant piece (placed via relevant Progress Cards).
    *   Bonus: Grants 2:1 trade on the resource of the hex it resides on.
    *   Bonus: Worth 1 Victory Point to the owner.

### 2. Progress Cards
*   **Decks:** 3 separate decks for Trade (Green), Politics (Blue), and Science (Yellow).
*   **Drawing Logic:**
    *   Event Die rolls determine card distribution.
    *   Players draw if the Event Die (Gate) matches their improvement track color AND their improvement level >= the Red Die roll.
*   **Hand Management:**
    *   Players can hold up to 4 progress cards (5 if they have `Printer/Constitution` VP cards - effectively hidden).
    *   **UI:** Dedicated progress card panel in the player tab showing available cards as interactive buttons.
*   **Implemented Card Actions:**
    *   **VP Cards:** Constitution, Printer (1 VP, revealed immediately).
    *   **Instant Effects:**
        *   *Warlord:* Activates all knights for free.
        *   *Engineer:* Builds a city wall for free.
        *   *Road Building:* Builds 2 roads for free.
        *   *Smith:* Promotes 2 knights for free.
        *   *Medicine:* Build city for 2 ore/1 grain.
        *   *Crane:* City improvement discount.
        *   *Irrigation:* Collect wheat for adjacent fields.
        *   *Mining:* Collect ore for adjacent mountains.
        *   *Merchant Fleet:* 2:1 resource/commodity trade for turn.

### 3. Frontend Integration
*   **Event Die Display:**
    *   The dice roll area now displays the Event Die result:
        *   **Ship:** Indicates barbarian movement.
        *   **Green/Blue/Yellow Gate:** Indicates potential progress card draw.
*   **Progress Card UI:**
    *   Players view their hand in a new "Progress" section.
    *   Clicking a card button triggers the action.
*   **Metropolis Markers:** Visual indicators on the board show which cities have metropolises.

## Pending Work

### 1. Complex Progress Cards
The following cards require interactive follow-up actions (e.g., selecting a player, hex, or resource) and are partially implemented or require UI flows:
*   **Politics:** *Spy* (look at hand/steal), *Diplomat* (remove road), *Intrigue* (displace knight), *Deserter* (remove opponent knight), *Saboteur* (force discard), *Wedding* (collect resources).
*   **Trade:** *Commercial Harbor* (force trade), *Master Merchant* (steal from hand), *Resource Monopoly*, *Trade Monopoly*.
*   **Science:** *Alchemist* (set dice roll), *Inventor* (swap number tokens).

### 2. Full Barbarian Resolution
*   While the track and attack trigger exist, the resolution logic for **pillaging cities** (reducing a city to a settlement if knights lose) is not fully automated.
*   Defender of Catan VP awards (for highest knight strength contribution) need full integration.

### 3. Testing
*   **Integration Tests:** Needed for full barbarian attack cycles.
*   **Manual Playtesting:** Verify UI responsiveness and edge cases in a live browser session.

## Technical Notes
*   **State Management:** `MasterReferee` now initializes C&K components only when `_gameSettings.isCitiesAndKnights` is true.
*   **Serialization:** `CatanConverter` has been updated to include `metropolis`, `barbarian_position`, `knight_strength`, etc., in the WebSocket JSON payloads.
*   **Unit Tests:** See `src/test/java/edu/brown/cs/catan/MetropolisTest.java` for verification of new mechanics.
