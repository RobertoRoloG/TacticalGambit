package com.tacticalgambit.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Record Square (Inmutabilidad y Límites)")
class SquareTest {

    @Test
    @DisplayName("Debe crear una coordenada válida dentro del rango [0-7]")
    void shouldCreateValidSquare() {
        Square square = new Square(4, 3);
        assertEquals(4, square.file());
        assertEquals(3, square.rank());
    }

    @Test
    @DisplayName("Debe garantizar inmutabilidad e igualdad estructural por ser Java Record")
    void shouldGuaranteeEqualityAndImmutability() {
        Square square1 = new Square(2, 5);
        Square square2 = new Square(2, 5);
        Square square3 = new Square(3, 5);

        assertEquals(square1, square2);
        assertEquals(square1.hashCode(), square2.hashCode());
        assertNotEquals(square1, square3);
    }

    @ParameterizedTest
    @CsvSource({
        "-1, 0",
        "8, 0",
        "0, -1",
        "0, 8",
        "-5, 10"
    })
    @DisplayName("Debe rechazar coordenadas fuera de los límites del tablero (0..7)")
    void shouldThrowExceptionForOutOfBoundsCoordinates(int file, int rank) {
        assertThrows(IllegalArgumentException.class, () -> new Square(file, rank));
    }

    @Test
    @DisplayName("Debe convertir y parsear correctamente a notación algebraica")
    void shouldHandleAlgebraicNotation() {
        Square e4 = Square.of('e', 4);
        assertEquals(4, e4.file());
        assertEquals(3, e4.rank());
        assertEquals("e4", e4.toAlgebraic());
    }
}
