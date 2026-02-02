# QBE Integration Progress

## Overview

Replacing the incomplete custom IR in `drift-ir` with QBE (a lightweight compiler backend). The goal is to translate Drift AST directly to QBE IL text, which QBE then compiles to assembly.

**Pipeline:** `Drift Source → Parser → AST → drift-ir (QBEEmitter) → QBE IL text → QBE → Assembly`

---

## Decisions Made

### 1. QBE Library Separation
- Created separate repo `qbe-kt` for QBE IL structures
- Allows reuse in other projects
- Connected to drift via Gradle composite build or Maven Local

### 2. Temporary Naming Strategy
- Use AST `nodeId` for naming temporaries: `%v.{nodeId}`
- Handles variable shadowing naturally (each Let has unique nodeId)
- Expression results also use their expression's nodeId

### 3. Type Mapping (Drift → QBE)
| Drift Type | QBE Type |
|------------|----------|
| `Int`, `UInt`, `Bool` | `QBEWord` (w) |
| `Int64`, `String`, pointers | `QBELong` (l) |
| `Float` | `QBESingle` (s) |
| `Double` | `QBEDouble` (d) |
| Custom classes | `QBELong` (pointer) |
| `Void` | `null` (no return type) |
| `Union` | Deferred (TODO) |

### 4. Emitter Architecture
- `emitExpression` returns `Pair<QBEUsableValue, List<QBEInstruction>>`
  - Value: where result lives (temp or constant)
  - Instructions: how to compute it
- Top-level emitters (function, class) append to module lists
- Body-level emitters (let, if, return) return instruction lists

### 5. String Handling
- String literals create data entries: `data $str.N = { b "...", b 0 }`
- Return `QBEGlobal` pointing to the data
- `QBEGlobal` implements `QBEUsableValue` to be used as operand

---

## Files Structure

### qbe-kt (separate repo)
```
lib/src/main/kotlin/drift/qbe/
├── memory/
│   └── NameRegister.kt        # Temp/label/data name generation
└── structure/
    ├── QBEType.kt             # w, l, s, d, aggregate
    ├── QBEValue.kt            # Temporary, Global, Constant, Label
    ├── QBEOpcode.kt           # All QBE operations (complete)
    ├── QBEInstruction.kt      # Unary, Binary, ControlFlow
    ├── QBEBlock.kt            # Label + instructions
    ├── QBEFunction.kt         # Function definition
    ├── QBEData.kt             # Data items (strings, bytes, etc.)
    ├── QBETypeDefinition.kt   # Aggregate type definitions
    └── QBEModule.kt           # Top-level container
```

### drift-ir
```
src/main/java/drift/ir/
├── qbe/
│   └── QBEEmitter.kt          # Main AST → QBE translation
├── symbols/                    # Kept from before
│   ├── SymbolCollector.kt
│   ├── SymbolTable.kt
│   ├── Symbols.kt
│   └── TempSlotAllocator.kt
├── inference/
│   └── TypeInference.kt       # Kept, may need updates
└── exceptions/
    └── IRExceptions.kt        # Updated for QBE errors
```

---

## What's Implemented

### QBE Structures (qbe-kt) - COMPLETE
- [x] QBEType (Word, Long, Single, Double, Aggregate)
- [x] QBEValue (Temporary, Global, Constants, Label)
- [x] QBEOpcode (all operations)
- [x] QBEInstruction (Unary, Binary, ControlFlow)
- [x] QBEBlock
- [x] QBEFunction + QBEParameter
- [x] QBEData + QBEDataItem variants
- [x] QBETypeDefinition
- [x] QBEModule
- [x] NameRegister (temp, label, data naming)

### QBEEmitter (drift-ir) - IN PROGRESS
- [x] Class structure with types/data/functions lists
- [x] `emit()` entry point
- [x] `emitStatement()` dispatcher
- [x] `emitType()` - Drift type to QBE type mapping
- [x] `emitLet()` - variable declaration
- [x] `emitLiteral()` - partial (primaries done, containers/OOP as TODO)

---

## What's Next (Priority Order)

### 1. Complete emitExpression
- [ ] `emitVariable` - look up variable's temp by nodeId
- [ ] `emitBinary` - arithmetic/comparison operations
- [ ] `emitUnary` - negation, not
- [ ] `emitCall` - function calls

### 2. Control Flow
- [ ] `emitIf` - conditional with labels/jumps
- [ ] `emitFor` - loops with labels
- [ ] `emitBlock` - sequence of statements

### 3. Functions
- [ ] `emitFunction` - create QBEFunction, process body
- [ ] `emitReturn` - return instruction

### 4. Module Output
- [ ] Add `emit()` methods to QBE structures for text output
- [ ] Generate complete QBE IL file

### 5. Later Features
- [ ] `emitClass` - type definitions, methods
- [ ] Lambda/closure support
- [ ] List/Range literals
- [ ] Union types (tagged unions)

---

## Example Target

**Drift input:**
```drift
fun main(): Int {
    let x = 1 + 2
    return x
}
```

**QBE output:**
```qbe
export function w $main() {
@start
    %v.2 =w add 1, 2
    %v.1 =w copy %v.2
    ret %v.1
}
```

---

## Open Questions

1. **Variable resolution** - How does `emitVariable` know which nodeId a variable reference points to? Need resolution map from SymbolCollector.

2. **NameRegister scope** - Currently a class, instantiated per emission. May need reset between functions or modules.

3. **Type inference** - If `let x = ...` has no type annotation, need to infer from expression. Currently assumes type is always present (at least `AnyType`).

---

## Commands Reference

```bash
# Build drift
./gradlew build

# Run tests
./gradlew :drift-core:test

# Publish qbe-kt to maven local (in qbe-kt repo)
./gradlew publishToMavenLocal
```
