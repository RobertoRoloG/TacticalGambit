package com.tacticalgambit.core.ui;

import com.tacticalgambit.core.action.CardPlayAction;
import com.tacticalgambit.core.action.DrawCardAction;
import com.tacticalgambit.core.action.GameAction;
import com.tacticalgambit.core.action.PieceMoveAction;
import com.tacticalgambit.core.action.DiscardCardAction;
import com.tacticalgambit.core.domain.Board;
import com.tacticalgambit.core.domain.Card;
import com.tacticalgambit.core.domain.Square;
import com.tacticalgambit.core.domain.card.CardTarget;
import com.tacticalgambit.core.domain.card.GlobalTarget;
import com.tacticalgambit.core.domain.card.PieceTarget;
import com.tacticalgambit.core.domain.card.SquareTarget;
import com.tacticalgambit.core.domain.card.DoublePieceTarget;
import com.tacticalgambit.core.state.TurnState;

/**
 * Analizador sintáctico de comandos ingresados por terminal.
 */
public class CommandParser {

    /**
     * Parsea una línea de comando y la mapea a una GameAction.
     * 
     * @param input Línea de entrada por teclado.
     * @param state Estado actual del turno.
     * @return Instancia de GameAction si es un comando de juego, o null si es PASS/EXIT.
     * @throws IllegalArgumentException Si el comando tiene formato incorrecto.
     */
    public static GameAction parse(String input, TurnState state) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("Comando vacío.");
        }

        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0].toUpperCase();

        switch (command) {
            case "MOVE" -> {
                if (tokens.length < 3) {
                    throw new IllegalArgumentException("Uso: MOVE <origen> <destino> [promoción: Q|R|B|N] (ej. MOVE E2 E4)");
                }
                Square from = parseAlgebraic(tokens[1]);
                Square to = parseAlgebraic(tokens[2]);
                com.tacticalgambit.core.domain.PieceType promo = com.tacticalgambit.core.domain.PieceType.QUEEN;
                if (tokens.length >= 4) {
                    promo = switch (tokens[3].toUpperCase()) {
                        case "Q" -> com.tacticalgambit.core.domain.PieceType.QUEEN;
                        case "R" -> com.tacticalgambit.core.domain.PieceType.ROOK;
                        case "B" -> com.tacticalgambit.core.domain.PieceType.BISHOP;
                        case "N" -> com.tacticalgambit.core.domain.PieceType.KNIGHT;
                        default -> throw new IllegalArgumentException("Pieza de coronación inválida. Use Q (Dama), R (Torre), B (Alfil) o N (Caballo).");
                    };
                }
                return new PieceMoveAction(from, to, promo);
            }
            case "DRAW" -> {
                return new DrawCardAction();
            }
            case "DISCARD" -> {
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Uso: DISCARD <índice> (ej. DISCARD 0)");
                }
                int index;
                try {
                    index = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Índice de carta inválido: " + tokens[1]);
                }
                if (index < 0 || index >= state.playerHand().size()) {
                    throw new IllegalArgumentException("Índice fuera de límites: " + index);
                }
                Card card = state.playerHand().cards().get(index);
                return new DiscardCardAction(card);
            }
            case "PLAY" -> {
                if (tokens.length < 2) {
                    throw new IllegalArgumentException("Uso: PLAY <índice_carta> [casilla_objetivo]");
                }
                int cardIndex;
                try {
                    cardIndex = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("El índice de la carta debe ser un número entero.");
                }

                if (cardIndex < 0 || cardIndex >= state.playerHand().size()) {
                    throw new IllegalArgumentException("Índice de carta fuera de rango [0.." + (state.playerHand().size() - 1) + "].");
                }

                Card card = state.playerHand().cards().get(cardIndex);

                // Si se proveen dos casillas de destino, es DoublePieceTarget
                if (tokens.length >= 4) {
                    Square s1 = parseAlgebraic(tokens[2]);
                    Square s2 = parseAlgebraic(tokens[3]);
                    java.util.Optional<com.tacticalgambit.core.domain.PieceType> promo = java.util.Optional.empty();
                    if (tokens.length >= 5) {
                        promo = java.util.Optional.of(switch (tokens[4].toUpperCase()) {
                            case "Q" -> com.tacticalgambit.core.domain.PieceType.QUEEN;
                            case "R" -> com.tacticalgambit.core.domain.PieceType.ROOK;
                            case "B" -> com.tacticalgambit.core.domain.PieceType.BISHOP;
                            case "N" -> com.tacticalgambit.core.domain.PieceType.KNIGHT;
                            default -> throw new IllegalArgumentException("Pieza de coronación inválida. Use Q (Dama), R (Torre), B (Alfil) o N (Caballo).");
                        });
                    }
                    return new CardPlayAction(card, new DoublePieceTarget(s1, s2, promo));
                }

                // Si no se provee casilla de destino, es objetivo Global
                if (tokens.length < 3) {
                    return new CardPlayAction(card, new GlobalTarget());
                }

                // Si se provee una casilla de destino, evaluar si es PieceTarget o SquareTarget
                Square targetSquare = parseAlgebraic(tokens[2]);
                CardTarget target;
                if (state.board().isOccupied(targetSquare)) {
                    target = new PieceTarget(targetSquare);
                } else {
                    target = new SquareTarget(targetSquare);
                }

                return new CardPlayAction(card, target);
            }
            case "PASS", "END", "EXIT", "QUIT" -> {
                return null; // El runner de consola intercepta estos comandos directamente
            }
            default -> throw new IllegalArgumentException("Comando desconocido: " + command);
        }
    }

    public static Square parseAlgebraic(String value) {
        if (value == null || value.length() != 2) {
            throw new IllegalArgumentException("Coordenada inválida (debe ser de 2 caracteres, ej. E4): " + value);
        }
        char fileChar = Character.toLowerCase(value.charAt(0));
        char rankChar = value.charAt(1);

        if (fileChar < 'a' || fileChar > 'h') {
            throw new IllegalArgumentException("Columna inválida (debe ser entre A y H): " + fileChar);
        }
        int rankNum = rankChar - '0';
        if (rankNum < 1 || rankNum > 8) {
            throw new IllegalArgumentException("Fila inválida (debe ser entre 1 y 8): " + rankChar);
        }

        return Square.of(fileChar, rankNum);
    }
}
