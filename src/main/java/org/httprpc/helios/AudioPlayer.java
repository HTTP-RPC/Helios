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

public abstract class AudioPlayer {
    private long handle;

    protected static void load(String libraryName) {
        var jniPath = MusicLibrary.getRootDirectory().resolve("jni");

        try {
            Files.createDirectories(jniPath);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        var libraryPath = jniPath.resolve(libraryName);

        try (var inputStream = WindowsAudioPlayer.class.getResourceAsStream(String.format("/%s", libraryName))) {
            Files.copy(inputStream, libraryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        System.load(libraryPath.toString());
    }

    public AudioPlayer(Path contentPath) {
        handle = allocate(contentPath.toString());
    }

    protected abstract long allocate(String contentPath);

    public void play() {
        play(handle);
    }

    protected abstract void play(long handle);

    public void pause() {
        pause(handle);
    }

    protected abstract void pause(long handle);

    public boolean isPlaying() {
        return isPlaying(handle);
    }

    protected abstract boolean isPlaying(long handle);

    public double getPosition() {
        return getPosition(handle);
    }

    protected abstract double getPosition(long handle);

    public void setPosition(double position) {
        setPosition(handle, position);
    }

    protected abstract void setPosition(long handle, double position);

    public double getVolume() {
        return getVolume(handle);
    }

    protected abstract double getVolume(long handle);

    public void setVolume(double volume) {
        setVolume(handle, volume);
    }

    protected abstract void setVolume(long handle, double volume);

    public void dispose() {
        destroy(handle);
    }

    protected abstract void destroy(long handle);

    static AudioPlayer create(Path contentPath) {
        if (Files.exists(contentPath)) {
            if (Helios.isMacOS()) {
                return new MacOSAudioPlayer(contentPath);
            } else {
                return new WindowsAudioPlayer(contentPath);
            }
        } else {
            return new AudioPlayer(contentPath) {
                @Override
                protected long allocate(String contentPath) {
                    return 0;
                }

                @Override
                protected void play(long handle) {
                    // No-op
                }

                @Override
                protected void pause(long handle) {
                    // No-op
                }

                @Override
                protected boolean isPlaying(long handle) {
                    return false;
                }

                @Override
                protected double getPosition(long handle) {
                    return 0;
                }

                @Override
                protected void setPosition(long handle, double position) {
                    // No-op
                }

                @Override
                protected double getVolume(long handle) {
                    return 0;
                }

                @Override
                protected void setVolume(long handle, double volume) {
                    // No-op
                }

                @Override
                protected void destroy(long handle) {
                    // No-op
                }
            };
        }
    }
}
