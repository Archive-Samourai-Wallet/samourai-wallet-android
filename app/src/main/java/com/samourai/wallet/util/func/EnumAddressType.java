package com.samourai.wallet.util.func;

import com.google.common.collect.ImmutableMap;
import com.samourai.wallet.SamouraiWallet;

import org.bitcoinj.core.Address;

import java.util.Map;

public enum EnumAddressType {
    BIP49_SEGWIT_COMPAT(49, "segwit compatible"),
    BIP84_SEGWIT_NATIVE(84, "segwit native"),
    BIP44_LEGACY(44, "legacy"),
    INVALID(0, "invalid"),
    ;

    private static final Map<Integer, EnumAddressType> CACHE_TYPE_TO_ENUM = createCacheTypeToEnum();

    private static Map<Integer, EnumAddressType> createCacheTypeToEnum() {
        final ImmutableMap.Builder<Integer, EnumAddressType> builder = ImmutableMap.builder();
        for (final EnumAddressType addressType : EnumAddressType.values()) {
            builder.put(addressType.type, addressType);
        }
        return builder.build();
    }

    private final int type;
    private final String description;

    EnumAddressType(final int type, final String description) {
        this.type = type;
        this.description = description;
    }

    public boolean isSegwitNative() {
        return this == BIP84_SEGWIT_NATIVE;
    }

    public boolean isSegwitCompatible() {
        return this == BIP49_SEGWIT_COMPAT;
    }

    public boolean isLegacy() {
        return this == BIP44_LEGACY;
    }

    public int getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public static EnumAddressType fromType(final int type) {
        return CACHE_TYPE_TO_ENUM.get(type);
    }

    public static EnumAddressType fromAddress(final String address) {
        if (! FormatsUtil.getInstance().isValidBitcoinAddress(address)) return INVALID;
        if(FormatsUtil.getInstance().isValidBech32(address)) return BIP84_SEGWIT_NATIVE;
        if(Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), address).isP2SHAddress()) return BIP49_SEGWIT_COMPAT;
        return BIP44_LEGACY;
    }

}
