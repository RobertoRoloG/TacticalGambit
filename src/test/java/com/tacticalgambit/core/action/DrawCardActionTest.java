package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - DrawCardAction (Límite de mano max 4 cartas)")
class DrawCardActionTest {

    @Test
    @DisplayName("Debe robar una carta exitosamente consumiendo 1 AP")
    void shouldDrawCardSuccessfully() {
        Board board = Board.empty();
        Deck deck = Deck.of(new Card("c1", "Ataque Táctico", 1));
        PlayerHand hand = new PlayerHand();
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, deck);

        DrawCardAction drawAction = new DrawCardAction();

        assertTrue(drawAction.isValid(state));
        boolean success = drawAction.execute(state);

        assertTrue(success);
        assertEquals(2, state.actionPoints().current()); // 3 - 1 = 2 AP
        assertEquals(1, state.playerHand().size());
    }

    @Test
    @DisplayName("Debe rechazar el robo de carta si la mano ya contiene 4 cartas (Límite máximo)")
    void shouldRejectDrawWhenHandIsFull() {
        Board board = Board.empty();
        Deck deck = Deck.of(new Card("c5", "Escudo Imperial", 2));

        // Llenar la mano con 4 cartas
        List<Card> fullHandCards = List.of(
            new Card("c1", "Carta 1", 1),
            new Card("c2", "Carta 2", 1),
            new Card("c3", "Carta 3", 2),
            new Card("c4", "Carta 4", 3)
        );
        PlayerHand hand = new PlayerHand(fullHandCards);
        assertTrue(hand.isFull());

        TurnState state = new TurnState(PieceColor.WHITE, board, hand, deck);
        DrawCardAction drawAction = new DrawCardAction();

        assertFalse(drawAction.isValid(state));
        assertFalse(drawAction.execute(state));
        assertEquals(4, state.playerHand().size());
        assertEquals(3, state.actionPoints().current()); // AP intactos
    }
}
