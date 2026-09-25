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

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.nio.file.Path;

public class JavaFXAudioPlayer implements AudioPlayer {
    private MediaPlayer mediaPlayer;

    private boolean playing = false;

    public JavaFXAudioPlayer(Path contentPath) {
        mediaPlayer = new MediaPlayer(new Media(contentPath.toUri().toString()));
    }

    @Override
    public void play() {
        mediaPlayer.play();

        playing = true;
    }

    @Override
    public void pause() {
        mediaPlayer.pause();

        playing = false;
    }

    @Override
    public boolean isPlaying() {
        return playing;
    }

    @Override
    public double getPosition() {
        return mediaPlayer.getCurrentTime().toSeconds();
    }

    @Override
    public void setPosition(double position) {
        mediaPlayer.seek(Duration.seconds(position));
    }

    @Override
    public double getVolume() {
        return mediaPlayer.getVolume();
    }

    @Override
    public void setVolume(double volume) {
        mediaPlayer.setVolume(volume);
    }

    @Override
    public void dispose() {
        mediaPlayer.dispose();
    }
}
