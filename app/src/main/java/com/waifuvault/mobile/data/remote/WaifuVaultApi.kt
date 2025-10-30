package com.waifuvault.mobile.data.remote

import com.waifuvault.mobile.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface WaifuVaultApi {

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

    @GET("/rest/{token}")
    suspend fun getFileInfo(
        @Path("token") token: String,
        @Query("formatted") formatted: Boolean = true
    ): Response<WaifuFileDto>

    @DELETE("/rest/{token}")
    suspend fun deleteFile(
        @Path("token") token: String
    ): Response<ResponseBody>

    @GET("/rest/{identifier}")
    suspend fun downloadFile(
        @Path("identifier") identifier: String,
        @Query("password") password: String? = null
    ): Response<ResponseBody>

    @PATCH("/rest/{token}")
    suspend fun modifyEntry(
        @Path("token") token: String,
        @Body request: ModifyEntryRequest
    ): Response<WaifuFileDto>

    // Bucket operations
    @POST("/rest/bucket/create")
    suspend fun createBucket(): Response<WaifuBucketDto>

    @GET("/rest/bucket/{token}")
    suspend fun getBucket(
        @Path("token") token: String
    ): Response<WaifuBucketDto>

    @DELETE("/rest/bucket/{token}")
    suspend fun deleteBucket(
        @Path("token") token: String
    ): Response<ResponseBody>

    // Album operations
    @POST("/rest/album/create")
    suspend fun createAlbum(
        @Body request: CreateAlbumRequest
    ): Response<WaifuAlbumDto>

    @GET("/rest/album/{token}")
    suspend fun getAlbum(
        @Path("token") token: String
    ): Response<WaifuAlbumDto>

    @DELETE("/rest/album/{token}")
    suspend fun deleteAlbum(
        @Path("token") token: String,
        @Query("deleteFiles") deleteFiles: Boolean = false
    ): Response<ResponseBody>

    @PUT("/rest/album/share/{token}")
    suspend fun shareAlbum(
        @Path("token") token: String
    ): Response<WaifuAlbumDto>

    @DELETE("/rest/album/share/{token}")
    suspend fun revokeAlbum(
        @Path("token") token: String
    ): Response<ResponseBody>

    @PUT("/rest/album/{albumToken}/files")
    suspend fun associateFiles(
        @Path("albumToken") albumToken: String,
        @Body request: AssociateFilesRequest
    ): Response<WaifuAlbumDto>

    @DELETE("/rest/album/{albumToken}/files")
    suspend fun disassociateFiles(
        @Path("albumToken") albumToken: String,
        @Body request: AssociateFilesRequest
    ): Response<WaifuAlbumDto>

    @GET("/rest/album/download/{token}")
    suspend fun downloadAlbum(
        @Path("token") token: String,
        @Query("fileIds") fileIds: String? = null
    ): Response<ResponseBody>

    // Restrictions
    @GET("/rest/resources/restrictions")
    suspend fun getRestrictions(): Response<RestrictionsDto>
}
