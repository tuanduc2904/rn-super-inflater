package com.itg.itghelper.helper;

import com.facebook.react.modules.network.OkHttpClientFactory;
import com.facebook.react.modules.network.ReactCookieJarContainer;
import okhttp3.OkHttpClient;

public class CustomOkHttpClientFactory implements OkHttpClientFactory {
    @Override
    public OkHttpClient createNewNetworkModuleClient() {
        return new OkHttpClient.Builder()
                .cookieJar(new ReactCookieJarContainer())
                .addInterceptor(new AutoDecompressionInterceptor()) // Giải nén gzip, deflate
                .build();
    }
}
