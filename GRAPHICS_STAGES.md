# Graphics upgrade stages

Goal: make the HD (OpenGL) renderer look better using only client-side changes.
Each stage is confirmed in-game before moving to the next.

Status: `pending` / `testing` / `confirmed` / `reverted`

## Stage 1 - Texture sharpness (confirmed)

- Anisotropic filtering on all mipmapped HD textures (`GlobalConfig.ANISOTROPIC_FILTERING`, default 16x, clamped to driver max).
- Full mip chain instead of a fixed 4 levels (`GlTexture.bind`, mipmap mode 2).
- HD textures generated at `GlobalConfig.HD_TEXTURE_SIZE` (default 256, was 128). Scroll speeds still use the original 128 base.

Round 2 fixes (after "bushes look flattened" + chunk-load lag):
- Bump map / emboss / normal-map strength and box-blur radius now scale with resolution (`Texture.resolutionScale`). At 256 they worked per pixel, so relief came out half as deep and blur half as strong.
- Generated texture pixels cached on disk in `<home>/texture_cache` (`GlTextureDiskCache`); each texture generates once ever. Bump `VERSION` there if generation output changes.

Files: `GlobalConfig.java`, `GlRenderer.java`, `GlTexture.java`, `GlTextureDiskCache.java`, `Texture.java`, `TextureOpEmboss.java`, `TextureOpNormalMap.java`, `TextureOpBoxBlur.java`, `SignLink.java` (getHomeDir)

To check: ground textures at a distance/steep camera angle, shimmer while panning, animated/scrolling textures (water, lava, portals), load hitches, texture look vs before.

## Stage 2 - Anti-aliasing (confirmed, fog work reverted)

- User keeps fog disabled (separate client edit) and wants fog untouched, so the radial/NICEST fog changes were reverted.
- In-game AA option 1 = 8x MSAA, option 2 ("4x" in menu) = 16x. User runs 16x, looks good.
- `<home>/gl_info.txt` written at GL init: GPU, MSAA samples, anisotropy.

## Stage 2b - Area load freeze (partial: stable, ~85-130ms per load remains)

Measured with `AreaLoadTimer` (`<home>/area_load_timing.log`; first run saved as `area_load_timing.before.log`): 60-420ms per load, typically 100-150ms. Phases: forced `System.gc()` ~25ms every load, readMap 20-250ms, buildScene 10-160ms.

Changes in `LoginManager.rebuildMap()`:
- Removed forced `System.gc()`.
- Replaced the 4 `client.unload()` calls (which wiped every definition/model cache, forcing a full re-decode) with `clearBuiltLocModels()` (only built/lit loc models, since buildScene merges normals into them in place) plus `client.discardUnpackedArchives()` once at the end.
- `LocTypeList.types` 64 -> 1024 (one area uses hundreds of loc types).
- Crash fix: first attempt also kept `rawModelCache` (raised to 2000) across loads and the client crashed on an area change. In HD that cache holds GlModels whose arrays are shared with scene copies, so it is cleared with the other built-model caches again (size back to 500). Client stdout/stderr now captured to `<home>/client_output.log` / `client_error.log` by the relaunch.

Findings for Stage 3: server sends real (non-zero) XTEA keys for most regions, so scenery beyond the loaded area can't be read from cache directly. Terrain is unencrypted. NPCs/players beyond ~15 tiles are never sent by the server (not client-fixable).

## Stage 2c - Background area loading (confirmed, merged to master)

Design, research and history in `ASYNC_SCENE_LOADING.md`. Areas are built in the
background (and ahead of the server where the route predicts the next rebuild),
swapped in without a freeze, the camera follows smoothly through the swap, and
the 2009scape server's skipped movement tick at every boundary is eased out on
the client (`MovementSmoother`). Diagnostics (`area_load_timing.log`,
`motion_trace.log`) are off by default: `GlobalConfig.DIAGNOSTICS`.

## Stage 3 - Extended draw distance (pending)

- Keys: most regions are encrypted, but `XteaKeyStore` (server-sent keys, plus verified keys from 2009scape's public xteas.json) can now supply them, and `AsyncSceneLoader` can build off the main thread. Plan: a display-only ring of lower-detail terrain/scenery beyond the 104x104 area.

## Stage 4 - Color banding (pending)

## Stage 5 - Shader renderer prototype (pending)

- Per-pixel lighting for world models using existing `LightingManager` lights.
