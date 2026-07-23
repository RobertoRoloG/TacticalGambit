package com.tacticalgambit.core.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsula la mano de cartas de un jugador con un límite estricto de 4 cartas.
 */
public class PlayerHand {

    public static final int MAX_HAND_SIZE = 4;

    private final List<Card> cards;

    public PlayerHand() {
        this.cards = new ArrayList<>();
    }

    public PlayerHand(List<Card> initialCards) {
        if (initialCards == null) {
            throw new IllegalArgumentException("La lista inicial de cartas no puede ser nula.");
        }
        if (initialCards.size() > MAX_HAND_SIZE) {
            throw new IllegalArgumentException("La mano inicial excede el límite de " + MAX_HAND_SIZE + " cartas.");
        }
        this.cards = new ArrayList<>(initialCards);
    }

    public boolean isFull() {
        return cards.size() >= MAX_HAND_SIZE;
    }

    public int size() {
        return cards.size();
    }

    public boolean contains(Card card) {
        return cards.contains(card);
    }

    /**
     * Agrega una carta a la mano garantizando la invariante de no exceder 4 cartas.
     */
    public void addCard(Card card) {
        if (card == null) {
            throw new IllegalArgumentException("La carta no puede ser nula.");
        }
        if (isFull()) {
            throw new IllegalStateException("No se puede añadir la carta. La mano ya contiene el límite máximo de " + MAX_HAND_SIZE + " cartas.");
        }
        cards.add(card);
    }

    /**
     * Remueve una carta de la mano tras jugarla.
     */
    public boolean removeCard(Card card) {
        return cards.remove(card);
    }

    /**
     * Retorna una vista inmutable de las cartas en mano.
     */
    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }
}
