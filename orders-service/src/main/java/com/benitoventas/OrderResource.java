package com.benitoventas;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

    @Inject 
    public OrderResource(OrderService orderService){
        this.orderService = orderService;
    }

    @POST 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@Valid CreateOrderRequest request){
        OrderResponse order = orderService.create(request);

        return Response.status(Response.Status.CREATED)
                .entity(order)
                .build();
    }

    @GET 
    @Path("/{id}")
    public OrderResponse findById(@PathParam("id") String id){
        return orderService.findById(id);
    }
}
