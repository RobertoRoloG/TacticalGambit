package com.tacticalgambit.core;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.impl.ShieldCard;
import com.tacticalgambit.core.state.GameState;
import com.tacticalgambit.core.state.GameStateManager;
import com.tacticalgambit.core.state.MatchInitializer;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Componentes Críticos MVP")
class MVPTest {

    @Test
    @DisplayName("a) Inicialización correcta del tablero con 32 piezas y repartos de mano")
    void shouldInitializeCorrectly() {
        TurnState state = MatchInitializer.initialize();

        assertNotNull(state);
        assertEquals(PieceColor.WHITE, state.activePlayer());
        assertEquals(GameState.IN_PROGRESS, state.gameState());

        // Verificar cantidad de piezas en el tablero
        assertEquals(32, state.board().pieces().size());

        // Verificar cartas repartidas
        assertEquals(2, state.whiteHand().size());
        assertEquals(2, state.blackHand().size());

        // Verificar cartas restantes en el mazo (20 iniciales - 2 repartidas = 18)
        assertEquals(18, state.whiteDeck().remainingCards());
        assertEquals(18, state.blackDeck().remainingCards());
    }

    @Test
    @DisplayName("b) Coronación efectiva de un peón al alcanzar la fila objetivo")
    void shouldPromotePawnOnExtremeRank() {
        Board board = Board.empty();
        Square a7 = Square.of('a', 7);
        Square a8 = Square.of('a', 8);

        Pawn pawn = new Pawn(PieceColor.WHITE);
        board.placePieceInternal(a7, pawn);

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        // Mover peón de a7 a a8 (Fila 7 a 8, indexada 0-7: fila 6 a 7)
        PieceMoveAction moveAction = new PieceMoveAction(a7, a8);
        assertTrue(moveAction.execute(state));

        // Debe haberse convertido automáticamente en una Dama (Queen)
        Piece pieceAtA8 = board.getPieceAt(a8).orElseThrow();
        assertEquals(PieceType.QUEEN, PieceDecorator.basePiece(pieceAtA8).type());
        assertEquals(PieceColor.WHITE, pieceAtA8.color());
    }

    @Test
    @DisplayName("c) Inmunidad temporal otorgada por ShieldCard")
    void shouldProtectPieceWithShieldCard() {
        Board board = Board.empty();
        Square e2 = Square.of('e', 2);
        Square e3 = Square.of('e', 3);

        Pawn friendlyPawn = new Pawn(PieceColor.WHITE);
        Rook enemyRook = new Rook(PieceColor.BLACK);

        board.placePieceInternal(e2, friendlyPawn);
        board.placePieceInternal(e3, enemyRook);

        ShieldCard shieldCard = new ShieldCard("shield_1", "Escudo Protector", 1);
        PlayerHand hand = new PlayerHand(List.of(shieldCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Jugar la carta sobre el Peón en e2
        CardPlayAction playCard = new CardPlayAction(shieldCard, new PieceTarget(e2));
        assertTrue(playCard.execute(state));

        // Verificar que la pieza en e2 ahora tiene escudo
        Piece piece = board.getPieceAt(e2).orElseThrow();
        assertTrue(PieceDecorator.isShielded(piece));

        // Finalizar turno de Blanco, pasa a Negro
        state.startNextTurn();
        assertEquals(PieceColor.BLACK, state.activePlayer());

        // Negro intenta capturar el Peón protegido con su Torre en e3
        PieceMoveAction captureAttempt = new PieceMoveAction(e3, e2);
        // Debe ser inválido porque la pieza objetivo está protegida con escudo
        assertFalse(captureAttempt.isValid(state));
        assertFalse(captureAttempt.execute(state));

        // Finalizar turno de Negro, vuelve a Blanco
        state.startNextTurn();
        assertEquals(PieceColor.WHITE, state.activePlayer());

        // El escudo debe haber expirado al comenzar el turno del dueño de la pieza
        Piece unwrappedPiece = board.getPieceAt(e2).orElseThrow();
        assertFalse(PieceDecorator.isShielded(unwrappedPiece));
    }

    @Test
    @DisplayName("d) Transición automática a CHECKMATE en la evaluación del estado del turno")
    void shouldTransitionToCheckmateWhenNoLegalActionsAndInCheck() {
        Board board = Board.empty();
        Square e8 = Square.of('e', 8); // Rey Negro
        Square e1 = Square.of('e', 1); // Torre Blanca en la columna 'e'
        Square d1 = Square.of('d', 1); // Torre Blanca en la columna 'd'
        Square f1 = Square.of('f', 1); // Torre Blanca en la columna 'f'

        // Colocar Rey Negro bloqueado
        board.placePieceInternal(e8, new King(PieceColor.BLACK));
        
        // Colocar torres que atacan el rey y sus casillas de escape laterales
        board.placePieceInternal(e1, new Rook(PieceColor.WHITE));
        board.placePieceInternal(d1, new Rook(PieceColor.WHITE));
        board.placePieceInternal(f1, new Rook(PieceColor.WHITE));

        // Inicializar estado del turno para Negro (sin cartas y sin AP)
        TurnState state = new TurnState(PieceColor.BLACK, board, new PlayerHand(), Deck.of());

        // Forzar chequeo de estado
        GameStateManager.checkAndUpdateGameState(state);

        // Debe transicionar a CHECKMATE ya que el Rey Negro está atacado y no tiene movimientos legales
        assertEquals(GameState.CHECKMATE, state.gameState());
    }

    @Test
    @DisplayName("e) Correcta expiración de JumpModifierDecorator tras ejecutar un movimiento")
    void shouldExpireJumpModifierAfterMove() {
        Board board = Board.empty();
        Square a1 = Square.of('a', 1);
        Square a2 = Square.of('a', 2);
        Square a3 = Square.of('a', 3);

        Rook rook = new Rook(PieceColor.WHITE);
        board.placePieceInternal(a1, rook);
        board.placePieceInternal(a2, new Pawn(PieceColor.WHITE));

        board.placePieceInternal(a1, new JumpModifierDecorator(rook));

        TurnState state = new TurnState(PieceColor.WHITE, board, new PlayerHand(), Deck.of());

        PieceMoveAction jumpMove = new PieceMoveAction(a1, a3);
        assertTrue(jumpMove.isValid(state));
        assertTrue(jumpMove.execute(state));

        Piece pieceAtA3 = board.getPieceAt(a3).orElseThrow();
        assertFalse(pieceAtA3 instanceof JumpModifierDecorator);

        PieceMoveAction blockMove = new PieceMoveAction(a3, a1);
        assertFalse(blockMove.isValid(state));
    }
}
