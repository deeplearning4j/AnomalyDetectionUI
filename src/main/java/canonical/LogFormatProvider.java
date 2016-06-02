package canonical;


import io.skymind.echidna.api.schema.SequenceSchema;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by Alex on 17/04/2016.
 */
public class LogFormatProvider {

    //TODO: We might filter things here to keep only top N eventually...
    private static List<String> NEUTRON_LOCATIONS =
            Arrays.asList("calico.openstack.mech_calico", "keystoneclient.middleware.auth_token", "keystonemiddleware.auth_token",
                    "neutron", "neutron.agent.dhcp.agent", "neutron.agent.dhcp_agent", "neutron.agent.l2.extensions.manager",
                    "neutron.agent.l3.agent", "neutron.agent.l3_agent", "neutron.agent.linux.async_process", "neutron.agent.linux.interface",
                    "neutron.agent.linux.ovs_lib", "neutron.agent.linux.ovsdb_monitor", "neutron.agent.linux.utils", "neutron.agent.metadata.agent",
                    "neutron.agent.ovsdb.impl_vsctl", "neutron.agent.securitygroups_rpc", "neutron.api.extensions", "neutron.api.rpc.agentnotifiers.dhcp_rpc_agent_api",
                    "neutron.api.rpc.handlers.dhcp_rpc", "neutron.api.v2.resource", "neutron.common.config", "neutron.db.dhcp_rpc_base",
                    "neutron.db.metering.metering_rpc", "neutron.notifiers.nova", "neutron.openstack.common.db.sqlalchemy.session",
                    "neutron.openstack.common.loopingcall", "neutron.openstack.common.rpc.amqp", "neutron.openstack.common.rpc.common",
                    "neutron.openstack.common.service", "neutron.plugins.ml2.drivers.openvswitch.agent.ovs_neutron_agent",
                    "neutron.plugins.ml2.drivers.type_gre", "neutron.plugins.ml2.plugin", "neutron.plugins.ml2.rpc",
                    "neutron.plugins.openvswitch.agent.ovs_neutron_agent", "neutron.quota", "neutron.scheduler.dhcp_agent_scheduler",
                    "neutron.scheduler.l3_agent_scheduler", "neutron.service", "neutron.services.loadbalancer.agent.agent_manager",
                    "neutron.services.metering.agents.metering_agent", "oslo.messaging._drivers.amqpdriver", "oslo.messaging._drivers.common",
                    "oslo.messaging._drivers.impl_rabbit", "oslo.messaging.rpc.dispatcher", "oslo.service.loopingcall",
                    "oslo_log.versionutils", "oslo_messaging._drivers.impl_rabbit", "oslo_messaging.server", "oslo_service.service");

    private static List<String> NOVA_LOCATIONS =
            Arrays.asList("248_add_expire_reservations_index", "keystoneclient.middleware.auth_token", "keystonemiddleware.auth_token",
                    "migrate.versioning.api", "nova.api.ec2.cloud", "nova.api.openstack", "nova.api.openstack.compute",
                    "nova.api.openstack.compute.contrib.attach_interfaces", "nova.api.openstack.compute.extensions",
                    "nova.api.openstack.compute.legacy_v2.contrib.admin_actions", "nova.api.openstack.compute.servers",
                    "nova.api.openstack.extensions", "nova.compute.manager", "nova.compute.monitors", "nova.compute.resource_tracker",
                    "nova.conductor.api", "nova.ec2.wsgi.server", "nova.network.neutronv2.api", "nova.objectstore.s3server",
                    "nova.openstack.common.loopingcall", "nova.openstack.common.periodic_task", "nova.openstack.common.service",
                    "nova.openstack.common.threadgroup", "nova.osapi_compute.wsgi.server", "nova.scheduler.driver",
                    "nova.scheduler.filter_scheduler", "nova.scheduler.host_manager", "nova.scheduler.utils", "nova.service",
                    "nova.servicegroup.drivers.db", "nova.virt.driver", "nova.virt.libvirt.driver", "nova.virt.libvirt.guest",
                    "nova.virt.libvirt.host", "nova.virt.libvirt.imagecache", "nova.volume.cinder", "nova.wsgi",
                    "oslo.messaging._drivers.amqpdriver", "oslo.messaging._drivers.common", "oslo.messaging._drivers.impl_rabbit",
                    "oslo.messaging.rpc.dispatcher", "oslo.service.loopingcall", "oslo_config.cfg", "oslo_db.sqlalchemy.engines",
                    "oslo_messaging._drivers.amqpdriver", "oslo_messaging._drivers.common", "oslo_messaging._drivers.impl_rabbit",
                    "oslo_messaging.rpc.dispatcher", "oslo_messaging.server", "oslo_service.periodic_task", "oslo_service.service",
                    "oslo_service.threadgroup");


    //Format: "YYYY-MM-dd HH:mm:ss.SSS ThreadId??? LogLevel Location [some UID in brackets] Message"
    private static final String NEUTRON_NOVA_CINDER_REGEX = "(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) (\\d+) ([A-Z]+) ([\\w|\\.]+) (\\[.*?\\]) (.*)";

    public static String getLogFormatRegexForFile(URI uri){

        String path = uri.getPath();

        //Format for nova/neutron/cinder (and possibly others)
        //TODO: this is a bit brittle
        if(path.contains("neutron-api") || path.contains("neutron-gateway") || path.contains("nova-cloud-controller")
                || path.contains("nova-compute") || path.contains("cinder")){

            return NEUTRON_NOVA_CINDER_REGEX;
        } else {
            throw new UnsupportedOperationException("Unsupported/not yet implemented regex for file: " + path);
        }
    }

    public static SequenceSchema getSchemaForService(Service service, boolean addlogNameField){

        if(service != Service.Neutron && service != Service.Nova && service != Service.Cinder){
            throw new UnsupportedOperationException("Service not yet implemented: " + service);
        }

        SequenceSchema.Builder schemaBuilder = (SequenceSchema.Builder)new SequenceSchema.Builder()
            .addColumnsString("DateTime")
            .addColumnInteger("ThreadID?")
            .addColumnCategorical("LogLevel", Arrays.asList("AUDIT","TRACE","INFO","WARNING","ERROR","CRITICAL"))
            .addColumnsString("Location","UID","LogMessage");

        if(addlogNameField) schemaBuilder.addColumnCategorical("LogFileName",LogUriProvider.getLogFileNamesForService(service));

        return schemaBuilder.build();
    }

    /**
     *
     * @param service    Service to get location values for
     * @param alsoAdd    A list (optionally null) of additional location values to add. Intended use: add "Other" value
     */
    public static List<String> getLocationsForService(Service service, String... alsoAdd){

        List<String> list = new ArrayList<>();
        if(alsoAdd != null && alsoAdd.length > 0) Collections.addAll(list,alsoAdd);

        switch(service){
            case Neutron:
                list.addAll(NEUTRON_LOCATIONS);
                break;
            case Nova:
                list.addAll(NOVA_LOCATIONS);
                break;
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
                throw new UnsupportedOperationException("Not yet implemented: " + service);
        }

        return list;
    }
}
