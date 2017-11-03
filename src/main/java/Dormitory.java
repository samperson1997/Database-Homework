import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class Dormitory {
    private Connection con;
    private Util util;

    public static void main(String[] args) {
        Dormitory dormitory = new Dormitory();
        long startTime, endTime;

        System.out.println("#2 建表");
        startTime = System.currentTimeMillis();
        dormitory.createTable();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#3 插入数据");
        startTime = System.currentTimeMillis();
        dormitory.insertData();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#4 查询“王小星”同学所在宿舍楼的所有院系");
        startTime = System.currentTimeMillis();
        dormitory.searchWangXiaoxing();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#5 陶园一舍的住宿费用提高至 1200 元");
        startTime = System.currentTimeMillis();
        dormitory.updatePrice();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#6 软件学院男女研究生互换宿舍楼");
        startTime = System.currentTimeMillis();
        dormitory.exchangeBuilding();
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

    /**
     * #2
     * 建表
     */
    private void createTable() {
        String createBuilding = "CREATE TABLE building\n" +
                "(\n" +
                "  build_id     INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  build_name   VARCHAR(50)     NOT NULL,\n" +
                "  build_campus VARCHAR(20)     NOT NULL,\n" +
                "  build_price  INT             NOT NULL,\n" +
                "  build_phone  VARCHAR(8)      NOT NULL\n" +
                ")DEFAULT CHARSET = utf8;";

        String createDepartment = "CREATE TABLE department\n" +
                "(\n" +
                "  dept_id   INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  dept_name VARCHAR(50)     NOT NULL\n" +
                ")DEFAULT CHARSET = utf8;";


        String createStudent = "CREATE TABLE student\n" +
                "(\n" +
                "  stu_id       VARCHAR(10) PRIMARY KEY NOT NULL,\n" +
                "  stu_name     VARCHAR(50)             NOT NULL,\n" +
                "  stu_gender   VARCHAR(5)              NOT NULL,\n" +
                "  stu_dept_id  INT                     NOT NULL,\n" +
                "  stu_build_id INT                     NOT NULL,\n" +
                "  FOREIGN KEY (stu_build_id) REFERENCES building (build_id),\n" +
                "  FOREIGN KEY (stu_dept_id) REFERENCES department (dept_id)\n" +
                ")DEFAULT CHARSET = utf8;";

        String[] statements = {createBuilding, createDepartment, createStudent};
        util.executeSQLs(statements, con);
    }

    /**
     * 3
     * 插入数据
     */
    private void insertData() {
        Map<String, String> dormitoryPhoneMap = new HashMap<>();
        List<String[]> dormitoryPhoneList = util.readFile("src/main/resources/电话.txt");
        dormitoryPhoneList.remove(0);
        for (String[] dormitoryPhone : dormitoryPhoneList) {
            for (String s : dormitoryPhone) {
                dormitoryPhoneMap.put(s.split(";")[0], s.split(";")[1]);
            }
        }

        List<String> studentList = util.readExcelFile("src/main/resources/分配方案.xls");
        studentList.remove(0); // 去掉标题行

        // 插入department
        HashSet<String> departmentNameSet = new HashSet<>();
        for (String s : studentList) {
            departmentNameSet.add(s.split(",")[0]);
        }

        HashMap<String, Integer> departmentIdMap = new HashMap<>();
        Integer departmentCount = 1;

        for (String department : departmentNameSet) {
            util.executeSQL("INSERT INTO department (dept_name) VALUES ('" + department + "');", con);
            departmentIdMap.put(department, departmentCount++);
        }

        // 插入building
        HashMap<String, String[]> buildingMaps = new HashMap<>();
        for (String s : studentList) {
            //build_name, build_campus, build_price
            buildingMaps.put(s.split(",")[5], new String[]{s.split(",")[4], s.split(",")[6].split(";")[0]});
        }

        HashMap<String, Integer> buildingIdMap = new HashMap<>();
        Integer buildingCount = 1;

        for (String key : buildingMaps.keySet()) {
            String campus = buildingMaps.get(key)[0];
            String price = buildingMaps.get(key)[1];
            util.executeSQL("INSERT INTO building (build_name, build_campus, build_price, build_phone) VALUES"
                    + " ('" + key + "', '" + campus + "', " + price + ", '" + dormitoryPhoneMap.get(key) + "');", con);

            buildingIdMap.put(key, buildingCount++);
        }

        // 插入student
        for (String s : studentList) {
            util.executeSQL("INSERT into student (stu_id, stu_name, stu_gender, stu_dept_id, stu_build_id) VALUES"
                    + " ('" + s.split(",")[1] + "', '" + s.split(",")[2] + "', '" + s.split(",")[3] + "', '"
                    + departmentIdMap.get(s.split(",")[0]) + "', '" + buildingIdMap.get(s.split(",")[5]) + "');", con);

        }
    }

    /**
     * 4
     * 查询“王小星”同学所在宿舍楼的所有院系
     */
    private void searchWangXiaoxing() {
        String statement = "SELECT DISTINCT dept_name\n" +
                "FROM department\n" +
                "WHERE dept_id IN\n" +
                "      (SELECT s.stu_dept_id\n" +
                "       FROM student s\n" +
                "       WHERE s.stu_build_id =\n" +
                "             (SELECT s2.stu_build_id\n" +
                "              FROM student s2\n" +
                "              WHERE s2.stu_name = '王小星'));";
        util.executeSQL(statement, con);
    }

    /**
     * 5
     * 陶园一舍的住宿费用提高至 1200 元
     */
    private void updatePrice() {
        String statement = "UPDATE building\n" +
                "SET build_price = 1200\n" +
                "WHERE build_name = '陶园1舍';";
        util.executeSQL(statement, con);
    }

    /**
     * 6
     * 软件学院男女研究生互换宿舍楼
     */
    private void exchangeBuilding() {
        String selectMales = "SELECT DISTINCT stu_build_id\n" +
                "INTO @male_build_id\n" +
                "FROM student\n" +
                "WHERE stu_gender = '男'\n" +
                "      AND stu_dept_id IN\n" +
                "          (SELECT dept_id\n" +
                "           FROM department\n" +
                "           WHERE dept_name = '软件学院');";

        String selectFemales = "SELECT DISTINCT stu_build_id\n" +
                "INTO @female_build_id\n" +
                "FROM student\n" +
                "WHERE stu_gender = '女'\n" +
                "      AND stu_dept_id IN\n" +
                "          (SELECT dept_id\n" +
                "           FROM department\n" +
                "           WHERE dept_name = '软件学院');";

        String updateMales = "UPDATE student\n" +
                "SET stu_build_id = @female_build_id\n" +
                "WHERE stu_gender = '男'\n" +
                "      AND stu_dept_id IN\n" +
                "          (SELECT dept_id\n" +
                "           FROM department\n" +
                "           WHERE dept_name = '软件学院');";

        String updateFemales = "UPDATE student\n" +
                "SET stu_build_id = @male_build_id\n" +
                "WHERE stu_gender = '女'\n" +
                "      AND stu_dept_id IN\n" +
                "          (SELECT dept_id\n" +
                "           FROM department\n" +
                "           WHERE dept_name = '软件学院');";

        util.executeSQL(selectMales, con);
        util.executeSQL(selectFemales, con);
        util.executeSQLs(new String[]{updateMales, updateFemales}, con);
    }
}