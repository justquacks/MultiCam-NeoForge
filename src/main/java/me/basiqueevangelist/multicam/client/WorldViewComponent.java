package me.basiqueevangelist.multicam.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import me.basiqueevangelist.multicam.client.owocode.AnimatableProperty;
import me.basiqueevangelist.windowapi.context.CurrentWindowContext;
import me.basiqueevangelist.windowapi.util.GlUtil;
import me.basiqueevangelist.multicam.mixin.client.CameraAccessor;
import java.lang.reflect.Method;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL32;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class WorldViewComponent {
    @ApiStatus.Internal
    public static com.mojang.blaze3d.pipeline.RenderTarget CURRENT_BUFFER = null;

    @ApiStatus.Internal
    public static WorldViewComponent CURRENT = null;

    private final Minecraft client = Minecraft.getInstance();
    com.mojang.blaze3d.pipeline.RenderTarget framebuffer = null;
    private final List<BiConsumer<Integer, Integer>> resizeListeners = new ArrayList<>();

    public final AnimatableProperty<AnimatableVec3d> position = AnimatableProperty.of(new AnimatableVec3d(client.gameRenderer.getMainCamera().getPosition()));
    public final AnimatableProperty<AnimatableFloat> yaw = AnimatableProperty.of(new AnimatableFloat(client.gameRenderer.getMainCamera().getYRot()));
    public final AnimatableProperty<AnimatableFloat> pitch = AnimatableProperty.of(new AnimatableFloat(client.gameRenderer.getMainCamera().getXRot()));

    public final AnimatableProperty<AnimatableFloat> fov = AnimatableProperty.of(new AnimatableFloat(client.options.fov().get()));

    private boolean disableEntities = false;
    private boolean disableBlockEntities = false;
    private boolean disableParticles = false;

    private int width = 0;
    private int height = 0;

    public Vec3 position() {
        return position.get().inner();
    }

    public float yaw() {
        return yaw.get().inner();
    }

    public float pitch() {
        return pitch.get().inner();
    }

    public float fov() {
        return fov.get().inner();
    }

    public boolean disableEntities() {
        return disableEntities;
    }

    public boolean disableBlockEntities() {
        return disableBlockEntities;
    }

    public boolean disableParticles() {
        return disableParticles;
    }

    public WorldViewComponent position(Vec3 position) {
        this.position.set(new AnimatableVec3d(position));
        return this;
    }

    public WorldViewComponent yaw(float yaw) {
        this.yaw.set(new AnimatableFloat(yaw));
        return this;
    }

    public WorldViewComponent pitch(float pitch) {
        this.pitch.set(new AnimatableFloat(pitch));
        return this;
    }

    public WorldViewComponent fov(float fov) {
        this.fov.set(new AnimatableFloat(fov));
        return this;
    }

    public WorldViewComponent disableEntities(boolean disableEntities) {
        this.disableEntities = disableEntities;
        return this;
    }

    public WorldViewComponent disableBlockEntities(boolean disableBlockEntities) {
        this.disableBlockEntities = disableBlockEntities;
        return this;
    }

    public WorldViewComponent disableParticles(boolean disableParticles) {
        this.disableParticles = disableParticles;
        return this;
    }

    protected void moveBy(float f, float g, float h, boolean rotate) {
        Vector3f vector3f = new Vector3f(h, g, -f);

        if (rotate) {
            var rotation = new Quaternionf();
            rotation.rotationYXZ((float) Math.PI - yaw() * (float) (Math.PI / 180.0), -pitch() * (float) (Math.PI / 180.0), 0.0F);
            vector3f.rotate(rotation);
        }

        position(new Vec3(position().x + vector3f.x, position().y + vector3f.y, position().z + vector3f.z));
    }

    public void lookAt(Vec3 target) {
        Vec3 rad = target.subtract(position());

        yaw((float) (Math.atan2(rad.z, rad.x) * 180 / Math.PI - 90));
        pitch((float) (-Math.atan2(rad.y, new Vec3(rad.x, 0, rad.z).length()) * 180 / Math.PI));

    }

    public void update(float delta) {
        this.position.update(delta);
        this.yaw.update(delta);
        this.pitch.update(delta);
        this.fov.update(delta);
    }

    public void resize(int width, int height) {
        if (this.framebuffer != null)
            this.framebuffer.destroyBuffers();

        int realWidth = (int) (CurrentWindowContext.current().scaleFactor() * width);
        int realHeight = (int) (CurrentWindowContext.current().scaleFactor() * height);

        try (var ignored = GlUtil.setContext(Minecraft.getInstance().getWindow().getWindow())) {
            this.framebuffer = new com.mojang.blaze3d.pipeline.TextureTarget(
                realWidth, 
                realHeight, 
                true,
                Minecraft.ON_OSX
            );
            this.framebuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            
            GlDebugUtils.labelObject(GL32.GL_FRAMEBUFFER, this.framebuffer.frameBufferId, "Framebuffer for " + this);
        }
        
        resizeListeners.forEach(x -> x.accept(realWidth, realHeight));

        this.width = width;
        this.height = height;
    }

    public void draw(GuiGraphics context, int x, int y) {
        if (framebuffer == null) {
            return;
        }
        
        try (var mainCtx = GlUtil.setContext(Minecraft.getInstance().getWindow().getWindow())) {
            try (var ignored = GlDebugUtils.pushGroup("Drawing world into FB for " + this)) {
                int oldFb = GL32.glGetInteger(GL32.GL_DRAW_FRAMEBUFFER_BINDING);
                int viewportX = GlStateManager.Viewport.x();
                int viewportY = GlStateManager.Viewport.y();
                int viewportW = GlStateManager.Viewport.width();
                int viewportH = GlStateManager.Viewport.height();

                framebuffer.bindWrite(true);
                framebuffer.clear(Minecraft.ON_OSX);

                CURRENT_BUFFER = framebuffer;
                CURRENT = this;
                ResizeHacks.resize(client.gameRenderer, this);

                GlStateManager._disableScissorTest();

                RenderSystem.getModelViewStack().pushMatrix();
                RenderSystem.getModelViewStack().identity();
                RenderSystem.applyModelViewMatrix();

                var camera = client.gameRenderer.getMainCamera();
                PoseStack matrixStack = new PoseStack();
                Matrix4f projectionMatrix = client.gameRenderer.getProjectionMatrix(fov());
                matrixStack.mulPose(projectionMatrix);

                Matrix4f matrix4f = matrixStack.last().pose();
                try (var ignored1 = GlUtil.setProjectionMatrix(matrix4f)) {
                    Vec3 oldPos = camera.getPosition();
                    float oldYaw = camera.getYRot();
                    float oldPitch = camera.getXRot();
                    
                    try {
                        Method setPositionMethod = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                        setPositionMethod.setAccessible(true);
                        setPositionMethod.invoke(camera, this.position());
                        
                        Method setRotationMethod = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
                        setRotationMethod.setAccessible(true);
                        setRotationMethod.invoke(camera, this.yaw(), this.pitch());
                    } catch (Exception e) {
                        System.err.println("Failed to set camera: " + e.getMessage());
                    }

                    PoseStack matrices = new PoseStack();
                    matrices.mulPose(com.mojang.math.Axis.XP.rotationDegrees(camera.getXRot()));
                    matrices.mulPose(com.mojang.math.Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));

                    Matrix4f cameraRotation = new Matrix4f().rotation(camera.rotation().conjugate(new Quaternionf()));

                    this.client.levelRenderer.prepareCullFrustum(camera.getPosition(), cameraRotation, client.gameRenderer.getProjectionMatrix(fov()));
                    
                    client.levelRenderer.renderLevel(
                        client.getTimer(),
                        false,
                        camera,
                        client.gameRenderer,
                        client.gameRenderer.lightTexture(),
                        cameraRotation,
                        matrix4f
                    );
                    
                    try {
                        Method setPositionMethod = Camera.class.getDeclaredMethod("setPosition", Vec3.class);
                        setPositionMethod.setAccessible(true);
                        setPositionMethod.invoke(camera, oldPos);
                        
                        Method setRotationMethod = Camera.class.getDeclaredMethod("setRotation", float.class, float.class);
                        setRotationMethod.setAccessible(true);
                        setRotationMethod.invoke(camera, oldYaw, oldPitch);
                    } catch (Exception e) {
                        System.err.println("Failed to restore camera: " + e.getMessage());
                    }
                }

                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
                GlStateManager._glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, oldFb);
                GlStateManager._viewport(viewportX, viewportY, viewportW, viewportH);
                GlStateManager._enableScissorTest();
                CURRENT_BUFFER = null;
                CURRENT = null;
                ResizeHacks.resize(client.gameRenderer, null);
            }
        } 
        
        int currentFb = GL32.glGetInteger(GL32.GL_DRAW_FRAMEBUFFER_BINDING);
        System.out.println("Current draw framebuffer when drawing quad: " + currentFb);
        System.out.println("Expected framebuffer: " + CurrentWindowContext.current().framebuffer().frameBufferId);
        
        CurrentWindowContext.current().framebuffer().bindWrite(false);
        
        RenderSystem.setShader(() -> MultiCam.WORLD_VIEW_PROGRAM);
        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, framebuffer.getColorTextureId());
        
        Matrix4f matrix4f = context.pose().last().pose();
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, x, y, 0).setUv(0, 1);
        bufferBuilder.addVertex(matrix4f, x, y + height, 0).setUv(0, 0);
        bufferBuilder.addVertex(matrix4f, x + width, y + height, 0).setUv(1, 0);
        bufferBuilder.addVertex(matrix4f, x + width, y, 0).setUv(1, 1);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    void whenResized(BiConsumer<Integer, Integer> onResized) {
        resizeListeners.add(onResized);
    }
}
