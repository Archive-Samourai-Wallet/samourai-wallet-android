package com.samourai.wallet.send.review;

public enum EnumTransactionPriority {
    LOW("Low", "20% probability of next block"),
    NORMAL("Normal", "50% probability of next block"),
    NEXT_BLOCK("Next Block", "99% probability of next block"),
    ;

    private final String caption;
    private final String description;

    EnumTransactionPriority(final String caption, final String description) {
        this.caption = caption;
        this.description = description;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }
}
