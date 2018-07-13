package org.pentaho.ctools.cpf.repository.rca;

import org.apache.commons.io.IOUtils;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

@Provider
@Produces("multipart/form-data")
public class ImportMessageBodyWriter implements MessageBodyWriter<ImportMessage> {
  final static String CRLF = "\r\n";

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return type == ImportMessage.class;
  }

  @Override
  public long getSize(ImportMessage importMessage, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  private byte[] prepareField(String boundary, String field, String value) {
    StringBuilder builder = new StringBuilder();
    builder.append("--");
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"" + field + "\"");
    builder.append(CRLF);
    builder.append(CRLF);
    builder.append(value);
    builder.append(CRLF);
    return builder.toString().getBytes();
  }

  private void writeContents(String boundary, InputStream contents, OutputStream destination) throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append("--");
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"fileUpload\"");
    builder.append(CRLF);
    builder.append(CRLF);
    destination.write(builder.toString().getBytes());
    IOUtils.copy(contents, destination);
    destination.write(CRLF.getBytes());
  }

  private byte[] createTerminator(String boundary) {
    return ("--" + boundary + "--" + CRLF).getBytes();
  }

  @Override
  public void writeTo(ImportMessage importMessage, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
    // define boundary
    String boundary = "------------------------b9cf97a74e305cd0";
    List<Object> contentType = httpHeaders.get(HttpHeaders.CONTENT_TYPE);
    contentType.set(0, contentType.get(0) + "; boundary=" + boundary);
    // write body
    entityStream.write(prepareField(boundary, "importDir", importMessage.importDir));
    writeContents(boundary, importMessage.contents, entityStream);
    entityStream.write(prepareField(boundary, "overwriteFile", String.valueOf(importMessage.overwrite)));
    entityStream.write(prepareField(boundary, "fileNameOverride", importMessage.filename));
    entityStream.write(createTerminator(boundary));
  }
}
