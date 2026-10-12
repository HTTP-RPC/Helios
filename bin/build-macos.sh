xcodebuild -scheme Helios -configuration Release -derivedDataPath ./build-macos

cp build-macos/Build/Products/Release/libHelios.dylib src/main/resources/helios.dylib
