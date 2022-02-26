package com.samourai.whirlpool.client.wallet;

import com.samourai.wallet.api.backend.beans.PushTxAddressReuseException;
import com.samourai.wallet.api.backend.beans.UnspentOutput;
import com.samourai.wallet.hd.AddressType;
import com.samourai.wallet.hd.HD_Address;
import com.samourai.wallet.segwit.SegwitAddress;
import com.samourai.whirlpool.client.tx0.Tx0;
import com.samourai.whirlpool.client.tx0.Tx0Config;
import com.samourai.whirlpool.client.tx0.Tx0Preview;
import com.samourai.whirlpool.client.tx0.Tx0Previews;
import com.samourai.whirlpool.client.utils.DebugUtils;
import com.samourai.whirlpool.client.wallet.beans.Tx0FeeTarget;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolAccount;
import com.samourai.whirlpool.client.wallet.beans.WhirlpoolUtxo;
import com.samourai.whirlpool.client.wallet.data.MockServerApi;
import com.samourai.whirlpool.client.wallet.data.dataSource.DataSourceWithStrictMode;
import com.samourai.whirlpool.client.whirlpool.beans.Pool;
import com.samourai.whirlpool.protocol.WhirlpoolProtocol;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.params.TestNet3Params;
import org.bouncycastle.util.encoders.Hex;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Ignore
public class WhirlpoolWalletTest extends AbstractWhirlpoolTest {
    private Logger log = LoggerFactory.getLogger(WhirlpoolWalletTest.class);

    @Before
    public void setUp() throws Exception {
        super.setUp(TestNet3Params.get());
    }

    @Test
    public void testStart() throws Exception {
        // start whirlpool wallet
        whirlpoolWallet.startAsync().blockingAwait();

        // list pools
        Collection<Pool> pools = whirlpoolWallet.getPoolSupplier().getPools();
        Assert.assertTrue(!pools.isEmpty());

        // find pool by poolId
        Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById("0.01btc");
        Assert.assertNotNull(pool);

        // list premix utxos
        Collection<WhirlpoolUtxo> utxosPremix = whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.PREMIX);
        log.info(utxosPremix.size()+" PREMIX utxos:");
        log.info(DebugUtils.getDebugUtxos(utxosPremix, 9999999));

        // list postmix utxos
        Collection<WhirlpoolUtxo> utxosPostmix = whirlpoolWallet.getUtxoSupplier().findUtxos(WhirlpoolAccount.POSTMIX);
        log.info(utxosPostmix.size()+" POSTMIX utxos:");
        log.info(DebugUtils.getDebugUtxos(utxosPostmix, 9999999));

        // keep running
        /*for(int i=0; i<50; i++) {
            MixingState mixingState = whirlpoolWallet.getMixingState();
            log.debug("WHIRLPOOL: "+mixingState.getNbQueued()+" queued, "+mixingState.getNbMixing()+" mixing: "+mixingState.getUtxosMixing());

            synchronized (this) {
                wait(10000);
            }
        }*/
    }

    @Test
    public void testTx0() throws Exception {
        Collection<UnspentOutput> spendFroms = new LinkedList<>();

        HD_Address inputAddress = whirlpoolWallet.getWalletDeposit().getAddressAt(0,61);
        String inputAddressBech32 = inputAddress.getAddressString(AddressType.SEGWIT_NATIVE);
        ECKey ecKey = inputAddress.getECKey();
        UnspentOutput unspentOutput = newUnspentOutput(
                "cc588cdcb368f894a41c372d1f905770b61ecb3fb8e5e01a97e7cedbf5e324ae", 1, "xpub", inputAddressBech32, 500000000, 1);
        unspentOutput.addr = new SegwitAddress(ecKey, networkParameters).getBech32AsString();
        spendFroms.add(unspentOutput);
        utxoSupplier.setKey(unspentOutput.computeOutpoint(networkParameters), ecKey);

        Pool pool = whirlpoolWallet.getPoolSupplier().findPoolById("0.01btc");
        Tx0Config tx0Config = whirlpoolWallet.getTx0Config(Tx0FeeTarget.BLOCKS_2, Tx0FeeTarget.BLOCKS_2);
        Tx0Previews tx0Previews = whirlpoolWallet.tx0Previews(tx0Config, spendFroms);
        Tx0Preview tx0Preview = tx0Previews.getTx0Preview(pool.getPoolId());
        Tx0 tx0 = whirlpoolWallet.tx0(spendFroms, tx0Config, pool);

        Assert.assertEquals("0.01btc", tx0.getPool().getPoolId());
        Assert.assertEquals(MockServerApi.MOCK_FEE_ADDRESS, tx0.getTx0Data().getFeeAddress());
        Assert.assertEquals(MockServerApi.MOCK_FEE_PAYLOAD, WhirlpoolProtocol.encodeBytes(tx0.getTx0Data().getFeePayload()));
        Assert.assertNull(tx0.getTx0Data().getMessage());
        Assert.assertEquals(25520, tx0.getTx0MinerFee());
        Assert.assertEquals(211750, tx0.getMixMinerFee());
        Assert.assertEquals(3025, tx0.getPremixMinerFee());
        Assert.assertEquals(10, tx0.getTx0MinerFeePrice());
        Assert.assertEquals(10, tx0.getMixMinerFeePrice());
        Assert.assertEquals(42500, tx0.getFeeValue());
        Assert.assertEquals(0, tx0.getFeeChange());
        Assert.assertEquals(0, tx0.getFeeDiscountPercent());
        Assert.assertEquals(1003025, tx0.getPremixValue());
        Assert.assertEquals(429720230, tx0.getChangeValue());
        Assert.assertEquals(70, tx0.getNbPremix());
        Assert.assertEquals(70, tx0.getPremixOutputs().size());
        Assert.assertEquals(1, tx0.getChangeOutputs().size());
        Assert.assertEquals("dbd0fa1d36e4cfdf24bf05e2eef4c107a73ed888fc917481913b162616ed6f34", tx0.getTx().getHashAsString());
        Assert.assertEquals("01000000000101ae24e3f5dbcee7971ae0e5b83fcb1eb67057901f2d371ca494f868b3dc8c58cc0100000000ffffffff490000000000000000426a40c5f4e556c2920792839cb6daeafa4e04e5bda3b32235fc9663e7b0f8e457f11848cbbd476e0c618a489b04f66d87dd76efbe5103d4e2ce6b569f741a1848740104a6000000000000160014dc8afa52ec75659f57dffe795c3e0cc3f6fb3d22114e0f0000000000160014004f284de19d8cdc7021bc756bb69b11bb22148f114e0f00000000001600140347b1dbc3f037703a9cb3a157e56be504a3e665114e0f00000000001600140634d32f1712a70ed9bdcf1aca2c2f9fb20470cd114e0f00000000001600141068445a0772e85536226597ba534c69f890daf7114e0f000000000016001410f2900d12b6e7f67a74a221e619d611797ef74c114e0f000000000016001411b5f21c2965b2881b392cde36300b31f013c795114e0f0000000000160014233ad055c0c82605d9a3e03283c5cf27568fab90114e0f000000000016001423a8d90571265d311f7d3cdaf61c42d12899a8e3114e0f00000000001600142467c3fa60592eb7789b8b1b403c0abb0c5de53a114e0f000000000016001428d4ea51f9ccaeaa788861a5a11d2940432f7e7f114e0f00000000001600142ed7230a0cbbb087d8aee98a7593e7f57d94be2b114e0f00000000001600142ee01084cfc4a39c021c8699e4db00b6389f3a5e114e0f000000000016001435e119bdf1c9b101adc46fdc96eb7d35be48a7f2114e0f000000000016001437329d432f582d5466618f662915796ea8ef8b61114e0f000000000016001439d85f76a974a0fa90594cb5d1b06894256ef117114e0f000000000016001440f9f885177fd5b4c7be47abd62ada585669c6bc114e0f0000000000160014460c4711c7773aa9c76497bc2d3570400c36c20c114e0f000000000016001449fec93e4171438c95e3ec5c9de78d9742c1d2ba114e0f00000000001600144b87cd93bbbb293fe472ab12eddbf43cf57bef08114e0f00000000001600144bdfde2455abc9195505e86199a1b15e435c5752114e0f0000000000160014546468f753c19ffc0069b7540fc6de60294715a9114e0f00000000001600145a3a5cacc9bb24ae2bf265b3e01a662aaa41251f114e0f000000000016001465cc6ced6980ed3e9ddbd73a0e6025e74a7e9174114e0f0000000000160014697b300d45f65e644178812e781511a6c03ca4ba114e0f0000000000160014697d2dccf90cc000d009fbcdebbc086575984142114e0f00000000001600146e23fbcb9c93103b38ec27c112828c662b22d1dd114e0f00000000001600147427f37f92b119821da5de7c3fc04249f50e624b114e0f00000000001600147551da8b5fa1d6ca4ba49c3736e367866c88d338114e0f00000000001600147b0ee7f491bb2ae200a664a9738b9a902fe5e422114e0f00000000001600147fca4dc09b450163d06349f7827a7e7a21abaf1a114e0f0000000000160014831ab0eee4e96558971a1fa979f5fe268022b530114e0f00000000001600148b30ddf3287ebafc487a42a7995d1c47c25a1882114e0f00000000001600148b9caf5c7e986182c390c6433d025ff90fdac9cd114e0f0000000000160014927cf8b0b8b9965648e3560ddbca119d6b00edda114e0f000000000016001495bafea4dc8082e18f807559f4435b579a784824114e0f000000000016001499d83d248a2d509498e88f13eb835d80e24e1c8a114e0f0000000000160014a1bd2b445a0c7c77553e019ef3c4a2d674a6259e114e0f0000000000160014a3b7a56c481ac8f252ecb3737b9ad7f6fdf62f89114e0f0000000000160014a444dd47e66c33371db5640412dab5e43eba389b114e0f0000000000160014a6f6bc0120a5588375610177f767742896149361114e0f0000000000160014abaad21856444f2a6e5684be4672d8c3cb7bd2af114e0f0000000000160014ad2a88f473aa6944cdc919b8ddb354a97c6e6c58114e0f0000000000160014b4588a27b2275d4bd558612bff6e23fcc80a54f1114e0f0000000000160014b7a58575140d0f143fb7619a7baddc8321f90e23114e0f0000000000160014ba685d4e136c51e6d24ee4865b3a9bc2ff100b8f114e0f0000000000160014bac0145bfbcae915ba838acf1e130f8936490537114e0f0000000000160014bac25c9ada2fe55df0fcf15a0d6804dc8b640c80114e0f0000000000160014bee680c84b50713440d5e386e29de6bf65dd119b114e0f0000000000160014c1aecf1ac3d3ec9db81c3450498eb4449e03c287114e0f0000000000160014c42f48010c1e577f5df1d6d473f5b9fb9e59c194114e0f0000000000160014c64839aa111c90a79e5114cf26fa173f1396c137114e0f0000000000160014c8829c5d19bf805101123c0bbc7c411b2df0d64b114e0f0000000000160014c8c1da2894cad713be9ef0b16844ffe47fe0b4f6114e0f0000000000160014d3172e2f519074045e2e475ff99e48659cd1bdd6114e0f0000000000160014d4001cd735c523df3865bdb33f46f2f44d9c7e4e114e0f0000000000160014d7aef3e19c028891ee981b5741b9fd27ced884b0114e0f0000000000160014d917bd7165cd471c04c0ed1f5ea328389ab02e02114e0f0000000000160014da863d8656e126e078597bd356bb0d95a80fe308114e0f0000000000160014dbd6e225c03e43e297167c8e894a1235bda1423b114e0f0000000000160014dc3fcf0056b8cd82ab4dfd11156e50de3beb1869114e0f0000000000160014e445237704b312052f1f59981dcd211712255f67114e0f0000000000160014e633e0a29ad1ea7f4cd21566f2b0026716a27ec1114e0f0000000000160014ea7ba15f292747e9f9d6df9d4354afce0610dd0a114e0f0000000000160014eb60d4f6139475843929e35c761cbee6a1cb4491114e0f0000000000160014edb30a7dab18d2dbc85f95260b407f117c11565f114e0f0000000000160014f45fae1988cdb4a1dbddc6726ea83504018bf555114e0f0000000000160014f98378e676ce9193703025b4d5f9967c35b5a180114e0f0000000000160014faa759e6c68cb028e42ce9c1c616f5605dffe127114e0f0000000000160014fc29204f85df294d34c9b60015c5d70c2d2fdc76114e0f0000000000160014fe472841ac3242fc7791ff1ba08862b6a5607f9ba6029d19000000001600149acdf5fc120fb737ff703b4a607570e239d7325402473044022064fdcb5a26b4ac175869ae2911a31a51aa613b1731b30c0e9d1ef0bce6297d49022072d29ff3eb19a183f41d2619a06dcb0fb92b6fefd0fecbdec345a7e0bb42502301210355fc7bdebe776afd29bb88b707f571434df6a7a7d3f0d423601f2bfffdc2ddf500000000", new String(Hex.encode(tx0.getTx().bitcoinSerialize())));
    }

    @Test
    public void pushTx_strictReuse() throws Exception{
        String raw = "01000000000101d64a5408b6e96ccb2543e89eccc1fdd67908d66f8bfedb0925f385e12dbd4d811b00000000ffffffff1c0000000000000000426a4015430cf31ce5faadf6f83c0f9dadf22442857814de0f9655e8b14d9c507f66e265aa3baffcfaa4ed7d2094bd52c206649aec67a351fddaacb9ade56295729a1e88130000000000001600149b94b8a53befa7f7a9ddc70511e040d311168511ce870100000000001600140c52d7aeb7ebd6099dd0d1f4db031d509b25e685ce8701000000000016001410e17ee9e34cd658750327e716dd47a92d21dae0ce870100000000001600141e29d9d0b67a1dea00c39d00ec2f6b88b123e5d6ce870100000000001600142af5e134eef5354b34380c178adb3176fa5b0433ce8701000000000016001435aa9ab779c01925ee1803291721cd75f110294fce870100000000001600143dbad33301cec1912bc1557455a3a54a762a50e1ce87010000000000160014520b8b56de1ed52799d0e2a005fa83887fb61b9ece8701000000000016001454200beab2eec98dd823b38b02b09b685747c060ce870100000000001600145b3cfa0bbbacd3dbbd2132bc21f8976023fa8854ce870100000000001600145d1f4806910db9169f9948455c14d05213f124a5ce8701000000000016001464f0f1a2ebefd51c9c33fbb6d8fa409f38f2574dce870100000000001600148fe0e7680b35ce7a04b1d3d1dd1648a6c5eb878dce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce8701000000000016001498e884a8e01e74abec991f63b177c32fe9325f2fce87010000000000160014b318b660be76d4de1f3d2aa01ea139efff0b8fc9ce87010000000000160014d808d713a5e564fc36456c52473f6b7d5730de87ce87010000000000160014df11c688573f144542b6188890d015043f28fdf7ce87010000000000160014e6911a2111293fa147692e20b809cbc9471a74e2ce87010000000000160014edbf73dc57e38a4efae41cf674255d1019b42e99ce87010000000000160014ee6fce9ca983a97023522d2fcd898e9ddcf2583ece87010000000000160014fd255c8fa497b9077c56eec21e5bba647c1ca782cd43160000000000160014942219f5020596ce6bb1e73f74c12b00e97514b80247304402204298dee5523bea47455f7762728b2ff8d5b927a98a48a5c72e5ccf19cc5293e4022063fa7cf77b31f971071274f5478f7dced26d4aee698496ea81b595e357b3a141012103f19bd0c973ad4158b5ecf24607f0bba6f2683180e852a5f8df17bcb5c03272d600000000";
        String strictVoutsStr = "23|6|5|2|13|24|12|3|26|22|9|25|21|7|8|11|10|4|20|14|15|16|17|18|19|27";
        List<Integer> strictVouts = Arrays.stream(strictVoutsStr.split("\\|")).map(s -> Integer.parseInt(s)).collect(Collectors.toList());
        try {
            ((DataSourceWithStrictMode) whirlpoolWallet.getDataSource()).pushTx(raw, strictVouts); // should throw
            Assert.assertTrue(false);
        } catch (PushTxAddressReuseException e) {
            Integer[] reusedOutputs = new Integer[]{23, 6, 5, 2, 13, 24, 12, 3, 26, 22, 9, 25, 21, 7, 8, 11, 10, 4, 20, 14, 15, 16, 17, 18, 19, 27};
            Assert.assertArrayEquals(reusedOutputs, e.getAdressReuseOutputIndexs().toArray(new Integer[]{}));
        }
    }
}