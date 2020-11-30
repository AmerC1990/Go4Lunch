
import com.google.gson.annotations.SerializedName

data class NearbySearch(
    @SerializedName("results")
    val results: List<Result>
)