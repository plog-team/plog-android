package com.example.plog.model;

import com.google.gson.annotations.SerializedName;

/**
 * 백엔드 공통 응답 래퍼 {success, data, error}.
 * 모든 ApiService 메서드는 Call<ApiResponse<T>>로 호출 후 body().data로 접근.
 */
public class ApiResponse<T> {

    @SerializedName("success")
    public boolean success;

    @SerializedName("data")
    public T data;

    @SerializedName("error")
    public String error;
}
