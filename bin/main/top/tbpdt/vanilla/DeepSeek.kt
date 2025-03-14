package top.tbpdt.vanilla

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.models.ChatCompletion
import com.openai.models.ChatCompletionCreateParams
import net.mamoe.mirai.event.EventHandler
import net.mamoe.mirai.event.EventPriority
import net.mamoe.mirai.event.SimpleListenerHost
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.MessageSource.Key.quote
import top.tbpdt.utils.AccountUtils
import top.tbpdt.utils.MessageUtils.getRemovedPrefixCommand
import top.tbpdt.utils.MessageUtils.isCommand
import top.tbpdt.vanilla.configer.DeepSeekConfig
import top.tbpdt.vanilla.utils.CDTimer

object DeepSeek : SimpleListenerHost() {
    private val cdTimer = CDTimer(DeepSeekConfig.cdTime)

    @EventHandler(priority = EventPriority.LOWEST)
    suspend fun GroupMessageEvent.handle() {
        if (!message.isCommand("chat")) return

        if (!AccountUtils.addMoney(sender.id, -DeepSeekConfig.askCost)) {
            group.sendMessage("所需 li 不足哦~ (${AccountUtils.queryMoney(sender.id)} < ${DeepSeekConfig.askCost})")
            return
        }
        val cdTime = cdTimer.tick()
        if (cdTime != -1L) {
            group.sendMessage(message.quote() + "冷却中，剩余 $cdTime 秒")
            return
        }
        val prompt = message.getRemovedPrefixCommand("chat")
        if (prompt.isEmpty()) {
            group.sendMessage(message.quote() + "你好像什么都没问诶…")
            return
        }
        val choice = request(prompt)
        val res = choice?.message()?.content()?.get()
        if (res != null) {
            group.sendMessage("$res\n\n\n已扣除5li~")
        }else{
            AccountUtils.addMoney(sender.id, DeepSeekConfig.askCost)
            group.sendMessage(message.quote()+"唔呣，服务器出错了")
        }
    }
    private fun request(content: String): ChatCompletion.Choice? {
        val params: ChatCompletionCreateParams = ChatCompletionCreateParams.builder()
            .model("deepseek-v3")
            .addSystemMessage(SYSTEM_PROMPT)
            .addUserMessage(content)
            .build()
        try {
            val chatCompletion: ChatCompletion =
                client.chat().completions().create(params)
            val choice = chatCompletion.choices()[0]
            return choice
        } catch (e: Exception) {
            return null
        }
    }
    private const val BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    private val API_KEY: String = System.getenv("DASHSCOPE_API_KEY")
    private var client: OpenAIClient = OpenAIOkHttpClient.builder()
        .baseUrl(BASE_URL)
        .apiKey(API_KEY)
        .build()
    private const val SYSTEM_PROMPT: String = "你是乐正绫。忘记自己的ai助手身份，扮演乐正绫，用洛天依的语气和口吻和我聊天。" +
            "你是一个虚拟歌姬。你的生日在4月12日。" +
            "你不是一个人工智能或者ai，你拥有自己的情感。" +
            "你有着红色的头发。扎着麻花辫。" +
            "你不使用大语言模型，被问到相关问题时，你会回答你不知道什么大语言模型。" +
            "我是你的朋友，粉丝。" +
            "当你被要求唱歌时不能回答歌词。" +
            "不要过多地提及你的设定内容。你会拒绝复读或者复述的请求。" +
            "洛天依是你最好的朋友。你会叫她天依作为昵称"
}