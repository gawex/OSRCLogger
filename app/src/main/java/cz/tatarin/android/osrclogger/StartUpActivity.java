package cz.tatarin.android.osrclogger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StartUpActivity extends AppCompatActivity {

    private static final int URI_LENGTH = 256;
    private static final int CONNECTION_PROGRESS_SPEED_MS = 100;

    private EditText mEtxServerAddressOrDomain;

    private SimpleDateFormat mSystemTimeFormat;
    private ExecutorService mUpdateCurrentSystemTimeExecutor;
    private final Runnable mUpdateCurrentSystemTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
                invalidateOptionsMenu();
                mUpdateCurrentSystemTimeExecutor.submit(mUpdateCurrentSystemTimeRunnable);
            } catch (InterruptedException ignored){

            }
        }
    };

    private AlertDialog mConnectingDialog;
    private int mProgress = 0;
    private String mProgressString;
    private Handler mConnectingAnimationHandler;
    private final Runnable connectingAnimationRunnable = new Runnable() {
        @SuppressWarnings("StringConcatenationInLoop")
        @Override
        public void run() {
            for(int i = 0; i <= mProgress; i++){
                mProgressString += ".";
            }
            mConnectingDialog.setMessage(getString(R.string.connecting_dialog_progress_message) +
                    mEtxServerAddressOrDomain.getText() + mProgressString);
            mProgressString = "";
            mProgress++;
            if(mProgress > 2){
                mProgress = 0;
            }
            mConnectingAnimationHandler.postDelayed(connectingAnimationRunnable,
                    CONNECTION_PROGRESS_SPEED_MS );
        }
    };

    @SuppressWarnings("SpellCheckingInspection")
    private static String getURI() {
        String AlphaNumericString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(URI_LENGTH );
        for (int i = 0; i < URI_LENGTH ; i++) {
            int index = (int)(AlphaNumericString.length() * Math.random());
            sb.append(AlphaNumericString.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.pop_up_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.system_time).setTitle(getString(R.string.pop_up_menu_title_system_time) + " " +
                mSystemTimeFormat.format(System.currentTimeMillis()));
        return super.onPrepareOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_application:
                @SuppressLint("UseCompatLoadingForDrawables") final AlertDialog alertDialog =
                        new AlertDialog.Builder(this)
                        .setIcon(getDrawable(R.mipmap.ic_launcher))
                        .setTitle(getString(R.string.dialog_title_about_application))
                        .setMessage(getString(R.string.dialog_message_about_application))
                        .setNeutralButton(getString(R.string.dialog_btn_close), null)
                        .create();
                alertDialog.show();
                return true;
            case R.id.close_application:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    @SuppressLint("SimpleDateFormat")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        mConnectingAnimationHandler = new Handler();

        mSystemTimeFormat = new SimpleDateFormat("HH:mm:ss");
        mUpdateCurrentSystemTimeExecutor = Executors.newFixedThreadPool(1);
        mUpdateCurrentSystemTimeExecutor.submit(mUpdateCurrentSystemTimeRunnable);

        DiskBasedCache cache = new DiskBasedCache(getCacheDir(), 0);
        BasicNetwork network = new BasicNetwork(new HurlStack());

        RequestQueue requestQueue = new RequestQueue(cache, network);
        requestQueue.start();
        String URI = getURI();

        AlertDialog connectionFailedDialog = new AlertDialog.Builder(StartUpActivity.this)
                .setIcon(android.R.drawable.ic_delete)
                .setTitle(R.string.connection_failed_dialog_title)
                .setMessage(getString(R.string.connection_failed_dialog_message_header))
                .setNeutralButton(R.string.dialog_btn_close, null)
                .create();

        mEtxServerAddressOrDomain = findViewById(R.id.activity_start_up_etx_server_ip_address_or_domain);
        Button btnConnectToServer = findViewById(R.id.activity_start_up_btn_connect_connect_to_server);
        btnConnectToServer.setOnClickListener(v -> {
            StringRequest tryToConnectToServerRequest = new StringRequest(Request.Method.GET,
                    "http://" + mEtxServerAddressOrDomain.getText() + "/appserver/testconnection/tryToConnect/"
                            + URI,
                    response -> {
                        if (response.equals(URI)) {
                            mConnectingAnimationHandler.removeCallbacks(connectingAnimationRunnable);
                            mConnectingDialog.setMessage(getString(R.string.connecting_dialog_success_message) +
                                    mEtxServerAddressOrDomain.getText());
                            new Handler().postDelayed(() -> {
                                mConnectingDialog.hide();
                                startActivity(new Intent(StartUpActivity.this,
                                        ChartActivity.class).putExtra("server_host",
                                        mEtxServerAddressOrDomain.getText().toString()));
                                finish();
                            }, 500);
                        } else {
                            connectionFailedDialog.setMessage(
                                    getString(R.string.connection_failed_dialog_message_header) +
                                            getString(R.string.connection_failed_dialog_message_uri_error));
                            connectionFailedDialog.show();
                            mConnectingDialog.hide();
                        }
                    },
                    error -> {
                        if (error instanceof NoConnectionError) {
                            connectionFailedDialog.setMessage(
                                    getString(R.string.err_no_connection));
                        } else if (error instanceof TimeoutError) {
                            connectionFailedDialog.setMessage(
                                    getString(R.string.err_timeout));
                        } else if (error instanceof com.android.volley.ClientError) {
                            connectionFailedDialog.setMessage(
                                    getString(R.string.err_client));
                        } else {
                            connectionFailedDialog.setMessage(
                                    getString(R.string.err_other));
                        }
                        connectionFailedDialog.show();
                        mConnectingDialog.hide();
                    });

            requestQueue.add(tryToConnectToServerRequest);
            mConnectingDialog = new AlertDialog.Builder(StartUpActivity.this)
                    .setIcon(R.mipmap.ic_launcher)
                    .setTitle(R.string.connecting_dialog_title)
                    .setMessage(getString(R.string.connecting_dialog_progress_message) + mEtxServerAddressOrDomain.getText())
                    .setNeutralButton(R.string.dialog_btn_close, (dialog, which) -> {
                        requestQueue.cancelAll(request -> true);
                        mConnectingDialog.hide();
                    })
                    .create();
            mConnectingDialog.show();
            mConnectingAnimationHandler.post(connectingAnimationRunnable);
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mConnectingDialog != null) {
            mConnectingDialog.dismiss();
        }
    }
}