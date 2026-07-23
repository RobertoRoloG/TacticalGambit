package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * Acción atómica de movimiento de pieza.
 * 
 * Reglas de negocio:
 * - Costo: 2 AP.
 * - Invariante estricta: Máximo 1 movimiento de pieza por turno.
 * - Soporte de Coronación de Peón opcional (por defecto a Queen).
 */
public record PieceMoveAction(Square from, Square to, PieceType promotionType) implements GameAction {

    public static final int MOVE_AP_COST = 2;

    public PieceMoveAction(Square from, Square to) {
        this(from, to, PieceType.QUEEN);
    }

    public PieceMoveAction {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Las casillas de origen y destino son requeridas.");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("Origen y destino no pueden ser idénticos.");
        }
        if (promotionType == null) {
            throw new IllegalArgumentException("El tipo de coronación no puede ser nulo.");
        }
    }

    @Override
    public boolean isValid(TurnState state) {
        if (state == null) {
            return false;
        }
        // Invariante 1: No haber movido pieza en el turno actual
        if (state.hasMovedPieceThisTurn()) {
            return false;
        }
        // Invariante 2: Tener al menos 2 AP disponibles
        if (!state.actionPoints().canAfford(MOVE_AP_COST)) {
            return false;
        }
        // Invariante 3: Existencia de pieza propia en la casilla de origen y geometría legal del movimiento
        Optional<Piece> pieceOpt = state.board().getPieceAt(from);
        if (pieceOpt.isEmpty()) {
            return false;
        }
        Piece piece = pieceOpt.get();
        if (piece.color() != state.activePlayer()) {
            return false;
        }

        // Invariante 4: Comprobación de Escudo (imposibilidad de capturar pieza protegida)
        Optional<Piece> destPieceOpt = state.board().getPieceAt(to);
        if (destPieceOpt.isPresent()) {
            Piece destPiece = destPieceOpt.get();
            if (PieceDecorator.isShielded(destPiece)) {
                return false;
            }
        }
        // Invariante 4b: Comprobación de Barricada (ninguna pieza excepto el Caballo puede finalizar su movimiento en una casilla barricadada)
        if (state.board().isBarricaded(to)) {
            Piece base = PieceDecorator.basePiece(piece);
            if (!(base instanceof Knight)) {
                return false;
            }
        }
        if (!piece.canMove(state.board(), from, to)) {
            return false;
        }

        // Invariante 5: Prevención de Jaque propio (no exponer al propio Rey a nuevos ataques)
        Board simulatedBoard = state.board().simulateMove(from, to);
        boolean hasCardResources = !state.playerHand().cards().isEmpty() || state.deck().remainingCards() > 0;
        if (!hasCardResources) {
            return !com.tacticalgambit.core.state.CheckDetector.isInCheck(simulatedBoard, state.activePlayer());
        }
        java.util.Set<Square> checkingBefore = com.tacticalgambit.core.state.CheckDetector.getCheckingSquares(state.board(), state.activePlayer());
        java.util.Set<Square> checkingAfter = com.tacticalgambit.core.state.CheckDetector.getCheckingSquares(simulatedBoard, state.activePlayer());
        return checkingBefore.containsAll(checkingAfter);
    }

    @Override
    public boolean execute(TurnState state) {
        if (!isValid(state)) {
            return false;
        }

        // Mutación atómica irreversible
        state.consumeAP(MOVE_AP_COST);
        Piece movedPiece = state.board().removePieceInternal(from).orElseThrow();

        // Limpiar salto táctico al consumirse el movimiento
        movedPiece = PieceDecorator.cleanTacticalJump(movedPiece);



        // Remover pieza en destino si hay captura
        state.board().removePieceInternal(to);

        // Verificar Coronación de Peón
        Piece base = PieceDecorator.basePiece(movedPiece);
        if (base instanceof Pawn pawn) {
            int targetRank = (pawn.color() == PieceColor.WHITE) ? 7 : 0;
            if (to.rank() == targetRank) {
                // Coronar peón
                movedPiece = createPromotedPiece(pawn.color(), promotionType);
            }
        }

        state.board().placePieceInternal(to, movedPiece);
        state.markPieceMoved();
        return true;
    }

    private Piece createPromotedPiece(PieceColor color, PieceType type) {
        return switch (type) {
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> new Queen(color); // Default fallback
        };
    }
}
