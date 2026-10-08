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

public class WindowsAudioPlayer extends AudioPlayer {
    static {
        load("helios.dll");

        initialize();
    }

    private static native void initialize();

    public WindowsAudioPlayer(Path contentPath) {
        super(contentPath);
    }

    @Override
    protected native long allocate(String contentPath);

    @Override
    protected native void play(long handle);

    @Override
    protected native void pause(long handle);

    @Override
    protected native boolean isPlaying(long handle);

    @Override
    protected native double getPosition(long handle);

    @Override
    protected native void setPosition(long handle, double position);

    @Override
    protected native double getVolume(long handle);

    @Override
    protected native void setVolume(long handle, double volume);

    @Override
    protected native void destroy(long handle);
}
