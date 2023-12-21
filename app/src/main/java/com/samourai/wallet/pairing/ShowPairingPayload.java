package com.samourai.wallet.pairing;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

    private final CompositeDisposable disposables = new CompositeDisposable();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottomsheet_pairing_payload, null);
        String pairingPayload = getArguments().getString("payload");
        String passphrase = getArguments().getString("password");
        String type = getArguments().getString("type");


        ImageView pairingQR = view.findViewById(R.id.qr_pairing_code);
        ImageView copyBtn = view.findViewById(R.id.copy_pairing_payload);
        TextView passwordText = view.findViewById(R.id.passwordText);
        TextView pwdTypeText = view.findViewById(R.id.textView1);
        passwordText.setText(passphrase);

        TextView payloadTypeText = view.findViewById(R.id.payload_type_text);
        if (type != null && type.equals("full")) {
            payloadTypeText.setText("Full wallet pairing code");
            pwdTypeText.setText("Login password");
        }

        if (passphrase != null && passphrase.equals("Your BIP39 Passphrase"))
            passwordText.setTextColor(getResources().getColor(R.color.white));

        copyBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clipData = android.content.ClipData
                        .newPlainText("Pairing Payload", pairingPayload);
                if (cm != null) {
                    cm.setPrimaryClip(clipData);
                    Toast.makeText(getActivity().getApplicationContext(), getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show();
                }
            }
        });

        Disposable disposable = generateQRCode(pairingPayload).subscribe(pairingQR::setImageBitmap);
        disposables.add(disposable);

        return view;
    }



    private Observable<Bitmap> generateQRCode(String uri) {
        return Observable.fromCallable(() -> {
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 500);
            try {
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (WriterException e) {
                e.printStackTrace();
            }
            return bitmap;
        });
    }
}