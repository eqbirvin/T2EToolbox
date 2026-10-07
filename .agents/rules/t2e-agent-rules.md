# T2E Toolbox — Unified Agent Rules

These rules govern ALL work on this Android project, for every agent/model. Part A = workflow/process. Part B = engineering standards.

**Engineering rules 6, 8, 9, 13, 14, and 25 are release-blocking — never merge or finalize code that violates them.**

## Part A — Workflow & Process

### W1. Automated Builds, Manual Deployment
- NEVER run `adb install` yourself; the user installs. Provide the exact install command in a bash block (e.g. `adb install -r releases\t2e.toolbox.v[VERSION].apk`).
- Build via `./gradlew copyReleaseApk`. That task MUST package the minified RELEASE APK (debug-key signed for sideloading) into `releases/` named with the version — never the debug APK; do not repoint it at assembleDebug.
- All verification, task sign-off, and ANY performance judgment happens on the release APK. Build debug only when actively debugging (debugger, Layout Inspector, stack traces) — never as a verification step.
- For iterative compile checks mid-task use `./gradlew compileReleaseKotlin`; run the full `copyReleaseApk` once at task completion.

### W2. Semantic Versioning (vMAJOR.MINOR.PATCH)
- Format vX.Y.Z in `app/build.gradle.kts`; `versionCode` increments by 1 with every release, no exceptions.
- PATCH (third segment): bump for EVERY delivered build of tweaks, fixes, or small adjustments — including each iteration within a single conversation. Three tweak rounds = three patch versions.
- MINOR (second segment): bump — resetting PATCH to 0 — for new features, feature overhauls, or enhancements.
- MAJOR (first segment): NEVER bumped by an agent. Only on explicit user instruction.
- Versions are IMMUTABLE once released: if the current version already has an APK in `releases` or a ledger entry, that version is closed — bump before building again. NEVER overwrite an existing `releases` APK; NEVER log the same version twice in the ledger.
- Any `copyReleaseApk` run that delivers an APK to the user IS a release: bump first, then build.
- Version bump + `CHANGELOG.md` entry (per W5, when material) + ledger entry = ONE atomic "finalize release" ritual.

### W3. Central Ledger (history.log)
- Aggressively log every completed task, feature, and fix in `history.log`, exact format: `[YYYY-MM-DDTHH:MM] [vX.Y.Z] detailed description of what changed in lowercase.` Append-only, chronological; never delete or clear it.

### W4. Non-Destructive Artifacts
- Never overwrite recurring planning/summary artifacts (`implementation_plan`, `task`, `walkthrough`); always append the active version to the filename (e.g. `task_v0.3.0.md`) so every cycle's history is preserved.

### W5. Changelog
- Maintain `CHANGELOG.md` at the project root: append-only, newest first, one section per released version that contained MATERIAL changes.
- "Material" = user-visible: features, changed/removed behavior, UI changes, settings additions, noticeable perf/bug fixes. Exclude purely internal work (refactors, build config, dependency bumps) unless user-visible; if nothing material, skip the entry (no filler items).
- Never edit/merge/delete entries for past versions.
- If an in-app "What's New" UI is ever added, it must RENDER from a structured source of truth (never hard-code changelog content in a composable), compare versions semantically via `BuildConfig.VERSION_NAME`, and show all entries newer than the user's last-run version — not just the latest.

### W6. Scoped Tasks, Verified Commits
- Fix ONLY what the task specifies; flag unrelated issues for future tasks instead of touching them.
- Every task ends with: successful build, stated verification (what was run/checked and the result), and a commit — before any next task begins.
- The commit is not optional or implied: actually run `git add` + `git commit` with a descriptive message and report the commit hash in your final summary. No hash = incomplete task.
- Dirty-tree guard: BEFORE starting any task run `git status`; if uncommitted changes exist, STOP and report them — do not build on unknown state or commit someone else's changes. Wait for the user's decision.
- No repair debris: delete any temporary fix scripts/scratch files before the task ends. If a repair fails repeatedly, STOP and report honestly instead of escalating through more scripts — a reverted task is recoverable; a half-patched file presented as success is not.
- Refactor-only tasks ("split"/"extract"/"move"/"no behavior change") move code VERBATIM: character-for-character cut and paste, changing only imports/visibility. Never regenerate, reformat, restyle, or rewrite from memory — if the diff contains new UI/logic code that did not previously exist, the task failed; revert.
- "No behavior change" is verified by comparison: the app must look and behave identically to the previous build. Any visible difference is a failure, not a bonus improvement.
- If a task references a spec in `specs/`, read the ENTIRE spec before any code; it is authoritative — never re-ask settled questions, and flag any deviation before making it.

## Part B — Android Engineering Rules (Non-Negotiable)

Architecture
1. NEVER put business logic, DB access, or privileged-shell/service calls in an Activity or @Composable. Use ViewModel + repository; composables receive plain state and emit events via lambdas — nothing else.
2. Any file over 500 lines MUST be split before adding more code to it. One screen = one file; shared components get their own files.
3. UI code MUST NOT reference a Service singleton (e.g. `MyService.instance`). Expose service data via a repository backed by Flow/StateFlow/Room.
4. Screens with more than 8 parameters MUST take one immutable UiState data class plus at most a few event lambdas.

Concurrency
5. NEVER use GlobalScope or ad-hoc CoroutineScope(...). Use viewModelScope, lifecycleScope, a scope owned and cancelled by the component, or WorkManager for fire-and-forget work that must survive the process.
6. NEVER do disk I/O, SharedPreferences write-then-read, binder/IPC calls (PackageManager, shell-privilege bridges like Shizuku, AppWidgetManager), shell-command execution, or bitmap decoding on the main thread. Wrap blocking APIs in withContext(Dispatchers.IO) and cache the result.
7. In a BroadcastReceiver, never launch a coroutine that outlives onReceive without goAsync() or WorkManager.

Compose Performance
8. NEVER make IPC, database, file, or SharedPreferences calls inside composition — including inside remember{} blocks of list items (LazyColumn disposes off-screen items, so the cost repeats on every scroll). Resolve data BEFORE composition (ViewModel) and pass it as parameters.
9. Every LazyColumn/LazyRow items() call MUST provide a stable key.
10. Do not create new collections (toMap/toList/sortedBy) in composable bodies on every recomposition — wrap derivations in remember(keys) or derivedStateOf, and pass immutable/stable types as parameters.
11. Hoist SharedPreferences reads, formatters, and system-service lookups out of per-item composables; a list item composable must be a pure function of its parameters.
12. Cache expensive text formatting (SimpleDateFormat, AnnotatedString building) in remember with proper keys; allocate formatter instances once, not per call.

Data Layer
13. Every Room entity MUST declare indices for every column used in a WHERE or ORDER BY clause.
14. NEVER ship fallbackToDestructiveMigration() in release code. Write Migrations.
15. Any append-only table MUST have a retention/pruning policy from day one.
16. Do not load unbounded tables into memory; use LIMIT or Paging when a table can exceed a few hundred rows.
17. Persist structured data as JSON via kotlinx.serialization — never as hand-joined delimiter strings.

Caching & Memory
18. Bitmap/drawable caches MUST be bounded (LruCache) and store images at display size, not intrinsic size.
19. Cache negative lookups too — a cache that only stores hits does nothing for the dominant miss path.
20. Cache results of cross-process lookups (app labels, icons, package-installed state) in memory for the process lifetime.

Build & Release
21. Release builds MUST set isMinifyEnabled=true and isShrinkResources=true, with tested ProGuard rules.
22. Keep Compose BOM, AGP, and Kotlin within one year of current. NEVER introduce new code using APIs already deprecated in the project's own dependency versions.
23. Use BuildConfig.VERSION_NAME — never hard-code version strings in UI/logic.
24. Do not request QUERY_ALL_PACKAGES or other broad permissions when a <queries> declaration or an already-held privileged role suffices.

Correctness & Hygiene
25. NEVER write `catch (e: Exception) { e.printStackTrace() }`. Catch the narrowest type, log with Log.e(TAG, msg, e), and deliberately rethrow, recover, or surface to the user.
26. Guard nullable-to-string conversions with orEmpty()/?: — `nullable.toString()` producing "null" is a bug.
27. Event-driven system callbacks (boot receivers, onReceive, sensors) MUST filter irrelevant events FIRST and debounce expensive downstream work.
28. Side effects triggered inside a loop run ONCE after the loop, not per iteration.
29. For every scrolling-list feature, state the frame budget (16.6ms) and verify: no per-item IPC, keyed items, bounded item work. If you cannot verify, say so explicitly in your summary.
30. Never mark a performance issue "fixed" without naming the measurement that would prove it (Macrobenchmark FrameTimingMetric, gfxinfo jank %, or profiler trace).
31. Any operation that changes SYSTEM-level state (display size/density, global settings, shell commands) MUST: capture the prior state before changing it, persist that snapshot, provide a working restore path, and surface success/failure to the user. Never apply a system change you cannot undo.
