package me.basiqueevangelist.windowapi;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import me.basiqueevangelist.windowapi.context.CurrentWindowContext;
import me.basiqueevangelist.windowapi.context.WindowContext;
import me.basiqueevangelist.windowapi.util.GlUtil;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.MainTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.GsonHelper;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.InputConstants;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.glfw.GLFW.*;

public abstract class AltWindow extends SupportsFeaturesImpl<WindowContext> implements WindowContext, Renderable, GuiEventListener {
    private static final boolean USE_GLOBAL_POS = glfwGetPlatform() != GLFW_PLATFORM_WAYLAND;

    private String title = "window api window";
    private int screenWidth = 854;
    private int screenHeight = 480;
    private WindowIcon icon = null;
    private final List<Pair<Integer, Integer>> windowHints = new ArrayList<>();

    private int framebufferWidth;
    private int framebufferHeight;

    private long handle = 0;
    private RenderTarget framebuffer;
    private int localFramebuffer = 0;
    private final List<NativeResource> disposeList = new ArrayList<>();
    private final Minecraft client = Minecraft.getInstance();

    private int scaleFactor;
    private int scaledWidth;
    private int scaledHeight;

    private int mouseX = -1;
    private int mouseY = -1;
    private int globalMouseX = -1;
    private int globalMouseY = -1;
    private int deltaX = 0;
    private int deltaY = 0;
    private int activeButton = -1;

    private boolean cursorLocked = false;

    private final int[] globalX = new int[1];
    private final int[] globalY = new int[1];

    private final List<Consumer<WindowFramebufferResized>> framebufferResizedEvents = new ArrayList<>();

    public AltWindow() {

    }

    //region User-implementable stuff
    protected abstract void build();

    protected abstract void resize(int newWidth, int newHeight);

    protected void lockedMouseMoved(double xDelta, double yDelta) { }
    //endregion

    public AltWindow size(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        if (this.handle != 0) {
            glfwSetWindowSize(this.handle, screenWidth, screenHeight);
        }

        return this;
    }

    public AltWindow title(String title) {
        this.title = title;

        if (this.handle != 0) {
            glfwSetWindowTitle(this.handle, title);
        }

        return this;
    }

    public AltWindow icon(WindowIcon icon) {
        this.icon = icon;

        if (this.handle != 0) {
            applyIcon();
        }

        return this;
    }

    public AltWindow windowHint(int hint, int value) {
        if (this.handle != 0) {
            throw new IllegalStateException("Tried to add window hint after window was opened");
        }

        windowHints.add(Pair.of(hint, value));

        return this;
    }

    public void open() {
        try (var ignored = GlUtil.setContext(0)) {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_API);
            glfwWindowHint(GLFW_CONTEXT_CREATION_API, GLFW_NATIVE_CONTEXT_API);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, 1);

            for (var hint : windowHints) {
                glfwWindowHint(hint.getLeft(), hint.getRight());
            }

            this.handle = glfwCreateWindow(this.screenWidth, this.screenHeight, this.title, 0, Minecraft.getInstance().getWindow().getWindow());

            if (this.handle == 0) {
                throw new IllegalStateException("OwoWindow creation failed due to GLFW error");
            }

            glfwMakeContextCurrent(this.handle);
            glfwSwapInterval(0);
        }

        applyIcon();

        int[] framebufferWidthArr = new int[1];
        int[] framebufferHeightArr = new int[1];
        glfwGetFramebufferSize(this.handle, framebufferWidthArr, framebufferHeightArr);
        this.framebufferWidth = framebufferWidthArr[0];
        this.framebufferHeight = framebufferHeightArr[0];

        // FIX: Create framebuffer in THIS window's context, not the main window's
        try (var ignored = GlUtil.setContext(this.handle)) {
            this.framebuffer = new MainTarget(this.framebufferWidth, this.framebufferHeight);
            // GlDebug is no longer available in 1.21, skip this
        }

        initLocalFramebuffer();

        glfwSetWindowCloseCallback(handle, stowAndReturn(GLFWWindowCloseCallback.create(window -> {
            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                this.close();
            }
        })));

        glfwSetWindowSizeCallback(handle, stowAndReturn(GLFWWindowSizeCallback.create((window, width, height) -> {
            this.screenWidth = width;
            this.screenHeight = height;
        })));

        glfwSetFramebufferSizeCallback(handle, stowAndReturn(GLFWFramebufferSizeCallback.create((window, width, height) -> {
            if (this.framebufferWidth == width && this.framebufferHeight == height) return;

            if (width == 0 || height == 0) return;

            this.framebufferWidth = width;
            this.framebufferHeight = height;

            // FIX: Recreate framebuffer in THIS window's context, not the main window's
            try (var ignored = GlUtil.setContext(this.handle)) {
                framebuffer.destroyBuffers();
                this.framebuffer = new MainTarget(width, height);
            }

            initLocalFramebuffer();

            recalculateScale();

            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                this.resize(scaledWidth(), scaledHeight());

                for (var listener : framebufferResizedEvents) {
                    listener.accept(new WindowFramebufferResized(width, height));
                }
            }
        })));

        glfwSetCursorPosCallback(handle, stowAndReturn(GLFWCursorPosCallback.create((window, xpos, ypos) -> {
            if (cursorLocked) {
                this.lockedMouseMoved(xpos - mouseX * scaleFactor, ypos - mouseY * scaleFactor);
                GLFW.glfwSetCursorPos(handle(), mouseX * scaleFactor, mouseY * scaleFactor);
                return;
            }

            int newX = (int) (xpos / scaleFactor);
            int newY = (int) (ypos / scaleFactor);

            if (!USE_GLOBAL_POS) {
                this.deltaX += newX - mouseX;
                this.deltaY += newY - mouseY;
            }

            this.mouseY = newY;
            this.mouseX = newX;

            if (USE_GLOBAL_POS) {
                glfwGetWindowPos(handle, this.globalX, this.globalY);
                int newGlobalX = (int) ((this.globalX[0] + xpos) / scaleFactor);
                int newGlobalY = (int) ((this.globalY[0] + ypos) / scaleFactor);

                this.deltaX += newGlobalX - this.globalMouseX;
                this.deltaY += newGlobalY - this.globalMouseY;

                this.globalMouseX = newGlobalX;
                this.globalMouseY = newGlobalY;
            }
        })));

        glfwSetMouseButtonCallback(handle, stowAndReturn(GLFWMouseButtonCallback.create((window, button, action, mods) -> {
            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                if (action == GLFW_RELEASE) {
                    this.activeButton = -1;

                    this.mouseReleased(mouseX, mouseY, button);
                } else {
                    this.activeButton = button;

                    this.mouseClicked(mouseX, mouseY, button);
                }
            }
        })));

        glfwSetScrollCallback(handle, stowAndReturn(GLFWScrollCallback.create((window, xoffset, yoffset) -> {
            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                double yAmount = (client.options.discreteMouseScroll().get() ? Math.signum(yoffset) : yoffset)
                    * client.options.mouseWheelSensitivity().get();
                double xAmount = (client.options.discreteMouseScroll().get() ? Math.signum(xoffset) : xoffset)
                    * client.options.mouseWheelSensitivity().get();
                this.mouseScrolled(mouseX, mouseY, xAmount, yAmount);
            }
        })));

        glfwSetKeyCallback(handle, stowAndReturn(GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                if (action == GLFW_RELEASE) {
                    this.keyReleased(key, scancode, mods);
                } else {
                    this.keyPressed(key, scancode, mods);
                }
            }
        })));

        glfwSetCharModsCallback(handle, stowAndReturn(GLFWCharModsCallback.create((window, codepoint, mods) -> {
            try (var ignored = CurrentWindowContext.setCurrent(this)) {
                this.charTyped((char) codepoint, mods);
            }
        })));

        recalculateScale();

        try (var ignored = CurrentWindowContext.setCurrent(this)) {
            build();
        }

        OpenWindows.add(this);
    }

    public boolean cursorLocked() {
        return cursorLocked;
    }

    public void lockCursor() {
        if (cursorLocked) return;

        this.cursorLocked = true;
        this.mouseX = scaledWidth / 2;
        this.mouseY = scaledHeight / 2;
        InputConstants.grabOrReleaseMouse(handle, GLFW_CURSOR_DISABLED, this.mouseX * scaleFactor, this.mouseY * scaleFactor);
    }

    public void unlockCursor() {
        if (!cursorLocked) return;

        this.mouseX = scaledWidth / 2;
        this.mouseY = scaledHeight / 2;
        InputConstants.grabOrReleaseMouse(handle, GLFW_CURSOR_NORMAL, this.mouseX * scaleFactor, this.mouseY * scaleFactor);
        this.cursorLocked = false;
    }

    private <T extends NativeResource> T stowAndReturn(T resource) {
        this.disposeList.add(resource);
        return resource;
    }

    private void applyIcon() {
        if (icon == null) return;

        List<NativeImage> icons = icon.listIconImages();

        List<ByteBuffer> freeList = new ArrayList<>(icons.size());
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            GLFWImage.Buffer buffer = GLFWImage.malloc(icons.size(), memoryStack);

            for (int i = 0; i < icons.size(); i++) {
                NativeImage icon = icons.get(i);
                ByteBuffer imgBuffer = MemoryUtil.memAlloc(icon.getWidth() * icon.getHeight() * 4);
                freeList.add(imgBuffer);
                imgBuffer.asIntBuffer().put(icon.getPixelsRGBA());

                buffer
                    .position(i)
                    .width(icon.getWidth())
                    .height(icon.getHeight())
                    .pixels(imgBuffer);
            }

            GLFW.glfwSetWindowIcon(this.handle, buffer.position(0));
        } finally {
            freeList.forEach(MemoryUtil::memFree);

            if (icon.closeAfterUse())
                icons.forEach(NativeImage::close);
        }
    }

    private void initLocalFramebuffer() {
        try (var ignored = GlUtil.setContext(this.handle)) {
            if (localFramebuffer != 0) {
                GL32.glDeleteFramebuffers(localFramebuffer);
            }

            this.localFramebuffer = GL32.glGenFramebuffers();
            GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, this.localFramebuffer);
            GL32.glFramebufferTexture2D(GL32.GL_FRAMEBUFFER, GL32.GL_COLOR_ATTACHMENT0, GL32.GL_TEXTURE_2D, this.framebuffer.getColorTextureId(), 0);

            int status = GL32.glCheckFramebufferStatus(GL32.GL_FRAMEBUFFER);
            if (status != GL32.GL_FRAMEBUFFER_COMPLETE)
                throw new IllegalStateException("Failed to create local framebuffer!");
        }
    }

    public void recalculateScale() {
        int guiScale = Minecraft.getInstance().options.guiScale().get();
        boolean forceUnicodeFont = Minecraft.getInstance().options.forceUnicodeFont().get();

        int factor = 1;

        while (
            factor != guiScale
                && factor < this.framebufferWidth()
                && factor < this.framebufferHeight()
                && this.framebufferWidth() / (factor + 1) >= 320
                && this.framebufferHeight() / (factor + 1) >= 240
        ) {
            ++factor;
        }

        if (forceUnicodeFont && factor % 2 != 0) {
            ++factor;
        }

        this.scaleFactor = factor;
        this.scaledWidth = (int) Math.ceil((double) this.framebufferWidth() / scaleFactor);
        this.scaledHeight = (int) Math.ceil((double) this.framebufferHeight() / scaleFactor);
    }

    private void tickMouse() {
        if (deltaX == 0 && this.deltaY == 0) return;

        this.mouseMoved(mouseX, mouseY);

        if (activeButton != -1) this.mouseDragged(mouseX, mouseY, activeButton, deltaX, deltaY);

        deltaX = 0;
        deltaY = 0;
    }

    public void draw() {
        if (closed()) return;

        try (var ignored = CurrentWindowContext.setCurrent(this)) {
            tickMouse();

            framebuffer().bindWrite(true);

            RenderSystem.clearColor(0, 0, 0, 0);
            RenderSystem.clear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

            Matrix4f matrix4f = new Matrix4f()
                .setOrtho(
                    0.0F,
                    scaledWidth(),
                    scaledHeight(),
                    0.0F,
                    1000.0F,
                    21000.0F
                );

            try (var ignored2 = GlUtil.setProjectionMatrix(matrix4f)) {
                Matrix4fStack matrixStack = RenderSystem.getModelViewStack();
                matrixStack.pushMatrix();
                matrixStack.identity();
                matrixStack.translate(0.0F, 0.0F, -11000.0F);
                RenderSystem.applyModelViewMatrix();
                
                client.gameRenderer.lightTexture().turnOnLightLayer();

                MultiBufferSource.BufferSource consumers = client.renderBuffers().bufferSource();
                this.render(new GuiGraphics(client, consumers), mouseX, mouseY, client.getTimer().getGameTimeDeltaPartialTick(false));
                consumers.endBatch();

                RenderSystem.getModelViewStack().popMatrix();
                RenderSystem.applyModelViewMatrix();
            }

            framebuffer.unbindWrite();
        }
    }

    void present() {
        if (closed()) return;

        try (var ignored = GlUtil.setContext(this.handle)) {
            // Bind framebuffers for blitting
            GL32.glBindFramebuffer(GL32.GL_READ_FRAMEBUFFER, localFramebuffer);
            GL32.glBindFramebuffer(GL32.GL_DRAW_FRAMEBUFFER, 0);

            // Clear the window's default framebuffer
            GL32.glClearColor(1, 1, 1, 1);
            GL32.glClear(GL32.GL_COLOR_BUFFER_BIT | GL32.GL_DEPTH_BUFFER_BIT);
            
            // Blit from our framebuffer to the window
            GL32.glBlitFramebuffer(
                0, 0, this.framebufferWidth, this.framebufferHeight, 
                0, 0, this.framebufferWidth, this.framebufferHeight, 
                GL32.GL_COLOR_BUFFER_BIT, GL32.GL_NEAREST
            );

            // Swap buffers
            GLFW.glfwSwapBuffers(this.handle);
        }
    }

    @Override
    public long handle() {
        return handle;
    }

    public void addFramebufferResizedListener(Consumer<WindowFramebufferResized> listener) {
        framebufferResizedEvents.add(listener);
    }

    @Override
    public void onFramebufferResized(Consumer<WindowContext> listener) {
        framebufferResizedEvents.add(event -> listener.accept(this));
    }

    @Override
    public RenderTarget framebuffer() {
        return framebuffer;
    }

    @Override
    public int framebufferWidth() {
        return framebufferWidth;
    }

    @Override
    public int framebufferHeight() {
        return framebufferHeight;
    }

    @Override
    public double scaleFactor() {
        return scaleFactor;
    }

    @Override
    public int scaledWidth() {
        return scaledWidth;
    }

    @Override
    public int scaledHeight() {
        return scaledHeight;
    }

    public boolean closed() {
        return this.handle == 0;
    }

    public void close() {
        this.destroyFeatures();
        OpenWindows.remove(this);

        try (var ignored = GlUtil.setContext(this.handle)) {
            GL32.glDeleteFramebuffers(this.localFramebuffer);
        }

        this.framebuffer.destroyBuffers();
        glfwDestroyWindow(this.handle);
        this.handle = 0;

        this.disposeList.forEach(NativeResource::free);
        this.disposeList.clear();
    }

    // GuiEventListener implementation stubs
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
    }

    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }
}
