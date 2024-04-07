package com.samourai.wallet.api.seen;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RawSeenAddresses {
    private final Map<String, Boolean> seenAddresses;

    private RawSeenAddresses(final Map<String, Boolean> seenAddressesMap) {
        this.seenAddresses = ImmutableMap.copyOf(seenAddressesMap);
    }

    public static RawSeenAddresses createEmpty() {
        return new RawSeenAddresses(ImmutableMap.of());
    }

    public static RawSeenAddresses create(final Map<String, Boolean> seenAddressesMap) {
        return new RawSeenAddresses(seenAddressesMap);
    }

    public boolean isAddressSeen(final String address) {
        return Objects.equals(seenAddresses.get(address), true);
    }

    public Map<String, Boolean> getContent() {
        return seenAddresses;
    }

    public Set<String> filterSeenAddresses(final Collection<String> addresses) {
        final Set<String> filteredAddr = Sets.newHashSet();
        for (final String addr : CollectionUtils.emptyIfNull(addresses)) {
            if (isAddressSeen(addr)) {
                filteredAddr.add(addr);
            }
        }
        return filteredAddr;
    }

    public Set<String> allSeenAddresses() {
        final Set<String> filteredAddr = Sets.newHashSet();
        for (final Map.Entry<String, Boolean> seenAddrEntry : seenAddresses.entrySet()) {
            if (Objects.equals(seenAddrEntry.getValue(), true)) {
                filteredAddr.add(seenAddrEntry.getKey());
            }
        }
        return filteredAddr;
    }
}
