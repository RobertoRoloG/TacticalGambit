package com.tacticalgambit.core.domain.card;

import com.tacticalgambit.core.domain.Square;

/**
 * Objetivo de carta dirigido a una casilla específica del tablero (terreno/trampa).
 */
public record SquareTarget(Square square) implements CardTarget {
    public SquareTarget {
        if (square == null) {
            throw new IllegalArgumentException("La casilla objetivo no puede ser nula.");
        }
    }
}
