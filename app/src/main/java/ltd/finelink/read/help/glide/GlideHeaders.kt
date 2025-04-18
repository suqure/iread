package ltd.finelink.read.help.glide

import com.bumptech.glide.load.model.Headers

class GlideHeaders(private val headers: MutableMap<String, String>) : Headers {

    override fun getHeaders(): MutableMap<String, String> {
        return headers
    }

}