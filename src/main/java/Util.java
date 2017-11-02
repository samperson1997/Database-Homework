import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Util {

    public Util() {

    }

    /**
     * 执行数据库语句
     *
     * @param s
     * @param con
     */
    public void executeSQL(String s, Connection con) {
        try (Statement statement = con.createStatement()) {
            statement.execute(s);
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

    /**
     * 执行数据库语句
     *
     * @param statements
     * @param con
     */
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

    /**
     * 读文件
     *
     * @param filePath
     * @return
     */
    public List<String[]> readFile(String filePath) {
        List<String[]> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "utf-8"));) {
            String temp = br.readLine();

            while (temp != null) {
                list.add(temp.split("\t"));
                temp = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    /**
     * 读Excel文件
     *
     * @param filePath
     * @return
     */
    public List<String[]> readExcelFile(String filePath) {
        // 1、构造excel文件输入流对象
        List<String[]> list = new ArrayList<>();
        try {
            //创建输入流
            InputStream stream = new FileInputStream(filePath);
            //获取Excel文件对象
            Workbook rwb = Workbook.getWorkbook(stream);
            //获取文件的指定工作表 默认的第一个
            Sheet sheet = rwb.getSheet(0);
            //行数(表头的目录不需要，从1开始)
            for (int i = 0; i < sheet.getRows(); i++) {
                //创建一个数组 用来存储每一列的值
                String[] str = new String[sheet.getColumns()];
                Cell cell;
                //列数
                for (int j = 0; j < sheet.getColumns(); j++) {
                    //获取第i行，第j列的值
                    cell = sheet.getCell(j, i);
                    str[j] = cell.getContents();
                }
                //把刚获取的列存入list
                list.add(str);
            }
        } catch (IOException | BiffException e) {
            e.printStackTrace();
        }

        return list;
    }
}
