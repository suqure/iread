@echo off
"D:\\Program Files\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\Android\\voiceread\\modules\\mnn\\src\\main\\cpp" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=26" ^
  "-DANDROID_PLATFORM=android-26" ^
  "-DANDROID_ABI=armeabi-v7a" ^
  "-DCMAKE_ANDROID_ARCH_ABI=armeabi-v7a" ^
  "-DANDROID_NDK=D:\\Program Files\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_ANDROID_NDK=D:\\Program Files\\Android\\Sdk\\ndk\\25.1.8937393" ^
  "-DCMAKE_TOOLCHAIN_FILE=D:\\Program Files\\Android\\Sdk\\ndk\\25.1.8937393\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=D:\\Program Files\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_CXX_FLAGS=-std=c++17" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\Android\\voiceread\\modules\\mnn\\build\\intermediates\\cxx\\RelWithDebInfo\\3r5a5h43\\obj\\armeabi-v7a" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\Android\\voiceread\\modules\\mnn\\build\\intermediates\\cxx\\RelWithDebInfo\\3r5a5h43\\obj\\armeabi-v7a" ^
  "-DCMAKE_BUILD_TYPE=RelWithDebInfo" ^
  "-BD:\\Android\\voiceread\\modules\\mnn\\.cxx\\RelWithDebInfo\\3r5a5h43\\armeabi-v7a" ^
  -GNinja
