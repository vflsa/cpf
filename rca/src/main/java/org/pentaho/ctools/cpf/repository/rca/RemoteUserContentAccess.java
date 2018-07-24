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
import pt.webdetails.cpf.api.IFileContent;
import pt.webdetails.cpf.api.IUserContentAccessExtended;
import pt.webdetails.cpf.repository.api.FileAccess;

import java.io.IOException;

public class RemoteUserContentAccess extends RemoteReadWriteAccess implements IUserContentAccessExtended {

  private static final Log logger = LogFactory.getLog( RemoteReadWriteAccess.class );

  public RemoteUserContentAccess( String reposURL, String username, String password ) {
    super( reposURL, username, password );
  }

  @Override
  public boolean hasAccess( String filePath, FileAccess access ) {
    //TODO: dummy implementation
    return fileExists( filePath );
  }

  @Override
  public boolean saveFile( IFileContent file ) {
    //TODO: dummy implementation
    try {
      return saveFile( file.getPath(), file.getContents() );
    } catch ( IOException ex ) {
      logger.error( ex );
      return false;
    }
  }
}
