package catgirlroutes.module.impl.dungeons.puzzlesolvers

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.events.impl.RoomEnterEvent
import catgirlroutes.module.impl.dungeons.puzzlesolvers.Puzzles.iceFillAuto
import catgirlroutes.module.impl.dungeons.puzzlesolvers.Puzzles.iceFillDelay
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.MovementUtils.stopVelo
import catgirlroutes.utils.Utils.Vec2
import catgirlroutes.utils.Utils.addVec
import catgirlroutes.utils.dungeon.DungeonUtils
import catgirlroutes.utils.dungeon.DungeonUtils.getRealCoords
import catgirlroutes.utils.dungeon.IceFillFloors
import catgirlroutes.utils.dungeon.tiles.Rotations
import catgirlroutes.utils.isAir
import catgirlroutes.utils.render.WorldRenderUtils
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.odinmain.utils.toVec3
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import java.awt.Color
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import kotlin.math.floor

object IceFillSolver {
    var currentPatterns: ArrayList<Vec3> = ArrayList()

    private var representativeFloors: List<List<List<Int>>>
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val isr = this::class.java.getResourceAsStream("/icefillFloors.json")
        ?.let { InputStreamReader(it, StandardCharsets.UTF_8) }

    init {
        try {
            val text = isr?.readText()
            representativeFloors = gson.fromJson(text, object : TypeToken<List<List<List<Int>>>>() {}.type)
            isr?.close()
        } catch (e: Exception) {
            representativeFloors = emptyList()
        }
    }

    private var awaitingClip = false

    fun onRenderWorld(color: Color) {
        if (currentPatterns.isEmpty() || DungeonUtils.currentRoomName != "Ice Fill") return
        for (i in 0 until currentPatterns.size - 1) {
            val p1 = currentPatterns[i]
            val p2 = currentPatterns[i + 1]

            WorldRenderUtils.drawLine(
                p1.xCoord, p1.yCoord, p1.zCoord,
                p2.xCoord, p2.yCoord, p2.zCoord,
                color, 4.0f, false
            )
            val x = floor(mc.thePlayer.posX) + 0.5
            val y = floor(mc.thePlayer.posY) + 0.1
            val z = floor(mc.thePlayer.posZ) + 0.5
            if (x == p1.xCoord && y == p1.yCoord && z == p1.zCoord && !awaitingClip && iceFillAuto.value) {

                awaitingClip = true
                scheduleTask(iceFillDelay.value.toInt() - 1) {
                    if(mc.thePlayer.isCollidedVertically) {
                        stopVelo()
                        mc.thePlayer.setPosition(p2.xCoord, p2.yCoord - 0.1, p2.zCoord)
                    }
                    awaitingClip = false
                }
                //modMessage("Next pos: ${p2.xCoord} ${p2.yCoord} ${p2.zCoord}")
            }
        }
    }

    fun onRoomEnter(event: RoomEnterEvent) {
        if (event.room?.data?.name != "Ice Fill" || currentPatterns.isNotEmpty()) return
        awaitingClip = false
        scanAllFloors(event.room.getRealCoords(15, 70, 7).toVec3(), event.room.rotation)
    }

    private fun scanAllFloors(pos: Vec3, rotation: Rotations) {
        listOf(pos, pos.add(transformTo(Vec3i(5, 1, 0), rotation)), pos.add(transformTo(Vec3i(12, 2, 0), rotation))).forEachIndexed { floorIndex, startPosition ->
            val floorHeight = representativeFloors[floorIndex]
            val startTime = System.nanoTime()

            for (patternIndex in floorHeight.indices) {
                if (
                    isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][0], floorHeight[patternIndex][1], rotation).toVec3i())) &&
                    !isAir(BlockPos(startPosition).add(transform(floorHeight[patternIndex][2], floorHeight[patternIndex][3], rotation).toVec3i()))
                ) {
                    modMessage("Section $floorIndex scan took ${(System.nanoTime() - startTime) / 1000000.0}ms pattern: $patternIndex")

                    IceFillFloors.IceFillFloors[floorIndex][patternIndex].toMutableList().let {
                        currentPatterns.addAll(it.map { startPosition.addVec(x = 0.5, y = 0.1, z = 0.5).add(transformTo(it, rotation)) })
                    }
                    return@forEachIndexed
                }
            }
            modMessage("§cFailed to scan floor ${floorIndex + 1}")
        }
        stupidStairs(rotation)
    }

    private fun transform(x: Int, z: Int, rotation: Rotations): Vec2 {
        return when (rotation) {
            Rotations.NORTH -> Vec2(z, -x)
            Rotations.WEST -> Vec2(-x, -z)
            Rotations.SOUTH -> Vec2(-z, x)
            Rotations.EAST -> Vec2(x, z)
            else -> Vec2(x, z)
        }
    }

    fun transformTo(vec: Vec3i, rotation: Rotations): Vec3 = with(transform(vec.x, vec.z, rotation)) {
        Vec3(x.toDouble(), vec.y.toDouble(), z.toDouble())
    }


    fun Vec2.toVec3i(): Vec3i = Vec3i(this.x.toInt(), 0, this.z.toInt())

    private fun stupidStairs(rotation: Rotations) {
        val updatedPatterns = ArrayList<Vec3>()

        var stupid71 = false
        var stupid72 = false

        for (point in currentPatterns) {
            if (point.yCoord == 71.1 && !stupid71) {
                updatedPatterns.add(adjustStupid(point, rotation, 70.6))
                stupid71 = true
            } else if (point.yCoord == 72.1 && !stupid72) {
                updatedPatterns.add(adjustStupid(point, rotation, 71.6))
                stupid72 = true
            }

            updatedPatterns.add(point)
            if (stupid71 && stupid72) break
        }

        currentPatterns = updatedPatterns
    }

    private fun adjustStupid(point: Vec3, rotation: Rotations, adjustedY: Double): Vec3 {
        return when (rotation) {
            Rotations.NORTH -> Vec3(point.xCoord, adjustedY, point.zCoord + 0.35)
            Rotations.SOUTH -> Vec3(point.xCoord, adjustedY, point.zCoord - 0.35)
            Rotations.EAST -> Vec3(point.xCoord - 0.35, adjustedY, point.zCoord) // might be wrong
            Rotations.WEST -> Vec3(point.xCoord + 0.35, adjustedY, point.zCoord) // might be wrong
            else -> point
        }
    }

    fun reset() {
        currentPatterns = ArrayList()
    }
}
