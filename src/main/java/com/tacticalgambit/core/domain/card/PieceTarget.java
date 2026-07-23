package com.tacticalgambit.core.domain.card;

import com.tacticalgambit.core.domain.Square;

/**
 * Objetivo de carta dirigido a una pieza específica en el tablero.
 */
public record PieceTarget(Square pieceSquare) implements CardTarget {
    public PieceTarget {
        if (pieceSquare == null) {
            throw new IllegalArgumentException("La casilla de la pieza objetivo no puede ser nula.");
        }
    }
}
