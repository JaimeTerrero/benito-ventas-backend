package com.benitoventas;

import io.quarkus.mongodb.panache.PanacheMongoRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped 
public class OrderRepository implements PanacheMongoRepository<Order>{

}
