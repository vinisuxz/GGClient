package com.github.vinisuxz.ggclient

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ChatLine
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiNewChat
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard

class Modulo(
    val nome: String,
    var ativo: Boolean = false,
    val layout: Boolean = false,
    val temEscala: Boolean = false,
    val temTecla: Boolean = false
) {
    val configuravel: Boolean
        get() = layout || temEscala || temTecla

    var fundo = true
    var offX = 0
    var offY = 0
    var escalaPct = 100
    var tecla = Keyboard.KEY_H
}

object Mods {
    val fps = Modulo("FPS", true)
    val toggleSprint = Modulo("ToggleSprint")
    val fullbright = Modulo("Fullbright")
    val perspective = Modulo("Perspective", false, temTecla = true)
    val noParticles = Modulo("NoParticles")
    val scoreboard = Modulo("Scoreboard", false, layout = true, temEscala = true)
    val chat = Modulo("Chat", false, layout = true)

    val lista = listOf(fps, toggleSprint, fullbright, perspective, noParticles, scoreboard, chat)
}

class TelaConfig(private val mod: Modulo, private val pai: GuiScreen) : GuiScreen() {

    private var esperandoTecla = false
    private var botaoTecla: GuiButton? = null
    private var botaoEscala: GuiButton? = null

    private fun textoFundo(): String = "Fundo: " + (if (mod.fundo) "ON" else "OFF")

    private fun textoTecla(): String =
        if (esperandoTecla) "> Aperte uma tecla <"
        else "Tecla: " + Keyboard.getKeyName(mod.tecla)

    private fun textoEscala(): String = "Tamanho: " + mod.escalaPct + "%"

    override fun initGui() {
        buttonList.clear()
        val cx = width / 2
        var y = 50

        if (mod.temTecla) {
            val b = GuiButton(10, cx - 75, y, 150, 20, textoTecla())
            botaoTecla = b
            buttonList.add(b)
            y += 30
        }

        if (mod.temEscala) {
            buttonList.add(GuiButton(7, cx - 75, y, 30, 20, "-"))
            val b = GuiButton(9, cx - 43, y, 86, 20, textoEscala())
            botaoEscala = b
            buttonList.add(b)
            buttonList.add(GuiButton(8, cx + 45, y, 30, 20, "+"))
            y += 30
        }

        if (mod.layout) {
            buttonList.add(GuiButton(0, cx - 75, y, 150, 20, textoFundo()))
            y += 30
            buttonList.add(GuiButton(1, cx - 75, y, 70, 20, "Esquerda"))
            buttonList.add(GuiButton(2, cx + 5, y, 70, 20, "Direita"))
            y += 24
            buttonList.add(GuiButton(3, cx - 75, y, 70, 20, "Cima"))
            buttonList.add(GuiButton(4, cx + 5, y, 70, 20, "Baixo"))
            y += 30
            buttonList.add(GuiButton(5, cx - 75, y, 150, 20, "Resetar posicao"))
            y += 30
        }

        buttonList.add(GuiButton(6, cx - 75, y, 150, 20, "Voltar"))
    }

    override fun actionPerformed(button: GuiButton) {
        val passo = 10
        when (button.id) {
            0 -> {
                mod.fundo = !mod.fundo
                button.displayString = textoFundo()
            }
            1 -> mod.offX -= passo
            2 -> mod.offX += passo
            3 -> mod.offY -= passo
            4 -> mod.offY += passo
            5 -> {
                mod.offX = 0
                mod.offY = 0
            }
            6 -> mc.displayGuiScreen(pai)
            7 -> {
                mod.escalaPct = (mod.escalaPct - 10).coerceIn(50, 200)
                botaoEscala?.displayString = textoEscala()
            }
            8 -> {
                mod.escalaPct = (mod.escalaPct + 10).coerceIn(50, 200)
                botaoEscala?.displayString = textoEscala()
            }
            10 -> {
                esperandoTecla = true
                button.displayString = textoTecla()
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (esperandoTecla) {
            if (keyCode != Keyboard.KEY_ESCAPE) {
                mod.tecla = keyCode
            }
            esperandoTecla = false
            botaoTecla?.displayString = textoTecla()
            return
        }
        if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RSHIFT) {
            mc.displayGuiScreen(pai)
        } else {
            super.keyTyped(typedChar, keyCode)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // sem fundo escuro, pra você ver o scoreboard/chat mexendo ao vivo
        drawCenteredString(fontRendererObj, "Config: " + mod.nome, width / 2, 25, 0xFFFFFF)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun onGuiClosed() {
        Salvar.salvar()
    }

    override fun doesGuiPauseGame(): Boolean = false
}

class Eventos {

    private val mc = Minecraft.getMinecraft()

    init {
        Salvar.carregar()
    }

    // ToggleSprint
    private var sprintLigado = false
    private var ctrlAnterior = false

    // Menu
    private var rshiftAnterior = false

    // Fullbright
    private var gammaOriginal = -1f

    // Perspective
    private var perspAtiva = false
    private var camYaw = 0f
    private var camPitch = 0f
    private var realYaw = 0f
    private var realPitch = 0f
    private var viewAnterior = 0

    // Scoreboard
    private var objetivoOculto: ScoreObjective? = null

    // Chat (campos privados do Minecraft, acessados por reflexão)
    private val campoLinhas = achaCampo("drawnChatLines", "field_146253_i")
    private val campoScroll = achaCampo("scrollPos", "field_146250_j")

    private fun achaCampo(vararg nomes: String): java.lang.reflect.Field? {
        for (n in nomes) {
            try {
                val f = GuiNewChat::class.java.getDeclaredField(n)
                f.isAccessible = true
                return f
            } catch (ex: Exception) {
                // tenta o próximo nome
            }
        }
        return null
    }

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

        // NoParticles
        if (Mods.noParticles.ativo && mc.theWorld != null) {
            mc.effectRenderer.clearEffects(mc.theWorld)
        }

        // Perspective (ativo só enquanto segura a tecla configurada)
        val teclaPersp = Keyboard.isKeyDown(Mods.perspective.tecla) && Mods.perspective.ativo
        if (teclaPersp && !perspAtiva && semTela) {
            ligarPerspective()
        } else if (!teclaPersp && perspAtiva) {
            desligarPerspective()
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

    private fun ligarPerspective() {
        val p = mc.thePlayer ?: return
        realYaw = p.rotationYaw
        realPitch = p.rotationPitch
        camYaw = realYaw
        camPitch = realPitch
        viewAnterior = mc.gameSettings.thirdPersonView
        mc.gameSettings.thirdPersonView = 1
        perspAtiva = true
    }

    private fun desligarPerspective() {
        perspAtiva = false
        mc.gameSettings.thirdPersonView = viewAnterior
    }

    @SubscribeEvent
    fun aoRender(e: TickEvent.RenderTickEvent) {
        if (e.phase == TickEvent.Phase.START) {
            // NoParticles: limpa antes de desenhar o quadro
            if (Mods.noParticles.ativo && mc.theWorld != null) {
                mc.effectRenderer.clearEffects(mc.theWorld)
            }
        } else {
            // segurança: devolve o scoreboard se algo deu errado
            val oculto = objetivoOculto
            val mundo = mc.theWorld
            if (oculto != null && mundo != null) {
                mundo.scoreboard.setObjectiveInDisplaySlot(1, oculto)
                objetivoOculto = null
            }
        }

        val p = mc.thePlayer ?: return
        if (!perspAtiva) return
        if (e.phase == TickEvent.Phase.START) {
            // câmera usa a rotação "falsa"
            p.rotationYaw = camYaw
            p.prevRotationYaw = camYaw
            p.rotationPitch = camPitch
            p.prevRotationPitch = camPitch
        } else {
            // guarda o quanto o mouse mexeu e devolve a rotação real
            camYaw = p.rotationYaw
            camPitch = p.rotationPitch
            p.rotationYaw = realYaw
            p.prevRotationYaw = realYaw
            p.rotationPitch = realPitch
            p.prevRotationPitch = realPitch
        }
    }

    @SubscribeEvent
    fun aoDesenhar(e: RenderGameOverlayEvent.Text) {
        if (Mods.toggleSprint.ativo && sprintLigado) {
            mc.fontRendererObj.drawStringWithShadow("[Sprint: ATIVADO]", 5f, 35f, 0x55FF55)
        }
    }

    // ---------- SCOREBOARD ----------

    @SubscribeEvent
    fun aoOverlayPre(e: RenderGameOverlayEvent.Pre) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return
        if (!Mods.scoreboard.ativo) return
        val mundo = mc.theWorld ?: return
        val sb = mundo.scoreboard
        val obj = sb.getObjectiveInDisplaySlot(1)
        if (obj != null) {
            objetivoOculto = obj
            sb.setObjectiveInDisplaySlot(1, null)
        }
    }

    @SubscribeEvent
    fun aoOverlayPost(e: RenderGameOverlayEvent.Post) {
        if (e.type != RenderGameOverlayEvent.ElementType.ALL) return
        val obj = objetivoOculto ?: return
        val mundo = mc.theWorld ?: return
        mundo.scoreboard.setObjectiveInDisplaySlot(1, obj)
        objetivoOculto = null
        desenharScoreboard(obj, e.resolution)
    }

    private fun desenharScoreboard(obj: ScoreObjective, res: ScaledResolution) {
        val m = Mods.scoreboard
        val sb = obj.scoreboard
        val fonte = mc.fontRendererObj
        val alturaLinha = fonte.FONT_HEIGHT

        var scores = sb.getSortedScores(obj).filter {
            it.playerName != null && !it.playerName.startsWith("#")
        }
        if (scores.size > 15) scores = scores.takeLast(15)

        var largura = fonte.getStringWidth(obj.displayName)
        for (s in scores) {
            val time = sb.getPlayersTeam(s.playerName)
            val txt = ScorePlayerTeam.formatPlayerName(time, s.playerName) + ": " + EnumChatFormatting.RED + s.scorePoints
            largura = maxOf(largura, fonte.getStringWidth(txt))
        }

        val altura = scores.size * alturaLinha
        val baseY = res.scaledHeight / 2 + altura / 3
        val margem = 3
        val esq = res.scaledWidth - largura - margem
        val dir = res.scaledWidth - margem + 2

        val escala = m.escalaPct / 100f
        val ancoraX = res.scaledWidth.toFloat()
        val ancoraY = res.scaledHeight / 2f

        GlStateManager.pushMatrix()
        // posição escolhida no Cfg
        GlStateManager.translate(m.offX.toFloat(), m.offY.toFloat(), 0f)
        // tamanho: cresce/encolhe a partir do lado direito da tela
        GlStateManager.translate(ancoraX, ancoraY, 0f)
        GlStateManager.scale(escala, escala, 1f)
        GlStateManager.translate(-ancoraX, -ancoraY, 0f)

        var j = 0
        for (s in scores) {
            j++
            val time = sb.getPlayersTeam(s.playerName)
            val nome = ScorePlayerTeam.formatPlayerName(time, s.playerName)
            val pontos = EnumChatFormatting.RED.toString() + s.scorePoints
            val y = baseY - j * alturaLinha

            if (m.fundo) Gui.drawRect(esq - 2, y, dir, y + alturaLinha, 1342177280)
            fonte.drawString(nome, esq, y, 0xFFFFFF)
            fonte.drawString(pontos, dir - fonte.getStringWidth(pontos), y, 0xFFFFFF)

            if (j == scores.size) {
                val titulo = obj.displayName
                if (m.fundo) {
                    Gui.drawRect(esq - 2, y - alturaLinha - 1, dir, y - 1, 1610612736)
                    Gui.drawRect(esq - 2, y - 1, dir, y, 1342177280)
                }
                fonte.drawString(titulo, esq + largura / 2 - fonte.getStringWidth(titulo) / 2, y - alturaLinha, 0xFFFFFF)
            }
        }

        GlStateManager.popMatrix()
    }

    // ---------- CHAT ----------

    @SubscribeEvent
    fun aoChat(e: RenderGameOverlayEvent.Chat) {
        if (!Mods.chat.ativo) return
        val m = Mods.chat
        val x = e.posX + m.offX
        val y = e.posY + m.offY

        if (m.fundo || campoLinhas == null || campoScroll == null) {
            // só muda de lugar
            e.posX = x
            e.posY = y
        } else {
            // desenha o chat sem o fundo
            e.isCanceled = true
            desenharChatSemFundo(x, y)
        }
    }

    private fun desenharChatSemFundo(x: Int, y: Int) {
        if (mc.gameSettings.chatVisibility == EntityPlayer.EnumChatVisibility.HIDDEN) return
        val chat = mc.ingameGUI.chatGUI
        @Suppress("UNCHECKED_CAST")
        val linhas = campoLinhas!!.get(chat) as List<ChatLine>
        val scroll = campoScroll!!.getInt(chat)
        if (linhas.isEmpty()) return

        val maxLinhas = chat.lineCount
        val aberto = chat.chatOpen
        val opacidade = mc.gameSettings.chatOpacity * 0.9f + 0.1f
        val tick = mc.ingameGUI.updateCounter
        val escala = chat.chatScale

        GlStateManager.pushMatrix()
        GlStateManager.translate(x.toFloat() + 2f, y.toFloat() + 20f, 0f)
        GlStateManager.scale(escala, escala, 1f)

        var i = 0
        while (i + scroll < linhas.size && i < maxLinhas) {
            val linha = linhas[i + scroll]
            val idade = tick - linha.updatedCounter
            if (idade < 200 || aberto) {
                var d = 1.0 - idade / 200.0
                d = (d * 10.0).coerceIn(0.0, 1.0)
                d *= d
                var alfa = (255.0 * d).toInt()
                if (aberto) alfa = 255
                alfa = (alfa * opacidade).toInt()
                if (alfa > 3) {
                    GlStateManager.enableBlend()
                    mc.fontRendererObj.drawStringWithShadow(
                        linha.chatComponent.formattedText,
                        0f,
                        (-i * 9 - 8).toFloat(),
                        16777215 + (alfa shl 24)
                    )
                    GlStateManager.disableAlpha()
                    GlStateManager.disableBlend()
                }
            }
            i++
        }
        GlStateManager.popMatrix()
    }
}
