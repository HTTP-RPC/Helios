// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

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
        // TODO
        return playing;
    }

    @Override
    public int getPosition() {
        // TODO
        return 0;
    }

    @Override
    public int getDuration() {
        // TODO
        return 0;
    }

    @Override
    public void dispose() {
        // TODO
    }
}
