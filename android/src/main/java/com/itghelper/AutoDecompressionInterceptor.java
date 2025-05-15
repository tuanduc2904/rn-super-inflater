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
import android.os.Environment;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.ConnectException;
import java.util.zip.Inflater;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AutoDecompressionInterceptor implements Interceptor {
    private static final String TAG = "OkHttpInterceptor";

    private void logToFile(String message) {
        try {
            String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            PrintWriter out = new PrintWriter(new FileWriter(downloadPath + "/API_Error.txt", true));
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            out.println("[" + timeStamp + "] " + message);
            out.close();
        } catch (Exception ex) {
            Log.e(TAG, "Failed to write log to file: " + ex.getMessage());
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // Thiết lập timeout cho request này là 30 giây (30000 ms)
        // Chỉ áp dụng nếu OkHttp version >= 3.12 (có support timeout per-call)
        // Nếu không, cần set ở OkHttpClient builder
        Response response;
        try {
            response = chain
                .withConnectTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .withReadTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .withWriteTimeout(30_000, java.util.concurrent.TimeUnit.MILLISECONDS)
                .proceed(request);
        } catch (SocketTimeoutException e) {
            logToFile("Request timed out: " + e.getMessage()
                    + " \n URL: " + request.url()
                    + " \n RequestBody: " + (request.body() != null ? request.body().toString() : "null"));
            throw new IOException("Server took too long to respond.");
        } catch (ConnectException e) {
            logToFile("Failed to connect to server: " + e.getMessage()
                    + " \n URL: " + request.url()
                    + " \n RequestBody: " + (request.body() != null ? request.body().toString() : "null"));
            throw new IOException("Unable to connect to server. Please check your connection.");
        } catch (IOException e) {
            String responseBodyStr = null;
            try {
                if (e instanceof okhttp3.internal.http2.StreamResetException) {
                    responseBodyStr = "StreamResetException: cannot read response body";
                } else if (chain != null && chain.call() != null && chain.call().isExecuted()) {
                    Response errorResponse = chain.proceed(request);
                    if (errorResponse.body() != null) {
                        ResponseBody peekBody = errorResponse.peekBody(1024);
                        responseBodyStr = peekBody.string();
                    }
                }
            } catch (Exception ex) {
                responseBodyStr = "Cannot read response body: " + ex.getMessage();
            }
            logToFile("Network error: " + e.getMessage()
                    + " \n URL: " + request.url()
                    + " \n RequestBody: " + (request.body() != null ? request.body().toString() : "null")
                    + " \n ResponseBody: " + responseBodyStr);
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

        // Bỏ qua nếu không có body hoặc là response không có nội dung
        if (responseBody == null || response.code() == 204 || response.code() == 304) {
            return response;
        }

        byte[] compressedBytes = responseBody.bytes();
        byte[] decompressedBytes = null;
        boolean success = false;
        IOException lastException = null;

        // Thử giải nén với cả nowrap = true (raw deflate) và false (zlib-wrapped)
        for (boolean nowrap : new boolean[]{true, false}) {
            Inflater inflater = new Inflater(nowrap);
            try (
                BufferedSource inflaterSource = Okio.buffer(
                    new InflaterSource(
                        Okio.source(new java.io.ByteArrayInputStream(compressedBytes)), inflater
                    )
                );
                Buffer buffer = new Buffer()
            ) {
                while (inflaterSource.read(buffer, 8192) != -1) {
                    // đọc hết dữ liệu giải nén vào buffer
                }

                decompressedBytes = buffer.readByteArray();
                success = true;
                break;
            } catch (IOException e) {
                Log.w(TAG, "Decompression failed with nowrap=" + nowrap + ": " + e.getMessage());
                lastException = e;
            } finally {
                inflater.end();
            }
        }

        if (!success) {
            throw new IOException("Failed to decompress response", lastException);
        }

        ResponseBody decompressedBody = ResponseBody.create(
            responseBody.contentType(),
            decompressedBytes
        );

        return response.newBuilder()
                .body(decompressedBody)
                .removeHeader("Content-Encoding")
                .build();
    }
}