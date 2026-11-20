package de.zannagh.armorhider.client.render;

//? if >= 26.4-0.snapshot.1 {
/*import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.ExternalTextureProvider;
import net.minecraft.client.renderer.texture.TextureResources;
import net.minecraft.resources.Identifier;

import java.util.function.Supplier;

// 26.4 removed DynamicTexture and TextureManager.register(Identifier, AbstractTexture). An in-memory image
// is now uploaded with TextureResources.from2dImage and registered through an ExternalTextureProvider
// (the same path vanilla's SkinTextureDownloader uses). ExternalTextureProvider.releaseTexture is a
// no-op, so TextureManager.release(id) alone would leak the GPU texture: the TextureResources are kept
// per id here and closed on release(id) or when the id is registered again.
// (Line comments on purpose: the class body sits inside a stonecutter block comment before 26.4.)
final class ImageTextures {

    private static final OwnedTextureRegistry<Identifier, TextureResources> OWNED =
            new OwnedTextureRegistry<>(TextureResources::close);

    private ImageTextures() {}

    // Uploads image under id and closes it: unlike DynamicTexture, TextureResources does not take
    // ownership of the NativeImage, so it is closed here even when the upload throws.
    static void registerAndClose(Identifier id, Supplier<String> label, NativeImage image) {
        try {
            TextureResources resources = TextureResources.from2dImage(label, image);
            OWNED.register(id, resources, owned -> Minecraft.getInstance().getTextureManager()
                    .registerProvider(id, new ExternalTextureProvider(owned)));
        } finally {
            image.close();
        }
    }

    // Replacement for TextureManager.release(id) on ids registered through registerAndClose.
    static void release(Identifier id) {
        OWNED.release(id, Minecraft.getInstance().getTextureManager()::release);
    }
}
*///?}
