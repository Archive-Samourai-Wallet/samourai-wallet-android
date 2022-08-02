package com.samourai.wallet.collaborate

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.paynym.WebUtil
import com.samourai.wallet.cahoots.CahootsMode
import com.samourai.wallet.cahoots.CahootsType
import com.samourai.wallet.collaborate.viewmodels.CahootsTransactionViewModel
import com.samourai.wallet.collaborate.viewmodels.CollaborateViewModel
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.squareup.picasso.Picasso
import com.squareup.picasso.Target
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun PaynymChooser(paynymChooser: ModalBottomSheetState?, onClose: () -> Unit) {
    val collaborateViewModel = viewModel<CollaborateViewModel>()
    val cahootsTransactionViewModel = viewModel<CahootsTransactionViewModel>()
    val cahootType by cahootsTransactionViewModel.cahootsTypeLive.observeAsState()
    val following by collaborateViewModel.following.observeAsState(initial = listOf())
    val loading by collaborateViewModel.loadingLive.observeAsState(false)
    var enableSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var paynyms = arrayListOf<String>().apply { addAll(following) }
    if (cahootType?.cahootsMode == CahootsMode.SOROBAN && cahootType?.cahootsType != CahootsType.STOWAWAY) {
        paynyms = arrayListOf<String>().apply {
            add(BIP47Meta.getMixingPartnerCode())
            addAll(following)
        }
    }
    Scaffold(
        backgroundColor = samouraiBottomSheetBackground,
        topBar = {
            if (!enableSearch) TopAppBar(title = {
                Text(
                    "Select Collaborator", fontSize = 13.sp
                )
            },
                modifier = Modifier.shadow(1.dp),
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            modifier = Modifier,
                            contentDescription = "back"
                        )
                    }
                }, actions = {
                    IconButton(onClick = {
                        enableSearch = true;
                        scope.launch {
                            paynymChooser?.animateTo(ModalBottomSheetValue.Expanded)
                        }
                    }) {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "")
                    }
                }
            )
            if (enableSearch) SearchTextField(
                onSearch = {
                    collaborateViewModel.applySearch(it)
                },
                onClose = {
                    enableSearch = false
                    collaborateViewModel.applySearch(null)
                }
            )
        }
    ) {
        AnimatedVisibility(visible = loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                LinearProgressIndicator(
                    color = Color.White,
                    backgroundColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)

                )
            }
        }
        LazyColumn(
            state = listState,
        ) {
            if (cahootType?.cahootsMode == CahootsMode.SOROBAN && cahootType?.cahootsType != CahootsType.STOWAWAY) {
                item {

                }
                item {
                }
            }
            itemsIndexed(paynyms) { index, it ->
                if (it == BIP47Meta.getMixingPartnerCode()) {
                    Column(modifier = Modifier.layoutId(it)) {
                        PaynymAvatar(pcode = BIP47Meta.getMixingPartnerCode(), nym = BIP47Meta.getInstance().getLabel(BIP47Meta.getMixingPartnerCode()),
                            modifier = Modifier
                                .clickable
                                {
                                    cahootsTransactionViewModel.setCollaborator(BIP47Meta.getMixingPartnerCode())
                                    onClose()
                                }
                        )
                        Divider()
                    }
                } else {
                    PaynymAvatar(pcode = it, nym = BIP47Meta.getInstance().getLabel(it),
                        modifier = Modifier
                            .layoutId(it)
                            .clickable
                            {
                                cahootsTransactionViewModel.setCollaborator(it)
                                onClose()
                            }
                            .animateItemPlacement())
                }
            }

        }
    }
}

@Composable
fun SearchTextField(
    onSearch: (value: String) -> Unit,
    onClose: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusRequester = FocusRequester()
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
    TopAppBar(
        modifier = Modifier.shadow(1.dp),
    ) {

        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text,
                autoCorrect = false
            ),
            singleLine = true,
            keyboardActions = KeyboardActions(
                onSearch = {
                    onSearch(query)
                    onClose()
                }
            ),
            colors = TextFieldDefaults.textFieldColors(
                backgroundColor = Color.Transparent,
                textColor = Color.White,
                focusedIndicatorColor = Color.White,
                unfocusedIndicatorColor = Color.White.copy(alpha = 0.6f),
                errorCursorColor = Color.Transparent,
            ),
            trailingIcon = {
                IconButton(
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        tint = Color.White,
                        contentDescription = ""
                    )
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    tint = Color.White,
                    contentDescription = ""
                )
            },
        )
    }
}

@Composable
fun PaynymAvatar(pcode: String, modifier: Modifier = Modifier, nym: String) {

    val url = "${WebUtil.PAYNYM_API}${pcode}/avatar"

    Box(
        modifier = modifier
            .requiredHeight(70.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .requiredHeight(70.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .padding(start = 12.dp)
            ) {
                PicassoImage(
                    url = url,
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(100)),
                    contentDescription = "Avatar for $nym"
                )
            }
            Text(text = nym, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Preview
@Composable
fun PaynymAvatarPreview() {
    PaynymAvatar("", nym = "+snowflake");
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(heightDp = 320, uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PaynymSheetPreview() {
    PaynymChooser(null, {})
}

@Composable
fun PicassoImage(
    url: String, modifier: Modifier = Modifier,
    default: Painter? = null,
    contentDescription: String = "",
) {
    var imageBitMap by remember { mutableStateOf<ImageBitmap?>(null) }
    val scope = rememberCoroutineScope();
    LaunchedEffect(url) {
        scope.launch {
            Picasso.get()
                .load(url)
                .into(object : Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        if (bitmap != null) {
                            imageBitMap = bitmap.asImageBitmap();
                        }
                    }

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {

                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
                    }
                })
        }
    }
    if (imageBitMap == null) {
        if (default == null) {
            Box(modifier = modifier)
        } else {
            Image(
                modifier = modifier,
                painter = default, contentDescription = contentDescription
            )
        }
    } else {
        Image(
            modifier = modifier,
            bitmap = imageBitMap!!,
            contentDescription = contentDescription
        )
    }
}
