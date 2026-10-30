package com.benitoventas.resources;

import com.benitoventas.dto.AdminOrderResponse;
import com.benitoventas.dto.CreateOrderRequest;
import com.benitoventas.dto.OrderResponse;
import com.benitoventas.dto.UpdateOrderNoteRequest;
import com.benitoventas.services.OrderService;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.common.annotation.Blocking;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.QueryParam;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderResource {

    private final OrderService orderService;
    private final JsonWebToken jwt;

    @Inject 
    public OrderResource(OrderService orderService, JsonWebToken jwt){
        this.orderService = orderService;
        this.jwt = jwt;
    }

    @GET
    @RolesAllowed("ADMIN")
    public List<AdminOrderResponse> listForAdmin(@QueryParam("customerId") String customerId, @QueryParam("status") String status,
            @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {

        return orderService.listForAdmin(customerId, status, page, size);
    }

    @POST
    @Blocking
    @RolesAllowed("CLIENTE")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull @Valid CreateOrderRequest request){
        var order = orderService.create(request, jwt.getSubject());

        return Response.status(Response.Status.CREATED).entity(order).build();
    }

    @GET 
    @Path("/{id}")
    @RolesAllowed({"CLIENTE", "ADMIN"})
    public OrderResponse findById(@PathParam("id") String id){
        var order = orderService.findById(id);

        boolean isAdmin = jwt.getGroups().contains("ADMIN");
        boolean isOwner = jwt.getSubject().equals(order.customerId());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("No puedes consultar ordenes de otro cliente");
        }

        return order;
    }

    @PUT
    @Path("/{id}/admin-note")
    @RolesAllowed("ADMIN")
    @Consumes(MediaType.APPLICATION_JSON)
    public AdminOrderResponse updateAdminNote(@PathParam("id") String id, @NotNull @Valid UpdateOrderNoteRequest request) {
        return orderService.updateAdminNote(id, request, jwt.getSubject());
    }
}
