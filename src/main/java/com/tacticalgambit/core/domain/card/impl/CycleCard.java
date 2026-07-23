package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.state.TurnState;

/**
 * CycleCard: Coste 1 AP. Roba hasta 2 cartas del mazo (respetando el límite de mano de 4 cartas).
 */
public class CycleCard extends Card {

    public CycleCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        // La mano final no debe exceder 4 cartas tras el descarte propio (-1) y el robo (+2) -> neto de +1.
        // Así, la mano actual debe ser <= 3. Y debe haber al menos 1 carta en el mazo.
        return state.playerHand().size() <= 3 && !state.deck().isEmpty();
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        // Robar primera carta
        state.deck().drawCard().ifPresent(drawn -> state.playerHand().addCard(drawn));
        
        // Robar segunda carta si la mano aún no está llena
        if (!state.playerHand().isFull()) {
            state.deck().drawCard().ifPresent(drawn -> state.playerHand().addCard(drawn));
        }
    }
}
