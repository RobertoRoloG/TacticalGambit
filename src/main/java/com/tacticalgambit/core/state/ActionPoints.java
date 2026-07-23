package com.tacticalgambit.core.state;

/**
 * Value Object inmutable para representar la reserva de Puntos de Acción (AP).
 * 
 * Reglas de negocio:
 * - Base de recarga: +3 AP por turno.
 * - Reserva máxima acumulable (banca): 5 AP.
 */
public record ActionPoints(int current) {

    public static final int MAX_AP = 5;
    public static final int TURN_REFILL_AP = 3;

    public ActionPoints {
        if (current < 0) {
            throw new IllegalArgumentException("Los AP no pueden ser negativos: " + current);
        }
        if (current > MAX_AP) {
            throw new IllegalArgumentException("Los AP no pueden exceder el máximo de " + MAX_AP + ": " + current);
        }
    }

    /**
     * Retorna los AP iniciales al comenzar una partida (3 AP).
     */
    public static ActionPoints initial() {
        return new ActionPoints(TURN_REFILL_AP);
    }

    /**
     * Aplica la recarga estándar de +3 AP al inicio de un turno respetando el límite máximo de 5 AP.
     */
    public ActionPoints addTurnRefill() {
        int nextAp = Math.min(MAX_AP, current + TURN_REFILL_AP);
        return new ActionPoints(nextAp);
    }

    /**
     * Aplica la recarga con penalización, acotado inferiormente a 0 y superiormente al máximo de 5 AP.
     */
    public ActionPoints addTurnRefillWithPenalty(int penalty) {
        int nextAp = Math.max(0, current + TURN_REFILL_AP - penalty);
        nextAp = Math.min(MAX_AP, nextAp);
        return new ActionPoints(nextAp);
    }

    /**
     * Añade AP respetando el máximo de 5 AP.
     */
    public ActionPoints add(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("La cantidad no puede ser negativa.");
        }
        int nextAp = Math.min(MAX_AP, current + amount);
        return new ActionPoints(nextAp);
    }

    /**
     * Verifica si se cuenta con AP suficientes para cubrir el costo especificado.
     */
    public boolean canAfford(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo: " + cost);
        }
        return current >= cost;
    }

    /**
     * Consume AP de forma atómica retornando una nueva instancia de ActionPoints.
     */
    public ActionPoints consume(int cost) {
        if (!canAfford(cost)) {
            throw new IllegalStateException("AP insuficientes. Requeridos: " + cost + ", Disponibles: " + current);
        }
        return new ActionPoints(current - cost);
    }
}
