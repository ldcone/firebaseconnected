package org.tensorflow.lite.examples.posenet

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.tensorflow.lite.examples.posenet.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private var _binding : ActivitySignupBinding? = null
    private val binding get() =_binding!!
    private lateinit var auth : FirebaseAuth

    var imm : InputMethodManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as
                InputMethodManager?

        auth = FirebaseAuth.getInstance()

        var check : Int = 0

        // 뒤로가기 버튼
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        //
        if(check == 0) {
            binding.btnUnvisibleSignup.visibility = View.VISIBLE
            binding.btnVisibleSignup.visibility = View.INVISIBLE
        }
        else if(check == 1) {
            binding.btnVisibleSignup.visibility = View.VISIBLE
            binding.btnUnvisibleSignup.visibility = View.INVISIBLE
        }
        var edit1 = 0
        var edit2 = 0
        var edit3 = 0
        var edit4 = 0
        var edit5 = 0

        //이메일주소
        binding.editInputemail.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString() == "") {
                    edit1 = 0
                    check = 0
                    binding.btnUnvisibleSignup.visibility = View.VISIBLE
                    binding.btnVisibleSignup.visibility = View.INVISIBLE
                }
                else {
                    edit1 = 1
                    check = checkedittext(edit1, edit2, edit3, edit4, edit5)
                    //check가 0이면 회색 버튼으로 보여주고, 1이면 활성화하여 보여줌
                    if(check == 0) {
                        binding.btnUnvisibleSignup.visibility = View.VISIBLE
                        binding.btnVisibleSignup.visibility = View.INVISIBLE
                    }
                    if(check == 1) {
                        binding.btnVisibleSignup.visibility = View.VISIBLE
                        binding.btnUnvisibleSignup.visibility = View.INVISIBLE
                    }
                }
            }
        })

        //비밀번호
        binding.editInputpassword2.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString() == "") {
                    edit2 = 0
                    check = 0
                    binding.btnUnvisibleSignup.visibility = View.VISIBLE
                    binding.btnVisibleSignup.visibility = View.INVISIBLE
                }
                else {
                    edit2 = 1
                    check = checkedittext(edit1, edit2, edit3, edit4, edit5)
                    //check가 0이면 회색 버튼으로 보여주고, 1이면 활성화하여 보여줌
                    if(check == 0) {
                        binding.btnUnvisibleSignup.visibility = View.VISIBLE
                        binding.btnVisibleSignup.visibility = View.INVISIBLE
                    }
                    if(check == 1) {
                        binding.btnVisibleSignup.visibility = View.VISIBLE
                        binding.btnUnvisibleSignup.visibility = View.INVISIBLE
                    }
                }
            }
        })
        //비밀번호 재확인
        binding.editConfirmpassword.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString() == "") {
                    edit3 = 0
                    check = 0
                    binding.btnUnvisibleSignup.visibility = View.VISIBLE
                    binding.btnVisibleSignup.visibility = View.INVISIBLE
                }
                else {
                    edit3 = 1
                    check = checkedittext(edit1, edit2, edit3, edit4, edit5)
                    //check가 0이면 회색 버튼으로 보여주고, 1이면 활성화하여 보여줌
                    if(check == 0) {
                        binding.btnUnvisibleSignup.visibility = View.VISIBLE
                        binding.btnVisibleSignup.visibility = View.INVISIBLE
                    }
                    if(check == 1) {
                        binding.btnVisibleSignup.visibility = View.VISIBLE
                        binding.btnUnvisibleSignup.visibility = View.INVISIBLE
                    }
                }
            }
        })

        //이름
        binding.editInputname.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString() == "") {
                    edit4 = 0
                    check = 0
                    binding.btnUnvisibleSignup.visibility = View.VISIBLE
                    binding.btnVisibleSignup.visibility = View.INVISIBLE
                }
                else {
                    edit4 = 1
                    check = checkedittext(edit1, edit2, edit3, edit4, edit5)
                    //check가 0이면 회색 버튼으로 보여주고, 1이면 활성화하여 보여줌
                    if(check == 0) {
                        binding.btnUnvisibleSignup.visibility = View.VISIBLE
                        binding.btnVisibleSignup.visibility = View.INVISIBLE
                    }
                    if(check == 1) {
                        binding.btnVisibleSignup.visibility = View.VISIBLE
                        binding.btnUnvisibleSignup.visibility = View.INVISIBLE
                    }
                }
            }
        })

        //소속
        binding.editInputbelong.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                if(s.toString() == "") {
                    edit5 = 0
                    check = 0
                    binding.btnUnvisibleSignup.visibility = View.VISIBLE
                    binding.btnVisibleSignup.visibility = View.INVISIBLE
                }
                else {
                    edit5 = 1
                    check = checkedittext(edit1, edit2, edit3, edit4, edit5)
                    //check가 0이면 회색 버튼으로 보여주고, 1이면 활성화하여 보여줌
                    if(check == 0) {
                        binding.btnUnvisibleSignup.visibility = View.VISIBLE
                        binding.btnVisibleSignup.visibility = View.INVISIBLE
                    }
                    if(check == 1) {
                        binding.btnVisibleSignup.visibility = View.VISIBLE
                        binding.btnUnvisibleSignup.visibility = View.INVISIBLE
                    }
                }
            }
        })
        //회원가입 버튼
        binding.btnVisibleSignup.setOnClickListener {

            val email = binding.editInputemail.text.toString().trim()
            val password = binding.editInputpassword2.text.toString().trim()
            createUser(email,password)

        }
    }

    fun hideKeyboard(v:View){
        imm?.hideSoftInputFromWindow(v.windowToken,0)
    }

    private fun createUser(email: String, password: String){
        auth.createUserWithEmailAndPassword(email,password)
            .addOnCompleteListener{ task ->
                when {
                    task.isSuccessful -> {
                        Toast.makeText(this,"회원가입 성공 ",Toast.LENGTH_SHORT).show()
                        val user = auth.currentUser
                        updateUI(user)
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    task.isCanceled -> {
                        Toast.makeText(this,"회원가입 실패1", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                    task.isComplete -> {
                        Toast.makeText(this,"회원가입 실패2", Toast.LENGTH_SHORT).show()
                        updateUI(null)
                    }
                }
            }

    }

    private fun updateUI(user: FirebaseUser?){
        user?.let{
            Toast.makeText(this,"Email: ${user.email}\n uUid:${user.uid}",Toast.LENGTH_LONG).show()
        }
    }

    fun checkedittext(edit1: Int, edit2: Int, edit3: Int, edit4: Int, edit5: Int) : Int {
        return if((edit1 == 1) and (edit2 == 1) and (edit3== 1) and (edit4 == 1) and (edit5 == 1)) {
            1
        } else 0
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}