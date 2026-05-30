import java.io.*;
import java.nio.file.*;
import java.sql.*;

public class SeedRunner {
  public static void main(String[] a) throws Exception {
    String sql = new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8");
    StringBuilder clean = new StringBuilder();
    for (String line : sql.split("\n")) {
      if (line.trim().startsWith("--")) continue;   // 去行注释
      clean.append(line).append("\n");
    }
    Class.forName("com.mysql.cj.jdbc.Driver");
    String host = System.getenv().getOrDefault("MYSQL_HOST", "localhost");
    String url = "jdbc:mysql://" + host + ":3306/?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowMultiQueries=true";
    try (Connection c = DriverManager.getConnection(url, "root", a[1]);
         Statement st = c.createStatement()) {
      boolean hasRs = st.execute(clean.toString());   // 服务器端按引号正确分句
      int count = 0;
      while (true) {
        if (!hasRs) { int u = st.getUpdateCount(); if (u == -1) break; count++; }
        hasRs = st.getMoreResults();
      }
      System.out.println("OK, statement batches applied (updateCounts seen=" + count + ")");
    }
  }
}
