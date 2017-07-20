package com.akrome.creditsuisse;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.ExecutionContexts;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.unmarshalling.Unmarshaller;
import akka.stream.Materializer;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

public class HttpUtils {
    final ActorSystem system;
    final Materializer materializer;
    final ObjectMapper mapper;

    public HttpUtils(ActorSystem system) {
        this.system = system;
        system.actorOf(Props.create(MaterializerActor.class));
        this.materializer = MaterializerActor.materializer;
        mapper = new ObjectMapper();
    }

    public <T> T get(String url, Class<T> clazz) throws ExecutionException, InterruptedException, IOException {
        CompletionStage<HttpResponse> futureResponse = Http.get(system).singleRequest(HttpRequest.GET(url), materializer);
        CompletionStage<String> bodyFuture = futureResponse.thenCompose(response -> Unmarshaller.entityToString()
                .unmarshal(response.entity(), ExecutionContexts.global(), materializer));
        return mapper.readValue(bodyFuture.toCompletableFuture().get(), clazz);
    }

    public String post(String url, Object payload) throws ExecutionException, InterruptedException, IOException {
        HttpRequest httpRequest = HttpRequest.POST(url)
                .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, mapper.writeValueAsString(payload)));
        CompletionStage<HttpResponse> futureResponse = Http.get(system)
                .singleRequest(httpRequest, materializer);
        CompletionStage<String> bodyFuture = futureResponse.thenCompose(response -> Unmarshaller.entityToString()
                .unmarshal(response.entity(), ExecutionContexts.global(), materializer));
        return bodyFuture.toCompletableFuture().get();
    }

    public boolean delete(String url) throws ExecutionException, InterruptedException, IOException {
        CompletionStage<HttpResponse> futureResponse = Http.get(system).singleRequest(HttpRequest.DELETE(url), materializer);
        return futureResponse.toCompletableFuture().get().status().isSuccess();
    }


}
