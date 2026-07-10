```markdown
# MapWriter

A lightweight minimap mod for Minecraft 1.12.2.

This is a community-maintained continuation of the original MapWriter 2.

## License
MIT – see the [LICENSE](LICENSE) file.

## Features
- Small / large / full‑screen minimap
- Waypoints with colour coding
- Player trail and death markers
- Underground mapping mode
- Optional mob overlay
- Minimal performance impact

## Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/moos233/mapwriter.git
   cd mapwriter
   ```

2. Build with Gradle (wrapper included):
   ```bash
   ./gradlew build
   ```
   The built JAR will be in `build/libs/`.

## Import into IDE

- **IntelliJ IDEA**: Open the project folder (select the `build.gradle` file).
- **Eclipse**: Run `./gradlew eclipse` then import the project.

## Run the Client

- In your IDE, execute the `runClient` Gradle task.
- Or from the command line: `./gradlew runClient`

## Configuration

After the first run, a configuration file is created at `./config/mapwriter.cfg`.  
You can adjust map size, opacity, key bindings, zoom levels and performance options.

## Contributing

Issues and pull requests are welcome on the [GitHub repository](https://github.com/moos233/mapwriter).

## Credits

- **Original author**: Mapwriter (Liam Davey)
- **Previous maintainer**: Vectron
- **Current maintainer**: moos233
- **Contributors**: Chrixian, ProfMobius, taelnia, LoneStar144, jk-5

---

Enjoy!
```