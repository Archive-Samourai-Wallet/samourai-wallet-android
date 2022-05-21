import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.client.android.Contents
import com.google.zxing.client.android.encode.QRCodeEncoder
import com.samourai.wallet.R
import com.samourai.wallet.theme.samouraiAccent
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.theme.samouraiTextSecondary
import com.samourai.wallet.theme.samouraiWindow
import com.samourai.wallet.tools.AddressCalculatorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


private const val ProgressThreshold = 0.35f

private val Int.ForOutgoing: Int
    get() = (this * ProgressThreshold).toInt()

private val Int.ForIncoming: Int
    get() = this - this.ForOutgoing


@Composable
fun AddressCalculator(onDismiss: () -> Unit = {}) {

    var page by remember { mutableStateOf(0) }
    var previewAddress by remember { mutableStateOf("") }
    var previewTitle by remember { mutableStateOf("") }

    Box(modifier = Modifier.requiredHeight( 420.dp)) {
        AnimatedVisibility(
            visible = 0 == page,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 350.ForIncoming,
                    delayMillis = 2.ForOutgoing,
                    easing = LinearOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 350.ForOutgoing,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing
                )
            )
        ) {
            Column(Modifier.background(samouraiBottomSheetBackground)) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = "Address Calculator", fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    },
                )
                AddressCalcForm(
                    onAdvanceClick = {
                        page = 1
                    }
                )
            }
        }
        AnimatedVisibility(
            visible = 1 == page,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 350.ForIncoming,
                    delayMillis = 2.ForOutgoing,
                    easing = LinearOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 350.ForOutgoing,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing
                )
            )
        ) {
            Column(
                Modifier
                    .background(samouraiBottomSheetBackground)
            ) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = "Address Calculator", fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    }, navigationIcon = {
                        IconButton(onClick = {
                            page = 0
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24), tint = samouraiAccent,
                                contentDescription = ""
                            )
                        }
                    }
                )
                Box(modifier = Modifier.fillMaxSize()) {
                    AddressDetails(onSelect = { it, title ->
                        previewAddress = it
                        previewTitle = title
                        page = 2
                    })
                }
            }
        }
        AnimatedVisibility(
            visible = 2 == page,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 350.ForIncoming,
                    delayMillis = 2.ForOutgoing,
                    easing = LinearOutSlowInEasing
                )
            ),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 350.ForOutgoing,
                    delayMillis = 0,
                    easing = FastOutLinearInEasing
                )
            )
        ) {
            AddressQRPreview(previewAddress, previewTitle, onDismiss = {
                page = 1
            })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddressCalcForm(onAdvanceClick: () -> Unit) {
    val context = LocalContext.current;
    val vm = viewModel<AddressCalculatorViewModel>()
    val addressDetails by vm.getAddressLiveData().observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var index by remember { mutableStateOf("${addressDetails?.selectedIndex}") }
    var isExternal by remember { mutableStateOf(addressDetails?.isExternal ?: true) }
    val types = stringArrayResource(id = R.array.account_types)
    var selectedType by remember { mutableStateOf(addressDetails?.keyType ?: types.first()) }

    fun applyChanges() {
        vm.calculateAddress(
            type = selectedType,
            index = if (index.isEmpty().or(index.isBlank())) 0 else index.toInt(),
            isExternal = isExternal,
            context = context,
        )
    }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DropDownTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.6f),
                label = stringResource(id = R.string.address_type),
                value = selectedType,
                onOptionSelected = {
                    selectedType = it
                    applyChanges()
                },
                options = types.toList()
            )
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 4.dp)
                ,
                value = index,
                textStyle = TextStyle(fontSize = 12.sp),
                keyboardOptions = KeyboardOptions(
                    autoCorrect = false,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Number,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    applyChanges()
                    keyboardController?.hide()
                }),
                onValueChange = {
                    index = it
                },

                label = {
                    Text(
                        "Address Index", fontSize = 12.sp
                    )
                },
            )
        }
        Box(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Current Index: ${addressDetails?.currentIndex}", textAlign = TextAlign.End,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Box(modifier = Modifier.padding(vertical = 2.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = isExternal, onClick = {
                isExternal = true
                applyChanges()
            })
            Text("Receive address (external)", Modifier.clickable {
                isExternal = true
                applyChanges()
            })
        }
        Box(modifier = Modifier.padding(vertical = 2.dp))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = !isExternal, onClick = {
                isExternal = false
                applyChanges()
            })
            Text("Change address (internal)", Modifier.clickable {
                isExternal = false
                applyChanges()
            })
        }
        Text(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            text = addressDetails?.pubKey ?: ""
        )
        Button(
            onClick = {
                onAdvanceClick()
            }, Modifier
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 12.dp),
            colors = ButtonDefaults.textButtonColors(
                backgroundColor = samouraiAccent,
                contentColor = Color.White
            )
        ) {
            Text("Address Details")
        }
        Box(modifier = Modifier.padding(24.dp))
    }

}

@Composable
fun AddressDetails(onSelect: (address: String, title: String) -> Unit) {

    val vm = viewModel<AddressCalculatorViewModel>()
    val addressDetails by vm.getAddressLiveData().observeAsState()

    Column(
        modifier = Modifier
            .fillMaxHeight(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect(addressDetails?.pubKey ?: "", "Receive address (external)")
                }
                .padding(horizontal = 40.dp, vertical = 24.dp),

            ) {
            Text(
                "Receive address (external)",
                style = MaterialTheme.typography.subtitle1,
                fontSize = 16.sp
            )
            Text(
                text = addressDetails?.pubKey ?: "",
                fontSize = 12.sp,
                style = MaterialTheme.typography.caption,
                color = samouraiTextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }
        Column(
            Modifier
                .clickable {
                    onSelect(addressDetails?.privateKey ?: "", "Private key")
                }
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = "Private key",
                style = MaterialTheme.typography.subtitle1,
                fontSize = 16.sp,
                color = Color.White
            )
            Text(
                text = addressDetails?.privateKey ?: "",
                style = MaterialTheme.typography.caption,
                color = samouraiTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Column(
            Modifier
                .clickable {
                    onSelect(addressDetails?.redeemScript ?: "", "Redeem Script")
                }
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxWidth()
        ) {
            Text(
                text = "Redeem Script",
                style = MaterialTheme.typography.subtitle1,
                fontSize = 16.sp
            )
            Text(
                text = addressDetails?.redeemScript ?: "",
                style = MaterialTheme.typography.caption,
                color = samouraiTextSecondary,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }


    }
}

@Composable
fun AddressQRPreview(previewAddress: String, previewTitle: String, onDismiss: () -> Unit) {
    var bitmap by rememberSaveable {
        mutableStateOf<ImageBitmap?>(null)
    }
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    LaunchedEffect(previewAddress) {
        withContext(Dispatchers.Default) {
            val qrCodeEncoder = QRCodeEncoder(previewAddress, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500)
            qrCodeEncoder.setMargin(1)
            bitmap = qrCodeEncoder.encodeAsBitmap().asImageBitmap()
        }
    }
    val context = LocalContext.current
    var openDialog by remember { mutableStateOf(false) }

    Column(Modifier.background(samouraiBottomSheetBackground)) {
        if (openDialog) {
            AlertDialog(
                shape = RoundedCornerShape(8.dp),
                backgroundColor = samouraiWindow,
                contentColor = Color.White,
                title = {
                    Text(text = stringResource(id = R.string.app_name), color = Color.White.copy(alpha = 0.8f))
                },
                text = {
                    Text(text = stringResource(id = R.string.copy_warning_generic), color = Color.White.copy(alpha = 0.8f))
                },
                onDismissRequest = {
                    openDialog = false
                },
                confirmButton = {
                    TextButton(onClick = {
                        clipboardManager.setText(AnnotatedString(previewAddress))
                        openDialog = false
                        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()

                    })
                    {
                        Text(
                            text = stringResource(id = R.string.ok),
                            color = samouraiAccent, fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        openDialog = false
                    })
                    {
                        Text(
                            text = stringResource(
                                id = R.string.cancel
                            ),color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
        TopAppBar(
            elevation = 0.dp,
            backgroundColor = samouraiBottomSheetBackground,
            actions = {
                IconButton(onClick = {
                    openDialog = true
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_content_copy_24),
                        contentDescription = "Copy"
                    )
                }
            },
            title = {
                Text(text = previewTitle, fontSize = 13.sp, color = samouraiAccent)
            }, navigationIcon = {
                IconButton(onClick = {
                    onDismiss()
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                        tint = samouraiAccent,
                        contentDescription = "Back button"
                    )
                }
            }
        )
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = previewAddress,
                Modifier
                    .padding(vertical = 8.dp, horizontal = 24.dp),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            if (bitmap != null) Box(
                modifier = Modifier
                    .padding(12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .align(alignment = CenterHorizontally)
                    .background(Color.White)
            ) {
                Image(
                    bitmap = bitmap!!, contentDescription = "",
                    Modifier
                        .requiredSize(240.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            else {
                Box(modifier = Modifier.requiredSize(250.dp))
            }
        }
    }

}

@Preview(showBackground = true, widthDp = 420)
@Composable
fun AddressCalculatorPreview() {
    AddressCalculator()
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropDownTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    onOptionSelected: (value: String) -> Unit,
    options: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(value) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        }
    ) {
        TextField(
            readOnly = true,
            modifier = modifier,
            value = selectedOptionText,
            textStyle = TextStyle(fontSize = 11.sp),
            onValueChange = {
            },
            label = {
                Text(
                    label, fontSize = 12.sp
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = expanded
                )
            },
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
            }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    onClick = {
                        selectedOptionText = selectionOption
                        expanded = false
                        onOptionSelected(selectedOptionText)
                    }
                ) {
                    Text(text = selectionOption, fontSize = 12.sp)
                }
            }
        }
    }

}

