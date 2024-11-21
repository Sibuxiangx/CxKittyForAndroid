cmake_minimum_required(VERSION 3.22.1)

project("termux-bootstrap")

enable_language(ASM)


set(SOURCE_FILE "${CMAKE_SOURCE_DIR}/bootstrap-aarch64.zip")
set(DESTINATION_FILE "${CMAKE_BINARY_DIR}/bootstrap-aarch64.zip")
configure_file(${SOURCE_FILE} ${DESTINATION_FILE} COPYONLY)
set(SOURCE_FILE "${CMAKE_SOURCE_DIR}/bootstrap-x86_64.zip")
set(DESTINATION_FILE "${CMAKE_BINARY_DIR}/bootstrap-x86_64.zip")
configure_file(${SOURCE_FILE} ${DESTINATION_FILE} COPYONLY)

add_library(${CMAKE_PROJECT_NAME} SHARED
    xxt.cpp
        termux-bootstrap-zip.S
        termux-bootstrap.c)

target_link_libraries(${CMAKE_PROJECT_NAME}
    android
    log)
