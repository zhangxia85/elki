package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.Collection;
import java.util.Iterator;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.result.ReferencePointsResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * The actual visualization instance, for a single projection
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ReferencePointsResult oneway - - visualizes
 */
// TODO: add a result listener for the reference points.
public class ReferencePointsVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * Generic tag to indicate the type of element. Used in IDs, CSS-Classes etc.
   */
  public static final String REFPOINT = "refpoint";

  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Reference Points";

  /**
   * Serves reference points.
   */
  protected ReferencePointsResult<NV> result;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public ReferencePointsVisualization(VisualizationTask task) {
    super(task);
    this.result = task.getResult();
    incrementalRedraw();
  }

  @Override
  public void redraw() {
    setupCSS(svgp);
    Iterator<NV> iter = result.iterator();

    final double dotsize = context.getStyleLibrary().getSize(StyleLibrary.REFERENCE_POINTS);
    while(iter.hasNext()) {
      NV v = iter.next();
      double[] projected = proj.fastProjectDataToRenderSpace(v);
      Element dot = svgp.svgCircle(projected[0], projected[1], dotsize);
      SVGUtil.addCSSClass(dot, REFPOINT);
      layer.appendChild(dot);
    }
  }

  /**
   * Registers the Reference-Point-CSS-Class at a SVGPlot.
   * 
   * @param svgp the SVGPlot to register the -CSS-Class.
   */
  private void setupCSS(SVGPlot svgp) {
    CSSClass refpoint = new CSSClass(svgp, REFPOINT);
    refpoint.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getColor(StyleLibrary.REFERENCE_POINTS));
    svgp.addCSSClassOrLogError(refpoint);
  }

  /**
   * Generates a SVG-Element visualizing reference points.
   * 
   * @author Remigius Wojdanowski
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses ReferencePointsVisualization oneway - - «create»
   * 
   * @param <NV> Type of the DatabaseObject being visualized.
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends AbstractVisFactory<NV> {
    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super();
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, Result result) {
      if(!VisualizerUtil.isNumberVectorDatabase(context.getDatabase())) {
        return;
      }
      Collection<ReferencePointsResult<NV>> rps = ResultUtil.filterResults(result, ReferencePointsResult.class);
      for(ReferencePointsResult<NV> rp : rps) {
        final VisualizationTask task = new VisualizationTask(NAME, context, rp, this);
        task.put(VisualizationTask.META_LEVEL, VisualizationTask.LEVEL_DATA);
        context.addVisualizer(rp, task);
      }
    }

    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new ReferencePointsVisualization<NV>(task);
    }

    @Override
    public Object getVisualizationType() {
      return P2DVisualization.class;
    }
  }
}