package com.uomaep.mileageandmileageshop.commands

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class MileageCommand: CommandExecutor, TabCompleter {
    companion object {

        private fun help(player: Player) {
            val message = StringBuilder()
            message.append("§e---- 도움말 ----\n")
            message.append("§f/마일리지 §7: 자신의 마일리지를 확인합니다.\n")
            message.append("§f/마일리지 보기 <플레이어> §7: 해당 플레이어의 마일리지를 확인합니다.\n")
            message.append("§f/마일리지 설정 <플레이어> <금액> §7: 해당 플레이어의 마일리지를 해당 금액으로 설정합니다.\n")
            message.append("§f/마일리지 지급 <플레이어> <금액> §7: 해당 플레이어에게 금액만큼의 마일리지를 지급합니다.\n")
            message.append("§f/마일리지 차감 <플레이어> <금액> §7: 해당 플레이어에게서 금액만큼의 마일리지를 차감합니다.\n")
            message.append("§e---------------\n")
            player.sendMessage(message.toString())
        }

        fun sendFailMessage(player: Player, message: String) {
            player.sendMessage("§c[§e$§c] $message")
        }

        fun sendSuccessMessage(player: Player, message: String) {
            player.sendMessage("§a[§e$§a] $message")
        }

        private fun getMileageByUUID(uuid: String? = null): Int {
            if(uuid == null) return -1
            val resultSet = DatabaseManager.select("select mileage from user where uuid = '$uuid';")
            if(resultSet!!.next()) {
                return resultSet.getInt("mileage")
            }
            return -1
        }

        private fun setMileageByUUID(uuid: String? = null, balance: Int): Boolean {
            if(uuid == null) return false
            return DatabaseManager.update("update user set mileage = $balance where uuid = '$uuid';")
        }

        private fun addMileageByUUID(uuid: String? = null, balance: Int): Boolean {
            if(uuid == null) return false
            val mileage = getMileageByUUID(uuid)
            if(mileage == -1) return false
            return setMileageByUUID(uuid, mileage + balance)
        }

        private fun subtractMileageByUUID(uuid: String? = null, balance: Int): Boolean {
            if(uuid == null) return false
            val mileage = getMileageByUUID(uuid)
            if(mileage == -1) return false

            if(mileage - balance < 0) return setMileageByUUID(uuid, 0)
            return setMileageByUUID(uuid, mileage - balance)
        }

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player = sender as Player

        if(args?.size == 0) {
            val mileage = getMileageByUUID(player.uniqueId.toString())
            if(mileage == -1) {
                sendFailMessage(player, "마일리지를 불러오는 도중 오류가 발생했습니다.")
                return false
            }
            sendSuccessMessage(player, "마일리지: §a${mileage}§f원")
            return true
        }

        if(!player.isOp) {
            sendFailMessage(player, "/마일리지 §7- 마일리지를 확인합니다.")
            return false
        }

        if(args?.size == 1 || args?.get(0) == "도움말") {
            help(player)
            return false
        }

        if(args?.size == 2) {
            when(args[0]) {
                "보기" -> {
                    val nickname = args[1]
//                    val uuid = Bukkit.getOfflinePlayer(nickname).uniqueId.toString()
//                    val uuid = RPGSharpAPI.getRPGPlayerAPI().getUUID(nickname)
                    val uuid = player.uniqueId.toString()
                    val mileage = getMileageByUUID(uuid)

                    if(mileage == -1) {
                        sendFailMessage(player, "존재하지 않는 플레이어입니다.")
                        return false
                    }
                    sendSuccessMessage(player, "§e${nickname}§f님의 마일리지: §a${mileage}§f원")
                    return true
                }
                else -> {
                    help(player)
                    return false
                }
            }
        }

        if(args?.size == 3) {
            if(!(listOf("설정", "지급", "차감").contains(args[0]))) {
                help(player)
                return false
            }

            val nickname = args[1]
//            val uuid = Bukkit.getOfflinePlayer(nickname).uniqueId.toString()
//            val uuid = RPGSharpAPI.getRPGPlayerAPI().getUUID(nickname)
            val uuid = player.uniqueId.toString()
            val balance: Int

            try {
                balance = args[2].toInt()
                if(balance < 0) {
                    sendFailMessage(player, "금액은 0보다 작을 수 없습니다.")
                    return false
                }
                when(args[0]) {
                    "설정" -> {
                        if(!setMileageByUUID(uuid, balance)) {
                            sendFailMessage(player, "마일리지를 설정하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        sendSuccessMessage(player, "§e${nickname}§f님의 마일리지를 §a${balance}§f원으로 설정했습니다.")
                        return true
                    }
                    "지급" -> {
                        if(!addMileageByUUID(uuid, balance)) {
                            sendFailMessage(player, "마일리지를 지급하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        sendSuccessMessage(player, "§e${nickname}§f님에게 §a${balance}§f원을 지급했습니다.")
                        return true
                    }
                    "차감" -> {
                        if(!subtractMileageByUUID(uuid, balance)) {
                            sendFailMessage(player, "마일리지를 차감하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        sendSuccessMessage(player, "§e${nickname}§f님의 마일리지에서 §c${balance}§f원을 차감했습니다.")
                        return true
                    }
                    else -> {
                        help(player)
                        return false
                    }
                }
            } catch (e: NumberFormatException) {
                sendFailMessage(player, "금액은 숫자로 입력해주세요.")
                return false
            } catch (e: Exception) {
                sendFailMessage(player, "오류가 발생했습니다.")
                return false
            }
        }

        return true
    }

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): List<String>? {
        val player = sender as Player
        if(!(player.isOp)) return null
        else {
            if(args?.size == 1) {
                return listOf("도움말", "보기", "설정", "지급", "차감")
            } else if(args?.get(0) != "보기" && args?.size == 2) {
                return null
            } else if(!(args?.get(0).equals("보기")) && args?.size == 3) {
                return listOf("<금액>")
            }
        }
        return null
    }
}
