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

public class RemoteReadAccess implements IReadAccess {
  Client client;

  static final String reposURL = "http://localhost:8080/pentaho";
  static private final String DEFAULT_PATH_SEPARATOR = "/";

  public RemoteReadAccess() {
    client = ClientBuilder.newClient().register(new HttpBasicAuthFilter("admin", "password"));
  }

  @Override
  public InputStream getFileInputStream(String path) throws IOException {
    IBasicFile file = fetchFile(path);
    return file.getContents();
  }

  @Override
  public boolean fileExists(String path) {
    String requestURL = createRequestURL(path, "properties");
    RepositoryFileDto response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .get(RepositoryFileDto.class);
    return response != null;
  }

  @Override
  public long getLastModified(String path) {
    String requestURL = createRequestURL(path, "properties");
    RepositoryFileDto response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .get(RepositoryFileDto.class);
    return response.getLastModifiedDate().getTime();
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs, boolean showHiddenFilesAndFolders) {
    String requestURL = createRequestURL(path, "children");
    List<RepositoryFileDto> response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .get(new GenericType<List<RepositoryFileDto>>(){});
    if (response == null) return null;
    return response.stream().map(dto -> new RemoteBasicFile(client, dto)).collect(Collectors.toList());
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs) {
    return listFiles( path, filter, maxDepth, includeDirs, false );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth) {
    return listFiles( path, filter, maxDepth, false );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter) {
    return listFiles( path, filter, -1 );
  }

  @Override
  public IBasicFile fetchFile(String path) {
    String requestURL = createRequestURL(path, "properties");
    RepositoryFileDto response = client.target(requestURL)
        .request(MediaType.APPLICATION_XML)
        .get(RepositoryFileDto.class);
    return new RemoteBasicFile(client, response);
  }

  static String createRequestURL(String path, String method) {
    path = path.startsWith(DEFAULT_PATH_SEPARATOR) ? path : DEFAULT_PATH_SEPARATOR + path;
    return reposURL + "/api/repo/files" + path + DEFAULT_PATH_SEPARATOR + method;
  }
}
