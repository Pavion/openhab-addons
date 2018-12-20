/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.neato.internal.handler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.io.net.http.HttpUtil;
import org.openhab.binding.neato.internal.VendorVorwerk;
import org.openhab.binding.neato.internal.classes.BeehiveAuthentication;
import org.openhab.binding.neato.internal.classes.NeatoAccountInformation;
import org.openhab.binding.neato.internal.classes.Robot;
import org.openhab.binding.neato.internal.config.NeatoAccountConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Bridge handler to manage Neato Cloud Account
 *
 * @author Jeff Lauterbach - Initial Contribution
 * @author Pavion - Vendor added
 *
 */
public class NeatoAccountHandler extends BaseBridgeHandler {

    private final Logger logger = LoggerFactory.getLogger(NeatoAccountHandler.class);
    private NeatoAccountConfig accountConfig;

    public NeatoAccountHandler(Bridge bridge) {
        super(bridge);
        accountConfig = getConfigAs(NeatoAccountConfig.class);
    }

    public String getVendor() {
        return accountConfig.getVendor().toLowerCase().trim();
    }

    @Override
    public void handleCommand(@NonNull ChannelUID channelUID, Command command) {
    }

    private List<Robot> sendGetRobots(String accessToken) {
        Properties headers = new Properties();
        headers.setProperty("Accept", "application/vnd.neato.nucleo.v1");

        headers.setProperty("Authorization", "Token token=" + accessToken);

        try {
            String resultString = "";
            if (getVendor().equals(VendorVorwerk.VENDOR_NAME)) {
                resultString = VendorVorwerk.executeRequest("GET", VendorVorwerk.BEEHIVE_URL + "/dashboard", headers,
                        null, "application/json", 20000);
            } else {
                resultString = HttpUtil.executeUrl("GET", "https://beehive.neatocloud.com/dashboard", headers, null,
                        "application/json", 20000);
            }

            Gson gson = new Gson();
            NeatoAccountInformation accountInformation = gson.fromJson(resultString, NeatoAccountInformation.class);

            logger.debug("Result from WS call to get Robots: {}", resultString);

            return accountInformation.getRobots();
        } catch (IOException e) {
            logger.debug("Error attempting to find robots registered to account", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error attempting to find robots registered to account");
            return new ArrayList<Robot>();
        }
    }

    public @NonNull List<Robot> getRobotsFromNeato() {
        logger.debug("Attempting to find robots tied to account");
        String accessToken = authenticate(accountConfig.getEmail(), accountConfig.getPassword());

        if (accessToken != null) {
            return sendGetRobots(accessToken);
        }

        return new ArrayList<Robot>();
    }

    private String authenticate(String username, String password) {
        Gson gson = new Gson();

        AuthRequest req = new AuthRequest(username, password, "ios",
                new BigInteger(130, new SecureRandom()).toString(64));

        String authenticationResponse = "";
        try {
            if (getVendor().equals(VendorVorwerk.VENDOR_NAME)) {
                authenticationResponse = sendAuthRequestToVorwerk(req);
            } else {
                authenticationResponse = sendAuthRequestToNeato(gson.toJson(req));
            }
        } catch (IOException e) {
            logger.debug("Error when sending Authentication request to Neato.", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Error when sending Authentication request to Neato.");
        }

        BeehiveAuthentication authenticationObject = gson.fromJson(authenticationResponse, BeehiveAuthentication.class);
        return authenticationObject.getAccessToken();
    }

    private String sendAuthRequestToNeato(String data) throws IOException {

        Properties headers = new Properties();
        headers.setProperty("Accept", "application/vnd.neato.nucleo.v1");

        InputStream stream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        String resultString = HttpUtil.executeUrl("POST", "https://beehive.neatocloud.com/sessions", headers, stream,
                "application/json", 20000);

        logger.debug("Authentication Response from Neato: {}", resultString);

        return resultString;
    }

    private String sendAuthRequestToVorwerk(AuthRequest data) throws IOException {

        String payload = "email=" + data.getEmail() + "&password=" + data.getPassword() + "&os=" + data.getOs()
                + "&token=" + data.getToken();

        Properties headers = new Properties();
        headers.setProperty("Accept", "application/vnd.neato.nucleo.v1");

        String resultString = VendorVorwerk.executeRequest("POST", VendorVorwerk.BEEHIVE_URL + "/sessions", headers,
                payload, "application/json", 20000);

        logger.debug("Authentication Response from Vorwerk: {}", resultString);

        return resultString;
    }

    private static class AuthRequest {
        private String email;
        private String password;
        private String os;
        private String token;

        public AuthRequest(String email, String password, String os, String token) {
            this.email = email;
            this.password = password;
            this.os = os;
            this.token = token;
        }

        public String getEmail() {
            return email;
        }

        public String getPassword() {
            return password;
        }

        public String getOs() {
            return os;
        }

        public String getToken() {
            return token;
        }
    }

}
