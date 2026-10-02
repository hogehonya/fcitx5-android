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
find_program(PROTOC_EXECUTABLE protoc
    HINTS "${CMAKE_CURRENT_SOURCE_DIR}/../../../build/host-protoc/bin" "$ENV{HOME}/.local/bin"
    NO_CMAKE_FIND_ROOT_PATH REQUIRED)
set(protobuf_BUILD_PROTOC_BINARIES OFF)

set(MOZC_TARGET_BUILD ON)
add_subdirectory(fcitx5-mozc)

target_link_libraries(mozc log utf8_validity)

# Build patched copies; configuring must not modify the pinned submodule.
set(mozc_android_patch "${CMAKE_CURRENT_SOURCE_DIR}/../../../patches/0002-android-conversion-flow.patch")
set_property(DIRECTORY APPEND PROPERTY CMAKE_CONFIGURE_DEPENDS "${mozc_android_patch}")
set(mozc_android_sources "${CMAKE_CURRENT_BINARY_DIR}/android-sources")
foreach(source mozc_state.cc fcitx_key_translator.cc)
    configure_file("fcitx5-mozc/src/unix/fcitx5/${source}"
        "${mozc_android_sources}/src/unix/fcitx5/${source}" COPYONLY)
endforeach()
find_program(PATCH_EXECUTABLE patch REQUIRED)
execute_process(COMMAND "${PATCH_EXECUTABLE}" --batch --fuzz=0 -p1 -i "${mozc_android_patch}"
    WORKING_DIRECTORY "${mozc_android_sources}"
    RESULT_VARIABLE mozc_patch_result OUTPUT_VARIABLE mozc_patch_output ERROR_VARIABLE mozc_patch_error)
if(NOT mozc_patch_result EQUAL 0)
    message(FATAL_ERROR "Android Mozc patch failed: ${mozc_patch_output}${mozc_patch_error}")
endif()
get_target_property(mozc_sources mozc SOURCES)
list(FILTER mozc_sources EXCLUDE REGEX "(^|/)(mozc_state|fcitx_key_translator)\\.cc$")
set_property(TARGET mozc PROPERTY SOURCES "${mozc_sources}")
target_sources(mozc PRIVATE
    "${mozc_android_sources}/src/unix/fcitx5/mozc_state.cc"
    "${mozc_android_sources}/src/unix/fcitx5/fcitx_key_translator.cc")
# Supersedes the trial build's manual 0001 patch.
target_sources(mozc-static PRIVATE "${CMAKE_CURRENT_SOURCE_DIR}/fcitx5-mozc/mozc/src/base/android_util.cc")
