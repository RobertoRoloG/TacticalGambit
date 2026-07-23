package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * ShieldCard: Envuelve a la pieza objetivo en un ShieldedPieceDecorator.
 */
public class ShieldCard extends Card {

    public ShieldCard(String id, String name, int apCost) {
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
        // No se puede proteger una pieza ya protegida con escudo
        return !PieceDecorator.isShielded(pieceOpt.get());
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (!(target instanceof PieceTarget pieceTarget)) {
            return;
        }
        Square square = pieceTarget.pieceSquare();
        Piece originalPiece = state.board().removePieceInternal(square).orElseThrow();
        Piece shieldedPiece = new ShieldedDecorator(originalPiece);
        state.board().placePieceInternal(square, shieldedPiece);
    }
}
