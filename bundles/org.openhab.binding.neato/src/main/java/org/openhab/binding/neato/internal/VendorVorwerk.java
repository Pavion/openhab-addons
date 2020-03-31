/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.neato.internal;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Properties;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Florian Dietrich - Initial contribution
 * @author Pavion - 2.5.0 adaptation
 *
 */
public class VendorVorwerk {

    public static final String BEEHIVE_URL = "https://vorwerk-beehive-production.herokuapp.com";
    public static final String NUCLEO_URL = "https://nucleo.ksecosys.com";
    public static final String VENDOR_NAME = "vorwerk";

    public static String executeRequest(String httpMethod, String url, Properties httpHeaders, String content,
            String contentType, int timeout) throws IOException {
        URL requestUrl = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) requestUrl.openConnection();
        applyNucleoSslConfiguration(connection);
        connection.setRequestMethod(httpMethod);
        for (String propName : httpHeaders.stringPropertyNames()) {
            connection.addRequestProperty(propName, httpHeaders.getProperty(propName));
        }
        connection.setUseCaches(false);
        connection.setDoOutput(true);
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        if (content != null) {
            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(content);
            wr.flush();
            wr.close();
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        StringBuilder sb = new StringBuilder();
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            sb.append(inputLine);
        }

        in.close();
        connection.disconnect();

        return sb.toString();
    }

    /**
     * Trust the self signed certificate.
     *
     * @param connection
     */
    private static void applyNucleoSslConfiguration(HttpsURLConnection connection) {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            } };

            SSLContext sslctx = SSLContext.getInstance("SSL");
            // sslctx.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            sslctx.init(null, trustAllCerts, new SecureRandom());
            connection.setSSLSocketFactory(sslctx.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            connection.setHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
        } catch (KeyManagementException e) {
        }
    }

}
