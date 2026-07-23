package com.tacticalgambit.core.command;

import com.tacticalgambit.core.domain.Board;

/**
 * Interfaz base para el patrón Command enfocado en las acciones de movimiento y su reversibilidad (undo/redo).
 */
public interface MoveCommand {

    /**
     * Ejecuta el comando sobre el tablero indicado.
     * @param board Tablero sobre el cual aplicar la acción.
     * @return true si el movimiento fue exitoso, false en caso contrario.
     */
    boolean execute(Board board);

    /**
     * Revierte el efecto del comando restaurando el estado previo en el tablero.
     * @param board Tablero sobre el cual aplicar el deshacer.
     */
    void undo(Board board);
}
