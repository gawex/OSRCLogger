package cz.tatarin.android.osrclogger;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class TemperatureValueFormatter extends ValueFormatter {

    private final DecimalFormat DecimalFormat;

    public TemperatureValueFormatter() {
        DecimalFormat = new DecimalFormat("###0.00");
    }

    @Override
    public String getFormattedValue(float value) {
        String sign = "";
        if(value >= 0){
            sign = "+ ";
        }
        return sign + DecimalFormat.format(value) + " °C";
    }
}
