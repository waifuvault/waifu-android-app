package moe.waifuvault.mobile.data.remote

import moe.waifuvault.mobile.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface WaifuVaultApi {

    @GET("/rest/resources/restrictions")
    suspend fun getRestrictions(): Response<List<RestrictionDto>>

    @Multipart
    @PUT("/rest")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
        @Part("expires") expires: RequestBody? = null,
        @Part("hide_filename") hideFilename: RequestBody? = null,
        @Part("password") password: RequestBody? = null,
        @Part("one_time_download") oneTimeDownload: RequestBody? = null,
        @Part("bucket_token") bucketToken: RequestBody? = null
    ): Response<WaifuFileDto>
}
