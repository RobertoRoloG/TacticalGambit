package com.tacticalgambit.core.domain;

import java.util.Optional;

public record Knight(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.KNIGHT;
    }

    @Override
    public boolean canMove(Board board, Square from, Square to) {
        if (board == null || from == null || to == null || from.equals(to)) {
            return false;
        }

        // Fuego amigo
        Optional<Piece> destPiece = board.getPieceAt(to);
        if (destPiece.isPresent() && destPiece.get().color() == color) {
            return false;
        }

        int absFile = Math.abs(to.file() - from.file());
        int absRank = Math.abs(to.rank() - from.rank());

        // Movimiento en L (2 en un eje y 1 en el otro)
        return (absFile == 1 && absRank == 2) || (absFile == 2 && absRank == 1);
    }
}
