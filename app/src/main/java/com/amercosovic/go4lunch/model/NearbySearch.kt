
import com.amercosovic.go4lunch.model.Result
import com.google.gson.annotations.SerializedName

data class NearbySearch(
    @SerializedName("results")
    val results: List<Result>
)