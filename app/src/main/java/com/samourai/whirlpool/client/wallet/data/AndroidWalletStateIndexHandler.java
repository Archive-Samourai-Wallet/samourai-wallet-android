package com.samourai.whirlpool.client.wallet.data;

import com.samourai.wallet.client.indexHandler.AbstractIndexHandler;
import com.samourai.wallet.constants.WALLET_INDEX;
import com.samourai.wallet.util.func.AddressFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AndroidWalletStateIndexHandler extends AbstractIndexHandler {
  private static final Logger log = LoggerFactory.getLogger(AndroidWalletStateIndexHandler.class);

  private AddressFactory addressFactory;
  private WALLET_INDEX walletIndex;

  public AndroidWalletStateIndexHandler(AddressFactory addressFactory, WALLET_INDEX walletIndex) {
    super();
    this.addressFactory = addressFactory;
    this.walletIndex = walletIndex;
  }

  @Override
  public int get() {
    int idx = addressFactory.getAddress(walletIndex).getLeft();
    return idx;
  }

  @Override
  public synchronized int getAndIncrement() {
    int idx = addressFactory.getAddressAndIncrement(walletIndex).getLeft();
    return idx;
  }

  @Override
  public synchronized void set(int value) {
    addressFactory.setWalletIdx(walletIndex, value, true);
  }
}