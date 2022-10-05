package com.samourai.wallet.stealth.vpn


import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.material.TextField
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.samourai.wallet.R
import com.samourai.wallet.stealth.StealthModeController
import com.samourai.wallet.stealth.calculator.Pink80
import com.samourai.wallet.stealth.calculator.Purple80
import com.samourai.wallet.stealth.calculator.PurpleGrey80
import com.samourai.wallet.stealth.calculator.Typography
import com.samourai.wallet.util.PrefsUtil
import kotlinx.coroutines.*

val countries = arrayListOf(
    Triple("United States", "\uD83C\uDDFA\uD83C\uDDF8", arrayListOf("Chicago", "San Francisco", "Phoenix", "New York", "Wyoming")),
    Triple("Canada", "\uD83C\uDDE8\uD83C\uDDE6", arrayListOf("Quebec", "Victoria", "Toronto", "Nova Scotia")),
    Triple("France", "\uD83C\uDDEB\uD83C\uDDF7", arrayListOf("Paris", "Cannes")),
    Triple("Netherlands", "\uD83C\uDDF3\uD83C\uDDF1", arrayListOf("Amsterdam", "Haarlem", "Harderwijk")),
    Triple("Germany", "\uD83C\uDDE9\uD83C\uDDEA", arrayListOf("Berlin", "Munich", "Cologne")),
    Triple("India", "\uD83C\uDDEE\uD83C\uDDF3", arrayListOf("Mumbai", "Cochin", "Chennai")),
    Triple("Japan", "\uD83C\uDDEF\uD83C\uDDF5", arrayListOf("Tokyo", "Yokohama", "Kobe", "Kawasaki")),
    Triple("United Kingdom", "\uD83C\uDDEC\uD83C\uDDE7", arrayListOf("London", "Edinburgh", "Birmingham", "Bristol")),
    Triple("Sweden", "\uD83C\uDDF8\uD83C\uDDEA", arrayListOf("Blekinge", "Dalarna", "Kalmar")),
    Triple("Czechia", "\uD83C\uDDE8\uD83C\uDDFF", arrayListOf("Prague", "Olomouc", "Liberec")),
    Triple("Romania", "\uD83C\uDDF7\uD83C\uDDF4", arrayListOf("Bucharest", "Timișoara")),
)
val pings = listOf(8, 12, 26, 9, 52, 48, 100, 31, 19)

class VPNActivityViewModel : ViewModel() {

    private val randomDelay = listOf(6800L, 6000L, 7000L, 1000L, 9000L, 8000L, 12000L, 16000L)
    val connectionState: MutableLiveData<String> = MutableLiveData()
    val selectedLoc: MutableLiveData<Triple<String, String, String>> = MutableLiveData(
        Triple(
            countries.first().first,
            countries.first().second,
            countries.first().third.first(),
        )
    )

    fun initiateConnection() {
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                val dl = randomDelay.random();
                connectionState.postValue("Connecting")
                delay(dl)
                connectionState.postValue("Disconnected")
            }
        }
    }

    fun setCountry(item: Triple<String, String, ArrayList<String>>, s: String) {
        selectedLoc.postValue(Triple(item.first, item.second, s));
    }

}

class VPNActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            samouraiVPNTheme {
                VpnMainScreen(this)
            }
        }
    }

    companion object {
        const val STL_VPN_LOCATION = "stl_vpn_selection"
    }
}


@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VpnMainScreen(activity: ComponentActivity?) {
    activity?.window?.statusBarColor = Color.Transparent.toArgb()
    var menu by remember {
        mutableStateOf(false)
    }
    val scope = rememberCoroutineScope()
    val viewModel = viewModel<VPNActivityViewModel>();
    var pinEntryValue by remember { mutableStateOf("") }
    var pinEntryDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val bottomSheet = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    var selectedLoc by remember {
        mutableStateOf("")
    }
    val focusRequester = remember { FocusRequester() }


    LaunchedEffect(Unit) {
        selectedLoc = PrefsUtil.getInstance(context).getValue(VPNActivity.STL_VPN_LOCATION, countries.first().third.first())
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .border(
                BorderStroke(
                    1.dp, Brush.verticalGradient(
                        colors = listOf(
                            Color.DarkGray,
                            Color.White,
                            Color.Transparent,
                            Color.Transparent,
                        )
                    )
                ), shape = RoundedCornerShape(8.dp)
            ), color = androidx.compose.material3.MaterialTheme.colorScheme.background
    ) {

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 25.dp, vertical = 32.dp)
                        .padding(top = 8.dp)
                ) {
                    Card(
                        elevation = 0.dp,
                        modifier = Modifier, shape = RoundedCornerShape(12.dp),
                        backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            CenterAlignedTopAppBar(
                                navigationIcon = {
                                    Icon(Icons.Default.Menu, contentDescription = "", tint = Color.White, modifier = Modifier.padding(start = 6.dp))
                                },
                                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
                                title = {
                                    Text(text = "Edge VPN", fontSize = 16.sp)
                                },
                                actions = {
                                    Icon(Icons.Default.Star, contentDescription = "", tint = Color.White, modifier = Modifier.padding(end = 8.dp))
                                    Icon(Icons.Default.MoreVert, contentDescription = "", tint = Color.White, modifier = Modifier.padding(end = 2.dp))
                                }
                            )
                        }
                    }
                }
            },
            bottomBar = {
                val value by viewModel.selectedLoc.observeAsState(Triple("", "", ""));
                val ping = pings.random()
                Card(
                    modifier = Modifier.clickable {
                        scope.launch {
                            bottomSheet.animateTo(ModalBottomSheetValue.HalfExpanded)
                        }
                    }, backgroundColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(topEnd = 24.dp, topStart = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(bottom = 12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxSize(), Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Spacer(modifier = Modifier.padding(12.dp))
                            Text(text = "${value.second}", fontSize = 34.sp)
                            Column {
                                Text(text = "${value.first}", fontSize = 16.sp)
                                Text(text = "${value.third}", fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.padding(12.dp))
                            Spacer(modifier = Modifier.padding(12.dp))
                            Row {
                                Text(text = "${ping} ms")
                                Icon(Icons.Default.KeyboardArrowRight, "menu", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.padding(12.dp))
                        }
                    }
                }

            }
        ) {
            VpnConnectScreen()
            if (pinEntryDialog) {
                AlertDialog(
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 12.dp,
                    onDismissRequest = {
                        pinEntryDialog = false
                    },
                    title = {
                        Text(text = "Enter Stealth Code", fontSize = 14.sp)
                    },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                    text = {
                        TextField(
                            value = pinEntryValue,
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                textColor = Color.White, cursorColor = Color.White,
                                focusedIndicatorColor = Color.White
                            ),
                            keyboardOptions = KeyboardOptions(
                                autoCorrect = false,
                                keyboardType = KeyboardType.Decimal,
                            ),
                            modifier = Modifier.focusRequester(focusRequester),
                            onValueChange = {
                                if (!it.contains("*") && !it.contains("-") && !it.contains(",") && !it.contains(".") && !it.contains(" ")) {
                                    pinEntryValue = it
                                }
                            },
                        )
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            pinEntryDialog = false
                        }) {
                            Text(text = "Cancel")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            pinEntryDialog = false
                            if (pinEntryValue.isNotEmpty() && StealthModeController.isPinMatched(context,pinEntryValue)){
                                MaterialAlertDialogBuilder(context)
                                    .setTitle(R.string.app_name)
                                    .setMessage(R.string.do_you_want_to_disable_stealth_mode)
                                    .setPositiveButton(R.string.ok) { dialog, _ ->
                                        dialog.dismiss()
                                        StealthModeController.enableStealth(StealthModeController.StealthApp.SAMOURAI, context)
                                    }.setNegativeButton(R.string.cancel) { dialog, _ ->
                                        dialog.dismiss()
                                    }.show()
                            }
                        }) {
                            Text(text = "Ok")
                        }
                    }
                )
            }

        }
        ModalBottomSheetLayout(
            sheetBackgroundColor = Color.Transparent,
            sheetState = bottomSheet,
            sheetContent = {
                Scaffold(
                    modifier = Modifier.border(
                        BorderStroke(
                            1.dp, Brush.verticalGradient(
                                colors = listOf(
                                    Color.DarkGray,
                                    Color.White,
                                    Color.Transparent,
                                    Color.Transparent,
                                )
                            )
                        ), shape = RoundedCornerShape(12.dp)
                    ),
                    topBar = {
                        CenterAlignedTopAppBar(
                            modifier = Modifier.padding(vertical = 24.dp),
                            title = { Text(text = "Choose Location") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
                            )
                        )
                    }
                ) {
                    LazyColumn {
                        items(countries) { country ->
                            VpnCountryOption(country) { selectedCountry, index: Int ->
                                scope.launch {
                                    bottomSheet.hide()
                                    viewModel.setCountry(selectedCountry, selectedCountry.third[index])
                                    if (selectedLoc.lowercase().contains(selectedCountry.third[index].lowercase())) {
                                        pinEntryDialog = true
                                        scope.launch {
                                            delay(300)
                                            focusRequester.requestFocus()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) {}

        BackHandler {
            scope.launch {
                bottomSheet.hide()
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VpnCountryOption(country: Triple<String, String, ArrayList<String>>, onClick: (Triple<String, String, ArrayList<String>>, index: Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "${country.second}", fontSize = 25.sp)
                Spacer(Modifier.padding(5.dp))
                Text(text = "${country.first}")
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp),
                Arrangement.Bottom, Alignment.Start
            ) {
                Divider()
                repeat(country.third.size) { item ->
                    val ping = pings.random()
                    ListItem(
                        Modifier.clickable {
                            onClick(country, item)
                        },
                        trailing = {
                            Text(text = "$ping ms")
                        }
                    ) {
                        Text(text = country.third[item], fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun VpnConnectScreen() {
    val viewModel = viewModel<VPNActivityViewModel>()
    val status by viewModel.connectionState.observeAsState("Disconnected")

    Box(modifier = Modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Box(
                modifier
                = Modifier
                    .size(140.dp, 140.dp)
            ) {
                Card(
                    modifier = Modifier
                        .size(140.dp, 140.dp),
                    backgroundColor = Color.White.copy(alpha = if (status == "Connecting") 0.0f else 0.8f),
                    elevation = 12.dp,
                    shape = RoundedCornerShape(100)
                ) {
                    Image(
                        modifier = Modifier
                            .size(120.dp, 120.dp)
                            .clickable {
                                viewModel.initiateConnection()
                            }
                            .rotate(180f),
                        painter = painterResource(id = R.drawable.ic_baseline_power_settings_new_24),
                        colorFilter = ColorFilter.tint(if (status == "Connecting") androidx.compose.material3.MaterialTheme.colorScheme.primary else Color.DarkGray),
                        contentDescription = ""
                    )
                    if (status == "Connecting")
                        CircularProgressIndicator(
                            modifier = Modifier.size(140.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp
                        )
                }


            }
            Spacer(modifier = Modifier.padding(18.dp))
            Text(text = status, fontSize = 16.sp, color = Color.White)
            Spacer(modifier = Modifier.padding(6.dp))
            Text(text = "00 : 00 : 00")
            Spacer(modifier = Modifier.padding(68.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Download", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "0 kbps /100 mbps", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.padding(34.dp))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Upload", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(text = "0 kbps / 100 mbps", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
            }

        }
    }
}


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    background = Color(0xff1B1C20),
    secondary = PurpleGrey80,
    tertiary = Pink80,
)


@Composable
fun samouraiVPNTheme(
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
        content = content,
    )
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VPNStealthAPPSettings(callback: () -> Unit) {

    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var serverLocations by remember { mutableStateOf<List<String>>(arrayListOf()) }
    var selectedItem by remember { mutableStateOf("") }
    val context = LocalContext.current


    LaunchedEffect(Unit) {
        scope.launch {
            selectedItem = ""
            serverLocations = countries.map { item ->
                item.third.map { " ${item.second}  ${item.first} $it" }.toList()
            }.flatten().toList()

            selectedItem = PrefsUtil.getInstance(context).getValue(VPNActivity.STL_VPN_LOCATION, serverLocations.first())
        }
    }

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
            Text("Instructions", style = MaterialTheme.typography.h6, color = Color.White)
            ListItem(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .padding(top = 8.dp),
                text = {
                    Text("Enable stealth mode", color = Color.White)
                },
                secondaryText = {
                    Text("Enter stealth CODE in samourai pin entry screen or use QUICK tiles option to trigger stealth mode", color = Color.White)
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ListItem(
                modifier = Modifier.padding(vertical = 8.dp),
                text = {
                    Text("Disable stealth mode", color = Color.White)
                },
                secondaryText = {
                    Column(modifier = Modifier) {
                        Text("To exit stealth mode choose the specified location in the vpn app and enter stealth code ", color = Color.White)
                        Divider(modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
                        OutlinedButton(onClick = { expanded = true }) {
                            Text(text = "Location:  $selectedItem", color = Color.White)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                        ) {
                            serverLocations.forEachIndexed { _, country ->
                                DropdownMenuItem(onClick = {
                                    selectedItem = country
                                    expanded = false
                                    PrefsUtil.getInstance(context).setValue(VPNActivity.STL_VPN_LOCATION, country)
                                }) {
                                    Text(text = country, color = Color.White)
                                }
                            }
                        }
                    }

                }
            )
        }

    }
}


@Preview
@Composable
fun VpnMainScreenPreview() {
    samouraiVPNTheme {
        VpnMainScreen(null)
    }
}
