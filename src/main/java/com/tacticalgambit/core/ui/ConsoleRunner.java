package com.tacticalgambit.core.ui;

import com.tacticalgambit.core.action.GameAction;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.state.GameState;
import com.tacticalgambit.core.state.MatchInitializer;
import com.tacticalgambit.core.state.TurnState;
import java.util.Scanner;

/**
 * Punto de entrada principal (REPL) para probar de forma interactiva el motor de TacticalGambit.
 */
public class ConsoleRunner {

    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_GREEN = "\u001B[32m";
    private static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) {
        // Configurar codificación UTF-8 para consola en Windows
        try {
            System.setOut(new java.io.PrintStream(System.out, true, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println("Advertencia: No se pudo forzar codificación UTF-8 en la salida estándar.");
        }

        // 1. Inicializar partida con MatchInitializer
        TurnState state = MatchInitializer.initialize();

        Scanner scanner;
        try {
            scanner = new Scanner(System.in, "UTF-8");
        } catch (Exception e) {
            scanner = new Scanner(System.in);
        }

        System.out.println("========================================================");
        System.out.println(" Bienvenido a TACTICAL GAMBIT - REPL de Consola          ");
        System.out.println("========================================================");
        System.out.println(" Comandos disponibles:");
        System.out.println("   - MOVE <origen> <destino> [P] (ej. MOVE E2 E4 o MOVE A7 A8 Q)");
        System.out.println("   - DRAW                        (Roba una carta de la baraja)");
        System.out.println("   - PLAY <índice> [objetivo]    (ej. PLAY 0 D4 o PLAY 1)");
        System.out.println("   - PASS                        (Finaliza el turno actual)");
        System.out.println("   - EXIT                        (Cierra el programa)");
        System.out.println("========================================================");

        while (true) {
            BoardConsoleRenderer.render(state);

            // Verificar fin de partida
            if (state.gameState() == GameState.CHECKMATE) {
                System.out.println(ANSI_RED + "\n========================================================");
                System.out.println("       ¡JAQUE MATE! Partida finalizada. Ganador: " + state.activePlayer().opposite());
                System.out.println("========================================================\n" + ANSI_RESET);
                break;
            } else if (state.gameState() == GameState.STALEMATE) {
                System.out.println(ANSI_RED + "\n========================================================");
                System.out.println("       ¡TABLAS POR AHOGADO (STALEMATE)! Partida finalizada.");
                System.out.println("========================================================\n" + ANSI_RESET);
                break;
            }

            System.out.print("Comando > ");
            String line = scanner.nextLine();

            if (line == null || line.trim().equalsIgnoreCase("EXIT") || line.trim().equalsIgnoreCase("QUIT")) {
                System.out.println("Saliendo de Tactical Gambit. ¡Hasta la próxima!");
                break;
            }

            if (line.trim().equalsIgnoreCase("PASS") || line.trim().equalsIgnoreCase("END")) {
                state.startNextTurn();
                System.out.println(ANSI_GREEN + "--> Turno finalizado. Nuevo jugador activo." + ANSI_RESET);
                continue;
            }

            try {
                GameAction action = CommandParser.parse(line, state);
                if (action != null) {
                    boolean success = action.execute(state);
                    if (success) {
                        System.out.println(ANSI_GREEN + "Acción ejecutada con éxito." + ANSI_RESET);
                    } else {
                        System.out.println(ANSI_RED + "Error: La acción no es legal o válida en el estado actual." + ANSI_RESET);
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                System.out.println(ANSI_RED + "Error de regla: " + e.getMessage() + ANSI_RESET);
            } catch (Exception e) {
                System.out.println(ANSI_RED + "Error inesperado: " + e.getClass().getSimpleName() + " - " + e.getMessage() + ANSI_RESET);
            }
        }

        scanner.close();
    }
}
