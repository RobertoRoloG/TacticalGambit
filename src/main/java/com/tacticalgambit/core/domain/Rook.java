package com.tacticalgambit.core.domain;

import java.util.Optional;

public record Rook(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.ROOK;
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

        boolean isOrthogonal = (from.file() == to.file() || from.rank() == to.rank());
        if (!isOrthogonal) {
            return false;
        }

        return board.isPathClear(from, to);
    }
}
