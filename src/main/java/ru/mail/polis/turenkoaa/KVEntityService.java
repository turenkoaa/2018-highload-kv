package ru.mail.polis.turenkoaa;

import one.nio.http.*;
import one.nio.pool.PoolException;
import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;
import ru.mail.polis.turenkoaa.util.PreparedRequest;

import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toMap;
import static one.nio.http.Response.*;
import static ru.mail.polis.turenkoaa.util.ServiceHelper.*;

public class KVEntityService extends HttpServer implements KVService {
    private final EntityController controller;

    public KVEntityService(final int port, final KVDao dao, Set<String> topology) throws IOException {
        super(from(port));
        ArrayList<String> nodePaths = new ArrayList<>(topology);
        int nodeId = nodePaths.indexOf(NODE_PATH + port);
        Map<Integer, HttpClient> replicas = extractReplicas(port, nodePaths);
        controller = new EntityController(dao, nodeId, replicas);
    }

    @Path(STATUS_PATH)
    public Response status() {
        return ok("Server starts ok");
    }

    @Override
    public void handleDefault(@NotNull final Request request, @NotNull final HttpSession session) throws IOException {
        try {
            controller.resolveRequest(request, session);
        } catch (IllegalArgumentException e) {
            session.sendResponse(new Response(BAD_REQUEST, e.getMessage().getBytes()));
        } catch (Exception e) {
            session.sendResponse(new Response(INTERNAL_ERROR, e.getMessage().getBytes()));
        }

    }

//    private boolean checkNodesStatuses(@NotNull HttpSession session, PreparedRequest query, List<Integer> nodes) throws InterruptedException, PoolException, HttpException, IOException {
//        int counterActiveNodes = 0;
//        for (Integer nodeId : nodes) {
//            if (nodeId.equals(this.nodeId)) counterActiveNodes++;
//            else {
//                Response responseStatus = replicas.get(nodeId).get(STATUS_PATH);
//                if (responseStatus.getStatus() == 200) counterActiveNodes++;
//            }
//        }
//        if (counterActiveNodes < query.getAck()) {
//            session.sendResponse(new Response(GATEWAY_TIMEOUT));
//            return false;
//        }
//        return true;
//    }

}

