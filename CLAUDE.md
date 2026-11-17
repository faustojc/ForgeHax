# ForgeHax

Minecraft utility mod for **Forge 1.20.1**. Branch `1.20.1` is an in-progress port from
the 1.12.2 codebase; `1.16` is the repo's main branch.

## Build

Java **17** (toolchain-enforced), Gradle 8.8, ForgeGradle 6, MinecraftForge 47.4.10,
**official (Mojang) mappings**, SpongePowered Mixin.

```
./gradlew.bat compileJava     # fastest feedback loop
./gradlew.bat test jar        # the verification bar for a change
```

Do **not** run the Gradle `build` task — it finalizes with the deployment-side-effectful
`copyJar` task.

`compileJava` is configured with `-Xmaxerrs 5000` so the whole error surface is visible
during the port. Prefix shell commands with `rtk` when it is installed (see your user-level `RTK.md`).

## Architecture

- `Helper` / `Globals` — static accessors for `Minecraft`, the local player, the level,
  the mod manager, and chat printing. Almost every class extends or statically imports
  `Helper`.
- `util/mod/` — module base classes: `BaseMod`, `ToggleMod`, `CommandMod`, `HudMod`,
  `ServiceMod`, plus `Category`. `loader/ModManager` discovers modules by scanning the
  classpath for `@RegisterMod`; there is no hand-maintained registry, so adding a module
  file is enough to register it.
- `util/command/` — the CLI: `Command`, `CommandBuilder`, `Setting<T>`, `Options`,
  `CommandStub`. Settings are the single source of truth for module configuration and
  are serialized through `ForgeHaxProperties`.
- `util/draw/` — rendering: `SurfaceBuilder`, `SurfaceHelper`, `RenderUtils`,
  `GeometryTessellator`, `font/`. All of it sits on `RenderSystem` + `PoseStack` +
  `BufferBuilder`/`BufferUploader`. **Never reintroduce `GlStateManager`.**
- `mods/` — the modules themselves. `mods/services/` are always-on `ServiceMod`s that
  translate Forge/vanilla events into ForgeHax events; `mods/managers/` hold shared
  cross-module state; `mods/commands/` are CLI-only commands.
- `asm/ForgeHaxHooks` — static hook methods invoked from mixin injection points. It fires
  the event classes in `asm/events/`. Mixins should call into `ForgeHaxHooks`, not
  contain module logic.
- `mixin/` — SpongePowered mixins, registered in `src/main/resources/forgehax.mixins.json`
  (package `com.matt.forgehax.mixin`, `JAVA_17`, client-only, refmap
  `forgehax.refmap.json`) and declared to Forge by the `[[mixins]]` block in
  `META-INF/mods.toml`. A mixin missing from either does not load. `mixin/accessor/`
  holds the `@Accessor`/`@Invoker` interfaces; the rest are injection mixins.
  The `org.spongepowered:mixin:...:processor` annotation processor writes the refmap at
  compile time — without it every target resolves to nothing under production names.

## 1.20.1 port state

`build.gradle` gates the compiled source set while the port lands:

- Excluded: `asm/patches/**`, `asm/utils/transforming/**`, `asm/reflection/**`,
  `util/schematica/**`, and every top-level `mods/*.java` **not** named in the
  `coreMods` list.
- Re-enabling a module = add its name to `coreMods` and fix what the compiler reports.
  Excluded files are kept on disk deliberately: they are the 1.12.2 reference for
  whatever replaces them. Do not delete them wholesale.

Mixins currently in place (`forgehax.mixins.json`): `MixinMinecraft`, `MixinLocalPlayer`,
`MixinMultiPlayerGameMode`, `MixinConnection`, `MixinBlockCollisions`, `MixinEntity`,
plus the `accessor/` interfaces for `Minecraft`, `OptionInstance`,
`DisconnectedScreen` and `BlockItem`. That is deliberately narrower than the old patch set:
a hook whose only listeners live in an excluded module is not worth a live injection. Write
the mixin when the module comes back — `asm/patches/**` still holds the 1.12.2 original, and
`ForgeHaxHooks` already has the ported hook method waiting.

Confirmed to have no 1.20.1 equivalent (do not invent one): `ChunkRenderContainer`,
`World.checkLightFor`, `BufferBuilder.putColorMultiplier`, `Block.canRenderInLayer`
(now `ItemBlockRenderTypes.getChunkRenderType`), `ChunkRenderWorker`,
`ForgeModContainer.forgeLightPipelineEnabled`, `Block.canSilkHarvest`.

Gone for good, do not resurrect:

- The ModLauncher coremod (`asm/ForgeHaxTransformer` +
  `META-INF/services/cpw.mods.modlauncher.api.ITransformationService`). Bytecode
  injection is Mixin now.
- The hand-rolled SRG/notch remapping layer (`TypesHook`, `ObfuscatedStateMapper`,
  `FileDumper`). Official mappings give real names in source and in dev runtime;
  production runs on SRG and the mixin refmap bridges it. Nothing else needs remapping.
- Schematica integration — `com.github.lunatrius.schematica` has no 1.20.1 release.

## Conventions

- Prefer a Forge 1.20.1 event over a mixin when one fires at the same point. Mixin only
  what Forge does not expose, and keep the injection thin: call `ForgeHaxHooks`.
- Use `@Accessor`/`@Invoker` mixins for private vanilla state. The old reflection helper
  (`asm/reflection/FastReflection`) is deleted and must not come back.
- Where 1.20.1 has no equivalent for a 1.12.2 behavior, leave the code compiling with a
  `// TODO(1.20.1):` naming what is missing. Do not silently no-op something that does
  have an equivalent.
- No new dependencies. No abstraction with a single implementation.
- Read names, descriptions, commands, settings and keybinds from the existing module
  APIs. Never hard-code module names or duplicate configuration state.
- HUD drawing hangs off `RenderGuiEvent.Post`, which fires once a frame.
  `RenderGuiOverlayEvent.Post` fires once per registered overlay (~14x a frame) and must
  not be used for whole-HUD work.

## Keybinds

- A bind is one int on `CommandStub`. `CommandStub.NO_KEYBIND` means the command takes no
  bind at all; `InputConstants.UNKNOWN` (-1) means bindable but unbound. They are not the
  same value and must not be conflated - collapsing them silently drops every bind.
- Negative codes below -1 are mouse buttons in the 1.12.2 encoding (-100 left, -99 right,
  -98 middle). Resolve every stored code through `BindingHelper.getKey(int)`; handing a
  negative code to `InputConstants.Type.KEYSYM` makes GLFW reject the key at render time.
- Saved binds carry a `"codes":"glfw"` marker. Configs without one hold LWJGL2 scancodes
  and are translated once by `LegacyKeyCodes` and rewritten. The translation is not
  idempotent, so never run it on a value that already has the marker.
- Vanilla only updates `KeyMapping` pressed state while no screen is open, so
  `BindEventService` reads the key straight off `InputEvent.Key`/`InputEvent.MouseButton`.
  Do not go back to `consumeClick()`/`isDown()` - the GUI could then never close itself.

## GUI rule

Scope: the ForgeHax GUI replacement and files directly required by it.

### Navigation

- One centered, non-draggable modal with a dimmed full-screen backdrop.
- Render individual visible modules in one sidebar. Do not render, group, filter, or lay
  out modules by category.
- Keep `Category` and module constructors unchanged; categories remain backend metadata.
- Enumerate `Helper.getModManager().getMods()`, exclude `mod.isHidden()`, and sort module
  names case-insensitively.
- Clicking a module row selects it. Only the dedicated enabled control may toggle it.

### Modal and visual behavior

- Preserve `ClickGui` as the screen type and the existing GUI service/keybind entry point.
- Keep `isPauseScreen()` false. The backdrop consumes clicks and does not close the GUI.
- Escape, the active GUI keybind, and the visible close control close the modal when no
  editor is capturing input.
- No animation, transitions, dragging, gradients, glow, or external GUI dependency.
- Use the Minecraft font (`Minecraft.getInstance().font`) and the existing rendering
  helpers, with these tokens: backdrop `#080A0D`, modal `#12171D`, surface `#19212A`,
  divider `#2B3642`, text `#EDF1F5`, accent `#D9773F`.
- Recalculate modal bounds from the current scaled window
  (`Minecraft.getInstance().getWindow().getGuiScaledWidth()/getGuiScaledHeight()`) and
  restore render/scissor state after clipped drawing.

### Settings and keybinds

- Discover descendant `Setting<?>` objects from the selected module, sort them
  deterministically, and show the module's `enabled` setting only through the dedicated
  header switch.
- Boolean settings use switches; enums use explicit choices; fully bounded numbers use a
  slider plus value field; other numbers, strings, characters and custom converter types
  use validated text fields.
- Use `Setting` mutation APIs so callbacks and bounds stay authoritative. Reject parse
  failures without writing `null`, reread the source value after every attempted change,
  and serialize accepted changes.
- Reset through `Setting.reset()`. Toggle modules through `enable()`/`disable()` and
  persist the existing enabled setting.
- Edit binds through the existing `CommandStub`/`BindingHelper` APIs (`KeyMapping` +
  `InputConstants`, GLFW key codes). Do not create a second bind store.
- Keep `Options` collections and arbitrary action commands in the CLI. Show one passive
  CLI-only notice when a module has unsupported commands.

### Scope

- Keep the CLI and all module/category/command public behavior unchanged.
- Remove legacy GUI files only when they become unreferenced. Do not refactor unrelated
  code, and preserve pre-existing user changes outside the GUI scope.
