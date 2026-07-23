package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.state.TurnState;

/**
 * OverchargeCard: Suma +2 AP inmediatamente a coste 0 AP, aplicando una penalización
 * de -2 AP al inicio del siguiente turno. Límite de 1 uso por turno.
 */
public class OverchargeCard extends Card {

    public OverchargeCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (!(target instanceof GlobalTarget)) {
            return false;
        }
        // Restricción de uso: máximo 1 sobrecarga por turno
        return !state.hasPlayedOverchargeThisTurn();
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        state.addAP(2);
        state.setActiveApPenalty(2);
        state.markOverchargePlayed();
    }
}
