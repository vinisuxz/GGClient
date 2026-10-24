package SEU.PACKAGE

import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class ToggleSprint {

    private val mc = Minecraft.getMinecraft()
    private var ativo = false
    private var ctrlAnterior = false

    @SubscribeEvent
    fun aoTick(e: TickEvent.ClientTickEvent) {
        if (e.phase != TickEvent.Phase.END || mc.thePlayer == null) return

        val ctrlApertado = mc.currentScreen == null &&
                Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)

        if (ctrlApertado && !ctrlAnterior) {
            ativo = !ativo
            if (!ativo) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, false)
            }
        }
        ctrlAnterior = ctrlApertado

        if (ativo) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
        }
    }

    @SubscribeEvent
    fun aoDesenhar(e: RenderGameOverlayEvent.Text) {
        if (ativo) {
            mc.fontRendererObj.drawStringWithShadow("[Sprint: ATIVADO]", 5f, 35f, 0x55FF55)
        }
    }
}
