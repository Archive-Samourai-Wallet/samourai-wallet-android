package com.samourai.wallet.stealth.qrscannerapp

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.samourai.wallet.BuildConfig
import com.samourai.wallet.R
import com.samourai.wallet.stealth.StealthModeController
import com.samourai.wallet.stealth.stealthTapListener
import kotlinx.coroutines.launch


class QRStealthAppViewModel : ViewModel() {
    fun add(text: String?) {
        if (text != null) {
            qrContent.postValue(arrayListOf<String>().apply {
                qrContent.value?.let { addAll(it) }
                add(text)
                reverse()
            })
        }
    }

    fun clearItem(item: String) {
        val values = qrContent.value ?: arrayListOf();
        qrContent.postValue(values.filter { it != item }.toList())
    }

    val permissionGranted = MutableLiveData(0)
    val qrContent = MutableLiveData<List<String>>(arrayListOf())
}

class QRStealthAppActivity : ComponentActivity() {
    private val vm by viewModels<QRStealthAppViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if(isGranted){
                    vm.permissionGranted.postValue(1)
                }else{
                    if (VERSION.SDK_INT > VERSION_CODES.M && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        vm.permissionGranted.postValue(-1)
                    } else {
                        vm.permissionGranted.postValue(0)
                    }
                }

            }
        setContent {
            samouraiStealthAppScanner {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    QRScannerScreen(requestPermissionLauncher)
                }
            }
        }

    }

    override fun onResume() {
        super.onResume()
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        )
        if (permission == PackageManager.PERMISSION_GRANTED) {
            vm.permissionGranted.postValue(1)
        } else {
            if (VERSION.SDK_INT > VERSION_CODES.M && shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                vm.permissionGranted.postValue(-1)
            } else {
                vm.permissionGranted.postValue(0)
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QRScannerScreen(requestPermissionLauncher: ActivityResultLauncher<String>?) {
    val scope = rememberCoroutineScope()
    val bottomSheet = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var alertShow by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        if (bottomSheet.targetValue == ModalBottomSheetValue.Hidden) {
                            bottomSheet.show()
                        } else {
                            bottomSheet.hide()
                        }
                    }
                },
                backgroundColor = Color.White,
                contentColor = Color.Black,
                shape = RoundedCornerShape(100)
            ) {
                Icon(
                    if (bottomSheet.targetValue != ModalBottomSheetValue.Hidden) painterResource(id = R.drawable.ic_close_white_24dp) else painterResource(id = R.drawable.ic_list_alt_white),
                    tint = Color.Black,
                    contentDescription = ""
                )
            }
        },
        isFloatingActionButtonDocked = true,
        floatingActionButtonPosition = androidx.compose.material.FabPosition.End,
        bottomBar = {
            BottomAppBar(
                elevation = 12.dp,
                backgroundColor = MaterialTheme.colorScheme.background,
                cutoutShape = RoundedCornerShape(50),
            ) {
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                IconButton(
                    onClick = {
                        alertShow = true
                    }
                ) {
                    Icon(Icons.Filled.Menu, "")
                }
                Text(
                    text = "QR Scanner", modifier = Modifier.stealthTapListener(
                        onTapCallBack = {
                            disableStealth(context)
                        }), style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            }
        }

    ) {
        QRCameraView(requestPermissionLauncher, bottomSheet)
        ModalBottomSheetLayout(
            sheetBackgroundColor = MaterialTheme.colorScheme.background,
            sheetState = bottomSheet,
            sheetContent = {
                Scaffold(
                    backgroundColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(text = "Scan Results", style = MaterialTheme.typography.titleSmall)
                            }
                        )
                    }
                ) {
                    QRResultView()
                }
            }
        ) {}
        if (alertShow) {
            AlertDialog(
                shape = RoundedCornerShape(8.dp),
                tonalElevation = 12.dp,
                onDismissRequest = {
                    alertShow = false
                },
                title = {
                    Text(
                        text = "QR Scanner", style = MaterialTheme.typography.titleMedium, modifier = Modifier.stealthTapListener(
                            onTapCallBack = {
                                disableStealth(context)
                            }
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                text = {
                    Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Version : ${BuildConfig.VERSION_CODE}")
                        ListItem(
                            text = {
                                Text(text = "Supported Formats")
                            },
                            secondaryText = {
                                Column {
                                    ListItem(
                                        text = {
                                            Text(text = "1D product", style = MaterialTheme.typography.titleMedium)
                                        },
                                        secondaryText = {
                                            Column {
                                                Text(text = "UPC-A")
                                                Text(text = "UPC-E")
                                                Text(text = "EAN-8")
                                                Text(text = "EAN-13")
                                                Text(text = "UPC/EAN Extension 2/5")
                                            }
                                        }
                                    )
                                    ListItem(
                                        text = {
                                            Text(text = "1D industrial", style = MaterialTheme.typography.titleMedium)
                                        },
                                        secondaryText = {
                                            Column {
                                                Text(text = "Code 39")
                                                Text(text = "Code 93")
                                                Text(text = "Code 128")
                                                Text(text = "Codabar")
                                                Text(text = "ITF")
                                            }
                                        }
                                    )
                                    ListItem(
                                        text = {
                                            Text(text = "2D", style = MaterialTheme.typography.titleMedium)
                                        },
                                        secondaryText = {
                                            Column {
                                                Text(text = "QR Code")
                                                Text(text = "Data Matrix")
                                                Text(text = "Aztec")
                                                Text(text = "PDF 417")
                                                Text(text = "MaxiCode")
                                                Text(text = "RSS-14")
                                                Text(text = "RSS-Expanded")
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        alertShow = false
                    }) {
                        Text(text = "Ok")
                    }
                }
            )
        }

    }

}


fun disableStealth(context: Context) {
    StealthModeController.enableStealth(StealthModeController.StealthApp.SAMOURAI, context)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QRResultView() {
    val viewModel = viewModel<QRStealthAppViewModel>()
    val qrValue by viewModel.qrContent.observeAsState(arrayListOf())
    val context = LocalContext.current
    LazyColumn {
        items(qrValue) { item ->
            Card(
                elevation = 8.dp,
                modifier = Modifier
                    .padding(vertical = 8.dp, horizontal = 16.dp)
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp),
                backgroundColor = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
            ) {
                Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize()) {
                    ListItem(
                        text = {
                            Text(text = item, modifier = Modifier.padding(vertical = 12.dp))
                        }, trailing = {
                            Row(modifier = Modifier.width(80.dp), Arrangement.SpaceBetween) {
                                IconButton(onClick = {
                                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("scan", item))
                                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(painterResource(id = R.drawable.ic_baseline_content_copy_24), contentDescription = "Copy")
                                }
                                IconButton(onClick = {
                                    viewModel.clearItem(item)
                                    Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(painterResource(id = R.drawable.ic_delete_24), contentDescription = "delete")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QRCameraView(requestPermissionLauncher: ActivityResultLauncher<String>?, bottomSheet: ModalBottomSheetState) {
    val viewModel = viewModel<QRStealthAppViewModel>()
    val permissionState by viewModel.permissionGranted.observeAsState(0)
    var scanner by remember { mutableStateOf<CodeScanner?>(null) }
    val context = LocalContext.current
    LaunchedEffect(permissionState) {
        if (permissionState == 1) {
            scanner?.startPreview()
        }
    }
    val scope = rememberCoroutineScope()
    AndroidView(
        factory = {
            val view = LayoutInflater.from(it).inflate(R.layout.bottomsheet_camera, null)
            val permissionView = view.findViewById<MaterialCardView>(R.id.permissionCameraDialog)
            permissionView.visibility = View.GONE
            val scannerView = view.findViewById<CodeScannerView>(R.id.scanner_view)
            val mCodeScanner = CodeScanner(it.applicationContext, scannerView)
            mCodeScanner.isAutoFocusEnabled = true
            scanner = mCodeScanner
            view.setOnClickListener {
                if (permissionState == 1) {
                    mCodeScanner.startPreview()
                }
            }
            mCodeScanner.decodeCallback = DecodeCallback { result ->
                viewModel.add(result.text)
                scope.launch {
                    bottomSheet.show()
                }
            }
            permissionView.findViewById<MaterialButton>(R.id.permissionCameraDialogGrantBtn)
                .setOnClickListener {
                    if (permissionState == 0) {
                        requestPermissionLauncher?.launch(Manifest.permission.CAMERA)
                    }
                    if (permissionState == -1) {
                        val intent = Intent()
                        val uri: Uri = Uri.fromParts("package", context.packageName, null)
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).data = uri
                        context.startActivity(intent)
                    }
                }
            return@AndroidView view;
        },
        update = {
            val permissionView = it.findViewById<MaterialCardView>(R.id.permissionCameraDialog)
            permissionView.visibility = if (permissionState == -1 || permissionState == 0) View.VISIBLE else View.GONE
        },
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview4() {
    samouraiStealthAppScanner {
        QRScannerScreen(null)
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun QRStealthAppSettings(callback: () -> Unit) {
    val secondaryColor = Color(0xffbab9b9)
    Box(
        Modifier
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 12.dp),
        ) {
            Text("Instructions", style = androidx.compose.material.MaterialTheme.typography.h6, color = Color.White)
            ListItem(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .padding(top = 8.dp),
                text = {
                    Text(
                        stringResource(R.string.enable_stealth_mode), color = Color.White, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                },
                secondaryText = {
                    Text(
                        stringResource(R.string.upon_exiting_samourai_wallet_stealth_mode),
                        style = MaterialTheme.typography.bodyMedium, color = secondaryColor
                    )
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ListItem(
                modifier = Modifier.padding(vertical = 8.dp),
                text = {
                    Text(
                        stringResource(R.string.disable_stealth_mode), color = Color.White, style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                },
                secondaryText = {
                    Column(modifier = Modifier) {
                        Text(
                            "Tap “QR scanner” at bottom of screen 5 times",
                            style = MaterialTheme.typography.bodyMedium, color = secondaryColor
                        )
                    }

                }
            )
        }

    }
}


val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    background = Color(0xFF0B0D0F),
    surface = Color(0xFF303942),
)

@Composable
fun samouraiStealthAppScanner(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = Color.Black.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}