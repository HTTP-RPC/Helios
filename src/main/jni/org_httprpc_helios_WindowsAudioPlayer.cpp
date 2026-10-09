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

#pragma comment(lib, "runtimeobject.lib")

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

#include <jni.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Storage.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Media.Core.h>
#include <winrt/Windows.Media.Playback.h>
#include "org_httprpc_helios_WindowsAudioPlayer.h"

BOOL APIENTRY DllMain(HMODULE hModule,
    DWORD  ul_reason_for_call,
    LPVOID lpReserved
)
{
    switch (ul_reason_for_call)
    {
    case DLL_PROCESS_ATTACH:
    case DLL_THREAD_ATTACH:
    case DLL_THREAD_DETACH:
    case DLL_PROCESS_DETACH:
        break;
    }
    return TRUE;
}

using namespace winrt::Windows::Storage;
using namespace winrt::Windows::Storage::Streams;
using namespace winrt::Windows::Media::Core;
using namespace winrt::Windows::Media::Playback;

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_initialize
  (JNIEnv *env, jclass type) {
    winrt::init_apartment();
}

JNIEXPORT jlong JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_allocate
  (JNIEnv *env, jobject instance, jstring contentPath) {
    const jchar *contentPathChars = env->GetStringChars(contentPath, nullptr);
    jsize contentPathLength = env->GetStringLength(contentPath);

    winrt::hstring path { reinterpret_cast<const wchar_t*>(contentPathChars), static_cast<uint32_t>(contentPathLength) };

    env->ReleaseStringChars(contentPath, contentPathChars);

    StorageFile storageFile = StorageFile::GetFileFromPathAsync(path).get();
    MediaSource mediaSource = MediaSource::CreateFromStorageFile(storageFile);

    MediaPlayer mediaPlayer;

    mediaPlayer.Source(mediaSource);

    return reinterpret_cast<jlong>(winrt::detach_abi(mediaPlayer));
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_play
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer { nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    mediaPlayer.Play();

    winrt::detach_abi(mediaPlayer);
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_pause
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    mediaPlayer.Pause();

    winrt::detach_abi(mediaPlayer);
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_isPlaying
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    MediaPlaybackState playbackState = mediaPlayer.PlaybackSession().PlaybackState();

    bool playing = (playbackState == MediaPlaybackState::Opening
        || playbackState == MediaPlaybackState::Buffering
        || playbackState == MediaPlaybackState::Playing);

    winrt::detach_abi(mediaPlayer);

    return static_cast<jboolean>(playing);
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_getPosition
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    int64_t positionCount = mediaPlayer.PlaybackSession().Position().count();

    winrt::detach_abi(mediaPlayer);

    return static_cast<jdouble>(positionCount) / 10'000'000.0;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_setPosition
  (JNIEnv *env, jobject instance, jlong handle, jdouble position) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    mediaPlayer.PlaybackSession().Position(std::chrono::milliseconds(static_cast<long>(position * 1000)));

    winrt::detach_abi(mediaPlayer);
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_getVolume
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    jdouble volume = mediaPlayer.Volume();

    winrt::detach_abi(mediaPlayer);

    return volume;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_setVolume
  (JNIEnv *env, jobject instance, jlong handle, jdouble volume) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    mediaPlayer.Volume(volume);

    winrt::detach_abi(mediaPlayer);
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_destroy
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer mediaPlayer{ nullptr };

    winrt::attach_abi(mediaPlayer, reinterpret_cast<void*>(handle));

    mediaPlayer.Close();
}
