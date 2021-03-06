/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.calcite.adapter.csv;

import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.impl.AbstractTable;
import org.apache.commons.vfs.FileSystemException;
import org.apache.commons.vfs.FileSystemManager;
import org.apache.commons.vfs.VFS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for table that reads CSV files.
 */
public abstract class CsvTable extends AbstractTable {
  //Using an inputstream instead of a file, so we're able to read data from any source
  protected final InputStream inputStream; 
  private final RelProtoDataType protoRowType;
  protected List<CsvFieldType> fieldTypes;
  protected File file;

  /** Creates a CsvAbstractTable. */
  CsvTable(File file, RelProtoDataType protoRowType) {
    this.file = file;
    
    try {
      this.inputStream = new FileInputStream(file);
    } catch (FileNotFoundException e) {
      throw new CsvTableException(e);
    }
    
    this.protoRowType = protoRowType;
    
    setupInputStream();    
  }

  CsvTable(String uri, RelProtoDataType protoRowType) {
    this.protoRowType = protoRowType;

    ApacheVfsVirtualFileHandler fileHandler = new ApacheVfsVirtualFileHandler();
    try {
      this.inputStream = fileHandler.readVirtualFile(uri);
    } catch (FileSystemException e) {
      throw new CsvTableException(e);
    }
    
    setupInputStream();
  }
  
  private void setupInputStream() {
    this.inputStream.mark(Integer.MAX_VALUE);
  }

  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    if (protoRowType != null) {
      return protoRowType.apply(typeFactory);
    }
    if (fieldTypes == null) {
      fieldTypes = new ArrayList<CsvFieldType>();
      
      if (file != null) {
        CsvEnumerator.setFile(file);
        return CsvEnumerator.deduceRowType((JavaTypeFactory) typeFactory, file, fieldTypes);
      }
      
      CsvEnumerator.setFile(null);
      return CsvEnumerator.deduceRowType((JavaTypeFactory) typeFactory, inputStream, fieldTypes);
    } else {
      if (file != null) {
        CsvEnumerator.setFile(file);
        return CsvEnumerator.deduceRowType((JavaTypeFactory) typeFactory, file, null);
      }
      
      CsvEnumerator.setFile(null);
      return CsvEnumerator.deduceRowType((JavaTypeFactory) typeFactory, inputStream, null);
    }
  }

  /** Various degrees of table "intelligence". */
  public enum Flavor {
    SCANNABLE, FILTERABLE, TRANSLATABLE
  }
  
  public static class CsvTableException extends RuntimeException {
    public CsvTableException(Throwable throwable) {
      super(throwable);
    }
  }
}

// End CsvTable.java
