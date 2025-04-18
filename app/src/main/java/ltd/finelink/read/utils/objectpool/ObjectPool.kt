package ltd.finelink.read.utils.objectpool

interface ObjectPool<T> {

    fun obtain(): T

    fun recycle(target: T)

    fun create(): T

}
