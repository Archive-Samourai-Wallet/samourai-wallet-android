package com.samourai.wallet.tools

import AddressCalculator
import SweepPrivateKeyView
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.samourai.wallet.R
import com.samourai.wallet.theme.SamouraiWalletTheme
import com.samourai.wallet.theme.samouraiBottomSheetBackground
import com.samourai.wallet.tools.viewmodels.Auth47ViewModel
import com.samourai.wallet.tools.viewmodels.BroadcastHexViewModel
import com.samourai.wallet.tools.viewmodels.SweepViewModel
import kotlinx.coroutines.launch


class ToolsBottomSheet : BottomSheetDialogFragment() {

    enum class ToolType {
        ADDRESS_CALC,
        AUTH47,
        SIGN,
        SWEEP
    }

    var behavior: BottomSheetBehavior<*>? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val parent = view.parent as View
        val params = parent.layoutParams as CoordinatorLayout.LayoutParams
        behavior = params.behavior as BottomSheetBehavior<*>?
        if (behavior != null) {
            behavior?.state = BottomSheetBehavior.STATE_EXPANDED
            behavior?.skipCollapsed = true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    //Disables tools bottom sheet dragging
    //this will prevent accidental closing of tools dialog
    fun disableDragging(disable: Boolean = true) {
        behavior?.isDraggable = !disable
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val compose = ComposeView(requireContext())
        compose.setContent {
            SamouraiWalletTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    ToolsMainView(this, parentFragmentManager, this@ToolsBottomSheet.dialog?.window)
                }
            }
        }
        compose.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        container?.addView(compose)
        return compose
    }

    companion object {
        fun showTools(fragment: FragmentManager) {
            ToolsBottomSheet()
                .apply {
                    show(fragment, this.tag)
                }
        }

        fun showTools(fragment: FragmentManager, type: ToolType, bundle: Bundle? = null) {
            SingleToolBottomSheet(type)
                .apply {
                    arguments = bundle
                    show(fragment, this.tag)
                }
        }
    }


    class SingleToolBottomSheet(private val toolType: ToolType) : BottomSheetDialogFragment() {
        var behavior: BottomSheetBehavior<*>? = null

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            val parent = view.parent as View
            val params = parent.layoutParams as CoordinatorLayout.LayoutParams
            behavior = params.behavior as BottomSheetBehavior<*>?
            if (behavior != null) {
                behavior?.state = BottomSheetBehavior.STATE_EXPANDED
                behavior?.skipCollapsed = true
            }
            super.onViewCreated(view, savedInstanceState)
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
            val compose = ComposeView(requireContext())
            val model: AddressCalculatorViewModel by viewModels()
            if (toolType != ToolType.SWEEP) {
                val types = requireContext().resources.getStringArray(R.array.account_types)
                model.calculateAddress(types.first(), true, index = 0, context = requireContext())
            }
            val key = arguments?.getString("KEY", "") ?: ""
            compose.setContent {
                SamouraiWalletTheme {
                    Surface(color = MaterialTheme.colors.background) {
                        when (toolType) {
                            ToolType.SWEEP -> SweepPrivateKeyView(parentFragmentManager, keyParameter = key)
                            ToolType.SIGN -> SignMessage()
                            ToolType.ADDRESS_CALC -> AddressCalculator(this@SingleToolBottomSheet.dialog?.window)
                            ToolType.AUTH47 -> Auth47Login(param = key, onClose = {
                                dismiss()
                            })
                        }
                    }
                }
            }
            compose.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            container?.addView(compose)
            return compose
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ToolsMainView(toolsBottomSheet: ToolsBottomSheet?, parentFragmentManager: FragmentManager?, window: Window?) {
    val vm = viewModel<AddressCalculatorViewModel>()
    val sweepViewModel = viewModel<SweepViewModel>()
    val auth47ViewModel = viewModel<Auth47ViewModel>()
    val broadcastHexViewModel = viewModel<BroadcastHexViewModel>()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current;
    val addressCalcBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val signMessageBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val verifyMessageBottomSheetState = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val sweepPrivateKeyBottomSheet = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val auth47BottomSheet = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val broadcastBottomSheet = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden)
    val keyboard = LocalSoftwareKeyboardController.current

    //Handle BackPress
    LaunchedEffect(true) {
        toolsBottomSheet?.dialog?.setOnKeyListener { dialog, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                scope.launch {
                    if (addressCalcBottomSheetState.isVisible) {
                        addressCalcBottomSheetState.hide()
                    } else if (signMessageBottomSheetState.isVisible) {
                        signMessageBottomSheetState.hide()
                    } else if (verifyMessageBottomSheetState.isVisible) {
                        verifyMessageBottomSheetState.hide()
                    } else if (auth47BottomSheet.isVisible) {
                        auth47BottomSheet.hide()
                    } else if (broadcastBottomSheet.isVisible) {
                        broadcastBottomSheet.hide()
                    } else if (sweepPrivateKeyBottomSheet.isVisible) {
                        sweepPrivateKeyBottomSheet.hide()
                    } else {
                        dialog.dismiss()
                    }
                }
            }
            false
        }
    }

    LaunchedEffect(
        addressCalcBottomSheetState.isVisible,
        signMessageBottomSheetState.isVisible,
        verifyMessageBottomSheetState.isVisible,
        auth47BottomSheet.isVisible,
        broadcastBottomSheet.isVisible,
        sweepPrivateKeyBottomSheet.isVisible,
    ) {
        val anyToolWindowIsVisible = (
            addressCalcBottomSheetState.isVisible ||
                signMessageBottomSheetState.isVisible ||
                verifyMessageBottomSheetState.isVisible ||
                auth47BottomSheet.isVisible ||
                broadcastBottomSheet.isVisible ||
                sweepPrivateKeyBottomSheet.isVisible)
        toolsBottomSheet?.dialog?.setCancelable(!anyToolWindowIsVisible)
    }

    Scaffold {
        Column {
            TopAppBar(backgroundColor = MaterialTheme.colors.primary, title = { Text(text = "Tools ", color = Color.White) }, navigationIcon = {
                IconButton(onClick = {
                    toolsBottomSheet?.dismiss()
                }) {
                    Icon(painter = painterResource(id = R.drawable.ic_close_white_24dp), contentDescription = "", tint = Color.White)
                }
            }
            )
            ToolsItem(
                title = stringResource(id = R.string.action_sweep),
                subTitle = stringResource(R.string.enter_a_private_key_and),
                icon = R.drawable.ic_broom,
                onClick = {
                    scope.launch {
                        toolsBottomSheet?.disableDragging()
                        sweepViewModel.clear()
                        sweepPrivateKeyBottomSheet.animateTo(ModalBottomSheetValue.Expanded)
                    }
                }
            )
            ToolsItem(
                title = stringResource(id = R.string.sign_message),
                subTitle = stringResource(R.string.sign_messages_using_your),
                icon = R.drawable.ic_signature,
                onClick = {
                    scope.launch {
                        val types = context.resources.getStringArray(R.array.account_types)
                        vm.calculateAddress(types.first(), true, index = 0, context = context)
                        vm.clearMessage()
                        toolsBottomSheet?.disableDragging()
                        signMessageBottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
                    }
                }
            )
            ToolsItem(
                title = "Verify message",
                subTitle = "Verify that a signed message contains a valid signature",
                icon = R.drawable.ic_verify_message,
                onClick = {
                    scope.launch {
                        val types = context.resources.getStringArray(R.array.account_types)
                        vm.calculateAddress(types.first(), true, index = 0, context = context)
                        vm.clearMessage()
                        toolsBottomSheet?.disableDragging()
                        verifyMessageBottomSheetState.animateTo(ModalBottomSheetValue.Expanded)
                    }
                }
            )
            ToolsItem(
                title = stringResource(R.string.wallet_address_calc),
                subTitle = stringResource(R.string.calculate_any_address_derived),
                icon = R.drawable.ic_baseline_calculate,
                onClick = {
                    scope.launch {
                        toolsBottomSheet?.disableDragging()
                        //Load first account type to viewmodel
                        val types = context.resources.getStringArray(R.array.account_types)
                        vm.calculateAddress(types.first(), true, index = 0, context = context)
                        vm.clearMessage()
                        addressCalcBottomSheetState.show()
                    }
                }
            )
            ToolsItem(
                title = stringResource(id = R.string.auth_using_paynym),
                subTitle = stringResource(R.string.simple_and_secure_auth_with),
                icon = R.drawable.ic_auth_with_paynym,
                onClick = {
                    scope.launch {
                        toolsBottomSheet?.disableDragging()
                        auth47BottomSheet.show()
                    }
                }
            )
            ToolsItem(
                title = stringResource(id = R.string.broadcast_transactions),
                subTitle = stringResource(R.string.options_broadcast_hex2),
                icon = R.drawable.ic_broadcast_transaction,
                onClick = {
                    scope.launch {
                        broadcastHexViewModel.clear()
                        toolsBottomSheet?.disableDragging()
                        broadcastBottomSheet.animateTo(ModalBottomSheetValue.Expanded)
                    }
                }
            )
        }

        //Clear previous address calc state
        if (addressCalcBottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    toolsBottomSheet?.disableDragging(disable = false)
                    val types = context.resources.getStringArray(R.array.account_types)
                    vm.calculateAddress(types.first(), true, index = 0, context = context)
                    vm.setPage(0)
                    keyboard?.hide()
                }
            }
        }
        if (signMessageBottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    val types = context.resources.getStringArray(R.array.account_types)
                    vm.calculateAddress(types.first(), true, index = 0, context = context)
                    toolsBottomSheet?.disableDragging(disable = false)
                    keyboard?.hide()
                }
            }
        }
        if (verifyMessageBottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    val types = context.resources.getStringArray(R.array.account_types)
                    vm.calculateAddress(types.first(), true, index = 0, context = context)
                    vm.clearVerifiedMessageState()
                    toolsBottomSheet?.disableDragging(disable = false)
                    keyboard?.hide()
                }
            }
        }
        if (sweepPrivateKeyBottomSheet.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    sweepViewModel.clear()
                    toolsBottomSheet?.disableDragging(disable = false)
                    keyboard?.hide()
                }
            }
        }

        if (auth47BottomSheet.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    auth47ViewModel.clear()
                    toolsBottomSheet?.disableDragging(disable = false)
                    keyboard?.hide()
                }
            }
        }

        if (broadcastBottomSheet.currentValue != ModalBottomSheetValue.Hidden) {
            DisposableEffect(Unit) {
                onDispose {
                    broadcastHexViewModel.clear()
                    toolsBottomSheet?.disableDragging(disable = false)
                    keyboard?.hide()
                }
            }
        }

        ModalBottomSheetLayout(
            sheetState = addressCalcBottomSheetState,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                AddressCalculator(window)
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {}

        ModalBottomSheetLayout(
            sheetState = broadcastBottomSheet,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                BroadcastTransactionTool(
                    onCloseClick = {
                        scope.launch {
                            broadcastBottomSheet.hide()
                        }
                    }
                )
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {}

        ModalBottomSheetLayout(
            sheetState = sweepPrivateKeyBottomSheet,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                SweepPrivateKeyView(parentFragmentManager)
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {}

        ModalBottomSheetLayout(
            sheetState = signMessageBottomSheetState,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                SignMessage()
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {}

        ModalBottomSheetLayout(
            sheetState = verifyMessageBottomSheetState,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                VerifyMessage(
                    modal = verifyMessageBottomSheetState
                )
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {}

        ModalBottomSheetLayout(
            sheetState = auth47BottomSheet,
            scrimColor = Color.Black.copy(alpha = 0.7f),
            sheetBackgroundColor = samouraiBottomSheetBackground,
            sheetContent = {
                Auth47Login(onClose = {
                    scope.launch {
                        auth47BottomSheet.hide()
                    }
                })
            },
            sheetShape = MaterialTheme.shapes.small.copy(topEnd = CornerSize(12.dp), topStart = CornerSize(12.dp))
        ) {
        }
    }
}

@Composable
fun ToolsItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    icon: Int,
    title: String,
    subTitle: String,
) {
    Box(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                }
                .border(0.dp, Color.Transparent) // outer border
                .padding(12.dp)
        ) {
            Box(modifier = Modifier.size(54.dp)) {
                Icon(
                    painter = painterResource(id = icon),
                    tint = MaterialTheme.colors.onSecondary,
                    modifier = Modifier
                        .size(34.dp)
                        .padding(end = 12.dp)
                        .align(Center)
                        .apply { },
                    contentDescription = null
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.size(5.dp))
                Text(
                    text = subTitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colors.onSecondary
                )
            }
        }
    }

}

@Preview(showBackground = true, heightDp = 80, widthDp = 410)
@Composable
fun DefaultToolsItemPreview() {
    SamouraiWalletTheme {
        Surface {
            ToolsItem(
                title = "Sweep Private Key",
                subTitle = "Enter a private key and sweep any funds to" +
                    "your next bitcoin address",
                icon = R.drawable.ic_broom
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SamouraiWalletTheme {
        // A surface container using the 'background' color from the theme
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
            ToolsMainView(null, null, null)
        }
    }
}
