package com.akrome.creditsuisse;

import akka.actor.ActorSystem;
import com.akrome.creditsuisse.orders.OrderType;
import com.akrome.creditsuisse.routes.OrderRoute;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class OrderTest {
    static ActorSystem system;
    static HttpUtils httpUtils;

    @BeforeClass
    public static void setup() {
        Program p = new Program();
        p.run(null);
        system = p.getSystem();
        httpUtils = new HttpUtils(system);
    }

    @Test
    public void order_post_delete_test() throws InterruptedException, ExecutionException, IOException {
        String userId = "userId";
        int qtyInGrams = 112;
        int priceInPence = 145;
        OrderType orderType = OrderType.BUY;

        OrderRoute.CreateOrderBean cob = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence, orderType);
        String postResponse = httpUtils.post("http://localhost:8080/order", cob);

        String orderId = postResponse.substring("Order Created with ID: ".length());

        OrderBoardRetrieverActor.Board board = getBoard();
        TreeMap<Integer, BigInteger> buyMap = board.buyBoard;
        TreeMap<Integer, BigInteger> sellMap = board.sellBoard;

        TreeMap<Integer, BigInteger> expectedBuyMap = new TreeMap<>(Comparator.reverseOrder());
        expectedBuyMap.put(priceInPence, BigInteger.valueOf(qtyInGrams));

        TreeMap<Integer, BigInteger> expectedSellMap = new TreeMap<>(Comparator.naturalOrder());

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);

        httpUtils.delete("http://localhost:8080/order/"+orderId);
        expectedBuyMap.clear();
        expectedSellMap.clear();
        board = getBoard();
        buyMap = board.buyBoard;
        sellMap = board.sellBoard;

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);
    }

    @Test
    public void verify_grouping_by_price() throws InterruptedException, ExecutionException, IOException {
        String userId = "userId";
        int qtyInGrams = 112;
        int priceInPence = 145;
        OrderType orderType = OrderType.BUY;

        OrderRoute.CreateOrderBean cob = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence, orderType);
        String postResponse1 = httpUtils.post("http://localhost:8080/order", cob);
        String orderId1 = postResponse1.substring("Order Created with ID: ".length());

        String postResponse2 = httpUtils.post("http://localhost:8080/order", cob);
        String orderId2 = postResponse2.substring("Order Created with ID: ".length());

        OrderBoardRetrieverActor.Board board = getBoard();
        TreeMap<Integer, BigInteger> buyMap = board.buyBoard;
        TreeMap<Integer, BigInteger> sellMap = board.sellBoard;

        TreeMap<Integer, BigInteger> expectedBuyMap = new TreeMap<>(Comparator.reverseOrder());
        expectedBuyMap.put(priceInPence, BigInteger.valueOf(2*qtyInGrams));

        TreeMap<Integer, BigInteger> expectedSellMap = new TreeMap<>(Comparator.naturalOrder());

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);

        httpUtils.delete("http://localhost:8080/order/"+orderId1);
        httpUtils.delete("http://localhost:8080/order/"+orderId2);
        expectedBuyMap.clear();
        expectedSellMap.clear();
        board = getBoard();
        buyMap = board.buyBoard;
        sellMap = board.sellBoard;

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);
    }

    @Test
    public void verify_sorting_by_price() throws InterruptedException, ExecutionException, IOException {
        String userId = "userId";
        int qtyInGrams = 112;
        int priceInPence = 145;

        OrderRoute.CreateOrderBean cobBuy1 = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence, OrderType.BUY);
        OrderRoute.CreateOrderBean cobBuy2 = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence+1, OrderType.BUY);
        OrderRoute.CreateOrderBean cobSell1 = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence, OrderType.SELL);
        OrderRoute.CreateOrderBean cobSell2 = new OrderRoute.CreateOrderBean(userId, qtyInGrams, priceInPence+1, OrderType.SELL);

        String buyOrderId1 = httpUtils.post("http://localhost:8080/order", cobBuy1).substring("Order Created with ID: ".length());
        String buyOrderId2 = httpUtils.post("http://localhost:8080/order", cobBuy2).substring("Order Created with ID: ".length());
        String sellOrderId1 = httpUtils.post("http://localhost:8080/order", cobSell1).substring("Order Created with ID: ".length());
        String sellOrderId2 = httpUtils.post("http://localhost:8080/order", cobSell2).substring("Order Created with ID: ".length());

        OrderBoardRetrieverActor.Board board = getBoard();
        TreeMap<Integer, BigInteger> buyMap = board.buyBoard;
        TreeMap<Integer, BigInteger> sellMap = board.sellBoard;

        assertEquals(buyMap.size(), 2);
        Integer previous = buyMap.firstKey();
        for (Integer price: buyMap.keySet()) {
            assertTrue(previous >= price);
            previous = price;
        }
        assertEquals(sellMap.size(), 2);
        previous = sellMap.firstKey();
        for (Integer price: sellMap.keySet()) {
            assertTrue(previous <= price);
            previous = price;
        }

        TreeMap<Integer, BigInteger> expectedBuyMap = new TreeMap<>(Comparator.reverseOrder());
        expectedBuyMap.put(priceInPence, BigInteger.valueOf(qtyInGrams));
        expectedBuyMap.put(priceInPence+1, BigInteger.valueOf(qtyInGrams));

        TreeMap<Integer, BigInteger> expectedSellMap = new TreeMap<>(Comparator.naturalOrder());
        expectedSellMap.put(priceInPence, BigInteger.valueOf(qtyInGrams));
        expectedSellMap.put(priceInPence+1, BigInteger.valueOf(qtyInGrams));

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);

        httpUtils.delete("http://localhost:8080/order/"+buyOrderId1);
        httpUtils.delete("http://localhost:8080/order/"+buyOrderId2);
        httpUtils.delete("http://localhost:8080/order/"+sellOrderId1);
        httpUtils.delete("http://localhost:8080/order/"+sellOrderId2);
        expectedBuyMap.clear();
        expectedSellMap.clear();
        board = getBoard();
        buyMap = board.buyBoard;
        sellMap = board.sellBoard;

        assertEquals(expectedBuyMap, buyMap);
        assertEquals(expectedSellMap, sellMap);
    }

    OrderBoardRetrieverActor.Board getBoard() throws InterruptedException, ExecutionException, IOException {
        return httpUtils.get("http://localhost:8080/board", OrderBoardRetrieverActor.Board.class);
    }
}
