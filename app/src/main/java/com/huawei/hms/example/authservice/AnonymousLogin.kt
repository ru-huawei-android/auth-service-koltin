/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.hms.example.authservice

import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.SignInResult
import kotlinx.android.synthetic.main.bottom_info.*
import kotlinx.android.synthetic.main.buttons_lll.*

class AnonymousLogin : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        buttonLinkage.visibility = View.GONE

        buttonLogin.setOnClickListener {
            AGConnectAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener { signInResult: SignInResult ->
                        val user = signInResult.user
                        results.text = getUserInfo(user, avatarView)
                    }
                    .addOnFailureListener { e: Exception? ->
                        Toast.makeText(
                                this@AnonymousLogin,
                                "Anonymous SignIn Failed",
                                Toast.LENGTH_LONG
                        ).show()
                        results.text = e?.localizedMessage
                    }
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    override fun onResume() {
        super.onResume()
        getUserInfoAndSwitchUI()
    }

    private fun getUserInfoAndSwitchUI() {
        /** Проверяем наличие текущего уже авторизированного пользователя*/
        getAGConnectUser()?.let {
            /** Выводим информацию о пользователе*/
            results.text = getUserInfo(getAGConnectUser()!!, avatarView)
            /** Скрываем кнопку Login & LinkUnlink*/
            buttonLogin.visibility = View.GONE
            btnLogout.visibility = View.VISIBLE
        } ?: run {
            /** Стандартный режим*/
            buttonLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
        }
    }
}