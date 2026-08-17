package com.anxietywatch.mobile.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tu compañero de backend confirmo: el JWT de accept-by-code dura 30 minutos. Cuando
 * expira (la API responde 401, o expiresAt ya paso), toda la app -- incluido el
 * MonitoringForegroundService en segundo plano -- debe volver a "Ingresa tu código",
 * sin excepciones ni pantallas a medias. Este es el canal por el que CUALQUIER parte
 * de la app (un interceptor de red, el bridge del reloj, un Worker) avisa "se acabó
 * la sesión", y MainActivity reacciona en un solo lugar.
 */
@Singleton
class SessionExpiryNotifier @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyExpired() {
        _events.tryEmit(Unit)
    }
}
