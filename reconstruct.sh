#!/usr/bin/env bash
set -euo pipefail

UPSTREAM_URL="https://github.com/MCRcortex/nvidium.git"
UPSTREAM_COMMIT="9cb209d86b53c242de47fbb3e56eda68085f87d8"
DESTINATION="${1:-nvidium-iris-exp3}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

if ! command -v git >/dev/null 2>&1; then
    echo "git is required." >&2
    exit 1
fi

if [[ -e "$DESTINATION" ]]; then
    echo "Destination already exists: $DESTINATION" >&2
    exit 1
fi

git clone "$UPSTREAM_URL" "$DESTINATION"
cd "$DESTINATION"
git checkout "$UPSTREAM_COMMIT"
git switch -c iris-exp3-source
git apply "$SCRIPT_DIR/patches/0001-nvidium-iris-exp3.patch"

echo
echo "Source reconstructed in: $DESTINATION"
echo "Build with: cd '$DESTINATION' && ./gradlew build"
