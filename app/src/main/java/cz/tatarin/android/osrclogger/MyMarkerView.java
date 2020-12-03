package cz.tatarin.android.osrclogger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.TextView;

import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.DecimalFormat;


@SuppressLint("ViewConstructor")
public class MyMarkerView extends MarkerView {

    private final TextView mTxvContent;
    private final DecimalFormat mTemperatureFormat = new DecimalFormat("###0.00");
    private final DecimalFormat mMinuteFormat = new DecimalFormat("##");

    public MyMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        mTxvContent = findViewById(R.id.marker_view_txv_content);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String sign = "";
        if(e.getY() >= 0){
            sign = "+ ";
        }
        mTxvContent.setText(getResources().getString(R.string.marker_view_temperature) + " " +
                sign + mTemperatureFormat.format(e.getY()) + " °C\n" +
                getResources().getString(R.string.marker_view_minute) + " " +
                mMinuteFormat.format(e.getX()) + ".");
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-((float) getWidth() / 2), -getHeight());
    }
}