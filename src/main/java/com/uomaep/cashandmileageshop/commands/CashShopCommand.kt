package com.uomaep.kotlintestplugin.command

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import com.uomaep.kotlintestplugin.DatabaseConfig
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import java.sql.SQLException
import java.sql.Statement

class CashShopCommand(): CommandExecutor, TabCompleter {

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): List<String> {
        if (args!!.size == 1) {
            return listOf("생성","아이템삭제","제거","목록","진열","주기","뺏기","랭킹","오픈")
        }
        return when (args[0]) {
            "생성"-> {
                listOf("<이름> <줄 수>")
            }
            "아이템삭제"-> {
                val cashShopNames = mutableListOf<String>()

                val sql = "select name from cash_shop;"
                val resultSet = DatabaseManager.select(sql)!!

                while(resultSet.next()){
                    cashShopNames += resultSet.getString("name")
                }

                when(val cashShopName = args[1]){
                    in cashShopNames -> {
                        val slotNums = mutableListOf<String>()

                        val sql2 = "select cash_item.slot_num from cash_item join " +
                                "(select id from cash_shop where name = '$cashShopName') as cash_shop_id " +
                                "on cash_shop_id.id = cash_item.cash_shop_id " +
                                "and state = 1;"

                        val resultSet1 = DatabaseManager.select(sql2)!!

                        while(resultSet1.next()){
                            slotNums += resultSet1.getInt("slot_num").toString()
                        }

                        slotNums
                    }
                    else -> cashShopNames
                }
            }
            else -> {
                listOf()
            }
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§c권한이 없습니다.")
            return false
        }

        if (args!!.isEmpty()) {
            val message = StringBuilder()
            message.append("/캐시샵 생성 <이름> <줄 수> : 캐시샵을 생성하는 명령어").append("\n")
            message.append("/캐시샵 아이템삭제 <이름> <슬롯번호> : 해당 캐시샵의 <슬롯번호> 칸의 물품을 삭제").append("\n")
            message.append("/캐시샵 삭제 <이름> : 해당 캐시샵 삭제").append("\n")
            message.append("/캐시샵 목록 : 캐시샵 목록 출력").append("\n")
            message.append("/캐시샵 진열 <샵이름> <아이템이름> <슬롯번호> <가격> <최대구매가능갯수(개인)> <최대구매가능갯수(전역)>").append("\n")
            message.append("    : 캐시샵에 물품 등록. 구매 가능 갯수가 -1일 경우 제한이 없음. (개인)은 인당 최대 구매가능 갯수고 전역은 전체 유저 한정 물품 갯수.").append("\n")
            message.append("/캐시샵 주기 <닉네임> <수치> : 해당 유저에게 캐시 지급").append("\n")
            message.append("/캐시샵 뺏기 <닉네임> <수치> : 해당 유저의 캐시 박탈").append("\n")
            message.append("/캐시샵 설정 <닉네임> <수치> : 해당 유저의 캐시 설정").append("\n")
            message.append("/캐시샵 랭킹 : 캐시 보유량 랭킹 출력 ( 닉네임 ) : (보유량) 형태").append("\n")
            message.append("/캐시샵 오픈 <샵이름> : 샵 오픈").append("\n")

            sender.sendMessage(message.toString())
            return false
        }

        when(args[0]){
            "생성" -> {
                if (args.size != 3) {
                    sender.sendMessage("명령어 사용법: /캐시샵 생성 <이름> <줄 수>")
                    return false
                }

                val name = args[1]
                val lineNum = args[2].toInt()

                val sql = "insert into cash_shop(name, line_num) values('$name', $lineNum);"
                val result = DatabaseManager.insert(sql)

                if (!result){
                    sender.sendMessage("#Error 캐시샵 생성 실패: $name")
                    return false
                }

                sender.sendMessage("<$name> <$lineNum> 캐시샵이 생성되었습니다.")
            }

            "아이템삭제" -> {
                if (args.size != 3) {
                    sender.sendMessage("명령어 사용법: /캐시샵 아이템삭제 <이름> <슬롯번호>")
                    return false
                }

                val name = args[1]
                val slotNum = args[2].toInt()

                val sql = "update cash_item " +
                        "set state = 2 " +
                        "where slot_num = $slotNum " +
                        "    and cash_shop_id = (select id " +
                        "                        from cash_shop " +
                        "                        where cash_shop.name = '$name');"
                val result = DatabaseManager.update(sql)

                if (!result){
                    sender.sendMessage("#Error 캐시샵 아이템삭제 실패: $name")
                    return false
                }

                sender.sendMessage("<$name> <$slotNum> 아이템이 캐시샵에서 삭제됐습니다.")
            }

            else -> {
                sender.sendMessage("명령어 사용법: /캐시샵")
                return false
            }
        }

        return true
    }
}
