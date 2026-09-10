// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.sun.jna.Platform;

import java.nio.file.Path;

public interface AudioPlayer {
    void play();
    void pause();

    boolean isPlaying();

    double getPosition();
    void setPosition(double position);

    double getDuration();

    void dispose();

    static AudioPlayer create(Path contentPath) {
        if (Platform.isMac()) {
            return new MacOSAudioPlayer(contentPath);
        } else if (Platform.isWindows()) {
            return new WindowsAudioPlayer(contentPath);
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
