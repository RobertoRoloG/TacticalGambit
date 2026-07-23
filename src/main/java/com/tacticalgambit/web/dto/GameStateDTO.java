package com.tacticalgambit.web.dto;

import java.util.List;
import java.util.Map;

public record GameStateDTO(
    String activePlayer,
    int actionPoints,
    boolean hasMovedPiece,
    String gameState,
    boolean isInCheck,
    List<CardDTO> hand,
    Map<String, PieceDTO> board,
    Map<String, Integer> barricades,
    List<String> actionLogs,
    int deckSize
) {
    public String toJson() {
        StringBuilder handJson = new StringBuilder("[");
        for (int i = 0; i < hand.size(); i++) {
            if (i > 0) handJson.append(",");
            handJson.append(hand.get(i).toJson());
        }
        handJson.append("]");

        StringBuilder boardJson = new StringBuilder("{");
        int count = 0;
        for (var entry : board.entrySet()) {
            if (count > 0) boardJson.append(",");
            boardJson.append("\"").append(entry.getKey()).append("\":").append(entry.getValue().toJson());
            count++;
        }
        boardJson.append("}");

        StringBuilder barricadesJson = new StringBuilder("{");
        int bCount = 0;
        for (var entry : barricades.entrySet()) {
            if (bCount > 0) barricadesJson.append(",");
            barricadesJson.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            bCount++;
        }
        barricadesJson.append("}");

        StringBuilder logsJson = new StringBuilder("[");
        for (int i = 0; i < actionLogs.size(); i++) {
            if (i > 0) logsJson.append(",");
            String escaped = actionLogs.get(i) == null ? "" : actionLogs.get(i).replace("\"", "\\\"");
            logsJson.append("\"").append(escaped).append("\"");
        }
        logsJson.append("]");

        return String.format("{\"activePlayer\":\"%s\",\"actionPoints\":%d,\"hasMovedPiece\":%b,\"gameState\":\"%s\",\"isInCheck\":%b,\"hand\":%s,\"board\":%s,\"barricades\":%s,\"actionLogs\":%s,\"deckSize\":%d}",
            activePlayer, actionPoints, hasMovedPiece, gameState, isInCheck, handJson, boardJson, barricadesJson, logsJson, deckSize);
    }
}
