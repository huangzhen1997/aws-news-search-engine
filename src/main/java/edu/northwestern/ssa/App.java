package edu.northwestern.ssa;
// total html record : 39123

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.TaggedIOException;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.jsoup.UncheckedIOException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import software.amazon.awssdk.http.HttpExecuteResponse;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.services.s3.*;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.regions.*;
import org.archive.io.warc.WARCReaderFactory;
import org.archive.io.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import org.jsoup.nodes.*;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import sun.nio.ch.IOUtil;

import javax.swing.plaf.synth.SynthScrollBarUI;


public class App {
    public static void main(String[] args) {


        File f = new File("./output");

//         Step 1 :
//         downloading webarchive file


        try{
            S3Client s3 = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();

            String filename = System.getenv("COMMON_CRAWL_FILENAME");

            if (filename == null ) {

            ListObjectsV2Response list = s3.listObjectsV2(ListObjectsV2Request.builder().bucket("commoncrawl").prefix("crawl-data/CC-NEWS/2019/10").build());
            List<S3Object>  l = list.contents();
            filename = l.get(l.size()-1).key();

        }

            System.out.println(filename);
            GetObjectRequest request = GetObjectRequest.builder().bucket("commoncrawl").key(filename).build();
            System.out.println("File is downloading - - - - -");
            s3.getObject(request, ResponseTransformer.toFile(f));
            s3.close();
            System.out.println("File has been succesfully downloaded");
        }

        catch (SdkClientException sdk){

            System.out.println(sdk.toString());
        }

        //jump over to do part of the step 4
        //Creating Index
        try {

            ElasticSearch e = new ElasticSearch("es");
            HttpExecuteResponse output = e.CreateIndex();
            e.close();
            System.out.println("Index has been created");
        }

        catch (IOException ioe){
            ioe.toString();
        }




//        Step 2,3:
//         Parsing webarchive file


        try {

            int total = 0;
            int counter = 0;
            ArchiveReader reader;
            reader = WARCReaderFactory.get(f);
            ArrayList<JSONObject> queue = new ArrayList<>();

            for (ArchiveRecord record : reader) {


                try {

                    ElasticSearch es = new ElasticSearch("es");

                    int size = record.available();
                    byte[] currentRecord = IOUtils.toByteArray(record,size);
                    String to_parse = new String(currentRecord);

                    int cur = record.read(); // current reading pointer in the record
                    StringBuilder header = new StringBuilder();
                    while (cur != -1) {      // keep reading by calling read() until cur is  -1 (end of record)

                        header.append((char) cur);
                        cur = record.read();
                    }

                    String[] outPutString = to_parse.split("\r\n\r\n"); // split the record into header + body
                    String html="";

                    if (outPutString.length==1){
                        continue;
                    }

                    for (int i = 1; i<outPutString.length;i++){
                            html += outPutString[i];
                    }
                    String url = record.getHeader().getUrl();  // get the url

                    Document doc = Jsoup.parse(html);
                    String title = doc.title();
                    String text = doc.text();

                    JSONObject body = new JSONObject();
                    body.put("title",title);
                    body.put("text",text);
                    body.put("url",url);

                    //System.out.println(text);

                    queue.add(body);
                    //Step 4
                    // post to elasticsearch

                    if (counter == 100){

                    HttpExecuteResponse response = es.PostBulk(queue);
                    System.out.println(response.httpResponse().statusCode());
                    System.out.println(response.httpResponse().statusText());

                    queue.clear();
                    counter = 0;
                    }
                    es.close();
                    System.out.println(total);
                    total++;
                    counter++;

                }

                catch (UncheckedIOException ioe){
                    System.out.println(ioe.toString());
                    continue;
                }
            }

//            ElasticSearch es = new ElasticSearch("es");
//            HttpExecuteResponse response = es.PostBulk(queue);
//            System.out.println(response.httpResponse().statusCode());
//            System.out.println(response.httpResponse().statusText());
            queue.clear();

        }

        catch(IOException ie){
                System.out.println(ie.toString());
            }

    }
}







