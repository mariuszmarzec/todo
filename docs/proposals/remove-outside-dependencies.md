# Proposal: Remove outside dependencies from `com.marzec.navigation`

Status: **proposal only — no production code changes** (per issue
[#2](https://github.com/mariuszmarzec/todo/issues/2)).

Goal: make the `com.marzec.navigation` package self-contained so it can later
be extracted into a separate library.

The navigation package lives in
`multiplatformUtils/src/commonMain/kotlin/com/marzec/navigation/` and contains
6 files:

| File | Self-contained? |
|---|---|
| `NavigationAction.kt` | ✅ no outside types |
| `NavigationEntry.kt` | ⚠️ only `kotlinx.serialization.Serializable` (external lib) |
| `NavigationEntryCache.kt` | ❌ `com.marzec.cache.Cache` |
| `NavigationState.kt` | ✅ no outside types |
| `NavigationStore.kt` | ❌ quickmvi (`Store4Impl`, `IntentContext`), `com.marzec.preferences.StateCache` |
| `ResultCache.kt` | ❌ `com.marzec.cache.Cache` |

---

## 1. Inventory of outside dependencies

### 1.1 Project-internal packages (the actual extraction blockers)

#### `com.marzec.cache.Cache` (multiplatformUtils `cache` package)

| Location | Declaration | Usage |
|---|---|---|
| `ResultCache.kt` | `class ResultCache(private val memoryCache: Cache)` | `memoryCache.observe(key)`, `memoryCache.put(key, value)`, `memoryCache.remove(key)`, `memoryCache.toMap()` |
| `NavigationEntryCache.kt` | `class NavigationEntryCache(private val navigationStore: NavigationStore, private val cache: Cache) : Cache by cache` | constructor param + full interface delegation (`Cache by cache`); overrides `put` to only cache keys present in the current navigation state |

Note: `NavigationEntryCache` is also **exposed to clients as a `Cache`**
(`scrollStateCache` / `listScrollStateCache` in
`common/src/commonMain/kotlin/com/marzec/todo/DI.kt`), so its public type
change ripples to the client.

#### `com.marzec.preferences.StateCache` (multiplatformUtils `preferences` package)

| Location | Declaration | Usage |
|---|---|---|
| `NavigationStore.kt` | `class NavigationStore(..., private val stateCache: StateCache, ...) : Store4Impl<NavigationState>(scope, stateCache.get(cacheKey) ?: initialState)` | `stateCache.get(cacheKey)` (initial state), `stateCache.set(cacheKey, newState)` (in `onNewState`), `stateCache.remove(entry.cacheKey)` (in `clearCache`) |

#### quickmvi — `com.marzec.mvi.Store4Impl` / `IntentContext` (EXTERNAL library)

| Location | Declaration | Usage |
|---|---|---|
| `NavigationStore.kt` | `import com.marzec.mvi.Store4Impl` / `import com.marzec.mvi.IntentContext` | `NavigationStore` **extends `Store4Impl<NavigationState>`**; `IntentContext<NavigationState, NavigationUpdate>` used as receiver in `requestKey(...)` and `navigate(...)` |

Additionally, through the `Store4Impl` superclass the store relies on the
quickmvi API: `intent<NavigationUpdate> { ... }`, `resultNonNull()`,
`state.value` (a `MutableStateFlow`), `onNewState` override and the
`onNewStateCallback` property.

Important nuance: `Store4Impl`, `IntentContext`, `Intent3`, `IntentBuilder`,
`intent`, `resultNonNull`, `onNewStateCallback`, `collectState`, etc. are **not
defined in this repository**. They come from the published Maven artifact
`io.github.mariuszmarzec:quickmvi:1.1.0` (declared as `api(libs.quickMvi)` in
`multiplatformUtils/build.gradle.kts`, version in
`gradle/libs.versions.toml`), which happens to publish its API under the
package name `com.marzec.mvi`. The local `multiplatformUtils` `mvi` package
(`State.kt`, `StateData.kt`, `builderExtensions.kt`, `CacheStateStore.kt`)
re-exports/extends that API and itself depends on `com.marzec.content`,
`com.marzec.extensions` and `com.marzec.preferences` — but navigation does not
use any of those local mvi files.

So: the mvi dependency of navigation is a **third-party library dependency**,
not a project-internal package dependency.

### 1.2 External library dependencies (acceptable for a library, listed for completeness)

| Dependency | Where used | Notes |
|---|---|---|
| `kotlinx.coroutines` (`CoroutineScope`, `Flow`, `flowOf`, `filter`, `filterIsInstance`, `filterNotNull`, `map`) | `NavigationStore.kt`, `ResultCache.kt` | normal published library dependency |
| `kotlinx.serialization` (`@Serializable`, `Json`, `decodeFromString`, `encodeToString`) | `NavigationEntry.kt` (`@Serializable ResultKey`), `ResultCache.kt` (key encoding/decoding) | normal published library dependency |

An extracted library may keep these as regular Gradle dependencies — no
wrappers needed.

### 1.3 Already self-contained

- `NavigationAction.kt` — no imports at all.
- `NavigationState.kt` — no imports at all.

---

## 2. Proposed refactorings (wrapper interface + client-side proxy)

The pattern requested in the issue: define the contract **on the navigation
side** (interface), and provide a **proxy implementation on the client side**
that adapts the client's own implementation to that contract. This is applied
to each internal dependency below.

### 2.1 `com.marzec.cache.Cache` → `NavigationCache`

**Navigation side** — new file `NavigationCache.kt` in `com.marzec.navigation`:

```kotlin
interface NavigationCache {
    suspend fun put(key: String, value: Any?)
    suspend fun <T> get(key: String): T?
    suspend fun remove(key: String)
    suspend fun <T> observe(key: String): Flow<T?>
    suspend fun toMap(): Map<String, Any?>
}
```

Only the methods navigation actually uses are declared (`update` from
`com.marzec.cache.Cache` is never used by navigation and is intentionally
omitted).

- `ResultCache(private val memoryCache: NavigationCache)` — type change only.
- `NavigationEntryCache` — implement `NavigationCache` instead of `Cache`
  (drop the `Cache by cache` delegation; delegate to the wrapped cache
  explicitly or keep delegation via a `NavigationCache` field). Since the
  client currently consumes it **as** a `Cache`, the client-side properties
  (`scrollStateCache`, `listScrollStateCache` in `DI.kt`) must be retyped to
  `NavigationCache`.

**Client side** — new file in the client module (e.g. `common`):

```kotlin
class NavigationCacheProxy(private val cache: Cache) : NavigationCache {
    override suspend fun put(key: String, value: Any?) = cache.put(key, value)
    override suspend fun <T> get(key: String): T? = cache.get(key)
    override suspend fun remove(key: String) = cache.remove(key)
    override suspend fun <T> observe(key: String): Flow<T?> = cache.observe(key)
    override suspend fun toMap(): Map<String, Any?> = cache.toMap()
}
```

(Manual delegation is required — `Cache` has the extra `update` member, so
`NavigationCache by cache` does not compile.)

Wiring: `NavigationStore` is constructed with
`ResultCache(NavigationCacheProxy(memoryCache))` and
`NavigationEntryCache(navigationStore, NavigationCacheProxy(memoryCache))`
instead of raw `MemoryCache()` / `Cache` values.

### 2.2 `com.marzec.preferences.StateCache` → `NavigationStateCache`

**Navigation side** — new file `NavigationStateCache.kt` in
`com.marzec.navigation`:

```kotlin
interface NavigationStateCache {
    fun set(key: String, value: Any)
    fun <T> get(key: String): T?
    fun remove(key: String)
}
```

`NavigationStore` takes `stateCache: NavigationStateCache` instead of
`StateCache`. The interfaces are shape-identical, so the proxy is trivial:

**Client side:**

```kotlin
class NavigationStateCacheProxy(private val stateCache: StateCache) : NavigationStateCache {
    override fun set(key: String, value: Any) = stateCache.set(key, value)
    override fun <T> get(key: String): T? = stateCache.get(key)
    override fun remove(key: String) = stateCache.remove(key)
}
```

Wiring: `DI.stateCache` (a `MemoryStateCache()`) stays as-is; it is wrapped at
the navigation boundary: `NavigationStateCacheProxy(stateCache)`.

### 2.3 quickmvi (`Store4Impl` / `IntentContext`) — two options

Unlike `cache`/`preferences`, quickmvi is a **published external artifact**, so
an extracted library can legitimately declare it as a dependency. Two options:

**Option A (recommended for the first step): keep quickmvi as a library
dependency.** `NavigationStore` keeps extending `Store4Impl`, and the extracted
module declares `io.github.mariuszmarzec:quickmvi:1.1.0` in its own Gradle
file — exactly like `kotlinx-coroutines` and `kotlinx-serialization`. Zero
behavioral change, and the module becomes extractable because no
*project-internal* package leaks in.

**Option B (follow-up, full isolation): hide quickmvi behind a
navigation-owned contract.** Define a minimal navigation-side interface
capturing what `NavigationStore` needs from the engine:

```kotlin
interface NavigationEngine<S> {
    val state: StateFlow<S>
    val onNewStateCallback: ((S) -> Unit)?
    suspend fun run(intent: suspend IntentContextImpl<S, *>.() -> Any)
    // or a small `submit/reduce/sideEffect` surface tailored to NavigationStore
}
```

Move the quickmvi-dependent implementation (`Store4Impl` subclass, `intent`,
`resultNonNull`, `IntentContext`) into an internal adapter (e.g.
`QuickMviNavigationEngine`) that the public `NavigationStore` delegates to.
Public API then exposes neither `Store4Impl` nor `IntentContext`. This is more
invasive (the store's `intent { }` DSL, `resultNonNull()` and
`onNewStateCallback` are used throughout `NavigationStore`), so it is best done
as a separate step after the module is extracted. Note there is no
"client-side proxy" here — the engine is internal plumbing, not something the
client supplies.

### 2.4 External libraries

No wrappers. `kotlinx-coroutines-core` and `kotlinx-serialization-json` become
declared dependencies of the extracted module.

---

## 3. Other seams needed for full extraction of navigation into a library

1. **New Gradle module** — move the 6 navigation files into a new
   multiplatform module (e.g. `:navigation`) with `kotlin("multiplatform")` +
   `kotlin("plugin.serialization")` and dependencies:
   `kotlinx-coroutines-core`, `kotlinx-serialization-json`, `quickmvi`.
   The navigation package itself has **no Compose dependency**, so the module
   does not need Compose.

2. **`NavigationHost.kt` factories** —
   `multiplatformUtils/src/commonMain/kotlin/com/marzec/view/NavigationHost.kt`
   currently holds the `navigationStore(...)` factory functions and
   `initialState(...)` helper that construct `NavigationStore` (wiring
   `ResultCache(MemoryCache())` and setting `onNewStateCallback`). These live
   outside the navigation package but are tightly coupled to it; they must
   move into the extracted module (as convenience factories that now use
   `NavigationCache`/`NavigationStateCache`) or into the client. `NavigationHost`
   itself (Compose UI + `collectState` from quickmvi-compose) stays in the
   client/UI layer.

3. **Client wiring updates** —
   `common/src/commonMain/kotlin/com/marzec/todo/DI.kt` (provide the
   `navigationCache` proxy, wrap `stateCache`, retype
   `scrollStateCache`/`listScrollStateCache`), the desktop example
   (`multiplatformUtils/src/desktopMain/kotlin/com/marzec/example/Main.kt`),
   and anything constructing `NavigationStore`/`NavigationEntryCache`/`ResultCache`.

4. **Tests** —
   `multiplatformUtils/src/desktopTest/kotlin/com/marzec/navigation/NavigationStoreTest.kt`
   uses `com.marzec.preferences.StateCache` (mockk) and `ResultCache`; it moves
   with the module and its mocks are retyped to `NavigationStateCache` /
   `NavigationCache`. No test logic changes expected (the interfaces are
   shape-identical to today's types).

5. **Version catalog / build files** — `quickMvi`, coroutines and
   serialization versions already exist in `gradle/libs.versions.toml`; the
   extracted module just references them. No new versions needed.

6. **`com.marzec.mvi` local package stays behind** — the extracted module must
   depend only on the published `quickmvi` artifact, not on the local
   `multiplatformUtils` `mvi` package (which drags in `content`, `extensions`,
   `preferences`).

7. **No production code change in this PR** — this document is the complete
   groundwork; the refactorings above are to be implemented in follow-up PRs.

---

## 4. Verification performed

- Source-level inventory via `grep` of all `import` statements in
  `multiplatformUtils/src/commonMain/kotlin/com/marzec/navigation/*.kt`
  (all 6 files reviewed in full).
- Cross-checked that `Store4`/`Store4Impl`/`IntentContext` are **not defined**
  anywhere in the repository and that `quickMvi` is declared in
  `gradle/libs.versions.toml` (`io.github.mariuszmarzec:quickmvi:1.1.0`) and
  wired as `api(libs.quickMvi)` in `multiplatformUtils/build.gradle.kts`.
- Reviewed client wiring (`DI.kt`, `NavigationHost.kt`, desktop `Main.kt`) to
  confirm how the navigation types are constructed/consumed.
- No build was executed and no production code was changed — this task is
  analysis/proposal only.
