package com.example.beaconattendance

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        val btnBackToLogin = findViewById<Button>(R.id.btnBackToLogin)
        btnBackToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }


        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val editId = findViewById<EditText>(R.id.editId)
        val editPassword = findViewById<EditText>(R.id.editPassword)

        btnRegister.setOnClickListener{
            val id = editId.text.toString()
            val pw = editPassword.text.toString()

            // 비어있으면 막기
            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val phoneData = PhoneData(
                id = id,
                password = pw,
                device_name = android.os.Build.MODEL,
                device_address = android.provider.Settings.Secure.getString(
                    contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                )
            )

            val logData = createLogData(this@RegisterActivity,"SignIn")


            RetrofitClient.api.register(phoneData).enqueue(object : Callback<RegisterResponse> {
                override fun onResponse(call: Call<RegisterResponse>, response: Response<RegisterResponse>) {
                    when (response.body()?.success) {
                        1 -> {
                            RetrofitClient.api.saveAttendance(logData).enqueue(object : Callback<Any> {
                                override fun onResponse(call: Call<Any>, response: Response<Any>) {
                                }
                                override fun onFailure(call: Call<Any>, t: Throwable) {
                                }
                            })

                            Toast.makeText(this@RegisterActivity, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@RegisterActivity, LoginActivity::class.java))
                            finish()
                        }
                        2 -> Toast.makeText(this@RegisterActivity, "이미 등록된 기기입니다", Toast.LENGTH_SHORT).show()
                        3 -> Toast.makeText(this@RegisterActivity, "이미 존재하는 아이디입니다", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<RegisterResponse>, t: Throwable) {
                    Toast.makeText(this@RegisterActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            })



        }
    }
}