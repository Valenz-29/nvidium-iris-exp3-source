# Nvidium Iris Experimental — unofficial source patch

> **Unofficial community modification.** This repository is not an official Nvidium or Iris release and does not claim ownership of the original Nvidium project.

This repository publishes the source-level changes and patches corresponding to the experimental Nvidium + Iris modification for Minecraft 26.1.x.

## Attribution

- **Original project:** [Nvidium](https://github.com/MCRcortex/nvidium)
- **Original author:** Cortex / MCRcortex
- **Additional upstream contributor listed by the mod:** drouarb
- **Upstream base commit:** `9cb209d86b53c242de47fbb3e56eda68085f87d8`
- **Original license:** GNU Lesser General Public License v3.0
- **Status:** Unofficial and unsupported community modification

See [`NOTICE.md`](NOTICE.md) for the origin and attribution statement.

## Patches

1. [`0001-nvidium-iris-exp3.patch`](patches/0001-nvidium-iris-exp3.patch) adds the experimental Iris framebuffer bridge.
2. [`0002-adaptive-ram-staging.patch`](patches/0002-adaptive-ram-staging.patch) adds adaptive RAM-backed geometry upload staging and disables the Iris bridge after its first reflective failure.

The second patch is experimental and should be tested before being used in a public modpack release.

## Apply and build

Linux, macOS, Git Bash, or WSL:

```bash
git clone https://github.com/MCRcortex/nvidium.git nvidium-iris-exp3
cd nvidium-iris-exp3
git checkout 9cb209d86b53c242de47fbb3e56eda68085f87d8
git switch -c iris-exp3-source
git apply /path/to/patches/0001-nvidium-iris-exp3.patch
git apply /path/to/patches/0002-adaptive-ram-staging.patch
./gradlew build
```

The resulting JAR is written to `build/libs/` when the upstream build completes successfully.

## Adaptive upload staging

Nvidium's upstream renderer uses a fixed 32 MB persistent client-mapped buffer to upload rebuilt chunk geometry from the CPU to the GPU. The experimental patch sizes that buffer from 32 to 512 MiB according to the configured JVM maximum heap:

- 4 GiB maximum heap: 256 MiB staging.
- 6 GiB maximum heap: 384 MiB staging.
- 8 GiB or more: 512 MiB staging.

The staging allocation is native/client-mapped memory, not Java heap and not VRAM. Its purpose is to absorb bursty geometry uploads and reduce cases where the renderer has to perform a blocking GPU wait.

The F3/debug display adds:

```text
Upload staging: <size> MiB, forced waits: <count>
```

For a meaningful comparison, use the same world and route with the same render distance, shader and FPS limit. A lower `forced waits` count and smoother frame-time graph are the intended success criteria. Higher average FPS is not guaranteed.

## Configuration

The generated `config/nvidium-config.json` supports:

```json
{
  "automatic_upload_buffer_memory": true,
  "upload_buffer_memory": 256
}
```

When automatic sizing is enabled, the manual value is ignored. Manual values are clamped to 32–512 MiB.

## License and redistribution

Nvidium is distributed under the GNU LGPL v3.0. This repository retains the upstream license text, attribution, exact base revision, modification source, patches, and build instructions.

When redistributing a modified binary, keep the license and attribution available and provide a public link to this repository as the corresponding modification source.
