package com.daratrix.ronapi.utils;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class RenderingUtils {

    private static final Minecraft MC = Minecraft.getInstance();
    private static final ResourceLocation WhiteresourceLocation = ResourceLocation.tryParse("forge:textures/white.png");
    public static final VertexConsumer TranslucentVertexConsumer = MC.renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(WhiteresourceLocation));
    //public static final VertexConsumer TranslucentVertexConsumer = null;
}
