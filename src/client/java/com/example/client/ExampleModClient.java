package com.example;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

public class ExampleModClient implements ClientModInitializer {

    public static boolean enabled = false;
    private static final Set<BlockPos> storages = new HashSet<>();
    private static int tickCounter = 0;

    private static final KeyBinding TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.storageesp.toggle",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_SHIFT,
            "category.storageesp"
    ));

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.wasPressed()) {
                enabled = !enabled;
                if (client.player != null) {
                    String status = enabled ? "§a§lAÇIK" : "§c§lKAPALI";
                    client.player.sendMessage(Text.literal("§6§l[StorageESP] §r" + status), false);
                }
            }
        });

        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            if (!enabled || context.world() == null) return;

            MinecraftClient mc = MinecraftClient.getInstance();
            updateStorages(mc.world);

            MatrixStack matrices = context.matrixStack();
            Vec3d camPos = context.camera().getPos();

            matrices.push();
            matrices.translate(-camPos.x, -camPos.y, -camPos.z);

            var vertexConsumers = mc.getBufferBuilders().getEntityVertexConsumers();

            for (BlockPos pos : storages) {
                BlockEntity be = mc.world.getBlockEntity(pos);
                if (be == null) continue;

                Color color = getColor(be);
                float r = color.getRed() / 255f;
                float g = color.getGreen() / 255f;
                float b = color.getBlue() / 255f;

                Box box = new Box(pos).expand(0.001);

                RenderSystem.disableDepthTest();
                VertexConsumer filled = vertexConsumers.getBuffer(RenderLayer.getDebugQuads());
                WorldRenderer.drawBox(matrices, filled, box, r, g, b, 0.18f);

                RenderSystem.enableDepthTest();
                VertexConsumer outline = vertexConsumers.getBuffer(RenderLayer.getLines());
                WorldRenderer.drawBox(matrices, outline, box, r, g, b, 0.9f);
            }

            matrices.pop();
            vertexConsumers.draw();
        });
    }

    private void updateStorages(net.minecraft.world.World world) {
        if (++tickCounter % 6 != 0) return;
        storages.clear();

        for (BlockEntity be : world.blockEntities) {
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity ||
                be instanceof ShulkerBoxBlockEntity || be instanceof HopperBlockEntity) {
                storages.add(be.getPos().toImmutable());
            }
        }
    }

    private Color getColor(BlockEntity be) {
        if (be instanceof ShulkerBoxBlockEntity) return new Color(0xBB00FF);
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) return new Color(0xFF8800);
        if (be instanceof HopperBlockEntity) return new Color(0x00EEFF);
        return Color.WHITE;
    }
}
