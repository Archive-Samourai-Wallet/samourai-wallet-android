package com.samourai.wallet.send.review;

import static java.util.Objects.nonNull;

import android.content.Context;

import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

public class TxAlertReview {

    private final int title;
    private final int explanation;
    private final boolean withFixSuggestion;
    private final Callable<String> fixAction;

    private final Set<String> reusedAddresses = Sets.newHashSet();

    private TxAlertReview(
            final int title,
            final int explanation,
            final boolean withFixSuggestion,
            final Callable<String> fixAction) {

        this.title = title;
        this.explanation = explanation;
        this.withFixSuggestion = withFixSuggestion;
        this.fixAction = fixAction;
    }

    public static TxAlertReview create(
            final int title,
            final int explanation,
            final Callable<String> fixAction) {

        return new TxAlertReview(title, explanation, nonNull(fixAction), fixAction);
    }

    public String getTitle(final Context context) {
        return nonNull(context) ? context.getString(title) : "missing title";
    }

    public String getExplanation(final Context context) {
        return nonNull(context) ? context.getString(explanation) : "missing explanation";
    }

    public boolean isWithFixSuggestion() {
        return withFixSuggestion;
    }

    public Callable<String> getFixAction() {
        return fixAction;
    }

    public Set<String> getReusedAddresses() {
        return reusedAddresses;
    }

    public TxAlertReview addReusedAddress(final String addr) {
        reusedAddresses.add(addr);
        return this;
    }

    public TxAlertReview addReusedAddresses(final Collection<String> addresses) {
        reusedAddresses.addAll(addresses);
        return this;
    }

    public boolean isReusedAddress(final String address) {
        return reusedAddresses.contains(address);
    }
}
