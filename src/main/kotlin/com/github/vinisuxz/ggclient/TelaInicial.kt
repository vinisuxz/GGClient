package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.GuiMultiplayer
import net.minecraft.client.gui.GuiOptions
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSelectWorld
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.ResourceLocation
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11

private object CoresInicio {
    val topo = 0xFF030B1F.toInt()
    val base = 0xFF0A2A66.toInt()
    val painel = 0xFF07122B.toInt()
    val painelBorda = 0xFF173A7A.toInt()
    val destaque = 0xFF1F6BFF.toInt()
    val destaqueClaro = 0xFF4D8DFF.toInt()
    val branco = 0xFFFFFFFF.toInt()
    val cinza = 0xFF98A3B8.toInt()
    val verde = 0xFF22E06B.toInt()
    val btnFundo = 0xFF08122E.toInt()
    val btnFundoHover = 0xFF0E1F4D.toInt()
    val btnBorda = 0xFF1F4FD0.toInt()
    val btnBordaHover = 0xFF3D7BFF.toInt()
    val prinFundo = 0xFF0B5BFF.toInt()
    val prinFundoHover = 0xFF2A74FF.toInt()
}

private class BotaoInicio(
    val texto: String,
    val icone: String,
    val principal: Boolean,
    val acao: () -> Unit
)

// troca a tela inicial do Minecraft pela do client
object TelaInicialHook {
    fun register() {
        MinecraftForge.EVENT_BUS.register(this)
    }

    @SubscribeEvent
    fun aoAbrir(e: GuiOpenEvent) {
        if (e.gui is GuiMainMenu) e.gui = TelaInicial()
    }
}

class TelaInicial : GuiScreen() {
    private var u = 1f
    private var esc = 1
    private var w = 960f   // largura em unidades de design
    private var h = 540f   // altura em unidades de design

    private val botoes = listOf(
        BotaoInicio("Jogar", "jogar", true) { mc.displayGuiScreen(GuiSelectWorld(this)) },
        BotaoInicio("Multiplayer", "multiplayer", false) { mc.displayGuiScreen(GuiMultiplayer(this)) },
        BotaoInicio("Configurações", "engrenagem", false) { mc.displayGuiScreen(GuiOptions(this, mc.gameSettings)) },
        BotaoInicio("Sair do jogo", "sair", false) { mc.shutdown() }
    )

    private val btnW = 244f
    private val btnH = 38f

    private fun layout() {
        esc = ScaledResolution(mc).scaleFactor
        val dw = mc.displayWidth.toFloat()
        val dh = mc.displayHeight.toFloat()
        u = minOf(dw / 960f, dh / 540f)
        w = dw / u
        h = dh / u
    }

    private fun mx(): Float = Mouse.getX() / u
    private fun my(): Float = (mc.displayHeight - Mouse.getY() - 1) / u

    private fun dentro(x: Float, y: Float, rx: Float, ry: Float, rw: Float, rh: Float): Boolean =
        x >= rx && x < rx + rw && y >= ry && y < ry + rh

    // desenha um PNG com largura e altura próprias (pra fundo e logo)
    private fun img(id: String, x: Float, y: Float, iw: Float, ih: Float): Boolean {
        if (!Icones.existe(id)) return false
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        mc.textureManager.bindTexture(ResourceLocation("ggclient", "textures/gui/$id.png"))
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        Desenho.usaCor(-1)
        Desenho.quadTex(x, y, x + iw, y + ih, 0.0, 0.0, 1.0, 1.0)
        GlStateManager.color(1f, 1f, 1f, 1f)
        return true
    }

    private fun ping(): String {
        val s = mc.currentServerData
        return if (s != null && s.pingToServer > 0) s.pingToServer.toString() else "--"
    }

    override fun initGui() {
        buttonList.clear()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        layout()
        val mxd = mx()
        val myd = my()

        // fundo de reserva (se não tiver o fundo.png)
        drawGradientRect(0, 0, width, height, CoresInicio.topo, CoresInicio.base)

        GlStateManager.pushMatrix()
        GlStateManager.disableCull()
        GlStateManager.scale(1f / esc, 1f / esc, 1f)

        // fundo.png em tela cheia (proporção 16:9, cortando o que sobrar)
        val dw = mc.displayWidth.toFloat()
        val dh = mc.displayHeight.toFloat()
        val prop = 16f / 9f
        var bw = dw
        var bh = dw / prop
        if (bh < dh) {
            bh = dh
            bw = dh * prop
        }
        img("fundo", (dw - bw) / 2f, (dh - bh) / 2f, bw, bh)

        GlStateManager.scale(u, u, 1f)

        val cx = w / 2f
        val oy = (h - 540f) / 2f   // desloca o conteúdo central quando a tela é mais alta

        // ---- topo esquerdo ----
        if (!Icones.desenhar("logo", 20f, 14f, 34f)) Desenho.coroa(20f, 18f, 30f)
        Fonte.desenhar(mc.session.username, 62f, Fonte.yCentro(22f, 15f), 15f, CoresInicio.branco)
        Fonte.desenhar("Bem-vindo de volta!", 62f, Fonte.yCentro(38f, 10f), 10f, CoresInicio.destaqueClaro)

        // ---- topo direito: FPS e ping ----
        val pxp = w - 116f
        Desenho.rrect(pxp, 11f, 104f, 40f, 9f, CoresInicio.painelBorda)
        Desenho.rrect(pxp + 1f, 12f, 102f, 38f, 8f, CoresInicio.painel)
        if (!Icones.desenhar("sinal", pxp + 9f, 21f, 20f)) {
            for (k in 0 until 4) {
                val alt = 6f + k * 3.5f
                Desenho.rrect(pxp + 10f + k * 5f, 39f - alt, 3f, alt, 1f, CoresInicio.destaque)
            }
        }
        val tx = pxp + 36f
        val fpsTxt = "FPS: "
        Fonte.desenhar(fpsTxt, tx, Fonte.yCentro(23f, 9f), 9f, CoresInicio.cinza)
        Fonte.desenhar(Minecraft.getDebugFPS().toString(), tx + Fonte.largura(fpsTxt, 9f), Fonte.yCentro(23f, 9f), 9f, CoresInicio.destaque)
        val pingTxt = "Ping: "
        Fonte.desenhar(pingTxt, tx, Fonte.yCentro(38f, 9f), 9f, CoresInicio.cinza)
        Fonte.desenhar(ping() + " ms", tx + Fonte.largura(pingTxt, 9f), Fonte.yCentro(38f, 9f), 9f, CoresInicio.destaque)

        // ---- logo central ----
        if (!img("logo_grande", cx - 97f, oy + 80f, 194f, 166f)) {
            Desenho.coroa(cx - 28f, oy + 84f, 56f)
            val gg = 64f
            Fonte.negrito("GG", cx - Fonte.largura("GG", gg) / 2f, Fonte.yCentro(oy + 182f, gg), gg, CoresInicio.branco)
            val cl = 20f
            Fonte.negrito("CLIENT", cx - Fonte.largura("CLIENT", cl) / 2f, Fonte.yCentro(oy + 234f, cl), cl, CoresInicio.destaque)
        }

        // ---- botões ----
        botoes.forEachIndexed { i, b ->
            val x = cx - btnW / 2f
            val y = oy + 279f + i * 44f
            val hover = dentro(mxd, myd, x, y, btnW, btnH)
            val borda = if (b.principal) CoresInicio.destaqueClaro else if (hover) CoresInicio.btnBordaHover else CoresInicio.btnBorda
            val fundo = if (b.principal) (if (hover) CoresInicio.prinFundoHover else CoresInicio.prinFundo)
                        else (if (hover) CoresInicio.btnFundoHover else CoresInicio.btnFundo)
            Desenho.rrect(x, y, btnW, btnH, 9f, borda)
            Desenho.rrect(x + 1f, y + 1f, btnW - 2f, btnH - 2f, 8f, fundo)

            val tam = 13f
            val tw = Fonte.largura(b.texto, tam)
            val temIcone = Icones.existe(b.icone)
            val iw = if (temIcone) 24f else 0f
            val sx = cx - (iw + tw) / 2f
            if (temIcone) Icones.desenhar(b.icone, sx, y + (btnH - 16f) / 2f, 16f)
            Fonte.desenhar(b.texto, sx + iw, Fonte.yCentro(y + btnH / 2f, tam), tam, CoresInicio.branco)
        }

        // ---- rodapé esquerdo ----
        if (!Icones.desenhar("logo", 20f, h - 46f, 28f)) Desenho.coroa(20f, h - 42f, 24f)
        Fonte.desenhar("GG CLIENT", 56f, Fonte.yCentro(h - 38f, 10f), 10f, CoresInicio.branco)
        Fonte.desenhar("v1.0", 56f + Fonte.largura("GG CLIENT ", 10f), Fonte.yCentro(h - 38f, 10f), 10f, CoresInicio.cinza)
        Fonte.desenhar("Mais que um client.", 56f, Fonte.yCentro(h - 26f, 9f), 9f, CoresInicio.cinza)

        // ---- rodapé direito ----
        val cyR = h - 30f
        val mcTxt = "Minecraft 1.8.9"
        val xM = w - 20f - Fonte.largura(mcTxt, 9f)
        Fonte.desenhar(mcTxt, xM, Fonte.yCentro(cyR, 9f), 9f, CoresInicio.cinza)
        Desenho.rrect(xM - 8f, cyR - 5f, 1f, 10f, 0f, CoresInicio.painelBorda)
        val conTxt = "Conectado"
        val xC = xM - 16f - Fonte.largura(conTxt, 9f)
        Fonte.desenhar(conTxt, xC, Fonte.yCentro(cyR, 9f), 9f, CoresInicio.verde)
        Desenho.rrect(xC - 10f, cyR - 2.5f, 5f, 5f, 2.5f, CoresInicio.verde)

        GlStateManager.enableCull()
        GlStateManager.popMatrix()
        GlStateManager.disableBlend()
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        layout()
        val x = mx()
        val y = my()
        val cx = w / 2f
        val oy = (h - 540f) / 2f
        botoes.forEachIndexed { i, b ->
            if (dentro(x, y, cx - btnW / 2f, oy + 279f + i * 44f, btnW, btnH)) {
                b.acao()
                return
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // Esc não faz nada na tela inicial
    }

    override fun doesGuiPauseGame(): Boolean = false
}
