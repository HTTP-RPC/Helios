/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.httprpc.helios;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class WindowsAudioPlayer implements AudioPlayer {
    private long handle;

    private static final String LIBRARY_NAME = "helios.dll";

    static {
        var jniPath = MusicLibrary.getRootDirectory().resolve("jni");

        try {
            Files.createDirectories(jniPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var libraryPath = jniPath.resolve(LIBRARY_NAME);

        try (var inputStream = WindowsAudioPlayer.class.getResourceAsStream(String.format("/%s", LIBRARY_NAME))) {
            Files.copy(inputStream, libraryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        System.load(libraryPath.toString());

        initialize();
    }

    private static native void initialize();

    public WindowsAudioPlayer(Path contentPath) {
        handle = allocate(contentPath.toString());
    }

    private native long allocate(String contentPath);

    @Override
    public void play() {
        play(handle);
    }

    private native void play(long handle);

    @Override
    public void pause() {
        pause(handle);
    }

    private native void pause(long handle);

    @Override
    public boolean isPlaying() {
        return isPlaying(handle);
    }

    private native boolean isPlaying(long handle);

    @Override
    public double getPosition() {
        return getPosition(handle);
    }

    private native double getPosition(long handle);

    @Override
    public void setPosition(double position) {
        setPosition(handle, position);
    }

    private native void setPosition(long handle, double position);

    @Override
    public double getVolume() {
        return getVolume(handle);
    }

    private native double getVolume(long handle);

    @Override
    public void setVolume(double volume) {
        setVolume(handle, volume);
    }

    private native void setVolume(long handle, double volume);

    @Override
    public void dispose() {
        destroy(handle);
    }

    private native void destroy(long handle);
}
