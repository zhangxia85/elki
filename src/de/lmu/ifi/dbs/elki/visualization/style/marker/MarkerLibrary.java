package de.lmu.ifi.dbs.elki.visualization.style.marker;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

/**
 * A marker library is a class that can generate and draw various styles of markers.
 * Different uses might require different marker libraries (e.g. full screen, thumbnail, print)
 * 
 * @author Erich Schubert
 *
 * @apiviz.uses Element oneway - - «create»
 */
public interface MarkerLibrary {
  /**
   * Insert a marker at the given coordinates.
   * Markers will be defined in the defs part of the document, and then SVG-"use"d at the
   * given coordinates. This supposedly is more efficient and significantly reduces file size.
   * Symbols will be named "s0", "s1" etc.; these names must not be used by other elements in
   * the SVG document!
   * 
   * @param plot Plot to draw on
   * @param parent parent node
   * @param x coordinate
   * @param y coordinate
   * @param style style (enumerated)
   * @param size size
   * @return Element node generated.
   */
  public Element useMarker(SVGPlot plot, Element parent, double x, double y, int style, double size);
}