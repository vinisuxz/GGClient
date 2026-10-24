package com.github.vinisuxz.ggclient

import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

private object CoresMenu {
    val fundo = 0xF2070F22.toInt()
    val borda = 0xFF1E3F7A.toInt()
    val linha = 0xFF0B1630.toInt()
    val linhaBorda = 0xFF1A2E58.toInt()
    val iconeFundo = 0xFF14223F.toInt()
    val destaque = 0xFF1F6BFF.toInt()
    val destaqueClaro = 0xFF6FA0FF.toInt()
    val branco = 0xFFFFFFFF.toInt()
    val cinza = 0xFF98A3B8.toInt()
    val verde = 0xFF22E06B.toInt()
    val trilhoOff = 0xFF3A4156.toInt()
    val bolinhaOff = 0xFFC3CAD9.toInt()
}

private class ItemMenu(
    val nome: String,
    val desc: String,
    val icone: String?,
    val ativo: () -> Boolean,
    val alterna: () -> Unit,
    val engrenagem: (() -> Unit)?,
    val seta: Boolean = false
)

class TelaMenu : GuiScreen() {
    private val painelW = 584f
    private val painelH = 350f
    private val abas = listOf("Home", "Mods", "HUD", "Movement", "Configurações")
    private val iconesAba = listOf("tab_home", "tab_mods", "tab_hud", "tab_movement", "tab_config")
    private val iconesAltAba = listOf("aba_modulos", "aba_modulos", "aba_modulos", "aba_perfil", "aba_config")

    // área de conteúdo
    private val lx = 157f
    private val lw = 416f
    private val ly = 84f
    private val lh = 252f
    private val linhaH = 40f
    private val espaco = 6f

    private var aba = 0
    private var scroll = 0f
    private var u = 1f
    private var px = 0f
    private var py = 0f
    private var esc = 1
    private var apertado = false
    private var arrastou = false
    private var yInicio = 0f
    private var yUltimo = 0f

    private fun layout() {
        esc = ScaledResolution(mc).scaleFactor
        u = minOf(340f * esc / painelW, mc.displayWidth * 0.92f / painelW, mc.displayHeight * 0.92f / painelH)
        px = (mc.displayWidth - painelW * u) / 2f
        py = (mc.displayHeight - painelH * u) / 2f
    }

    private fun mx(): Float = (Mouse.getX() - px) / u
    private fun my(): Float = (mc.displayHeight - Mouse.getY() - 1 - py) / u

    private fun dentro(x: Float, y: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean =
        x >= rx && x < rx + rw && y >= ry && y < ry + rh

    private fun abrirConfig(m: Modulo) {
        if (m.temAuto) {
            mc.displayGuiScreen(TelaAutoText(this))
        } else {
            mc.displayGuiScreen(TelaConfig(m, this))
        }
    }

    private fun desc(nome: String): String = when (nome) {
        "FPS" -> "Exibe o FPS na tela"
        "CPS" -> "Mostra seus cliques"
        "ToggleSprint" -> "Corre automaticamente"
        "Fullbright" -> "Enxerga no escuro"
        "Perspective" -> "Altera sua perspectiva"
        "NoParticles" -> "Remove as partículas"
        "NoHurtCam" -> "Sem tremor ao tomar dano"
        "AutoText" -> "Mensagens em uma tecla"
        "Scoreboard" -> "Personaliza o placar"
        "Chat" -> "Personaliza o chat"
        else -> ""
    }

    private fun item(m: Modulo): ItemMenu {
        val gear: (() -> Unit)? = if (m.configuravel) ({ abrirConfig(m) }) else null
        return ItemMenu(m.nome, desc(m.nome), m.nome.lowercase(), { m.ativo }, { m.ativo = !m.ativo }, gear)
    }

    private fun itensDaAba(): List<ItemMenu> = when (aba) {
        1 -> Mods.lista.map { item(it) }
        2 -> listOf(Mods.fps, Mods.cps, Mods.scoreboard, Mods.chat).map { item(it) }
        3 -> listOf(Mods.toggleSprint, Mods.perspective, Mods.noHurtCam).map { item(it) }
        4 -> Boost.opcoes.map { o -> ItemMenu(o.nome, "", null, { o.ativo }, { Boost.definir(o, !o.ativo) }, null) }
        else -> emptyList()
    }

    private fun cardsHome(): List<ItemMenu> {
        val l = listOf(Mods.cps, Mods.fps, Mods.toggleSprint, Mods.perspective, Mods.fullbright).map { item(it) }
        return l + ItemMenu("Configurações Rápidas", "Ajustes rápidos", "engrenagem", { false }, { aba = 4; scroll = 0f }, null, true)
    }

    private fun maxScroll(): Float {
        val n = itensDaAba().size
        if (n == 0) return 0f
        val tot = n * (linhaH + espaco) - espaco
        return maxOf(0f, tot - lh)
    }

    private fun ajustaScroll() {
        scroll = scroll.coerceIn(0f, maxScroll())
    }

    private fun corta(t: String, tam: Float, maxW: Float): String {
        if (Fonte.largura(t, tam) <= maxW) return t
        var s = t
        while (s.length > 1 && Fonte.largura(s + "..", tam) > maxW) s = s.dropLast(1)
        return s + ".."
    }

    private fun icone(id: String?, x: Float, y: Float, s: Float): Boolean =
        id != null && Icones.desenhar(id, x, y, s)

    private fun toggle(x: Float, y: Float, ligado: Boolean) {
        Desenho.rrect(x, y, 32f, 16f, 8f, if (ligado) CoresMenu.destaque else CoresMenu.trilhoOff)
        val bx = if (ligado) x + 32f - 14f else x + 2f
        Desenho.rrect(bx, y + 2f, 12f, 12f, 6f, if (ligado) CoresMenu.branco else CoresMenu.bolinhaOff)
    }

    private fun avatar(cx: Float, cy: Float) {
        if (icone("avatar", cx - 20f, cy - 20f, 40f)) return
        Desenho.rrect(cx - 20f, cy - 20f, 40f, 40f, 20f, CoresMenu.borda)
        Desenho.rrect(cx - 19f, cy - 19f, 38f, 38f, 19f, CoresMenu.iconeFundo)
        Desenho.rrect(cx - 5f, cy - 11f, 10f, 10f, 5f, CoresMenu.cinza)
        Desenho.rrect(cx - 10f, cy + 2f, 20f, 10f, 5f, CoresMenu.cinza)
    }

    override fun initGui() {
        buttonList.clear()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        layout()
        ajustaScroll()
        val mxd = mx()
        val myd = my()
        Gui.drawRect(0, 0, width, height, 0x99000000.toInt())
        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        GlStateManager.scale(1f / esc, 1f / esc, 1f)
        GlStateManager.translate(px, py, 0f)
        GlStateManager.scale(u, u, 1f)

        // painel
        Desenho.rrect(0f, 0f, painelW, painelH, 14f, CoresMenu.borda)
        Desenho.rrect(1.2f, 1.2f, painelW - 2.4f, painelH - 2.4f, 13f, CoresMenu.fundo)

        // cabeçalho
        if (!Icones.desenhar("logo", 14f, 7f, 26f)) Desenho.coroa(16f, 11f, 22f)
        Fonte.negrito("GG", 50f, Fonte.yCentro(20f, 17f), 17f, CoresMenu.branco)
        Fonte.negrito("CLIENT", 50f + Fonte.largura("GG ", 17f), Fonte.yCentro(20f, 17f), 17f, CoresMenu.destaque)
        val hoverX = dentro(mxd, myd, painelW - 40f, 6f, 34f, 28f)
        Fonte.desenhar("×", painelW - 30f, Fonte.yCentro(20f, 22f), 22f, if (hoverX) CoresMenu.branco else CoresMenu.destaqueClaro)
        Desenho.rrect(1f, 40f, painelW - 2f, 1f, 0f, CoresMenu.borda)

        // barra lateral
        Desenho.rrect(143f, 41f, 1f, painelH - 43f, 0f, CoresMenu.borda)
        for (i in abas.indices) {
            val ty = 53f + i * 36f
            val sel = i == aba
            if (sel) Desenho.rrect(10f, ty, 123f, 33f, 8f, CoresMenu.destaque)
            val corTxt = if (sel) CoresMenu.branco else CoresMenu.destaqueClaro
            val temIc = Icones.desenhar(iconesAba[i], 20f, ty + 8f, 17f) ||
                Icones.desenhar(iconesAltAba[i], 20f, ty + 8f, 17f)
            Fonte.desenhar(abas[i], if (temIc) 46f else 22f, Fonte.yCentro(ty + 16.5f, 11f), 11f, corTxt)
        }
        if (!Icones.desenhar("logo", 14f, painelH - 34f, 18f)) Desenho.coroa(15f, painelH - 31f, 16f)
        Fonte.desenhar("GG CLIENT v1.0", 38f, Fonte.yCentro(painelH - 25f, 9f), 9f, CoresMenu.cinza)

        if (aba == 0) desenharHome() else desenharAba()

        GlStateManager.enableCull()
        GlStateManager.popMatrix()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun desenharHome() {
        // perfil
        avatar(lx + 23f, 73f)
        Fonte.desenhar(mc.session.username, lx + 56f, Fonte.yCentro(66f, 16f), 16f, CoresMenu.branco)
        Desenho.rrect(lx + 56f, 83.5f, 5f, 5f, 2.5f, CoresMenu.verde)
        Fonte.desenhar("Online", lx + 66f, Fonte.yCentro(86f, 9f), 9f, CoresMenu.verde)
        val frase = "Jogue do seu jeito."
        val fw = Fonte.largura(frase, 9f)
        Fonte.desenhar(frase, lx + lw - 32f - fw, Fonte.yCentro(73f, 9f), 9f, CoresMenu.cinza)
        if (!Icones.desenhar("logo", lx + lw - 25f, 62f, 22f)) Desenho.coroa(lx + lw - 25f, 65f, 20f)
        Desenho.rrect(lx, 104f, lw, 1f, 0f, CoresMenu.linhaBorda)

        Fonte.desenhar("Módulos", lx, Fonte.yCentro(121f, 13f), 13f, CoresMenu.branco)

        cardsHome().forEachIndexed { i, m ->
            desenharCard(lx + (i % 3) * 141f, 135f + (i / 3) * 78f, m)
        }

        // faixa de baixo
        val by = 291f
        Desenho.rrect(lx, by, lw, 44f, 9f, CoresMenu.linhaBorda)
        Desenho.rrect(lx + 1f, by + 1f, lw - 2f, 42f, 8f, CoresMenu.linha)
        val temFoguete = Icones.desenhar("foguete", lx + 14f, by + 10f, 24f)
        val tx = if (temFoguete) lx + 52f else lx + 16f
        Fonte.desenhar("Desempenho e simplicidade", tx, Fonte.yCentro(by + 15f, 10f), 10f, CoresMenu.branco)
        Fonte.desenhar("Feito para uma melhor experiência em PvP.", tx, Fonte.yCentro(by + 29f, 9f), 9f, CoresMenu.destaqueClaro)
        Fonte.desenhar(">", lx + lw - 20f, Fonte.yCentro(by + 22f, 13f), 13f, CoresMenu.destaque)
    }

    private fun desenharCard(x: Float, y: Float, m: ItemMenu) {
        Desenho.rrect(x, y, 131f, 68f, 9f, CoresMenu.linhaBorda)
        Desenho.rrect(x + 1f, y + 1f, 129f, 66f, 8f, CoresMenu.linha)
        if (!icone(m.icone, x + 11f, y + 10f, 18f)) {
            Fonte.desenhar(m.nome.first().toString(), x + 15f, Fonte.yCentro(y + 19f, 14f), 14f, CoresMenu.destaque)
        }
        Fonte.desenhar(corta(m.nome, 10f, 86f), x + 38f, Fonte.yCentro(y + 17f, 10f), 10f, CoresMenu.branco)
        Fonte.desenhar(corta(m.desc, 8f, 88f), x + 38f, Fonte.yCentro(y + 30f, 8f), 8f, CoresMenu.destaqueClaro)
        if (m.seta) {
            Fonte.desenhar(">", x + 131f - 20f, Fonte.yCentro(y + 54f, 13f), 13f, CoresMenu.destaque)
        } else {
            toggle(x + 131f - 42f, y + 46f, m.ativo())
        }
    }

    private fun desenharAba() {
        Fonte.desenhar(abas[aba], lx, Fonte.yCentro(62f, 17f), 17f, CoresMenu.branco)
        if (aba == 4) {
            Fonte.desenhar(
                "Otimizações de FPS",
                lx + Fonte.largura(abas[aba], 17f) + 10f,
                Fonte.yCentro(64f, 10f), 10f, CoresMenu.cinza
            )
        }
        val itens = itensDaAba()
        // recorte: nada desenha fora da lista
        val sx = (px + lx * u).toInt()
        val sw = (lw * u).toInt() + 1
        val sh = (lh * u).toInt()
        val sy = (mc.displayHeight - (py + (ly + lh) * u)).toInt()
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(sx, sy, sw, sh)
        itens.forEachIndexed { i, m ->
            val ry = ly + i * (linhaH + espaco) - scroll
            if (ry + linhaH >= ly && ry <= ly + lh) {
                Desenho.rrect(lx, ry, lw, linhaH, 10f, CoresMenu.linhaBorda)
                Desenho.rrect(lx + 1f, ry + 1f, lw - 2f, linhaH - 2f, 9f, CoresMenu.linha)
                var nx = lx + 14f
                if (m.icone != null) {
                    nx = lx + 48f
                    Desenho.rrect(lx + 9f, ry + 6f, 28f, 28f, 8f, CoresMenu.iconeFundo)
                    if (!icone(m.icone, lx + 14f, ry + 11f, 18f)) {
                        val letra = m.nome.first().toString()
                        Fonte.desenhar(
                            letra,
                            lx + 9f + (28f - Fonte.largura(letra, 14f)) / 2f,
                            Fonte.yCentro(ry + 20f, 14f), 14f, CoresMenu.destaque
                        )
                    }
                }
                if (m.desc.isEmpty()) {
                    Fonte.desenhar(m.nome, nx, Fonte.yCentro(ry + 20f, 12f), 12f, CoresMenu.branco)
                } else {
                    Fonte.desenhar(m.nome, nx, Fonte.yCentro(ry + 14f, 12f), 12f, CoresMenu.branco)
                    Fonte.desenhar(m.desc, nx, Fonte.yCentro(ry + 28f, 9f), 9f, CoresMenu.destaqueClaro)
                }

                // engrenagem
                if (m.engrenagem != null) {
                    val gx = lx + lw - 82f
                    Desenho.rrect(gx, ry + 10f, 26f, 20f, 7f, CoresMenu.linhaBorda)
                    val desenhou = Icones.desenhar("engrenagem", gx + 5f, ry + 12f, 16f) ||
                        Icones.desenhar("aba_config", gx + 5f, ry + 12f, 16f)
                    if (!desenhou) {
                        Desenho.rrect(gx + 8f, ry + 14f, 10f, 10f, 5f, CoresMenu.cinza)
                    }
                }

                toggle(lx + lw - 44f, ry + (linhaH - 16f) / 2f, m.ativo())
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // barrinha de rolagem
        val max = maxScroll()
        if (max > 0f) {
            val tot = lh + max
            val thumbH = maxOf(24f, lh * lh / tot)
            val thumbY = ly + (scroll / max) * (lh - thumbH)
            Desenho.rrect(lx + lw + 5f, thumbY, 3f, thumbH, 1.5f, CoresMenu.trilhoOff)
        }
    }

    private fun clicar(x: Float, y: Float) {
        // fechar
        if (dentro(x, y, painelW - 40f, 6f, 34f, 28f)) {
            mc.displayGuiScreen(null)
            return
        }
        // abas
        for (i in abas.indices) {
            if (dentro(x, y, 10f, 53f + i * 36f, 123f, 33f)) {
                aba = i
                scroll = 0f
                return
            }
        }
        // Home: cards e faixa de baixo
        if (aba == 0) {
            cardsHome().forEachIndexed { i, m ->
                if (dentro(x, y, lx + (i % 3) * 141f, 135f + (i / 3) * 78f, 131f, 68f)) {
                    m.alterna()
                    return
                }
            }
            if (dentro(x, y, lx, 291f, lw, 44f)) {
                aba = 4
                scroll = 0f
            }
            return
        }
        // listas
        if (!dentro(x, y, lx, ly, lw, lh)) return
        itensDaAba().forEachIndexed { i, m ->
            val ry = ly + i * (linhaH + espaco) - scroll
            if (dentro(x, y, lx, ry, lw, linhaH)) {
                val g = m.engrenagem
                if (g != null && dentro(x, y, lx + lw - 88f, ry, 38f, linhaH)) {
                    g()
                } else {
                    m.alterna()
                }
                return
            }
        }
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton == 0) {
            layout()
            apertado = true
            arrastou = false
            yInicio = my()
            yUltimo = yInicio
        }
    }

    override fun mouseClickMove(mouseX: Int, mouseY: Int, clickedMouseButton: Int, timeSinceLastClick: Long) {
        if (!apertado) return
        val y = my()
        if (!arrastou && Math.abs(y - yInicio) > 5f) arrastou = true
        if (arrastou && aba != 0) {
            scroll -= (y - yUltimo)
            ajustaScroll()
        }
        yUltimo = y
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (apertado && !arrastou) {
            layout()
            clicar(mx(), my())
        }
        apertado = false
        arrastou = false
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        val d = Mouse.getEventDWheel()
        if (d != 0 && aba != 0) {
            scroll += if (d > 0) -24f else 24f
            ajustaScroll()
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(null)
        } else {
            super.keyTyped(typedChar, keyCode)
        }
    }

    override fun onGuiClosed() {
        Salvar.salvar()
    }

    override fun doesGuiPauseGame(): Boolean = false
}
