package com.samourai.wallet.util.func;

import android.content.Context;

import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.tools.AddressCalculatorViewModel;
import com.samourai.wallet.tools.AddressDetailsModel;

import org.apache.commons.lang3.StringUtils;
import org.bitcoinj.core.Address;

public class AddressHelper {

    private AddressHelper() {}

    public static int getAddressType(final String address) {
        if (! FormatsUtil.getInstance().isValidBitcoinAddress(address)) return 0;
        if(FormatsUtil.getInstance().isValidBech32(address)) return 84;
        if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) return 49;
        return 44;
    }

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
}
