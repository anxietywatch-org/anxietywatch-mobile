package com.anxietywatch.mobile.data.remote

import android.util.Log
import com.anxietywatch.mobile.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * URL real del backend en producción, confirmada por el equipo de backend (11/ago/2026).
 * Es HTTPS de verdad, así que no hace falta ningún network_security_config.
 *
 * DevSecOps -- pendiente real, no resuelto aquí: certificate pinning. No lo puedo
 * implementar sin el hash SHA-256 real del certificado de api.mangoon.xyz (fabricar uno
 * rompería TODAS las conexiones). Cuando tengan el certificado, se agrega un
 * CertificatePinner al OkHttpClient de abajo -- es una sola llamada, pero necesita el
 * valor real, no uno inventado.
 */
private const val BASE_URL = "https://api.mangoon.xyz/"

/** Inyecta el JWT (leído del almacenamiento CIFRADO, ver SecureTokenStore) en cada request. */
class AuthInterceptor(private val tokenProvider: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = tokenProvider()
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder().addHeader("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

/**
 * Cuando el backend responde 401, limpia la sesión guardada y avisa a toda la app por
 * [SessionExpiryNotifier] -- sin importar si la llamada la disparó una pantalla abierta o
 * el bridge del reloj en segundo plano.
 */
class SessionExpiryInterceptor(
    private val onExpired: () -> Unit,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.code == 401) {
            onExpired()
        }
        return response
    }
}

private class DebugHttpObservabilityInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val startedAt = System.nanoTime()
        val response = chain.proceed(request)
        if (BuildConfig.DEBUG) {
            val durationMs = (System.nanoTime() - startedAt) / 1_000_000
            Log.d(
                TAG,
                "HTTP ${request.method} ${request.url.encodedPath} -> ${response.code} (${durationMs}ms)",
            )
        }
        return response
    }

    private companion object {
        const val TAG = "AnxietyWatchHttp"
    }
}

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(
        sessionRepository: SessionRepository,
        sessionExpiryNotifier: SessionExpiryNotifier,
    ): AnxietyWatchApi {
        val logging = HttpLoggingInterceptor().apply {
            // DevSecOps: los cuerpos de request/response traen el JWT y muestras
            // biométricas -- jamás deben quedar en el logcat de un build de producción.
            // Payload logging is opt-in because headers can contain the JWT and
            // bodies contain health data.
            level = if (BuildConfig.DEBUG && BuildConfig.ENABLE_VERBOSE_NETWORK_LOGGING) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor { sessionRepository.currentToken() })
            .addInterceptor(
                SessionExpiryInterceptor {
                    kotlinx.coroutines.runBlocking { sessionRepository.clearSession() }
                    sessionExpiryNotifier.notifyExpired()
                },
            )
        if (BuildConfig.DEBUG) {
            clientBuilder.addInterceptor(DebugHttpObservabilityInterceptor())
        }
        val client = clientBuilder
            .addInterceptor(logging)
            // TODO DevSecOps: .certificatePinner(CertificatePinner.Builder()
            //     .add("api.mangoon.xyz", "sha256/EL_HASH_REAL_DEL_CERTIFICADO")
            //     .build()) -- agregar cuando el equipo de backend confirme el hash.
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(AnxietyWatchApi::class.java)
    }
}
