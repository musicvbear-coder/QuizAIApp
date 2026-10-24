package com.example.quizaiapp.data.presentation.auth

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quizaiapp.R
import com.example.quizaiapp.data.local.KetNoiCoSoDuLieu
import com.example.quizaiapp.data.local.entity.NguoiDung
import com.example.quizaiapp.data.repository.FirebaseRepository
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private val firebaseRepo by lazy { FirebaseRepository() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val edtFullName = findViewById<TextInputEditText>(R.id.edtFullName)
        val edtRegEmail = findViewById<TextInputEditText>(R.id.edtRegEmail)
        val edtRegPassword = findViewById<TextInputEditText>(R.id.edtRegPassword)
        val edtConfirmPassword = findViewById<TextInputEditText>(R.id.edtConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLogin)

        val nguoiDungDao = KetNoiCoSoDuLieu.layInstance(applicationContext).nguoiDungDao()

        btnRegister.setOnClickListener {
            val hoTen = edtFullName.text.toString().trim()
            val email = edtRegEmail.text.toString().trim()
            val matKhau = edtRegPassword.text.toString().trim()
            val xacNhanMatKhau = edtConfirmPassword.text.toString().trim()

            if (hoTen.isEmpty() || email.isEmpty() || matKhau.isEmpty() || xacNhanMatKhau.isEmpty()) {
                Toast.makeText(this, "Vui lòng điền đầy đủ tất cả các trường!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (matKhau != xacNhanMatKhau) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                Toast.makeText(this@RegisterActivity, "Đang khởi tạo tài khoản Cloud...", Toast.LENGTH_SHORT).show()

                val fbResult = firebaseRepo.dangKyFirebase(hoTen, email, matKhau)

                val isEmailExist = nguoiDungDao.kiemTraEmailDaTonTai(email)
                if (!isEmailExist) {
                    val nguoiDungMoi = NguoiDung(
                        hoTen = hoTen,
                        email = email,
                        matKhau = matKhau
                    )
                    nguoiDungDao.dangKyTaiKhoan(nguoiDungMoi)
                }

                if (fbResult.isSuccess) {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công tài khoản Firebase Cloud!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công (Lưu cục bộ): ${fbResult.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
                finish()
            }
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }
}