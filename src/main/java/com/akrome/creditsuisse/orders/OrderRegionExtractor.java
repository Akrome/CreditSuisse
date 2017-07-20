package com.akrome.creditsuisse.orders;

import akka.cluster.sharding.ShardRegion;

public class OrderRegionExtractor implements ShardRegion.MessageExtractor {
    public static final String REGION_NAME = "orders";

    @Override
    public String entityId(Object message) {
        if (message instanceof OrderActor.HasEntityId) {
            OrderActor.HasEntityId msg = (OrderActor.HasEntityId) message;
            return msg.getEntityId();
        }
        else {
            return null;
        }
    }

    @Override
    public Object entityMessage(Object message) {
        if (message instanceof OrderActor.HasEntityId) {
            return message;
        }
        else {
            return null;
        }
    }

    final static int NUMBER_OF_SHARDS = 10;
    @Override
    public String shardId(Object message) {
        if (message instanceof OrderActor.HasEntityId) {
            return (entityId(message).hashCode() % NUMBER_OF_SHARDS)+"";
        }
        else {
            return null;
        }
    }
}
