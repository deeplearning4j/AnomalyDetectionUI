package canonical;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.net.URI;
import java.util.*;

public class LogUriProvider {

    private static final String[] NEUTRON_API_LOGS = new String[]{"server.log"};

    private static final String[] NEUTRON_GATEWAY_LOGS = new String[]{
            "dhcp-agent.log", "lbaas-agent.log", "metadata-agent.log", "metering_agent.log", "openvswitch-agent.log", "vpn_agent.log"};

    private static final String[] NOVA_CLOUD_CONTROLLER_LOGS = new String[]{
            "nova-api-ec2.log", "nova-api-os-compute.log", "nova-cert.log", "nova-conductor.log", "nova-manage.log", "nova-objectstore.log", "nova-scheduler.log"};

    private static final String[] NOVA_COMPUTE_LOGS = new String[]{
            "nova-compute.log", "nova-manage.log"};

    public static List<String> getLogFileNamesForService(Service service){
        Set<String> logNames = new HashSet<>();
        switch(service){
            case Neutron:
                Collections.addAll(logNames,NEUTRON_API_LOGS);
                Collections.addAll(logNames,NEUTRON_GATEWAY_LOGS);
                return new ArrayList<>(logNames);
            case Nova:
                Collections.addAll(logNames,NOVA_CLOUD_CONTROLLER_LOGS);
                Collections.addAll(logNames,NOVA_COMPUTE_LOGS);
                return new ArrayList<>(logNames);

            case Ceilometer:
            case Cinder:
            case Glance:
            case Heat:
            case Keystone:
            case MongoDB:
            case MySQL:
            case OpenStackDashboard:
            case RabbitMQ:
            case Swift:
            default:
                throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    /**
     * Get a list of URIs for log files, for the given service.
     * Assumption here is that the baseDirectory contains training data: /fail-.../, /pass-.../
     *
     * @param service          Service to fetch log URIs for
     * @param baseDirectory    Based directory (contains /fail-.../, /pass-.../)
     */
    public static List<URI> getURIs(Service service, File baseDirectory){

        List<URI> out = new ArrayList<>();

        File[] files = baseDirectory.listFiles();
        if(files == null || files.length == 0) throw new RuntimeException();

        for(File f : files){
            if(!f.isDirectory()) continue;

            out.addAll(getURIsForExample(service, f));
        }

        return out;
    }

    public static List<URI> getURIsForExample(Service service, File exampleDirectory){

        switch(service){
            case Neutron:
                return getURIsForNeutron(exampleDirectory);
            case Nova:
                return getURIsForNova(exampleDirectory);
            case Cinder:

            case Glance:
            case Heat:
            case Keystone:
            case MongoDB:
            case MySQL:
            case OpenStackDashboard:
            case RabbitMQ:
            case Swift:
            case Ceilometer:
            default:
                throw new UnsupportedOperationException("Loading for service not yet implemented: " + service);
        }


    }

    /**
     * Get URIs for neutron (both neutron-api and neutron-gateway)
     *
     * @param exampleDirectory  Directory of the example
     */
    private static List<URI> getURIsForNeutron(File exampleDirectory){
        List<URI> out = new ArrayList<>();

        File[] sub = exampleDirectory.listFiles();
        if(sub != null) {
            for (File s : sub) {
                if (!s.isDirectory()) continue;

                String[] logFileNames;

                String directoryName = s.getName();
                if (directoryName.startsWith("neutron-api")) {
                    logFileNames = NEUTRON_API_LOGS;
                } else if (directoryName.startsWith("neutron-gateway")) {
                    logFileNames = NEUTRON_GATEWAY_LOGS;
                } else {
                    //Not a neutron directory...
                    continue;
                }

                for (String logFileName : logFileNames) {
                    String path = FilenameUtils.concat(s.getAbsolutePath(), "var/log/neutron/" + logFileName);
                    File f = new File(path);
                    if (f.exists()) {
                        out.add(f.toURI());
                    }
                }
            }
        }

        return out;
    }

    /**
     * Get URIs for nova (both nova-cloud-controller and nova-compute)
     *
     * @param exampleDirectory  Directory of the example
     */
    private static List<URI> getURIsForNova(File exampleDirectory){
        List<URI> out = new ArrayList<>();

        File[] sub = exampleDirectory.listFiles();
        if(sub != null) {
            for (File s : sub) {
                if (!s.isDirectory()) continue;

                String directoryName = s.getName();
                String[] logFileNames;
                if (directoryName.startsWith("nova-cloud-controller")) {
                    logFileNames = NOVA_CLOUD_CONTROLLER_LOGS;
                } else if (directoryName.startsWith("nova-compute")) {
                    logFileNames = NOVA_COMPUTE_LOGS;
                } else {
                    //Not a nova directory...
                    continue;
                }

                for (String logFileName : logFileNames) {
                    String path = FilenameUtils.concat(s.getAbsolutePath(), "var/log/nova/" + logFileName);
                    File f = new File(path);
                    if (f.exists()) {
                        out.add(f.toURI());
                    }
                }
            }
        }

        return out;
    }
}
