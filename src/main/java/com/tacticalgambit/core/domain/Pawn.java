package com.tacticalgambit.core.domain;

import java.util.Optional;

public record Pawn(PieceColor color) implements Piece {
    @Override
    public PieceType type() {
        return PieceType.PAWN;
    }

    @Override
    public boolean canMove(Board board, Square from, Square to) {
        if (board == null || from == null || to == null || from.equals(to)) {
            return false;
        }

        // Fuego amigo: Rechazar si el destino contiene pieza propia
        Optional<Piece> destPiece = board.getPieceAt(to);
        if (destPiece.isPresent() && destPiece.get().color() == color) {
            return false;
        }

        int deltaFile = to.file() - from.file();
        int deltaRank = to.rank() - from.rank();
        int direction = (color == PieceColor.WHITE) ? 1 : -1;
        int initialRank = (color == PieceColor.WHITE) ? 1 : 6;

        // 1. Avance frontal simple (1 casilla adelante a vacía)
        if (deltaFile == 0 && deltaRank == direction) {
            return board.isEmpty(to);
        }

        // 2. Avance doble desde posición inicial (2 casillas adelante a vacía con camino despejado)
        if (deltaFile == 0 && deltaRank == 2 * direction && from.rank() == initialRank) {
            return board.isEmpty(to) && board.isPathClear(from, to);
        }

        // 3. Captura diagonal (1 casilla diagonal hacia adelante si está ocupada por pieza enemiga)
        if (Math.abs(deltaFile) == 1 && deltaRank == direction) {
            return destPiece.isPresent() && destPiece.get().color() != color;
        }

        return false;
    }
}
