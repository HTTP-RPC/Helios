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

#include <jni.h>
#include "org_httprpc_helios_MacOSAudioPlayer.h"

@import AVFoundation;

JNIEXPORT jlong JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_allocate
  (JNIEnv *env, jobject instance, jstring contentPath) {
    const char *contentPathChars = (*env)->GetStringUTFChars(env, contentPath, 0);

    id url = [NSURL URLWithString:[NSString stringWithUTF8String:contentPathChars]];

    (*env)->ReleaseStringUTFChars(env, contentPath, contentPathChars);

    id audioPlayer = [[AVAudioPlayer alloc] initWithContentsOfURL:url error:nil];

    return (jlong)CFBridgingRetain(audioPlayer);
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_play
  (JNIEnv *env, jobject instance, jlong handle) {
    [(__bridge AVAudioPlayer *)((CFTypeRef)handle) play];
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_pause
  (JNIEnv *env, jobject instance, jlong handle) {
    [(__bridge AVAudioPlayer *)((CFTypeRef)handle) pause];
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_isPlaying
  (JNIEnv *env, jobject instance, jlong handle) {
    return [(__bridge AVAudioPlayer *)((CFTypeRef)handle) isPlaying];
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_getPosition
  (JNIEnv *env, jobject instance, jlong handle) {
    return [(__bridge AVAudioPlayer *)((CFTypeRef)handle) currentTime];
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_setPosition
  (JNIEnv *env, jobject instance, jlong handle, jdouble position) {
    [(__bridge AVAudioPlayer *)((CFTypeRef)handle) setCurrentTime:position];
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_getVolume
  (JNIEnv *env, jobject instance, jlong handle) {
    return [(__bridge AVAudioPlayer *)((CFTypeRef)handle) volume];
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_setVolume
  (JNIEnv *env, jobject instance, jlong handle, jdouble volume) {
    [(__bridge AVAudioPlayer *)((CFTypeRef)handle) setVolume:volume];
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_destroy
  (JNIEnv *env, jobject instance, jlong handle) {
    CFRelease((CFTypeRef)handle);
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_isDarkMode
  (JNIEnv *env, jclass type) {
    return [[[[NSUserDefaults standardUserDefaults] stringForKey:@"AppleInterfaceStyle"] lowercaseString] isEqualTo:@"dark"];
}
