#include <sys/mman.h>
#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <jni.h>

extern void* mremap(void* old_address, size_t old_size, size_t new_size, int flags);

static inline void* cast(jlong value) {
    return (void*)(intptr_t)value;
}

JNIEXPORT jlong JNICALL
Java_one_nio_os_Mem_mmap(JNIEnv* env, jclass cls, jlong start, jlong length, jint prot, jint flags, jint fd, jlong offset) {
    return (jlong)(intptr_t) mmap(cast(start), length, prot, flags, fd, offset);
}

JNIEXPORT jlong JNICALL
Java_one_nio_os_Mem_mremap(JNIEnv* env, jclass cls, jlong oldAddress, jlong oldSize, jlong newSize, jint flags) {
    return (jlong)(intptr_t) mremap(cast(oldAddress), oldSize, newSize, flags);
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_munmap(JNIEnv* env, jclass cls, jlong start, jlong length) {
    return munmap(cast(start), length) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_mprotect(JNIEnv* env, jclass cls, jlong addr, jlong len, jint prot) {
    return mprotect(cast(addr), len, prot) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_msync(JNIEnv* env, jclass cls, jlong start, jlong length, jint flags) {
    return msync(cast(start), length, flags) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_mlock(JNIEnv* env, jclass cls, jlong addr, jlong len) {
    return mlock(cast(addr), len) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_munlock(JNIEnv* env, jclass cls, jlong addr, jlong len) {
    return munlock(cast(addr), len) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_mlockall(JNIEnv* env, jclass cls, jint flags) {
    return mlockall(flags) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_munlockall(JNIEnv* env, jclass cls) {
    return munlockall() == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_posix_1madvise(JNIEnv* env, jclass cls, jlong addr, jlong len, jint advice) {
    return posix_madvise(cast(addr), len, advice) == 0 ? 0 : errno;
}

JNIEXPORT jint JNICALL
Java_one_nio_os_Mem_posix_1fadvise(JNIEnv* env, jclass cls, jint fd, jlong offset, jlong len, jint advice) {
    return posix_fadvise(fd, offset, len, advice) == 0 ? 0 : errno;
}
