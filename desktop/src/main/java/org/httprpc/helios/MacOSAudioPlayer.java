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

        long objc_msgSend(Pointer self, Pointer op);
        long objc_msgSend(Pointer self, Pointer op, Object arg);
        long objc_msgSend(Pointer self, Pointer op, Object arg1, Object arg2);

        Pointer alloc = instance.sel_registerName("alloc");

        static Pointer alloc(Pointer type) {
            return new Pointer(instance.objc_msgSend(type, alloc));
        }
    }

    public interface Foundation extends Library {
        Foundation instance = Native.load(Foundation.class.getSimpleName(), Foundation.class);

        interface NSString {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSString");

            Pointer stringWithUTF8String_ = ObjectiveCRuntime.instance.sel_registerName("stringWithUTF8String:");
        }

        interface NSURL {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSURL");

            Pointer fileURLWithPath_ = ObjectiveCRuntime.instance.sel_registerName("fileURLWithPath:");
        }

        int NS_UTF8_STRING_ENCODING = 4;
    }

    public interface AVFoundation extends Library {
        AVFoundation instance = Native.load(AVFoundation.class.getSimpleName(), AVFoundation.class);

        interface AVAudioPlayer {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("AVAudioPlayer");

            Pointer initWithContentsOfURL_Error_ = ObjectiveCRuntime.instance.sel_registerName("initWithContentsOfURL:error:");

            Pointer play = ObjectiveCRuntime.instance.sel_registerName("play");
            Pointer playAtTime_ = ObjectiveCRuntime.instance.sel_registerName("playAtTime:");
            Pointer pause = ObjectiveCRuntime.instance.sel_registerName("pause");
            Pointer stop = ObjectiveCRuntime.instance.sel_registerName("stop");
            Pointer playing = ObjectiveCRuntime.instance.sel_registerName("playing");
            Pointer currentTime = ObjectiveCRuntime.instance.sel_registerName("currentTime");
            Pointer setCurrentTime = ObjectiveCRuntime.instance.sel_registerName("setCurrentTime");
            Pointer duration = ObjectiveCRuntime.instance.sel_registerName("duration");
        }
    }

    private Pointer audioPlayer;

    public MacOSAudioPlayer(Path contentPath) {
        var urlString = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSString.type,
            Foundation.NSString.stringWithUTF8String_,
            contentPath.toString()));

        var url = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSURL.type,
            Foundation.NSURL.fileURLWithPath_,
            urlString));

        audioPlayer = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(ObjectiveCRuntime.alloc(AVFoundation.AVAudioPlayer.type),
            AVFoundation.AVAudioPlayer.initWithContentsOfURL_Error_,
            url, null));
    }

    @Override
    public void play() {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.play);
    }

    @Override
    public void pause() {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.pause);
    }

    @Override
    public void stop() {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.stop);
    }

    @Override
    public boolean isPlaying() {
        return ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.playing) > 0;
    }

    @Override
    public double getPosition() {
        return ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.currentTime);
    }

    @Override
    public void setPosition(double position) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setCurrentTime, position);
    }

    @Override
    public double getDuration() {
        return ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.duration);
    }

    @Override
    public void dispose() {
        // TODO
    }
}
