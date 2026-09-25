cmake_minimum_required(VERSION 3.15)
project(helios-windows LANGUAGES CXX)

find_package(JNI REQUIRED)

include_directories(
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
    ${CMAKE_CURRENT_SOURCE_DIR}/../../../build/generated/sources/headers/java/main
)

add_library(helios-windows SHARED org_httprpc_helios_WindowsAudioPlayer.cpp)
