package it.yuruni.kariview.client.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private final Class<?> base;
    private final String typeFieldName;
    private final Map<String, Class<?>> labelToSubtype = new LinkedHashMap<>();
    private final Map<Class<?>, String> subtypeToLabel = new LinkedHashMap<>();

    private RuntimeTypeAdapterFactory(Class<?> base, String typeFieldName) {
        if (typeFieldName == null || base == null) {
            throw new NullPointerException();
        }
        this.base = base;
        this.typeFieldName = typeFieldName;
    }

    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> base, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<>(base, typeFieldName);
    }

    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
        if (type == null || label == null) {
            throw new NullPointerException();
        }
        if (subtypeToLabel.containsKey(type) || labelToSubtype.containsKey(label)) {
            throw new IllegalArgumentException("types and labels must be unique");
        }
        labelToSubtype.put(label, type);
        subtypeToLabel.put(type, label);
        return this;
    }

    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> typeToken) {
        if (typeToken.getRawType() != base) {
            return null;
        }

        final Map<String, TypeAdapter<?>> labelToDelegate = new LinkedHashMap<>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new LinkedHashMap<>();
        for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }

        return new TypeAdapter<R>() {
            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                String label = subtypeToLabel.get(srcType);
                TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                if (label == null || delegate == null) {
                    throw new IllegalArgumentException("cannot serialize " + srcType.getName()
                            + "; did you forget to register a subtype?");
                }
                JsonObject jsonObject = delegate.toJsonTree(value).getAsJsonObject();
                if (jsonObject.has(typeFieldName)) {
                    throw new IllegalArgumentException("Type field name '" + typeFieldName + "' is already in use.");
                }
                JsonObject clone = jsonObject.deepCopy();
                clone.add(typeFieldName, new JsonPrimitive(label));
                out.jsonValue(clone.toString());
            }

            @Override
            public R read(JsonReader in) throws IOException {
                JsonElement jsonElement = com.google.gson.JsonParser.parseReader(in);
                if (!jsonElement.isJsonObject()) {
                    throw new IOException("JSON element must be an object.");
                }
                JsonObject jsonObject = jsonElement.getAsJsonObject();
                JsonElement typeElement = jsonObject.get(typeFieldName);
                if (typeElement == null || !typeElement.isJsonPrimitive()) {
                    throw new IOException("Missing or invalid type field: " + typeFieldName);
                }
                String label = typeElement.getAsString();
                Class<?> subtype = labelToSubtype.get(label);
                if (subtype == null) {
                    throw new IOException("Unknown type label: " + label);
                }
                TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(label);
                if (delegate == null) {
                    throw new IOException("No adapter found for type: " + label);
                }
                return delegate.fromJsonTree(jsonObject);
            }
        }.nullSafe();
    }
}
