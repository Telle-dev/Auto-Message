# AutoMessages v1.0.0 by Tellegram

Lightweight auto-message scheduler with clickable links and optional sounds. No NMS. No external dependencies.

## Features
- Rotating chat messages on a timer
- Clickable links and command buttons
- Per-group intervals, optional permission gating
- Optional sound per group/message
- Hex colors (1.16+) and legacy `&` codes
- Placeholders: `%online%`, `%maxplayers%`
- Safe hot-reload: `/automessages reload`

## Compatibility
- Servers: `Paper`, `Purpur`, `Spigot`
- Versions: `1.13` → `1.21+`
- Java: `8+`
- `plugin.yml`: `api-version: 1.13` for forward-compatible loading
- Public Bukkit/Paper API only (no `NMS`/`CraftBukkit`)

## Install
1. Download the jar from **Releases** of this repository.
2. Put it into your server’s `plugins/` folder.
3. Use `Plugman load AutoMessages` to load the plugin.
4. Or Restart/Start Your server.
5. Edit `plugins/AutoMessages/config.yml` as needed, then run `/automessages reload`.

```bash
Command & Permission
```/automessages reload — reloads configuration```
Permission: automessages.admin

Configuration (example)
AutoMessages:
  Discord:
    enabled: true
    interval: 300       # seconds between messages
    wait: 60            # initial delay in seconds
    permission: ''      # empty = all players; or e.g. 'group.default'
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
Formatting notes
Legacy colors: &a, &e, etc.

Hex: &#RRGGBB on 1.16+ (ignored gracefully on older versions).

Clickable: [text](https://url) opens links, [text](/command) runs commands.

Backticks: https://url or /command are also detected as clickable.

Why it’s safe
No version-specific server internals

Cancels scheduled tasks on disable

Works with plugin managers for hot-load/unload

Support
Open an issue in this repository’s Issues tab.