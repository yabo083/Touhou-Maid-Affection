package com.github.touhoumaidaffection.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class YsmModelActionIndexTest {
    @Test
    void shouldReturnEmptyWhenResourceManagerThrowsArrayIndexOutOfBounds() throws Exception {
        ResourceManager manager = createResourceManager((pathPrefix, filter) -> {
            throw new ArrayIndexOutOfBoundsException("modernfix regression simulation");
        });

        List<ResourceLocation> locations = YsmModelActionIndex.listResourceLocationsSafely(manager, "", resource -> true);
        assertEquals(0, locations.size());
    }

    @Test
    void shouldReturnLocationsWhenResourceManagerListResourcesSucceeds() throws Exception {
        ResourceManager manager = createResourceManager((pathPrefix, filter) -> Map.of(
                new ResourceLocation("test", "foo/ysm.json"), new Object()
        ));

        List<ResourceLocation> locations = YsmModelActionIndex.listResourceLocationsSafely(manager, "foo", resource -> true);
        assertFalse(locations.isEmpty());
    }

    private static ResourceManager createResourceManager(ListResourcesBehavior behavior) {
        return (ResourceManager) Proxy.newProxyInstance(
                ResourceManager.class.getClassLoader(),
                new Class[]{ResourceManager.class},
                (proxy, method, args) -> {
                    if ("listResources".equals(method.getName())) {
                        return behavior.apply((String) args[0], (Predicate<ResourceLocation>) args[1]);
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("toString".equals(method.getName())) {
                        return "resourceManagerProxy";
                    }
                    throw new UnsupportedOperationException("Unsupported method: " + method.getName());
                }
        );
    }

    @FunctionalInterface
    private interface ListResourcesBehavior {
        Map<ResourceLocation, ?> apply(String pathPrefix, Predicate<ResourceLocation> filter);
    }
}
