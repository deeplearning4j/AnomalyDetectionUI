package canonical.ui.resources;

import io.dropwizard.views.View;
import org.deeplearning4j.ui.api.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Alex on 14/03/2016.
 */
@Path("/ui")
@Produces(MediaType.TEXT_HTML)
public class UIResource {

    private Map<String,Long> lastUpdateTimeForResource = new ConcurrentHashMap<>();
    private Map<String,List<Component>> lastComponentsForResource = new ConcurrentHashMap<>();

    @GET
    public View get(){
        return new UIView();
    }

    @GET
    @Path("/lastUpdateTime/{resource}")
    public Response getLastUpdateTime(@PathParam("resource") String resourceName){
        Long lastUpd = lastUpdateTimeForResource.get(resourceName);
        long time;
        if(lastUpd == null) time = 0;
        else time = lastUpd;
        return Response.ok(time).build();
    }

    @POST
    @Path("update/{resource}/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(@PathParam("resource") String resourceName, List<Component> components){

        lastComponentsForResource.put(resourceName, components);
        lastUpdateTimeForResource.put(resourceName, System.currentTimeMillis());

//        System.out.println("*** GOT COMPONENTS FOR RESOURCE: + \"" + resourceName + "\", Components = " + components);
        return Response.ok(Collections.singletonMap("status", "ok")).build();
    }

    @GET
    @Path("/components/{resource}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponents(@PathParam("resource") String resourceName){
        List<Component> components = lastComponentsForResource.get(resourceName);
        if(components == null) components = Collections.emptyList();
//        System.out.println("**** UIResource: Returning values for resource \"" + resourceName + "\": " + components);
        return Response.ok(components).build();
    }

}
