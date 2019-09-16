package xin.monus.checkit.login

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.*
import xin.monus.checkit.R
import xin.monus.checkit.data.entity.User
import xin.monus.checkit.inbox.InboxActivity
import xin.monus.checkit.network.API
import xin.monus.checkit.network.api.NetWorkApi
import xin.monus.checkit.util.Injection

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameTxt: EditText
    private lateinit var passwordTxt: EditText
    private lateinit var progressCircle: ProgressBar

    val TAG = "FIRST_LOGIN"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        checkAutoLogin()
        setupUI()
    }

    private fun checkAutoLogin() {
        val preferences = getSharedPreferences("login", Context.MODE_PRIVATE)
        val firstLogin = preferences.getBoolean(TAG, true)
        if (!firstLogin) {
            UserProfile.isLogin = true
            val intent = Intent(this@LoginActivity, InboxActivity::class.java)
            startActivity(intent)
            finish()
        }
    }


    private fun setupUI() {
        usernameTxt = findViewById(R.id.username)
        passwordTxt = findViewById(R.id.password)
        progressCircle = findViewById(R.id.login_progress)


        btn_login.setOnClickListener {
            checkLogin()
        }

        btn_register.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
        }

        btn_nologin.setOnClickListener {
            alert(R.string.confirm_direct_use) {
                yesButton {
                    UserProfile.generateDefaultUser(this@LoginActivity)
                    updateAutoLogin()
                    val intent = Intent(this@LoginActivity, InboxActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                noButton {
                    println("quit login")
                }
            }.show()
        }
    }


    private fun checkLogin() {
        usernameTxt.error = null
        passwordTxt.error = null

        if (usernameTxt.text.isEmpty()) {
            usernameTxt.error = getString(R.string.no_null_username)
            return
        }
        if (passwordTxt.text.isEmpty()) {
            passwordTxt.error = getString(R.string.no_null_password)
            return
        }

        progressCircle.visibility = View.VISIBLE

        val username = usernameTxt.text.toString()
        val password = passwordTxt.text.toString()

        val inboxRepository = Injection.getInboxItemRepository(this)
        if (inboxRepository.cachedInboxItems.isNotEmpty()) {
            inboxRepository.cachedInboxItems.clear()
        }

        doAsync {
            API.checkLogin(username, password, object: NetWorkApi.UserCallback {
                override fun success(user: User) {
                    uiThread {
                        println("login success")
                        UserProfile.saveUser(user, this@LoginActivity)
                        progressCircle.visibility = View.GONE
                        updateAutoLogin()
                        val intent = Intent(this@LoginActivity, InboxActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                }

                override fun fail(msg: String) {
                    uiThread {
                        println("login fail")
                        progressCircle.visibility = View.GONE
                        Toast.makeText(this@LoginActivity, R.string.login_error, Toast.LENGTH_SHORT).show()
                    }
                }

            })
        }
    }

    private fun updateAutoLogin() {
        UserProfile.isLogin = true
        val preferences = getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean(TAG, false)
        editor.apply()
    }

}
