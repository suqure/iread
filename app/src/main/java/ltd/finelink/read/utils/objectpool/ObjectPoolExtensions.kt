package ltd.finelink.read.utils.objectpool

fun <T> ObjectPool<T>.synchronized(): ObjectPool<T> = ObjectPoolLocked(this)
