package com.example.beaconattendance

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init()
        setContentView(R.layout.activity_login)

        val prefs = getSharedPreferences("login", MODE_PRIVATE)
        if (prefs.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        val editId = findViewById<EditText>(R.id.editId)
        val editPassword = findViewById<EditText>(R.id.editPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val checkAutoLogin = findViewById<CheckBox>(R.id.checkAutoLogin)



        // 로그인 버튼
        btnLogin.setOnClickListener {
            val id = editId.text.toString()
            val pw = editPassword.text.toString()

            if (id.isEmpty() || pw.isEmpty()) {
                Toast.makeText(this, "아이디와 비밀번호를 입력해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val loginData = LoginData(id = id, password = pw)

            // 로그 보내는 로직


            // 로그인 확인하는 로직
            RetrofitClient.api.login(loginData).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    val result = response.body()  // ← JSON 응답 받기

                    if (result?.success == true) {  // ← success 값 확인
                        // 로그인이 되었음을 로그로 보내는 로직
                        val logData = createLogData(this@LoginActivity, "LogIn")

                        RetrofitClient.api.saveAttendance(logData).enqueue(object : Callback<Any> {
                            override fun onResponse(call: Call<Any>, response: Response<Any>) {
                                // 성공해도 딱히 처리 안 해도 됨
                            }
                            override fun onFailure(call: Call<Any>, t: Throwable) {
                                // 실패해도 딱히 처리 안 해도 됨
                            }
                        })

                        if (checkAutoLogin.isChecked) {
                            prefs.edit().putBoolean("isLoggedIn", true).apply()
                        }
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "아이디 또는 비밀번호가 틀렸습니다", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@LoginActivity, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                }
            })
        }

        // 회원가입 버튼
        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}