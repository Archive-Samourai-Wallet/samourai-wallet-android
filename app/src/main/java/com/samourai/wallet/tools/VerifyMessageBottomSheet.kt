package com.samourai.wallet.tools

import android.text.Selection
import android.text.SpannableString
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import com.samourai.wallet.theme.samouraiError
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.theme.samouraiTextFieldBg
import com.samourai.wallet.util.ArmoredSignatureParser
import kotlinx.coroutines.delay
import org.apache.commons.lang3.StringUtils.length

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun VerifyMessage(
    modal: ModalBottomSheetState?,
    onClose: () -> Unit,
) {

    val vm = viewModel<AddressCalculatorViewModel>()
    val verifiedMessage by vm.isVerifiedMessage().observeAsState()

    var signature by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    val addressFocusRequester = FocusRequester()
    val messageFocusRequester = FocusRequester()
    val signatureFocusRequester = FocusRequester()

    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(addressFocusRequester) {
        if (modal?.isVisible == true) {
            addressFocusRequester.requestFocus()
            delay(50) // Make sure you have delay here
            keyboard?.show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(samouraiBottomSheetBackground)
    ) {
        WrapToolsPageAnimation(
            visible = true
        ) {
            Column(
                modifier = Modifier
                    .background(samouraiBottomSheetBackground)
            ) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = "Verify Message",
                            fontSize = 13.sp,
                            color = samouraiAccent
                        )
                        Text(
                            text = if (verifiedMessage == null) "" else if (verifiedMessage == true) "Valid !" else "Not Valid !!!",
                            fontSize = 16.sp,
                            color = if (verifiedMessage == true) samouraiSuccess else samouraiError,
                            textAlign = TextAlign.Right,
                            modifier = Modifier
                                .padding(16.dp, 0.dp)
                                .fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            onClose()
                        }) {
                            Icon(imageVector = Icons.Outlined.Close, contentDescription = stringResource(id = R.string.close))
                        }
                    },
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .focusRequester(addressFocusRequester)
                            .onFocusChanged { focusState ->
                                // on the next focus will place teh cursor at the end of text
                                if (! focusState.isFocused) {
                                    Selection.setSelection(
                                        SpannableString.valueOf(address), length(address), length(address)
                                    )
                                }
                            },
                        value = address,
                        onValueChange = {
                            address = it
                            vm.clearVerifiedMessageState()
                        },
                        trailingIcon = {
                            if (address.isNotEmpty()) IconButton(onClick = {
                                address = ""
                                vm.clearVerifiedMessageState()
                                addressFocusRequester.requestFocus()
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
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                        ),
                        singleLine = true,
                        keyboardActions = KeyboardActions(
                            onNext = {
                                messageFocusRequester.requestFocus()
                            }),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = samouraiTextFieldBg
                        ),
                        label = {
                            Text(
                                "Address", fontSize = 12.sp
                            )
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .focusRequester(messageFocusRequester)
                            .onFocusChanged { focusState ->
                                // on the next focus will place teh cursor at the end of text
                                if (! focusState.isFocused) {
                                    Selection.setSelection(
                                        SpannableString.valueOf(address), length(address), length(address)
                                    )
                                }
                            },
                        value = message,
                        onValueChange = {
                            val signedMessage = SignedMessage.parse(it)
                            message = signedMessage.message
                            if (signedMessage.address.isNotEmpty()) {
                                address = signedMessage.address;
                            }
                            if (signedMessage.signature.isNotEmpty()) {
                                signature = signedMessage.signature;
                            }
                            vm.clearVerifiedMessageState()
                        },
                        trailingIcon = {
                            if (message.isNotEmpty()) IconButton(onClick = {
                                message = ""
                                vm.clearVerifiedMessageState()
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
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                        ),
                        singleLine = false,
                        keyboardActions = KeyboardActions(
                            onNext = {
                                signatureFocusRequester.requestFocus()
                            }),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = samouraiTextFieldBg
                        ),
                        label = {
                            Text(
                                "Message or armored signature", fontSize = 12.sp
                            )
                        },
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                        .fillMaxWidth()

                ) {

                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .focusRequester(signatureFocusRequester)
                            .onFocusChanged { focusState ->
                                // on the next focus will place teh cursor at the end of text
                                if (! focusState.isFocused) {
                                    Selection.setSelection(
                                        SpannableString.valueOf(address), length(address), length(address)
                                    )
                                }
                            },
                        value = signature,
                        trailingIcon = {
                            if (signature.isNotEmpty()) IconButton(onClick = {
                                signature = ""
                                vm.clearVerifiedMessageState()
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
                            imeAction = ImeAction.Next,
                            keyboardType = KeyboardType.Text,
                        ),
                        singleLine = false,
                        keyboardActions = KeyboardActions(
                            onNext = {
                                addressFocusRequester.requestFocus()
                        }),
                        onValueChange = {
                            signature = it
                            vm.clearVerifiedMessageState()
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = samouraiTextFieldBg
                        ),
                        label = {
                            Text(
                                "Signature", fontSize = 12.sp
                            )
                        },
                    )
                }
                Box(modifier = Modifier.padding(4.dp))
                Button(
                    onClick = {
                        vm.executeVerifyMessage(address, message, signature)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = samouraiAccent,
                        contentColor = Color.White
                    ),
                    enabled = address.isNotEmpty() &&
                        message.isNotEmpty() &&
                        signature.isNotEmpty() &&
                        verifiedMessage == null

                ) {
                    Text("Verify Message")
                }
                Box(modifier = Modifier.padding(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(widthDp = 320, heightDp = 480)
fun VerifyMessagePreview() {
    VerifyMessage(null, onClose = {})
}

data class SignedMessage(
    val message: String,
    val address: String,
    val signature: String,
) {
    companion object {
        fun parse(content: String): SignedMessage {
            val armoredSignatureParser = ArmoredSignatureParser.parse(content);
            if (armoredSignatureParser != null) {
                return SignedMessage(
                    armoredSignatureParser.message,
                    armoredSignatureParser.address,
                    armoredSignatureParser.signature
                )
            }
            return SignedMessage(content, "", "")
        }
    }
}