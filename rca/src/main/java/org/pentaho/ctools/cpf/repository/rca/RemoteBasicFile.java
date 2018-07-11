package org.pentaho.ctools.cpf.repository.rca;

import org.pentaho.ctools.cpf.repository.rca.dto.RepositoryFileDto;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IReadAccess;

import java.io.IOException;
import java.io.InputStream;

public class RemoteBasicFile implements IBasicFile {
  IReadAccess remote;
  RepositoryFileDto repositoryFile;

  public RemoteBasicFile(IReadAccess remote, RepositoryFileDto dto) {
    this.remote = remote;
    repositoryFile = dto;
  }

  @Override
  public InputStream getContents() throws IOException {
    return remote.getFileInputStream(repositoryFile.getPath());
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
