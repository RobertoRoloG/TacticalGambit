package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * TacticalJumpCard: Otorga a una Torre o Alfil el flag temporal de ignorar la primera pieza intermedia.
 */
public class TacticalJumpCard extends Card {

    public TacticalJumpCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (!(target instanceof PieceTarget pieceTarget)) {
            return false;
        }

        Optional<Piece> pieceOpt = state.board().getPieceAt(pieceTarget.pieceSquare());
        if (pieceOpt.isEmpty()) {
            return false;
        }

        Piece piece = PieceDecorator.basePiece(pieceOpt.get());
        // Aplicable a Rook, Bishop o Queen
        return piece instanceof Rook || piece instanceof Bishop || piece instanceof Queen;
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (!(target instanceof PieceTarget pieceTarget)) {
            return;
        }
        Square square = pieceTarget.pieceSquare();
        Piece originalPiece = state.board().removePieceInternal(square).orElseThrow();
        Piece decoratedPiece = new JumpModifierDecorator(originalPiece);
        state.board().placePieceInternal(square, decoratedPiece);
    }
}
