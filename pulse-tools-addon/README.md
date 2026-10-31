# Pulse Tools (Meteor Client addon)

Modules: swing-speed, chunk-finder, spawner-finder (category "Pulse Tools").

## Build
1. Clone https://github.com/MeteorDevelopment/meteor-addon-template (use the branch/tag matching your Minecraft version).
2. Delete its example package (src/main/java/com/example/addon), its mixins json and assets folder if present.
3. Copy this project's `src/` folder over the template's `src/` (overwrite fabric.mod.json).
4. If the template's build.gradle(.kts) references the old mixin json / package, update it to pulse-tools.
5. Run `./gradlew build` -> jar is in build/libs/. Put it in .minecraft/mods next to Meteor Client.

Do not load it together with the old Pulse jar (same module names).
