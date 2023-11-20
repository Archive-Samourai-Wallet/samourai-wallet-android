package com.samourai.wallet.pin;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Looper;
import android.text.InputType;
import android.transition.ChangeBounds;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.collect.Lists;
import com.samourai.wallet.R;
import com.samourai.wallet.RecoveryWordsActivity;
import com.samourai.wallet.access.AccessFactory;
import com.samourai.wallet.crypto.AESUtil;
import com.samourai.wallet.crypto.DecryptionException;
import com.samourai.wallet.hd.HD_Wallet;
import com.samourai.wallet.hd.HD_WalletFactory;
import com.samourai.wallet.payload.ExternalBackupManager;
import com.samourai.wallet.payload.PayloadUtil;
import com.samourai.wallet.util.func.AddressFactory;
import com.samourai.wallet.util.tech.AppUtil;
import com.samourai.wallet.util.CharSequenceX;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.TimeOutUtil;
import com.samourai.wallet.widgets.PinEntryView;

import org.apache.commons.codec.DecoderException;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.crypto.MnemonicException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class PinEntryManager {

    private final static int MAX_ATTEMPTS = 6;
    private final FragmentActivity activity;
    private final int layoutResource;
    private final boolean attemptToLog;
    private View viewBuilt;

    private ImageButton tsend = null;
    private ImageButton tback = null;

    private StringBuilder userInput = null;

    private boolean create = false;             // create PIN
    private boolean confirm = false;            // confirm PIN
    private String strConfirm = null;
    private String strSeed = null;
    private String strPassphrase = "";

    private String strUri = null;

    private int failures = 0;
    private PinEntryView pinEntryView;
    private LinearLayout restoreLayout;
    private LinearLayout pinEntryMaskLayout;
    private TextView walletStatusTextView;
    private ProgressBar progressBar;

    private final List<OnSuccessCallback> onSuccessCallbackList = Lists.newLinkedList();

    public interface OnSuccessCallback {
        void onSuccess();
    }

    private PinEntryManager(final FragmentActivity activity,
                            final int layoutResource,
                            final boolean attemptToLog) {

        this.activity = activity;
        this.layoutResource = layoutResource;
        this.attemptToLog = attemptToLog;
    }

    public static PinEntryManager create(final FragmentActivity activity,
                                         final int layoutResource,
                                         final boolean attemptToLog) {

        return new PinEntryManager(activity, layoutResource, attemptToLog);
    }

    public View getView() {
        if (isNull(viewBuilt)) {
            viewBuilt = activity.getLayoutInflater().inflate(layoutResource, null);
        }
        return viewBuilt;
    }

    public PinEntryManager setTitle(final String title) {
        return setTitleOnView(title);
    }

    public PinEntryManager setDescription(final String description) {
        return setDescriptionOnView(description);
    }

    private PinEntryManager setTitleOnView(final String title) {
        final TextView textView = getView().findViewById(R.id.pin_entry_title);
        if (nonNull(textView)) {
            textView.setText(title);
        }
        return this;
    }

    private PinEntryManager setDescriptionOnView(final String description) {
        final TextView textView = getView().findViewById(R.id.pin_entry_wallet_status);
        if (nonNull(textView)) {
            textView.setText(description);
        }
        return this;
    }

    public PinEntryManager install(final OnSuccessCallback onSuccessCallback) {

        if (nonNull(onSuccessCallback)) {
            onSuccessCallbackList.add(onSuccessCallback);
        }

        final View viewBuilt = getView();

        if (attemptToLog && PrefsUtil.getInstance(activity).getValue(PrefsUtil.ATTEMPTS, 0) > 0) {
            failures = PrefsUtil.getInstance(activity).getValue(PrefsUtil.ATTEMPTS, 0);
        }
        userInput = new StringBuilder();
        pinEntryView = viewBuilt.findViewById(R.id.pinentry_view);
        walletStatusTextView = viewBuilt.findViewById(R.id.pin_entry_wallet_status);
        restoreLayout = viewBuilt.findViewById(R.id.pin_entry_restore);
        MaterialButton restoreBtn = viewBuilt.findViewById(R.id.pin_entry_restore_btn);
        pinEntryMaskLayout = viewBuilt.findViewById(R.id.pin_entry_mask_layout);
        progressBar = viewBuilt.findViewById(R.id.progress_pin_entry);
        activity.getWindow().setStatusBarColor(ContextCompat.getColor(activity, R.color.window));
        restoreLayout.setVisibility(View.GONE);
        pinEntryView.setEntryListener((key, view) -> {
            if (userInput.length() <= (AccessFactory.MAX_PIN_LENGTH - 1)) {
                userInput = userInput.append(key);
                if (userInput.length() >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton();
                } else {
                    pinEntryView.hideCheckButton();
                }
                setPinMaskView();
            }
        });
        restoreBtn.setOnClickListener(v -> doBackupRestore());
        pinEntryView.setClearListener(clearType -> {
            if (clearType == PinEntryView.KeyClearTypes.CLEAR) {
                if (userInput.length() != 0)
                    userInput = new StringBuilder(userInput.substring(0, (userInput.length() - 1)));
                if (userInput.length() >= AccessFactory.MIN_PIN_LENGTH) {
                    pinEntryView.showCheckButton();
                } else {
                    pinEntryView.hideCheckButton();
                }
            } else {
                strPassphrase = "";
                userInput = new StringBuilder();
                pinEntryMaskLayout.removeAllViews();
                pinEntryView.hideCheckButton();
            }
            setPinMaskView();
        });

        boolean scramble = PrefsUtil.getInstance(activity).getValue(PrefsUtil.SCRAMBLE_PIN, false);

        strUri = PrefsUtil.getInstance(activity).getValue("SCHEMED_URI", "");
        if (strUri.length() > 0) {
            PrefsUtil.getInstance(activity).setValue("SCHEMED_URI", "");
        } else {
            strUri = null;
        }
        if (scramble) {
            pinEntryView.setScramble(true);
        }

        Bundle extras = activity.getIntent().getExtras();

        if (extras != null && extras.containsKey("create") && extras.getBoolean("create")) {
            scramble = false;
            create = true;
            confirm = false;
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(activity, R.string.pin_5_8, Toast.LENGTH_LONG).show();
        } else if (extras != null && extras.containsKey("confirm") && extras.getBoolean("confirm")) {
            scramble = false;
            create = false;
            confirm = true;
            strConfirm = extras.getString("first");
            strSeed = extras.getString("seed");
            strPassphrase = extras.getString("passphrase");
            Toast.makeText(activity, R.string.pin_5_8_confirm, Toast.LENGTH_LONG).show();
        } else {
            if (isLocked()) {
                lockWallet();
            }
        }

        if (strSeed != null && strSeed.length() < 1) {
            strSeed = null;
        }

        if (strPassphrase == null) {
            strPassphrase = "";
        }
        if (!PrefsUtil.getInstance(activity).getValue(PrefsUtil.HAPTIC_PIN, true)) {
            pinEntryView.disableHapticFeedBack();
        }
        pinEntryView.setConfirmClickListener(view -> {

            if (create && strPassphrase.length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {
                Intent intent = new Intent(activity, PinEntryActivity.class);
                intent.putExtra("confirm", true);
                intent.putExtra("create", false);
                intent.putExtra("first", userInput.toString());
                intent.putExtra("seed", strSeed);
                intent.putExtra("passphrase", strPassphrase);
                activity.startActivity(intent);
                activity.finish();
            } else if (confirm && strPassphrase.length() >= AccessFactory.MIN_PIN_LENGTH && userInput.toString().length() <= AccessFactory.MAX_PIN_LENGTH) {

                if (userInput.toString().equals(strConfirm)) {

                    progressBar.setVisibility(View.VISIBLE);

                    initThread(strSeed == null, userInput.toString(), strPassphrase, strSeed == null ? null : strSeed);

                } else {
                    Intent intent = new Intent(activity, PinEntryActivity.class);
                    intent.putExtra("create", true);
                    intent.putExtra("seed", strSeed);
                    intent.putExtra("passphrase", strPassphrase);
                    activity.startActivity(intent);
                    activity.finish();
                }

            } else {
                validateThread(userInput.toString(), strUri);
            }
        });
        
        return this;
    }
    
    private void setPinMaskView() {

        pinEntryMaskLayout.post(() -> {
            if (userInput.length() == 0) {
                pinEntryMaskLayout.removeAllViews();
                return;
            }
            if (userInput.length() > pinEntryMaskLayout.getChildCount() && userInput.length() != 0) {
                ImageView image = new ImageView(activity.getApplicationContext());
                image.setImageDrawable(activity.getResources().getDrawable(R.drawable.circle_dot_white));
                image.getDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.ADD);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(8, 0, 8, 0);
                TransitionManager.beginDelayedTransition(pinEntryMaskLayout, new ChangeBounds().setDuration(50));
                pinEntryMaskLayout.addView(image, params);
            } else {
                if (pinEntryMaskLayout.getChildCount() != 0) {
                    TransitionManager.beginDelayedTransition(pinEntryMaskLayout, new ChangeBounds().setDuration(200));
                    pinEntryMaskLayout.removeViewAt(pinEntryMaskLayout.getChildCount() - 1);
                }
            }
        });
    }
    
    void doBackupRestore() {
        if (ExternalBackupManager.backupAvailable()) {
            Disposable disposable = Single.fromCallable(ExternalBackupManager::read)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((data, throwable) -> {
                        if (throwable != null) {
                            Toast.makeText(activity, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (data != null && data.length() > 0) {

                            final EditText passphrase = new EditText(activity);
                            passphrase.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                            passphrase.setHint(R.string.passphrase);
                            final FrameLayout layout = new FrameLayout(activity);
                            layout.addView(passphrase);

                            MaterialAlertDialogBuilder dlg = new MaterialAlertDialogBuilder(activity)
                                    .setTitle(R.string.app_name)
                                    .setView(layout)
                                    .setMessage(R.string.restore_wallet_from_backup)
                                    .setCancelable(false)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {

                                            final String pw = passphrase.getText().toString();
                                            if (pw == null || pw.length() < 1) {
                                                Toast.makeText(activity, R.string.invalid_passphrase, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                                                activity.finish();
                                            }

                                            String decrypted = null;
                                            try {
                                                decrypted = PayloadUtil.getInstance(activity).getDecryptedBackupPayload(data, new CharSequenceX(pw));
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            if (decrypted == null || decrypted.length() < 1) {
                                                Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                                                activity.finish();
                                            }


                                            progressBar.setVisibility(View.VISIBLE);
                                            final String _decrypted = decrypted;
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    Looper.prepare();

                                                    try {

                                                        JSONObject json = new JSONObject(_decrypted);
                                                        PayloadUtil.getInstance(activity).restoreWalletfromJSON(json, false);
                                                        String guid = AccessFactory.getInstance(activity).createGUID();
                                                        String hash = AccessFactory.getInstance(activity).getHash(guid, new CharSequenceX(AccessFactory.getInstance(activity).getPIN()), AESUtil.DefaultPBKDF2Iterations);
                                                        PrefsUtil.getInstance(activity).setValue(PrefsUtil.ACCESS_HASH, hash);
                                                        PrefsUtil.getInstance(activity).setValue(PrefsUtil.ACCESS_HASH2, hash);
                                                        PayloadUtil.getInstance(activity).saveWalletToJSON(new CharSequenceX(guid + AccessFactory.getInstance().getPIN()));

                                                    } catch (MnemonicException.MnemonicLengthException mle) {
                                                        mle.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } catch (DecoderException de) {
                                                        de.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } catch (JSONException je) {
                                                        je.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } catch (IOException ioe) {
                                                        ioe.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } catch (java.lang.NullPointerException npe) {
                                                        npe.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } catch (DecryptionException de) {
                                                        de.printStackTrace();
                                                        Toast.makeText(activity, R.string.decryption_error, Toast.LENGTH_SHORT).show();
                                                    } finally {
                                                        activity.runOnUiThread(() -> {
                                                            progressBar.setVisibility(View.INVISIBLE);
                                                        });

                                                        new MaterialAlertDialogBuilder(activity)
                                                                .setTitle(R.string.app_name)
                                                                .setMessage(activity.getString(R.string.pin_reminder) + "\n\n" + AccessFactory.getInstance(activity).getPIN())
                                                                .setCancelable(false)
                                                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                                    public void onClick(DialogInterface dialog, int whichButton) {

                                                                        dialog.dismiss();
                                                                        AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                                                                        activity.finish();

                                                                    }
                                                                }).show();

                                                    }

                                                    Looper.loop();

                                                }
                                            }).start();

                                        }
                                    }).setNegativeButton(R.string.cancel, (dialog, whichButton) -> {

                                    });
                            dlg.show();

                        }

                    });
        } else {
            Toast.makeText(activity, "Backup file not available ", Toast.LENGTH_SHORT).show();
            if (!ExternalBackupManager.hasPermissions()) {
                ExternalBackupManager.askPermission(activity);
            }
        }
    }



    boolean isLocked() {
        return PrefsUtil.getInstance(activity.getApplication()).getValue(PrefsUtil.ATTEMPTS, 0) >= MAX_ATTEMPTS;
    }

    void lockWallet() {

        try {
            PayloadUtil.getInstance(activity).wipe();
        } catch(final Exception e) {}

        PrefsUtil.getInstance(activity.getApplication()).setValue(PrefsUtil.PIN_TIMEOUT, 0L);
        pinEntryView.disable(true);
        TransitionManager.beginDelayedTransition((ViewGroup) restoreLayout.getRootView());
        restoreLayout.setVisibility(View.VISIBLE);
        walletStatusTextView.setText(R.string.wallet_locked);
    }

    private void initThread(final boolean create, final String pin, final String passphrase, final String seed) {

        new Thread(() -> {

            Looper.prepare();

            String guid = AccessFactory.getInstance(activity).createGUID();
            String hash = AccessFactory.getInstance(activity).getHash(guid, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations);
            PrefsUtil.getInstance(activity).setValue(PrefsUtil.ACCESS_HASH, hash);
            PrefsUtil.getInstance(activity).setValue(PrefsUtil.ACCESS_HASH2, hash);

            if (create) {

                try {
                    HD_WalletFactory.getInstance(activity).newWallet(12, passphrase);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } finally {
                    ;
                }

            } else if (seed == null) {
                ;
            } else {

                try {
                    HD_WalletFactory.getInstance(activity).restoreWallet(seed, passphrase);
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (DecoderException de) {
                    de.printStackTrace();
                } catch (AddressFormatException afe) {
                    afe.printStackTrace();
                } catch (MnemonicException.MnemonicLengthException mle) {
                    mle.printStackTrace();
                } catch (MnemonicException.MnemonicChecksumException mce) {
                    mce.printStackTrace();
                } catch (MnemonicException.MnemonicWordException mwe) {
                    mwe.printStackTrace();
                } finally {
                    ;
                }

            }

            PrefsUtil.getInstance(activity).setValue(PrefsUtil.SCRAMBLE_PIN, true);

            try {

                String msg = null;

                if (HD_WalletFactory.getInstance(activity).get() != null) {

                    if (create) {
                        msg = activity.getString(R.string.wallet_created_ok);
                    } else {
                        msg = activity.getString(R.string.wallet_restored_ok);
                    }

                    try {
                        AccessFactory.getInstance(activity).setPIN(pin);
                        PayloadUtil.getInstance(activity).saveWalletToJSON(new CharSequenceX(AccessFactory.getInstance(activity).getGUID() + pin));

                        if (create) {
                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.WALLET_ORIGIN, "new");
                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.FIRST_RUN, true);
                        } else {
                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.WALLET_ORIGIN, "restored");
                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.FIRST_RUN, true);
                        }

                    } catch (JSONException | IOException | DecryptionException je) {
                        je.printStackTrace();
                    }

                    for (int i = 0; i < 2; i++) {
                        AddressFactory.getInstance().account2xpub().put(i, HD_WalletFactory.getInstance(activity).get().getAccount(i).xpubstr());
                        AddressFactory.getInstance().xpub2account().put(HD_WalletFactory.getInstance(activity).get().getAccount(i).xpubstr(), i);
                    }

                    //
                    // backup wallet for alpha
                    //
                    if (create) {

                        String seed1 = HD_WalletFactory.getInstance(activity).get().getMnemonic();

                        Intent intent = new Intent(activity, RecoveryWordsActivity.class);
                        intent.putExtra(RecoveryWordsActivity.WORD_LIST, seed1);
                        intent.putExtra(RecoveryWordsActivity.PASSPHRASE, passphrase);
                        activity.startActivity(intent);
                        activity.finish();

                    } else {
                        AccessFactory.getInstance(activity).setIsLoggedIn(true);
                        TimeOutUtil.getInstance().updatePin();
                        AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                        activity.finish();
                    }

                } else {
                    if (create) {
                        msg = activity.getString(R.string.wallet_created_ko);
                    } else {
                        msg = activity.getString(R.string.wallet_restored_ko);
                    }
                }

                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();

            } catch (final Exception e) {
                e.printStackTrace();
            }

            progressBar.setVisibility(View.INVISIBLE);

            Looper.loop();

        }).start();

    }

    private void validateThread(final String pin, final String uri) {

        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            Looper.prepare();

            if (pin.length() < AccessFactory.MIN_PIN_LENGTH || pin.length() > AccessFactory.MAX_PIN_LENGTH) {
                activity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    pinEntryView.resetPinLen();
                });
                Toast.makeText(activity, R.string.pin_error, Toast.LENGTH_SHORT).show();
                AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                activity.finish();
            }

            String randomKey = AccessFactory.getInstance(activity).getGUID();
            if (randomKey.length() < 1) {
                activity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    pinEntryView.resetPinLen();
                });
                Toast.makeText(activity, R.string.random_key_error, Toast.LENGTH_SHORT).show();
                AppUtil.getInstance(activity).restartApp(activity.getIntent().getExtras());
                activity.finish();
            }

            String hash = PrefsUtil.getInstance(activity).getValue(PrefsUtil.ACCESS_HASH, "");
            if (AccessFactory.getInstance(activity).validateHash(hash, randomKey, new CharSequenceX(pin), AESUtil.DefaultPBKDF2Iterations)) {

                AccessFactory.getInstance(activity).setPIN(pin);

                try {

                    if (attemptToLog) {
                        HD_Wallet hdw = PayloadUtil.getInstance(activity).restoreWalletfromJSON(new CharSequenceX(AccessFactory.getInstance(activity).getGUID() + pin));

                        if (isNull(hdw)) {

                            activity.runOnUiThread(() -> {
                                incFailures();
                                PrefsUtil.getInstance(activity).setValue(PrefsUtil.ATTEMPTS, failures);
                                pinEntryMaskLayout.removeAllViews();
                                pinEntryView.hideCheckButton();
                                pinEntryView.resetPinLen();
                                setPinMaskView();
                                managePinFailureCount();
                            });

                        } else {
                            PrefsUtil.getInstance(activity).setValue(PrefsUtil.ATTEMPTS, 0);
                        }
                    }

                    fireOnSuccessMessage();

                } catch (final Exception e) {
                    e.printStackTrace();
                } finally {
                    activity.runOnUiThread(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                    });
                }

            } else {
                activity.runOnUiThread(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    incFailures();
                    PrefsUtil.getInstance(activity).setValue(PrefsUtil.ATTEMPTS, failures);
                    userInput = new StringBuilder();
                    pinEntryMaskLayout.removeAllViews();
                    pinEntryView.hideCheckButton();
                    pinEntryView.resetPinLen();
                    managePinFailureCount();
                });

            }

            activity.runOnUiThread(() -> {
                progressBar.setVisibility(View.INVISIBLE);

            });

        }).start();

    }

    private void managePinFailureCount() {
        if (attemptToLog) {
            if (failures < MAX_ATTEMPTS) {
                walletStatusTextView.setText(activity.getText(R.string.login_error) + ": " + failures + "/" + MAX_ATTEMPTS);
            } else {
                failures = 0;
                lockWallet();
            }
        } else {
            walletStatusTextView.setText(activity.getText(R.string.invalid_pin_entered));
        }
    }

    private void incFailures() {
        if (attemptToLog) {
            ++ failures;
        }
    }

    private void fireOnSuccessMessage() {
        for (final OnSuccessCallback onSuccessCallback : onSuccessCallbackList) {
            onSuccessCallback.onSuccess();
        }
    }
}
