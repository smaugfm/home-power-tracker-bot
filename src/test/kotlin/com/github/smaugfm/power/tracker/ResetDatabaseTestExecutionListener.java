package com.github.smaugfm.power.tracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

public class ResetDatabaseTestExecutionListener extends AbstractTestExecutionListener {

  @Autowired
  private DataSource dataSource;

  public final int getOrder() {
    return 2001;
  }

  private boolean alreadyCleared = false;

  @Override
  public void beforeTestClass(TestContext testContext) {
    testContext.getApplicationContext()
        .getAutowireCapableBeanFactory()
        .autowireBean(this);
  }

  @Override
  public void prepareTestInstance(TestContext testContext) throws Exception {

    if (!alreadyCleared) {
      cleanupDatabase();
      alreadyCleared = true;
    }
  }

  @Override
  public void beforeTestMethod(TestContext testContext) throws Exception {
    cleanupDatabase();
  }

  private void cleanupDatabase() throws SQLException {
    Connection c = dataSource.getConnection();
    Statement s = c.createStatement();

    // Disable FK
    s.execute("SET REFERENTIAL_INTEGRITY FALSE");

    // Find all tables and truncate them
    Set<String> tables = new HashSet<>();
    ResultSet rs = s.executeQuery(
        "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES  where TABLE_SCHEMA='PUBLIC'");
    while (rs.next()) {
      tables.add(rs.getString(1));
    }
    rs.close();
    for (String table : tables) {
      s.executeUpdate("TRUNCATE TABLE " + table);
    }

    // Idem for sequences
    Set<String> sequences = new HashSet<>();
    rs = s.executeQuery(
        "SELECT SEQUENCE_NAME FROM INFORMATION_SCHEMA.SEQUENCES WHERE SEQUENCE_SCHEMA='PUBLIC'");
    while (rs.next()) {
      sequences.add(rs.getString(1));
    }
    rs.close();
    for (String seq : sequences) {
      s.executeUpdate("ALTER SEQUENCE " + seq + " RESTART WITH 1");
    }

    // Enable FK
    s.execute("SET REFERENTIAL_INTEGRITY TRUE");
    s.close();
    c.close();
  }
}
