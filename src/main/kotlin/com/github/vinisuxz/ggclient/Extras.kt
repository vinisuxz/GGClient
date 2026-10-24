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
