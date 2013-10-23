/******************************************************************************
* Licensed Materials - Property of IBM
*
* (C) Copyright IBM Corp. 2005, 2012 All Rights Reserved.
*
* US Government Users Restricted Rights - Use, duplication, or
* disclosure restricted by GSA ADP Schedule Contract with IBM Corp.
*
*****************************************************************************/
/******************************************************************************
* v 0.1
*
*****************************************************************************/

package com.goonit.isimExtension;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;


import com.ibm.itim.dataservices.model.domain.Person;
import com.ibm.itim.logging.SystemLog;
import com.ibm.itim.workflow.application.WorkflowApplication;
import com.ibm.itim.workflow.application.WorkflowExecutionContext;
import com.ibm.itim.workflow.model.ActivityResult;

/**
 * Custom class for asynchronous activity
 */
public class RestfulExtension implements WorkflowApplication {


	protected WorkflowExecutionContext ctx;

	public RestfulExtension() {
	}

	/**
	 * Passes the workflow execution context to the application.
	 * 
	 * @param context
	 *            WorklowExecutionContext holding information about the
	 *            currently executing activity.
	 */
	public void setContext(WorkflowExecutionContext ctx) {
		this.ctx = ctx;
	}

	/**
	 * Perform change password extension asynchronously
	 * 
	 * @return ActivityResult The result of the activity. If summary==PENDING,
	 *         then the activity will be executed asynchronously; otherwise the
	 *         activity is completed. There is no detail returned.
	 * 
	 */
	public ActivityResult restPostExtension(Person person, String urlString, String urlParameters) {
		//following lines just call ISIM api's for demonstration to show we have been passed a person object
		System.out.println("I am in the extension");
		String cn = person.getAttribute("cn").getValueString();
		
	
		SystemLog logger = SystemLog.getInstance();
		logger.logInformation(this,"GoonIT REST extension, processing request for  : " + cn +"/" + ctx.getProcessEO().getId() );
		
		//we will send the Process and Activity ids to the RESTFul Service
		urlParameters+="&activityId="+ctx.getActivityEO().getId();
		urlParameters+="&processId="+ctx.getProcessVO().getId();
		urlParameters+="&cn="+cn;
		//create HTTP request
		URL url = null;
		try {
			url = new URL(urlString);
		} catch (MalformedURLException e) {
			
			e.printStackTrace();
			return new ActivityResult(ActivityResult.FAILED, "Invalid URL: "+e.toString(), null);
		} 
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			
		} catch (IOException e) {
			
			e.printStackTrace();
			return new ActivityResult(ActivityResult.FAILED, "Connection failed: "+e.toString(), null);
		}           
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false); 
		try {
			connection.setRequestMethod("POST");
		} catch (ProtocolException e) {
			
			e.printStackTrace();
			return new ActivityResult(ActivityResult.FAILED, "Protocol Exception: "+e.toString(), null);
		} 
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
		connection.setUseCaches (false);

		DataOutputStream wr = null;
		String response = null;
		int responseCode = 0;
		
		// perform the request return any connection errors as failed
		try {
			wr = new DataOutputStream(connection.getOutputStream ());
			wr.writeBytes(urlParameters);
			wr.flush();
			wr.close();
			 responseCode = connection.getResponseCode();
		
		
		//all important HTTP response code - 200, 202, etc
			 //Get Response	
		      InputStream is = connection.getInputStream();
		      BufferedReader rd = new BufferedReader(new InputStreamReader(is));
		      String line;
		      StringBuffer strBuffer = new StringBuffer(); 
		      while((line = rd.readLine()) != null) {
		    	  strBuffer.append(line);
		    	  strBuffer.append('\r');
		      }
		      rd.close();
		   response = strBuffer.toString();
		   response = response.substring(0, response.length() - 1);
		System.out.println("REST service response message " + response);
		//close the connection
		connection.disconnect();
		} catch (IOException e) {
			e.printStackTrace();
			return new ActivityResult(ActivityResult.FAILED, response, null);
			
		}
		
		
		//process the response
		switch (responseCode) {
		
		case 200:
			return new ActivityResult(ActivityResult.SUCCESS, response, null);
		case 202:
			//request received by REST Service but processing is being performed
			return new ActivityResult(ActivityResult.STATUS_WAIT,
					ActivityResult.PENDING, response, null);

		default:
			return new ActivityResult(ActivityResult.FAILED, responseCode + ":"+response, null);
			
		}
		
		
		
	}

}
