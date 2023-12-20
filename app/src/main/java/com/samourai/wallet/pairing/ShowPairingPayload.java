package com.samourai.wallet.pairing;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

public class ShowPairingPayload extends BottomSheetDialogFragment {

    private String pairingPayload;
    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_pairing_payload, null);
        pairingPayload =  getArguments().getString("payload");
        ImageView pairingQR = view.findViewById(R.id.qr_pairing_code);

        Disposable disposable = generateQRCode("here pairing payload").subscribe(pairingQR::setImageBitmap);
        disposables.add(disposable);

        return view;
    }



    private Observable<Bitmap> generateQRCode(String uri) {
        return Observable.fromCallable(() -> {
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 250);
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                e.printStackTrace();
            }
            return bitmap;
        });
    }
}