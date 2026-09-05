import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider

fun main() {
    EmojiManager.install(IosEmojiProvider())
    val e1 = "\uD83E\uDD26\u200D\u2640\uFE0F"
    val e2 = "\uD83E\uDD26\u200D\u2640"
    
    println("With FE0F: " + EmojiManager.getInstance().findAllEmojis(e1).isNotEmpty())
    println("Without FE0F: " + EmojiManager.getInstance().findAllEmojis(e2).isNotEmpty())
}
