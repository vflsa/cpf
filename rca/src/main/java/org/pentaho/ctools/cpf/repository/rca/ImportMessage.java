package org.pentaho.ctools.cpf.repository.rca;

import java.io.InputStream;

public class ImportMessage {
  public String importDir;
  public boolean overwrite;
  public String filename;
  public InputStream contents;

  public ImportMessage(String importDir, String filename, InputStream contents, boolean overwrite) {
    this.importDir = importDir;
    this.filename = filename;
    this.contents = contents;
    this.overwrite = overwrite;
  }

  @Override
  public String toString() {
    return "ImportMessage{ " + importDir + "/" + filename + ", overwrite=" + overwrite + "}";
  }
}
