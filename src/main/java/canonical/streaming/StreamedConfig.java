package canonical.streaming;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Alex on 22/04/2016.
 */
@AllArgsConstructor @Data
public class StreamedConfig {

    private String[][] deploy;
    private String[][] image;
    private String[][] network;
}
