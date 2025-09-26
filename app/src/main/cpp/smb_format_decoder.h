// app/src/main/cpp/smb_format_decoder.h
// No ROM assets, this project uses original tilesets. Format inspired by SMB metatile approach.
#pragma once

#include <jni.h>
#include <android/asset_manager.h>

#include <map>
#include <string>
#include <vector>

namespace crobot {

struct EntityDefinition {
    std::string type;
    int x = 0;
    int y = 0;
    std::map<std::string, std::string> extras;
};

struct LevelDefinition {
    int world = 0;
    int stage = 0;
    int width = 0;
    int height = 0;
    int tileWidth = 0;
    int tileHeight = 0;
    std::string tilesetPath;
    std::vector<int> tiles;            // width * height entries (row-major, 1-based GIDs)
    std::vector<int> collisionFlags;   // index = GID, value = bitmask (bit 0 -> solid)
    std::vector<EntityDefinition> entities;
};

/**
 * Load a level definition from the assets folder. The loader first attempts to
 * load a Tiled/JSON representation ("levels/worldX_stageY.json"). If that file
 * does not exist, it falls back to an Area/Object inspired JSON format
 * ("levels/worldX_stageY.area.json").
 */
LevelDefinition loadLevelFromAssets(AAssetManager* assetManager, int world, int stage);

/**
 * Utility that exposes whether an asset exists.
 */
bool assetExists(AAssetManager* assetManager, const std::string& path);

}  // namespace crobot
