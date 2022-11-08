package com.samourai.wallet.stealth.notepad

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import com.samourai.wallet.R
import com.samourai.wallet.stealth.StealthModeController
import com.samourai.wallet.stealth.qrscannerapp.Purple40
import com.samourai.wallet.stealth.qrscannerapp.PurpleGrey40
import com.samourai.wallet.stealth.stealthTapListener
import com.samourai.wallet.tools.WrapToolsPageAnimation

class NotepadActivity : ComponentActivity() {
    override fun onBackPressed() {
        return
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SamouraiStealthAppNotepad {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    NotesScreen()
                }
            }
        }
    }
}

@Composable
fun NotesScreen() {
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    var isEditing by remember { mutableStateOf(false) }
    var hasBeenEdited by remember { mutableStateOf(false) }
    var titleText by remember { mutableStateOf("") }
    var contentText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val prefs by remember { mutableStateOf(context.getSharedPreferences("${context.packageName}_stealth_prefs", Context.MODE_MULTI_PROCESS)) }
    var titleEdit = ""
    var contentEdit = ""


    androidx.compose.material.Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isEditing) {
                        val titleID = if (titleEdit == "") "title" + (prefs.all.size-2).toString() else titleEdit
                        val contentID = if (contentEdit == "") "content" + (prefs.all.size-2).toString() else contentEdit
                        prefs?.edit()?.putString(titleID, titleText)?.apply()
                        prefs?.edit()?.putString(contentID, contentText)?.apply()
                        isEditing = false
                        titleText = ""
                        contentText = ""
                        titleEdit = ""
                        contentEdit = ""
                    }
                    else
                        isEditing = true
                },
                backgroundColor = androidx.compose.material.MaterialTheme.colors.primary
            ) {
                androidx.compose.material.Icon(
                    imageVector = if (isEditing) Icons.Default.ArrowBack else Icons.Default.Add,
                    contentDescription = "Add note"
                )
            }
        },
        scaffoldState = scaffoldState,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "Notepad",
                        modifier = Modifier.stealthTapListener(
                            onTapCallBack = {
                                disableStealth(context)
                            }
                        ),
                    style = androidx.compose.material.MaterialTheme.typography.h4, color = Black) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = White
                )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            WrapToolsPageAnimation(visible = isEditing) {
                Box() {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .padding(end = 32.dp)
                    ) {
                        TextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = titleText,
                            placeholder = { Text("Write a title here", color = Black, fontSize = 30.sp) },
                            onValueChange = {
                                titleText = it
                                hasBeenEdited = true
                            },
                            textStyle = androidx.compose.material.MaterialTheme.typography.h4,
                            singleLine = true,
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                textColor = Black,
                                focusedIndicatorColor = Color.LightGray,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.6f),
                                errorCursorColor = Color.Transparent,
                            ),
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            modifier = Modifier
                                .fillMaxWidth(),
                            value = contentText,
                            placeholder = { Text("Write your note here", color = Black, fontSize = 15.sp) },
                            onValueChange = {
                                contentText = it
                                hasBeenEdited = true
                            },
                            textStyle = androidx.compose.material.MaterialTheme.typography.h6,
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color.Transparent,
                                textColor = Black,
                                focusedIndicatorColor = Color.LightGray,
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.6f),
                                errorCursorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                var counter = prefs.all.size
                prefs.all.forEach { entry ->
                    if(entry.key.contains("title")) {
                        val number = entry.key.toString().removePrefix("title")
                        if (counter < number.toInt())
                            counter = number.toInt()
                    }
                }
                items(counter) { index ->
                    val titleID = "title" + (index+1).toString()
                    val contentID = "content" + (index+1).toString()
                    if (prefs.getString(titleID, null) != null) {
                        NoteItem(
                            title = prefs.getString(titleID, null)!!,
                            content = prefs.getString(contentID, null)!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isEditing = true
                                    titleText = prefs.getString(titleID, null)!!
                                    contentText = prefs.getString(contentID, null)!!
                                    titleEdit = titleID
                                    contentEdit = contentID
                                },
                            onDeleteClick = {
                                prefs.edit().remove(titleID).apply()
                                prefs.edit().remove(contentID).apply()
                                isEditing = true
                                isEditing = false
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}


fun disableStealth(context: Context){
    StealthModeController.enableStealth(StealthModeController.StealthApp.SAMOURAI, context)
}
@Composable
fun NoteItem(
    title: String,
    content: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 10.dp,
    cutCornerSize: Dp = 30.dp,
    onDeleteClick: () -> Unit
) {
    Box(
        modifier = modifier
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val clipPath = Path().apply {
                lineTo(size.width - cutCornerSize.toPx(), 0f)
                lineTo(size.width, cutCornerSize.toPx())
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }

            clipPath(clipPath) {
                drawRoundRect(
                    color = Color(0xffffdf80),
                    size = size,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
                drawRoundRect(
                    color = Color(
                        ColorUtils.blendARGB(0x003409, 0x000000, 0.2f)
                    ),
                    topLeft = Offset(size.width - cutCornerSize.toPx(), -100f),
                    size = Size(cutCornerSize.toPx() + 100f, cutCornerSize.toPx() + 100f),
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(end = 32.dp)
        ) {
            Text(
                text = title,
                style = androidx.compose.material.MaterialTheme.typography.h6,
                color = androidx.compose.material.MaterialTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = androidx.compose.material.MaterialTheme.typography.body1,
                color = androidx.compose.material.MaterialTheme.colors.onSurface,
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
        androidx.compose.material.IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            androidx.compose.material.Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete note",
                tint = androidx.compose.material.MaterialTheme.colors.onSurface
            )
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    background = Color(0xFF0B0D0F),
    surface = Color(0xFF303942),
)

@Composable
fun SamouraiStealthAppNotepad(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> DarkColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as Activity).window.statusBarColor = Black.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun NotepadAppStealthSettings(callback: () -> Unit) {
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
            Text(stringResource(R.string.instructions), style = androidx.compose.material.MaterialTheme.typography.h6, color = Color.White)
            ListItem(
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .padding(top = 8.dp),
                text = {
                    Text( stringResource(R.string.enable_stealth_mode), color = Color.White, style = MaterialTheme.typography.titleSmall,
                        modifier =  Modifier.padding(bottom = 8.dp))

                },
                secondaryText = {
                    Text(
                        stringResource(id = R.string.upon_exiting_samourai_wallet_stealth_mode),
                        style = MaterialTheme.typography.bodyMedium, color =    secondaryColor )
                }
            )
            Divider(
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ListItem(
                modifier = Modifier.padding(vertical = 8.dp),
                text = {
                    Text(stringResource(R.string.disable_stealth_mode), color = Color.White,style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp))
                },
                secondaryText = {
                    Column(modifier = Modifier) {
                        Text("Tap “Notepad” at top of screen 5 times",
                            style = MaterialTheme.typography.bodyMedium, color =    secondaryColor  )
                    }

                }
            )
        }

    }
}
