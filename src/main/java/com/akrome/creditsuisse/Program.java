package com.akrome.creditsuisse;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.akrome.creditsuisse.orders.OrderActor;
import com.akrome.creditsuisse.orders.OrderRegionExtractor;
import com.akrome.creditsuisse.routes.OrderRoute;

public class Program {
    ActorSystem system;

    public static void main(String[] args) {
        Program p = new Program();
        p.run(args);
    }

    public void run(String[] args) {
        system = ActorSystem.create("ClusterSystem");
        initRegions(system);
        initHttp(system);
    }

    private void initRegions(ActorSystem system) {
        ClusterSharding cs = ClusterSharding.get(system);
        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        cs.start(OrderRegionExtractor.REGION_NAME, Props.create(OrderActor.class), settings, new OrderRegionExtractor());
    }

    private void initHttp(ActorSystem system) {
        Http http = Http.get(system);
        ActorMaterializer materializer = ActorMaterializer.create(system);
        Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = new OrderRoute(system).getRoute().flow(system, materializer);
        http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8080), materializer);
    }

    public ActorSystem getSystem() {
        return system;
    }
}
