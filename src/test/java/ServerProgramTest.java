
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.client_server.server.ServerProgram;
import org.junit.jupiter.api.*;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bản refactor đầy đủ cho ServerProgramTest.
 * - Có helper sendRequest
 * - Nhóm test bằng @Nested
 * - Cover các action: INSERT, LIST, FIND, UPDATE, DELETE, QUIT
 * - Cover một số negative cases (missing payload, invalid gpa, unknown action)
 *
 * Lưu ý: tests này là dạng integration test chạy trực tiếp lên ServerProgram (ServerProgram.main(...)).
 * Hãy chắc chắn ServerProgram lắng nghe ở cổng 12345 như hằng số PORT ở đây.
 */
@Slf4j
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServerProgramTest {
    private static final int PORT = 12345;
    private ExecutorService executor;
    private Future<?> serverFuture;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    void startServer() throws Exception {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "test-server");
            t.setDaemon(true);
            return t;
        });
        serverFuture = executor.submit(() -> ServerProgram.main(new String[]{}));

        // nhỏ ngủ để server kịp bind cổng
        try {
            TimeUnit.SECONDS.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @AfterAll
    void stopServer() throws Exception {
        if (serverFuture != null) serverFuture.cancel(true);
        if (executor != null) executor.shutdownNow();

        // thêm 1 khoảng nghỉ ngắn để giải phóng resource
        try {
            TimeUnit.MILLISECONDS.sleep(200);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /* -------------------- Helpers -------------------- */

    private ObjectNode sendRequest(ObjectNode request) throws Exception {
        try (Socket socket = new Socket("localhost", PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(3));

            writer.write(mapper.writeValueAsString(request));
            writer.write("\n");
            writer.flush();

            String responseStr = reader.readLine();
            if (responseStr == null) return null;
            return (ObjectNode) mapper.readTree(responseStr);
        }
    }

    private long findStudentIdByName(String name) throws Exception {
        ObjectNode listReq = mapper.createObjectNode();
        listReq.put("action", "list");

        ObjectNode resp = sendRequest(listReq);
        assertNotNull(resp, "Response from LIST should not be null");
        assertEquals("OK", resp.path("status").asText(), "LIST response must be OK");

        ArrayNode arr = (ArrayNode) resp.path("data");
        for (JsonNode n : arr) {
            if (name.equals(n.path("name").asText(null))) {
                // có thể các field id có tên khác nhau trong model -> kiểm tra các key thường gặp
                if (n.has("studentId")) return n.path("studentId").asLong(-1);
                if (n.has("id")) return n.path("id").asLong(-1);
                if (n.has("id_student")) return n.path("id_student").asLong(-1);
                if (n.has("student_id")) return n.path("student_id").asLong(-1);

                // fallback: nếu không tìm thấy tên trường id, thử coi có key số bất kỳ
                for (String key : new String[]{"studentId", "id", "id_student", "student_id"}) {
                    if (n.has(key)) return n.path(key).asLong(-1);
                }
            }
        }
        throw new IllegalStateException("Không tìm thấy student có name = " + name + " trong LIST response");
    }

    private long insertStudentReturningId(String name) throws Exception {
        ObjectNode req = mapper.createObjectNode();
        req.put("action", "insert");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("name", name);
        payload.put("dob", "2000-01-12");
        payload.put("gpa", 3.5);
        payload.put("sex", "male");
        payload.put("major", "IT");
        req.set("payload", payload);

        ObjectNode resp = sendRequest(req);
        assertNotNull(resp);
        assertEquals("OK", resp.path("status").asText(), "Insert should return OK");

        // tìm id vừa insert bằng LIST (dùng name unique)
        return findStudentIdByName(name);
    }

    /* -------------------- Tests grouped -------------------- */

    @Nested
    @DisplayName("Happy path: INSERT -> LIST -> FIND -> UPDATE -> DELETE")
    class HappyPath {

        @Test
        @DisplayName("Insert then list contains the student")
        void testInsertAndList() throws Exception {
            String name = "tst-" + UUID.randomUUID().toString().substring(0, 8);
            long id = insertStudentReturningId(name);
            assertTrue(id > 0, "Inserted student id phải lớn hơn 0");

            // kiểm tra LIST chứa tên
            ObjectNode listReq = mapper.createObjectNode();
            listReq.put("action", "list");
            ObjectNode listResp = sendRequest(listReq);
            assertEquals("OK", listResp.path("status").asText());
            ArrayNode arr = (ArrayNode) listResp.path("data");
            boolean found = false;
            for (JsonNode n : arr) if (name.equals(n.path("name").asText(null))) found = true;
            assertTrue(found, "LIST phải chứa student vừa insert");
        }

        @Test
        @DisplayName("Full flow: find -> update -> find -> delete -> find (not exist)")
        void testFindUpdateDeleteFlow() throws Exception {
            String name = "tst-" + UUID.randomUUID().toString().substring(0, 8);
            long id = insertStudentReturningId(name);

            // FIND
            ObjectNode findReq = mapper.createObjectNode();
            findReq.put("action", "find");
            ObjectNode fp = mapper.createObjectNode();
            fp.put("id", id);
            findReq.set("payload", fp);
            ObjectNode findResp = sendRequest(findReq);
            assertEquals("OK", findResp.path("status").asText());
            assertEquals(name, findResp.path("data").path("name").asText());

            // UPDATE name
            String newName = name + "-upd";
            ObjectNode updReq = mapper.createObjectNode();
            updReq.put("action", "update");
            ObjectNode up = mapper.createObjectNode();
            up.put("id", id);
            up.put("name", newName);
            updReq.set("payload", up);
            ObjectNode updResp = sendRequest(updReq);
            assertEquals("OK", updResp.path("status").asText());

            // FIND again to verify
            ObjectNode findReq2 = mapper.createObjectNode();
            findReq2.put("action", "find");
            ObjectNode fp2 = mapper.createObjectNode();
            fp2.put("id", id);
            findReq2.set("payload", fp2);
            ObjectNode findResp2 = sendRequest(findReq2);
            assertEquals("OK", findResp2.path("status").asText());
            assertEquals(newName, findResp2.path("data").path("name").asText());

            // DELETE
            ObjectNode delReq = mapper.createObjectNode();
            delReq.put("action", "delete");
            ObjectNode dp = mapper.createObjectNode();
            dp.put("id", id);
            delReq.set("payload", dp);
            ObjectNode delResp = sendRequest(delReq);
            assertEquals("OK", delResp.path("status").asText());

            // FIND again -> must return ID_NOT_EXIST
            ObjectNode findReq3 = mapper.createObjectNode();
            findReq3.put("action", "find");
            ObjectNode fp3 = mapper.createObjectNode();
            fp3.put("id", id);
            findReq3.set("payload", fp3);
            ObjectNode findResp3 = sendRequest(findReq3);
            assertEquals("ERROR", findResp3.path("status").asText());
            assertEquals("ID_NOT_EXIST", findResp3.path("code").asText());
        }

        @Test
        @DisplayName("QUIT should close the socket from server side")
        void testQuitClosesSocket() throws Exception {
            try (Socket socket = new Socket("localhost", PORT);
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

                socket.setSoTimeout((int) TimeUnit.SECONDS.toMillis(2));

                ObjectNode req = mapper.createObjectNode();
                req.put("action", "quit");
                writer.write(mapper.writeValueAsString(req));
                writer.write("\n");
                writer.flush();

                // server hiện implementation không trả response cho QUIT, mà return -> đóng socket
                String resp = reader.readLine();
                assertNull(resp, "Sau QUIT, server nên đóng socket => readLine() trả về null");
            }
        }
    }

    @Test
    void testInsertEmptyPayload() throws IOException {
        try (Socket socket = new Socket("localhost", PORT);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {

            ObjectMapper mapper = new ObjectMapper();
            ObjectNode request = mapper.createObjectNode();
            request.put("action", "INSERT");
            request.set("payload", mapper.createObjectNode()); // payload rỗng

            writer.write(request.toString());
            writer.write("\n");
            writer.flush();

            String response = reader.readLine();
            JsonNode respNode = mapper.readTree(response);

            assertEquals("ERROR", respNode.path("status").asText());
            assertEquals("MISSING_PAYLOAD", respNode.path("code").asText());
        }
    }


    @Nested
    @DisplayName("Negative cases: invalid input / missing payload / unknown action")
    class NegativeCases {

        @Test
        @DisplayName("Insert with invalid GPA should return INVALID_GPA")
        void testInsertInvalidGpa() throws Exception {
            String name = "tst-" + UUID.randomUUID().toString().substring(0, 8);
            ObjectNode req = mapper.createObjectNode();
            req.put("action", "insert");
            ObjectNode p = mapper.createObjectNode();
            p.put("name", name);
            p.put("dob", "2000-01-12");
            p.put("gpa", 10); // invalid
            p.put("sex", "male");
            p.put("major", "IT");
            req.set("payload", p);

            ObjectNode resp = sendRequest(req);
            assertEquals("ERROR", resp.path("status").asText());
            assertEquals("INVALID_GPA", resp.path("code").asText());
        }

        @Test
        @DisplayName("Insert missing payload should return MISSING_PAYLOAD")
        void testInsertMissingPayload() throws Exception {
            ObjectNode req = mapper.createObjectNode();
            req.put("action", "insert");

            ObjectNode resp = sendRequest(req);
            assertEquals("ERROR", resp.path("status").asText());
            assertEquals("MISSING_PAYLOAD", resp.path("code").asText());
        }

        @Test
        @DisplayName("Unknown action returns UNKNOWN_ACTION")
        void testUnknownAction() throws Exception {
            ObjectNode req = mapper.createObjectNode();
            req.put("action", "i-dont-know");

            ObjectNode resp = sendRequest(req);
            assertEquals("ERROR", resp.path("status").asText());
            assertEquals("UNKNOWN_ACTION", resp.path("code").asText());
        }
    }
}
