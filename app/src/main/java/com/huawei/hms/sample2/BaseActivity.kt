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
package com.huawei.hms.sample2

import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import coil.clear
import coil.load
import com.huawei.agconnect.auth.AGCAuthException
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectUser
import kotlinx.android.synthetic.main.bottom_info.*
import kotlinx.android.synthetic.main.buttons_lll.*

open class BaseActivity : AppCompatActivity() {

    fun getUserInfo(user: AGConnectUser, avatar: ImageView? = null): String {
        val info = getString(
                R.string.user_info_formatted_string,
                user.displayName,
                user.uid,
                user.email,
                user.emailVerified,
                user.isAnonymous,
                user.passwordSetted,
                user.phone,
                providersMap[user.providerId?.toInt()],
                user.providerId,
                user.providerInfo?.toString())

        avatar?.load(user.photoUrl) { crossfade(true) }
        return info
    }

    fun isProviderLinked(user: AGConnectUser?, providerId: Int): Boolean {
        for (provider in user?.providerInfo!!) {
            if (provider.containsKey("provider") && provider.getValue("provider").toInt() == providerId) {
                return true
            }
        }
        return false
    }

    fun getAGConnectUser(): AGConnectUser? {
        return AGConnectAuth.getInstance().currentUser
    }

    fun checkError(exception: Exception): String {
        return if (exception is AGCAuthException) {
            when (exception.code) {
                AGCAuthException.INVALID_PHONE -> getString(R.string.invalid_phone)
                AGCAuthException.PASSWORD_VERIFICATION_CODE_OVER_LIMIT -> getString(R.string.password_verification_code_over_limit)
                AGCAuthException.PASSWORD_VERIFY_CODE_ERROR -> getString(R.string.password_verify_code_error)
                AGCAuthException.VERIFY_CODE_ERROR -> getString(R.string.verify_code_error)
                AGCAuthException.VERIFY_CODE_FORMAT_ERROR -> getString(R.string.verify_code_format_error)
                AGCAuthException.VERIFY_CODE_AND_PASSWORD_BOTH_NULL -> getString(R.string.verify_code_and_password_both_null)
                AGCAuthException.VERIFY_CODE_EMPTY -> getString(R.string.verify_code_empty)
                AGCAuthException.VERIFY_CODE_LANGUAGE_EMPTY -> getString(R.string.verify_code_language_empty)
                AGCAuthException.VERIFY_CODE_RECEIVER_EMPTY -> getString(R.string.verify_code_receiver_empty)
                AGCAuthException.VERIFY_CODE_ACTION_ERROR -> getString(R.string.verify_code_action_error)
                AGCAuthException.VERIFY_CODE_TIME_LIMIT -> getString(R.string.verify_code_time_limit)
                AGCAuthException.ACCOUNT_PASSWORD_SAME -> getString(R.string.account_password_same)
                AGCAuthException.USER_HAVE_BEEN_REGISTERED -> getString(R.string.user_have_been_registered)
                AGCAuthException.PROVIDER_USER_HAVE_BEEN_LINKED -> getString(R.string.provider_user_have_been_linked)
                AGCAuthException.USER_NOT_REGISTERED -> getString(R.string.user_not_registered)
                AGCAuthException.PROVIDER_HAVE_LINKED_ONE_USER -> getString(R.string.provider_have_linked_one_user)
                AGCAuthException.CANNOT_UNLINK_ONE_PROVIDER_USER -> getString(R.string.cannot_unlink_one_provider_user)
                AGCAuthException.AUTH_METHOD_IS_DISABLED -> getString(R.string.auth_method_is_disabled)
                AGCAuthException.FAIL_TO_GET_THIRD_USER_INFO -> getString(R.string.fail_to_get_third_user_info)
                else -> exception.localizedMessage
            }
        } else {
            exception.localizedMessage
        }
    }

    open fun logout() {
        if (AGConnectAuth.getInstance().currentUser != null) {
            AGConnectAuth.getInstance().signOut()
        }
        results.text = ""
        avatarView.clear()
    }

    fun getUserInfoAndSwitchUI(providerId: Int) {
        /** Проверяем наличие текущего уже авторизированного пользователя*/
        if (getAGConnectUser() != null) {
            /** Выводим инфу о пользователе*/
            results.text = getUserInfo(AGConnectAuth.getInstance().currentUser, avatarView)
            /** проверяем кол-во привязанных провайдеров*/
            if (getAGConnectUser()?.providerInfo != null && getAGConnectUser()!!.providerInfo!!.size > 1
                    /** Если один из них = providerId*/
                    && isProviderLinked(getAGConnectUser(), providerId)
            ) {
                /** то меняем текст кнопки*/
                buttonLogin.visibility = View.GONE
                btnLogout.visibility = View.VISIBLE
                buttonLinkage.apply {
                    text = getString(R.string.unlink)
                    visibility = View.VISIBLE
                }
            }
            /** Если у нас всего один провайдер и он = providerId*/
            else if (getAGConnectUser()?.providerInfo != null && getAGConnectUser()!!.providerInfo!!.size == 1
                    && isProviderLinked(getAGConnectUser(), providerId)
            ) {
                /** Скрываем кнопку Login & LinkUnlink*/
                buttonLogin.visibility = View.GONE
                btnLogout.apply {
                    visibility = View.VISIBLE
                    text = getString(R.string.logout)
                }
                buttonLinkage.visibility = View.GONE
            } else {
                /** Стандартный режим для Link/Unlink*/
                buttonLogin.visibility = View.GONE
                btnLogout.visibility = View.VISIBLE
                buttonLinkage.apply {
                    text = getString(R.string.link)
                    visibility = View.VISIBLE
                }
            }
        } else {
            /** Стандартный режим для Login*/
            buttonLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
            buttonLinkage.apply {
                text = getString(R.string.link)
                visibility = View.GONE
            }
        }
    }

    private val providersMap = mapOf(
            0 to "Anonymous_Provider",
            1 to "Huawei_Provider",
            2 to "Facebook_Provider",
            3 to "Twitter_Provider",
            4 to "WeiXin_Provider",
            5 to "Huawei_Game_Provider",
            6 to "QQ_Provider",
            7 to "WeiBo_Provider",
            8 to "Google_Provider",
            9 to "GoogleGame_Provider",
            10 to "SelfBuild_Provider",
            11 to "Phone_Provider",
            12 to "Email_Provider"
    )
}