#!/usr/bin/env bash
set -euo pipefail

tools_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
plugin_dir=$(cd -- "$tools_dir/.." && pwd)
host_build="$plugin_dir/build/host-protoc"
jobs=${CMAKE_BUILD_PARALLEL_LEVEL:-2}

# Nix exposes the launcher as bazelisk; setup-bazel also provides bazel.
if command -v bazelisk >/dev/null; then
    bazel_cmd=bazelisk
elif command -v bazel >/dev/null; then
    bazel_cmd=bazel
else
    printf '%s\n' 'Install Bazelisk before preparing Mozc host tools.' >&2
    exit 1
fi
cd "$plugin_dir/src/main/cpp/fcitx5-mozc/mozc/src"
"$bazel_cmd" --version

cmake -S "$tools_dir/host-protoc" -B "$host_build" -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build "$host_build" --target protoc --parallel "$jobs"
"$host_build/bin/protoc" --version

"$bazel_cmd" build --jobs="$jobs" //data_manager/oss:mozc_dataset_for_oss@header
# install can replace a read-only copy from an earlier Bazel build.
install -m 644 bazel-bin/data_manager/oss/mozc_data.inc data_manager/oss/mozc_data.inc
