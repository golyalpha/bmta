package com.shielddagger.auth.oidc_debugger.oidc

import android.net.Uri
import java.io.Serializable
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

enum class OIDCAuthState(private val type: String, message: String, success: Boolean) {
    CODE_OK("code_ok", "Authorization Code OK", true),
    CODE_FAIL("code_fail","Authorization Code FAIL", false),
    TOKEN_OK("token_ok", "Token OK", true),
    TOKEN_FAIL("token_fail", "Token FAIL", false),
    ID_TOKEN_OK("id_token_ok", "ID Token OK", true),
    ID_TOKEN_FAIL("id_token_fail", "ID Token FAIL", false),
    INVALID_STATE("invalid_state", "Invalid State Parameter", false),
    INTERACTION_REQUIRED("interaction_required", "IdP Interaction Required", false),
    LOGIN_REQUIRED("login_required", "IdP Login Required", false),
    ACCOUNT_SELECTION_REQUIRED("account_selection_required", "IdP Account Selection Required", false),
    CONSENT_REQUIRED("consent_required", "IdP Consent Required", false),
    INVALID_REQUEST_URI("invalid_request_uri", "Invalid Request URI", false),
    INVALID_REQUEST_OBJECT("invalid_request_object", "Invalid Request Object", false),
    REQUEST_NOT_SUPPORTED("request_not_supported", "Request Not Supported", false),
    REQUEST_URI_NOT_SUPPORTED("request_uri_not_supported", "Request URI Not Supported", false),
    REGISTRATION_NOT_SUPPORTED("registration_not_supported", "Registration Not Supported", false);

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
    val scope: List<String>,
    val clientId: String,
    val redirectUri: String,
    val authorizeUri: String,
    val tokenUri: String,
    val userinfoUri: String,
    val clientSecret: String = "",
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