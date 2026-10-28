# Optional natives, all off by default (included after SRCS is globbed). A game that needs one turns it on with
# -DSWITCHGDX_<NAME>=ON; for Box2D and FreeType it also adds gdx-box2d/gdx-freetype to its own dependencies and the
# package to "nonOptimized" in its own Clearwing config (see the README's "Optional modules")
option(SWITCHGDX_BULLET "Bullet physics natives (gdx-bullet)" OFF)
option(SWITCHGDX_BOX2D "Box2D natives (gdx-box2d)" OFF)
option(SWITCHGDX_FREETYPE "FreeType natives (gdx-freetype)" OFF)
option(SWITCHGDX_NET "HTTP requests through libcurl (Gdx.net)" OFF)

if(SWITCHGDX_BULLET)
    add_definitions(-DBT_NO_PROFILE -DBT_USE_INVERSE_DYNAMICS_WITH_BULLET2)
    include_directories(src/switchgdx/bullet/bullet src/switchgdx/bullet/custom src/switchgdx/bullet/extras/Serialize src/switchgdx/bullet/extras)
else()
    list(FILTER SRCS EXCLUDE REGEX "/src/switchgdx/bullet/")
endif()

if(NOT SWITCHGDX_BOX2D)
    list(FILTER SRCS EXCLUDE REGEX "/src/(Box2D|switchgdx/box2d)/")
endif()

if(NOT SWITCHGDX_FREETYPE)
    list(FILTER SRCS EXCLUDE REGEX "/src/switchgdx/FreeType_native\\.cpp$")
endif()

if(SWITCHGDX_NET)
    add_definitions(-DSWITCHGDX_NET)
endif()
