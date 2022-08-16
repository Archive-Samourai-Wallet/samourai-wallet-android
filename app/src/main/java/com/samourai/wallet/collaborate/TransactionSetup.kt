package com.samourai.wallet.collaborate

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.isDigitsOnly
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.cahoots.CahootsMode
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.tools.getSupportFragmentManger
import com.samourai.wallet.util.FormatsUtil
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToLong


@Composable
fun SetUpTransaction(onClose: (() -> Unit)?) {
    val transactionViewModel = viewModel<CahootsTransactionViewModel>()
    val page by transactionViewModel.pageLive.observeAsState(0)
    BoxWithConstraints(modifier = Modifier) {
        Box(modifier = Modifier.requiredHeight(this.maxHeight.times(0.576f))) {
            WrapToolsPageAnimation(visible = page == 0) {
                ChooseAccount()
            }
            WrapToolsPageAnimation(visible = page == 1) {
                ComposeCahootsTransaction(
                    onReviewClick = {
                        onClose?.invoke()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ComposeCahootsTransaction(onReviewClick: () -> Unit = {}) {

    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val supportFragmentManager = getSupportFragmentManger()

    Scaffold(backgroundColor = samouraiBottomSheetBackground,
        modifier = Modifier.fillMaxHeight(),
        topBar = {
            TopAppBar(
                backgroundColor = samouraiBottomSheetBackground,
                elevation = 0.dp,
                title = {
                    Text(
                        "Transaction setup",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(start = 4.dp),
                        color = samouraiAccent
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        cahootsTransactionViewModel.setPage(0)
                    }) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                    }

                },
                actions = {
                    IconButton(onClick = {
                        if (supportFragmentManager != null) {
                            val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
                            cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
                            cameraFragmentBottomSheet.setQrCodeScanListener {
                                cameraFragmentBottomSheet.dismiss()
                                var uri = it
                                if (it.startsWith("BITCOIN:")) {
                                    uri = "bitcoin:" + uri.substring(8)
                                    uri = uri.trim()
                                }
                                if (FormatsUtil.getInstance().isBitcoinUri(uri)) {
                                    val address = FormatsUtil.getInstance().getBitcoinAddress(uri)
                                    val amount = FormatsUtil.getInstance().getBitcoinAmount(uri)
                                    if (amount != null && amount.isNotEmpty()) {
                                        cahootsTransactionViewModel.setAmount(amount.toLong())
                                    }
                                    if (address != null && address.isNotEmpty()) {
                                        cahootsTransactionViewModel.setAddress(address)
                                    }
                                } else {
                                    if (FormatsUtil.getInstance().isValidBitcoinAddress(it)) {
                                        cahootsTransactionViewModel.setAddress(it)
                                    }
                                }
                            }
                        }
                    }) {
                        Icon(painter = painterResource(id = R.drawable.ic_crop_free_white_24dp), contentDescription = "")
                    }
                }
            )
        }) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                SendDestination()
                SendAmount()
                Box(modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(
                        top = 4.dp
                    )) {
                    Text(
                        text = "Miner fee rate", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = samouraiTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 13.dp)
                        .padding(bottom = 2.dp, end = 1.dp)
                ) {
                    SliderSegment()
                }
                Box(modifier = Modifier
                    .padding(horizontal = 16.dp)
               ) {
                   EstimatedBlockConfirm()
                }
            }
            ReviewButton(onClick = {
                onReviewClick.invoke()
            })
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun EstimatedBlockConfirm() {
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val estBlockConfirm by cahootsTransactionViewModel.estBlockLive.observeAsState("__")

    Column(
        modifier = Modifier.padding(
            vertical = 1.dp,
        )
    ) {
        Text(
            text = stringResource(id = R.string.estimated_wait_time),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = samouraiTextSecondary
        )
        Text(
            text = estBlockConfirm,
            style = MaterialTheme.typography.subtitle2,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

}

@Composable
fun ReviewButton(onClick: () -> Unit) {
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val validTransaction by cahootsTransactionViewModel.validTransactionLive.observeAsState(false)
    Button(
        onClick = {
            onClick.invoke()
        },
        enabled = validTransaction,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        colors = ButtonDefaults.textButtonColors(
            backgroundColor = samouraiAccent,
            contentColor = Color.White
        ),
    ) {
        Text("Review transaction setup")
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SendAmount() {
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val amount by cahootsTransactionViewModel.amountLive.observeAsState(0L)

    ListItem(
        text = {
            Box(modifier = Modifier.padding(bottom = 12.dp)) {
                Text(
                    text = "Send Amount", fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = samouraiTextSecondary
                )
            }
        },
        secondaryText = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceEvenly
            )
            {
                AmountInputField(amount, onChange = {
                    cahootsTransactionViewModel.setAmount(it)
                })
            }
        }
    )

}

/**
 * [amount] should be in sats format
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalAnimationApi::class)
@Composable
fun AmountInputField(amount: Long, onChange: (Long) -> Unit) {
    var amountEdit by remember {
        mutableStateOf(
            TextFieldValue(
                text = ""
            )
        )
    }
    var amountInSats by remember {
        mutableStateOf(0L)
    }
    var format by remember {
        mutableStateOf("BTC")
    }
    val keyboardController = LocalSoftwareKeyboardController.current

    fun calculateSats() {
        if (amountEdit.text.isNotBlank()) {
            try {
                val value = amountEdit.text.replace(",", "").toDouble()
                amountInSats = if (format == "sat") {
                    value.toLong()
                } else {
                    value.times(1e8).roundToLong()
                }
                onChange(amountInSats);
                keyboardController?.hide()
            } catch (e: Exception) {//NO-OP
                e.printStackTrace()
            }
        }
    }

    fun formatTextField(amount: Number): TextFieldValue {
        if (format == "BTC") {
            val value = NumberFormat.getInstance()
                .apply {
                    maximumFractionDigits = 8
                }
                .format(
                    amount
                        .toLong().div(1e8)
                )
            return TextFieldValue(
                text = value.replace(",", " "),
                selection = TextRange(value.length)
            )
        } else {
            val amountSanitized = amount
                .toDouble();
            var value: String
            val nFormat = NumberFormat.getNumberInstance(Locale.US)
            val decimalFormat = nFormat as DecimalFormat
            decimalFormat.applyPattern("#,###")
            //Remove all spaces. this will be handled by BTCFormatter
            value = nFormat.format(amountSanitized.times(1e8))
                .replace(",", "")
            if (amountSanitized >= 21000000.times(1e8)) {
                value = "";
            }
            return TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    LaunchedEffect(key1 = amount) {
        if (amount != amountInSats) {
            amountEdit = formatTextField(amount)
        }
    }

    @Synchronized
    fun onChangeFormatType() {
        format = if (format == "BTC") "sat" else "BTC"
        val amountText = amountEdit.text
            .replace(",", "")
            .replace(" ", "")
        if (amountText.isNotEmpty() && amountText != "0") {
            try {
                val amountFormatted = if (format == "BTC") {
                    amountText.toLong()
                } else {
                    amountText.toDouble()
                }
                amountEdit = formatTextField(
                    amountFormatted
                )
            } catch (e: Exception) {
            }
        }
    }


    TextField(
        value = amountEdit,
        onValueChange = {
            amountEdit = it
            if (amountEdit.text.isNotBlank()) {
                try {
                    var value = amountEdit.text
                        .replace(" ", "")
                        .toDouble()

                    if (format == "BTC" && amountEdit.text.split(".")[1].length > 8) {
                        value = it.text.dropLast(1).toDouble()
                        amountEdit = TextFieldValue(
                            text = it.text.dropLast(1)
                        )
                    }
                    println("Sema maxima: " + amountEdit.text.replace(" ", "").toDouble())
                    if (format == "sat" && amountEdit.text.replace(" ", "").toDouble() > 2.1E15) {
                        value = it.text.dropLast(1).toDouble()
                        amountEdit = TextFieldValue(
                            text = it.text.dropLast(1)
                        )
                    }
                    amountInSats = if (format == "sat") {
                        value.toLong()
                    } else {
                        value.times(1e8).roundToLong()
                    }
                    onChange(amountInSats);
                } catch (e: Exception) {//NO-OP
                    e.printStackTrace()
                }
            }
        },
        modifier = Modifier
            .padding(vertical = 4.dp)
            .fillMaxWidth()
            .onFocusChanged {
                calculateSats()
            },
        colors = TextFieldDefaults.textFieldColors(
            backgroundColor = samouraiTextFieldBg,
            cursorColor = samouraiAccent
        ),
        trailingIcon = {
            AnimatedContent(
                targetState = format,
                transitionSpec = {
                    if (format == "BTC") {
                        slideInVertically { height -> height } + fadeIn() with
                                slideOutVertically { height -> -height } + fadeOut()
                    } else {
                        slideInVertically { height -> -height } + fadeIn() with
                                slideOutVertically { height -> height } + fadeOut()
                    }.using(
                        SizeTransform(clip = false)
                    )
                }
            ) {
                ClickableText(
                    text = AnnotatedString(format),
                    onClick = { onChangeFormatType() },
                    style = TextStyle(
                        color = Color.Gray,
                        fontSize = 13.sp,
                    )
                )
            }
        },
        placeholder = {
            if (format == "sat")
                Text(text = "0", fontSize = 13.sp)
            else
                Text(text = "0.00000000", fontSize = 13.sp)
        },
        keyboardActions = KeyboardActions(
            onDone = { calculateSats() }
        ),
        visualTransformation = BTCFormatter(format == "sat"),
        textStyle = TextStyle(fontSize = 13.sp),
        keyboardOptions = KeyboardOptions(
            autoCorrect = false,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number,
        ),
    )
}

class BTCFormatter(private val isSat: Boolean) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (isSat) {
            val originalText = text.text
            var useDecimalFormat = false
            val formattedText = if (originalText.isNotBlank() && originalText.isDigitsOnly()) {
                useDecimalFormat = true
                DecimalFormat("#,###").format(originalText.toDouble())
            } else {
                originalText
            }
            val offsetMapping = object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    if (useDecimalFormat) {
                        val commas = formattedText.count { it == ',' }
                        return offset + commas
                    }
                    return offset
                }

                override fun transformedToOriginal(offset: Int): Int {
                    if (useDecimalFormat) {
                        val commas = formattedText.count { it == ' ' }
                        return offset - commas
                    }
                    return offset
                }
            }
            return TransformedText(
                text = AnnotatedString(formattedText.replace(",", " ")),
                offsetMapping = offsetMapping
            )
        } else {
            return TransformedText(
                text = text,
                offsetMapping = OffsetMapping.Identity
            )
        }
    }

}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SendDestination(modifier: Modifier = Modifier) {

    val invalidAddressError = stringResource(id = R.string.invalid_address)
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    var addressEdit by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    var addressError by remember { mutableStateOf<String?>(null) }
    var allowPaynymClear by remember { mutableStateOf(false) }
    val destinationAddress by cahootsTransactionViewModel.destinationAddressLive.observeAsState()
    val cahootsType by cahootsTransactionViewModel.cahootsTypeLive.observeAsState()
    val collaborator by cahootsTransactionViewModel.collaboratorPcodeLive.observeAsState()
    var pcode by remember { mutableStateOf<String?>(null) }

    fun validateAddress() {
        keyboardController?.hide()
        if (addressEdit.isEmpty()) {
            return
        }
        addressError = null
        if (FormatsUtil
                .getInstance()
                .isValidBitcoinAddress(addressEdit)
        ) {
            addressError = null
            addressEdit.let { cahootsTransactionViewModel.setAddress(it) }
        } else {
            addressError = if (addressEdit.isNotEmpty())
                invalidAddressError
            else
                null
        }
    }

    LaunchedEffect(key1 = destinationAddress) {
        pcode = if (FormatsUtil.getInstance().isValidPaymentCode(destinationAddress)) {
            destinationAddress
        } else {
            null
        }
        if (destinationAddress != addressEdit) {
            destinationAddress?.let {
                addressEdit = it
            }
        }
        if (destinationAddress?.isNotBlank() == true && destinationAddress?.isNotEmpty() == true) {
            validateAddress()
        }
    }

    LaunchedEffect(collaborator) {
        if (cahootsType?.cahootsType == CahootsType.STOWAWAY) {
            allowPaynymClear = false
            if (cahootsTransactionViewModel.isMultiCahoots()) {
                allowPaynymClear = true
            } else {
                pcode = cahootsTransactionViewModel.collaboratorPcodeLive.value
            }
        }
    }
    if (cahootsType != null) {
        when (cahootsType?.cahootsType) {
            CahootsType.STOWAWAY -> {
                allowPaynymClear = false
                if (cahootsTransactionViewModel.isMultiCahoots()) {
                    allowPaynymClear = true
                } else {
                    pcode = cahootsTransactionViewModel.collaboratorPcodeLive.value
                }
            }
            CahootsType.STONEWALLX2 -> {
                allowPaynymClear = true
            }
            CahootsType.MULTI -> {
                allowPaynymClear = true
            }
        }
    }
    Box(modifier = modifier) {
        if (!(cahootsType?.cahootsType == CahootsType.STOWAWAY && cahootsType?.cahootsMode == CahootsMode.MANUAL)) {
            ListItem(
                text = {
                    Box(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = "Send Destination", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = samouraiTextSecondary
                        )
                    }
                },
                secondaryText = {
                    if (pcode != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PicassoImage(
                                    modifier = modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(40.dp)),
                                    url = "${WebUtil.PAYNYM_API}${pcode}/avatar"
                                )
                                Text(text = BIP47Meta.getInstance().getDisplayLabel(pcode))
                            }
                            if (allowPaynymClear) IconButton(onClick = {
                                cahootsTransactionViewModel.setAddress("")
                            }) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "")
                            }
                        }


                    } else {
                        Column(modifier = Modifier) {
                            TextField(
                                value = addressEdit,
                                onValueChange = {
                                    addressEdit = it
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .onFocusChanged {
                                        cahootsTransactionViewModel.setAddress(addressEdit)
                                    },
                                trailingIcon = {
                                    IconButton(onClick = {
                                        cahootsTransactionViewModel.showSpendPaynymChooser()
                                    }) {
                                        Icon(painter = painterResource(id = R.drawable.ic_action_account_circle), contentDescription = "")
                                    }
                                },
                                colors = TextFieldDefaults.textFieldColors(
                                    backgroundColor = samouraiTextFieldBg,
                                    cursorColor = samouraiAccent
                                ),
                                textStyle = TextStyle(fontSize = 12.sp),
                                keyboardOptions = KeyboardOptions(
                                    autoCorrect = false,
                                    imeAction = ImeAction.Done,
                                    keyboardType = KeyboardType.Text,
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        cahootsTransactionViewModel.setAddress(addressEdit)
                                        keyboardController?.hide()
                                    }
                                ),
                                isError = addressError != null,

                                )
                            if (addressError != null)
                                Text(
                                    text = "$addressError",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    fontSize = 12.sp, color = samouraiError
                                )
                        }
                    }

                }
            )
        }
    }
}

@Preview(heightDp = 400)
@Composable
fun ComposeCahootsTransactionPreview() {
    ComposeCahootsTransaction()
}

@Composable
fun SliderSegment() {
    val vm = viewModel<CahootsTransactionViewModel>()
    val feeSliderValue by vm.feeSliderValue.observeAsState(0.5f)
    var sliderPosition by rememberSaveable { mutableStateOf(feeSliderValue ?: 0.5f) }
    LaunchedEffect(true) {
        sliderPosition = feeSliderValue
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Slider(value = sliderPosition,
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(horizontal = 4.dp),
            colors = SliderDefaults.colors(
                thumbColor = samouraiAccent,
                activeTickColor = samouraiAccent,
                activeTrackColor = samouraiAccent,
                inactiveTickColor = Color.Gray,
                inactiveTrackColor = samouraiWindow
            ),
            onValueChange = {
                sliderPosition = it
                vm.setFeeRange(it)
            })
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent)
        ) {
            SatsPerByte()
        }
    }
}

@Composable
fun SatsPerByte() {
    val vm = viewModel<CahootsTransactionViewModel>()
    val satsPerByte by vm.getFeeSatsValueLive().observeAsState("0")

    Text(
        text = "$satsPerByte sat/b",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        textAlign = TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )

}

