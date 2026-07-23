# Tactical Gambit ♟️🃏

Tactical Gambit is an interactive multiplayer game that fuses classic chess rules with tactical card play and action points (AP). It runs on a modern Java 21 backend and interacts with a lightweight HTML5/JavaScript frontend using WebSockets.

---

## 🛠️ Architecture & Design Patterns

The engine has been designed following solid software engineering principles, leveraging modern Java features and classic design patterns to ensure clean, decoupled, and maintainable code:

### 1. Modern Java 21 Sealed Interfaces
The core engine models pieces using Java **Sealed Interfaces** (`Piece`). This guarantees compile-time exhaustiveness checks inside pattern-matching switches and prevents unauthorized class extensions.
```java
public sealed interface Piece permits Pawn, Knight, Bishop, Rook, Queen, King, PieceDecorator { ... }
```

### 2. Decorator Pattern (Dynamic Modifiers)
Instead of relying on deep and rigid inheritance trees, modifiers applied by cards (such as shields or jump capabilities) are implemented using the **Decorator Pattern** (`PieceDecorator`).
* **`JumpModifierDecorator`**: Dynamically wraps a piece to grant it jumping capabilities (similar to a Knight) for its next moves.
* **`ShieldedDecorator`**: Grants protection to pieces, intercepting and absorbing capture events.

### 3. Command Pattern (Decoupling Game Actions)
Every interaction (moving a piece, drawing cards, playing cards) is encapsulated as a `MoveCommand` or a `GameAction` sub-type. This keeps the execution logic decoupled from the state manager, allowing easy expansion of actions or potential replay/undo systems.

### 4. Client-Server Architecture via WebSockets
The communication flow between the browser client and the Java engine is asynchronous and event-driven:

```mermaid
graph TD
    subgraph Frontend (Client Browser)
        HTML[index.html & style.css] --> JS[game.js]
    end

    subgraph Backend (Java Server)
        Launcher[WebLauncher] --> Server[GameWebSocketServer]
        Server <--> |WebSocket JSON| JS
        Server --> Core[Tactical Gambit Core]
    end

    subgraph Tactical Gambit Core
        Core --> State[TurnState / GameState]
        Core --> Board[Board]
        Core --> Rules[GameConditionChecker]
        Core --> Cmd[Command & Action]
        Core --> Card[Card & Deck]
    end
```

---

## 🚀 Tech Stack

* **Language**: Java 21 (using Sealed Classes, Records, and Pattern Matching).
* **Web Server / WebSocket**: Javalin 6 (running on embedded Jetty).
* **Testing Framework**: JUnit 5 (Junit Jupiter) for automated unit testing.
* **Frontend**: Vanilla HTML5, CSS3, and JavaScript (WebSockets API).
* **Build System**: Apache Maven.

---

## 📋 Requirements

* **Java Development Kit (JDK) 21** or higher.
* **Apache Maven** (configured in your system's PATH).

---

## ⚙️ How to Compile and Run

### Method 1: Using Maven (Recommended)
Compile the source code and run the WebSocket server in one go:
```bash
mvn compile exec:java
```
Once started, navigate to:
👉 **[http://localhost:7070](http://localhost:7070)**

### Method 2: Import into an IDE (IntelliJ IDEA / VS Code)
1. Open your IDE and import the repository folder as a **Maven project** (using `pom.xml`).
2. Run the main class `com.tacticalgambit.web.WebLauncher`.

### Method 3: Build a Fat JAR (Exec Release)
To package the entire application (including dependencies like Javalin) into a single standalone executable `.jar` file:
```bash
mvn clean package
```
This generates the jar in the `target/` directory. Run it with:
```bash
java -jar target/tacticalgambit-core-1.0.0-SNAPSHOT.jar
```

---

## 🧪 Running Tests

The project includes an extensive test suite to validate game rules, card behaviors, checkmate/stalemate checks, and decorators:
```bash
mvn test
```
*Continuous Integration (CI) is configured via GitHub Actions. Every push or pull request automatically triggers test executions to guarantee stability.*
