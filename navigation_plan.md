# Navigation Extraction Plan

## Goal
Extract the navigation package into a separate Gradle library module as described in GitHub issue #9.

## Current State Analysis

### Navigation Package Location
- `/multiplatformUtils/src/commonMain/kotlin/com/marzec/navigation/` contains core navigation classes:
  - NavigationStore.kt (core logic with StateCache dependency)
  - NavigationAction.kt (Destination interface)
  - NavigationEntry.kt, NavigationState.kt, NavigationFlow.kt
  - ResultCache.kt, NavigationCache.kt, NavigationEntryCache.kt
- `/multiplatformUtils/src/commonMain/kotlin/com/marzec/view/NavigationHost.kt` (UI factories)
- `/multiplatformUtils/src/commonMain/kotlin/com/marzec/preferences/StateCache.kt` (dependency to wrap)
- `/multiplatformUtils/src/commonMain/kotlin/com/marzec/cache/MemoryCache.kt` (used in factories)

### Dependencies Identified
- NavigationStore depends on StateCache (from preferences package)
- NavigationHost factories depend on StateCache and MemoryCache
- Current module structure: :common, :app, :desktop, :multiplatformUtils
- QuickMVI dependency configured in version catalog

## Plan

### Key Decisions

#### QuickMVI Strategy: Option B (Copy minimal required parts)
**Rationale:**
1. Avoids creating an external dependency that would need to be published
2. Reduces complexity for initial extraction
3. Allows navigation module to be self-contained
4. Follows principle of minimal dependencies for core infrastructure
5. Can be revisited later if quickmvi needs to be shared elsewhere

#### StateCache Interface Approach
Create a navigation-owned interface that abstracts StateCache functionality, allowing:
1. Navigation module to not depend on preferences package
2. Implementation to be provided by the consuming module
3. Clean separation of concerns

### Files to be Changed/Moved/Created

#### New Module Structure
```
:navigation
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   └── com/marzec/navigation/
│   │   │       ├── NavigationStore.kt
│   │   │       ├── NavigationAction.kt
│   │   │       ├── NavigationEntry.kt
│   │   │       ├── NavigationState.kt
│   │   │       ├── NavigationFlow.kt
│   │   │       ├── ResultCache.kt
│   │   │       ├── NavigationCache.kt
│   │   │       ├── NavigationEntryCache.kt
│   │   │       ├── StateReader.kt (new interface)
│   │   │       └── StateEditor.kt (new interface)
│   │   └── androidMain/ (empty initially)
│   │   └── iosMain/ (empty initially)
│   └── src/androidTest/ (tests to be moved)
│       └── kotlin/com/marzec/navigation/
└── build.gradle.kts
```

#### Files to Move
From `:multiplatformUtils` to `:navigation`:
- All files in `src/commonMain/kotlin/com/marzec/navigation/`
- `NavigationHost.kt` (from view package - will be updated to use new interfaces)
- Test files (to be identified)

#### New Files to Create
- `:navigation/src/commonMain/kotlin/com/marzec/navigation/StateReader.kt`
- `:navigation/src/commonMain/kotlin/com/marzec/navigation/StateEditor.kt`
- `:navigation/build.gradle.kts`

#### Files to Modify
- `:multiplatformUtils/build.gradle.kts` (remove navigation code, add :navigation dependency)
- `:multiplatformUtils/src/commonMain/kotlin/com/marzec/view/NavigationHost.kt` (update to use new interfaces)
- `:multiplatformUtils/src/commonMain/kotlin/com/marzec/preferences/StateCache.kt` (implement new interfaces)
- `:app/build.gradle.kts` (add :navigation dependency if needed)
- `:desktop/build.gradle.kts` (add :navigation dependency if needed)
- `settings.gradle.kts` (add :navigation module)
- `gradle/libs.versions.toml` (add navigation version if needed)

### Sequence of Steps

#### Phase 1: Preparation
1. Create `:navigation` directory structure
2. Create initial `build.gradle.kts` for navigation module
3. Add `:navigation` to `settings.gradle.kts`

#### Phase 2: Interface Creation
1. Create `StateReader.kt` interface in navigation module (wraps StateCache.get functionality)
2. Create `StateEditor.kt` interface in navigation module (wraps StateCache.set/remove functionality)
3. Update NavigationStore to depend on these interfaces instead of concrete StateCache

#### Phase 3: Code Migration
1. Move all navigation package files from multiplatformUtils to navigation module
2. Move NavigationHost.kt to navigation module (update package to com.marzec.navigation)
3. Update NavigationHost factories to accept StateReader/StateEditor parameters
4. Implement StateReader/StateEditor in StateCache class (in preferences package)

#### Phase 4: Dependency Updates
1. Update NavigationStore constructor to take StateReader/StateEditor instead of StateCache
2. Update NavigationHost factory functions to take StateReader/StateEditor
3. Update multiplatformUtils to depend on :navigation module
4. Update app/desktop modules to depend on :navigation if they use navigation directly

#### Phase 5: Test Migration
1. Identify and move navigation-related tests to navigation module
2. Update test dependencies as needed
3. Ensure tests still pass with new interface-based approach

#### Phase 6: Version Catalog & Finalization
1. Add navigation version to libs.versions.toml if needed
2. Run detekt/checks to ensure code quality
3. Verify all existing functionality works
4. Clean up any unused imports/dependencies in multiplatformUtils

### Risks & Edge Cases

#### Risks
1. **Circular Dependencies**: Navigation depending on preferences for StateCache implementation
   - Mitigation: Use interface segregation - navigation defines interfaces, preferences implements them

2. **Test Breakage**: Tests may break due to package/import changes
   - Mitigation: Move tests early and fix package statements

3. **Factory Signature Changes**: NavigationHost factory signatures will change
   - Mitigation: Update all call sites in app/desktop modules

4. **MemoryCache Coupling**: NavigationHost factories create MemoryCache internally
   - Mitigation: Consider abstracting Cache interface as well, or accept it as parameter

#### Edge Cases
1. **Null State Handling**: Ensure StateReader handles null states correctly
2. **Cache Key Consistency**: Verify cache key generation remains consistent
3. **ResultCache Dependency**: ResultCache currently constructed in factories - may need review
4. **Platform-specific Code**: Ensure commonMain navigation code doesn't accidentally use platform-specific APIs

### Success Criteria (Verifiable)
1. [ ] Navigation module compiles independently
2. [ ] Application runs successfully with navigation extracted
3. [ ] All navigation-related tests pass in new module
4. [ ] No direct dependencies from navigation module to preferences package
5. [ ] StateReader/StateEditor interfaces are implemented by StateCache
6. [ ] NavigationHost factories accept StateReader/StateEditor parameters
7. [ ] MultiplatformUtils module depends on :navigation module
8. [ ] No navigation-related code remains in multiplatformUtils source (except StateCache implementations)
9. [ ] Detekt checks pass
10. [ ] Version catalog updated appropriately

### Open Questions
1. Should ResultCache also be abstracted behind an interface?
2. Should the Cache abstraction (for MemoryCache) also be extracted?
3. How should we handle the factory functions that currently create ResultCache with MemoryCache?
4. Should we create a separate :navigation-common module for pure Kotlin code?
5. What is the minimum set of tests that must pass to consider this extraction successful?