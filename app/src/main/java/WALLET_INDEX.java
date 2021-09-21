package com.samourai.wallet.hd;

import com.samourai.wallet.send.SendParams;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum WALLET_INDEX {

  BIP44_RECEIVE(WhirlpoolAccount.DEPOSIT, AddressType.LEGACY, Chain.RECEIVE),
  BIP44_CHANGE(WhirlpoolAccount.DEPOSIT, AddressType.LEGACY, Chain.CHANGE),

  BIP49_RECEIVE(WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_COMPAT, Chain.RECEIVE),
  BIP49_CHANGE(WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_COMPAT, Chain.CHANGE),

  BIP84_RECEIVE(WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_NATIVE, Chain.RECEIVE),
  BIP84_CHANGE(WhirlpoolAccount.DEPOSIT, AddressType.SEGWIT_NATIVE, Chain.CHANGE),

  PREMIX_RECEIVE(WhirlpoolAccount.PREMIX, AddressType.SEGWIT_NATIVE, Chain.RECEIVE),
  PREMIX_CHANGE(WhirlpoolAccount.PREMIX, AddressType.SEGWIT_NATIVE, Chain.CHANGE),

  POSTMIX_RECEIVE(WhirlpoolAccount.POSTMIX, AddressType.SEGWIT_NATIVE, Chain.RECEIVE),
  POSTMIX_CHANGE(WhirlpoolAccount.POSTMIX, AddressType.SEGWIT_NATIVE, Chain.CHANGE),

  BADBANK_RECEIVE(WhirlpoolAccount.BADBANK, AddressType.SEGWIT_NATIVE, Chain.RECEIVE),
  BADBANK_CHANGE(WhirlpoolAccount.BADBANK, AddressType.SEGWIT_NATIVE, Chain.CHANGE);

  private static final Logger log = LoggerFactory.getLogger(WALLET_INDEX.class.getSimpleName());
  private WhirlpoolAccount account;
  private AddressType addressType;
  private Chain chain;

  WALLET_INDEX(WhirlpoolAccount account, AddressType addressType, Chain chain) {
    this.account = account;
    this.addressType = addressType;
    this.chain = chain;
  }

  public static WALLET_INDEX find(WhirlpoolAccount account, AddressType addressType, Chain chain) {
    for (WALLET_INDEX walletIndex : WALLET_INDEX.values()) {
      if (walletIndex.account == account && walletIndex.addressType == addressType && walletIndex.chain == chain) {
        return walletIndex;
      }
    }
    log.error("WALLET_INDEX not found: "+account+"/"+addressType+"/"+chain);
    return null;
  }

  public static WALLET_INDEX findChangeIndex(int account, int addressType) {
    if (account == WhirlpoolAccount.POSTMIX.getAccountIndex()) {
      return WALLET_INDEX.POSTMIX_CHANGE;
    }
    /* if (account == WhirlpoolAccount.PREMIX.getAccountIndex()) {
      return WALLET_INDEX.PREMIX_CHANGE;
    } */
    if (addressType == 84) {
      return WALLET_INDEX.BIP84_CHANGE;
    } else if (addressType == 49) {
      return WALLET_INDEX.BIP49_CHANGE;
    } else {
      return WALLET_INDEX.BIP44_CHANGE;
    }
  }

  public WhirlpoolAccount getAccount() {
    return account;
  }

  public int getAccountIndex() {
    return account.getAccountIndex();
  }

  public AddressType getAddressType() {
    return addressType;
  }

  public Chain getChain() {
    return chain;
  }

  public int getChainIndex() {
    return chain.getIndex();
  }
}
