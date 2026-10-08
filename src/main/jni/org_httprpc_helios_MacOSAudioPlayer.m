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

JNIEXPORT jlong JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_allocate
  (JNIEnv *env, jobject instance, jstring contentPath) {
    // TODO
    return 0;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_play
  (JNIEnv *env, jobject instance, jlong handle) {
  // TODO
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_pause
  (JNIEnv *env, jobject instance, jlong handle) {
    // TODO
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_isPlaying
  (JNIEnv *env, jobject instance, jlong handle) {
    // TODO
    return 0;
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_getPosition
  (JNIEnv *env, jobject instance, jlong handle) {
    // TODO
    return 0.0;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_setPosition
  (JNIEnv *env, jobject instance, jlong handle, jdouble position) {
    // TODO
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_getVolume
  (JNIEnv *env, jobject instance, jlong handle) {
    // TODO
    return 0.0;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_setVolume
  (JNIEnv *env, jobject instance, jlong handle, jdouble volume) {
    // TODO
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_destroy
  (JNIEnv *env, jobject instance, jlong handle) {
    // TODO
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_MacOSAudioPlayer_isDarkMode
  (JNIEnv *env, jclass type) {
    // TODO
    return 0;
}
