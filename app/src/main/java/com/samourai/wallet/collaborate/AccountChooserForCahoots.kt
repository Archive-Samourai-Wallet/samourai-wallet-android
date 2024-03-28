package com.samourai.wallet.collaborate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.ListItem
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.constants.SamouraiAccountIndex
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.func.FormatsUtil
import com.samourai.wallet.util.PrefsUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseAccount() {
    val transactionViewModel = viewModel<CahootsTransactionViewModel>()
    val accountType by transactionViewModel.transactionAccountTypeLive.observeAsState()
    val context = LocalContext.current;
    val walletBalanceTicker by APIFactory.getInstance(context).walletBalanceObserverLiveData.observeAsState();
    var balanceDeposit by remember {
        mutableStateOf("")
    }
    var balancePostMix by remember {
        mutableStateOf("")
    }
    var depositUtxos by remember {
        mutableStateOf("")
    }
    var postMixUtxos by remember {
        mutableStateOf("")
    }

    LaunchedEffect(walletBalanceTicker) {
        withContext(Dispatchers.Default) {
            val balance = APIFactory.getInstance(context)
                .xpubBalance
            val postMixBalance = APIFactory.getInstance(context)
                .xpubPostMixBalance
            if (PrefsUtil.getInstance(context).getValue(PrefsUtil.IS_SAT, true)) {
                balanceDeposit = FormatsUtil.formatSats(balance)
                balancePostMix = FormatsUtil.formatSats(postMixBalance)
            } else {
                balanceDeposit = FormatsUtil.formatBTC(balance)
                balancePostMix = FormatsUtil.formatBTC(postMixBalance)
            }
            depositUtxos = "${APIFactory.getInstance(context).getUtxos(true).size}";
            postMixUtxos = "${APIFactory.getInstance(context).getUtxosPostMix(true).size}";
        }
    }

    Column(
        verticalArrangement = Arrangement.Top,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(text = "Select account", fontSize = 13.sp, color = samouraiAccent)
        }
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            horizontalAlignment = CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ListItem(
                modifier = Modifier
                    .clickable {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.DEPOSIT, context)
                    },
                trailing = {
                    Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "")
                },
                text = {
                    Text(text = stringResource(id = R.string.deposit_account), style = MaterialTheme.typography.subtitle2)
                },
                secondaryText = {
                    Text(text = "$balanceDeposit · $depositUtxos UTXOs", style = MaterialTheme.typography.caption)
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 12.dp)
            )
            ListItem(
                modifier = Modifier
                    .clickable {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.POSTMIX, context)
                    },
                trailing = {
                    Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "")
                },
                text = {
                    Text(text = stringResource(id = R.string.postmix_account), style = MaterialTheme.typography.subtitle2)
                },
                secondaryText = {
                    Text(text = "$balancePostMix · $postMixUtxos UTXOs", style = MaterialTheme.typography.caption)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseCahootsType(modalBottomSheetState: ModalBottomSheetState? = null) {
    val transactionViewModel = viewModel<CahootsTransactionViewModel>()
    val selectedCahootType by transactionViewModel.cahootsTypeLive.observeAsState()
    val scope = rememberCoroutineScope()

    val density = LocalDensity.current
    var cahootsType by remember {
        mutableStateOf<CahootsType?>(null);
    }
    val titleOffset: Dp by animateDpAsState(
        if (cahootsType == null) 8.dp else 44.dp,
    )
    DisposableEffect(modalBottomSheetState?.isVisible, effect = {
        onDispose {
            if (selectedCahootType == null && modalBottomSheetState?.isVisible == false) {
                cahootsType = null
            }
        }
    })
    LaunchedEffect(selectedCahootType, block = {
        if (selectedCahootType == null && cahootsType != null) {
            cahootsType = null
        }
    })

    Scaffold(
        modifier = Modifier.requiredHeight(380.dp),
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            TopAppBar(
                elevation = 0.dp,
                backgroundColor = samouraiBottomSheetBackground,
            ) {
                Column(
                    modifier = Modifier
                        .width(titleOffset)
                        .padding(top = 16.dp)
                ) {
                    AnimatedVisibility(
                        enter = slideInHorizontally {
                            with(density) { -34.dp.roundToPx() }
                        },
                        exit = slideOutHorizontally(
                            targetOffsetX = {
                                with(density) { -34.dp.roundToPx() }
                            }
                        ),
                        visible = cahootsType != null) {
                        IconButton(onClick = {
                            cahootsType = null
                            transactionViewModel.setCahootType(null)
                        }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "")
                        }
                    }
                }
                Box(modifier = Modifier.padding(start = 4.dp, top = 16.dp)) {
                    var value = if (cahootsType == CahootsType.STOWAWAY) "Stowaway" else "StonewallX2"
                    if (cahootsType == null) {
                        value = ""
                    }
                    Text(text = "Select Transaction Type${if (value.isNotEmpty()) " | " else ""}$value", fontSize = 13.sp, color = samouraiAccent)
                }
            }
        }
    ) {
        WrapToolsPageAnimation(visible = cahootsType == null) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            cahootsType = CahootsType.STONEWALLX2
                        },
                    trailing = {
                        Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "")
                    },
                    text = {
                        Text(text = "STONEWALLx2", style = MaterialTheme.typography.subtitle2)
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            text = "You initiate a transaction to a third party with\n" +
                                    "the help of a collaborator to create a high\n" +
                                    "entropy transaction",
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }

                )
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                ListItem(
                    modifier = Modifier
                        .clickable {
                            cahootsType = CahootsType.STOWAWAY
                        },
                    trailing = {
                        Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = "")
                    },
                    text = {
                        Text(text = "Stowaway", style = MaterialTheme.typography.subtitle2)
                    },
                    secondaryText = {
                        Text(
                            text = "You initiate a transaction to the collaborator\n" +
                                    "while making use of their UTXOs to create a\n" +
                                    "type of swap transaction.",
                            style = MaterialTheme.typography.caption
                        )
                    }
                )
            }
        }
        WrapToolsPageAnimation(visible = cahootsType != null) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val typeInPerson = if (CahootsType.STONEWALLX2 == cahootsType) CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_MANUAL else CahootsTransactionViewModel.CahootTransactionType.STOWAWAY_MANUAL
                            transactionViewModel.setCahootType(typeInPerson)
                            scope.launch {
                                modalBottomSheetState?.hide()
                            }
                        },
                    trailing = {
                        if (
                            selectedCahootType == CahootsTransactionViewModel.CahootTransactionType.STOWAWAY_MANUAL ||
                            selectedCahootType == CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_MANUAL
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_check_white), contentDescription = "")
                        } else {
                            Icon(painter = painterResource(id = R.drawable.ic_chevron_right_white_24dp), contentDescription = "")
                        }
                    },
                    text = {
                        Text(
                            text = "In Person / Manual ",
                            style = MaterialTheme.typography.subtitle2,
                            fontSize = 14.sp,
                        )
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp),

                            text = stringResource(id = R.string.manually_compose_this_transaction), style = MaterialTheme.typography.caption
                        )
                    }
                )
                Divider(
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val typeInPerson = if (CahootsType.STONEWALLX2 == cahootsType) CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_SOROBAN else CahootsTransactionViewModel.CahootTransactionType.STOWAWAY_SOROBAN
                            transactionViewModel.setCahootType(typeInPerson)
                            scope.launch {
                                modalBottomSheetState?.hide()
                            }
                        },
                    text = {
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.subtitle2,
                            fontSize = 14.sp,
                        )
                    },
                    trailing = {
                        if (
                            selectedCahootType == CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_SOROBAN ||
                            selectedCahootType == CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_SOROBAN
                        ) {
                            Icon(painter = painterResource(id = R.drawable.ic_check_white), contentDescription = "")
                        } else {
                            Icon(painter = painterResource(id = R.drawable.ic_chevron_right_white_24dp), contentDescription = "")
                        }
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                            text = stringResource(id = R.string.compose_this_transaction_online_with),
                            style = MaterialTheme.typography.caption
                        )
                    }
                )
            }
        }
    }
}

@Preview
@Composable
fun ChooseAccountPreview() {
    ChooseAccount()
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
fun ChooseCahootsTypePreview() {
    ChooseCahootsType(null)
}
