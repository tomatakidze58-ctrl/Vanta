package dev.vanta.client.module;

import com.google.gson.JsonObject;

public class Setting {
    public enum Type { BOOL, NUM, COLOR, MODE }

    public static final int[] PALETTE = {
        0xFFFFFFFF, 0xFFFF5555, 0xFFFFAA00, 0xFFFFFF55, 0xFF55FF55,
        0xFF55FFFF, 0xFF4DA3FF, 0xFFAA55FF, 0xFFFF77CC
    };

    public final String name;
    public final Type type;
    public boolean b;
    public double n, min, max, step;
    public int color;
    public String[] modes;
    public int mode;

    private Setting(String name, Type type) { this.name = name; this.type = type; }

    public static Setting bool(String name, boolean def) {
        Setting s = new Setting(name, Type.BOOL); s.b = def; return s;
    }
    public static Setting num(String name, double def, double min, double max, double step) {
        Setting s = new Setting(name, Type.NUM);
        s.n = def; s.min = min; s.max = max; s.step = step; return s;
    }
    public static Setting color(String name, int def) {
        Setting s = new Setting(name, Type.COLOR); s.color = def; return s;
    }
    public static Setting mode(String name, int def, String... modes) {
        Setting s = new Setting(name, Type.MODE); s.modes = modes; s.mode = def; return s;
    }

    public int i() { return (int) Math.round(n); }
    public float f() { return (float) n; }
    public String modeName() { return modes[mode]; }
    public void cycle() { mode = (mode + 1) % modes.length; }

    public void setNum(double v) {
        if (step > 0) v = Math.round(v / step) * step;
        n = Math.max(min, Math.min(max, v));
    }

    public void write(JsonObject o) {
        switch (type) {
            case BOOL -> o.addProperty(name, b);
            case NUM -> o.addProperty(name, n);
            case COLOR -> o.addProperty(name, color);
            case MODE -> o.addProperty(name, mode);
        }
    }

    public void read(JsonObject o) {
        if (!o.has(name)) return;
        try {
            var e = o.get(name);
            switch (type) {
                case BOOL -> b = e.getAsBoolean();
                case NUM -> setNum(e.getAsDouble());
                case COLOR -> color = e.getAsInt();
                case MODE -> mode = Math.floorMod(e.getAsInt(), modes.length);
            }
        } catch (Exception ignored) {}
    }
}
