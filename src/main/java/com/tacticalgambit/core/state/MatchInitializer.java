package com.tacticalgambit.core.state;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.impl.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inicializador de partidas. Configura la disposición estándar de las piezas,
 * genera las barajas con el catálogo de cartas concretas y reparte la mano inicial.
 */
public class MatchInitializer {

    public static TurnState initialize() {
        Board board = Board.standardInitialSetup();

        List<Card> whiteDeckList = createInitialDeckList("W");
        List<Card> blackDeckList = createInitialDeckList("B");

        PlayerHand whiteHand = new PlayerHand();
        whiteHand.addCard(whiteDeckList.remove(0));
        whiteHand.addCard(whiteDeckList.remove(0));

        PlayerHand blackHand = new PlayerHand();
        blackHand.addCard(blackDeckList.remove(0));
        blackHand.addCard(blackDeckList.remove(0));

        Deck whiteDeck = new Deck(whiteDeckList);
        Deck blackDeck = new Deck(blackDeckList);

        return new TurnState(PieceColor.WHITE, board, whiteHand, blackHand, whiteDeck, blackDeck);
    }

    private static List<Card> createInitialDeckList(String prefix) {
        List<Card> deckList = new ArrayList<>();
        
        // 4 Paso Lateral (Side Step, 1 AP)
        for (int i = 1; i <= 4; i++) {
            deckList.add(new SideStepCard(prefix + "_sidestep_" + i, "Side Step", 1));
        }
        // 3 Ciclado (Cycle Hand, 1 AP)
        for (int i = 1; i <= 3; i++) {
            deckList.add(new CycleCard(prefix + "_cycle_" + i, "Cycle Hand", 1));
        }
        // 3 Impulso Táctico (Tactical Dash, 1 AP)
        for (int i = 1; i <= 3; i++) {
            deckList.add(new TacticalDashCard(prefix + "_dash_" + i, "Tactical Dash", 1));
        }
        // 3 Escudo Táctico (Shield, 2 AP)
        for (int i = 1; i <= 3; i++) {
            deckList.add(new ShieldCard(prefix + "_shield_" + i, "Shield", 2));
        }
        // 2 Salto Táctico (Tactical Jump, 2 AP)
        for (int i = 1; i <= 2; i++) {
            deckList.add(new TacticalJumpCard(prefix + "_jump_" + i, "Tactical Jump", 2));
        }
        // 2 Reagrupamiento (Regroup, 2 AP)
        for (int i = 1; i <= 2; i++) {
            deckList.add(new RegroupCard(prefix + "_regroup_" + i, "Regroup", 2));
        }
        // 2 Barricada de Terreno (Barricade, 3 AP)
        for (int i = 1; i <= 2; i++) {
            deckList.add(new BarricadeCard(prefix + "_barricade_" + i, "Barricade", 3));
        }
        // 1 Sobrecarga (Overcharge, 0 AP)
        deckList.add(new OverchargeCard(prefix + "_overcharge_1", "Overcharge", 0));

        Collections.shuffle(deckList);
        return deckList;
    }
}
