package com.example.beaconattendance

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// 서버로 보내는 데이터
data class LogData(
    val action: String,
    val device_address: String
)
data class PhoneData(
    val id: String,
    val password: String,
    val device_name: String,
    val device_address: String,
    val seat: Int
)
data class LoginData(
    val id: String,
    val password: String
)

data class Check(
    val device_address: String
)

// 반환 받는 데이터
data class LoginResponse(
    val success: Boolean
)

data class RegisterResponse(
    val success: Int
)

data class StatusResponse(
    val status: Int
)

interface AttendanceApi {
    @POST("attendance")
    fun saveAttendance(@Body data: LogData): Call<Any>

    @POST("Add_Device")
    fun register(@Body data: PhoneData): Call<RegisterResponse>

    @POST("login")
    fun login(@Body data: LoginData): Call<LoginResponse>

    @POST("check")
    fun check(@Body data: Check): Call<Any>

    @POST("status")
    fun getStatus(@Body data: Check): Call<StatusResponse>
}