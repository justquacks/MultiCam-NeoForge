package me.basiqueevangelist.multicam.mixin.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(PostChain.class)
public interface PostEffectProcessorAccessor {
    
    @Accessor("screenWidth")
    int getWidth();
    
    @Accessor("screenWidth")
    void setWidth(int width);
    
    @Accessor("screenHeight")
    int getHeight();
    
    @Accessor("screenHeight")
    void setHeight(int height);
    
    @Accessor("shaderOrthoMatrix")
    Matrix4f getProjectionMatrix();
    
    @Accessor("shaderOrthoMatrix")
    void setProjectionMatrix(Matrix4f matrix);
    
    @Accessor("passes")
    List<PostPass> getPasses();
    
    @Accessor("fullSizedTargets")
    List<RenderTarget> getDefaultSizedTargets();
    
    @Accessor("screenTarget")
    RenderTarget getMainTarget();
    
    @Accessor("customRenderTargets")
    Map<String, RenderTarget> getCustomRenderTargets();
}
