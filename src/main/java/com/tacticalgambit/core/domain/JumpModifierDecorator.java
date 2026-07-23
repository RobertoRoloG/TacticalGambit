package com.tacticalgambit.core.domain;

import java.util.Optional;

/**
 * Decorador de pieza que permite a una Torre, Alfil o Dama ignorar (saltar) la primera pieza intermedia.
 */
public final class JumpModifierDecorator extends PieceDecorator {

    public JumpModifierDecorator(Piece delegate) {
        super(delegate);
    }

    @Override
    public boolean canMove(Board board, Square from, Square to) {
        if (board == null || from == null || to == null || from.equals(to)) {
            return false;
        }

        // Fuego amigo
        Optional<Piece> destPiece = board.getPieceAt(to);
        if (destPiece.isPresent() && destPiece.get().color() == color()) {
            return false;
        }

        int absFile = Math.abs(to.file() - from.file());
        int absRank = Math.abs(to.rank() - from.rank());

        boolean isOrthogonal = (from.file() == to.file() || from.rank() == to.rank());
        boolean isDiagonal = (absFile == absRank && absFile != 0);

        Piece base = PieceDecorator.basePiece(delegate);

        if (base instanceof Rook) {
            if (!isOrthogonal) return false;
        } else if (base instanceof Bishop) {
            if (!isDiagonal) return false;
        } else if (base instanceof Queen) {
            if (!isOrthogonal && !isDiagonal) return false;
        } else {
            return delegate.canMove(board, from, to);
        }

        return isPathClearWithOneJump(board, from, to);
    }

    private boolean isPathClearWithOneJump(Board board, Square from, Square to) {
        int deltaFile = to.file() - from.file();
        int deltaRank = to.rank() - from.rank();

        int stepFile = Integer.signum(deltaFile);
        int stepRank = Integer.signum(deltaRank);

        int currentFile = from.file() + stepFile;
        int currentRank = from.rank() + stepRank;

        int obstacles = 0;
        while (currentFile != to.file() || currentRank != to.rank()) {
            Square stepSquare = new Square(currentFile, currentRank);
            if (board.isOccupied(stepSquare)) {
                obstacles++;
            }
            currentFile += stepFile;
            currentRank += stepRank;
        }

        return obstacles <= 1;
    }
}
