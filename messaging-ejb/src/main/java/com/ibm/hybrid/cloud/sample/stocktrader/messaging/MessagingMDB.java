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

package com.ibm.hybrid.cloud.sample.stocktrader.messaging;

import com.ibm.hybrid.cloud.sample.stocktrader.messaging.NotificationClient;
import com.ibm.hybrid.cloud.sample.stocktrader.messaging.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.messaging.json.NotificationResult;

//Standard I/O classes
import java.io.PrintWriter;
import java.io.StringWriter;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//CDI 1.2
import javax.inject.Inject;

//EJB 3.2
import javax.ejb.MessageDriven;

//JMS 2.0
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

//JSON-B (JSR 367).  This largely replaces the need for JSON-P
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

//mpRestClient 1.0
import org.eclipse.microprofile.rest.client.inject.RestClient;

//mpJWT 1.0
import org.eclipse.microprofile.jwt.JsonWebToken;


/**
 * Message-Driven Bean implementation class for: MessagingMDB
 */
@MessageDriven(name = "MessagingMDB", mappedName = "jms/StockTrader/NotificationQueue")
public class MessagingMDB implements MessageListener {
	private static Logger logger = Logger.getLogger(MessagingMDB.class.getName());

	private @Inject @RestClient NotificationClient notificationClient;

	private @Inject JsonWebToken jwt;


	/**
	 * @see MessageListener#onMessage(Message)
	 */
	public void onMessage(Message message) {
		if (message instanceof TextMessage) try {
			TextMessage text = (TextMessage) message;
			String payload = text.getText();
			logger.info("Sending "+payload+" to the Notification microservice");

			Jsonb jsonb = JsonbBuilder.create();
			LoyaltyChange loyaltyChange = jsonb.fromJson(payload, LoyaltyChange.class);

			NotificationResult result = notificationClient.notify("Bearer "+jwt.getRawToken(), loyaltyChange);
			logger.info("Received the following response from the Notification microservice: "+result);
		} catch (Throwable t) {
			logger.warning("An error occurred processing a JMS message from the queue");
			logException(t);
		} else {
			logger.warning("onMessage received a non-TextMessage!");
		}
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
