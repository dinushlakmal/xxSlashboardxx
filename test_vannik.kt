import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider

fun main() {
    EmojiManager.install(IosEmojiProvider())
    // Wait, EmojiManager doesn't have getInstance(). It just has install()
}
