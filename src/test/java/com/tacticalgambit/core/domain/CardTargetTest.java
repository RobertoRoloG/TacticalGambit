package com.tacticalgambit.core.domain;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.SquareTarget;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Taxonomía CardTarget y Regla Antimaten")
class CardTargetTest {

    @Test
    @DisplayName("a) Debe rechazar PieceTarget sobre piezas enemigas o sobre el Rey propio")
    void shouldRejectPieceTargetOnEnemyOrKing() {
        Board board = Board.standardInitialSetup();
        Card card = new Card("c1", "Escudo Mágico", 1);
        PlayerHand hand = new PlayerHand(List.of(card));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // 1. Intento de objetivo sobre pieza enemiga (Peón Negro en e7)
        Square e7 = Square.of('e', 7);
        CardPlayAction actionOnEnemy = new CardPlayAction(card, new PieceTarget(e7));
        assertFalse(actionOnEnemy.isValid(state));

        // 2. Intento de objetivo sobre Rey propio (Rey Blanco en e1)
        Square e1 = Square.of('e', 1);
        CardPlayAction actionOnKing = new CardPlayAction(card, new PieceTarget(e1));
        assertFalse(actionOnKing.isValid(state));

        // 3. Aplicación válida sobre un Peón propio en e2
        Square e2 = Square.of('e', 2);
        CardPlayAction actionOnPawn = new CardPlayAction(card, new PieceTarget(e2));
        assertTrue(actionOnPawn.isValid(state));
    }

    @Test
    @DisplayName("b) Debe aplicar correctamente SquareTarget sobre casillas válidas")
    void shouldAllowSquareTargetOnEmptySquare() {
        Board board = Board.standardInitialSetup();
        Card card = new Card("c2", "Muro de Piedra", 2);
        PlayerHand hand = new PlayerHand(List.of(card));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Aplicación sobre casilla vacía e4
        Square e4 = Square.of('e', 4);
        CardPlayAction actionOnSquare = new CardPlayAction(card, new SquareTarget(e4));

        assertTrue(actionOnSquare.isValid(state));
        boolean success = actionOnSquare.execute(state);

        assertTrue(success);
        assertEquals(1, state.actionPoints().current()); // 3 AP - 2 AP = 1 AP
        assertEquals(0, state.playerHand().size());
    }

    @Test
    @DisplayName("c) Debe rechazar cartas si el estado del tablero sitúa al Rey enemigo en Jaque (Regla Antimaten)")
    void shouldRejectCardActionIfEnemyKingIsInCheck() {
        Board board = Board.empty();
        // Colocar Rey Negro en e8 y una Torre Blanca atacante en e1 (Torre ataca e8 -> Jaque)
        Square e8 = Square.of('e', 8);
        Square e1 = Square.of('e', 1);
        Square a1 = Square.of('a', 1);

        board.placePieceInternal(e8, new King(PieceColor.BLACK));
        board.placePieceInternal(e1, new Rook(PieceColor.WHITE));
        board.placePieceInternal(a1, new Pawn(PieceColor.WHITE));

        // Verificar que con este tablero el Rey Negro está en Jaque
        assertTrue(GameConditionChecker.isInCheck(board, PieceColor.BLACK));

        Card card = new Card("c3", "Robo Global", 1);
        PlayerHand hand = new PlayerHand(List.of(card));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // La regla Antimaten prohíbe jugar cualquier carta táctica si el Rey enemigo queda/está en Jaque por el tablero
        CardPlayAction globalAction = new CardPlayAction(card, new GlobalTarget());
        assertFalse(globalAction.isValid(state));
        assertFalse(globalAction.execute(state));
    }
}
