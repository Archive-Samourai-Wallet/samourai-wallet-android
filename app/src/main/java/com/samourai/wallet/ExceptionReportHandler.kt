package com.samourai.wallet

import android.content.Context
import com.samourai.wallet.tor.SamouraiTorManager
import com.samourai.whirlpool.client.utils.ClientUtils
import com.samourai.whirlpool.client.wallet.AndroidWhirlpoolWalletService
import kotlinx.coroutines.*
import java.io.File
import java.lang.Thread.UncaughtExceptionHandler
import java.text.SimpleDateFormat
import java.util.*


class ExceptionReportHandler(context: Context) : UncaughtExceptionHandler {

    private var uncaughtExceptionHandler: UncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()!!;
    private var reportFile: File = File(context.filesDir.path, LOG_FILE_NAME)
    private val scope = CoroutineScope(Dispatchers.IO) + SupervisorJob()
    var sdf: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    init {
        scope.launch {
            withContext(Dispatchers.IO) {
                if (!reportFile.exists()) {
                    reportFile.createNewFile()
                }
            }
        }
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        scope.launch {
            withContext(Dispatchers.IO) {
                //clear logs if the file is above 500k
                if (reportFile.length() > 500000) {
                    reportFile.writeText("")
                }
                val rt = Runtime.getRuntime()
                val total = rt.totalMemory()
                val free = rt.freeMemory()
                val used = total - free
                val whirlpoolWalletService = AndroidWhirlpoolWalletService.getInstance()
                val whirlpoolWallet = whirlpoolWalletService.whirlpoolWallet()
                val builder = buildString {
                    append("\n----------------BEGIN--------------\n")
                    append("\n---------\n")
                    append("Device\n")
                    append("---------")
                    append("\nTime                   : ${sdf.format(Date())}")
                    append("\nAndroid SDK            : ${android.os.Build.VERSION.SDK_INT} (${android.os.Build.VERSION.CODENAME}) (${android.os.Build.VERSION.RELEASE})")
                    append("\nDevice                 : ${android.os.Build.BRAND} ${android.os.Build.PRODUCT} ${android.os.Build.MODEL}")
                    append("\nDisplay                : ${android.os.Build.DISPLAY}")
                    append("\nApp Version            : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    append("\nTor                    : ${SamouraiTorManager.isConnected()}")
                    append("\nWhirlpool              : ${if (whirlpoolWallet == null) "Not Running" else "Running"}")
                    append("\nThread                 : [id: ${t.id}] [name :${t.name}] [state :${t.state}]")
                    append("\nMemory                 : [total :${ClientUtils.bytesToMB(total)}M] [free :${ClientUtils.bytesToMB(used)}M] [used :${ClientUtils.bytesToMB(used)}M]\n")
                    append("------------\n")
                    append("STACK TRACE\n")
                    append("------------\n")
                    append("${e.stackTraceToString()}\n")
                    append("----------------END----------------\n")
                }
                reportFile.writeText("${builder}${reportFile.readText()}")
            }
        }
        uncaughtExceptionHandler.uncaughtException(t, e)
    }


    companion object {
        fun attach(context: Context) {
            ExceptionReportHandler(context)
        }
        const val LOG_FILE_NAME = "crash_report.log"
    }
}