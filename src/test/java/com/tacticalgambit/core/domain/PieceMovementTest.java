package com.tacticalgambit.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Reglas de Movimiento Geométrico por Pieza")
class PieceMovementTest {

    @Nested
    @DisplayName("Reglas de Peón (Pawn)")
    class PawnTest {

        @Test
        @DisplayName("Debe permitir avance simple de 1 casilla hacia adelante a casilla vacía")
        void shouldAllowSingleStepForward() {
            Board board = Board.empty();
            Square e2 = Square.of('e', 2);
            Square e3 = Square.of('e', 3);
            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            board.placePieceInternal(e2, whitePawn);

            assertTrue(whitePawn.canMove(board, e2, e3));
        }

        @Test
        @DisplayName("Debe permitir avance doble de 2 casillas desde la fila inicial si el camino está libre")
        void shouldAllowDoubleStepFromInitialRank() {
            Board board = Board.empty();
            Square e2 = Square.of('e', 2);
            Square e4 = Square.of('e', 4);
            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            board.placePieceInternal(e2, whitePawn);

            assertTrue(whitePawn.canMove(board, e2, e4));
        }

        @Test
        @DisplayName("Debe rechazar avance doble si el camino está bloqueado por una pieza")
        void shouldRejectDoubleStepIfBlocked() {
            Board board = Board.empty();
            Square e2 = Square.of('e', 2);
            Square e3 = Square.of('e', 3);
            Square e4 = Square.of('e', 4);

            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            Pawn blockingPawn = new Pawn(PieceColor.BLACK);

            board.placePieceInternal(e2, whitePawn);
            board.placePieceInternal(e3, blockingPawn);

            assertFalse(whitePawn.canMove(board, e2, e4));
        }

        @Test
        @DisplayName("Debe rechazar avance frontal si la casilla destino está ocupada (El peón NO captura de frente)")
        void shouldRejectForwardMoveIfOccupied() {
            Board board = Board.empty();
            Square e2 = Square.of('e', 2);
            Square e3 = Square.of('e', 3);

            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            Pawn enemyPawn = new Pawn(PieceColor.BLACK);

            board.placePieceInternal(e2, whitePawn);
            board.placePieceInternal(e3, enemyPawn);

            assertFalse(whitePawn.canMove(board, e2, e3));
        }

        @Test
        @DisplayName("Debe permitir captura únicamente en diagonal de 1 casilla si hay una pieza enemiga")
        void shouldAllowDiagonalCapture() {
            Board board = Board.empty();
            Square e2 = Square.of('e', 2);
            Square d3 = Square.of('d', 3);
            Square f3 = Square.of('f', 3);

            Pawn whitePawn = new Pawn(PieceColor.WHITE);
            Pawn enemyPawn = new Pawn(PieceColor.BLACK);

            board.placePieceInternal(e2, whitePawn);
            board.placePieceInternal(d3, enemyPawn);

            assertTrue(whitePawn.canMove(board, e2, d3)); // Captura diagonal
            assertFalse(whitePawn.canMove(board, e2, f3)); // Diagonal vacía -> rechazo
        }
    }

    @Nested
    @DisplayName("Reglas de Caballo (Knight)")
    class KnightTest {

        @Test
        @DisplayName("Debe permitir movimiento en L saltando piezas intermedias")
        void shouldAllowLMovementJumpingOverPieces() {
            Board board = Board.standardInitialSetup(); // Con piezas alineadas en fila 1 y 2
            Square b1 = Square.of('b', 1);
            Square c3 = Square.of('c', 3);
            Square a3 = Square.of('a', 3);

            Knight knight = new Knight(PieceColor.WHITE);

            assertTrue(knight.canMove(board, b1, c3));
            assertTrue(knight.canMove(board, b1, a3));
        }

        @Test
        @DisplayName("Debe rechazar movimiento sobre casilla ocupada por pieza del mismo color (Fuego amigo)")
        void shouldRejectFriendlyFire() {
            Board board = Board.standardInitialSetup();
            Square b1 = Square.of('b', 1);
            Square d2 = Square.of('d', 2); // Ocupada por peón blanco

            Knight knight = new Knight(PieceColor.WHITE);

            assertFalse(knight.canMove(board, b1, d2));
        }
    }

    @Nested
    @DisplayName("Reglas de Torre (Rook)")
    class RookTest {

        @Test
        @DisplayName("Debe permitir desplazamiento ortogonal libre y rechazar si hay bloqueo de trayectoria")
        void shouldValidateOrthogonalAndBlocking() {
            Board board = Board.empty();
            Square a1 = Square.of('a', 1);
            Square a5 = Square.of('a', 5);
            Square a3 = Square.of('a', 3);
            Square e1 = Square.of('e', 1);

            Rook rook = new Rook(PieceColor.WHITE);
            board.placePieceInternal(a1, rook);

            assertTrue(rook.canMove(board, a1, a5));
            assertTrue(rook.canMove(board, a1, e1));

            // Colocar obstáculo en a3
            board.placePieceInternal(a3, new Pawn(PieceColor.BLACK));
            assertFalse(rook.canMove(board, a1, a5)); // Bloqueado por a3
            assertTrue(rook.canMove(board, a1, a3));  // Captura enemiga en a3
        }
    }

    @Nested
    @DisplayName("Reglas de Alfil (Bishop)")
    class BishopTest {

        @Test
        @DisplayName("Debe permitir desplazamiento diagonal libre y rechazar no-diagonales o bloqueos")
        void shouldValidateDiagonalAndBlocking() {
            Board board = Board.empty();
            Square c1 = Square.of('c', 1);
            Square f4 = Square.of('f', 4);
            Square d2 = Square.of('d', 2);

            Bishop bishop = new Bishop(PieceColor.WHITE);
            board.placePieceInternal(c1, bishop);

            assertTrue(bishop.canMove(board, c1, f4));

            // Obstáculo en d2
            board.placePieceInternal(d2, new Pawn(PieceColor.WHITE));
            assertFalse(bishop.canMove(board, c1, f4)); // Bloqueado
        }
    }

    @Nested
    @DisplayName("Reglas de Dama (Queen)")
    class QueenTest {

        @Test
        @DisplayName("Debe permitir desplazamientos ortogonales y diagonales")
        void shouldAllowOrthogonalAndDiagonal() {
            Board board = Board.empty();
            Square d1 = Square.of('d', 1);
            Square d7 = Square.of('d', 7);
            Square h5 = Square.of('h', 5);

            Queen queen = new Queen(PieceColor.WHITE);
            board.placePieceInternal(d1, queen);

            assertTrue(queen.canMove(board, d1, d7)); // Ortogonal
            assertTrue(queen.canMove(board, d1, h5)); // Diagonal
        }
    }

    @Nested
    @DisplayName("Reglas de Rey (King)")
    class KingTest {

        @Test
        @DisplayName("Debe permitir movimiento de 1 casilla en cualquier dirección y rechazar saltos lejanos")
        void shouldAllowSingleStepOnly() {
            Board board = Board.empty();
            Square e1 = Square.of('e', 1);
            Square e2 = Square.of('e', 2);
            Square f2 = Square.of('f', 2);
            Square e3 = Square.of('e', 3);

            King king = new King(PieceColor.WHITE);
            board.placePieceInternal(e1, king);

            assertTrue(king.canMove(board, e1, e2));
            assertTrue(king.canMove(board, e1, f2));
            assertFalse(king.canMove(board, e1, e3)); // Mas de 1 casilla
        }
    }
}
