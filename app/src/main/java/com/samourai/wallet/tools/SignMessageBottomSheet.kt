package com.samourai.wallet.tools

import AddressCalcForm
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.theme.samouraiTextFieldBg
import com.samourai.wallet.theme.samouraiWindow



@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SignMessage() {
    val vm = viewModel<AddressCalculatorViewModel>()
    val signedMessage by vm.getSignedMessage().observeAsState()
    val context = LocalContext.current
    var openDialog by remember { mutableStateOf(false) }
    val message by vm.getMessage().observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val insets = WindowInsets.ime.asPaddingValues(LocalDensity.current)
    val clipboardManager: ClipboardManager = LocalClipboardManager.current

    Box(modifier = Modifier.heightIn(min = 430.dp)) {
        if (openDialog) {
            AlertDialog(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(24.dp),
                backgroundColor = samouraiWindow,
                contentColor = Color.White,
                title = {
                    Text(text = stringResource(id = R.string.app_name), color = Color.White.copy(alpha = 0.8f))
                },
                text = {
                    Text(text = stringResource(id = R.string.copy_warning_generic), color = Color.White.copy(alpha = 0.8f))
                },
                onDismissRequest = {
                    openDialog = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        signedMessage?.let {
                            clipboardManager.setText(AnnotatedString(it))
                            openDialog = false
                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        }
                    })
                    {
                        Text(
                            text = stringResource(id = R.string.ok),
                            color = samouraiAccent, fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openDialog = false
                    })
                    {
                        Text(
                            text = stringResource(
                                id = R.string.cancel
                            ), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }

        WrapToolsPageAnimation(
            visible = signedMessage.isNullOrEmpty(),
        ) {
            Column(
                Modifier
                    .padding(
                        bottom = insets
                            .calculateBottomPadding()
                            .minus(200.dp)
                            .coerceAtLeast(0.dp)
                    )
                    .background(samouraiBottomSheetBackground)
            ) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = "Sign message", fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    },
                )
                AddressCalcForm()
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth()

                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        value = message!!,
                        trailingIcon = {
                            if (message!!.isNotEmpty()) IconButton(onClick = {
                                vm.setMessage("")
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close_white_24dp),
                                    contentDescription = "Clear"
                                )
                            }
                        },
                        textStyle = TextStyle(fontSize = 12.sp),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text,
                        ),
                        singleLine = false,
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                        }),
                        onValueChange = {
                            vm.setMessage(it)
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = samouraiTextFieldBg
                        ),
                        label = {
                            Text(
                                "Message", fontSize = 12.sp
                            )
                        },

                        )
                }
                Button(
                    onClick = {
                        keyboardController?.hide()
                        vm.executeSignMessage(message!!, context)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = samouraiAccent,
                        contentColor = Color.White
                    ),
                ) {
                    Text("Sign message")
                }
                Box(modifier = Modifier.padding(24.dp))
            }
        }

        WrapToolsPageAnimation(
            visible = !signedMessage.isNullOrEmpty(),
        ) {
            Box(
                modifier = Modifier
            ) {
                Column(
                    Modifier
                        .padding(
                            bottom = insets
                                .calculateBottomPadding()
                                .minus(250.dp)
                                .coerceAtLeast(0.dp)
                        )
                        .background(samouraiBottomSheetBackground)
                ) {
                    TopAppBar(
                        elevation = 0.dp,
                        backgroundColor = samouraiBottomSheetBackground,
                        title = {
                            Text(
                                text = stringResource(id = R.string.sign_message), fontSize = 13.sp,
                                color = samouraiAccent
                            )
                        },
                        actions = {
                            IconButton(onClick = {
                                openDialog = true
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_baseline_content_copy_24),
                                    contentDescription = "Copy"
                                )
                            }
                        }
                    )
                    Text(
                        text = "$signedMessage",
                        Modifier.padding(24.dp),
                        textAlign = TextAlign.Start,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Box(modifier = Modifier.padding(24.dp))
                }
            }
        }
    }
}


@Composable
@Preview(widthDp = 320, heightDp = 450)
fun SignMessagePreview() {
    SignMessage()
}