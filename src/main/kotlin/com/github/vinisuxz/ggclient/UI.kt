package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import java.awt.image.BufferedImage
import kotlin.math.cos
import kotlin.math.sin

object Desenho {

    fun usaCor(c: Int) {
        GlStateManager.color(
            ((c shr 16) and 255) / 255f,
            ((c shr 8) and 255) / 255f,
            (c and 255) / 255f,
            ((c shr 24) and 255) / 255f
        )
    }

    private fun arco(wr: WorldRenderer, cx: Float, cy: Float, r: Float, ini: Int, fim: Int) {
        val passos = 8
        for (k in 0..passos) {
            val a = Math.toRadians(ini + (fim - ini) * k / passos.toDouble())
            wr.pos(cx + r * cos(a), cy + r * sin(a), 0.0).endVertex()
        }
    }

    fun rrect(x: Float, y: Float, w: Float, h: Float, raio: Float, c: Int) {
        val r = minOf(raio, w / 2f, h / 2f)
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        usaCor(c)
        val wr = Tessellator.getInstance().worldRenderer
        wr.begin(6, DefaultVertexFormats.POSITION)
        wr.pos((x + w / 2f).toDouble(), (y + h / 2f).toDouble(), 0.0).endVertex()
        arco(wr, x + r, y + r, r, 180, 270)
        arco(wr, x + w - r, y + r, r, 270, 360)
        arco(wr, x + w - r, y + h - r, r, 0, 90)
        arco(wr, x + r, y + h - r, r, 90, 180)
        wr.pos(x.toDouble(), (y + r).toDouble(), 0.0).endVertex()
        Tessellator.getInstance().draw()
        GlStateManager.enableTexture2D()
        GlStateManager.color(1f, 1f, 1f, 1f)
    }

    fun quadTex(x0: Float, y0: Float, x1: Float, y1: Float, u0: Double, v0: Double, u1: Double, v1: Double) {
        val wr = Tessellator.getInstance().worldRenderer
        wr.begin(7, DefaultVertexFormats.POSITION_TEX)
        wr.pos(x0.toDouble(), y1.toDouble(), 0.0).tex(u0, v1).endVertex()
        wr.pos(x1.toDouble(), y1.toDouble(), 0.0).tex(u1, v1).endVertex()
        wr.pos(x1.toDouble(), y0.toDouble(), 0.0).tex(u1, v0).endVertex()
        wr.pos(x0.toDouble(), y0.toDouble(), 0.0).tex(u0, v0).endVertex()
        Tessellator.getInstance().draw()
    }

    fun coroa(x: Float, y: Float, t: Float) {
        val ouro = 0xFFFFC107.toInt()
        rrect(x, y + t * 0.5f, t, t * 0.28f, 1.5f, ouro)
        rrect(x, y + t * 0.1f, t * 0.2f, t * 0.45f, 1f, ouro)
        rrect(x + t * 0.4f, y, t * 0.2f, t * 0.55f, 1f, ouro)
        rrect(x + t * 0.8f, y + t * 0.1f, t * 0.2f, t * 0.45f, 1f, ouro)
    }
}

object Icones {

    private val cache = HashMap<String, Boolean>()

    private fun loc(id: String) = ResourceLocation("ggclient", "textures/gui/$id.png")

    fun existe(id: String): Boolean {
        val guardado = cache[id]
        if (guardado != null) return guardado
        val r = try {
            Minecraft.getMinecraft().resourceManager.getResource(loc(id)).inputStream.close()
            true
        } catch (e: Exception) {
            false
        }
        cache[id] = r
        return r
    }

    fun desenhar(id: String, x: Float, y: Float, tam: Float, c: Int = -1): Boolean {
        if (!existe(id)) return false
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        Minecraft.getMinecraft().textureManager.bindTexture(loc(id))
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        Desenho.usaCor(c)
        Desenho.quadTex(x, y, x + tam, y + tam, 0.0, 0.0, 1.0, 1.0)
        GlStateManager.color(1f, 1f, 1f, 1f)
        return true
    }
}

// Parte que usa java.awt fica separada, pra o client não quebrar se o AWT não existir
object FonteAwt {
    fun gerar(tam: Int, cols: Int, larguras: IntArray, dim: IntArray, ttf: java.io.InputStream?): BufferedImage {
        var base: java.awt.Font? = null
        if (ttf != null) {
            try {
                base = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, ttf)
            } catch (e: Exception) {
                base = null
            }
            try {
                ttf.close()
            } catch (e: Exception) {
            }
        }
        val fonte = (base ?: java.awt.Font("SansSerif", java.awt.Font.PLAIN, 12)).deriveFont(tam.toFloat())

        val medida = BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB)
        val gm = medida.createGraphics()
        gm.font = fonte
        val fm = gm.fontMetrics
        val celW = tam + 8
        val celH = fm.height + 4
        val ascent = fm.ascent
        for (i in 32..255) larguras[i] = fm.charWidth(i)
        gm.dispose()

        val img = BufferedImage(celW * cols, celH * 16, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.font = fonte
        g.color = java.awt.Color.WHITE
        for (i in 32..255) {
            val col = i % cols
            val row = i / cols
            g.drawString(i.toChar().toString(), col * celW + 4, row * celH + ascent + 2)
        }
        g.dispose()
        dim[0] = celW
        dim[1] = celH
        return img
    }
}

object Fonte {

    private const val TAM = 40
    private const val COLS = 16

    private val larguras = IntArray(256)
    private var textura: DynamicTexture? = null
    private var celW = 1
    private var celH = 1
    private var atlasW = 1f
    private var atlasH = 1f
    private var tentou = false
    private var ok = false

    private fun montar() {
        tentou = true
        try {
            val mc = Minecraft.getMinecraft()
            var ttf: java.io.InputStream? = null
            try {
                ttf = mc.resourceManager.getResource(ResourceLocation("ggclient", "fonts/ui.ttf")).inputStream
            } catch (e: Exception) {
                ttf = null
            }
            val dim = IntArray(2)
            val img = FonteAwt.gerar(TAM, COLS, larguras, dim, ttf)
            celW = dim[0]
            celH = dim[1]
            atlasW = img.width.toFloat()
            atlasH = img.height.toFloat()
            textura = DynamicTexture(img)
            ok = true
        } catch (e: Throwable) {
            ok = false
            GGClient.LOGGER.warn("Fonte TTF indisponivel, usando a fonte do Minecraft", e)
        }
    }

    fun largura(s: String, tamanho: Float): Float {
        if (!tentou) montar()
        if (!ok) return Minecraft.getMinecraft().fontRendererObj.getStringWidth(s) * tamanho / 9f
        var w = 0
        for (ch in s) {
            var i = ch.code
            if (i < 32 || i > 255) i = 63
            w += larguras[i]
        }
        return w * tamanho / TAM
    }

    // centraliza o texto na vertical em volta de cy
    fun yCentro(cy: Float, tamanho: Float): Float = cy - 0.61f * tamanho

    fun negrito(s: String, x: Float, y: Float, tamanho: Float, cor: Int) {
        desenhar(s, x, y, tamanho, cor)
        desenhar(s, x + 0.7f, y, tamanho, cor)
    }

    fun desenhar(s: String, x: Float, y: Float, tamanho: Float, cor: Int) {
        if (!tentou) montar()
        val tex = textura
        if (!ok || tex == null) {
            val k = tamanho / 9f
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 0f)
            GlStateManager.scale(k, k, 1f)
            Minecraft.getMinecraft().fontRendererObj.drawString(s, 0, 0, cor)
            GlStateManager.popMatrix()
            return
        }

        val k = tamanho / TAM
        GlStateManager.enableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)
        GlStateManager.bindTexture(tex.glTextureId)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
        Desenho.usaCor(cor)

        val wr = Tessellator.getInstance().worldRenderer
        wr.begin(7, DefaultVertexFormats.POSITION_TEX)
        var cx = x
        for (ch in s) {
            var i = ch.code
            if (i < 32 || i > 255) i = 63
            val col = i % COLS
            val row = i / COLS
            val u0 = (col * celW) / atlasW.toDouble()
            val u1 = (col * celW + celW) / atlasW.toDouble()
            val v0 = (row * celH) / atlasH.toDouble()
            val v1 = (row * celH + celH) / atlasH.toDouble()
            val x0 = cx - 4f * k
            val x1 = x0 + celW * k
            val y0 = y
            val y1 = y + celH * k
            wr.pos(x0.toDouble(), y1.toDouble(), 0.0).tex(u0, v1).endVertex()
            wr.pos(x1.toDouble(), y1.toDouble(), 0.0).tex(u1, v1).endVertex()
            wr.pos(x1.toDouble(), y0.toDouble(), 0.0).tex(u1, v0).endVertex()
            wr.pos(x0.toDouble(), y0.toDouble(), 0.0).tex(u0, v0).endVertex()
            cx += larguras[i] * k
        }
        Tessellator.getInstance().draw()
        GlStateManager.color(1f, 1f, 1f, 1f)
    }
}
