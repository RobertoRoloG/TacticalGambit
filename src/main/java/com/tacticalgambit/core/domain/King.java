package com.tacticalgambit.core.domain;

import java.util.Optional;

public record King(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.KING;
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

        // Máximo 1 casilla de distancia en cualquier dirección
        return absFile <= 1 && absRank <= 1;
    }
}
