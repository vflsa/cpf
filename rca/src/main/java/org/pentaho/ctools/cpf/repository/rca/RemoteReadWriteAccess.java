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
import org.pentaho.ctools.cpf.repository.rca.dto.RepositoryFileDto;
import org.pentaho.ctools.cpf.repository.rca.dto.StringKeyStringValueDto;
import pt.webdetails.cpf.repository.api.IRWAccess;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;

public class RemoteReadWriteAccess extends RemoteReadAccess implements IRWAccess {
  private static final Log logger = LogFactory.getLog( RemoteReadWriteAccess.class );

  public RemoteReadWriteAccess(String reposURL) {
    super(reposURL);
  }

  @Override
  public boolean saveFile(String path, InputStream contents) {
    String requestURL = createRequestURL( path, null);

    Response response = client.target(requestURL)
        .request(MediaType.APPLICATION_OCTET_STREAM)
        .put(Entity.entity(contents, MediaType.APPLICATION_OCTET_STREAM));

    //TODO: handle non-OK status codes? log? exception?
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

  /*
  // TESTE DEBUG!!!
  @Override
  public boolean fileExists(String path) {
    //createFolder("/home/admin/cpf/newfolder", true);
    //deleteFile("/home/admin/cpf/index.html");
    //copyFile("/home/admin/cpf/index.html", "/home/admin/cpf/copy.html");
    return super.fileExists(path);
  }
  */
}
