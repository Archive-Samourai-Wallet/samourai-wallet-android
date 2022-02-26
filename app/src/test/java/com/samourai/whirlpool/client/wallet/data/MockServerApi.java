package com.samourai.whirlpool.client.wallet.data;

import com.samourai.http.client.IHttpClientService;
import com.samourai.whirlpool.client.whirlpool.ServerApi;
import com.samourai.whirlpool.protocol.rest.Tx0DataRequestV2;
import com.samourai.whirlpool.protocol.rest.Tx0DataResponseV2;

import io.reactivex.Observable;
import java8.util.Optional;

public class MockServerApi extends ServerApi {
    public static final String MOCK_FEE_ADDRESS = "tb1qmj9055hvw4je747lleu4c0svc0m0k0fz3ha2ln";
    public static final String MOCK_FEE_PAYLOAD = "009613+VJu0000000000000000000000000000000000000000000000000000000000000000000000";

    public MockServerApi(String urlServer, IHttpClientService httpClientService) {
        super(urlServer, httpClientService);
    }

    @Override
    public Observable<Optional<Tx0DataResponseV2>> fetchTx0Data(Tx0DataRequestV2 tx0DataRequest) throws Exception {
        return super.fetchTx0Data(tx0DataRequest).map(tx0DataResponseV2Optional -> {
            // mock static fee address for reproductible tests
            for (Tx0DataResponseV2.Tx0Data tx0Data : tx0DataResponseV2Optional.get().tx0Datas) {
                tx0Data.feeAddress = MOCK_FEE_ADDRESS;
                tx0Data.feePayload64 = MOCK_FEE_PAYLOAD;
            }
            return tx0DataResponseV2Optional;
        });
    }
}
