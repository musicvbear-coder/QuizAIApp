package com.example.quizaiapp.data.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.local.SessionManager
import com.example.quizaiapp.data.presentation.main.MainActivity
import com.example.quizaiapp.data.repository.FirebaseRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private val firebaseRepo by lazy { FirebaseRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionManager = SessionManager(applicationContext)
        if (sessionManager.isDaDangNhap()) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val edtEmail = findViewById<TextInputEditText>(R.id.edtEmail)
        val edtPassword = findViewById<TextInputEditText>(R.id.edtPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)

        val nguoiDungDao = KetNoiCoSoDuLieu.layInstance(applicationContext).nguoiDungDao()

        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin tài khoản!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                Toast.makeText(this@LoginActivity, "Đang xác thực...", Toast.LENGTH_SHORT).show()

                val fbResult = firebaseRepo.dangNhapFirebase(email, password)
                if (fbResult.isSuccess) {
                    val (uid, hoTen) = fbResult.getOrThrow()
                    sessionManager.taoPhienDangNhap(uid.hashCode(), hoTen, email)
                    Toast.makeText(this@LoginActivity, "Đăng nhập Cloud thành công!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@launch
                }

                val user = nguoiDungDao.dangNhap(email, password)
                if (user != null) {
                    sessionManager.taoPhienDangNhap(user.id, user.hoTen, user.email)
                    Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Email hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}