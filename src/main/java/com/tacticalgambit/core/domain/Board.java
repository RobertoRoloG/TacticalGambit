package com.tacticalgambit.core.domain;

import com.tacticalgambit.core.command.MoveCommand;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Representa el tablero de ajedrez con encapsulamiento estricto.
 * Protege las invariantes del estado evitando la mutación directa arbitraria por medio de setters.
 */
public class Board {

    private final Map<Square, Piece> grid;
    private final Map<Square, Integer> barricades;

    public Board(Map<Square, Piece> grid) {
        this(grid, new HashMap<>());
    }

    public Board(Map<Square, Piece> grid, Map<Square, Integer> barricades) {
        if (grid == null || barricades == null) {
            throw new IllegalArgumentException("Grid y Barricades no pueden ser nulos.");
        }
        this.grid = new HashMap<>(grid);
        this.barricades = new HashMap<>(barricades);
    }

    /**
     * Crea un tablero totalmente vacío.
     */
    public static Board empty() {
        return new Board(Map.of());
    }

    /**
     * Instancia un tablero con la disposición inicial estándar del ajedrez.
     */
    public static Board standardInitialSetup() {
        Map<Square, Piece> initialGrid = new HashMap<>();

        // Piezas blancas
        initialGrid.put(new Square(0, 0), new Rook(PieceColor.WHITE));
        initialGrid.put(new Square(1, 0), new Knight(PieceColor.WHITE));
        initialGrid.put(new Square(2, 0), new Bishop(PieceColor.WHITE));
        initialGrid.put(new Square(3, 0), new Queen(PieceColor.WHITE));
        initialGrid.put(new Square(4, 0), new King(PieceColor.WHITE));
        initialGrid.put(new Square(5, 0), new Bishop(PieceColor.WHITE));
        initialGrid.put(new Square(6, 0), new Knight(PieceColor.WHITE));
        initialGrid.put(new Square(7, 0), new Rook(PieceColor.WHITE));

        for (int file = 0; file < 8; file++) {
            initialGrid.put(new Square(file, 1), new Pawn(PieceColor.WHITE));
        }

        // Piezas negras
        initialGrid.put(new Square(0, 7), new Rook(PieceColor.BLACK));
        initialGrid.put(new Square(1, 7), new Knight(PieceColor.BLACK));
        initialGrid.put(new Square(2, 7), new Bishop(PieceColor.BLACK));
        initialGrid.put(new Square(3, 7), new Queen(PieceColor.BLACK));
        initialGrid.put(new Square(4, 7), new King(PieceColor.BLACK));
        initialGrid.put(new Square(5, 7), new Bishop(PieceColor.BLACK));
        initialGrid.put(new Square(6, 7), new Knight(PieceColor.BLACK));
        initialGrid.put(new Square(7, 7), new Rook(PieceColor.BLACK));

        for (int file = 0; file < 8; file++) {
            initialGrid.put(new Square(file, 6), new Pawn(PieceColor.BLACK));
        }

        return new Board(initialGrid);
    }

    /**
     * Obtiene la pieza en la casilla especificada, encapsulada en un Optional.
     */
    public Optional<Piece> getPieceAt(Square square) {
        return Optional.ofNullable(grid.get(square));
    }

    /**
     * Verifica si una casilla no contiene ninguna pieza.
     */
    public boolean isEmpty(Square square) {
        return !grid.containsKey(square);
    }

    /**
     * Verifica si una casilla contiene una pieza.
     */
    public boolean isOccupied(Square square) {
        return grid.containsKey(square);
    }

    /**
     * Retorna una vista inmutable del mapa actual de piezas.
     */
    public Map<Square, Piece> pieces() {
        return Collections.unmodifiableMap(grid);
    }

    /**
     * Método de negocio semántico para ejecutar un MoveCommand sobre el tablero.
     */
    public boolean executeMove(MoveCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de movimiento no puede ser nulo.");
        }
        return command.execute(this);
    }

    /**
     * Operación interna de colocación de pieza (utilizada por comandos autorizados dentro del dominio).
     */
    public void placePieceInternal(Square square, Piece piece) {
        if (square == null || piece == null) {
            throw new IllegalArgumentException("Casilla y pieza son requeridas.");
        }
        grid.put(square, piece);
    }

    /**
     * Operación interna de remoción de pieza (utilizada por comandos autorizados dentro del dominio).
     */
    public Optional<Piece> removePieceInternal(Square square) {
        if (square == null) {
            throw new IllegalArgumentException("Casilla es requerida.");
        }
        return Optional.ofNullable(grid.remove(square));
    }

    /**
     * Verifica que no existan piezas intermedias bloqueando la trayectoria entre 'from' y 'to'.
     * Aplica para desplazamientos puramente ortogonales o diagonales (excluye las casillas de inicio y fin).
     */
    public boolean isPathClear(Square from, Square to) {
        if (from == null || to == null || from.equals(to)) {
            return false;
        }

        int deltaFile = to.file() - from.file();
        int deltaRank = to.rank() - from.rank();

        boolean isOrthogonal = (deltaFile == 0 || deltaRank == 0);
        boolean isDiagonal = (Math.abs(deltaFile) == Math.abs(deltaRank));

        if (!isOrthogonal && !isDiagonal) {
            return false;
        }

        int stepFile = Integer.signum(deltaFile);
        int stepRank = Integer.signum(deltaRank);

        int currentFile = from.file() + stepFile;
        int currentRank = from.rank() + stepRank;

        while (currentFile != to.file() || currentRank != to.rank()) {
            Square stepSquare = new Square(currentFile, currentRank);
            if (isOccupied(stepSquare) || isBarricaded(stepSquare)) {
                return false;
            }
            currentFile += stepFile;
            currentRank += stepRank;
        }

        return true;
    }

    /**
     * Devuelve una copia ligera en memoria del tablero tras aplicar un movimiento.
     * Utilizado para simular movimientos y validar si previenen o exponen al Rey a Jaque.
     */
    public Board simulateMove(Square from, Square to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Casilla de origen y destino son requeridas.");
        }
        java.util.Map<Square, Piece> tempGrid = new java.util.HashMap<>(this.grid);
        Piece piece = tempGrid.remove(from);
        if (piece != null) {
            tempGrid.put(to, piece);
        }
        return new Board(tempGrid, this.barricades);
    }

    public boolean isBarricaded(Square square) {
        return barricades.containsKey(square) && barricades.get(square) > 0;
    }

    public Map<Square, Integer> barricades() {
        return Collections.unmodifiableMap(barricades);
    }

    public void addBarricade(Square square, int turns) {
        barricades.put(square, turns);
    }

    public void removeBarricade(Square square) {
        barricades.remove(square);
    }

    public void decrementBarricades() {
        java.util.Iterator<Map.Entry<Square, Integer>> it = barricades.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Square, Integer> entry = it.next();
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
            } else {
                entry.setValue(remaining);
            }
        }
    }
}
