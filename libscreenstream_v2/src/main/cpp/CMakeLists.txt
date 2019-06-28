# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

add_library(
        # Sets the name of the library.
        screenstream
        # Sets the library as a shared library.
        SHARED
        # Provides a relative path to your source file(s).
        native_lib.c
)

target_link_libraries(
        screenstream
        log android EGL GLESv3)