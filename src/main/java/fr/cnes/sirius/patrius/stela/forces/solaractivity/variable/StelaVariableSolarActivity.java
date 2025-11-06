package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.AbstractStelaSolarActivity;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Variable model of solar activity. This model uses the solar activity file provided in the <i>configuration</i>
 * folder.
 *
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaVariableSolarActivity extends AbstractStelaSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = 595786521112217789L;

    /** Interpolation interval (in days). */
    protected static final double INTERPOLATION_INTERVAL = 81;

    /** Constant. */
    protected static final double CONST_ONE_ON_EIGHT = 0.125;

    /** One-day interval between two solar activities in reference file. */
    private static final int SOLAR_ACT_FILE_INTERVAL = 1;

    /** Hour to second */
    private static final double H_TO_S = 3600;

    /** Constant. */
    private static final double CONST_DAILY = 12;

    /** Constant. */
    private static final double CONST_3H_1 = 1.5;

    /** Constant. */
    private static final double CONST_3H_2 = 4.5;

    /** Constant. */
    private static final double CONST_3H_3 = 7.5;

    /** Constant. */
    private static final double CONST_3H_4 = 10.5;

    /** Constant. */
    private static final double CONST_HALFDAY = 12;

    /** Constant. */
    private static final double CONST_1DAYANDHALF = 36;

    /** Constant. */
    private static final double CONST_2DAYANDHALF = 60;

    /** Constant. */
    private static final int ONE_ON_EIGHT_TIMES_THOUSAND = 125;

    /** Epsilon. */
    private static final double EPSILON = 1E-9;

    /** Constant. */
    private static final double THREE_HOUR_IN_DAYS = 3.0 / 24;

    /** TimeScale */
    protected final TimeScale timeScale;

    /** Cache date. */
    private AbsoluteDate cacheDate;

    /** Cache flux. */
    private double cacheFlux = Double.NaN;

    /** Solar activity computed coefficients. */
    private final double[] solarActivity = new double[9];

    /** Solar activity coefficients map, read in a file following the pattern [date, F107]. */
    private NavigableMap<AbsoluteDate, Double> solarFluxMap;

    /** Solar activity coefficients map, read in a file and then following the pattern [date + APtime, AP]. */
    private NavigableMap<AbsoluteDate, Double> solarAPMap;

    /** First useful date of solarFluxMap for a specified date. */
    private AbsoluteDate startDate;

    /** Last useful date of solarFluxMap for a specified date. */
    private AbsoluteDate endDate;

    /** Solar activity coefficients reader */
    protected final SolarActivityDataProvider reader;

    /** Entire Flux Map */
    protected final TreeMap<AbsoluteDate, Double> entireFluxMap;

    /** Entire AP Map */
    protected final TreeMap<AbsoluteDate, Double[]> entireApKPMap;

    /**
     * Constructor of a variable solar activity model.
     * 
     * @param reader the solar activity reader
     * 
     * @throws PatriusException for an initialization error
     */
    public StelaVariableSolarActivity(final SolarActivityDataProvider reader) throws PatriusException {
        this(reader, StelaSolarActivityType.VARIABLE);
    }

    /**
     * Constructor of a solar activity model.
     *
     * @param reader the solar activity reader
     * @param solarActivityType the solar activity type
     *
     * @throws PatriusException for an initialization error
     */
    protected StelaVariableSolarActivity(final SolarActivityDataProvider reader,
                                         final StelaSolarActivityType solarActivityType)
        throws PatriusException {
        super(solarActivityType);
        Arrays.fill(this.solarActivity, 0.0);
        this.timeScale = TimeScalesFactory.getUTC();

        if (reader != null) {
            this.reader = reader;
            this.entireFluxMap = new TreeMap<>(reader.getInstantFluxValues(reader.getMinDate(),
                reader.getMaxDate()));
            this.entireApKPMap = new TreeMap<>(reader.getApKpValues(reader.getMinDate(),
                reader.getMaxDate()));
        } else {
            // To deal with {@link StelaPastCyclesSolarActivity}
            this.reader = null;
            this.entireFluxMap = null;
            this.entireApKPMap = null;
        }
    }

    /**
     * Compute weighted mean flux, given a weights array.
     * 
     * @param date date
     * @param weights weights array
     *
     * @return weighted mean flux
     *
     * @throws PatriusException throw if computation failed
     */
    public double getMeanFlux(final AbsoluteDate date, final double[] weights) throws PatriusException {

        // Retrieve the floor date
        final AbsoluteDate floorDate = this.entireFluxMap.floorKey(date);

        // Check for cache
        if (this.cacheDate == null || !this.cacheDate.equals(floorDate)) {

            // Get keys
            final int halfSize = (int) FastMath.floor((double) (weights.length - 1) / 2);
            this.startDate = floorDate.shiftedBy(-(halfSize) * Constants.JULIAN_DAY);
            this.endDate = floorDate.shiftedBy(halfSize * Constants.JULIAN_DAY);

            // Retrieve the maps
            retrieveCurrentMaps();

            // Initialization
            double sumW = 0;
            double meanFlux = 0.;
            int i = 0;
            final Iterator<Entry<AbsoluteDate, Double>> it = this.solarFluxMap.entrySet().iterator();

            // Compute mean flux
            while (it.hasNext() && i < weights.length) {
                final double instantFlux = it.next().getValue();
                final double w = weights[i];
                meanFlux += (w * instantFlux);
                sumW += w;
                i++;
            }
            this.cacheFlux = MathLib.divide(meanFlux, sumW);
            this.cacheDate = floorDate;
        }

        return this.cacheFlux;
    }

    /** {@inheritDoc} */
    @Override
    public double getInstantFluxValue(final AbsoluteDate date) throws PatriusException {
        return this.reader.getInstantFluxValue(date);
    }

    /** {@inheritDoc} */
    @Override
    public double getAp(final AbsoluteDate date) throws PatriusException {
        return this.reader.getAp(date);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getSolarActivity(final AbsoluteDate date) throws PatriusException {

        double dateCJD = date.toCNESJulianDate(this.timeScale);
        dateCJD = Math.floor(dateCJD);
        // Get keys
        this.startDate = new AbsoluteDate(Math.floor(dateCJD - (INTERPOLATION_INTERVAL / 2 + 3)), this.timeScale);
        this.endDate = new AbsoluteDate(Math.floor(dateCJD + (INTERPOLATION_INTERVAL / 2 + 3)), this.timeScale);

        // Get a sub-part of the whole solar activity map
        retrieveCurrentMaps();

        // -----------------------------------------------------------------------
        // Argument "F107": Flux value at (t-1) calculated by linear interpolation
        // -----------------------------------------------------------------------

        // Date used for computation (t-1)
        final AbsoluteDate dateMinus1Day = date.shiftedBy(-Constants.JULIAN_DAY);

        // Define tk and tkp1 such as tk <= (t-1) < tkp1 and {tk, tkp1} are successive keys of solarFluxMap
        final AbsoluteDate tk = this.solarFluxMap.floorKey(dateMinus1Day);
        final AbsoluteDate tkp1 = this.solarFluxMap.ceilingKey(dateMinus1Day);

        // Interpolated daily flux
        this.solarActivity[0] = solarFluxLinearInterpolationF107(tk, tkp1, dateMinus1Day);

        // -----------------------------------------------------------------------
        // Argument "F107A": Mean flux F107A is the mean of the daily fluxes over 81 days centered in the current date
        // -----------------------------------------------------------------------

        // Define the interpolation interval [ta;tb]
        final AbsoluteDate ta = date.shiftedBy(-((INTERPOLATION_INTERVAL / 2) * Constants.JULIAN_DAY));
        final AbsoluteDate tb = date.shiftedBy((INTERPOLATION_INTERVAL / 2) * Constants.JULIAN_DAY);

        // Define tn and tnp1 such as tn <= ta < tnp1 and {tn, tnp1} are successive keys of solarFluxMap
        final AbsoluteDate tn = this.solarFluxMap.floorKey(ta);
        final AbsoluteDate tnp1 = this.solarFluxMap.ceilingKey(ta);

        // Define tm and tmp1 such as tm <= tb < tmp1 and {tm, tmp1} are successive keys of solarFluxMap
        final AbsoluteDate tm = this.solarFluxMap.floorKey(tb);
        final AbsoluteDate tmp1 = this.solarFluxMap.ceilingKey(tb);

        // Compute associated fluxes
        final double fta = solarFluxLinearInterpolationF107(tn, tnp1, ta);
        final double ftb = solarFluxLinearInterpolationF107(tm, tmp1, tb);
        final double ftnp1 = this.solarFluxMap.get(tnp1);
        final double ftm = this.solarFluxMap.get(tm);

        // final F107A computation

        double sum = 0;
        // More complex way but only way to be sure there are not round off errors (simple solution leads to round off
        // errors)
        final Iterator<Entry<AbsoluteDate, Double>> it = this.solarFluxMap.entrySet().iterator();

        double value1 = it.next().getValue();
        while (it.hasNext()) {
            final Entry<AbsoluteDate, Double> next = it.next();
            final double value2 = next.getValue();

            // An epsilon is used in case of round off leading to forgetting a term...
            if (next.getKey()
                .durationFrom(tn.shiftedBy((2 * SOLAR_ACT_FILE_INTERVAL - EPSILON) * Constants.JULIAN_DAY)) > 0
                    && next.getKey()
                        .durationFrom(tm.shiftedBy((SOLAR_ACT_FILE_INTERVAL - EPSILON) * Constants.JULIAN_DAY)) < 0) {
                sum += (value1 + value2) / 2;
            }
            value1 = value2;
        }

        this.solarActivity[1] =
            (1 / (tb.durationFrom(ta) / Constants.JULIAN_DAY))
                    * ((fta + ftnp1) * (tnp1.durationFrom(ta) / Constants.JULIAN_DAY) / 2 + sum
                            + (ftm + ftb) * (tb.durationFrom(tm) / Constants.JULIAN_DAY) / 2);

        // -----------------------------------------------------------------------
        // Geomagnetic activity
        // -----------------------------------------------------------------------

        // AP1 = Daily AP
        AbsoluteDate t1 = date.shiftedBy(-(CONST_DAILY * H_TO_S));
        AbsoluteDate t2 = date.shiftedBy(CONST_DAILY * H_TO_S);
        this.solarActivity[2] = apCoeffsMeanValue(t1, t2);

        // AP2 = Mean AP value over 3 hours centered in input date (t)
        t1 = date.shiftedBy(-(CONST_3H_1 * H_TO_S));
        t2 = date.shiftedBy(CONST_3H_1 * H_TO_S);
        this.solarActivity[3] = apCoeffsMeanValue(t1, t2);

        // AP3 = Mean AP value over 3 hours centered in input date t-3h with t the input date
        t1 = date.shiftedBy(-(CONST_3H_2 * H_TO_S));
        t2 = date.shiftedBy(-(CONST_3H_1 * H_TO_S));
        this.solarActivity[4] = apCoeffsMeanValue(t1, t2);

        // AP4 = Mean AP value over 3 hours centered in input date t-6h with t the input date
        t1 = date.shiftedBy(-(CONST_3H_3 * H_TO_S));
        t2 = date.shiftedBy(-(CONST_3H_2 * H_TO_S));
        this.solarActivity[5] = apCoeffsMeanValue(t1, t2);

        // AP5 = Mean AP value over 3 hours centered in input date t-9h with t the input date
        t1 = date.shiftedBy(-(CONST_3H_4 * H_TO_S));
        t2 = date.shiftedBy(-(CONST_3H_3 * H_TO_S));
        this.solarActivity[6] = apCoeffsMeanValue(t1, t2);

        // AP6 = Mean AP value over 24 hours centered in input date t-1day with t the input date
        t1 = date.shiftedBy(-(CONST_1DAYANDHALF * H_TO_S));
        t2 = date.shiftedBy(-(CONST_HALFDAY * H_TO_S));
        this.solarActivity[7] = apCoeffsMeanValue(t1, t2);

        // AP7 = Mean AP value over 24 hours centered in input date t-2day with t the input date
        t1 = date.shiftedBy(-(CONST_2DAYANDHALF * H_TO_S));
        t2 = date.shiftedBy(-(CONST_1DAYANDHALF * H_TO_S));
        this.solarActivity[8] = apCoeffsMeanValue(t1, t2);

        return this.solarActivity;
    }

    /**
     * Perform linear interpolation to compute the flux value at date t. The
     * solar flux value is known at dates tk and tkp1 such as tk <= t < tkp1.
     *
     * @param tk the min date of the interval.
     * @param tkp1 the max date of the interval.
     * @param t the date to compute the flux value
     *
     * @return the interpolated flux value at date t.
     */
    private double solarFluxLinearInterpolationF107(final AbsoluteDate tk, final AbsoluteDate tkp1,
                                                    final AbsoluteDate t) {
        // Compute fluxes associated with tk and tkp1
        final double result;
        if (tk == tkp1) {
            result = this.solarFluxMap.get(tk);
        } else {
            final double ftk = this.solarFluxMap.get(tk);
            final double ftkp1 = this.solarFluxMap.get(tkp1);
            result = ftk + (ftkp1 - ftk) * (t.durationFrom(tk)) / (tkp1.durationFrom(tk));
        }
        return result;
    }

    /**
     * Mean value of the AP coefficients over an interval [ta;tb]. NB: The
     * calculus is based on the mean value of a step function. A step function
     * is a piecewise constant function having only finitely many pieces.
     *
     * @param ta the min date of the interval.
     * @param tb the max date of the interval.
     *
     * @return the mean value of the AP coefficients over the interval [ta;tb].
     */
    private double apCoeffsMeanValue(final AbsoluteDate ta, final AbsoluteDate tb) {

        // ta and tb in cnes julian days
        final double taJD = ta.toCNESJulianDate(this.timeScale);
        final double tbJD = tb.toCNESJulianDate(this.timeScale);

        final double decimA = (taJD - Math.floor(taJD)) * 1000;
        final double decimB = (tbJD - Math.floor(tbJD)) * 1000;

        final double quotA = Math.floor(decimA / ONE_ON_EIGHT_TIMES_THOUSAND);
        final double quotB = Math.floor(decimB / ONE_ON_EIGHT_TIMES_THOUSAND);

        final double keyA = Math.floor(taJD) + (quotA + 1) * CONST_ONE_ON_EIGHT;
        final double keyB = Math.floor(tbJD) + quotB * CONST_ONE_ON_EIGHT;

        // Borders
        double sum =
            this.solarAPMap.get(new AbsoluteDate(keyA - CONST_ONE_ON_EIGHT, this.timeScale))
                    * (keyA - taJD);
        sum += this.solarAPMap.get(new AbsoluteDate(keyB, this.timeScale)) * (tbJD - keyB);

        // Center
        final int imax = (int) ((keyB - keyA) * 8);
        for (int i = 0; i < imax; i++) {
            sum += this.solarAPMap.get(new AbsoluteDate(keyA + i * CONST_ONE_ON_EIGHT, this.timeScale))
                    * THREE_HOUR_IN_DAYS;
        }

        // Mean: result
        return sum / (tbJD - taJD);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String CR = System.lineSeparator();
        return "[ Solar Activity ]" + CR + "Solar Activity Type : " + getSolActType() + CR;
    }

    /** {@inheritDoc} */
    @Override
    public StelaVariableSolarActivity copy() throws PatriusException, IOException, ParseException {
        return new StelaVariableSolarActivity(this.reader);
    }

    /**
     * Get AP and F10.7 maps from the solar activity reader with current dates
     */
    protected void retrieveCurrentMaps() throws PatriusException {
        // Retrieve solar flux values
        this.solarFluxMap =
            new TreeMap<>(this.reader.getInstantFluxValues(this.startDate, this.endDate));

        // Retrieve AP/KP values
        final TreeMap<AbsoluteDate, Double[]> aPKPMap =
            new TreeMap<>(this.reader.getApKpValues(this.startDate, this.endDate));

        final TreeMap<AbsoluteDate, Double> aPMap = new TreeMap<>();

        // Populate the AP map with the first element of the Double[] array
        for (final Map.Entry<AbsoluteDate, Double[]> entry : aPKPMap.entrySet()) {
            // Check if the value array is not null and has at least one element
            if (entry.getValue() != null && entry.getValue().length > 0) {
                aPMap.put(entry.getKey(), entry.getValue()[0]);
            }
        }
        this.solarAPMap = aPMap;
    }

    /**
     * Set the solar flux map.
     * 
     * @param newMap the new solar flux map
     */
    protected void setSolarFluxMap(final NavigableMap<AbsoluteDate, Double> newMap) {
        this.solarFluxMap = newMap;
    }

    /**
     * Set the AP map.
     * 
     * @param newMap
     *        the new AP map
     */
    protected void setAPMap(final NavigableMap<AbsoluteDate, Double> newMap) {

        this.solarAPMap = newMap;
    }

    /**
     * Get start date.
     *
     * @return startKey
     */
    protected AbsoluteDate getStartDate() {
        return this.startDate;
    }

    /**
     * Get end date.
     *
     * @return endKey
     */
    protected AbsoluteDate getEndDate() {
        return this.endDate;
    }

    /**
     * Get Geomagnetic activity (AP map).
     * 
     * @return AP map
     */
    public NavigableMap<AbsoluteDate, Double> getEntireAPMap() {

        final TreeMap<AbsoluteDate, Double> entireAPMap = new TreeMap<>();

        // Populate the AP map with the first element of the Double[] array
        for (final Map.Entry<AbsoluteDate, Double[]> entry : this.entireApKPMap.entrySet()) {
            // Check if the value array is not null and has at least one element
            if (entry.getValue() != null && entry.getValue().length > 0) {
                entireAPMap.put(entry.getKey(), entry.getValue()[0]);
            }
        }

        return entireAPMap;
    }

    /**
     * Get the loaded past cycles solar activity reader
     *
     * @return the loaded past cycles solar activity reader
     *
     * @throws IOException if an error occur while the creation of InputStreams
     * @throws ParseException if data can't be parsed
     * @throws PatriusException if some data is missing or if some loader specific error occurs
     */
    protected static StelaPastCyclesSolarActivityReader getLoadedPastCyclesSolarActivityReader()
        throws IOException, ParseException, PatriusException {
        final StelaPastCyclesSolarActivityReader pastCyclesReader;
        pastCyclesReader = new StelaPastCyclesSolarActivityReader();
        for (final InputStream file : StelaPastCycleSolarActivityProperties.getPastCycleFilePath()) {
            if (pastCyclesReader.stillAcceptsData()) {
                pastCyclesReader.loadData(file, "DUMMY");
            }
        }
        pastCyclesReader.setReadCompleted(true);
        return pastCyclesReader;
    }
}