package moe.syrup.data;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import moe.syrup.Softcore;
import moe.syrup.ban.BanState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SoftcoreData {
    private static SoftcoreData instance;
    private static MinecraftServer server;

    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Path dataPath;
    private final Gson gson;

    private SoftcoreData(Path dataPath) {
        this.dataPath = dataPath;
        this.gson = createGson();
    }

    public static void init(MinecraftServer minecraftServer) {
        server = minecraftServer;
        Path worldPath = minecraftServer.getWorldPath(LevelResource.ROOT);
        Path dataPath = worldPath.resolve("data").resolve("softcore").resolve("players.json");
        instance = new SoftcoreData(dataPath);
        instance.load();
    }

    public static SoftcoreData getInstance() {
        return instance;
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public PlayerData getPlayerData(UUID playerId) {
        return players.computeIfAbsent(playerId, PlayerData::new);
    }

    public Map<UUID, PlayerData> getAllPlayers() {
        return players;
    }

    public void save() {
        try {
            Files.createDirectories(dataPath.getParent());
            String json = gson.toJson(players);
            Files.writeString(dataPath, json);
        } catch (IOException e) {
            Softcore.LOGGER.error("Failed to save player data", e);
        }
    }

    public void load() {
        if (!Files.exists(dataPath)) {
            return;
        }
        try {
            String json = Files.readString(dataPath);
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            Map<UUID, PlayerData> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                players.clear();
                players.putAll(loaded);
            }
            Softcore.LOGGER.info("Loaded {} player records", players.size());
        } catch (IOException | JsonSyntaxException e) {
            Softcore.LOGGER.error("Failed to load player data", e);
        }
    }

    private static Gson createGson() {
        return new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .registerTypeAdapter(BlockPos.class, new BlockPosAdapter())
            .registerTypeHierarchyAdapter(ResourceKey.class, new ResourceKeyAdapter())
            .create();
    }

    private static class InstantAdapter implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }

        @Override
        public Instant deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return Instant.parse(json.getAsString());
        }
    }

    private static class BlockPosAdapter implements JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {
        @Override
        public JsonElement serialize(BlockPos src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", src.getX());
            obj.addProperty("y", src.getY());
            obj.addProperty("z", src.getZ());
            return obj;
        }

        @Override
        public BlockPos deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            JsonObject obj = json.getAsJsonObject();
            return new BlockPos(obj.get("x").getAsInt(), obj.get("y").getAsInt(), obj.get("z").getAsInt());
        }
    }

    private static class ResourceKeyAdapter implements JsonSerializer<ResourceKey<?>>, JsonDeserializer<ResourceKey<?>> {
        @Override
        public JsonElement serialize(ResourceKey<?> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.identifier().toString());
        }

        @Override
        public ResourceKey<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            String locationStr = json.getAsString();
            net.minecraft.resources.Identifier location = net.minecraft.resources.Identifier.parse(locationStr);
            return ResourceKey.create(Registries.DIMENSION, location);
        }
    }
}
