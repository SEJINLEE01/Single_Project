package com.example.beaconattendance

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.content.Intent
import java.util.Calendar


class MainActivity : AppCompatActivity() {
    private lateinit var checkData: Check
    private var isProcessing = false
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var statusText: TextView
    private lateinit var logText: TextView
    private var isScanning = false
    private val resumeHandler = Handler(Looper.getMainLooper())
    private val resumeRunnable = Runnable {
        isProcessing = false
        startScan()
    }

    private var lastAttendanceTime: Long = 0
    private val ATTENDANCE_COOLDOWN = 30 * 1000L // 30초

    var inTimeTotal = 0 //입실시간
    var OutTimeTotal = 0 // 퇴실 시간

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        checkData = Check(device_address = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID))

        var result = RetrofitClient.GetCheckInTime()
        inTimeTotal = result.first * 60 + result.second + 20  // 입실 시간 + 20분

        result = RetrofitClient.GetCheckOutTime()
        OutTimeTotal = result.first * 60 + result.second // 퇴실 시간

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            val logData = createLogData(this@MainActivity, "LogOut")
            RetrofitClient.api.saveAttendance(logData).enqueue(object : Callback<Any> {
                override fun onResponse(call: Call<Any>, response: Response<Any>) {}
                override fun onFailure(call: Call<Any>, t: Throwable) {}
            })

            val prefs = getSharedPreferences("login", MODE_PRIVATE)
            prefs.edit().putBoolean("isLoggedIn", false).apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        isProcessing = false
        //앱을 껏다켰을때도 저장해있던 값을 가져옴
        lastAttendanceTime = getSharedPreferences("AttendanceTime", MODE_PRIVATE)
            .getLong("lastAttendanceTime", 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceLast = currentTime - lastAttendanceTime

        when {
            lastAttendanceTime == 0L -> startScan()
            timeSinceLast >= ATTENDANCE_COOLDOWN -> startScan()
            else -> {
                val remaining = ATTENDANCE_COOLDOWN - timeSinceLast
                addLog("⏳ 남은 시간: ${remaining / 1000}초")
                resumeHandler.postDelayed(resumeRunnable, remaining)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        resumeHandler.removeCallbacks(resumeRunnable)
        stopScan()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 1)
        }
    }

    private fun startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) return
        if (isScanning) return

        isScanning = true
        statusText.text = "상태: 스캔 중..."
        addLog("스캔 시작")
        bluetoothAdapter.bluetoothLeScanner.startScan(scanCallback)
    }

    private fun stopScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) return
        if (!isScanning) return

        isScanning = false
        statusText.text = "상태: 대기 중"
        addLog("스캔 중지")
        bluetoothAdapter.bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val deviceName = if (ActivityCompat.checkSelfPermission(
                    this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) result.device.name ?: "이름없음" else "권한없음"

            val rssi = result.rssi

            // 원래값 -60 디버그용으로 그냥 -120까지 받도록 설정
            if (deviceName.contains("MiniBeacon") && rssi >= -120) {
                if (isProcessing) return  // 처리 중이면 무시
                isProcessing = true

                val currentTime = System.currentTimeMillis()

                val calendar = Calendar.getInstance()
                val currentTotal = calendar.get(Calendar.HOUR_OF_DAY)*60 + calendar.get(Calendar.MINUTE)


                RetrofitClient.api.getStatus(checkData).enqueue(object : Callback<StatusResponse> {
                    override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                        val status = response.body()?.status
                        val today = response.body()?.today
                        if (status == 0 && today != 1) {
                            if (currentTotal < 8 * 60) {
                                // 8시 이전 → 출석 안됨
                                addLog("⚠️ 아직 출석 시간이 아닙니다.")
                                isProcessing = false
                                return
                            }
                            if (currentTotal > OutTimeTotal) {
                                // 퇴실시간 이후 -> 출석시간 이미 지남
                                addLog("⚠️ 출석시간이 아닙니다.")
                                isProcessing = false
                                return
                            }

                            lastAttendanceTime = currentTime

                            //마지막 시간으로 저장
                            getSharedPreferences("AttendanceTime", MODE_PRIVATE)
                                .edit().putLong("lastAttendanceTime", lastAttendanceTime)
                                .apply()
                            // 퇴실 상태 → 출석 처리
                            // 지각처리
                            if(currentTotal <= inTimeTotal)  {
                                sendAttendance("attendance")
                                addLog("✅ 출석 처리")
                            }
                            else {
                                RetrofitClient.api.updateStatistics(StatData(checkData.device_address, "late")).enqueue(object : Callback<Any> {
                                    override fun onResponse(call: Call<Any>, response: Response<Any>) {}
                                    override fun onFailure(call: Call<Any>, t: Throwable) {}
                                })
                                sendAttendance("Tardy") // 지각 로그
                                addLog("⚠️ 지각 처리")
                            }

                        } else if(status==1) {
                            // 출석 상태 → 퇴실 처리
                            if(currentTotal<OutTimeTotal){
                                runOnUiThread {
                                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                                        .setTitle("퇴실 확인")
                                        .setMessage("아직 퇴실 시간이 아닙니다. 퇴실하시겠습니까?")
                                        .setPositiveButton("확인") { _, _ ->
                                            RetrofitClient.api.updateStatistics(StatData(checkData.device_address, "early_leave")).enqueue(object : Callback<Any> {
                                                override fun onResponse(call: Call<Any>, response: Response<Any>) {}
                                                override fun onFailure(call: Call<Any>, t: Throwable) {}
                                            })

                                            sendAttendance("leave_early")
                                            addLog("조퇴 처리")
                                        }
                                        .setNegativeButton("취소") { _, _ ->
                                            isProcessing = false
                                            startScan()
                                        }
                                        .show()
                                }
                            }
                            else{
                                RetrofitClient.api.updateStatistics(StatData(checkData.device_address, "attendance")).enqueue(object : Callback<Any> {
                                    override fun onResponse(call: Call<Any>, response: Response<Any>) {}
                                    override fun onFailure(call: Call<Any>, t: Throwable) {}
                                })

                                sendAttendance("checkout")
                                addLog("👋 퇴실 처리")
                            }
                            lastAttendanceTime = currentTime
                            getSharedPreferences("AttendanceTime", MODE_PRIVATE)
                                .edit().putLong("lastAttendanceTime", currentTime).apply()
                        }

                        // 시간이 지난뒤 정상적으로 스캔이된다면 실행
                        stopScan()
                        resumeHandler.postDelayed(resumeRunnable, ATTENDANCE_COOLDOWN)
                    }
                    override fun onFailure(call: Call<StatusResponse>, t: Throwable) {
                        addLog("❌ 서버 연결 실패: ${t.message}")
                        isProcessing = false
                    }
                })
            }
        }
    }

    private fun sendAttendance(action: String) {


        val logData = createLogData(this@MainActivity, action)
        RetrofitClient.api.saveAttendance(logData).enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.isSuccessful) {
                    addLog("✅ 서버 전송 성공: $action")
                } else {
                    addLog("⚠️ 서버 오류: ${response.code()}")
                }
            }
            override fun onFailure(call: Call<Any>, t: Throwable) {
                addLog("❌ 서버 연결 실패: ${t.message}")
            }
        })

        // 서버에서 값 출석 <-> 퇴실 바꾸는 api
        RetrofitClient.api.check(checkData).enqueue(object : Callback<Any> {
            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                if (response.isSuccessful) {

                }
            }
            override fun onFailure(call: Call<Any>, t: Throwable) {

            }
        })
    }
    private fun addLog(message: String) {
        runOnUiThread {
            val current = logText.text.toString()
            logText.text = "• $message\n$current"
        }
    }
}