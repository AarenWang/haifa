package org.wrj.haifa.openfeign.broadcast.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.FeignException;
import feign.Request;
import feign.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.wrj.haifa.openfeign.broadcast.rpc.BroadcastResult;
import org.wrj.haifa.openfeign.broadcast.rpc.InstanceResult;
import org.wrj.haifa.openfeign.broadcast.rpc.FeignBroadcast;

class BroadcastFeignClient implements Client, BroadcastClientMarker {

    private static final Logger log = LoggerFactory.getLogger(BroadcastFeignClient.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_PAYLOAD_SNIPPET = 200;

    private final Client originalClient;
    private final Client rawHttpClient;
    private final DiscoveryClient discoveryClient;

    BroadcastFeignClient(Client originalClient, DiscoveryClient discoveryClient) {
        this.originalClient = originalClient;
        this.rawHttpClient = extractDelegate(originalClient);
        this.discoveryClient = discoveryClient;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        Collection<String> broadcastValues = request.headers().get(FeignBroadcastConstants.BROADCAST_HEADER);
        if (CollectionUtils.isEmpty(broadcastValues)) {
            return originalClient.execute(request, options);
        }

        boolean failFast = parseFailFast(broadcastValues);
        Request sanitized = sanitize(request);
        String serviceId = resolveServiceId(sanitized);
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceId);
        if (CollectionUtils.isEmpty(instances)) {
            throw new IllegalStateException("No available service instances for " + serviceId);
        }

        List<InstanceResult> results = new ArrayList<>();
        boolean anyFailure = false;
        IOException lastException = null;

        for (ServiceInstance instance : instances) {
            Request perInstanceRequest = rebuildRequest(sanitized, instance);
            try {
                Response response = executeOnRawClient(perInstanceRequest, options);
                InstanceResult instanceResult = toInstanceResult(instance, response, null);
                results.add(instanceResult);
                if (!instanceResult.success()) {
                    anyFailure = true;
                    if (failFast) {
                        throw FeignException.errorStatus("broadcast", response);
                    }
                }
            } catch (IOException ex) {
                anyFailure = true;
                lastException = ex;
                log.warn(
                        String.format(
                                "Broadcast request to %s failed on %s:%d, failFast=%s. Reason: %s",
                                serviceId,
                                instance.getHost(),
                                Integer.valueOf(instance.getPort()),
                                Boolean.toString(failFast),
                                ex.getMessage()),
                        ex);
                results.add(toInstanceResult(instance, null, ex));
                if (failFast) {
                    throw ex;
                }
            }
        }

        if (results.isEmpty() && lastException != null) {
            throw lastException;
        }

        BroadcastResult result = new BroadcastResult(serviceId, failFast, results);
        String payload = OBJECT_MAPPER.writeValueAsString(result);
        Map<String, Collection<String>> headers =
                Map.of("Content-Type", List.of("application/json"));

        return Response.builder()
                .status(anyFailure ? 207 : 200)
                .reason(anyFailure ? "MULTI_STATUS" : "OK")
                .headers(headers)
                .request(sanitized)
                .body(payload, StandardCharsets.UTF_8)
                .build();
    }

    private Response executeOnRawClient(Request request, Request.Options options) throws IOException {
        Client client = (rawHttpClient != null) ? rawHttpClient : originalClient;
        return client.execute(request, options);
    }

    private boolean parseFailFast(Collection<String> broadcastValues) {
        return broadcastValues.stream()
                .anyMatch(FeignBroadcastConstants.FAIL_FAST_TRUE::equalsIgnoreCase);
    }

    private Request sanitize(Request request) {
        Map<String, Collection<String>> headers = new LinkedHashMap<>(request.headers());
        headers.remove(FeignBroadcastConstants.BROADCAST_HEADER);
        return Request.create(
                request.httpMethod(),
                request.url(),
                headers,
                request.body(),
                request.charset(),
                request.requestTemplate());
    }

    private String resolveServiceId(Request request) {
        URI uri = URI.create(request.url());
        String host = uri.getHost();
        Assert.hasText(host, "Unable to resolve serviceId from request url " + request.url());
        return host;
    }

    private Request rebuildRequest(Request request, ServiceInstance instance) {
        URI original = URI.create(request.url());
        URI target = UriComponentsBuilder.fromUri(instance.getUri())
                .path(original.getRawPath())
                .replaceQuery(original.getRawQuery())
                .build(true)
                .toUri();
        return Request.create(
                request.httpMethod(),
                target.toString(),
                request.headers(),
                request.body(),
                request.charset(),
                request.requestTemplate());
    }

    private InstanceResult toInstanceResult(ServiceInstance instance, Response response, IOException error)
            throws IOException {
        String instanceId = instance.getInstanceId();
        if (!StringUtils.hasText(instanceId)) {
            instanceId = instance.getHost() + ":" + instance.getPort();
        }
        if (error != null) {
            return new InstanceResult(
                    instanceId,
                    -1,
                    false,
                    error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        int status = response.status();
        boolean success = status >= 200 && status < 300;
        String body = readBodyAsString(response);
        log.debug(String.format(
                "Broadcast response from %s status=%d success=%s payload=%s",
                instanceId,
                Integer.valueOf(status),
                Boolean.toString(success),
                abbreviate(body)));
        return new InstanceResult(instanceId, status, success, body);
    }

    private String readBodyAsString(Response response) throws IOException {
        if (response.body() == null) {
            return "";
        }
        try (InputStream stream = response.body().asInputStream()) {
            byte[] bytes = stream.readAllBytes();
            Charset charset = response.request().charset() != null
                    ? response.request().charset()
                    : StandardCharsets.UTF_8;
            return new String(bytes, charset);
        }
    }

    private String abbreviate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() <= MAX_PAYLOAD_SNIPPET
                ? body
                : body.substring(0, MAX_PAYLOAD_SNIPPET) + "...";
    }

    private Client extractDelegate(Client client) {
        try {
            Method method = client.getClass().getMethod("getDelegate");
            Object candidate = method.invoke(client);
            if (candidate instanceof Client delegate) {
                return delegate;
            }
        } catch (NoSuchMethodException ignored) {
            // fall back to the original client
        } catch (IllegalAccessException | InvocationTargetException ex) {
            log.debug("Failed to extract delegate from {}", client.getClass().getName(), ex);
        }
        return null;
    }
}
