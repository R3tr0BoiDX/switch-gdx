cmake_minimum_required(VERSION 3.1)

get_filename_component(PROJECT_NAME ${CMAKE_SOURCE_DIR} NAME)

project(${PROJECT_NAME})

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)

SET(CMAKE_C_RESPONSE_FILE_LINK_FLAG "@")
SET(CMAKE_CXX_RESPONSE_FILE_LINK_FLAG "@")
set(CMAKE_ASM_USE_RESPONSE_FILE_FOR_OBJECTS 1)
set(CMAKE_C_USE_RESPONSE_FILE_FOR_OBJECTS 1)
set(CMAKE_CXX_USE_RESPONSE_FILE_FOR_OBJECTS 1)

set(CMAKE_MODULE_PATH ${CMAKE_SOURCE_DIR})
include(SwitchTools)

add_definitions(-DNOJNI -DEGL_NO_X11 -D_REENTRANT -DASMJIT_STATIC -DBT_NO_PROFILE -DBT_USE_INVERSE_DYNAMICS_WITH_BULLET2)

file(GLOB_RECURSE SRCS src/*.cpp src/*.c)
include_directories(src src/switchgdx/bullet/bullet src/switchgdx/bullet/custom src/switchgdx/bullet/extras/Serialize src/switchgdx/bullet/extras)
add_executable(${PROJECT_NAME} ${SRCS})

target_compile_options(${PROJECT_NAME} PRIVATE -Wno-unused-but-set-variable -Wno-unused-parameter -Wno-unused-variable -Wno-unused-label -Wno-unused-function)

set_target_properties(${PROJECT_NAME} PROPERTIES
        APP_TITLE "$ENV{APP_TITLE}"
        APP_AUTHOR "$ENV{APP_AUTHOR}"
        APP_VERSION "$ENV{APP_VERSION}"
        ROMFS "${CMAKE_SOURCE_DIR}/romfs"
        ICON "${CMAKE_SOURCE_DIR}/icon.jpg")

target_include_directories(${PROJECT_NAME} PUBLIC include ${DEVKITPRO}/libnx/include ${PORTLIBS}/include ${PORTLIBS}/include/freetype2 ${PORTLIBS}/include/SDL2)
# Static portlibs pull in further libraries that change between devkitPro releases (freetype now needs
# harfbuzz, for example), so ask the portlibs pkg-config for their static dependencies instead of only
# hardcoding them. Missing .pc files are skipped and the hardcoded list below still applies.
set(PORTLIBS_PKG_CONFIG "${PORTLIBS}/bin/aarch64-none-elf-pkg-config")
set(PORTLIBS_DEPENDENCIES "")
if(EXISTS ${PORTLIBS_PKG_CONFIG})
    foreach(package freetype2 SDL2_mixer libcurl zziplib)
        execute_process(COMMAND ${PORTLIBS_PKG_CONFIG} --static --libs-only-l ${package}
                OUTPUT_VARIABLE package_libs OUTPUT_STRIP_TRAILING_WHITESPACE RESULT_VARIABLE package_result ERROR_QUIET)
        if(package_result EQUAL 0)
            separate_arguments(package_libs UNIX_COMMAND "${package_libs}")
            list(APPEND PORTLIBS_DEPENDENCIES ${package_libs})
        endif()
    endforeach()
endif()

# Grouped because some portlibs depend on each other (freetype <-> harfbuzz)
target_link_libraries(${PROJECT_NAME} -march=armv8-a+crc+crypto -mtune=cortex-a57 -mtp=soft -fPIC -ftls-model=local-exec -fPIE -L${DEVKITPRO}/libnx/lib -L${PORTLIBS}/lib
        -Wl,--start-group
        -lglad -lEGL -lglapi -ldrm_nouveau -lcurl -lzzip -lSDL2_mixer -lSDL2 -lvorbisidec -lvorbis -lmodplug -logg -lopusfile -lopus -lmpg123 -lpthread -lfreetype
        -lz -lbz2 -lpng ${PORTLIBS_DEPENDENCIES} -lnx -lffi -lstdc++ -lm
        -Wl,--end-group)

add_nro_target(${PROJECT_NAME})
