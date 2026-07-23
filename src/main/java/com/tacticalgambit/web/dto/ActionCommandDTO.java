package com.tacticalgambit.web.dto;

public record ActionCommandDTO(
    String type,
    String from,
    String to,
    Integer cardIndex,
    String targetSquare,
    String targetSquare2,
    String promoType
) {
    public static ActionCommandDTO parseJson(String json) {
        String type = getJsonValue(json, "type");
        String from = getJsonValue(json, "from");
        String to = getJsonValue(json, "to");
        String cardIndexStr = getJsonValue(json, "cardIndex");
        Integer cardIndex = cardIndexStr == null ? null : Integer.parseInt(cardIndexStr);
        String targetSquare = getJsonValue(json, "targetSquare");
        String targetSquare2 = getJsonValue(json, "targetSquare2");
        String promoType = getJsonValue(json, "promoType");

        return new ActionCommandDTO(type, from, to, cardIndex, targetSquare, targetSquare2, promoType);
    }

    private static String getJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(":", idx + search.length());
        if (colonIdx == -1) return null;
        int start = colonIdx + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t' || json.charAt(start) == '\r' || json.charAt(start) == '\n')) {
            start++;
        }
        if (start >= json.length()) return null;
        
        if (json.charAt(start) == '"') {
            int end = json.indexOf("\"", start + 1);
            if (end == -1) return null;
            return json.substring(start + 1, end);
        } else {
            int end = start;
            while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-' || json.charAt(end) == '+')) {
                end++;
            }
            if (end == start) return null;
            return json.substring(start, end);
        }
    }
}
