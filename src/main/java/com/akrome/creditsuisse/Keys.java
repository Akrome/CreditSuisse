package com.akrome.creditsuisse;

import akka.cluster.ddata.Key;
import akka.cluster.ddata.PNCounterMap;
import akka.cluster.ddata.PNCounterMapKey;
import com.akrome.creditsuisse.orders.Order;

public class Keys {
    public static final Key<PNCounterMap<Integer>> BUY = PNCounterMapKey.create("BUY");
    public static final Key<PNCounterMap<Integer>> SELL = PNCounterMapKey.create("SELL");

    public static Key<PNCounterMap<Integer>> getKeyForOrder(Order order) {
        switch(order.orderType) {
            case BUY: return Keys.BUY;
            case SELL: return Keys.SELL;
            default: return null;
        }
    }
}
