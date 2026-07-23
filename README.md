# MZMOD Advance Menu v4.0

A Java Swing desktop application for merging multiple J2ME JAR files into a single output JAR, with bytecode relocation and a modern dark-mode UI.

## Features

- **JAR Merging** - Merge multiple copies of a J2ME JAR into one output file
- **Bytecode Processing** - Automatic class relocation and reference rewriting using ASM 9.x
- **Library Shim Remapping** - J2ME platform classes (MIDlet, Canvas, Display, etc.) redirected to custom `lib/` shims
- **Embedded JAR Support** - Bundle an embedded library JAR into the merged output
- **Custom Icon** - Set a custom PNG icon for the merged JAR
- **Modern UI** - Dark mode interface with FlatLaf, rounded corners, and keyboard shortcuts
- **Persistent Settings** - User preferences saved to `mzmod-settings.properties`

## Requirements

- Java 8 or later
- Maven 3.x

## Build

```bash
mvn clean package
```

The shaded JAR will be generated at `target/mzmod-advance-menu.jar`.

## Run

```bash
java -jar target/mzmod-advance-menu.jar
```

## Usage

1. **JAR Files Tab** - Select a JAR file and set the number of copies to merge
2. **Settings Tab** - Configure program name, display name, vendor, icon, and output path
3. **Merge Tab** - Click "Start Merge" or press `F5` to begin

### Keyboard Shortcuts

| Shortcut | Action |
|----------|--------|
| `Ctrl+O` | Select JAR file |
| `F5` | Start merge |
| `Ctrl+Shift+O` | Open output folder |

## Project Structure

```
src/main/java/mzmod/
  MzModSwingApp.java          # Entry point
  merger/
    JarMerger.java             # Core merge logic
    BytecodeProcessor.java     # ASM bytecode rewriting
  ui/
    MainFrame.java             # Main Swing window
    UIHelpers.java             # Styled component factories
    Theme.java                 # Dark mode design tokens
    UserSettings.java          # Settings persistence
```

## How It Works

1. Select a J2ME JAR file as the source
2. The merger creates N copies (configurable), each relocated under a unique prefix (`a/`, `b/`, etc.) to avoid class name collisions
3. J2ME platform classes (`javax/microedition/*`) are redirected to `lib/` shim classes
4. Method names prone to collisions (`keyPressed`, `paint`, `forName`, etc.) are rewritten
5. A `Static.class` registration file is generated to track all modified classes
6. An `embedded.jar` resource is merged verbatim into the output

## License

See repository for license details.

## Author

- **[Duc Huy]** - Developer
