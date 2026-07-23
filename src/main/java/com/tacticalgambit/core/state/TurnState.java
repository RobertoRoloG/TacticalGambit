package com.tacticalgambit.core.state;

import com.tacticalgambit.core.domain.Board;
import com.tacticalgambit.core.domain.Deck;
import com.tacticalgambit.core.domain.PieceColor;
import com.tacticalgambit.core.domain.PlayerHand;

/**
 * Agregado que representa el estado del turno actual en tiempo real.
 * Encapsula la reserva de AP, las restricciones de movimiento de pieza por turno y el cambio de turno.
 */
public class TurnState {

    private PieceColor activePlayer;
    private ActionPoints whiteActionPoints;
    private ActionPoints blackActionPoints;
    private boolean hasMovedPieceThisTurn;
    private GameState gameState;

    private int whiteApPenalty = 0;
    private int blackApPenalty = 0;
    private boolean whitePlayedOverchargeThisTurn = false;
    private boolean blackPlayedOverchargeThisTurn = false;

    private final PlayerHand whiteHand;
    private final PlayerHand blackHand;
    private final Deck whiteDeck;
    private final Deck blackDeck;
    private final Board board;

    public TurnState(PieceColor activePlayer, Board board, PlayerHand playerHand, Deck deck) {
        if (activePlayer == null || board == null || playerHand == null || deck == null) {
            throw new IllegalArgumentException("Todos los parámetros son requeridos para inicializar TurnState.");
        }
        this.activePlayer = activePlayer;
        this.board = board;
        this.whiteHand = activePlayer == PieceColor.WHITE ? playerHand : new PlayerHand();
        this.blackHand = activePlayer == PieceColor.BLACK ? playerHand : new PlayerHand();
        this.whiteDeck = activePlayer == PieceColor.WHITE ? deck : new Deck(java.util.List.of());
        this.blackDeck = activePlayer == PieceColor.BLACK ? deck : new Deck(java.util.List.of());
        this.whiteActionPoints = activePlayer == PieceColor.WHITE ? ActionPoints.initial() : new ActionPoints(0);
        this.blackActionPoints = activePlayer == PieceColor.BLACK ? ActionPoints.initial() : new ActionPoints(0);
        this.hasMovedPieceThisTurn = false;
        this.gameState = GameState.IN_PROGRESS;
        this.whiteApPenalty = 0;
        this.blackApPenalty = 0;
        this.whitePlayedOverchargeThisTurn = false;
        this.blackPlayedOverchargeThisTurn = false;
    }

    public TurnState(PieceColor activePlayer, Board board, PlayerHand whiteHand, PlayerHand blackHand, Deck whiteDeck, Deck blackDeck) {
        if (activePlayer == null || board == null || whiteHand == null || blackHand == null || whiteDeck == null || blackDeck == null) {
            throw new IllegalArgumentException("Todos los parámetros son requeridos para inicializar TurnState.");
        }
        this.activePlayer = activePlayer;
        this.board = board;
        this.whiteHand = whiteHand;
        this.blackHand = blackHand;
        this.whiteDeck = whiteDeck;
        this.blackDeck = blackDeck;
        this.whiteActionPoints = activePlayer == PieceColor.WHITE ? ActionPoints.initial() : new ActionPoints(0);
        this.blackActionPoints = activePlayer == PieceColor.BLACK ? ActionPoints.initial() : new ActionPoints(0);
        this.hasMovedPieceThisTurn = false;
        this.gameState = GameState.IN_PROGRESS;
    }

    public PieceColor activePlayer() {
        return activePlayer;
    }

    public ActionPoints actionPoints() {
        return activePlayer == PieceColor.WHITE ? whiteActionPoints : blackActionPoints;
    }

    public boolean hasMovedPieceThisTurn() {
        return hasMovedPieceThisTurn;
    }

    public GameState gameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public PlayerHand playerHand() {
        return activePlayer == PieceColor.WHITE ? whiteHand : blackHand;
    }

    public PlayerHand whiteHand() {
        return whiteHand;
    }

    public PlayerHand blackHand() {
        return blackHand;
    }

    public Deck deck() {
        return activePlayer == PieceColor.WHITE ? whiteDeck : blackDeck;
    }

    public Deck whiteDeck() {
        return whiteDeck;
    }

    public Deck blackDeck() {
        return blackDeck;
    }

    public Board board() {
        return board;
    }

    /**
     * Consume AP de forma atómica.
     */
    public void consumeAP(int cost) {
        if (activePlayer == PieceColor.WHITE) {
            this.whiteActionPoints = this.whiteActionPoints.consume(cost);
        } else {
            this.blackActionPoints = this.blackActionPoints.consume(cost);
        }
    }

    /**
     * Registra que se ha realizado el movimiento de pieza permitido en el turno.
     */
    public void markPieceMoved() {
        this.hasMovedPieceThisTurn = true;
    }

    /**
     * Transiciona al siguiente turno, recargando AP (+3 AP hasta 5 con penalización) y reseteando el flag de movimiento.
     */
    public void startNextTurn() {
        clearOverchargePlayed();

        this.activePlayer = this.activePlayer.opposite();
        
        int penalty = activeApPenalty();
        if (activePlayer == PieceColor.WHITE) {
            this.whiteActionPoints = this.whiteActionPoints.addTurnRefillWithPenalty(penalty);
        } else {
            this.blackActionPoints = this.blackActionPoints.addTurnRefillWithPenalty(penalty);
        }
        setActiveApPenalty(0);

        this.hasMovedPieceThisTurn = false;

        // Limpiar escudos del jugador que empieza el turno
        java.util.List<com.tacticalgambit.core.domain.Square> squaresToClean = new java.util.ArrayList<>();
        for (java.util.Map.Entry<com.tacticalgambit.core.domain.Square, com.tacticalgambit.core.domain.Piece> entry : board.pieces().entrySet()) {
            if (entry.getValue().color() == this.activePlayer && com.tacticalgambit.core.domain.PieceDecorator.isShielded(entry.getValue())) {
                squaresToClean.add(entry.getKey());
            }
        }
        for (com.tacticalgambit.core.domain.Square sq : squaresToClean) {
            com.tacticalgambit.core.domain.Piece p = board.getPieceAt(sq).orElseThrow();
            com.tacticalgambit.core.domain.Piece unwrapped = com.tacticalgambit.core.domain.PieceDecorator.cleanShield(p);
            board.placePieceInternal(sq, unwrapped);
        }

        // Decrementar contadores de barricadas activas en el tablero
        board.decrementBarricades();

        // Evaluar condiciones de fin de partida (Jaque Mate / Tablas)
        GameStateManager.checkAndUpdateGameState(this);
    }

    public int activeApPenalty() {
        return activePlayer == PieceColor.WHITE ? whiteApPenalty : blackApPenalty;
    }

    public void setActiveApPenalty(int penalty) {
        if (activePlayer == PieceColor.WHITE) {
            whiteApPenalty = penalty;
        } else {
            blackApPenalty = penalty;
        }
    }

    public boolean hasPlayedOverchargeThisTurn() {
        return activePlayer == PieceColor.WHITE ? whitePlayedOverchargeThisTurn : blackPlayedOverchargeThisTurn;
    }

    public void markOverchargePlayed() {
        if (activePlayer == PieceColor.WHITE) {
            whitePlayedOverchargeThisTurn = true;
        } else {
            blackPlayedOverchargeThisTurn = true;
        }
    }

    public void clearOverchargePlayed() {
        if (activePlayer == PieceColor.WHITE) {
            whitePlayedOverchargeThisTurn = false;
        } else {
            blackPlayedOverchargeThisTurn = false;
        }
    }

    public void addAP(int amount) {
        if (activePlayer == PieceColor.WHITE) {
            this.whiteActionPoints = this.whiteActionPoints.add(amount);
        } else {
            this.blackActionPoints = this.blackActionPoints.add(amount);
        }
    }
}
