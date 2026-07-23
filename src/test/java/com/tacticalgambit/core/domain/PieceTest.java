package com.tacticalgambit.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Sealed Interface Piece y Pattern Matching")
class PieceTest {

    @Test
    @DisplayName("Debe instanciar correctamente cada tipo de pieza con su color y tipo")
    void shouldInstantiatePiecesCorrectly() {
        Piece whitePawn = new Pawn(PieceColor.WHITE);
        Piece blackKnight = new Knight(PieceColor.BLACK);
        Piece whiteKing = new King(PieceColor.WHITE);

        assertEquals(PieceColor.WHITE, whitePawn.color());
        assertEquals(PieceType.PAWN, whitePawn.type());

        assertEquals(PieceColor.BLACK, blackKnight.color());
        assertEquals(PieceType.KNIGHT, blackKnight.type());

        assertEquals(PieceColor.WHITE, whiteKing.color());
        assertEquals(PieceType.KING, whiteKing.type());
    }

    @Test
    @DisplayName("Debe permitir evaluar tipos de pieza usando Pattern Matching para switch (Java 21)")
    void shouldSupportPatternMatchingSwitch() {
        Piece piece = new Queen(PieceColor.WHITE);

        String description = switch (piece) {
            case Pawn p -> "Peon " + p.color();
            case Knight k -> "Caballo " + k.color();
            case Bishop b -> "Alfil " + b.color();
            case Rook r -> "Torre " + r.color();
            case Queen q -> "Dama " + q.color();
            case King k -> "Rey " + k.color();
            case PieceDecorator dec -> "Decorada";
        };

        assertEquals("Dama WHITE", description);
    }
}
