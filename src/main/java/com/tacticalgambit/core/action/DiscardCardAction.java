package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.Card;
import com.tacticalgambit.core.state.TurnState;

/**
 * Acción de descarte de una carta de la mano a coste 0 AP.
 */
public record DiscardCardAction(Card card) implements GameAction {

    public DiscardCardAction {
        if (card == null) {
            throw new IllegalArgumentException("La carta a descartar es requerida.");
        }
    }

    @Override
    public boolean isValid(TurnState state) {
        if (state == null) {
            return false;
        }
        return state.playerHand().contains(card);
    }

    @Override
    public boolean execute(TurnState state) {
        if (!isValid(state)) {
            return false;
        }
        state.playerHand().removeCard(card);
        state.deck().addToDiscardPile(card);
        return true;
    }
}
