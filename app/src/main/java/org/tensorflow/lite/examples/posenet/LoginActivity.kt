package org.tensorflow.lite.examples.posenet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import org.tensorflow.lite.examples.posenet.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    //퍼미션 응답 처리 코드
    private val multiplePermissionsCode = 100

    // 필요한 퍼미션 리스트
    // 원하는 퍼미션을 이곳에 추가
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.INTERNET
    )

    private val RC_SIGN_IN = 9001

    //Firebase Auth
    private lateinit var firebaseAuth: FirebaseAuth

    // 뷰 바인딩 선언
    private lateinit var binding : ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        checkPermissions()

        binding.editInputid.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if(binding.editInputid.text.toString() == "" || binding.editInputpassword.text.toString() == ""){
                    binding.btnLogin.isClickable = false
                    binding.btnLogin.isEnabled = false
                }else{
                    binding.btnLogin.isClickable = true
                    binding.btnLogin.isEnabled = true
                }
            }
        })

        binding.editInputpassword.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if(binding.editInputid.text.toString() == "" || binding.editInputpassword.text.toString() == ""){
                    binding.btnLogin.isClickable = false
                    binding.btnLogin.isEnabled = false
                }else{
                    binding.btnLogin.isClickable = true
                    binding.btnLogin.isEnabled = true
                }

            }
        })



        firebaseAuth = FirebaseAuth.getInstance()

        if(binding.editInputid.text.toString() == "" && binding.editInputpassword.text.toString() == ""){
            binding.btnLogin.isClickable = false
            binding.btnLogin.isEnabled = false
        }else{
            binding.btnLogin.isClickable = true
            binding.btnLogin.isEnabled = true
        }

        binding.btnLogin.setOnClickListener {
           val email = binding.editInputid.text.toString().trim()
           val password = binding.editInputpassword.text.toString().trim()
            Log.d("email/password", "$email/$password")
            val intent = Intent(this, SelectActivity::class.java)
                        startActivity(intent)
                        finish()
            if(email == "1" && password == "1"){
                val intent = Intent(this, SelectActivity::class.java)
                startActivity(intent)
                finish()
            }

            firebaseAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(this) {
                    if(it.isSuccessful){
                        val user = firebaseAuth.currentUser
                        Toast.makeText(this,"Authentication success.",Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, SelectActivity::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        Toast.makeText(this,"Authentication failed",Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.btnSignup.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한을 저장할 문자열 배열 리스트
        val rejectedPermissionList = ArrayList<String> ()

        for(permission in requiredPermissions) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 만약 권한이 없다면 rejectedPermissionList에 추가
                rejectedPermissionList.add(permission)
            }
        }

        // 거절된 퍼미션이 있다면
        if(rejectedPermissionList.isNotEmpty()) {
            // 권한 요청
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array), multiplePermissionsCode)
        }
    }

    // 권한 요청 결과 함수
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode) {
            multiplePermissionsCode -> {
                if(grantResults.isNotEmpty()) {
                    for((i, permission) in permissions.withIndex()) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            // 권한 획득 실패
                            Log.i("TAG", "The user has denied to $permission")
                        }
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
    @Override
    override fun onResume(){
        super.onResume()

    }
}