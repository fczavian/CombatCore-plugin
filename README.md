# CombatCore ⚔️
<!-- test change for git push -->

![Java Version](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=java)
![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20%2B-brightgreen?style=for-the-badge&logo=minecraft)
![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot-blue?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**CombatCore** is a high-performance, feature-packed combat tagging and anti-combat-logging plugin designed for modern Minecraft Paper/Spigot servers (1.20+). Built with modularity and extensibility in mind, CombatCore features multi-cause combat detection, customizable HUD elements, advanced punishment mechanics, progressive penalties for repeat offenders, multi-backend database support, zero-dependency safezone integrations, and real-time Discord webhook alerts.

---

## 🌟 Key Features

### ⚔️ Advanced Combat Tagging & 1v1 Protection
- **Multi-Cause Combat Triggers**: Supports independent toggles for Melee, Bow/Crossbow, TNT, End Crystals, Respawn Anchors, and Thrown Tridents.
- **Tag Extension**: Resets/extends tag duration dynamically when players hit or get hit during combat.
- **Locked Combat Mode (1v1 Isolation)**: Enforces strict 1v1 duels—prevents third-party interference by blocking outside damage while a pair is actively tagged.

### 🚫 Anti-Escape & Command Restrictions
- **Blacklist & Whitelist Modes**: Restrict commands like `/spawn`, `/home`, `/warp`, `/tpa` during active combat.
- **Regex Pattern Matching**: Block complex command variants and sub-commands using standard Regular Expressions (`^/spawn.*`).
- **Tab-Completion Blocker**: Automatically hides blacklisted commands from tab-completion while combat-tagged.

### 🛡️ Safezone & Region Protection Integration
- **Zero-Dependency Reflection Integration**: Fully supports **WorldGuard (v6 & v7)**, **Towny**, and **Factions** without hard plugin dependencies.
- **Integration Modes**:
  - `deny`: Blocks combat-tagged players from entering non-PvP safezones with knockback physics and a visual temporary Red Stained Glass wall.
  - `extend`: Allows safezone entry while extending the combat tag duration to keep players vulnerable.
  - `off`: Disables safezone checks completely.

### ⚖️ Comprehensive Combat-Log Punishments
Punish players who log out during active combat with fine-grained controls:
- **Instant Death & Inventory Wipe**: Option to instantly kill the combat logger and/or wipe their inventory.
- **Lightning Strikes**: Decorative/warning visual lightning at logout location.
- **Vault Fines & Economy Deductions**: Deduct money directly from Vault-supported economy accounts.
- **Bans & Mutes**: Temporary or permanent bans and mutes applied automatically on next join.
- **Jail & Command Execution**: Send loggers to Essentials/Vault jails or run custom console commands upon logging.
- **Potion Effects & Stat Reductions**: Apply debuffs (e.g., Slowness, Weakness) or RPG stat penalties upon login.
- **Progressive Punishment System**: Multiplies penalties automatically for repeat offenders after passing configurable thresholds.

### 📊 Real-Time Display & Visual HUDs
- **Action Bar HUD**: Configurable, real-time action bar timer with custom text templates.
- **Boss Bar Timer**: Animated Boss Bar indicating remaining combat time, transitioning dynamically from red to green when combat expires.
- **PlaceholderAPI Support**: Soft-dependency integration for custom placeholders.

### 💾 Multi-Backend Storage System
Supports 4 seamless data storage backends for tracking player combat stats, kill credits, bounties, and offense records:
- **SQLite**: Local file-based database (`combatcore.db`).
- **MySQL / MariaDB**: Remote relational database for network/bungeecord setups.
- **MongoDB**: Remote NoSQL document database via URI string.
- **Local JSON Storage**: Lightweight fallback JSON files (`combat_stats.json`, `combat_offenses.json`).

### 🔔 Discord Webhook Alerts
- Send automated, real-time Discord notifications for combat logging events, kill credits, and applied punishments directly to your Discord staff channels.

---

## 📜 Commands & Permissions

### Commands
`Usage: /combatcore <subcommand>` (Alias: `/cc`)

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/combatcore reload` | Reloads configuration files | `combatcore.admin` |
| `/combatcore debug` | Toggles internal debug logging | `combatcore.debug` |
| `/combatcore toggle [player]` | Manually toggles combat tag for a player | `combatcore.admin` |
| `/combatcore bounty <add/remove/list>` | Manages player bounties | `combatcore.admin` |
| `/combatcore stats [player]` | Views player combat stats and offense history | `combatcore.stats.view` |

### Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `combatcore.admin` | Access to all administrative commands | `op` |
| `combatcore.debug` | Access to debug toggles | `op` |
| `combatcore.commandblock.bypass` | Bypass command restrictions while combat-tagged | `op` |
| `combatcore.punishment.override` | Immune to combat logging punishments | `op` |
| `combatcore.safezone.bypass` | Bypass safezone entry restrictions while tagged | `op` |
| `combatcore.stats.view` | View combat statistics and offense records | `op` |

---

## 🛠️ Building & Installation

### Prerequisites
- **JDK 17** or higher
- **Apache Maven 3.8+**
- Server running **Paper 1.20+** (or Paper forks like Purpur)

### Building from Source

```bash
# Clone repository
git clone https://github.com/fczavian/CombatCore-plugin.git
cd CombatCore-plugin

# Build project with Maven
mvn clean package
```

The compiled JAR file will be available in the `target/` directory:
`target/CombatCore-1.0.0.jar`

### Installation
1. Drop `CombatCore-1.0.0.jar` into your server's `plugins/` directory.
2. (Optional) Install **PlaceholderAPI**, **WorldGuard**, **Towny**, or **Factions** for region & placeholder features.
3. Restart your server to generate default configuration files in `plugins/CombatCore/`.
4. Configure `config.yml` to fit your server's needs and reload with `/combatcore reload`.

---

## 📄 License
This project is open-source software licensed under the MIT License.
