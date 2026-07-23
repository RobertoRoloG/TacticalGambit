package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - PieceMoveAction (Invariante 1 Mov/Turno & AP)")
class PieceMoveActionTest {

    @Test
    @DisplayName("Debe ejecutar un movimiento consumir 2 AP y marcar el flag de movimiento")
    void shouldExecuteFirstPieceMove() {
        Board board = Board.standardInitialSetup();
        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        Square e2 = Square.of('e', 2);
        Square e4 = Square.of('e', 4);
        PieceMoveAction move1 = new PieceMoveAction(e2, e4);

        assertTrue(move1.isValid(state));
        boolean success = move1.execute(state);

        assertTrue(success);
        assertEquals(1, state.actionPoints().current()); // 3 AP iniciales - 2 AP = 1 AP
        assertTrue(state.hasMovedPieceThisTurn());
    }

    @Test
    @DisplayName("Debe rechazar un segundo movimiento de pieza en el mismo turno")
    void shouldRejectSecondPieceMoveInSameTurn() {
        Board board = Board.standardInitialSetup();
        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        Square e2 = Square.of('e', 2);
        Square e4 = Square.of('e', 4);
        PieceMoveAction move1 = new PieceMoveAction(e2, e4);
        assertTrue(move1.execute(state));

        // Intento de mover un caballo en el mismo turno
        Square b1 = Square.of('b', 1);
        Square c3 = Square.of('c', 3);
        PieceMoveAction move2 = new PieceMoveAction(b1, c3);

        assertFalse(move2.isValid(state));
        assertFalse(move2.execute(state));
    }
}
