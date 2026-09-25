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

import java.nio.file.Path;

public class WindowsAudioPlayer implements AudioPlayer {
    private boolean playing = false;

    public WindowsAudioPlayer(Path contentPath) {
        // TODO
    }

    @Override
    public void play() {
        // TODO
        playing = true;
    }

    @Override
    public void pause() {
        // TODO
        playing = false;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public double getPosition() {
        // TODO
        return 0.0;
    }

    @Override
    public void setPosition(double position) {
        // TODO
    }

    @Override
    public double getVolume() {
        // TODO
        return 0.0;
    }

    @Override
    public void setVolume(double volume) {
        // TODO
    }

    @Override
    public void dispose() {
        // TODO
    }
}
