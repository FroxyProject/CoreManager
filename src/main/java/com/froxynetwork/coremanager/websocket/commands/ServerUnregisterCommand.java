package com.froxynetwork.coremanager.websocket.commands;

import java.util.regex.Pattern;

import org.java_websocket.framing.CloseFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.froxynetwork.coremanager.Main;
import com.froxynetwork.coremanager.server.VPS;
import com.froxynetwork.froxynetwork.network.websocket.IWebSocketCommander;
import com.froxynetwork.froxynetwork.network.websocket.WebSocketServerImpl;

/**
 * MIT License
 *
 * Copyright (c) 2020 FroxyNetwork
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * 
 * @author 0ddlyoko
 */
public class ServerUnregisterCommand implements IWebSocketCommander {
	private final Logger LOG = LoggerFactory.getLogger(getClass());
	private Pattern spacePattern = Pattern.compile(" ");
	private WebSocketServerImpl webSocket;

	public ServerUnregisterCommand(WebSocketServerImpl webSocket) {
		this.webSocket = webSocket;
	}

	@Override
	public String name() {
		return "unregister";
	}

	@Override
	public String description() {
		return "On server close";
	}

	@Override
	public void onReceive(String message) {
		// unregister <id> <type>
		if (!webSocket.isAuthenticated()) {
			// Server not authenticated
			LOG.error("Got \"unregister {}\" from an unauthenticated server", message);
			return;
		}
		String[] split = spacePattern.split(message);
		if (split.length < 2) {
			// Error
			LOG.error("Invalid message: {}", message);
			return;
		}
		String id = split[0];
		String type = split[1];

		VPS vps = Main.get().getWebSocketManager().get(webSocket);
		if (vps == null) {
			// WTF ?
			LOG.error("No VPS found for webSocket ! Closing it");
			webSocket.disconnect(CloseFrame.NORMAL, "No VPS link found");
			return;
		}
		vps.onUnregister(id, type);
	}
}
