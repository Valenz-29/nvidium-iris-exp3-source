package me.cortex.nvidium.compat;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.caffeinemc.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;

import java.lang.reflect.Method;

/**
 * Experimental bridge that binds Iris' framebuffer for the active Sodium terrain pass.
 *
 * <p>The Iris classes are accessed through reflection so Nvidium can still load when
 * Iris is not installed. If the bridge cannot resolve or bind an Iris framebuffer,
 * rendering falls back to the framebuffer supplied by the vanilla/Sodium target.</p>
 */
public final class IrisFramebufferBridge {
    private static final ThreadLocal<TerrainRenderPass> CURRENT_PASS = new ThreadLocal<>();

    private static boolean reflectionInitialized;
    private static boolean bridgeDisabled;
    private static boolean bridgeReported;
    private static boolean failureReported;

    private static Method irisGetPipelineManager;
    private static Method pipelineManagerGetPipelineNullable;
    private static Method worldPipelineGetSodiumPrograms;
    private static Method sodiumProgramsGetFramebuffer;
    private static Method irisFramebufferBind;

    private IrisFramebufferBridge() {
    }

    public static RenderTarget captureTarget(TerrainRenderPass pass) {
        CURRENT_PASS.set(pass);
        return pass.getTarget();
    }

    public static void bindFramebuffer(int target, int fallbackFramebuffer) {
        TerrainRenderPass pass = CURRENT_PASS.get();

        if (!bridgeDisabled && pass != null) {
            try {
                initializeReflection();

                Object pipelineManager = irisGetPipelineManager.invoke(null);
                Object pipeline = pipelineManagerGetPipelineNullable.invoke(pipelineManager);
                if (pipeline != null) {
                    Object sodiumPrograms = worldPipelineGetSodiumPrograms.invoke(pipeline);
                    if (sodiumPrograms != null) {
                        Object framebuffer = sodiumProgramsGetFramebuffer.invoke(sodiumPrograms, pass);
                        if (framebuffer != null) {
                            irisFramebufferBind.invoke(framebuffer);
                            if (!bridgeReported) {
                                bridgeReported = true;
                                System.out.println("[Nvidium Iris Exp3] Iris terrain framebuffer bridge active");
                            }
                            return;
                        }
                    }
                }
            } catch (Throwable throwable) {
                bridgeDisabled = true;
                if (!failureReported) {
                    failureReported = true;
                    System.err.println("[Nvidium Iris Exp3] Iris framebuffer bridge failed; disabling bridge and using vanilla target");
                    throwable.printStackTrace();
                }
            }
        }

        GlStateManager._glBindFramebuffer(target, fallbackFramebuffer);
    }

    private static void initializeReflection() throws ReflectiveOperationException {
        if (reflectionInitialized) {
            return;
        }

        Class<?> irisClass = Class.forName("net.irisshaders.iris.Iris");
        Class<?> pipelineManagerClass = Class.forName("net.irisshaders.iris.pipeline.PipelineManager");
        Class<?> worldPipelineClass = Class.forName("net.irisshaders.iris.pipeline.WorldRenderingPipeline");
        Class<?> sodiumProgramsClass = Class.forName("net.irisshaders.iris.pipeline.programs.SodiumPrograms");
        Class<?> irisFramebufferClass = Class.forName("net.irisshaders.iris.gl.framebuffer.GlFramebuffer");

        irisGetPipelineManager = irisClass.getMethod("getPipelineManager");
        pipelineManagerGetPipelineNullable = pipelineManagerClass.getMethod("getPipelineNullable");
        worldPipelineGetSodiumPrograms = worldPipelineClass.getMethod("getSodiumPrograms");
        sodiumProgramsGetFramebuffer = sodiumProgramsClass.getMethod("getFramebuffer", TerrainRenderPass.class);
        irisFramebufferBind = irisFramebufferClass.getMethod("bind");
        reflectionInitialized = true;
    }
}
