/**
 * Copyright 2011-2017 CNES
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
 * VERSION:4.5:FA:FA-2364:27/05/2020:Problèmes rencontres dans le modèle MSIS00
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:660:24/09/2016:add getters to frames configuration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import fr.cnes.sirius.patrius.frames.configuration.FrameConvention;
import fr.cnes.sirius.patrius.frames.configuration.eop.PoleCorrection;
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
 * Compute tidal correction to the pole motion.
 * 
 * <p>
 * This class computes the diurnal and semidiurnal variations in the Earth orientation. It is a java translation of the
 * fortran subroutine found at ftp://tai.bipm.org/iers/conv2003/chapter8/ortho_eop.f.
 * </p>
 * 
 * <b>This class has been adapted from the TidalCorrection Orekit class.</b>
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: TidalCorrectionCache.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.2
 */
class TidalCorrectionCache implements TidalCorrectionModel {

    /** Serial UID. */
    private static final long serialVersionUID = -2821120317271237601L;

    /** Slot size. */
    private static final double SLOT_SIZE = 30;

    /** 32. */
    private static final double THIRTYTWO = 32.;

    /** 8 */
    private static final int C_8 = 8;

    /** 3. */
    private static final double C_3 = 3;

    /** Interpolation points. */
    private final int interpolationPoints;

    /** Synchronization token. */
    private final Object token = new Object();

    /** Current set. */
    private TidalCorrection current;

    /** Cache for pole and time correction data. */
    private final TimeStampedCache<TidalCorrection> cache;

    /** Tides correction model. */
    private final TidalCorrectionModel model;

    /**
     * @param tcModel
     *        Tides correction model to use
     */
    public TidalCorrectionCache(final TidalCorrectionModel tcModel) {
        this(tcModel, C_3 / THIRTYTWO * Constants.JULIAN_DAY, C_8);
    }

    /**
     * @param tcModel
     *        Tides correction model to use
     * @param span
     *        time spane between interpolation points
     * @param interpolationPointsIn
     *        number of interpolation points to use
     */
    public TidalCorrectionCache(final TidalCorrectionModel tcModel, final double span,
        final int interpolationPointsIn) {
        this.interpolationPoints = interpolationPointsIn;
        this.model = tcModel;
        final TidalCorrectionGenerator generator = new TidalCorrectionGenerator(this.model, interpolationPointsIn,
                span);
        this.cache = new TimeStampedCache<TidalCorrection>(interpolationPointsIn,
            PatriusConfiguration.getCacheSlotsNumber(), Constants.JULIAN_YEAR, SLOT_SIZE * Constants.JULIAN_DAY,
            generator, TidalCorrection.class);
        this.current = computeTidesCorrection(AbsoluteDate.J2000_EPOCH);
    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(date.durationFrom(this.current.getDate())) < Precision.EPSILON) {
                return this.current.getPoleCorrection();
            } else {
                this.current = this.computeTidesCorrection(date);
                return this.current.getPoleCorrection();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1Correction(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(date.durationFrom(this.current.getDate())) < Precision.EPSILON) {
                return this.current.getUT1Correction();
            } else {
                this.current = this.computeTidesCorrection(date);
                return this.current.getUT1Correction();
            }
        }
    }

    /**
     * Compute tidal correction data at given date.
     * 
     * @param date
     *        date at which to compute tides correction data
     * @return a {@link TidalCorrection} set for the given date
     */
    private TidalCorrection computeTidesCorrection(final AbsoluteDate date) {

        try {

            // get neighbours for interpolation
            final TidalCorrection[] neighbours = this.cache.getNeighbors(date);

            // starting date
            final AbsoluteDate startingDate = neighbours[0].getDate();

            // elapsed duration from n0
            final double dt = date.durationFrom(startingDate);

            // interpolation arrays
            final NevilleInterpolator interp = new NevilleInterpolator();

            final double[] ts = new double[this.interpolationPoints];
            final double[] dxs = new double[this.interpolationPoints];
            final double[] dys = new double[this.interpolationPoints];
            final double[] ut1Mutcs = new double[this.interpolationPoints];
            final double[] lods = new double[this.interpolationPoints];
            for (int i = 0; i < this.interpolationPoints; i++) {
                ts[i] = neighbours[i].getDate().durationFrom(startingDate);
                dxs[i] = neighbours[i].getPoleCorrection().getXp();
                dys[i] = neighbours[i].getPoleCorrection().getYp();
                ut1Mutcs[i] = neighbours[i].getUT1Correction();
                lods[i] = neighbours[i].getLODCorrection();
            }

            // interpolated results
            final double dx = interp.interpolate(ts, dxs).value(dt);
            final double dy = interp.interpolate(ts, dys).value(dt);
            final double ut1Mutc = interp.interpolate(ts, ut1Mutcs).value(dt);
            final double lod = interp.interpolate(ts, lods).value(dt);

            return new TidalCorrection(date, new PoleCorrection(dx, dy), ut1Mutc, lod);
        } catch (final TimeStampedCacheException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getLODCorrection(final AbsoluteDate date) {
        synchronized (this.token) {
            if (MathLib.abs(date.durationFrom(this.current.getDate())) < Precision.EPSILON) {
                return this.current.getLODCorrection();
            } else {
                this.current = this.computeTidesCorrection(date);
                return this.current.getLODCorrection();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public FrameConvention getOrigin() {
        return this.model.getOrigin();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDirect() {
        return false;
    }
}
