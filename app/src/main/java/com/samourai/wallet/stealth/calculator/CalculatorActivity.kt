package com.samourai.wallet.stealth.calculator

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ListItem
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.R
import com.samourai.wallet.stealth.StealthModeController
import com.samourai.wallet.stealth.stealthTapListener


class CalculatorActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorComposeView()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CalculatorStealthAppSettings() {
    val secondaryColor = Color(0xffbab9b9)

    Box(
        Modifier
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 12.dp),
        ) {
            Text(stringResource(R.string.instructions), style = androidx.compose.material.MaterialTheme.typography.h6)
            ListItem(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .padding(top = 8.dp),
                text = {
                    Text( stringResource(R.string.enable_stealth_mode), style = MaterialTheme.typography.titleSmall, color = Color.White)
                },
                secondaryText = {
                    Text(
                        stringResource(id = R.string.upon_exiting_samourai_wallet_stealth_mode),
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor)
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ListItem(
                modifier = Modifier.padding(vertical = 8.dp),
                text = {
                    Text(stringResource(R.string.disable_stealth_mode),style = MaterialTheme.typography.titleSmall, color = Color.White)
                },
                secondaryText = {
                    Text("Tap = symbol 5 times",
                        color = secondaryColor,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CalculatorComposeView() {
    SamouraiCalcTheme {
        val activity = LocalView.current.context as Activity
        val backgroundArgb = MaterialTheme.colorScheme.background.toArgb()
        activity.window.statusBarColor = backgroundArgb
        val viewModel = viewModel<CalculatorViewModel>()
        val state = viewModel.state
        val buttonSpacing = 8.dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                verticalArrangement = Arrangement.spacedBy(buttonSpacing),
            ) {
                val calcPreview = state.number1 + (state.operator?.symbol ?: "") + state.number2
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = calcPreview,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        fontWeight = FontWeight.Light,
                        fontSize = 74.sp,
                        color = Color.White,
                        maxLines = 2
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorButton(
                        symbol = "AC",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier
                            .aspectRatio(2f)
                            .weight(2f)
                    ) {
                        viewModel.onAction(CalculatorAction.Clear)
                    }
                    CalculatorButton(
                        symbol = "",
                        icon = ImageVector.vectorResource(id = R.drawable.ic_backspace_white_24dp),
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Delete)
                    }
                    CalculatorButton(
                        symbol = "/",
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Operation(CalculatorOperation.Divide))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorButton(
                        symbol = "7",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(7))
                    }
                    CalculatorButton(
                        symbol = "8",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(8))
                    }
                    CalculatorButton(
                        symbol = "9",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(9))
                    }
                    CalculatorButton(
                        symbol = "",
                        icon = Icons.Default.Clear,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Operation(CalculatorOperation.Multiply))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorButton(
                        symbol = "4",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(4))
                    }
                    CalculatorButton(
                        symbol = "5",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(5))
                    }
                    CalculatorButton(
                        symbol = "6",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(6))
                    }
                    CalculatorButton(
                        symbol = "",
                        icon = ImageVector.vectorResource(id = R.drawable.ic_baseline_remove_24),
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Operation(CalculatorOperation.Subtract))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorButton(
                        symbol = "1",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(1))
                    }
                    CalculatorButton(
                        symbol = "2",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(2))
                    }
                    CalculatorButton(
                        symbol = "3",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(3))
                    }
                    CalculatorButton(
                        symbol = "",
                        icon = Icons.Default.Add,
                        color = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Operation(CalculatorOperation.Add))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
                ) {
                    CalculatorButton(
                        symbol = "0",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(2f)
                            .weight(2f)
                    ) {
                        viewModel.onAction(CalculatorAction.Number(0))
                    }
                    CalculatorButton(
                        symbol = ".",
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.04f),
                        textColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .weight(1f)
                    ) {
                        viewModel.onAction(CalculatorAction.Decimal)
                    }
                    CalculatorButton(
                        symbol = "=",
                        color = MaterialTheme.colorScheme.inversePrimary,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .stealthTapListener(
                                click = {
                                    viewModel.onAction(CalculatorAction.Calculate)
                                },
                                onTapCallBack = {
                                    StealthModeController.enableStealth(StealthModeController.StealthApp.SAMOURAI, activity)
                                }
                            )
                            .weight(1f)
                    ) {

                    }
                }
            }
        }
    }
}


@ExperimentalComposeUiApi
@Composable
fun CalculatorButton(
    symbol: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    textColor: Color = Color.White,
    textStyle: TextStyle = TextStyle(),
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(color)
            .defaultMinSize(54.dp)
            .clickable {
                onClick()
            }
            .then(modifier)
    ) {
        if (symbol == "") {
            if (icon != null) {
                androidx.compose.material.Icon(
                    imageVector = icon,
                    tint = textColor,
                    contentDescription = ""
                )
            }
        } else {
            Text(
                text = symbol,
                style = textStyle,
                fontSize = 36.sp,
                color = textColor
            )
        }
    }
}


@Preview(widthDp = 300, heightDp = 400)
@Composable
fun CalculatorComposePreview() {
    CalculatorComposeView()
}

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun SamouraiCalcTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicDarkColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
