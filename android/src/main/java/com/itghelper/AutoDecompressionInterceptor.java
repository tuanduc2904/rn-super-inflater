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
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoDecompressionInterceptor implements Interceptor {
    private static final String TAG = "OkHttpInterceptor";
    private static final String API_LOG_FILE = "API_Log.txt";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    /** --- Ghi log lỗi như cũ --- */
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

    /** --- Ghi log thời gian API --- */
    private void logApiTime(String path, long durationMs) {
        try {
            String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            File logFile = new File(downloadPath, API_LOG_FILE);

            // Nếu sang ngày mới thì xóa file cũ
            if (logFile.exists()) {
                String lastDate = DATE_FORMAT.format(new Date(logFile.lastModified()));
                String today = DATE_FORMAT.format(new Date());
                if (!today.equals(lastDate)) {
                    boolean deleted = logFile.delete();
                    if (deleted) Log.i(TAG, "Old API log deleted for new day");
                }
            }

            // Ghi log mới
            PrintWriter out = new PrintWriter(new FileWriter(logFile, true));
            String timeNow = TIME_FORMAT.format(new Date());
            out.println(timeNow + " | " + durationMs + " ms | " + path);
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "Failed to write API log: " + e.getMessage());
        }
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        long startTime = System.nanoTime();
        Response response;

        try {
            response = chain
                .withConnectTimeout(30_000, TimeUnit.MILLISECONDS)
                .withReadTimeout(30_000, TimeUnit.MILLISECONDS)
                .withWriteTimeout(30_000, TimeUnit.MILLISECONDS)
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
            logToFile("Network error: " + e.getMessage()
                    + " \n URL: " + request.url()
                    + " \n RequestBody: " + (request.body() != null ? request.body().toString() : "null"));
            throw e;
        }

  
        String encoding = response.header("Content-Encoding");
        if ("deflate".equalsIgnoreCase(encoding)) {
            response = decompressResponse(response);
        }
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000L;
        logApiTime(request.url().encodedPath(), durationMs);

        return response;
    }

    /** --- Giải nén response deflate --- */
    private Response decompressResponse(Response response) throws IOException {
        ResponseBody body = response.body();
        if (body == null || response.code() == 204 || response.code() == 304) {
            return response; // không có nội dung thì bỏ qua
        }

        Inflater inflater = new Inflater(true); // nowrap = true → raw deflate
        try {
            // Tạo nguồn đọc giải nén
            BufferedSource decompressedSource = Okio.buffer(
                new InflaterSource(body.source(), inflater)
            );

            // Đọc toàn bộ dữ liệu đã giải nén vào buffer
            Buffer buffer = new Buffer();
            decompressedSource.readAll(buffer);

            // Tạo response body mới từ dữ liệu đã giải nén
            ResponseBody decompressedBody = ResponseBody.create(
                body.contentType(),
                buffer.readByteArray()
            );

            // Xây lại response, bỏ header Content-Encoding
            return response.newBuilder()
                    .removeHeader("Content-Encoding")
                    .body(decompressedBody)
                    .build();
        } finally {
            inflater.end(); // giải phóng tài nguyên native
        }
    }
}
