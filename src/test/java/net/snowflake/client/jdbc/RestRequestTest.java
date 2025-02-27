/*
 * Copyright (c) 2012-2022 Snowflake Computing Inc. All rights reserved.
 */
package net.snowflake.client.jdbc;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.snowflake.client.core.HttpUtil;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** RestRequest unit tests. */
public class RestRequestTest {

  static final int DEFAULT_CONNECTION_TIMEOUT = 60000;
  static final int DEFAULT_HTTP_CLIENT_SOCKET_TIMEOUT = 300000; // ms

  private CloseableHttpResponse retryResponse() {
    StatusLine retryStatusLine = mock(StatusLine.class);
    when(retryStatusLine.getStatusCode()).thenReturn(503);

    CloseableHttpResponse retryResponse = mock(CloseableHttpResponse.class);
    when(retryResponse.getStatusLine()).thenReturn(retryStatusLine);

    return retryResponse;
  }

  private CloseableHttpResponse successResponse() {
    StatusLine successStatusLine = mock(StatusLine.class);
    when(successStatusLine.getStatusCode()).thenReturn(200);

    CloseableHttpResponse successResponse = mock(CloseableHttpResponse.class);
    when(successResponse.getStatusLine()).thenReturn(successStatusLine);

    return successResponse;
  }

  private void execute(
      CloseableHttpClient client,
      String uri,
      int retryTimeout,
      int authTimeout,
      int socketTimeout,
      boolean includeRetryParameters)
      throws IOException, SnowflakeSQLException {

    RequestConfig.Builder builder =
        RequestConfig.custom()
            .setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT)
            .setConnectionRequestTimeout(DEFAULT_CONNECTION_TIMEOUT)
            .setSocketTimeout(DEFAULT_HTTP_CLIENT_SOCKET_TIMEOUT);
    RequestConfig defaultRequestConfig = builder.build();
    HttpUtil util = new HttpUtil();
    util.setRequestConfig(defaultRequestConfig);

    RestRequest.execute(
        client,
        new HttpGet(uri),
        retryTimeout, // retry timeout
        authTimeout,
        socketTimeout,
        0,
        0, // inject socket timeout
        new AtomicBoolean(false), // canceling
        false, // without cookie
        includeRetryParameters,
        true,
        true);
  }

  @Test
  public void testRetryParamsInRequest() throws IOException, SnowflakeSQLException {
    CloseableHttpClient client = mock(CloseableHttpClient.class);
    when(client.execute(any(HttpUriRequest.class)))
        .thenAnswer(
            new Answer<CloseableHttpResponse>() {
              int callCount = 0;

              @Override
              public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                HttpUriRequest arg = (HttpUriRequest) invocation.getArguments()[0];
                String params = arg.getURI().getQuery();

                if (callCount == 0) {
                  assertFalse(params.contains("retryCount="));
                  assertFalse(params.contains("clientStartTime="));
                  assertTrue(params.contains("request_guid="));
                } else {
                  assertTrue(params.contains("retryCount=" + callCount));
                  assertTrue(params.contains("clientStartTime="));
                  assertTrue(params.contains("request_guid="));
                }

                callCount += 1;
                if (callCount >= 3) {
                  return successResponse();
                } else {
                  return retryResponse();
                }
              }
            });

    execute(client, "fakeurl.com/?requestId=abcd-1234", 0, 0, 0, true);
  }

  @Test
  public void testRetryNoParamsInRequest() throws IOException, SnowflakeSQLException {
    CloseableHttpClient client = mock(CloseableHttpClient.class);
    when(client.execute(any(HttpUriRequest.class)))
        .thenAnswer(
            new Answer<CloseableHttpResponse>() {
              int callCount = 0;

              @Override
              public CloseableHttpResponse answer(InvocationOnMock invocation) throws Throwable {
                HttpUriRequest arg = (HttpUriRequest) invocation.getArguments()[0];
                String params = arg.getURI().getQuery();

                assertFalse(params.contains("retryCount="));
                assertFalse(params.contains("clientStartTime="));
                assertTrue(params.contains("request_guid="));

                callCount += 1;
                if (callCount >= 3) {
                  return successResponse();
                } else {
                  return retryResponse();
                }
              }
            });

    execute(client, "fakeurl.com/?requestId=abcd-1234", 0, 0, 0, false);
  }

  private CloseableHttpResponse anyStatusCodeResponse(int statusCode) {
    StatusLine successStatusLine = mock(StatusLine.class);
    when(successStatusLine.getStatusCode()).thenReturn(statusCode);

    CloseableHttpResponse response = mock(CloseableHttpResponse.class);
    when(response.getStatusLine()).thenReturn(successStatusLine);

    return response;
  }

  @Test
  public void testIsNonRetryableHTTPCode() throws Exception {
    class TestCase {
      TestCase(int statusCode, boolean retryHTTP403, boolean result) {
        this.statusCode = statusCode;
        this.retryHTTP403 = retryHTTP403;
        this.result = result; // expected result of calling isNonRetryableHTTPCode()
      }

      public int statusCode;
      public boolean retryHTTP403;
      public boolean result;
    }
    List<TestCase> testCases = new ArrayList<>();
    // no retry on HTTP 403 option

    testCases.add(new TestCase(100, false, true));
    testCases.add(new TestCase(101, false, true));
    testCases.add(new TestCase(103, false, true));
    testCases.add(new TestCase(200, false, true));
    testCases.add(new TestCase(201, false, true));
    testCases.add(new TestCase(202, false, true));
    testCases.add(new TestCase(203, false, true));
    testCases.add(new TestCase(204, false, true));
    testCases.add(new TestCase(205, false, true));
    testCases.add(new TestCase(206, false, true));
    testCases.add(new TestCase(300, false, true));
    testCases.add(new TestCase(301, false, true));
    testCases.add(new TestCase(302, false, true));
    testCases.add(new TestCase(303, false, true));
    testCases.add(new TestCase(304, false, true));
    testCases.add(new TestCase(307, false, true));
    testCases.add(new TestCase(308, false, true));
    testCases.add(new TestCase(400, false, true));
    testCases.add(new TestCase(401, false, true));
    testCases.add(new TestCase(403, false, true)); // no retry on HTTP 403
    testCases.add(new TestCase(404, false, true));
    testCases.add(new TestCase(405, false, true));
    testCases.add(new TestCase(406, false, true));
    testCases.add(new TestCase(407, false, true));
    testCases.add(new TestCase(408, false, false)); // do retry on HTTP 408
    testCases.add(new TestCase(410, false, true));
    testCases.add(new TestCase(411, false, true));
    testCases.add(new TestCase(412, false, true));
    testCases.add(new TestCase(413, false, true));
    testCases.add(new TestCase(414, false, true));
    testCases.add(new TestCase(415, false, true));
    testCases.add(new TestCase(416, false, true));
    testCases.add(new TestCase(417, false, true));
    testCases.add(new TestCase(418, false, true));
    testCases.add(new TestCase(425, false, true));
    testCases.add(new TestCase(426, false, true));
    testCases.add(new TestCase(428, false, true));
    testCases.add(new TestCase(429, false, false)); // do retry on HTTP 429
    testCases.add(new TestCase(431, false, true));
    testCases.add(new TestCase(451, false, true));
    testCases.add(new TestCase(500, false, false));
    testCases.add(new TestCase(501, false, false));
    testCases.add(new TestCase(502, false, false));
    testCases.add(new TestCase(503, false, false));
    testCases.add(new TestCase(504, false, false));
    testCases.add(new TestCase(505, false, false));
    testCases.add(new TestCase(506, false, false));
    testCases.add(new TestCase(507, false, false));
    testCases.add(new TestCase(508, false, false));
    testCases.add(new TestCase(509, false, false));
    testCases.add(new TestCase(510, false, false));
    testCases.add(new TestCase(511, false, false));
    // do retry on HTTP 403 option
    testCases.add(new TestCase(100, true, true));
    testCases.add(new TestCase(101, true, true));
    testCases.add(new TestCase(103, true, true));
    testCases.add(new TestCase(200, true, true));
    testCases.add(new TestCase(201, true, true));
    testCases.add(new TestCase(202, true, true));
    testCases.add(new TestCase(203, true, true));
    testCases.add(new TestCase(204, true, true));
    testCases.add(new TestCase(205, true, true));
    testCases.add(new TestCase(206, true, true));
    testCases.add(new TestCase(300, true, true));
    testCases.add(new TestCase(301, true, true));
    testCases.add(new TestCase(302, true, true));
    testCases.add(new TestCase(303, true, true));
    testCases.add(new TestCase(304, true, true));
    testCases.add(new TestCase(307, true, true));
    testCases.add(new TestCase(308, true, true));
    testCases.add(new TestCase(400, true, true));
    testCases.add(new TestCase(401, true, true));
    testCases.add(new TestCase(403, true, false)); // do retry on HTTP 403
    testCases.add(new TestCase(404, true, true));
    testCases.add(new TestCase(405, true, true));
    testCases.add(new TestCase(406, true, true));
    testCases.add(new TestCase(407, true, true));
    testCases.add(new TestCase(408, true, false)); // do retry on HTTP 408
    testCases.add(new TestCase(410, true, true));
    testCases.add(new TestCase(411, true, true));
    testCases.add(new TestCase(412, true, true));
    testCases.add(new TestCase(413, true, true));
    testCases.add(new TestCase(414, true, true));
    testCases.add(new TestCase(415, true, true));
    testCases.add(new TestCase(416, true, true));
    testCases.add(new TestCase(417, true, true));
    testCases.add(new TestCase(418, true, true));
    testCases.add(new TestCase(425, true, true));
    testCases.add(new TestCase(426, true, true));
    testCases.add(new TestCase(428, true, true));
    testCases.add(new TestCase(429, true, false)); // do retry on HTTP 429
    testCases.add(new TestCase(431, true, true));
    testCases.add(new TestCase(451, true, true));
    testCases.add(new TestCase(500, true, false));
    testCases.add(new TestCase(501, true, false));
    testCases.add(new TestCase(502, true, false));
    testCases.add(new TestCase(503, true, false));
    testCases.add(new TestCase(504, true, false));
    testCases.add(new TestCase(505, true, false));
    testCases.add(new TestCase(506, true, false));
    testCases.add(new TestCase(507, true, false));
    testCases.add(new TestCase(508, true, false));
    testCases.add(new TestCase(509, true, false));
    testCases.add(new TestCase(510, true, false));
    testCases.add(new TestCase(511, true, false));

    for (TestCase t : testCases) {
      if (t.result) {
        assertTrue(
            String.format(
                "Result must be true but false: HTTP Code: %d, RetryHTTP403: %s",
                t.statusCode, t.retryHTTP403),
            RestRequest.isNonRetryableHTTPCode(
                anyStatusCodeResponse(t.statusCode), t.retryHTTP403));
      } else {
        assertFalse(
            String.format(
                "Result must be false but true: HTTP Code: %d, RetryHTTP403: %s",
                t.statusCode, t.retryHTTP403),
            RestRequest.isNonRetryableHTTPCode(
                anyStatusCodeResponse(t.statusCode), t.retryHTTP403));
      }
    }
  }

  @Test
  public void testExceptionAuthBasedTimeout() throws IOException {
    CloseableHttpClient client = mock(CloseableHttpClient.class);
    when(client.execute(any(HttpUriRequest.class)))
        .thenAnswer((Answer<CloseableHttpResponse>) invocation -> retryResponse());

    try {
      execute(client, "login-request.com/?requestId=abcd-1234", 2, 1, 30000, true);
    } catch (SnowflakeSQLException ex) {
      assertThat(
          ex.getErrorCode(), equalTo(ErrorCode.AUTHENTICATOR_REQUEST_TIMEOUT.getMessageCode()));
    }
  }
}
