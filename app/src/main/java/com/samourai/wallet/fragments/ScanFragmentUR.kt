package com.samourai.wallet.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.transition.TransitionManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.transition.MaterialSharedAxis
import com.invertedx.hummingbird.QRScanner
import com.samourai.wallet.R
import com.samourai.wallet.permissions.PermissionsUtil
import com.samourai.wallet.util.Util.bytesToHex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ScanFragment : BottomSheetDialogFragment() {

    private var mCodeScanner: QRScanner? = null
    private var onScan: (scanData: String) -> Unit = {}
    private var onScanUR: (scanData: String) -> Unit = {}
    private var permissionView: MaterialCardView? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.bottomsheet_camera_ur, container, false);
    }

    fun setOnScanListener(callback: (scanData: String) -> Unit) {
        this.onScan = callback
        this.onScanUR = callback
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        permissionView = view.findViewById(R.id.permissionCameraDialog)
        mCodeScanner = view.findViewById(R.id.scanner_view);
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionView(false)
        } else {
            showPermissionView(true)
        }
        mCodeScanner?.setQRDecodeListener {
            GlobalScope.launch(Dispatchers.Main) {
                onScan(it)
            }
        }

        mCodeScanner?.setURDecodeListener { result ->
            onScanUR(bytesToHex(result.getOrNull()?.ur?.toBytes()))
        }

        permissionView!!.findViewById<View>(R.id.permissionCameraDialogGrantBtn)
            .setOnClickListener {
                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_DENIED
                ) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.CAMERA),
                        PermissionsUtil.CAMERA_PERMISSION_CODE
                    )
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        startCamera()
    }

    private fun startCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionView(false)
        } else {
            showPermissionView(true)
        }
    }
    private fun showPermissionView(show: Boolean) {
        val sharedAxis = MaterialSharedAxis(MaterialSharedAxis.Y, show)
        TransitionManager.beginDelayedTransition(
            (permissionView!!.rootView as ViewGroup),
            sharedAxis
        )
        if (show) {
            permissionView!!.visibility = View.VISIBLE
            mCodeScanner!!.visibility = View.INVISIBLE
        } else {
            permissionView!!.visibility = View.INVISIBLE
            mCodeScanner!!.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        startCamera()
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mCodeScanner?.startScanner()
        }
    }

    override fun onStart() {
        super.onStart()
        val view = view
        view?.post {
            val parent = view.parent as View
            val params =
                parent.layoutParams as CoordinatorLayout.LayoutParams
            val behavior = params.behavior
            val bottomSheetBehavior = behavior as BottomSheetBehavior<*>?
            if (bottomSheetBehavior != null) {
                bottomSheetBehavior.peekHeight = view.measuredHeight
            }
        }
    }

}