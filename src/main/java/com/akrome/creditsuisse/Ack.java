package com.akrome.creditsuisse;

import java.io.Serializable;
import java.util.Optional;

public class Ack implements Serializable {
    private static Optional<Ack> optionalInstance = Optional.empty();
    private Ack(){}
    public static Ack getInstance() {
        if (!optionalInstance.isPresent()) {
            optionalInstance = Optional.of(new Ack());
        }
        return optionalInstance.get();
    }
}
