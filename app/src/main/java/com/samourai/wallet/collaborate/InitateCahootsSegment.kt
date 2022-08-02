package com.samourai.wallet.collaborate

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.theme.samouraiError
import com.samourai.wallet.theme.samouraiSuccess
import com.samourai.wallet.theme.samouraiTextSecondary
import com.samourai.wallet.util.FormatsUtil
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex


@Composable
fun InitiateSegment(
    onCollaboratorClick: () -> Unit,
    onCahootTypeSelection: () -> Unit,
    setUpTransaction: () -> Unit
) {
    val collaborateViewModel = viewModel<CahootsTransactionViewModel>()
    val collaboratorPcode by collaborateViewModel.collaboratorPcodeLive.observeAsState()
    val validTransaction by collaborateViewModel.validTransactionLive.observeAsState(false)
    val cahootType by collaborateViewModel.cahootsTypeLive.observeAsState()
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxHeight()) {
        LazyColumn(
            verticalArrangement =  Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.9f)
                        .padding(
                            vertical = 12.dp,
                            horizontal = 14.dp
                        ),
                    Arrangement.SpaceBetween
                ) {
                    Column() {
                        TransactionOptionSegment(
                            title = "Transaction type",
                            showSubSection = cahootType != null,
                            onClick = onCahootTypeSelection,
                            subSection = {
                                Text(text = "${cahootType?.cahootsType} (${cahootType?.cahootsMode})", fontSize = 13.sp)
                            }
                        )

                        Divider()
                        var enableCollabSelection = cahootType != null
                        if (cahootType?.cahootsType == CahootsType.STONEWALLX2 && cahootType?.cahootsMode == CahootsMode.MANUAL) {
                            enableCollabSelection = false
                        }
                        if (enableCollabSelection) {
                            TransactionOptionSegment(
                                title = "Collaborator",
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
                        if (cahootType?.cahootsType == CahootsType.STONEWALLX2 && cahootType?.cahootsMode == CahootsMode.MANUAL) {
                            enableTransaction = true
                        }
                        if (enableTransaction && !validTransaction) {
                            TransactionOptionSegment(
                                title = "Set up transaction",
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
                    Button(
                        onClick = {
                            collaborateViewModel.send(context)
                        },
                        enabled = validTransaction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                        colors = ButtonDefaults.textButtonColors(
                            backgroundColor = samouraiSuccess,
                            contentColor = Color.White
                        ),
                    ) {
                        Text("BEGIN TRANSACTION")
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
    var showClearDialog by remember { mutableStateOf(false) }
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
    if (validTransaction) {
        TransactionOptionSegment(
            title = "Account",
            showSubSection = false,
            onClick = onClick,
            showSubSectionText = if (account == SamouraiAccountIndex.DEPOSIT) "Deposit account" else "Postmix account",
        )
        Divider()
        if (FormatsUtil.getInstance().isValidPaymentCode(destinationAddress)) {
            TransactionOptionSegment(
                title = "Destination",
                showSubSection = true,
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
                onClick = onClick,
                showSubSectionText = if (destinationAddress != null) destinationAddress!! else ""
            )
            Divider()
        }
        TransactionOptionSegment(
            title = "Amount to send",
            showSubSection = false,
            onClick = onClick,
            showSubSectionText = FormatsUtil.formatBTC(amount)
        )
        Divider()
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

@Composable
fun TransactionOptionSegment(
    title: String,
    showSubSectionText: String = "None selected",
    onClick: () -> Unit,
    showSubSection: Boolean = false,
    subSection: @Composable () -> Unit = {}
) {
    Column(
        Modifier
            .clickable {
                onClick()
            }
            .padding(vertical = 12.dp)
            .fillMaxWidth()) {
        Text(text = title, color = samouraiTextSecondary, fontSize = 13.sp)
        Box(Modifier.padding(vertical = 4.dp)) {
            if (!showSubSection) {
                Text(text = showSubSectionText, fontSize = 13.sp)
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

