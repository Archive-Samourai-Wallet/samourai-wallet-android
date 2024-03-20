package com.samourai.wallet.send.review.ref;

public enum EnumCoinSelectionType {

    STONEWALL("STONEWALL", "Highest entropy transaction", false),
    SIMPLE("Simple", "Lowest miner fees", false),
    CUSTOM("Custom", "Manually select inputs", true),
    BATCH_SPEND("Batch spend", "Lowest miner fees", false),
    CUSTOM_BATCH_SPEND("Custom batch spend", "Manually select inputs", true),
    RICOCHET("Ricochet", "Lowest miner fees", false),
    RICOCHET_CUSTOM("Custom ricochet", "Manually select inputs", true),
    ;

    private final String caption;
    private final String description;
    private final boolean customSelection;

    EnumCoinSelectionType(
            final String caption,
            final String description,
            final boolean customSelection) {

        this.caption = caption;
        this.description = description;
        this.customSelection = customSelection;
    }

    public String getCaption() {
        return caption;
    }

    public String getDescription() {
        return description;
    }

    public boolean isCustomSelection() {
        return customSelection;
    }
}
