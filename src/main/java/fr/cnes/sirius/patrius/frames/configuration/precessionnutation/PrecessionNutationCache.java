/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2364:27/05/2020:Problèmes rencontres dans le modèle MSIS00
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.math.analysis.interpolation.NevilleInterpolator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * Cache for precession nutation correction computation.
 * 
 * <p>
 * This implementation includes a caching/interpolation feature to tremendously improve efficiency. The IAU-2000 model
 * involves lots of terms (1600 components for x, 1275 components for y and 66 components for s). Recomputing all these
 * components for each point is really slow. The shortest period for these components is about 5.5 days (one fifth of
 * the moon revolution period), hence the pole motion is smooth at the day or week scale. This implies that these
 * motions can be computed accurately using a few reference points per day or week and interpolated between these
 * points. This implementation uses 12 points separated by 1/2 day (43200 seconds) each, the resulting maximal
 * interpolation error on the frame is about 1.3&times;10<sup>-10</sup> arcseconds. <i>-- Orekit </i>
 * </p>
 * 
 * <p>
 * This class has been adapted from the CIRF2000Frame Orekit class.
 * </p>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: PrecessionNutationCache.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
public class PrecessionNutationCache implements PrecessionNutationModel {

     /** Serializable UID. */
    private static final long serialVersionUID = -9099446578781877240L;

    /** Default number of interpolation points. */
    private static final int DEFAULT_INTERP_POINTS = 12;

    /** Default time span between generated reference points. */
    private static final double DEFAULT_SPAN = 43200.;

    /** Slot size. */
    private static final double SLOT_SIZE = 30;

    /** Token for synchronized access to current sets. */
    private final Object token = new Object();

    /** Time stamped cache of coordinates. */
    private final TimeStampedCache<CIPCoordinates> cache;

    /** Current set. */
    private CIPCoordinates currentSet = null;

    /** Model. */
    private PrecessionNutationModel model = null;

    /**
     * @param pnModel
     *        IERS model to use
     * @param span
     *        time spane between interpolation points
     * @param interpolationPoints
     *        number of interpolation points to use
     * 
     */
    public PrecessionNutationCache(final PrecessionNutationModel pnModel, final double span,
        final int interpolationPoints) {

        // store parameters
        this.model = pnModel;
        final CIPCoordinatesGenerator generator = new CIPCoordinatesGenerator(this.model, interpolationPoints, span);
        this.cache = new TimeStampedCache<>(interpolationPoints, PatriusConfiguration.getCacheSlotsNumber(),
            Constants.JULIAN_YEAR, SLOT_SIZE * Constants.JULIAN_DAY, generator, CIPCoordinates.class);
        this.currentSet = getPoleCoordinates(AbsoluteDate.J2000_EPOCH);
    }

    /**
     * @param pnModel
     *        IERS model to use
     */
    public PrecessionNutationCache(final PrecessionNutationModel pnModel) {
        this(pnModel, DEFAULT_SPAN, DEFAULT_INTERP_POINTS);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCIPMotion(final AbsoluteDate date) {

        // container
        final double[] result;

        // synchronized access to already computed set
        synchronized (this.token) {
            // if dates equate, return the current set, otherwise recompute
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                result = this.currentSet.getCIPMotion();
            } else {
                this.currentSet = this.getPoleCoordinates(date);
                result = this.currentSet.getCIPMotion();
            }
        }

        return result;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCIPMotionTimeDerivative(final AbsoluteDate date) {

        // synchronized access to already computed set
        synchronized (this.token) {
            // if dates equate, return the current set, otherwise recompute
            if (MathLib.abs(this.currentSet.getDate().durationFrom(date)) < Precision.EPSILON) {
                return this.currentSet.getCIPMotionTimeDerivatives();
            }
            this.currentSet = this.getPoleCoordinates(date);
            return this.currentSet.getCIPMotionTimeDerivatives();
        }
    }

    /**
     * Get Pole Coordinates.
     * 
     * @param date
     *        for pole coordinates
     * @return pole coordinates
     */
    private CIPCoordinates getPoleCoordinates(final AbsoluteDate date) {

        // call generator if direct computation required, otherwise interpolate using cached data
        try {
            final CIPCoordinates[] neighbours = this.cache.getNeighbors(date);
            final int n = neighbours.length;

            // starting date
            final AbsoluteDate startingDate = neighbours[0].getDate();

            // elapsed duration from n0
            final double dt = date.durationFrom(startingDate);

            // interpolation arrays
            final NevilleInterpolator interp = new NevilleInterpolator();

            // Initialize arrays
            final double[] ts = new double[n];
            final double[] xs = new double[n];
            final double[] ys = new double[n];
            final double[] ss = new double[n];
            final double[] xps = new double[n];
            final double[] yps = new double[n];
            final double[] sps = new double[n];

            for (int i = 0; i < n; i++) {
                // Loop on data size
                ts[i] = neighbours[i].getDate().durationFrom(startingDate);
                xs[i] = neighbours[i].getX();
                ys[i] = neighbours[i].getY();
                ss[i] = neighbours[i].getS();
                xps[i] = neighbours[i].getxP();
                yps[i] = neighbours[i].getyP();
                sps[i] = neighbours[i].getsP();
            }

            // interpolated results
            final double x = interp.interpolate(ts, xs).value(dt);
            final double y = interp.interpolate(ts, ys).value(dt);
            final double s = interp.interpolate(ts, ss).value(dt);
            final double xP = interp.interpolate(ts, xps).value(dt);
            final double yP = interp.interpolate(ts, yps).value(dt);
            final double sP = interp.interpolate(ts, sps).value(dt);

            // Return result
            return new CIPCoordinates(date, x, xP, y, yP, s, sP);
        } catch (final TimeStampedCacheException e) {
            throw new PatriusExceptionWrapper(e);
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.model.getOrigin();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isConstant() {
        return this.model.isConstant();
    }

}
