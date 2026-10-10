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

import com.sun.jna.FunctionMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

import java.nio.file.Path;

import static org.httprpc.kilo.util.Collections.*;
import static org.httprpc.kilo.util.Optionals.*;

public class MacOSx64AudioPlayer extends AudioPlayer {
    public interface ObjectiveCRuntime extends Library {
        ObjectiveCRuntime instance = Native.load("objc", ObjectiveCRuntime.class, mapOf(
            entry(Library.OPTION_FUNCTION_MAPPER, (FunctionMapper)(library, method) -> {
                var name = method.getName();

                if (name.equals("objc_msgSend_float")
                    || name.equals("objc_msgSend_double")
                    || name.equals("objc_msgSend_String")) {
                    return "objc_msgSend";
                } else {
                    return method.getName();
                }
            })
        ));

        Pointer objc_getClass(String name);

        Pointer sel_registerName(String name);

        long objc_msgSend(Pointer self, Pointer selector);
        long objc_msgSend(Pointer self, Pointer selector, Object arg);
        long objc_msgSend(Pointer self, Pointer selector, Object arg1, Object arg2);

        float objc_msgSend_float(Pointer self, Pointer selector);

        double objc_msgSend_double(Pointer self, Pointer selector);

        String objc_msgSend_String(Pointer self, Pointer selector);

        Pointer alloc = instance.sel_registerName("alloc");
        Pointer release = instance.sel_registerName("release");

        static Pointer alloc(Pointer type) {
            return new Pointer(instance.objc_msgSend(type, alloc));
        }

        static void release(Pointer self) {
            instance.objc_msgSend(self, release);
        }
    }

    public interface Foundation extends Library {
        Foundation instance = Native.load(Foundation.class.getSimpleName(), Foundation.class);

        interface NSString {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSString");

            Pointer stringWithUTF8String_ = ObjectiveCRuntime.instance.sel_registerName("stringWithUTF8String:");

            Pointer UTF8String = ObjectiveCRuntime.instance.sel_registerName("UTF8String");
        }

        interface NSURL {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSURL");

            Pointer fileURLWithPath_ = ObjectiveCRuntime.instance.sel_registerName("fileURLWithPath:");
        }

        interface NSUserDefaults {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSUserDefaults");

            Pointer standardUserDefaults = ObjectiveCRuntime.instance.sel_registerName("standardUserDefaults");

            Pointer stringForKey_ = ObjectiveCRuntime.instance.sel_registerName("stringForKey:");
        }
    }

    public interface AVFoundation extends Library {
        AVFoundation instance = Native.load(AVFoundation.class.getSimpleName(), AVFoundation.class);

        interface AVAudioPlayer {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("AVAudioPlayer");

            Pointer initWithContentsOfURL_Error_ = ObjectiveCRuntime.instance.sel_registerName("initWithContentsOfURL:error:");

            Pointer play = ObjectiveCRuntime.instance.sel_registerName("play");
            Pointer pause = ObjectiveCRuntime.instance.sel_registerName("pause");
            Pointer isPlaying = ObjectiveCRuntime.instance.sel_registerName("isPlaying");

            Pointer currentTime = ObjectiveCRuntime.instance.sel_registerName("currentTime");
            Pointer setCurrentTime_ = ObjectiveCRuntime.instance.sel_registerName("setCurrentTime:");

            Pointer volume = ObjectiveCRuntime.instance.sel_registerName("volume");
            Pointer setVolume_ = ObjectiveCRuntime.instance.sel_registerName("setVolume:");
        }
    }

    private Pointer audioPlayer;

    public MacOSx64AudioPlayer(Path contentPath) {
        super(contentPath);
    }

    @Override
    protected long allocate(String contentPath) {
        var urlString = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSString.type,
            Foundation.NSString.stringWithUTF8String_,
            contentPath));

        var url = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSURL.type,
            Foundation.NSURL.fileURLWithPath_,
            urlString));

        audioPlayer = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(ObjectiveCRuntime.alloc(AVFoundation.AVAudioPlayer.type),
            AVFoundation.AVAudioPlayer.initWithContentsOfURL_Error_,
            url, null));

        return 0;
    }

    @Override
    protected void play(long handle) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.play);
    }

    @Override
    protected void pause(long handle) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.pause);
    }

    @Override
    protected boolean isPlaying(long handle) {
        return ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.isPlaying) > 0;
    }

    @Override
    protected double getPosition(long handle) {
        return ObjectiveCRuntime.instance.objc_msgSend_double(audioPlayer, AVFoundation.AVAudioPlayer.currentTime);
    }

    @Override
    protected void setPosition(long handle, double position) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setCurrentTime_, position);
    }

    @Override
    protected double getVolume(long handle) {
        return ObjectiveCRuntime.instance.objc_msgSend_float(audioPlayer, AVFoundation.AVAudioPlayer.volume);
    }

    @Override
    protected void setVolume(long handle, double volume) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setVolume_, (float)volume);
    }

    @Override
    protected void destroy(long handle) {
        ObjectiveCRuntime.release(audioPlayer);
    }

    public static boolean isDarkMode() {
        var standardUserDefaults = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSUserDefaults.type,
            Foundation.NSUserDefaults.standardUserDefaults));

        var key = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(Foundation.NSString.type,
            Foundation.NSString.stringWithUTF8String_,
            "AppleInterfaceStyle"));

        var interfaceStyle = new Pointer(ObjectiveCRuntime.instance.objc_msgSend(standardUserDefaults,
            Foundation.NSUserDefaults.stringForKey_, key));

        return coalesce(map(ObjectiveCRuntime.instance.objc_msgSend_String(interfaceStyle, Foundation.NSString.UTF8String),
            value -> value.equalsIgnoreCase("dark")), () -> false);
    }
}
