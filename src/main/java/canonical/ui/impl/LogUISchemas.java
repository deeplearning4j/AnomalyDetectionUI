package canonical.ui.impl;

import canonical.LogFormatProvider;
import canonical.LogUriProvider;
import canonical.Service;
import canonical.ui.UIConstants;
import org.deeplearning4j.preprocessing.api.ReduceOp;
import org.deeplearning4j.preprocessing.api.TransformProcess;
import org.deeplearning4j.preprocessing.api.reduce.IReducer;
import org.deeplearning4j.preprocessing.api.reduce.Reducer;
import org.deeplearning4j.preprocessing.api.schema.SequenceSchema;
import org.deeplearning4j.preprocessing.api.sequence.window.ReduceSequenceByWindowTransform;
import org.deeplearning4j.preprocessing.api.sequence.window.TimeWindowFunction;
import org.deeplearning4j.preprocessing.api.sequence.window.WindowFunction;
import org.deeplearning4j.preprocessing.api.transform.categorical.StringToCategoricalTransform;
import org.joda.time.DateTimeZone;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Alex on 20/04/2016.
 */
public class LogUISchemas {

    public static SequenceSchema getSchemaAtImport( Service service ){

        SequenceSchema schema = (SequenceSchema)new SequenceSchema.Builder()
                .addColumnCategorical("LogFileName", LogUriProvider.getLogFileNamesForService(service))
                .addColumnTime("DateTime", DateTimeZone.UTC)
                .addColumnCategorical("LogLevel", Arrays.asList("AUDIT","TRACE","INFO","WARNING","ERROR","CRITICAL"))
                .addColumnsString("Location","LogMessage")
                .build();

        return schema;
    }


    public static SequenceSchema getSchemaPostReduce(Service service ){

        List<String> logFileNames = LogUriProvider.getLogFileNamesForService(service);
        String[] logFileColNamesOneHot = new String[logFileNames.size()];
        for (int i = 0; i < logFileColNamesOneHot.length; i++) {
            logFileColNamesOneHot[i] = "LogFileName[" + logFileNames.get(i) + "]";
        }

        List<String> locations = LogFormatProvider.getLocationsForService(service,"Other");
        String[] locationColNamesOneHot = new String[locations.size()];
        for (int i = 0; i < locationColNamesOneHot.length; i++) {
            locationColNamesOneHot[i] = "Location[" + locations.get(i) + "]";
        }

        IReducer reducer = new Reducer.Builder(ReduceOp.CountUnique)
                .sumColumns("LogLevel[AUDIT]", "LogLevel[TRACE]", "LogLevel[INFO]", "LogLevel[WARNING]", "LogLevel[ERROR]", "LogLevel[CRITICAL]")
                .sumColumns(logFileColNamesOneHot)
                .sumColumns(locationColNamesOneHot)
                .takeFirstColumns("DateTime")
                .build();

        WindowFunction wf = new TimeWindowFunction("DateTime", UIConstants.WINDOW_SIZE, UIConstants.WINDOW_SIZE_UNIT);
        TransformProcess tp2 = new TransformProcess.Builder(getSchemaAtImport(service))
                .transform(new StringToCategoricalTransform("Location", LogFormatProvider.getLocationsForService(service, "Other")))
                .categoricalToOneHot("LogLevel", "LogFileName", "Location")
                .transform(new ReduceSequenceByWindowTransform(reducer, wf))
                .removeColumns("first(DateTime)") //Don't need the time values anymore
                .build();

        return (SequenceSchema)tp2.getFinalSchema();
    }

}
