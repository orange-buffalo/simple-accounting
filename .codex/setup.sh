#!/usr/bin/env bash
set -euo pipefail

# Codex Cloud environment setup for Simple Accounting.
# Mirrors the GitHub Copilot setup workflow used in CI.

if ! command -v java >/dev/null 2>&1; then
  echo "Java is required (JDK 21). Please ensure it is installed in the Codex environment." >&2
  exit 1
fi

if ! command -v bun >/dev/null 2>&1; then
  echo "Bun is required. Install Bun before running this setup script." >&2
  exit 1
fi

./gradlew --version
bun --version

# Install Playwright
./gradlew installPlaywrightDependencies --console=plain --build-cache

# Build to cache dependencies in Codex cloud
./gradlew assemble --console=plain --build-cache
