package com.github.vinisuxz.ggclient

import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

private object Tema {
    val fundo = 0xF2091226.toInt()
    val borda = 0xFF1E3F7A.toInt()
    val linha = 0xFF0F1A33.toInt()
    val linhaBorda = 0xFF182A50.toInt()
    val iconeFundo = 0xFF14223F.toInt()
    val destaque = 0xFF1F6BFF.toInt()
    val branco = 0xFFFFFFFF.toInt()
    val cinza = 0xFF98A3B8.toInt()
    val trilhoOff = 0xFF3A4156.toInt()
    val bolinhaOff = 0xFFC3CAD9.toInt()
}

class TelaMenu : GuiScreen() {

    // tamanho do painel em "unidades de design"
    private val painelW = 520f
    private val painelH = 360f

    private val abas = listOf("Módulos", "Configurações", "Perfil")
    private val iconesAba = listOf("aba_modulos", "aba_config", "aba_perfil")

    // área da lista de módulos
    private val lx = 170f
    private val ly = 98f
    private val lw = 332f
    private val lh = 248f
    private val linhaH = 44f
    private val espaco = 7f

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
        u = minOf(mc.displayWidth * 0.92f / painelW, mc.displayHeight * 0.92f / painelH, 3f)
        px = (mc.displayWidth - painelW * u) / 2f
        py = (mc.displayHeight - painelH * u) / 2f
    }

    private fun mx(): Float = (Mouse.getX() - px) / u
    private fun my(): Float = (mc.displayHeight - Mouse.getY() - 1 - py) / u

    private fun maxScroll(): Float {
        val total = Mods.lista.size * (linhaH + espaco) - espaco
        return maxOf(0f, total - lh)
    }

    private fun ajustaScroll() {
        scroll = scroll.coerceIn(0f, maxScroll())
    }

    private fun dentro(x: Float, y: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean =
        x >= rx && x < rx + rw && y >= ry && y < ry + rh

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
        Desenho.rrect(0f, 0f, painelW, painelH, 16f, Tema.borda)
        Desenho.rrect(1.2f, 1.2f, painelW - 2.4f, painelH - 2.4f, 15f, Tema.fundo)

        // cabeçalho
        if (!Icones.desenhar("logo", 16f, 11f, 32f)) Desenho.coroa(18f, 16f, 28f)
        Fonte.negrito("GG", 58f, Fonte.yCentro(28f, 22f), 22f, Tema.branco)
        Fonte.negrito("CLIENT", 58f + Fonte.largura("GG ", 22f), Fonte.yCentro(28f, 22f), 22f, Tema.destaque)
        val hoverX = dentro(mxd, myd, painelW - 44f, 10f, 34f, 34f)
        Fonte.desenhar("×", painelW - 34f, Fonte.yCentro(28f, 28f), 28f, if (hoverX) Tema.branco else Tema.cinza)
        Desenho.rrect(1f, 55f, painelW - 2f, 1f, 0f, Tema.borda)

        // barra lateral
        Desenho.rrect(150f, 56f, 1f, painelH - 58f, 0f, Tema.borda)
        for (i in abas.indices) {
            val ty = 68f + i * 40f
            val sel = i == aba
            if (sel) Desenho.rrect(12f, ty, 126f, 34f, 10f, Tema.destaque)
            val corTxt = if (sel) Tema.branco else Tema.cinza
            val temIcone = Icones.desenhar(iconesAba[i], 24f, ty + 8f, 18f)
            Fonte.desenhar(abas[i], if (temIcone) 52f else 26f, Fonte.yCentro(ty + 17f, 14f), 14f, corTxt)
        }
        if (!Icones.desenhar("logo", 16f, painelH - 52f, 26f)) Desenho.coroa(18f, painelH - 48f, 22f)
        Fonte.desenhar("GG CLIENT", 50f, Fonte.yCentro(painelH - 44f, 12f), 12f, Tema.branco)
        Fonte.desenhar("v1.0", 50f, Fonte.yCentro(painelH - 30f, 11f), 11f, Tema.cinza)

        // título da aba
        Fonte.desenhar(abas[aba], lx, Fonte.yCentro(76f, 22f), 22f, Tema.branco)

        when (aba) {
            0 -> desenharModulos()
            1 -> {
                Fonte.desenhar("Abrir ou fechar o menu: Right Shift", lx, Fonte.yCentro(112f, 14f), 14f, Tema.branco)
                Fonte.desenhar("Suas configurações são salvas sozinhas.", lx, Fonte.yCentro(138f, 13f), 13f, Tema.cinza)
                Fonte.desenhar("Use o botão Cfg de cada módulo para", lx, Fonte.yCentro(164f, 13f), 13f, Tema.cinza)
                Fonte.desenhar("ajustar tecla, posição e tamanho.", lx, Fonte.yCentro(182f, 13f), 13f, Tema.cinza)
            }
            else -> {
                Fonte.desenhar("Jogador: " + mc.session.username, lx, Fonte.yCentro(112f, 14f), 14f, Tema.branco)
                Fonte.desenhar("GG CLIENT v1.0", lx, Fonte.yCentro(138f, 13f), 13f, Tema.cinza)
            }
        }

        GlStateManager.enableCull()
        GlStateManager.popMatrix()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    private fun desenharModulos() {
        // recorte: nada desenha fora da lista
        val sx = (px + lx * u).toInt()
        val sw = (lw * u).toInt() + 1
        val sh = (lh * u).toInt()
        val sy = (mc.displayHeight - (py + (ly + lh) * u)).toInt()
        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(sx, sy, sw, sh)

        Mods.lista.forEachIndexed { i, mod ->
            val ry = ly + i * (linhaH + espaco) - scroll
            if (ry + linhaH >= ly && ry <= ly + lh) {
                Desenho.rrect(lx, ry, lw, linhaH, 12f, Tema.linhaBorda)
                Desenho.rrect(lx + 1f, ry + 1f, lw - 2f, linhaH - 2f, 11f, Tema.linha)

                // ícone do módulo (PNG) ou a inicial, se não tiver PNG
                Desenho.rrect(lx + 10f, ry + 8f, 28f, 28f, 8f, Tema.iconeFundo)
                if (!Icones.desenhar(mod.nome.lowercase(), lx + 14f, ry + 12f, 20f)) {
                    val letra = mod.nome.first().toString()
                    Fonte.desenhar(
                        letra,
                        lx + 10f + (28f - Fonte.largura(letra, 15f)) / 2f,
                        Fonte.yCentro(ry + 22f, 15f), 15f, Tema.destaque
                    )
                }

                Fonte.desenhar(mod.nome, lx + 52f, Fonte.yCentro(ry + 22f, 15f), 15f, Tema.branco)

                if (mod.configuravel) {
                    Desenho.rrect(lx + lw - 112f, ry + 11f, 40f, 22f, 8f, Tema.linhaBorda)
                    Fonte.desenhar(
                        "Cfg",
                        lx + lw - 112f + (40f - Fonte.largura("Cfg", 12f)) / 2f,
                        Fonte.yCentro(ry + 22f, 12f), 12f, Tema.cinza
                    )
                }

                // switch
                val tx = lx + lw - 58f
                val ty = ry + 11f
                Desenho.rrect(tx, ty, 44f, 22f, 11f, if (mod.ativo) Tema.destaque else Tema.trilhoOff)
                val bx = if (mod.ativo) tx + 44f - 19f else tx + 3f
                Desenho.rrect(bx, ty + 3f, 16f, 16f, 8f, if (mod.ativo) Tema.branco else Tema.bolinhaOff)
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        // barrinha de rolagem
        val max = maxScroll()
        if (max > 0f) {
            val total = lh + max
            val thumbH = maxOf(24f, lh * lh / total)
            val thumbY = ly + (scroll / max) * (lh - thumbH)
            Desenho.rrect(lx + lw + 6f, thumbY, 3f, thumbH, 1.5f, Tema.trilhoOff)
        }
    }

    private fun clicar(x: Float, y: Float) {
        // fechar
        if (dentro(x, y, painelW - 44f, 10f, 34f, 34f)) {
            mc.displayGuiScreen(null)
            return
        }
        // abas
        for (i in abas.indices) {
            if (dentro(x, y, 12f, 68f + i * 40f, 126f, 34f)) {
                aba = i
                scroll = 0f
                return
            }
        }
        if (aba != 0) return
        if (!dentro(x, y, lx, ly, lw, lh)) return

        Mods.lista.forEachIndexed { i, mod ->
            val ry = ly + i * (linhaH + espaco) - scroll
            if (dentro(x, y, lx, ry, lw, linhaH)) {
                if (mod.configuravel && dentro(x, y, lx + lw - 118f, ry, 52f, linhaH)) {
                    mc.displayGuiScreen(TelaConfig(mod, this))
                } else {
                    mod.ativo = !mod.ativo
                }
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
        if (arrastou && aba == 0) {
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
        if (d != 0 && aba == 0) {
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
