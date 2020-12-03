package cz.tatarin.android.osrclogger;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.TimeoutError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChartActivity extends AppCompatActivity {

    private static final int DATA_DOWNLOAD_INTERVAL_MS = 5000;

    private RequestQueue mRequestQueue;
    private StringRequest mDownloadLastHourDataRequest;

    private SimpleDateFormat mSystemTimeFormat;
    private ExecutorService mUpdateCurrentSystemTimeExecutor;
    private final Runnable mUpdateCurrentSystemTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                Thread.sleep(100);
                invalidateOptionsMenu();
                mUpdateCurrentSystemTimeExecutor.submit(mUpdateCurrentSystemTimeRunnable);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };

    private TextView mTxvChartTimeLabel;
    private TextView mTxvChartDateLabel;
    private LineChart mLineChart;

    private Handler mDownloadLastHourDataHandler;
    private final Runnable mDownloadLastHourDataRunnable = new Runnable() {
        @Override
        public void run() {
            mRequestQueue.add(mDownloadLastHourDataRequest);
            mDownloadLastHourDataHandler.postDelayed(mDownloadLastHourDataRunnable,
                    DATA_DOWNLOAD_INTERVAL_MS);
        }
    };

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
        String serverAddressOrDomain = getIntent().getStringExtra("server_host");
        setTitle(serverAddressOrDomain);
        setContentView(R.layout.activity_chart);

        mSystemTimeFormat = new SimpleDateFormat("HH:mm:ss");
        mUpdateCurrentSystemTimeExecutor = Executors.newFixedThreadPool(1);
        mUpdateCurrentSystemTimeExecutor.submit(mUpdateCurrentSystemTimeRunnable);

        mTxvChartTimeLabel = findViewById(R.id.activity_chart_txv_chart_time_label);
        mTxvChartDateLabel = findViewById(R.id.activity_chart_txv_chart_date_label);

        mLineChart = findViewById(R.id.activity_chart_cht_mean_temperatures);
        mLineChart.setMaxVisibleValueCount(0);
        mLineChart.getDescription().setEnabled(false);
        mLineChart.setPinchZoom(true);
        mLineChart.setScaleYEnabled(false);
        mLineChart.setMarker(new MyMarkerView(getApplicationContext(), R.layout.marker_view));
        mLineChart.setNoDataText(getString(R.string.chart_no_data_available));
        mLineChart.setNoDataTextColor(getColor(R.color.raspberry));
        mLineChart.setNoDataTextTypeface(Typeface.create("font_family_condensed",
                Typeface.BOLD));

        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1);
        xAxis.setLabelCount(7);
        xAxis.setAxisMinimum(1);
        xAxis.setAxisMaximum(59);
        xAxis.setValueFormatter(new MinuteValueFormatter());

        YAxis yAxisRight = mLineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxisLeft = mLineChart.getAxisLeft();
        yAxisLeft.setValueFormatter(new TemperatureValueFormatter());

        Legend legend = mLineChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
        legend.setForm(Legend.LegendForm.SQUARE);
        legend.setFormSize(11);
        legend.setTextSize(11);
        legend.setXEntrySpace(4);

        DiskBasedCache cache = new DiskBasedCache(getCacheDir(), 0);
        BasicNetwork network = new BasicNetwork(new HurlStack());

        mRequestQueue = new RequestQueue(cache, network);
        mRequestQueue.start();

        mDownloadLastHourDataHandler = new Handler();
        mDownloadLastHourDataRequest = new StringRequest(Request.Method.GET,
                "http://" + serverAddressOrDomain + "/appserver/handledatabase/getLastHourData/",
                response -> {
                    updateChartData(response);
                    updateChartLabel();
                },
                error -> {
                    if(error instanceof NoConnectionError){
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_no_connection), Toast.LENGTH_SHORT).show();
                    } else if(error instanceof TimeoutError) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_timeout), Toast.LENGTH_SHORT).show();
                    } else if (error instanceof com.android.volley.ClientError){
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_client), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.err_other), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @SuppressWarnings("SpellCheckingInspection")
    @SuppressLint("SetTextI18n")
    private void updateChartLabel(){
        Calendar from = Calendar.getInstance();
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.SECOND, 0);

        Calendar to = Calendar.getInstance();
        to.set(Calendar.MINUTE, 59);
        to.set(Calendar.SECOND, 59);

        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat fromTimeFormat = new SimpleDateFormat("HH:mm:ss");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat toTimeFormat = new SimpleDateFormat("HH:mm:ss");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("d. MMMM yyyy");
        mTxvChartTimeLabel.setText(getString(R.string.txv_chart_label_from) + " " +
                fromTimeFormat.format(from.getTime()) + " " +
                getString(R.string.txv_chart_label_to) + " " +
                toTimeFormat.format(to.getTime()));
        mTxvChartDateLabel.setText(dateFormat.format(from.getTime()));
    }

    private void updateChartData(String data){
        Calendar calendar = Calendar.getInstance();
        int records = 0;
        ArrayList<Entry> temperatureEntries = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(data);
            double temperatureSum = 0;
            calendar.setTime(new Date(jsonArray.getJSONObject(0)
                    .getLong("da_timestamp") * 1000));
            int lastMinute = calendar.get(Calendar.MINUTE);
            for(int i = 0; i < jsonArray.length(); i++){
                calendar.setTime(new Date(jsonArray.getJSONObject(i)
                        .getLong("da_timestamp") * 1000));
                if(lastMinute != calendar.get(Calendar.MINUTE)){
                    temperatureEntries.add(new Entry(lastMinute, (float) temperatureSum/records));
                    temperatureSum = 0;
                    records = 0;
                }
                temperatureSum += jsonArray.getJSONObject(i).getDouble("da_temperature");
                lastMinute = calendar.get(Calendar.MINUTE);
                records++;
            }
            temperatureEntries.add(new Entry(lastMinute, (float) temperatureSum/records));
            LineDataSet lineDataSet = new LineDataSet(temperatureEntries,
                    getString(R.string.legend_line_data_label));
            lineDataSet.setColor(getColor(R.color.raspberry));
            lineDataSet.setLineWidth(2f);
            lineDataSet.setCircleColor(R.color.raspberry);
            lineDataSet.setCircleRadius(2f);
            lineDataSet.setFillColor(R.color.raspberry);
            lineDataSet.setMode(LineDataSet.Mode.HORIZONTAL_BEZIER);
            lineDataSet.setDrawValues(true);
            lineDataSet.setValueTextSize(10f);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);

            LineData lineData = new LineData(lineDataSet);

            mLineChart.setData(lineData);
            mLineChart.invalidate();
        } catch (JSONException e) {
            mLineChart.setData(null);
            mLineChart.invalidate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mDownloadLastHourDataHandler.post(mDownloadLastHourDataRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDownloadLastHourDataHandler.removeCallbacks(mDownloadLastHourDataRunnable);
    }
}