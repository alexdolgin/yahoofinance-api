package yahoofinance.quotes.query1v7;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import yahoofinance.Utils;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes2.Crumb;
import yahoofinance.histquotes2.CrumbManager;
import yahoofinance.util.RedirectableRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 *
 * @author Stijn Strickx
 * @param <T> Type of object that can contain the retrieved information from a
 * quotes request
 */
public abstract class QuotesRequest<T> {

    private static final Logger log = LoggerFactory.getLogger(QuotesRequest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String symbols;

    public QuotesRequest(String symbols) {
        this.symbols = symbols;
    }

    public String getSymbols() {
        return symbols;
    }

    protected abstract T parseJson(JsonNode node);

    protected void parseJsonTo(JsonNode node, Collection<? super T> col) {
        try {
            T obj = parseJson(node);
            if (obj != null) {
                col.add(obj);
            }
        } catch (Throwable e) {
            log.error("Can't parse json: " + node, e);
        }
    }

    public T getSingleResult() throws IOException {
        List<T> results = this.getResult();
        if (results.size() > 0) {
            return results.get(0);
        }
        return null;
    }

    /**
     * Sends the request to Yahoo Finance and parses the result
     *
     * @return List of parsed objects resulting from the Yahoo Finance request
     * @throws IOException when there's a connection problem or the request is incorrect
     */
    public List<T> getResult() throws IOException {
        int responseCode = -1;
        boolean forceRefresh = false;
        while (true) {
            boolean fresh = true;
            try {
                List<T> result = new ArrayList<T>();

                Map<String, String> params = new LinkedHashMap<String, String>();
                params.put("symbols", this.symbols);
                Crumb crumb = CrumbManager.getCrumb(forceRefresh);
                fresh = crumb.isFresh();
                params.put("crumb", crumb.getValue());

                String url = YahooFinance.QUOTES_QUERY1V7_BASE_URL + "?" + Utils.getURLParameters(params);

                // Get JSON from Yahoo
                log.info("Sending request: " + url);

                URL request = new URL(url);
                RedirectableRequest redirectableRequest = new RedirectableRequest(request, 5);
                redirectableRequest.setConnectTimeout(YahooFinance.CONNECTION_TIMEOUT);
                redirectableRequest.setReadTimeout(YahooFinance.CONNECTION_TIMEOUT);
                HttpURLConnection connection = redirectableRequest.openConnection();
                responseCode = connection.getResponseCode();
                log.info("Response code: " + responseCode);
                InputStreamReader is = new InputStreamReader(connection.getInputStream());
                JsonNode node = objectMapper.readTree(is);
                if (node.has("quoteResponse") && node.get("quoteResponse").has("result")) {
                    node = node.get("quoteResponse").get("result");
                    for (int i = 0; i < node.size(); i++) {
                        parseJsonTo(node.get(i), result);
                    }
                } else {
                    throw new IOException("Invalid response");
                }

                return result;
            } catch (IOException e) {
                log.debug("responseCode=" + responseCode + " fresh=" + fresh, e);
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED && !fresh) {
                    forceRefresh = true;
                    responseCode = -1;
                    continue;
                } else {
                    throw e;
                }
            }
        }
    }

    protected String getStringValue(JsonNode node, String field) {
        if(node.has(field)) {
            return node.get(field).asText();
        }
        return null;
    }
}
