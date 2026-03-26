# KMP Extract String Resource

An Android Studio plugin that extracts hardcoded string literals into `strings.xml` with a single `Alt+Enter` — with full support for **Kotlin Multiplatform (KMP)** projects using Compose Resources and regular **Android** projects.

---

## Demo
https://github.com/user-attachments/assets/091fcb01-9b57-4384-8d68-b7d333b0d25b

---

## Features

- **Alt+Enter intention action** — appears on any plain string literal in `.kt` files (Mac: `Option+Enter`)
- **KMP support** — writes to `composeResources/values/strings.xml`; replaces with `stringResource(Res.string.key)` inside Composables, or `Res.string.key` outside (ViewModels, repositories, etc.)
- **Android support** — writes to `res/values/strings.xml`; replaces with `stringResource(R.string.key)` inside Composables, or `R.string.key` outside
- **Auto-detects project type** — scans the module's source sets; prefers KMP if both are present
- **Auto-imports** — adds `stringResource` import and (for KMP) auto-detects and imports your generated `Res` class
- **Composable-aware** — outside Compose it returns the raw resource reference, leaving you free to resolve it however fits your architecture (`getString`, suspend `getString`, pass to UI layer, etc.)
- **Prompts for key name** — pre-fills with a `snake_case` suggestion derived from the string value
- **Conflict detection with diff preview** — if the key already exists with a different value, shows a diff dialog before overwriting
- **XML-safe** — automatically escapes `&`, `<`, `>`, `"`, `'` when writing to `strings.xml`
- **Parameterized string support** — string templates with `$variable` / `${expr}` are converted to format strings (`%1$d`, `%2$s`, etc.) with automatic type detection; unsupported types cancel the extraction with a clear warning

---

## Installation

### From disk (manual)

1. Download the latest `.zip` from the [Releases](https://github.com/alexiyous/kmp-extract-string-plugin/releases) page
2. Open Android Studio → **Settings** → **Plugins**
3. Click the **⚙️** gear icon → **Install Plugin from Disk...**
4. Select the downloaded `.zip` file
5. Restart Android Studio

### Build from source

```bash
git clone https://github.com/alexiyous/kmp-extract-string-plugin.git
cd kmp-extract-string-plugin
./gradlew buildPlugin
```

The output `.zip` will be at `build/distributions/`.

---

## Usage

1. Place your cursor inside any hardcoded string literal in a `.kt` file:
   ```kotlin
   Text("Pick Location")
   //    ^cursor here
   ```

2. Press `Alt+Enter` (Windows/Linux) or `Option+Enter` (Mac)

3. Select **"Extract to string resource (KMP/Android)"** from the intention menu

4. Enter a key name in the dialog (pre-filled as `pick_location`):

   <img width="738" height="335" alt="image" src="https://github.com/user-attachments/assets/b36d396a-3268-4814-a95c-25313a3c15f4" />

5. The plugin writes to `strings.xml` and replaces the literal:

   **Inside a `@Composable`:**
   ```kotlin
   // Before
   Text("Pick Location")

   // After — KMP
   Text(stringResource(Res.string.pick_location))

   // After — Android
   Text(stringResource(R.string.pick_location))
   ```

   **Outside a `@Composable`** (ViewModel, repository, etc.):
   ```kotlin
   // Before
   val error = "Pick Location"

   // After — KMP (StringResource reference, resolve as needed)
   val error = Res.string.pick_location

   // After — Android
   val error = R.string.pick_location
   ```

   Imports are added automatically.

### Conflict handling

If the key already exists in `strings.xml` with a **different value**, a diff preview dialog appears:
<img width="743" height="471" alt="image" src="https://github.com/user-attachments/assets/90b58076-b49c-4e1c-8820-ee980f4a096d" />


---

## Compatibility

| Item | Value |
|---|---|
| Android Studio | Hedgehog (2023.1) and later |
| Kotlin Plugin | K1 and K2 mode |
| Build range | 243+ (no upper bound) |

---

## Project type detection

The plugin scans the module's content roots for:

| Path | Detected as |
|---|---|
| `composeResources/values/strings.xml` | KMP |
| `res/values/strings.xml` | Android |

If both exist in the same module (common in KMP projects with `androidMain`), KMP takes priority.

---

## License
MIT License - see [LICENSE](https://github.com/alexiyous/kmp-extract-string-plugin/blob/master/LICENSE) for details.

