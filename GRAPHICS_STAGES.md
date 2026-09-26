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

## Stage 2 - Fog and MSAA (pending)

- Confirm MSAA works via in-game anti-aliasing option.
- Exponential/per-pixel fog in `FogManager`.

## Stage 3 - Extended draw distance (pending)

- Check whether 2009scape's region XTEA keys are all zero (log `Protocol.java:431`); if so, load regions beyond 104x104 from the cache.

## Stage 4 - Color banding (pending)

## Stage 5 - Shader renderer prototype (pending)

- Per-pixel lighting for world models using existing `LightingManager` lights.
