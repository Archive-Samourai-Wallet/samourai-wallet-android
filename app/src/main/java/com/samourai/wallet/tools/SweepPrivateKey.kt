import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
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
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.hd.WALLET_INDEX
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.tools.viewmodels.SweepViewModel
import com.samourai.wallet.util.AddressFactory
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.PrivKeyReader
import org.bitcoinj.core.Coin

@Composable
fun SweepPrivateKeyView(
    supportFragmentManager: FragmentManager?,
    keyParameter: String = "",
    onCloseClick: () -> Unit
) {
    val vm = viewModel<SweepViewModel>()
    val page by vm.getPageLive().observeAsState()
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        vm.initWithContext(context, keyParameter)
    }
    SamouraiWalletTheme {
        Scaffold(
            modifier = Modifier.requiredHeight(530.dp),
            backgroundColor = samouraiBottomSheetBackground,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
            ) {
                WrapToolsPageAnimation(
                    visible = page == 0
                ) {
                    SweepFormSweepForm(supportFragmentManager)
                }
                WrapToolsPageAnimation(
                    visible = page == 1
                ) {
                    SweepTransactionPreview()
                }
                WrapToolsPageAnimation(
                    visible = page == 2
                ) {
                    SweepBroadcast(onCloseClick)
                }
            }
        }
    }
}

@Composable
fun SweepBroadcast(onCloseClick: () -> Unit) {
    val vm = viewModel<SweepViewModel>()
    val broadcastError by vm.getBroadcastErrorStateLive().observeAsState()
    val broadCastLoading by vm.getBroadcastStateLive().observeAsState(false)

    val message = if (broadcastError != null) {
        broadcastError
    } else {
        if (broadCastLoading) {
            stringResource(id = R.string.tx_broadcast_ok)
        } else {
            "Sweep transaction success"
        }
    }

    val painter = if (broadcastError != null)
        painterResource(id = R.drawable.ic_baseline_error_outline_24)
    else
        painterResource(id = R.drawable.ic_broom)

    Scaffold(
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.action_sweep), fontSize = 13.sp, color = samouraiAccent)
                },
                backgroundColor = samouraiBottomSheetBackground,
                elevation = 0.dp,
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .align(CenterHorizontally)
            ) {
                if (broadCastLoading) CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 1.dp,
                    modifier = Modifier.size(160.dp),
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Center)
                        .clip(RoundedCornerShape(150.dp))
                        .background(if (broadcastError != null) samouraiError else Color(0xff00D47D))
                ) {
                    Icon(
                        painter = painter,
                        modifier = Modifier
                            .size(48.dp)
                            .align(Center),
                        tint = Color.White,
                        contentDescription = ""
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "$message", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.size(44.dp))
            TextButton(onClick = onCloseClick) {
                Text(text = "Close", color = Color.White)
            }
        }
    }

}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SweepFormSweepForm(supportFragmentManager: FragmentManager?) {
    val vm = viewModel<SweepViewModel>()
    val address by vm.getAddressLive().observeAsState()
    val keyFormat by vm.getPrivateKeyFormatLive().observeAsState()
    val loading by vm.getLoadingLive().observeAsState(false)
    val addressEdit = remember { mutableStateOf(address ?: "") }
    val addressValidationError by vm.getAddressValidationLive().observeAsState()
    val context = LocalContext.current
    val isOffline by AppUtil.getInstance(context).offlineStateLive().observeAsState(false)
    val keyboardController = LocalSoftwareKeyboardController.current

    val bip38Passphrase by vm.getBIP38PassphraseLive().observeAsState("")
    var passphraseEntry by remember { mutableStateOf(bip38Passphrase) }

    LaunchedEffect(key1 = address, block = {
        if (addressEdit.value.isEmpty()) {
            addressEdit.value = address ?: "";
        }
        AppUtil.getInstance(context).checkOfflineState()
    })

    Scaffold(
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.action_sweep), fontSize = 13.sp, color = samouraiAccent)
                },
                backgroundColor = samouraiBottomSheetBackground,
                elevation = 0.dp,
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp),
            Arrangement.Center
        ) {
            AnimatedVisibility(visible = isOffline) {
                Text(
                    text = stringResource(id = R.string.in_offline_mode),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(
                            vertical = 12.dp,
                            horizontal = 2.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(samouraiWarning)
                        .padding(
                            12.dp
                        )
                        .fillMaxWidth(),
                    color = Color.White
                )
            }
            TextField(value = addressEdit.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        vm.setAddress(addressEdit.value, context, passphraseEntry)
                    },
                onValueChange = {
                    addressEdit.value = it
                }, colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = samouraiTextFieldBg,
                    cursorColor = samouraiAccent
                ), label = {
                    Text(stringResource(id = R.string.enter_privkey), color = Color.White)
                }, textStyle = TextStyle(fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    vm.setAddress(addressEdit.value, context, passphraseEntry)
                    keyboardController?.hide()
                }),
                isError = addressValidationError != null,
                trailingIcon = {
                    Icon(
                        painter = if (addressEdit.value.isNullOrEmpty()) painterResource(id = R.drawable.ic_crop_free_white_24dp)
                        else painterResource(id = R.drawable.ic_close_white_24dp),
                        contentDescription = "Clear/Scan",
                        modifier = Modifier.clickable {
                            if (addressEdit.value.isEmpty()) {
                                if (supportFragmentManager != null) {
                                    val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
                                    cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
                                    cameraFragmentBottomSheet.setQrCodeScanListener {
                                        cameraFragmentBottomSheet.dismiss()
                                        addressEdit.value = it
                                        vm.setAddress(it, context, passphraseEntry)
                                    }
                                }
                            } else {
                                addressEdit.value = "";
                                vm.clear()
                            }
                        }
                    )
                })
            AnimatedVisibility(visible = addressValidationError != null) {
                Text(
                    text = addressValidationError ?: "",
                    maxLines = 2,
                    modifier = Modifier.padding(vertical = 8.dp),
                    overflow = TextOverflow.Ellipsis,
                    color = samouraiError, fontSize = 13.sp
                )
            }
            if (keyFormat == PrivKeyReader.BIP38) TextField(
                value = passphraseEntry ?: "",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                onValueChange = {
                    passphraseEntry = it
                },
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = samouraiTextFieldBg,
                    cursorColor = samouraiAccent
                ),
                label = {
                    Text(stringResource(id = R.string.bip38_pw), color = Color.White)
                },
                textStyle = TextStyle(fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    keyboardController?.hide()
                    vm.setAddress(addressEdit.value, context, passphraseEntry)
                }),
            )
            Button(
                onClick = {
                    keyboardController?.hide()
                    vm.setAddress(addressEdit.value, context, passphraseEntry)
                },
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = samouraiAccent,
                    contentColor = Color.White
                ),
            ) {
                AnimatedVisibility(visible = !loading) {
                    Text(
                        stringResource(R.string.preview_sweep),
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                AnimatedVisibility(visible = loading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}


@Composable
fun SweepTransactionPreview() {
    val vm = viewModel<SweepViewModel>()
    val context = LocalContext.current
    val sweepAddress by vm.getSweepAddressLive().observeAsState()
    val sweepAmount by vm.getAmountLive().observeAsState()
    val validFees by vm.getValidFees().observeAsState(true)
    val dustOutput by vm.getDustStatus().observeAsState(true)
    val sweepFees by vm.getSweepFees().observeAsState()
    val receiveAddressType by vm.getReceiveAddressType().observeAsState()
    var showAdvanceOption by rememberSaveable { mutableStateOf(false) }
    var size by rememberSaveable { mutableStateOf(490) }
    var receiveAddress by rememberSaveable { mutableStateOf("") }
    val density = LocalDensity.current
    var confirmDialog by remember { mutableStateOf(false) }
    var satsFormat by remember { mutableStateOf(true) }

    LaunchedEffect(receiveAddressType) {
        receiveAddress = AddressFactory.getInstance().getAddress(receiveAddressType).right
    }

    Box(modifier = Modifier
        .onGloballyPositioned {
            size = it.size.height
        }) {
        Scaffold(
            backgroundColor = samouraiBottomSheetBackground,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = stringResource(id = R.string.action_sweep), fontSize = 13.sp, color = samouraiAccent)
                    },
                    backgroundColor = samouraiBottomSheetBackground,
                    elevation = 0.dp,
                    actions = {
                        IconButton(onClick = {
                            showAdvanceOption = true
                        }) {
                            Icon(painter = painterResource(id = R.drawable.ic_cogs), contentDescription = "", tint = Color.White)
                        }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(Modifier.weight(.8f)) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {

                        item {
                            ListItem(
                                title = stringResource(id = R.string.address),
                                value = "$sweepAddress"
                            )
                        }

                        item {
                            ListItem(
                                modifier = Modifier.clickable {
                                    satsFormat = !satsFormat
                                },
                                title = stringResource(R.string.balance_of_unspent),
                                value = "${if (satsFormat) FormatsUtil.formatSats(sweepAmount) else FormatsUtil.formatBTC(sweepAmount)} "
                            )
                        }
                        item {
                            ListItem(
                                modifier = Modifier.clickable {
                                    satsFormat = !satsFormat
                                },
                                title = stringResource(id = R.string.cost_of_sweep),
                                value = "${if (satsFormat) FormatsUtil.formatSats(sweepFees) else FormatsUtil.formatBTC(sweepFees)} "
                            )
                        }
                        item {
                            ListItem(
                                title = stringResource(id = R.string.receive_address),
                                value = receiveAddress
                            )
                        }
                        item {
                            SliderSegment()
                        }
                        item {
                            SweepEstimatedBlockConfirm()
                        }
                    }
                }
                Box(modifier = Modifier.padding(vertical = 12.dp)) {
                    val enable = validFees && !dustOutput;
                    Button(
                        enabled = enable,
                        onClick = {
                            if (enable)
                                confirmDialog = true
                        },
                        contentPadding = PaddingValues(
                            vertical = 12.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = if (enable) samouraiAccent else samouraiSlateGreyAccent.copy(alpha = 0.6f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = stringResource(id = R.string.sweep))
                    }
                }

            }


        }
        AnimatedVisibility(
            enter = slideInVertically {
                with(density) { -size.dp.roundToPx() }
            },
            exit = slideOutVertically(
                targetOffsetY = { -it },
            ),
            modifier = Modifier
                .fillMaxSize(),
            visible = showAdvanceOption
        ) {
            SweepAdvanceOption(
                onClose = {
                    showAdvanceOption = false
                }
            )
        }
    }

    if (confirmDialog) {
        AlertDialog(
            onDismissRequest = {
                confirmDialog = false
            },
            text = {
                Text("${stringResource(id = R.string.sweep)} ${Coin.valueOf(sweepAmount ?: 0L).toPlainString()} from $sweepAddress  (fee: ${Coin.valueOf(sweepFees ?: 0L).toPlainString()})?")
            },
            modifier = Modifier.shadow(24.dp),
            shape = RoundedCornerShape(16.dp),
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.initiateSweep(context)
                    }) {
                    Text(stringResource(id = R.string.confirm), color = samouraiTextPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        confirmDialog = false
                    }) {
                    Text(stringResource(id = R.string.cancel), color = samouraiTextPrimary)
                }
            }
        )
    }

}

@Composable
fun SweepEstimatedBlockConfirm() {
    val vm = viewModel<SweepViewModel>()
    val nbBlocks by vm.getBlockWaitTime().observeAsState()
    ListItem(
        title = stringResource(id = R.string.estimated_wait_time),
        value = nbBlocks.toString()
    )
}

@Composable
fun SliderSegment() {
    val vm = viewModel<SweepViewModel>()
    var sliderPosition by rememberSaveable { mutableStateOf(0.5f) }
    val feeRange by vm.getFeeRangeLive().observeAsState(0.5f)
    val satsPerByte by vm.getFeeSatsValueLive().observeAsState()
    val validFees by vm.getValidFees().observeAsState(true)
    val dustOutput by vm.getDustStatus().observeAsState(false)
    LaunchedEffect(true) {
        sliderPosition = feeRange
    }
    val context = LocalContext.current
    Column(modifier = Modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(value = sliderPosition,
                modifier = Modifier
                    .weight(.7f)
                    .padding(horizontal = 2.dp),
                colors = SliderDefaults.colors(
                    thumbColor = if (validFees) samouraiAccent else samouraiError,
                    activeTickColor = if (validFees) samouraiAccent else samouraiError,
                    activeTrackColor = if (validFees) samouraiAccent else samouraiError,
                    inactiveTickColor = Color.Gray,
                    inactiveTrackColor = samouraiWindow
                ),
                onValueChange = {
                    sliderPosition = it;
                    vm.setFeeRange(it, context = context)
                })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(.3f)
            ) {
                Text(
                    text = if (validFees) "$satsPerByte sats/b" else "_.__",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                )
            }
        }
        AnimatedVisibility(visible = !validFees) {
            Text(
                text = stringResource(R.string.sweep_invalid_fee_warning),
                fontSize = 10.sp,
                color = samouraiError,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        AnimatedVisibility(visible = dustOutput) {
            Text(
                text = stringResource(R.string.sweep_dust_warning),
                fontSize = 10.sp,
                color = samouraiError,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun SweepAdvanceOption(onClose: () -> Unit) {
    val list = stringArrayResource(id = R.array.address_types)
    val vm = viewModel<SweepViewModel>()
    val item by vm.getReceiveAddressType().observeAsState()
    val selectedItem = when (item) {
        WALLET_INDEX.BIP84_RECEIVE -> list[0]
        WALLET_INDEX.BIP49_RECEIVE -> list[1]
        WALLET_INDEX.BIP44_RECEIVE -> list[2]
        else -> list[0]
    }
    Scaffold(
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Advance options", fontSize = 13.sp, color = samouraiAccent)
                },
                backgroundColor = samouraiBottomSheetBackground,
                elevation = 0.dp,
                navigationIcon = {
                    IconButton(onClick = {
                        onClose()
                    }) {
                        Icon(
                            painter =
                            painterResource(id = R.drawable.ic_close_white_24dp),
                            contentDescription = "", tint = samouraiAccent
                        )
                    }
                }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(samouraiBottomSheetBackground)
        ) {
            Column(
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {
                DropDownTextField(
                    modifier = Modifier
                        .fillMaxWidth(),
                    value = selectedItem,
                    label = stringResource(id = R.string.address_type),
                    onOptionSelected = {
                        val index = when (it) {
                            list[0] -> WALLET_INDEX.BIP84_RECEIVE
                            list[1] -> WALLET_INDEX.BIP49_RECEIVE
                            list[2] -> WALLET_INDEX.BIP44_RECEIVE
                            else -> WALLET_INDEX.BIP84_RECEIVE
                        }
                        vm.setAddressType(index)
                    },
                    options = list.toList()
                )
                Column(
                    Modifier.padding(vertical = 24.dp)
                ) {
                    Text(
                        "Sweep to address",
                        fontSize = 16.sp, color = samouraiTextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.padding(top = 4.dp))
                    Text(
                        AddressFactory.getInstance().getAddress(item).right, fontSize = 13.sp, color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ListItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(
            vertical = 1.dp,
        )
    ) {
        Text(
            text = title, fontSize = 13.sp,
            color = samouraiTextSecondary,
            style = MaterialTheme.typography.subtitle2
        )
        Text(
            text = value,
            style = MaterialTheme.typography.subtitle2,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
@Preview(widthDp = 320, heightDp = 480)
fun SweepPreviewCompose() {
    SweepTransactionPreview()
}

@Composable
@Preview(widthDp = 320, heightDp = 480)
fun SweepFormPreview() {
    SweepFormSweepForm(null)
}


@Composable
@Preview(widthDp = 320, heightDp = 480)
fun SweepBroadcastPreview() {
    SweepBroadcast(onCloseClick = {})
}
