package com.tacticalgambit.core.domain;

import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.state.TurnState;
import java.util.Objects;

/**
 * Representa una carta de acción.
 */
public class Card {
    private final String id;
    private final String name;
    private final int apCost;

    public Card(String id, String name, int apCost) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("El ID de la carta no puede estar vacío.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre de la carta no puede estar vacío.");
        }
        if (apCost < 0 || apCost > 3) {
            throw new IllegalArgumentException("El costo de AP debe estar entre 0 y 3. Valor recibido: " + apCost);
        }
        this.id = id;
        this.name = name;
        this.apCost = apCost;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int apCost() {
        return apCost;
    }

    /**
     * Valida el efecto específico de esta carta sobre el objetivo.
     */
    public boolean canPlay(TurnState state, CardTarget target) {
        return true;
    }

    /**
     * Aplica el efecto específico de la carta sobre el TurnState.
     */
    public void apply(TurnState state, CardTarget target) {
        // No-op en carta genérica
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Card card)) return false;
        return Objects.equals(id, card.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
