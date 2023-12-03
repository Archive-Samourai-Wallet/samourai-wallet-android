package com.samourai.wallet.home

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import com.samourai.wallet.R
import com.samourai.wallet.SamouraiActivity
import com.samourai.wallet.send.SendActivity
import com.samourai.wallet.theme.samouraiWindow
import com.samourai.wallet.util.func.BalanceUtil
import com.samourai.wallet.util.func.FormatsUtil.formatBTC
import com.samourai.whirlpool.client.wallet.beans.SamouraiAccountIndex
import org.apache.commons.lang3.StringUtils
import java.util.Objects.nonNull

class AccountSelectionActivity : SamouraiActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.statusBarColor = resources.getColor(R.color.samouraiWindow)
        } else {
            window.statusBarColor = getColor(R.color.samouraiWindow)
        }

        setContent {
            ComposeActivityContent(activity = this)
        }
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
fun ComposeActivityContent(activity: SamouraiActivity?) {

    var depositBalance = BalanceUtil.getBalance(SamouraiAccountIndex.DEPOSIT, activity)
    var postmixBalance = BalanceUtil.getBalance(SamouraiAccountIndex.POSTMIX, activity)

    val context = LocalContext.current
    val items = listOf(
        Triple(SamouraiAccountIndex.DEPOSIT, R.drawable.ic_deposit_account, Color(110, 118, 137)),
        Triple(SamouraiAccountIndex.POSTMIX, R.drawable.ic_postmix_account, Color(59, 105, 244)))

    val currentIntent: Intent? = if (nonNull(activity)) activity!!.intent else null;
    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )

    Surface(
        color = samouraiWindow
    ) {
        Column {
            Column (
                modifier = Modifier
                    .weight(0.10f)
                    .fillMaxWidth()
            ) {
                Row (

                ) {

                    Column(
                        modifier = Modifier
                            .weight(0.17f)
                            .padding(20.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = { activity!!.onBackPressed() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close_white_24dp),
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                    }

                    Column(
                        modifier = Modifier
                            .weight(0.83f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 12.dp),

                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(R.string.select_spending_account),
                                color = Color.White,
                                fontSize = 20.sp,
                                fontFamily = robotoMediumBoldFont
                            )
                        }

                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(0.90f)
                    .fillMaxSize()
            ) {
                items.forEach { item ->
                    val isPostmixAccount = item.first == SamouraiAccountIndex.POSTMIX;
                    val balance =
                        if (isPostmixAccount) BalanceUtil.getBalance(SamouraiAccountIndex.POSTMIX, activity)
                        else BalanceUtil.getBalance(SamouraiAccountIndex.DEPOSIT, activity);
                    itemRow(item = item, balance = balance) {

                        val intent = Intent(context, SendActivity::class.java)
                        if (nonNull(currentIntent)) {
                            if (currentIntent!!.hasExtra("uri")) {
                                intent.putExtra("uri", currentIntent!!.getStringExtra("uri"))
                            }
                            intent.putExtra(
                                "via_menu",
                                currentIntent!!.getBooleanExtra("via_menu", false))
                        }

                        if (isPostmixAccount) {
                            intent.putExtra("_account", SamouraiAccountIndex.POSTMIX)
                        } else {
                            intent.putExtra("_account", SamouraiAccountIndex.DEPOSIT)
                        }

                        startActivity(context, intent, null)
                    }
                }
            }
        }
    }
}


@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
fun itemRow(item: Triple<Int, Int, Color>, balance: Long, onItemClick: () -> Unit) {

    val robotoMediumBoldFont = FontFamily(
        Font(R.font.roboto_medium, FontWeight.Bold)
    )
    val robotoMediumNormalFont = FontFamily(
        Font(R.font.roboto_light, FontWeight.Normal)
    )

    Row (
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .padding(vertical = 16.dp)
            .background(Color.Transparent)
            .clickable { onItemClick.invoke() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column (
            modifier = Modifier
                .weight(0.28f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box (
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.third)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = item.second),
                    contentDescription = "Clear",
                    tint = Color.White
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(0.62f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                Text(
                    text = getAccountName(item.first),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = robotoMediumBoldFont
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBTC(balance),
                    color = Color(184, 184, 184),
                    fontFamily = robotoMediumNormalFont
                )
            }

        }
        Column(
            modifier = Modifier
                .weight(0.12f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun getAccountName(account: Int): String {
    if (account == SamouraiAccountIndex.POSTMIX) {
        return stringResource(id = R.string.postmix_account);
    } else if (account == SamouraiAccountIndex.DEPOSIT) {
        return stringResource(id = R.string.deposit_account);
    } else {
        return StringUtils.EMPTY;
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ComposeActivityContent(null)
}