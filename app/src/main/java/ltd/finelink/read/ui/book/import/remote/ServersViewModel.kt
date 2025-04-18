package ltd.finelink.read.ui.book.import.remote

import android.app.Application
import ltd.finelink.read.base.BaseViewModel
import ltd.finelink.read.data.appDb
import ltd.finelink.read.data.entities.Server

class ServersViewModel(application: Application): BaseViewModel(application) {


    fun delete(server: Server) {
        execute {
            appDb.serverDao.delete(server)
        }
    }

}