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
package org.openhab.binding.neato.internal.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.eclipse.jdt.annotation.NonNull;
import org.openhab.binding.neato.internal.VendorVorwerk;
import org.openhab.binding.neato.internal.classes.NeatoAccountInformation;
import org.openhab.binding.neato.internal.classes.Robot;
import org.openhab.binding.neato.internal.config.NeatoAccountConfig;
import org.openhab.core.io.net.http.HttpUtil;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * Bridge handler to manage Neato Cloud Account
 *
 * @author Jeff Lauterbach - Initial Contribution
 * @author Pavion - Vendor added
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
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.ONLINE);
    }

    private List<Robot> sendGetRobots(String accessToken) {
        Properties headers = new Properties();
        headers.setProperty("Accept", "application/vnd.neato.nucleo.v1");
        headers.setProperty("Authorization", "Auth0Bearer " + accessToken);

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
            return new ArrayList<>();
        }
    }

    public @NonNull List<Robot> getRobotsFromNeato() {
        logger.debug("Attempting to find robots tied to account");
        String accessToken = accountConfig.getToken();

        if (accessToken != null) {
            if (!accessToken.equals("")) {
                return sendGetRobots(accessToken);
            }
        }

        return new ArrayList<>();
    }
}
