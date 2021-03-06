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
import android.util.Log
import android.view.View
import android.widget.Toast
import com.huawei.agconnect.auth.*
import com.huawei.hmf.tasks.Task
import kotlinx.android.synthetic.main.activity_email_login.*
import kotlinx.android.synthetic.main.bottom_info.*
import java.util.*

//author Ivantsov Alexey
class EmailActivity : BaseActivity() {

    private var verifyCode: String? = ""
    private var email: String? = ""

    /**
     * Переменная для внутренней логики - для демо
     * Если True - AGConnectAuthCredential будет сформирован с паролем
     * Если False - AGConnectAuthCredential будет сформирован с кодом верификации, пароль не требуется
     * */
    private val credentialType: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_login)

        btnEmailCode.setOnClickListener {
            email = editTextEmail.text.toString()

            if (email.isNullOrEmpty()) {
                Toast.makeText(
                    this@EmailActivity,
                    "Please put the phone number",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            requestVerificationCode()
        }

        btnEmailOk.setOnClickListener {
            verifyCode = editTextVerificationCode.text.toString()

            if (verifyCode.isNullOrEmpty()) {
                Toast.makeText(
                    this@EmailActivity,
                    "Please put the verification code",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }
            /**
             *  После создания учетной записи пользователь входит в систему со сформированным credential.
             **/
            signInToAppGalleryConnect()
        }

        btnCreateUserInAg.setOnClickListener {
            createUserInAppGallery()
        }

        btnEmailLogout.setOnClickListener {
            logout()
        }
    }

    private fun requestVerificationCode() {
        editTextEmail.isEnabled = false

        val settings = VerifyCodeSettings.newBuilder()
            .action(VerifyCodeSettings.ACTION_REGISTER_LOGIN) //ACTION_REGISTER_LOGIN o ACTION_RESET_PASSWORD
            /**
             *  Минимальный интервал отправки, значения от 30 с до 120 с.
             *  */
            .sendInterval(30)
            /**
             *  Необязательный параметр. Указывает язык для отправки кода подтверждения.
             *  Значение должно содержать информацию о языке и стране / регионе.
             *  Значением по умолчанию является Locale.getDefault.
             *  */
            .locale(
                Locale(
                    "ru",
                    "RU"
                )
            ).build()

        /**
         * Запрос на код подтверждения.
         * Код подтверждения будет отправлен на указанный электронный почтовый яшик
         */
        val task: Task<VerifyCodeResult> =
            EmailAuthProvider.requestVerifyCode(
                email,
                settings
            )
        /**
         * Запрос на код подтверждения отправлен успешно.
         * */
        task.addOnSuccessListener {
            llCodeInput.visibility = View.VISIBLE
            Toast.makeText(
                this@EmailActivity,
                "Please wait verification code, and then type it and press OK",
                Toast.LENGTH_LONG
            ).show()
        }.addOnFailureListener { e: Exception ->
            Log.e(TAG, e.message.toString())
            val message = checkError(e)
            Toast.makeText(
                this@EmailActivity,
                message,
                Toast.LENGTH_LONG
            ).show()
            results.text = message
        }
    }

    private fun createUserInAppGallery() {
        /**
         * Регистрирация аккаунта в AppGallery Connect, используя e-mail.
         * */
        @Suppress("ConstantConditionIf")
        val emailUser = if (credentialType) {
            EmailUser.Builder()
                .setEmail(email)
                .setVerifyCode(verifyCode)
                /**
                 * Обязательно.
                 * Если этот параметр установлен, по умолчанию для текущего пользователя должен быть создан пароль,
                 * и в дальнейшем пользователь может войти в систему с помощью пароля.
                 * В противном случае пользователь может войти в систему только с помощью кода подтверждения.
                 */
                .setPassword("password")
                .build()
        } else {
            EmailUser.Builder()
                .setEmail(email)
                .setVerifyCode(verifyCode)
                .build()
        }
        AGConnectAuth.getInstance().createUser(emailUser)
            .addOnSuccessListener {
                /**
                 *  После создания учетной записи пользователь входит в систему
                 **/
                signInToAppGalleryConnect()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, e.message.toString())
                val message = checkError(e)
                Toast.makeText(
                    this@EmailActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
                results.text = message
            }
    }

    //TODO error in documentation credentialWithPassowrd
    private fun signInToAppGalleryConnect() {
        /** Формируем AGConnectAuthCredential */
        @Suppress("ConstantConditionIf")
        val credential: AGConnectAuthCredential = if (credentialType) {
            /** С паролем */
            EmailAuthProvider.credentialWithPassword(
                email,
                "password"//TODO() we need request password from user...
            )
        } else {
            /** с кодом верификации, пароль опционален*/
            EmailAuthProvider.credentialWithVerifyCode(
                email,
                /** пароль опционален*/
                "",
                verifyCode
            )
        }
        /**Осуществляем вход.*/
        AGConnectAuth.getInstance().signIn(credential)
            .addOnSuccessListener { signInResult: SignInResult ->
                llCodeInput.visibility = View.GONE
                val user = signInResult.user
                Toast.makeText(this@EmailActivity, user.uid, Toast.LENGTH_LONG).show()
                results.text = getUserInfo(user, avatarView)
                btnEmailCode.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, e.message.toString())
                val message = checkError(e)
                Toast.makeText(
                    this@EmailActivity,
                    message,
                    Toast.LENGTH_LONG
                ).show()
                results.text = message
                /** Если получаем ошибку AGCAuthException.USER_NOT_REGISTERED,
                 * то начинаем регистрацию пользователя в AGC
                 * */
                if (e is AGCAuthException && e.code == AGCAuthException.USER_NOT_REGISTERED) {
                    btnCreateUserInAg.visibility = View.VISIBLE
                }
            }
    }

    override fun onResume() {
        super.onResume()
        /** Проверяем наличие текущего уже авторизированного пользователя*/
        if (AGConnectAuth.getInstance().currentUser != null){
            results.text = getUserInfo(AGConnectAuth.getInstance().currentUser, avatarView)
            editTextEmail.visibility = View.GONE
            btnEmailCode.visibility = View.GONE
            llCodeInput.visibility = View.GONE
            btnCreateUserInAg.visibility = View.GONE
            btnEmailLogout.visibility = View.VISIBLE
        }
    }

    override fun logout() {
        super.logout()
        editTextEmail.apply {
            isEnabled = true
            visibility = View.VISIBLE
        }
        btnEmailCode.visibility = View.VISIBLE
        llCodeInput.visibility = View.GONE
        btnCreateUserInAg.visibility = View.GONE
        btnEmailLogout.visibility = View.GONE
    }

    companion object {
        private const val TAG = "EmailActivity"
    }
}