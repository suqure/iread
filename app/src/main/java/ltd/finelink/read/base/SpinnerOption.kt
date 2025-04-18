package ltd.finelink.read.base

class SpinnerOption (
    val id: Long = 0L,
    val text: String = "Default"
){
    override fun toString(): String {
        return text
    }
}