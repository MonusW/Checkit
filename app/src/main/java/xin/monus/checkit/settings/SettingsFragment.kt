package xin.monus.checkit.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import org.jetbrains.anko.*
import xin.monus.checkit.R
import xin.monus.checkit.data.entity.User
import xin.monus.checkit.db.LocalDbHelper
import xin.monus.checkit.login.LoginActivity
import xin.monus.checkit.login.UserProfile
import xin.monus.checkit.network.API
import xin.monus.checkit.network.api.NetWorkApi


class SettingsFragment: Fragment(), SettingsContract.View {

    override lateinit var presenter: SettingsContract.Presenter

    private lateinit var userMessage : User
    private lateinit var nicknameEdit : EditText
    private lateinit var heightEdit : EditText
    private lateinit var weightEdit : EditText
    private lateinit var calEdit: EditText
    private lateinit var modifyBtn : Button
    private lateinit var logoutBtn : Button

    private lateinit var nicknameLabel: TextView
    private lateinit var usernameLabel: TextView
    private lateinit var syncBtn: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.activity_settings_frag, container, false)

        with(root) {
            userMessage = UserProfile.getUser(requireActivity())
            nicknameLabel = findViewById(R.id.nickname_label)
            usernameLabel = findViewById(R.id.username_label)
            nicknameEdit = findViewById(R.id.nickname_edit)
            heightEdit = findViewById(R.id.height_edit)
            weightEdit = findViewById(R.id.weight_edit)
            calEdit  = findViewById(R.id.cal_edit)
            modifyBtn = findViewById(R.id.btn_confirm_modify)
            logoutBtn = findViewById(R.id.btn_logout)
            syncBtn = findViewById(R.id.btn_sync)
        }

        nicknameLabel.text = userMessage.nickname
        usernameLabel.text = userMessage.username
        nicknameEdit.setText(userMessage.nickname)
        heightEdit.setText(userMessage.height.toString())
        weightEdit.setText(userMessage.weight.toString())
        calEdit.setText(userMessage.daily_calorie.toString())

        modifyBtn.setOnClickListener {
            checkModify()
        }

        logoutBtn.setOnClickListener {
            checkLogout()
        }

        syncBtn.setOnClickListener {
            if (UserProfile.isLogin) {
                syncFromServer()
            }
        }

        return root
    }

    private fun syncFromServer() {
        println("start sync")
        Toast.makeText(activity, getString(R.string.settings_start_sync), Toast.LENGTH_SHORT).show()
        doAsync {
            API.syncUserInfo(userMessage.username, requireActivity(), object : NetWorkApi.SyncResult {
                override fun success() {
                    println("sync user success")
                }
                override fun fail() {
                    println("sync user fail")
                }
            })
            API.syncInboxItems(userMessage.username, requireActivity(), object : NetWorkApi.SyncResult {
                override fun success() {
                    println("sync inbox item success")
                }
                override fun fail() {
                    println("sync inbox item failed")
                }
            })
            uiThread {
                Toast.makeText(activity, getString(R.string.settings_end_sync), Toast.LENGTH_SHORT).show()
                println("sync success")
            }
        }

    }

    private fun checkLogout() {
        context!!.alert(R.string.settings_msg_logout) {
            yesButton {
                clearData()
                val intent = Intent(activity, LoginActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }
            noButton {
                println("cancel logout")
            }
        }.show()
    }

    private fun clearData() {
        val preferences = activity!!.getSharedPreferences("login", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putBoolean("FIRST_LOGIN", true)
        editor.apply()
        UserProfile.deleteUser(requireActivity())
        LocalDbHelper.deleteDatabase(requireActivity())
    }

    private fun checkModify() {
        if (nicknameEdit.text.isEmpty() || heightEdit.text.isEmpty() ||
                weightEdit.text.isEmpty() || calEdit.text.isEmpty()) {
            Toast.makeText(activity, getString(R.string.settings_incorrect_input), Toast.LENGTH_SHORT).show()
            return
        }

        userMessage.nickname = nicknameEdit.text.toString()
        userMessage.height = heightEdit.text.toString().toDouble()
        userMessage.weight = weightEdit.text.toString().toDouble()
        userMessage.daily_calorie = calEdit.text.toString().toDouble()

        if (UserProfile.update(userMessage, requireActivity())) {
            nicknameLabel.text = userMessage.nickname
            Toast.makeText(activity, getString(R.string.settings_edit_success), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, getString(R.string.settings_edit_failed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        println("settings fragment resume")
        presenter.start()
    }

    companion object {
        fun newInstance() = SettingsFragment()
    }
}