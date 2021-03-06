package com.froxynetwork.coremanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.froxynetwork.coremanager.command.CommandManager;
import com.froxynetwork.coremanager.scheduler.Scheduler;
import com.froxynetwork.coremanager.server.ServerManager;
import com.froxynetwork.coremanager.server.config.ServerConfigManager;
import com.froxynetwork.coremanager.websocket.WebSocketManager;
import com.froxynetwork.froxynetwork.network.NetworkManager;

import lombok.Getter;

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
public class Main {

	private final Logger LOG = LoggerFactory.getLogger(getClass());

	private static Main INSTANCE;

	private Properties p;

	@Getter
	private NetworkManager networkManager;
	@Getter
	private ServerConfigManager serverConfigManager;
	@Getter
	private CommandManager commandManager;
	@Getter
	private ServerManager serverManager;
	@Getter
	private WebSocketManager webSocketManager;

	public Main(String[] args) {
		INSTANCE = this;
		try {
			LOG.info("CoreManager initialization");
			if (args == null || args.length != 1) {
				LOG.error("Invalid argument number, please enter correct arguments ! (<propertiesFile>)");
				System.exit(1);
			}
			String properties = args[0];
			File fProperties = new File(properties);
			if (fProperties == null || !fProperties.exists()) {
				LOG.error("Properties file not found ({})", properties);
				System.exit(1);
			}
			if (!fProperties.isFile() || !fProperties.canRead()) {
				LOG.error("Properties file is not a file or we don't have permission to read the properties file ({})",
						properties);
				System.exit(1);
			}
			p = new Properties();
			try {
				p.load(new FileInputStream(fProperties));
			} catch (IOException ex) {
				LOG.error("Error while reading properties file ({})", properties);
				LOG.error("", ex);
				System.exit(1);
			}

			initializeNetwork();
			initializeServerConfig(() -> {
				// Initialize Servers once ServerConfig is initialized
				initializeServer();
				initializeWebSocket();
				initializeCommands();
				LOG.info("All initialized");
			});
		} catch (Exception ex) {
			LOG.error("ERROR: ", ex);
			System.exit(1);
		}
	}

	private void initializeNetwork() {
		LOG.info("Initializing NetworkManager");
		String url = p.getProperty("url");
		String clientId = p.getProperty("client_id");
		String clientSecret = p.getProperty("client_secret");
		LOG.info("url = {}, client_id = {}, client_secret = {}", url, clientId,
				clientSecret == null ? "null" : "<hidden>");
		try {
			networkManager = new NetworkManager(url, clientId, clientSecret);
		} catch (Exception ex) {
			LOG.error("An error has occured while initializing NetworkManager: ", ex);
			System.exit(1);
		}
		LOG.info("NetworkManager initialized");
	}

	private void initializeServerConfig(Runnable then) {
		LOG.info("Initializing ServerConfigManager");
		serverConfigManager = new ServerConfigManager();
		try {
			serverConfigManager.reload(() -> {
				LOG.info("ServerConfigManager initialized");
				then.run();
			});
		} catch (Exception ex) {
			LOG.error("An error has occured while initializing ServerConfigManager: ", ex);
			System.exit(1);
		}
	}

	private void initializeServer() {
		LOG.info("Initializing ServerManager");
		serverManager = new ServerManager();
		serverManager.reload();
		LOG.info("ServerManager initialized");
	}

	private void initializeWebSocket() {
		LOG.info("Initializing WebSocket");
		String websocketUrl = p.getProperty("websocket_url");
		String strWebsocketPort = p.getProperty("websocket_port");
		if (websocketUrl == null || "".equalsIgnoreCase(websocketUrl.trim())) {
			LOG.error("websocketUrl is empty");
			LOG.info("Using default websocketUrl (localhost)");
			websocketUrl = "localhost";
		}
		int websocketPort = 35565;
		try {
			websocketPort = Integer.parseInt(strWebsocketPort);
		} catch (NumberFormatException ex) {
			LOG.error("websocketPort is not a number: {}", strWebsocketPort);
			LOG.info("Using default websocketPort ({})", websocketPort);
		}
		webSocketManager = new WebSocketManager(websocketUrl, websocketPort);
		LOG.info("WebSocket initialized");
	}

	private void initializeCommands() {
		LOG.info("Initializing CommandManager");
		commandManager = new CommandManager();
		LOG.info("CommandManager initialized");
	}

	public void stop() {
		LOG.info("Shutdowning WebSocket");
		webSocketManager.stop();

		LOG.info("Shutdowning NetworkManager");
		networkManager.shutdown();

		LOG.info("Shutdowning Scheduler");
		Scheduler.stop();

		// Exit
		System.exit(0);
	}

	public static Main get() {
		return INSTANCE;
	}

	public static void main(String[] args) {
		Main main = new Main(args);
		// main.getServerManager().openServer("Koth", srv -> {
		// System.out.println("Done: " + srv);
		// }, () -> {
		// System.out.println("ERROR");
		// });
		// main.getServerManager().openServer("Koth", srv -> {
		// System.out.println("Done: " + srv);
		// }, () -> {
		// System.out.println("ERROR");
		// });
	}
}
