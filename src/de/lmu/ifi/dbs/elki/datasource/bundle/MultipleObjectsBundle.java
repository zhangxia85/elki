package de.lmu.ifi.dbs.elki.datasource.bundle;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * This class represents a set of "packaged" objects, which is a transfer
 * container for objects e.g. from parsers to a database. It contains the object
 * with multiple representations outside of any index structure.
 * 
 * @author Erich Schubert
 */
public class MultipleObjectsBundle implements ObjectBundle {
  /**
   * Storing the meta data.
   */
  private BundleMeta meta;

  /**
   * Storing the real contents.
   */
  private List<List<Object>> columns;

  /**
   * Constructor.
   * 
   * @param meta Meta data contained.
   * @param columns Content in columns
   */
  public MultipleObjectsBundle(BundleMeta meta, List<List<Object>> columns) {
    super();
    this.meta = meta;
    this.columns = columns;
    if(this.columns.size() != this.meta.size()) {
      throw new AbortException("Meta size and columns do not agree!");
    }
    int len = -1;
    for(List<Object> col : columns) {
      if(len < 0) {
        len = col.size();
      }
      else {
        if(col.size() != len) {
          throw new AbortException("Column lengths do not agree.");
        }
      }
    }
  }

  @Override
  public BundleMeta meta() {
    return meta;
  }

  @Override
  public SimpleTypeInformation<?> meta(int i) {
    return meta.get(i);
  }

  @Override
  public int metaLength() {
    return meta.size();
  }

  @Override
  public Object data(int onum, int rnum) {
    if(rnum < 0 || rnum >= meta.size()) {
      throw new ArrayIndexOutOfBoundsException();
    }
    return columns.get(rnum).get(onum);
  }

  @Override
  public int dataLength() {
    try {
      return columns.get(0).size();
    }
    catch(IndexOutOfBoundsException e) {
      return 0;
    }
  }

  /**
   * Append a new record to the data set. Pay attention to having the right
   * number of values!
   * 
   * @param data Data to append
   */
  public void appendSimple(Object... data) {
    if(data.length != meta.size()) {
      throw new AbortException("Invalid number of attributes in 'append'.");
    }
    for(int i = 0; i < data.length; i++) {
      columns.get(i).add(data[i]);
    }
  }

  /**
   * Get the raw objects columns. Use with caution!
   * 
   * @param i column number
   * @return the ith column
   */
  public List<Object> getColumn(int i) {
    return columns.get(i);
  }
}