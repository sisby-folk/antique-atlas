package folk.sisby.antique_atlas.client.resource.reloader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import folk.sisby.antique_atlas.AntiqueAtlas;
import folk.sisby.antique_atlas.client.MarkerType;
import folk.sisby.antique_atlas.client.resource.MarkerTypes;
import folk.sisby.antique_atlas.resource.reloader.ResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Maps marker type to texture.
 *
 * @author Hunternif
 */
public class MarkerTextureConfig implements ResourceReloadListener<Map<Identifier, MarkerType>> {
    public static final Identifier ID = AntiqueAtlas.id("markers");
    private static final int VERSION = 1;
    private static final JsonParser parser = new JsonParser();

    @Override
    public CompletableFuture<Map<Identifier, MarkerType>> load(ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            Map<Identifier, MarkerType> typeMap = new HashMap<>();

            for (Identifier id : manager.findResources("atlas/markers", id -> id.toString().endsWith(".json")).keySet()) {
                Identifier markerId = new Identifier(
                    id.getNamespace(),
                    id.getPath().replace("atlas/markers/", "").replace(".json", "")
                );

                try {
                    Resource resource = manager.getResource(id).orElseThrow(IOException::new);
                    try (
                        InputStream stream = resource.getInputStream();
                        InputStreamReader reader = new InputStreamReader(stream)
                    ) {
                        JsonObject object = parser.parse(reader).getAsJsonObject();

                        int version = object.getAsJsonPrimitive("version").getAsInt();

                        if (version != VERSION) {
                            throw new RuntimeException("Incompatible version (" + VERSION + " != " + version + ")");
                        }

                        MarkerType markerType = new MarkerType(markerId);
                        markerType.getJSONData().readFrom(object);
                        typeMap.put(markerId, markerType);
                    }
                } catch (Exception e) {
                    AntiqueAtlas.LOG.warn("Error reading marker " + markerId + "!", e);
                }
            }

            return typeMap;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> apply(Map<Identifier, MarkerType> data, ResourceManager manager, Profiler profiler, Executor executor) {
        return CompletableFuture.runAsync(() -> {
            for (Map.Entry<Identifier, MarkerType> entry : data.entrySet()) {
                MarkerTypes.register(entry.getKey(), entry.getValue());
            }
        }, executor);
    }

    @Override
    public String getName() {
        return ID.toString();
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Collection<Identifier> getDependencies() {
        return Collections.emptyList();
    }
}
