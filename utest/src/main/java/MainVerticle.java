package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

    private MongoClient mongoClient;

    @Override
    public void start() {
        // Configure MongoDB client
        JsonObject mongoConfig = new JsonObject()
                .put("db_name", "StationaryShop")
                .put("connection_string", "mongodb://localhost:27017");

        mongoClient = MongoClient.createShared(vertx, mongoConfig);

        // Set up HTTP server and routes
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        // Define routes and their handlers
        router.post("/inventory").handler(this::addItem);
        router.get("/inventory/:id").handler(this::getItem);
        router.put("/inventory/:id").handler(this::updateItem);
        router.delete("/inventory/:id").handler(this::deleteItem);

        // Start HTTP server
        vertx.createHttpServer().requestHandler(router).listen(8080, result -> {
            if (result.succeeded()) {
                System.out.println("HTTP server started on port 8080");
            } else {
                System.out.println("Failed to start HTTP server: " + result.cause().getMessage());
            }
        });
    }

    private void addItem(RoutingContext context) {
        JsonObject item = context.getBodyAsJson();
        mongoClient.save("inventory", item, res -> {
            if (res.succeeded()) {
                context.response().setStatusCode(201).end("Item added with ID: " + res.result());
            } else {
                context.response().setStatusCode(500).end("Failed to add item: " + res.cause().getMessage());
            }
        });
    }

    private void getItem(RoutingContext context) {
        String id = context.pathParam("id");
        mongoClient.findOne("inventory", new JsonObject().put("_id", id), null, res -> {
            if (res.succeeded()) {
                if (res.result() != null) {
                    context.response().end(res.result().encodePrettily());
                } else {
                    context.response().setStatusCode(404).end("Item not found");
                }
            } else {
                context.response().setStatusCode(500).end("Failed to retrieve item: " + res.cause().getMessage());
            }
        });
    }

    private void updateItem(RoutingContext context) {
        String id = context.pathParam("id");
        JsonObject updates = context.getBodyAsJson();
        mongoClient.updateCollection("inventory", new JsonObject().put("_id", id), new JsonObject().put("$set", updates), res -> {
            if (res.succeeded()) {
                context.response().end("Item updated");
            } else {
                context.response().setStatusCode(500).end("Failed to update item: " + res.cause().getMessage());
            }
        });
    }

    private void deleteItem(RoutingContext context) {
        String id = context.pathParam("id");
        mongoClient.removeDocument("inventory", new JsonObject().put("_id", id), res -> {
            if (res.succeeded()) {
                context.response().end("Item deleted");
            } else {
                context.response().setStatusCode(500).end("Failed to delete item: " + res.cause().getMessage());
            }
        });
    }

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MainVerticle());
    }
}