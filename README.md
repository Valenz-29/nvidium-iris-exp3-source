# Nvidium Iris Experimental — source reconstruction

> **Unofficial community modification.** This repository is not an official Nvidium or Iris release and does not claim ownership of the original Nvidium project.

This repository publishes the reconstructed source-level changes corresponding to the experimental binary:

`nvidium-0.4.4-beta2-26.1-iris-exp3.jar`

The modification adds an experimental Iris framebuffer bridge to Nvidium for Minecraft 26.1.x. The source was reconstructed from the supplied JVM binary and compared against the exact upstream Nvidium source revision identified below.

## Attribution

- **Original project:** [Nvidium](https://github.com/MCRcortex/nvidium)
- **Original author:** Cortex / MCRcortex
- **Additional upstream contributor listed by the mod:** drouarb
- **Upstream base commit:** `9cb209d86b53c242de47fbb3e56eda68085f87d8`
- **Original license:** GNU Lesser General Public License v3.0
- **Modification:** Experimental Iris framebuffer compatibility bridge
- **Status:** Unofficial and unsupported community modification

See [`NOTICE.md`](NOTICE.md) for the full origin and attribution statement.

## Repository contents

- Reconstructed `IrisFramebufferBridge` source.
- Modified `MixinSodiumWorldRenderer` source.
- Modified Fabric metadata and version properties.
- A Git patch against the exact upstream Nvidium commit.
- The upstream LGPL-3.0 license text.
- A reconstruction script and build instructions.
- JVM bytecode evidence used during reconstruction.
- Suggested external-file permission text for Modrinth.

## Reconstruct the complete source tree

Linux, macOS, Git Bash, or WSL:

```bash
chmod +x reconstruct.sh
./reconstruct.sh
```

The script performs the following operations:

1. Clones `MCRcortex/nvidium`.
2. Checks out commit `9cb209d86b53c242de47fbb3e56eda68085f87d8`.
3. Creates the branch `iris-exp3-source`.
4. Applies [`patches/0001-nvidium-iris-exp3.patch`](patches/0001-nvidium-iris-exp3.patch).

Manual equivalent:

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

## Reconstruction accuracy

The bridge source was reconstructed from JVM bytecode, line tables, local-variable information, method signatures, constants, and reflective class and method names. The modified call sites were recovered from bytecode and compared with the exact upstream source file.

Comments and formatting not preserved in the compiled class cannot be guaranteed to match the original handwritten source. The behavior and class structure are reconstructed from the supplied binary.

## License and redistribution

Nvidium is distributed under the GNU LGPL v3.0. This repository retains the upstream license text, attribution, exact base revision, modification source, patch, and reconstruction instructions.

When redistributing the modified binary, keep the license and attribution available and provide a public link to this repository as the corresponding modification source.

## Modrinth

For Modrinth's **Unknown external content** review, select **License** and use the text in [`MODRINTH_PERMISSION_NOTES.md`](MODRINTH_PERMISSION_NOTES.md), replacing the repository placeholder with this repository's public URL.
