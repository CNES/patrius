package fr.cnes.sirius.patrius.stela.forces.solaractivity.variable;

import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.stela.forces.solaractivity.StelaSolarActivityType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class for variable solar activity using 3 steps method:
 * keep variable file until {@link #date1}, disperse file between {@link #date1} and {@link #date2}
 * (using coefficients {@link #f107Coef} and {@link #apCoef}) and use random cycles after {@link #date2}.
 * 
 * @author Mathis Guillemette
 * HISTORY
 * VERSION:4.16:OPENFD-389:25/04/2025:[STELA-PATRIUS] Activites solaires additionnelles
 * END-HISTORY
 * @since 4.16
 */
public class Stela3StepsSolarActivity extends StelaVariableSolarActivity {

    /** Serializable UID. */
    private static final long serialVersionUID = 1314071186223511853L;

    /** Date 1. */
    private final AbsoluteDate date1;

    /** Date 2. */
    private final AbsoluteDate date2;

    /** Coefficient applied to the solar activity F10.7 (between {@link #date1} and {@link #date2}). */
    private final double f107Coef;

    /** Coefficient applied to the solar activity AP (between {@link #date1} and {@link #date2}). */
    private final double apCoef;

    /** Solar activity cycles list (after date {@link #date2}). */
    private final List<Integer> solarActivityCycles;

    /** Solar activity F10.7 coefficients map following the pattern [date, F107]. */
    private TreeMap<AbsoluteDate, Double> solarFluxMap;

    /** Solar activity coefficients map, following the pattern [date + APtime, AP]. */
    private TreeMap<AbsoluteDate, Double> solarAPMap;

    /**
     * Past cycles solar activity reader
     */
    private final StelaPastCyclesSolarActivityReader pastCyclesReader;

    // =============================== CONSTRUCTORS ================================ //

    /**
     * Constructor.
     * 
     * @param date1 first model date
     * @param date2 second model date
     * @param f107Coef F10.7 dispersion coefficient
     * @param apCoef Ap dispersion coefficient
     * @param solarActivityCycles solar activity cycles list
     * @param reader solar activity reader
     * 
     * @throws PatriusException if an error occur while reading the solar activity files
     * @throws IOException if an error occur while reading the solar activity files
     * @throws ParseException if an error occur while reading the solar activity files
     */
    public Stela3StepsSolarActivity(final AbsoluteDate date1, final AbsoluteDate date2, final double f107Coef,
                                    final double apCoef,
                                    final List<Integer> solarActivityCycles, final SolarActivityDataProvider reader)
        throws PatriusException, IOException, ParseException {
        super(reader, StelaSolarActivityType.MIXED_3DATE_RANGES);
        this.date1 = date1;
        this.date2 = date2;
        this.f107Coef = f107Coef;
        this.apCoef = apCoef;
        this.solarActivityCycles = solarActivityCycles;

        this.pastCyclesReader = getLoadedPastCyclesSolarActivityReader();
    }

    /**
     * Basis constructor. F10.7 coefficient = 1 ; AP coefficient = 1
     * 
     * @param reader solar activity reader
     * 
     * @throws PatriusException if an error occur while reading the solar activity files
     * @throws IOException if an error occur while reading the solar activity files
     * @throws ParseException if an error occur while reading the solar activity files
     */
    public Stela3StepsSolarActivity(final SolarActivityDataProvider reader)
        throws PatriusException, IOException, ParseException {
        this(new AbsoluteDate(), new AbsoluteDate(), 1, 1, new ArrayList<>(), reader);
    }

    // =============================== METHODS ================================ //

    /** {@inheritDoc} */
    @Override
    public String toString() {
        final String CR = System.lineSeparator();
        return "[ Solar Activity ]" + CR + " Solar Activity Type : " + getSolActType() + CR + " Date 1 : "
                + this.date1.toCNESJulianDate(this.timeScale)
                + "JJCNES" + CR + " Date 2 : " + this.date2.toCNESJulianDate(this.timeScale)
                + "JJCNES" + CR + " F10.7 coefficient : " + this.f107Coef + CR + " Ap coefficient : " + this.apCoef + CR
                + " Cycles : " + this.solarActivityCycles + CR;
    }

    /** {@inheritDoc} */
    @Override
    public Stela3StepsSolarActivity copy() throws PatriusException, IOException, ParseException {
        return new Stela3StepsSolarActivity(this.date1, this.date2, this.f107Coef, this.apCoef,
            new ArrayList<>(this.solarActivityCycles), this.reader);
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
     * Getter for the first date.
     * 
     * @return the first date
     */
    public AbsoluteDate getDate1() {
        return this.date1;
    }

    /**
     * Getter for the second date.
     * 
     * @return the second date
     */
    public AbsoluteDate getDate2() {
        return this.date2;
    }

    /**
     * Getter for the F10.7 dispersion coefficient.
     * 
     * @return the F10.7 dispersion coefficient
     */
    public double getF107Coef() {
        return this.f107Coef;
    }

    /**
     * Getter for the Ap dispersion coefficient.
     * 
     * @return the Ap dispersion coefficient
     */
    public double getApCoef() {
        return this.apCoef;
    }

    /**
     * Build solar activity. The resulting solar activity is stored in {@link #solarFluxMap}
     * and {@link #solarAPMap}. The resulting maps is a map following the pattern [{@link AbsoluteDate date}, solar
     * activity] and built similarly as {@link StelaPastCyclesSolarActivity#buildSolarActivity(AbsoluteDate)}.
     * 
     * @param startDate simulation starting date
     */
    public void buildSolarActivity(final AbsoluteDate startDate) {

        // Get first date.
        // First date is shifted to taken into account F107A need for 80 values
        final AbsoluteDate initDate = startDate
            .shiftedBy((-StelaVariableSolarActivity.INTERPOLATION_INTERVAL / 2 - 3) * Constants.JULIAN_DAY);

        // Initialization
        this.solarFluxMap = new TreeMap<>();
        this.solarAPMap = new TreeMap<>();

        // Load solar activity file and cycles files

        final TreeMap<AbsoluteDate, Double> solarFileApKpMap = new TreeMap<>(super.getEntireAPMap());
        final TreeMap<Double, List<Double>> cyclesFluxMaps = new TreeMap<>(this.pastCyclesReader.getSolarFluxMap());
        final TreeMap<Double, List<Double>> cyclesApMaps = new TreeMap<>(this.pastCyclesReader.getSolarAPMap());

        // Keep usual solar activity until t1
        AbsoluteDate date = initDate;
        while (date.durationFrom(this.date1) < 0) {
            this.solarFluxMap.put(date, this.entireFluxMap.get(this.entireFluxMap.floorKey(date)));
            for (int i = 0; i < 8; i++) {
                this.solarAPMap.put(date.shiftedBy(i * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY),
                    solarFileApKpMap
                        .get(solarFileApKpMap.floorKey(date.shiftedBy(i * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY))));
            }
            date = date.shiftedBy(Constants.JULIAN_DAY);
        }

        // Disperse solar activity between t1 and t2
        while (date.durationFrom(this.date2) < 0) {
            final double currentFlux = this.entireFluxMap.get(this.entireFluxMap.floorKey(date));
            final double newFlux = currentFlux * this.f107Coef;

            final double[] newAp = new double[8];
            for (int i = 0; i < 8; i++) {
                newAp[i] = solarFileApKpMap.get(
                    solarFileApKpMap.floorKey(date.shiftedBy(i * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY)))
                        * this.apCoef;
            }

            this.solarFluxMap.put(date, newFlux);
            for (int i = 0; i < 8; i++) {
                this.solarAPMap.put(date.shiftedBy(i * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY), newAp[i]);
            }
            date = date.shiftedBy(Constants.JULIAN_DAY);
        }

        // Use cycles after t2
        for (final double newCycle : this.solarActivityCycles) {
            final List<Double> newSolarFluxMap = cyclesFluxMaps.get(newCycle);
            final List<Double> newSolarApMap = cyclesApMaps.get(newCycle);

            // Append new cycle to solar activity maps
            for (int j = 0; j < newSolarFluxMap.size(); j++) {
                this.solarFluxMap.put(date, newSolarFluxMap.get(j));
                for (int k = 0; k < 8; k++) {
                    this.solarAPMap.put(date.shiftedBy(k * CONST_ONE_ON_EIGHT * Constants.JULIAN_DAY),
                        newSolarApMap.get(j * 8 + k));
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

    /**
     * Write solar activity array into file following STELA solar activity file pattern.
     * 
     * @param fileName
     *        file name
     */
    public void writeToFile(final String fileName) {

        try {
            // Initialize the buffer with the name of the file to write
            final FileWriter fileWriter = new FileWriter(fileName);
            final BufferedWriter output = new BufferedWriter(fileWriter);

            final String cr = System.lineSeparator();

            // File header
            output.write("# version 1" + cr);
            output.write("# ******************************************************" + cr);
            output.write("#                              STELA SOLAR ACTIVITY FILE" + cr);
            output.write("# ******************************************************" + cr);
            output.write("# DATE(JJ/1950) DAILY_FLUX 8x(3H-AP)" + cr);
            output.write("# ------------------------------------------------------" + cr);

            // Solar activity file
            final Iterator<Entry<AbsoluteDate, Double>> itFlux = this.solarFluxMap.entrySet().iterator();
            final Iterator<Entry<AbsoluteDate, Double>> itAp = this.solarAPMap.entrySet().iterator();
            while (itFlux.hasNext()) {
                final Entry<AbsoluteDate, Double> next = itFlux.next();
                final AbsoluteDate date = next.getKey();
                final double flux = next.getValue();
                final int[] ap = new int[8];
                for (int i = 0; i < ap.length; i++) {
                    ap[i] = itAp.next().getValue().intValue();
                }

                final String line =
                    String.format(Locale.US, "%s %.2f %d %d %d %d %d %d %d %d", date.toCNESJulianDate(this.timeScale),
                        flux, ap[0], ap[1], ap[2], ap[3], ap[4], ap[5], ap[6], ap[7]) + cr;
                output.write(line);
            }

            // Close files
            output.flush();
            output.close();
            fileWriter.close();

        } catch (final IOException e) {
            // I/O exception: writing failed
            throw new PatriusRuntimeException("STELA_SOLAR_ACTIVITY_FILE_SAVE_ERROR", e);
        }
    }
}
