package com.amazonaws.xray.proxies.apache.http;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.Subsegment;

/**
 * Wraps an instance of {@code org.apache.http.client.ResponseHandler} and adds response information to the current subsegment.
 *
 */
public class TracedResponseHandler<T> implements ResponseHandler<T> {

    private final ResponseHandler<T> wrappedHandler;

    public TracedResponseHandler(ResponseHandler<T> wrappedHandler) {
        this.wrappedHandler = wrappedHandler;
    }

    public static void addResponseInformation(Subsegment subsegment, HttpResponse response) {
        if (null == subsegment) {
            return;
        }

        Map<String, Object> responseInformation = new HashMap<>();

        int responseCode = response.getStatusLine().getStatusCode();
        switch (responseCode/100) {
            case 4:
                subsegment.setError(true);
                if (429 == responseCode) {
                    subsegment.setThrottle(true);
                }
                break;
            case 5:
                subsegment.setFault(true);
                break;
        }
        responseInformation.put("status", responseCode);

        if (null != response.getEntity()) {
            responseInformation.put("content_length", response.getEntity().getContentLength());
        }

        subsegment.putHttp("response", responseInformation);
    }

    @Override
    public T handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        T handled = wrappedHandler.handleResponse(response);
        Subsegment currentSubsegment = AWSXRay.getCurrentSubsegment();
        if (null != currentSubsegment) {
            TracedResponseHandler.addResponseInformation(currentSubsegment, response);
        }
        return handled;
    }

}
