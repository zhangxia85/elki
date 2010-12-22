package de.lmu.ifi.dbs.elki.result.textwriter.naming;

import de.lmu.ifi.dbs.elki.data.Cluster;

/**
 * Naming scheme implementation for clusterings.
 * 
 * @author Erich Schubert
 *
 * @apiviz.uses Cluster
 */
public interface NamingScheme {
  /**
   * Retrieve a name for the given cluster.
   * 
   * @param cluster cluster to get a name for
   * @return cluster name
   */
  public String getNameFor(Cluster<?> cluster);
}