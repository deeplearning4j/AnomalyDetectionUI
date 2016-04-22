package canonical.ui.impl;

import org.deeplearning4j.ui.api.LengthUnit;
import org.deeplearning4j.ui.api.Style;
import org.deeplearning4j.ui.components.chart.style.StyleChart;
import org.deeplearning4j.ui.components.component.style.StyleDiv;
import org.deeplearning4j.ui.components.table.style.StyleTable;
import org.deeplearning4j.ui.components.text.style.StyleText;

import java.awt.*;

/**
 * Created by Alex on 20/04/2016.
 */
public class LogFileUIStyles {

    public static final String topHeaderColor = "#772953";
    public static final String headerColor = "#2C001E";
    public static final String tableHeaderColor = "#EBEBEB";


    //For log table:
    public static final String[] tableHeader = new String[]{"Log File","Time","Level","Location","Message"};
    public static final StyleTable styleTable = new StyleTable.Builder()
            .backgroundColor(Color.WHITE)
            .headerColor(tableHeaderColor)
            .width(100, LengthUnit.Percent)
            .columnWidths(LengthUnit.Percent, 13, 13, 6, 18, 50)
            .borderWidth(1)
            .margin(LengthUnit.Px,1,1,1,1)
            .build();

    //Style for charts:
    public static final StyleChart styleChartTotalLogEntries = new StyleChart.Builder()
            .width(466,LengthUnit.Px)
            .height(240,LengthUnit.Px)
            .strokeWidth(2.0)
            .seriesColors(topHeaderColor)
            .margin(LengthUnit.Px, 20, 20, 45, 20)
            .build();

    public static final StyleChart styleChartLogEntriesByLevel = new StyleChart.Builder()
            .width(466,LengthUnit.Px)
            .height(240,LengthUnit.Px)
            .strokeWidth(2.0)
            //Levels: "AUDIT","TRACE","INFO","WARNING","ERROR","CRITICAL"
            .seriesColors("#59E0FF", "#0059FF", "#47FF5F", "#FFA500", "#FF001D", "#000000")
            .margin(LengthUnit.Px, 20, 20, 45, 20)
            .build();

    public static final StyleChart styleChartLogEntriesBySource = new StyleChart.Builder()
            .width(466,LengthUnit.Px)
            .height(240,LengthUnit.Px)
            .strokeWidth(2.0)
            .margin(LengthUnit.Px, 20, 20, 45, 20)

//            .seriesColors("#59E0FF", "#0059FF", "#47FF5F", "#FFA500", "#FF001D", "#000000")
            .build();

    //Div that the charts go in:
    public static final Style graphDivStyle = new StyleDiv.Builder()
            .width(33.333, LengthUnit.Percent)
            .height(245, LengthUnit.Px)
            .floatValue(StyleDiv.FloatValue.left)
            .build();


    //Style for headers:
    public static final Style topDivStyle = new StyleDiv.Builder()
            .backgroundColor(headerColor)
            .width(33.333, LengthUnit.Percent)
            .floatValue(StyleDiv.FloatValue.left)
            .build();

    public static final Style tableHeaderDivStyle = new StyleDiv.Builder()
            .backgroundColor(headerColor)
            .width(100, LengthUnit.Percent)
            .floatValue(StyleDiv.FloatValue.left)
            .build();

    public static final Style headerDivStyle = new StyleDiv.Builder()
            .width(100,LengthUnit.Percent)
            .build();

    public static final StyleText headerTextStyle = new StyleText.Builder()
            .color(Color.WHITE)
            .fontSize(12)
            .margin(LengthUnit.Px,null,null,15,null)
            .build();


    
}
