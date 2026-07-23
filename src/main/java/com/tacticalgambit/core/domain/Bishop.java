package com.tacticalgambit.core.domain;

import java.util.Optional;

public record Bishop(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.BISHOP;
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

        if (absFile != absRank || absFile == 0) {
            return false;
        }

        return board.isPathClear(from, to);
    }
}
