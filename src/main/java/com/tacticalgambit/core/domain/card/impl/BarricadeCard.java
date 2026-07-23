package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.SquareTarget;
import com.tacticalgambit.core.state.TurnState;

/**
 * BarricadeCard: Convierte una casilla vacía en un obstáculo intransitable durante 2 turnos completos.
 * Ninguna pieza (salvo el Caballo) puede atravesarla o finalizar su movimiento en ella. Coste: 3 AP.
 */
public class BarricadeCard extends Card {

    public BarricadeCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (!(target instanceof SquareTarget squareTarget)) {
            return false;
        }

        Square square = squareTarget.square();

        // Debe estar libre (sin piezas y sin barricadas activas)
        return state.board().isEmpty(square) && !state.board().isBarricaded(square);
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (!(target instanceof SquareTarget squareTarget)) {
            return;
        }
        Square square = squareTarget.square();
        state.board().addBarricade(square, 4);
    }
}
