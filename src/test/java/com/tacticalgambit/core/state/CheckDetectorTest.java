package com.tacticalgambit.core.state;

import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - CheckDetector e Invariantes de Jaque")
class CheckDetectorTest {

    @Test
    @DisplayName("a) Imposibilidad de mover una pieza clavada si ello expone al Rey propio")
    void shouldRejectMoveOfPinnedPiece() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1); // Rey Blanco
        Square e2 = Square.of('e', 2); // Alfil Blanco (pieza clavada)
        Square e8 = Square.of('e', 8); // Torre Negra (atacante)

        board.placePieceInternal(e1, new King(PieceColor.WHITE));
        board.placePieceInternal(e2, new Bishop(PieceColor.WHITE));
        board.placePieceInternal(e8, new Rook(PieceColor.BLACK));

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        // El Alfil Blanco intenta moverse fuera de la columna 'e' (ej. a d3)
        Square d3 = Square.of('d', 3);
        PieceMoveAction moveAction = new PieceMoveAction(e2, d3);

        // Debería ser inválido porque expone al Rey Blanco a la Torre Negra
        assertFalse(moveAction.isValid(state));
    }

    @Test
    @DisplayName("b) Imposibilidad de mover el Rey a una casilla controlada por el enemigo")
    void shouldRejectKingMoveToAttackedSquare() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1); // Rey Blanco
        Square d2 = Square.of('d', 2); // Casilla adyacente controlada por enemigo
        Square d8 = Square.of('d', 8); // Torre Negra controlando la columna 'd'

        board.placePieceInternal(e1, new King(PieceColor.WHITE));
        board.placePieceInternal(d8, new Rook(PieceColor.BLACK));

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        // Intentar mover el Rey a d2
        PieceMoveAction moveAction = new PieceMoveAction(e1, d2);

        // Debería ser inválido porque d2 está bajo ataque de la Torre Negra
        assertFalse(moveAction.isValid(state));
    }

    @Test
    @DisplayName("c) Obligación de responder a un Jaque")
    void shouldForceCheckResolution() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1); // Rey Blanco
        Square e8 = Square.of('e', 8); // Torre Negra (da Jaque al Rey Blanco)
        Square a2 = Square.of('a', 2); // Peón Blanco alejado
        Square a3 = Square.of('a', 3);

        board.placePieceInternal(e1, new King(PieceColor.WHITE));
        board.placePieceInternal(e8, new Rook(PieceColor.BLACK));
        board.placePieceInternal(a2, new Pawn(PieceColor.WHITE));

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        // Verificar que Blanco está en Jaque
        assertTrue(CheckDetector.isInCheck(board, PieceColor.WHITE));

        // 1. Intento ilegal: Mover el peón alejado (no resuelve el jaque)
        PieceMoveAction movePawn = new PieceMoveAction(a2, a3);
        assertFalse(movePawn.isValid(state));

        // 2. Intento legal: Mover el Rey Blanco a f1 (fuera del jaque)
        Square f1 = Square.of('f', 1);
        PieceMoveAction escapeKing = new PieceMoveAction(e1, f1);
        assertTrue(escapeKing.isValid(state));
    }

    @Test
    @DisplayName("d) Rechazo del movimiento si se dispone de menos de 2 AP o si ya se realizó un movimiento")
    void shouldRejectMoveOnInsufficientResources() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1);
        Square e2 = Square.of('e', 2);

        board.placePieceInternal(e1, new King(PieceColor.WHITE));

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        // Caso 1: AP suficientes, no se ha movido
        PieceMoveAction move1 = new PieceMoveAction(e1, e2);
        assertTrue(move1.isValid(state));

        // Caso 2: Consumir AP para que queden menos de 2
        state.consumeAP(2); // Queda 1 AP de los 3 iniciales
        assertFalse(move1.isValid(state));

        // Caso 3: Resetear AP pero marcar hasMovedPieceThisTurn a true
        state.startNextTurn(); // Pasa a Negro
        state.startNextTurn(); // Vuelve a Blanco con 4 AP
        state.markPieceMoved();
        assertFalse(move1.isValid(state));
    }
}
