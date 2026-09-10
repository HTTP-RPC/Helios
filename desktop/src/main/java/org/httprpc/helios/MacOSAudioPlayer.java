// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.file.Path;

public class MacOSAudioPlayer implements AudioPlayer {
    public interface Foundation extends Library {
        Foundation instance = Native.load(Foundation.class.getSimpleName(), Foundation.class);

        Pointer objc_getClass(String name);
        Pointer sel_registerName(String str);

        Pointer class_createInstance(Pointer type, int extraBytes);

        Object objc_msgSend(Pointer self, Pointer op, Object... args);

        void object_dispose(Pointer obj);
    }

    public interface AVFoundation extends Library {
        AVFoundation instance = Native.load(AVFoundation.class.getSimpleName(), AVFoundation.class);

        interface AVAudioPlayer {
            Pointer type = Foundation.instance.objc_getClass("AVAudioPlayer");

            Pointer initializer = Foundation.instance.sel_registerName("initWithContentsOfURL:error:");

            Pointer playAtTimeSelector = Foundation.instance.sel_registerName("playAtTime:");
            Pointer pauseSelector = Foundation.instance.sel_registerName("pause");
            Pointer stopSelector = Foundation.instance.sel_registerName("stop");
            Pointer playingSelector = Foundation.instance.sel_registerName("playing");
            Pointer currentTimeSelector = Foundation.instance.sel_registerName("currentTime");
            Pointer setCurrentTimeSelector = Foundation.instance.sel_registerName("setCurrentTime");
            Pointer durationSelector = Foundation.instance.sel_registerName("duration");
        }
    }

    private Pointer instance;

    public MacOSAudioPlayer(Path contentPath) {
        instance = Foundation.instance.class_createInstance(AVFoundation.AVAudioPlayer.type, 0);

        // TODO Call initializer
    }

    @Override
    public void playFrom(double position) {
        Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.playAtTimeSelector, position);
    }

    @Override
    public void pause() {
        Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.pauseSelector);
    }

    @Override
    public void stop() {
        Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.stopSelector);
    }

    @Override
    public boolean isPlaying() {
        return (Boolean)Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.playingSelector);
    }

    @Override
    public double getPosition() {
        return (Double)Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.currentTimeSelector);
    }

    @Override
    public void setPosition(double position) {
        Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.setCurrentTimeSelector, position);
    }

    @Override
    public double getDuration() {
        return (Double)Foundation.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.durationSelector);
    }

    @Override
    public void dispose() {
        Foundation.instance.object_dispose(instance);
    }
}
