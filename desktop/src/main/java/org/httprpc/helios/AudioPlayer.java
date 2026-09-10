// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import java.nio.file.Path;

public interface AudioPlayer {
    default void play() {
        playFrom(0.0);
    }

    void playFrom(double position);

    void pause();
    void stop();

    boolean isPlaying();

    double getPosition();
    void setPosition(double position);

    double getDuration();

    static AudioPlayer create(Path contentPath) {
        return switch (OperatingSystem.getCurrent()) {
            case MAC_OS -> new MacOSAudioPlayer(contentPath);
            case WINDOWS -> new WindowsAudioPlayer(contentPath);
            default -> throw new UnsupportedOperationException();
        };
    }
}
