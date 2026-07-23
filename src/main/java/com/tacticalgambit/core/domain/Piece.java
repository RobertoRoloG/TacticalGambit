package com.tacticalgambit.core.domain;

/**
 * Interface sellada (Sealed Interface) para todas las piezas del motor.
 * Garantiza exhaustividad en pattern matching switch y evita extensiones no autorizadas.
 */
public sealed interface Piece permits Pawn, Knight, Bishop, Rook, Queen, King, PieceDecorator {
    PieceColor color();
    PieceType type();

    /**
     * Evalúa si la pieza puede desplazarse geométricamente desde 'from' hasta 'to'
     * en el tablero actual, respetando las reglas de su tipo y bloqueos de camino.
     */
    boolean canMove(Board board, Square from, Square to);
}
