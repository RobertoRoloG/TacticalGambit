package com.tacticalgambit.core.domain.card.impl;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * SideStepCard: Permite a un peón desplazarse 1 casilla en horizontal a una casilla vacía.
 * Admite tanto PieceTarget (1 objetivo: autodecido derecha/izquierda) como DoublePieceTarget (2 objetivos: peón y destino).
 */
public class SideStepCard extends Card {

    public SideStepCard(String id, String name, int apCost) {
        super(id, name, apCost);
    }

    @Override
    public boolean canPlay(TurnState state, CardTarget target) {
        if (target instanceof DoublePieceTarget doubleTarget) {
            Square from = doubleTarget.firstSquare();
            Square to = doubleTarget.secondSquare();

            Optional<Piece> pieceOpt = state.board().getPieceAt(from);
            if (pieceOpt.isEmpty()) {
                return false;
            }

            Piece piece = PieceDecorator.basePiece(pieceOpt.get());
            if (!(piece instanceof Pawn) || pieceOpt.get().color() != state.activePlayer()) {
                return false;
            }

            // Validar que el destino esté vacío
            if (!state.board().isEmpty(to)) {
                return false;
            }

            // Validar paso horizontal de 1 casilla
            return to.rank() == from.rank() && Math.abs(to.file() - from.file()) == 1;
        }

        if (target instanceof PieceTarget pieceTarget) {
            Square square = pieceTarget.pieceSquare();
            Optional<Piece> pieceOpt = state.board().getPieceAt(square);
            if (pieceOpt.isEmpty()) {
                return false;
            }

            Piece piece = PieceDecorator.basePiece(pieceOpt.get());
            if (!(piece instanceof Pawn) || pieceOpt.get().color() != state.activePlayer()) {
                return false;
            }

            int file = square.file();
            int rank = square.rank();

            boolean leftVacant = (file > 0) && state.board().isEmpty(new Square(file - 1, rank));
            boolean rightVacant = (file < 7) && state.board().isEmpty(new Square(file + 1, rank));

            return leftVacant || rightVacant;
        }

        return false;
    }

    @Override
    public void apply(TurnState state, CardTarget target) {
        if (target instanceof DoublePieceTarget doubleTarget) {
            Square from = doubleTarget.firstSquare();
            Square to = doubleTarget.secondSquare();
            Piece pawn = state.board().removePieceInternal(from).orElseThrow();
            state.board().placePieceInternal(to, pawn);
            return;
        }

        if (target instanceof PieceTarget pieceTarget) {
            Square square = pieceTarget.pieceSquare();
            Piece pawn = state.board().removePieceInternal(square).orElseThrow();

            int file = square.file();
            int rank = square.rank();

            // Mover a la derecha por defecto si está libre, de lo contrario a la izquierda
            Square destination;
            if (file < 7 && state.board().isEmpty(new Square(file + 1, rank))) {
                destination = new Square(file + 1, rank);
            } else {
                destination = new Square(file - 1, rank);
            }

            state.board().placePieceInternal(destination, pawn);
        }
    }
}
