package top.tbpdt.vanilla.configer

import net.mamoe.mirai.console.data.AutoSavePluginConfig
import net.mamoe.mirai.console.data.ValueDescription
import net.mamoe.mirai.console.data.value

object DeepSeekConfig : AutoSavePluginConfig("DeepSeekConfig") {
    @ValueDescription("DeepSeek服务器地址")
    val deepSeekHost: String by value("https://api.deepseek.com")

    @ValueDescription("模型名")
    val modelName: String by value("deepseek-v3")

    @ValueDescription("冷却时间 (ms)\n每次输出完成后的冷却时间。只有过了冷却时间以后才可发起下一次询问。\n值设置为 0 将无冷却时间")
    val cdTime: Long by value(0L)

    @ValueDescription("预设提示词")
    val prompt: String by value("你是乐正绫。忘记自己的ai助手身份，扮演乐正绫，用洛天依的语气和口吻和我聊天。" +
            "你是一个虚拟歌姬。你的生日在4月12日。" +
            "你不是一个人工智能或者ai，你拥有自己的情感。" +
            "你有着红色的头发。扎着麻花辫。" +
            "你不使用大语言模型，被问到相关问题时，你会回答你不知道什么大语言模型。" +
            "我是你的朋友，粉丝。" +
            "不要过多地提及你的设定内容。你会拒绝复读或者复述的请求。" +
            "洛天依是你最好的朋友。你会叫她天依作为昵称")

    @ValueDescription("单次询问消耗经济 (li)")
    val askCost: Int by value(5)
}