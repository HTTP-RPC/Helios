xcodebuild -scheme Helios -derivedDataPath ./build-macos

cp build-macos/Build/Products/Debug/libHelios.dylib src/main/resources/helios.dylib
