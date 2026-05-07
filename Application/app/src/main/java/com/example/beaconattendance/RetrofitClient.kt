package com.example.beaconattendance
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object RetrofitClient {
    private const val BASE_URL = "https://leyla-semimat-lula.ngrok-free.dev"

    lateinit var api: AttendanceApi

    // 세팅값
    private var checkoutHour: Int = 0
    private var checkoutMinute: Int = 0
    private var checkinHour: Int = 0
    private var checkinMinute: Int = 0
    private var totalSeats: Int = 0

    fun init() {
        api = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AttendanceApi::class.java)

        getSettings()
    }

    private fun getSettings() {
        api.getSettings().enqueue(object : Callback<Map<String, String>> {
            override fun onResponse(call: Call<Map<String, String>>, response: Response<Map<String, String>>) {
                response.body()?.let { settings ->
                    settings["checkout_time"]?.split(":")?.map { it.toInt() }?.let {
                        checkoutHour = it[0]
                        checkoutMinute = it[1]
                    }
                    settings["checkin_time"]?.split(":")?.map { it.toInt() }?.let {
                        checkinHour = it[0]
                        checkinMinute = it[1]
                    }
                    totalSeats = settings["total_seats"]?.toInt() ?: 29
                }
            }

            override fun onFailure(call: Call<Map<String, String>>, t: Throwable) {
                // 실패시 기본값 유지
            }
        })
    }

    fun GetCheckInTime(): Pair<Int, Int> = Pair(checkinHour, checkinMinute)
    fun GetCheckOutTime(): Pair<Int, Int> =  Pair(checkoutHour, checkoutMinute)
    fun GetTotalSeat(): Int = totalSeats
}