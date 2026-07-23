package com.tacticalgambit.core.domain;

import java.util.Optional;

public record Queen(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.QUEEN;
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

        boolean isOrthogonal = (from.file() == to.file() || from.rank() == to.rank());
        boolean isDiagonal = (absFile == absRank && absFile != 0);

        if (!isOrthogonal && !isDiagonal) {
            return false;
        }

        return board.isPathClear(from, to);
    }
}
