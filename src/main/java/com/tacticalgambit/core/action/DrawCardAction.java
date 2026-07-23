package com.tacticalgambit.core.action;

import com.tacticalgambit.core.domain.Card;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * Acción atómica de robar carta del mazo.
 * 
 * Reglas de negocio:
 * - Costo: 1 AP.
 * - Invariante estricta: Rechazo si la mano excede el límite de 4 cartas.
 */
public record DrawCardAction() implements GameAction {

    public static final int DRAW_AP_COST = 1;

    @Override
    public boolean isValid(TurnState state) {
        if (state == null) {
            return false;
        }
        // Invariante 1: AP suficientes (1 AP)
        if (!state.actionPoints().canAfford(DRAW_AP_COST)) {
            return false;
        }
        // Invariante 2: Mano no llena (máximo 4 cartas)
        if (state.playerHand().isFull()) {
            return false;
        }
        // Invariante 3: Cartas disponibles en el mazo
        return !state.deck().isEmpty();
    }

    @Override
    public boolean execute(TurnState state) {
        if (!isValid(state)) {
            return false;
        }
        Optional<Card> cardOpt = state.deck().drawCard();
        if (cardOpt.isEmpty()) {
            return false;
        }
        state.consumeAP(DRAW_AP_COST);
        state.playerHand().addCard(cardOpt.get());
        return true;
    }
}
