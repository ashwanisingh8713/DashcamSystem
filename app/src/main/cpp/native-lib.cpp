#include <jni.h>
#include <string>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <android/log.h>
#include <cstring>

#define LOG_TAG "bgcam_native"
#define ALOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define ALOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_cam_et_bgcamapp_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jboolean JNICALL
Java_cam_et_dashcamsystem_nativelib_NativeLib_isImageDark(JNIEnv* env, jobject clazz, jintArray jpixels, jint width, jint height, jint threshold) {
    if (jpixels == nullptr) return JNI_FALSE;
    jint *pixels = env->GetIntArrayElements(jpixels, nullptr);
    if (pixels == nullptr) return JNI_FALSE;
    long long sum = 0;
    const int total = width * height;
    for (int i = 0; i < total; ++i) {
        jint pix = pixels[i];
        int a = (pix >> 24) & 0xFF;
        int r = (pix >> 16) & 0xFF;
        int g = (pix >> 8) & 0xFF;
        int b = pix & 0xFF;
        // luminance
        int l = (299 * r + 587 * g + 114 * b) / 1000;
        sum += l;
    }
    env->ReleaseIntArrayElements(jpixels, pixels, JNI_ABORT);
    int avg = (int)(sum / total);
    ALOGI("Average luminance=%d threshold=%d", avg, threshold);
    return (avg < threshold) ? JNI_TRUE : JNI_FALSE;
}

// POSIX save bytes
extern "C" JNIEXPORT jboolean JNICALL
Java_cam_et_dashcamsystem_nativelib_NativeLib_saveBytesToFile(JNIEnv* env, jobject clazz, jstring jpath, jbyteArray jdata) {
    if (jpath == nullptr || jdata == nullptr) return JNI_FALSE;
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    jsize len = env->GetArrayLength(jdata);
    jbyte* data = env->GetByteArrayElements(jdata, nullptr);
    if (path == nullptr || data == nullptr) {
        if (path) env->ReleaseStringUTFChars(jpath, path);
        return JNI_FALSE;
    }

    int fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0) {
        ALOGE("open failed: %s", path);
        env->ReleaseStringUTFChars(jpath, path);
        env->ReleaseByteArrayElements(jdata, data, JNI_ABORT);
        return JNI_FALSE;
    }
    ssize_t wrote = write(fd, data, (size_t)len);
    if (wrote < 0 || wrote != len) {
        ALOGE("write failed wrote=%zd len=%d", wrote, len);
        close(fd);
        env->ReleaseStringUTFChars(jpath, path);
        env->ReleaseByteArrayElements(jdata, data, JNI_ABORT);
        return JNI_FALSE;
    }
    fsync(fd);
    close(fd);
    env->ReleaseStringUTFChars(jpath, path);
    env->ReleaseByteArrayElements(jdata, data, JNI_ABORT);
    return JNI_TRUE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_cam_et_dashcamsystem_nativelib_NativeLib_appendLog(JNIEnv* env, jobject clazz, jstring jpath, jstring jline) {
    if (jpath == nullptr || jline == nullptr) return JNI_FALSE;
    const char* path = env->GetStringUTFChars(jpath, nullptr);
    const char* line = env->GetStringUTFChars(jline, nullptr);
    if (path == nullptr || line == nullptr) {
        if (path) env->ReleaseStringUTFChars(jpath, path);
        if (line) env->ReleaseStringUTFChars(jline, line);
        return JNI_FALSE;
    }
    int fd = open(path, O_WRONLY | O_CREAT | O_APPEND, 0644);
    if (fd < 0) {
        ALOGE("appendLog open failed: %s", path);
        env->ReleaseStringUTFChars(jpath, path);
        env->ReleaseStringUTFChars(jline, line);
        return JNI_FALSE;
    }
    size_t len = strlen(line);
    ssize_t wrote = write(fd, line, len);
    if (wrote < 0 || (size_t)wrote != len) {
        ALOGE("append write failed wrote=%zd len=%zu", wrote, len);
        close(fd);
        env->ReleaseStringUTFChars(jpath, path);
        env->ReleaseStringUTFChars(jline, line);
        return JNI_FALSE;
    }
    fsync(fd);
    close(fd);
    env->ReleaseStringUTFChars(jpath, path);
    env->ReleaseStringUTFChars(jline, line);
    return JNI_TRUE;
}
