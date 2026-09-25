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
    private long instance;

    static {
        System.loadLibrary("helios-windows");
    }

    public WindowsAudioPlayer(Path contentPath) {
        instance = construct(contentPath.toString());
    }

    private native long construct(String contentPath);

    @Override
    public void play() {
        play(instance);
    }

    private native void play(long instance);

    @Override
    public void pause() {
        pause(instance);
    }

    private native void pause(long instance);

    @Override
    public boolean isPlaying() {
        return isPlaying(instance);
    }

    private native boolean isPlaying(long instance);

    @Override
    public double getPosition() {
        return getPosition(instance);
    }

    private native double getPosition(long instance);

    @Override
    public void setPosition(double position) {
        setPosition(instance, position);
    }

    private native void setPosition(double instance, double position);

    @Override
    public double getVolume() {
        return getVolume(instance);
    }

    private native double getVolume(long instance);

    @Override
    public void setVolume(double volume) {
        setVolume(instance, volume);
    }

    private native void setVolume(long instance, double volume);

    @Override
    public void dispose() {
        dispose(instance);
    }

    private native void dispose(long instance);
}
