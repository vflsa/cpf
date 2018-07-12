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

import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.pentaho.ctools.cpf.repository.rca.dto.RepositoryFileDto;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IBasicFileFilter;
import pt.webdetails.cpf.repository.api.IReadAccess;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class {@code RemoteReadAccess} provides an implementation of {@code IReadAccess} via REST calls to the Pentaho Server.
 *
 * @see IReadAccess
 */
public class RemoteReadAccess implements IReadAccess {
  Client client;

  String reposURL;
  private static final String DEFAULT_PATH_SEPARATOR = "/";

  public RemoteReadAccess( String reposURL ) {
    this.reposURL = reposURL;
    client = ClientBuilder.newClient().register( new HttpBasicAuthFilter( "admin", "password" ) );
  }

  @Override
  public InputStream getFileInputStream( String path ) throws IOException {
    String requestURL = createRequestURL( path, "" );
    InputStream responseData = client.target( requestURL )
        .request( MediaType.APPLICATION_OCTET_STREAM_TYPE )
        .get( InputStream.class );

    return responseData;
  }

  @Override
  public boolean fileExists( String path ) {
    String requestURL = createRequestURL( path, "properties" );
    RepositoryFileDto response = client.target( requestURL )
        .request( MediaType.APPLICATION_XML )
        .get( RepositoryFileDto.class );
    return response != null;
  }

  @Override
  public long getLastModified( String path ) {
    String requestURL = createRequestURL( path, "properties" );
    RepositoryFileDto response = client.target( requestURL )
        .request( MediaType.APPLICATION_XML )
        .get( RepositoryFileDto.class );
    return response.getLastModifiedDate().getTime();
  }

  @Override
  public List<IBasicFile> listFiles( String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs, boolean showHiddenFilesAndFolders ) {
    String requestURL = createRequestURL( path, "children" );
    List<RepositoryFileDto> response = client.target( requestURL )
        .request( MediaType.APPLICATION_XML )
        .get( new GenericType<List<RepositoryFileDto>>() { } );
    if ( response == null ) {
      return null;
    }

    return response
        .stream()
        .filter( dto -> ( includeDirs || !dto.isFolder() ) && ( showHiddenFilesAndFolders || !dto.isHidden() ) )
        .map( dto -> new RemoteBasicFile( this, dto ) )
        .filter( filter::accept )
        .collect( Collectors.toList() );
  }

  @Override
  public List<IBasicFile> listFiles( String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs ) {
    return listFiles( path, filter, maxDepth, includeDirs, false );
  }

  @Override
  public List<IBasicFile> listFiles( String path, IBasicFileFilter filter, int maxDepth ) {
    return listFiles( path, filter, maxDepth, false );
  }

  @Override
  public List<IBasicFile> listFiles( String path, IBasicFileFilter filter ) {
    return listFiles( path, filter, -1 );
  }

  @Override
  public IBasicFile fetchFile( String path ) {
    String requestURL = createRequestURL( path, "properties" );
    RepositoryFileDto response = client.target( requestURL )
        .request( MediaType.APPLICATION_XML )
        .get( RepositoryFileDto.class );
    return new RemoteBasicFile( this, response );
  }

  static String encodePath( String path ) {
    return path.replaceAll( "/", ":" );
  }

  String createRequestURL( String path, String method ) {
    return createRequestURL( "/api/repo/files/", path, method );
  }

  String createRequestURL( String endpoint, String path, String method ) {
    if ( method != null ) {
      return reposURL + endpoint + encodePath( path ) + DEFAULT_PATH_SEPARATOR + method;
    }
    return reposURL + endpoint + encodePath( path );
  }
}
