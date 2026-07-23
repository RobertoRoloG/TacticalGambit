package com.tacticalgambit.core.ui;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.action.DrawCardAction;
import com.tacticalgambit.core.action.GameAction;
import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.domain.card.SquareTarget;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Analizador de Comandos (CommandParser)")
class CommandParserTest {

    @Test
    @DisplayName("Debe parsear correctamente comandos de movimiento")
    void shouldParseMoveCommand() {
        Board board = Board.empty();
        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        GameAction action = CommandParser.parse("MOVE E2 E4", state);

        assertNotNull(action);
        assertInstanceOf(PieceMoveAction.class, action);
        PieceMoveAction move = (PieceMoveAction) action;
        assertEquals(Square.of('e', 2), move.from());
        assertEquals(Square.of('e', 4), move.to());
    }

    @Test
    @DisplayName("Debe parsear correctamente el comando robar carta (DRAW)")
    void shouldParseDrawCommand() {
        Board board = Board.empty();
        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        GameAction action = CommandParser.parse("DRAW", state);

        assertNotNull(action);
        assertInstanceOf(DrawCardAction.class, action);
    }

    @Test
    @DisplayName("Debe parsear correctamente comandos para jugar cartas (PLAY)")
    void shouldParsePlayCommands() {
        Board board = Board.empty();
        Card card = new Card("c1", "Carta Test", 1);
        PlayerHand hand = new PlayerHand(List.of(card));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // 1. Comando PLAY sin objetivo adicional (GlobalTarget)
        GameAction globalAction = CommandParser.parse("PLAY 0", state);
        assertNotNull(globalAction);
        assertInstanceOf(CardPlayAction.class, globalAction);
        CardPlayAction play1 = (CardPlayAction) globalAction;
        assertEquals(card, play1.card());
        assertInstanceOf(GlobalTarget.class, play1.target());

        // 2. Comando PLAY con coordenadas de casilla (SquareTarget)
        GameAction squareAction = CommandParser.parse("PLAY 0 D4", state);
        assertNotNull(squareAction);
        assertInstanceOf(CardPlayAction.class, squareAction);
        CardPlayAction play2 = (CardPlayAction) squareAction;
        assertEquals(card, play2.card());
        assertInstanceOf(SquareTarget.class, play2.target());
        SquareTarget st = (SquareTarget) play2.target();
        assertEquals(Square.of('d', 4), st.square());
    }

    @Test
    @DisplayName("Debe lanzar excepción al recibir comandos mal formados")
    void shouldThrowExceptionForInvalidCommands() {
        Board board = Board.empty();
        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("MOVE E2", state));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("PLAY A D4", state));
        assertThrows(IllegalArgumentException.class, () -> CommandParser.parse("UNKNOWN_CMD", state));
    }
}
