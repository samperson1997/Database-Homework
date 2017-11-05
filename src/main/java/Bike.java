import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Bike {
    private Connection con;
    private Util util;
    private PreparedStatement pstmt;
    private PreparedStatement pstmt1;

    public static void main(String[] args) {
        Bike bike = new Bike();
        long startTime, endTime;

        System.out.println("#1 建表与插入数据");
        System.out.println("#3 自动补全费用字段，并在用户账户中扣除相应的金额");
        startTime = System.currentTimeMillis();
        bike.createTable();
        bike.insertData();
        endTime = System.currentTimeMillis();
        System.out.println("Time: " + (endTime - startTime) + " ms");

        System.out.println("#2 家庭住址添加在用户表");
        startTime = System.currentTimeMillis();
        bike.updateUserPlace();
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
                "  rec_cost           INT             NOT NULL,\n" +
                "  rec_user_id        INT             NOT NULL,\n" +
                "  rec_bike_id        INT             NOT NULL,\n" +
                "  rec_start_place_id INT             NOT NULL,\n" +
                "  rec_end_place_id   INT             NOT NULL,\n" +
                "  FOREIGN KEY (rec_user_id) REFERENCES user (user_id),\n" +
                "  FOREIGN KEY (rec_bike_id) REFERENCES bike (bike_id),\n" +
                "  FOREIGN KEY (rec_start_place_id) REFERENCES place (place_id),\n" +
                "  FOREIGN KEY (rec_end_place_id) REFERENCES place (place_id)\n" +
                ")DEFAULT CHARSET = utf8;";

        String[] statements = {createBike, createPlace, createUser, createRecord};
        util.executeSQLs(statements, con);
    }

    /**
     * 1-2
     * 插入数据
     */
    private void insertData() {
        // 插入bike
        List<String[]> bikeList = util.readFile("src/main/resources/bike.txt");
        try {
            pstmt = con.prepareStatement("INSERT INTO bike (bike_id) VALUES (?)");
            for (String[] bike : bikeList) {
                for (String s : bike) {
//                util.executeSQL("INSERT INTO bike (bike_id) VALUES (" + s + ");", con);
                    pstmt.setString(1, s);
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
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
        try {
            pstmt = con.prepareStatement("INSERT INTO place (place_name) VALUES (?)");

            for (String place : placeSet) {
                pstmt.setString(1, place);
                pstmt.addBatch();

//            util.executeSQL("INSERT INTO place (place_name) VALUES ('" + place + "');", con);
                placeIdMap.put(place, placeCount++);
            }
            pstmt.executeBatch();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 插入user
        List<String[]> userList = util.readFile("src/main/resources/user.txt");
        Map<String, String> userAmountMap = new HashMap<>();
        try {
            pstmt = con.prepareStatement("INSERT INTO user (user_id, user_name, user_phone, user_amount) VALUES (?,?,?,?)");

            for (String[] user : userList) {
                for (String s : user) {
                    userAmountMap.put(s.split(";")[0], s.split(";")[3]);
//                util.executeSQL("INSERT INTO user (user_id, user_name, user_phone, user_amount) VALUES ("
//                        + s.split(";")[0] + ", '" + s.split(";")[1] + "', '" + s.split(";")[2] + "', " + s.split(";")[3] + ");", con);
                    pstmt.setInt(1, Integer.parseInt(s.split(";")[0]));
                    pstmt.setString(2, s.split(";")[1]);
                    pstmt.setString(3, s.split(";")[2]);
                    pstmt.setDouble(4, Double.parseDouble(s.split(";")[3]));
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // 插入record
        DateFormat recDateFormat = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
        DateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try {
            pstmt = con.prepareStatement("INSERT INTO record (rec_cost, rec_user_id, rec_bike_id, rec_start_place_id, " +
                    "rec_start_time, rec_end_place_id, rec_end_time) VALUES (?,?,?,?,?,?,?)");
            pstmt1 = con.prepareStatement("UPDATE user\n" +
                    "SET user_amount = ? WHERE user_id = ?");
            for (String[] record : recordList) {
                for (String s : record) {
                    String[] ssplit = s.split(";");
                    try {
                        //计算本次record_cost
                        int minutes = (int) (recDateFormat.parse(ssplit[5]).getTime() - recDateFormat.parse(ssplit[3]).getTime()) / 1000;
                        int rec_cost;
                        if (minutes <= 1800) {
                            rec_cost = 1;
                        } else if (minutes <= 3600) {
                            rec_cost = 2;
                        } else if (minutes <= 5400) {
                            rec_cost = 3;
                        } else {
                            rec_cost = 4;
                        }

                        // 判断user余额是否足够
                        String user_id = ssplit[0].trim().replaceAll("[^(a-zA-Z0-9\\u4e00-\\u9fa5)]", "");
                        double user_amount = Double.parseDouble(userAmountMap.get(user_id)) - rec_cost;
                        if (user_amount > 0) {

                            // 足够，则插入record
                            pstmt.setInt(1, rec_cost);
                            pstmt.setInt(2, Integer.parseInt(user_id));
                            pstmt.setInt(3, Integer.parseInt(ssplit[1]));
                            pstmt.setInt(4, placeIdMap.get(ssplit[2]));
                            pstmt.setString(5, sqlDateFormat.format(recDateFormat.parse(ssplit[3])));
                            pstmt.setInt(6, placeIdMap.get(ssplit[4]));
                            pstmt.setString(7, sqlDateFormat.format(recDateFormat.parse(ssplit[5])));
                            pstmt.addBatch();

//                        util.executeSQL("INSERT INTO record (rec_cost, rec_user_id, rec_bike_id, rec_start_place_id, " +
//                                "rec_start_time, rec_end_place_id, rec_end_time) VALUES ("
//                                + rec_cost + ", " + user_id + ", " + ssplit[1] + ", " + placeIdMap.get(ssplit[2]) + ", '"
//                                + sqlDateFormat.format(recDateFormat.parse(ssplit[3])) + "', " + placeIdMap.get(ssplit[4]) + ", '"
//                                + sqlDateFormat.format(recDateFormat.parse(ssplit[5])) + "');", con);

                            // 并更新用户余额
                            pstmt1.setDouble(1, user_amount);
                            pstmt1.setInt(2, Integer.parseInt(user_id));
                            pstmt1.addBatch();
//
//                        util.executeSQL("UPDATE user\n" +
//                                "SET user_amount = " + user_amount
//                                + "WHERE user_id = " + user_id + ";", con);
                        }

                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

            }
            pstmt.executeBatch();
            pstmt1.executeBatch();
            con.commit();
        } catch (SQLException e) {
            e.printStackTrace();
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
     * 4
     * 禁用上一个月内使用超200小时的单车
     */
    private void updateRepairTable() {
        Calendar c = Calendar.getInstance();

        if (c.get(Calendar.DAY_OF_MONTH) == 1) {
            String dropStatement = "DROP TABLE IF EXISTS repair;";
            String createStatement = "CREATE TABLE repair(\n" +
                    "  SELECT\n" +
                    "    r.rec_bike_id,\n" +
                    "    r.rec_end_place_id\n" +
                    "  FROM\n" +
                    "    (SELECT\n" +
                    "       rec_bike_id,\n" +
                    "       max(rec_end_time)                                        AS last_time,\n" +
                    "       sum(TIMESTAMPDIFF(SECOND, rec_start_time, rec_end_time)) AS use_time\n" +
                    "     FROM record\n" +
                    "     WHERE date_format(rec_start_time, '%Y%m') = date_format(now(), '%Y%m') - 1\n" +
                    "     GROUP BY rec_bike_id) f, record r\n" +
                    "  WHERE f.use_time > 200 * 3600 AND f.rec_bike_id = r.rec_bike_id AND r.rec_end_time = f.last_time);";
            String[] statements = new String[]{dropStatement, createStatement};
            util.executeSQLs(statements, con);
        }
    }
}
