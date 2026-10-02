cmake_minimum_required(VERSION 3.22)
project(mozc_host_protoc LANGUAGES C CXX)

# Use the same pinned protobuf and Abseil as the Android library.
set(CMAKE_CXX_STANDARD 20)
set(protobuf_BUILD_TESTS OFF)
set(protobuf_INSTALL OFF)
set(protobuf_BUILD_LIBUPB ON)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY "${CMAKE_BINARY_DIR}/bin")
get_filename_component(MOZC_SOURCE "${CMAKE_CURRENT_LIST_DIR}/../../src/main/cpp/fcitx5-mozc" ABSOLUTE)
add_subdirectory("${MOZC_SOURCE}/abseil-cpp" abseil-cpp)
add_subdirectory("${MOZC_SOURCE}/protobuf" protobuf)
