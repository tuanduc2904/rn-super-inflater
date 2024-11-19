package com.itghelper;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;
import android.util.Log;
import org.apache.commons.io.output.ByteArrayOutputStream;
import java.io.BufferedInputStream;



public class AutoDecompressionInterceptor implements Interceptor {
    private static final String TAG = "OkHttpInterceptor";

    @Override
    public Response intercept(Chain chain) throws IOException {
        // Tạo request mới với Header yêu cầu gzip, deflate
        Request request = chain.request().newBuilder().build();

        Response response = chain.proceed(request);
        String encoding = response.header("Content-Encoding");
        // Kiểm tra và giải nén theo encoding
        Log.d(TAG, "bodyRaw: "+ response.body().toString());
        if ("deflate".equalsIgnoreCase(encoding)) {
            Inflater deCompressor = new Inflater(true);
            return decompressResponse(response, new InflaterInputStream(response.body().byteStream(), deCompressor));
        }

        return response; // Trả về phản hồi nếu không cần giải nén
    }

    // Phương thức giải nén dữ liệu từ InputStream
    private Response decompressResponse(Response response, InputStream decompressedStream) throws IOException {
        String validJson;
        byte[] bodyString = readAllBytes(decompressedStream);
        Log.d(TAG, "bodyFinal"+ bodyString.toString());

        ResponseBody decompressedBody = ResponseBody.create(response.body().contentType(), bodyString);
        return response.newBuilder().body(decompressedBody).build();
    }

    // Đọc toàn bộ nội dung từ InputStream
    private byte[] readAllBytes(InputStream inputStream) throws IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int length;
        BufferedInputStream b = new BufferedInputStream(inputStream);

        while ((length = b.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        return out.toByteArray();
    }
}
