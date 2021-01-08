package edu.utec.tools.stressify.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import edu.utec.tools.stressify.common.AssertsHelper;
import edu.utec.tools.stressify.common.SmartHttpClient;
import edu.utec.tools.stressify.common.TimeHelper;
import edu.utec.tools.stressify.core.BaseScriptExecutor;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class ContinuosRestClient implements BaseScriptExecutor {

  private final Logger logger = LogManager.getLogger(this.getClass());

  public String[] output;
  private SmartHttpClient smartHttpClient = new SmartHttpClient();
  private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy:MM:dd-HH:mm:ss:SSS");
  private HashMap<String, Object> response = null;

  public void performRequest(String method, String url, String body,
      HashMap<String, String> headers, String assertScript) {

    String id = UUID.randomUUID().toString();
    Date dateOnError = new Date();
    try {
      response = smartHttpClient.performRequest(method, url, body, headers);
    } catch (Exception e) {
      logger.error("Failed to execute http invocation with id: " + id, e);
      response = new HashMap<String, Object>();
      response.put("startDate", dateFormat.format(TimeHelper.millisToDate(dateOnError.getTime())));
      response.put("log", "Connection error:" + e.getMessage());
      return;
    }

    response.put("id", id);
    response.put("startDate",
        dateFormat.format(TimeHelper.millisToDate((Long) response.get("startMillisDate"))));
    response.put("endDate",
        dateFormat.format(TimeHelper.millisToDate((Long) response.get("endMillisDate"))));

    try {
      AssertsHelper.evaluateSimpleAssert((String) response.get("responseBody"), assertScript);
      response.put("asserts", true);
    } catch (Exception e) {
      logger.error("Failed to execute asserts on http response with id: " + id, e);
      logger.error("http response with id: \n" + (String) response.get("responseBody"), e);
      response.put("asserts", false);
      response.put("log", "Assert error:" + e.getMessage());
      return;
    }
  }

  public void performRequest2(String method, String url, String body,
      ArrayList<HashMap<String, String>> headers, String assertScript) {

    output = new String[7];

    switch (method) {

      case "GET":
        performGetRequest(url, headers, assertScript);
        break;
      case "POST":
        performRequest(url, headers, body, assertScript, "POST");
        break;
      case "PUT":
        performRequest(url, headers, body, assertScript, "PUT");
        break;
      default:
        output[3] = "Error: Method not implemented yet:" + method;
        break;
    }
  }

  public void performRequest(String url, ArrayList<HashMap<String, String>> headers, String body,
      String assertScript, String method) {
    try {

      Date start = new Date();
      long startMillis = start.getTime();

      URL obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();
      con.setDoOutput(true);

      con.setRequestMethod(method);

      // add request header
      for (HashMap<String, String> headerData : headers) {
        Iterator<?> it = headerData.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<?, ?> pair = (Map.Entry<?, ?>) it.next();
          con.setRequestProperty("" + pair.getKey(), "" + pair.getValue());
        }
      }

      try (OutputStream os = con.getOutputStream()) {
        byte[] input = body.getBytes("utf-8");
        os.write(input, 0, input.length);
      }

      int responseCode = con.getResponseCode();
      System.out.println("\nSending '" + method + "' request to URL : " + url);
      System.out.println("Response Code : " + responseCode);

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }

      Date now = new Date();
      long nowMillis = now.getTime();

      in.close();

      SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy:MM:ddd");
      output[0] = dateFormat1.format(start);

      SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss-SSS");
      output[1] = dateFormat2.format(start);

      output[2] = dateFormat2.format(now);
      output[3] = "" + responseCode;

      if (assertScript != null && !assertScript.equals("")) {
        output[4] = "" + evaluateAssert(response.toString(), assertScript);
      } else {
        output[4] = "-";
      }

      output[5] = "" + (nowMillis - startMillis);

    } catch (Exception ex) {
      ex.printStackTrace();
      output[3] = "Error:" + ex.getMessage();
    }
  }

  public boolean evaluateAssert(String response, String script) {
    Binding binding = new Binding();
    binding.setVariable("response", response);
    GroovyShell shell = new GroovyShell(binding);
    shell.evaluate(script);
    // if no errors were thrown
    return true;
  }

  public void performGetRequest(String url, ArrayList<HashMap<String, String>> headers,
      String assertScript) {

    try {

      Date start = new Date();
      long startMillis = start.getTime();

      URL obj = new URL(url);
      HttpURLConnection con = (HttpURLConnection) obj.openConnection();

      // optional default is GET
      con.setRequestMethod("GET");

      // add request header
      for (HashMap<String, String> headerData : headers) {
        Iterator<?> it = headerData.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry<?, ?> pair = (Map.Entry<?, ?>) it.next();
          con.setRequestProperty("" + pair.getKey(), "" + pair.getValue());
        }
      }

      int responseCode = con.getResponseCode();
      logger.info("Sending 'GET' request to URL : " + url);
      logger.info("Response Code : " + responseCode);

      BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer();

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }

      Date now = new Date();
      long nowMillis = now.getTime();

      in.close();

      output[0] = UUID.randomUUID().toString();

      SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy:MM:ddd");
      output[1] = dateFormat1.format(start);

      SimpleDateFormat dateFormat2 = new SimpleDateFormat("HH:mm:ss-SSS");
      output[2] = dateFormat2.format(start);

      output[3] = dateFormat2.format(now);
      output[4] = "" + responseCode;

      if (assertScript != null && !assertScript.equals("")) {
        output[5] = "" + evaluateAssert(response.toString(), assertScript);
      } else {
        output[5] = "-";
      }

      output[6] = "" + (nowMillis - startMillis);

    } catch (Exception ex) {
      ex.printStackTrace();
      output[4] = "Error:" + ex.getMessage();
    }
  }

  @Override
  public Object getOutput() {
    return output;
  }

  public HashMap<String, Object> getResponse() {
    return response;
  }

}
