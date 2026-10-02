# Mozc host tools

Run `bash plugin/mozc/tools/prepare-host.sh` from a checkout with initialized
submodules before building the plugin. Requires a C++20 compiler, CMake 3.22+,
Ninja, Python 3 (also available as `python`), and Bazelisk (`bazelisk` or `bazel`).

The script builds host protoc from the pinned protobuf/Abseil sources into
`plugin/mozc/build/host-protoc/bin` and generates the OSS dictionary with the
Bazel version pinned in Mozc's `.bazeliskrc`. Set
`CMAKE_BUILD_PARALLEL_LEVEL` to change the default of two build jobs.
Generation uses the current dictionary sources, including any intentional
local dictionary additions. No generated binary or dictionary is committed.
