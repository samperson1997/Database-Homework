import java.sql.Connection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Bike {
    private Connection con;
    private Util util;

    public static void main(String[] args) {
        Bike bike = new Bike();
        long startTime, endTime;

        System.out.println("#1 建表与插入数据");
        startTime = System.currentTimeMillis();
//        bike.createTable();
//        bike.insertData();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#2 家庭住址添加在用户表");
        startTime = System.currentTimeMillis();
//        bike.updateUserPlace();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#3 自动补全费用字段，并在用户账户中扣除相应的金额");
        startTime = System.currentTimeMillis();
        bike.updatePriceAndAmount();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#4 禁用上一个月内使用超200小时的单车");
        startTime = System.currentTimeMillis();
        bike.updateRepairTable();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");
    }

    public Bike() {
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
     * 1-1
     * 建表
     */
    private void createTable() {
        String createBike = "CREATE TABLE bike\n" +
                "(\n" +
                "  bike_id INT PRIMARY KEY NOT NULL\n" +
                ")DEFAULT CHARSET = utf8;";

        String createPlace = "CREATE TABLE place\n" +
                "(\n" +
                "  place_id   INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  place_name VARCHAR(255)    NOT NULL\n" +
                ")DEFAULT CHARSET = utf8;";

        String createUser = "CREATE TABLE user\n" +
                "(\n" +
                "  user_id       INT PRIMARY KEY NOT NULL,\n" +
                "  user_name     VARCHAR(50)     NOT NULL,\n" +
                "  user_phone    VARCHAR(20)     NOT NULL,\n" +
                "  user_amount   FLOAT,\n" +
                "  user_place_id INT,\n" +
                "  FOREIGN KEY (user_place_id) REFERENCES place (place_id)\n" +
                ")DEFAULT CHARSET = utf8;";

        String createRecord = "CREATE TABLE record\n" +
                "(\n" +
                "  rec_id             INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  rec_start_time     DATETIME        NOT NULL,\n" +
                "  rec_end_time       DATETIME        NOT NULL,\n" +
                "  rec_price          FLOAT,\n" +
                "  rec_user_id        INT             NOT NULL,\n" +
                "  rec_bike_id        INT             NOT NULL,\n" +
                "  rec_start_place_id INT             NOT NULL,\n" +
                "  rec_end_place_id   INT             NOT NULL,\n" +
                "  FOREIGN KEY (rec_user_id) REFERENCES user (user_id),\n" +
                "  FOREIGN KEY (rec_bike_id) REFERENCES bike (bike_id),\n" +
                "  FOREIGN KEY (rec_start_place_id) REFERENCES place (place_id),\n" +
                "  FOREIGN KEY (rec_end_place_id) REFERENCES place (place_id)\n" +
                ")DEFAULT CHARSET = utf8;";

        String createRepair = "CREATE TABLE repair\n" +
                "(\n" +
                "  repair_id       INT PRIMARY KEY NOT NULL AUTO_INCREMENT,\n" +
                "  repair_bike_id  INT             NOT NULL,\n" +
                "  repair_place_id INT             NOT NULL,\n" +
                "  FOREIGN KEY (repair_bike_id) REFERENCES bike (bike_id),\n" +
                "  FOREIGN KEY (repair_place_id) REFERENCES place (place_id)\n" +
                ")DEFAULT CHARSET = utf8;";

        String[] statements = {createBike, createPlace, createUser, createRecord, createRepair};
        util.executeSQLs(statements, con);
    }

    /**
     * 1-2
     * 插入数据
     */
    private void insertData() {
        // 插入bike
        List<String[]> bikeList = util.readFile("src/main/resources/bike.txt");
        for (String[] bike : bikeList) {
            for (String s : bike) {
                util.executeSQL("INSERT INTO bike (bike_id) VALUES (" + s + ");", con);
            }
        }

        // 插入place
        List<String[]> recordList = util.readFile("src/main/resources/record.txt");
        Set<String> placeSet = new HashSet<>();
        for (String[] record : recordList) {
            for (String s : record) {
                placeSet.add(s.split(";")[2]);
                placeSet.add(s.split(";")[4]);
            }
        }

        Map<String, Integer> placeIdMap = new HashMap<>();
        Integer placeCount = 1;

        for (String place : placeSet) {
            util.executeSQL("INSERT INTO place (place_name) VALUES ('" + place + "');", con);
            placeIdMap.put(place, placeCount++);
        }

        // 插入user
        List<String[]> userList = util.readFile("src/main/resources/user.txt");

        for (String[] user : userList) {
            for (String s : user) {
                util.executeSQL("INSERT INTO user (user_id, user_name, user_phone, user_amount) VALUES ("
                        + s.split(";")[0] + ", '" + s.split(";")[1] + "', '" + s.split(";")[2] + "', " + s.split(";")[3] + ");", con);
            }

        }

        // 插入record
        DateFormat recDateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        DateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        int i = 0;
        for (String[] record : recordList) {
            for (String s : record) {
                String[] ssplit = s.split(";");
                try {
                    util.executeSQL("INSERT INTO record (rec_user_id, rec_bike_id, rec_start_place_id, rec_start_time, rec_end_place_id, rec_end_time) VALUES ("
                            + ssplit[0].trim().replaceAll("[^(a-zA-Z0-9\\u4e00-\\u9fa5)]", "") + ", " + ssplit[1] + ", " + placeIdMap.get(ssplit[2]) + ", '" + sqlDateFormat.format(recDateFormat.parse(ssplit[3]))
                            + "', " + placeIdMap.get(ssplit[4]) + ", '" + sqlDateFormat.format(recDateFormat.parse(ssplit[5])) + "');", con);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * 2
     * 家庭住址添加在用户表
     */
    private void updateUserPlace() {
        String statement = "UPDATE user, (\n" +
                "               SELECT\n" +
                "                 rec_user_id,\n" +
                "                 rec_end_place_id\n" +
                "               FROM record\n" +
                "               WHERE HOUR(rec_start_time) >= 18 AND HOUR(rec_end_time) <= 24\n" +
                "               GROUP BY rec_user_id, rec_end_place_id\n" +
                "               HAVING COUNT(rec_end_place_id) >= ALL (SELECT COUNT(r.rec_end_place_id) num\n" +
                "                                                      FROM record r\n" +
                "                                                      WHERE HOUR(r.rec_start_time) >= 18 AND HOUR(r.rec_end_time) <= 24\n" +
                "                                                            AND r.rec_user_id = record.rec_user_id\n" +
                "                                                      GROUP BY rec_user_id, rec_end_place_id)) f\n" +
                "SET user_place_id = f.rec_end_place_id\n" +
                "WHERE user_id = f.rec_user_id";
        util.executeSQL(statement, con);
    }

    /**
     * 3
     * 自动补全费用字段，并在用户账户中扣除相应的金额
     */
    private void updatePriceAndAmount() {

    }

    /**
     * 4
     * 禁用上一个月内使用超200小时的单车
     */
    private void updateRepairTable() {

    }
}
