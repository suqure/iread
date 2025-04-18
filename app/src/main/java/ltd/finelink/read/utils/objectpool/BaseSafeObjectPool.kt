package ltd.finelink.read.utils.objectpool

import androidx.core.util.Pools

abstract class BaseSafeObjectPool<T : Any>(size: Int): BaseObjectPool<T>(size) {

    override val pool = Pools.SynchronizedPool<T>(size)

}
