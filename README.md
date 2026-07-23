# Tactical Gambit

Tactical Gambit is an interactive web-based game that combines classic chess with tactical card mechanics and action points (AP).

## Requirements

- **Java Development Kit (JDK) 21** or higher installed on your system.
- **Apache Maven** installed (optional, but highly recommended for command line execution).

## How to Compile and Run

### Method 1: Using Maven (Recommended & Universal)

If you have Maven installed and added to your system's PATH, you can compile and start the server with a single command from the project root:

```bash
mvn compile exec:java
```

Once the server is running, open your web browser and navigate to:
👉 **[http://localhost:7070](http://localhost:7070)**

---

### Method 2: Import into an IDE (IntelliJ IDEA / VS Code / Eclipse)

1. Open your preferred IDE.
2. Import the project folder as a **Maven project** (using `pom.xml`).
3. Locate the main class: `com.tacticalgambit.web.WebLauncher` located under `src/main/java/com/tacticalgambit/web/WebLauncher.java`.
4. Right-click on the class and select **Run**.

---

### Method 3: Compile and Run Manually from Terminal (without Maven in PATH)

If you have the JDK installed but do not have Maven configured in your PATH environment variable, you can execute the appropriate commands to compile and start the application directly:

#### On Windows (PowerShell):
```powershell
# Compile
javac -d target/classes -sourcepath src/main/java (Get-ChildItem -Recurse -Filter *.java src/main/java).FullName

# Run
java -cp "target/classes;src/main/resources" com.tacticalgambit.web.WebLauncher
```

#### On Linux / macOS:
```bash
# Compile
find src/main/java -name "*.java" > sources.txt
javac -d target/classes -sourcepath src/main/java @sources.txt
rm sources.txt

# Run
java -cp "target/classes:src/main/resources" com.tacticalgambit.web.WebLauncher
```
