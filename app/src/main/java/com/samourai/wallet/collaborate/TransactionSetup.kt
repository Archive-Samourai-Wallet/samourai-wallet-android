package com.samourai.wallet.collaborate

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.fragments.PaynymSelectModalFragment
import com.samourai.wallet.fragments.PaynymSelectModalFragment.Companion.newInstance
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.tools.getSupportFragmentManger
import com.samourai.wallet.util.FormatsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import java.text.NumberFormat


@Composable
fun SetUpTransaction(onClose: (() -> Unit)?) {
    val transactionViewModel = viewModel<CahootsTransactionViewModel>()
    val context = LocalContext.current
    LaunchedEffect(true ){
        ///Initialize to default account
        transactionViewModel.setAccountType(account = SamouraiAccountIndex.DEPOSIT, context = context)
    }
    val page by transactionViewModel.pageLive.observeAsState(0)
    Box(modifier = Modifier.requiredHeight(420.dp)) {
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
                                        cahootsTransactionViewModel.setAmount(amount.toDouble() / 100000000)
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
//                Box(modifier = Modifier.padding(horizontal = 8.dp)) {
//                    SliderSegment()
//                }
            }
            ReviewButton(onClick = {
                onReviewClick.invoke()
            })
        }
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


@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun SendAmount() {
    var amountEdit by remember { mutableStateOf("") }
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val amount by cahootsTransactionViewModel.amountLive.observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = amount) {
        amount?.let {
            val value = NumberFormat.getInstance().apply {
                maximumFractionDigits = 8
            }.format(it)
            if (amountEdit != value && it != 0.0) {
                amountEdit = value
            }
        }
    }

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
            TextField(
                value = amountEdit,
                onValueChange = {
                    amountEdit = it
                    if (amountEdit.isNotBlank()) {
                        try {
                            val value = amountEdit.toDouble()
                            cahootsTransactionViewModel.setAmount(value)
                        } catch (e: Exception) {//NO-OP
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .onFocusChanged {
                        if (amountEdit.isNotBlank()) {
                            try {
                                val value = amountEdit.toDouble()
                                cahootsTransactionViewModel.setAmount(value)
                                keyboardController?.hide()
                            } catch (e: Exception) {//NO-OP
                            }
                        }
                    },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = samouraiTextFieldBg,
                    cursorColor = samouraiAccent
                ),
                trailingIcon = {
                    Text(text = "BTC")
                },
                placeholder = {
                    Text(text = "0.00000000", fontSize = 13.sp)
                },
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (amountEdit.isNotBlank()) {
                            try {
                                val value = amountEdit.toDouble()
                                cahootsTransactionViewModel.setAmount(value)
                                keyboardController?.hide()
                            } catch (e: Exception) {//NO-OP
                            }
                        }
                    }
                ),
                textStyle = TextStyle(fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
            )
        }
    )

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
    var pcode by remember { mutableStateOf<String?>(null) }
    val paynym = stringResource(id = R.string.paynym)

    val getSupportFragmentManager = getSupportFragmentManger()

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
            addressError = invalidAddressError
        }
    }

    if (cahootsType != null) {
        when (cahootsType?.cahootsType) {
            CahootsType.STOWAWAY -> {
                pcode = cahootsTransactionViewModel.collaboratorPcodeLive.value
                allowPaynymClear = false
            }
            CahootsType.STONEWALLX2 -> {
                allowPaynymClear = true
            }
            CahootsType.MULTI -> {
                allowPaynymClear = true
            }
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
        if (destinationAddress?.isNotBlank() == true) {
            validateAddress()
        }
    }

    Box(modifier = modifier) {
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
                                    getSupportFragmentManager?.let {
                                        val paynymSelectModalFragment = newInstance(selectListener = object : PaynymSelectModalFragment.Listener {
                                            override fun onPaynymSelectItemClicked(code: String?) {
                                                code?.let { it1 -> cahootsTransactionViewModel.setAddress(it1) }
                                            }
                                        }, paynym, false)
                                        paynymSelectModalFragment.show(getSupportFragmentManager, "paynym_select")
                                    }
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

@Preview(heightDp = 400)
@Composable
fun ComposeCahootsTransactionPreview() {
    ComposeCahootsTransaction()
}


@Composable
fun SliderSegment() {
    val vm = viewModel<CahootsTransactionViewModel>()
    val feeSliderValue by vm.feeSliderValue.observeAsState()
    var sliderPosition by rememberSaveable { mutableStateOf(feeSliderValue ?: 0.5f) }
    val satsPerByte by vm.getFeeSatsValueLive().observeAsState(feeSliderValue)
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(value = sliderPosition,
            modifier = Modifier
                .fillMaxWidth(.75f)
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
        ) {
            Text(
                text = "$satsPerByte sats/b",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
            )
        }
    }
}
