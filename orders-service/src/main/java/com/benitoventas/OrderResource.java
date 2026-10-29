package com.benitoventas;

import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.common.annotation.Blocking;
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

    @POST
    @Blocking
    @RolesAllowed("CLIENTE")
    @Consumes(MediaType.APPLICATION_JSON)
    public CompletionStage<Response> create(@Valid CreateOrderRequest request){
        return orderService.create(request, jwt.getSubject()).thenApply(order -> Response.status(Response.Status.CREATED).entity(order).build());
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
}
