/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 * Copyright IBM Corp. 2024, 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.quartz.impl.jdbcjobstore;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;


import org.junit.jupiter.api.Test;

/**
 *
 * @author cdennis
 */
class UpdateLockRowSemaphoreTest {
  
  private static final PreparedStatement GOOD_STATEMENT = mock(PreparedStatement.class);
  private static final PreparedStatement FAIL_STATEMENT = mock(PreparedStatement.class);
  private static final PreparedStatement BAD_STATEMENT = mock(PreparedStatement.class);

  static {
    try {
      when(GOOD_STATEMENT.executeUpdate()).thenReturn(1);
      when(FAIL_STATEMENT.executeUpdate()).thenReturn(0);
      when(BAD_STATEMENT.executeUpdate()).thenThrow(SQLException.class);
    } catch (SQLException e) {
      throw new AssertionError(e);
    }
  }
  
  @Test
  void testSingleSuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }
  
  @Test
  void testSingleFailureFollowedBySuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(BAD_STATEMENT)
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }

  @Test
  void testDoubleFailureFollowedBySuccessUsingUpdate() throws LockException, SQLException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(BAD_STATEMENT, BAD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    try {
      semaphore.obtainLock(mockConnection, "test");
      fail();
    } catch (LockException e) {
      //expected
    }
  }
  
  @Test
  void testFallThroughToInsert() throws SQLException, LockException {
    UpdateLockRowSemaphore semaphore = new UpdateLockRowSemaphore();
    semaphore.setSchedName("test");

    Connection mockConnection = mock(Connection.class);
    when(mockConnection.prepareStatement(startsWith("UPDATE")))
            .thenReturn(FAIL_STATEMENT)
            .thenThrow(AssertionError.class);
    when(mockConnection.prepareStatement(startsWith("INSERT")))
            .thenReturn(GOOD_STATEMENT)
            .thenThrow(AssertionError.class);
    
    assertTrue(semaphore.obtainLock(mockConnection, "test"));
  }
}
