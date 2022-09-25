package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.utils.extensions.getEntitiesInRadius
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import java.lang.ref.SoftReference

/**
 * Entity AABB cacher for backtracing entities
 */
class LocationCache : MinecraftInstance(), Listenable {
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            val theWorld = mc.theWorld ?: return
            val thePlayer = mc.thePlayer ?: return

            while (playerLocationList.size >= listSize - 1) playerLocationList.removeAt(0)

            playerLocationList.add(Location(Vec3(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ), RotationUtils.serverRotation))

            val entities = theWorld.getEntitiesInRadius(thePlayer, 64.0)

            // Manual garbage collect by distance check
            aabbList.keys.filterNot(entities.map(Entity::getEntityId)::contains).forEach(aabbList::remove)

            for (entity in entities) {
                val entityId = entity.entityId

                val list = aabbList[entityId]?.get() ?: mutableListOf()

                while (list.size >= listSize - 1) list.removeAt(0)

                list.add(entity.entityBoundingBox)

                aabbList[entityId] = SoftReference(list)
            }
        }
    }

    override fun handleEvents(): Boolean = true

    companion object {
        private const val listSize = 50

        // Automated garbage collect by SoftReference
        private val aabbList = HashMap<Int, SoftReference<MutableList<AxisAlignedBB>>>(listSize)
        private val playerLocationList = ArrayList<Location>(listSize)

        fun getPreviousAABB(entityId: Int, n: Int, default: AxisAlignedBB): AxisAlignedBB {
            if (aabbList.isEmpty()) return default

            return aabbList[entityId]?.get()?.run {
                val indexLimit = size - 1
                get((indexLimit - n).coerceIn(0, indexLimit))
            } ?: default
        }

        fun getPreviousPlayerLocation(n: Int, default: Location): Location {
            if (playerLocationList.isEmpty()) return default

            val indexLimit = playerLocationList.size - 1

            return playerLocationList[(indexLimit - n).coerceIn(0, indexLimit)]
        }
    }
}

data class Location(val position: Vec3, val rotation: Rotation)
