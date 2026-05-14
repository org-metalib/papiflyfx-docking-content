# papiflyfx-docking-content

Extracted from the PapiflyFX Docking monorepo.

## Modules

- `papiflyfx-docking-tree`
- `papiflyfx-docking-media`
- `papiflyfx-docking-hugo`
- `papiflyfx-docking-github`

## Build

Use the split-local Maven repository so cross-repo snapshots resolve from the extraction workspace:

```bash
./mvnw -Dmaven.repo.local=$HOME/github/papiflyfx/.m2-split -Dtestfx.headless=true clean verify
```

Lead agent: `@feature-dev`.

## Notes

- The GitHub module keeps its managed JGit dependency unchanged.
