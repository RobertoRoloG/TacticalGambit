package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * TacticalDashCard: Mueve una pieza propia (no Rey ni Peón) 1 casilla en cualquier dirección a una casilla vacía,
 * sin generar amenaza de Jaque directo al Rey enemigo. Coste: 1 AP.
 */
public class TacticalDashCard extends Card {

    public TacticalDashCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (!(target instanceof DoublePieceTarget doubleTarget)) {
            return false;
        }

        Square s1 = doubleTarget.firstSquare();
        Square s2 = doubleTarget.secondSquare();

        Optional<Piece> pOpt = state.board().getPieceAt(s1);
        if (pOpt.isEmpty()) {
            return false;
        }

        Piece piece = pOpt.get();
        if (piece.color() != state.activePlayer()) {
            return false;
        }

        Piece base = PieceDecorator.basePiece(piece);
        if (base.type() == PieceType.KING || base.type() == PieceType.PAWN) {
            return false;
        }

        // Casilla de destino debe estar vacía y sin barricadas
        if (!state.board().isEmpty(s2) || state.board().isBarricaded(s2)) {
            return false;
        }

        // Distancia debe ser exactamente 1 casilla en cualquier dirección
        int deltaFile = Math.abs(s1.file() - s2.file());
        int deltaRank = Math.abs(s1.rank() - s2.rank());
        if (Math.max(deltaFile, deltaRank) != 1) {
            return false;
        }

        // Simular movimiento para verificar jaque directo
        Board simulatedBoard = state.board().simulateMove(s1, s2);
        
        // Crear un tablero con solo las piezas enemigas y la pieza movida
        java.util.Map<Square, Piece> cleanGrid = new java.util.HashMap<>();
        PieceColor enemyColor = state.activePlayer().opposite();
        
        for (java.util.Map.Entry<Square, Piece> entry : simulatedBoard.pieces().entrySet()) {
            if (entry.getValue().color() == enemyColor || entry.getKey().equals(s2)) {
                cleanGrid.put(entry.getKey(), entry.getValue());
            }
        }
        
        Board cleanBoard = new Board(cleanGrid, simulatedBoard.barricades());
        if (com.tacticalgambit.core.state.CheckDetector.isInCheck(cleanBoard, enemyColor)) {
            return false; // Genera jaque directo al Rey enemigo
        }

        return true;
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (!(target instanceof DoublePieceTarget doubleTarget)) {
            return;
        }
        Square s1 = doubleTarget.firstSquare();
        Square s2 = doubleTarget.secondSquare();

        Piece piece = state.board().removePieceInternal(s1).orElseThrow();
        state.board().placePieceInternal(s2, piece);
    }
}
