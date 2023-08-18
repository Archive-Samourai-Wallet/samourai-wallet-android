package com.samourai.wallet.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.viewmodels.BroadcastHexViewModel
import com.samourai.wallet.util.AppUtil


@Composable
fun BroadcastTransactionTool(
    onCloseClick: () -> Unit
) {
    val vm = viewModel<BroadcastHexViewModel>()
    val page by vm.page.observeAsState(0)

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
                    SweepHexInputForm()
                }
                WrapToolsPageAnimation(
                    visible = page == 1
                ) {
                    SweepHexBroadcast(onCloseClick = onCloseClick)
                }
                WrapToolsPageAnimation(
                    visible = page == 2
                ) {
                    SweepSuccessView(onCloseClick = onCloseClick)
                }
            }
        }
    }
}


@Composable
fun SweepSuccessView(onCloseClick: () -> Unit) {
    val vm = viewModel<BroadcastHexViewModel>()
    val transaction by vm.validTransaction.observeAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                title = {
                    Text(text = "Broadcast Transaction", color = Color.White)
                }
            )
        },
        backgroundColor = samouraiBottomSheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp),
            Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {

                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(150.dp))
                        .background(samouraiSuccess)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_white),
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center),
                        tint = Color.White,
                        contentDescription = ""
                    )
                }
            }
            Text(
                text = "${transaction?.hashAsString}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(
                        vertical = 12.dp,
                        horizontal = 2.dp
                    )
                    .clip(RoundedCornerShape(4.dp))
                    .padding(
                        12.dp
                    )
                    .fillMaxWidth(),
                color = Color.White
            )
            Spacer(modifier = Modifier.size(44.dp))
            TextButton(onClick = {
                onCloseClick()
            }) {
                Text(text = "Close", color = Color.White)
            }
        }
    }
}

@Composable
fun SweepHexBroadcast(onCloseClick: () -> Unit) {

    val vm = viewModel<BroadcastHexViewModel>()
    val broadCastLoading by vm.loading.observeAsState(false)
    val broadcastError by vm.broadcastError.observeAsState(null)
    val tx by vm.validTransaction.observeAsState(null)

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                title = {
                    Text(text = "Broadcast Transaction", color = Color.White)
                }
            )
        },
        backgroundColor = samouraiBottomSheetBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(top = 24.dp)
                .padding(horizontal = 24.dp),
            Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            ) {

                if (broadCastLoading) CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 1.dp,
                    modifier = Modifier.size(160.dp),
                )
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(150.dp))
                        .background(if (broadcastError != null) samouraiError else Color(0xff00D47D))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_broadcast_transaction),
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(0.96f)
                            .align(Alignment.Center),
                        tint = Color.White,
                        contentDescription = ""
                    )
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            AnimatedVisibility(visible = broadcastError == null) {
                Text(text = "Broadcasting transaction...", fontWeight = FontWeight.SemiBold, color = Color.White)
            }
            AnimatedVisibility(visible = broadcastError != null) {
                Text(
                    text = "$broadcastError",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(
                            vertical = 12.dp,
                            horizontal = 2.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(samouraiError)
                        .padding(
                            12.dp
                        )
                        .fillMaxWidth(),
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.size(44.dp))
            if (tx != null) {
                SelectionContainer(modifier = Modifier) {
                    Text(text = "${tx?.hashAsString}", fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
            TextButton(onClick = {
                onCloseClick()
            }, enabled = !broadCastLoading) {
                Text(text = "Close", color = Color.White)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SweepHexInputForm() {
    val context = LocalContext.current
    val isOffline by AppUtil.getInstance(context).offlineStateLive().observeAsState(false)
    var addressEdit by remember { mutableStateOf("") }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    val vm = viewModel<BroadcastHexViewModel>()
    val validTransaction by vm.validTransaction.observeAsState(null)
    val supportFragmentManager = getSupportFragmentManger()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                title = {
                    Text(text = "Broadcast transaction", color = samouraiAccent)
                }
            )
        },
        backgroundColor = samouraiBottomSheetBackground,
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
            Column(
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.padding(12.dp))
                TextField(value = addressEdit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged {
                            vm.setHex(addressEdit)
                        },
                    singleLine = false,
                    maxLines = 12,
                    onValueChange = {
                        addressEdit = it
                        vm.setHex(it)
                    }, colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = samouraiTextFieldBg,
                        cursorColor = samouraiAccent
                    ), label = {
                        Text("Enter Transaction Hex", color = Color.White)
                    }, textStyle = TextStyle(fontSize = 13.sp),
                    keyboardOptions = KeyboardOptions(
                        autoCorrect = false,
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        vm.setHex(addressEdit)
                        keyboardController?.hide()
                    }),
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_crop_free_white_24dp),
                            contentDescription = "Paste hex",
                            modifier = Modifier.clickable {
                                if (supportFragmentManager != null) {
                                    val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
                                    cameraFragmentBottomSheet.show(
                                        supportFragmentManager,
                                        cameraFragmentBottomSheet.tag
                                    )
                                    cameraFragmentBottomSheet.setQrCodeScanListener {
                                        cameraFragmentBottomSheet.dismiss()
                                        addressEdit = it
                                        vm.setHex(addressEdit)
                                    }
                                }
                            }
                        )
                    })
                Button(
                    onClick = {
                        vm.broadcast(context, addressEdit)
                    },
                    Modifier
                        .fillMaxWidth()
                        .alpha(if (validTransaction != null) 1f else 0.5f)
                        .padding(top = 3.dp),
                    enabled = validTransaction != null,
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = samouraiAccent,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.broadcast), color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}