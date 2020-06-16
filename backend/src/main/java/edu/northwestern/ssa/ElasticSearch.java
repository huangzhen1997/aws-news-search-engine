package edu.northwestern.ssa;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.auth.signer.params.Aws4SignerParams;
import software.amazon.awssdk.auth.signer.Aws4Signer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import software.amazon.awssdk.regions.Region;
import sun.tools.jstack.JStack;
import vc.inreach.aws.request.AWSSigningRequestInterceptor;

public class ElasticSearch extends AwsSignedRestRequest {


    /**
     * @param serviceName would be "es" for Elasticsearch
     */
    ElasticSearch(String serviceName) {
        super(serviceName);
    }


    protected HttpExecuteResponse CreateIndex() throws IOException {

        String index = System.getenv("ELASTIC_SEARCH_INDEX");
        JSONObject body = new JSONObject("{}");
        SdkHttpMethod method = SdkHttpMethod.PUT;
        Optional<Map<String, String>> q = Optional.empty();
        Optional<String> b = Optional.of(body.toString());;


        return super.restRequest(method,System.getenv("ELASTIC_SEARCH_HOST"),index,q,b);
    }

    protected HttpExecuteResponse PostDocument(JSONObject body,int counter) throws IOException {

        SdkHttpMethod method = SdkHttpMethod.PUT;
        String index = System.getenv("ELASTIC_SEARCH_INDEX");
        String path = "/"+index+"/_create/"+ counter;
        Optional<Map<String, String>> q = Optional.empty();
        Optional<String> b = Optional.of(body.toString());

        return super.restRequest(method,System.getenv("ELASTIC_SEARCH_HOST"),path,q,b);
    }

    protected HttpExecuteResponse PostBulk(ArrayList<JSONObject> queue) throws IOException{

        SdkHttpMethod method = SdkHttpMethod.POST;
        String index = System.getenv("ELASTIC_SEARCH_INDEX");
        String path = "/"+"_bulk";
        Optional<Map<String, String>> q = Optional.empty();
        String createIndex = "{"+"\"index\""+":{\"_index\":\""+index+"\"}}\n";
        StringBuilder sb = new StringBuilder();

        for( JSONObject j : queue){
            sb.append(createIndex);
            sb.append(j.toString());
            sb.append("\n");
        }

        Optional<String> b= Optional.of(sb.toString());

        //System.out.println(b);


        return  super.restRequest(method,System.getenv("ELASTIC_SEARCH_HOST"),path,q,b);
    }

    protected  HttpExecuteResponse Search(Map<String, String> map) throws IOException{

        SdkHttpMethod method = SdkHttpMethod.PUT;
        String index = System.getenv("ELASTIC_SEARCH_INDEX");
        String path = "/"+index+"/_search";
        Optional<Map<String, String>> mapping = Optional.of(map);
        Optional<String> body = Optional.empty();

        return super.restRequest(SdkHttpMethod.GET,System.getenv("ELASTIC_SEARCH_HOST"),path,mapping,body);
    }
}
