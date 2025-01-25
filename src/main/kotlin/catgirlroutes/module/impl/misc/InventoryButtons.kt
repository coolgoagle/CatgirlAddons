package catgirlroutes.module.impl.misc

import catgirlroutes.CatgirlRoutes.Companion.display
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.config.InventoryButtonsConfig
import catgirlroutes.config.InventoryButtonsConfig.allButtons
import catgirlroutes.events.impl.PacketReceiveEvent
import catgirlroutes.mixins.accessors.AccessorGuiContainer
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.BooleanSetting
import catgirlroutes.ui.clickgui.util.FontUtil
import catgirlroutes.ui.misc.inventorybuttons.InventoryButtonEditor
import catgirlroutes.utils.ChatUtils.debugMessage
import catgirlroutes.utils.LocationManager.inSkyblock
import catgirlroutes.utils.Utils.lore
import catgirlroutes.utils.Utils.noControlCodes
import catgirlroutes.utils.toItemStack
import catgirlroutes.utils.toJson
import catgirlroutes.utils.toJsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraftforge.client.event.GuiScreenEvent
import net.minecraftforge.fml.client.config.GuiUtils
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Mouse

object InventoryButtons : Module(
    "Inventory buttons",
    Category.MISC,
    tag = TagType.WHIP
) {

    val equipmentOverlay: BooleanSetting = BooleanSetting("Equipment Overlay", false) // todo: make it work when you right click eq in hand

    val editMode: ActionSetting = ActionSetting("Edit") { display = InventoryButtonEditor() }

    private var shouldScanEq = false

    init {
        addSettings(this.equipmentOverlay, this.editMode)
    }

    @SubscribeEvent
    fun onDrawScreenPost(event: GuiScreenEvent.DrawScreenEvent.Post) {
        if (!inSkyblock || mc.currentScreen !is GuiInventory) return
        val accessor = event.gui as AccessorGuiContainer

        allButtons.filter { it.isActive }
            .forEach { button ->
                GlStateManager.pushMatrix()
                GlStateManager.disableLighting()

                button.render(accessor.guiLeft.toDouble(), accessor.guiTop.toDouble())

                GlStateManager.enableLighting()
                GlStateManager.popMatrix()
            }

        // different loop so hovering text is always above the buttons
        allButtons.filter { it.isActive }
            .forEach { button ->
                if (button.isHovered(event.mouseX - accessor.guiLeft, event.mouseY - accessor.guiTop)) {
                    val textLines = when {
                        button.isEquipment -> {
                            if (button.icon == "barrier") {
                                listOf("Empty")
                            } else {
                                button.icon.toJsonObject().toItemStack().let { itemStack ->
                                    listOf(itemStack.displayName) + itemStack.lore
                                }
                            }
                        }
                        else -> listOf("§7/${button.command.replace("/", "")}")
                    }

                    val xOff = if (button.isEquipment) calculateTooltipXOffset(textLines) else 0

                    GuiUtils.drawHoveringText(
                        textLines,
                        event.mouseX - xOff, event.mouseY,
                        event.gui.width, event.gui.height,
                        -1, mc.fontRendererObj
                    )
                }
            }
    }

    @SubscribeEvent
    fun onMouseInputPre(event: GuiScreenEvent.MouseInputEvent.Pre) {
        if (!inSkyblock || mc.currentScreen !is GuiInventory) return

        val sr = ScaledResolution(mc)

        val mouseX: Int = Mouse.getX() * sr.scaledWidth / mc.displayWidth
        val mouseY: Int = sr.scaledHeight - (Mouse.getY() * sr.scaledHeight / mc.displayHeight)

        val accessor = event.gui as AccessorGuiContainer

        allButtons.filter { it.isActive && it.isHovered(mouseX - accessor.guiLeft, mouseY - accessor.guiTop) && Mouse.getEventButtonState() }.forEach { it.action }
    }

    @SubscribeEvent
    fun onS2DPacketOpenWindow(event: PacketReceiveEvent) {
        if (!inSkyblock || !this.equipmentOverlay.enabled || event.packet !is S2DPacketOpenWindow) return
        shouldScanEq = event.packet.windowTitle.unformattedText == "Your Equipment and Stats"
        debugMessage(shouldScanEq)
    }

    @SubscribeEvent
    fun onS2EPacketCloseWindow(event: PacketReceiveEvent) {
        if (this.equipmentOverlay.enabled && event.packet is S2EPacketCloseWindow) shouldScanEq = false
    }

    @SubscribeEvent
    fun S2FPacketSetSlot(event: PacketReceiveEvent) {
        if (!shouldScanEq || event.packet !is S2FPacketSetSlot) return
        val buttons = allButtons.filter { it.isEquipment }
        val equipmentSlots = listOf(10, 19, 28, 37)

        buttons.zip(equipmentSlots).forEach { (button, slot) ->
            if (event.packet.func_149173_d() != slot) return@forEach
            val itemStack = event.packet.func_149174_e()

            button.icon = if (itemStack.displayName.noControlCodes == "Empty Equipment Slot") {
                "barrier"
            } else {
                val tagString = itemStack.toJson()
                if (!tagString.has("internalname")) {
                    tagString.add("internalname", JsonPrimitive("_"))
                }
                tagString.toString()
            }

            InventoryButtonsConfig.save()
        }
    }

    private fun calculateTooltipXOffset(tooltipToDisplay: List<String>?): Int {
        var offset = 0
        tooltipToDisplay?.forEach { line ->
            val lineWidth = FontUtil.getStringWidth(line)
            if (lineWidth > offset) {
                offset = lineWidth
            }
        }
        return offset + 20
    }
}