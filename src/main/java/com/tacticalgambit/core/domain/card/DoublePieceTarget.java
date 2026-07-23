package com.tacticalgambit.core.domain.card;

import com.tacticalgambit.core.domain.PieceType;
import com.tacticalgambit.core.domain.Square;
import java.util.Optional;

/**
 * Objetivo de carta que selecciona dos casillas ocupadas por piezas y una pieza de coronación opcional.
 */
public record DoublePieceTarget(Square firstSquare, Square secondSquare, Optional<PieceType> promotionType) implements CardTarget {
    
    public DoublePieceTarget {
        if (firstSquare == null || secondSquare == null || promotionType == null) {
            throw new IllegalArgumentException("Todos los campos de DoublePieceTarget son obligatorios.");
        }
    }

    public DoublePieceTarget(Square firstSquare, Square secondSquare) {
        this(firstSquare, secondSquare, Optional.empty());
    }
}
