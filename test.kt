import com.vanniktech.emoji.EmojiView
fun test() {
    val x = EmojiView::class.java.declaredMethods
    for (m in x) println(m.name)
}
