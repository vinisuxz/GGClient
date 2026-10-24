package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object FpsHud {
    fun register() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Text) {
        if (!Mods.fps.ativo) return
        val mc = Minecraft.getMinecraft()
        if (mc.gameSettings.showDebugInfo) return
        val fps = Minecraft.getDebugFPS()
        mc.fontRendererObj.drawStringWithShadow("FPS: $fps", 4f, 4f, 0xFFFFFF)
    }
}
