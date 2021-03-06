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

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.huawei.agconnect.auth.AGConnectAuth
import com.huawei.agconnect.auth.AGConnectAuthCredential
import com.huawei.agconnect.auth.HWGameAuthProvider
import com.huawei.hms.api.HuaweiMobileServicesUtil
import com.huawei.hms.jos.JosApps
import com.huawei.hms.jos.games.Games
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import com.huawei.hms.support.hwid.result.AuthHuaweiId
import com.huawei.hms.support.hwid.service.HuaweiIdAuthService
import kotlinx.android.synthetic.main.bottom_info.*
import kotlinx.android.synthetic.main.buttons_lll.*

class HuaweiGameIdActivity : BaseActivity() {

    private lateinit var mHuaweiIdAuthService: HuaweiIdAuthService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        HuaweiMobileServicesUtil.setApplication(application)
        val appsClient = JosApps.getJosAppsClient(this)
        appsClient.init()
        val authParams =
                HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM_GAME)
                        .createParams()
        mHuaweiIdAuthService =
                HuaweiIdAuthManager.getService(this@HuaweiGameIdActivity, authParams)

        buttonLogin.setOnClickListener {
            login()
        }

        buttonLinkage.setOnClickListener {
            link()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun login() {
        startActivityForResult(mHuaweiIdAuthService.signInIntent, HUAWEI_GAME_SIGN_IN)
    }

    private fun link() {
        if (!isProviderLinked(getAGConnectUser(), AGConnectAuthCredential.HWGame_Provider)) {
            startActivityForResult(mHuaweiIdAuthService.signInIntent, LINK_CODE)
        } else {
            unlink()
        }
    }

    override fun onResume() {
        super.onResume()
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
    }

    override fun logout() {
        super.logout()
        getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
    }

    private fun unlink() {
        if (AGConnectAuth.getInstance().currentUser != null) {
            AGConnectAuth.getInstance().currentUser
                    .unlink(AGConnectAuthCredential.HWGame_Provider)
                    .addOnSuccessListener { signInResult ->
                        val user = signInResult.user
                        Toast.makeText(this@HuaweiGameIdActivity, user.uid, Toast.LENGTH_LONG)
                                .show()
                        getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, e.message.toString())
                        val message = checkError(e)
                        Toast.makeText(
                                this@HuaweiGameIdActivity,
                                message,
                                Toast.LENGTH_LONG
                        ).show()
                        results.text = message
                    }
        }
    }

    override fun onActivityResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data == null) {
            Toast.makeText(
                    this@HuaweiGameIdActivity,
                    "Huawei Game Service Sign in Intent is null",
                    Toast.LENGTH_LONG
            ).show()
            return
        }
        val task = HuaweiIdAuthManager.parseAuthResultFromIntent(data)
        task.addOnSuccessListener { signInHuaweiId: AuthHuaweiId ->
            getHwGameUserInfo(signInHuaweiId, requestCode)
        }.addOnFailureListener { e ->
            Toast.makeText(
                    this@HuaweiGameIdActivity,
                    "HuaweiGameId signIn failed" + e.message,
                    Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun getHwGameUserInfo(signInHuaweiId: AuthHuaweiId, requestCode: Int) {
        val client = Games.getPlayersClient(this@HuaweiGameIdActivity, signInHuaweiId)
        val playerTask = client.currentPlayer
        playerTask.addOnSuccessListener { player ->
            val imageUrl =
                    if (player.hasHiResImage()) {
                        player.hiResImageUri.toString()
                    } else {
                        player.iconImageUri.toString()
                    }
            val credential = HWGameAuthProvider.Builder()
                    .setPlayerSign(player.playerSign)
                    .setPlayerId(player.playerId)
                    .setDisplayName(player.displayName)
                    .setImageUrl(imageUrl)
                    .setPlayerLevel(player.level)
                    .setSignTs(player.signTs)
                    .build()
            if (requestCode == HUAWEI_GAME_SIGN_IN) {
                if (getAGConnectUser() == null) {
                    AGConnectAuth.getInstance().signIn(credential)
                            .addOnSuccessListener { signInResult ->
                                val user = signInResult.user
                                Toast.makeText(
                                        this@HuaweiGameIdActivity,
                                        user.uid,
                                        Toast.LENGTH_LONG
                                ).show()
                                getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                                val message = checkError(e)
                                Toast.makeText(
                                        this@HuaweiGameIdActivity,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show()
                                results.text = message
                            }
                } else {
                    val user = getAGConnectUser()
                    Toast.makeText(this@HuaweiGameIdActivity, user?.uid, Toast.LENGTH_LONG)
                            .show()
                    getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
                }
            } else if (requestCode == LINK_CODE) {
                if (getAGConnectUser() != null) {
                    getAGConnectUser()!!.link(credential)
                            .addOnSuccessListener { signInResult ->
                                val user = signInResult.user
                                Toast.makeText(
                                        this@HuaweiGameIdActivity,
                                        user.uid,
                                        Toast.LENGTH_LONG
                                ).show()
                                getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
                            }.addOnFailureListener { e ->
                                e.printStackTrace()
                                val message = checkError(e)
                                Toast.makeText(
                                        this@HuaweiGameIdActivity,
                                        message,
                                        Toast.LENGTH_LONG
                                ).show()
                                results.text = message
                            }
                } else {
                    val user = getAGConnectUser()
                    Toast.makeText(this@HuaweiGameIdActivity, user?.uid, Toast.LENGTH_LONG).show()
                    getUserInfoAndSwitchUI(AGConnectAuthCredential.HWGame_Provider)
                }
            }
        }.addOnFailureListener { e ->
            e.printStackTrace()
            val message = checkError(e)
            Toast.makeText(
                    this@HuaweiGameIdActivity,
                    message,
                    Toast.LENGTH_LONG
            ).show()
            results.text = message
        }
    }

    companion object {
        private const val TAG = "HuaweiGameIdActivity"
        private const val HUAWEI_GAME_SIGN_IN = 7000
        private const val LINK_CODE = 7002
    }
}
