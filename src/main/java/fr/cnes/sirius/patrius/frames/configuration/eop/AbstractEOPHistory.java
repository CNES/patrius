/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
/*
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Added possibility of storing UT1-TAI instead of UT1-UTC
 * VERSION::FA:831:25/01/2017:computation times optimization for calls outside interval)
 * VERSION::FA:979:01/09/2017:useless management of TUC jump in getUT1MinusTAI
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.time.TimeStampedCache;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * 
 * @author Pascal Parraud
 */
public abstract class AbstractEOPHistory implements Serializable, EOPHistory {

    /** Serializable UID. */
    private static final long serialVersionUID = 5659073889129159070L;

    /** Cache span (in days). */
    private static final double CACHE_SPAN = 30;

    /** Number of points to use in interpolation. */
    private final int interpolationPoints;

    /** Earth Orientation Parameter entries. */
    private final SortedSet<TimeStamped> entries;

    /** EOP history entries. */
    private final TimeStampedCache<EOPEntry> cache;

    /** Interpolation method. */
    private final EOPInterpolators interpolator;

    /**
     * Simple constructor.
     * 
     * @param interpMethod
     *        EOP interpolation method
     */
    protected AbstractEOPHistory(final EOPInterpolators interpMethod) {
        this.interpolator = interpMethod;
        switch (this.interpolator) {
            case LAGRANGE4:
                this.interpolationPoints = 4;
                break;
            case LINEAR:
            default:
                this.interpolationPoints = 2;
                break;
        }
        this.entries = new TreeSet<TimeStamped>(new ChronologicalComparator());
        this.cache = new TimeStampedCache<EOPEntry>(this.interpolationPoints,
            PatriusConfiguration.getCacheSlotsNumber(), Constants.JULIAN_YEAR,
            CACHE_SPAN * Constants.JULIAN_DAY,
            new Generator(), EOPEntry.class);
    }

    /** {@inheritDoc} */
    @Override
    public void addEntry(final EOPEntry entry) {
        this.entries.add(entry);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<TimeStamped> iterator() {
        return this.entries.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.cache.getEntries();
    }

    /**
     * Returns true if no EOP have been loaded.
     * 
     * @return true if no EOP have been loaded
     */
    protected boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /**
     * Returns first EOP entry.
     * 
     * @return first EOP entry
     */
    protected EOPEntry getFirst() {
        return (EOPEntry) this.entries.first();
    }

    /**
     * Returns last EOP entry.
     * 
     * @return last EOP entry
     */
    protected EOPEntry getLast() {
        return (EOPEntry) this.entries.last();
    }

    /** {@inheritDoc} */
    @Override
    public EOPInterpolators getEOPInterpolationMethod() {
        return this.interpolator;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getStartDate() {
        return this.entries.first().getDate();
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getEndDate() {
        return this.entries.last().getDate();
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1MinusUTC(final AbsoluteDate date) {
        try {
            return this.getUT1MinusTAI(date) - TimeScalesFactory.getUTC().offsetFromTAI(date);
        } catch (final PatriusException e) {
            throw new PatriusExceptionWrapper(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getUT1MinusTAI(final AbsoluteDate date) {
        try {

            // get the entries surrounding a central date
            final EOPEntry[] neighbors = this.getNeighbors(date);
            final double dutP = neighbors[0].getUT1MinusTAI();
            double interpolated = 0.;

            // apply the interpolation method
            switch (this.interpolator) {
                case LAGRANGE4:
                    // Four neighbors are used to build
                    // a fourth order Lagrange polynomial
                    final double[] dvals = new double[4];
                    final double[] dutvals = new double[4];
                    // If neighbors are less than four, it will fail; but
                    // since it's supposed never to happen, we do not check it.
                    // (the outofbounds exception will do).
                    for (int i = 0; i < 4; i++) {
                        final double dut = neighbors[i].getUT1MinusTAI();
                        dvals[i] = neighbors[i].getDate().durationFrom(date);
                        dutvals[i] = dut;
                    }
                    final PolynomialFunctionLagrangeForm pflf =
                        new PolynomialFunctionLagrangeForm(dvals, dutvals);
                    interpolated = pflf.value(0);
                    break;
                case LINEAR:
                default:
                    final double dutN = neighbors[1].getUT1MinusTAI();
                    final double dt0 = date.durationFrom(neighbors[0].getDate());
                    final double dt1 = neighbors[1].getDate().durationFrom(date);
                    interpolated = (dt0 * dutN + dt1 * dutP) / (dt0 + dt1);
                    break;
            }
            return interpolated;
        } catch (final TimeStampedCacheException tce) {
            // raise an specific exception linked to the cache
            throw new PatriusExceptionWrapper(tce);
        }
    }

    /**
     * Get the entries surrounding a central date.
     * 
     * @param central
     *        central date
     * @return array of cached entries surrounding specified date
     * @exception TimeStampedCacheException
     *            if EOP data cannot be retrieved
     */
    protected EOPEntry[] getNeighbors(final AbsoluteDate central) throws TimeStampedCacheException {
        return this.cache.getNeighbors(central);
    }

    /** {@inheritDoc} */
    @Override
    public double getLOD(final AbsoluteDate date) {
        try {
            double lod = 0.;
            // get the entries surrounding a central date
            final EOPEntry[] neighbors = this.getNeighbors(date);
            // apply the interpolation method
            switch (this.interpolator) {
                case LAGRANGE4:
                    // Four neighbors are used to build
                    // a fourth order Lagrange polynomial
                    final double[] dvals = new double[4];
                    final double[] lvals = new double[4];
                    // If neighbors are less than four, it will fail; but
                    // since it's supposed never to happen, we do not check it.
                    // (the outofbounds exception will do).
                    for (int i = 0; i < 4; i++) {
                        dvals[i] = neighbors[i].getDate().durationFrom(date);
                        lvals[i] = neighbors[i].getLOD();
                    }
                    final PolynomialFunctionLagrangeForm pflf =
                        new PolynomialFunctionLagrangeForm(dvals, lvals);
                    lod = pflf.value(0);
                    break;
                case LINEAR:
                default:
                    final double lod0 = neighbors[0].getLOD();
                    final double lod1 = neighbors[1].getLOD();
                    final double dt0 = date.durationFrom(neighbors[0].getDate());
                    final double dt1 = neighbors[1].getDate().durationFrom(date);
                    lod = (dt0 * lod1 + dt1 * lod0) / (dt0 + dt1);
                    break;
            }
            return lod;
        } catch (final TimeStampedCacheException tce) {
            // raise an specific exception linked to the cache
            throw new PatriusExceptionWrapper(tce);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) throws TimeStampedCacheException {
        PoleCorrection corr = null;
        // Get points around date
        final EOPEntry[] neighbors = this.getNeighbors(date);
        switch (this.interpolator) {
            case LAGRANGE4:
                // Four neighbors are used to build
                // a fourth order Lagrange polynomial
                final double[] dvals = new double[4];
                final double[] xvals = new double[4];
                final double[] yvals = new double[4];
                // If neighbors are less than four, it will fail; but
                // since it's supposed never to happen, we do not check it.
                // (the outofbounds exception will do).
                for (int i = 0; i < 4; i++) {
                    dvals[i] = neighbors[i].getDate().durationFrom(date);
                    xvals[i] = neighbors[i].getX();
                    yvals[i] = neighbors[i].getY();
                }
                final PolynomialFunctionLagrangeForm pflfx =
                    new PolynomialFunctionLagrangeForm(dvals, xvals);
                final PolynomialFunctionLagrangeForm pflfy =
                    new PolynomialFunctionLagrangeForm(dvals, yvals);
                corr = new PoleCorrection(pflfx.value(0), pflfy.value(0));
                break;
            case LINEAR:
            default:
                final double x0 = neighbors[0].getX();
                final double x1 = neighbors[1].getX();
                final double y0 = neighbors[0].getY();
                final double y1 = neighbors[1].getY();
                final double dt0 = date.durationFrom(neighbors[0].getDate());
                final double dt1 = neighbors[1].getDate().durationFrom(date);
                final double x = (dt0 * x1 + dt1 * x0) / (dt0 + dt1);
                final double y = (dt0 * y1 + dt1 * y0) / (dt0 + dt1);
                corr = new PoleCorrection(x, y);
                break;
        }
        return corr;
    }

    /** {@inheritDoc} */
    @Override
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {
        try {
            // Initialiaztion
            NutationCorrection corr = null;
            // Get points around date
            final EOPEntry[] neighbors = this.getNeighbors(date);
            switch (this.interpolator) {
                case LAGRANGE4:
                    // Four neighbors are used to build
                    // a fourth order Lagrange polynomial
                    final double[] dvals = new double[4];
                    final double[] dxvals = new double[4];
                    final double[] dyvals = new double[4];
                    // If neighbors are less than four, it will fail; but
                    // since it's supposed never to happen, we do not check it.
                    // (the outofbounds exception will do).
                    for (int i = 0; i < 4; i++) {
                        dvals[i] = neighbors[i].getDate().durationFrom(date);
                        dxvals[i] = neighbors[i].getDX();
                        dyvals[i] = neighbors[i].getDY();
                    }
                    final PolynomialFunctionLagrangeForm pflfx =
                        new PolynomialFunctionLagrangeForm(dvals, dxvals);
                    final PolynomialFunctionLagrangeForm pflfy =
                        new PolynomialFunctionLagrangeForm(dvals, dyvals);
                    corr = new NutationCorrection(pflfx.value(0), pflfy.value(0));
                    break;
                case LINEAR:
                default:
                    final double x0 = neighbors[0].getDX();
                    final double x1 = neighbors[1].getDX();
                    final double y0 = neighbors[0].getDY();
                    final double y1 = neighbors[1].getDY();
                    final double dt0 = date.durationFrom(neighbors[0].getDate());
                    final double dt1 = neighbors[1].getDate().durationFrom(date);
                    final double x = (dt0 * x1 + dt1 * x0) / (dt0 + dt1);
                    final double y = (dt0 * y1 + dt1 * y0) / (dt0 + dt1);
                    corr = new NutationCorrection(x, y);
                    break;
            }
            return corr;
        } catch (final TimeStampedCacheException tce) {
            throw new PatriusExceptionWrapper(tce);
        }
    }

    /**
     * Check Earth orientation parameters continuity.
     * 
     * @param maxGap
     *        maximal allowed gap between entries (in seconds)
     * @exception PatriusException
     *            if there are holes in the data sequence
     */
    public void checkEOPContinuity(final double maxGap) throws PatriusException {
        TimeStamped preceding = null;
        for (final TimeStamped current : this.entries) {

            // compare the dates of preceding and current entries
            if ((preceding != null) && ((current.getDate().durationFrom(preceding.getDate())) > maxGap)) {
                throw new PatriusException(PatriusMessages.MISSING_EARTH_ORIENTATION_PARAMETERS_BETWEEN_DATES,
                    preceding.getDate(), current.getDate());

            }

            // prepare next iteration
            preceding = current;

        }
    }

    /**
     * Populates a {@link EOPHistory} instance from a collection of {@link EOPEntry}.
     * 
     * @param entries
     *        collection of {@link EOPEntry}
     * @param history
     *        instance to be populated
     */
    public static void fillHistory(final Collection<? extends EOPEntry> entries, final EOPHistory history) {
        for (final EOPEntry entry : entries) {
            history.addEntry(entry);
        }
    }

    /** Local generator for entries. */
    private class Generator implements TimeStampedGenerator<EOPEntry> {

        /** {@inheritDoc} */
        @Override
        public List<EOPEntry> generate(final EOPEntry existing,
                                       final AbsoluteDate date) throws TimeStampedCacheException {

            // Initialization
            final List<EOPEntry> generated = new ArrayList<EOPEntry>();

            // depending on the user provided EOP file, entry points are expected to
            // be every day or every 5 days, using 5 days is a safe margin
            final double timeMargin = 5 * Constants.JULIAN_DAY;
            final AbsoluteDate start;
            final AbsoluteDate end;
            if (existing == null) {
                start = date.shiftedBy(-AbstractEOPHistory.this.interpolationPoints * timeMargin);
                end = date.shiftedBy(AbstractEOPHistory.this.interpolationPoints * timeMargin);
            } else if (existing.getDate().compareTo(date) <= 0) {
                start = existing.getDate();
                end = date.shiftedBy(AbstractEOPHistory.this.interpolationPoints * timeMargin);
            } else {
                start = date.shiftedBy(-AbstractEOPHistory.this.interpolationPoints * timeMargin);
                end = existing.getDate();
            }

            // gather entries in the interval from existing to date (with some margins)
            for (final TimeStamped ts : AbstractEOPHistory.this.entries.tailSet(start).headSet(end)) {
                generated.add((EOPEntry) ts);
            }

            if (generated.isEmpty()) {
                if (AbstractEOPHistory.this.entries.isEmpty()) {
                    throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER, date);
                } else if (AbstractEOPHistory.this.entries.last().getDate().compareTo(date) < 0) {
                    throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_AFTER,
                        AbstractEOPHistory.this.entries.last().getDate());
                } else {
                    throw new TimeStampedCacheException(PatriusMessages.UNABLE_TO_GENERATE_NEW_DATA_BEFORE,
                        AbstractEOPHistory.this.entries.first().getDate());
                }
            }

            // Return result
            return generated;

        }

    }

}
