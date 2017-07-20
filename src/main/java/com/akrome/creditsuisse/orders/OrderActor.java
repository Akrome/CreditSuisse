package com.akrome.creditsuisse.orders;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.PNCounterMap;
import akka.cluster.ddata.Replicator;
import com.akrome.creditsuisse.Ack;
import com.akrome.creditsuisse.Keys;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

public class OrderActor extends AbstractActor {

    Optional<Order> optionalOrder = Optional.empty();

    ActorRef replicator = DistributedData.get(getContext().getSystem()).replicator();
    private final Cluster node = Cluster.get(context().system());

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetOrder.class, this::handleGetOrder)
                .match(PostOrder.class, this::handleRegisterOrder)
                .match(DeleteOrder.class, this::handleCancelOrder)
                .matchAny(msg -> unhandled(msg))
                .build();
    }

    void handleGetOrder(GetOrder getOrder) {
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();
            sender().tell(order, self());
        }
        else {
            sender().tell(NoData.getInstance(), self());
        }
    }

    void handleRegisterOrder(PostOrder postOrder) {
        Order order = postOrder.order;
        optionalOrder = Optional.of(order);
        sender().tell(Ack.getInstance(), self());

        Replicator.Update<PNCounterMap<Integer>> update = new Replicator.Update<>(
                Keys.getKeyForOrder(order),
                PNCounterMap.create(),
                Replicator.writeLocal(),
                curr -> curr.increment(node, order.priceInPence, order.qtyInGrams));
        replicator.tell(update, getSelf());
    }

    void handleCancelOrder(DeleteOrder deleteOrder) {
        if (optionalOrder.isPresent()) {
            Order order = optionalOrder.get();

            Replicator.Update<PNCounterMap<Integer>> update = new Replicator.Update<>(
                    Keys.getKeyForOrder(order),
                    PNCounterMap.create(),
                    Replicator.writeLocal(),
                    curr -> curr.decrement(node, order.priceInPence, order.qtyInGrams)
            );
            replicator.tell(update, getSelf());

            optionalOrder = Optional.empty();
            getContext().stop(getSelf());
        }
        sender().tell(Ack.getInstance(), self());
    }

    interface HasEntityId {
        String getEntityId();
    }

    public static class GetOrder implements HasEntityId, Serializable {
        public final UUID orderId;

        public GetOrder(UUID orderId) {
            this.orderId = orderId;
        }

        @Override
        public String getEntityId() {
            return orderId.toString();
        }
    }

    public static class PostOrder implements HasEntityId, Serializable {
        public final Order order;

        public PostOrder(Order order) {
            this.order = order;
        }

        @Override
        public String getEntityId() {
            return order.orderId.toString();
        }
    }

    public static class DeleteOrder implements HasEntityId, Serializable {
        public final UUID orderId;

        public DeleteOrder(UUID orderId) {
            this.orderId = orderId;
        }

        @Override
        public String getEntityId() {
            return orderId.toString();
        }
    }

    public static class NoData implements Serializable {
        @JsonProperty
        public final String message = "DATA NOT FOUND";

        private NoData(){}

        private static Optional<NoData> optionalInstance = Optional.empty();

        public static NoData getInstance() {
            if (!optionalInstance.isPresent()) {
                optionalInstance = Optional.of(new NoData());
            }
            return optionalInstance.get();
        }
    }
}
