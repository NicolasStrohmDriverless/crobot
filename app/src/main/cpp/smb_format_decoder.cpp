// app/src/main/cpp/smb_format_decoder.cpp
// No ROM assets, this project uses original tilesets. Format inspired by SMB metatile approach.

#include "smb_format_decoder.h"

#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>

#include <algorithm>
#include <cctype>
#include <sstream>
#include <stdexcept>

#include "third_party/json.hpp"

namespace crobot {
namespace {

using nlohmann::json;

constexpr int kCollisionSolid = 0x1;

std::string readAsset(AAssetManager* assetManager, const std::string& path) {
    if (assetManager == nullptr) {
        throw std::runtime_error("Asset manager not initialised");
    }
    AAsset* asset = AAssetManager_open(assetManager, path.c_str(), AASSET_MODE_BUFFER);
    if (asset == nullptr) {
        throw std::runtime_error("Asset not found: " + path);
    }
    const off_t length = AAsset_getLength(asset);
    std::string buffer;
    buffer.resize(static_cast<size_t>(length));
    int64_t read = AAsset_read(asset, buffer.data(), length);
    AAsset_close(asset);
    if (read < length) {
        throw std::runtime_error("Failed to read entire asset: " + path);
    }
    return buffer;
}

std::vector<int> parseCsv(const std::string& data) {
    std::vector<int> values;
    std::stringstream stream(data);
    std::string cell;
    while (std::getline(stream, cell, ',')) {
        size_t start = 0;
        size_t end = cell.size();
        while (start < end && std::isspace(static_cast<unsigned char>(cell[start]))) {
            ++start;
        }
        while (end > start && std::isspace(static_cast<unsigned char>(cell[end - 1]))) {
            --end;
        }
        if (end <= start) {
            values.push_back(0);
        } else {
            int value = std::stoi(cell.substr(start, end - start));
            values.push_back(value);
        }
    }
    return values;
}

void parseEntities(const json& source, std::vector<EntityDefinition>& out) {
    out.clear();
    if (!source.is_array()) {
        return;
    }
    for (const json& entry : source) {
        EntityDefinition entity;
        entity.type = entry.value("type", "unknown");
        entity.x = entry.value("x", 0);
        entity.y = entry.value("y", 0);
        if (entry.contains("properties") && entry["properties"].is_object()) {
            for (auto it = entry["properties"].begin(); it != entry["properties"].end(); ++it) {
                if (it.value().is_string()) {
                    entity.extras[it.key()] = it.value().get<std::string>();
                } else if (it.value().is_number_integer()) {
                    entity.extras[it.key()] = std::to_string(it.value().get<int>());
                } else if (it.value().is_number_float()) {
                    entity.extras[it.key()] = std::to_string(it.value().get<double>());
                } else if (it.value().is_boolean()) {
                    entity.extras[it.key()] = it.value().get<bool>() ? "true" : "false";
                }
            }
        }
        out.push_back(std::move(entity));
    }
}

LevelDefinition loadTiledJson(AAssetManager* assetManager, int world, int stage, const std::string& path) {
    std::string contents = readAsset(assetManager, path);
    json doc = json::parse(contents);

    LevelDefinition level;
    level.world = world;
    level.stage = stage;
    level.tileWidth = doc.value("tileWidth", doc.value("tilewidth", 16));
    level.tileHeight = doc.value("tileHeight", doc.value("tileheight", 16));
    level.width = doc.value("width", 0);
    level.height = doc.value("height", 0);
    level.tilesetPath = doc.value("tileset", "");

    if (!doc.contains("layers") || !doc["layers"].is_array() || doc["layers"].empty()) {
        throw std::runtime_error("Level JSON missing layers array");
    }

    const json& layer = doc["layers"].front();
    std::string encoding = layer.value("encoding", "");
    if (encoding != "csv") {
        throw std::runtime_error("Only CSV-encoded layers are supported");
    }
    std::string data = layer.value("data", "");
    level.tiles = parseCsv(data);
    if (static_cast<int>(level.tiles.size()) != level.width * level.height) {
        throw std::runtime_error("CSV tile data does not match declared dimensions");
    }

    std::vector<int> solid = doc.value("solidGids", std::vector<int>());
    int maxGid = 0;
    for (int gid : level.tiles) {
        maxGid = std::max(maxGid, gid);
    }
    for (int gid : solid) {
        maxGid = std::max(maxGid, gid);
    }
    level.collisionFlags.assign(maxGid + 1, 0);
    for (int gid : solid) {
        if (gid >= 0 && gid < static_cast<int>(level.collisionFlags.size())) {
            level.collisionFlags[gid] |= kCollisionSolid;
        }
    }

    parseEntities(doc.value("entities", json::array()), level.entities);
    return level;
}

LevelDefinition loadAreaJson(AAssetManager* assetManager, int world, int stage, const std::string& path) {
    std::string contents = readAsset(assetManager, path);
    json doc = json::parse(contents);

    LevelDefinition level;
    level.world = world;
    level.stage = stage;
    level.tileWidth = doc.value("tileWidth", 16);
    level.tileHeight = doc.value("tileHeight", 16);
    level.height = doc.value("height", 0);
    level.tilesetPath = doc.value("tileset", "");

    if (!doc.contains("columns") || !doc["columns"].is_array()) {
        throw std::runtime_error("Area JSON requires a columns array");
    }

    int computedWidth = 0;
    std::vector<int> expanded;
    for (const json& column : doc["columns"]) {
        int repeat = std::max(1, column.value("repeat", 1));
        std::vector<int> columnTiles(level.height, 0);
        if (column.contains("metatile") && column["metatile"].is_array()) {
            const json& arr = column["metatile"];
            int limit = std::min(static_cast<int>(arr.size()), level.height);
            for (int i = 0; i < limit; ++i) {
                columnTiles[i] = arr[i].get<int>();
            }
        }
        if (column.contains("rows") && column["rows"].is_array()) {
            for (const json& row : column["rows"]) {
                int from = std::max(0, row.value("from", 0));
                int to = std::min(level.height - 1, row.value("to", from));
                int gid = row.value("gid", 0);
                for (int y = from; y <= to && y < level.height; ++y) {
                    columnTiles[y] = gid;
                }
            }
        }
        for (int i = 0; i < repeat; ++i) {
            expanded.insert(expanded.end(), columnTiles.begin(), columnTiles.end());
        }
        computedWidth += repeat;
    }

    level.width = doc.value("width", computedWidth);
    if (level.width != computedWidth) {
        // If width is declared explicitly it must match the generated count.
        if (computedWidth != 0) {
            level.width = computedWidth;
        }
    }
    if (static_cast<int>(expanded.size()) != level.width * level.height) {
        throw std::runtime_error("Expanded column data does not match width/height");
    }
    level.tiles = std::move(expanded);

    std::vector<int> solid = doc.value("solidGids", std::vector<int>());
    int maxGid = 0;
    for (int gid : level.tiles) {
        maxGid = std::max(maxGid, gid);
    }
    for (int gid : solid) {
        maxGid = std::max(maxGid, gid);
    }
    level.collisionFlags.assign(maxGid + 1, 0);
    for (int gid : solid) {
        if (gid >= 0 && gid < static_cast<int>(level.collisionFlags.size())) {
            level.collisionFlags[gid] |= kCollisionSolid;
        }
    }

    parseEntities(doc.value("entities", json::array()), level.entities);
    return level;
}

}  // namespace

bool assetExists(AAssetManager* assetManager, const std::string& path) {
    if (assetManager == nullptr) {
        return false;
    }
    AAsset* asset = AAssetManager_open(assetManager, path.c_str(), AASSET_MODE_UNKNOWN);
    if (asset == nullptr) {
        return false;
    }
    AAsset_close(asset);
    return true;
}

LevelDefinition loadLevelFromAssets(AAssetManager* assetManager, int world, int stage) {
    std::string basePath = "levels/world" + std::to_string(world) + "_stage" + std::to_string(stage);
    std::string tiledPath = basePath + ".json";
    if (assetExists(assetManager, tiledPath)) {
        return loadTiledJson(assetManager, world, stage, tiledPath);
    }

    std::string areaPath = basePath + ".area.json";
    if (assetExists(assetManager, areaPath)) {
        return loadAreaJson(assetManager, world, stage, areaPath);
    }

    throw std::runtime_error("Level asset not found for world/stage combination");
}

}  // namespace crobot
