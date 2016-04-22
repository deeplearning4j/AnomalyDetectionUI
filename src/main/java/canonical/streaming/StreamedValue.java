package canonical.streaming;

import canonical.Service;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.canova.api.writable.Writable;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Alex on 19/04/2016.
 */
@AllArgsConstructor @Data
public class StreamedValue implements Serializable {

    private Service service;
    private long stepInSequence;
    private List<List<Writable>> originalWindow;
    private List<Writable> reduced;

}
