package com.adyen.examples.modifications;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * Capture a Payment (HTTP Post)
 * 
 * Authorised (card) payments can be captured to get the money from the shopper. Payments can be automatically captured
 * by our platform. A payment can also be captured by performing an API call. In order to capture an authorised (card)
 * payment you have to send a modification request. This file shows how an authorised payment should be captured by
 * sending a modification request using HTTP Post.
 * 
 * Please note: using our API requires a web service user. Set up your Webservice user:
 * Adyen CA >> Settings >> Users >> ws@Company. >> Generate Password >> Submit
 * 
 * @link /4.Modifications/HttpPost/CapturePayment
 * @author Created by Adyen - Payments Made Easy
 */

@WebServlet("/4.Modifications/HttpPost/CapturePayment")
public class CapturePaymentHttpPost extends HttpServlet {

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/**
		 * HTTP Post settings
		 * - apiUrl: URL of the Adyen API you are using (Test/Live)
		 * - wsUser: your web service user
		 * - wsPassword: your web service user's password
		 */
		String apiUrl = "https://pal-test.adyen.com/pal/adapter/httppost";
		String wsUser = "YourWSUser";
		String wsPassword = "YourWSPassword";

		/**
		 * Create HTTP Client (using Apache HttpComponents library) and set up Basic Authentication
		 */
		CredentialsProvider provider = new BasicCredentialsProvider();
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(wsUser, wsPassword);
		provider.setCredentials(AuthScope.ANY, credentials);

		HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

		/**
		 * Perform capture request by sending in a modification request, containing the following variables:
		 * 
		 * <pre>
		 * - action                 : Payment.capture
		 * - modificationRequest
		 *   - merchantAccount      : The merchant account used to process the payment.
		 *   - modificationAmount
		 *       - currency         : The three character ISO currency code, must match that of the original payment request.
		 *       - value            : The amount to capture (in minor units), must be less than or equal to the authorised amount.
		 *   - originalReference    : The pspReference that was assigned to the authorisation.
		 *   - reference            : Your own reference or description of the modification. (optional)
		 * </pre>
		 */
		List<NameValuePair> postParameters = new ArrayList<NameValuePair>();
		Collections.addAll(postParameters,
			new BasicNameValuePair("action", "Payment.capture"),
			new BasicNameValuePair("modificationRequest.merchantAccount", "YourMerchantAccount"),
			new BasicNameValuePair("modificationRequest.modificationAmount.currency", "EUR"),
			new BasicNameValuePair("modificationRequest.modificationAmount.value", "199"),
			new BasicNameValuePair("modificationRequest.originalReference", "PspReferenceOfTheAuthorisedPayment"),
			new BasicNameValuePair("modificationRequest.reference", "YourReference")
			);

		/**
		 * Send the HTTP Post request with the specified variables.
		 */
		HttpPost httpPost = new HttpPost(apiUrl);
		httpPost.setEntity(new UrlEncodedFormEntity(postParameters));

		HttpResponse httpResponse = client.execute(httpPost);
		String modificationResponse = EntityUtils.toString(httpResponse.getEntity(), "UTF-8");

		/**
		 * Keep in mind that you should handle errors correctly.
		 * If the Adyen platform does not accept or store a submitted request, you will receive a HTTP response with
		 * status code 500 Internal Server Error. The fault string can be found in the paymentResponse.
		 */
		if (httpResponse.getStatusLine().getStatusCode() == 500) {
			throw new ServletException(modificationResponse);
		}
		else if (httpResponse.getStatusLine().getStatusCode() != 200) {
			throw new ServletException(httpResponse.getStatusLine().toString());
		}

		/**
		 * If the message was syntactically valid and merchantAccount is correct you will receive a modification
		 * response with the following fields:
		 * - pspReference: A new reference to uniquely identify this modification request.
		 * - response: A confirmation indicating we received the request: [capture-received].
		 * 
		 * Please note: The result of the capture is sent via a notification with eventCode CAPTURE.
		 */
		Map<String, String> modificationResult = parseQueryString(modificationResponse);
		PrintWriter out = response.getWriter();

		out.println("Modification Result:");
		out.println("- pspReference: " + modificationResult.get("modificationResult.pspReference"));
		out.println("- response: " + modificationResult.get("modificationResult.response"));

	}

	/**
	 * Parse the result of the HTTP Post request (will be returned in the form of a query string)
	 */
	private Map<String, String> parseQueryString(String queryString) throws UnsupportedEncodingException {
		Map<String, String> parameters = new HashMap<String, String>();
		String[] pairs = queryString.split("&");

		for (String pair : pairs) {
			String[] keyval = pair.split("=");
			parameters.put(URLDecoder.decode(keyval[0], "UTF-8"), URLDecoder.decode(keyval[1], "UTF-8"));
		}

		return parameters;
	}

}
