package ru.mail.polis.turenkoaa.util;

import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.server.AcceptorConfig;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.turenkoaa.model.PreparedRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class RequestUtil {

    public static final String STATUS_PATH = "/v0/status";
    public static final String ENTITY_PATH = "/v0/entity";
    public static final String NODE_PATH = "http://localhost:";

    private static final String ID = "id";
    private static final String AND = "&";
    private static final String EQUALS = "=";
    private static final String DELIMITER = "/";
    private static final String ENCODING = "UTF-8";
    private static final String REPLICAS = "replicas";
    private static final String INVALID_QUERY = "Invalid query";

    public static final String[] replicaRequestHeaders;

    public static final String HEADER_REPLICA_REQUEST = "replica_request";

    static {
        replicaRequestHeaders = new String[1];
        replicaRequestHeaders[0] = HEADER_REPLICA_REQUEST + true;
    }

    public static PreparedRequest prepareRequest(@NotNull Request request, int topologySize) {
        String key = request.getQueryString();
        if (key == null) {
            throw new IllegalArgumentException(INVALID_QUERY);
        }

        Map<String, String> params = getParams(key);
        String id = params.get(ID);

        int ack;
        int from ;
        if (params.containsKey(REPLICAS)) {
            String[] rp = params.get(REPLICAS).split(DELIMITER);
            ack = Integer.valueOf(rp[0]);
            from = Integer.valueOf(rp[1]);
        } else {
            ack = topologySize / 2 + 1;
            from = topologySize;
        }
        if (id == null || "".equals(id) || ack < 1 || ack > from) {
            throw new IllegalArgumentException(INVALID_QUERY);
        }
        boolean isRequestForReplica = Boolean.parseBoolean(request.getHeader(HEADER_REPLICA_REQUEST));

        return new PreparedRequest(id, ack, from, request.getBody(), isRequestForReplica, request.getURI());
    }

    private static Map<String, String> getParams(@NotNull String query) {
        try {
            Map<String, String> params = new HashMap<>();
            for (String param : query.split(AND)) {
                int index = param.indexOf(EQUALS);
                String param1 = URLDecoder.decode(param.substring(0, index), ENCODING);
                String param2 = URLDecoder.decode(param.substring(index + 1), ENCODING);
                params.put(param1, param2);
            }
            return params;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(INVALID_QUERY);
        }
    }

    public static HttpServerConfig from(final int port) {
        final AcceptorConfig ac = new AcceptorConfig();
        ac.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{ac};
        return config;
    }

}
