package com.tacticalgambit.core.action;

import com.tacticalgambit.core.state.TurnState;

/**
 * Interface sellada para el motor de acciones atómicas Forward-Only (No-Undo Policy).
 * Define el contrato de validación y ejecución irreversible sobre el TurnState.
 */
public sealed interface GameAction permits PieceMoveAction, CardPlayAction, DrawCardAction, DiscardCardAction {

    /**
     * Evalúa si la acción es legal en el estado actual del turno.
     */
    boolean isValid(TurnState state);

    /**
     * Ejecuta atómicamente la acción sobre el TurnState si es válida.
     * @return true si la ejecución fue exitosa; false en caso contrario.
     */
    boolean execute(TurnState state);
}
