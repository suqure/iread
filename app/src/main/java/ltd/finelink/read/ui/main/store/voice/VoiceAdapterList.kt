package ltd.finelink.read.ui.main.store.voice

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import ltd.finelink.read.R
import ltd.finelink.read.base.adapter.ItemViewHolder
import ltd.finelink.read.constant.EventBus
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.TTSSpeaker
import ltd.finelink.read.databinding.ItemVoiceListBinding
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.visible
import splitties.views.onLongClick

class VoiceAdapterList(
    context: Context,
    private val callBack: CallBack,
    private val lifecycle: Lifecycle
) :
    BaseVoiceAdapter<ItemVoiceListBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemVoiceListBinding {
        return ItemVoiceListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemVoiceListBinding,
        item: TTSSpeaker,
        payloads: MutableList<Any>
    ) = binding.run {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            tvName.text = item.name
            tvAuthor.text = item.description
            ivCover.load(item.cover, item.name, item.description, false, null)
            ivProgressBar.progress = item.progress

            tvLast.text = item.categroy()
            if(item.path.equals("asset")){
                ivMenuDelete.visible(false)
            }else{
                ivMenuDelete.visible(item.download)
            }
            ivDownload.visible(!item.download)
            ivApply.visible(item.download)
            if(item.path==""){
                ivMenuDelete.visible(false)
                ivDownload.visible(false)
            }
            ivMenuDelete.setOnClickListener {
                callBack.remove(item)
            }
            ivApply.setOnClickListener {
                callBack.openSetting(item)
            }

        } else {
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "description" -> tvAuthor.text = item.description
                    "status" ->{
                        ivDownload.visible(item.status!=1)
                    }
                    "download" -> {
                        ivDownload.visible(item.status!=1&&!item.download)
                        ivMenuDelete.visible(item.download)
                        ivApply.visible(item.download)
                    }
                    "progress"-> ivProgressBar.progress = item.progress
                    "cover" -> ivCover.load(
                        null,
                        item.name,
                        item.description,
                        false,
                        null,
                        lifecycle
                    )

                }
            }
        }
    }

    private fun getEngineName(type:Int):String{
        if(type==1){
            return context.getString(R.string.tts_voice)
        }
        if(type==3){
            return context.getString(R.string.chat_voice)
        }
        if(type==4){
            return context.getString(R.string.cos_voice)
        }
        if(type==5){
            return context.getString(R.string.fish_voice)
        }
        return  context.getString(R.string.clone_voice)

    }


    private fun showDelMenu(view: View, file: TTSSpeaker) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.file_long_click)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_del -> {
                    if(file.download){
                        callBack.remove(file,false)
                    }
                    appDb.ttsSpeakerDao.delete(file)
                    postEvent(EventBus.UP_STORE_VOICE,"")
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemVoiceListBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.openVoiceInfo(it)
                }
            }
            onLongClick {
                getItem(holder.layoutPosition)?.let {
                    showDelMenu(this,it)
                }
            }
        }
    }
}