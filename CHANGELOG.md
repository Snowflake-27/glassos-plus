# Changes from upstream v1.0.2-beta

## 1.1.0

- Added the native, D-pad-focusable Hero refresh icon beside Open. Cached titles shuffle without a consecutive repeat, using the existing loader/crossfade; last committed selection survives restart.
- Kept the distinct Settings catalog-refresh action with a clearer name.
- Added WuPlay IMDb movie/series links and a third preferred-player setting; missing apps/unsupported exact routes never fall back to a generic player home.
- Preserved compact Continue Watching, original Hero expansion, app customization and the pending opt-in Direct Home key experiment. Physical HOME interception is firmware dependent and not certified.
- Kept the application ID while branding the installed app GlassOS+. Added dedicated release signing, tag release automation, bundled license notices, stable download URL and recovery guidance.
- Retained targetSdk 28/minSdk 22 for inherited device compatibility; direct APK distribution only. WuPlay positive-case device testing is pending an installed compatible build.


## GlassOS+ initial source publication — 2026-11-04

- Separate Personal namespace/application ID, retaining coexistence with upstream GlassOS.
- Public Watch Next / Preview Channel reader with permission flow, filtering, deduplication, recent-activity ordering, original resume links and bounded private caches.
- Compact reusable Continue Watching carousel aligned to app dock metrics; progress and episode/source labels use published metadata.
- Persistent auto-scroll switch, default Off, with focus/interaction pause and idle resumption.
- Restore original Hero dock geometry and transitions; handle cached/warm Activity startup without moving focus.
- Layout configuration for app columns, margins, spacing and size; immediate hidden-app persistence.
- Larger app artwork rendering and Cinemeta backdrops, varied catalog selection, movie/series controls and exact IMDb routes to Nuvio/Stremio.
- Optional event-driven Fast Home accessibility helper without window-content access or key filtering; usage watcher fallback.
- Dependency-free checks for layout, routing, Home matching, progress, ordering, carousel wrapping and idle rules.
- Portable build helper, public documentation, upstream attribution and GPL v3 license retained.
