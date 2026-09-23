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

package org.httprpc.helios.macos;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

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
