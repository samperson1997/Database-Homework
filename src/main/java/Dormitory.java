import java.sql.Connection;

public class Dormitory {
    private Connection con;
    private Util util;

    public static void main(String[] args) {
        Dormitory dormitory = new Dormitory();
        long startTime, endTime;

        System.out.println("#2");
        startTime = System.currentTimeMillis();
        dormitory.createTable();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

    }

    public Dormitory() {
        try {
            DBConnection dbConnection = new DBConnection();
            con = dbConnection.getConnection();
            con.setAutoCommit(false);

            util = new Util();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTable() {
        String createBuilding = "CREATE TABLE building\n" +
                "(\n" +
                "  build_id     INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  build_name   VARCHAR(50)     NOT NULL,\n" +
                "  build_campus VARCHAR(20)     NOT NULL,\n" +
                "  build_price  INT             NOT NULL,\n" +
                "  build_phone  VARCHAR(8)      NOT NULL\n" +
                ");";

        String createDepartment = "CREATE TABLE department\n" +
                "(\n" +
                "  dept_id   INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  dept_name VARCHAR(50)     NOT NULL\n" +
                ");";


        String createStudent = "CREATE TABLE student\n" +
                "(\n" +
                "  stu_id       VARCHAR(10) PRIMARY KEY NOT NULL,\n" +
                "  stu_name     VARCHAR(50)             NOT NULL,\n" +
                "  stu_gender   VARCHAR(5)              NOT NULL,\n" +
                "  stu_dept_id  INT                     NOT NULL,\n" +
                "  stu_build_id INT                     NOT NULL,\n" +
                "  FOREIGN KEY (stu_build_id) REFERENCES building (build_id),\n" +
                "  FOREIGN KEY (stu_dept_id) REFERENCES department (dept_id)\n" +
                ");";

        String[] statements = {createBuilding, createDepartment, createStudent};
        util.executeSQLs(statements, con);
    }
}