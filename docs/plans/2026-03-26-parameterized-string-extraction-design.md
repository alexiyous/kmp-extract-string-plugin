# Parameterized String Extraction — Design

## Overview

Extend the existing `ExtractKmpStringIntention` to support Kotlin string templates with interpolated variables (e.g. `"Hello $name, you have $count messages"`), converting them into parameterized Android/KMP string resources with typed format specifiers.

---

## Approach

Extend the existing single `IntentionAction` (Approach A). No new intention action is added. The flow branches internally based on whether the string is plain or parameterized.

---

## New Component: `StringTemplateAnalyzer`

Walks all entries of a `KtStringTemplateExpression` and produces a `StringAnalysis` sealed class.

```kotlin
sealed class StringAnalysis {
    data class PlainString(val value: String, val xmlSafeValue: String) : StringAnalysis()
    data class ParameterizedString(
        val formatString: String,
        val args: List<TemplateArg>
    ) : StringAnalysis()
}

data class TemplateArg(
    val expression: String,   // e.g. "user.name", "inbox.count()"
    val formatSpec: String,   // e.g. "%1$s", "%2$d"
    val typeUnresolved: Boolean
)
```

### Entry handling

| Entry type | Action |
|---|---|
| `KtLiteralStringTemplateEntry` | Append as plain text to format string |
| `KtSimpleNameStringTemplateEntry` (`$var`) | Resolve type via PSI |
| `KtBlockStringTemplateEntry` (`${expr}`) | Resolve return type via PSI |

### Type → format specifier mapping

| Kotlin type | Specifier | Behaviour |
|---|---|---|
| `Int`, `Short`, `Byte`, `Long` | `%N$d` | Proceed |
| `Float`, `Double` | `%N$f` | Proceed |
| `Char` | `%N$c` | Proceed |
| `String` | `%N$s` | Proceed |
| Everything else + unresolved | — | **Cancel + warn** |

If any argument maps to an unsupported or unresolved type, `StringTemplateAnalyzer` returns a `UnsupportedArgs` result listing the offending expressions and their resolved (or unresolved) types. The `invoke` method shows the warning and cancels — nothing is written.

---

## Changes to Existing Components

### `isAvailable`
Remove the single-entry plain-string guard. Now returns `true` for any `KtStringTemplateExpression` in a `.kt` file.

### `invoke`
1. Call `StringTemplateAnalyzer.analyze(stringExpr, file)`
2. If unsupported args → show warning dialog listing offending expressions → cancel
3. Otherwise continue existing flow, passing `StringAnalysis` through

### `ExtractStringDialog`
Add a read-only preview field showing the generated format string (e.g. `This is number %1$d with name = %2$s`) so the user can verify it before typing the key name.

### `buildReplacement`

| Context | KMP | Android |
|---|---|---|
| Inside `@Composable` | `stringResource(Res.string.key, arg1, arg2)` | `stringResource(R.string.key, arg1, arg2)` |
| Outside `@Composable` | `Res.string.key` | `R.string.key` |

Outside composable, args are omitted — the resource reference is returned and the developer resolves it with args in their own context.

### `writeEntry`
No changes. The format string is written as the value as-is. Existing XML escaping covers all necessary characters.

---

## Warning Dialog

Shown when any arg type is unsupported or unresolved. Cancels the extraction.

```
Cannot extract — unsupported argument types:
  • user.metadata → UserMetadata
  • response → unresolved

Only Int, Long, Float, Double, Char, and String are supported.
```

---

## Out of Scope

- Nested string templates
- Multi-line strings with interpolation
- Resolving args outside `@Composable` into `getString(Res.string.key, args)` — left to developer
