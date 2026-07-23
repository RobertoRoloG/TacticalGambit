package com.tacticalgambit.core.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Value Object ActionPoints")
class ActionPointsTest {

    @Test
    @DisplayName("Debe inicializarse con 3 AP base")
    void shouldInitializeWithThreeAP() {
        ActionPoints ap = ActionPoints.initial();
        assertEquals(3, ap.current());
    }

    @Test
    @DisplayName("Debe acumular +3 AP en la recarga respetando el tope de 5 AP")
    void shouldAccumulateAPRespectingCap() {
        ActionPoints ap = ActionPoints.initial(); // 3 AP
        ActionPoints refilled = ap.addTurnRefill(); // 3 + 3 = 6 -> Clamped to 5 AP

        assertEquals(5, refilled.current());
    }

    @Test
    @DisplayName("Debe consumir AP correctamente y rechazar consumos mayores a los disponibles")
    void shouldConsumeAPCorrectlyAndRejectOverdraw() {
        ActionPoints ap = new ActionPoints(4);

        assertTrue(ap.canAfford(2));
        ActionPoints consumed = ap.consume(2);
        assertEquals(2, consumed.current());

        assertFalse(consumed.canAfford(3));
        assertThrows(IllegalStateException.class, () -> consumed.consume(3));
    }
}
