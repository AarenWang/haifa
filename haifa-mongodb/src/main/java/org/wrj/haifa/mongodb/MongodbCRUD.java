package org.wrj.haifa.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.Date;

public class MongodbCRUD {

    public static void main(String[] args) {
        MongoClient mongoClient = new MongoClient();
        MongoDatabase database = mongoClient.getDatabase("test");
        //database.createCollection("pod");
        MongoCollection<Document> collection = database.getCollection("pod");
        Document document1 = new Document();
        document1.append("pod-name", "Pod1").append("createdate", new Date(System.currentTimeMillis())).append("status", 1);

        Document document2 = new Document();
        document2.append("pod-name", "Pod2").append("createdate", new Date(System.currentTimeMillis())).append("status", 1);

        collection.insertOne(document1);
        collection.insertOne(document2);


        System.out.println(collection.count());

        BasicDBObject query = new BasicDBObject("pod-name", "Pod1");
        System.out.println(query + ":" + collection.count(query));


        BasicDBObject emptyQuery = new BasicDBObject();

        collection.deleteMany(emptyQuery);


        FindIterable<Pod> findIterable = collection.find(query, Pod.class);
        Pod firstPod = findIterable.first();
        System.out.println(firstPod);


    }

}


class Pod {

    private String name;

    private Date createdate;

    private Integer status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedate() {
        return createdate;
    }

    public void setCreatedate(Date createdate) {
        this.createdate = createdate;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }


}