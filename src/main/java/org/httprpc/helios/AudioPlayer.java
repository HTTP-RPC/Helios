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

import com.sun.jna.Platform;

import java.nio.file.Files;
import java.nio.file.Path;

public interface AudioPlayer {
    void play();
    void pause();

    boolean isPlaying();

    double getPosition();
    void setPosition(double position);

    double getVolume();
    void setVolume(double volume);

    void dispose();

    static AudioPlayer create(Path contentPath) {
        if (Files.exists(contentPath)) {
            if (Platform.isMac()) {
                return new MacOSAudioPlayer(contentPath);
            } else if (Platform.isWindows()) {
                return new WindowsAudioPlayer(contentPath);
            } else {
                throw new UnsupportedOperationException();
            }
        } else {
            return new AudioPlayer() {
                @Override
                public void play() {
                    // No-op
                }

                @Override
                public void pause() {
                    // No-op
                }

                @Override
                public boolean isPlaying() {
                    return false;
                }

                @Override
                public double getPosition() {
                    return 0.0;
                }

                @Override
                public void setPosition(double position) {
                    // No-op
                }

                @Override
                public double getVolume() {
                    return 0.0;
                }

                @Override
                public void setVolume(double volume) {
                    // No-op
                }

                @Override
                public void dispose() {
                    // No-op
                }
            };
        }
    }
}
