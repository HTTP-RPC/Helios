msbuild Helios.slnx /p:Configuration=Release

copy /Y build-windows\Release\Helios.dll src\main\resources\helios.dll
