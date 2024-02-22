package top.tbpdt

import configer.CaveConfig
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.emptyMessageChain
import top.tbpdt.PluginMain.save
import top.tbpdt.configer.GlobalConfig
import top.tbpdt.utils.CaveUtils
import top.tbpdt.utils.CaveUtils.getCommentCount
import top.tbpdt.utils.CaveUtils.loadCaveIds
import top.tbpdt.utils.CaveUtils.loadComments
import top.tbpdt.utils.CaveUtils.replaceExpiredImage
import top.tbpdt.utils.CaveUtils.updatePickCount
import top.tbpdt.utils.MessageUtils.getRemovedPrefixCommand
import top.tbpdt.utils.MessageUtils.isCommand
import java.text.SimpleDateFormat

/**
 * @author Takeoff0518
 */
object Cave : SimpleListenerHost() {
    @EventHandler(priority = EventPriority.HIGH)
    suspend fun GroupMessageEvent.handle() {
        if (message.isCommand("ca")) {
            val text = message.getRemovedPrefixCommand("ca")
            if (text.isEmpty()) {
                group.sendMessage("不能添加空信息！")
                return
            }
            if (text.toIntOrNull() != null) {
                group.sendMessage("只输入了数字……是不是要用 .ci 呢？")
                return
            }
            if (message.any { it is Image }) {
                group.sendMessage("由于技术原因，暂不支持图片的投稿哦~")
                return
            }
            val cdTime = CaveUtils.checkInterval()
            if (cdTime != -1L) {
                group.sendMessage("冷却中，剩余 $cdTime 秒")
                return
            }
            val id = getCommentCount() + 1
            CaveUtils.saveComment(id, text, sender.id, sender.nick, group.id, group.name)
            loadComments(id).first().replaceExpiredImage(group)
            group.sendMessage("回声洞 #${id} 添加成功~")
        }
        if (message.isCommand("cq") || message.isCommand("捡")) {
            val randomId = (1..getCommentCount()).filter { it !in CaveConfig.caveBlackList }.random()
            val comment = loadComments(randomId)
            for (i in comment) {
                /*
                    回声洞 #233

                    逸一时误一世。

                    --洛雨辰~
                 */
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.replaceExpiredImage(group)
                result += PlainText("\n\n--${i.senderNick}")
                group.sendMessage(result)
                updatePickCount(randomId)
            }
        }
        if (message.isCommand("ci")) {
            val id: Int
            try {
                id = message.getRemovedPrefixCommand("ci").toInt()
            } catch (e: NumberFormatException) {
                group.sendMessage("解析失败……参数是不是没有填数字或者是填的不是数字？")
                return
            }
            if (id !in 1..getCommentCount()) {
                group.sendMessage("你所查询的回声洞不在范围里呢，现在共有${getCommentCount()}条回声洞~")
                return
            }
            if (id in CaveConfig.caveBlackList) {
                group.sendMessage("该回声洞已被删除！")
                return
            }
            val comment = loadComments(id)
            for (i in comment) {
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.replaceExpiredImage(group)
                result += PlainText("\n\n--${i.senderNick}(${i.senderId})\n")
                result += PlainText("from ${i.groupNick}(${i.groupId})\n")
                result += PlainText("已被捡起 ${i.pickCount} 次\n")
                result += PlainText("at " + SimpleDateFormat("yy/MM/dd HH:mm:ss").format(i.date))
                group.sendMessage(result)
                updatePickCount(id)
            }
        }
        if (message.isCommand("cf")) {
            val target = message.getRemovedPrefixCommand("cf")
            if (target.isEmpty()) {
                group.sendMessage("查询条件不能为空！")
                return
            }
            group.sendMessage("查询中，请稍后……")
            val comment = loadComments(target).filter { it.caveId !in CaveConfig.caveBlackList }
            if (comment.isEmpty()) {
                group.sendMessage("一个也没找到惹……")
                return
            }
            group.sendMessage("共计：${comment.size}")
            val forwardResult = ForwardMessageBuilder(group)
            for (i in comment) {
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.replaceExpiredImage(group)
                result += PlainText("\n\n--${i.senderNick}(${i.senderId})\n")
                result += PlainText("from ${i.groupNick}(${i.groupId})\n")
                result += PlainText("已被捡起 ${i.pickCount} 次\n")
                result += PlainText("at " + SimpleDateFormat("yy/MM/dd HH:mm:ss").format(i.date))
                updatePickCount(i.caveId)
                forwardResult.add(bot.id, "#" + i.caveId, result)
                // 超长分条发送
                if (forwardResult.size > 20) {
                    group.sendMessage(forwardResult.build())
                    forwardResult.clear()
                }
            }
            group.sendMessage(forwardResult.build())
        }
        if (message.isCommand("rc") && (sender.id in GlobalConfig.admin)) {
            group.sendMessage("下载中……")
            val (totalCount, successCount) = CaveUtils.downloadAllPictures()
            group.sendMessage("下载结束~\n共向服务器请求 $totalCount 张，成功下载 $successCount 张")
        }
        if (message.isCommand("mycave")) {
            val queryId = message.getRemovedPrefixCommand("mycave").toLongOrNull()
            val userId = queryId ?: sender.id
            val commentIds = loadCaveIds(userId)
            val messagePrefix = if (queryId != null) "ta" else "你"
            if (commentIds.isEmpty()) {
                group.sendMessage("${messagePrefix}似乎……还没有投稿过回声洞呢~")
            } else {
                val (availableComments, removedComments) = commentIds.partition { it !in CaveConfig.caveBlackList }
                var result = "${messagePrefix}共有 ${availableComments.size} 条回声洞：\n$availableComments"
                if (removedComments.isNotEmpty()) {
                    result += "\n被删除 ${removedComments.size} 条回声洞：\n$removedComments"
                }
                group.sendMessage(result)
            }
        }
        if (message.isCommand("rmcave")) {
            val queryId = message.getRemovedPrefixCommand("rmcave").toIntOrNull()
            if (sender.id in GlobalConfig.admin) {
                if (queryId == null) {
                    group.sendMessage("共有 ${CaveConfig.caveBlackList.size} 条删除的回声洞：\n${CaveConfig.caveBlackList}")
                    return
                }
                if (queryId !in 1..getCommentCount()) {
                    group.sendMessage("你所查询的回声洞不在范围里呢，现在共有${getCommentCount()}条回声洞~")
                    return
                }
                removeCave(queryId, group)
            } else {
                if (queryId == null) {
                    return
                }
                if (queryId !in 1..getCommentCount()) {
                    group.sendMessage("你所查询的回声洞不在范围里呢，现在共有${getCommentCount()}条回声洞~")
                    return
                }
                if (loadComments(queryId).first().senderId == sender.id) {
                    removeCave(queryId, group)
                } else {
                    group.sendMessage("移除失败，请通过 .mycave 查看自己投稿过的回声洞哦~")
                }
            }
        }
    }

    private suspend fun removeCave(queryId: Int, group: Group) {
        if (queryId in CaveConfig.caveBlackList) {
            CaveConfig.caveBlackList.remove(queryId)
            group.sendMessage("已撤销对回声洞 #$queryId 的删除~")
        } else {
            CaveConfig.caveBlackList.add(queryId)
            group.sendMessage("已删除回声洞 #$queryId ~")
        }
        CaveConfig.save()
    }
}