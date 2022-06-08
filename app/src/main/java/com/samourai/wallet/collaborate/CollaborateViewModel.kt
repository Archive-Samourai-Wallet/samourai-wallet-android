package com.samourai.wallet.collaborate

import android.content.Context
import android.util.Log
import androidx.compose.ui.text.toLowerCase
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.samourai.wallet.bip47.BIP47Meta
import com.samourai.wallet.bip47.BIP47Util
import com.samourai.wallet.paynym.api.PayNymApiService
import com.samourai.wallet.paynym.models.NymResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CollaborateViewModel : ViewModel() {


    private var followingList = ArrayList<String>()
    private var followingListLive = MutableLiveData<ArrayList<String>>()
    private var loadingStateLive = MutableLiveData<Boolean>()
    private var collaboratorPcodeLive = MutableLiveData<String?>(null)

    val following: LiveData<ArrayList<String>>
        get() = followingListLive

    val loading: LiveData<Boolean>
        get() = loadingStateLive

    val collaboratorLive: LiveData<String?>
        get() = collaboratorPcodeLive


    fun setCollaborator(pcode: String?) {
        collaboratorPcodeLive.postValue(pcode);
    }

    fun initWithContext(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    loadingStateLive.postValue(true)
                }
                val strPaymentCode = BIP47Util.getInstance(context).paymentCode.toString()
                val apiService = PayNymApiService.getInstance(strPaymentCode, context)
                try {
                    val response = apiService.getNymInfo()
                    withContext(Dispatchers.Main) {
                        loadingStateLive.postValue(false)
                    }
                    if (response.isSuccessful) {
                        val responseJson = response.body?.string()
                        if (responseJson != null) {
                            val jsonObject = (JSONObject(responseJson))
                            val nym = Gson().fromJson(jsonObject.toString(), NymResponse::class.java);
                            val array = jsonObject.getJSONArray("codes")
                            if (array.getJSONObject(0).has("claimed") &&
                                array.getJSONObject(0).getBoolean("claimed")
                            ) {
                                nym.following?.let { codes ->
                                    codes.forEach { paynym ->
                                        BIP47Meta.getInstance().setSegwit(paynym.code, paynym.segwit)
                                        if (BIP47Meta.getInstance().getDisplayLabel(paynym.code).contains(paynym.code.substring(0, 4))) {
                                            BIP47Meta.getInstance().setLabel(paynym.code, paynym.nymName)
                                        }
                                    }
                                    val followings = ArrayList(codes.distinctBy { it.code }.map { it.code }.take(4))
                                    BIP47Meta.getInstance().addFollowings(followings)
                                    sortByLabel(followings);
                                    viewModelScope.launch(Dispatchers.Main) {
                                        followingListLive.postValue(followings)
                                        followingList = followings
                                    }
                                }

                            }
                        } else
                            throw Exception("Invalid response ")
                    }
                } catch (ex: Exception) {
                    withContext(Dispatchers.Main) {
                        loadingStateLive.postValue(false)
                    }
                    throw CancellationException("Error ${ex}")
                }


            }
        }
    }

    private fun sortByLabel(list: ArrayList<String>) {
        list.sortWith { pcode1: String?, pcode2: String? ->
            var res = java.lang.String.CASE_INSENSITIVE_ORDER.compare(BIP47Meta.getInstance().getDisplayLabel(pcode1), BIP47Meta.getInstance().getDisplayLabel(pcode2))
            if (res == 0) {
                res = BIP47Meta.getInstance().getDisplayLabel(pcode1).compareTo(BIP47Meta.getInstance().getDisplayLabel(pcode2))
            }
            res
        }
    }

    fun applySearch(query: String?) {
        if (query == null) {
            followingListLive.postValue(followingList);
            return
        }
        viewModelScope.launch {
            val items = followingList.filter {
                BIP47Meta.getInstance().getDisplayLabel(it).lowercase().indexOf(query.lowercase()) != -1
            }.toList()
            followingListLive.postValue(ArrayList(items))
        }
    }
}