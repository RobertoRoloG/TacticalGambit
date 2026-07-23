// game.js - Tactical Gambit Web Client

const canvas = document.getElementById("gameBoard");
const ctx = canvas.getContext("2d");
const CELL_SIZE = canvas.width / 8;

let socket = null;
let gameState = null;

// Estados de interacción por ratón
let selectedSquare = null; // e.g. "E2"
let selectedCardIndex = null; // Índice de carta en mano seleccionada
let cardTargetSquare1 = null; // Primer target de carta (para Regroup)
let currentState = "NEUTRAL"; // 'NEUTRAL', 'PIECE_SELECTED', 'CARD_TARGETING'

// Piezas Unicode mapeadas de DTO a símbolo visual
const PIECE_GLYPHS = {
    "KING": { "WHITE": "♔", "BLACK": "♚" },
    "QUEEN": { "WHITE": "♕", "BLACK": "♛" },
    "ROOK": { "WHITE": "♖", "BLACK": "♜" },
    "BISHOP": { "WHITE": "♗", "BLACK": "♝" },
    "KNIGHT": { "WHITE": "♘", "BLACK": "♞" },
    "PAWN": { "WHITE": "♙", "BLACK": "♟" }
};

const CARD_DESCRIPTIONS = {
    "Side Step": "Move an allied Pawn 1 square horizontally.",
    "Cycle Hand": "Discard this card and draw 2 cards.",
    "Tactical Dash": "Move own piece (not King/Pawn) 1 square.",
    "Shield": "Shield allied piece (not King) for 1 turn.",
    "Tactical Jump": "Allow jumping over an allied piece for 1 turn.",
    "Regroup": "Swap 2 allied pieces at dist. <= 3 (1 Pawn max).",
    "Barricade": "Block an empty square for 2 turns.",
    "Overcharge": "+2 💎 now. -2 💎 on your next turn."
};

// Conectar por WebSocket al backend
function connect() {
    socket = new WebSocket("ws://localhost:7070/ws/game");

    socket.onopen = () => {
        console.log("Conectado al servidor de Tactical Gambit.");
    };

    socket.onmessage = (event) => {
        const data = JSON.parse(event.data);

        if (data.type === "ERROR") {
            showNotification(data.message);
            if (data.message.includes("AP insuficiente")) {
                const apCard = document.getElementById("apCard");
                if (apCard) {
                    apCard.classList.add("error-ap");
                    setTimeout(() => {
                        apCard.classList.remove("error-ap");
                    }, 1500);
                }
            }
        } else {
            // Es un GameStateDTO
            gameState = data;
            selectedSquare = null;
            selectedCardIndex = null;
            cardTargetSquare1 = null;
            currentState = "NEUTRAL";
            updateUI();
            drawBoard();
        }
    };

    socket.onclose = () => {
        console.log("Conexión perdida con el servidor. Reintentando en 3 segundos...");
        setTimeout(connect, 3000);
    };

    socket.onerror = (err) => {
        console.error("Error en WebSocket:", err);
    };
}

// Dibujar el tablero en el canvas
function drawBoard() {
    if (!gameState) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Actualizar el cursor según el estado de la máquina de estados
    if (currentState === "CARD_TARGETING") {
        canvas.style.cursor = "crosshair";
    } else {
        canvas.style.cursor = "pointer";
    }

    for (let rank = 7; rank >= 0; rank--) {
        for (let file = 0; file < 8; file++) {
            const coord = String.fromCharCode(65 + file) + (rank + 1);
            const x = file * CELL_SIZE;
            const y = (7 - rank) * CELL_SIZE;

            // Determinar color de casilla alternada (estilo pergamino/azul-teal de las cartas)
            const isDark = (file + rank) % 2 === 0;
            ctx.fillStyle = isDark ? "#52737f" : "#eaddc7";
            ctx.fillRect(x, y, CELL_SIZE, CELL_SIZE);

            // Resaltar casilla seleccionada para movimiento físico: Fondo teal translúcido
            if (selectedSquare === coord) {
                ctx.fillStyle = "rgba(13, 148, 136, 0.3)";
                ctx.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            }

            // Dibujar barricada si existe en esta casilla
            const barricadeTurns = gameState.barricades?.[coord];
            if (barricadeTurns !== undefined) {
                ctx.fillStyle = "rgba(120, 53, 4, 0.45)"; // Ámbar/Marrón oscuro translúcido
                ctx.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                ctx.strokeStyle = "rgba(120, 53, 4, 0.8)";
                ctx.lineWidth = 3;
                ctx.strokeRect(x + 4, y + 4, CELL_SIZE - 8, CELL_SIZE - 8);
                ctx.beginPath();
                ctx.moveTo(x + 4, y + 4);
                ctx.lineTo(x + CELL_SIZE - 4, y + CELL_SIZE - 4);
                ctx.moveTo(x + CELL_SIZE - 4, y + 4);
                ctx.lineTo(x + 4, y + CELL_SIZE - 4);
                ctx.stroke();

                ctx.fillStyle = "#fffdfa";
                ctx.font = "bold 12px 'Plus Jakarta Sans', sans-serif";
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                const displayTurns = Math.ceil(barricadeTurns / 2);
                ctx.fillText(`🚧 ${displayTurns}T`, x + CELL_SIZE / 2, y + CELL_SIZE / 2);
            }

            // Resaltar primer target de carta seleccionado: Borde sólido amarillo/dorado
            if (cardTargetSquare1 === coord) {
                ctx.strokeStyle = "#D97706";
                ctx.lineWidth = 3;
                ctx.strokeRect(x + 1.5, y + 1.5, CELL_SIZE - 3, CELL_SIZE - 3);
            }

            // Dibujar la pieza si existe
            const piece = gameState.board[coord];
            if (piece) {
                // Si el Rey está en Jaque: Borde sólido rojo de 3px
                if (piece.type === "KING" && piece.color === gameState.activePlayer && gameState.isInCheck) {
                    ctx.strokeStyle = "#DC2626";
                    ctx.lineWidth = 3;
                    ctx.strokeRect(x + 1.5, y + 1.5, CELL_SIZE - 3, CELL_SIZE - 3);
                }

                // Dibujar decoradores especiales
                if (piece.isShielded) {
                    // Dibujar círculo esmeralda de escudo
                    ctx.strokeStyle = "#0d9488";
                    ctx.lineWidth = 4;
                    ctx.beginPath();
                    ctx.arc(x + CELL_SIZE/2, y + CELL_SIZE/2, CELL_SIZE/2.5, 0, Math.PI * 2);
                    ctx.stroke();
                }

                if (piece.hasJumpModifier) {
                    // Dibujar círculo terracotta de salto
                    ctx.strokeStyle = "#c2410c";
                    ctx.lineWidth = 2.5;
                    ctx.setLineDash([5, 3]);
                    ctx.beginPath();
                    ctx.arc(x + CELL_SIZE/2, y + CELL_SIZE/2, CELL_SIZE/2.8, 0, Math.PI * 2);
                    ctx.stroke();
                    ctx.setLineDash([]);
                }

                // Dibujar ficha circular premium (estilo mármol/lujo) para la pieza
                ctx.save();
                
                // Sombra de la pieza física
                ctx.shadowColor = "rgba(43, 34, 26, 0.18)";
                ctx.shadowBlur = 6;
                ctx.shadowOffsetY = 3;
                
                const isWhite = piece.color === "WHITE";
                const badgeGrad = ctx.createRadialGradient(
                    x + CELL_SIZE/2 - 4, y + CELL_SIZE/2 - 4, 3,
                    x + CELL_SIZE/2, y + CELL_SIZE/2, CELL_SIZE/2.6
                );
                if (isWhite) {
                    badgeGrad.addColorStop(0, "#ffffff");
                    badgeGrad.addColorStop(1, "#f3ece4"); // Mármol crema
                } else {
                    badgeGrad.addColorStop(0, "#5a4b41");
                    badgeGrad.addColorStop(1, "#2b201a"); // Ébano/Charcoal
                }
                
                ctx.fillStyle = badgeGrad;
                ctx.beginPath();
                ctx.arc(x + CELL_SIZE/2, y + CELL_SIZE/2, CELL_SIZE/2.6, 0, Math.PI * 2);
                ctx.fill();
                
                // Borde de la ficha
                ctx.strokeStyle = isWhite ? "rgba(43, 34, 26, 0.12)" : "rgba(255, 255, 255, 0.08)";
                ctx.lineWidth = 1.5;
                ctx.stroke();
                
                ctx.restore(); // Limpiar sombras para el glifo
                
                // Dibujar el símbolo vectorial premium en el centro de la ficha circular
                drawPieceSymbol(ctx, piece.type, isWhite, x + CELL_SIZE / 2, y + CELL_SIZE / 2, CELL_SIZE * 0.72);
            }

            // Notación Algebraica: etiquetas con alto contraste dinámico y fuente monospaciada JetBrains Mono
            if (file === 0) {
                const isDarkSquare = (0 + rank) % 2 === 0;
                ctx.fillStyle = isDarkSquare ? "#fdfbf7" : "#2b221a";
                ctx.font = "bold 10px 'JetBrains Mono', monospace";
                ctx.textAlign = "left";
                ctx.fillText(rank + 1, x + 4, y + 13);
            }
            if (rank === 0) {
                const isDarkSquare = (file + 0) % 2 === 0;
                ctx.fillStyle = isDarkSquare ? "#fdfbf7" : "#2b221a";
                ctx.font = "bold 10px 'JetBrains Mono', monospace";
                ctx.textAlign = "right";
                ctx.fillText(String.fromCharCode(65 + file), x + CELL_SIZE - 4, y + CELL_SIZE - 4);
            }
        }
    }
}

// Actualizar los elementos de la interfaz web
function updateUI() {
    if (!gameState) return;

    // Jugador activo
    const activePlayerEl = document.getElementById("activePlayerVal");
    activePlayerEl.innerText = gameState.activePlayer === "WHITE" ? "WHITE" : "BLACK";
    activePlayerEl.className = "info-value " + (gameState.activePlayer === "WHITE" ? "val-white" : "val-black");

    // AP
    document.getElementById("apVal").innerHTML = `${gameState.actionPoints} / 5 <span style="font-size: 0.9rem; margin-left: 2px;">💎</span>`;

    // Movimiento Físico
    const moveStatusEl = document.getElementById("moveStatusVal");
    if (moveStatusEl) {
        if (gameState.hasMovedPiece) {
            moveStatusEl.innerHTML = `<span class="move-badge completed">Done</span>`;
        } else {
            moveStatusEl.innerHTML = `<span class="move-badge pending">Available</span>`;
        }
    }

    // Estado
    const stateVal = document.getElementById("gameStateVal");
    const overlay = document.getElementById("gameOverOverlay");
    const overlayTitle = document.getElementById("gameOverTitle");
    const overlayMsg = document.getElementById("gameOverMessage");

    if (gameState.gameState === "CHECKMATE") {
        stateVal.innerText = `CHECKMATE (Winner: ${gameState.activePlayer === "WHITE" ? "BLACK" : "WHITE"})`;
        stateVal.style.color = "var(--accent-red)";
        
        const winner = gameState.activePlayer === "WHITE" ? "BLACK" : "WHITE";
        overlayTitle.innerText = "CHECKMATE!";
        overlayTitle.style.color = "var(--accent-terracotta)";
        overlayMsg.innerHTML = `Game over.<br>The winner is the <strong>${winner}</strong> player.`;
        overlay.style.display = "flex";
    } else if (gameState.gameState === "STALEMATE") {
        stateVal.innerText = "STALEMATE";
        stateVal.style.color = "var(--accent-gold)";
        
        overlayTitle.innerText = "STALEMATE!";
        overlayTitle.style.color = "var(--accent-amber)";
        overlayMsg.innerHTML = "Game over.<br>Stalemate.";
        overlay.style.display = "flex";
    } else {
        stateVal.innerText = gameState.isInCheck ? "KING IN CHECK!" : "In Progress";
        stateVal.style.color = gameState.isInCheck ? "var(--accent-red)" : "var(--accent-cyan)";
        overlay.style.display = "none";
    }

    // Action log
    const actionLogEl = document.getElementById("actionLogVal");
    if (actionLogEl && gameState.actionLogs) {
        actionLogEl.innerHTML = gameState.actionLogs.map(log => `<div style="margin-bottom: 4px; border-bottom: 1px solid rgba(43,34,26,0.03); padding-bottom: 4px;">${log}</div>`).join("");
        actionLogEl.scrollTop = actionLogEl.scrollHeight;
    }

    // Mano de cartas
    const cardsListEl = document.getElementById("cardsList");
    cardsListEl.innerHTML = "";

    if (gameState.hand.length === 0) {
        cardsListEl.innerHTML = `
            <div class="card-item empty-hand" style="border: 1px dashed rgba(43,34,26,0.15); justify-content: center; align-items: center; background: none;">
                <span style="color: var(--text-muted); font-size: 0.85rem;">Empty hand</span>
            </div>`;
    } else {
        gameState.hand.forEach((card, index) => {
            let rarityClass = "rarity-common";
            let rarityLabel = "Common Card";
            if (card.apCost === 2) {
                rarityClass = "rarity-rare";
                rarityLabel = "Rare Card";
            } else if (card.apCost === 3 || card.apCost === 0) {
                rarityClass = "rarity-epic";
                rarityLabel = "Epic Card";
            }

            const isSelected = selectedCardIndex === index;
            const cardEl = document.createElement("div");
            cardEl.className = `card-item ${rarityClass} ${isSelected ? "selected" : ""}`;

            const cardImageName = card.name.toLowerCase().replace(/\s+/g, '_') + '.png';
            const cardEmojis = {
                "Side Step": "👣",
                "Cycle Hand": "🔄",
                "Tactical Dash": "⚡",
                "Shield": "🛡️",
                "Tactical Jump": "🦘",
                "Regroup": "👥",
                "Barricade": "🚧",
                "Overcharge": "🔋"
            };
            const emoji = cardEmojis[card.name] || "🃏";

            // Escala de imagen personalizada para evitar recortes (ej. flechas de Tactical Dash)
            const imgScales = {
                "Side Step": "2.0",
                "Cycle Hand": "2.0",
                "Tactical Dash": "1.35",
                "Shield": "2.0",
                "Tactical Jump": "1.35",
                "Regroup": "1.35",
                "Barricade": "1.35",
                "Overcharge": "1.35"
            };
            const scale = imgScales[card.name] || "2.0";

            cardEl.innerHTML = `
                <div class="card-inner-frame">
                    <div class="card-header-wrapper">
                        <div class="card-checkered-emblem"></div>
                        <span class="card-name">${card.name}</span>
                        <div class="card-ap-diamond">
                            <span class="ap-num">${card.apCost}</span>
                            <span class="ap-lbl">AP</span>
                        </div>
                    </div>
                    
                    <div class="card-illustration-frame">
                        <div class="gear-decor gear-decor-tl"></div>
                        <div class="gear-decor gear-decor-tr"></div>
                        <div class="gear-decor gear-decor-bl"></div>
                        <div class="gear-decor gear-decor-br"></div>
                        
                        <img src="/assets/cards/${cardImageName}" alt="${card.name}" style="transform: scale(${scale}); transform-origin: center;" onerror="this.style.display='none'; this.nextElementSibling.classList.add('active');" />
                        <div class="card-illustration-placeholder">
                            <span>${emoji}</span>
                        </div>
                    </div>
                    
                    <div class="card-rarity-badge">${rarityLabel}</div>
                    
                    <div class="card-body-wrapper">
                        <p class="card-desc">${CARD_DESCRIPTIONS[card.name] || ""}</p>
                        <div class="card-help">L-Click: Target | R-Click: Discard</div>
                    </div>
                </div>
            `;

            // Clic izquierdo en la carta para Jugar/Seleccionar
            cardEl.addEventListener("click", () => {
                if (card.name === "Cycle Hand" || card.name === "Overcharge") {
                    currentState = "NEUTRAL";
                    sendAction({
                        type: "PLAY_CARD",
                        cardIndex: index
                    });
                } else {
                    if (selectedCardIndex === index) {
                        selectedCardIndex = null;
                        cardTargetSquare1 = null;
                        currentState = "NEUTRAL";
                    } else {
                        // Cancelar cualquier selección física activa
                        selectedSquare = null;
                        selectedCardIndex = index;
                        cardTargetSquare1 = null;
                        currentState = "CARD_TARGETING";
                        if (card.name === "Regroup" || card.name === "Side Step" || card.name === "Tactical Dash") {
                            showNotification(`Card ${card.name} selected. Choose source piece.`, "info");
                        } else {
                            showNotification(`Card ${card.name} selected. Click target on the board.`, "info");
                        }
                    }
                    updateUI();
                    drawBoard();
                }
            });

            // Clic derecho para descartar la carta
            cardEl.addEventListener("contextmenu", (e) => {
                e.preventDefault();
                e.stopPropagation();
                currentState = "NEUTRAL";
                sendAction({
                    type: "DISCARD_CARD",
                    cardIndex: index
                });
            });

            cardsListEl.appendChild(cardEl);
        });
    }

    // Mostrar estado de mano
    document.getElementById("handStatus").innerText = `${gameState.hand.length} / 4 cartas`;

    // Actualizar indicador de baraja
    const deckSize = gameState.deckSize !== undefined ? gameState.deckSize : 0;
    const deckCountEl = document.getElementById("deckCount");
    const deckIndicatorEl = document.getElementById("deckIndicator");
    if (deckCountEl) deckCountEl.innerText = deckSize;
    if (deckIndicatorEl) {
        if (deckSize === 0) {
            deckIndicatorEl.classList.add("empty");
        } else {
            deckIndicatorEl.classList.remove("empty");
        }
        // Ocultar capas de cartas según el tamaño de la baraja
        const card3 = deckIndicatorEl.querySelector(".deck-card-3");
        const card2 = deckIndicatorEl.querySelector(".deck-card-2");
        if (card3) card3.style.display = deckSize >= 3 ? "block" : "none";
        if (card2) card2.style.display = deckSize >= 2 ? "block" : "none";
    }
}

// Manejar los clics en el canvas (tablero)
canvas.addEventListener("click", (e) => {
    if (!gameState || gameState.gameState !== "IN_PROGRESS") return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const file = Math.floor(x / CELL_SIZE);
    const rank = 7 - Math.floor(y / CELL_SIZE);

    if (file < 0 || file > 7 || rank < 0 || rank > 7) return;

    const coord = String.fromCharCode(65 + file) + (rank + 1);

    // Si hay una carta seleccionada (CARD_TARGETING)
    if (selectedCardIndex !== null) {
        const card = gameState.hand[selectedCardIndex];
        
        // Si es Regroup, Side Step o Tactical Dash (requieren 2 objetivos)
        if (card.name === "Regroup" || card.name === "Side Step" || card.name === "Tactical Dash") {
            if (cardTargetSquare1 === null) {
                cardTargetSquare1 = coord;
                if (card.name === "Side Step") {
                    showNotification("Pawn selected. Choose adjacent empty horizontal square.");
                } else if (card.name === "Tactical Dash") {
                    showNotification("Piece selected. Choose adjacent empty square in any direction.");
                } else {
                    showNotification("First target selected. Click on the second target.");
                }
                drawBoard();
            } else {
                if (cardTargetSquare1 === coord) {
                    // Click on same source piece cancels target selection
                    cardTargetSquare1 = null;
                    selectedCardIndex = null;
                    currentState = "NEUTRAL";
                    updateUI();
                    drawBoard();
                    return;
                }
                // Check if card action promotes a Pawn
                const p1 = gameState.board[cardTargetSquare1];
                const p2 = gameState.board[coord];
                const activeColor = gameState.activePlayer;
                const isP1PawnPromo = p1 && p1.type === "PAWN" && ((p1.color === "WHITE" && rank === 7) || (p1.color === "BLACK" && rank === 0));
                const isP2PawnPromo = p2 && p2.type === "PAWN" && ((p2.color === "WHITE" && cardTargetSquare1.endsWith("8")) || (p2.color === "BLACK" && cardTargetSquare1.endsWith("1")));

                if (isP1PawnPromo || isP2PawnPromo) {
                    showPromotionModal(activeColor, (promo) => {
                        sendAction({
                            type: "PLAY_CARD",
                            cardIndex: selectedCardIndex,
                            targetSquare: cardTargetSquare1,
                            targetSquare2: coord,
                            promoType: promo
                        });
                    });
                } else {
                    sendAction({
                        type: "PLAY_CARD",
                        cardIndex: selectedCardIndex,
                        targetSquare: cardTargetSquare1,
                        targetSquare2: coord
                    });
                }
            }
        } else {
            // Carta de 1 objetivo
            sendAction({
                type: "PLAY_CARD",
                cardIndex: selectedCardIndex,
                targetSquare: coord
            });
        }
    } else {
        // Interacción de movimiento físico de piezas
        if (selectedSquare === null) {
            // Seleccionar pieza propia
            const piece = gameState.board[coord];
            if (piece && piece.color === gameState.activePlayer) {
                selectedSquare = coord;
                currentState = "PIECE_SELECTED";
                drawBoard();
            }
        } else {
            // Confirmar movimiento
            if (selectedSquare === coord) {
                // Deseleccionar si hace clic de nuevo en la misma pieza
                selectedSquare = null;
                currentState = "NEUTRAL";
                drawBoard();
            } else {
                // If physical piece is a Pawn moving to back rank, show promotion modal
                const piece = gameState.board[selectedSquare];
                if (piece && piece.type === "PAWN" && ((piece.color === "WHITE" && rank === 7) || (piece.color === "BLACK" && rank === 0))) {
                    showPromotionModal(piece.color, (promo) => {
                        sendAction({
                            type: "MOVE",
                            from: selectedSquare,
                            to: coord,
                            promoType: promo
                        });
                    });
                } else {
                    sendAction({
                        type: "MOVE",
                        from: selectedSquare,
                        to: coord
                    });
                }
            }
        }
    }
});

// Enviar acción vía WebSocket
function sendAction(actionObj) {
    if (socket && socket.readyState === WebSocket.OPEN) {
        socket.send(JSON.stringify(actionObj));
    } else {
        showNotification("No connection to the game server.");
    }
}

// Botones de acción general
document.getElementById("drawBtn").addEventListener("click", () => {
    sendAction({ type: "DRAW" });
});

document.getElementById("passBtn").addEventListener("click", () => {
    sendAction({ type: "END_TURN" });
});

document.getElementById("resetBtn").addEventListener("click", () => {
    if (confirm("Are you sure you want to restart the match?")) {
        sendAction({ type: "RESET" });
    }
});

document.getElementById("overlayResetBtn").addEventListener("click", () => {
    sendAction({ type: "RESET" });
});

// Cancelación mediante teclado (Escape) y click derecho (contextmenu)
window.addEventListener("keydown", (e) => {
    if (e.key === "Escape") {
        cancelActiveInteraction();
    }
});

canvas.addEventListener("contextmenu", (e) => {
    e.preventDefault(); // Evitar menú de click derecho nativo
    cancelActiveInteraction();
});

function cancelActiveInteraction() {
    if (currentState !== "NEUTRAL" || selectedSquare !== null || selectedCardIndex !== null) {
        selectedSquare = null;
        selectedCardIndex = null;
        cardTargetSquare1 = null;
        currentState = "NEUTRAL";
        showNotification("Interaction cancelled.", "info");
        updateUI();
        drawBoard();
    }
}

// Mostrar notificaciones de error o información
function showNotification(message, type = "error") {
    const container = document.getElementById("notifications");
    const notification = document.createElement("div");
    notification.className = "notification";
    if (type === "info") {
        notification.style.background = "rgba(6, 182, 212, 0.9)";
        notification.style.borderLeft = "4px solid #0891b2";
    }
    notification.innerHTML = `
        <span>${message}</span>
        <span class="notification-close">&times;</span>
    `;

    notification.querySelector(".notification-close").addEventListener("click", () => {
        notification.remove();
    });

    container.appendChild(notification);

    // Auto-eliminar a los 5 segundos
    setTimeout(() => {
        notification.remove();
    }, 5000);
}

// Inicializar conexión
connect();

// Dibuja el símbolo vectorial premium para cada tipo de pieza en el centro de su ficha
function drawPieceSymbol(ctx, type, isWhite, cx, cy, size) {
    ctx.save();
    ctx.translate(cx, cy);
    ctx.scale(size / 32, size / 32); // Normalizar coordenadas de -16 a 16

    ctx.fillStyle = isWhite ? "#2b221a" : "#fdfbf7";
    ctx.strokeStyle = isWhite ? "#2b221a" : "#fdfbf7";
    ctx.lineWidth = 1;
    ctx.lineJoin = "round";
    ctx.lineCap = "round";

    // Efecto de resplandor sutil (shadow) bajo el glifo
    ctx.shadowColor = isWhite ? "rgba(13, 148, 136, 0.35)" : "rgba(194, 65, 12, 0.4)";
    ctx.shadowBlur = 2.5;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 1;

    switch (type) {
        case "PAWN":
            // Cabeza
            ctx.beginPath();
            ctx.arc(0, -4.5, 4.5, 0, Math.PI * 2);
            ctx.fill();
            // Cuello / Anillo
            ctx.beginPath();
            ctx.roundRect(-4, 1, 8, 1.5, 0.5);
            ctx.fill();
            // Cuerpo & Base
            ctx.beginPath();
            ctx.moveTo(-3, 1);
            ctx.quadraticCurveTo(-4.5, 4.5, -5.5, 8.5);
            ctx.lineTo(5.5, 8.5);
            ctx.quadraticCurveTo(4.5, 4.5, 3, 1);
            ctx.closePath();
            ctx.fill();
            // Anillo base inferior
            ctx.beginPath();
            ctx.roundRect(-6.5, 8.5, 13, 2, 1);
            ctx.fill();
            break;

        case "KNIGHT":
            // Base
            ctx.beginPath();
            ctx.roundRect(-7, 8.5, 14, 2, 1);
            ctx.fill();
            // Cuerpo y Cabeza de Caballo
            ctx.beginPath();
            ctx.moveTo(5, 8.5);
            // Curva de la espalda
            ctx.quadraticCurveTo(6.5, 3, 4.5, -3);
            // Orejas
            ctx.lineTo(5.5, -7);
            ctx.lineTo(3.5, -6);
            ctx.lineTo(2.5, -8);
            ctx.lineTo(1.5, -6);
            // Frente y hocico
            ctx.quadraticCurveTo(-1.5, -7.5, -5.5, -4);
            ctx.quadraticCurveTo(-7.5, -2, -6.5, 0);
            ctx.quadraticCurveTo(-5.5, 1.5, -4.5, 1);
            // Pecho y crin
            ctx.quadraticCurveTo(-1.5, 1, -2.5, 4);
            ctx.quadraticCurveTo(-3.5, 6.5, -4.5, 8.5);
            ctx.closePath();
            ctx.fill();

            // Ojo cortado (del color de la ficha de fondo)
            ctx.fillStyle = isWhite ? "#f6efe2" : "#3a2d24";
            ctx.beginPath();
            ctx.arc(-2, -3.5, 0.9, 0, Math.PI * 2);
            ctx.fill();
            break;

        case "BISHOP":
            // Base
            ctx.beginPath();
            ctx.roundRect(-7, 8.5, 14, 2, 1);
            ctx.fill();
            // Cuello
            ctx.beginPath();
            ctx.roundRect(-4.5, 2, 9, 1.5, 0.5);
            ctx.fill();
            // Cuerpo
            ctx.beginPath();
            ctx.moveTo(-3, 2);
            ctx.quadraticCurveTo(-4, 5.5, -5, 8.5);
            ctx.lineTo(5, 8.5);
            ctx.quadraticCurveTo(4, 5.5, 3, 2);
            ctx.closePath();
            ctx.fill();
            // Cabeza de Mitra
            ctx.beginPath();
            ctx.moveTo(0, -6.5);
            ctx.quadraticCurveTo(4.5, -3.5, 3.5, 1.5);
            ctx.lineTo(-3.5, 1.5);
            ctx.quadraticCurveTo(-4.5, -3.5, 0, -6.5);
            ctx.closePath();
            ctx.fill();
            // Esfera superior
            ctx.beginPath();
            ctx.arc(0, -8, 1.5, 0, Math.PI * 2);
            ctx.fill();

            // Corte diagonal de la Mitra (del color de la ficha)
            ctx.strokeStyle = isWhite ? "#f6efe2" : "#3a2d24";
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(1, -3);
            ctx.lineTo(-1.5, -0.5);
            ctx.stroke();
            break;

        case "ROOK":
            // Base
            ctx.beginPath();
            ctx.roundRect(-7.5, 8.5, 15, 2, 1);
            ctx.fill();
            // Cuerpo de Torre
            ctx.beginPath();
            ctx.moveTo(-4.5, -1.5);
            ctx.lineTo(-5.5, 8.5);
            ctx.lineTo(5.5, 8.5);
            ctx.lineTo(4.5, -1.5);
            ctx.closePath();
            ctx.fill();
            // Almena/Corona
            ctx.beginPath();
            ctx.roundRect(-6, -6, 12, 4.5, 0.5);
            ctx.fill();

            // Cortes de las almenas (del color de la ficha)
            ctx.fillStyle = isWhite ? "#f6efe2" : "#3a2d24";
            ctx.fillRect(-4.5, -6.5, 1.8, 2);
            ctx.fillRect(-0.9, -6.5, 1.8, 2);
            ctx.fillRect(2.7, -6.5, 1.8, 2);
            break;

        case "QUEEN":
            // Base
            ctx.beginPath();
            ctx.roundRect(-8, 8.5, 16, 2, 1);
            ctx.fill();
            // Cuerpo
            ctx.beginPath();
            ctx.moveTo(-3.5, 1.5);
            ctx.quadraticCurveTo(-5, 5, -6, 8.5);
            ctx.lineTo(6, 8.5);
            ctx.quadraticCurveTo(5, 5, 3.5, 1.5);
            ctx.closePath();
            ctx.fill();
            // Picos de la Corona
            ctx.beginPath();
            ctx.moveTo(-4.5, 1.5);
            ctx.lineTo(-5.5, -4);
            ctx.lineTo(-2.5, -1);
            ctx.lineTo(0, -6);
            ctx.lineTo(2.5, -1);
            ctx.lineTo(5.5, -4);
            ctx.lineTo(4.5, 1.5);
            ctx.closePath();
            ctx.fill();
            // Esferas superiores de la corona
            ctx.beginPath();
            ctx.arc(-5.5, -4.5, 0.8, 0, Math.PI * 2);
            ctx.arc(0, -6.5, 0.8, 0, Math.PI * 2);
            ctx.arc(5.5, -4.5, 0.8, 0, Math.PI * 2);
            ctx.fill();
            break;

        case "KING":
            // Base
            ctx.beginPath();
            ctx.roundRect(-8, 8.5, 16, 2, 1);
            ctx.fill();
            // Cuerpo
            ctx.beginPath();
            ctx.moveTo(-3.5, 1.5);
            ctx.quadraticCurveTo(-5, 5, -6, 8.5);
            ctx.lineTo(6, 8.5);
            ctx.quadraticCurveTo(5, 5, 3.5, 1.5);
            ctx.closePath();
            ctx.fill();
            // Cuerpo de la corona real
            ctx.beginPath();
            ctx.roundRect(-5.5, -4, 11, 5.5, 1);
            ctx.fill();
            // Cruz superior
            ctx.strokeStyle = isWhite ? "#2b221a" : "#fdfbf7";
            ctx.lineWidth = 1.2;
            ctx.beginPath();
            ctx.moveTo(0, -4.5);
            ctx.lineTo(0, -8.5);
            ctx.moveTo(-2, -6.5);
            ctx.lineTo(2, -6.5);
            ctx.stroke();
            break;
    }

    ctx.restore();
}

// Muestra el modal de coronación interactivo con fichas estilizadas
function showPromotionModal(playerColor, callback) {
    const overlay = document.getElementById("promotionOverlay");
    const optionsContainer = document.getElementById("promotionOptions");
    
    // Limpiar opciones previas
    optionsContainer.innerHTML = "";
    
    const isWhite = playerColor === "WHITE";
    const promoClass = isWhite ? "promo-white" : "promo-black";
    const pieces = [
        { type: "Q", glyph: isWhite ? "♕" : "♛", title: "Queen" },
        { type: "R", glyph: isWhite ? "♖" : "♜", title: "Rook" },
        { type: "B", glyph: isWhite ? "♗" : "♝", title: "Bishop" },
        { type: "N", glyph: isWhite ? "♘" : "♞", title: "Knight" }
    ];
    
    pieces.forEach(p => {
        const btn = document.createElement("button");
        btn.className = `promo-btn ${promoClass}`;
        btn.innerHTML = p.glyph;
        btn.title = p.title;
        btn.addEventListener("click", () => {
            overlay.style.display = "none";
            callback(p.type);
        });
        optionsContainer.appendChild(btn);
    });
    
    overlay.style.display = "flex";
}
