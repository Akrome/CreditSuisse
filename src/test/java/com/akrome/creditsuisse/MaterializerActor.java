package com.akrome.creditsuisse;

import akka.actor.AbstractActor;
import akka.japi.pf.ReceiveBuilder;
import akka.stream.ActorMaterializer;

public class MaterializerActor extends AbstractActor {
    public static ActorMaterializer materializer;

    public MaterializerActor() {
        materializer = ActorMaterializer.create(getContext());
    }

    @Override
    public Receive createReceive() {
        return ReceiveBuilder.create().matchAny(this::unhandled).build();
    }
}