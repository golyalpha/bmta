package com.shielddagger.auth.oidc_debugger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.shielddagger.auth.oidc_debugger.oidc.ClientAuthType
import com.shielddagger.auth.oidc_debugger.oidc.OIDCCore
import com.shielddagger.auth.oidc_debugger.oidc.OIDCResponseType
import com.shielddagger.auth.oidc_debugger.ui.theme.OIDCDebuggerTheme
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OIDCDebuggerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = {
                                Text("Issuer Configuration")
                            }
                        )
                    },

                    ) { innerPadding ->
                    IssuerForm(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Composable
fun IssuerForm(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var client: OIDCCore? = null;

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

    var authorizeUrl by remember { mutableStateOf(client?.authorizeUri ?: "") }
    var tokenUrl by remember { mutableStateOf(client?.tokenUri ?: "") }
    var userinfoUrl by remember { mutableStateOf(client?.userinfoUri ?: "") }
    var clientId by remember { mutableStateOf(client?.clientId ?: "") }
    var clientSecret by remember { mutableStateOf(client?.clientSecret ?: "") }
    var scopes by remember { mutableStateOf(client?.scope ?: listOf("")) }

    Column (modifier = modifier
        .padding(10.dp)
        .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        TextField(authorizeUrl, { authorizeUrl = it },
            label = { Text("Authorize URL")},
            modifier = Modifier.fillMaxWidth())
        TextField(tokenUrl, { tokenUrl = it },
            label = { Text("Token URL")},
            modifier = Modifier.fillMaxWidth())
        TextField(userinfoUrl, { userinfoUrl = it },
            label = { Text("Userinfo URL")},
            modifier = Modifier.fillMaxWidth())
        TextField(clientId, { clientId = it },
            label = { Text("Client ID")},
            modifier = Modifier.fillMaxWidth())
        TextField(clientSecret, { clientSecret = it },
            label = { Text("Client Secret")},
            modifier = Modifier.fillMaxWidth())
        TextField(scopes.joinToString(" "), { scopes = it.split(" ")},
            label = { Text("Scopes")},
            modifier = Modifier.fillMaxWidth())
        Button({
            val oidcClient = OIDCCore(
                listOf(OIDCResponseType.CODE),
                scopes,
                clientId,
                context.resources.getString(R.string.redirect_uri),
                authorizeUrl,
                tokenUrl,
                userinfoUrl,
                clientSecret,
                ClientAuthType.POST
            )
            val authUri = oidcClient.beginAuth()
            Log.d("oidcdebugger", "IssuerForm: authUri: $authUri")

            val of = context.openFileOutput("issuerConfig", Context.MODE_PRIVATE)
            val bv = of.bufferedWriter()
            val data = Json.encodeToString(oidcClient)
            bv.write(data)
            bv.close()
            of.close()

            val intent = Intent(Intent.ACTION_VIEW, authUri)
            context.startActivity(intent)
        }) {
            Text("Authorize")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    OIDCDebuggerTheme {
        IssuerForm()
    }
}