# Session TODO

## Bug: Nested class not visible as type annotation inside function body

### What we found

`SymbolTable.hasClass` only checks `scopes.last()`. This is fine during type-checking
because `TypeChecker` never pushes/pops scopes — `scopes.last()` is always `scope0`
(the root scope) for the entire type-checking pass.

The real bug: `SymbolCollector` registers nested classes into a function's scope, then
pops that scope when leaving the function. By the time `TypeChecker` runs, the nested
class's name binding is gone from `scopes` (it still exists in `allSymbols` by `nodeId`,
but is unreachable by name).

### Reproducer

```drift
fun t {
    class A
    let x: A = A()   // DTCClassNotFoundException: Class 'A' not found
}
```

`A()` works (resolved via `refResolutions` during collection while scopes are live).
`let x: A` fails (TypeChecker calls `hasClass("A")` at `TypeChecker.kt:360`, scope0
has no binding for `A`).

### Two fix approaches to evaluate next session

**Approach A — TypeChecker mirrors SymbolCollector scope push/pop**
- TypeChecker pushes/pops scopes as it descends into function bodies
- Risk: two components must stay in sync; drift between them causes subtle bugs

**Approach B — Resolve type annotations during collection (like `refResolutions`)**
- SymbolCollector adds a `typeResolutions: Map<Int, Int>` (annotation nodeId → class nodeId)
- Resolved while scopes are still live, same pattern as variable reference resolution
- TypeChecker at line 360 uses `typeResolutions` instead of `hasClass` by name
- More consistent with how the codebase already handles variable references

### Files involved

- `drift-analysis/src/main/kotlin/drift/analysis/symbols/SymbolTable.kt` — `hasClass` (line 86)
- `drift-analysis/src/main/kotlin/drift/analysis/symbols/SymbolCollector.kt` — scope push/pop, `refResolutions`
- `drift-analysis/src/main/kotlin/drift/analysis/checkers/TypeChecker.kt` — `hasClass` call (line 360), `refResolutions` (line 192)
- `drift-bootstrap/src/main/kotlin/drift/bootstrap/Bootstrap.kt` — shared `SymbolTable` instance