package com.tacticalgambit.core.card;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.*;
import com.tacticalgambit.core.domain.card.impl.BarricadeCard;
import com.tacticalgambit.core.domain.card.impl.TacticalDashCard;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Tactical Dash y Barricada")
class CardDashAndBarricadeTest {

    @Test
    @DisplayName("a) Tactical Dash debe permitir mover una pieza propia 1 casilla si es legal")
    void shouldAllowTacticalDashWhenValid() {
        Board board = Board.empty();
        Square d4 = Square.of('d', 4);
        Square d5 = Square.of('d', 5);

        // Colocar una Torre (Rook) blanca en d4
        board.placePieceInternal(d4, new Rook(PieceColor.WHITE));

        TacticalDashCard dashCard = new TacticalDashCard("d1", "Tactical Dash", 1);
        PlayerHand hand = new PlayerHand(List.of(dashCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        CardPlayAction playDash = new CardPlayAction(dashCard, new DoublePieceTarget(d4, d5));
        assertTrue(playDash.isValid(state));
        assertTrue(playDash.execute(state));

        // La torre debe haberse movido a d5
        assertFalse(board.isOccupied(d4));
        assertTrue(board.isOccupied(d5));
        assertEquals(PieceType.ROOK, PieceDecorator.basePiece(board.getPieceAt(d5).get()).type());
    }

    @Test
    @DisplayName("b) Tactical Dash debe ser inválido si genera Jaque directo al Rey enemigo")
    void shouldNotAllowTacticalDashIfItAttacksEnemyKingDirectly() {
        Board board = Board.empty();
        Square d4 = Square.of('d', 4);
        Square d8 = Square.of('d', 8); // Rey enemigo (Negro)
        Square d7 = Square.of('d', 7); // Destino del dash (Torre atacará directamente al Rey desde d7)

        board.placePieceInternal(d4, new Rook(PieceColor.WHITE));
        board.placePieceInternal(d8, new King(PieceColor.BLACK));

        TacticalDashCard dashCard = new TacticalDashCard("d1", "Tactical Dash", 1);
        PlayerHand hand = new PlayerHand(List.of(dashCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Dash de d4 a d7 (distancia es 3, no 1; debe ser inválido por distancia también)
        CardPlayAction playDashFar = new CardPlayAction(dashCard, new DoublePieceTarget(d4, d7));
        assertFalse(playDashFar.isValid(state));

        // Ahora colocamos la torre en d6 y el destino en d7 (distancia 1)
        board.removePieceInternal(d4);
        board.placePieceInternal(Square.of('d', 6), new Rook(PieceColor.WHITE));
        
        CardPlayAction playDashClose = new CardPlayAction(dashCard, new DoublePieceTarget(Square.of('d', 6), d7));
        // Debe ser inválido porque desde d7 la Torre daría jaque directo al rey enemigo en d8
        assertFalse(playDashClose.isValid(state));
    }

    @Test
    @DisplayName("c) Colocación de Barricada y bloqueo de paso/aterrizaje para piezas comunes")
    void shouldBlockNormalMovesOnBarricadedSquare() {
        Board board = Board.empty();
        Square d4 = Square.of('d', 4); // Casilla vacía para barricada
        Square d3 = Square.of('d', 3); // Torre Blanca
        Square d5 = Square.of('d', 5); // Casilla detrás de la barricada
        Square b3 = Square.of('b', 3); // Caballo Blanco

        board.placePieceInternal(d3, new Rook(PieceColor.WHITE));
        board.placePieceInternal(b3, new Knight(PieceColor.WHITE));

        BarricadeCard barricadeCard = new BarricadeCard("b1", "Barricade", 3);
        PlayerHand hand = new PlayerHand(List.of(barricadeCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // 1. Colocar barricada en d4
        CardPlayAction playBarricade = new CardPlayAction(barricadeCard, new SquareTarget(d4));
        assertTrue(playBarricade.isValid(state));
        assertTrue(playBarricade.execute(state));
        assertTrue(board.isBarricaded(d4));

        // Añadir AP para poder costear los movimientos físicos (2 AP cada uno)
        state.addAP(6);

        // 2. Intentar mover Torre blanca de d3 a d4 (aterrizar en barricada) -> debe ser ilegal
        PieceMoveAction moveRookToBarricade = new PieceMoveAction(d3, d4);
        assertFalse(moveRookToBarricade.isValid(state));

        // 3. Intentar mover Torre blanca de d3 a d5 (atravesar la barricada en d4) -> debe ser ilegal
        PieceMoveAction moveRookThroughBarricade = new PieceMoveAction(d3, d5);
        assertFalse(moveRookThroughBarricade.isValid(state));

        // 4. Intentar mover Caballo blanco de b3 a d4 (aterrizar en barricada) -> debe ser LEGAL para el Caballo
        PieceMoveAction moveKnightToBarricade = new PieceMoveAction(b3, d4);
        assertTrue(moveKnightToBarricade.isValid(state));
    }

    @Test
    @DisplayName("d) Decaimiento y eliminación de Barricadas tras 2 turnos completos")
    void shouldDecayBarricadesAfterTwoTurns() {
        Board board = Board.empty();
        Square d4 = Square.of('d', 4);

        BarricadeCard barricadeCard = new BarricadeCard("b1", "Barricade", 3);
        PlayerHand hand = new PlayerHand(List.of(barricadeCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Colocar barricada
        CardPlayAction playBarricade = new CardPlayAction(barricadeCard, new SquareTarget(d4));
        assertTrue(playBarricade.execute(state));
        assertTrue(board.isBarricaded(d4));
        assertEquals(4, board.barricades().get(d4));

        // 1er medio turno
        state.startNextTurn();
        assertTrue(board.isBarricaded(d4));
        assertEquals(3, board.barricades().get(d4));

        // 2º medio turno (1er turno completo finalizado)
        state.startNextTurn();
        assertTrue(board.isBarricaded(d4));
        assertEquals(2, board.barricades().get(d4));

        // 3er medio turno
        state.startNextTurn();
        assertTrue(board.isBarricaded(d4));
        assertEquals(1, board.barricades().get(d4));

        // 4º medio turno (2º turno completo finalizado)
        state.startNextTurn();
        // La barricada debe haber decaído por completo y desaparecer
        assertFalse(board.isBarricaded(d4));
    }
}
