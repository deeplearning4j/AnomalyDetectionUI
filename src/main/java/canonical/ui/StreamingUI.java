package canonical.ui;

import canonical.ui.resources.UIResource;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.deeplearning4j.ui.api.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Note re: the the use of Object here instead of a generic type:
 * Dropwizard uses Class.getGenericSuperClass() to pick up the config type for the application (here: UiConfig).
 * If we use a generic type on the StreamingUI class, it picks that up instead of the
 * So: no generics here -> Object instead of a generic type. Ugly and a little unsafe, but it works
 */
public abstract class StreamingUI extends Application<UiConfig> {

    private static final Logger log = LoggerFactory.getLogger(StreamingUI.class);
    private static final String WEB_TARGET_BASE_PATH = "http://localhost:8080/ui/update";
    private static volatile StreamingUI instance;

    private LinkedBlockingQueue<Object> predictionsToProcess = new LinkedBlockingQueue<>();

    //Web target for posting results:
    private final Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
//    private final WebTarget uiUpdateTarget = client.target("http://localhost:8080/ui/update");

    //Thread that calls processRecords function periodically:
    private Thread uiThread;
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    //Contains the updates for each resource (resources: specified by String key)
    private Map<String,ConcurrentLinkedQueue<UiUpdate>> updatesForResources = new ConcurrentHashMap<>();
    private Map<String,WebTarget> webTargetsForResources = new ConcurrentHashMap<>();
//    //Last time that updateUI(Component...) was called
//    private long lastUIComponentUpdateTime = 0L;
//    //Last time that the UI components were posted to the server
//    private long lastUIComponentPostTime = 0L;

    public StreamingUI() {

        //TODO: this is hacky, and needs to be done properly
        instance = this;

        try {
//            run("server", "dropwizard.yml");
            run("server", "nids-dropwizard.yml");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        //Create UI thread
        uiThread = new Thread(new UIProcessingThreadRunnable());
        uiThread.setDaemon(true);
        uiThread.start();

        log.info("UIDriver started: http://localhost:8080/ui/");

    }

    public static StreamingUI getInstance(){
        return instance;
    }

    @Override
    public void run(UiConfig uiConfig, Environment environment) throws Exception {
        environment.jersey().register(new UIResource());
    }

    @Override
    public String getName() {
        return "StreamingUI";
    }

    @Override
    public void initialize(Bootstrap<UiConfig> bootstrap) {
        //Necessary to avoid abstract method issue with certain dependencies/versions of dropwizard
        bootstrap.addBundle(new ViewBundle<UiConfig>() {
            @Override
            public Map<String, Map<String, String>> getViewConfiguration(UiConfig nidsConfig) {
                return ImmutableMap.of();
            }
        });
        bootstrap.addBundle(new AssetsBundle());
    }

    /**
     * Implement this method for processing new records. This will be run async
     * This method should periodically call updateUI to update the user interface
     *
     * @param newRecords    Most recent records to be processed
     */
    public abstract void processRecords(List<Object> newRecords);


    /**
     * Receive a single prediction (to be processed and/or displayed in the UI)
     */
    public void receivePrediction(Object prediction) {
        receivePredictions(Collections.singletonList(prediction));
    }

    /**
     * Receive a multiple predictions (to be processed and/or displayed in the UI)
     */
    public void receivePredictions(List<Object> predictions) {
        System.out.println("***** StreamingUI.receivePredictions() called: ***** " + predictions);
        predictionsToProcess.addAll(predictions);
    }


    /**
     * UiUpdate the UI. This should be called periodically by processRecords
     *
     * @param components New components to display in the UI
     */
    protected synchronized void updateUI(String resource, Component... components) {
        ConcurrentLinkedQueue<UiUpdate> updatesToBeProcessedForResource = updatesForResources.get(resource);
        if(updatesToBeProcessedForResource == null){
            updatesToBeProcessedForResource = new ConcurrentLinkedQueue<>();
            updatesForResources.put(resource,updatesToBeProcessedForResource);
        }

        UiUpdate update = new UiUpdate(System.currentTimeMillis(), components);
        updatesToBeProcessedForResource.add(update);

//        lastUIComponentUpdateTime = System.currentTimeMillis();
//        System.out.println("***** UPDATEUI WAS CALLED. TIME=" + lastUIComponentUpdateTime + " *****");
        System.out.println("***** UPDATEUI WAS CALLED FOR RESOURCE \"" + resource + "\". TIME=" + update.time + " *****");
    }

    /**
     * This runnable is responsible for:
     * - batching updates: i.e., periodically calling ProcessRecords on the new data
     * - posting the updates provided by processRecords (via that method calling updateUI)
     */
    private class UIProcessingThreadRunnable implements Runnable {

        private static final long updateFrequency = 2000L;  //milliseconds


        @Override
        public void run() {
            try {
                runHelper();
            } catch (Exception e) {
                log.error("Unchecked exception thrown during UI processing", e);
                e.printStackTrace();
            }
        }

        private void runHelper() {

            System.out.println("***** WE'RE DOING THE PROCESSING THING *****");

            List<Object> list = new ArrayList<>(100);

            while (!shutdown.get()) {

                //First: go through the queued predictions to process...
                try {
                    list.add(predictionsToProcess.take());   //Blocks if no elements are available
                } catch (InterruptedException e) {
                    log.warn("Interrupted exception thrown in UI driver thread");
                }
                predictionsToProcess.drainTo(list);      //Doesn't block, but retrieves + removes all elements

                System.out.println("***** GOT SOME DATA *****");
                processRecords(list);
                list.clear();


                //Iterate through all resources:
                Set<String> resources = updatesForResources.keySet();
                long processStartTime = System.currentTimeMillis();
                for(String resource : resources){

                    ConcurrentLinkedQueue<UiUpdate> updates = updatesForResources.get(resource);
                    if(updates == null || updates.size() == 0) continue;

                    //Really only want to process the most recent update, in case we have multiple ones...
                    //So we can throw away the earlier ones
                    UiUpdate lastUpdate = null;
                    while(!updates.isEmpty()){
                        UiUpdate temp = updates.poll();
                        if(temp != null) lastUpdate = temp;
                    }

                    //Post the components:
                    WebTarget wt = webTargetsForResources.get(resource);
                    if(wt == null){
                        wt = getWebTargetForResource(resource);
                        webTargetsForResources.put(resource,wt);
                    }

                    Response resp = wt.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
                            .post(Entity.entity(lastUpdate.components, MediaType.APPLICATION_JSON));
                    if(resp.getStatus() >= 400 && resp.getStatus() < 600) log.warn("UI update response for resource \"{}\": {}",resource,resp);

                    System.out.println("***** POSTED UPDATE FOR RESOURCE \"" + resource + "\": " + lastUpdate);
                }

                //Now, wait a bit before running the UI update loop again...
                long waitTime = updateFrequency - (System.currentTimeMillis() - processStartTime);
                if (waitTime > 0) {
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        log.warn("Interrupted exception thrown by Thread.sleep() for UI thread");
                    }
                }
            }
        }
    }


    @AllArgsConstructor @NoArgsConstructor @Data
    private static class UiUpdate {
        private long time;
        private Component[] components;

    }

    private WebTarget getWebTargetForResource(String resource){
        if(!resource.matches("(\\w|\\-|_)+")) throw new IllegalArgumentException("Invalid resource name: \"" + resource + "\"; must be (a-z,A-Z,0-9) and \"-\", \"_\" only");

        return client.target("http://localhost:8080/ui/update/" + resource + "/");
    }
}
