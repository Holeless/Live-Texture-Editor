# Texture Editor (NeoForge 1.21.1)

NeoForge 1.21.1 port of **Texture Editor** by [zeeesea](https://github.com/zeeesea/TextureEditorMod)
(original: Fabric 1.21, [Modrinth](https://modrinth.com/mod/livetextureeditor)).

Edit block, item, entity and GUI textures directly in-game. Draw with pencil, brush, fill and line tools,
export as resource packs, and see changes live - no restart required. Client-side only.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.x
- Java 21

No other mods are needed. The Fabric version's Mod Menu integration is replaced by the standard
**Config** button in NeoForge's mod list.

## Default keys

| Key | Action |
|-----|--------|
| `J` | Toggle editor mode |
| `R` | Open editor (held block / item / mob, or looked-at block / mob) |
| `Z` (hold) | Preview original textures |

All keys can be rebound in the normal controls menu. Tool shortcuts inside the editor are configured in the mod's
settings screen.

## Building

```sh
./gradlew build          # jar ends up in build/libs/
./gradlew runClient      # start a dev client
```

## Porting notes

This port was produced from the compiled Fabric jar (no source was available), so the code is decompiled output
translated to Mojang mappings and NeoForge APIs. Things that changed versus the Fabric build:

- Loader entry points: `ModInitializer` / `ClientModInitializer` / Fabric API events replaced by one `@Mod` class
  (`TextureEditor`) using NeoForge's `RegisterKeyMappingsEvent`, `RegisterClientReloadListenersEvent` and
  `ClientTickEvent.Post`.
- `fabric.mod.json` -> `META-INF/neoforge.mods.toml`; mixin config merged into one client-side config
  (only the `SpriteContents` accessor is needed).
- Example/empty mixins and the empty data generator were dropped.
- `ItemTextureExtractor`: Fabric API's `ModelManager.getModel(ResourceLocation)` does not exist in vanilla/NeoForge;
  it now uses `ModelResourceLocation.inventory(itemId)`.
- `BlockFilter`: the list of blocks that are not editable as plain cubes was reconstructed from the equivalent
  vanilla block classes. Behaviour may differ slightly for some blocks.
- Reflection based mob-variant detection now also recognises Mojang's `Variant` enum names (horses).

If something is off, open an issue - this port has not been run in-game by its author yet.

## License

All Rights Reserved, Copyright (c) 2026 zeeesea. See [LICENSE](LICENSE). This port is a derivative work; it may only
be published or distributed with the original author's written permission.
