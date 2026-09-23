cmake_minimum_required(VERSION 3.18)

project(fcitx5-android-plugin-mozc VERSION ${VERSION_NAME})

# For reproducible build
add_link_options("LINKER:--hash-style=gnu,--build-id=none")

# prefab dependency
find_package(fcitx5 REQUIRED CONFIG)
get_target_property(FCITX5_CMAKE_MODULES fcitx5::cmake INTERFACE_INCLUDE_DIRECTORIES)
set(CMAKE_MODULE_PATH ${FCITX5_CMAKE_MODULES} ${CMAKE_MODULE_PATH})

find_package(ECM MODULE)
find_package(Fcitx5Core MODULE)
find_package(Fcitx5Module MODULE)

include_directories(mozc
        "${CMAKE_CURRENT_SOURCE_DIR}/fcitx5-mozc"
        "${PROJECT_SOURCE_DIR}/../../../../../lib/fcitx5/build/headers/usr/include"
        "${PROJECT_SOURCE_DIR}/../../../../../lib/fcitx5/build/headers/usr/include/Fcitx5/Module"
)

# Cross-build: protoc must run on the host, not on the target ABI.
set(PROTOC_EXECUTABLE "$ENV{HOME}/.local/bin/protoc" CACHE FILEPATH "host protoc" FORCE)
set(protobuf_BUILD_PROTOC_BINARIES OFF)

set(MOZC_TARGET_BUILD ON)
add_subdirectory(fcitx5-mozc)

target_link_libraries(mozc log utf8_validity)
