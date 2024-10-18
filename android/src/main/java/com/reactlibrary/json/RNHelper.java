package com.reactlibrary.json;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

public class RNHelper {
    public static WritableMap jsonToReact(String mJson) {
        WritableMap mWrite = Arguments.createMap();

        // when mJson is null or empty, we need to set it to "{}" to avoid crash with error JSON Parse error: Unrecognized token 'undefined'
        String mBody = mJson;
        if (mJson == null || mJson.isEmpty()) {
            mBody = "{}";
        }

        mWrite.putString("body", mBody);
        mWrite.putString("message", "Fetch data success");

        return mWrite;
    }
}
