package com.lantanagroup.link.db;

import com.lantanagroup.link.FhirContextProvider;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.json.JsonMode;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bson.json.JsonWriterSettings;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.io.StringWriter;

public class BaseCodec<T extends IBaseResource> implements Codec<T> {
  private Class<T> type;

  public BaseCodec(Class<T> type) {
    this.type = type;
  }

  @Override
  public T decode(BsonReader bsonReader, DecoderContext decoderContext) {
    StringWriter stringWriter = new StringWriter();
    JsonWriterSettings writerSettings = JsonWriterSettings
            .builder()
            .outputMode(JsonMode.RELAXED)
            .objectIdConverter((objectId, strictJsonWriter) -> {
              strictJsonWriter.writeString(objectId.toHexString());
            })
            .build();
    new JsonWriter(stringWriter, writerSettings).pipe(bsonReader);
    String json = stringWriter.toString();
    return (T) FhirContextProvider.getFhirContext().newJsonParser().parseResource(json);
  }

  @Override
  public void encode(BsonWriter bsonWriter, T resource, EncoderContext encoderContext) {
    String json = FhirContextProvider.getFhirContext().newJsonParser().encodeResourceToString(resource);
    bsonWriter.pipe(new JsonReader(json));
  }

  @Override
  public Class<T> getEncoderClass() {
    return this.type;
  }
}
