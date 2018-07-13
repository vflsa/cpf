/*!
 * Copyright 2018 Webdetails, a Hitachi Vantara company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */
package org.pentaho.ctools.cpf.repository.rca;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/*
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
*/
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.pentaho.ctools.cpf.repository.rca.dto.RepositoryFileDto;
import org.pentaho.ctools.cpf.repository.rca.dto.StringKeyStringValueDto;
import pt.webdetails.cpf.repository.api.IRWAccess;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

public class RemoteReadWriteAccess extends RemoteReadAccess implements IRWAccess {
  private static final Log logger = LogFactory.getLog( RemoteReadWriteAccess.class );

  public RemoteReadWriteAccess(String reposURL) {
    super(reposURL);
  }

  public boolean saveFileApacheHTTP(String path, InputStream contents ) {
    // split into folder and filename
    int splitIndex = path.lastIndexOf('/');
    String folder = splitIndex > 0 ? path.substring(0, splitIndex) : "/";
    String filename = splitIndex > 0 ? path.substring(splitIndex + 1, path.length()) : null;

    if ( filename == null || filename.isEmpty() ) {
      throw new IllegalArgumentException( "Invalid path: " + path );
    }

    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials  = new UsernamePasswordCredentials("admin", "password");
    provider.setCredentials(AuthScope.ANY, credentials);

    HttpClient client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();

    /* auth context */
    HttpClientContext ctx = HttpClientContext.create();
    ctx.setCredentialsProvider(provider);
    ctx.setAuthCache(new BasicAuthCache());

    HttpHost targetHost = new HttpHost("127.0.0.1", 8080);
    ctx.getAuthCache().put(targetHost, new BasicScheme());
    /* ---- */


    HttpEntity entity = MultipartEntityBuilder
        .create()
        .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        .addTextBody("importDir", folder)
        .addBinaryBody("fileUpload", contents)
        .addTextBody("overwriteFile", "true")
        .addTextBody("fileNameOverride", filename)
        .build();

    String address = reposURL + "/api/repo/files/import";
    HttpPost httpPost = new HttpPost(address);
    httpPost.setEntity(entity);

    try {
      HttpResponse response = client.execute(httpPost, ctx);
      return response.getStatusLine().getStatusCode() == 200;
      // TODO: change metadata so that file is not hidden!
    }
    catch (Exception ex) {
      System.out.println(ex.getStackTrace());
    }
    return false;
  }


  Attachment formDataField(String field, String value) {
    MultivaluedMap<String, String> headers =
        new MetadataMap<String, String>(false, true);
    headers.putSingle( HttpHeaders.CONTENT_DISPOSITION, "form-data; name=\"" + field + "\"" );
    return new Attachment(headers, value);
  }

  public boolean saveFileCXF( String path, InputStream contents ) {
    // split into folder and filename
    int splitIndex = path.lastIndexOf('/');
    String folder = splitIndex > 0 ? path.substring(0, splitIndex) : "/";
    String filename = splitIndex > 0 ? path.substring(splitIndex + 1, path.length()) : null;

    if ( filename == null || filename.isEmpty() ) {
      throw new IllegalArgumentException( "Invalid path: " + path );
    }

    //
    String address = reposURL + "/api/repo/files/import";
    WebClient client = WebClient.create(address).header(HttpHeaders.AUTHORIZATION, "Basic YWRtaW46cGFzc3dvcmQ=");
    client.type("multipart/form-data").accept("text/plain");

    List<Attachment> atts = new ArrayList<>();

    // importDir
    atts.add(formDataField("importDir", folder));

    // fileUpload
    atts.add( new Attachment( "fileUpload", contents, new ContentDisposition( "form-data; name=\"fileUpload\"" ) ) );

    // overwriteFile
    atts.add(formDataField("overwriteFile", "true"));

    // fileNameOverride
    atts.add(formDataField("fileNameOverride", filename));

    MultipartBody body = new MultipartBody(atts);

    //Response response = client.post(body);
    Response response = client.postCollection(atts, Attachment.class);

    return response.getStatus() == Response.Status.OK.getStatusCode();
  }

  @Override
  public boolean saveFile( String path, InputStream contents ) {
    // split into folder and filename
    int splitIndex = path.lastIndexOf('/');
    String folder = splitIndex > 0 ? path.substring(0, splitIndex) : "/";
    String filename = splitIndex > 0 ? path.substring(splitIndex + 1, path.length()) : null;

    if ( filename == null || filename.isEmpty() ) {
      throw new IllegalArgumentException( "Invalid path: " + path );
    }

    // this endpoint requires a different encoding for paths
    String requestURL = createRequestURL("/api/repo/files/import", "", null);

    // create post data
    /*
    List<Attachment> atts = new ArrayList<>();
    atts.add(new Attachment("importPath", MediaType.TEXT_PLAIN, encodedPath));
    atts.add(new Attachment("fileUpload", contents, new ContentDisposition("form-data; name=\"fileUpload\"")));
    atts.add(new Attachment("overwriteFile", MediaType.TEXT_PLAIN, encodedPath));

    MultipartBody body = new MultipartBody(atts);

    Variant var = new Variant();*/

    String boundary = "------------------------d74496d66958873e";
    String CRLF = "\r\n";

    StringBuilder builder = new StringBuilder();

    // importPath
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"importDir\"");
    builder.append(CRLF);
    builder.append(CRLF);
    builder.append(folder);

    // file upload
    builder.append(CRLF);
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"fileUpload\"");
    builder.append(CRLF);
    builder.append(CRLF);
    builder.append("dummy-to-be-completed"); // TODO

    // overwriteFile
    builder.append(CRLF);
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"overwriteFile\"");
    builder.append(CRLF);
    builder.append(CRLF);
    builder.append("true");

    // fileNameOverride
    builder.append(CRLF);
    builder.append(boundary);
    builder.append(CRLF);
    builder.append("Content-Disposition: form-data; name=\"fileNameOverride\"");
    builder.append(CRLF);
    builder.append(CRLF);
    builder.append(filename);

    // end multipart
    builder.append(CRLF);
    builder.append(boundary);
    builder.append("--");
    builder.append(CRLF);

    String body = builder.toString();

    Response response = client.target( requestURL )
        .request()
        .post(Entity.entity(body, "multipart/form-data; boundary=" + boundary)); /* TODO: check what gets sent */

    /*


    FormDataMultiPart form = new FormDataMultiPart();
    form.field( "importPath", encodedPath );
    form.field( "fileUpload", contents, MediaType.APPLICATION_OCTET_STREAM_TYPE );
    form.field( "overwriteFile", "true" );

    Response response = client.register(MultiPartFeature.class)
        .target( requestURL )
        .request()
        .post(Entity.entity((MultiPart) form, MediaType.MULTIPART_FORM_DATA_TYPE), Response.class);

    //TODO: handle non-OK status codes? log? exception?
    return response.getStatus() == Response.Status.OK.getStatusCode();
    */
    return response.getStatus() == Response.Status.OK.getStatusCode();
  }

  @Override
  public boolean copyFile(String pathFrom, String pathTo) {
    try {
      InputStream contents = getFileInputStream(pathFrom);
      if (contents == null) return false;

      return saveFile(pathTo, contents);
    } catch (IOException ex) {
      logger.error( ex.getMessage() );
    }
    return false;
  }

  @Override
  public boolean deleteFile(String path) {
    String fileId = remoteFileId( path );

    String requestURL = createRequestURL( "", "delete"); // TODO: delete or deletepermanent
    Response response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .put(Entity.text(fileId));

    //TODO: handle non-OK status codes? log? exception?
    return response.getStatus() == Response.Status.OK.getStatusCode();
  }

  @Override
  public boolean createFolder(String path) {
    String requestURL = createRequestURL("/api/repo/dirs/", path, null);
    Response response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .put(Entity.text(path));

    //TODO: handle non-OK status codes? log? exception?
    return response.getStatus() == Response.Status.OK.getStatusCode();
  }

  @Override
  public boolean createFolder(String path, boolean isHidden) {
    if ( createFolder( path ) ) {
      if ( isHidden ) {
        StringKeyStringValueDto hiddenMeta = new StringKeyStringValueDto();
        hiddenMeta.setKey("_PERM_HIDDEN");
        hiddenMeta.setValue("true");

        List<StringKeyStringValueDto> metadata = new ArrayList<>();
        metadata.add(hiddenMeta);

        String requestURL = createRequestURL(path, "metadata");
        GenericEntity<List<StringKeyStringValueDto>> entity = new GenericEntity<List<StringKeyStringValueDto>>(metadata)
        {
        };
        Response response = client.target(requestURL)
            .request(MediaType.APPLICATION_XML)
            .put(Entity.xml(entity));

        // TODO: handle non-OK status codes? log? exceptions?
        // TODO: revert directory creation???
        return response.getStatus() == Response.Status.OK.getStatusCode();
      }
      return true;
    }
    return false;
  }

  String remoteFileId(String path) {
    String requestURL = createRequestURL(path, "properties");
    RepositoryFileDto properties = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .get(RepositoryFileDto.class);

    if ( properties == null ) return null; //TODO: exception? log?
    return properties.getId();
  }

  // TESTE DEBUG!!!
  @Override
  public boolean fileExists(String path) {
    //createFolder("/home/admin/cpf/newfolder", true);
    //deleteFile("/home/admin/cpf/index.html");
    //copyFile("/home/admin/cpf/index.html", "/home/admin/cpf/copy.html");
    try {
      FileInputStream inputStream = new FileInputStream("C:\\Users\\amartins\\Documents\\sprint_work\\BACKLOG-24375\\teste\\index.html");
      //saveFile("/home/admin/cpf/copy.html", inputStream);
      //saveFileCXF("/home/admin/cpf/copy.html", inputStream);
      saveFileApacheHTTP("/home/admin/cpf/copy.html", inputStream);
    } catch ( IOException ex ) {
      System.out.println(ex.getStackTrace());
    }
    return super.fileExists(path);
  }

}
