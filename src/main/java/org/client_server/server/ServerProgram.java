package org.client_server.server;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerProgram {
    private static final int PORT = 12345;
    private static final int THREAD_POOL_SIZE = 10;// pool chứa số các luồng của client khi truy cập server


    public static void main(String[] args) {
        ExecutorService executor = new ThreadPoolExecutor(
                THREAD_POOL_SIZE, THREAD_POOL_SIZE, // số luồng core, số luồng max

                0L,TimeUnit.MILLISECONDS, // thời giản nghỉ của các luồng phụ (max > core)
                                                // phục vụ việc giãn nở pool thread, cao điểm thì mở rộng pool

                new ArrayBlockingQueue<>(100),// hàng đợi chô client khi thread pool đã đầy, có giới hạn

                new ThreadPoolExecutor.CallerRunsPolicy()//khi cả thread pool và queue đều đầu thì gọi callerRunsPolicy
                                                            // hàm main sẽ tự mình chạy thêm 1 client handler và không nhận thêm client kết nối
        );

        try(ServerSocket serverSocket = new ServerSocket(PORT)){

            log.info("Server started. Listening on port : {}", PORT);

            while (true){
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(100000);// quá 10s mà Client không gửi dữ liệu cho client handler xử lý thì ném ra ngoại lệ
                log.info("New client connected : {}", clientSocket.getRemoteSocketAddress());

                executor.submit(new ClientHandler(clientSocket));

            }
        } catch (IOException e) {
            log.error("Server có một lỗi", e);
        } finally {
            executor.shutdown();//shutdown không nhận thêm task mới
            try {
                if(!executor.awaitTermination(30, TimeUnit.SECONDS)){
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
            log.info("Server shut down.");
        }


    }
}
