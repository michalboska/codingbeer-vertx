package ch.erni.beer.vertx;


import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Container;

import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Michal Boska on 4. 12. 2014.
 */
public final class Configuration {
    private static final Pattern OPTION_NESTED_PATTERN = Pattern.compile("(.+?)\\.(.*)");

    public static String getString(String key, Container remoteContainer) {
        return (String) getOptionRecursive(key, remoteContainer.config(), JsonObject::getString);
    }

    public static Integer getInteger(String key, Container remoteContainer) {
        return (Integer) getOptionRecursive(key, remoteContainer.config(), JsonObject::getInteger);
    }

    public static JsonArray getArray(String key, Container remoteContainer) {
        return (JsonArray) getOptionRecursive(key, remoteContainer.config(), JsonObject::getArray);
    }

    public static String getMandatoryString(String key, JsonObject jsonObject) {
        checkKeyExists(key, jsonObject);
        return jsonObject.getString(key);
    }

    private static Object getOptionRecursive(String key, JsonObject jsonObject, BiFunction<JsonObject, String, ? extends Object> getterFunction) {
        if (!key.contains(".")) { //simple key, return directly
            checkKeyExists(key, jsonObject);
            return getterFunction.apply(jsonObject, key);
        } else { //complicated expression in form of obj.property.property2 .... etc
            Matcher matcher = OPTION_NESTED_PATTERN.matcher(key);
            if (!matcher.matches()) {
                throw new IllegalArgumentException(key + " is not a valid option expression");
            }
            String immediateKey = matcher.group(1);
            String followingKeys = matcher.group(2);
            JsonObject nestedObject = jsonObject.getObject(immediateKey);
            if (nestedObject == null) {
                throw new IllegalArgumentException("Nested JSON object " + key + " not found");
            }
            return getOptionRecursive(followingKeys, nestedObject, getterFunction);
        }
    }

    private static void checkKeyExists(String key, JsonObject jsonObject) {
        if (!jsonObject.containsField(key)) {
            throw new IllegalArgumentException("Key " + key + " does not exist in this JSON object");
        }
    }

}
