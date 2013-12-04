package org.phauna.litecoinwidget;

import org.json.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.util.HashMap;
import java.util.Date;
import android.util.Log;
import android.widget.Toast;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class Downloaders {

  private UpdateWidgetService.GetPriceTask.Toaster mToaster;

  public Downloaders(UpdateWidgetService.GetPriceTask.Toaster toaster) {
    mToaster = toaster;
  }

  public void toastLong(String msg) {
    msg = "LitecoinWidget: " + msg;
    if (mToaster != null) {
      mToaster.toast(new Toasty(msg, Toast.LENGTH_LONG));
    }
  }

  public void toastShort(String msg) {
    msg = "LitecoinWidget: " + msg;
    if (mToaster != null) {
      mToaster.toast(new Toasty(msg, Toast.LENGTH_SHORT));
    }
  }

  public static HashMap<String, ExchangeRateEntry> currencyExchangeCache = new HashMap();

  public double getCachedExchangeRate(String from, String to) {
    String cPair = from + to;
    ExchangeRateEntry e = currencyExchangeCache.get(cPair);
    Date now = new Date();
    if (e == null) {
      Log.d(C.LOG, "no cached rate found for " + from + to);
    }
    if (e == null || now.getTime() - e.mDate.getTime() > 1000 * 60 * 60) { // 1 hour
      // refresh it
      double newRate = getCurrencyExchangeRate(from, to);
      Log.d(C.LOG, "got rate " + newRate + " for " + cPair);
      if (newRate == -1) {
        // don't save bad result. instead, return old stale result if possible
        if (e == null) return -1;
        return e.mRate;
      }
      Log.d(C.LOG, "caching rate " + newRate + " for " + cPair);
      ExchangeRateEntry newEntry = new ExchangeRateEntry(newRate, now);
      currencyExchangeCache.put(cPair, newEntry);
      return newRate;
    }
    return e.mRate;
  }

  public static class ExchangeRateEntry {
    public double mRate;
    public Date mDate;
    public ExchangeRateEntry(double rate, Date date) {
      mRate = rate;
      mDate = date;
    }
  }

  public double getCurrencyExchangeRate(String from, String to) {
    from = from.toUpperCase();
    to = to.toUpperCase();
    try {
      //URL url = new URL("http://rate-exchange.appspot.com/currency?from=" + from + "&to=" + to);
      URL url = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=" + from + to +"=X&f=l1");
      String csv = downloadReq(url);
      if (csv == null) return 0;
      //JSONObject j = new JSONObject(json);
      //Log.d(C.LOG, "got json: " + j.toString());
      double price = Double.parseDouble(csv);
      return price;
    } catch (MalformedURLException e) {
      assert false;
    }
    return -1;
  }

  public double getBtcchinaPrice() {
    try {
      URL url = new URL("https://vip.btcchina.com/bc/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getJSONObject("ticker").getDouble("last");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getBit2cPrice() {
    try {
      URL url = new URL("https://www.bit2c.co.il/Exchanges/NIS/Ticker.json");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getDouble("ll");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getCoinbasePrice() {
    try {
      URL url = new URL("https://coinbase.com/api/v1/currencies/exchange_rates");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getDouble("btc_to_usd");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getKrakenPrice(String coin, String owc) {
    coin = coin.toUpperCase();
    owc = owc.toUpperCase();
    String coinverted = coin;
    if (coin.equals("BTC")) {
      coinverted = "XBT";
    }
    String silliness = "X" + coinverted + "Z" + owc;
    try {
      URL url = new URL("https://api.kraken.com/0/public/Ticker?pair=" + coinverted + owc);
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        String priceS = j.getJSONObject("result").getJSONObject(silliness).getJSONArray("c").getString(0);
        double price = Double.parseDouble(priceS);
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getBitcoinAveragePrice(String owc) {
    owc = owc.toUpperCase();
    try {
      URL url = new URL("https://api.bitcoinaverage.com/ticker/" + owc);
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getDouble("last");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getMcxnowPrice(String coin) {
    try {
      URL url = new URL("https://mcxnow.com/orders?cur=" + coin);
      String xml = downloadReq(url);
      if (xml == null) return 0;
      try {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new InputSource(new StringReader(xml)));
        String priceString = doc.getElementsByTagName("lprice").item(0).getTextContent();
        double price = Double.parseDouble(priceString);
        return price;
      } catch (Exception e) {
        toastLong("Exception parsing: " + xml);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getVirtexPrice() {
    try {
      URL url = new URL("https://www.cavirtex.com/api/CAD/ticker.json");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getDouble("last");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getMtgoxPrice() {
    try {
      URL url = new URL("https://data.mtgox.com/api/1/BTCUSD/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double price = j.getJSONObject("return").getJSONObject("last").getDouble("value");
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }
    return 0;
  }

  public double getVircurexPrice(String coin) {
    try {
      URL url = new URL("https://vircurex.com/api/get_last_trade.json?base=" + coin.toUpperCase() + "&alt=BTC");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        String priceString = j.getString("value");
        double price = Double.valueOf(priceString);
        return price;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public double getBtcePrice(String coin, String in) {
    try {
      URL url = new URL("https://btc-e.com/api/2/"
          + coin.toLowerCase() + "_" + in.toLowerCase() + "/ticker");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        JSONObject ticker = j.getJSONObject("ticker");
        double last = ticker.getDouble("last");
        return last;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  // all prices @ cryptsy are currently in BTC
  public double getCryptsyPrice(String coin) {
    try {
      URL url = new URL("http://cryptsy.phauna.org/getLast.php?ticker=" + coin);
      String s = downloadReq(url);
      if (s == null) return 0;
      double last = Double.parseDouble(s);
      return last;
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public double getBitstampPrice() {
    try {
      URL url = new URL("https://www.bitstamp.net/api/ticker/");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double last = j.getDouble("last");
        return last;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public double getCampBXPrice() {
    try {
      URL url = new URL("http://CampBX.com/api/xticker.php");
      String json = downloadReq(url);
      if (json == null) return 0;
      try {
        JSONObject j = new JSONObject(json);
        double last = j.getDouble("Last Trade");
        return last;
      } catch (JSONException e) {
        toastLong("jsonException parsing: " + json);
      }
    } catch (MalformedURLException e) {
      assert false;
    }

    return 0;
  }

  public String downloadReq(URL url) {
    InputStream in = null;
    OutputStream out = null;
    HttpURLConnection conn = null;

    System.setProperty("http.keepAlive", "false");

    try {

      if (url.getProtocol().toLowerCase().equals("https")) {
        trustAllHosts();
        HttpsURLConnection https = (HttpsURLConnection) url.openConnection();
        https.setHostnameVerifier(DO_NOT_VERIFY);
        conn = https;
      } else {
        conn = (HttpURLConnection) url.openConnection();
      }

      conn.setReadTimeout(10000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("GET");
      conn.setDoInput(true);

      conn.connect();

      int response = conn.getResponseCode();
      if (response == 404) {
        toastLong("http 404 (not found) for " + url);
        return null;
      } else if (response == 403) {
        toastLong("http 403 (forbidden) for " + url);
        return "";
      } else if (response == 200) {
        toastShort("ok!");
        in = new BufferedInputStream(conn.getInputStream());
        String res = convertStreamToString(in);
        return res;
      } else {
        toastLong("unrecognized http code: " + response + " for " + url);
        return null;
      }
    } catch (java.net.ConnectException e) {
      toastLong("exception: " + e.toString() + " for " + url);
      return null;
    } catch (java.io.IOException e) {
      toastLong("exception: " + e.toString() + " for " + url);
      return null;
    } finally {
        try {
          if (conn != null) { conn.disconnect(); };
          if (in != null) { in.close(); };
          if (out != null) { out.close(); };
        } catch (java.io.IOException e) {
          toastLong("exception while closing: " + e.toString() + " for " + url);
        }
      }
    }

  public static String convertStreamToString(InputStream is)
      throws java.io.IOException {
    BufferedReader reader =
      new BufferedReader(new InputStreamReader(is));
    StringBuilder sb = new StringBuilder();
    String line = null;

    while ((line = reader.readLine()) != null) {
      sb.append(line);
    }

    is.close();

    return sb.toString();
  }

  // always verify the host - dont check for certificate
  final static HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
    public boolean verify(String hostname, SSLSession session) {
      return true;
    }
  };

  /**
   * Trust every server - dont check for any certificate
   */
  private static void trustAllHosts() {
    // Create a trust manager that does not validate certificate chains
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return new java.security.cert.X509Certificate[] {};
      }

      public void checkClientTrusted(X509Certificate[] chain,
          String authType) throws CertificateException {
      }

      public void checkServerTrusted(X509Certificate[] chain,
          String authType) throws CertificateException {
      }
    } };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("TLS");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection
          .setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }


}
