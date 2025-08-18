package org.client_server.util;

import com.sun.jdi.connect.Connector;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class DBConnector {
    private static final Logger LOGGER = Logger.getLogger(DBConnector.class.getName());
    private static final String DB_PROPERTIES_FILE = "db.properties";

    private static String url;
    private static String user;
    private static String password;

    //Khối này sẽ được chạy ngay khi được nhắc đến và chạy 1 lần duy nhất
    static {
        loadProperties();
    }

    private DBConnector() {
        //ngăn tạo instance - không cho tạo như 1 object bình thường
    }

    public static Connection getConnector() throws SQLException {
        return DriverManager.getConnection(url,user,password);
    }

    private static void loadProperties(){
        Properties properties = new Properties(); //properties là object chứa các cặp key = value sẽ được load từ in
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(DB_PROPERTIES_FILE)) {

            if (in == null) {
                throw new IllegalStateException("Không tìm thấy cấu hình: " + DB_PROPERTIES_FILE
                        + "(file trong resource/)");
            }
            properties.load(in);//nạp toàn bộ nội dung bên trong db.properties vào

            url = require(properties,"db.url");
            user = require(properties, "db.user");
            password = require(properties, "db.password");

            //log để kiểm tra
            LOGGER.info(()->"DB properties load. url = " + url + " \tuser = "+user);

        } catch (IOException e) {
            throw new IllegalStateException("Lỗi khi đọc file cấu hình " + DB_PROPERTIES_FILE, e);
        }
    }

    private static String require(Properties p , String key){
        String v =p.getProperty(key);
        return Objects.requireNonNull(v,"Thiếu khóa cấu hình: "+ key);
    }
}
