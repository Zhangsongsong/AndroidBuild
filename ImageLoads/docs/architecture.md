# ImageLoads Package Structure

ImageLoads currently uses a single Gradle module. Source code is organized by
feature and responsibility so packages can be converted into Gradle modules
later without changing their ownership boundaries again.

## Primary Packages

```text
com.zasko.imageloads/
|-- core/
|   |-- download/       Shared download infrastructure and queue state
|   `-- ui/             Reusable Compose UI and shared detail/download UI
|-- data/
|   `-- model/          Serializable application and network models
|-- feature/
|   |-- download/       Download queue and downloaded-content screens
|   |-- generic/        Configurable source list and detail flow
|   |-- home/           Home screen and home state
|   |-- meizi5/         Meizi5 source feature
|   |-- settings/       Settings, import, and export screens
|   |-- taotu/          TaoTu source feature
|   |-- trendszine/     Trendszine source feature
|   `-- xiuren/         XiuRen source feature and its legacy flow
`-- MainActivity.kt     Application-level feature composition
```

## Ownership Rules

- `feature/*` owns screens, feature state, repositories, and feature-specific
  image models.
- `core/ui` contains UI used by more than one feature. Feature-only composables
  stay in their feature package.
- `core/download` contains download behavior shared by multiple sources.
- `data/model` contains models shared across multiple packages. Model fields are
  not given fallback values outside their existing contracts.
- The application root composes features and handles top-level navigation.

## Compatibility Packages

The existing `adapter`, `base`, `components`, `dialog`, `fragment`, `manager`,
`services`, `ui`, `utils`, and `view` packages still contain legacy XML or
cross-cutting code. They remain in place until each responsibility has a clear
destination and can be moved without mixing a structural change with a behavior
rewrite.

New feature code should not be added to the compatibility packages. Prefer the
closest existing `feature`, `core`, or `data` package.

## Next Extraction Candidates

1. Move HTTP clients and parsing from `components` and `manager` to
   `core/network`.
2. Split source configuration and favorite persistence out of
   `core/ui/common` into data-oriented packages.
3. Move remaining reusable dialogs and legacy views into `core/ui` as they are
   converted or reused.
4. Convert stable package boundaries into Gradle modules only when build-time or
   ownership isolation provides a measurable benefit.
