package com.example.beaconattendance

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET

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
    val status: Int,
    val today: Int
)

data class StatData(
    val device_address: String,
    val action: String
)

interface AttendanceApi {
    // 출석 기록 저장
    @POST("attendance")
    fun saveAttendance(@Body data: LogData): Call<Any>

    // 기기 등록
    @POST("Add_Device")
    fun register(@Body data: PhoneData): Call<RegisterResponse>

    // 로그인
    @POST("login")
    fun login(@Body data: LoginData): Call<LoginResponse>

    // 출석 체크
    @POST("check")
    fun check(@Body data: Check): Call<Any>

    // 상태 확인
    @POST("status")
    fun getStatus(@Body data: Check): Call<StatusResponse>

    // 통계 업데이트
    @POST("statistics")
    fun updateStatistics(@Body data: StatData): Call<Any>

    // 설정 가져오기
    @GET("settings")
    fun getSettings(): Call<Map<String, String>>
}