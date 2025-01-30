package com.shielddagger.auth.oidc_debugger

import android.media.session.MediaSession.Token
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.volley.toolbox.Volley
import com.shielddagger.auth.oidc_debugger.oidc.OIDCCore
import com.shielddagger.auth.oidc_debugger.oidc.OIDCTokenErrorResponse
import com.shielddagger.auth.oidc_debugger.oidc.TokenResponse
import com.shielddagger.auth.oidc_debugger.ui.theme.OIDCDebuggerTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.json.JSONObject

class ValidateActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = intent?.data

        enableEdgeToEdge()
        setContent {
            OIDCDebuggerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {TopAppBar(
                        title = {
                            Text("Response Analysis")
                        }
                    )}
                ) { innerPadding ->
                    Analysis(
                        data = data,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun Analysis(data: Uri?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var client: OIDCCore? = null;

    val tokenExchange = remember {
        mutableStateOf(true)
    }
    val tokenData = remember {
        mutableStateOf(TokenResponse())
    }

    val userinfoExchange = remember {
        mutableStateOf(true)
    }
    val userinfoError = remember {
        mutableStateOf<String?>(null)
    }
    val userinfoData = remember {
        mutableStateOf(JSONObject())
    }

    if (context.fileList().contains("issuerConfig")) {
        val ifo = context.openFileInput("issuerConfig")
        try {
            client = Json.decodeFromStream<OIDCCore>(ifo);
        }
        catch (e: Throwable) {
            Log.e("persistence", "IssuerForm: Unable to parse issuerConfig, ignoring")
        }
        ifo.close()
    }

    Column (modifier = modifier
        .padding(10.dp)
        .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Authorization State",
                modifier = Modifier
                    .padding(16.dp),
                textAlign = TextAlign.Center,
            )
            if (client == null) {
                Text(
                    "Client Missing",
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp),
                    color = Color.Red
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }
            if (data == null) {
                Text(
                    "Data Missing",
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp),
                    color = Color.Red
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }

            client.validateAuthResponse(data).forEach {
                Text(
                    it.message,
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp),
                    color = if (it.success) Color(context.resources.getColor(R.color.success))
                            else Color.Red
                )
            }

            Spacer(Modifier.height(32.dp))

        }
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Token Exchange",
                modifier = Modifier
                    .padding(16.dp),
                textAlign = TextAlign.Center,
            )

            if (tokenExchange.value) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(32.dp, 0.dp)
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }

            if (tokenData.value.error != null) {
                Text(
                    text="Error: ${tokenData.value.error!!.message}",
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp),
                    color = Color.Red
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }
            Text(
                text = "Type: ${tokenData.value.tokenType.toString()}",
                modifier = Modifier
                    .padding(32.dp, 0.dp, 0.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Scopes: ${tokenData.value.scope?.joinToString(", ")}",
                modifier = Modifier
                    .padding(32.dp, 0.dp, 0.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Expires In: ${tokenData.value.expiresIn?.toString() ?: Int.MAX_VALUE}s",
                modifier = Modifier
                    .padding(32.dp, 0.dp, 0.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Refresh Token: ${tokenData.value.refreshToken != null}",
                modifier = Modifier
                    .padding(32.dp, 0.dp, 0.dp),
                textAlign = TextAlign.Center
            )
            Text(
                text = "ID Token: ${tokenData.value.idToken != null}",
                modifier = Modifier
                    .padding(32.dp, 0.dp, 0.dp),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
        }
        ElevatedCard(
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "User Info",
                modifier = Modifier
                    .padding(16.dp),
                textAlign = TextAlign.Center,
            )

            if (userinfoExchange.value) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(32.dp, 0.dp)
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }
            if (userinfoError.value != null){
                Text(
                    userinfoError.value!!,
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp),
                    color = Color.Red
                )
                Spacer(Modifier.height(32.dp))
                return@ElevatedCard
            }

            userinfoData.value.keys().forEach {
                Text(
                    "$it: ${userinfoData.value[it]}",
                    modifier = Modifier
                        .padding(32.dp, 0.dp, 0.dp)
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (client == null){
        tokenExchange.value = false
        tokenData.value = TokenResponse(OIDCTokenErrorResponse.INVALID_CLIENT)
        userinfoExchange.value = false
        return
    }

    val queue = Volley.newRequestQueue(context)

    val tokenRequest = client.getTokenFromCode(data!!, { tokenResponse ->
        val tokenResponseData = client.validateTokenResponse(tokenResponse)
        tokenData.value = tokenResponseData
        tokenExchange.value = false

        if (tokenResponseData.error != null) {
            return@getTokenFromCode
        }

        val userinfoRequest = client.getUserinfo(tokenData.value.accessToken!!, { userdataResponse ->
            userinfoData.value = userdataResponse
            userinfoExchange.value = false
            Log.d("oidcdebugger", "userinfo: ${userdataResponse.toString(4)}")
        }, {
            userinfoError.value = (it.message ?: "Unknown Error")
            userinfoExchange.value = false
            Log.d("oidcdebugger", it.message.toString())
        })
        queue.add(userinfoRequest)
    }, {
        tokenData.value = TokenResponse(OIDCTokenErrorResponse.INVALID_REQUEST)
        tokenExchange.value = false
        userinfoError.value = "Token Exchange Failed"
        userinfoExchange.value = false
    })
    queue.add(tokenRequest)


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview2() {
    OIDCDebuggerTheme {
        Analysis(null)
    }
}