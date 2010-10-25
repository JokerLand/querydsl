/*
 * Copyright (c) 2010 Mysema Ltd.
 * All rights reserved.
 *
 */
package com.mysema.query.mongodb;

import static junit.framework.Assert.assertEquals;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mysema.query.mongodb.domain.QUser;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.path.DatePath;
import com.mysema.query.types.path.DateTimePath;
import com.mysema.query.types.path.NumberPath;
import com.mysema.query.types.path.PathBuilder;
import com.mysema.query.types.path.StringPath;

public class MongodbSerializerTest {
    
    private PathBuilder<Object> entityPath;
    private StringPath title;
    private NumberPath<Integer> year;
    private NumberPath<Double> gross;

    private NumberPath<Long> longField;
    private NumberPath<Short> shortField;
    private NumberPath<Byte> byteField;
    private NumberPath<Float> floatField;
    
    private DatePath<Date> date;
    private Date dateVal = new Date();
    private DateTimePath<Timestamp> dateTime;
    private Timestamp dateTimeVal = new Timestamp(System.currentTimeMillis());
 
    private MongodbSerializer serializer;
    
    @Before
    public void before() {
        serializer = new MongodbSerializer();
        entityPath = new PathBuilder<Object>(Object.class, "obj");
        title = entityPath.getString("title");
        year = entityPath.getNumber("year", Integer.class);
        gross = entityPath.getNumber("gross", Double.class);
        longField = entityPath.getNumber("longField", Long.class);
        shortField = entityPath.getNumber("shortField", Short.class);
        byteField = entityPath.getNumber("byteField", Byte.class);
        floatField = entityPath.getNumber("floatField", Float.class);
        date = entityPath.getDate("date", Date.class);
        dateTime = entityPath.getDateTime("dateTime", Timestamp.class);
    }
    
    @Test
    public void Paths(){
        QUser user = QUser.user;
        assertEquals("user", serializer.visit(user, null));
        assertEquals("addresses", serializer.visit(user.addresses, null));
        assertEquals("addresses", serializer.visit(user.addresses.any(), null));
        assertEquals("addresses.street", serializer.visit(user.addresses.any().street, null));
        assertEquals("firstName", serializer.visit(user.firstName, null));
    }
    
    @Test
    public void CollectionAny(){
        QUser user = QUser.user;
        assertQuery(user.addresses.any().street.eq("Aakatu"), dbo("addresses.street","Aakatu"));
    }
    
    @Test
    public void Equals() {
        assertQuery(title.eq("A"), dbo("title","A"));
        assertQuery(year.eq(1), dbo("year",1));
        assertQuery(gross.eq(1.0D), dbo("gross", 1.0D));
        assertQuery(longField.eq(1L), dbo("longField", 1L));
        assertQuery(shortField.eq((short)1), dbo("shortField", 1));
        assertQuery(byteField.eq((byte)1), dbo("byteField", 1L));
        assertQuery(floatField.eq(1.0F), dbo("floatField", 1.0F));
        
        assertQuery(date.eq(dateVal), dbo("date", dateVal));
        assertQuery(dateTime.eq(dateTimeVal), dbo("dateTime", dateTimeVal));
    }
    
    @Test
    public void EqAndEq() {
        assertQuery(
            title.eq("A").and(year.eq(1)), 
            dbo("title","A").append("year", 1)
        );
        
        assertQuery(
            title.eq("A").and(year.eq(1).and(gross.eq(1.0D))), 
            dbo("title","A").append("year", 1).append("gross", 1.0D)
        );
    }
    
    @Test
    public void NotEq() {
        assertQuery(title.ne("A"), dbo("title", dbo("$ne", "A")));
    }
    
    @Test
    public void LessAndGreaterAndBetween() {
        
        assertQuery(title.lt("A"), dbo("title", dbo("$lt", "A")));
        assertQuery(year.gt(1), dbo("year", dbo("$gt", 1)));
        
        assertQuery(title.loe("A"), dbo("title", dbo("$lte", "A")));
        assertQuery(year.goe(1), dbo("year", dbo("$gte", 1)));
        
        assertQuery(
                year.gt(1).and(year.lt(10)),
                dbo("year", dbo("$gt", 1)).
                append("year", dbo("$lt", 10))
        );        
        
        assertQuery(
                year.between(1, 10), 
                dbo("year", dbo("$gt", 1).append("$lt", 10))
        );
    }
    
    @Test
    public void In() {
        assertQuery(year.in(1,2,3), dbo("year", dbo("$in", 1,2,3)));        
    }
    
    @Test
    public void OrderBy() {
        DBObject orderBy = serializer.toSort(sortList(year.asc()));
        assertEquals(dbo("year", 1), orderBy);

        orderBy = serializer.toSort(sortList(year.desc()));
        assertEquals(dbo("year", -1), orderBy);

        orderBy = serializer.toSort(sortList(year.desc(), title.asc()));
        assertEquals(dbo("year", -1).append("title", 1), orderBy);
    }
    
    @Test
    public void Regexcases() {
        assertQuery(title.startsWith("A"), 
                dbo("title", dbo("$regex", "^\\QA\\E").append("$options", "")));
        assertQuery(title.startsWithIgnoreCase("A"),
                dbo("title", dbo("$regex", "^\\QA\\E").append("$options", "i")));
        
        assertQuery(title.endsWith("A"), 
                dbo("title", dbo("$regex", "\\QA\\E$").append("$options", "")));
        assertQuery(title.endsWithIgnoreCase("A"),
                dbo("title", dbo("$regex", "\\QA\\E$").append("$options", "i")));
        
        assertQuery(title.equalsIgnoreCase("A"),
                dbo("title", dbo("$regex", "\\QA\\E").append("$options", "i")));
        
        assertQuery(title.contains("A"), 
                dbo("title", dbo("$regex", ".*\\QA\\E.*").append("$options", "")));
        assertQuery(title.containsIgnoreCase("A"),
                dbo("title", dbo("$regex", ".*\\QA\\E.*").append("$options", "i")));
        
        assertQuery(title.matches(".*A^"),
                dbo("title", dbo("$regex", ".*A^").append("$options", "")));
        
    }
    
    @Test
    public void Not() {
        assertQuery(title.eq("A").not(), dbo("title", dbo("$ne","A")));
        
        assertQuery(title.lt("A").not().and(year.ne(1800)), 
                dbo("title", dbo("$not", dbo("$lt","A"))).
                append("year", dbo("$ne", 1800)));
    }
    
    
    private List<OrderSpecifier<?>> sortList(OrderSpecifier<?> ... order) {
        return Arrays.asList(order);
    }

    private void assertQuery(Expression<?> e, BasicDBObject expected) {
        BasicDBObject result = (BasicDBObject) serializer.handle(e);
        assertEquals(expected.toString(), result.toString());
    }

    public static BasicDBObject dbo(String key, Object... value) {
        if (value.length == 1) {
            return new BasicDBObject(key, value[0]);
        }
        
        return new BasicDBObject(key, value);
    }
    
    
    

}
