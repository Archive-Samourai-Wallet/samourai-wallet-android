package com.samourai.wallet.pairing;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.R;
import com.samourai.wallet.util.tech.BitmapHelper;

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

        if (passphrase != null && passphrase.equals("your BIP39 Passphrase")) {
            passwordText.setTextColor(getResources().getColor(R.color.white));
            Typeface customTypeface = ResourcesCompat.getFont(requireContext(), R.font.raleway_regular);
            passwordText.setTypeface(customTypeface);
        }

        copyBtn.setOnClickListener((event) -> new MaterialAlertDialogBuilder(getContext())
                .setTitle(R.string.app_name)
                .setMessage(R.string.pairing_payload_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = null;
                    clip = android.content.ClipData.newPlainText("pairing_payload", pairingPayload);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(getContext(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                }).setNegativeButton(R.string.no, (dialog, whichButton) -> {
                }).show());

        Disposable disposable = generateQRCode(pairingPayload).subscribe(pairingQR::setImageBitmap);
        disposables.add(disposable);

        return view;
    }

    private Observable<Bitmap> generateQRCode(String uri) {
        return Observable.fromCallable(() -> {
            Bitmap bitmap = null;
            QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), 1000);
            try {
                //bitmap = BitmapHelper.cropBitmap(qrCodeEncoder.encodeAsBitmap(), 50);
                bitmap = qrCodeEncoder.encodeAsBitmap();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return bitmap;
        });
    }

}