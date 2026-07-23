# Tactical Gambit

Tactical Gambit es un juego web interactivo que combina ajedrez clásico con mecánicas de cartas tácticas y puntos de acción (AP).

## Requisitos

- **Java Development Kit (JDK) 21** o superior instalado en el sistema.
- **Apache Maven** instalado (opcional, pero altamente recomendado para ejecución desde terminal).

## Cómo Compilar y Ejecutar

### Método 1: Usando Maven (Recomendado y Universal)

Si tienes Maven instalado y en el PATH del sistema, puedes compilar e iniciar el servidor con un solo comando desde la raíz del proyecto:

```bash
mvn compile exec:java
```

Una vez iniciado el servidor, abre tu navegador web en:
👉 **[http://localhost:7070](http://localhost:7070)**

---

### Método 2: Importar en un IDE (IntelliJ IDEA / VS Code / Eclipse)

1. Abre tu IDE de preferencia.
2. Importa la carpeta del proyecto como un **proyecto Maven** (`pom.xml`).
3. Busca la clase principal: `com.tacticalgambit.web.WebLauncher` en la ruta `src/main/java/com/tacticalgambit/web/WebLauncher.java`.
4. Haz clic derecho sobre la clase y selecciona **Run** (Ejecutar).

---

### Método 3: Compilar y Ejecutar Manualmente desde Terminal (sin Maven instalado en PATH)

Si tienes JDK instalado pero no tienes Maven configurado en tu variable de entorno PATH, puedes ejecutar el script adecuado para compilar e iniciar directamente:

#### En Windows (PowerShell):
```powershell
# Compilar
javac -d target/classes -sourcepath src/main/java (Get-ChildItem -Recurse -Filter *.java src/main/java).FullName

# Ejecutar
java -cp "target/classes;src/main/resources" com.tacticalgambit.web.WebLauncher
```

#### En Linux / macOS:
```bash
# Compilar
find src/main/java -name "*.java" > sources.txt
javac -d target/classes -sourcepath src/main/java @sources.txt
rm sources.txt

# Ejecutar
java -cp "target/classes:src/main/resources" com.tacticalgambit.web.WebLauncher
```
