package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.Display

// ---------- OTIMIZAÇÕES DE FPS ----------
class OpcaoBoost(val chave: String, val nome: String) {
    var ativo = false
}

object Boost {
    val opcoes = listOf(
        OpcaoBoost("graficos", "Gráficos rápidos"),
        OpcaoBoost("sombras", "Sem sombras de entidades"),
        OpcaoBoost("luz", "Iluminação simples"),
        OpcaoBoost("ilimitado", "FPS ilimitado"),
        OpcaoBoost("vsync", "VSync desligado"),
        OpcaoBoost("distancia", "Distância curta (6 chunks)")
    )

    // valores originais do jogo, pra voltar quando o switch for desligado
    val originais = HashMap<String, Int>()
    private var recarregar = false

    private fun ler(chave: String): Int {
        val gs = Minecraft.getMinecraft().gameSettings
        return when (chave) {
            "graficos" -> if (gs.fancyGraphics) 1 else 0
            "sombras" -> if (gs.entityShadows) 1 else 0
            "luz" -> gs.ambientOcclusion
            "ilimitado" -> gs.limitFramerate
            "vsync" -> if (gs.enableVsync) 1 else 0
            "distancia" -> gs.renderDistanceChunks
            else -> 0
        }
    }

    private fun escrever(chave: String, v: Int) {
        val mc = Minecraft.getMinecraft()
        val gs = mc.gameSettings
        when (chave) {
            "graficos" -> {
                gs.fancyGraphics = v == 1
                recarregar = true
            }
            "sombras" -> {
                gs.entityShadows = v == 1
            }
            "luz" -> {
                gs.ambientOcclusion = v
                recarregar = true
            }
            "ilimitado" -> {
                gs.limitFramerate = v
            }
            "vsync" -> {
                gs.enableVsync = v == 1
                Display.setVSyncEnabled(v == 1)
            }
            "distancia" -> {
                gs.renderDistanceChunks = v
                mc.renderGlobal.setDisplayListEntitiesDirty()
            }
        }
    }

    private fun valorBoost(chave: String, orig: Int): Int = when (chave) {
        "graficos", "sombras", "luz", "vsync" -> 0
        "ilimitado" -> 260
        "distancia" -> minOf(orig, 6)
        else -> orig
    }

    private fun recarregarSeNecessario() {
        if (recarregar) {
            recarregar = false
            val rg = Minecraft.getMinecraft().renderGlobal
            if (rg != null) rg.loadRenderers()
        }
    }

    fun definir(o: OpcaoBoost, ligar: Boolean) {
        if (ligar == o.ativo) return
        if (ligar) {
            if (!originais.containsKey(o.chave)) originais[o.chave] = ler(o.chave)
            escrever(o.chave, valorBoost(o.chave, originais[o.chave] ?: 0))
        } else {
            val orig = originais[o.chave]
            if (orig != null) escrever(o.chave, orig)
            originais.remove(o.chave)
        }
        o.ativo = ligar
        recarregarSeNecessario()
    }

    // chamado uma vez ao entrar no mundo, pra reaplicar o que estava ligado
    fun aplicarSalvos() {
        for (o in opcoes) {
            if (o.ativo) {
                if (!originais.containsKey(o.chave)) originais[o.chave] = ler(o.chave)
                escrever(o.chave, valorBoost(o.chave, originais[o.chave] ?: 0))
            }
        }
        recarregarSeNecessario()
    }
}

// ---------- AUTOTEXT ----------
object AutoTextDados {
    const val SLOTS = 4
    val teclas = IntArray(SLOTS)
    val msgs = Array(SLOTS) { "" }
}

class TelaAutoText(private val pai: GuiScreen) : GuiScreen() {
    private val campos = ArrayList<GuiTextField>()
    private var esperando = -1

    private fun textoTecla(i: Int): String {
        if (esperando == i) return "> aperte <"
        val k = AutoTextDados.teclas[i]
        return if (k == 0) "Sem tecla" else Keyboard.getKeyName(k)
    }

    private fun atualizaBotoes() {
        for (b in buttonList) {
            if (b.id < AutoTextDados.SLOTS) b.displayString = textoTecla(b.id)
        }
    }

    private fun guardaTextos() {
        for (i in campos.indices) {
            AutoTextDados.msgs[i] = campos[i].text
        }
    }

    override fun initGui() {
        guardaTextos()
        buttonList.clear()
        campos.clear()
        Keyboard.enableRepeatEvents(true)
        val cx = width / 2
        for (i in 0 until AutoTextDados.SLOTS) {
            val y = 55 + i * 30
            buttonList.add(GuiButton(i, cx - 145, y, 80, 20, textoTecla(i)))
            val c = GuiTextField(100 + i, fontRendererObj, cx - 60, y, 205, 20)
            c.setMaxStringLength(100)
            c.text = AutoTextDados.msgs[i]
            campos.add(c)
        }
        buttonList.add(GuiButton(50, cx - 75, 55 + AutoTextDados.SLOTS * 30 + 10, 150, 20, "Voltar"))
    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id == 50) {
            mc.displayGuiScreen(pai)
        } else if (button.id < AutoTextDados.SLOTS) {
            esperando = button.id
            atualizaBotoes()
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        for (c in campos) c.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (esperando >= 0) {
            val i = esperando
            if (keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_DELETE) {
                AutoTextDados.teclas[i] = 0
            } else if (keyCode != Keyboard.KEY_ESCAPE) {
                AutoTextDados.teclas[i] = keyCode
            }
            esperando = -1
            atualizaBotoes()
            return
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(pai)
            return
        }
        for (c in campos) {
            if (c.isFocused) c.textboxKeyTyped(typedChar, keyCode)
        }
    }

    override fun updateScreen() {
        for (c in campos) c.updateCursorCounter()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawDefaultBackground()
        drawCenteredString(fontRendererObj, "AutoText", width / 2, 18, 0xFFFFFF)
        drawCenteredString(fontRendererObj, "Escolha uma tecla e a mensagem (ou comando) que ela envia", width / 2, 32, 0xAAAAAA)
        drawCenteredString(fontRendererObj, "Backspace remove a tecla", width / 2, 43, 0x777777)
        super.drawScreen(mouseX, mouseY, partialTicks)
        for (c in campos) c.drawTextBox()
    }

    override fun onGuiClosed() {
        guardaTextos()
        Keyboard.enableRepeatEvents(false)
        Salvar.salvar()
    }

    override fun doesGuiPauseGame(): Boolean = false
}
