package com.example.freelancefirst.Network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadAPI {
    @Multipart
    @POST("uploadImage")
    Call<MyResponse> uploadImage(@Part MultipartBody.Part file);
}
