import org.client_server.util.DBConnector;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class DBConnector_Test {
    private static Logger LOGGER = Logger.getLogger(DBConnector_Test.class.getName());
    @Test
    void testConnectorNotNull() throws Exception{
        try(Connection connection = DBConnector.getConnector()){
            assertNotNull(connection,() -> "Connection đang là null");
            LOGGER.info("Test thành công: kết nối hoạt động bình thường");
        }
    }
}
