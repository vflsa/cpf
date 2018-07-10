package org.pentaho.ctools.cpf.repository.rca;

import org.pentaho.ctools.cpf.repository.rca.dto.RepositoryFileDto;
import pt.webdetails.cpf.repository.api.IBasicFile;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

public class RemoteBasicFile implements IBasicFile {
  Client client;
  RepositoryFileDto repositoryFile;

  public RemoteBasicFile(Client client, RepositoryFileDto dto) {
    this.client = client;
    repositoryFile = dto;
  }

  @Override
  public InputStream getContents() throws IOException {
    String requestURL = RemoteReadAccess.createRequestURL(repositoryFile.getPath(), "");
    InputStream responseData = client.target(requestURL)
        .request(MediaType.APPLICATION_OCTET_STREAM_TYPE)
        .get(InputStream.class);

    return responseData;
  }

  @Override
  public String getName() {
    return repositoryFile.getName();
  }

  @Override
  public String getFullPath() {
    return repositoryFile.getPath();
  }

  @Override
  public String getPath() {
    return repositoryFile.getPath();
  }

  @Override
  public String getExtension() {
    final String path = repositoryFile.getPath();
    if ( path.length() == 0 ) {
      return path;
    }
    final int index = path.lastIndexOf( "." );
    if ( index == -1 ) {
      return "";
    }
    return path.substring( index + 1 );
  }

  @Override
  public boolean isDirectory() {
    return repositoryFile.isFolder();
  }
}
