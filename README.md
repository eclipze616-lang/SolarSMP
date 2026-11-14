# Solar SMP (Paper 1.21.11)

Build: `mvn clean package` (Java 21) -> `target/SolarSMP.jar` -> drop into `plugins/`.

Commands
- `/solarsmp ability <1|2>`  cast your planet's ability
- `/solarsmp info`           your planet, energy, abilities
- `/solarsmp recipes`        read-only recipe GUI (Energy Core + Planet Reroller)
- `/setsolar <player> <planet>`  (op)
- `/setenergy <player> <0-5>`    (op)
- `/solarsmp give <player> <core|reroller> [amount]` (op, for testing)

Power scale: Energy 0 = 25% power ... Energy 5 = 100%. Cooldowns shrink as energy rises.
Recipes live in SolarItems.java (CORE_GRID / REROLL_GRID) - edit to rebalance.

## Config GUI (op only)
`/solarsmp config` opens a GUI: per-ability cooldown (clock) and damage multiplier (sword),
plus global cooldown/damage multipliers, a toggle for losing energy on death, and a reset button.
Left-click = up, right-click = down, shift = bigger steps. Saved to plugins/SolarSMP/config.yml instantly.

## Building without installing anything
Push this folder to a GitHub repo -> Actions tab -> "Build SolarSMP" -> download the SolarSMP-jar artifact.
