cmake_minimum_required(VERSION 3.22.1)
project(crobot_native LANGUAGES CXX)

add_library(crobot_native SHARED
    jni_bridge.cpp
    smb_format_decoder.cpp)

find_library(log-lib log)
find_library(android-lib android)

target_include_directories(crobot_native PRIVATE ${CMAKE_CURRENT_SOURCE_DIR}/third_party)

target_compile_features(crobot_native PRIVATE cxx_std_17)

target_link_libraries(crobot_native
    ${log-lib}
    ${android-lib})
