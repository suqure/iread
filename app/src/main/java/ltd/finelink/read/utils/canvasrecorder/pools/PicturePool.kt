package ltd.finelink.read.utils.canvasrecorder.pools

import android.graphics.Picture
import ltd.finelink.read.utils.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
