package com.samourai.whirlpool.client.wallet.data.dataPersister;

import android.content.Context;

import com.samourai.whirlpool.client.exception.NotifiableException;
import com.samourai.whirlpool.client.wallet.WhirlpoolUtils;
import com.samourai.whirlpool.client.wallet.data.dataPersister.AndroidFileDataPersisterFactory;

import java.io.File;

public class MockAndroidFileDataPersisterFactory extends AndroidFileDataPersisterFactory {
    private File fileIndex;
    private File fileUtxo;

    public MockAndroidFileDataPersisterFactory(WhirlpoolUtils whirlpoolUtils, Context ctx, File fileIndex, File fileUtxo) {
        super(whirlpoolUtils, ctx);
        this.fileIndex = fileIndex;
        this.fileUtxo = fileUtxo;
    }
    @Override
    protected File computeFileIndex(String walletIdentifier) throws NotifiableException {
        return fileIndex.getAbsoluteFile();
    }

    @Override
    protected File computeFileUtxos(String walletIdentifier) throws NotifiableException {
        return fileUtxo.getAbsoluteFile();
    }
}
