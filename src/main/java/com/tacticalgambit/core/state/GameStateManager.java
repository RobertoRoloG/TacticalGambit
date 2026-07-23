package com.tacticalgambit.core.state;

import com.tacticalgambit.core.domain.GameConditionChecker;

/**
 * Gestor del ciclo de vida y fin de partida. Evalúa las condiciones de Jaque Mate y ahogado.
 */
public class GameStateManager {

    /**
     * Evalúa si el jugador activo tiene acciones válidas disponibles.
     * Si no tiene acciones y está en Jaque, transiciona a CHECKMATE.
     * Si no tiene acciones y no está en Jaque, transiciona a STALEMATE.
     */
    public static void checkAndUpdateGameState(TurnState state) {
        if (state == null || state.gameState() != GameState.IN_PROGRESS) {
            return;
        }

        if (!GameConditionChecker.hasAnyLegalAction(state)) {
            if (GameConditionChecker.isInCheck(state.board(), state.activePlayer())) {
                state.setGameState(GameState.CHECKMATE);
            } else {
                state.setGameState(GameState.STALEMATE);
            }
        }
    }
}
