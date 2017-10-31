import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Util {

    public Util() {

    }

    public void executeSQLs(String[] statements, Connection con) {
        try (Statement statement = con.createStatement()) {
            for (String s : statements) {
                statement.addBatch(s);
            }
            statement.executeBatch();
            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void executeSQL(String sql, Connection con) {
        try (Statement statement = con.createStatement()) {
            statement.execute(sql);
            con.commit();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                con.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }
    }

}
