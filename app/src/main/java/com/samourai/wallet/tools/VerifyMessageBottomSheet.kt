package com.samourai.wallet.tools

import android.text.Selection
import android.text.SpannableString
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

class MessageFormatType  {
    companion object {
        val RFC2440Format = "RFC2440 format"
        val BitcoinQtFormat = "Bitcoin-QT format"
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VerifyMessage(
    modal: ModalBottomSheetState?,
    onClose: () -> Unit,
) {

    val vm = viewModel<AddressCalculatorViewModel>()
    val verifiedMessage by vm.isVerifiedMessage().observeAsState()

    val selectedFormatValue = remember { mutableStateOf(MessageFormatType.RFC2440Format) }

    var signature = remember { mutableStateOf("") }
    var message = remember { mutableStateOf("") }
    var address = remember { mutableStateOf("") }
    var rfc2440Message = remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

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

                Header(verifiedMessage, focusManager, onClose)

                Spacer(modifier = Modifier.padding(10.dp))

                MessageFormatSelection(vm, selectedFormatValue)

                Spacer(modifier = Modifier.padding(10.dp))

                MessageComponent(
                    modal,
                    vm,
                    address,
                    message,
                    signature,
                    rfc2440Message,
                    selectedFormatValue,
                )

                Spacer(modifier = Modifier.padding(6.dp))

                VerifyButton(
                    vm,
                    selectedFormatValue,
                    address,
                    message,
                    signature,
                    rfc2440Message)

                Spacer(modifier = Modifier.padding(10.dp))

                Footer(
                    verifiedMessage,
                    selectedFormatValue,
                    rfc2440Message)

                Spacer(modifier = Modifier.padding(6.dp))
            }
        }
    }
}

@Composable
private fun Header(
    verifiedMessage: Boolean?,
    focusManager: FocusManager,
    onClose: () -> Unit
) {

    TopAppBar(
        elevation = 0.dp,
        backgroundColor = samouraiBottomSheetBackground,
        title = {

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Verify Message",
                    fontSize = 13.sp,
                    color = samouraiAccent
                )
                DisplayStatusSelection(verifiedMessage)

            }

        },
        navigationIcon = {
            IconButton(onClick = {
                focusManager.clearFocus()
                onClose()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(id = R.string.close)
                )
            }
        },
    )
}

@Composable
private fun DisplayStatusSelection(
    verifiedMessage: Boolean?
) {
    when(verifiedMessage) {
        true -> DisplayStatus(R.drawable.ic_message_check, samouraiSuccess, "Valid signature")
        false -> DisplayStatus(R.drawable.ic_message_alert, samouraiError, "Invalid signature")
        null -> {}
    }
}

@Composable
fun DisplayStatus(iconId: Int, contentColor: Color, message: String) {

    val iconDrawable = painterResource(iconId)

    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {

        Image(
            painter = iconDrawable,
            contentDescription = null,
            colorFilter = ColorFilter.tint(contentColor)
        )
        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = contentColor,
        )
    }
}

@Composable
fun MessageFormatSelection(
    vm: AddressCalculatorViewModel,
    selectedFormatValue: MutableState<String>,
) {

    val isSelectedItem: (String) -> Boolean = { selectedFormatValue.value == it }

    val onChangeState: (String) -> Unit = {
        selectedFormatValue.value = it
        vm.clearVerifiedMessageState()
    }

    val items = listOf(MessageFormatType.RFC2440Format, MessageFormatType.BitcoinQtFormat)

    Box(
        modifier = Modifier
            .background(samouraiBottomSheetBackground)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {

            items.forEach { item ->
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .selectable(
                            selected = isSelectedItem(item),
                            onClick = { onChangeState(item) },
                            role = Role.RadioButton
                        )
                ) {
                    RadioButton(
                        onClick = null,
                        selected = isSelectedItem(item)
                    )
                    Spacer(modifier = Modifier.padding(6.dp))
                    Text(
                        text = item,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MessageComponent(
    modal: ModalBottomSheetState?,
    vm: AddressCalculatorViewModel,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    selectedFormatValue: MutableState<String>,
) {

    when(selectedFormatValue.value) {
        MessageFormatType.RFC2440Format ->     RFC2440MessageComponent(
            modal,
            vm,
            rfc2440Message
        )
        MessageFormatType.BitcoinQtFormat ->     BitcoinQtMessageComponent(
            modal,
            vm,
            address,
            message,
            signature,
        )
        else -> {}
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterialApi::class)
@Composable
fun RFC2440MessageComponent(
    modal: ModalBottomSheetState?,
    vm: AddressCalculatorViewModel,
    rfc2440Message: MutableState<String>,
) {

    val messageFocusRequester = FocusRequester()

    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(messageFocusRequester) {
        if (modal?.isVisible == true) {
            messageFocusRequester.requestFocus()
            delay(50) // Make sure you have delay here
            keyboard?.show()
        }
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp)
                .focusRequester(messageFocusRequester)
                .onFocusChanged { focusState ->
                    // on the next focus will place teh cursor at the end of text
                    if (!focusState.isFocused) {
                        Selection.setSelection(
                            SpannableString.valueOf(rfc2440Message.value),
                            length(rfc2440Message.value),
                            length(rfc2440Message.value)
                        )
                    }
                },
            value = rfc2440Message.value,
            onValueChange = {
                rfc2440Message.value = it;
                vm.clearVerifiedMessageState()
            },
            trailingIcon = {
                if (rfc2440Message.value.isNotEmpty()) IconButton(onClick = {
                    rfc2440Message.value = ""
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
                imeAction = ImeAction.None,
                keyboardType = KeyboardType.Text,
            ),
            singleLine = false,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = samouraiTextFieldBg
            ),
        )
    }

}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
private fun BitcoinQtMessageComponent(
    modal: ModalBottomSheetState?,
    vm: AddressCalculatorViewModel,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
) {

    val messageFocusRequester = remember { FocusRequester() }
    val signatureFocusRequester = remember { FocusRequester() }
    val addressFocusRequester = remember { FocusRequester() }

    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(addressFocusRequester) {
        if (modal?.isVisible == true) {
            addressFocusRequester.requestFocus()
            delay(50) // Make sure you have delay here
            keyboard?.show()
        }
    }

    Column(
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
                    // on the next focus will place the cursor at the end of text
                    if (!focusState.isFocused) {
                        Selection.setSelection(
                            SpannableString.valueOf(address.value),
                            length(address.value),
                            length(address.value)
                        )
                    }
                },
            value = address.value,
            onValueChange = {
                address.value = it
                vm.clearVerifiedMessageState()
            },
            trailingIcon = {
                if (address.value.isNotEmpty()) IconButton(onClick = {
                    address.value = ""
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

        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .focusRequester(messageFocusRequester)
                .onFocusChanged { focusState ->
                    // on the next focus will place the cursor at the end of text
                    if (!focusState.isFocused) {
                        Selection.setSelection(
                            SpannableString.valueOf(message.value),
                            length(message.value),
                            length(message.value)
                        )
                    }
                },
            value = message.value,
            onValueChange = {
                val signedMessage = SignedMessage.parse(it)
                message.value = signedMessage.message
                if (signedMessage.address.isNotEmpty()) {
                    address.value = signedMessage.address;
                }
                if (signedMessage.signature.isNotEmpty()) {
                    signature.value = signedMessage.signature;
                }
                vm.clearVerifiedMessageState()
            },
            trailingIcon = {
                if (message.value.isNotEmpty()) IconButton(onClick = {
                    message.value = ""
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
                    "Message", fontSize = 12.sp
                )
            },
        )

        Spacer(modifier = Modifier.padding(vertical = 8.dp))

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .focusRequester(signatureFocusRequester)
                .onFocusChanged { focusState ->
                    // on the next focus will place the cursor at the end of text
                    if (!focusState.isFocused) {
                        Selection.setSelection(
                            SpannableString.valueOf(signature.value),
                            length(signature.value),
                            length(signature.value)
                        )
                    }
                },
            value = signature.value,
            trailingIcon = {
                if (signature.value.isNotEmpty()) IconButton(onClick = {
                    signature.value = ""
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
                signature.value = it
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
}

@Composable
private fun VerifyButton(
    vm: AddressCalculatorViewModel,
    selectedFormatValue: MutableState<String>,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
) {

    val enableButton = isEnableVerifyButton(
        selectedFormatValue,
        address,
        message,
        signature,
        rfc2440Message,
        vm)

    Button(
        onClick = {
            when(selectedFormatValue.value) {
                MessageFormatType.RFC2440Format -> {
                    val signedMessage = SignedMessage.parse(rfc2440Message.value)
                    vm.executeVerifyMessage(
                        signedMessage.address,
                        signedMessage.message,
                        signedMessage.signature
                    )
                }
                MessageFormatType.BitcoinQtFormat -> {
                    vm.executeVerifyMessage(
                        address.value,
                        message.value,
                        signature.value
                    )
                }
                else -> {}
            }
        },
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = samouraiAccent,
            contentColor = Color.White
        ),
        enabled = enableButton

    ) {
        Text("Verify Message")
    }
}

@Composable
fun Footer(
    verifiedMessage: Boolean?,
    selectedFormatValue: MutableState<String>,
    rfc2440Message: MutableState<String>
) {

    if (verifiedMessage != false) return
    if (selectedFormatValue.value != MessageFormatType.RFC2440Format) return
    if (rfc2440Message.value.isEmpty()) return
    val signedMessage = SignedMessage.parse(rfc2440Message.value)
    if (signedMessage.address.isNotEmpty()) return

    val errorMessage = "This message is not in RFC2440 format!"
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = errorMessage,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = samouraiError
            )
        }
    }
}

fun isEnableVerifyButton(
    selectedFormatValue: MutableState<String>,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    vm: AddressCalculatorViewModel
): Boolean {

    return when(selectedFormatValue.value) {
        MessageFormatType.RFC2440Format -> {
            return vm.isVerifiedMessage().value == null &&
                rfc2440Message.value.isNotEmpty()
        }
        MessageFormatType.BitcoinQtFormat -> {
            return vm.isVerifiedMessage().value == null &&
                address.value.isNotEmpty() &&
                message.value.isNotEmpty() &&
                signature.value.isNotEmpty()
        }
        else -> return false
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