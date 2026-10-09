msbuild Helios.slnx /p:Configuration=Debug

copy /Y build-windows\Debug\Helios.dll src\main\resources\helios.dll
