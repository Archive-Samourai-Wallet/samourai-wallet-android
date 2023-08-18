package com.samourai.wallet.tools

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomCenter
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.collaborate.PicassoImage
import com.samourai.wallet.fragments.CameraFragmentBottomSheet
import com.samourai.wallet.paynym.PayNymHome
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.viewmodels.Auth47ViewModel
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.PrefsUtil


@Composable
fun Auth47Login(param: String? = null, onClose: () -> Unit) {
    val vm = viewModel<Auth47ViewModel>()
    val page by vm.pageLive.observeAsState(0)
    val authChallenge by vm.authChallengeLive.observeAsState("")
    var showAuth47Details by remember { mutableStateOf(false) }
    val context = LocalContext.current;
    val offlineState by AppUtil.getInstance(context).offlineStateLive().observeAsState(false)
    val density = LocalDensity.current

    LaunchedEffect(true) {
        if (param != null) {
            vm.setChallengeValue(param)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxHeight(.5f),
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            TopAppBar(
                backgroundColor = samouraiBottomSheetBackground,
                elevation = 0.dp,
                title = {
                    Text(text = stringResource(R.string.auth_with_paynym), fontSize = 13.sp, color = samouraiAccent)
                },
                actions = {
                    if (authChallenge.isNotEmpty())
                        IconButton(onClick = {
                            showAuth47Details = !showAuth47Details
                        }) {
                            Icon(imageVector = Icons.Outlined.Info, contentDescription = stringResource(id = R.string.close))
                        }
                }
            )
        }
    ) {
        BoxWithConstraints {
            val height = maxHeight
            AnimatedVisibility(
                enter = slideInVertically {
                    with(density) { -height.times(0.6f).roundToPx() }
                },
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f),
                visible = showAuth47Details
            ) {
                Auth47DetailsView(onClose = {
                    showAuth47Details = false
                })
            }
            AnimatedVisibility(visible = offlineState) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Text(
                        text = stringResource(id = R.string.in_offline_mode),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(samouraiWarning)
                            .padding(
                                12.dp
                            )
                            .fillMaxWidth(),
                        color = Color.White
                    )
                }
            }
            WrapToolsPageAnimation(visible = page == 0) {
                Auth47Form(onClose)
            }
            WrapToolsPageAnimation(visible = page == 1) {
                Auth47Authentication()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(24.dp)
            )
        }
    }
}

@Composable
fun Auth47DetailsView(onClose: (() -> Unit)?) {
    val vm = viewModel<Auth47ViewModel>()
    val authChallenge by vm.authChallengeLive.observeAsState("")
    Box(
        modifier = Modifier
            .shadow(24.dp)
            .background(samouraiBottomSheetBackground)
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Center)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                Arrangement.Center,
                Alignment.Start
            ) {
                Column {
                    Text(stringResource(R.string.auth_challenge), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.padding(8.dp))
                    SelectionContainer {
                        Text(
                            text = authChallenge,
                            color = samouraiTextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            TextButton(onClick = {
                onClose?.invoke()
            }, modifier = Modifier.align(BottomCenter)) {
                Text(text = stringResource(id = R.string.close), color = Color.White)
            }
        }

    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun Auth47Form(onClose: (() -> Unit)? = null) {
    val vm = viewModel<Auth47ViewModel>()
    val keyboardController = LocalSoftwareKeyboardController.current
    val authChallengeValue by vm.authChallengeLive.observeAsState("")
    var isPaynymClaimed by remember { mutableStateOf(true) }
    var authChallengeEdit by remember { mutableStateOf(authChallengeValue) }
    val supportFragmentManager = getSupportFragmentManger()
    val context = LocalContext.current

    LaunchedEffect(authChallengeValue) {
        if (authChallengeEdit.isEmpty() && authChallengeValue.isNotEmpty()) {
            authChallengeEdit = authChallengeValue;
        }
        if (!PrefsUtil.getInstance(context).getValue(PrefsUtil.PAYNYM_CLAIMED, false)) {
            isPaynymClaimed = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 24.dp),
            Arrangement.Center,
            horizontalAlignment = CenterHorizontally
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth(),
                value = authChallengeEdit,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (supportFragmentManager != null) {
                                val cameraFragmentBottomSheet = CameraFragmentBottomSheet()
                                cameraFragmentBottomSheet.show(supportFragmentManager, cameraFragmentBottomSheet.tag)
                                cameraFragmentBottomSheet.setQrCodeScanListener {
                                    cameraFragmentBottomSheet.dismiss()
                                    authChallengeEdit = it
                                    vm.setChallengeValue(it)
                                }
                            }
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_crop_free_white_24dp), contentDescription = "scan")
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
                    vm.setChallengeValue(authChallengeEdit)
                    keyboardController?.hide()
                }),
                onValueChange = {
                    authChallengeEdit = it
                },
                enabled = isPaynymClaimed,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = Color.Black.copy(alpha = 0.06f)
                ),
                label = {
                    Text(
                        stringResource(R.string.enter_challenge), fontSize = 12.sp
                    )
                },
            )
        }
        if (!isPaynymClaimed) Box(
            modifier = Modifier
                .fillMaxSize()
                .background(samouraiBottomSheetBackground)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {
                Spacer(modifier = Modifier.padding(4.dp))
                OutlinedButton(
                    onClick = {
                        val intent = Intent(context, PayNymHome::class.java)
                        context.startActivity(intent)
                        onClose?.let { it() }
                    }, colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(id = R.string.claim_paynym))
                }
            }
        }
    }
}

@Composable
fun Auth47Authentication() {
    val vm = viewModel<Auth47ViewModel>()
    var paynymUrl by remember { mutableStateOf("") }
    val loading by vm.loadingLive.observeAsState(true)
    val error by vm.errorsLive.observeAsState(null)
    val success by vm.authSuccessLive.observeAsState(false)
    val context = LocalContext.current.applicationContext
    val avatar by BIP47Util.getInstance(context).payNymLogoLive.observeAsState(null);

    LaunchedEffect(key1 = true) {
        val pcode = BIP47Util.getInstance(context).paymentCode.toString();
        if (avatar == null) {
            paynymUrl = "${WebUtil.PAYNYM_API}${pcode}/avatar"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .animateContentSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Column(
            horizontalAlignment = CenterHorizontally,
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            Box(modifier = Modifier.align(CenterHorizontally)) {
                Box(modifier = Modifier) {
                    if (!success && paynymUrl.isNotEmpty()) PicassoImage(
                        url = paynymUrl,
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(100)),
                        contentDescription = ""
                    )
                    if (!success && avatar != null) Image(
                        bitmap = avatar!!.asImageBitmap(),
                        modifier = Modifier
                            .size(104.dp)
                            .clip(RoundedCornerShape(100)),
                        contentDescription = ""
                    )
                    if (success) Box(
                        modifier = Modifier
                            .size(104.dp)
                            .border(color = Color.White, shape = RoundedCornerShape(100), width = 2.dp)
                            .clip(RoundedCornerShape(100)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            tint = Color.White,
                            modifier = Modifier
                                .align(Center)
                                .size(64.dp)
                                .fillMaxSize(),
                            contentDescription = ""
                        )
                    }

                }
                if (loading) CircularProgressIndicator(
                    modifier = Modifier
                        .size(104.dp),
                    color = Color.White,
                    strokeWidth = 3.dp,
                )
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(104.dp),
                    color = Color.White,
                    progress = 1f,
                    strokeWidth = Dp.Hairline,
                )
            }
            AnimatedVisibility(visible = success) {
                Text(
                    text = stringResource(R.string.auth_success), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    color = samouraiSuccess,
                    modifier = Modifier.padding(top = 18.dp, start = 18.dp, end = 18.dp)
                )
            }
            if (!success)
                AuthMessage()
            AnimatedVisibility(visible = error != null) {
                Box(
                    modifier = Modifier
                        .requiredHeightIn(max = 120.dp)
                        .padding(
                            vertical = 8.dp,
                            horizontal = 24.dp
                        )
                        .clip(RoundedCornerShape(4.dp))
                        .background(samouraiError)
                        .padding(
                            12.dp
                        )
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        item {
                            Text(
                                text = "Error $error",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .fillMaxWidth(),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = !loading && !success) {
            Button(
                onClick = {
                    vm.initiateAuthentication(context)
                },
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 1.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                colors = ButtonDefaults.textButtonColors(
                    backgroundColor = samouraiAccent,
                    contentColor = Color.White
                ),
            ) {
                Text(stringResource(R.string.authenticate))
            }
        }
    }
}

@Composable
fun AuthMessage() {
    val vm = viewModel<Auth47ViewModel>()
    val authCallbackDomainValue by vm.authCallbackDomainLive.observeAsState("")
    val resourceHostLive by vm.resourceHostLive.observeAsState("")
    var paynym by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(key1 = true, block = {
        val pcode = BIP47Util.getInstance(context).paymentCode.toString();
        val label = BIP47Meta.getInstance().getDisplayLabel(pcode)
        val nym = PrefsUtil.getInstance(context)
            .getValue(
                PrefsUtil.PAYNYM_BOT_NAME,
                label
            )
        paynym = nym
    })

    Text(
        text = buildAnnotatedString {

            if (resourceHostLive.isNotEmpty() && resourceHostLive != authCallbackDomainValue) {
                append("Warning: ")
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                ) {
                    append("$authCallbackDomainValue ")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp,
                        color = Color.White
                    )
                ) {
                    append("is requesting an authentication to ")
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                ) {
                    append("${resourceHostLive}.")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp
                    )
                ) {
                    append("\n${stringResource(R.string.auth_using_paynym)} ")
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                ) {
                    append("$paynym ?")
                }
            } else {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                ) {
                    append("$authCallbackDomainValue ")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp,
                        color = Color.White
                    )
                ) {
                    append(stringResource(R.string.is_requesting_your_auth))
                }
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp
                    )
                ) {
                    append("\n${stringResource(R.string.auth_using_paynym)} ")
                }
                withStyle(
                    SpanStyle(
                        fontSize = 14.sp,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append(paynym)
                }
            }

        },
        textAlign = TextAlign.Center,
        maxLines = 8,
        letterSpacing = .5.sp,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 8.dp, start = 18.dp, end = 18.dp)
    )
}

@Preview(heightDp = 400)
@Composable
fun Auth47Preview() {
    SamouraiWalletTheme {
        Auth47Login(onClose = {})
    }
}

@Preview(heightDp = 400)
@Composable
fun Auth47AuthenticationPreview() {
    SamouraiWalletTheme {
        Auth47Authentication()
    }
}

@Preview(heightDp = 400)
@Composable
fun Auth47DetailsViewPreview() {
    SamouraiWalletTheme {
        Auth47DetailsView(onClose = {})
    }
}

@Composable
fun getSupportFragmentManger(): FragmentManager? {
    val context = LocalContext.current
    if (context is AppCompatActivity) {
        return context.supportFragmentManager;
    }
    return null
}