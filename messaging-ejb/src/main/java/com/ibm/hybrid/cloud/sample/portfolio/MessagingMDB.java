/*
       Copyright 2017 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//EJB 3.2
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

//JMS 2.0
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

//JSON-P 1.0
import javax.json.Json;
import javax.json.JsonObject;

/**
 * Message-Driven Bean implementation class for: MessagingMDB
 */
@MessageDriven(name = "MessagingMDB", mappedName = "jms/Portfolio/NotificationQueue")
public class MessagingMDB implements MessageListener {
	private static Logger logger = Logger.getLogger(MessagingMDB.class.getName());

	private static final String NOTIFICATION_SERVICE = "http://notification-service:9080/notification";


	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message message) {
		if (message instanceof TextMessage) try {
			TextMessage text = (TextMessage) message;
			String json = text.getText();

			logger.fine("Sending "+json+" to "+NOTIFICATION_SERVICE);
			JsonObject output = invokeREST("POST", NOTIFICATION_SERVICE, json);
			logger.info("Received the following response from the Notification microservice: "+output);
		} catch (Throwable t) {
			logger.warning("An error occurred processing a JMS message from the queue");
			logException(t);
		} else {
			logger.warning("onMessage received a non-TextMessage!");
		}
	}

	private static JsonObject invokeREST(String verb, String uri, String input) throws IOException {
		logger.fine("Preparing to invoke "+verb+" on "+uri);
		URL url = new URL(uri);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(verb);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		if (input != null) {
			logger.fine("Writing JSON to body of REST call:"+input);
			OutputStream body = conn.getOutputStream();
			body.write(input.getBytes());
			body.flush();
			body.close();
			logger.fine("Successfully wrote JSON");
		}

		logger.fine("Driving call to REST API");
		InputStream stream = conn.getInputStream();

		logger.fine("Parsing results as JSON");
//		JSONObject json = JSONObject.parse(stream); //JSON4J
		JsonObject json = Json.createReader(stream).readObject();

		stream.close();

		logger.fine("Returning JSON to caller of REST API");
		return json;
	}

	private static void logException(Throwable t) {
		logger.warning(t.getClass().getName()+": "+t.getMessage());

		//only log the stack trace if the level has been set to at least FINE
		if (logger.isLoggable(Level.FINE)) {
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			logger.fine(writer.toString());
		}
	}
}
