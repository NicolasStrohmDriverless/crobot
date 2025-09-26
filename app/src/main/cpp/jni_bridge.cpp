// app/src/main/cpp/jni_bridge.cpp
// No ROM assets, this project uses original tilesets. Format inspired by SMB metatile approach.

#include <jni.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <mutex>
#include <stdexcept>
#include <string>

#include "smb_format_decoder.h"

namespace {

constexpr const char* kLogTag = "crobot_native";
constexpr const char* kEntityClassName = "com/crobot/game/level/LevelModel$Entity";

struct CachedLevel {
    crobot::LevelDefinition definition;
    bool valid = false;
};

CachedLevel g_cachedLevel;
AAssetManager* g_assetManager = nullptr;
std::mutex g_mutex;

void logDebug(const std::string& message) {
    __android_log_print(ANDROID_LOG_DEBUG, kLogTag, "%s", message.c_str());
}

void throwJavaException(JNIEnv* env, const char* className, const std::string& message) {
    if (env->ExceptionCheck()) {
        return;
    }
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        env->ExceptionClear();
        clazz = env->FindClass("java/lang/RuntimeException");
    }
    env->ThrowNew(clazz, message.c_str());
}

void ensureAssetManager(JNIEnv* env) {
    if (g_assetManager == nullptr) {
        throwJavaException(env, "java/io/IOException", "AssetManager not initialised. Call nativeSetAssetManager first.");
        throw std::runtime_error("asset manager not initialised");
    }
}

void loadLevelLocked(JNIEnv* env, int world, int stage) {
    try {
        g_cachedLevel.definition = crobot::loadLevelFromAssets(g_assetManager, world, stage);
        g_cachedLevel.valid = true;
    } catch (const std::exception& ex) {
        g_cachedLevel.valid = false;
        throwJavaException(env, "java/io/IOException", ex.what());
    }
}

jobject makeJavaString(JNIEnv* env, const std::string& value) {
    return env->NewStringUTF(value.c_str());
}

jobject buildJavaMap(JNIEnv* env, const std::map<std::string, std::string>& source) {
    jclass hashMapClass = env->FindClass("java/util/HashMap");
    if (hashMapClass == nullptr) {
        throwJavaException(env, "java/lang/ClassNotFoundException", "HashMap class missing");
        return nullptr;
    }
    jmethodID init = env->GetMethodID(hashMapClass, "<init>", "()V");
    jmethodID put = env->GetMethodID(hashMapClass, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
    jobject map = env->NewObject(hashMapClass, init);
    for (const auto& entry : source) {
        jobject key = makeJavaString(env, entry.first);
        jobject value = makeJavaString(env, entry.second);
        env->CallObjectMethod(map, put, key, value);
        env->DeleteLocalRef(key);
        env->DeleteLocalRef(value);
    }
    env->DeleteLocalRef(hashMapClass);
    return map;
}

jobject buildJavaEntity(JNIEnv* env, jclass entityClass, jmethodID ctor, const crobot::EntityDefinition& definition) {
    jobject typeString = makeJavaString(env, definition.type);
    jobject extrasMap = buildJavaMap(env, definition.extras);
    if (extrasMap == nullptr) {
        env->DeleteLocalRef(typeString);
        return nullptr;
    }
    jobject entity = env->NewObject(entityClass, ctor, typeString, definition.x, definition.y, extrasMap);
    env->DeleteLocalRef(typeString);
    env->DeleteLocalRef(extrasMap);
    return entity;
}

void ensureLevel(JNIEnv* env, int world, int stage) {
    if (g_cachedLevel.valid && g_cachedLevel.definition.world == world && g_cachedLevel.definition.stage == stage) {
        return;
    }
    loadLevelLocked(env, world, stage);
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_crobot_game_level_LevelRepository_nativeSetAssetManager(JNIEnv* env, jclass, jobject assetManager) {
    AAssetManager* manager = AAssetManager_fromJava(env, assetManager);
    if (manager == nullptr) {
        throwJavaException(env, "java/lang/IllegalArgumentException", "AssetManager was null");
        return;
    }
    std::scoped_lock<std::mutex> lock(g_mutex);
    g_assetManager = manager;
    g_cachedLevel.valid = false;
    logDebug("Asset manager initialised");
}

JNIEXPORT jintArray JNICALL
Java_com_crobot_game_level_LevelRepository_nativeLoadTileMap(JNIEnv* env, jclass, jint world, jint stage) {
    std::scoped_lock<std::mutex> lock(g_mutex);
    ensureAssetManager(env);
    ensureLevel(env, world, stage);
    if (!g_cachedLevel.valid) {
        return nullptr;
    }
    const auto& tiles = g_cachedLevel.definition.tiles;
    jintArray array = env->NewIntArray(static_cast<jsize>(tiles.size()));
    if (array == nullptr) {
        throwJavaException(env, "java/lang/OutOfMemoryError", "Failed to allocate tile array");
        return nullptr;
    }
    env->SetIntArrayRegion(array, 0, static_cast<jsize>(tiles.size()), tiles.data());
    return array;
}

JNIEXPORT jobjectArray JNICALL
Java_com_crobot_game_level_LevelRepository_nativeLoadEntities(JNIEnv* env, jclass, jint world, jint stage) {
    std::scoped_lock<std::mutex> lock(g_mutex);
    ensureAssetManager(env);
    ensureLevel(env, world, stage);
    if (!g_cachedLevel.valid) {
        return nullptr;
    }

    jclass entityClass = env->FindClass(kEntityClassName);
    if (entityClass == nullptr) {
        throwJavaException(env, "java/lang/ClassNotFoundException", "Unable to locate LevelModel.Entity class");
        return nullptr;
    }
    jmethodID ctor = env->GetMethodID(entityClass, "<init>", "(Ljava/lang/String;IILjava/util/Map;)V");
    if (ctor == nullptr) {
        throwJavaException(env, "java/lang/NoSuchMethodError", "LevelModel.Entity constructor signature mismatch");
        env->DeleteLocalRef(entityClass);
        return nullptr;
    }
    const auto& entities = g_cachedLevel.definition.entities;
    jobjectArray array = env->NewObjectArray(static_cast<jsize>(entities.size()), entityClass, nullptr);
    if (array == nullptr) {
        throwJavaException(env, "java/lang/OutOfMemoryError", "Failed to allocate entity array");
        env->DeleteLocalRef(entityClass);
        return nullptr;
    }
    for (jsize i = 0; i < static_cast<jsize>(entities.size()); ++i) {
        jobject entity = buildJavaEntity(env, entityClass, ctor, entities[static_cast<size_t>(i)]);
        if (entity == nullptr) {
            env->DeleteLocalRef(entityClass);
            return nullptr;
        }
        env->SetObjectArrayElement(array, i, entity);
        env->DeleteLocalRef(entity);
    }
    env->DeleteLocalRef(entityClass);
    return array;
}

JNIEXPORT jintArray JNICALL
Java_com_crobot_game_level_LevelRepository_nativeGetCollisionMask(JNIEnv* env, jclass) {
    std::scoped_lock<std::mutex> lock(g_mutex);
    ensureAssetManager(env);
    if (!g_cachedLevel.valid) {
        throwJavaException(env, "java/lang/IllegalStateException", "No level cached. Load a level before requesting collision mask.");
        return nullptr;
    }
    const auto& flags = g_cachedLevel.definition.collisionFlags;
    jintArray array = env->NewIntArray(static_cast<jsize>(flags.size()));
    if (array == nullptr) {
        throwJavaException(env, "java/lang/OutOfMemoryError", "Failed to allocate collision mask array");
        return nullptr;
    }
    env->SetIntArrayRegion(array, 0, static_cast<jsize>(flags.size()), flags.data());
    return array;
}

JNIEXPORT jintArray JNICALL
Java_com_crobot_game_level_LevelRepository_nativeGetLevelDimensions(JNIEnv* env, jclass) {
    std::scoped_lock<std::mutex> lock(g_mutex);
    ensureAssetManager(env);
    if (!g_cachedLevel.valid) {
        throwJavaException(env, "java/lang/IllegalStateException", "No level cached. Load a level first.");
        return nullptr;
    }
    jint data[4];
    data[0] = g_cachedLevel.definition.width;
    data[1] = g_cachedLevel.definition.height;
    data[2] = g_cachedLevel.definition.tileWidth;
    data[3] = g_cachedLevel.definition.tileHeight;
    jintArray array = env->NewIntArray(4);
    if (array == nullptr) {
        throwJavaException(env, "java/lang/OutOfMemoryError", "Failed to allocate dimension array");
        return nullptr;
    }
    env->SetIntArrayRegion(array, 0, 4, data);
    return array;
}

JNIEXPORT jstring JNICALL
Java_com_crobot_game_level_LevelRepository_nativeGetTilesetPath(JNIEnv* env, jclass) {
    std::scoped_lock<std::mutex> lock(g_mutex);
    ensureAssetManager(env);
    if (!g_cachedLevel.valid) {
        throwJavaException(env, "java/lang/IllegalStateException", "No level cached. Load a level first.");
        return nullptr;
    }
    return env->NewStringUTF(g_cachedLevel.definition.tilesetPath.c_str());
}

}  // extern "C"
