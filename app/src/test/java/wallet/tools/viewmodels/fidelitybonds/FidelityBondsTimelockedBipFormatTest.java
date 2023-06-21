package wallet.tools.viewmodels.fidelitybonds;

import com.samourai.wallet.segwit.FidelityTimelockAddress;
import com.samourai.wallet.tools.viewmodels.fidelitybonds.FidelityBondsTimelockedBipFormat;

import org.bitcoinj.core.ECKey;
import org.junit.Assert;
import org.junit.Test;

public class FidelityBondsTimelockedBipFormatTest {

    @Test
    public void should_convert_index_time_lock_in_good_time_lock() throws Exception {
        for (int timeIndex = 0; timeIndex < 960; ++ timeIndex) {
            final FidelityTimelockAddress fAddress = new FidelityTimelockAddress((ECKey) null, null, timeIndex);
            final long timelock = FidelityBondsTimelockedBipFormat.create(timeIndex).getTimelock();
            Assert.assertEquals(fAddress.getTimelock(), timelock);
        }
    }

}
