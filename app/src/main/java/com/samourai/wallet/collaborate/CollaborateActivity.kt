package com.samourai.wallet.collaborate

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.collaborate.viewmodels.CollaborateViewModel
import com.samourai.wallet.paynym.PayNymHome
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.AppUtil
import com.samourai.wallet.util.PrefsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CollaborateActivity : SamouraiActivity() {
    private val collaborateViewModel: CollaborateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val showParticipateTab = intent.extras?.getBoolean(SHOW_PARTICIPATE, false) ?: false
        val claimed = PrefsUtil.getInstance(applicationContext).getValue(PrefsUtil.PAYNYM_CLAIMED, false);
        setContent {
            SamouraiWalletTheme {
                if (claimed) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                        CollaborateScreen(this, showParticipateTab)
                    }
                } else {
                    val scope = rememberCoroutineScope()
                    PaynymNotClaimed(
                        onBackPressed = {
                            onBackPressed()
                        },
                        onClaim = {
                            scope.launch {
                                withContext(Dispatchers.Main){
                                    val intent = Intent(this@CollaborateActivity, PayNymHome::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    )
                }
            }
        }
        if(claimed){
            collaborateViewModel.initWithContext(applicationContext)
        }
    }

    companion object {
        const val SHOW_PARTICIPATE = "participate"
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CollaborateScreen(collaborateActivity: CollaborateActivity?, showParticipateTab: Boolean) {
    val context = LocalContext.current;
    var selected by remember { mutableStateOf(0) }
    val scaffoldState = rememberScaffoldState()
    val paynymChooser = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val paynymSpendChooser = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val setUpTransactionModal = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val accountChooser = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val collaborateViewModel = viewModel<CollaborateViewModel>()
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val collaborateError by collaborateViewModel.errorsLive.observeAsState(initial = null)
    val showSpendFromPaynymChooser by cahootsTransactionViewModel.showSpendFromPaynymChooserLive.observeAsState(false)
    val walletLoading by AppUtil.getInstance(context).walletLoading.observeAsState(false);
//    val offlineState by AppUtil.getInstance(context).offlineStateLive().observeAsState()


    LaunchedEffect(true) {

        if (showParticipateTab) {
            collaborateViewModel.startListen()
            selected = 1
        }
    }

    LaunchedEffect(showSpendFromPaynymChooser) {
        if (showSpendFromPaynymChooser && paynymSpendChooser.currentValue == ModalBottomSheetValue.Hidden) {
            scope.launch {
                paynymSpendChooser.show()
            }
        } else {
            if (!showSpendFromPaynymChooser && paynymSpendChooser.currentValue != ModalBottomSheetValue.Hidden) {
                paynymSpendChooser.hide()
            }
        }
    }

    LaunchedEffect(key1 = collaborateError, block = {
        if (collaborateError != null)
            scaffoldState.snackbarHostState.showSnackbar(collaborateError!!)
    })
    Box(modifier = Modifier) {
        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { host ->
                SnackbarHost(hostState = host) {
                    Snackbar(
                        modifier = Modifier,
                        contentColor = Color.White,
                        backgroundColor = samouraiError,
                    ) {
                        Text(it.message, fontSize = 12.sp)
                    }
                }
            },
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        title = { Text(text = "Collaborate", color = samouraiTextPrimary) },
                        backgroundColor = samouraiSlateGreyAccent,
                        navigationIcon = {
                            Box(modifier = Modifier.padding(12.dp)) {
                                Icon(painter = painterResource(id = R.drawable.ic_close_white_24dp),
                                    tint = samouraiTextPrimary,
                                    contentDescription = "Close", modifier = Modifier.clickable {
                                        collaborateActivity?.onBackPressed()
                                    })
                            }
                        },
                    )
                    if (walletLoading)
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), backgroundColor = Color.Transparent, color = samouraiAccent)
                }
            },
        ) {
            Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
                TabRow(
                    divider = {
                        Box {}
                    },
                    selectedTabIndex = selected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    indicator = { tabPositions ->
                        TabIndicator(tabPositions, tabPage = selected)
                    },
                ) {
                    Tab(
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(painter = painterResource(id = R.drawable.ic_collaborate_initiate), contentDescription = "")
                                Text("Initiate", color = Color.White)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        selected = selected == 0,
                        onClick = { selected = 0 }
                    )
                    Tab(
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(painter = painterResource(id = R.drawable.ic_connect_without_contact), contentDescription = "")
                                Text("Participate", color = Color.White)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .padding(12.dp),
                        selected = selected == 1,
                        onClick = { selected = 1 }
                    )
                }

                Box(
                    modifier = Modifier
                ) {
                    WrapToolsPageAnimation(
                        selected == 0
                    ) {
                        InitiateSegment(
                            setUpTransaction = {
                                scope.launch {
                                    setUpTransactionModal.animateTo(ModalBottomSheetValue.Expanded)
                                }
                            },
                            onCahootTypeSelection = {
                                scope.launch {
                                    accountChooser.animateTo(ModalBottomSheetValue.Expanded)
                                }
                            },
                            onCollaboratorClick = {
                                scope.launch {
                                    paynymChooser.show()
                                }
                            }
                        )
                    }
                    WrapToolsPageAnimation(visible = selected != 0) {
                        ParticipateSegment()
                    }
                }

            }
        }

    }

    //Clear previous address calc state
    if (paynymChooser.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) {
            onDispose {
                scope.launch {
                    keyboardController?.hide()
                }
            }
        }
    }
    if (setUpTransactionModal.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) {
            onDispose {
                scope.launch {
                    keyboardController?.hide();
                }
            }
        }
    }

    if (paynymSpendChooser.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) {
            onDispose {
                scope.launch {
                    keyboardController?.hide()
                    cahootsTransactionViewModel.showSpendPaynymChooser(false)
                }
            }
        }
    }

    val handleBackPress = setUpTransactionModal.currentValue != ModalBottomSheetValue.Hidden ||
            paynymChooser.currentValue != ModalBottomSheetValue.Hidden ||
            accountChooser.currentValue != ModalBottomSheetValue.Hidden ||
            paynymSpendChooser.currentValue != ModalBottomSheetValue.Hidden

    BackHandler(enabled = handleBackPress) {
        scope.launch {
            if (setUpTransactionModal.currentValue != ModalBottomSheetValue.Hidden) {
                setUpTransactionModal.hide()
            } else if (paynymChooser.currentValue != ModalBottomSheetValue.Hidden) {
                paynymChooser.hide()
            } else if (accountChooser.currentValue != ModalBottomSheetValue.Hidden) {
                accountChooser.hide()
            } else if (paynymSpendChooser.currentValue != ModalBottomSheetValue.Hidden) {
                cahootsTransactionViewModel.showSpendPaynymChooser(false)
            }
        }

    }


    ModalBottomSheetLayout(
        sheetState = paynymChooser,
        modifier = Modifier.zIndex(5f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetBackgroundColor = samouraiBottomSheetBackground,
        sheetContent = {
            PaynymChooser(paynymChooser, PaynymChooserType.COLLABORATE, {
                cahootsTransactionViewModel.setCollaborator(it)
            }, {
                scope.launch {
                    paynymChooser.hide()
                }
            })
        },
        sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
    ) {
    }

    ModalBottomSheetLayout(
        sheetState = paynymSpendChooser,
        modifier = Modifier.zIndex(5f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetBackgroundColor = samouraiBottomSheetBackground,
        sheetContent = {
            PaynymChooser(paynymSpendChooser, PaynymChooserType.SPEND, {
                cahootsTransactionViewModel.setAddress(it)
            }, {
                scope.launch {
                    cahootsTransactionViewModel.showSpendPaynymChooser(false)
                }
            })
        },
        sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
    ) {
    }

    ModalBottomSheetLayout(
        sheetState = setUpTransactionModal,
        modifier = Modifier.zIndex(.4f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetBackgroundColor = samouraiBottomSheetBackground,
        sheetContent = {
            SetUpTransaction(onClose = {
                scope.launch {
                    setUpTransactionModal.hide()
                }
            })
        },
        sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
    ) {
    }
    ModalBottomSheetLayout(
        sheetState = accountChooser,
        modifier = Modifier
            .zIndex(5f)
            .fillMaxHeight(.48f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetBackgroundColor = samouraiBottomSheetBackground,
        sheetContent = {
            ChooseCahootsType(accountChooser)
        },
        sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
    ) {
    }

}


@Composable
fun PaynymAvatar(pcode: String?) {
    if (pcode == null) {
        return Box(modifier = Modifier)
    }
    var url by remember { mutableStateOf("${WebUtil.PAYNYM_API}${pcode}/avatar") }

    LaunchedEffect(pcode) {
        url = "${WebUtil.PAYNYM_API}${pcode}/avatar"
    }
    Row(
        modifier = Modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.size(34.dp)) {
            PicassoImage(
                url = url,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(100)),
                contentDescription = "Avatar for ${BIP47Meta.getInstance().getLabel(pcode)}"
            )
        }
        Text(BIP47Meta.getInstance().getDisplayLabel(pcode), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}


@Composable
private fun TabIndicator(
    tabPositions: List<TabPosition>,
    tabPage: Int
) {
    val transition = updateTransition(
        tabPage,
        label = "Tab indicator"
    )
    val indicatorLeft by transition.animateDp(
        label = "Indicator left"
    ) { page ->
        tabPositions[page].left
    }
    val indicatorRight by transition.animateDp(
        label = "Indicator right"
    ) { page ->
        tabPositions[page].right
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = indicatorLeft)
            .width(indicatorRight - indicatorLeft)
            .padding(2.dp)
            .fillMaxSize()
            .zIndex(-4f)
            .border(
                BorderStroke(120.dp, samouraiAccent),
                RoundedCornerShape(8.dp)
            )
    )
}


@Composable
fun PaynymNotClaimed(onBackPressed: () -> Unit = {}, onClaim: () -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                backgroundColor = samouraiSlateGreyAccent,
                title = { Text(text = "Collaborate", color = samouraiTextPrimary) },
                navigationIcon = {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Icon(painter = painterResource(id = R.drawable.ic_close_white_24dp),
                            tint = samouraiTextPrimary,
                            contentDescription = "Close", modifier = Modifier.clickable {
                                onBackPressed.invoke()
                            })
                    }
                },
            )
        }
    ) {
        Column(
            Modifier.fillMaxSize(),
            Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                onClaim.invoke()
            }) {
                Text(stringResource(id = R.string.claim_paynym))
            }
        }
    }
}

@Preview
@Composable
fun PaynymNotClaimedPreview() {
    PaynymNotClaimed()
}