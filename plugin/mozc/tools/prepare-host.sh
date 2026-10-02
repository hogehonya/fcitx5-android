#!/usr/bin/env bash
set -euo pipefail

tools_dir=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
plugin_dir=$(cd -- "$tools_dir/.." && pwd)
host_build="$plugin_dir/build/host-protoc"
jobs=${CMAKE_BUILD_PARALLEL_LEVEL:-2}

cmake -S "$tools_dir/host-protoc" -B "$host_build" -G Ninja -DCMAKE_BUILD_TYPE=Release
cmake --build "$host_build" --target protoc --parallel "$jobs"
"$host_build/bin/protoc" --version

cd "$plugin_dir/src/main/cpp/fcitx5-mozc/mozc/src"
bazel build --jobs="$jobs" //data_manager/oss:mozc_dataset_for_oss@header
# install can replace a read-only copy from an earlier Bazel build.
install -m 644 bazel-bin/data_manager/oss/mozc_data.inc data_manager/oss/mozc_data.inc
