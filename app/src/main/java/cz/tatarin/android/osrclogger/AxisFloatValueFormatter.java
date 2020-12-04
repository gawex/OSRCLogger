package cz.tatarin.android.osrclogger;

import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.DecimalFormat;

public class AxisFloatValueFormatter extends ValueFormatter {

    private final DecimalFormat mDecimalFormat;
    private final String mUnit;
    private final boolean mPositiveSign;

    public AxisFloatValueFormatter(String unit, boolean positiveSign) {
        mDecimalFormat = new DecimalFormat("##0.0");
        mUnit = unit;
        mPositiveSign = positiveSign;
    }

    @Override
    public String getFormattedValue(float value) {
        String sign = "";
        if(value >= 0 && mPositiveSign){
            sign = "+ ";
        }
        return sign + mDecimalFormat.format(value) + " " + mUnit;
    }
}
