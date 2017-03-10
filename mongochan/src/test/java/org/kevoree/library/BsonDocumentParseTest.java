package org.kevoree.library;

import org.bson.Document;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by mleduc on 25/11/15.
 */
public class BsonDocumentParseTest {


    @Test(expected = org.bson.json.JsonParseException.class)
    public void test2() throws Exception {
        Document.parse("{\"a\":b}");
    }

    @Test
    public void test() throws Exception {
        Assert.assertNotNull(Document.parse("{\"a\":1}"));
    }
}
