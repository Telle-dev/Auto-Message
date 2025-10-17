# AutoMessages v1.0.0  
by [Tellegram](https://github.com/Telle-dev)

Lightweight auto-message scheduler for Paper, Purpur, and Spigot.  
Includes clickable links, optional sounds, and full hex color support.  
No NMS, no dependencies.

---

## Features
- Rotating chat messages on configurable timers  
- Clickable links and `/command` buttons  
- Grouped message sets with individual intervals  
- Optional sounds per group or message  
- Supports hex (`&#RRGGBB`) and legacy `&` color codes  
- Built-in placeholders:  
  - `%online%` → current online player count  
  - `%maxplayers%` → maximum player slots  
- Safe hot-reload with `/automessages reload`

---

## Compatibility
| Component | Supported |
|------------|------------|
| Servers | Paper, Purpur, Spigot |
| Versions | 1.13 → 1.21+ |
| Java | 8+ |
| API | `plugin.yml` → `api-version: 1.13` |
| NMS / CraftBukkit | Not used |

---

## Installation
1. Download the JAR file from the [Releases](https://github.com/Telle-dev/Auto-Message/releases/tag/v.1.0.0) page.  
2. Move it into your server’s `/plugins/` folder.  
3. Restart the server or use `Plugman load AutoMessages`.  
4. Edit the config at: `plugins/AutoMessages/config.yml`
5. Reload the plugin: `/automessages reload`


---

## Command & Permission
| Command | Description | Permission |
|----------|--------------|-------------|
| `/automessages reload` | Reloads configuration | `automessages.admin` |

---

## Example Configuration
```yaml
AutoMessages:
Discord:
 enabled: true
 interval: 300       # seconds between messages
 wait: 60            # initial delay before first message
 permission: ''      # empty = all players; e.g. 'group.default'
 sound:
   enabled: true
   name: ENTITY_EXPERIENCE_ORB_PICKUP
   volume: 1.0
   pitch: 1.0
 messages:
   - '&#3498db&m                                          '
   - '&#2ecc71Join our &#3498dbDiscord [discord.gg/yourcode](https://discord.gg/yourcode)'
   - '&#3498db&m                                          '

Tips:
 enabled: true
 interval: 180
 wait: 30
 permission: ''
 sound:
   enabled: false
 messages:
   - '&7Online: &a%online%&7/&a%maxplayers%'
   - '&eUse &b/warp &eto explore!'
   ```
Formatting Notes
Type	Example	Description
Legacy color	&aHello	Standard Bukkit color codes
Hex color	&#FFAA00Hello	Supported on 1.16+
Clickable link	[Discord](https://discord.gg/example)	Opens external link
Clickable command	[Click me](/spawn)	Runs /spawn
Auto-detect	https://example.com or /rules	Automatically clickable
Why It’s Safe

No version-specific internals

Uses only Bukkit API

Cancels all scheduled tasks on disable

Supports hot reload/unload via plugin managers

Support

For bugs or feature requests, open an issue in the Issues tab
.

License

License Tellegram

Free to use, modify, and distribute with credit.

