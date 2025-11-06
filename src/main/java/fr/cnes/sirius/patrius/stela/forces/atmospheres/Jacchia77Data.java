package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BiLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.UniLinearIntervalsFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.UniLinearIntervalsInterpolator;

/**
 * This class implements the Jaccia77 atmospheric model data and functions.
 * <p>
 * The constants used in the equations relative to this model are stored in this class.
 * </p>
 * 
 * @concurrency thread-hostile
 * @concurrency.comment This class can read data through a public access (initTempWmMaps()) which is not synchronized
 *                      (thread lock) at the moment.
 * 
 * @author Thomas Rodrigues, Thibaut BONIT
 * HISTORY
 * VERSION:4.16:OPENFD-390:25/04/2025:[STELA-PATRIUS] Modeles d'atmosphere additionnels
 * END-HISTORY
 * @since 4.16
 */
public final class Jacchia77Data implements Serializable {

    /** Table used for Ap/Kp conversion. */
    public static final double[][] AP_KP_TABLE = {
        { 0., 1.0 / 3.0, 2.0 / 3.0, 1., 4.0 / 3.0, 5.0 / 3.0, 2., 7.0 / 3.0, 8.0 / 3.0, 3., 10.0 / 3.0, 11.0 / 3.0, 4.,
            13.0 / 3.0,
            14.0 / 3.0, 5., 16.0 / 3.0, 17.0 / 3.0, 6., 19.0 / 3.0, 20.0 / 3.0, 7., 22.0 / 3.0, 23.0 / 3.0, 8.,
            25.0 / 3.0, 26.0 / 3.0,
            9. },
        { 0, 2, 3, 4, 5, 6, 7, 9, 12, 15, 18, 22, 27, 32, 39, 48, 56, 67, 80, 94, 111, 132, 154, 179, 207, 236, 300,
            400 } };

    /** 1D interpolator for Ap/Kp conversion table. */
    public static final UniLinearIntervalsFunction AP_FUNCTION =
        new UniLinearIntervalsInterpolator().interpolate(AP_KP_TABLE[1], AP_KP_TABLE[0]);

    /** Altitudes table for density logarithm interpolation. */
    public static final double[] ALT_TABLE = { 0.0, 1000.0, 2000.0, 3000.0, 4000.0, 5000.0, 6000.0, 7000.0, 8000.0,
        9000.0,
        10000.0,
        11000.0, 12000.0, 13000.0, 14000.0, 15000.0, 16000.0, 17000.0, 18000.0, 19000.0, 20000.0, 21000.0, 22000.0,
        23000.0, 24000.0,
        25000.0, 26000.0, 27000.0, 28000.0, 29000.0, 30000.0, 31000.0, 32000.0, 33000.0, 34000.0, 35000.0, 36000.0,
        37000.0, 38000.0,
        39000.0, 40000.0, 41000.0, 42000.0, 43000.0, 44000.0, 45000.0, 46000.0, 47000.0, 48000.0, 49000.0, 50000.0,
        51000.0, 52000.0,
        53000.0, 54000.0, 55000.0, 56000.0, 57000.0, 58000.0, 59000.0, 60000.0, 61000.0, 62000.0, 63000.0, 64000.0,
        65000.0, 66000.0,
        67000.0, 68000.0, 69000.0, 70000.0, 71000.0, 72000.0, 73000.0, 74000.0, 75000.0, 76000.0, 77000.0, 78000.0,
        79000.0, 80000.0,
        81000.0, 82000.0, 83000.0, 84000.0, 85000.0, 86000.0, 87000.0, 88000.0, 89000.0, 90000.0, 91000.0, 92000.0,
        93000.0, 94000.0,
        95000.0, 96000.0, 97000.0, 98000.0, 99000.0, 100000.0, 101000.0, 102000.0, 103000.0, 104000.0, 105000.0,
        106000.0, 107000.0,
        108000.0, 109000.0, 110000.0, 111000.0, 112000.0, 113000.0, 114000.0, 115000.0, 116000.0, 117000.0, 118000.0,
        119000.0, 120000.0,
        121000.0, 122000.0, 123000.0, 124000.0, 125000.0, 126000.0, 127000.0, 128000.0, 129000.0, 130000.0, 131000.0,
        132000.0, 133000.0,
        134000.0, 135000.0, 136000.0, 137000.0, 138000.0, 139000.0, 140000.0, 141000.0, 142000.0, 143000.0, 144000.0,
        145000.0, 146000.0,
        147000.0, 148000.0, 149000.0, 150000.0, 151000.0, 152000.0, 153000.0, 154000.0, 155000.0, 156000.0, 157000.0,
        158000.0, 159000.0,
        160000.0, 161000.0, 162000.0, 163000.0, 164000.0, 165000.0, 166000.0, 167000.0, 168000.0, 169000.0, 170000.0,
        171000.0, 172000.0,
        173000.0, 174000.0, 175000.0, 176000.0, 177000.0, 178000.0, 179000.0, 180000.0, 181000.0, 182000.0, 183000.0,
        184000.0, 185000.0,
        186000.0, 187000.0, 188000.0, 189000.0, 190000.0, 191000.0, 192000.0, 193000.0, 194000.0, 195000.0, 196000.0,
        197000.0, 198000.0,
        199000.0, 200000.0, 210000.0, 220000.0, 230000.0, 240000.0, 250000.0, 260000.0, 270000.0, 280000.0, 290000.0,
        300000.0, 310000.0,
        320000.0, 330000.0, 340000.0, 350000.0, 360000.0, 370000.0, 380000.0, 390000.0, 400000.0, 410000.0, 420000.0,
        430000.0, 440000.0,
        450000.0, 460000.0, 470000.0, 480000.0, 490000.0, 500000.0, 510000.0, 520000.0, 530000.0, 540000.0, 550000.0,
        560000.0, 570000.0,
        580000.0, 590000.0, 600000.0, 610000.0, 620000.0, 630000.0, 640000.0, 650000.0, 660000.0, 670000.0, 680000.0,
        690000.0, 700000.0,
        710000.0, 720000.0, 730000.0, 740000.0, 750000.0, 760000.0, 770000.0, 780000.0, 790000.0, 800000.0, 810000.0,
        820000.0, 830000.0,
        840000.0, 850000.0, 860000.0, 870000.0, 880000.0, 890000.0, 900000.0, 910000.0, 920000.0, 930000.0, 940000.0,
        950000.0, 960000.0,
        970000.0, 980000.0, 990000.0, 1000000.0, 1010000.0, 1020000.0, 1030000.0, 1040000.0, 1050000.0, 1060000.0,
        1070000.0, 1080000.0,
        1090000.0, 1100000.0, 1110000.0, 1120000.0, 1130000.0, 1140000.0, 1150000.0, 1160000.0, 1170000.0, 1180000.0,
        1190000.0, 1200000.0,
        1210000.0, 1220000.0, 1230000.0, 1240000.0, 1250000.0, 1260000.0, 1270000.0, 1280000.0, 1290000.0, 1300000.0,
        1310000.0, 1320000.0,
        1330000.0, 1340000.0, 1350000.0, 1360000.0, 1370000.0, 1380000.0, 1390000.0, 1400000.0, 1410000.0, 1420000.0,
        1430000.0, 1440000.0,
        1450000.0, 1460000.0, 1470000.0, 1480000.0, 1490000.0, 1500000.0, 1510000.0, 1520000.0, 1530000.0, 1540000.0,
        1550000.0, 1560000.0,
        1570000.0, 1580000.0, 1590000.0, 1600000.0, 1610000.0, 1620000.0, 1630000.0, 1640000.0, 1650000.0, 1660000.0,
        1670000.0, 1680000.0,
        1690000.0, 1700000.0, 1710000.0, 1720000.0, 1730000.0, 1740000.0, 1750000.0, 1760000.0, 1770000.0, 1780000.0,
        1790000.0, 1800000.0,
        1810000.0, 1820000.0, 1830000.0, 1840000.0, 1850000.0, 1860000.0, 1870000.0, 1880000.0, 1890000.0, 1900000.0,
        1910000.0, 1920000.0,
        1930000.0, 1940000.0, 1950000.0, 1960000.0, 1970000.0, 1980000.0, 1990000.0, 2000000.0, 2010000.0, 2020000.0,
        2030000.0, 2040000.0,
        2050000.0, 2060000.0, 2070000.0, 2080000.0, 2090000.0, 2100000.0, 2110000.0, 2120000.0, 2130000.0, 2140000.0,
        2150000.0, 2160000.0,
        2170000.0, 2180000.0, 2190000.0, 2200000.0, 2210000.0, 2220000.0, 2230000.0, 2240000.0, 2250000.0, 2260000.0,
        2270000.0, 2280000.0,
        2290000.0, 2300000.0, 2310000.0, 2320000.0, 2330000.0, 2340000.0, 2350000.0, 2360000.0, 2370000.0, 2380000.0,
        2390000.0, 2400000.0,
        2410000.0, 2420000.0, 2430000.0, 2440000.0, 2450000.0, 2460000.0, 2470000.0, 2480000.0, 2490000.0, 2500000.0 };

    /** Temperatures table for density logarithm interpolation. */
    public static final double[] TEMP_TABLE =
        { 200.0, 225.0, 250.0, 275.0, 300.0, 325.0, 350.0, 375.0, 400.0, 425.0, 450.0, 475.0,
            500.0, 525.0, 550.0, 575.0, 600.0, 625.0, 650.0, 675.0, 700.0, 725.0, 750.0, 775.0, 800.0, 825.0, 850.0,
            875.0, 900.0, 925.0,
            950.0, 975.0, 1000.0, 1025.0, 1050.0, 1075.0, 1100.0, 1125.0, 1150.0, 1175.0, 1200.0, 1225.0, 1250.0,
            1275.0, 1300.0, 1325.0,
            1350.0, 1375.0, 1400.0, 1425.0, 1450.0, 1475.0, 1500.0, 1525.0, 1550.0, 1575.0, 1600.0, 1625.0, 1650.0,
            1675.0, 1700.0, 1725.0,
            1750.0, 1775.0, 1800.0, 1825.0, 1850.0, 1875.0, 1900.0, 1925.0, 1950.0, 1975.0, 2000.0, 2025.0, 2050.0,
            2075.0, 2100.0, 2125.0,
            2150.0, 2175.0, 2200.0, 2225.0, 2250.0, 2275.0, 2300.0, 2325.0, 2350.0, 2375.0, 2400.0, 2425.0, 2450.0,
            2475.0, 2500.0, 2525.0,
            2550.0, 2575.0, 2600.0, 2625.0, 2650.0, 2675.0, 2700.0, 2725.0, 2750.0, 2775.0, 2800.0, 2825.0, 2850.0,
            2875.0, 2900.0, 2925.0,
            2950.0, 2975.0, 3000.0 };

    /** Serializable UID. */
    private static final long serialVersionUID = -9204710564642175400L;

    /** Folder containing the density interpolation map. */
    private static final String J77_RHO_FOLDER = "/META-INF/jacchia77_tables";

    /** Density map file. */
    private static final String J77_RHO_FILE_PATH = J77_RHO_FOLDER + "/J77_densityMap.txt";

    /** Temperature map file. */
    private static final String J77_TEMP_FILE_PATH = J77_RHO_FOLDER + "/J77_temperatureMap.txt";

    /** Mean molar mass map file. */
    private static final String J77_WM_FILE_PATH = J77_RHO_FOLDER + "/J77_molWeightMap.txt";

    /** 2D interpolator for density map. */
    private final BiLinearIntervalsFunction rhoFunction;

    /** 2D interpolator for temperature map. */
    private BiLinearIntervalsFunction tempFunction;

    /** 2D interpolator for mean molar mass map. */
    private BiLinearIntervalsFunction meanMolarMassFunction;

    /**
     * Constructor.<br>
     * The density map is directly initialized.
     */
    public Jacchia77Data() {

        // Load file
        final InputStream fileRho = Jacchia77Data.class.getResourceAsStream(J77_RHO_FILE_PATH);

        // Read file
        final MatrixFileReader readerRho = new MatrixFileReader(fileRho);

        // Initialization of the density interpolation map
        final double[][] rhoMap = readerRho.getData();

        // Initialization of the 2D interpolator for density map
        this.rhoFunction = new BiLinearIntervalsInterpolator().interpolate(TEMP_TABLE, ALT_TABLE, rhoMap);
    }

    /**
     * Initialization of temperature and mean molar mass interpolation maps.
     */
    public void initTempWmMaps() {

        // Check if the temperature map is already initialized or not
        if (this.tempFunction == null) {
            // Load files
            final InputStream fileTemp = Jacchia77Data.class.getResourceAsStream(J77_TEMP_FILE_PATH);
            final InputStream fileWm = Jacchia77Data.class.getResourceAsStream(J77_WM_FILE_PATH);

            // Read files
            final MatrixFileReader readerTemp = new MatrixFileReader(fileTemp);
            final MatrixFileReader readerWm = new MatrixFileReader(fileWm);
            final double[][] temperatureMap = readerTemp.getData();
            final double[][] meanMassMolarMap = readerWm.getData();

            // Initialize interpolators
            this.tempFunction = new BiLinearIntervalsInterpolator().interpolate(TEMP_TABLE, ALT_TABLE, temperatureMap);
            this.meanMolarMassFunction =
                new BiLinearIntervalsInterpolator().interpolate(TEMP_TABLE, ALT_TABLE, meanMassMolarMap);
        }
    }

    /**
     * Getter for the 2D interpolator for density map.
     *
     * @return the 2D interpolator for density map
     */
    public BiLinearIntervalsFunction getRhoFunction() {
        return this.rhoFunction;
    }

    /**
     * Getter for the 2D interpolator for temperature map.
     *
     * @return the 2D interpolator for temperature map
     */
    public BiLinearIntervalsFunction getTempFunction() {
        return this.tempFunction;
    }

    /**
     * Getter for the 2D interpolator for mean molar mass map.
     *
     * @return the 2D interpolator for mean molar mass map
     */
    public BiLinearIntervalsFunction getMeanMolarMassFunction() {
        return this.meanMolarMassFunction;
    }

    /**
     * Private utility class to store data from a txt file in matrices.
     */
    private static class MatrixFileReader {

        /** File path. */
        private final InputStream inputStream;

        /** Data. */
        private double[][] data;

        /**
         * Reading of a text file containing a matrix.
         * 
         * @param myFilePath
         *        path to the file
         * @throws InternalError
         *         if the file is not found or cannot be read (shouldn't happened as the files managed by this private
         *         class aren't user-specified
         */
        public MatrixFileReader(final InputStream myFilePath) {
            this.inputStream = myFilePath;

            try {
                parseFile();
            } catch (final IOException e) {
                // If the file is not found or cannot be read
                // Shouldn't happened as the files managed by this private class aren't user-specified
                throw new InternalError(e);
            }
        }

        /**
         * Get the matrix contained in the file.<br>
         * data[0] first line of the file<br>
         * data[1] second line of the file<br>
         * etc...
         * 
         * @return data
         */
        public double[][] getData() {
            return this.data;
        }

        /**
         * Parse the file.
         * 
         * @throws IOException
         *         if parsing failed
         */
        private void parseFile() throws IOException {
            // Initialization
            final BufferedReader reader = new BufferedReader(new InputStreamReader(this.inputStream));
            String line;
            final List<String> items = new ArrayList<>();

            // Read lines
            StringTokenizer splitter;
            while ((line = reader.readLine()) != null) {
                if (!line.contains("#")) {
                    items.add(line);
                }
            }

            final int nbLig = items.size();
            int nbCol = 0;

            // Store data in array
            final String firstLigne = items.get(0);
            splitter = new StringTokenizer(firstLigne, " ");

            nbCol = splitter.countTokens();

            final double[][] dataTransposed = new double[nbCol][nbLig];
            int counter = 0;
            for (final String item : items) {
                splitter = new StringTokenizer(item, " ");
                for (int i = 0; i < nbCol; i++) {
                    dataTransposed[i][counter] = Double.parseDouble((String) splitter.nextElement());
                }
                counter++;
            }

            // Retrieve transposed data
            this.data = new double[nbLig][nbCol];
            for (int i = 0; i < nbLig; i++) {
                for (int j = 0; j < nbCol; j++) {
                    this.data[i][j] = dataTransposed[j][i];
                }
            }

            // Close reader
            reader.close();
        }
    }
}
