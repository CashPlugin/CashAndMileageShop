package com.uomaep.cashandmileageshop.commands

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.cashandmileageshop.utils.UserMessage
import com.uomaep.cashandmileageshop.utils.UserUtil
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class CashCommand : CommandExecutor, TabCompleter {

    companion object {

        private fun help(player: Player) {
            val message = StringBuilder()
            message.append("§e---- 도움말 ----\n")
            message.append("§f/캐시 §7: 자신의 캐시를 확인합니다.\n")
            message.append("§f/캐시 보기 <플레이어> §7: 해당 플레이어의 캐시를 확인합니다.\n")
            message.append("§f/캐시 설정 <플레이어> <금액> §7: 해당 플레이어의 캐시를 해당 금액으로 설정합니다.\n")
            message.append("§f/캐시 지급 <플레이어> <금액> §7: 해당 플레이어에게 금액만큼의 캐시를 지급합니다.\n")
            message.append("§f/캐시 차감 <플레이어> <금액> §7: 해당 플레이어에게서 금액만큼의 캐시를 차감합니다.\n")
            message.append("§e---------------\n")
            UserMessage.sendSuccessMessage(player, message.toString())
        }

        private fun getCashByUUID(uuid: String? = null): Int {
            if (uuid == null) return -1
            val resultSet = DatabaseManager.select("select cash from user where uuid = '$uuid';")
            if (resultSet!!.next()) {
                return resultSet.getInt("cash")
            }
            return -1
        }

        private fun setCashByUUID(uuid: String? = null, balance: Int): Boolean {
            if (uuid == null) return false
            return DatabaseManager.update("update user set cash = $balance where uuid = '$uuid';")
        }

        private fun addCashByUUID(uuid: String? = null, balance: Int): Boolean {
            if (uuid == null) return false
            val cash = getCashByUUID(uuid)
            if (cash == -1) return false
            return setCashByUUID(uuid, cash + balance)
        }

        private fun subtractCashByUUID(uuid: String? = null, balance: Int): Boolean {
            if (uuid == null) return false
            val cash = getCashByUUID(uuid)
            if (cash == -1) return false

            if (cash - balance < 0) return setCashByUUID(uuid, 0)
            return setCashByUUID(uuid, cash - balance)
        }

    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        val player = sender as Player

        if (args?.size == 0) {
            val cash = getCashByUUID(player.uniqueId.toString())
            if (cash == -1) {
                UserMessage.sendFailMessage(player, "캐시를 불러오는 도중 오류가 발생했습니다.")
                return false
            }
            UserMessage.sendSuccessMessage(player, "캐시: §a${cash}§f원")
            return true
        }

        if (!player.isOp) {
            UserMessage.sendFailMessage(player, "/캐시 §7- 캐시를 확인합니다.")
            return false
        }

        if (args?.size == 1 || args?.get(0) == "도움말") {
            help(player)
            return false
        }

        if (args?.size == 2) {
            when (args[0]) {
                "보기" -> {
                    val nickname = args[1]
                    val uuid = UserUtil.getPlayerUUID(nickname)
                    val cash = getCashByUUID(uuid.toString())

                    if (cash == -1) {
                        UserMessage.sendFailMessage(player, "존재하지 않는 플레이어입니다.")
                        return false
                    }
                    UserMessage.sendSuccessMessage(player, "§e${nickname}§f님의 캐시: §a${cash}§f원")
                    return true
                }

                else -> {
                    help(player)
                    return false
                }
            }
        }

        if (args?.size == 3) {
            if (!(listOf("설정", "지급", "차감").contains(args[0]))) {
                help(player)
                return false
            }

            val nickname = args[1]
            val uuid = UserUtil.getPlayerUUID(nickname)
            val balance: Int

            try {
                balance = args[2].toInt()
                if (balance < 0) {
                    UserMessage.sendFailMessage(player, "금액은 0보다 작을 수 없습니다.")
                    return false
                }
                when (args[0]) {
                    "설정" -> {
                        if (!setCashByUUID(uuid.toString(), balance)) {
                            UserMessage.sendFailMessage(player, "캐시를 설정하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        UserMessage.sendSuccessMessage(player, "§e${nickname}§f님의 캐시를 §a${balance}§f원으로 설정했습니다.")
                        return true
                    }

                    "지급" -> {
                        if (!addCashByUUID(uuid.toString(), balance)) {
                            UserMessage.sendFailMessage(player, "캐시를 지급하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        UserMessage.sendSuccessMessage(player, "§e${nickname}§f님에게 §a${balance}§f원을 지급했습니다.")
                        return true
                    }

                    "차감" -> {
                        if (!subtractCashByUUID(uuid.toString(), balance)) {
                            UserMessage.sendFailMessage(player, "캐시를 차감하는 도중 오류가 발생했습니다.")
                            return false
                        }
                        UserMessage.sendSuccessMessage(player, "§e${nickname}§f님의 캐시에서 §c${balance}§f원을 차감했습니다.")
                        return true
                    }

                    else -> {
                        help(player)
                        return false
                    }
                }
            } catch (e: NumberFormatException) {
                UserMessage.sendFailMessage(player, "금액은 숫자로 입력해주세요.")
                return false
            } catch (e: Exception) {
                UserMessage.sendFailMessage(player, "오류가 발생했습니다.")
                return false
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): List<String>? {
        val player = sender as Player
        if (!(player.isOp)) return null
        else {
            if (args?.size == 1) {
                return listOf("도움말", "보기", "설정", "지급", "차감")
            } else if (args?.get(0) != "보기" && args?.size == 2) {
                return null
            } else if (!(args?.get(0).equals("보기")) && args?.size == 3) {
                return listOf("<금액>")
            }
        }
        return null
    }
}
