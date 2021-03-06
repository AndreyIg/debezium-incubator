/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.cassandra.transforms.type.deserializer;

import io.debezium.connector.cassandra.transforms.CassandraTypeToAvroSchemaMapper;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.SetType;

import java.nio.ByteBuffer;
import java.util.Set;

public class SetTypeDeserializer extends TypeDeserializer {

    @Override
    @SuppressWarnings("unchecked")
    public Object deserialize(AbstractType<?> abstractType, ByteBuffer bb) {
        Set<?> deserializedList = (Set<?>) super.deserialize(abstractType, bb);
        return new GenericData.Array(getSchema(abstractType), deserializedList);
    }

    @Override
    public Schema getSchema(AbstractType<?> abstractType) {
        SetType<?> listType = (SetType<?>) abstractType;
        AbstractType<?> elementsType = listType.getElementsType();
        Schema innerSchema = CassandraTypeToAvroSchemaMapper.getSchema(elementsType, false);
        return SchemaBuilder.array().items(innerSchema);
    }
}
