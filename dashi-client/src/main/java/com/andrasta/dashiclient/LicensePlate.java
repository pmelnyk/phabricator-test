package com.andrasta.dashiclient;

import android.support.annotation.NonNull;

import com.andrasta.dashi.utils.Preconditions;

import java.util.UUID;

public class LicensePlate {

    private UUID uuid;
    private String description;
    private String number;
    private Integer priority;

    public LicensePlate(@NonNull UUID uuid, @NonNull String number, String description,  Integer priority) {

        Preconditions.assertParameterNotNull(uuid, "uuid");
        Preconditions.assertParameterNotNull(number, "number");
        this.uuid = uuid;
        this.description = description;
        this.number = number;
        this.priority = priority;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDescription() {
        return description;
    }

    public String getNumber() {
        return number;
    }

    public Integer getPriority() {
        return priority;
    }

    public boolean matches(String plateNumber) {
        return number.equalsIgnoreCase(plateNumber);
    }
}
