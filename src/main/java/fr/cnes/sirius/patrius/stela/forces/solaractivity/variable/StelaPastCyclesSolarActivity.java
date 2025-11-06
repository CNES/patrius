package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;

/**
 * Class for variable solar activity using past cycles.
 * Past solar activity cycles are stored in the <i>configuration</i> folder. Each cycle is of length of about 11 years.
 * This solar activity builds a solar activity file from the sequence of chosen cycles (given by their number).
 * Then the start of first cycle (in days) is also chosen (so that solar activity does not always start with low
 * values).
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class StelaPastCyclesSolarActivity extends StelaVariableSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = 8509826926373780700L;

    /** Solar activity first day of first cycle. */
    private final int solarActivityFirstDay;

    /** Solar activity cycles list. */
    private final List<Integer> solarActivityCycles;

    /**
     * True if an additional cycle is appended before the first cycle: this can be necessary for F107A computation
     * since it requires the average of 80 days of solar data around considered day.
     */
    private boolean isAdditionalCycle = false;

    /** Solar activity F10.7 coefficients map following the pattern [date (CNES JD), F107]. */
    private TreeMap<AbsoluteDate, Double> solarFluxMap;

    /** Solar activity AP coefficients map, following the pattern [date (CNES JD) + APtime_in_day, AP]. */
    private TreeMap<AbsoluteDate, Double> solarAPMap;

    /** Past cycles solar activity reader */
    private final StelaPastCyclesSolarActivityReader pastCyclesReader;

    // =============================== CONSTRUCTORS ================================ //

    /**
     * Constructor.
     *
     * @param solarActivityFirstDay solar activity first day of first cycle (in number of days). It should then be a
     *        number between
     *        1 and the size of the first cycles in days.
     * @param solarActivityCycles solar activity cycles sequence. The sequence must be long enough to contain the whole
     *        simulation.
     *        As cycles are roughly 11 years long, a 100 years simulation will require 10 cycles (9 might not be
     *        enough).
     * 
     * @throws PatriusException if an error occur while reading the solar activity files
     * @throws IOException if an error occur while reading the solar activity files
     * @throws ParseException if an error occur while reading the solar activity files
     */
    public StelaPastCyclesSolarActivity(final int solarActivityFirstDay, final List<Integer> solarActivityCycles)
        throws PatriusException, IOException, ParseException {
        super(null, StelaSolarActivityType.RANDOM_CYCLES);
        this.solarActivityFirstDay = solarActivityFirstDay;
        this.solarActivityCycles = solarActivityCycles;

        this.pastCyclesReader = getLoadedPastCyclesSolarActivityReader();
    }

    /**
     * Constructor.
     * 
     * @param pastCyclesData past cycles data
     * 
     * @throws PatriusException if an error occur while reading the solar activity files
     * @throws IOException if an error occur while reading the solar activity files
     * @throws ParseException if an error occur while reading the solar activity files
     */
    public StelaPastCyclesSolarActivity(final StelaPastCyclesData pastCyclesData)
        throws PatriusException, IOException, ParseException {
        this(pastCyclesData.getSolarActivityFirstDay(), pastCyclesData.getSolarActivityCycles());
        this.isAdditionalCycle = pastCyclesData.isAdditionalCycle();
    }

    // =============================== METHODS ================================ //

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        final String CR = System.lineSeparator();

        result.append("[ Solar Activity ]").append(CR);
        result.append(" Solar Activity Type : ").append(getSolActType()).append(CR);
        result.append(" First day of first cycle : ").append(this.solarActivityFirstDay).append(CR);
        result.append(" Cycles : ").append(this.solarActivityCycles).append(CR);
        return result.toString();
    }

    /** {@inheritDoc} */
    @Override
    public StelaPastCyclesSolarActivity copy() throws PatriusException, IOException, ParseException {
        return new StelaPastCyclesSolarActivity(this.solarActivityFirstDay, new ArrayList<>(this.solarActivityCycles));
    }

    /**
     * Read AP and F10.7 maps in the solar activity file
     */
    @Override
    protected void retrieveCurrentMaps() {
        final AbsoluteDate startKey = getStartDate();
        final AbsoluteDate endKey = getEndDate();
        setSolarFluxMap(this.solarFluxMap.subMap(startKey, true, endKey, true));
        setAPMap(this.solarAPMap.subMap(startKey, true, endKey, true));
    }

    /**
     * Getter for the solar activity first day.
     * 
     * @return the solar activity first day
     */
    public int getSolarActivityFirstDay() {
        return this.solarActivityFirstDay;
    }

    /**
     * Getter for the solar activity cycles list
     * 
     * @return the solar activity cycles list
     */
    public List<Integer> getSolarActivityCycles() {
        return this.solarActivityCycles;
    }

    /**
     * Getter for the additional cycle flag.
     * 
     * @return the additional cycle flag
     */
    public boolean isAdditionalCycle() {
        return this.isAdditionalCycle;
    }

    /**
     * Empty maps.
     */
    public void emptyMaps() {
        if (this.solarAPMap != null) {
            this.solarAPMap.clear();
        }
        if (this.solarFluxMap != null) {
            this.solarFluxMap.clear();
        }
    }

    /**
     * Build solar activity using past cycles. The resulting solar activity is stored in {@link #solarFluxMap}
     * and {@link #solarAPMap}. The resulting maps is a map following the pattern [date (CNES JD), solar activity]
     * with date starting at simulation date (provided by startDate) and with solar activity starting at
     * solar activity of first date of first cycle of cycles sequence.<br/>
     * For example, if simulation starting date is 15-06-2014 and first date of first cycle is day number 154
     * and cycles sequence is [2 1 3 4], then the resulting map will be:
     * <ul>
     * <li>...<i>(some data is added if additional cycle is required)</i></li>
     * <li><15-06-2014, solar activity met at day number 154 of cycle number 2></li>
     * <li><16-06-2014, solar activity met at day number 155 of cycle number 2></li>
     * <li><17-06-2014, solar activity met at day number 156 of cycle number 2></li>
     * <li>...<i>(until the end of cycles sequence)</i></li>
     * </ul>
     * Note: there may be some values before in the map in case of needed additional cycle and first day of first cycle
     * not being 1.
     * But in any case, at startDate, solar activity value is the value met at first day of first cycle.
     * 
     * @param startDate simulation starting date
     */
    public void buildSolarActivity(final AbsoluteDate startDate) {

        // Load solar activity files
        final TreeMap<Double, List<Double>> solarFluxMaps = new TreeMap<>(this.pastCyclesReader.getSolarFluxMap());
        final TreeMap<Double, List<Double>> solarApMaps = new TreeMap<>(this.pastCyclesReader.getSolarAPMap());

        // Get initial date of solar activity cycles
        final double firstCycle = this.solarActivityCycles.get(0);
        final int delta;
        if (this.isAdditionalCycle) {
            delta = solarFluxMaps.get(firstCycle).size();
        } else {
            delta = 0;
        }
        final int initDate =
            (int) FastMath.floor(startDate.toCNESJulianDate(this.timeScale) - this.solarActivityFirstDay - delta);

        // Initialization
        this.solarFluxMap = new TreeMap<>();
        this.solarAPMap = new TreeMap<>();

        // Loop on all solar cycle files
        AbsoluteDate date = new AbsoluteDate(initDate, this.timeScale);
        for (final Integer solarActivityCycle : this.solarActivityCycles) {
            // Get cycle and corresponding maps
            final double newCycle = (double) solarActivityCycle;

            final List<Double> newSolarFluxMap = solarFluxMaps.get(newCycle);
            final List<Double> newSolarApMap = solarApMaps.get(newCycle);

            // Append new cycle to solar activity maps
            for (int j = 0; j < newSolarFluxMap.size(); j++) {
                this.solarFluxMap.put(date, newSolarFluxMap.get(j));
                for (int k = 0; k < 8; k++) {
                    this.solarAPMap.put(date.shiftedBy(k * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY),
                        newSolarApMap.get(8 * j + k));
                }
                date = date.shiftedBy(Constants.JULIAN_DAY);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public TreeMap<AbsoluteDate, Double> getEntireAPMap() {
        return this.solarAPMap;
    }
}
