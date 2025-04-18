package ltd.finelink.read.help.llm

import ltd.finelink.read.data.entities.LLMConfig

object LLMHelp {

    private var model: LLMModel? = null

    fun loadModel(modelConfig: LLMConfig): LLMModel {
         if(model==null){
             if(modelConfig.type==0){
                 model =  MLCModel(modelConfig)
             }else {
                 model =  APIModel(modelConfig);
             }
         }else {
             if(modelConfig.type==model!!.modelType()){
                 model!!.requestReload(modelConfig)
             }else {
                 model!!.unload()
                 if(modelConfig.type==0){
                     model = MLCModel(modelConfig)
                 }else {
                     model = APIModel(modelConfig);
                 }
             }
         }
        return model!!
    }


}




