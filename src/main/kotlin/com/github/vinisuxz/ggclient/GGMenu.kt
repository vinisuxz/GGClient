package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class Modulo(val nome: String, var ativo: Boolean = false)

object Mods {
    val fps = Modulo("FPS", true)
    val toggleSprint = Modulo("ToggleSprint")
    val fullbright = Modulo("Fullbright")
    val lista = listOf(fps, toggleSprint, fullbright)
}

class TelaMenu : GuiScreen() {

    private fun texto(mod: Modulo): String =
        mod.nome + ": " + (if (mod.ativo) "ON" else "OFF")

    override fun initGui() {
        buttonList.clear()
        Mods.lista.forEachIndexed { i, mod ->
            buttonList.add(GuiButton(i, width / 2 - 75, 50 + i * 24, 150, 20, texto(mod)))
        }
    }

    override fun actionPerformed(button: GuiButton) {
        val mod = Mods.lista[button.id]
        mod.ativo = !mod.ativo
        button.displayString = texto(mod)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null)
        } else {
            super.keyTyped(typedChar, keyCode)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawCenteredString(fontRendererObj, "GGClient - Mods", width / 2, 25, 0xFFFFFF)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun doesGuiPauseGame(): Boolean = false
}

class Eventos {

    private val mc = Minecraft.getMinecraft()
    private var sprintLigado = false
    private var ctrlAnterior = false
    private var rshiftAnterior = false
    private var gammaOriginal = -1f

    @SubscribeEvent
    fun aoTick(e: TickEvent.ClientTickEvent) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return

        val semTela = mc.currentScreen == null
        val codigoSprint = mc.gameSettings.keyBindSprint.keyCode
        // Fullbright
        if (Mods.fullbright.ativo) {
            if (gammaOriginal < 0f) gammaOriginal = mc.gameSettings.gammaSetting
            mc.gameSettings.gammaSetting = 100f
        } else if (gammaOriginal >= 0f) {
            mc.gameSettings.gammaSetting = gammaOriginal
            gammaOriginal = -1f
        }
        // Right Shift abre o menu
        val rshift = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
        if (rshift && !rshiftAnterior && semTela) {
            mc.displayGuiScreen(TelaMenu())
        }
        rshiftAnterior = rshift

        // Control liga/desliga o sprint (só se o mod estiver ON no menu)
        val ctrl = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
        if (ctrl && !ctrlAnterior && semTela && Mods.toggleSprint.ativo) {
            sprintLigado = !sprintLigado
            if (!sprintLigado) KeyBinding.setKeyBindState(codigoSprint, false)
        }
        ctrlAnterior = ctrl

        // se desligar o mod no menu, para o sprint
        if (!Mods.toggleSprint.ativo && sprintLigado) {
            sprintLigado = false
            KeyBinding.setKeyBindState(codigoSprint, false)
        }

        if (sprintLigado) KeyBinding.setKeyBindState(codigoSprint, true)
    }

    @SubscribeEvent
    fun aoDesenhar(e: RenderGameOverlayEvent.Text) {
        if (Mods.toggleSprint.ativo && sprintLigado) {
            mc.fontRendererObj.drawStringWithShadow("[Sprint: ATIVADO]", 5f, 35f, 0x55FF55)
        }
    }
}
