package com.samourai.wallet.collaborate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.cahoots.CahootsMode
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.constants.SamouraiAccountIndex
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.theme.samouraiError
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.theme.samouraiTextSecondary
import com.samourai.wallet.util.func.FormatsUtil


@Composable
fun InitiateSegment(
    onCollaboratorClick: () -> Unit,
    onCahootTypeSelection: () -> Unit,
    setUpTransaction: () -> Unit
) {
    val collaborateViewModel = viewModel<CahootsTransactionViewModel>()
    val collaboratorPcode by collaborateViewModel.collaboratorPcodeLive.observeAsState()
    val feeRate by collaborateViewModel.getFeeSatsValueLive().observeAsState("")
    val validTransaction by collaborateViewModel.validTransactionLive.observeAsState(false)
    val cahootType by collaborateViewModel.cahootsTypeLive.observeAsState()
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }


    Box(modifier = Modifier.fillMaxHeight()) {
        LazyColumn(
            verticalArrangement =  Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(1f)
                        .padding(
                            vertical = 12.dp,
                            horizontal = 14.dp
                        ),
                    Arrangement.SpaceBetween
                ) {
                    Column() {
                        TransactionOptionSegment(
                            title = "Transaction type",
                            modifier = Modifier.fillMaxWidth(),
                            showSubSection = cahootType != null,
                            onClick = onCahootTypeSelection,
                            subSection = {
                                Text(text = "${cahootType?.cahootsType} (${cahootType?.cahootsMode})", fontSize = 13.sp)
                            }
                        )

                        Divider()
                        var enableCollabSelection = cahootType != null
                        if ((cahootType?.cahootsType == CahootsType.STONEWALLX2 || cahootType?.cahootsType == CahootsType.STOWAWAY) && cahootType?.cahootsMode == CahootsMode.MANUAL) {
                            enableCollabSelection = false
                        }
                        if (enableCollabSelection) {
                            TransactionOptionSegment(
                                title = "Collaborator",
                                modifier = Modifier.fillMaxWidth(),
                                showSubSection = collaboratorPcode != null,
                                onClick = onCollaboratorClick,
                                subSection = {
                                    if (collaboratorPcode != null)
                                        PaynymAvatar(collaboratorPcode)
                                }
                            )
                            Divider()
                        }
                        var enableTransaction = cahootType != null && collaboratorPcode != null
                        if ((cahootType?.cahootsType == CahootsType.STONEWALLX2 || cahootType?.cahootsType == CahootsType.STOWAWAY) && cahootType?.cahootsMode == CahootsMode.MANUAL) {
                            enableTransaction = true
                        }
                        if (enableTransaction && !validTransaction) {
                            TransactionOptionSegment(
                                title = "Set up transaction",
                                modifier = Modifier.fillMaxWidth(),
                                showSubSection = false,
                                onClick = setUpTransaction,
                            )
                            Divider()
                        }
                        TransactionPreview(
                            onClick = setUpTransaction
                        )
                    }

                }
            }
            item {
                if (validTransaction) {
                    if (showClearDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showClearDialog = false
                            },
                            title = {
                                Text(text = stringResource(id = R.string.confirm))
                            },
                            text = {
                                Text("Do you want to discard?")
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showClearDialog = false
                                    collaborateViewModel.clearTransaction()
                                })
                                { Text(text = "Discard") }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showClearDialog = false
                                })
                                { Text(text = "Cancel") }
                            }
                        )
                    }
                    Button(
                        onClick = {
                            collaborateViewModel.send(context)
                        },
                        enabled = validTransaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 4.dp)
                            .padding(top = 0.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = samouraiSuccess,
                            contentColor = Color.White
                        ),
                    ) {
                        Text("BEGIN TRANSACTION")
                    }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showClearDialog = true
                            }, colors = ButtonDefaults.textButtonColors(
                                backgroundColor = Color.Transparent,
                                contentColor = samouraiError
                            )
                        ) {
                            Text("Clear", textAlign = TextAlign.Center, fontSize = 12.sp)
                        }
                    }
                }

            }
        }
    }
}


@Composable
fun TransactionPreview(onClick: () -> Unit) {
    val collaborateViewModel = viewModel<CahootsTransactionViewModel>()
    val validTransaction by collaborateViewModel.validTransactionLive.observeAsState(false)
    val account by collaborateViewModel.transactionAccountTypeLive.observeAsState(SamouraiAccountIndex.DEPOSIT)
    val destinationAddress by collaborateViewModel.destinationAddressLive.observeAsState(null)
    val amount by collaborateViewModel.amountLive.observeAsState(0L)
    val cahootType by collaborateViewModel.cahootsTypeLive.observeAsState()
    val feeRate by collaborateViewModel.getFeeSatsValueLive().observeAsState("")


    if (validTransaction) {
        TransactionOptionSegment(
            title = "Account",
            showSubSection = false,
            onClick = onClick,
            showSubSectionText =
            if (account == SamouraiAccountIndex.DEPOSIT) stringResource(id = R.string.deposit_account)
            else stringResource(id = R.string.postmix_account),
        )
        Divider()
        if (!(cahootType?.cahootsType == CahootsType.STOWAWAY && cahootType?.cahootsMode == CahootsMode.MANUAL)) {
            if (FormatsUtil.getInstance().isValidPaymentCode(destinationAddress)) {
                TransactionOptionSegment(
                        title = "Destination",
                        showSubSection = true,
                    modifier = Modifier.fillMaxWidth(),

                    onClick = onClick,
                        subSection = {
                            if (destinationAddress != null)
                                PaynymAvatar(destinationAddress)
                        }
                )
                Divider()
            } else {
                TransactionOptionSegment(
                        title = "Destination",
                        showSubSection = false,
                    modifier = Modifier.fillMaxWidth(),

                    onClick = onClick,
                        showSubSectionText = if (destinationAddress != null) destinationAddress!! else ""
                )
                Divider()
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            TransactionOptionSegment(
                title = "Amount to send",
                showSubSection = false,
                modifier = Modifier.fillMaxWidth(.5f),
                onClick = onClick,
                showSubSectionText = FormatsUtil.formatBTC(amount)
            )
            TransactionOptionSegment(
                title = "Fee rate",
                showSubSection = false,
                modifier = Modifier.fillMaxWidth(.5f),
                onClick = onClick,
                textAlign = TextAlign.End,
                showSubSectionText = "${feeRate} sat/b"
            )
        }
        Divider()
    }

}

@Composable
fun TransactionOptionSegment(
    title: String,
    modifier: Modifier=Modifier,
    textAlign: TextAlign=TextAlign.Start,
    showSubSectionText: String = "None selected",
    onClick: () -> Unit,
    showSubSection: Boolean = false,
    subSection: @Composable () -> Unit = {}
) {
    Column(
        modifier
            .clickable {
                onClick()
            }
            .padding(vertical = 9.dp)
           ) {
        Text(text = title, color = samouraiTextSecondary, fontSize = 13.sp,textAlign=textAlign)
        Box(Modifier.padding(vertical = 4.dp)) {
            if (!showSubSection) {
                Text(text = showSubSectionText, fontSize = 13.sp,textAlign= textAlign)
            } else {
                subSection()
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 620, widthDp = 420)
@Composable
fun DefaultPreview2() {
    SamouraiWalletTheme {
        CollaborateScreen(null, false)
    }
}

