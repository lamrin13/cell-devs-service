cmake_minimum_required(VERSION 3.23)
project(${project})

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/bin)

add_library(${project} INTERFACE)

target_include_directories(${project} INTERFACE ../cadmium_v2/include)

add_executable(main main.cpp)
target_link_libraries(main ${project})
