package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * RegroupCard: Intercambia las posiciones de dos piezas aliadas a distancia <= 3.
 * Al menos una debe ser un Peón y ninguna puede ser el Rey.
 * Si un peón es posicionado en la fila de coronación, requiere especificar la pieza de destino.
 */
public class RegroupCard extends Card {

    public RegroupCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (!(target instanceof DoublePieceTarget doubleTarget)) {
            return false;
        }

        Square s1 = doubleTarget.firstSquare();
        Square s2 = doubleTarget.secondSquare();

        Optional<Piece> p1Opt = state.board().getPieceAt(s1);
        Optional<Piece> p2Opt = state.board().getPieceAt(s2);

        if (p1Opt.isEmpty() || p2Opt.isEmpty()) {
            return false;
        }

        Piece p1 = p1Opt.get();
        Piece p2 = p2Opt.get();

        if (p1.color() != state.activePlayer() || p2.color() != state.activePlayer()) {
            return false;
        }

        Piece base1 = PieceDecorator.basePiece(p1);
        Piece base2 = PieceDecorator.basePiece(p2);

        if (base1.type() == PieceType.KING || base2.type() == PieceType.KING) {
            return false;
        }

        // Evitar intercambiar piezas del mismo tipo (por ejemplo, dos peones o dos caballos)
        if (base1.type() == base2.type()) {
            return false;
        }

        if (base1.type() != PieceType.PAWN && base2.type() != PieceType.PAWN) {
            return false;
        }

        // Si p1 (Peón) se mueve a la fila de coronación de s2
        if (base1.type() == PieceType.PAWN) {
            int targetRank = (base1.color() == PieceColor.WHITE) ? 7 : 0;
            if (s2.rank() == targetRank && doubleTarget.promotionType().isEmpty()) {
                throw new IllegalArgumentException("Se requiere especificar la pieza de coronación (Q, R, B, N) para esta jugada.");
            }
        }

        // Si p2 (Peón) se mueve a la fila de coronación de s1
        if (base2.type() == PieceType.PAWN) {
            int targetRank = (base2.color() == PieceColor.WHITE) ? 7 : 0;
            if (s1.rank() == targetRank && doubleTarget.promotionType().isEmpty()) {
                throw new IllegalArgumentException("Se requiere especificar la pieza de coronación (Q, R, B, N) para esta jugada.");
            }
        }

        int deltaFile = Math.abs(s1.file() - s2.file());
        int deltaRank = Math.abs(s1.rank() - s2.rank());
        int distance = Math.max(deltaFile, deltaRank);

        return distance <= 3;
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (!(target instanceof DoublePieceTarget doubleTarget)) {
            return;
        }

        Square s1 = doubleTarget.firstSquare();
        Square s2 = doubleTarget.secondSquare();

        Piece p1 = state.board().removePieceInternal(s1).orElseThrow();
        Piece p2 = state.board().removePieceInternal(s2).orElseThrow();

        Piece base1 = PieceDecorator.basePiece(p1);
        if (base1 instanceof Pawn pawn) {
            int targetRank = (pawn.color() == PieceColor.WHITE) ? 7 : 0;
            if (s2.rank() == targetRank) {
                PieceType promo = doubleTarget.promotionType().orElse(PieceType.QUEEN);
                p1 = createPromotedPiece(pawn.color(), promo);
            }
        }

        Piece base2 = PieceDecorator.basePiece(p2);
        if (base2 instanceof Pawn pawn) {
            int targetRank = (pawn.color() == PieceColor.WHITE) ? 7 : 0;
            if (s1.rank() == targetRank) {
                PieceType promo = doubleTarget.promotionType().orElse(PieceType.QUEEN);
                p2 = createPromotedPiece(pawn.color(), promo);
            }
        }

        state.board().placePieceInternal(s1, p2);
        state.board().placePieceInternal(s2, p1);
    }

    private Piece createPromotedPiece(PieceColor color, PieceType type) {
        return switch (type) {
            case QUEEN -> new Queen(color);
            case ROOK -> new Rook(color);
            case BISHOP -> new Bishop(color);
            case KNIGHT -> new Knight(color);
            default -> throw new IllegalArgumentException("Pieza de coronación no permitida: " + type);
        };
    }
}
