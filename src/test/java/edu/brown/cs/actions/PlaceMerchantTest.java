package edu.brown.cs.actions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import com.google.gson.JsonObject;

import org.junit.Test;

import edu.brown.cs.board.HexCoordinate;
import edu.brown.cs.catan.MasterReferee;
import edu.brown.cs.catan.Referee;

public class PlaceMerchantTest {

    private Referee setupGame() {
        Referee ref = new MasterReferee();
        ref.addPlayer("Alice", "#000000");
        ref.addPlayer("Bob", "#ffffff");
        ref.startNextTurn();
        return ref;
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testExecuteWithoutSetup() {
        PlaceMerchant action = new PlaceMerchant(0);
        action.execute();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPlayerSetup() {
        Referee ref = setupGame();
        PlaceMerchant action = new PlaceMerchant(0);
        JsonObject params = makeParams(0, 0, 0);
        action.setupAction(ref, 1, params);
    }

    @Test
    public void testSetupValid() {
        Referee ref = setupGame();
        int p1 = 0;

        PlaceMerchant action = new PlaceMerchant(p1);
        ref.addFollowUp(Collections.singletonList(action));
        JsonObject params = makeParams(0, 0, 0);
        action.setupAction(ref, p1, params);

        assertEquals("placeMerchant", action.getID());
        assertEquals(p1, action.getPlayerID());
    }

    @Test
    public void testMerchantStateAfterSetMerchant() {
        Referee ref = setupGame();
        int p1 = 0;

        // Initially no merchant
        assertEquals(-1, ref.getMerchantOwner());
        assertNull(ref.getMerchantHex());

        // Set merchant directly on referee
        HexCoordinate hex = new HexCoordinate(0, 0, 0);
        ref.setMerchant(p1, hex);

        assertEquals(p1, ref.getMerchantOwner());
        assertEquals(hex, ref.getMerchantHex());
    }

    @Test
    public void testMerchantOwnerChangeOnSecondPlace() {
        Referee ref = setupGame();
        int p1 = 0, p2 = 1;

        HexCoordinate hex1 = new HexCoordinate(0, 0, 0);
        HexCoordinate hex2 = new HexCoordinate(1, -1, 0);

        ref.setMerchant(p1, hex1);
        assertEquals(p1, ref.getMerchantOwner());
        assertEquals(hex1, ref.getMerchantHex());

        // Player 2 takes over the merchant
        ref.setMerchant(p2, hex2);
        assertEquals(p2, ref.getMerchantOwner());
        assertEquals(hex2, ref.getMerchantHex());
    }

    @Test
    public void testGetVerb() {
        PlaceMerchant action = new PlaceMerchant(0);
        assertEquals("place the merchant", action.getVerb());
    }

    @Test
    public void testGetDataMessage() {
        PlaceMerchant action = new PlaceMerchant(0);
        JsonObject data = action.getData();
        assertTrue(data.has("message"));
        assertTrue(data.get("message").getAsString()
                .contains("Place the Merchant"));
    }

    private JsonObject makeParams(int x, int y, int z) {
        JsonObject hex = new JsonObject();
        hex.addProperty("x", x);
        hex.addProperty("y", y);
        hex.addProperty("z", z);
        JsonObject params = new JsonObject();
        params.add("hex", hex);
        return params;
    }
}
