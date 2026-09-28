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

## Stage 3 - Extended draw distance (confirmed)

- 3a (done): `FarTerrain` draws level-0 ground for `GlobalConfig.FAR_TERRAIN_TILES` (64) tiles beyond the view distance and the loaded 104x104 area. Decoded from the unencrypted map files like `SceneGraph.readTile`, underlay colours blended 11x11 like `buildScene`, overlay colours as `buildGlShapedTiles` (base colour through `getTexturedColor`, times the texture's real average colour from its pixels; overlays without a base colour aren't drawn). Meshed per 64x64 region (one per frame, cached), drawn in 8x8 zones at depth layer 301.5 behind the real ground; zones the real scene fully covers are skipped. Far clip plane extended by the same distance. Off in instances.
- 3b (done): far ground textured like the real ground (per-texture batches; water/special materials stay flat average colours). `GlobalConfig.FAR_TERRAIN_TEXTURES`.
- 3c (done): `FarScenery` - level-0 walls, centrepieces and roofs merged per region/zone, flat-shaded; face colour through `getTexturedColor` times the texture's real average (most HD model textures are greyscale detail maps). Centrepieces under `FAR_SCENERY_MIN_HEIGHT` (96) skipped. Rebuilt up to 8x while textures download. Keys: every candidate (server-sent, bundled, none) verified with `Js5.checkKey` - the server sends wrong keys for some regions. Measured: 48 fps / 1.0 GB GPU zoomed out at Falador (was 34 fps / 1.3 GB before the clutter cut).
- 3d (done):
  - Far scenery is built like the real scene builds it: a `GlModel` per loc (same raw model, rotation and lighting as `LocType.getStaticEntity`), exported with `GlModel.exportTriangles` into per-texture, per-zone meshes. Textured and smooth-shaded like nearby scenery; diagonal centrepieces (shape 11) and models still downloading (rebuilt up to 8x) now appear.
  - Performance: frustum culling per 8x8 zone (`readFrustum`/`inFrustum`), runs of visible zones merged into one draw, scenery drawn texture by texture across all regions. HD texture cache raised 200 -> 1000 (`GlobalConfig.HD_TEXTURE_CACHE`): the far view uses ~180 scenery textures, and with the nearby scene that overflowed 200, so textures evicted each other and were regenerated every frame (37 -> 65 fps).
  - `FAR_SCENERY_SIMPLIFY` (vertex grid, default 0 = full detail): 32 halved the cost but erased fence rails and dead-tree branches.
  - In-game settings: the FarView plugin (KondoKit panel, or `::farview <tiles>`, `::farscenery`, `::fartextures`, `::farsimplify <grid>`) via `API.SetFarView`. `FAR_TERRAIN_TILES` is now the total distance from the camera (default 200).
  - Plugin camera calls (`API.SetCameraYaw` etc.) made from other threads are queued for the game thread (fixes the BasicInputQOL crash during a background build).
  - The normal area loader uses the verified keys from `XteaKeyStore` too (the server's wrong keys threw in `prepareMapFiles`); a region nothing decrypts loads without scenery.
  - Camera clamp: `SceneGraph.setPlainTile` clamped the camera into the loaded 104x104 area. Zoomed far out near an area edge the camera is outside it, so the view stopped following the player and overheads (projected from the real camera) drifted off until the next area swap. In HD the scene is now drawn from the real camera; only the camera tile used to index per-tile arrays is clamped.
  - Measured: ~65 fps, ~325 MB GPU zoomed out.

## Stage 4 - Color banding (skipped)

With high-detail lighting the HD renderer already lights in floating point (GL lighting per vertex, `GlModel`/`GlTile`); the 7-bit lightness baking only runs with it off. No visible banding in game; the remaining 16-bit HSL is how the assets are authored.

## Stage 5 - Shader renderer (in progress)

- Step 1 (done): `PixelLighting` - a GLSL program reproducing the fixed-function scene lighting per pixel (vertex colour material, sun + negative back light, 4 local point lights, modulate/replace texturing, linear fog), reading the existing GL light state. Active only during the scene draw and only for state it reproduces; materials (water, specular...), the light-mesh pass and baked shadows stay fixed function. `GlobalConfig.PIXEL_LIGHTING`, `::pixellight`. Visually subtle (the old lighting was already smooth almost everywhere).
- Step 2 (built, off by default): sun shadow maps (`GlobalConfig.SHADOWS`, `::shadows`, `::shadowdebug`). The scene is drawn from the sun into a 4096 depth map: scenery into a static map redrawn only after moving 8 tiles, an area/sun change or every 5s; each frame it is blitted and players/NPCs/projectiles (`SceneGraph.scenery`, `Scenery.temporary`) are added (~0.8 ms). Depth compared in the shader, 3x3 PCF (the hardware compare mode read raw depth on this driver). Off because in this game's lighting (strong ambient, weak sun) the shadows come out fainter than the baked scenery shadows they replace, and the per-pixel map lookups cost GPU time (30-50 fps vs smoother without). Kept for experiments.
- Step 3 (done): local lights' ground glow (`LightingManager.renderLightMeshes`) with the falloff worked out per pixel instead of per tile corner (`GlobalConfig.SMOOTH_LIGHT_GLOW`, `::smoothglow`); the per-corner facing term rides in the light mesh's unused vertex alpha (`LightMesh`). Correct and free, but barely visible in game.
