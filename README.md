# SkyRandomizer
> A Minecraft plugin which recreates the BastiGHG Challenge "30 SEKUNDEN = NEUES ITEM" from [Youtube](https://www.youtube.com/watch?v=X1rXPrWWMGw).

## Installation
1. Clone the repository
2. Build the project with `./gradlew build`
3. Copy the generated JAR file from `build/libs` to your Minecraft server's `plugins` directory
4. Set the world generator in you `bukkit.yml`:
```yaml
worlds:
  world:
    generator: SkyRandomizer
```
5. Start your server