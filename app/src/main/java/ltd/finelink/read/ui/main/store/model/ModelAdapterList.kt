package ltd.finelink.read.ui.main.store.model

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
import ltd.finelink.read.data.entities.LocalTTS
import ltd.finelink.read.databinding.ItemModelListBinding
import ltd.finelink.read.utils.postEvent
import ltd.finelink.read.utils.visible
import splitties.views.onLongClick

class ModelAdapterList(
    context: Context,
    private val callBack: CallBack,
    private val lifecycle: Lifecycle
) :
    BaseModelAdapter<ItemModelListBinding>(context) {

    override fun getViewBinding(parent: ViewGroup): ItemModelListBinding {
        return ItemModelListBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemModelListBinding,
        item: LocalTTS,
        payloads: MutableList<Any>
    ) = binding.run {
        val bundle = payloads.getOrNull(0) as? Bundle
        if (bundle == null) {
            tvName.text = item.name
            tvAuthor.text = item.speakerName?:"Default"
            ivCover.load(item.cover, item.name, item.speaker, false, null)
            ivProgressBar.progress = item.progress
            ivEdit.visible(item.download)
            ivSetting.visible(item.download)
            tvLast.text = item.categroy()
            if(item.path.equals("asset")||item.path==""){
                ivMenuDelete.visible(false)
            }else{
                ivMenuDelete.visible(item.download)
            }
            ivDownload.visible(!item.download)
            ivEdit.setOnClickListener {
                callBack.openSetting(item)
            }
            ivMenuDelete.setOnClickListener {
                callBack.remove(item)
            }

            ivSetting.setOnClickListener {
                callBack.setEngine(item)
            }
        } else {
            bundle.keySet().forEach {
                when (it) {
                    "name" -> tvName.text = item.name
                    "speaker" -> tvAuthor.text = item.speakerName?:"Default"
                    "status" ->{
                        ivDownload.visible(item.status!=1)
                    }
                    "download" -> {
                        ivDownload.visible(item.status!=1&&!item.download)
                        ivEdit.visible(item.download)
                        ivMenuDelete.visible(item.download)
                        ivSetting.visible(item.download)
                    }
                    "progress"->  ivProgressBar.progress = item.progress
                    "cover" -> ivCover.load(
                        item.cover,
                        item.name,
                        item.speaker,
                        false,
                        null,
                        lifecycle
                    )
                    "refresh"->{
                        tvName.text = item.name
                        tvAuthor.text = item.speakerName?:"Default"
                        ivCover.load(
                            item.cover,
                            item.name,
                            item.speaker,
                            false,
                            null,
                            lifecycle
                        )
                        ivDownload.visible(item.status!=1&&!item.download)
                        ivEdit.visible(item.download)
                        ivMenuDelete.visible(item.download)
                        ivSetting.visible(item.download)
                    }

                }
            }
        }
    }

    private fun getEngineName(type:Int):String{
        if(type==0){
            return context.getString(R.string.gpt_engine)
        }
        if(type==1){
            return context.getString(R.string.tts_engine)
        }
        if(type==3){
            return context.getString(R.string.chat_engine)
        }
        if(type==4){
            return context.getString(R.string.cos_engine)
        }
        if(type==5){
            return context.getString(R.string.fish_engine)
        }
        return  context.getString(R.string.clone_engine)

    }


    private fun showDelMenu(view: View, file: LocalTTS) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.inflate(R.menu.file_long_click)
        popupMenu.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_del -> {
                    if(file.download){
                        callBack.remove(file,false)
                    }
                    appDb.localTTSDao.delete(file)
                    postEvent(EventBus.UP_STORE_MODEL,file.id)
                }
            }
            true
        }
        popupMenu.show()
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemModelListBinding) {
        holder.itemView.apply {
            setOnClickListener {
                getItem(holder.layoutPosition)?.let {
                    callBack.openModelInfo(it)
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