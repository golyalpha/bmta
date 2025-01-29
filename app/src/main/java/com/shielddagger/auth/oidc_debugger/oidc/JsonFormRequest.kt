package com.shielddagger.auth.oidc_debugger.oidc

import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class JsonFormRequest(
    method: Int,
    url: String?,
    private var params: MutableMap<String, String>,
    listener: Response.Listener<JSONObject>?,
    errorListener: Response.ErrorListener?
) : JsonObjectRequest(method, url, null, listener, errorListener) {

    override fun getBodyContentType(): String {
        return "application/x-www-form-urlencoded; charset=UTF-8"
    }

    private fun encodeParameters(params: MutableMap<String, String>, paramsEncoding: String): ByteArray {
        val encodedParams = StringBuilder()
        try {
            for ((key, value) in params) {
                String.format(
                    "Request#getParams() or Request#getPostParams() returned a map "
                            + "containing a null key or value: (%s, %s). All keys "
                            + "and values must be non-null.",
                    key, value
                )
                encodedParams.append(URLEncoder.encode(key, paramsEncoding))
                encodedParams.append('=')
                encodedParams.append(URLEncoder.encode(value, paramsEncoding))
                encodedParams.append('&')
            }
            return encodedParams.toString().toByteArray(charset(paramsEncoding))
        } catch (uee: UnsupportedEncodingException) {
            throw RuntimeException("Encoding not supported: $paramsEncoding", uee)
        }
    }

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray? {
        if (params.isNotEmpty()) {
            return encodeParameters(params, paramsEncoding)
        }
        return null
    }
}