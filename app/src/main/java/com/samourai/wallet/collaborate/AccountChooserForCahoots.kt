package com.samourai.wallet.collaborate

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.tools.WrapToolsPageAnimation
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import kotlinx.coroutines.Dispatchers
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
        withContext(Dispatchers.Default){
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
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(text = "Select account", fontSize = 13.sp, color = samouraiAccent)
        }
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Spacer(modifier = Modifier.padding(top = 4.dp))
            ListItem(
                modifier = Modifier
                    .clickable {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.DEPOSIT, context)
                    },
                icon = {
                    RadioButton(selected = accountType == SamouraiAccountIndex.DEPOSIT, onClick = {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.DEPOSIT, context)
                    })
                },
                text = {
                    Text(text = "Deposit account")
                },
                secondaryText = {
                    Text(text = "$balanceDeposit · $depositUtxos UTXOs")
                }
            )
            ListItem(
                modifier = Modifier
                    .clickable {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.POSTMIX, context)
                    },
                icon = {
                    RadioButton(selected = accountType == SamouraiAccountIndex.POSTMIX, onClick = {
                        transactionViewModel.setAccountType(SamouraiAccountIndex.POSTMIX, context)

                    })
                },
                text = {
                    Text(text = "Postmix account")
                },
                secondaryText = {
                    Text(text = "$balancePostMix · $postMixUtxos UTXOs")
                }
            )
        }
        Box(
            modifier = Modifier
                .align(alignment = CenterHorizontally)
                .padding(bottom = 16.dp)
        ) {
            val isValid = if (accountType == SamouraiAccountIndex.DEPOSIT) depositUtxos.isNotEmpty() else postMixUtxos.isNotEmpty()
            if (isValid) {
                Button(
                    onClick = {
                        transactionViewModel.setPage(1)
                    },
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        backgroundColor = samouraiAccent,
                        contentColor = Color.White
                    ),
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChooseCahootsType(onClose: (() -> Unit)? = null) {
    val transactionViewModel = viewModel<CahootsTransactionViewModel>()
    val selectedCahootType by transactionViewModel.cahootsTypeLive.observeAsState()

    val density = LocalDensity.current
    var cahootsType by remember {
        mutableStateOf<CahootsType?>(null);
    }
    val titleOffset: Dp by animateDpAsState(
        if (cahootsType == null) 8.dp else 44.dp,
    )
    LaunchedEffect(true) {

    }
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
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = CenterHorizontally,
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 18.dp)
            ) {
                ListItem(
                    icon = {
                        RadioButton(selected = cahootsType == CahootsType.STONEWALLX2, onClick = {
                            cahootsType = CahootsType.STONEWALLX2
                        })
                    },
                    modifier = Modifier
                        .clickable {
                            cahootsType = CahootsType.STONEWALLX2
                        },
                    text = {
                        Text(
                            text = "STONEWALLx2",
                            fontSize = 14.sp,
                        )
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            text = "You initiate a transaction to a third party with\n" +
                                    "the help of a collaborator to create a high\n" +
                                    "entropy transaction",
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                )
                ListItem(
                    icon = {
                        RadioButton(selected = cahootsType == CahootsType.STOWAWAY, onClick = {
                            cahootsType = CahootsType.STOWAWAY
                        })
                    },
                    modifier = Modifier
                        .clickable {
                            cahootsType = CahootsType.STOWAWAY
                        },
                    text = {
                        Text(
                            text = "Stowaway",
                            fontSize = 14.sp,
                        )
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            text = "You initiate a transaction to the collaborator\n" +
                                    "while making use of their UTXOs to create a\n" +
                                    "type of swap transaction.",
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                )

            }
        }
        WrapToolsPageAnimation(visible = cahootsType != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 18.dp)
            ) {
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val typeInPerson = if (CahootsType.STONEWALLX2 == cahootsType) CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_MANUAL else CahootsTransactionViewModel.CahootTransactionType.STOWAWAY_MANUAL
                            transactionViewModel.setCahootType(typeInPerson)
                            onClose?.invoke()
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
                            fontSize = 14.sp,
                        )
                    },
                    secondaryText = {
                        Text(
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                            text = stringResource(id = R.string.manually_compose_this_transaction)
                        )
                    }
                )
                Divider()
                ListItem(
                    modifier = Modifier
                        .clickable {
                            val typeInPerson = if (CahootsType.STONEWALLX2 == cahootsType) CahootsTransactionViewModel.CahootTransactionType.STONEWALLX2_SOROBAN else CahootsTransactionViewModel.CahootTransactionType.STOWAWAY_SOROBAN
                            transactionViewModel.setCahootType(typeInPerson)
                            onClose?.invoke()
                        },
                    text = {
                        Text(
                            text = "Online",
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
                            text = stringResource(id = R.string.compose_this_transaction_online_with)
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

@Preview
@Composable
fun ChooseCahootsTypePreview() {
    ChooseCahootsType()
}
