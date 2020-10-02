/*
 * Copyright (c) 2012-2020 Snowflake Computing Inc. All right reserved.
 */
package net.snowflake.client.jdbc;

import net.snowflake.client.category.TestCategoryArrow;
import org.junit.experimental.categories.Category;

/**
 * PreparedStatement integration tests for the latest JDBC driver. This doesn't work for the oldest
 * supported driver. Drop this file when PrepareStatement2IT is dropped.
 */
@Category(TestCategoryArrow.class)
public class PreparedStatementArrow2LatestIT extends PreparedStatement2LatestIT {
  public PreparedStatementArrow2LatestIT() {
    super("arrow");
  }
}
