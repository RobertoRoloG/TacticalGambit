package com.tacticalgambit.web;

import com.tacticalgambit.core.action.*;
import com.tacticalgambit.core.domain.*;
import com.tacticalgambit.core.domain.card.*;
import com.tacticalgambit.core.state.MatchInitializer;
import com.tacticalgambit.core.state.TurnState;
import com.tacticalgambit.core.ui.CommandParser;
import com.tacticalgambit.web.dto.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class GameWebSocketServer {
    private static final int PORT = 7070;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static TurnState gameState = MatchInitializer.initialize();
    private static final List<String> actionLogs = new CopyOnWriteArrayList<>(Collections.singletonList("Partida inicializada."));

    public static void start() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)) {
                System.out.println("=== SERVIDOR TACTICAL GAMBIT INICIADO ===");
                System.out.println("Escuchando en puerto: " + PORT);
                System.out.println("Abra en su navegador: http://localhost:" + PORT);
                System.out.println("===========================================");
                while (true) {
                    Socket socket = server.accept();
                    new Thread(() -> handleConnection(socket)).start();
                }
            } catch (Exception e) {
                System.err.println("Error en ServerSocket: " + e.getMessage());
            }
        }).start();
    }

    private static void handleConnection(Socket socket) {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            OutputStream out = socket.getOutputStream();
            String firstLine = in.readLine();
            if (firstLine == null) {
                socket.close();
                return;
            }

            if (firstLine.contains("/ws/game")) {
                handleWebSocket(socket, firstLine, in, out);
            } else if (firstLine.startsWith("GET ")) {
                handleStaticFile(socket, firstLine, out);
            } else {
                socket.close();
            }
        } catch (Exception e) {
            try { socket.close(); } catch (Exception ex) {}
        }
    }

    private static void handleWebSocket(Socket socket, String firstLine, BufferedReader in, OutputStream out) throws Exception {
        String key = null;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("sec-websocket-key:")) {
                key = line.substring(18).trim();
            }
        }

        if (key == null) {
            socket.close();
            return;
        }

        // Handshake response
        String acceptKey = Base64.getEncoder().encodeToString(
            MessageDigest.getInstance("SHA-1").digest(
                (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")
            )
        );

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        pw.print("HTTP/1.1 101 Switching Protocols\r\n");
        pw.print("Upgrade: websocket\r\n");
        pw.print("Connection: Upgrade\r\n");
        pw.print("Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n");
        pw.flush();

        ClientHandler handler = new ClientHandler(socket);
        clients.add(handler);
        handler.sendState();
        handler.runLoop();
    }

    private static void handleStaticFile(Socket socket, String firstLine, OutputStream out) throws Exception {
        String[] tokens = firstLine.split("\\s+");
        if (tokens.length < 2) {
            socket.close();
            return;
        }
        String path = tokens[1];
        if (path.equals("/")) {
            path = "/index.html";
        }

        InputStream is = GameWebSocketServer.class.getResourceAsStream("/public" + path);
        if (is == null) {
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
            pw.print("HTTP/1.1 404 Not Found\r\n");
            pw.print("Content-Length: 0\r\n\r\n");
            pw.flush();
            socket.close();
            return;
        }

        byte[] data = is.readAllBytes();
        is.close();

        String contentType = "text/html";
        if (path.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (path.endsWith(".css")) {
            contentType = "text/css";
        }

        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        pw.print("HTTP/1.1 200 OK\r\n");
        pw.print("Content-Type: " + contentType + "; charset=UTF-8\r\n");
        pw.print("Content-Length: " + data.length + "\r\n\r\n");
        pw.flush();

        out.write(data);
        out.flush();
        socket.close();
    }

    private static void broadcastState() {
        String json = serializeState();
        for (ClientHandler client : clients) {
            client.sendText(json);
        }
    }

    private static String serializeState() {
        boolean isInCheck = com.tacticalgambit.core.domain.GameConditionChecker.isInCheck(gameState.board(), gameState.activePlayer());
        
        List<CardDTO> handList = new ArrayList<>();
        PlayerHand hand = gameState.playerHand();
        for (int i = 0; i < hand.size(); i++) {
            Card c = hand.cards().get(i);
            handList.add(new CardDTO(i, c.name(), c.apCost()));
        }

        Map<String, PieceDTO> boardMap = new HashMap<>();
        Board board = gameState.board();
        for (int rank = 0; rank < 8; rank++) {
            for (int file = 0; file < 8; file++) {
                final int fFile = file;
                final int fRank = rank;
                Square sq = Square.of((char)('a' + fFile), fRank + 1);
                board.getPieceAt(sq).ifPresent(p -> {
                    String coord = "" + (char)('A' + fFile) + (fRank + 1);
                    boardMap.put(coord, new PieceDTO(
                        p.type().name(),
                        p.color().name(),
                        PieceDecorator.isShielded(p),
                        PieceDecorator.hasJumpModifier(p)
                    ));
                });
            }
        }

        Map<String, Integer> barricadesMap = new HashMap<>();
        for (var entry : board.barricades().entrySet()) {
            Square sq = entry.getKey();
            String coord = "" + (char)('A' + sq.file()) + (sq.rank() + 1);
            barricadesMap.put(coord, entry.getValue());
        }

        GameStateDTO dto = new GameStateDTO(
            gameState.activePlayer().name(),
            gameState.actionPoints().current(),
            gameState.hasMovedPieceThisTurn(),
            gameState.gameState().name(),
            isInCheck,
            handList,
            boardMap,
            barricadesMap,
            new ArrayList<>(actionLogs),
            gameState.deck().remainingCards()
        );

        return dto.toJson();
    }

    private static class ClientHandler {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void sendState() {
            sendText(serializeState());
        }

        public void sendText(String text) {
            try {
                byte[] raw = text.getBytes("UTF-8");
                OutputStream os = socket.getOutputStream();
                os.write(0x81); // FIN = 1, Opcode = 1
                if (raw.length <= 125) {
                    os.write(raw.length);
                } else if (raw.length <= 65535) {
                    os.write(126);
                    os.write((raw.length >> 8) & 0xFF);
                    os.write(raw.length & 0xFF);
                } else {
                    return;
                }
                os.write(raw);
                os.flush();
            } catch (Exception e) {
                clients.remove(this);
            }
        }

        public void runLoop() {
            try {
                InputStream is = socket.getInputStream();
                while (true) {
                    int b1 = is.read();
                    if (b1 == -1) break;
                    int b2 = is.read();
                    if (b2 == -1) break;

                    int opcode = b1 & 0x0F;
                    if (opcode == 8) { // CLOSE
                        break;
                    }

                    boolean masked = (b2 & 0x80) != 0;
                    int payloadLength = b2 & 0x7F;
                    if (payloadLength == 126) {
                        int b3 = is.read();
                        int b4 = is.read();
                        payloadLength = (b3 << 8) | b4;
                    } else if (payloadLength == 127) {
                        for (int i = 0; i < 8; i++) is.read();
                        break;
                    }

                    byte[] maskingKey = new byte[4];
                    if (masked) {
                        is.read(maskingKey);
                    }

                    byte[] payload = new byte[payloadLength];
                    int read = 0;
                    while (read < payloadLength) {
                        int r = is.read(payload, read, payloadLength - read);
                        if (r == -1) break;
                        read += r;
                    }

                    if (masked) {
                        for (int i = 0; i < payloadLength; i++) {
                            payload[i] = (byte) (payload[i] ^ maskingKey[i % 4]);
                        }
                    }

                    String text = new String(payload, "UTF-8");
                    processMessage(text);
                }
            } catch (Exception e) {
                // Ignore disconnect
            } finally {
                clients.remove(this);
                try { socket.close(); } catch (Exception e) {}
            }
        }

        private void processMessage(String text) {
            try {
                ActionCommandDTO cmd = ActionCommandDTO.parseJson(text);
                if (cmd == null || cmd.type() == null) {
                    sendError("Comando JSON no reconocido o inválido.");
                    return;
                }

                if (gameState.gameState() != com.tacticalgambit.core.state.GameState.IN_PROGRESS && !"RESET".equals(cmd.type())) {
                    sendError("La partida ha finalizado. Por favor, pulse reiniciar.");
                    return;
                }

                switch (cmd.type()) {
                    case "MOVE" -> {
                        Square from = CommandParser.parseAlgebraic(cmd.from());
                        Square to = CommandParser.parseAlgebraic(cmd.to());
                        com.tacticalgambit.core.domain.PieceType promo = com.tacticalgambit.core.domain.PieceType.QUEEN;
                        if (cmd.promoType() != null && !cmd.promoType().trim().isEmpty()) {
                            promo = switch (cmd.promoType().toUpperCase()) {
                                case "Q" -> com.tacticalgambit.core.domain.PieceType.QUEEN;
                                case "R" -> com.tacticalgambit.core.domain.PieceType.ROOK;
                                case "B" -> com.tacticalgambit.core.domain.PieceType.BISHOP;
                                case "N" -> com.tacticalgambit.core.domain.PieceType.KNIGHT;
                                default -> throw new IllegalArgumentException("Pieza de coronación inválida.");
                            };
                        }
                        PieceMoveAction action = new PieceMoveAction(from, to, promo);
                        if (action.isValid(gameState)) {
                            action.execute(gameState);
                            actionLogs.add("Movimiento: " + cmd.from() + " -> " + cmd.to());
                            broadcastState();
                        } else {
                            if (gameState.hasMovedPieceThisTurn()) {
                                sendError("Ya se ha realizado un movimiento de pieza en este turno.");
                            } else if (!gameState.actionPoints().canAfford(PieceMoveAction.MOVE_AP_COST)) {
                                sendError("AP insuficiente para realizar esta acción (se requieren 2 AP).");
                            } else {
                                sendError("Movimiento ilegal.");
                            }
                        }
                    }
                    case "PLAY_CARD" -> {
                        int idx = cmd.cardIndex();
                        if (idx < 0 || idx >= gameState.playerHand().size()) {
                            sendError("Índice de carta fuera de rango.");
                            return;
                        }
                        Card card = gameState.playerHand().cards().get(idx);
                        CardTarget target = parseTarget(card, cmd);
                        CardPlayAction action = new CardPlayAction(card, target);
                        if (action.isValid(gameState)) {
                            action.execute(gameState);
                            actionLogs.add("Carta jugada: " + card.name());
                            broadcastState();
                        } else {
                            if (!gameState.actionPoints().canAfford(card.apCost())) {
                                sendError("AP insuficiente para realizar esta acción (se requieren " + card.apCost() + " AP).");
                            } else {
                                sendError("No se cumplen las condiciones para jugar esta carta.");
                            }
                        }
                    }
                    case "DISCARD_CARD" -> {
                        int idx = cmd.cardIndex();
                        if (idx < 0 || idx >= gameState.playerHand().size()) {
                            sendError("Índice de carta fuera de rango.");
                            return;
                        }
                        Card card = gameState.playerHand().cards().get(idx);
                        DiscardCardAction action = new DiscardCardAction(card);
                        if (action.isValid(gameState)) {
                            action.execute(gameState);
                            actionLogs.add("Carta descartada: " + card.name());
                            broadcastState();
                        } else {
                            sendError("No es válido descartar esta carta.");
                        }
                    }
                    case "DRAW" -> {
                        DrawCardAction action = new DrawCardAction();
                        if (action.isValid(gameState)) {
                            action.execute(gameState);
                            actionLogs.add("Robó una carta del mazo.");
                            broadcastState();
                        } else {
                            if (!gameState.actionPoints().canAfford(1)) {
                                sendError("AP insuficiente para realizar esta acción (se requiere 1 AP).");
                            } else if (gameState.playerHand().size() >= 4) {
                                sendError("No se puede robar: mano llena (límite de 4 cartas).");
                            } else {
                                sendError("No se puede robar carta.");
                            }
                        }
                    }
                    case "END_TURN" -> {
                        if (com.tacticalgambit.core.domain.GameConditionChecker.isInCheck(gameState.board(), gameState.activePlayer())) {
                            gameState.setGameState(com.tacticalgambit.core.state.GameState.CHECKMATE);
                            actionLogs.add("¡Jaque Mate! El jugador " + (gameState.activePlayer() == com.tacticalgambit.core.domain.PieceColor.WHITE ? "BLANCO" : "NEGRO") + " no pudo salvar a su Rey. Ganador: " + (gameState.activePlayer() == com.tacticalgambit.core.domain.PieceColor.WHITE ? "NEGRO" : "BLANCO") + ".");
                        } else {
                            gameState.startNextTurn();
                            actionLogs.add("Turno finalizado. Juega " + (gameState.activePlayer() == com.tacticalgambit.core.domain.PieceColor.WHITE ? "BLANCO" : "NEGRO"));
                        }
                        broadcastState();
                    }
                    case "RESET" -> {
                        gameState = MatchInitializer.initialize();
                        actionLogs.clear();
                        actionLogs.add("Partida reiniciada.");
                        broadcastState();
                    }
                    default -> sendError("Comando desconocido: " + cmd.type());
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                sendError("Error de regla: " + e.getMessage());
            } catch (Exception e) {
                sendError("Error inesperado: " + e.getMessage());
            }
        }

        private CardTarget parseTarget(Card card, ActionCommandDTO cmd) {
            if (cmd.targetSquare2() != null && !cmd.targetSquare2().trim().isEmpty()) {
                Square s1 = CommandParser.parseAlgebraic(cmd.targetSquare());
                Square s2 = CommandParser.parseAlgebraic(cmd.targetSquare2());
                java.util.Optional<com.tacticalgambit.core.domain.PieceType> promo = java.util.Optional.empty();
                if (cmd.promoType() != null && !cmd.promoType().trim().isEmpty()) {
                    promo = java.util.Optional.of(switch (cmd.promoType().toUpperCase()) {
                        case "Q" -> com.tacticalgambit.core.domain.PieceType.QUEEN;
                        case "R" -> com.tacticalgambit.core.domain.PieceType.ROOK;
                        case "B" -> com.tacticalgambit.core.domain.PieceType.BISHOP;
                        case "N" -> com.tacticalgambit.core.domain.PieceType.KNIGHT;
                        default -> throw new IllegalArgumentException("Pieza de coronación inválida.");
                    });
                }
                return new DoublePieceTarget(s1, s2, promo);
            } else if (cmd.targetSquare() != null && !cmd.targetSquare().trim().isEmpty()) {
                Square s = CommandParser.parseAlgebraic(cmd.targetSquare());
                if (card instanceof com.tacticalgambit.core.domain.card.impl.BarricadeCard) {
                    return new com.tacticalgambit.core.domain.card.SquareTarget(s);
                }
                return new PieceTarget(s);
            } else {
                return new GlobalTarget();
            }
        }

        private void sendError(String errorMsg) {
            String errJson = String.format("{\"type\":\"ERROR\",\"message\":\"%s\"}", errorMsg.replace("\"", "\\\""));
            sendText(errJson);
        }
    }
}
