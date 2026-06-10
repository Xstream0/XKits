# XKits — Guida rapida e README completo

Plugin per Minecraft che gestisce kit personalizzati con cooldown e permessi.

**Requisiti**
- Java 17+
- Maven
- Server Paper/Spigot 1.20.4

**Build**
Esegui nella cartella del progetto:

```bash
mvn clean package
```

Il JAR risultante si trova in `target/` (es. `xkits-1.0.0.jar`). Copialo in `plugins/` sul server.

**Installazione**
1. Modifica `src/main/resources/config.yml` con le credenziali MySQL.
2. Metti il JAR in `plugins/` e avvia il server.
3. Il plugin creerà automaticamente le tabelle necessarie al primo avvio.

Esempio di `config.yml` minimo:

```yaml
mysql:
  host: localhost
  port: 3306
  database: xkits
  user: xkits
  password: "tua_password"

messages:
  prefix: '&8[&bxkit&8] &r'
```

Se non hai ancora creato il database/MySQL user, ecco un esempio (esegui in MySQL):

```sql
CREATE DATABASE IF NOT EXISTS xkits CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER 'xkits'@'localhost' IDENTIFIED BY 'tua_password';
GRANT ALL PRIVILEGES ON xkits.* TO 'xkits'@'localhost';
FLUSH PRIVILEGES;
```

**Comandi (rapido)**
- `/kit <name>` — Riscatta un kit (permesso: `xkits.use`)
- `/kitcreate <name> <cooldown_seconds> [permission]` — Salva l'inventario corrente come kit (permesso: `xkits.admin`)
- `/kitdelete <name>` — Elimina un kit (permesso: `xkits.admin`)
- `/kitrename <oldName> <newName>` — Rinomina un kit (permesso: `xkits.admin`)
- `/kitgive <player> <kit>` — Dà un kit a un giocatore (permesso: `xkits.admin`)

**Permissions**
- `xkits.admin` — Gestione completa dei kit (default: op)
- `xkits.use` — Usare i comandi dei kit (default: true)

**File importanti**
- Descriptor plugin: [src/main/resources/plugin.yml](src/main/resources/plugin.yml)
- Config: [src/main/resources/config.yml](src/main/resources/config.yml)
- Codice: `src/main/java/com/xkits/`

**Esempi d'uso**
Creare un kit dall'inventario:

```text
/kitcreate starter 86400
```

Dare un kit a un giocatore online (admin):

```text
/kitgive PlayerName starter
```