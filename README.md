# iQOO Code

**Version:** 1.0  
**Platform:** Android (Jetpack Compose)  
**Package:** `com.example.iqoo_code`

---

## Features

### Terminal v0.1

A fully functional sandboxed terminal inside the iQOO Code Android app.

#### Terminal UI
- Full-screen dark/black terminal appearance with monospace font
- Scrollable output history with auto-scroll to newest output
- Input field with prompt (`~/ $`) at the bottom
- Command history navigation (up / down buttons)
- Enter / IME action submits commands
- Keyboard stays open after command execution
- Visually distinguishable input, output, error, and system lines

#### Built-in Commands
| Command | Description |
|---------|-------------|
| `help` | Show all available commands |
| `clear` | Clear terminal output |
| `pwd` | Print current working directory |
| `echo <text>` | Print arguments (supports quoted strings) |
| `cd [path]` | Change directory (`cd`, `cd ..`, `cd .`, relative, absolute) |
| `ls [path]` | List files and directories |
| `cat <file>` | Display text file contents |
| `mkdir <dir>` | Create a new directory |
| `rm [-r] <path>` | Remove files or directories |
| `date` | Print current date and time |
| `whoami` | Print current user (`iQOOUser`) |
| `exit` | Reset terminal session |

#### External Commands
Commands not in the built-in registry are executed via `/system/bin/sh -c` on a background IO dispatcher. Stdout, stderr, and exit codes are captured and displayed. No root, no privilege escalation, no sandbox bypass.

#### Filesystem Safety
- All operations are confined to the app's private files directory
- Sandbox enforcement prevents traversal outside the permitted workspace
- No access to other applications' private data

#### Architecture
```
Compose UI (TerminalScreen)
    ↓
TerminalViewModel
    ↓
TerminalEngine
    ↓
TerminalEnvironment
    ↓
Built-in Commands + ExternalCommandExecutor
```

UI logic is fully separated from terminal/business logic, keeping the door open for a future native C++ runtime engine via JNI.

---

## Building

```bash
./gradlew assembleDebug
```

The debug APK is generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## Tech Stack

- Kotlin
- Jetpack Compose (Material 3)
- Gradle 9.5.0
- AGP 9.3.1
- Kotlin 2.2.10

---

## License

iQOO Code — Internal project.
