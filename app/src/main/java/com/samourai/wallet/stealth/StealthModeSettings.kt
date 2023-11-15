package com.samourai.wallet.stealth

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.access.AccessFactory
import com.samourai.wallet.stealth.calculator.CalculatorStealthAppSettings
import com.samourai.wallet.stealth.notepad.NotepadAppStealthSettings
import com.samourai.wallet.stealth.qrscannerapp.QRStealthAppSettings
import com.samourai.wallet.stealth.vpn.VPNStealthAPPSettings
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.tech.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StealthModeSettings : SamouraiActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamouraiWalletTheme {
                StealthModeSettingsView(this)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StealthModeSettingsView(stealthModeSettings: StealthModeSettings) {

    var showAlert by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf(StealthModeController.StealthApp.CALCULATOR) }
    var isStealthEnabled by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(true) {
        isStealthEnabled = StealthModeController.isStealthEnabled(context)
        val app = StealthModeController.getSelectedApp(context)
        selectedApp = StealthModeController.StealthApp.values().find { it.getAppKey() == app } ?: StealthModeController.StealthApp.CALCULATOR
    }
    if (showAlert) Dialog(
        onDismissRequest = {
            showAlert = false
        },
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium.copy(CornerSize(12.dp)),
            color = samouraiSurface,
            elevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.set_stealth_pin), style = MaterialTheme.typography.subtitle1.copy(color = samouraiTextPrimary))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .padding(vertical = 8.dp)
                ) {
                    TextField(
                        value = value,
                        onValueChange = {
                            if (it.length <= 8) {
                                if (!it.contains("*") && !it.contains("-") && !it.contains(",") && !it.contains(".") && !it.contains(" ")) {
                                    value = it
                                }
                            }
                        },
                        Modifier
                            .focusRequester(focusRequester),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = samouraiTextSecondary,
                            focusedBorderColor = samouraiTextSecondary,
                            unfocusedBorderColor = samouraiTextSecondary,
                            focusedLabelColor = samouraiTextSecondary,
                            unfocusedLabelColor = samouraiTextSecondary
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        maxLines = 1,
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        showAlert = false
                    }) {
                        Text(text = stringResource(id = R.string.cancel), color = samouraiTextPrimary)
                    }
                    TextButton(onClick = {
                        try {
                            if (value.length > 4) {
                                if (AccessFactory.getInstance(stealthModeSettings.applicationContext).pin == value) {
                                    Toast.makeText(stealthModeSettings.applicationContext, R.string.stealth_pin_warning, Toast.LENGTH_SHORT).show()
                                } else {
                                    isStealthEnabled = true
                                }
                            }
                        } catch (e: Exception) {
                            LogUtil.error("tag", e)
                        }
                        showAlert = false;
                    }, enabled = value.length > 4) {
                        Text(text = stringResource(id = R.string.ok), color = if (value.length > 4) samouraiTextPrimary else samouraiTextPrimary.copy(alpha = 0.6f))
                    }
                }
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = samouraiSlateGreyAccent,
                title = {
                    Text(stringResource(id = R.string.stealth_mode), color = Color.White)
                },
                navigationIcon = {
                    IconButton(onClick = {
                        stealthModeSettings.onBackPressed()
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                    }
                }
            )
        },
        content = {
            Column(modifier = Modifier.fillMaxHeight()) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            scope.launch {
                                withContext(Dispatchers.Default) {
                                    isStealthEnabled = StealthModeController.toggleStealthState(context);
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    text = {
                        Text(text = stringResource(R.string.enable_stealth_mode_on_or_off))
                    },
                    trailing = {
                        Switch(checked = isStealthEnabled, onCheckedChange = {
                            scope.launch {
                                withContext(Dispatchers.Default) {
                                    isStealthEnabled = StealthModeController.toggleStealthState(context);
                                }
                            }
                        })
                    },
                )
                Divider()
                if (isStealthEnabled) {

                    ListItem(
                        text = {
                            Text(text = stringResource(R.string.choose_stealth_app))
                        },
                        secondaryText = {
                            Text(text = stringResource(id = selectedApp.getAppName()))
                        },
                    )
                    LazyRow(
                        content = {
                            StealthModeController.StealthApp.values().forEach {
                                if (it != StealthModeController.StealthApp.SAMOURAI) {
                                    item {
                                        Box(modifier = Modifier.padding(horizontal = 6.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .clickable {
                                                        selectedApp = it
                                                        StealthModeController.setSelectedApp(
                                                            it.getAppKey(),
                                                            stealthModeSettings.applicationContext
                                                        );
                                                    }
                                            ) {
                                                Card(
                                                    elevation = if (selectedApp.getAppKey() == it.getAppKey()) 1.dp else 0.dp,
                                                    backgroundColor = if (selectedApp.getAppKey() == it.getAppKey()) MaterialTheme.colors.surface else Color.Transparent,
                                                    border = if (selectedApp.getAppKey() == it.getAppKey()) BorderStroke(1.dp, samouraiSlateGreyAccent) else BorderStroke(0.dp, Color.Transparent),
                                                ) {
                                                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                                                        Image(
                                                            painter = painterResource(id = it.getIcon()), contentDescription = "",
                                                            modifier = Modifier.size(44.dp)
                                                        )
                                                        Text(
                                                            stringResource(id = it.getAppName()),
                                                            style = MaterialTheme.typography.subtitle2,
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }, modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Divider()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        WrapToolsPageAnimation(selectedApp == StealthModeController.StealthApp.CALCULATOR) {
                            CalculatorStealthAppSettings()
                        }
                        WrapToolsPageAnimation(selectedApp == StealthModeController.StealthApp.VPN) {
                            VPNStealthAPPSettings {}
                        }
                        WrapToolsPageAnimation(selectedApp == StealthModeController.StealthApp.QRAPP) {
                            QRStealthAppSettings {}
                        }
                        WrapToolsPageAnimation(selectedApp == StealthModeController.StealthApp.NOTEPAD) {
                            NotepadAppStealthSettings {}
                        }
                        Box(Modifier.weight(1f))
                    }
                }
            }
        }

    )
}

@Preview(showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun DefaultPreview3() {
    StealthModeSettingsView(StealthModeSettings())
}
