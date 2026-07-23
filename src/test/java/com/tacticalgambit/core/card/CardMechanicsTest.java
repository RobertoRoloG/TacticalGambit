package com.tacticalgambit.core.card;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.action.DiscardCardAction;
import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.impl.CycleCard;
import com.tacticalgambit.core.domain.card.impl.OverchargeCard;
import com.tacticalgambit.core.domain.card.impl.RegroupCard;
import com.tacticalgambit.core.domain.card.impl.TacticalJumpCard;
import com.tacticalgambit.core.state.TurnState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Refactorización y Mecánicas de Cartas")
class CardMechanicsTest {

    @Test
    @DisplayName("a) Descarte efectivo a coste 0 AP aumentando espacio en mano")
    void shouldDiscardCardAtZeroCost() {
        Board board = Board.empty();
        Card card = new CycleCard("c1", "Cycle", 1);
        PlayerHand hand = new PlayerHand(List.of(card));
        Deck deck = new Deck(List.of());
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, deck);

        // Intentar descartar la carta
        DiscardCardAction discardAction = new DiscardCardAction(card);
        assertTrue(discardAction.isValid(state));
        assertTrue(discardAction.execute(state));

        // Debe haberse retirado de la mano y estar en la pila de descartes
        assertEquals(0, state.playerHand().size());
        assertEquals(1, state.deck().getDiscardPile().size());
        assertEquals(card, state.deck().getDiscardPile().get(0));

        // El coste debió ser 0 AP (sigue con 3 AP iniciales)
        assertEquals(3, state.actionPoints().current());
    }

    @Test
    @DisplayName("a2) Uso correcto de CycleCard en mano (coste 1 AP, roba 2 cartas)")
    void shouldPlayCycleCardCorrectly() {
        Board board = Board.empty();
        CycleCard cycle = new CycleCard("cycle", "Cycle Hand", 1);
        Card shield = new Card("shield", "Shield", 2);
        PlayerHand hand = new PlayerHand(List.of(cycle, shield));
        Deck deck = new Deck(List.of(
            new Card("drawn1", "Drawn Card 1", 1),
            new Card("drawn2", "Drawn Card 2", 1)
        ));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, deck);

        CardPlayAction playCard = new CardPlayAction(cycle, new GlobalTarget());
        assertTrue(playCard.isValid(state));
        assertTrue(playCard.execute(state));

        // Debe haber consumido Cycle Hand, mantenido Shield, y robado Drawn Card 1 y 2 (mano final de tamaño 3)
        assertEquals(3, state.playerHand().size());
        assertTrue(state.playerHand().contains(shield));
        assertEquals("drawn1", state.playerHand().cards().get(1).id());
        assertEquals("drawn2", state.playerHand().cards().get(2).id());

        // Coste de AP fue 1 (3 iniciales - 1 = 2 AP)
        assertEquals(2, state.actionPoints().current());
    }

    @Test
    @DisplayName("b) Aplicación del salto táctico sobre una Dama")
    void shouldApplyTacticalJumpToQueen() {
        Board board = Board.empty();
        Square a1 = Square.of('a', 1); // Dama Blanca
        Square a2 = Square.of('a', 2); // Peón Blanco (obstáculo)
        Square a3 = Square.of('a', 3);

        Queen queen = new Queen(PieceColor.WHITE);
        board.placePieceInternal(a1, queen);
        board.placePieceInternal(a2, new Pawn(PieceColor.WHITE));

        TacticalJumpCard jumpCard = new TacticalJumpCard("jump_1", "Tactical Jump", 2);
        PlayerHand hand = new PlayerHand(List.of(jumpCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Jugar carta sobre la Dama
        CardPlayAction playCard = new CardPlayAction(jumpCard, new PieceTarget(a1));
        assertTrue(playCard.isValid(state));
        assertTrue(playCard.execute(state));

        // Otorgar AP adicionales para poder realizar el movimiento de pieza (cuesta 2 AP, quedaba 1 AP)
        state.addAP(2);

        // Dama debe poder saltar el peón a a3
        PieceMoveAction jumpMove = new PieceMoveAction(a1, a3);
        assertTrue(jumpMove.isValid(state));
        assertTrue(jumpMove.execute(state));

        // Tras el movimiento el decorador debe expirar
        Piece movedQueen = board.getPieceAt(a3).orElseThrow();
        assertFalse(movedQueen instanceof JumpModifierDecorator);
    }

    @Test
    @DisplayName("c) Intercambio de posiciones entre Peón y Caballo con RegroupCard")
    void shouldRegroupPawnAndKnight() {
        Board board = Board.empty();
        Square e2 = Square.of('e', 2); // Peón Blanco
        Square e4 = Square.of('e', 4); // Caballo Blanco (distancia rank = 2, Chebyshev = 2 <= 3)

        Pawn pawn = new Pawn(PieceColor.WHITE);
        Knight knight = new Knight(PieceColor.WHITE);

        board.placePieceInternal(e2, pawn);
        board.placePieceInternal(e4, knight);

        RegroupCard regroupCard = new RegroupCard("regroup_1", "Regroup", 2);
        PlayerHand hand = new PlayerHand(List.of(regroupCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Jugar la carta seleccionando ambas piezas
        CardPlayAction playCard = new CardPlayAction(regroupCard, new DoublePieceTarget(e2, e4));
        assertTrue(playCard.isValid(state));
        assertTrue(playCard.execute(state));

        // Las posiciones deben haberse intercambiado
        assertEquals(knight, PieceDecorator.basePiece(board.getPieceAt(e2).orElseThrow()));
        assertEquals(pawn, PieceDecorator.basePiece(board.getPieceAt(e4).orElseThrow()));
    }

    @Test
    @DisplayName("c2) Excepción al intentar reagrupar peón en fila extrema sin pieza de coronación")
    void shouldThrowExceptionWhenPawnSwappedToEndRankWithoutPromotionType() {
        Board board = Board.empty();
        Square a7 = Square.of('a', 7); // Peón Blanco
        Square a8 = Square.of('a', 8); // Caballo Blanco en la octava fila

        Pawn pawn = new Pawn(PieceColor.WHITE);
        Knight knight = new Knight(PieceColor.WHITE);

        board.placePieceInternal(a7, pawn);
        board.placePieceInternal(a8, knight);

        RegroupCard regroupCard = new RegroupCard("regroup_1", "Regroup", 2);
        PlayerHand hand = new PlayerHand(List.of(regroupCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Jugar sin proveer tipo de coronación (debe lanzar IllegalArgumentException)
        CardPlayAction playCard = new CardPlayAction(regroupCard, new DoublePieceTarget(a7, a8, java.util.Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> playCard.isValid(state));
    }

    @Test
    @DisplayName("c3) Reagrupación exitosa con coronación a Caballo (Knight) al especificar el parámetro")
    void shouldPromoteToKnightWhenPawnSwappedToEndRankWithPromotionType() {
        Board board = Board.empty();
        Square a7 = Square.of('a', 7); // Peón Blanco
        Square a8 = Square.of('a', 8); // Torre Blanca en la octava fila

        Pawn pawn = new Pawn(PieceColor.WHITE);
        Rook rook = new Rook(PieceColor.WHITE);

        board.placePieceInternal(a7, pawn);
        board.placePieceInternal(a8, rook);

        RegroupCard regroupCard = new RegroupCard("regroup_1", "Regroup", 2);
        PlayerHand hand = new PlayerHand(List.of(regroupCard));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Jugar especificando coronación a KNIGHT (N)
        CardPlayAction playCard = new CardPlayAction(regroupCard, new DoublePieceTarget(a7, a8, java.util.Optional.of(PieceType.KNIGHT)));
        assertTrue(playCard.isValid(state));
        assertTrue(playCard.execute(state));

        // En a8 debe haber un Caballo Blanco (Knight) coronado
        Piece pieceAtA8 = board.getPieceAt(a8).orElseThrow();
        assertEquals(PieceType.KNIGHT, PieceDecorator.basePiece(pieceAtA8).type());
        assertEquals(PieceColor.WHITE, pieceAtA8.color());
    }

    @Test
    @DisplayName("e) Denegación de jugada de carta si esta expone al Rey propio a Jaque")
    void shouldRejectCardPlayIfItExposesKingToCheck() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1); // Rey Blanco
        Square e2 = Square.of('e', 2); // Peón Blanco (clavado)
        Square e8 = Square.of('e', 8); // Torre Negra (atacante)

        board.placePieceInternal(e1, new King(PieceColor.WHITE));
        board.placePieceInternal(e2, new Pawn(PieceColor.WHITE));
        board.placePieceInternal(e8, new Rook(PieceColor.BLACK));

        // Peón Blanco tiene SideStepCard en mano
        com.tacticalgambit.core.domain.card.impl.SideStepCard sideStep = new com.tacticalgambit.core.domain.card.impl.SideStepCard("side_1", "Side Step", 1);
        PlayerHand hand = new PlayerHand(List.of(sideStep));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // El peón intenta desplazarse horizontalmente a la columna 'f' usando la carta
        CardPlayAction playCard = new CardPlayAction(sideStep, new PieceTarget(e2));

        // Debe ser inválido porque mover el peón expone al Rey Blanco a Jaque (rompe la clavada)
        assertFalse(playCard.isValid(state));
    }

    @Test
    @DisplayName("d) Ganancia de +2 AP actual y reducción a 1 AP en el turno siguiente por OverchargeCard")
    void shouldApplyOverchargeWithPenaltyAndFloorAP() {
        Board board = Board.empty();
        OverchargeCard overchargeCard1 = new OverchargeCard("over_1", "Overcharge", 0);
        OverchargeCard overchargeCard2 = new OverchargeCard("over_2", "Overcharge", 0);
        PlayerHand hand = new PlayerHand(List.of(overchargeCard1, overchargeCard2));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // 1. Jugar la primera sobrecarga
        CardPlayAction play1 = new CardPlayAction(overchargeCard1, new GlobalTarget());
        assertTrue(play1.isValid(state));
        assertTrue(play1.execute(state));

        // Debe haber ganado +2 AP (3 iniciales + 2 = 5 AP)
        assertEquals(5, state.actionPoints().current());

        // 2. Intento de jugar la segunda sobrecarga en el mismo turno (debe fallar por límite de 1 por turno)
        CardPlayAction play2 = new CardPlayAction(overchargeCard2, new GlobalTarget());
        assertFalse(play2.isValid(state));

        // Consumir todos los AP de Blanco para evaluar la penalización de forma neta
        state.consumeAP(5);

        // 3. Pasar turno a Negro
        state.startNextTurn();
        assertEquals(PieceColor.BLACK, state.activePlayer());
        // Negro recibe sus AP normales (0 iniciales + 3 recarga = 3 AP)
        assertEquals(3, state.actionPoints().current());

        // Consumir AP de Negro para que no afecte posteriores comprobaciones si fuera necesario
        state.consumeAP(3);

        // 4. Pasar turno de vuelta a Blanco (debe aplicarse la penalización de -2 AP sobre los 3 base, resultando en 1 AP)
        state.startNextTurn();
        assertEquals(PieceColor.WHITE, state.activePlayer());
        assertEquals(1, state.actionPoints().current());
    }

    @Test
    @DisplayName("f) Desplazamiento horizontal libremente elegido mediante DoublePieceTarget en SideStepCard")
    void shouldAllowSideStepWithDoublePieceTarget() {
        Board board = Board.empty();
        Square e2 = Square.of('e', 2); // Peón Blanco
        Square d2 = Square.of('d', 2); // Casilla vacía izquierda
        Square f2 = Square.of('f', 2); // Casilla vacía derecha

        board.placePieceInternal(e2, new Pawn(PieceColor.WHITE));

        com.tacticalgambit.core.domain.card.impl.SideStepCard sideStep = new com.tacticalgambit.core.domain.card.impl.SideStepCard("side_1", "Side Step", 1);
        PlayerHand hand = new PlayerHand(List.of(sideStep));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, Deck.of());

        // Probar a la izquierda
        CardPlayAction playLeft = new CardPlayAction(sideStep, new com.tacticalgambit.core.domain.card.DoublePieceTarget(e2, d2));
        assertTrue(playLeft.isValid(state));
        assertTrue(playLeft.execute(state));
        assertTrue(board.getPieceAt(d2).isPresent());
        assertFalse(board.getPieceAt(e2).isPresent());
    }

    @Test
    @DisplayName("g) Permitir jugar cartas globales (como Cycle Hand) cuando el propio Rey está en Jaque")
    void shouldAllowCardPlayWhileInCheckIfItDoesNotWorsenCheck() {
        Board board = Board.empty();
        Square e1 = Square.of('e', 1); // Rey Blanco
        Square e8 = Square.of('e', 8); // Torre Negra (jaque al Rey Blanco)

        board.placePieceInternal(e1, new King(PieceColor.WHITE));
        board.placePieceInternal(e8, new Rook(PieceColor.BLACK));

        // El Rey está en jaque por la Torre
        assertTrue(com.tacticalgambit.core.state.CheckDetector.isInCheck(board, PieceColor.WHITE));

        // El jugador tiene CycleCard en mano
        com.tacticalgambit.core.domain.card.impl.CycleCard cycleCard = new com.tacticalgambit.core.domain.card.impl.CycleCard("cycle_1", "Cycle Hand", 1);
        PlayerHand hand = new PlayerHand(List.of(cycleCard));
        
        // Mazo con al menos una carta para poder jugar Cycle
        java.util.List<Card> deckList = new java.util.ArrayList<>();
        deckList.add(new com.tacticalgambit.core.domain.card.impl.SideStepCard("side_1", "Side Step", 1));
        TurnState state = new TurnState(PieceColor.WHITE, board, hand, new Deck(deckList));

        // Jugar la carta Cycle Hand debe ser válido porque no cambia la disposición de piezas (no empeora el jaque)
        CardPlayAction playCard = new CardPlayAction(cycleCard, new GlobalTarget());
        assertTrue(playCard.isValid(state));
        assertTrue(playCard.execute(state));
    }
}
