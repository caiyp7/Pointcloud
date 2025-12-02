cmake_minimum_required(VERSION 3.22.1)
project(GroundStreamer LANGUAGES C CXX)

# ========= 基础设置 =========
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

add_compile_options(-Wall -Wextra)

# ========= 你的 native 库 =========
add_library(groundstreamer SHARED
        ground/GroundUnit.cpp
        jni/GroundBridge.cpp
)

# 你的本地头文件
target_include_directories(groundstreamer PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/ground
        ${CMAKE_CURRENT_SOURCE_DIR}/jni
)

# ========= Draco（开关可配） =========
option(WITH_DRACO "Enable Draco decoding" ON)

if (WITH_DRACO)
    # 1) 关闭多余组件以加快编译
    set(DRACO_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
    set(DRACO_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)

    # 2) 源/二进制目录
    set(DRACO_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/third_party/draco)
    if (NOT EXISTS ${DRACO_SRC_DIR}/CMakeLists.txt)
        message(FATAL_ERROR "Draco CMakeLists.txt not found at: ${DRACO_SRC_DIR}")
    endif()

    # Gradle 外部构建：每个 ABI/Variant 都有独立 binary 目录
    set(DRACO_BIN_DIR ${CMAKE_BINARY_DIR}/draco_build)

    # 3) 引入 Draco（两参形式，强制把生成物放进 DRACO_BIN_DIR）
    add_subdirectory(${DRACO_SRC_DIR} ${DRACO_BIN_DIR})

    # 4) 给源码宏，便于 #ifdef HAVE_DRACO
    target_compile_definitions(groundstreamer PRIVATE HAVE_DRACO=1)

    # 5) 让编译器能找到“生成头文件”draco_features.h（在 binary 目录）
    target_include_directories(groundstreamer PRIVATE
            ${DRACO_BIN_DIR}       # 使得 <draco/draco_features.h> 命中 ${DRACO_BIN_DIR}/draco/...
            ${DRACO_SRC_DIR}/src   # 源码头（如 <draco/compression/decode.h>）
    )

    # 6) 链接 Draco 库目标（兼容不同命名）
    set(_DRACO_LIB_TARGET "")
    foreach(_cand draco draco_static draco_shared)
        if (TARGET ${_cand})
            set(_DRACO_LIB_TARGET ${_cand})
            break()
        endif()
    endforeach()

    if (_DRACO_LIB_TARGET)
        message(STATUS "Linking Draco target: ${_DRACO_LIB_TARGET}")
        target_link_libraries(groundstreamer PRIVATE ${_DRACO_LIB_TARGET})
    else()
        # 兜底：在构建目录中搜索实际的库文件（libdraco.a/.so），用 IMPORTED 方式链接
        message(STATUS "No Draco CMake library target found; searching files under: ${DRACO_BIN_DIR}")
        unset(DRACO_LIB_FILE CACHE)
        find_library(DRACO_LIB_FILE
                NAMES draco libdraco
                PATHS ${DRACO_BIN_DIR} ${DRACO_BIN_DIR}/lib ${DRACO_BIN_DIR}/Release ${DRACO_BIN_DIR}/Debug
                NO_DEFAULT_PATH
        )
        if (NOT DRACO_LIB_FILE)
            file(GLOB_RECURSE _cand_libs
                    ${DRACO_BIN_DIR}/**/libdraco.a
                    ${DRACO_BIN_DIR}/**/libdraco.so)
            list(LENGTH _cand_libs _n)
            if (_n GREATER 0)
                list(GET _cand_libs 0 DRACO_LIB_FILE)
            endif()
        endif()

        if (DRACO_LIB_FILE)
            message(STATUS "Found Draco library file: ${DRACO_LIB_FILE}")
            add_library(draco_imported STATIC IMPORTED)
            set_target_properties(draco_imported PROPERTIES IMPORTED_LOCATION ${DRACO_LIB_FILE})
            target_link_libraries(groundstreamer PRIVATE draco_imported)
        else()
            get_property(_ALL_TGT DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR} PROPERTY BUILDSYSTEM_TARGETS)
            message(STATUS "Visible CMake targets: ${_ALL_TGT}")
            file(GLOB_RECURSE _all_bin_files RELATIVE ${DRACO_BIN_DIR} ${DRACO_BIN_DIR}/*)
            message(STATUS "Files under ${DRACO_BIN_DIR}: ${_all_bin_files}")
            message(FATAL_ERROR "Draco library NOT found. Ensure third_party/draco 是完整源码且生成了库（不是只有 draco_decoder/encoder 可执行）。")
        endif()
    endif()

    # （可选）调试输出
    message(STATUS "Draco SRC: ${DRACO_SRC_DIR}")
    message(STATUS "Draco BIN: ${DRACO_BIN_DIR}")
endif()

# ========= NDK 系统库 =========
find_library(log-lib     log)
find_library(android-lib android)

target_link_libraries(groundstreamer PRIVATE
        ${log-lib}
        ${android-lib}
)

# ========= 结束提示 =========
message(STATUS "GroundStreamer configured. WITH_DRACO=${WITH_DRACO}")
message(STATUS "CMAKE_BINARY_DIR = ${CMAKE_BINARY_DIR}")
