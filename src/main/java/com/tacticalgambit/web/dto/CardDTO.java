package com.tacticalgambit.web.dto;

public record CardDTO(
    int index,
    String name,
    int apCost
) {
    public String toJson() {
        return String.format("{\"index\":%d,\"name\":\"%s\",\"apCost\":%d}",
            index, name, apCost);
    }
}
