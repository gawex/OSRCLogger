package cz.tatarin.android.osrclogger;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class MinuteValueFormatter extends ValueFormatter {

    private final DecimalFormat DecimalFormat;

    public MinuteValueFormatter() {
        DecimalFormat = new DecimalFormat("##");
    }

    @Override
    public String getFormattedValue(float value) {
        return DecimalFormat.format(value) + "m";
    }
}
