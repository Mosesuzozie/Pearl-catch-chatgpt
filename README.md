# PearlCatch & Auto Swapper (Fabric 1.21.11, Java 21)

Client-side mod. Settings: Mod Menu > PearlCatch & Auto Swapper (also `config/pearlcatch.json`).

## Auto Swapper
While airborne it picks the nearest player in range, estimates swaps = distance / "blocks per swap"
(capped by max swaps and, optionally, by how long you stay in the air) and presses G that many times.
The press goes through Minecraft's keybinding system, so any client/mod action bound to G fires.
- Mode FAST / SLOW: ticks between presses (fast 2, slow 6 by default) and aim speed.
- Smoothness: how gradually the camera turns. 0% snaps, 100% is very gradual.
- Auto Move: smoothly turns the camera to look at the target.
- Activation: ALWAYS (as soon as you're falling) or KEY_HELD (hold the "Auto Swapper trigger" key, unbound by default).

## Pearl Catch (up) / Auto Side Pearl Catch
Throw an ender pearl and the mod tracks it. Throws at or above the "up-throw pitch" (60 degrees) use the
Pearl settings, everything else uses the Side Pearl settings. Each has its own on/off, FAST/SLOW,
smoothness and Auto Move. Auto Move turns the camera toward the pearl (up) or its predicted landing
spot (side). The action bar shows the predicted landing time.

## Build
GitHub: push the repo, the included workflow (`.github/workflows/build.yml`) builds the jar and uploads it as an artifact.
Local: Java 21 + Gradle 9.2, run `gradle build`, jar is in `build/libs/`.
If Gradle can't find a version, bump it in `gradle.properties` (versions come from Fabric's 1.21.11 pages).
