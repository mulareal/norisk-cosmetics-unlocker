package gg.norisk.cosmetics.unlocker.cosmetics;

import com.google.gson.*;
import gg.norisk.cosmetics.unlocker.Log;
import gg.norisk.cosmetics.unlocker.api.ClientHooks;
import gg.norisk.networking.model.core.NoRiskUserMinimal;
import gg.norisk.networking.model.cosmetics.*;
import net.minecraft.client.Minecraft;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Persists customized cosmetic outfits locally to disk so they survive restarts
 * and do not rely on backend server validation.
 */
public class LocalOutfitStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<CosmeticOutfit> outfits = new CopyOnWriteArrayList<>();
    private static final Map<UUID, String> remotePlayerIcons = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<UUID, JsonObject> remoteNametagData = new java.util.concurrent.ConcurrentHashMap<>();
    private static JsonObject cachedLocalNametagJson = null;
    private static String activeIconName = "vip";
    private static boolean loaded = false;
    private static CosmeticOutfit activeOutfit = null;

    /** Bumped whenever any nametag data changes; users are only re-applied when it moved (or every 2s). */
    private static final java.util.concurrent.atomic.AtomicLong nametagVersion = new java.util.concurrent.atomic.AtomicLong();
    private static final Map<UUID, Applied> appliedNametags = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long REAPPLY_MS = 2000;

    private record Applied(NoRiskUserMinimal user, long version, long time) {
    }

    private static void bumpNametagVersion() {
        nametagVersion.incrementAndGet();
    }

    public static UUID getLocalPlayerUuid() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                if (mc.player != null) {
                    return mc.player.getUUID();
                }
                if (mc.getUser() != null && mc.getUser().getProfileId() != null) {
                    return mc.getUser().getProfileId();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            return gg.norisk.compat.auth.NoriskAuth.INSTANCE.getPlayerUuid();
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static boolean isLocalPlayer(UUID uuid) {
        if (uuid == null) return false;
        UUID local = getLocalPlayerUuid();
        return local != null && local.equals(uuid);
    }

    public static boolean hasRemoteData(UUID uuid) {
        if (uuid == null) return false;
        return remoteNametagData.containsKey(uuid) || remotePlayerIcons.containsKey(uuid);
    }

    public static Set<UUID> getRemoteTrackedUuids() {
        Set<UUID> set = new HashSet<>(remoteNametagData.keySet());
        set.addAll(remotePlayerIcons.keySet());
        return set;
    }

    public static void clearRemoteData() {
        remoteNametagData.clear();
        remotePlayerIcons.clear();
        appliedNametags.clear();
        bumpNametagVersion();
    }

    public static void clearRemotePlayer(UUID uuid) {
        if (uuid == null) return;
        remoteNametagData.remove(uuid);
        remotePlayerIcons.remove(uuid);
        appliedNametags.remove(uuid);
        bumpNametagVersion();
    }

    public static String resolvePlayerIgn(UUID uuid) {
        if (uuid == null) return "";
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                if (isLocalPlayer(uuid)) {
                    if (mc.player != null) {
                        String name = mc.player.getScoreboardName();
                        if (name != null && !name.isEmpty()) return name;
                    }
                    if (mc.getUser() != null && mc.getUser().getName() != null) {
                        return mc.getUser().getName();
                    }
                }
                if (mc.getConnection() != null) {
                    net.minecraft.client.multiplayer.PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
                    if (info != null && info.getProfile() != null && info.getProfile().name() != null) {
                        return info.getProfile().name();
                    }
                }
                if (mc.level != null) {
                    net.minecraft.world.entity.player.Player p = mc.level.getPlayerByUUID(uuid);
                    if (p != null) {
                        String name = p.getScoreboardName();
                        if (name != null && !name.isEmpty()) return name;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    private static NoRiskUserMinimal instantiateUserMinimal(UUID uuid, String ign, NoRiskUserMinimal copyFrom) {
        String safeIgn = ign != null ? ign : "";
        try {
            for (java.lang.reflect.Constructor<?> ctor : NoRiskUserMinimal.class.getConstructors()) {
                Class<?>[] params = ctor.getParameterTypes();
                if (params.length >= 8 && params[0] == UUID.class && params[1] == String.class) {
                    String lastParamName = params[params.length - 1].getName();
                    if (lastParamName.contains("ConstructorMarker")) {
                        continue;
                    }
                    Object[] args = new Object[params.length];
                    args[0] = uuid;
                    args[1] = safeIgn;
                    for (int i = 2; i < params.length; i++) {
                        Class<?> pt = params[i];
                        if (pt == gg.norisk.networking.model.core.NoRiskRank.class) {
                            args[i] = (copyFrom != null && copyFrom.getRank() != null)
                                    ? copyFrom.getRank()
                                    : gg.norisk.networking.model.core.NoRiskRank.DEFAULT;
                        } else if (pt == kotlinx.datetime.LocalDateTime.class) {
                            // lastSeen is non-null since NoRisk 26.4
                            args[i] = (copyFrom != null && copyFrom.getLastSeen() != null)
                                    ? copyFrom.getLastSeen()
                                    : new kotlinx.datetime.LocalDateTime(2026, 1, 1, 0, 0, 0, 0);
                        } else if (pt == Long.class || pt == long.class) {
                            args[i] = 4102444800000L;
                        } else if (pt == CustomNameTag.class) {
                            args[i] = (copyFrom != null && copyFrom.getNameTag() != null)
                                    ? copyFrom.getNameTag()
                                    : new CustomNameTag();
                        } else if (pt == gg.norisk.networking.model.core.DailyStreak.class) {
                            args[i] = new gg.norisk.networking.model.core.DailyStreak();
                        } else if (pt == CustomIconInfo.class) {
                            args[i] = (copyFrom != null && copyFrom.getCustomIconInfo() != null)
                                    ? copyFrom.getCustomIconInfo()
                                    : new CustomIconInfo();
                        } else if (pt == AdditionalNameTag.class) {
                            args[i] = (copyFrom != null && copyFrom.getAdditionalNameTag() != null)
                                    ? copyFrom.getAdditionalNameTag()
                                    : new AdditionalNameTag();
                        } else if (pt == boolean.class || pt == Boolean.class) {
                            args[i] = Boolean.FALSE;
                        } else if (pt == int.class || pt == Integer.class) {
                            args[i] = 0;
                        } else {
                            args[i] = null;
                        }
                    }
                    return (NoRiskUserMinimal) ctor.newInstance(args);
                }
            }
        } catch (Throwable t) {
            Throwable cause = t instanceof java.lang.reflect.InvocationTargetException && t.getCause() != null ? t.getCause() : t;
            Log.warn("Reflective NoRiskUserMinimal instantiation failed: " + cause);
        }
        return copyFrom;
    }

    public static NoRiskUserMinimal createOrUpdateUser(UUID uuid, NoRiskUserMinimal existing) {
        if (uuid == null) return existing;
        // Hot path (called for every nametag/text every frame): skip if this exact user object is up to date
        long version = nametagVersion.get();
        long now = System.currentTimeMillis();
        Applied applied = appliedNametags.get(uuid);
        if (existing != null && applied != null && applied.user() == existing && applied.version() == version
                && now - applied.time() < REAPPLY_MS && !existing.getIgn().isEmpty()) {
            return existing;
        }
        String resolvedIgn = resolvePlayerIgn(uuid);
        NoRiskUserMinimal user = existing;
        if (user == null || (user.getIgn().isEmpty() && !resolvedIgn.isEmpty())) {
            String ign = (user != null && !user.getIgn().isEmpty()) ? user.getIgn() : resolvedIgn;
            user = instantiateUserMinimal(uuid, ign, existing);
        }
        if (user == null) return null;
        if (isLocalPlayer(uuid)) {
            applySavedNameTag(user);
        } else {
            JsonObject remoteObj = remoteNametagData.get(uuid);
            if (remoteObj != null) {
                applyNametagObjectToUser(remoteObj, user);
            }
        }
        appliedNametags.put(uuid, new Applied(user, version, now));
        return user;
    }

    public static synchronized String getActiveIconName() {
        loadOutfits();
        return activeIconName;
    }

    public static synchronized void setActiveIconName(String iconName) {
        if (iconName == null || iconName.trim().isEmpty()) {
            activeIconName = "default";
        } else {
            activeIconName = iconName.trim().toLowerCase();
        }
        persistToDisk();
        try {
            JsonObject obj = getOrLoadLocalNametagJson();
            obj.addProperty("activeIcon", activeIconName);
            writeJsonToAllLocations("norisk_unlocked_nametag.json", obj);
            ClientHooks.nametagChanged();
        } catch (Throwable ignored) {
        }
    }

    public static String getSelectedIconName(UUID playerUuid) {
        if (playerUuid == null) return null;
        return remotePlayerIcons.get(playerUuid);
    }

    public static void setRemotePlayerIcon(UUID playerUuid, String iconName) {
        if (playerUuid == null || iconName == null) return;
        remotePlayerIcons.put(playerUuid, iconName.toLowerCase());
    }

    private static List<File> getCandidateFiles(String fileName) {
        List<File> files = new ArrayList<>();
        File gameDir = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                gameDir = mc.gameDirectory;
            }
        } catch (Throwable ignored) {
        }
        if (gameDir == null) {
            gameDir = new File(".");
        }
        files.add(new File(gameDir, fileName));

        try {
            File parent = gameDir.getParentFile();
            if (parent != null && "profiles".equalsIgnoreCase(parent.getName())) {
                files.add(new File(parent, fileName));
                File[] siblings = parent.listFiles(File::isDirectory);
                if (siblings != null) {
                    Arrays.sort(siblings, Comparator.comparingLong(File::lastModified).reversed());
                    for (File sibling : siblings) {
                        if (!sibling.equals(gameDir)) {
                            files.add(new File(sibling, fileName));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isEmpty()) {
                files.add(new File(userHome, "." + fileName));
            }
        } catch (Throwable ignored) {
        }
        return files;
    }

    private static File findBestReadFile(String fileName) {
        List<File> candidates = getCandidateFiles(fileName);
        for (File f : candidates) {
            if (f.exists() && f.isFile() && f.length() > 5) {
                return f;
            }
        }
        return candidates.get(0);
    }

    private static void writeJsonToAllLocations(String fileName, JsonElement element) {
        List<File> targets = new ArrayList<>();
        File gameDir = null;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                gameDir = mc.gameDirectory;
            }
        } catch (Throwable ignored) {
        }
        if (gameDir == null) {
            gameDir = new File(".");
        }
        targets.add(new File(gameDir, fileName));

        try {
            File parent = gameDir.getParentFile();
            if (parent != null && "profiles".equalsIgnoreCase(parent.getName())) {
                targets.add(new File(parent, fileName));
                File[] siblings = parent.listFiles(File::isDirectory);
                if (siblings != null) {
                    for (File sibling : siblings) {
                        if (!sibling.equals(gameDir)) {
                            targets.add(new File(sibling, fileName));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            String userHome = System.getProperty("user.home");
            if (userHome != null && !userHome.isEmpty()) {
                targets.add(new File(userHome, "." + fileName));
            }
        } catch (Throwable ignored) {
        }

        for (File target : targets) {
            try (FileWriter writer = new FileWriter(target)) {
                GSON.toJson(element, writer);
            } catch (Throwable ignored) {
            }
        }
    }

    private static File getStorageFile() {
        return findBestReadFile("norisk_unlocked_outfits.json");
    }

    private static File getNametagFile() {
        return findBestReadFile("norisk_unlocked_nametag.json");
    }

    private static synchronized JsonObject getOrLoadLocalNametagJson() {
        if (cachedLocalNametagJson != null && cachedLocalNametagJson.size() > 0) {
            return cachedLocalNametagJson;
        }
        File file = getNametagFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonElement elem = JsonParser.parseReader(reader);
                if (elem != null && elem.isJsonObject()) {
                    cachedLocalNametagJson = elem.getAsJsonObject();
                    bumpNametagVersion();
                    if (cachedLocalNametagJson.size() > 0) {
                        writeJsonToAllLocations("norisk_unlocked_nametag.json", cachedLocalNametagJson);
                    }
                    return cachedLocalNametagJson;
                }
            } catch (Throwable t) {
                Log.warn("Failed to read nametag file: " + t.getMessage());
            }
        }
        cachedLocalNametagJson = new JsonObject();
        return cachedLocalNametagJson;
    }

    public static synchronized void saveCustomNameTag(CustomNameTag nameTag) {
        if (nameTag == null) return;
        try {
            JsonObject obj = getOrLoadLocalNametagJson();

            JsonObject tagObj = new JsonObject();
            if (nameTag.getStyleId() != null) {
                tagObj.addProperty("styleId", nameTag.getStyleId());
            } else {
                tagObj.add("styleId", JsonNull.INSTANCE);
            }
            JsonArray anims = new JsonArray();
            for (NameTagAnimationStyle a : nameTag.getAnimations()) {
                if (a != null) {
                    JsonObject animObj = new JsonObject();
                    animObj.addProperty("type", a.getType());
                    if (a.getColor() != null) {
                        animObj.addProperty("color", a.getColor());
                    }
                    if (a.getColor2() != null) {
                        animObj.addProperty("color2", a.getColor2());
                    }
                    animObj.addProperty("speed", a.getSpeed());
                    animObj.addProperty("intensity", a.getIntensity());
                    anims.add(animObj);
                }
            }
            tagObj.add("animations", anims);
            obj.add("customNameTag", tagObj);
            obj.addProperty("activeIcon", getActiveIconName());
            cachedLocalNametagJson = obj;
            bumpNametagVersion();

            writeJsonToAllLocations("norisk_unlocked_nametag.json", obj);

            UUID localUuid = getLocalPlayerUuid();
            if (localUuid != null) {
                try {
                    NoRiskUserMinimal user = gg.norisk.compat.icon.NrcPlayerCache.INSTANCE.getCached(localUuid);
                    if (user != null) {
                        applyNametagObjectToUser(obj, user);
                    }
                    gg.norisk.cosmetics.v2.NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
                } catch (Throwable ignored) {
                }
            }

            ClientHooks.nametagChanged();
        } catch (Throwable t) {
            Log.warn("Failed to save custom nametag: " + t.getMessage());
        }
    }

    public static synchronized String getNametagJson() {
        try {
            JsonObject obj = getOrLoadLocalNametagJson();
            if (!obj.has("activeIcon")) {
                obj.addProperty("activeIcon", getActiveIconName());
            }
            return obj.toString();
        } catch (Throwable ignored) {
        }
        return "{}";
    }

    private static void applyNametagObjectToUser(JsonObject obj, NoRiskUserMinimal user) {
        if (obj == null || user == null) return;
        try {
            if (obj.has("customNameTag") && obj.get("customNameTag").isJsonObject()) {
                JsonObject tagObj = obj.getAsJsonObject("customNameTag");
                CustomNameTag tag = user.getNameTag();
                if (tag != null) {
                    String styleId = (tagObj.has("styleId") && !tagObj.get("styleId").isJsonNull())
                            ? tagObj.get("styleId").getAsString()
                            : null;
                    tag.setStyleId(styleId);
                    if (tagObj.has("animations") && tagObj.get("animations").isJsonArray()) {
                        List<NameTagAnimationStyle> newAnims = new ArrayList<>();
                        for (JsonElement e : tagObj.getAsJsonArray("animations")) {
                            if (e != null && e.isJsonObject()) {
                                JsonObject a = e.getAsJsonObject();
                                if (!a.has("type") || a.get("type").isJsonNull()) continue;
                                String type = a.get("type").getAsString();
                                Integer color = (a.has("color") && !a.get("color").isJsonNull()) ? a.get("color").getAsInt() : null;
                                Integer color2 = (a.has("color2") && !a.get("color2").isJsonNull()) ? a.get("color2").getAsInt() : null;
                                float speed = (a.has("speed") && !a.get("speed").isJsonNull()) ? a.get("speed").getAsFloat() : 1.0f;
                                float intensity = (a.has("intensity") && !a.get("intensity").isJsonNull()) ? a.get("intensity").getAsFloat() : 1.0f;
                                newAnims.add(new NameTagAnimationStyle(type, color, color2, speed, intensity));
                            }
                        }
                        if (!tag.getAnimations().equals(newAnims)) {
                            tag.getAnimations().clear();
                            tag.getAnimations().addAll(newAnims);
                        }
                    }
                }
            }
            if (obj.has("additionalNameTag") && obj.get("additionalNameTag").isJsonObject()) {
                JsonObject addObj = obj.getAsJsonObject("additionalNameTag");
                AdditionalNameTag addTag = user.getAdditionalNameTag();
                if (addTag != null) {
                    if (addObj.has("text") && !addObj.get("text").isJsonNull()) {
                        addTag.setText(addObj.get("text").getAsString());
                    }
                    if (addObj.has("enabled") && !addObj.get("enabled").isJsonNull()) {
                        addTag.setEnabled(addObj.get("enabled").getAsBoolean());
                    }
                    if (addObj.has("size") && !addObj.get("size").isJsonNull()) {
                        addTag.setSize(addObj.get("size").getAsFloat());
                    }
                    if (addObj.has("hasBackground") && !addObj.get("hasBackground").isJsonNull()) {
                        addTag.setHasBackground(addObj.get("hasBackground").getAsBoolean());
                    }
                    if (addObj.has("position") && !addObj.get("position").isJsonNull()) {
                        try {
                            addTag.setPosition(AdditionalNameTag.Position.valueOf(addObj.get("position").getAsString()));
                        } catch (Throwable ignored) {
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.warn("Failed to apply nametag object to user: " + t.getMessage());
        }
    }

    public static synchronized void applyRemoteNametagJson(UUID playerUuid, String json) {
        if (playerUuid == null || json == null || json.trim().isEmpty() || json.equals("{}")) return;
        try {
            JsonElement parsed = JsonParser.parseString(json);
            if (parsed == null || !parsed.isJsonObject()) return;
            JsonObject obj = parsed.getAsJsonObject();
            remoteNametagData.put(playerUuid, obj);
            bumpNametagVersion();

            if (obj.has("activeIcon") && !obj.get("activeIcon").isJsonNull()) {
                setRemotePlayerIcon(playerUuid, obj.get("activeIcon").getAsString());
            }

            NoRiskUserMinimal user = gg.norisk.compat.icon.NrcPlayerCache.INSTANCE.getCached(playerUuid);
            if (user != null) {
                applyNametagObjectToUser(obj, user);
            }
            gg.norisk.cosmetics.v2.NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
        } catch (Throwable t) {
            Log.warn("Failed to apply remote nametag JSON: " + t.getMessage());
        }
    }

    public static synchronized void applySavedNameTag(NoRiskUserMinimal user) {
        if (user == null) return;
        try {
            JsonObject obj = getOrLoadLocalNametagJson();
            applyNametagObjectToUser(obj, user);
        } catch (Throwable t) {
            Log.warn("Failed to load saved nametag: " + t.getMessage());
        }
    }

    public static synchronized void saveAdditionalNameTag(AdditionalNameTag tag) {
        if (tag == null) return;
        try {
            JsonObject obj = getOrLoadLocalNametagJson();

            JsonObject addObj = new JsonObject();
            addObj.addProperty("text", tag.getText());
            addObj.addProperty("enabled", tag.isEnabled());
            addObj.addProperty("size", tag.getSize());
            addObj.addProperty("hasBackground", tag.getHasBackground());
            if (tag.getPosition() != null) {
                addObj.addProperty("position", tag.getPosition().name());
            }
            obj.add("additionalNameTag", addObj);
            obj.addProperty("activeIcon", getActiveIconName());
            cachedLocalNametagJson = obj;
            bumpNametagVersion();

            writeJsonToAllLocations("norisk_unlocked_nametag.json", obj);

            UUID localUuid = getLocalPlayerUuid();
            if (localUuid != null) {
                try {
                    NoRiskUserMinimal user = gg.norisk.compat.icon.NrcPlayerCache.INSTANCE.getCached(localUuid);
                    if (user != null) {
                        applyNametagObjectToUser(obj, user);
                    }
                    gg.norisk.cosmetics.v2.NRCPlusModule.INSTANCE.refreshNrcPlusAnimations();
                } catch (Throwable ignored) {
                }
            }

            ClientHooks.nametagChanged();
        } catch (Throwable t) {
            Log.warn("Failed to save additional nametag: " + t.getMessage());
        }
    }



    public static synchronized List<CosmeticOutfit> loadOutfits() {
        if (loaded) return outfits;
        loaded = true;
        outfits.clear();

        File file = getStorageFile();
        if (!file.exists()) {
            return outfits;
        }

        try (FileReader reader = new FileReader(file)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement elem : array) {
                    try {
                        CosmeticOutfit outfit = deserializeOutfit(elem.getAsJsonObject());
                        if (outfit != null) {
                            outfits.add(outfit);
                        }
                    } catch (Throwable t) {
                        Log.warn("Failed to parse outfit: " + t.getMessage());
                    }
                }
            } else if (root.isJsonObject()) {
                JsonObject rootObj = root.getAsJsonObject();
                if (rootObj.has("activeIcon")) {
                    activeIconName = rootObj.get("activeIcon").getAsString();
                }
                if (rootObj.has("outfits") && rootObj.get("outfits").isJsonArray()) {
                    for (JsonElement elem : rootObj.getAsJsonArray("outfits")) {
                        try {
                            CosmeticOutfit outfit = deserializeOutfit(elem.getAsJsonObject());
                            if (outfit != null) {
                                outfits.add(outfit);
                            }
                        } catch (Throwable t) {
                            Log.warn("Failed to parse outfit: " + t.getMessage());
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.warn("Failed to load outfits: " + t.getMessage());
        }

        if (!outfits.isEmpty() && activeOutfit == null) {
            activeOutfit = outfits.get(0);
        }

        return outfits;
    }

    public static synchronized void saveOutfit(CosmeticOutfit outfit) {
        if (outfit == null) return;
        loadOutfits();
        activeOutfit = outfit;

        // Update existing or add new
        boolean replaced = false;
        for (int i = 0; i < outfits.size(); i++) {
            if (outfits.get(i).getOutfitId().equals(outfit.getOutfitId())) {
                outfits.set(i, outfit);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            outfits.add(outfit);
        }

        persistToDisk();
    }

    public static synchronized void deleteOutfit(ObjectId outfitId) {
        if (outfitId == null) return;
        loadOutfits();
        outfits.removeIf(o -> o.getOutfitId().equals(outfitId));
        if (activeOutfit != null && activeOutfit.getOutfitId().equals(outfitId)) {
            activeOutfit = outfits.isEmpty() ? null : outfits.get(0);
        }
        persistToDisk();
    }

    public static CosmeticOutfit getActiveOutfit() {
        loadOutfits();
        if (activeOutfit != null) {
            return activeOutfit;
        }

        // Fallback 1: Check local player's current outfit from NrcCosmeticPlayerDataStore
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player != null) {
                gg.norisk.cosmetics.v2.player.NrcCosmeticPlayerData localData = gg.norisk.cosmetics.v2.player.NrcCosmeticPlayerDataStore.get(mc.player.getUUID());
                if (localData != null && localData.getCurrentOutfit() != null) {
                    activeOutfit = localData.getCurrentOutfit();
                    saveOutfit(activeOutfit);
                    return activeOutfit;
                }
            }
        } catch (Throwable ignored) {
        }

        // Fallback 2: Check WardrobeCache
        try {
            gg.norisk.networking.model.cosmetics.api.WardrobeResponse wr = gg.norisk.cosmetics.v2.ui.WardrobeCache.INSTANCE.getCached();
            if (wr != null && wr.getOutfits() != null && !wr.getOutfits().isEmpty()) {
                activeOutfit = wr.getOutfits().get(0);
                saveOutfit(activeOutfit);
                return activeOutfit;
            }
        } catch (Throwable ignored) {
        }

        if (!outfits.isEmpty()) {
            activeOutfit = outfits.get(0);
            return activeOutfit;
        }
        return null;
    }

    public static void setActiveOutfit(CosmeticOutfit outfit) {
        activeOutfit = outfit;
    }

    public static String serializeOutfitToJson(CosmeticOutfit outfit) {
        if (outfit == null) return "{}";
        JsonObject obj = serializeOutfit(outfit);
        return GSON.toJson(obj);
    }

    public static JsonObject serializeOutfit(CosmeticOutfit outfit) {
        JsonObject obj = new JsonObject();
        obj.addProperty("id", outfit.getOutfitId().toHexString());
        obj.addProperty("ownerId", outfit.getOwnerId().toString());
        obj.addProperty("name", outfit.getName());
        obj.addProperty("skinId", outfit.getSkinId().toString());
        obj.addProperty("createdAt", outfit.getCreatedAt());
        obj.addProperty("customCapeHash", outfit.getCustomCapeHash());
        obj.addProperty("overwriteSkin", outfit.getOverwriteSkin());

        JsonObject setObj = new JsonObject();
        for (Map.Entry<UUID, CosmeticSettings> entry : outfit.getCosmeticSettings().entrySet()) {
            JsonObject s = new JsonObject();
            CosmeticSettings settings = entry.getValue();
            if (settings != null) {
                if (settings.getColor() != null) {
                    JsonObject c = new JsonObject();
                    c.addProperty("red", settings.getColor().getRed());
                    c.addProperty("green", settings.getColor().getGreen());
                    c.addProperty("blue", settings.getColor().getBlue());
                    c.addProperty("alpha", settings.getColor().getAlpha());
                    c.addProperty("chroma", settings.getColor().getChroma());
                    s.add("color", c);
                }
                s.addProperty("scale", settings.getScale());
                s.addProperty("previewScale", settings.getPreviewScale());
                s.addProperty("adjustRotation", settings.getAdjustRotationWhenSwimmingOrFlying());
                s.addProperty("adjustPosition", settings.getAdjustPositionWhenSkeaking());
                s.addProperty("hash", settings.getHash());
                s.addProperty("frames", settings.getFrames());
                s.addProperty("frameDelay", settings.getFrameDelay());
                s.addProperty("selectedTexture", settings.getSelectedTexture());
            }
            setObj.add(entry.getKey().toString(), s);
        }
        obj.add("settings", setObj);
        return obj;
    }

    public static CosmeticOutfit deserializeOutfitFromJson(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("{}")) return null;
        try {
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            return deserializeOutfit(obj);
        } catch (Throwable t) {
            Log.warn("Failed to deserialize outfit from JSON: " + t.getMessage());
            return null;
        }
    }

    public static CosmeticOutfit deserializeOutfit(JsonObject obj) {
        try {
            String idStr = obj.get("id").getAsString();
            ObjectId outfitId = new ObjectId(idStr);
            UUID ownerId = UUID.fromString(obj.get("ownerId").getAsString());
            String name = obj.get("name").getAsString();
            UUID skinId = obj.has("skinId") ? UUID.fromString(obj.get("skinId").getAsString()) : UUID.randomUUID();
            long createdAt = obj.has("createdAt") ? obj.get("createdAt").getAsLong() : System.currentTimeMillis();
            String customCapeHash = obj.has("customCapeHash") && !obj.get("customCapeHash").isJsonNull() ? obj.get("customCapeHash").getAsString() : null;
            boolean overwriteSkin = obj.has("overwriteSkin") && obj.get("overwriteSkin").getAsBoolean();

            Map<UUID, CosmeticSettings> settingsMap = new LinkedHashMap<>();
            if (obj.has("settings") && obj.get("settings").isJsonObject()) {
                JsonObject setObj = obj.getAsJsonObject("settings");
                for (Map.Entry<String, JsonElement> entry : setObj.entrySet()) {
                    UUID cosId = UUID.fromString(entry.getKey());
                    JsonObject s = entry.getValue().getAsJsonObject();
                    CosmeticColor color = null;
                    if (s.has("color") && s.get("color").isJsonObject()) {
                        JsonObject c = s.getAsJsonObject("color");
                        float r = c.has("red") ? c.get("red").getAsFloat() : 1.0f;
                        float g = c.has("green") ? c.get("green").getAsFloat() : 1.0f;
                        float b = c.has("blue") ? c.get("blue").getAsFloat() : 1.0f;
                        float a = c.has("alpha") ? c.get("alpha").getAsFloat() : 1.0f;
                        boolean chroma = c.has("chroma") && c.get("chroma").getAsBoolean();
                        color = new CosmeticColor(r, g, b, a, chroma);
                    }
                    float scale = s.has("scale") ? s.get("scale").getAsFloat() : 1.0f;
                    float previewScale = s.has("previewScale") ? s.get("previewScale").getAsFloat() : 1.0f;
                    Vector3f offset = new Vector3f(0.0f, 0.0f, 0.0f);
                    Vector3f previewOffset = new Vector3f(0.0f, 0.0f, 0.0f);
                    boolean adjustRotation = s.has("adjustRotation") && s.get("adjustRotation").getAsBoolean();
                    boolean adjustPosition = !s.has("adjustPosition") || s.get("adjustPosition").getAsBoolean();
                    Vector3f sneakingOffset = new Vector3f(0.0f, 0.0f, 0.0f);
                    Map<String, CosmeticArmoredPositionOffset> armoredOffset = new LinkedHashMap<>();
                    CosmeticRenderMode renderMode = null;
                    String hash = s.has("hash") && !s.get("hash").isJsonNull() ? s.get("hash").getAsString() : null;
                    int frames = s.has("frames") ? s.get("frames").getAsInt() : 1;
                    int frameDelay = s.has("frameDelay") ? s.get("frameDelay").getAsInt() : 0;
                    CustomTextureSource customTexture = null;
                    String selectedTexture = s.has("selectedTexture") && !s.get("selectedTexture").isJsonNull() ? s.get("selectedTexture").getAsString() : null;

                    CosmeticSettings settings = new CosmeticSettings(
                            color, scale, previewScale, offset, previewOffset,
                            adjustRotation, adjustPosition, sneakingOffset,
                            armoredOffset, renderMode, hash, frames, frameDelay,
                            customTexture, selectedTexture
                    );
                    settingsMap.put(cosId, settings);
                }
            }

            return new CosmeticOutfit(outfitId, ownerId, name, settingsMap, skinId, createdAt, customCapeHash, overwriteSkin);
        } catch (Throwable t) {
            Log.warn("Failed to parse outfit: " + t.getMessage());
            return null;
        }
    }

    private static void persistToDisk() {
        try {
            JsonObject rootObj = new JsonObject();
            rootObj.addProperty("activeIcon", activeIconName);
            JsonArray array = new JsonArray();
            for (CosmeticOutfit outfit : outfits) {
                array.add(serializeOutfit(outfit));
            }
            rootObj.add("outfits", array);
            writeJsonToAllLocations("norisk_unlocked_outfits.json", rootObj);
        } catch (Throwable t) {
            Log.warn("Failed to save outfits to disk: " + t.getMessage());
        }
    }
}
