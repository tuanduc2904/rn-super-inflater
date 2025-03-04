package com.itghelper;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.InflaterSource;
import okio.Okio;
import android.util.Log;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import java.util.zip.Inflater;

public class AutoDecompressionInterceptor implements Interceptor {
    private static final String TAG = "OkHttpInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response;

        try {
            // Thực hiện yêu cầu HTTP
            response = chain.proceed(request);
        } catch (SocketTimeoutException e) {
            Log.e(TAG, "Request timed out: " + e.getMessage());
            throw new IOException("Server took too long to respond.");
        } catch (ConnectException e) {
            Log.e(TAG, "Failed to connect to server: " + e.getMessage());
            throw new IOException("Unable to connect to server. Please check your connection.");
        } catch (IOException e) {
            Log.e(TAG, "Network error: " + e.getMessage());
            throw e;
        }

        String encoding = response.header("Content-Encoding");
        if ("deflate".equalsIgnoreCase(encoding)) {
            return decompressResponse(response);
        }

        return response;
    }

    private Response decompressResponse(Response response) throws IOException {
        ResponseBody responseBody = response.body();
        if (responseBody == null) {
            return response;
        }

        Inflater inflater = new Inflater(true);
        try (BufferedSource inflaterSource = Okio.buffer(new InflaterSource(Okio.source(responseBody.byteStream()), inflater))) {
            Buffer buffer = new Buffer();
            while (inflaterSource.read(buffer, 8192) != -1) {
                // Đọc dữ liệu giải nén vào buffer
            }

            ResponseBody decompressedBody = ResponseBody.create(responseBody.contentType(), buffer.readByteArray());
            return response.newBuilder()
                    .body(decompressedBody)
                    .removeHeader("Content-Encoding")
                    .build();
        } catch (IOException e) {
            Log.e(TAG, "Decompression failed: " + e.getMessage());
            throw e;
        } finally {
            inflater.end();
        }
    }
}
