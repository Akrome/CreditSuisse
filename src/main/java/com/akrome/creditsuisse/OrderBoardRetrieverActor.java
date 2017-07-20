package com.akrome.creditsuisse;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.ddata.DistributedData;
import akka.cluster.ddata.PNCounterMap;
import akka.cluster.ddata.Replicator;
import akka.pattern.Patterns;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OrderBoardRetrieverActor extends AbstractActor {

    ActorRef replicator;
    ExecutionContext dispatcher;

    public OrderBoardRetrieverActor() {
        replicator = DistributedData.get(getContext().system()).replicator();
        dispatcher = getContext().system().dispatcher();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(GetBoard.class, this::handleGetBoard)
                .matchAny(msg -> unhandled(msg))
                .build();
    }

    private void handleGetBoard(GetBoard getBoard) {
        Future futureBuy = Patterns.ask(replicator, new Replicator.Get<>(Keys.BUY, Replicator.readLocal()), 5000);
        Future futureSell = Patterns.ask(replicator, new Replicator.Get<>(Keys.SELL, Replicator.readLocal()), 5000);
        Future futureBuyMap = futureBuy.map(f -> {
            Map<Integer, BigInteger> map;
            if (f instanceof Replicator.GetSuccess) {
                Replicator.GetSuccess getSuccess = (Replicator.GetSuccess) f;
                PNCounterMap<Integer> pnCounterMap = (PNCounterMap<Integer>) getSuccess.dataValue();
                map = pnCounterMap.getEntries();
                map = map.entrySet().stream().filter(e -> !e.getValue().equals(BigInteger.ZERO)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            else {
                map = new HashMap<>();
            }
            return map;
        }, dispatcher);
        Future futureSellMap = futureSell.map(f -> {
            Map<Integer, BigInteger> map;
            if (f instanceof Replicator.GetSuccess) {
                Replicator.GetSuccess getSuccess = (Replicator.GetSuccess) f;
                PNCounterMap<Integer> pnCounterMap = (PNCounterMap<Integer>) getSuccess.dataValue();
                map = pnCounterMap.getEntries();
                map = map.entrySet().stream().filter(e -> !e.getValue().equals(BigInteger.ZERO)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
            else {
                map = new HashMap<>();
            }
            return map;
        }, dispatcher);
        Future futureBoard = futureBuyMap.zip(futureSellMap).map(t -> {
            Tuple2<Map<Integer, BigInteger>, Map<Integer, BigInteger>> tuple = (Tuple2<Map<Integer, BigInteger>, Map<Integer, BigInteger>>) t;
            return new Board(tuple._1(), tuple._2);
        }, dispatcher);
        Patterns.pipe(futureBoard, dispatcher).to(sender());
        getContext().stop(self());
    }

    public static class GetBoard implements Serializable{}
    public static class Board implements Serializable {
        @JsonProperty
        final TreeMap<Integer, BigInteger> buyBoard = new TreeMap<>(Comparator.reverseOrder());
        @JsonProperty
        final TreeMap<Integer, BigInteger> sellBoard = new TreeMap<>(Comparator.naturalOrder());

        @JsonCreator
        public Board(
                @JsonProperty("buyBoard") Map<Integer, BigInteger> buyBoard,
                @JsonProperty("sellBoard") Map<Integer, BigInteger> sellBoard
                ) {
            this.buyBoard.clear();
            this.buyBoard.putAll(buyBoard);
            this.sellBoard.clear();
            this.sellBoard.putAll(sellBoard);
        }
    }
}
