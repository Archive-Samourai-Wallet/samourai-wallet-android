package com.samourai.wallet.collaborate

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.samourai.wallet.R
import com.samourai.wallet.theme.*
import kotlinx.coroutines.launch


class CollaborateActivity : ComponentActivity() {
    private val collaborateViewModel: CollaborateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SamouraiWalletTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                    CollaborateScreen(this)
                }
            }
        }
        collaborateViewModel.initWithContext(applicationContext)
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun CollaborateScreen(collaborateActivity: CollaborateActivity?) {
    var selected by remember { mutableStateOf(0) }
    val paynymChooser = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(text = "Collaborate", color = samouraiTextPrimary) },
                    backgroundColor = samouraiSlateGreyAccent,
                    navigationIcon = {
                        Box(modifier = Modifier.padding(12.dp)) {
                            Icon(painter = painterResource(id = R.drawable.ic_close_white_24dp),
                                tint = samouraiTextPrimary,
                                contentDescription = "Close", modifier = Modifier.clickable {
                                    collaborateActivity?.onBackPressed()
                                })
                        }
                    },

                    )

            }
        },
    ) {

        Column(modifier = Modifier.padding(vertical = 14.dp, horizontal = 12.dp)) {
            TabRow(
                divider = {
                    Box {}
                },
                selectedTabIndex = selected,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                indicator = { tabPositions ->
                    TabIndicator(tabPositions, tabPage = selected)
                },
            ) {
                Tab(
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(painter = painterResource(id = R.drawable.ic_collaborate_initiate), contentDescription = "")
                            Text("Initiate", color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    selected = selected == 0,
                    onClick = { selected = 0 }
                )
                Tab(
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Icon(painter = painterResource(id = R.drawable.ic_connect_without_contact), contentDescription = "")
                            Text("Participate", color = Color.White)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .padding(12.dp),
                    selected = selected == 1,
                    onClick = { selected = 1 }
                )
            }

            Box(

                modifier = Modifier
            ) {
                if (selected == 0) {
                    InitiateSegment(
                        onCollaboratorClick = {
                            scope.launch {
                                paynymChooser.show()
                            }
                        }
                    )
                } else {
                    ParticipateSegment()
                }
            }

        }

    }

    //Clear previous address calc state
    if (paynymChooser.currentValue != ModalBottomSheetValue.Hidden) {
        DisposableEffect(Unit) {
            onDispose {
               scope.launch {
                   keyboardController?.hide();
               }
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = paynymChooser,
        modifier = Modifier.zIndex(5f),
        scrimColor = Color.Black.copy(alpha = 0.7f),
        sheetBackgroundColor = samouraiBottomSheetBackground,
        sheetContent = {
            PaynymChooser(paynymChooser) {
                scope.launch {
                    paynymChooser.hide()
                }
            }
        },
        sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
    ) {
    }

}


@Preview(showBackground = true, heightDp = 620, widthDp = 420)
@Composable
fun DefaultPreview2() {
    SamouraiWalletTheme {
        CollaborateScreen(null)
    }
}


@Composable
fun InitiateSegment(
    onCollaboratorClick: () -> Unit
) {
    Column(
        modifier = Modifier.padding(
            vertical = 12.dp,
            horizontal = 14.dp
        )
    ) {
        Column(
            Modifier
                .clickable {
                    onCollaboratorClick()
                }
                .padding(vertical = 12.dp)
                .fillMaxWidth()) {
            Text(text = "Collaborator", fontSize = 14.sp)
            Text(text = "None selected",fontSize = 13.sp)
        }
    }
}


@Composable
fun ParticipateSegment() {
    Column {

    }
}

@Composable
private fun TabIndicator(
    tabPositions: List<TabPosition>,
    tabPage: Int
) {
    val transition = updateTransition(
        tabPage,
        label = "Tab indicator"
    )
    val indicatorLeft by transition.animateDp(
        label = "Indicator left"
    ) { page ->
        tabPositions[page].left
    }
    val indicatorRight by transition.animateDp(
        label = "Indicator right"
    ) { page ->
        tabPositions[page].right
    }

    Box(
        Modifier
            .fillMaxSize()
            .wrapContentSize(align = Alignment.BottomStart)
            .offset(x = indicatorLeft)
            .width(indicatorRight - indicatorLeft)
            .padding(2.dp)
            .fillMaxSize()
            .zIndex(-4f)
            .border(
                BorderStroke(120.dp, samouraiAccent),
                RoundedCornerShape(8.dp)
            )
    )
}
