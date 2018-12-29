package com.server.benchmark.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

public class VertxServer extends AbstractVerticle {

    private HttpServer server;

    private int port = 8001;

    public VertxServer(int port) {
        this.port = port;
    }

    public VertxServer() {
    }

    @Override
    public void start() {
        server = vertx.createHttpServer(new HttpServerOptions().setPort(port)
                .setTcpKeepAlive(true)
                .setAcceptBacklog(1000));
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        server.requestHandler(req -> {
            req.response().setStatusCode(200);
            req.response().end(req.path());
        });
        server.listen(s -> System.out.println("Vertx Server started"));
    }

    @Override
    public void stop() {
        server.close();
        System.out.println("Vertx server stopped");
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new VertxServer(8001));
//        vertx.deployVerticle(VertxServer.class, new DeploymentOptions().setInstances(2));
    }
}
