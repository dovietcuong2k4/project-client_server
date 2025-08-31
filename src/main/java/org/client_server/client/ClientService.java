package org.client_server.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.client_server.model.Student;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientService implements Closeable {
    private final int port;
    private final String host;
    private final ObjectMapper mapper;
    private Socket socket;
    private BufferedWriter writer;
    private BufferedReader reader;

    public ClientService(String host, int port) throws IOException {
        this.port = port;
        this.host = host;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        connect();
    }

    private void connect() throws IOException {
        this.socket = new Socket(host, port);
        this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
    }

    //Gửi yêu cầu
    private void sendRequest(String action, JsonNode payload) throws IOException {
        ObjectNode request = mapper.createObjectNode();
        request.put("action", action.toUpperCase());

        if (payload != null){
            request.set("payload", payload);
        }

        writer.write(mapper.writeValueAsString(request));
        writer.write("\n");
        writer.flush();
    }

    //Nhận phản hồi từ server
    private ObjectNode readResponse () throws IOException {
        String responseLine = reader.readLine();
        if (responseLine == null){
            throw new IOException("Server đóng kết nối");
        }
        return (ObjectNode) mapper.readTree(responseLine);
    }

    //Các api cho client
    public ObjectNode insert(Student student) throws IOException {
        sendRequest("INSERT", mapper.valueToTree(student));
        return readResponse();
    }

    public ObjectNode find(long id) throws IOException {
        ObjectNode payload = mapper.createObjectNode().put("id", id);
        sendRequest("FIND", payload);
        return readResponse();
    }

    public ObjectNode list() throws IOException{
        sendRequest("LIST", null);
        return  readResponse();
    }

    public ObjectNode update(Student student) throws IOException{
        sendRequest("UPDATE", mapper.valueToTree(student));
        return  readResponse();
    }

    public ObjectNode delete(long id) throws IOException{
        ObjectNode payload = mapper.createObjectNode().put("id",id);
        sendRequest("DELETE", payload);
        return  readResponse();
    }

    public ObjectNode quit() throws IOException{
        sendRequest("QUIT", null);
        return readResponse();
    }

    public Student toStudent(JsonNode node) throws JsonProcessingException {
        return mapper.treeToValue(node, Student.class);
    }

    @Override
    public void close() throws IOException {
        if (socket != null) socket.close();
        if (writer != null) writer.close();
        if (reader != null) reader.close();
    }
}
