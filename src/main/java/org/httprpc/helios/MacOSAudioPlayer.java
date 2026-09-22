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

public class MacOSAudioPlayer implements AudioPlayer {
    private interface ObjectiveCRuntime extends Library {
        ObjectiveCRuntime instance = Native.load("objc.A", ObjectiveCRuntime.class, mapOf(
            entry(Library.OPTION_FUNCTION_MAPPER, (FunctionMapper)(library, method) -> {
                if (method.getName().equals("objc_msgSend_fpret")) {
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

        double objc_msgSend_fpret(Pointer self, Pointer selector);

        Pointer alloc = instance.sel_registerName("alloc");
        Pointer release = instance.sel_registerName("release");

        static Pointer alloc(Pointer type) {
            return new Pointer(instance.objc_msgSend(type, alloc));
        }

        static void release(Pointer self) {
            instance.objc_msgSend(self, release);
        }
    }

    private interface Foundation extends Library {
        interface NSString {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSString");

            Pointer stringWithUTF8String_ = ObjectiveCRuntime.instance.sel_registerName("stringWithUTF8String:");
        }

        interface NSURL {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("NSURL");

            Pointer fileURLWithPath_ = ObjectiveCRuntime.instance.sel_registerName("fileURLWithPath:");
        }
    }

    private interface AVFoundation extends Library {
        interface AVAudioPlayer {
            Pointer type = ObjectiveCRuntime.instance.objc_getClass("AVAudioPlayer");

            Pointer initWithContentsOfURL_Error_ = ObjectiveCRuntime.instance.sel_registerName("initWithContentsOfURL:error:");

            Pointer play = ObjectiveCRuntime.instance.sel_registerName("play");
            Pointer pause = ObjectiveCRuntime.instance.sel_registerName("pause");
            Pointer isPlaying = ObjectiveCRuntime.instance.sel_registerName("isPlaying");

            Pointer currentTime = ObjectiveCRuntime.instance.sel_registerName("currentTime");
            Pointer setCurrentTime = ObjectiveCRuntime.instance.sel_registerName("setCurrentTime:");
        }
    }

    static {
        Native.load(Foundation.class.getSimpleName(), Foundation.class);
        Native.load(AVFoundation.class.getSimpleName(), AVFoundation.class);
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
    public boolean isPlaying() {
        return ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.isPlaying) > 0;
    }

    @Override
    public double getPosition() {
        return ObjectiveCRuntime.instance.objc_msgSend_fpret(audioPlayer, AVFoundation.AVAudioPlayer.currentTime);
    }

    @Override
    public void setPosition(double position) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setCurrentTime, position);
    }

    @Override
    public void dispose() {
        ObjectiveCRuntime.release(audioPlayer);
    }
}
