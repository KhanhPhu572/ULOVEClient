/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.FDPClient
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.util.BlockPos

@ModuleInfo(name = "AutoTool", category = ModuleCategory.PLAYER)
object AutoTool : Module() {

    private val noCombat = BoolValue("NoCombat", true)
    private val silent = BoolValue("Silent", false)

    private val autoSwordValue = BoolValue("AutoWeapon", false)
    private val onlySwordValue = BoolValue("OnlySword", false).displayable { autoSwordValue.get() }
    private val silentValue = BoolValue("SpoofItem", false).displayable { autoSwordValue.get() }
    private val ticksValue = IntegerValue("SpoofTicks", 10, 1, 20).displayable { silentValue.get() }

    private val noCombatTimer = MSTimer()

    private var attackEnemy = false
    private var spoofedSlot = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        if (FDPClient.combatManager.inCombat) noCombatTimer.reset()

        if (noCombat.get() && !noCombatTimer.hasTimePassed(800L)) return
        switchSlot(event.clickedBlock ?: return)
    }

    fun switchSlot(blockPos: BlockPos) {
        var bestSpeed = 1F
        var bestSlot = -1

        val block = mc.theWorld.getBlockState(blockPos).block

        for (i in 0..8) {
            val item = mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(block)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }

        if (bestSlot != -1) {
            if (!silent.get()) {
                mc.thePlayer.inventory.currentItem = bestSlot
            } else {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(bestSlot))
                mc.playerController.updateController()   
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is C02PacketUseEntity && event.packet.action == C02PacketUseEntity.Action.ATTACK &&
                attackEnemy) {
            attackEnemy = false

            // Find best weapon in hotbar (#Kotlin Style)
            val (slot, _) = (0..8)
                    .map { Pair(it, mc.thePlayer.inventory.getStackInSlot(it)) }
                    .filter { it.second != null && (it.second.item is ItemSword || (it.second.item is ItemTool && !onlySwordValue.get())) }
                    .maxByOrNull {
                        (it.second.attributeModifiers["generic.attackDamage"].first()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(it.second, Enchantment.sharpness)
                    } ?: return

            if (slot == mc.thePlayer.inventory.currentItem) { // If in hand no need to swap
                return
            }

            // Switch to best weapon
            if (silentValue.get()) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(slot))
                spoofedSlot = ticksValue.get()
            } else {
                mc.thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            mc.netHandler.addToSendQueue(event.packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Switch back to old item after some time
        if (spoofedSlot > 0) {
            if (spoofedSlot == 1) {
                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
            }
            spoofedSlot--
        }
    }
}
