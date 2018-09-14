package morningstarwang.com.tmd_mobile.api

import morningstarwang.com.tmd_mobile.pojo.PostData
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface PostService {

    @POST("get_json")
    fun predict(@Body postData: RequestBody): Call<ResponseBody>
}