import android.util.Log
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface TrilaterationApi {
    @POST("trilaterate/")
    suspend fun trilaterate(@Body data: TrilaterationRequest): Response<TrilaterationResponse>

}

data class TrilaterationRequest(
    val beaconLocations: List<List<Double>>,
    val estimatedDistances: List<Double>
)

data class TrilaterationResponse(
    val x: Double,
    val y: Double,
    val z: Double
)

class TrilaterationService(baseUrl: String) {

    private val TAG = "TrilaterationService" // Tag for logging

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    suspend fun getEstimatedLocation(data: TrilaterationRequest): TrilaterationResponse? {
        val api = retrofit.create(TrilaterationApi::class.java)
        Log.d("TAG", "Base URL: ${retrofit.baseUrl()}")

        // Check data size (beacon locations should match estimated distances)
        if (data.beaconLocations.size != data.estimatedDistances.size) {
            Log.e(
                TAG,
                "Error: Data size mismatch. Beacon locations and estimated distances must have the same number of elements."
            )
            return null
        }

        // Check individual elements within beacon locations
        for (location in data.beaconLocations) {
            if (location.size != 3) {
                Log.e(
                    TAG,
                    "Error: Invalid beacon location format. Each location should have 3 elements (x, y, z)."
                )
                return null
            }
        }

        // Convert TrilaterationRequest to JSON string
        val jsonData = Gson().toJson(data)
        Log.d(TAG, "Sending request data (JSON): $data")

        try {
            val response: Response<TrilaterationResponse> = api.trilaterate(data)
            if (response.isSuccessful) {
                return response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e(TAG, "Error: API request failed with status code: ${response.code()}")
                if (errorBody != null) {
                    Log.e(TAG, "Error body: $errorBody")
                }
                // Handle specific error codes here (optional)
            }
        } catch (e: HttpException) {
            // Handle network exceptions
            Log.e(TAG, "Network error: ${e.localizedMessage}")
        }
        return null
    }
}
