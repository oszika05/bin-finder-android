package hu.bme.binfinder.api

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType

class Bins(private val idToken: String?) {

    init {
        Log.d("BINS", "token: $idToken")
    }

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            install(DefaultRequest) {
                if (idToken !== null) {
                    headers.append("Authorization", "Bearer $idToken")
                }
            }
        }
    }

    suspend fun get(from: LatLng, to: LatLng, types: List<BinType>): List<Bin> {
        val typesQuery = if (types.isNotEmpty()) {
            "&" + types.joinToString("&") { "type=${it.id}" }
        } else {
            ""
        }

        return client.get<List<Bin>>("${API_URL}/bin?from-latitude=${from.latitude}&from-longitude=${from.longitude}&to-latitude=${to.latitude}&to-longitude=${to.longitude}${typesQuery}")
    }

    suspend fun getTypes(): List<BinType> {
        return client.get<List<BinType>>("${API_URL}/bin/types")
    }

    suspend fun create(lat: Double, long: Double, type: BinType) {
        val dto = CreateBinDto(lat, long, type.id)

        client.post<Unit> {
            url("${API_URL}/bin")
            contentType(ContentType.Application.Json)
            body = dto
        }
    }

    suspend fun report(binId: Int) {
        client.post<Unit>("${API_URL}/bin/$binId/report")
    }

    fun close() {
        client.close()
    }

    companion object {
        private const val API_URL = "http://192.168.0.241:3000"
    }

}