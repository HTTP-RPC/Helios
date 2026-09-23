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

import com.sun.jna.Pointer;
import org.httprpc.helios.macos.AVFoundation;
import org.httprpc.helios.macos.Foundation;
import org.httprpc.helios.macos.ObjectiveCRuntime;

import java.nio.file.Path;

public class MacOSAudioPlayer implements AudioPlayer {
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
        return ObjectiveCRuntime.instance.objc_msgSend_double(audioPlayer, AVFoundation.AVAudioPlayer.currentTime);
    }

    @Override
    public void setPosition(double position) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setCurrentTime_, position);
    }

    @Override
    public double getVolume() {
        return ObjectiveCRuntime.instance.objc_msgSend_float(audioPlayer, AVFoundation.AVAudioPlayer.volume);
    }

    @Override
    public void setVolume(double volume) {
        ObjectiveCRuntime.instance.objc_msgSend(audioPlayer, AVFoundation.AVAudioPlayer.setVolume_, (float)volume);
    }

    @Override
    public void dispose() {
        ObjectiveCRuntime.release(audioPlayer);
    }
}
