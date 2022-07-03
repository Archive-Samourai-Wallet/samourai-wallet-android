package com.samourai.wallet.collaborate

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.theme.SamouraiWalletTheme
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

    Column(
        modifier = Modifier
            .padding(
                vertical = 12.dp,
                horizontal = 14.dp
            )
            .fillMaxHeight(),
        Arrangement.SpaceBetween
    ) {

        Column {
            TransactionOptionSegment(
                title = "Transaction type",
                showSubSection = cahootType != null,
                onClick = onCahootTypeSelection,
                subSection = {
                    Text(text = "${cahootType?.cahootsType} (${cahootType?.cahootsMode})", fontSize = 13.sp)
                }
            )

            Divider()
            if (cahootType != null) {
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
            if (cahootType != null && collaboratorPcode != null && !validTransaction) {
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



@Composable
fun TransactionPreview(onClick: () -> Unit) {
    val collaborateViewModel = viewModel<CahootsTransactionViewModel>()
    val validTransaction by collaborateViewModel.validTransactionLive.observeAsState(false)
    val account by collaborateViewModel.transactionAccountTypeLive.observeAsState(SamouraiAccountIndex.DEPOSIT)
    val destinationAddress by collaborateViewModel.destinationAddressLive.observeAsState(null)
    val amount by collaborateViewModel.amountLive.observeAsState(0.0)

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
            showSubSectionText = FormatsUtil.formatBTC(amount.times(1e8).toLong())
        )
        Divider()

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
        CollaborateScreen(null)
    }
}

