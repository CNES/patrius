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
* VERSION:4.13:DM:DM-120:08/12/2023:[PATRIUS] Merge de la branche patrius-for-lotus dans Patrius
* VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-95:30/06/2023:[PATRIUS] Utilisation de types gen. dans les classes internes d'interp. de 
 * AbstractEOPHistory
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3223:03/11/2022:[PATRIUS] Frame implements PVCoordinatesProvider
 * VERSION:4.10:DM:DM-3211:03/11/2022:[PATRIUS] Ajout de fonctionnalites aux PyramidalField
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
import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import fr.cnes.sirius.patrius.math.analysis.polynomials.PolynomialFunctionLagrangeForm;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.ChronologicalComparator;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolationFunctionBuilder;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.serializablefunction.SerializableToDoubleFunction;

/**
 * This class loads any kind of Earth Orientation Parameter data throughout a large time range.
 * 
 * @author Pascal Parraud
 */
public abstract class AbstractEOPHistory implements Serializable, EOPHistory {

    /** Optimal cache size (determined by benchmark analysis). */
    protected static final int OPTIMAL_CACHE_SIZE = 3;

    /** Serializable UID. */
    private static final long serialVersionUID = 7130166524297186988L;

    /** Pole correction builder. */
    private static final CorrectionBuilder<PoleCorrection> POLE_CORRECTION = (a, b) -> new PoleCorrection(a, b);

    /** Nutation correction builder. */
    private static final CorrectionBuilder<NutationCorrection> NUTATION_CORRECTION =
        (a, b) -> new NutationCorrection(a, b);

    /** Earth Orientation Parameter entries. */
    private final SortedSet<TimeStamped> entries;

    /** Interpolation method. */
    private final EOPInterpolators interpolator;

    /** EOP entries array. */
    private transient EOPEntry[] eopEntryArray;

    /** UT1-TAI cache interpolable function (efficient way to store and give access to this value). */
    private transient TimeStampedInterpolableEphemeris<EOPEntry, Double> ut1MinusTaiEphem;

    /** LOD cache interpolable function (efficient way to store and give access to this value). */
    private transient TimeStampedInterpolableEphemeris<EOPEntry, Double> lodEphem;

    /** Pole correction cache interpolable function (efficient way to store and give access to this value). */
    private transient TimeStampedInterpolableEphemeris<EOPEntry, PoleCorrection> poleCorrection;

    /** Nutation correction cache interpolable function (efficient way to store and give access to this value). */
    private transient TimeStampedInterpolableEphemeris<EOPEntry, NutationCorrection> nutationCorrection;

    /**
     * Simple constructor.
     * 
     * @param interpMethod
     *        EOP interpolation method
     */
    protected AbstractEOPHistory(final EOPInterpolators interpMethod) {
        this.interpolator = interpMethod;
        this.entries = new TreeSet<>(new ChronologicalComparator());
    }

    /** {@inheritDoc} */
    @Override
    public void addEntry(final EOPEntry entry) {
        this.entries.add(entry);
        resetEphemeris();
    }

    /**
     * Reset the EOP entries array and the cache values.
     * <p>
     * Note: This method is called when an {@link #addEntry(EOPEntry) entry is added}.<br>
     * Most of the time the entries are first added, then the history is used. So these values shouldn't be reset too
     * often.
     * </p>
     */
    @SuppressWarnings("PMD.NullAssignment")
    private void resetEphemeris() {
        this.eopEntryArray = null;
        this.ut1MinusTaiEphem = null;
        this.lodEphem = null;
        this.poleCorrection = null;
        this.nutationCorrection = null;
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<TimeStamped> iterator() {
        return this.entries.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.entries.size();
    }

    /**
     * Returns {@code true} if no EOP have been loaded, {@code false} otherwise.
     * 
     * @return {@code true} if no EOP have been loaded, {@code false} otherwise
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
        if (this.ut1MinusTaiEphem == null) {
            this.ut1MinusTaiEphem = new TimeStampedInterpolableEphemeris<>(
                getEOPEntryArray(), this.interpolator.getInterpolationPoints(),
                new DoubleInterpolationFunctionBuilder<>(eopEntry -> eopEntry.getUT1MinusTAI()),
                true, false, false, OPTIMAL_CACHE_SIZE);
        }
        return this.ut1MinusTaiEphem.interpolate(date).doubleValue();
    }

    /** {@inheritDoc} */
    @Override
    public double getLOD(final AbsoluteDate date) {
        if (this.lodEphem == null) {
            this.lodEphem = new TimeStampedInterpolableEphemeris<>(
                getEOPEntryArray(), this.interpolator.getInterpolationPoints(),
                new DoubleInterpolationFunctionBuilder<>(eopEntry -> eopEntry.getLOD()),
                true, false, false, OPTIMAL_CACHE_SIZE);
        }
        return this.lodEphem.interpolate(date).doubleValue();
    }

    /** {@inheritDoc} */
    @Override
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        if (this.poleCorrection == null) {
            this.poleCorrection = new TimeStampedInterpolableEphemeris<>(
                getEOPEntryArray(), this.interpolator.getInterpolationPoints(),
                new CorrectionInterpolationFunctionBuilder<>(eopEntry -> eopEntry.getX(), eopEntry -> eopEntry.getY(),
                    POLE_CORRECTION), true, false, false, OPTIMAL_CACHE_SIZE);
        }
        return this.poleCorrection.interpolate(date);
    }

    /** {@inheritDoc} */
    @Override
    public NutationCorrection getNutationCorrection(final AbsoluteDate date) {

        if (this.nutationCorrection == null) {
            this.nutationCorrection = new TimeStampedInterpolableEphemeris<>(
                getEOPEntryArray(), this.interpolator.getInterpolationPoints(),
                new CorrectionInterpolationFunctionBuilder<>(eopEntry -> eopEntry.getDX(), eopEntry -> eopEntry.getDY(),
                    NUTATION_CORRECTION),
                true, false, false, OPTIMAL_CACHE_SIZE);
        }
        return this.nutationCorrection.interpolate(date);
    }

    /**
     * Getter for the EOP entries array.
     * 
     * @return the EOP entries array
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    // Reason: performances
    protected EOPEntry[] getEOPEntryArray() {
        if (this.eopEntryArray == null) { // Initialize the array once until it is reset
            this.eopEntryArray = this.entries.toArray(new EOPEntry[size()]);
        }
        return this.eopEntryArray;
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

    /**
     * Cache interpolable function builder used to generate {@link Double} values.
     * 
     * @param <T>
     *        The EOPEntry type with a generic representation
     */
    protected static class DoubleInterpolationFunctionBuilder<T extends EOPEntry>
        implements TimeStampedInterpolationFunctionBuilder<T, Double> {

        /** Serializable UID. */
        private static final long serialVersionUID = 5100897572222997590L;

        /** Function linking the EOP entries to {@link Double} values. */
        private final SerializableToDoubleFunction<T> func;

        /**
         * Constructor.
         * 
         * @param func
         *        Function linking the EOP entries to {@link Double} values
         */
        public DoubleInterpolationFunctionBuilder(final SerializableToDoubleFunction<T> func) {
            this.func = func;
        }

        /** {@inheritDoc} */
        @Override
        public Function<AbsoluteDate, ? extends Double> buildInterpolationFunction(final T[] samples,
                                                                                   final int indexInf,
                                                                                   final int indexSup) {
            return new LagrangeDoubleInterpolationFunction<>(samples, indexInf, indexSup, this.func);
        }
    }

    /**
     * Cache interpolable function used to generate the {@link Double} values.
     * 
     * @param <T>
     *        The EOPEntry type with a generic representation
     */
    private static class LagrangeDoubleInterpolationFunction<T extends EOPEntry>
        implements Function<AbsoluteDate, Double> {

        /** Function linking the EOP entries to {@link Double} values. */
        private final PolynomialFunctionLagrangeForm pflf;

        /** Reference date. */
        private AbsoluteDate refDate;

        /**
         * Constructor.
         * 
         * @param samples
         *        The samples of EOP entries data
         * @param indexInf
         *        Inferior index
         * @param indexSup
         *        Superior index
         * @param func
         *        Function linking the EOP entries to {@link Double} values
         */
        public LagrangeDoubleInterpolationFunction(final T[] samples, 
                                                   final int indexInf,
                                                   final int indexSup, 
                                                   final SerializableToDoubleFunction<T> func) {
          
            final int valuesLength = indexSup - indexInf;
            final double[] dvals = new double[valuesLength];
            final double[] dutvals = new double[valuesLength];

            for (int i = indexInf; i < indexSup; i++) {
                final T entry = samples[i];
                final int valsIndex = i - indexInf;
                final AbsoluteDate date = entry.getDate();
                if (this.refDate == null) {
                    this.refDate = date;
                }
                dvals[valsIndex] = date.durationFrom(this.refDate);
                dutvals[valsIndex] = func.applyAsDouble(entry);
            }
            this.pflf = new PolynomialFunctionLagrangeForm(dvals, dutvals);
        }

        /** {@inheritDoc} */
        @Override
        public Double apply(final AbsoluteDate date) {
            return new Double(this.pflf.value(date.durationFrom(this.refDate)));
        }
    }

    /**
     * Cache interpolable function builder used to generate the corrections.
     * 
     * @param <T>
     *        Generic representation of the interpolable function output type
     */
    private static class CorrectionInterpolationFunctionBuilder<T>
        implements TimeStampedInterpolationFunctionBuilder<EOPEntry, T> {

        /** Serializable UID. */
        private static final long serialVersionUID = -1004127630281484292L;

        /** Interpolation function of the X component of the correction. */
        private final SerializableToDoubleFunction<EOPEntry> funcx;

        /** Interpolation function of the Y component of the correction. */
        private final SerializableToDoubleFunction<EOPEntry> funcy;

        /** Correction builder. */
        private final CorrectionBuilder<T> correctionBuilder;

        /**
         * Constructor.
         * 
         * @param funcx
         *        Interpolation function of the X component of the correction
         * @param funcy
         *        Interpolation function of the Y component of the correction
         * @param correctionBuilder
         *        Correction builder
         */
        public CorrectionInterpolationFunctionBuilder(final SerializableToDoubleFunction<EOPEntry> funcx,
                                                      final SerializableToDoubleFunction<EOPEntry> funcy,
                                                      final CorrectionBuilder<T> correctionBuilder) {
            this.funcx = funcx;
            this.funcy = funcy;
            this.correctionBuilder = correctionBuilder;
        }

        /** {@inheritDoc} */
        @Override
        public Function<AbsoluteDate, T> buildInterpolationFunction(final EOPEntry[] samples,
                                                                    final int indexInf,
                                                                    final int indexSup) {
            return new LagrangeCorrectionInterpolationFunction<>(samples, indexInf, indexSup, this.funcx, this.funcy,
                this.correctionBuilder);
        }
    }

    /**
     * Cache interpolable function used to generate the corrections.
     * 
     * @param <T>
     *        Generic representation of the interpolable function output type
     */
    private static class LagrangeCorrectionInterpolationFunction<T> implements Function<AbsoluteDate, T> {

        /** Interpolation function of the X component of the correction. */
        private final PolynomialFunctionLagrangeForm pflfx;

        /** Interpolation function of the Y component of the correction. */
        private final PolynomialFunctionLagrangeForm pflfy;

        /** Correction builder. */
        private final CorrectionBuilder<T> correctionBuilder;

        /** Reference date. */
        private AbsoluteDate refDate;

        /**
         * Constructor.
         * 
         * @param samples
         *        The samples of EOP entries data
         * @param indexInf
         *        Inferior index
         * @param indexSup
         *        Superior index
         * @param funcx
         *        Interpolation function of the X component of the pole motion
         * @param funcy
         *        Interpolation function of the Y component of the pole motion
         * @param correctionBuilder
         *        Correction builder
         */
        public LagrangeCorrectionInterpolationFunction(final EOPEntry[] samples, final int indexInf,
                                                       final int indexSup,
                                                       final SerializableToDoubleFunction<EOPEntry> funcx,
                                                       final SerializableToDoubleFunction<EOPEntry> funcy,
                                                       final CorrectionBuilder<T> correctionBuilder) {
            final int valuesLength = indexSup - indexInf;
            final double[] dvals = new double[valuesLength];
            final double[] xvals = new double[valuesLength];
            final double[] yvals = new double[valuesLength];

            for (int i = indexInf; i < indexSup; i++) {
                final EOPEntry entry = samples[i];
                final int valsIndex = i - indexInf;
                final AbsoluteDate date = entry.getDate();
                if (this.refDate == null) {
                    this.refDate = date;
                }
                dvals[valsIndex] = date.durationFrom(this.refDate);
                xvals[valsIndex] = funcx.applyAsDouble(entry);
                yvals[valsIndex] = funcy.applyAsDouble(entry);
            }

            this.pflfx = new PolynomialFunctionLagrangeForm(dvals, xvals);
            this.pflfy = new PolynomialFunctionLagrangeForm(dvals, yvals);
            this.correctionBuilder = correctionBuilder;
        }

        /** {@inheritDoc} */
        @Override
        public T apply(final AbsoluteDate date) {
            final double duration = date.durationFrom(this.refDate);
            return this.correctionBuilder.apply(this.pflfx.value(duration), this.pflfy.value(duration));
        }
    }

    /**
     * Interface representing a correction which depends of two values.
     * 
     * @param <T>
     *        Generic representation of the correction output type
     */
    @FunctionalInterface
    private interface CorrectionBuilder<T> extends Serializable {

        /**
         * Apply the correction.
         * 
         * @param a
         *        First value
         * @param b
         *        Second value
         * @return the result
         */
        T apply(final double a, final double b);
    }
}
