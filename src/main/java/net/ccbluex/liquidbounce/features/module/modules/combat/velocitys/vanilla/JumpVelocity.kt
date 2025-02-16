package net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.vanilla

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.velocitys.VelocityMode
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.minecraft.network.play.server.S12PacketEntityVelocity

class JumpVelocity : VelocityMode("Jump") {
    private val modeValue = ListValue("${valuePrefix}Mode", arrayOf("Motion", "Jump", "Both"), "Jump")
    private val jumpReductionValue = BoolValue("${valuePrefix}ExtraReduction", false)
    private val jumpReductionAmountValue = FloatValue("${valuePrefix}ExtraReductionAmount", 1f, 0.1f, 1f)
    private val motionValue = FloatValue("${valuePrefix}Motion", 0.42f, 0.4f, 0.5f)
    private val failValue = BoolValue("${valuePrefix}SmartFail", true)
    private val failRateValue = FloatValue("${valuePrefix}FailRate", 0.3f, 0.0f, 1.0f).displayable { failValue.get() }
    private val failJumpValue = FloatValue("${valuePrefix}FailJumpRate", 0.25f, 0.0f, 1.0f).displayable { failValue.get() }
    
    private var doJump = true
    private var failJump = false
    private var skipVeloc = false
    
    override fun onVelocity(event: UpdateEvent) {
        if ((failJump || mc.thePlayer.hurtTime > 6) && mc.thePlayer.onGround) {
            if (failJump) {
                failJump = false
            }
            if (!doJump) {
                skipVeloc = true
            }
            if (Math.random() <= failRateValue.get() && failValue.get()) {
                if (Math.random() <= failJumpValue.get()) {
                    doJump = true
                    failJump = true
                } else {
                    doJump = false
                    failJump = false
                }
            } else {
                doJump = true
                failJump = false
            }
            if (skipVeloc) {
                skipVeloc = false
                return
            }
            when(modeValue.get().lowercase()) {
                "motion" -> mc.thePlayer.motionY = motionValue.get().toDouble()
                "jump" -> mc.thePlayer.jump()
                "both" -> {
                    mc.thePlayer.jump()
                    mc.thePlayer.motionY = motionValue.get().toDouble()
                }
            }
        }
    }
    override fun onVelocityPacket(event: PacketEvent) {
        val packet = event.packet
        if(packet is S12PacketEntityVelocity && jumpReductionValue.get()) {
            packet.motionX = (packet.getMotionX() * jumpReductionAmountValue.get().toDouble()).toInt()
            packet.motionZ = (packet.getMotionZ() * jumpReductionAmountValue.get().toDouble()).toInt()
        }
    }
}
