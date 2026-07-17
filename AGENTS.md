# AGENTS.md - MZMOD Advance Menu

## Project Overview
Java ME (J2ME/MIDP 2.0) application - a menu/mod system for mobile Java apps. Version 3.6.6.

## Build Commands
```bash
# Build JAR (default goal)
mvn package

# Clean build
mvn clean package

# Compile only (no tests in this project)
mvn compile
```

## Project Structure
```
src/main/java/mzmod/
├── app/         # Main MIDlet entry point (MzModApp) and JarInfo
├── zip/         # ZIP file handling utilities
├── compress/    # DEFLATE compression (Inflater/Deflater/Huffman)
├── ui/          # Canvas-based UI components (CanvasList)
└── bytecode/    # Java class file manipulation
```

## Key Technical Details

### Target Platform
- CLDC 1.1 / MIDP 2.0
- Java source/target: 1.8
- Dependencies: MicroEmulator libraries (provided scope - not bundled)

### Manifest Configuration
- Entry point: `classes.class_a` (mapped to `mzmod.app.MzModApp`)
- Resources stored as base64-encoded strings in MANIFEST.MF
- JAR files in `src/main/resources/` are runtime dependencies

### Architecture Notes
- No test framework configured - this is a legacy J2ME project
- Bytecode manipulation classes modify class files at runtime
- UI uses Canvas-based rendering (no LCDUI Forms)
- Compression utilities implement DEFLATE from scratch (no java.util.zip)
