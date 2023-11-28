package com.uomaep.cashandmileageshop.utils

import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import java.util.*

object UserUtil {
    fun getPlayerUUID(playerName: String): UUID? {
        val player: Player? = Bukkit.getPlayer(playerName)

        // 온라인 플레이어인 경우
        if (player != null) {
            return player.uniqueId
        }

        // 오프라인 플레이어인 경우
        // 여기서는 서버에 저장된 플레이어 데이터에서 UUID를 가져옵니다.
        return Bukkit.getOfflinePlayer(playerName)?.uniqueId
    }

    fun playBuyCompleteSound(player: HumanEntity): Unit {
        val p = player as Player
        p.playSound(p.location, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f)
    }
}
