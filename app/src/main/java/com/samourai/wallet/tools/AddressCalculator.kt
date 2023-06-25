import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import androidx.compose.ui.draw.shadow
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
import com.samourai.wallet.theme.*
import com.samourai.wallet.tools.AddressCalculatorViewModel
import com.samourai.wallet.tools.WrapToolsPageAnimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AddressCalculator(window: Window?) {
    val vm = viewModel<AddressCalculatorViewModel>()
    val page by vm.getPage().observeAsState()
    var previewAddress by remember { mutableStateOf("") }
    var previewTitle by remember { mutableStateOf("") }

    LaunchedEffect(key1 = page, block = {
        if(page == 1 || page == 2){
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }else{
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    })
    Box(modifier = Modifier.requiredHeight(400.dp)) {
        WrapToolsPageAnimation(
            visible = 0 == page,
        ) {
            Column(Modifier.background(samouraiBottomSheetBackground)) {
                TopAppBar(
                    elevation = 0.dp,
                    backgroundColor = samouraiBottomSheetBackground,
                    title = {
                        Text(
                            text = stringResource(id = R.string.options_address_calc), fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    },
                )
                AddressCalcForm()
                Button(
                    onClick = {
                        vm.setPage(1)
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
                    Text(stringResource(R.string.address_details))
                }
                Box(modifier = Modifier.padding(24.dp))
            }
        }
        WrapToolsPageAnimation(
            visible = 1 == page,
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
                            text = stringResource(id = R.string.options_address_calc), fontSize = 13.sp,
                            color = samouraiAccent
                        )
                    }, navigationIcon = {
                        IconButton(onClick = {
                            vm.setPage(0)
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
                        vm.setPage(2)
                    })
                }
            }
        }
        WrapToolsPageAnimation(
            visible = 2 == page,
        ) {
            AddressQRPreview(previewAddress, previewTitle, onDismiss = {
                vm.setPage(1)
            })
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AddressCalcForm() {
    val context = LocalContext.current;
    val vm = viewModel<AddressCalculatorViewModel>()
    val addressDetails by vm.getAddressLiveData().observeAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var index by remember { mutableStateOf("${addressDetails?.selectedIndex}") }
    var isExternal by remember { mutableStateOf(addressDetails?.isExternal ?: true) }
    val types = stringArrayResource(id = R.array.account_types)
    val selectedType = remember { mutableStateOf(addressDetails?.keyType ?: types.first()) }

    fun applyChanges() {
        vm.calculateAddress(
            type = selectedType.value,
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
                    selectedType.value = it
                    applyChanges()
                },
                options = types.toList()
            )
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(start = 4.dp),
                value = index,
                colors = TextFieldDefaults.textFieldColors(
                    backgroundColor = samouraiTextFieldBg
                ),
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
                    applyChanges()
                },
                label = {
                    Text(
                        stringResource(R.string.address_index), fontSize = 12.sp
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
            Text(stringResource(R.string.receive_address_external), Modifier.clickable {
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
            Text(stringResource(R.string.change_address_internal), Modifier.clickable {
                isExternal = false
                applyChanges()
            })
        }
        Text(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp),
            text = addressDetails?.pubKey ?: ""
        )
    }

}

@Composable
fun AddressDetails(onSelect: (address: String, title: String) -> Unit) {

    val vm = viewModel<AddressCalculatorViewModel>()
    val addressDetails by vm.getAddressLiveData().observeAsState()
    val stringReceiveAddress = stringResource(R.string.receive_address_external)
    Column(
        modifier = Modifier
            .fillMaxHeight(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onSelect(addressDetails?.pubKey ?: "", stringReceiveAddress)
                }
                .padding(horizontal = 40.dp, vertical = 24.dp),

            ) {
            Text(
                stringResource(R.string.receive_address_external),
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
        val stringPrivateKey = stringResource(R.string.private_key)
        Column(
            Modifier
                .clickable {
                    onSelect(addressDetails?.privateKey ?: "", stringPrivateKey)
                }
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.private_key),
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
        val stringRedeemScript = stringResource(R.string.redeem_script)
        Column(
            Modifier
                .clickable {
                    onSelect(addressDetails?.redeemScript ?: "", stringRedeemScript)
                }
                .padding(horizontal = 40.dp, vertical = 24.dp)
                .fillMaxWidth()
                .fillMaxWidth()
        ) {
            Text(
                text =  stringResource(R.string.redeem_script),
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
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(24.dp),
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
                            ), color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Bold
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
    AddressCalculator(null)
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DropDownTextField(
    modifier: Modifier = Modifier,
    label: String,
    value: MutableState<String>,
    onOptionSelected: (value: String) -> Unit,
    options: List<String>,
    enable: MutableState<Boolean> = mutableStateOf(true)
) {

    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(value.value) }
    var selectedItemIndex by remember { mutableStateOf(options.indexOf(value.value)) }

    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = enable.value && expanded,
        onExpandedChange = {
            expanded = enable.value && !expanded
        }
    ) {
        TextField(
            readOnly = true,
            modifier = modifier,
            value = value.value,
            textStyle = TextStyle(fontSize = 11.sp),
            onValueChange = {},
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = samouraiTextFieldBg
            ),
            label = {
                Text(
                    label, fontSize = 12.sp
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    expanded = enable.value && expanded
                )
            },
        )

        if (enable.value) {
            ExposedDropdownMenu(
                expanded = enable.value && expanded,
                onDismissRequest = {
                    expanded = false
                }
            ) {
                options.forEachIndexed { index, selectionOption ->
                    DropdownMenuItem(
                        onClick = {
                            selectedItemIndex = index
                            selectedOptionText = selectionOption
                            expanded = false
                            onOptionSelected(selectedOptionText)
                        }
                    ) {
                        Text(
                            text = selectionOption,
                            fontSize = 12.sp,
                            fontWeight = if (index == selectedItemIndex) FontWeight.Bold else null
                        )
                    }
                    Divider(color = samouraiBottomSheetBackground, thickness = 1.dp)
                }
            }
        }
    }

}


@Composable
fun getWindow(): Window? {
    val context = LocalContext.current
    if (context is AppCompatActivity) {
        return context.window;
    }
    return null
}