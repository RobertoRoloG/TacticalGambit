package com.tacticalgambit.core.domain;

import com.tacticalgambit.core.command.BasicMoveCommand;
import com.tacticalgambit.core.command.MoveCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Dominio Encapsulado de Board")
class BoardTest {

    @Test
    @DisplayName("Debe crear un tablero vacío sin piezas")
    void shouldCreateEmptyBoard() {
        Board board = Board.empty();
        assertTrue(board.pieces().isEmpty());
    }

    @Test
    @DisplayName("Debe inicializar la disposición estándar del ajedrez (32 piezas)")
    void shouldInitializeStandardSetup() {
        Board board = Board.standardInitialSetup();
        Map<Square, Piece> pieces = board.pieces();

        assertEquals(32, pieces.size());

        // Verificar Rey Blanco en e1 (columna 4, fila 0)
        Square e1 = Square.of('e', 1);
        Optional<Piece> e1Piece = board.getPieceAt(e1);
        assertTrue(e1Piece.isPresent());
        assertEquals(PieceType.KING, e1Piece.get().type());
        assertEquals(PieceColor.WHITE, e1Piece.get().color());

        // Verificar Peón Negro en e7 (columna 4, fila 6)
        Square e7 = Square.of('e', 7);
        Optional<Piece> e7Piece = board.getPieceAt(e7);
        assertTrue(e7Piece.isPresent());
        assertEquals(PieceType.PAWN, e7Piece.get().type());
        assertEquals(PieceColor.BLACK, e7Piece.get().color());
    }

    @Test
    @DisplayName("Debe proteger el mapa de piezas de mutaciones externas no autorizadas")
    void shouldPreventExternalModificationOfPiecesMap() {
        Board board = Board.standardInitialSetup();
        Map<Square, Piece> pieces = board.pieces();

        Square e4 = Square.of('e', 4);
        assertThrows(UnsupportedOperationException.class, () -> pieces.put(e4, new Pawn(PieceColor.WHITE)));
    }

    @Test
    @DisplayName("Debe permitir modificar el estado exclusivamente mediante comandos de negocio")
    void shouldExecuteMoveViaCommandPattern() {
        Board board = Board.standardInitialSetup();
        Square e2 = Square.of('e', 2);
        Square e4 = Square.of('e', 4);

        MoveCommand moveE2E4 = new BasicMoveCommand(e2, e4);

        assertTrue(board.isOccupied(e2));
        assertTrue(board.isEmpty(e4));

        boolean success = board.executeMove(moveE2E4);

        assertTrue(success);
        assertTrue(board.isEmpty(e2));
        assertTrue(board.isOccupied(e4));
        assertEquals(PieceType.PAWN, board.getPieceAt(e4).orElseThrow().type());

        // Test de deshacer (Undo)
        moveE2E4.undo(board);
        assertTrue(board.isOccupied(e2));
        assertTrue(board.isEmpty(e4));
    }
}
