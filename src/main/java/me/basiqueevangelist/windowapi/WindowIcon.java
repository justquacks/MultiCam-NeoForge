package me.basiqueevangelist.windowapi;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface WindowIcon {
    static WindowIcon fromResources(ResourceLocation... iconIds) {
        return fromResources(List.of(iconIds));
    }

    static WindowIcon fromResources(List<ResourceLocation> iconIds) {
        return () -> {
            List<NativeImage> images = new ArrayList<>(iconIds.size());
            var rm = Minecraft.getInstance().getResourceManager();
            for (var id : iconIds) {
                var res = rm.getResource(id);
                if (res.isEmpty()) continue;
                try {
                    images.add(NativeImage.read(res.get().open()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            return images;
        };
    }

    List<NativeImage> listIconImages();

    default boolean closeAfterUse() {
        return true;
    }
}
