package com.samourai.wallet;

import static com.samourai.wallet.util.SatoshiBitcoinUnitHelper.createDecimalFormat;
import static java.lang.Long.parseLong;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Contents;
import com.google.zxing.client.android.encode.QRCodeEncoder;
import com.samourai.wallet.api.APIFactory;
import com.samourai.wallet.explorer.ExplorerActivity;
import com.samourai.wallet.hd.WALLET_INDEX;
import com.samourai.wallet.tor.SamouraiTorManager;
import com.samourai.wallet.util.AddressFactory;
import com.samourai.wallet.util.AppUtil;
import com.samourai.wallet.util.DecimalDigitsInputFilter;
import com.samourai.wallet.util.FormatsUtil;
import com.samourai.wallet.util.PrefsUtil;
import com.samourai.wallet.util.SatoshiBitcoinUnitHelper;

import org.apache.commons.lang3.tuple.Pair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.uri.BitcoinURI;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

public class ReceiveActivity extends SamouraiActivity {
    private static int imgWidth = 0;

    private ImageView ivQR = null;
    private TextView tvAddress = null;
    private TextView tvPath = null;

    private EditText edAmountBTC, edAmountSAT = null;
    private TextWatcher textWatcherBTC = null;
    private LinearLayout advancedButton = null;
    private ConstraintLayout advanceOptionsContainer = null;
    private Spinner addressTypesSpinner = null;

    private boolean useSegwit = true;

    private String addr = null;
    private String addr44 = null;
    private String addr49 = null;
    private String addr84 = null;
    private int idx = 0;
    private int idx44 = 0;
    private int idx49 = 0;
    private int idx84 = 0;

    private boolean inc44 = false;
    private boolean inc49 = false;
    private boolean inc84 = false;

    private boolean canRefresh44 = false;
    private boolean canRefresh49 = false;
    private boolean canRefresh84 = false;
    private Menu _menu = null;

    public static final String ACTION_INTENT = "com.samourai.wallet.ReceiveFragment.REFRESH";

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {

                Bundle extras = intent.getExtras();
                if (extras != null && extras.containsKey("received_on")) {
                    String in_addr = extras.getString("received_on");

                    if (in_addr.equals(addr) || in_addr.equals(addr44) || in_addr.equals(addr49)) {
                        ReceiveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                ReceiveActivity.this.finish();

                            }
                        });

                    }

                }

            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        setTitle("");

        advanceOptionsContainer = findViewById(R.id.container_advance_options);
        tvAddress = findViewById(R.id.receive_address);
        tvPath = findViewById(R.id.path);
        addressTypesSpinner = findViewById(R.id.address_type_spinner);
        ivQR = findViewById(R.id.qr);
        advancedButton = findViewById(R.id.advance_button);
        edAmountBTC = findViewById(R.id.amountBTC);
        edAmountSAT = findViewById(R.id.amountSAT);
        populateSpinner();
        edAmountBTC.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(8,8)});

        edAmountBTC.addTextChangedListener(BTCWatcher);
        edAmountSAT.addTextChangedListener(satWatcher);

        Display display = (ReceiveActivity.this).getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        imgWidth = Math.max(size.x - 460, 150);
        ivQR.setMaxWidth(imgWidth);

        useSegwit = PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.USE_SEGWIT, true);

        advancedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TransitionManager.beginDelayedTransition(advanceOptionsContainer, new AutoTransition());
                int visibility = advanceOptionsContainer.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE;
                advanceOptionsContainer.setVisibility(visibility);
            }
        });

        Pair<Integer, String> pair84 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP84_RECEIVE);
        addr84 = pair84.getRight();
        idx84 = pair84.getLeft();
        Pair<Integer, String> pair49 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP49_RECEIVE);
        addr49 = pair49.getRight();
        idx49 = pair49.getLeft();
        Pair<Integer, String> pair44 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP44_RECEIVE);
        addr44 = pair44.getRight();
        idx44 = pair44.getLeft();

        if (!useSegwit) {
            addressTypesSpinner.setSelection(2);
        }

        if (useSegwit && isBIP84Selected()) {
            addr = addr84;
            idx = idx84;
            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP84_RECEIVE);
            inc84 = true;
        }
        else if (useSegwit && !isBIP84Selected()) {
            addr = addr49;
            idx = idx49;
            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP49_RECEIVE);
            inc49 = true;
        }
        else {
            addr = addr44;
            idx = idx44;
            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP44_RECEIVE);
            inc44 = true;
        }
        addressTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1: {
                        addr = addr49;
                        idx = idx49;
                        if(!inc49)    {
                            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP49_RECEIVE);
                            inc49 = true;
                        }
                        break;
                    }
                    case 2: {
                        addr = addr44;
                        idx = idx44;
                        if(!inc44)    {
                            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP44_RECEIVE);
                            inc44 = true;
                        }
                        break;
                    }
                    default: {
                        addr = addr84;
                        idx = idx84;
                        if(!inc84)    {
                            AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP84_RECEIVE);
                            inc84 = true;
                        }
                    }
                }
                displayQRCode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ;
            }
        });

        tvAddress.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_clipboard)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {
                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);
                                Toast.makeText(ReceiveActivity.this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).show();

                return false;
            }
        });

        ivQR.setOnTouchListener(new OnSwipeTouchListener(ReceiveActivity.this) {
            @Override
            public void onSwipeLeft() {
                if (useSegwit && isBIP84Selected() && canRefresh84) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP84_RECEIVE);
                    Pair<Integer, String> pair84 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP84_RECEIVE);
                    addr84 = pair84.getRight();
                    addr = addr84;
                    idx84 = pair84.getLeft();
                    idx = idx84;
                    canRefresh84 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                }
                else if (useSegwit && !isBIP84Selected() && canRefresh49) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP49_RECEIVE);
                    Pair<Integer, String> pair49 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP49_RECEIVE);
                    addr49 = pair49.getRight();
                    addr = addr49;
                    idx49 = pair49.getLeft();
                    idx = idx49;
                    canRefresh49 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                }
                else if (!useSegwit && canRefresh44) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP44_RECEIVE);
                    Pair<Integer, String> pair44 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP44_RECEIVE);
                    addr44 = pair44.getRight();
                    addr = addr44;
                    idx44 = pair44.getLeft();
                    idx = idx44;
                    canRefresh44 = false;
                    _menu.findItem(R.id.action_refresh).setVisible(false);
                    displayQRCode();
                }

            }
        });

        displayQRCode();
    }


    private String formattedSatValue(final Number number) {
        return createDecimalFormat("#,###", true).format(number);
    }

    private TextWatcher BTCWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            edAmountSAT.removeTextChangedListener(satWatcher);
            edAmountBTC.removeTextChangedListener(this);

            try {
                if (editable.toString().length() == 0) {
                    edAmountSAT.setText("0");
                    edAmountBTC.setText("");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    edAmountSAT.addTextChangedListener(satWatcher);
                    edAmountBTC.addTextChangedListener(this);
                    return;
                }

                final Double btc = Double.parseDouble(String.valueOf(editable));
                if (btc > SatoshiBitcoinUnitHelper.MAX_POSSIBLE_BTC) {
                    edAmountBTC.setText("0.00");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    edAmountSAT.setText("0");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
                else {
                    edAmountSAT.setText(formattedSatValue(SatoshiBitcoinUnitHelper.getSatValue(btc)));
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            edAmountSAT.addTextChangedListener(satWatcher);
            edAmountBTC.addTextChangedListener(this);

            displayQRCode();
        }
    };

    private TextWatcher satWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
            edAmountSAT.removeTextChangedListener(this);
            edAmountBTC.removeTextChangedListener(BTCWatcher);

            try {
                if (editable.toString().length() == 0) {
                    edAmountBTC.setText("0.00");
                    edAmountSAT.setText("");
                    edAmountSAT.addTextChangedListener(this);
                    edAmountBTC.addTextChangedListener(BTCWatcher);
                    return;
                }
                String cleared_space = editable.toString().replace(" ", "");

                final Long sats = parseLong(cleared_space);
                final String formatted = formattedSatValue(sats);
                edAmountSAT.setText(formatted);
                edAmountSAT.setSelection(formatted.length());

                final Double btc = SatoshiBitcoinUnitHelper.getBtcValue(sats);
                edAmountBTC.setText(String.format(Locale.US, "%.8f", btc));
                if (btc > SatoshiBitcoinUnitHelper.MAX_POSSIBLE_BTC) {
                    edAmountBTC.setText("0.00");
                    edAmountBTC.setSelection(edAmountBTC.getText().length());
                    edAmountSAT.setText("0");
                    edAmountSAT.setSelection(edAmountSAT.getText().length());
                    Toast.makeText(ReceiveActivity.this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();

            }
            edAmountSAT.addTextChangedListener(this);
            edAmountBTC.addTextChangedListener(BTCWatcher);
            displayQRCode();
        }
    };


    private void populateSpinner() {

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.address_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        addressTypesSpinner.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(ReceiveActivity.this).registerReceiver(receiver, filter);

        AppUtil.getInstance(ReceiveActivity.this).checkTimeOut();

    }

    private boolean isBIP84Selected() {
        return addressTypesSpinner.getSelectedItemPosition() == 0;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(ReceiveActivity.this).unregisterReceiver(receiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.receive_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_share_receive: {
                new AlertDialog.Builder(ReceiveActivity.this)
                        .setTitle(R.string.app_name)
                        .setMessage(R.string.receive_address_to_share)
                        .setCancelable(false)
                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                            public void onClick(DialogInterface dialog, int whichButton) {

                                String strFileName = AppUtil.getInstance(ReceiveActivity.this).getReceiveQRFilename();
                                File file = new File(strFileName);
                                if (!file.exists()) {
                                    try {
                                        file.createNewFile();
                                    } catch (Exception e) {
                                        Toast.makeText(ReceiveActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                file.setReadable(true, false);

                                FileOutputStream fos = null;
                                try {
                                    fos = new FileOutputStream(file);
                                } catch (FileNotFoundException fnfe) {
                                    ;
                                }

                                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) ReceiveActivity.this.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip = null;
                                clip = android.content.ClipData.newPlainText("Receive address", addr);
                                clipboard.setPrimaryClip(clip);

                                if (file != null && fos != null) {
                                    Bitmap bitmap = ((BitmapDrawable) ivQR.getDrawable()).getBitmap();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 0, fos);

                                    try {
                                        fos.close();
                                    } catch (IOException ioe) {
                                        ;
                                    }

                                    Intent intent = new Intent();
                                    intent.setAction(Intent.ACTION_SEND);
                                    intent.setType("image/png");
                                    if (android.os.Build.VERSION.SDK_INT >= 24) {
                                        //From API 24 sending FIle on intent ,require custom file provider
                                        intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                                                ReceiveActivity.this,
                                                getApplicationContext()
                                                        .getPackageName() + ".provider", file));
                                    } else {
                                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
                                    }
                                    startActivity(Intent.createChooser(intent, ReceiveActivity.this.getText(R.string.send_payment_code)));
                                }

                            }

                        }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        ;
                    }
                }).show();
                break;
            }
            case R.id.action_refresh: {
                if (useSegwit && isBIP84Selected() && canRefresh84) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP84_RECEIVE);
                    Pair<Integer, String> pair84 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP84_RECEIVE);
                    addr84 = pair84.getRight();
                    addr = addr84;
                    idx84 = pair84.getLeft();
                    idx = idx84;
                    canRefresh84 = false;
                    item.setVisible(false);
                    displayQRCode();
                }
                else if (useSegwit && !isBIP84Selected() && canRefresh49) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP49_RECEIVE);
                    Pair<Integer, String> pair49 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP49_RECEIVE);
                    addr49 = pair49.getRight();
                    addr = addr49;
                    idx49 = pair49.getLeft();
                    idx = idx49;
                    canRefresh49 = false;
                    item.setVisible(false);
                    displayQRCode();
                }
                else if (!useSegwit && canRefresh44) {
                    AddressFactory.getInstance(ReceiveActivity.this).increment(WALLET_INDEX.BIP44_RECEIVE);
                    Pair<Integer, String> pair44 = AddressFactory.getInstance(ReceiveActivity.this).getAddress(WALLET_INDEX.BIP44_RECEIVE);
                    addr44 = pair44.getRight();
                    addr = addr44;
                    idx44 = pair44.getLeft();
                    idx = idx44;
                    canRefresh44 = false;
                    item.setVisible(false);
                    displayQRCode();
                } else {
                    ;
                }
                break;
            }
            case R.id.action_support: {
                doSupport();
                break;
            }

//           Handle Toolbar back button press
            case android.R.id.home: {
                finish();
                break;
            }
        }


        return super.onOptionsItemSelected(item);
    }

    private void doSupport() {
        String url = "https://samouraiwallet.com/support";
        if (SamouraiTorManager.INSTANCE.isConnected())
            url = "http://72typmu5edrjmcdkzuzmv2i4zqru7rjlrcxwtod4nu6qtfsqegngzead.onion/support";
        Intent intent = new Intent(this, ExplorerActivity.class);
        intent.putExtra(ExplorerActivity.SUPPORT, url);
        startActivity(intent);
    }

    private void displayQRCode() {

        String _addr = null;
        if (useSegwit && isBIP84Selected()) {
            _addr = addr.toUpperCase();
        } else {
            _addr = addr;
        }

        try {
            final Number btcAmount = NumberFormat.getInstance(Locale.US)
                    .parse(edAmountBTC.getText().toString().trim());

            long satAmount = SatoshiBitcoinUnitHelper.getSatValue(btcAmount);
            if (satAmount != 0l) {
                if (!FormatsUtil.getInstance().isValidBech32(_addr)) {
                    ivQR.setImageBitmap(
                            generateQRCode(BitcoinURI.convertToBitcoinURI(
                                    Address.fromBase58(SamouraiWallet.getInstance().getCurrentNetworkParams(), _addr),
                                    Coin.valueOf(satAmount),
                                    null,
                                    null)));
                } else {
                    ivQR.setImageBitmap(generateQRCode("bitcoin:" + _addr +
                            "?amount=" + Coin.valueOf(satAmount).toPlainString()));
                }
            } else {
                ivQR.setImageBitmap(generateQRCode(_addr));
            }
        } catch (NumberFormatException nfe) {
            ivQR.setImageBitmap(generateQRCode(_addr));
        } catch (ParseException pe) {
            ivQR.setImageBitmap(generateQRCode(_addr));
        }

        tvAddress.setText(addr);
        displayPath();
        if(!AppUtil.getInstance(ReceiveActivity.this).isOfflineMode())    {
            checkPrevUse();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                Intent _intent = new Intent("com.samourai.wallet.MainActivity2.RESTART_SERVICE");
                LocalBroadcastManager.getInstance(ReceiveActivity.this).sendBroadcast(_intent);
            }
        }).start();

    }

    private Bitmap generateQRCode(String uri) {

        Bitmap bitmap = null;

        QRCodeEncoder qrCodeEncoder = new QRCodeEncoder(uri, null, Contents.Type.TEXT, BarcodeFormat.QR_CODE.toString(), imgWidth);

        try {
            bitmap = qrCodeEncoder.encodeAsBitmap();
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private void checkPrevUse() {

        final Handler handler = new Handler();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final JSONObject jsonObject = APIFactory.getInstance(ReceiveActivity.this).getAddressInfo(addr);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (jsonObject != null && jsonObject.has("addresses") && jsonObject.getJSONArray("addresses").length() > 0) {
                                    JSONArray addrs = jsonObject.getJSONArray("addresses");
                                    JSONObject _addr = addrs.getJSONObject(0);
                                    if (_addr.has("n_tx") && _addr.getLong("n_tx") > 0L) {
                                        Toast.makeText(ReceiveActivity.this, R.string.address_used_previously, Toast.LENGTH_SHORT).show();
                                        if (useSegwit && isBIP84Selected()) {
                                            canRefresh84 = true;
                                        } else if (useSegwit && !isBIP84Selected()) {
                                            canRefresh49 = true;
                                        } else {
                                            canRefresh44 = true;
                                        }
                                        if (_menu != null) {
                                            _menu.findItem(R.id.action_refresh).setVisible(true);
                                        }
                                    } else {
                                        if (useSegwit && isBIP84Selected()) {
                                            canRefresh84 = false;
                                        } else if (useSegwit && !isBIP84Selected()) {
                                            canRefresh49 = false;
                                        } else {
                                            canRefresh44 = false;
                                        }
                                        if (_menu != null) {
                                            _menu.findItem(R.id.action_refresh).setVisible(false);
                                        }
                                    }
                                }

                            } catch (Exception e) {
                                if (useSegwit && isBIP84Selected()) {
                                    canRefresh84 = false;
                                } else if (useSegwit && !isBIP84Selected()) {
                                    canRefresh49 = false;
                                } else {
                                    canRefresh44 = false;
                                }
                                if (_menu != null) {
                                    _menu.findItem(R.id.action_refresh).setVisible(false);
                                }
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    if (useSegwit && isBIP84Selected()) {
                        canRefresh84 = false;
                    } else if (useSegwit && !isBIP84Selected()) {
                        canRefresh49 = false;
                    } else {
                        canRefresh44 = false;
                    }
                    if (_menu != null) {
                        _menu.findItem(R.id.action_refresh).setVisible(false);
                    }
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void displayPath() {

        int sel = addressTypesSpinner.getSelectedItemPosition();
        String path = "m/";

        switch (sel) {
            case 1:
                path += "49'";
                break;
            case 2:
                path += "44'";
                break;
            default:
                path += "84'";
        }

        if (PrefsUtil.getInstance(ReceiveActivity.this).getValue(PrefsUtil.TESTNET, false)) {
            path += "/1'/0'/0/";
        } else {
            path += "/0'/0'/0/";
        }
        path += idx;
        tvPath.setText(path);

    }

}