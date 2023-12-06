package com.samourai.wallet.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class AccountSelectionModel extends AndroidViewModel {

    private MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private MutableLiveData<Long> depositBalance = new MutableLiveData<>(0l);
    private MutableLiveData<Long> postmixBalance = new MutableLiveData<>(0l);


    public AccountSelectionModel(@NonNull final Application application) {
        super(application);
    }

    public void setLoading(final boolean state) {
        loading.postValue(state);
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void setDepositBalance(final long balance) {
        depositBalance.postValue(balance);
    }

    public void setPostmixBalance(final long balance) {
        postmixBalance.postValue(balance);
    }

    public LiveData<Long> getDepositBalance() {
        return depositBalance;
    }

    public LiveData<Long> getPostmixBalance() {
        return postmixBalance;
    }
}
