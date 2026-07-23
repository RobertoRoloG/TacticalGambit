package com.tacticalgambit.web.dto;

public record PieceDTO(
    String type,
    String color,
    boolean isShielded,
    boolean hasJumpModifier
) {
    public String toJson() {
        return String.format("{\"type\":\"%s\",\"color\":\"%s\",\"isShielded\":%b,\"hasJumpModifier\":%b}",
            type, color, isShielded, hasJumpModifier);
    }
}
