package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import java.io.File
import java.util.Properties

object Salvar {

    private fun arquivo(): File {
        val pasta = File(Minecraft.getMinecraft().mcDataDir, "config")
        pasta.mkdirs()
        return File(pasta, "ggclient-mods.properties")
    }

    fun salvar() {
        try {
            val p = Properties()
            for (m in Mods.lista) {
                p.setProperty(m.nome + ".ativo", m.ativo.toString())
                p.setProperty(m.nome + ".fundo", m.fundo.toString())
                p.setProperty(m.nome + ".offX", m.offX.toString())
                p.setProperty(m.nome + ".offY", m.offY.toString())
                p.setProperty(m.nome + ".escalaPct", m.escalaPct.toString())
                p.setProperty(m.nome + ".tecla", m.tecla.toString())
            }
            arquivo().outputStream().use { p.store(it, "GGClient") }
        } catch (e: Exception) {
            GGClient.LOGGER.error("Erro ao salvar config", e)
        }
    }

    fun carregar() {
        try {
            val f = arquivo()
            if (!f.exists()) return
            val p = Properties()
            f.inputStream().use { p.load(it) }
            for (m in Mods.lista) {
                m.ativo = p.getProperty(m.nome + ".ativo")?.toBoolean() ?: m.ativo
                m.fundo = p.getProperty(m.nome + ".fundo")?.toBoolean() ?: m.fundo
                m.offX = p.getProperty(m.nome + ".offX")?.toIntOrNull() ?: m.offX
                m.offY = p.getProperty(m.nome + ".offY")?.toIntOrNull() ?: m.offY
                m.escalaPct = p.getProperty(m.nome + ".escalaPct")?.toIntOrNull() ?: m.escalaPct
                m.tecla = p.getProperty(m.nome + ".tecla")?.toIntOrNull() ?: m.tecla
            }
        } catch (e: Exception) {
            GGClient.LOGGER.error("Erro ao carregar config", e)
        }
    }
}
