
package com.akrome.creditsuisse.routes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.pattern.Patterns;
import com.akrome.creditsuisse.OrderBoardRetrieverActor;
import com.akrome.creditsuisse.orders.Order;
import com.akrome.creditsuisse.orders.OrderActor;
import com.akrome.creditsuisse.orders.OrderRegionExtractor;
import com.akrome.creditsuisse.orders.OrderType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import scala.concurrent.Future;

import java.util.UUID;

import static akka.http.javadsl.server.PathMatchers.segment;
import static scala.compat.java8.FutureConverters.toJava;

public class OrderRoute extends AllDirectives {
    ActorSystem system;
    ActorRef orderRegion;

    ObjectMapper sortedMapper;

    public OrderRoute(ActorSystem system) {
        this.system = system;
        orderRegion = ClusterSharding.get(this.system).shardRegion(OrderRegionExtractor.REGION_NAME);
        sortedMapper = new ObjectMapper();
        sortedMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, false);
    }

    public Route getRoute() {
        return route(
                get(() -> route(
                    path("board", this::handleGetBoard),
                    path(segment("order").slash(segment()), this::handleGetOrder))
                ),
                post (() ->
                    path("order", () ->
                        entity(Jackson.unmarshaller(CreateOrderBean.class), this::handlePostOrder
                        )
                    )
                ),
                delete(() ->
                        path(segment("order").slash(segment()), this::handleDeleteOrder)
                )

        );
    }

    private Route handleGetBoard() {
        ActorRef orderBoardRetriever = system.actorOf(Props.create(OrderBoardRetrieverActor.class));
        Future future = Patterns.ask(orderBoardRetriever, new OrderBoardRetrieverActor.GetBoard(), 5000);
        return completeOKWithFuture(future, Jackson.marshaller(sortedMapper));
    }

    private Route handleGetOrder(String orderId) {
        Future future = Patterns.ask(orderRegion, new OrderActor.GetOrder(UUID.fromString(orderId)), 5000);
        return completeOKWithFuture(future, Jackson.marshaller(sortedMapper));
    }

    private Route handlePostOrder(CreateOrderBean cob) {
        Order order = fromCreateOrderBean(cob);
        Future future = Patterns.ask(orderRegion, new OrderActor.PostOrder(order), 5000);
        return onSuccess(() -> toJava(future), x -> complete("Order Created with ID: "+order.orderId));
    }

    private Route handleDeleteOrder(String orderId) {
        Future future = Patterns.ask(orderRegion, new OrderActor.DeleteOrder(UUID.fromString(orderId)), 5000);
        return onSuccess(() -> toJava(future), x -> complete("Order deleted with ID: "+orderId));
    }

    public static class CreateOrderBean {
        @JsonProperty
        public final String userId;
        @JsonProperty
        public final int qtyInGrams;
        @JsonProperty
        public final int priceInPence;
        @JsonProperty
        public final OrderType orderType;

        @JsonCreator
        public CreateOrderBean(
                @JsonProperty("userId") String userId,
                @JsonProperty("qtyInGrams") int qtyInGrams,
                @JsonProperty("priceInPence") int priceInPence,
                @JsonProperty("orderType") OrderType orderType) {
            this.userId = userId;
            this.qtyInGrams = qtyInGrams;
            this.priceInPence = priceInPence;
            this.orderType = orderType;
        }
    }

    static Order fromCreateOrderBean(CreateOrderBean cob) {
        return new Order(
                UUID.randomUUID(),
                cob.userId,
                cob.qtyInGrams,
                cob.priceInPence,
                cob.orderType);
    }
}
