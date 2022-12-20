/*
 * Copyright (c) 2018-2020 Snowflake Computing Inc. All right reserved.
 */

package net.snowflake.client.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.io.File;
import java.io.IOException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import net.snowflake.client.TestUtil;
import net.snowflake.client.category.TestCategoryCore;
import net.snowflake.client.jdbc.SnowflakeUtil;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.mockito.MockedStatic;

@Category(TestCategoryCore.class)
public class SFTrustManagerMockitoMockLatestIT {

  @Rule public TemporaryFolder tmpFolder = new TemporaryFolder();

  /*
   * Test SF_OCSP_RESPONSE_CACHE_DIR environment variable changes the
   * location of the OCSP cache directory.
   */
  @Test
  @Ignore("static initialization block of SFTrustManager class doesn't run sometimes")
  public void testUnitOCSPWithCustomCacheDirectory() throws IOException {
    try (MockedStatic<TrustManagerFactory> mockedTrustManagerFactory =
            mockStatic(TrustManagerFactory.class);
        MockedStatic<SnowflakeUtil> mockedSnowflakeUtil = mockStatic(SnowflakeUtil.class)) {

      File cacheFolder = tmpFolder.newFolder();
      mockedSnowflakeUtil
          .when(() -> TestUtil.systemGetEnv("SF_OCSP_RESPONSE_CACHE_DIR"))
          .thenReturn(cacheFolder.getCanonicalPath());

      TrustManagerFactory tested = mock(TrustManagerFactory.class);
      when(tested.getTrustManagers()).thenReturn(new TrustManager[] {});

      mockedTrustManagerFactory
          .when(() -> TrustManagerFactory.getInstance("SunX509"))
          .thenReturn(tested);

      new SFTrustManager(
          new HttpClientSettingsKeyBuilder()
              .setMode(OCSPMode.FAIL_CLOSED)
              .createHttpClientSettingsKey(),
          null); // cache file location

      // The goal is to check if the cache file location is changed to the specified
      // directory, so it doesn't need to do OCSP check in this test.
      assertThat(
          "The cache file doesn't exist.",
          new File(cacheFolder, SFTrustManager.CACHE_FILE_NAME).exists());
    }
  }
}
