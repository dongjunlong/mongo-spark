/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.spark.api.java.sql;

import com.mongodb.spark.api.java.MongoSpark;
import com.mongodb.spark.api.java.RequiresMongoDB;
import com.mongodb.spark.rdd.api.java.JavaMongoRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.DataFrame;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.bson.Document;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static org.apache.spark.sql.types.DataTypes.createStructField;
import static org.apache.spark.sql.types.DataTypes.createStructType;
import static org.junit.Assert.assertEquals;

public final class MongoDataFrameReaderTest extends RequiresMongoDB {

    private final List<String> characters = asList(
        "{'name': 'Bilbo Baggins', 'age': 50}",
        "{'name': 'Gandalf', 'age': 1000}",
        "{'name': 'Thorin', 'age': 195}",
        "{'name': 'Balin', 'age': 178}",
        "{'name': 'Kíli', 'age': 77}",
        "{'name': 'Dwalin', 'age': 169}",
        "{'name': 'Óin', 'age': 167}",
        "{'name': 'Glóin', 'age': 158}",
        "{'name': 'Fíli', 'age': 82}",
        "{'name': 'Bombur'}"
    );

    @Test
    public void shouldBeEasilyCreatedFromTheSQLContext() {
        // Given
        JavaSparkContext sc = new JavaSparkContext(getSparkContext());
        MongoSpark.save(sc.parallelize(characters).map(JsonToDocument));

        StructField _idField = createStructField("_id", DataTypes.StringType, true);
        StructField nameField = createStructField("name", DataTypes.StringType, true);
        StructField ageField = createStructField("age", DataTypes.IntegerType, true);
        StructType expectedSchema = createStructType(asList(_idField, ageField, nameField));

        // When
        SQLContext sqlContext = new SQLContext(sc);
        DataFrame df = sqlContext.read().format("com.mongodb.spark.sql").load();

        // Then
        assertEquals(df.schema(), expectedSchema);
        assertEquals(df.count(), 10);
        assertEquals(df.filter("age > 100").count(), 6);
    }

    @Test
    public void shouldBeEasilyCreatedWithAProvidedRDDAndJavaBean() {
        // Given
        JavaSparkContext sc = new JavaSparkContext(getSparkContext());
        MongoSpark.save(sc.parallelize(characters).map(JsonToDocument));

        StructField nameField = createStructField("name", DataTypes.StringType, true);
        StructField ageField = createStructField("age", DataTypes.IntegerType, false);
        StructType expectedSchema = createStructType(asList(ageField, nameField));

        // When
        JavaMongoRDD<Document> mongoRDD = MongoSpark.load(sc);
        DataFrame df = new SQLContext(sc).createDataFrame(mongoRDD.map(new Function<Document, Character>() {
            @Override
            public Character call(final Document doc) throws Exception {
                Character character = new Character();
                if (doc.containsKey("age")) {
                    character.setAge(doc.getInteger("age"));
                }
                if (doc.containsKey("name")) {
                    character.setName(doc.getString("name"));
                }
                return character;
            }
        }), Character.class);

        // Then
        assertEquals(df.schema(), expectedSchema);
        assertEquals(df.count(), 10);
        assertEquals(df.filter("age > 100").count(), 6);
    }

    private static Function<String, Document> JsonToDocument = new Function<String, Document>() {
        @Override
        public Document call(final String json) throws Exception {
            return Document.parse(json);
        }
    };

}