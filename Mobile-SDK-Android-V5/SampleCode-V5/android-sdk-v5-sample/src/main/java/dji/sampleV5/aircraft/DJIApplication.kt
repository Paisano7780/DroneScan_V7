package dji.sampleV5.aircraft

import android.app.Application
import dji.sampleV5.aircraft.models.MSDKManagerVM
import dji.sampleV5.aircraft.models.globalViewModels

/**
 * Class Description
 *
 * @author Hoker
 * @date 2022/3/1
 *
 * Copyright (c) 2022, DJI All Rights Reserved.
 */
open class DJIApplication : Application() {

    private val msdkManagerVM: MSDKManagerVM by globalViewModels()

    override fun onCreate() {
        super.onCreate()
        // Inicializar y registrar la app con el SDK de DJI (lógica de Java)
        msdkManagerVM.initMobileSDK(this)
        // Inicialización de Helper global (si es necesario, lógica de Java)
        try {
            val helperClass = Class.forName("dji.sampleV5.aircraft.util.Helper")
            val installMethod = helperClass.getMethod("install", Application::class.java)
            installMethod.invoke(null, this)
        } catch (e: Exception) {
            // Si no existe Helper o método, ignorar
        }
    }

}
