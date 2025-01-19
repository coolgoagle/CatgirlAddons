package catgirlroutes.module.impl.render

import catgirlroutes.CatgirlRoutes.Companion.configPath
import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.module.Category
import catgirlroutes.module.Module
import catgirlroutes.module.settings.RegisterHudElement
import catgirlroutes.module.settings.impl.ActionSetting
import catgirlroutes.module.settings.impl.StringSelectorSetting
import catgirlroutes.ui.hud.HudElement
import net.minecraft.client.gui.Gui
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ScreenImage: Module(
    "Screen image",
    Category.RENDER
){
    private val imageSelector = StringSelectorSetting("Image", "None", arrayListOf("None"))
    private val updateImages = ActionSetting("Update images") { updateImages() }
    private val loadImage = ActionSetting("Load image") { loadImage("config/catgirlroutes/images/${imageSelector.selected}") }

    init {
        addSettings(imageSelector, updateImages, loadImage)
        updateImages()
        val file = configPath.resolve("images")
        if (!file.exists()) file.mkdir()
    }

    private fun updateImages() {
        val images = arrayListOf("None")
        val file = configPath.resolve("images")
        file.listFiles()?.forEach { image ->
            images.add(image.name)
        }
        imageSelector.options = images
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        loadImage("config/catgirlroutes/images/${imageSelector.selected}")
    }

    @RegisterHudElement
    object ImageHud : HudElement(
        this,
        width = 100,
        height = 100
    ) {
        override fun renderHud() {
            if (texture == null) return
            mc.textureManager.bindTexture(texture)
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f)
            Gui.drawModalRectWithCustomSizedTexture(0, 0, 0f, 0f, imageWidth / 8, imageHeight / 8, imageWidth.toFloat() / 8, imageHeight.toFloat() / 8)
        }
    }

    private var texture: ResourceLocation? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    private fun loadImage(filePath: String) {
        try {
            val image: BufferedImage = ImageIO.read(File(filePath))
            imageWidth = image.width
            imageHeight = image.height

            val dynamicTexture = DynamicTexture(image)
            texture = mc.textureManager.getDynamicTextureLocation("external_image", dynamicTexture)
        } catch (e: Exception) {
            e.printStackTrace()
            texture = null
            imageWidth = 0
            imageHeight = 0
        }
    }
}