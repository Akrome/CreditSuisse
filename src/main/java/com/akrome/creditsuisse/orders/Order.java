package com.akrome.creditsuisse.orders;

import java.io.Serializable;
import java.util.UUID;

public class Order implements Serializable {
    public final UUID orderId;
    public final String userId;
    public final int qtyInGrams;
    public final int priceInPence;
    public final OrderType orderType;

    public Order(UUID orderId, String userId, int qtyInGrams, int priceInPence, OrderType orderType) {
        this.orderId = orderId;
        this.userId = userId;
        this.qtyInGrams = qtyInGrams;
        this.priceInPence = priceInPence;
        this.orderType = orderType;
    }
}
