# libs

Mod jars this mod compiles against (compile only; the game supplies the real mods at runtime).
They are not committed. Copy these three files here from a 1.20.1 Forge `mods` folder:

| File | Mod |
|---|---|
| `ad_astra-forge-1.20.1-1.15.21.jar` | Ad Astra |
| `botarium-forge-1.20.1-2.3.4.jar` | Botarium (Ad Astra's library) |
| `resourcefullib-forge-1.20.1-2.1.29.jar` | Resourceful Lib (Ad Astra's library) |

If you use other versions, change the three `*_version` lines in `gradle.properties` to match the file names.
