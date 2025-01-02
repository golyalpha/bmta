package com.shielddagger.auth.oidc_debugger.oidc

import android.net.Uri
import java.util.ArrayList
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

enum class ClientAuthType {
    BASIC,
    POST,
    JWT,
    NONE
}

enum class OIDCResponseType(private val type:String) {
    CODE("code"),
    TOKEN("token"),
    ID_TOKEN("id_token");

    override fun toString(): String {
        return this.type
    }
}

enum class OIDCPromptTypes(private val type:String){
    NONE(""),
    LOGIN("login"),
    CONSENT("consent"),
    SELECT_ACCOUNT("select_account");

    override fun toString(): String {
        return this.type
    }
}

enum class OIDCAuthState(private val type: String) {
    CODE_OK("code_ok"),
    CODE_FAIL("code_fail"),
    TOKEN_OK("token_ok"),
    TOKEN_FAIL("token_fail"),
    ID_TOKEN_OK("id_token_ok"),
    ID_TOKEN_FAIL("id_token_fail"),
    INVALID_STATE("invalid_state"),
    INTERACTION_REQUIRED("interaction_required"),
    LOGIN_REQUIRED("login_required"),
    ACCOUNT_SELECTION_REQUIRED("account_selection_required"),
    CONSENT_REQUIRED("consent_required"),
    INVALID_REQUEST_URI("invalid_request_uri"),
    INVALID_REQUEST_OBJECT("invalid_request_object"),
    REQUEST_NOT_SUPPORTED("request_not_supported"),
    REQUEST_URI_NOT_SUPPORTED("request_uri_not_supported"),
    REGISTRATION_NOT_SUPPORTED("registration_not_supported");

    override fun toString(): String {
        return this.type
    }

    companion object {
        fun fromString(type: String): OIDCAuthState {
            val map = OIDCAuthState.entries.associateBy(OIDCAuthState::type)
            return map[type]!!
        }
    }
}

class OIDCCore(
    private val responseType: List<OIDCResponseType>,
    private val scope: List<String>,
    private val clientId: String,
    private val redirectUri: String,
    private val authorizeUri: String,
    private val tokenUri: String,
    private val clientSecret: String = "",
    private val clientAuth: ClientAuthType = ClientAuthType.BASIC
) {

    private var nonce:String = ""
    private var state:String = ""

    @OptIn(ExperimentalEncodingApi::class)
    fun beginAuth(prompt:List<OIDCPromptTypes>? = null):Uri {
        nonce = Base64.encode(Random.nextBytes(32))
        state = Base64.encode(Random.nextBytes(8))

        val uri = Uri.parse(authorizeUri).buildUpon()
        uri.appendQueryParameter("scope", scope.joinToString(" ", "openid "))
        uri.appendQueryParameter("response_type", responseType.joinToString(" "))
        uri.appendQueryParameter("client_id", clientId)
        uri.appendQueryParameter("redirect_uri", redirectUri)
        uri.appendQueryParameter("state", state)
        uri.appendQueryParameter("nonce", nonce)
        if (prompt != null){
            uri.appendQueryParameter("prompt", prompt.joinToString(" "))
        }

        return uri.build()
    }

    fun validateAuthResponse(returnUri:Uri): List<OIDCAuthState>{
        if (returnUri.getQueryParameter("state") != state){
            return listOf(OIDCAuthState.INVALID_STATE)
        }

        if (returnUri.getQueryParameter("error") != null){
            return listOf(OIDCAuthState.fromString(returnUri.getQueryParameter("error")!!))
        }

        val stateList = ArrayList<OIDCAuthState>(3)

        if (responseType.contains(OIDCResponseType.CODE)) {
            if (returnUri.getQueryParameter("code") != null){
                stateList.add(OIDCAuthState.CODE_OK)
            } else {
                stateList.add(OIDCAuthState.CODE_FAIL)
            }
        }

        if (responseType.contains(OIDCResponseType.TOKEN)) {
            if (returnUri.getQueryParameter("token") != null){
                stateList.add(OIDCAuthState.TOKEN_OK)
            } else {
                stateList.add(OIDCAuthState.TOKEN_FAIL)
            }
        }

        if (responseType.contains(OIDCResponseType.ID_TOKEN)) {
            if (returnUri.getQueryParameter("id_token") != null){
                stateList.add(OIDCAuthState.ID_TOKEN_OK)
            } else {
                stateList.add(OIDCAuthState.ID_TOKEN_FAIL)
            }
        }

        return stateList
    }
}