#include <jni.h>
#include <iostream>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Media.Playback.h>
#include <winrt/Windows.Media.Core.h>
#include "org_httprpc_helios_WindowsAudioPlayer.h"

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_initialize
  (JNIEnv *env, jclass type) {
    init_apartment();
}

JNIEXPORT jlong JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_construct
  (JNIEnv *env, jobject instance, jstring contentPath) {
    const jchar* contentPathChars = env->GetStringChars(contentPath, nullptr);
    jsize contentPathLength = env->GetStringLength(javaString);

    winrt::hstring path { reinterpret_cast<const wchar_t*>(contentPathChars), static_cast<uint32_t>(contentPathLength) };

    env->ReleaseStringChars(contentPath, contentPathChars);

    MediaPlayer* mediaPlayer = new MediaPlayer();

    auto asyncOperation = StorageFile::GetFileFromPathAsync(path);

    auto mediaSource = MediaSource::CreateFromStorageFile(asyncOperation.get());

    mediaPlayer->Source(mediaSource);

    return reinterpret_cast<jlong>(mediaPlayer);
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_play
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    mediaPlayer->Play();
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_pause
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    mediaPlayer->Pause();
}

JNIEXPORT jboolean JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_isPlaying
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    auto session = media_player.PlaybackSession();

    return session.PlaybackState() > 0;
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_getPosition
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    auto session = media_player.PlaybackSession();

    int64_t ticks = session.Position.count();

    return static_cast<double>(ticks) / 10'000'000.0;
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_setPosition
  (JNIEnv *env, jobject instance, jdouble, jdouble position) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    auto session = media_player.PlaybackSession();

    session.Position(std::chrono::seconds(position));
}

JNIEXPORT jdouble JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_getVolume
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    return mediaPlayer->Volume();
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_setVolume
  (JNIEnv *env, jobject instance, jlong handle, jdouble volume) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    mediaPlayer->Volume(volume);
}

JNIEXPORT void JNICALL Java_org_httprpc_helios_WindowsAudioPlayer_dispose
  (JNIEnv *env, jobject instance, jlong handle) {
    MediaPlayer* mediaPlayer = reinterpret_cast<MediaPlayer*>(handle);

    mediaPlayer->Close();

    delete mediaPlayer;
}
