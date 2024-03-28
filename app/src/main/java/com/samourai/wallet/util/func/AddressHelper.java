package com.samourai.wallet.util.func;

import android.content.Context;

import com.google.common.collect.Maps;
import com.samourai.wallet.R;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.tools.AddressCalculatorViewModel;
import com.samourai.wallet.tools.AddressDetailsModel;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

import static java.lang.Math.max;
import static java.util.Objects.isNull;

public class AddressHelper {

    private AddressHelper() {}

    public static int searchAddressIndex(
            final String addressToLook,
            final String addressTypeAsString,
            final boolean isExternal,
            final int startIndex,
            final int endIndex,
            final Context context) {

        for (int index = startIndex; index < endIndex; ++index) {
            final AddressDetailsModel model = AddressCalculatorViewModel.Companion.addressDetailsModel(
                    addressTypeAsString,
                    isExternal,
                    index,
                    context);
            final String pubKey = model.getPubKey();
            if (StringUtils.equals(pubKey, addressToLook)) return index;
        }
        return Integer.MIN_VALUE;
    }

    public static boolean sendToMyDepositAddress(final Context context, final String sendAddress) {

        final AddressFactory addressFactory = AddressFactory.getInstance(context);

        final EnumAddressType addressType = EnumAddressType.fromAddress(sendAddress);
        final String[] types = context.getResources().getStringArray(R.array.account_types);
        final String addrTypeAsString;
        final int receiveIndex;
        final int changeIndex;
        final Map<Boolean, Integer> currentIndex = Maps.newHashMap();
        switch (addressType) {
            case BIP49_SEGWIT_COMPAT:
                addrTypeAsString = types[0];
                currentIndex.put(true, addressFactory.getIndex(WALLET_INDEX.BIP49_RECEIVE));
                currentIndex.put(false, addressFactory.getIndex(WALLET_INDEX.BIP49_CHANGE));
                break;
            case BIP84_SEGWIT_NATIVE:
                addrTypeAsString = types[1];
                currentIndex.put(true, addressFactory.getIndex(WALLET_INDEX.BIP84_RECEIVE));
                currentIndex.put(false, addressFactory.getIndex(WALLET_INDEX.BIP84_CHANGE));
                break;
            case BIP44_LEGACY:
                addrTypeAsString = types[2];
                currentIndex.put(true, addressFactory.getIndex(WALLET_INDEX.BIP44_RECEIVE));
                currentIndex.put(false, addressFactory.getIndex(WALLET_INDEX.BIP44_CHANGE));
                break;
            default:
                addrTypeAsString = null;
                currentIndex.put(true, Integer.MIN_VALUE);
                currentIndex.put(false, Integer.MIN_VALUE);
                break;
        }

        if (isNull(addrTypeAsString)) return false;

        final int scanIndexMargin = 64;
        boolean isExternal = true;
        int startIdx = max(0, currentIndex.get(isExternal) - scanIndexMargin);
        int endIdx = currentIndex.get(isExternal)  + scanIndexMargin;

        int indexFound = searchAddressIndex(
                sendAddress,
                addrTypeAsString,
                isExternal,
                startIdx,
                endIdx,
                context);
        if (indexFound < endIdx && indexFound >= startIdx) return true;

        isExternal = false;
        startIdx = max(0, currentIndex.get(isExternal) - scanIndexMargin);
        endIdx = currentIndex.get(isExternal)  + scanIndexMargin;

        indexFound = searchAddressIndex(
                sendAddress,
                addrTypeAsString,
                isExternal,
                startIdx,
                endIdx,
                context);
        if (indexFound < endIdx && indexFound >= startIdx) return true;

        return false;
    }
}
