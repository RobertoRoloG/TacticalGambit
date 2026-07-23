package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.SquareTarget;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.state.TurnState;

import java.util.Optional;

/**
 * Acción atómica de jugar una carta táctica sobre un objetivo específico (CardTarget).
 * 
 * Reglas de negocio:
 * - Costo dinámico en AP según card.apCost().
 * - PieceTarget: Sólo piezas propias y NUNCA de tipo KING.
 * - SquareTarget: No aplicable en casilla del Rey enemigo.
 * - Regla Antimaten: Rechazo si el resultado coloca al Rey enemigo en estado de Jaque.
 */
public record CardPlayAction(Card card, CardTarget target) implements GameAction {

    public CardPlayAction {
        if (card == null || target == null) {
            throw new IllegalArgumentException("La carta y el objetivo son requeridos.");
        }
    }

    @Override
    public boolean isValid(TurnState state) {
        if (state == null) {
            return false;
        }

        // Invariante 1: Posesión de la carta en mano
        if (!state.playerHand().contains(card)) {
            return false;
        }

        // Invariante 2: AP suficientes
        if (!state.actionPoints().canAfford(card.apCost())) {
            return false;
        }

        // Invariante 3: Pattern Matching según la taxonomía del objetivo (CardTarget)
        boolean targetValid = switch (target) {
            case PieceTarget(Square pieceSquare) -> {
                Optional<Piece> pieceOpt = state.board().getPieceAt(pieceSquare);
                if (pieceOpt.isEmpty()) {
                    yield false;
                }
                Piece piece = pieceOpt.get();
                Piece basePiece = PieceDecorator.basePiece(piece);
                // Regla de Piezas Propias y exclusión del Rey propio
                yield piece.color() == state.activePlayer() && basePiece.type() != PieceType.KING;
            }
            case SquareTarget(Square square) -> {
                Optional<Piece> pieceOpt = state.board().getPieceAt(square);
                // Regla Terreno: No puede aplicarse en la casilla del Rey enemigo
                if (pieceOpt.isPresent()) {
                    Piece piece = pieceOpt.get();
                    Piece basePiece = PieceDecorator.basePiece(piece);
                    if (piece.color() == state.activePlayer().opposite() && basePiece.type() == PieceType.KING) {
                        yield false;
                    }
                }
                yield true;
            }
            case GlobalTarget() -> true;
            case DoublePieceTarget(Square firstSquare, Square secondSquare, java.util.Optional<PieceType> promo) -> {
                if ("Regroup".equals(card.name())) {
                    Optional<Piece> p1 = state.board().getPieceAt(firstSquare);
                    Optional<Piece> p2 = state.board().getPieceAt(secondSquare);
                    if (p1.isEmpty() || p2.isEmpty()) {
                        yield false;
                    }
                    Piece piece1 = p1.get();
                    Piece piece2 = p2.get();
                    Piece base1 = PieceDecorator.basePiece(piece1);
                    Piece base2 = PieceDecorator.basePiece(piece2);
                    yield piece1.color() == state.activePlayer() && piece2.color() == state.activePlayer()
                            && base1.type() != PieceType.KING && base2.type() != PieceType.KING;
                }
                if ("Side Step".equals(card.name())) {
                    Optional<Piece> p1 = state.board().getPieceAt(firstSquare);
                    if (p1.isEmpty()) {
                        yield false;
                    }
                    Piece piece1 = p1.get();
                    Piece base1 = PieceDecorator.basePiece(piece1);
                    yield piece1.color() == state.activePlayer() && base1.type() != PieceType.KING;
                }
                yield true;
            }
        };

        if (!targetValid) {
            return false;
        }

        // Invariante de validación específica de la propia carta
        if (!card.canPlay(state, target)) {
            return false;
        }

        // Invariante 5: Prevención de Jaque propio por cartas (no exponer al propio Rey a nuevos ataques)
        Board tempBoard = new Board(state.board().pieces());
        TurnState tempState = new TurnState(state.activePlayer(), tempBoard, new PlayerHand(state.playerHand().cards()), state.deck().copy());
        java.util.Set<Square> checkingBefore = com.tacticalgambit.core.state.CheckDetector.getCheckingSquares(state.board(), state.activePlayer());
        card.apply(tempState, target);
        java.util.Set<Square> checkingAfter = com.tacticalgambit.core.state.CheckDetector.getCheckingSquares(tempBoard, state.activePlayer());
        if (!checkingBefore.containsAll(checkingAfter)) {
            return false;
        }

        // Invariante 4: Regla Antimaten (Sin amenazas directas de Jaque al Rey enemigo por cartas)
        PieceColor enemyColor = state.activePlayer().opposite();
        return !GameConditionChecker.isInCheck(state.board(), enemyColor);
    }

    @Override
    public boolean execute(TurnState state) {
        if (!isValid(state)) {
            return false;
        }
        state.consumeAP(card.apCost());
        card.apply(state, target);
        state.playerHand().removeCard(card);
        return true;
    }
}
