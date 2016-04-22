package canonical.ui.impl;

import canonical.LogUriProvider;
import canonical.Service;
import canonical.streaming.StreamedConfig;
import canonical.streaming.StreamedValue;
import canonical.ui.StreamingUI;
import org.canova.api.writable.Writable;
import org.deeplearning4j.preprocessing.api.schema.SequenceSchema;
import org.deeplearning4j.ui.api.Component;
import org.deeplearning4j.ui.components.chart.ChartLine;
import org.deeplearning4j.ui.components.chart.ChartStackedArea;
import org.deeplearning4j.ui.components.component.ComponentDiv;
import org.deeplearning4j.ui.components.table.ComponentTable;
import org.deeplearning4j.ui.components.table.style.StyleTable;
import org.deeplearning4j.ui.components.text.ComponentText;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by Alex on 19/04/2016.
 */
public class LogFileUI extends StreamingUI {

    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("YYYY/MM/dd HH:mm:ss.SSS").withZone(DateTimeZone.UTC);

    //History for table: (newest first, oldest last)
    public static final int LOG_HISTORY_SIZE = 30;
    private Map<Service,Deque<List<Writable>>> mostRecentLogEntries = new ConcurrentHashMap<>();

    //History for charts:
    public static final int CHART_HISTORY_SIZE = 60;
        //# entries vs. time chart:
//    private LinkedList<Integer> countNumEntries = new LinkedList<>();
    private Map<Service,LinkedList<Integer>> countNumEntriesMap = new ConcurrentHashMap<>();
        //# entries by service:
//    private Map<String,Deque<Integer>> historyBySource = new ConcurrentHashMap<>();
//    private Map<String,Deque<Integer>> historyByLogLevel = new ConcurrentHashMap<>();
    private Map<Service,Map<String,Deque<Integer>>> historyBySource = new ConcurrentHashMap<>();
    private Map<Service,Map<String,Deque<Integer>>> historyByLogLevel = new ConcurrentHashMap<>();

    private static final Service[] services = new Service[]{Service.Neutron, Service.Nova};

//    private static SequenceSchema schemaPostReduce = LogUISchemas.getSchemaPostReduce(service);
//    private static final List<String> logFileNames = LogUriProvider.getLogFileNamesForService(service);
    private final Map<Service,SequenceSchema> schemasPostReduce = new HashMap<>();
    private final Map<Service,List<String>> logFileNames = new HashMap<>();
    private static final List<String> logLevels = Arrays.asList("AUDIT","TRACE","INFO","WARNING","ERROR","CRITICAL");


    private ComponentTable deployTable;

    public LogFileUI(){
        for(Service s : services){
            mostRecentLogEntries.put(s, new ConcurrentLinkedDeque<List<Writable>>());
            schemasPostReduce.put(s, LogUISchemas.getSchemaPostReduce(s));
            logFileNames.put(s, LogUriProvider.getLogFileNamesForService(s));
            historyBySource.put(s, new ConcurrentHashMap<String, Deque<Integer>>());
            historyByLogLevel.put(s, new ConcurrentHashMap<String, Deque<Integer>>());
            countNumEntriesMap.put(s, new LinkedList<Integer>());

            for(String fileName : logFileNames.get(s)){
                historyBySource.get(s).put(fileName, new ConcurrentLinkedDeque<Integer>());
            }
            for(String logLevel : logLevels){
                historyByLogLevel.get(s).put(logLevel, new ConcurrentLinkedDeque<Integer>());
            }
        }


    }


    @Override
    public void processRecords(List<Object> newRecords) {
        if(newRecords == null || newRecords.size() == 0) return;

        for(Object o : newRecords){
            if(o instanceof StreamedConfig){
                createConfigTables((StreamedConfig)o);
                continue;
            }

            if(!(o instanceof StreamedValue)) throw new RuntimeException("Invalid type: expected StreamedValue objects only. (got: " + o.getClass() + ")");
            StreamedValue sv = (StreamedValue)o;

            List<List<Writable>> mostRecentLogs = sv.getOriginalWindow();   //Input
            int n = mostRecentLogs.size();
//            for( int i=n-1; i>=0; i-- ){
//                //Add oldest to start first, so most recent ends up being first
//                mostRecentLogEntries.addFirst(mostRecentLogs.get(i));
//            }

            Deque<List<Writable>> mostRecentThisService = mostRecentLogEntries.get(sv.getService());
            for (List<Writable> mostRecentLog : mostRecentLogs) {
                //Add oldest to start first, so most recent ends up being first
                mostRecentThisService.addFirst(mostRecentLog);
            }

            updateNumLogEntriesHistory(sv);
            updateLogsBySourceHistory(sv);
            updateLogsByLevelHistory(sv);
        }
        for(Service s : services) {
            Deque<List<Writable>> mostRecentThisService = mostRecentLogEntries.get(s);
            while (mostRecentThisService.size() >= LOG_HISTORY_SIZE) {
                mostRecentThisService.removeLast();  //Remove oldest
            }
        }

        for(Service s : services) {
            Component logTable = getLogTable(s);


            Component titleTopThird1 = new ComponentDiv(LogFileUIStyles.topDivStyle, new ComponentText("# Log Entries Over Time", LogFileUIStyles.headerTextStyle));
            Component titleTopThird2 = new ComponentDiv(LogFileUIStyles.topDivStyle, new ComponentText("Log Entries By Source", LogFileUIStyles.headerTextStyle));
            Component titleTopThird3 = new ComponentDiv(LogFileUIStyles.topDivStyle, new ComponentText("Log Entries By Level", LogFileUIStyles.headerTextStyle));
            Component titleDiv = new ComponentDiv(LogFileUIStyles.headerDivStyle, titleTopThird1, titleTopThird2, titleTopThird3);

            Component lineChartNumEntries = getLogEntriesChart(s);
            Component graphDiv1 = new ComponentDiv(LogFileUIStyles.graphDivStyle, lineChartNumEntries);

            Component chartByService = getLogsBySourceChart(s);
            Component graphDiv2 = new ComponentDiv(LogFileUIStyles.graphDivStyle, chartByService);

            Component chartByLogLevel = getLogsBylevelChart(s);
            Component graphDiv3 = new ComponentDiv(LogFileUIStyles.graphDivStyle, chartByLogLevel);

            //Second header:
            Component titleDiv2 = new ComponentDiv(LogFileUIStyles.tableHeaderDivStyle, new ComponentText("    Log File Entries", LogFileUIStyles.headerTextStyle));


            updateUI(s.toString(), titleDiv, graphDiv1, graphDiv2, graphDiv3, titleDiv2, logTable);
        }

        updateUI("Overview",deployTable);

//        System.out.println("***** LogFileUI: called updateUI *****");
    }

    private void createConfigTables(StreamedConfig streamedConfig){

        this.deployTable = new ComponentTable.Builder(new StyleTable.Builder().build())
                .header("First","Second")
                .content(streamedConfig.getDeploy())
                .build();


    }

    private Component getLogTable(Service service){

        List<List<Writable>> temp = new ArrayList<>(mostRecentLogEntries.get(service));

        int n = Math.min(LOG_HISTORY_SIZE, temp.size());

        String[][] table = new String[temp.size()][5];
        for(int i=0; i<n; i++ ){
            List<Writable> logEntry = temp.get(i);
            for( int j=0; j<5; j++ ){
                if(j == 1) table[i][j] = formatter.print(logEntry.get(j).toLong());
                else table[i][j] = logEntry.get(j).toString();
            }
        }

        ComponentTable ct = new ComponentTable(LogFileUIStyles.tableHeader,table, LogFileUIStyles.styleTable);
        return ct;
    }

    private void updateNumLogEntriesHistory(StreamedValue sv) {
        Service service = sv.getService();
        int numEntries = sv.getOriginalWindow().size();
        LinkedList<Integer> countNumEntries = countNumEntriesMap.get(service);
        countNumEntries.add(numEntries);
        while(countNumEntries.size() > CHART_HISTORY_SIZE){
            countNumEntries.removeFirst();
        }
    }

    private Component getLogEntriesChart(Service service){
        LinkedList<Integer> countNumEntries = countNumEntriesMap.get(service);

        double[] x = new double[countNumEntries.size()];
        double[] y = new double[x.length];
        int count = 0;
        for( Integer i : countNumEntries){
            x[count] = count;
            y[count] = i;
            count++;
        }

        ChartLine chartLine = new ChartLine.Builder(null, LogFileUIStyles.styleChartTotalLogEntries)
                .addSeries("Log entries",x,y)
                .suppressAxisHorizontal(true)
                .showLegend(true)
                .build();

        return chartLine;
    }

    private void updateLogsBySourceHistory(StreamedValue sv) {
        Service service = sv.getService();

        //In windowed data: have columns for each source...
        List<Writable> reduced = sv.getReduced();

        for (String name : logFileNames.get(service)) {
            int idx = schemasPostReduce.get(service).getIndexOfColumn("sum(LogFileName[" + name + "])");
            int count = reduced.get(idx).toInt();

            Deque<Integer> list = this.historyBySource.get(service).get(name);
            if (list == null) {
                list = new ConcurrentLinkedDeque<>();   //Collections.synchronizedList(new ArrayList<Integer>());
                historyBySource.get(service).put(name, list);
            }

            list.add(count);

            while(list.size() > CHART_HISTORY_SIZE){
                list.removeFirst();
            }
        }
    }

    private Component getLogsBySourceChart(Service service){
        int numElements = historyBySource.get(service).get(logFileNames.get(service).get(0)).size();

        List<String> logFileNamesForService = logFileNames.get(service);

        double[] x = new double[numElements];
        double[][] y = new double[logFileNamesForService.size()][numElements];

        for( int i=0; i<x.length; i++ ) x[i] = i;
        for( int i=0; i<logFileNamesForService.size(); i++ ){
            Deque<Integer> list = historyBySource.get(service).get(logFileNamesForService.get(i));
            int j=0;
            for(Integer count : list){
                y[i][j++] = count;
            }
        }


//        ChartLine.Builder chartLine = new ChartLine.Builder(null,LogFileUIStyles.styleChartLogEntriesBySource);
//        for(int i=0; i<logFileNames.size(); i++ ){
//            chartLine.addSeries(logFileNames.get(i),x,y[i]);
//        }
//        chartLine.suppressAxisHorizontal(true).showLegend(true);
//        return chartLine.build();

        ChartStackedArea.Builder chart = new ChartStackedArea.Builder(null, LogFileUIStyles.styleChartLogEntriesBySource);
        chart.setXValues(x);

        for(int i=0; i<logFileNamesForService.size(); i++ ){
            String name = logFileNamesForService.get(i);
            name = name.substring(0, name.length()-4);
            chart.addSeries(name,y[i]);
        }
        chart.suppressAxisHorizontal(true).showLegend(true);
        return chart.build();
    }

    private void updateLogsByLevelHistory(StreamedValue sv) {
        Service service = sv.getService();

        //In windowed data: have columns for each service...
        List<Writable> reduced = sv.getReduced();

        for (String name : logLevels) {
            int idx = schemasPostReduce.get(service).getIndexOfColumn("sum(LogLevel[" + name + "])");
            int count = reduced.get(idx).toInt();

            Deque<Integer> list = historyByLogLevel.get(service).get(name);
            if (list == null) {
                list = new ConcurrentLinkedDeque<>();   //Collections.synchronizedList(new ArrayList<Integer>());
                historyByLogLevel.get(service).put(name, list);
            }

            list.add(count);

            while(list.size() > CHART_HISTORY_SIZE){
                list.removeFirst();
            }
        }
    }

    private Component getLogsBylevelChart(Service service){

        int numElements = historyByLogLevel.get(service).get(logLevels.get(0)).size();

        double[] x = new double[numElements];
        double[][] y = new double[logLevels.size()][numElements];

        for( int i=0; i<x.length; i++ ) x[i] = i;
        for( int i=0; i<logLevels.size(); i++ ){
            Deque<Integer> list = historyByLogLevel.get(service).get(logLevels.get(i));
            int j=0;
            for(Integer count : list){
                y[i][j++] = count;
            }
        }

        ChartStackedArea.Builder chart = new ChartStackedArea.Builder(null, LogFileUIStyles.styleChartLogEntriesByLevel);
        chart.setXValues(x);

//        ChartLine.Builder chartLine = new ChartLine.Builder(null,LogFileUIStyles.styleChartLogEntriesByLevel);
        for(int i=0; i<logLevels.size(); i++ ){
//            chartLine.addSeries(logLevels.get(i),x,y[i]);
            chart.addSeries(logLevels.get(i),y[i]);
        }

//        chartLine.suppressAxisHorizontal(true).showLegend(true);
//        return chartLine.build();
        chart.suppressAxisHorizontal(true).showLegend(true);
        return chart.build();
    }
}
