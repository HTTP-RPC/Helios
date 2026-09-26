#include <jni.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Storage.h>
#include <winrt/Windows.Storage.Streams.h>
#include <winrt/Windows.Media.Core.h>
#include <winrt/Windows.Media.Playback.h>
#include "org_httprpc_helios_WindowsAudioPlayer.h"

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
    const jchar* contentPathChars = env->GetStringChars(contentPath, nullptr);
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

    bool playing;
    if (playbackState == MediaPlaybackState::Opening || playbackState == MediaPlaybackState::Buffering) {
        playing = true;
    }
    else if (playbackState == MediaPlaybackState::Playing) {
        int64_t positionCount = mediaPlayer.PlaybackSession().Position().count();
        int64_t naturalDurationCount = mediaPlayer.PlaybackSession().NaturalDuration().count();

        playing = positionCount < naturalDurationCount;
    }
    else {
        playing = false;
    }

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
