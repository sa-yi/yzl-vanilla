package top.tbpdt

import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.code.MiraiCode.deserializeMiraiCode
import net.mamoe.mirai.message.data.ForwardMessageBuilder
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.emptyMessageChain
import top.tbpdt.configer.GlobalConfig
import top.tbpdt.utils.CaveUtils
import top.tbpdt.utils.CaveUtils.loadComments
import top.tbpdt.utils.CaveUtils.updatePickCount
import top.tbpdt.utils.MessageUtils.getPlainText
import java.text.SimpleDateFormat

/**
 * @author Takeoff0518
 */
object Cave : SimpleListenerHost() {
    @EventHandler(priority = EventPriority.HIGH)
    suspend fun GroupMessageEvent.handle() {
        if (message.getPlainText().startsWith("${GlobalConfig.commandPrefix}ca")) {
            val text = message.serializeToMiraiCode().removePrefix("${GlobalConfig.commandPrefix}ca").trim()
            if (text.isEmpty()) {
                group.sendMessage("不能添加空信息！")
                return
            }
            val id = CaveUtils.getCommentCount() + 1
            CaveUtils.saveComment(id, text, sender.id, sender.nick, group.id, group.name)
            group.sendMessage("回声洞 #${id} 添加成功~")
        }
        if (message.getPlainText().startsWith("${GlobalConfig.commandPrefix}cq")) {
            val randomId = (1..CaveUtils.getCommentCount()).random()
            val comment = loadComments(randomId)
            for (i in comment) {
                /*
                    回声洞 #233

                    逸一时误一世。

                    --洛雨辰~(1145141919)
                    at 23/01/16 9:15:20
                 */
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.text.deserializeMiraiCode()
                result += PlainText("\n\n--${i.senderNick}(${i.senderId})\nat ")
                result += PlainText(SimpleDateFormat("yy/MM/dd HH:mm:ss").format(i.date))
                group.sendMessage(result)
                updatePickCount(randomId)
            }
        }
        if (message.getPlainText().startsWith("${GlobalConfig.commandPrefix}ci")) {
            var id = 1
            try {
                id =
                    message.serializeToMiraiCode().removePrefix("${GlobalConfig.commandPrefix}ci").trim().toInt()
            } catch (e: NumberFormatException) {
                group.sendMessage("解析失败……参数是不是没有填数字或者是填的不是数字？")
                return
            }
            if (id !in 1..CaveUtils.getCommentCount()) {
                group.sendMessage("你所查询的回声洞不在范围里呢，现在共有${CaveUtils.getCommentCount()}条回声洞~")
                return
            }
            val comment = loadComments(id)
            for (i in comment) {
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.text.deserializeMiraiCode()
                result += PlainText("\n\n--${i.senderNick}(${i.senderId})\n")
                result += PlainText("from ${i.groupNick}(${i.groupId})\n")
                result += PlainText("已被捡起 ${i.pickCount} 次\n")
                result += PlainText("at " + SimpleDateFormat("yy/MM/dd HH:mm:ss").format(i.date))
                group.sendMessage(result)
            }
        }
        if (message.getPlainText().startsWith("${GlobalConfig.commandPrefix}cf")) {
            val target = message.serializeToMiraiCode().removePrefix("${GlobalConfig.commandPrefix}cf").trim()
            if (target.isEmpty()) {
                group.sendMessage("查询条件不能为空！")
                return
            }
            val comment = loadComments(target)
            val forwardResult = ForwardMessageBuilder(group)
            for (i in comment) {
                var result = emptyMessageChain()
                result += PlainText("回声洞 #${i.caveId}\n\n")
                result += i.text.deserializeMiraiCode()
                result += PlainText("\n\n--${i.senderNick}(${i.senderId})\n")
                result += PlainText("from ${i.groupNick}(${i.groupId})\n")
                result += PlainText("已被捡起 ${i.pickCount} 次\n")
                result += PlainText("at " + SimpleDateFormat("yy/MM/dd HH:mm:ss").format(i.date))
                forwardResult.add(bot.id, "#" + i.caveId, result)
            }
            forwardResult.add(bot.id, bot.nick, PlainText("共计：${comment.size}"))
            group.sendMessage(forwardResult.build())
        }
    }
}