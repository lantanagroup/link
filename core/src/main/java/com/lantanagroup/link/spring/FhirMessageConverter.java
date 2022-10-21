package com.lantanagroup.link.spring;

import ca.uhn.fhir.context.FhirContext;
import com.lantanagroup.link.FhirContextProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

/**
 * Implementation of HttpMessageConverter that is used by SpringBoot to serialize responses
 * from APIs in FHIR JSON or XML. Uses the HAPI libraries to serialize.
 */
public class FhirMessageConverter<T> implements HttpMessageConverter<T> {

  @Override
  public boolean canRead(Class<?> aClass, MediaType mediaType) {
    return false;
  }

  @Override
  public boolean canWrite(Class<?> aClass, MediaType mediaType) {
    return aClass.equals(Bundle.class);
  }

  @Override
  public List<MediaType> getSupportedMediaTypes() {
    return List.of(
            MediaType.APPLICATION_JSON,
            MediaType.valueOf("application/fhir+json"),
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/fhir+xml")
    );
  }

  @Override
  public T read(Class<? extends T> aClass, HttpInputMessage httpInputMessage) throws IOException, HttpMessageNotReadableException {
    throw new HttpMessageNotReadableException("Reading parameters for FHIR resources is not yet supported", httpInputMessage);
  }

  @Override
  public void write(Object o, MediaType mediaType, HttpOutputMessage httpOutputMessage) throws IOException, HttpMessageNotWritableException {
    FhirContext ctx = FhirContextProvider.getFhirContext();
    OutputStreamWriter writer = new OutputStreamWriter(httpOutputMessage.getBody());

    if (mediaType.equals(MediaType.APPLICATION_JSON) || mediaType.equals(MediaType.valueOf("application/fhir+json"))) {
      writer.write(ctx.newJsonParser().setPrettyPrint(true).encodeResourceToString((IBaseResource) o));
    } else if (mediaType.equals(MediaType.APPLICATION_XML) || mediaType.equals(MediaType.valueOf("application/fhir+xml"))) {
      writer.write(ctx.newXmlParser().setPrettyPrint(true).encodeResourceToString((IBaseResource) o));
    } else {
      throw new HttpMessageNotWritableException("Media type " + mediaType.toString() + " not supported");
    }

    writer.close();
  }
}
