// Copyright © 2026 GK Brown/httprpc.org. All rights reserved.

package org.httprpc.helios;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.file.Path;

public class MacOSAudioPlayer implements AudioPlayer {
    public interface ObjectiveCRuntime extends Library {
        ObjectiveCRuntime instance = Native.load("objc.A", ObjectiveCRuntime.class);

        Pointer objc_getClass(String name);

        Pointer sel_registerName(String str);

        Pointer class_createInstance(Pointer type, int extraBytes);

        long objc_msgSend(Pointer self, Pointer op);
        long objc_msgSend(Pointer self, Pointer op, Object arg);

        void object_dispose(Pointer obj);
    }

    public interface Foundation extends Library {
        Foundation instance = Native.load(Foundation.class.getSimpleName(), Foundation.class);

        interface NSString {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSString");

            Pointer factory = ObjectiveCRuntime.instance.sel_registerName("stringWithUTF8String:");
        }

        interface NSURL {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSURL");

            Pointer factory = ObjectiveCRuntime.instance.sel_registerName("URLWithString:");
        }

        int NS_UTF8_STRING_ENCODING = 4;
    }

    public interface AVFoundation extends Library {
        AVFoundation instance = Native.load(AVFoundation.class.getSimpleName(), AVFoundation.class);

        interface AVAudioPlayer {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("AVAudioPlayer");

            Pointer initializer = ObjectiveCRuntime.instance.sel_registerName("initWithContentsOfURL:error:");

            Pointer playAtTimeSelector = ObjectiveCRuntime.instance.sel_registerName("playAtTime:");
            Pointer pauseSelector = ObjectiveCRuntime.instance.sel_registerName("pause");
            Pointer stopSelector = ObjectiveCRuntime.instance.sel_registerName("stop");
            Pointer playingSelector = ObjectiveCRuntime.instance.sel_registerName("playing");
            Pointer currentTimeSelector = ObjectiveCRuntime.instance.sel_registerName("currentTime");
            Pointer setCurrentTimeSelector = ObjectiveCRuntime.instance.sel_registerName("setCurrentTime");
            Pointer durationSelector = ObjectiveCRuntime.instance.sel_registerName("duration");
        }
    }

    private Pointer instance;

    public MacOSAudioPlayer(Path contentPath) {
        var urlString = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSString.type,
            Foundation.NSString.factory,
            contentPath.toAbsolutePath().toString()));

        var url = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSURL.type,
            Foundation.NSURL.factory,
            urlString));

        instance = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(ObjectiveCRuntime.instance.class_createInstance(AVFoundation.AVAudioPlayer.type, 0),
            AVFoundation.AVAudioPlayer.initializer,
            url));
    }

    @Override
    public void playFrom(double position) {
        ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.playAtTimeSelector, position);
    }

    @Override
    public void pause() {
        ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.pauseSelector);
    }

    @Override
    public void stop() {
        ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.stopSelector);
    }

    @Override
    public boolean isPlaying() {
        return ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.playingSelector) > 0;
    }

    @Override
    public double getPosition() {
        return ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.currentTimeSelector);
    }

    @Override
    public void setPosition(double position) {
        ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.setCurrentTimeSelector, position);
    }

    @Override
    public double getDuration() {
        return ObjectiveCRuntime.instance.objc_msgSend(instance, AVFoundation.AVAudioPlayer.durationSelector);
    }

    @Override
    public void dispose() {
        ObjectiveCRuntime.instance.object_dispose(instance);
    }
}
