package com.tacticalgambit.core.ui;

import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.state.TurnState;
import java.util.Optional;

/**
 * Renderizador de consola (TUI) utilizando códigos de escape ANSI y glifos Unicode.
 */
public class BoardConsoleRenderer {

    // Códigos de escape ANSI para colores
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BLACK_TEXT = "\u001B[30m";
    private static final String ANSI_WHITE_TEXT = "\u001B[37m";
    private static final String ANSI_RED_TEXT = "\u001B[31m";
    private static final String ANSI_BOLD = "\u001B[1m";

    // Colores de texto para las piezas (Blancas en Cyan Brillante, Negras en Rojo Brillante para alto contraste)
    private static final String COLOR_WHITE_PIECE = "\u001B[96;1m"; // Cyan brillante + Negrita
    private static final String COLOR_BLACK_PIECE = "\u001B[91;1m"; // Rojo brillante + Negrita

    // Fondos de casillas (Uso de grises estándar de 16 colores ANSI para compatibilidad universal)
    private static final String BG_LIGHT = "\u001B[47m";   // Gris claro
    private static final String BG_DARK = "\u001B[100m";  // Gris oscuro
    private static final String BG_CHECK = "\u001B[41m";   // Fondo rojo (Jaque)

    public static void render(TurnState state) {
        if (state == null) {
            System.out.println("Error: Estado de turno nulo.");
            return;
        }

        boolean isKingInCheck = GameConditionChecker.isInCheck(state.board(), state.activePlayer());

        // 1. Panel de Estado Superior
        System.out.println("\n========================================================");
        System.out.println("                  TACTICAL GAMBIT - CORE                ");
        System.out.println("========================================================");
        System.out.print(" JUGADOR ACTIVO : ");
        if (state.activePlayer() == PieceColor.WHITE) {
            System.out.print(ANSI_BOLD + "BLANCO" + ANSI_RESET);
        } else {
            System.out.print(ANSI_BOLD + "NEGRO" + ANSI_RESET);
        }
        System.out.print("   |   AP DISPONIBLES: " + ANSI_BOLD + state.actionPoints().current() + "/5" + ANSI_RESET);
        System.out.println("   |   MOV. REALIZADO: " + (state.hasMovedPieceThisTurn() ? "SÍ" : "NO"));
        
        if (isKingInCheck) {
            System.out.println(" ESTADO         : " + ANSI_RED_TEXT + ANSI_BOLD + "¡REY EN JAQUE!" + ANSI_RESET);
        } else {
            System.out.println(" ESTADO         : NORMAL");
        }
        System.out.println("--------------------------------------------------------");

        // 2. Renderizado del Tablero 8x8 (Números solo a la izquierda, letras solo abajo)
        for (int rank = 7; rank >= 0; rank--) {
            System.out.print(" " + (rank + 1) + " "); // Número de fila (1-8)
            for (int file = 0; file < 8; file++) {
                Square square = new Square(file, rank);
                Optional<Piece> pieceOpt = state.board().getPieceAt(square);

                // Determinar color de fondo alternado
                String bg = ((file + rank) % 2 == 0) ? BG_DARK : BG_LIGHT;

                // Si es un Rey del color activo y está en jaque, pintar de rojo
                if (pieceOpt.isPresent()) {
                    Piece piece = pieceOpt.get();
                    if (piece.type() == PieceType.KING && piece.color() == state.activePlayer() && isKingInCheck) {
                        bg = BG_CHECK;
                    }
                }

                System.out.print(bg);
                if (pieceOpt.isPresent()) {
                    Piece p = pieceOpt.get();
                    String fg = (p.color() == PieceColor.WHITE) ? COLOR_WHITE_PIECE : COLOR_BLACK_PIECE;
                    System.out.print(fg + " " + getPieceGlyph(p) + " ");
                } else {
                    System.out.print("   ");
                }
                System.out.print(ANSI_RESET);
            }
            System.out.println(); // Salto de línea sin número a la derecha
        }
        System.out.println("    A  B  C  D  E  F  G  H ");
        System.out.println("--------------------------------------------------------");

        // 3. Panel Inferior: Cartas en Mano y Mazo
        System.out.println(" MAZO : " + state.deck().remainingCards() + " cartas restantes.");
        System.out.println(" MANO : " + (state.playerHand().isFull() ? "[LLENA]" : ""));
        if (state.playerHand().size() == 0) {
            System.out.println("   (Mano vacía)");
        } else {
            for (int i = 0; i < state.playerHand().size(); i++) {
                Card card = state.playerHand().cards().get(i);
                System.out.println("   [" + i + "] " + card.name() + " (Costo: " + card.apCost() + " AP)");
            }
        }
        System.out.println("--------------------------------------------------------");
        System.out.println(" COMANDOS DISPONIBLES:");
        System.out.println("   - Mover pieza   : MOVE <origen> <destino> (ej: MOVE E2 E4)");
        System.out.println("   - Jugar carta   : PLAY <index> [s1] [s2] [Q|R|B|N]");
        System.out.println("                     Global: PLAY 0");
        System.out.println("                     1 target: PLAY 0 E2 (ej: Side Step, Shield)");
        System.out.println("                     2 targets: PLAY 0 E2 G2");
        System.out.println("   - Descartar     : DISCARD <index> (ej: DISCARD 0)");
        System.out.println("   - Robar carta   : DRAW (Costo: 1 AP)");
        System.out.println("   - Terminar turno: PASS");
        System.out.println("   - Salir         : EXIT");
        System.out.println("========================================================\n");
    }

    private static String getPieceGlyph(Piece piece) {
        Piece base = PieceDecorator.basePiece(piece);
        if (base.color() == PieceColor.WHITE) {
            return switch (base) {
                case King k -> "♔";
                case Queen q -> "♕";
                case Rook r -> "♖";
                case Bishop b -> "♗";
                case Knight k -> "♘";
                case Pawn p -> "♙";
                case PieceDecorator d -> "?";
            };
        } else {
            return switch (base) {
                case King k -> "♚";
                case Queen q -> "♛";
                case Rook r -> "♜";
                case Bishop b -> "♝";
                case Knight k -> "♞";
                case Pawn p -> "♟";
                case PieceDecorator d -> "?";
            };
        }
    }
}
