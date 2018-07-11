package org.pentaho.ctools.cpf.repository.bundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.ctools.cpf.repository.factory.ContentAccessFactory;
import pt.webdetails.cpf.repository.api.IBasicFile;
import pt.webdetails.cpf.repository.api.IBasicFileFilter;
import pt.webdetails.cpf.repository.api.IRWAccess;
import pt.webdetails.cpf.repository.api.IReadAccess;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DummyReadWriteAccess implements IReadAccess, IRWAccess {
  IReadAccess readAccess;
  private static final Log logger = LogFactory.getLog( ContentAccessFactory.class );

  public DummyReadWriteAccess(IReadAccess readAccess) {
    this.readAccess = readAccess;
  }

  @Override
  public boolean saveFile(String path, InputStream contents) {
    logger.info( "faked saveFile for the OSGi environment" );
    return true;
  }

  @Override
  public boolean copyFile(String pathFrom, String pathTo) {
    logger.info( "faked copyFile for the OSGi environment" );
    return true;
  }

  @Override
  public boolean deleteFile(String path) {
    logger.info( "faked deleteFile for the OSGi environment" );
    return true;
  }

  @Override
  public boolean createFolder(String path) {
    logger.info( "faked createFile for the OSGi environment" );
    return true;
  }

  @Override
  public boolean createFolder(String path, boolean isHidden) {
    logger.info( "faked createFile for the OSGi environment" );
    return true;
  }

  @Override
  public InputStream getFileInputStream(String path) throws IOException {
    return readAccess.getFileInputStream( path );
  }

  @Override
  public boolean fileExists( String path ) {
    return readAccess.fileExists( path );
  }

  @Override
  public long getLastModified(String path) {
    return readAccess.getLastModified( path );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs, boolean showHiddenFilesAndFolders) {
    return readAccess.listFiles( path, filter, maxDepth, includeDirs, showHiddenFilesAndFolders );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth, boolean includeDirs) {
    return readAccess.listFiles( path, filter, maxDepth, includeDirs );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter, int maxDepth) {
    return readAccess.listFiles( path, filter, maxDepth );
  }

  @Override
  public List<IBasicFile> listFiles(String path, IBasicFileFilter filter) {
    return readAccess.listFiles( path, filter );
  }

  @Override
  public IBasicFile fetchFile(String path) {
    return readAccess.fetchFile( path );
  }
}
