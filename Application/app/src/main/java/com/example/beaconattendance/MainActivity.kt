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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        logText = findViewById(R.id.logText)
        checkData = Check(device_address = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ANDROID_ID))

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
                val timeSinceLast = currentTime - lastAttendanceTime

                if (lastAttendanceTime != 0L && timeSinceLast < ATTENDANCE_COOLDOWN) {
                    isProcessing = false
                    return
                }

                RetrofitClient.api.getStatus(checkData).enqueue(object : Callback<StatusResponse> {
                    override fun onResponse(call: Call<StatusResponse>, response: Response<StatusResponse>) {
                        val status = response.body()?.status
                        if (status == 0) {
                            // 퇴실 상태 → 출석 처리
                            sendAttendance("attendance")

                            lastAttendanceTime = currentTime

                            //마지막 시간으로 저장
                            getSharedPreferences("AttendanceTime", MODE_PRIVATE)
                                .edit().putLong("lastAttendanceTime", lastAttendanceTime).apply()

                            addLog("✅ 출석 처리")
                        } else {
                            // 출석 상태 → 퇴실 처리
                            sendAttendance("checkout")

                            lastAttendanceTime = currentTime  // 0L이 아니라 현재 시간 저장!
                            getSharedPreferences("AttendanceTime", MODE_PRIVATE)
                                .edit().putLong("lastAttendanceTime", currentTime).apply()

                            addLog("👋 퇴실 처리")
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