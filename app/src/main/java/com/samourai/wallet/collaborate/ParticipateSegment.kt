package com.samourai.wallet.collaborate

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.api.APIFactory
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CollaborateViewModel
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiTextSecondary
import com.samourai.wallet.util.FormatsUtil
import com.samourai.wallet.util.PrefsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ParticipateSegment() {
    val context = LocalContext.current
    val collaborateViewModel = viewModel<CollaborateViewModel>()
    val timeout by collaborateViewModel.sorobanTimeoutLive.observeAsState(null)
    val accountType by collaborateViewModel.meetingAccountLive.observeAsState()
    val walletBalanceTicker by APIFactory.getInstance(context).walletBalanceObserverLiveData.observeAsState()
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
        depositUtxos = "${APIFactory.getInstance(context).getUtxos(true).size}"
        postMixUtxos = "${APIFactory.getInstance(context).getUtxosPostMix(true).size}"
    }

    Box(modifier = Modifier) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.height(300.dp),
        ) {
            Box(modifier = Modifier.padding(12.dp)) {
                Text(text = "Account Selection", fontSize = 13.sp, color = samouraiTextSecondary)
            }
            Column(
                modifier = Modifier
                    .gesturesDisabled(timeout != null)
                    .padding(horizontal = 4.dp),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                ListItem(
                    modifier = Modifier
                        .clickable {
                            collaborateViewModel.setMeetingAccountIndex(SamouraiAccountIndex.DEPOSIT)
                        },
                    icon = {
                        RadioButton(selected = accountType == SamouraiAccountIndex.DEPOSIT, onClick = {
                            collaborateViewModel.setMeetingAccountIndex(SamouraiAccountIndex.DEPOSIT)
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
                            collaborateViewModel.setMeetingAccountIndex(SamouraiAccountIndex.POSTMIX)
                        },
                    icon = {
                        RadioButton(selected = accountType == SamouraiAccountIndex.POSTMIX, onClick = {
                            collaborateViewModel.setMeetingAccountIndex(SamouraiAccountIndex.POSTMIX)
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
                    .align(alignment = Alignment.CenterHorizontally)
                    .padding(bottom = 16.dp)
            ) {
                ListenCahootsButton()
            }
            CahootsRequestDialog()
        }
    }
}

@Composable
fun ListenCahootsButton() {
    val collaborateViewModel = viewModel<CollaborateViewModel>()
    val listenState by collaborateViewModel.cahootsListenStateLive.observeAsState(initial = CollaborateViewModel.CahootListenState.WAITING)
    val timeout by collaborateViewModel.sorobanTimeoutLive.observeAsState(initial = 0)

    Box(modifier = Modifier.height(48.dp)) {

        when (listenState) {
            CollaborateViewModel.CahootListenState.STOPPED -> {
                TextButton(onClick = {
                    collaborateViewModel.startListen()
                }) {
                    Text(text = "LISTEN FOR CAHOOTS REQUESTS", color = samouraiAccent)
                }
            }
            CollaborateViewModel.CahootListenState.WAITING -> {
                Column(
                    Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(26.dp)
                            .padding(bottom = 16.dp)
                    )
                    Text(text = "waiting for Cahoots request (${timeout})", fontSize = 12.sp)
                }
            }
            CollaborateViewModel.CahootListenState.TIMEOUT -> {
                TextButton(onClick = {
                    collaborateViewModel.startListen()
                }) {
                    Text(stringResource(id = R.string.no_cahoots_detected), fontSize = 13.sp, color = samouraiAccent)
                }
            }
        }

    }
}

fun Modifier.gesturesDisabled(disabled: Boolean = true) =
    if (disabled) {
        pointerInput(Unit) {
            awaitPointerEventScope {
                // we should wait for all new pointer events
                while (true) {
                    awaitPointerEvent(pass = PointerEventPass.Initial)
                        .changes
                        .forEach(PointerInputChange::consume)
                }
            }
        }
    } else {
        Modifier
    }

@Composable
fun CahootsRequestDialog() {
    val collaborateViewModel = viewModel<CollaborateViewModel>()
    val sorobanRequest by collaborateViewModel.sorobanRequestLive.observeAsState(null)
    val context = LocalContext.current
    if (sorobanRequest != null) {
        val nym = BIP47Meta.getInstance().getDisplayLabel(sorobanRequest!!.sender)
        val type = if (sorobanRequest!!.type == CahootsType.STOWAWAY) {
            "Stowaway"
        } else {
            "STONEWALLx2"
        }
        val feeDetails = if (sorobanRequest!!.type.isMinerFeeShared) {
            "You pay half of the miner fee"
        } else {
            "None"
        }

        val url = "${WebUtil.PAYNYM_API}${sorobanRequest!!.sender}/avatar"

        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(3),
                color = Color(0xff525252),
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(64.dp)) {
                                PicassoImage(
                                    url = url,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(100)))
                        }
                        Text(
                            text = nym,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                    Divider(color = samouraiAccent, modifier = Modifier.padding(vertical = 4.dp))

                    Column(modifier = Modifier, Arrangement.Center) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            text = "$nym wants to CAHOOT with you using a $type CoinJoin",
                            fontSize = 13.sp
                        )
                        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                            Row {
                                Text(
                                    text = "Type", Modifier
                                        .border(0.dp, samouraiTextSecondary)
                                        .weight(.3f)
                                        .padding(6.dp),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = type, Modifier
                                        .border(0.dp, samouraiTextSecondary)
                                        .weight(1.2f)
                                        .padding(6.dp),
                                    fontSize = 13.sp
                                )
                            }
                            Row(

                            ) {
                                Text(
                                    text = "Fee", Modifier
                                        .border(0.dp, samouraiTextSecondary)
                                        .weight(.3f)
                                        .padding(6.dp),
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = feeDetails,
                                    modifier = Modifier
                                        .border(0.dp, samouraiTextSecondary)
                                        .weight(1.2f)
                                        .padding(6.dp),
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        text = "Would you like to collaborate in this\n" +
                                "transaction?",
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        Arrangement.End
                    ) {

                        TextButton(onClick = {
                            collaborateViewModel.cancelSorobanRequest()
                        }) {
                            Text(text = "No", color = samouraiTextSecondary)
                        }
                        TextButton(onClick = {
                            collaborateViewModel.acceptRequest(context)
                        }) {
                            Text(text = "Yes", color = samouraiAccent)
                        }
                    }

                }
            }
        }
    }

}

@Preview(widthDp = 400, heightDp = 60)
@Composable
fun ListenCahootsButtonPreview() {
    ListenCahootsButton()
}

@Preview(widthDp = 400, heightDp = 600)
@Composable
fun ParticipateSegmentPreview() {
    SamouraiWalletTheme {
        ParticipateSegment()
    }
}

@Preview(widthDp = 400, heightDp = 480)
@Composable
fun CahootsRequestDialogPreview() {
    SamouraiWalletTheme {
        CahootsRequestDialog()
    }
}