@echo off
"C:\\Users\\nmiranda\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\cmake.exe" ^
  "-HD:\\projetos-nicoly\\projetos-android\\testes eu\\DemoOpenCV\\OpenCV\\libcxx_helper" ^
  "-DCMAKE_SYSTEM_NAME=Android" ^
  "-DCMAKE_EXPORT_COMPILE_COMMANDS=ON" ^
  "-DCMAKE_SYSTEM_VERSION=21" ^
  "-DANDROID_PLATFORM=android-21" ^
  "-DANDROID_ABI=x86_64" ^
  "-DCMAKE_ANDROID_ARCH_ABI=x86_64" ^
  "-DANDROID_NDK=C:\\Users\\nmiranda\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358" ^
  "-DCMAKE_ANDROID_NDK=C:\\Users\\nmiranda\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358" ^
  "-DCMAKE_TOOLCHAIN_FILE=C:\\Users\\nmiranda\\AppData\\Local\\Android\\Sdk\\ndk\\28.2.13676358\\build\\cmake\\android.toolchain.cmake" ^
  "-DCMAKE_MAKE_PROGRAM=C:\\Users\\nmiranda\\AppData\\Local\\Android\\Sdk\\cmake\\3.22.1\\bin\\ninja.exe" ^
  "-DCMAKE_LIBRARY_OUTPUT_DIRECTORY=D:\\projetos-nicoly\\projetos-android\\testes eu\\DemoOpenCV\\OpenCV\\build\\intermediates\\cxx\\Debug\\n3u6x172\\obj\\x86_64" ^
  "-DCMAKE_RUNTIME_OUTPUT_DIRECTORY=D:\\projetos-nicoly\\projetos-android\\testes eu\\DemoOpenCV\\OpenCV\\build\\intermediates\\cxx\\Debug\\n3u6x172\\obj\\x86_64" ^
  "-DCMAKE_BUILD_TYPE=Debug" ^
  "-BD:\\projetos-nicoly\\projetos-android\\testes eu\\DemoOpenCV\\OpenCV\\.cxx\\Debug\\n3u6x172\\x86_64" ^
  -GNinja ^
  "-DANDROID_STL=c++_shared"
