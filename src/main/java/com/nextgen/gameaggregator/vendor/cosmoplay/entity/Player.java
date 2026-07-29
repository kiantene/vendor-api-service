package com.nextgen.gameaggregator.vendor.cosmoplay.entity;

import com.google.gson.Gson;
import com.nextgen.core.exception.SignatureValidationException;
import com.nextgen.gameaggregator.exception.InternalServerException;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Data
public class Player {
    private static final String SEPARATOR = "-";
    private String id;
    private String error;
    private String partnerCode;

    public Player(String error) {
        this.error = error;
        this.id = "";
        this.partnerCode = "";
    }

    public Player(String playerId, String partnerCode) {
        this.id = playerId;
        this.error = null;
        this.partnerCode = partnerCode;
    }

    public static Player fromValidator(String seed) throws SignatureValidationException {
        String[] parts = Player.validated(seed, SignatureValidationException::new);

        return new Player(parts[1], parts[0]);
    }

    public static Player of(String seed) {
        try {
            String[] parts = Player.validated(seed, InternalServerException::new);

            return new Player(parts[1], parts[0]);
        } catch (InternalServerException e) {
            return new Player(e.getMessage());
        }
    }

    private static <E extends Throwable> String[] validated(String seed, Function<String, E> exceptionSupplier) throws E {
        try {
            String[] parts = Player.parse(seed);

            if (parts.length != 2) {
                throw exceptionSupplier.apply("Invalid seed length: " + seed);
            }

            if (parts[0].isBlank() || parts[1].isBlank()) {
                throw exceptionSupplier.apply("player and partner are required: " + seed);
            }

            return parts;
        } catch (IllegalArgumentException e) {
            throw exceptionSupplier.apply(e.getMessage());
        }
    }

    // --- Format: partnerCode-PlayerID
    //             The partner code comes from the vendor's credentials.
    //             @see Credentials
    private static String[] parse(String seed) {
        String[] parts = seed.split(SEPARATOR);

        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid playerID length: " + seed);
        }

        String partnerCode = parts[0];
        String playerID = parts[1];

        if (playerID.isBlank()) {
            throw new IllegalArgumentException("Invalid playerID: " + seed);
        }

        if (partnerCode.isBlank()) {
            throw new IllegalArgumentException("Invalid partnerID: " + seed);
        }

        return new String[]{
                partnerCode.trim(),
                playerID.trim()
        };
    }

    public Boolean hasError() {
        return this.error != null;
    }

    public String getVendorID() {
        return this.partnerCode + SEPARATOR + this.id;
    }

    public String toJson() {
        Map<String, String> params = new HashMap<>();

        params.put("partnerCode", this.partnerCode);
        params.put("playerId", this.id);

        return new Gson().toJson(params);
    }
}
