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
        loadJdbcDriver();
        checkURL();
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

    private static void loadJdbcDriver(){
        try {
            //bây giờ thì gần như không cần khai báo như này nữa vì jdbc 4.0+ và java 6 trở lên đã tự động
            //chỉ để chắc chắn đã cài jdbc driver vào project
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.info("MySQL jdbc Driver đã được load thành công.");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Không tìm thấy Jdbc Driver. Hãy thêm mysql-connector-java vào chương trình",e);
        }
    }

    //chỉ để kiểm tra các thuộc tính của url để tránh cảnh báo khi load db
    private static void checkURL(){
        if(!url.contains("serverTimeZone")){
            LOGGER.warning("Khuyến nghị thêm serverTimeZone=UTC vào url để tránh cảnh báo timezone");
        }
        if (!url.contains("useUnicode")){
            LOGGER.warning("Khuyến nghị thêm useUnicode=true&characterEncoding=UTF-8 vào url để hỗ trợ unicode Tiếng Việt ");
        }
    }

    public static void closeQuietly(){
        if ()
    }
}
