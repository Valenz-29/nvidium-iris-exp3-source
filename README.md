# Nvidium Iris Experimental — unofficial source patch

> **Unofficial community modification.** This repository is not an official Nvidium or Iris release and does not claim ownership of the original Nvidium project.

This repository publishes the source-level changes and patch corresponding to the experimental binary:

`nvidium-0.4.4-beta2-26.1-iris-exp3.jar`

The modification adds an experimental Iris framebuffer bridge to Nvidium for Minecraft 26.1.x.

## Attribution

- **Original project:** [Nvidium](https://github.com/MCRcortex/nvidium)
- **Original author:** Cortex / MCRcortex
- **Additional upstream contributor listed by the mod:** drouarb
- **Upstream base commit:** `9cb209d86b53c242de47fbb3e56eda68085f87d8`
- **Original license:** GNU Lesser General Public License v3.0
- **Modification:** Experimental Iris framebuffer compatibility bridge
- **Status:** Unofficial and unsupported community modification

See [`NOTICE.md`](NOTICE.md) for the origin and attribution statement.

## Repository contents

- Modified source files under [`src/`](src/).
- Patch against the exact upstream Nvidium commit under [`patches/`](patches/).
- Upstream LGPL-3.0 license text.
- Attribution and upstream revision information.
- Suggested external-file permission text for Modrinth.

## Apply the patch and build

Linux, macOS, Git Bash, or WSL:

```bash
git clone https://github.com/MCRcortex/nvidium.git nvidium-iris-exp3
cd nvidium-iris-exp3
git checkout 9cb209d86b53c242de47fbb3e56eda68085f87d8
git switch -c iris-exp3-source
git apply /path/to/patches/0001-nvidium-iris-exp3.patch
./gradlew build
```

The resulting JAR is written to `build/libs/` when the upstream build completes successfully.

## Modification behavior

The patch changes Nvidium's Sodium world-renderer integration so it:

1. Records the active Sodium terrain render pass.
2. Resolves Iris pipeline and framebuffer objects through reflection.
3. Binds the Iris framebuffer for the current terrain pass.
4. Falls back to the original framebuffer binding when Iris is absent or the bridge fails.

The exact development history of the experimental binary is not claimed by this repository. The repository documents the modified source-level changes against the identified upstream revision.

## License and redistribution

Nvidium is distributed under the GNU LGPL v3.0. This repository retains the upstream license text, attribution, exact base revision, modification source, patch, and build instructions.

When redistributing the modified binary, keep the license and attribution available and provide a public link to this repository as the corresponding modification source.

