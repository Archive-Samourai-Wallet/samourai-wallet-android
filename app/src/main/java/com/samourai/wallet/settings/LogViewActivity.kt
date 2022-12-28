package com.samourai.wallet.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import com.samourai.wallet.ExceptionReportHandler.Companion.LOG_FILE_NAME
import com.samourai.wallet.R
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.theme.samouraiSlateGreyAccent
import com.samourai.wallet.theme.samouraiSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class LogViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamouraiWalletTheme() {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    LogView(this)
                }
            }
        }
    }
}

@Composable
fun LogView(logViewActivity: LogViewActivity?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var logs by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val verticalScroll = rememberScrollState(0)
    val horizontalScroll = rememberScrollState(0)
    val clipboard = LocalClipboardManager.current
    val font = FontFamily(
        Font(R.font.roboto_mono)
    )
    LaunchedEffect(Unit) {
        scope.launch {
            loading = true
            withContext(Dispatchers.IO) {
                val logFile = File(context.filesDir, LOG_FILE_NAME)
                if (logFile.exists()) {
                    logs = logFile.readText()
                }
                loading = false
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = samouraiSlateGreyAccent,
                navigationIcon = {
                    IconButton(onClick = {
                        logViewActivity?.onBackPressed()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(text = logs))
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_content_copy_24),
                            tint = Color.White,
                            contentDescription = "copy"
                        )
                    }
                    IconButton(onClick = {
                        val filePath = context.externalCacheDir.toString() + File.separator + "samourai_report.log"
                        val file = File(filePath)
                        if (!file.exists()) {
                            try {
                                file.createNewFile()
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        try {
                            file.writeText(logs)
                            file.setReadable(true, false)
                            val intent = Intent()
                            intent.action = Intent.ACTION_SEND
                            intent.type = "text/plain"
                            if (Build.VERSION.SDK_INT >= 24) {
                                //From API 24 sending FIle on intent ,require custom file provider
                                intent.putExtra(
                                    Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                        context,
                                        context
                                            .packageName + ".provider", file
                                    )
                                )
                            } else {
                                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file))
                            }
                            startActivity(context, Intent.createChooser(intent, "Samourai Bug Report"), null);
                        } catch (ex: Exception) {
                            Toast.makeText(context, ex.message, Toast.LENGTH_SHORT).show()
                        }

                    }) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                    }
                },
                title = {
                    Text(
                        "Logs",
                        color = Color.White
                    )
                }
            )
        },
    ) {
        val customTextSelectionColors = TextSelectionColors(
            handleColor = samouraiSuccess,
            backgroundColor = samouraiSuccess.copy(alpha = 0.4f)
        )
        if (logs.isNotEmpty())
            CompositionLocalProvider(
                LocalTextSelectionColors provides customTextSelectionColors
            ) {
                SelectionContainer(Modifier.background(Color.Black)) {
                    Text(
                        logs,
                        modifier = Modifier
                            .verticalScroll(verticalScroll)
                            .horizontalScroll(horizontalScroll)
                            .padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.body1.copy(
                            fontSize = 10.sp,

                            fontFamily = font,
                        )
                    )
                }
            }
        if (loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color.White
                )
            }
        }
    }
}


@Preview
@Composable
fun LogViewPreview() {
    SamouraiWalletTheme() {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            LogView(null)
        }
    }
}