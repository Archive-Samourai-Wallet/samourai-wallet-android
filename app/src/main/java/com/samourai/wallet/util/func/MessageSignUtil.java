package com.samourai.wallet.util.func;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.samourai.wallet.R;
import com.samourai.wallet.SamouraiWallet;
import com.samourai.wallet.util.MessageSignUtilGeneric;

import org.bitcoinj.core.ECKey;

import java.security.SignatureException;
import java.sql.Date;

public class MessageSignUtil {

    private static Context context = null;
    private static MessageSignUtil instance = null;

    private MessageSignUtil() { ; }

    public static MessageSignUtil getInstance() {

        if(instance == null) {
            instance = new MessageSignUtil();
        }

        return instance;
    }

    public static MessageSignUtil getInstance(Context ctx) {

        context = ctx;

        if(instance == null) {
            instance = new MessageSignUtil();
        }

        return instance;
    }

    public void doSign(String title, String message1, String message2, final ECKey ecKey) {

        final String strDate = new Date(System.currentTimeMillis()).toLocaleString();
        final String message = message2 + " " + strDate;

        final EditText etMessage = new EditText(context);
        etMessage.setHint(message);

        AlertDialog.Builder dlg = new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message1)
                .setView(etMessage)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        dialog.dismiss();

                        String strSignedMessage = null;
                        String result = etMessage.getText().toString();
                        if(result == null || result.length() == 0)    {
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(ecKey, message);
                        }
                        else    {
                            strSignedMessage = MessageSignUtil.getInstance().signMessageArmored(ecKey, result);
                        }

                        TextView showText = new TextView(context);
                        showText.setText(strSignedMessage);
                        showText.setTextIsSelectable(true);
                        showText.setPadding(40, 10, 40, 10);
                        showText.setTextSize(18.0f);

                        String finalStrSignedMessage = strSignedMessage;
                        new AlertDialog.Builder(context)
                                .setTitle(R.string.app_name)
                                .setView(showText)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok, (dialog12, whichButton12) -> dialog12.dismiss())
                                .setNegativeButton(context.getString(R.string.copy),
                                        (dialog1, whichButton1) -> {
                                            ClipboardManager clipboard = (ClipboardManager)
                                                    context.getSystemService(Context.CLIPBOARD_SERVICE);
                                            ClipData clip = ClipData.newPlainText("", finalStrSignedMessage);
                                            clipboard.setPrimaryClip(clip);
                                            Toast.makeText(context, context.getString(R.string.message_copied), Toast.LENGTH_SHORT).show();
                                        }
                                ).show();


                    }

                }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });

        dlg.show();

    }

    public boolean verifySignedMessage(final String addr,
                                       final String msg,
                                       final String signature) throws SignatureException {

        return  MessageSignUtilGeneric.getInstance().verifySignedMessage(
                addr,
                msg,
                signature,
                SamouraiWallet.getInstance().getCurrentNetworkParams());
    }

    public String signMessage(ECKey key, String strMessage) {

        if(key == null || strMessage == null || !key.hasPrivKey())    {
            return null;
        }

        return key.signMessage(strMessage);
    }

    public String signMessageArmored(ECKey key, String strMessage) {

        String sig = signMessage(key, strMessage);
        String ret = null;

        if(sig != null)    {
            ret = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n";
            ret += strMessage;
            ret += "\n";
            ret += "-----BEGIN BITCOIN SIGNATURE-----\n";
            ret += "Version: Bitcoin-qt (1.0)\n";
            ret += "Address: " + key.toAddress(SamouraiWallet.getInstance().getCurrentNetworkParams()).toString() + "\n\n";
            ret += sig;
            ret += "\n";
            ret += "-----END BITCOIN SIGNATURE-----\n";
        }

        return ret;
    }

    private ECKey signedMessageToKey(String strMessage, String strSignature) throws SignatureException {

        if(strMessage == null || strSignature == null)    {
            return null;
        }

        return ECKey.signedMessageToKey(strMessage, strSignature);
    }

}
