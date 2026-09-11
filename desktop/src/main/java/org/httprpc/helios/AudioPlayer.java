// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.sun.jna.Platform;

import java.nio.file.Files;
import java.nio.file.Path;

public interface AudioPlayer {
    void play();
    void pause();

    boolean isPlaying();

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
                public void dispose() {
                    // No-op
                }
            };
        }
    }
}
