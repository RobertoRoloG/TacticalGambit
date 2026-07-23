package com.tacticalgambit.core.state;

import com.tacticalgambit.core.domain.*;
import java.util.Map;
import java.util.Optional;

/**
 * Motor de Detección de Jaques y validación de casillas amenazadas.
 */
public class CheckDetector {

    private CheckDetector() {
        // Clase de utilidad
    }

    /**
     * Evalúa si una casilla está amenazada por alguna pieza del bando atacante.
     */
    public static boolean isSquareUnderAttack(Board board, Square target, PieceColor attackerColor) {
        if (board == null || target == null || attackerColor == null) {
            return false;
        }

        for (Map.Entry<Square, Piece> entry : board.pieces().entrySet()) {
            Square from = entry.getKey();
            Piece piece = entry.getValue();

            if (piece.color() == attackerColor) {
                if (canPieceAttackSquare(board, piece, from, target)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Evalúa si el Rey del color especificado se encuentra en estado de Jaque.
     */
    public static boolean isInCheck(Board board, PieceColor kingColor) {
        if (board == null || kingColor == null) {
            return false;
        }

        Optional<Square> kingSquareOpt = findKingSquare(board, kingColor);
        if (kingSquareOpt.isEmpty()) {
            return false;
        }

        return isSquareUnderAttack(board, kingSquareOpt.get(), kingColor.opposite());
    }

    /**
     * Devuelve las casillas de todas las piezas enemigas que están dando jaque al Rey del color especificado.
     */
    public static java.util.Set<Square> getCheckingSquares(Board board, PieceColor kingColor) {
        java.util.Set<Square> checking = new java.util.HashSet<>();
        if (board == null || kingColor == null) {
            return checking;
        }

        Optional<Square> kingSquareOpt = findKingSquare(board, kingColor);
        if (kingSquareOpt.isEmpty()) {
            return checking;
        }

        Square kingSquare = kingSquareOpt.get();
        PieceColor attackerColor = kingColor.opposite();

        for (Map.Entry<Square, Piece> entry : board.pieces().entrySet()) {
            Square from = entry.getKey();
            Piece piece = entry.getValue();

            if (piece.color() == attackerColor) {
                if (canPieceAttackSquare(board, piece, from, kingSquare)) {
                    checking.add(from);
                }
            }
        }
        return checking;
    }

    private static Optional<Square> findKingSquare(Board board, PieceColor color) {
        return board.pieces().entrySet().stream()
                .filter(e -> {
                    Piece base = PieceDecorator.basePiece(e.getValue());
                    return base.type() == PieceType.KING && base.color() == color;
                })
                .map(Map.Entry::getKey)
                .findFirst();
    }

    private static boolean canPieceAttackSquare(Board board, Piece piece, Square from, Square target) {
        int deltaFile = target.file() - from.file();
        int deltaRank = target.rank() - from.rank();

        Piece base = PieceDecorator.basePiece(piece);
        return switch (base) {
            case Pawn p -> {
                int direction = (p.color() == PieceColor.WHITE) ? 1 : -1;
                yield deltaRank == direction && Math.abs(deltaFile) == 1;
            }
            case Knight k -> {
                int absFile = Math.abs(deltaFile);
                int absRank = Math.abs(deltaRank);
                yield (absFile == 1 && absRank == 2) || (absFile == 2 && absRank == 1);
            }
            case Bishop b -> isDiagonalClear(board, from, target, deltaFile, deltaRank);
            case Rook r -> isOrthogonalClear(board, from, target, deltaFile, deltaRank);
            case Queen q -> isDiagonalClear(board, from, target, deltaFile, deltaRank) ||
                            isOrthogonalClear(board, from, target, deltaFile, deltaRank);
            case King k -> Math.abs(deltaFile) <= 1 && Math.abs(deltaRank) <= 1;
            case PieceDecorator dec -> false;
        };
    }

    private static boolean isDiagonalClear(Board board, Square from, Square target, int deltaFile, int deltaRank) {
        if (Math.abs(deltaFile) != Math.abs(deltaRank) || deltaFile == 0) {
            return false;
        }
        int stepFile = Integer.signum(deltaFile);
        int stepRank = Integer.signum(deltaRank);

        int currFile = from.file() + stepFile;
        int currRank = from.rank() + stepRank;

        while (currFile != target.file() || currRank != target.rank()) {
            Square current = new Square(currFile, currRank);
            if (board.isOccupied(current)) {
                return false;
            }
            currFile += stepFile;
            currRank += stepRank;
        }
        return true;
    }

    private static boolean isOrthogonalClear(Board board, Square from, Square target, int deltaFile, int deltaRank) {
        if (deltaFile != 0 && deltaRank != 0) {
            return false;
        }
        int stepFile = Integer.signum(deltaFile);
        int stepRank = Integer.signum(deltaRank);

        int currFile = from.file() + stepFile;
        int currRank = from.rank() + stepRank;

        while (currFile != target.file() || currRank != target.rank()) {
            Square current = new Square(currFile, currRank);
            if (board.isOccupied(current)) {
                return false;
            }
            currFile += stepFile;
            currRank += stepRank;
        }
        return true;
    }
}
