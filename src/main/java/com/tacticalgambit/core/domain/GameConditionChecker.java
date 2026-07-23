package com.tacticalgambit.core.domain;

import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.state.TurnState;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluador de reglas de estado de juego (Jaque y amenazas directas al Rey).
 */
public class GameConditionChecker {

    private GameConditionChecker() {
        // Clase de utilidad
    }

    /**
     * Evalúa si el Rey del color especificado se encuentra actualmente en estado de Jaque.
     * @param board Estado actual del tablero.
     * @param kingColor Color del Rey a evaluar.
     * @return true si alguna pieza enemiga ataca la casilla del Rey; false en caso contrario.
     */
    public static boolean isInCheck(Board board, PieceColor kingColor) {
        return com.tacticalgambit.core.state.CheckDetector.isInCheck(board, kingColor);
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
            case PieceDecorator dec -> false; // Nunca debería ocurrir por basePiece()
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
                return false; // Camino bloqueado
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
                return false; // Camino bloqueado
            }
            currFile += stepFile;
            currRank += stepRank;
        }
        return true;
    }

    /**
     * Evalúa si el jugador activo tiene alguna acción legal posible en su turno.
     * Revisa movimientos físicos que no dejen a su propio Rey en Jaque, jugadas de carta o robar cartas.
     */
    public static boolean hasAnyLegalAction(TurnState state) {
        if (state == null) {
            return false;
        }

        // 1. Movimientos físicos de pieza
        for (Map.Entry<Square, Piece> entry : state.board().pieces().entrySet()) {
            Square from = entry.getKey();
            Piece piece = entry.getValue();

            if (piece.color() == state.activePlayer()) {
                for (int f = 0; f < 8; f++) {
                    for (int r = 0; r < 8; r++) {
                        Square to = new Square(f, r);
                        if (from.equals(to)) {
                            continue;
                        }
                        PieceMoveAction action = new PieceMoveAction(from, to);
                        if (action.isValid(state)) {
                            // Simular que el propio rey no quede en jaque
                            if (simulateAndCheckSelfCheck(state, action)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        // 2. Jugar cartas de la mano
        for (Card card : state.playerHand().cards()) {
            // Caso GlobalTarget
            com.tacticalgambit.core.action.CardPlayAction globalPlay = 
                new com.tacticalgambit.core.action.CardPlayAction(card, new com.tacticalgambit.core.domain.card.GlobalTarget());
            if (globalPlay.isValid(state)) {
                return true;
            }

            // Casos PieceTarget y SquareTarget
            for (int f = 0; f < 8; f++) {
                for (int r = 0; r < 8; r++) {
                    Square targetSq = new Square(f, r);
                    com.tacticalgambit.core.domain.card.CardTarget target = state.board().isOccupied(targetSq)
                        ? new com.tacticalgambit.core.domain.card.PieceTarget(targetSq)
                        : new com.tacticalgambit.core.domain.card.SquareTarget(targetSq);

                    com.tacticalgambit.core.action.CardPlayAction playCard = 
                        new com.tacticalgambit.core.action.CardPlayAction(card, target);
                    if (playCard.isValid(state)) {
                        return true;
                    }
                }
            }
        }

        // 3. Robar cartas (DrawCardAction)
        com.tacticalgambit.core.action.DrawCardAction draw = new com.tacticalgambit.core.action.DrawCardAction();
        return draw.isValid(state);
    }

    private static boolean simulateAndCheckSelfCheck(TurnState state, PieceMoveAction action) {
        Board tempBoard = Board.empty();
        for (Map.Entry<Square, Piece> entry : state.board().pieces().entrySet()) {
            tempBoard.placePieceInternal(entry.getKey(), entry.getValue());
        }
        Piece movedPiece = tempBoard.removePieceInternal(action.from()).orElseThrow();
        tempBoard.removePieceInternal(action.to());
        tempBoard.placePieceInternal(action.to(), movedPiece);
        return !isInCheck(tempBoard, state.activePlayer());
    }
}
