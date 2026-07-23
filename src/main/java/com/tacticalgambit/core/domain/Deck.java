package com.tacticalgambit.core.domain;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;

/**
 * Representa el mazo de cartas de un jugador.
 */
public class Deck {

    private final Deque<Card> cards;

    private final java.util.List<Card> discardPile = new java.util.ArrayList<>();

    public Deck(Collection<Card> initialCards) {
        if (initialCards == null) {
            throw new IllegalArgumentException("La colección de cartas no puede ser nula.");
        }
        this.cards = new ArrayDeque<>(initialCards);
    }

    public static Deck of(Card... cards) {
        return new Deck(java.util.List.of(cards));
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }

    public int remainingCards() {
        return cards.size();
    }

    /**
     * Roba la carta superior del mazo.
     */
    public Optional<Card> drawCard() {
        return Optional.ofNullable(cards.poll());
    }

    public java.util.List<Card> getDiscardPile() {
        return discardPile;
    }

    public void addToDiscardPile(Card card) {
        discardPile.add(card);
    }

    /**
     * Retorna una copia profunda inmutable del mazo para realizar simulaciones de juego aisladas.
     */
    public Deck copy() {
        Deck copy = new Deck(new java.util.ArrayList<>(this.cards));
        copy.discardPile.addAll(this.discardPile);
        return copy;
    }
}
