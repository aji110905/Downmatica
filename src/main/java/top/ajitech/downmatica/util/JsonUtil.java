package top.ajitech.downmatica.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public final class JsonUtil {
    private JsonUtil() {
    }

    @Nullable
    public static String getString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        return element.getAsString();
    }

    public static boolean hasNumber(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber();
    }
}
