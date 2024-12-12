package catgirlroutes.commands.impl

import catgirlroutes.CatgirlRoutes.Companion.mc
import catgirlroutes.commands.commodore
import catgirlroutes.module.impl.dungeons.puzzlesolvers.IceFillSolver
import catgirlroutes.utils.BlockAura.blockArray
import catgirlroutes.utils.BlockAura.breakArray
import catgirlroutes.utils.ChatUtils
import catgirlroutes.utils.ChatUtils.modMessage
import catgirlroutes.utils.ClientListener.scheduleTask
import catgirlroutes.utils.EntityAura
import catgirlroutes.utils.EntityAura.entityArray
import catgirlroutes.utils.PlayerUtils.swapFromName
import catgirlroutes.utils.dungeon.DungeonUtils.currentRoom
import catgirlroutes.utils.dungeon.DungeonUtils.getRealYaw
import catgirlroutes.utils.dungeon.DungeonUtils.getRelativeYaw
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import me.odinmain.utils.toVec3
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityHorse
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.BlockPos


val devCommands = commodore("dev") {

    literal("help").run { // todo: add description
        modMessage("""
            List of commands:
              §7/relativecoords
              §7/relativeyaw
              §7/realyaw
        """.trimIndent())
    }

    literal("relativecoords").runs {
        val block = mc.objectMouseOver?.blockPos ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Middle: $block
            Relative Coords: ${DungeonUtils.currentRoom?.getRelativeCoords(block.toVec3())?.toString()}
            --------
            """.trimIndent())
    }

    literal("relativeyaw").runs {
        val room = currentRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRelativeYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }

    literal("realyaw").runs {
        val room = currentRoom ?: return@runs
        ChatUtils.chatMessage(
            """
            ---------
            Player Yaw: ${mc.thePlayer.rotationYaw}
            Relative Yaw: ${room.getRealYaw(mc.thePlayer.rotationYaw)}
            --------
            """.trimIndent())
    }

    literal("icefill").runs {
        ChatUtils.chatMessage(
            """
            ---------
            IceFill solution: ${IceFillSolver.currentPatterns}
            --------
            """.trimIndent())
    }

    literal("icefillreset").runs {
        IceFillSolver.reset()
        ChatUtils.chatMessage("Resetting fill")
    }

    literal("swaptest").runs {
        swapFromName("orange")
        swapFromName("magenta")
        swapFromName("light blue")
        swapFromName("yellow")
        swapFromName("lime")
        scheduleTask(0) {
            swapFromName("pink")
            swapFromName("gray")
            swapFromName("light gray")
            swapFromName("cyan")
        }
    }
    literal("entityaura").runs {
        mc.theWorld.loadedEntityList.forEach{entity ->
            if (entity is EntityHorse || entity is EntitySlime || entity is EntityArmorStand) entityArray.add(EntityAura.EntityAuraAction(entity, C02PacketUseEntity.Action.INTERACT_AT))
        }
    }
    literal("blockaura").runs {
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        val blockPos1: BlockPos = BlockPos(eyePos.xCoord - 20, eyePos.yCoord - 20, eyePos.zCoord - 20)
        val blockPos2: BlockPos = BlockPos(eyePos.xCoord + 20, eyePos.yCoord + 20, eyePos.zCoord + 20)
        val blocks = BlockPos.getAllInBox(blockPos1, blockPos2)
        for (block in blocks) {
            val blockstate = mc.theWorld.getBlockState(block)
            if (blockstate.block == Blocks.chest || blockstate.block == Blocks.lever || blockstate.block == Blocks.emerald_block || blockstate.block == Blocks.stone_button) blockArray.add(block)
        }
    }
    literal("breakaura").runs {
        val eyePos = mc.thePlayer.getPositionEyes(0f)
        val blockPos1: BlockPos = BlockPos(eyePos.xCoord - 20, eyePos.yCoord - 20, eyePos.zCoord - 20)
        val blockPos2: BlockPos = BlockPos(eyePos.xCoord + 20, eyePos.yCoord + 20, eyePos.zCoord + 20)
        val blocks = BlockPos.getAllInBox(blockPos1, blockPos2)
        for (block in blocks) {
            val blockstate = mc.theWorld.getBlockState(block)
            if (blockstate.block == Blocks.chest || blockstate.block == Blocks.lever || blockstate.block == Blocks.emerald_block || blockstate.block == Blocks.stone_button) breakArray.add(block)
        }
    }
}