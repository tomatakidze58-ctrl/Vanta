package dev.vanta.client.util;

import com.google.gson.*;
import dev.vanta.client.VantaClient;
import dev.vanta.client.module.Module;
import dev.vanta.client.module.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Config() {}

    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("vantaclient.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject(), mods = new JsonObject(), theme = new JsonObject();
            for (Module m : VantaClient.modules.all) {
                JsonObject o = new JsonObject(), s = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("x", m.x); o.addProperty("y", m.y);
                for (Setting st : m.settings) st.write(s);
                o.add("settings", s);
                mods.add(m.name, o);
            }
            for (Setting st : Theme.settings) st.write(theme);
            root.add("modules", mods); root.add("theme", theme);
            Files.writeString(path(), GSON.toJson(root));
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void load() {
        try {
            Path p = path();
            if (!Files.exists(p)) return;
            JsonObject root = JsonParser.parseString(Files.readString(p)).getAsJsonObject();
            JsonObject mods = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
            for (Module m : VantaClient.modules.all) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("enabled")) m.enabled = o.get("enabled").getAsBoolean(); // onEnable runs on first tick
                if (o.has("x")) m.x = o.get("x").getAsInt();
                if (o.has("y")) m.y = o.get("y").getAsInt();
                if (o.has("settings")) for (Setting st : m.settings) st.read(o.getAsJsonObject("settings"));
            }
            if (root.has("theme")) for (Setting st : Theme.settings) st.read(root.getAsJsonObject("theme"));
        } catch (Exception e) { e.printStackTrace(); }
    }
}
