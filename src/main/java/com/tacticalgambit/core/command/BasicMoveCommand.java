package com.tacticalgambit.core.command;

import com.tacticalgambit.core.domain.Board;
import com.tacticalgambit.core.domain.Piece;
import com.tacticalgambit.core.domain.Square;
import java.util.Optional;

/**
 * Comando de movimiento básico entre dos casillas con soporte de undo/redo.
 */
public class BasicMoveCommand implements MoveCommand {

    private final Square from;
    private final Square to;
    private Piece movedPiece;
    private Optional<Piece> capturedPiece = Optional.empty();
    private boolean executed = false;

    public BasicMoveCommand(Square from, Square to) {
        if (from.equals(to)) {
            throw new IllegalArgumentException("La casilla de origen y destino no pueden ser idénticas.");
        }
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(Board board) {
        if (executed) {
            return false;
        }

        Optional<Piece> sourcePieceOpt = board.getPieceAt(from);
        if (sourcePieceOpt.isEmpty()) {
            return false;
        }

        this.movedPiece = sourcePieceOpt.get();
        this.capturedPiece = board.getPieceAt(to);

        board.removePieceInternal(from);
        board.placePieceInternal(to, movedPiece);
        this.executed = true;
        return true;
    }

    @Override
    public void undo(Board board) {
        if (!executed) {
            return;
        }

        board.removePieceInternal(to);
        board.placePieceInternal(from, movedPiece);

        capturedPiece.ifPresent(piece -> board.placePieceInternal(to, piece));
        this.executed = false;
    }

    public Square getFrom() {
        return from;
    }

    public Square getTo() {
        return to;
    }

    public Optional<Piece> getCapturedPiece() {
        return capturedPiece;
    }
}
