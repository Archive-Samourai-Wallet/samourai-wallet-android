package com.samourai.wallet.tools

import android.text.Selection
import android.text.SpannableString
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
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
import com.samourai.wallet.theme.samouraiError
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.theme.samouraiTextFieldBg
import com.samourai.wallet.util.func.ArmoredSignatureParser
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils.length

class MessageFormatType  {
    companion object {
        val RFC2440Format = "RFC2440 format"
        val BitcoinQtFormat = "Bitcoin-QT format"
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun VerifyMessage(
    modal: ModalBottomSheetState?
) {

    val vm = viewModel<AddressCalculatorViewModel>()
    val verifiedMessage by vm.isVerifiedMessage().observeAsState()

    val selectedFormatValue = remember { mutableStateOf(MessageFormatType.RFC2440Format) }

    var signature = remember { mutableStateOf("") }
    var message = remember { mutableStateOf("") }
    var address = remember { mutableStateOf("") }
    var rfc2440Message = remember { mutableStateOf("") }
    var rfc2440FormatErrorMessage = remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current

    // When the view is hidden, reset remembered fields which are not in vm
    LaunchedEffect(key1 = modal?.isVisible) {
        if (modal?.isVisible == false) {
            signature.value = ""
            message.value = ""
            address.value = ""
            rfc2440Message.value = ""
            rfc2440FormatErrorMessage.value = ""
            focusManager.clearFocus(true)
        }
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(samouraiBottomSheetBackground)
    ) {
        WrapToolsPageAnimation(
            visible = true
        ) {
            Column(
                modifier = Modifier
                    .background(samouraiBottomSheetBackground)
            ) {

                Header("Verify message", rfc2440FormatErrorMessage)

                Body(
                    vm,
                    verifiedMessage,
                    selectedFormatValue,
                    modal,
                    address,
                    message,
                    signature,
                    rfc2440Message,
                    rfc2440FormatErrorMessage,
                    bringIntoViewRequester
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun Body(
    vm: AddressCalculatorViewModel,
    verifiedMessage: Boolean?,
    selectedFormatValue: MutableState<String>,
    modal: ModalBottomSheetState?,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()) {
        BoxedBody(
            vm,
            verifiedMessage,
            selectedFormatValue,
            modal,
            address,
            message,
            signature,
            rfc2440Message,
            rfc2440FormatErrorMessage,
            bringIntoViewRequester
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun BoxedBody(
    vm: AddressCalculatorViewModel,
    verifiedMessage: Boolean?,
    selectedFormatValue: MutableState<String>,
    modal: ModalBottomSheetState?,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester
) {

    if (verifiedMessage == null) {
        InputBody(
            vm,
            selectedFormatValue,
            modal,
            address,
            message,
            signature,
            rfc2440Message,
            rfc2440FormatErrorMessage,
            bringIntoViewRequester)
    } else {

        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            if (verifiedMessage) {
                Spacer(modifier = Modifier.height(10.dp))
                ValidResultBody(selectedFormatValue, address, message, rfc2440Message)
                Spacer(modifier = Modifier.height(20.dp))
            } else {
                Spacer(modifier = Modifier.height(50.dp))
                InvalidResultBody()
                Spacer(modifier = Modifier.height(50.dp))
            }
        }

    }
}

@Composable
fun InvalidResultBody() {

    Column {
        Image(
            painter = painterResource(id = R.drawable.ic_message_alert),
            contentDescription = null,
            colorFilter = ColorFilter.tint(samouraiError),
            modifier = Modifier.size(96.dp)
        )
        Text(
            text = "Invalid signature",
            color = samouraiError,
        )
    }
}

@Composable
fun ValidResultBody(
    selectedFormatValue: MutableState<String>,
    address: MutableState<String>,
    message: MutableState<String>,
    rfc2440Message: MutableState<String>,
) {

    val messageToDisplay : String
    val addressToDisplay : String
    when (selectedFormatValue.value) {
        MessageFormatType.RFC2440Format -> {
            val signedMessage = SignedMessage.parse(rfc2440Message.value)
            messageToDisplay = signedMessage.message
            addressToDisplay = signedMessage.address
        }
        MessageFormatType.BitcoinQtFormat -> {
            messageToDisplay = message.value
            addressToDisplay = address.value
        }
        else -> { return }
    }

    Column {

        Column (
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_message_check),
                contentDescription = null,
                colorFilter = ColorFilter.tint(samouraiSuccess),
                modifier = Modifier.size(96.dp)
            )
            Text(
                text = "Valid signature",
                color = samouraiSuccess,
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = "Message",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = messageToDisplay,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Signed by address",
            color = Color.White,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = addressToDisplay,
            color = Color.White,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
private fun InputBody(
    vm: AddressCalculatorViewModel,
    selectedFormatValue: MutableState<String>,
    modal: ModalBottomSheetState?,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester
) {

    Column {

        MessageFormatSelection(vm, selectedFormatValue, rfc2440FormatErrorMessage)

        Spacer(modifier = Modifier.height(6.dp))

        MessageComponent(
            vm,
            address,
            message,
            signature,
            rfc2440Message,
            selectedFormatValue,
            rfc2440FormatErrorMessage,
            bringIntoViewRequester
        )

        Spacer(modifier = Modifier.height(2.dp))

        VerifyButton(
            vm,
            selectedFormatValue,
            address,
            message,
            signature,
            rfc2440Message,
            rfc2440FormatErrorMessage,
            bringIntoViewRequester
        )
    }
}

@Composable
private fun Header(
    title: String,
    rfc2440FormatErrorMessage: MutableState<String>,
) {

    TopAppBar(
        elevation = 0.dp,
        backgroundColor = samouraiBottomSheetBackground,
        title = {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = title,
                    fontSize = 13.sp,
                    color = samouraiAccent
                )

                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = rfc2440FormatErrorMessage.value,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Right,
                        color = samouraiError,
                    )
                }

            }

        },
    )
}

@Composable
fun MessageFormatSelection(
    vm: AddressCalculatorViewModel,
    selectedFormatValue: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
) {

    val isSelectedItem: (String) -> Boolean = { selectedFormatValue.value == it }

    val onChangeState: (String) -> Unit = {
        selectedFormatValue.value = it
        vm.clearVerifiedMessageState()
        rfc2440FormatErrorMessage.value = ""
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageComponent(
    vm: AddressCalculatorViewModel,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    selectedFormatValue: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester,
) {

    when(selectedFormatValue.value) {
        MessageFormatType.RFC2440Format ->     RFC2440MessageComponent(
            vm,
            rfc2440Message,
            rfc2440FormatErrorMessage,
            bringIntoViewRequester
        )
        MessageFormatType.BitcoinQtFormat ->     BitcoinQtMessageComponent(
            vm,
            address,
            message,
            signature,
            bringIntoViewRequester,
        )
        else -> {}
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RFC2440MessageComponent(
    vm: AddressCalculatorViewModel,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester,
) {

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .onFocusEvent { e ->
                    if (e.isFocused) {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView();
                        }
                    }
                }
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
                rfc2440Message.value = it
                vm.clearVerifiedMessageState()
                rfc2440FormatErrorMessage.value = ""
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
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                }
            ),
            singleLine = false,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = samouraiTextFieldBg
            ),
        )
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BitcoinQtMessageComponent(
    vm: AddressCalculatorViewModel,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester,
) {

    val messageFocusRequester = FocusRequester()
    val signatureFocusRequester = FocusRequester()

    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
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
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                },
            ),
            singleLine = true,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = samouraiTextFieldBg
            ),
            label = {
                Text(
                    "Address", fontSize = 12.sp
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
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
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                },
            ),
            singleLine = false,
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = samouraiTextFieldBg
            ),
            label = {
                Text(
                    "Message", fontSize = 12.sp
                )
            },
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .focusRequester(signatureFocusRequester)
                .onFocusEvent { e ->
                    if (e.isFocused) {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView();
                        }
                    }
                }
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
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Text,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                },
            ),
            singleLine = false,
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VerifyButton(
    vm: AddressCalculatorViewModel,
    selectedFormatValue: MutableState<String>,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    bringIntoViewRequester: BringIntoViewRequester,
) {

    val focusManager = LocalFocusManager.current

    val enableButton = isEnableVerifyButton(
        selectedFormatValue,
        address,
        message,
        signature,
        rfc2440Message,
        vm)

    Button(
        modifier = Modifier
            .bringIntoViewRequester(bringIntoViewRequester)
            .fillMaxWidth(),
        onClick = {
            focusManager.clearFocus(true)
            verifyMessageAction(
                selectedFormatValue,
                rfc2440Message,
                rfc2440FormatErrorMessage,
                vm,
                address,
                message,
                signature
            )
        },
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = samouraiAccent,
            contentColor = Color.White
        ),
        enabled = enableButton

    ) {
        Text("Verify message")
    }
}

private fun verifyMessageAction(
    selectedFormatValue: MutableState<String>,
    rfc2440Message: MutableState<String>,
    rfc2440FormatErrorMessage: MutableState<String>,
    vm: AddressCalculatorViewModel,
    address: MutableState<String>,
    message: MutableState<String>,
    signature: MutableState<String>
) {
    when (selectedFormatValue.value) {
        MessageFormatType.RFC2440Format -> {
            val signedMessage = SignedMessage.parse(rfc2440Message.value)
            if (signedMessage.address.isNotEmpty()) {
                rfc2440FormatErrorMessage.value = ""
                vm.executeVerifyMessage(
                    signedMessage.address,
                    signedMessage.message,
                    signedMessage.signature
                )
            } else {
                rfc2440FormatErrorMessage.value = "This is not in RFC2440 format!";
            }
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
    VerifyMessage(null)
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