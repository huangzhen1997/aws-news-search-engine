package edu.northwestern.ssa.api;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.JSONWrappedObject;
import edu.northwestern.ssa.AwsSignedRestRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import javax.sound.midi.SysexMessage;
import javax.swing.text.html.Option;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.awt.*;
import java.io.IOException;
import java.util.*;

@Path("/search")
public class Search {

    /** when testing, this is reachable at http://localhost:8080/api/search?query=hello */
    @GET
    public Response getMsg(@QueryParam("query") String q,@QueryParam("language") String language, @QueryParam("date") String date, @QueryParam("count") String count, @QueryParam("offset") String offset) throws IOException {

        JSONArray results = new JSONArray();
        Map<String,String> queryPara = new HashMap<>();

        String tempQ=q;
        String queryString = "txt:";

        // empty query, bad request
        if(q == null || q.isEmpty()){
            results.put("'query' is missing from url.");
            return Response.status(400).type("application/json").entity(results.toString(4))
                    // below header is for CORS
                    .header("Access-Control-Allow-Origin", "*").build();
        }

        else {
            if(tempQ.contains(" ")){
                String newq= tempQ.replace(" "," AND ");
                queryString = queryString+"("+newq+")";
            }
            else {
                queryString += tempQ;
            }
        }

        if(language!=null){
            queryString = queryString + " AND lang:"+language;
        }

        if(date!=null){
            queryString = queryString + " AND date:"+date;
        }

        //first query parameter:
        queryPara.put("q",queryString);
        System.out.println("the es query parameter is q: "+queryString);

        // if date provided, pre-load for search, otherwise just search how many it provided
        if(count!=null){
            queryPara.put("size",count);
        }

        if(offset!=null) {

            queryPara.put("from",offset); // the ES API use key "from"
        }

        queryPara.put("track_total_hits","true");


        //sending out the request:
        Optional<Map<String,String>> query = Optional.of(queryPara);
        Optional<JSONObject> body = Optional.empty();
        AwsSignedRestRequest request = new AwsSignedRestRequest("es");
        HttpExecuteResponse response = request.restRequest(SdkHttpMethod.GET,System.getenv("ELASTIC_SEARCH_HOST"),System.getenv("ELASTIC_SEARCH_INDEX")+"/_search",query,body);

        System.out.println("testing ------------------------");
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Map> jsonMap = mapper.readValue(response.responseBody().get(), Map.class);

        JSONObject returning = new JSONObject("{}");
        JSONObject t = new JSONObject(jsonMap.get("hits"));
        System.out.println(jsonMap);

        //System.out.println(response.httpResponse().statusText());
        JSONObject total = t.getJSONObject("total");
        int size_recieved= total.getInt("value");
        int size_userwant=10;

        if(count!=null){
            size_userwant= Integer.parseInt(count);
        }

        if(size_recieved < size_userwant){
            size_userwant=size_recieved;
        }

        returning.put("returned_results",size_userwant);
        JSONArray articleArray = new JSONArray();
        JSONArray arrayOfText = t.getJSONArray("hits");

        int i = 0;
        while(i<size_userwant){

            JSONObject Text = arrayOfText.getJSONObject(i) ;
            JSONObject line = Text.getJSONObject("_source");
            articleArray.put(line);

            i++;
        }

        returning.put("articles",articleArray);
        returning.put("total_results",size_recieved);


        //System.out.println(jsonMap.values());

         //results.put(jsonMap);

//        JSONObject ttt = new JSONObject((tt.get("date")));
//        System.out.println(ttt);


        return Response.status(200).type("application/json").entity(returning.toString(4))
                // below header is for CORS
                .header("Access-Control-Allow-Origin", "*").build();

    }
}
