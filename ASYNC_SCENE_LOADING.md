# Background area loading (no "Loading - please wait" freeze)

Branch: `async-scene-loading`. Rollback: `.\rollback-graphics.ps1 before-async-loading`
(or `before-graphics`). Kill switch: `GlobalConfig.ASYNC_SCENE_LOADING = false`.

## How OSRS / RuneLite do it (research, 2026-09-26)

- RuneLite 1.10.0 (May 2023) "greatly reduced, and in many cases completely
  removed" the loading-line freeze; same release added commit 37580e5
  "api: add scene loading callbacks" (`DrawCallbacks.loadScene` / `swapScene`).
- The vanilla OSRS client builds the next `Scene` object off the client
  thread while the old one keeps rendering, then swaps it in. The public repo
  only shows the plugin side; the client side lives in RuneLite's injected
  client, which isn't published.
- GPU plugin `loadScene` (maploader thread): works out which 8x8 zones can be
  reused from the previous scene (`dx = (newBase - oldBase) >> 3`), sizes and
  uploads only new zones into buffers that the client thread allocated and
  mapped (`clientThread.invoke` + `CountDownLatch`). `swapScene` (client
  thread): frees culled zones, swaps the zone array in, unmaps buffers.
  A second load arriving before the first finished just drops the first.
- Takeaways: (1) build into separate state while the old scene renders,
  (2) keep all GL calls on the GL thread, (3) swap is a cheap pointer flip.

## Why this client freezes

- Rebuild packet -> `LoginManager.loadRegion` shifts every entity into the new
  coordinates, sets game state 25, then the next logic tick runs
  `rebuildMap()` synchronously: decode terrain/locs, build the scene, build GL
  tiles, lights and shadows. While in state 25 nothing is drawn but the
  "Loading" box, and no packets or game logic run.
- Scene state is ~100 static fields (SceneGraph, LightingManager, FogManager,
  ShadowManager, PathFinder, Camera origin) that renderer, game logic and
  loader all share, so there's no `Scene` object to build separately.
- Measured: 85-130ms per load warmed up, up to ~400ms cold.
- Climbing stairs (`SceneGraph.checkPlaneChange`) triggers the same rebuild.

## Design

1. **Turn-taking builder thread.** The rebuild runs on a dedicated thread
   but never at the same time as the main thread: they hand a baton back and
   forth. Each rendered frame, main gives the builder a few ms (budget), the
   builder runs until the budget is spent, then hands back at a checkpoint.
   No two threads ever touch shared state at once, so caches, Js5 and audio
   need no locking - it's a coroutine, not concurrency.
2. **Static-state swapping.** `SceneState` holds a copy of every scene field
   the build writes (found by bytecode call-graph analysis of all 520 methods
   the build can reach). On each turn switch the builder's set is written
   into the statics and the main set restored afterwards, so the renderer
   always sees the old scene and the builder the new one. Objects the build
   mutates in place (renderFlags, collision maps, shadow tiles and shadow
   image, light arrays and grid, fog light positions and per-chunk atmosphere,
   ambient loc sounds) get separate instances for the builder.
3. **Deferred GL.** The only GL calls the build makes are vertex buffer
   uploads (`GlVertexBufferObject`). Off the GL thread they copy their data
   and upload on the main thread before the swap (or on first bind).
4. **Packets pause, game keeps running.** After a rebuild packet no further
   packets are read until the swap (they're in the new coordinates), but the
   game keeps running and rendering in the old scene: movement, animation,
   camera, clicks. At the swap the original coordinate shift runs, then the
   original end of `rebuildMap` (ground items, pending loc changes, roofs,
   "map loaded" packet), and packet reading resumes - exactly as if those
   packets had arrived a few frames later.
5. **Safety.** Any exception or unexpected state in the builder discards the
   new scene (the old one was never touched) and falls back to the original
   synchronous load. Login and any non-HD load use the original path.

## Stages

- [x] A. Split `loadRegion` / `rebuildMap` into header/shift and
      prepare/build/finish. Synchronous behaviour unchanged.
- [x] B. Deferred VBO uploads in `GlVertexBufferObject`.
- [x] C. `SceneState` + `AsyncSceneLoader` (baton, checkpoints, swap,
      fallback), wire into packet handling, plane changes and the main loop.
- [x] D. Checkpoints inside the heavy loops so no single slice runs long.
- [x] E. In-game testing, then merge to master.

Log: each background load appends an `async: wall=.. builder=.. slices=.. maxSlice=.. swap=..` line to `<home>/area_load_timing.log`; `failed`/`aborted`/`builder slow` lines include the reason.

## Stage F - Build ahead of the server (done)

Round 1 result: no loading box, but the character still stopped. Packets
are held until the swap and a build took 300-1100ms end to end, so the
player ran out of queued steps.

2009scape server (`WalkingQueue.updateRegion`, `UpdateSceneGraph`): rebuild
when `|chunk - lastRebuildChunk| >= 4` on either axis, centred on the
player's current chunk. So `AsyncSceneLoader.predict()` builds the next area
while the player walks the last ~12 tiles towards that line, with packets
still flowing, and a matching rebuild packet just swaps it in.

Loc files need the server's XTEA keys. Only keys the server has actually
sent are used (`XteaKeyStore`, `<home>/xtea_keys.txt`): a wrong key could
decrypt to a garbage length and the cache decompressor has no length cap.
First visit near new regions therefore falls back to the round-1 behaviour.
Log lines: `prebuilt` (used), `prediction missed/changed` (discarded),
`async` (not predicted); `packetsPaused` is the stop the player feels.

## Stage G - The server's skipped tick (done)

Frame-by-frame captures (`MotionTrace`) against the unmodified v1.0.9 client
showed the remaining stop was the server: `WalkingQueue.update()` returns
before moving the player on the tick it sends the new area, so the player
stands still on the server for one tick. v1.0.9 hid it behind its ~420ms
loading freeze. The server can't be changed, so:
- `PathingEntity.teleport`: the rebuild tick's placement was queued as one
  walk-speed step (running players covered 2 tiles at walking pace); a 2-tile
  placement is now two run steps and a same-tile placement is ignored.
- `MovementSmoother`: predicts the skipped tick from the route and rebuild rule
  and eases the player's own movement over the last few ticks before it, so
  the steps already received last until movement resumes.
- `LoginManager.shiftRegion`: the follow camera's trailing position is shifted
  with the scene; before, `Camera.updatePlayerCamera` snapped it onto the
  player at every area change.
- Shadow textures and static model buffers are prepared before the swap
  instead of in the new area's first frame.
