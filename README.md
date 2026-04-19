# Container Preview

**License notice:** this project is `All Rights Reserved` unless a separate written permission is provided by the copyright holder. Do not reupload, redistribute, or reuse the source, assets, or builds outside the permissions granted to you.

Multiloader Minecraft mod for `1.21.11` that previews the contents of container items directly in tooltips. The shared gameplay, rendering, config, and provider logic live in `common`; loader projects only handle bootstrap, platform glue, and loader-specific registration.

## What It Does

- Shows container contents in tooltip previews.
- Supports nested previews for supported items.
- Supports detail view, lock view, preview hold behavior, and configurable layout.
- Keeps the shared preview pipeline in `common` so the loader layers stay thin.
- Exposes a small extension API so other mods can add their own container providers.

## Supported Loaders

- Fabric
- NeoForge
- Forge

## Project Layout

- `common/` shared API, config, rendering, mixins, network payloads, and container providers.
- `fabric/` Fabric bootstrap, client hooks, networking, and Mod Menu integration.
- `neoforge/` NeoForge bootstrap, event bus registration, networking, and config screen integration.
- `forge/` Forge bootstrap, event bus registration, networking, and config screen integration.
- `buildSrc/` shared Gradle convention plugins.

## Build Requirements

- Java `21`
- Minecraft `1.21.11`
- Gradle wrapper included in repo

## Build

Build everything and copy the final jars into `build/1.21.11`:

```bash
./gradlew packageMods
```

That produces:

- `Container Preview-v1.0.0-Fabric-1.21.11.jar`
- `Container Preview-v1.0.0-NeoForge-1.21.11.jar`
- `Container Preview-v1.0.0-Forge-1.21.11.jar`

## Run

Fabric:

```bash
./gradlew :fabric:runClient
```

NeoForge:

```bash
./gradlew :neoforge:runClient
```

Forge:

```bash
./gradlew :forge:runClient
```

If you are testing config UI support on Fabric, YACL is wired into the dev runtime. Loader screens fall back automatically if YACL is unavailable.

## Configuration

The shared config file is stored per-loader in the normal config directory as `peekinside.json`.

Important user-facing settings:

- `Hold to Preview`
- `Enable Detailed Preview`
- `Enable Preview Locking`
- `Compress Similar Items`
- `Nested Preview`
- `Show Seller Metadata`
- `Show Empty Slot Message`

Key bindings are exposed in controls and drive runtime behavior directly:

- Preview key: default `Left Shift`
- Detail key: default `Left Control`
- Lock key: default `Left Alt`

## Extension API

Other mods can add preview support without editing the core renderer by registering a `ContainerProvider`.

### Key Types

- `dev.peekinside.api.ContainerProvider`
- `dev.peekinside.api.ContainerProviderRegistry`
- `dev.peekinside.api.PeekInsideEntrypoint`
- `dev.peekinside.api.PreviewRequest`
- `dev.peekinside.api.PreviewResult`

### Provider Contract

Implement `ContainerProvider` and return a `PreviewResult` when your item should show a custom container preview.

```java
public final class MyProvider implements ContainerProvider {
    @Override
    public @Nullable PreviewResult provide(PreviewRequest request) {
        ItemStack stack = request.stack();
        if (!isMyItem(stack)) {
            return null;
        }

        List<ItemStack> slots = List.of(
            ItemStack.EMPTY,
            ItemStack.EMPTY
        );

        return new PreviewResult(slots, 2, 1, Component.literal("My Container"));
    }
}
```

Register it from the shared entrypoint:

```java
public final class MyEntrypoint implements PeekInsideEntrypoint {
    @Override
    public void register(ContainerProviderRegistry registry) {
        registry.register(
            Identifier.fromNamespaceAndPath("my_mod", "my_container"),
            1000,
            new MyProvider()
        );
    }
}
```

Lower priority values run earlier. Providers should return `null` when they do not handle the request.

### `PreviewRequest`

Contains:

- `ItemStack stack`
- `@Nullable LocalPlayer player`
- `@Nullable Screen screen`
- `boolean fullMode`

Use `fullMode` when you want to distinguish between compact tooltip rendering and full detail view.

### `PreviewResult`

Contains:

- `List<ItemStack> slots`
- `int columns`
- `int rows`
- `@Nullable Component label`

`slots.size()` must equal `columns * rows`. Use `ItemStack.EMPTY` for blank cells.

## Loader Responsibilities

Keep this split clean:

- `common` contains the preview logic, config, providers, rendering, and mixins.
- loader modules contain registration only.
- do not move platform-independent preview logic into loader code unless the API truly requires it.

## Development Notes

- The project uses official mappings and Parchment.
- The shared renderer lives in `common` so tooltip behavior stays consistent across loaders.
- The repo has both YACL and fallback config screens; runtime chooses the best available option.
- The build outputs are intentionally renamed and collected in one place for release packaging.

## Customizing This Mod

If you are forking this project for your own mod, change these first:

- `gradle.properties`
- `fabric/src/main/resources/fabric.mod.json`
- `forge/src/main/resources/META-INF/mods.toml`
- `neoforge/src/main/resources/META-INF/neoforge.mods.toml`
- loader entrypoint classes in `fabric/`, `forge/`, and `neoforge/`

Then update any branding assets, translations, and package names to match your mod.

## License

This repository is published under `ARR` / `All Rights Reserved`. The source, binaries, and assets may not be reuploaded or reused without explicit permission from the copyright holder.
