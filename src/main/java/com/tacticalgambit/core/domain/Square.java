package com.tacticalgambit.core.domain;

/**
 * Representa una coordenada inmutable en el tablero de ajedrez (8x8).
 * 
 * @param file Columna de la casilla (0 a 7, representando 'a' a 'h').
 * @param rank Fila de la casilla (0 a 7, representando '1' a '8').
 */
public record Square(int file, int rank) {

    public Square {
        if (file < 0 || file > 7) {
            throw new IllegalArgumentException("Columna (file) fuera de límites [0-7]: " + file);
        }
        if (rank < 0 || rank > 7) {
            throw new IllegalArgumentException("Fila (rank) fuera de límites [0-7]: " + rank);
        }
    }

    /**
     * Factoría de conveniencia a partir de notación algebraica (ej. 'e', 4 -> fila 4).
     */
    public static Square of(char fileChar, int rankNum) {
        int file = fileChar - 'a';
        int rank = rankNum - 1;
        return new Square(file, rank);
    }

    /**
     * Retorna la notación algebraica de la casilla (ej: "e4").
     */
    public String toAlgebraic() {
        return String.valueOf((char) ('a' + file)) + (rank + 1);
    }
}
