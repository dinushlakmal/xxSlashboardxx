import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.ios.IosEmojiProvider
import org.json.JSONObject
import java.io.File

fun main() {
    EmojiManager.install(IosEmojiProvider())
    val allValid = EmojiManager.getInstance().findAllEmojis("").map { it.unicode } 
    // Wait, findAllEmojis is not quite right. 
}
