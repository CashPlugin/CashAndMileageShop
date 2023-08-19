package com.uomaep.kotlintestplugin.command

import com.uomaep.cashandmileageshop.utils.DatabaseManager
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class CashShopCommand(): CommandExecutor, TabCompleter {

    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<out String>?): List<String> {
        if (args!!.size == 1) {
            return listOf("생성", "삭제", "진열", "아이템삭제", "목록", "오픈", "닫기")
        }
        return when (args[0]) {
            "생성"-> {
                listOf("<이름> <줄 수>")
            }

            "삭제" -> {
                getCashShopNames(listOf(1, 2))
            }

            "진열" -> {
                val cashShopNames = getCashShopNames(listOf(1, 2))

                when(val cashShopName = args[1]){
                    in cashShopNames-> {
                        if (args.size == 2) {
                            return cashShopNames
                        }

                        val itemNames = mutableListOf<String>()

                        val sql = "select name from item;"
                        val resultSet = DatabaseManager.select(sql)!!

                        while (resultSet.next()) {
                            itemNames += resultSet.getString("name")
                        }

                        return when(val itemName = args[2]){
                            in itemNames -> {
                                listOf("<슬롯번호> <가격> <최대구매가능갯수(개인)> <최대구매가능갯수(전역)>")
                            }
                            else -> itemNames
                        }
                    }
                    else -> cashShopNames
                }
            }

            "아이템삭제"-> {
                val cashShopNames = getCashShopNames(listOf(1, 2))

                when(val cashShopName = args[1]){
                    in cashShopNames -> {
                        val slotNums = mutableListOf<String>()

                        val sql2 = "select cash_item.slot_num " +
                                "from cash_item join " +
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

            "목록" -> {
                val openCashShop: List<String> = getCashShopNames(listOf(1)).map { "$it(닫힘)" }
                val closeCashShop: List<String> = getCashShopNames(listOf(2)).map { "$it(오픈)" }
                val deleteCashShop: List<String> = getCashShopNames(listOf(3)).map { "$it(삭제됨)" }
                openCashShop.plus(closeCashShop.toTypedArray()).plus(deleteCashShop.toTypedArray())
            }

            "오픈" -> {
                getCashShopNames(listOf(1))
            }

            "닫기" -> {
                getCashShopNames(listOf(2))
            }

            else -> {
                listOf()
            }
        }
    }

    private fun getCashShopNames(states: List<Int>): MutableList<String> {
        val cashShopNames = mutableListOf<String>()
        val intList = states.toString().replace("[", "(").replace("]", ")")

        val sql = "select name from cash_shop where state in $intList;"
        val resultSet = DatabaseManager.select(sql)!!

        while (resultSet.next()) {
            cashShopNames += resultSet.getString("name")
        }

        return cashShopNames
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>?): Boolean {
        if (!sender.isOp) {
            sender.sendMessage("§c권한이 없습니다.")
            return false
        }

        if (args!!.isEmpty()) {
            val message = StringBuilder()
            message.append("/캐시샵 생성 <이름> <줄 수> : 캐시샵을 생성하는 명령어").append("\n")
            message.append("/캐시샵 삭제 <이름> : 해당 캐시샵 삭제").append("\n")
            message.append("/캐시샵 진열 <샵이름> <아이템이름> <슬롯번호> <가격> <최대구매가능갯수(개인)> <최대구매가능갯수(전역)>").append("\n")
            message.append("    : 캐시샵에 물품 등록. 구매 가능 갯수가 -1일 경우 제한이 없음. (개인)은 인당 최대 구매가능 갯수고 전역은 전체 유저 한정 물품 갯수.").append("\n")
            message.append("/캐시샵 아이템삭제 <이름> <슬롯번호> : 해당 캐시샵의 <슬롯번호> 칸의 물품을 삭제").append("\n")
            message.append("/캐시샵 목록 <샵이름>: 캐시샵 목록과 각 캐시샵에 등록된 아이템 목록을 출력").append("\n")
            message.append("    : 삭제된 캐시샵도 목록에 포함됨").append("\n")
            message.append("/캐시샵 오픈 <샵이름> : 샵 오픈").append("\n")
            message.append("/캐시샵 닫기 <샵이름>: 캐시샵을 임시로 닫음").append("\n")

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

                val sql1 = "select id from cash_shop where name = '$name';"
                val result1 = DatabaseManager.select(sql1)!!

                if (result1.next()){
                    sender.sendMessage("#Error 캐시샵 생성 실패: $name 이미 존재하는 상점")
                    return false
                }

                val sql2 = "insert into cash_shop(name, line_num) values('$name', $lineNum);"
                val result = DatabaseManager.insert(sql2)

                if (!result){
                    sender.sendMessage("#Error 캐시샵 생성 실패: $name DB에러")
                    return false
                }

                sender.sendMessage("<$name> <$lineNum> 캐시샵이 생성되었습니다.")
            }

            "삭제" -> {
                if (args.size != 2) {
                    sender.sendMessage("명령어 사용법: /캐시샵 삭제 <이름>")
                    return false
                }

                val cashShopName = args[1]

                val sql1 = "select id from cash_shop where name = '$cashShopName' and state in (1, 2);"
                val result = DatabaseManager.select(sql1)!!

                if (!result.next()){
                    sender.sendMessage("#Error 캐시샵 삭제 실패: $cashShopName 존재하지 않는 상정명")
                    return false
                }
                val cashShopId = result.getInt("id")

                val sql2 = "update cash_shop " +
                        "set state = 3 " +
                        "where id = $cashShopId;"
                val result2 = DatabaseManager.update(sql2)

                if (!result2){
                    sender.sendMessage("#Error 캐시샵 삭제 실패: $cashShopName DB에러")
                    return false
                }

                sender.sendMessage("<$cashShopName> 캐시샵이 삭제되었습니다.")
            }

            "진열" -> {
                if(args.size != 7){
                    sender.sendMessage("명령어 사용법: /캐시샵 진열 <샵이름> <아이템이름> <슬롯번호> <가격> <최대구매가능갯수(개인)> <최대구매가능갯수(전역)>" +
                            "    : 캐시샵에 물품 등록. 구매 가능 갯수가 -1일 경우 제한이 없음. (개인)은 인당 최대 구매가능 갯수고 전역은 전체 유저 한정 물품 갯수.")
                    return false
                }

                val cashShopName = args[1]
                val itemName = args[2]

                val sql1 = "select id from cash_shop where name = '$cashShopName' and state in (1, 2);"
                val result1 = DatabaseManager.select(sql1)!!
                if (!result1.next()){
                    sender.sendMessage("#Error 캐시샵 진열 실패: $cashShopName 존재하지 않는 상점명")
                    return false
                }

                val sql2 = "select id from item where name = '$itemName';"
                val result2 = DatabaseManager.select(sql2)!!
                if (!result2.next()){
                    sender.sendMessage("#Error 캐시샵 진열 실패: $itemName 존재하지 않는 아이템명")
                    return false
                }

                val cashShopId = result1.getInt("id")
                val itemId = result2.getInt("id")
                val slotNum = args[3].toInt()
                val price = args[4].toInt()
                val maxBuyableCnt = args[5].toInt()
                val maxBuyableCntServer = args[6].toInt()

                val sql3 = "insert into cash_item(max_buyable_cnt, price, item_id, cash_shop_id, max_buyable_cnt_server, slot_num)\n" +
                        "VALUES ($maxBuyableCnt, $price, $itemId, $cashShopId, $maxBuyableCntServer, $slotNum);"
                val result3 = DatabaseManager.insert(sql3)

                if(!result3){
                    sender.sendMessage("#Error 캐시샵 진열 실패: DB에러")
                    return false
                }

                sender.sendMessage("[<$cashShopName>]에 [<$itemName>: <$price>캐시]가 진열되었습니다.")
            }

            "아이템삭제" -> {
                if (args.size != 3) {
                    sender.sendMessage("명령어 사용법: /캐시샵 아이템삭제 <이름> <슬롯번호>")
                    return false
                }

                val cashShopName = args[1]

                val sql1 = "select id from cash_shop where name = '$cashShopName' and state in (1, 2);"
                val result1 = DatabaseManager.select(sql1)!!
                if (!result1.next()){
                    sender.sendMessage("#Error 캐시샵 아이템삭제 실패: $cashShopName 존재하지 않는 상점명")
                    return false
                }
                val cashShopId = result1.getInt("id")

                val itemSlotNum = args[2].toInt()
                val sql2 = "select slot_num from cash_item where cash_shop_id = $cashShopId;"
                val result2 = DatabaseManager.select(sql2)!!

                val slotNums = mutableListOf<Int>()
                while (result2.next()){
                    slotNums.add(result2.getInt("slot_num"))
                }

                if (itemSlotNum !in slotNums){
                    sender.sendMessage("#Error 캐시샵 아이템삭제 실패: $cashShopName -> $itemSlotNum 존재하지 않는 아이템")
                    return false
                }

                val sql = "update cash_item " +
                        "set state = 2 " +
                        "where slot_num = $itemSlotNum " +
                        "    and cash_shop_id = $cashShopId;"
                val result = DatabaseManager.update(sql)

                if (!result){
                    sender.sendMessage("#Error 캐시샵 아이템삭제 실패: $cashShopName DB에러")
                    return false
                }

                sender.sendMessage("<$cashShopName> <$itemSlotNum> 아이템이 캐시샵에서 삭제됐습니다.")
            }

            "목록" -> {
                if (args.size != 2) {
                    sender.sendMessage("명령어 사용법: /캐시샵 목록 <샵이름>: 캐시샵 목록과 각 캐시샵에 등록된 아이템 목록을 출력")
                    return false
                }

                val cashShopName = args[1].replace("(닫힘)", "").replace("(오픈)", "").replace("(삭제됨)", "")
                val sql1 = "select id from cash_shop where name = '$cashShopName';"
                val result1 = DatabaseManager.select(sql1)!!
                if (!result1.next()){
                    sender.sendMessage("#Error 캐시샵 목록 실패: $cashShopName 존재하지 않는 상점명")
                    return false
                }
                val cashShopId = result1.getInt("id")

                val sql2 = "select name, price from item join (select item_id, price from cash_item where cash_shop_id = $cashShopId) as cashItems on id = item_id;"
                val result2 = DatabaseManager.select(sql2)!!
                val sb = StringBuilder()

                val metaData = result2.metaData
                val columnCount = metaData.columnCount

                sb.append("=============================================================").append("\n")
                sb.append("$cashShopName 캐시샵에 등록된 아이템 목록").append("\n")
                var cnt=1
                while (result2.next()) {
                    val rowValues = StringBuilder()
                    for (i in 1..columnCount) {
                        if (i > 1) {
                            rowValues.append(", ")
                        }
                        rowValues.append(metaData.getColumnName(i)).append(": ").append(result2.getString(i))
                    }
                    sb.append("[${cnt++}]: ").append(rowValues).append("\n")
                }

                if (cnt==1){
                    sender.sendMessage("$cashShopName 캐시샵에 등록된 아이템이 없습니다.")
                    return false
                }
                sb.append("=============================================================").append("\n")

                sender.sendMessage(sb.toString())
            }

            "오픈" -> {
                if (args.size != 2){
                    sender.sendMessage("명령어 사용법: /캐시샵 오픈 <샵이름>: 캐시샵을 오픈")
                    return false
                }

                val cashShopName = args[1]
                val sql1 = "select id from cash_shop where name = '$cashShopName' and state = 1;"
                val result1 = DatabaseManager.select(sql1)!!

                if (!result1.next()){
                    sender.sendMessage("#Error 캐시샵 오픈 실패: $cashShopName 존재하지 않는 상점명")
                    return false
                }
                val cashShopId = result1.getInt("id")

                val sql2 = "update cash_shop set state = 2 where id = $cashShopId and state = 1;"
                val result2 = DatabaseManager.update(sql2)

                if (!result2){
                    sender.sendMessage("#Error 캐시샵 오픈 실패: $cashShopName DB에러")
                    return false
                }

                sender.sendMessage("$cashShopName 캐시샵이 오픈되었습니다.")
            }

            "닫기" -> {
                if (args.size != 2){
                    sender.sendMessage("명령어 사용법: /캐시샵 닫기 <샵이름>: 캐시샵을 임시로 닫음")
                    return false
                }

                val cashShopName = args[1]
                val sql1 = "select id from cash_shop where name = '$cashShopName' and state = 2;"
                val result1 = DatabaseManager.select(sql1)!!

                if (!result1.next()){
                    sender.sendMessage("#Error 캐시샵 오픈 실패: $cashShopName 존재하지 않는 상점명")
                    return false
                }
                val cashShopId = result1.getInt("id")

                val sql2 = "update cash_shop set state = 1 where id = $cashShopId and state = 2;"
                val result2 = DatabaseManager.update(sql2)

                if (!result2){
                    sender.sendMessage("#Error 캐시샵 닫기 실패: $cashShopName DB에러")
                    return false
                }

                sender.sendMessage("$cashShopName 캐시샵을 닫았습니다.")
            }

            else -> {
                sender.sendMessage("명령어 사용법: /캐시샵")
                return false
            }
        }

        return true
    }
}
