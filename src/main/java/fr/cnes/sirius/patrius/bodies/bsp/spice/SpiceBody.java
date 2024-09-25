/**
 * Copyright 2023-2023 CNES
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
 * HISTORY
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: Spice body list

/**
 * This class allows the translation of SPICE body identifiers to the body names and vice-versa. It will look first in
 * the built-in database, and then in the kernel manager if it has read bodies information.
 * <p>
 * This class is based on the bodc2n, bodc2s and zzbodtrn files of the SPICE library.
 * </p>
 *
 * @author T0281925
 *
 * @since 4.11
 */
public final class SpiceBody {

    /**
     * Boolean indicating if any method has been called yet.
     * For initialization.
     */
    private static boolean first = true;

    /** True if there is no data in the kernel manager. */
    private static boolean noData = true;

    // SpiceBody state counter and POOL state counter.
    /** Subsystem counter array. */
    private static CounterArray subctr;

    /** User counter array. */
    private static CounterArray pulctr;

    // Constants
    /** Number of variables to watch in the kernel manager. */
    private static final int NWATCH = 2;

    /** Name of variables to watch in the kernel manager. */
    private static final String[] WNAMES = { "NAIF_BODY_NAME", "NAIF_BODY_CODE" };

    /** Size of the internal id-name database lists. */
    private static final int MAXBOD = 853;

    /** Space for the kernel id-name lists. */
    private static final int NROOM = 14983;

    /** Class name (agent for Pool). */
    private static final String CLASSNAME = "SpiceBody";

    /** Count of default SPICE mapping assignments. */
    private static final int NPERM = 695;

    // Body-code lists
    /** Table of ids in the database. */
    private static int[] defcod = new int[NPERM];

    /** Table of normalized names in the database. */
    private static String[] defnor = new String[NPERM];

    /** Table of original names in the database. */
    private static String[] defnam = new String[NPERM];

    /** Constructed list of body names (no repetition). */
    private static List<String> nameList;

    /** Constructed list of normalized body names (no repetition). */
    private static List<String> norList;

    /** Constructed list of body ids (no repetition). */
    private static List<Integer> idList;

    /** Table of names in the kernel database. */
    private static String[] kernam = new String[NROOM];

    /** Table of normalized names in the kernel database. */
    private static String[] kernor = new String[NROOM];

    /** Table of ids in the kernel database. */
    private static int[] kercod = new int[NROOM];

    /** Constructed list of body names in the kernel database (no repetition). */
    private static List<String> kernamList;

    /** Constructed list of normalized body names in the kernel database (no repetition). */
    private static List<String> kernorList;

    /** Constructed list of body ids in the kernel database (no repetition). */
    private static List<Integer> keridList;

    /** Regex to trim string and not allow more than 1 space in a row. */
    private static final String REGEX_SPACE = "^ +|( )+";

    /**
     * Constructor.
     */
    private SpiceBody() {
        // Nothing to do
    }

    /**
     * Private routine for all initialization needed the first time a method from the class is called.
     * 
     * @throws PatriusException
     *         if there is a problem with the counter array or the pool initialization
     */
    private static void init() throws PatriusException {
        // Initialize the arrays
        Arrays.fill(kernam, "");
        Arrays.fill(kernor, "");
        Arrays.fill(kercod, 0);
        // Populate the initial values of the DEFNAM, DEFNOR, and DEFCOD arrays from the built-in code list.
        populateBodies();

        // Populate the initial built-in code-name lists.
        // Loop through the input arrays to populate lists. We do it backwards to pick and register only the highest
        // priority (latest) values for each normalized name.
        nameList = new ArrayList<>(MAXBOD);
        norList = new ArrayList<>(MAXBOD);
        idList = new ArrayList<>(MAXBOD);
        for (int i = defnor.length - 1; i >= 0; i--) {
            if (!norList.contains(defnor[i])) {
                norList.add(defnor[i]);
                nameList.add(defnam[i]);
                idList.add(Integer.valueOf(defcod[i]));
            }
        }

        // Initialize counter arrays
        subctr = new CounterArray("SUBSYSTEM");
        pulctr = new CounterArray("USER");

        // Set up the watchers for the kernel pool name-code mapping variables
        PoolSpice.setWatch(CLASSNAME, NWATCH, WNAMES);

        // Set FIRST to .FALSE. to not repeat initialization again
        first = false;
    }

    /**
     * Translate the SPICE integer code of a body into a common name for that body.<br>
     * This routine is a translation of the BODC2N routine from the Spice library
     * 
     * @param code
     *        Integer ID code to be translated into a name
     * @return a common name for the body identified by code (if the code is not found, an empty string is returned)
     * @throws PatriusException
     *         if there is a problem in the Pool
     */
    public static String bodyCode2Name(final int code) throws PatriusException {
        // Assume we will not find the name we seek.
        boolean extker = false;
        String name = "";

        // On the first pass through this entry point, initialize the built-in arrays, set the kernel pool watchers, and
        // state counters
        if (first) {
            init();
        }

        // Check for updates to the kernel pool variables. Note: the first call to ZZCVPOOL after initialization always
        // returns .TRUE. for update. This ensures that any initial assignments are properly processed.
        final boolean update = PoolSpice.checkUpdatesIfCounterUpdate(pulctr, CLASSNAME);

        if (update || noData) {
            // Conservatively increment the SpiceBody state counter in expectation of successful update.
            subctr.increment();

            // Update kernel pool mapping lists and hashes.
            extker = zzbodker();
            if (kernam.length == 0) {
                noData = true;
                name = "";
            }

            noData = false;
        }

        // If necessary, first examine the contents of the kernel pool name-code mapping list
        if (extker) {
            // Check if this code is in the kernel pool codes list.
            final int index = keridList.indexOf(Integer.valueOf(code));

            if (index >= 0) {
                name = kernamList.get(index);
            }
        }

        // If we reach here, we did not find this code in the kernel pool codes list. Check the built-in codes list.
        final int index = idList.indexOf(Integer.valueOf(code));

        // If we find a match, verify that it is not masked by a kernel pool entry before returning.
        if (index >= 0) {
            if (extker) {
                // Only bother performing this check if there are actually mappings present in the kernel pool lists.
                final int j = kernorList.indexOf(norList.get(index));

                if (j >= 0) {
                    // This name is defined in the kernel pool mappings. Set return " " as not found, as the contents of
                    // the kernel pool have higher precedence than any entries in the built-in mapping list.
                    name = "";
                } else {
                    // No match for this name in the kernel pool mapping list. Return the name.
                    name = nameList.get(index);
                }
            } else {
                // No kernel pool mappings were defined, simply return the name.
                name = nameList.get(index);
            }
        }

        return name;
    }

    /**
     * Translate the name of a body or object to the corresponding SPICE ID code.
     * <p>
     * Based on the BODN2C routine from the SPICE library
     * </p>
     * 
     * @param name
     *        Body name to be translated into a SPICE ID code
     * @param found
     *        (out) returns if the body was found
     * @return SPICE integer ID code for the named body
     * @throws PatriusException
     *         if there is a Pool or initialization problem
     */
    public static int bodyName2Code(final String name, final boolean[] found) throws PatriusException {
        // On the first pass through this entry point, initialize the built-in arrays, set the kernel pool watchers, and
        // state counters.
        if (first) {
            init();
        }
        // Check for updates to the kernel pool variables. Note: the first call to ZZCVPOOL after initialization always
        // returns .TRUE. for update. This ensures that any initial assignments are properly processed.
        found[0] = false;
        final boolean update = PoolSpice.checkUpdatesIfCounterUpdate(pulctr, CLASSNAME);
        boolean extker = false;

        if (update || noData) {
            // Conservatively increment the SpiceBody state counter in expectation of successful update.
            subctr.increment();

            // Update kernel pool mapping lists and hashes.
            extker = zzbodker();
            if (kernam.length == 0) {
                noData = true;
                return Integer.MAX_VALUE;
            }

            noData = false;
        }

        // Normalize the input argument NAME. We will look this normalized name up in the built-in and kernel pool names
        // lists.
        final String nname = normalize(name);

        // If necessary, first examine the contents of the kernel pool name-code mapping list.
        if (extker) {
            // Check if this name is the kernel pool names list
            final int index = kernorList.indexOf(nname);

            if (index >= 0) {
                found[0] = true;
                return keridList.get(index).intValue();
            }
        }

        // If we reach here, we did not find this name in the kernel pool names list. Check the built-in names list.
        final int index = norList.indexOf(nname);

        if (index >= 0) {
            found[0] = true;
            return idList.get(index).intValue();
        }
        found[0] = false;
        return 0;
    }

    /**
     * Translate a body ID code to either the corresponding name or if no name to ID code mapping exists, the string
     * representation of the body ID value.
     * <p>
     * This routine is a translation of the BODC2S routine from the SPICE library.
     * </p>
     * 
     * @param code
     *        Integer ID code to be translated into a name
     * @return a common name for the body identified by code (if the code is not found, an empty string is returned)
     * @throws PatriusException
     *         if there is a problem translating the code to the name
     */
    public static String bodyCode2String(final int code) throws PatriusException {
        if (first) {
            init();
        }
        // Attempt to translate the input CODE to a name. Use the bodyCode2Name method.
        String name = bodyCode2Name(code);

        if (name.isEmpty()) {
            // If execution reaches this level, the SPICE body ID to name mapping lacks an assignment for CODE. Convert
            // CODE to a string representation of the integer value.
            name = String.valueOf(code);
        }

        // Return the string
        return name;
    }

    /**
     * Translate a string representing a body name into its code. If the name is a string representation of a integer,
     * the integer represented is returned.
     * 
     * @param name
     *        String representing a body name
     * @param found (out) return if the body was found
     * @return code corresponding to the string (0 if it was not found)
     * @throws PatriusException
     *         if there is a problem translating the name into the code
     */
    public static int bodyString2Code(final String name, final boolean[] found) throws PatriusException {
        if (first) {
            init();
        }
        // Attempt to translate the input name to an integer code.
        int code = bodyName2Code(name, found);

        if (!found[0]) {
            // It's possible the name is a string representation of an integer, for example, '999'. If so, find the
            // equivalent datum of INTEGER type.
            try {
                code = Integer.parseInt(name);
                found[0] = true;
            } catch (final NumberFormatException e) {
                found[0] = false;
                code = 0;
            }

        }
        return code;
    }

    /**
     * Translate a string containing a body name or ID code to an integer code, but bypass calling
     * {@link SpiceBody#bodyString2Code(String)} and return saved values provided by the caller if the name is the same
     * as the saved name and the SpiceBody state did not change.
     * <p>
     * Based on the routine ZZBODS2C routine of the SPICE library.
     * </p>
     * 
     * @param usrctr
     *        (in/out) SpiceBody state counter saved by the caller
     * @param savName
     *        (in/out) Body name saved by the caller
     * @param savCode
     *        (in/out) Body ID code saved by the caller
     * @param savFound
     *        (in/out) Translation success flag saved in the caller
     * @param name
     *        Body name
     * @param found
     *        (out) Translation success flag
     * @return the body ID code
     * @throws PatriusException
     *         if there is a problem checking the state or translating
     */
    public static int bodyString2CodeBypass(final CounterArray usrctr, final String[] savName, final int[] savCode,
                                            final boolean[] savFound, final String name, final boolean[] found)
        throws PatriusException {
        if (first) {
            init();
        }
        // Check/update SpiceBody state counter.
        final boolean update = checkUpdateCounterTracked(usrctr);

        // Check update flag, saved found flag, and saved name against the input
        if (!update && savFound[0] && name.equals(savName[0])) {
            // No change in body-name mapping state, the saved name was successfully resolved earlier, and input and
            // saved names are the same. Return saved ID and FOUND.
            found[0] = savFound[0];
            return savCode[0];
        }

        // Body-name mapping state changed, or the saved name was never successfully resolved earlier, or input and
        // saved names are different. Call BODS2C to look up ID and FOUND and reset saved values.
        final int code = bodyString2Code(name, found);
        savName[0] = name;
        savCode[0] = code;
        savFound[0] = found[0];
        return code;
    }

    /**
     * Check and update the ZZBODTRN state counter tracked by a caller (user) routine.
     * <p>
     * Based on ZZBCTRCK routine of the SPICE library.
     * </p>
     * 
     * @param usrctr
     *        SpiceBody state counter tracked by the caller
     * @return a flag indicating if input counter was updated
     * @throws PatriusException
     *         if the counter is not about to overflow
     */
    private static boolean checkUpdateCounterTracked(final CounterArray usrctr) throws PatriusException {
        // Check/update the state counter
        final boolean update = PoolSpice.checkUpdatesIfCounterUpdate(pulctr, CLASSNAME);

        if (update || noData) {
            // Conservatively increment the SpiceBody state counter in expectation of successful update.
            subctr.increment();

            // Update kernel pool mapping lists and hashes.
            zzbodker();
            if (kernam.length == 0) {
                noData = true;
                return update;
            }

            noData = false;
        }

        return usrctr.checkAndUpdate(subctr);
    }

    /**
     * This routine processes the kernel pool vectors NAIF_BODY_NAME and NAIF_BODY_CODE into the lists and hashes
     * required by ZZBODTRN to successfully compute code-name mappings.
     * 
     * @return a boolean indicating if we found something in the kernel pool
     * @throws PatriusException
     *         if there is a problem in the pool or with the information retrieved
     */
    private static boolean zzbodker() throws PatriusException {
        final String nbc = WNAMES[1];
        final String nbn = WNAMES[0];

        // Until the code below proves otherwise, we shall assume we lack kernel pool name/code mappings.
        boolean extker = false;

        // Check for the external body ID variables in the kernel pool.
        final ArrayList<String> auxnam = (ArrayList<String>) PoolSpice.getStrVals(nbn);
        if (auxnam.isEmpty()) {
            auxnam.toArray(kernam);
        }
        final ArrayList<Integer> auxcod = (ArrayList<Integer>) PoolSpice.getIntVals(nbc);
        for (int i = 0; i < auxcod.size(); i++) {
            kercod[i] = auxcod.get(i).intValue();
        }

        if (auxnam.size() != auxcod.size()) {
            throw new PatriusException(PatriusMessages.PDB_NOT_SAME_SIZE);

        } else if (auxnam.isEmpty()) {
            // Both lists are absent. Return.
            return extker;
        }

        // Compute the canonical member of the equivalence class of NAMES, NORNAM. This normalization compresses groups
        // of spaces into a single space, left justifies the string, and upper-cases the contents. While passing through
        // the NAMES array, look for any blank strings and signal an appropriate error.
        for (int i = 0; i < auxnam.size(); i++) {
            // Check for blank strings
            if (kernam[i].isEmpty()) {
                throw new PatriusException(PatriusMessages.PDB_EMPTY_STRING);
            }
            kernor[i] = normalize(kernam[i]);
        }

        // Populate the lists
        kernamList = new ArrayList<>(NROOM);
        kernorList = new ArrayList<>(NROOM);
        keridList = new ArrayList<>(NROOM);
        for (int i = kernor.length - 1; i == 0; i--) {
            if (!kernorList.contains(kernor[i])) {
                kernorList.add(kernor[i]);
                kernamList.add(kernam[i]);
                keridList.add(Integer.valueOf(kercod[i]));
            }
        }

        // If we arrived here, set extker to true
        extker = true;
        return extker;
    }

    /**
     * Populate the bodies arrays.
     */
    //CHECKSTYLE: stop MethodLength check
    //CHECKSTYLE: stop CommentRatio check
    //CHECKSTYLE: stop MultipleStringLiterals check
    //Reason: Spice code kept as such
    private static void populateBodies() {
        //CHECKSTYLE: resume MethodLength check
        //CHECKSTYLE: resume CommentRatio check
        defcod[0] = 0;
        defnam[0] = "SOLAR_SYSTEM_BARYCENTER";

        defcod[1] = 0;
        defnam[1] = "SSB";

        defcod[2] = 0;
        defnam[2] = "SOLAR SYSTEM BARYCENTER";

        defcod[3] = 1;
        defnam[3] = "MERCURY_BARYCENTER";

        defcod[4] = 1;
        defnam[4] = "MERCURY BARYCENTER";

        defcod[5] = 2;
        defnam[5] = "VENUS_BARYCENTER";

        defcod[6] = 2;
        defnam[6] = "VENUS BARYCENTER";

        defcod[7] = 3;
        defnam[7] = "EARTH_BARYCENTER";

        defcod[8] = 3;
        defnam[8] = "EMB";

        defcod[9] = 3;
        defnam[9] = "EARTH MOON BARYCENTER";

        defcod[10] = 3;
        defnam[10] = "EARTH-MOON BARYCENTER";

        defcod[11] = 3;
        defnam[11] = "EARTH BARYCENTER";

        defcod[12] = 4;
        defnam[12] = "MARS_BARYCENTER";

        defcod[13] = 4;
        defnam[13] = "MARS BARYCENTER";

        defcod[14] = 5;
        defnam[14] = "JUPITER_BARYCENTER";

        defcod[15] = 5;
        defnam[15] = "JUPITER BARYCENTER";

        defcod[16] = 6;
        defnam[16] = "SATURN_BARYCENTER";

        defcod[17] = 6;
        defnam[17] = "SATURN BARYCENTER";

        defcod[18] = 7;
        defnam[18] = "URANUS_BARYCENTER";

        defcod[19] = 7;
        defnam[19] = "URANUS BARYCENTER";

        defcod[20] = 8;
        defnam[20] = "NEPTUNE_BARYCENTER";

        defcod[21] = 8;
        defnam[21] = "NEPTUNE BARYCENTER";

        defcod[22] = 9;
        defnam[22] = "PLUTO_BARYCENTER";

        defcod[23] = 9;
        defnam[23] = "PLUTO BARYCENTER";

        defcod[24] = 10;
        defnam[24] = "SUN";

        defcod[25] = 199;
        defnam[25] = "MERCURY";

        defcod[26] = 299;
        defnam[26] = "VENUS";

        defcod[27] = 399;
        defnam[27] = "EARTH";

        defcod[28] = 301;
        defnam[28] = "MOON";

        defcod[29] = 499;
        defnam[29] = "MARS";

        defcod[30] = 401;
        defnam[30] = "PHOBOS";

        defcod[31] = 402;
        defnam[31] = "DEIMOS";

        defcod[32] = 599;
        defnam[32] = "JUPITER";

        defcod[33] = 501;
        defnam[33] = "IO";

        defcod[34] = 502;
        defnam[34] = "EUROPA";

        defcod[35] = 503;
        defnam[35] = "GANYMEDE";

        defcod[36] = 504;
        defnam[36] = "CALLISTO";

        defcod[37] = 505;
        defnam[37] = "AMALTHEA";

        defcod[38] = 506;
        defnam[38] = "HIMALIA";

        defcod[39] = 507;
        defnam[39] = "ELARA";

        defcod[40] = 508;
        defnam[40] = "PASIPHAE";

        defcod[41] = 509;
        defnam[41] = "SINOPE";

        defcod[42] = 510;
        defnam[42] = "LYSITHEA";

        defcod[43] = 511;
        defnam[43] = "CARME";

        defcod[44] = 512;
        defnam[44] = "ANANKE";

        defcod[45] = 513;
        defnam[45] = "LEDA";

        defcod[46] = 514;
        defnam[46] = "THEBE";

        defcod[47] = 515;
        defnam[47] = "ADRASTEA";

        defcod[48] = 516;
        defnam[48] = "METIS";

        defcod[49] = 517;
        defnam[49] = "CALLIRRHOE";

        defcod[50] = 518;
        defnam[50] = "THEMISTO";

        defcod[51] = 519;
        defnam[51] = "MEGACLITE";

        defcod[52] = 520;
        defnam[52] = "TAYGETE";

        defcod[53] = 521;
        defnam[53] = "CHALDENE";

        defcod[54] = 522;
        defnam[54] = "HARPALYKE";

        defcod[55] = 523;
        defnam[55] = "KALYKE";

        defcod[56] = 524;
        defnam[56] = "IOCASTE";

        defcod[57] = 525;
        defnam[57] = "ERINOME";

        defcod[58] = 526;
        defnam[58] = "ISONOE";

        defcod[59] = 527;
        defnam[59] = "PRAXIDIKE";

        defcod[60] = 528;
        defnam[60] = "AUTONOE";

        defcod[61] = 529;
        defnam[61] = "THYONE";

        defcod[62] = 530;
        defnam[62] = "HERMIPPE";

        defcod[63] = 531;
        defnam[63] = "AITNE";

        defcod[64] = 532;
        defnam[64] = "EURYDOME";

        defcod[65] = 533;
        defnam[65] = "EUANTHE";

        defcod[66] = 534;
        defnam[66] = "EUPORIE";

        defcod[67] = 535;
        defnam[67] = "ORTHOSIE";

        defcod[68] = 536;
        defnam[68] = "SPONDE";

        defcod[69] = 537;
        defnam[69] = "KALE";

        defcod[70] = 538;
        defnam[70] = "PASITHEE";

        defcod[71] = 539;
        defnam[71] = "HEGEMONE";

        defcod[72] = 540;
        defnam[72] = "MNEME";

        defcod[73] = 541;
        defnam[73] = "AOEDE";

        defcod[74] = 542;
        defnam[74] = "THELXINOE";

        defcod[75] = 543;
        defnam[75] = "ARCHE";

        defcod[76] = 544;
        defnam[76] = "KALLICHORE";

        defcod[77] = 545;
        defnam[77] = "HELIKE";

        defcod[78] = 546;
        defnam[78] = "CARPO";

        defcod[79] = 547;
        defnam[79] = "EUKELADE";

        defcod[80] = 548;
        defnam[80] = "CYLLENE";

        defcod[81] = 549;
        defnam[81] = "KORE";

        defcod[82] = 550;
        defnam[82] = "HERSE";

        defcod[83] = 553;
        defnam[83] = "DIA";

        defcod[84] = 699;
        defnam[84] = "SATURN";

        defcod[85] = 601;
        defnam[85] = "MIMAS";

        defcod[86] = 602;
        defnam[86] = "ENCELADUS";

        defcod[87] = 603;
        defnam[87] = "TETHYS";

        defcod[88] = 604;
        defnam[88] = "DIONE";

        defcod[89] = 605;
        defnam[89] = "RHEA";

        defcod[90] = 606;
        defnam[90] = "TITAN";

        defcod[91] = 607;
        defnam[91] = "HYPERION";

        defcod[92] = 608;
        defnam[92] = "IAPETUS";

        defcod[93] = 609;
        defnam[93] = "PHOEBE";

        defcod[94] = 610;
        defnam[94] = "JANUS";

        defcod[95] = 611;
        defnam[95] = "EPIMETHEUS";

        defcod[96] = 612;
        defnam[96] = "HELENE";

        defcod[97] = 613;
        defnam[97] = "TELESTO";

        defcod[98] = 614;
        defnam[98] = "CALYPSO";

        defcod[99] = 615;
        defnam[99] = "ATLAS";

        defcod[100] = 616;
        defnam[100] = "PROMETHEUS";

        defcod[101] = 617;
        defnam[101] = "PANDORA";

        defcod[102] = 618;
        defnam[102] = "PAN";

        defcod[103] = 619;
        defnam[103] = "YMIR";

        defcod[104] = 620;
        defnam[104] = "PAALIAQ";

        defcod[105] = 621;
        defnam[105] = "TARVOS";

        defcod[106] = 622;
        defnam[106] = "IJIRAQ";

        defcod[107] = 623;
        defnam[107] = "SUTTUNGR";

        defcod[108] = 624;
        defnam[108] = "KIVIUQ";

        defcod[109] = 625;
        defnam[109] = "MUNDILFARI";

        defcod[110] = 626;
        defnam[110] = "ALBIORIX";

        defcod[111] = 627;
        defnam[111] = "SKATHI";

        defcod[112] = 628;
        defnam[112] = "ERRIAPUS";

        defcod[113] = 629;
        defnam[113] = "SIARNAQ";

        defcod[114] = 630;
        defnam[114] = "THRYMR";

        defcod[115] = 631;
        defnam[115] = "NARVI";

        defcod[116] = 632;
        defnam[116] = "METHONE";

        defcod[117] = 633;
        defnam[117] = "PALLENE";

        defcod[118] = 634;
        defnam[118] = "POLYDEUCES";

        defcod[119] = 635;
        defnam[119] = "DAPHNIS";

        defcod[120] = 636;
        defnam[120] = "AEGIR";

        defcod[121] = 637;
        defnam[121] = "BEBHIONN";

        defcod[122] = 638;
        defnam[122] = "BERGELMIR";

        defcod[123] = 639;
        defnam[123] = "BESTLA";

        defcod[124] = 640;
        defnam[124] = "FARBAUTI";

        defcod[125] = 641;
        defnam[125] = "FENRIR";

        defcod[126] = 642;
        defnam[126] = "FORNJOT";

        defcod[127] = 643;
        defnam[127] = "HATI";

        defcod[128] = 644;
        defnam[128] = "HYRROKKIN";

        defcod[129] = 645;
        defnam[129] = "KARI";

        defcod[130] = 646;
        defnam[130] = "LOGE";

        defcod[131] = 647;
        defnam[131] = "SKOLL";

        defcod[132] = 648;
        defnam[132] = "SURTUR";

        defcod[133] = 649;
        defnam[133] = "ANTHE";

        defcod[134] = 650;
        defnam[134] = "JARNSAXA";

        defcod[135] = 651;
        defnam[135] = "GREIP";

        defcod[136] = 652;
        defnam[136] = "TARQEQ";

        defcod[137] = 653;
        defnam[137] = "AEGAEON";

        defcod[138] = 799;
        defnam[138] = "URANUS";

        defcod[139] = 701;
        defnam[139] = "ARIEL";

        defcod[140] = 702;
        defnam[140] = "UMBRIEL";

        defcod[141] = 703;
        defnam[141] = "TITANIA";

        defcod[142] = 704;
        defnam[142] = "OBERON";

        defcod[143] = 705;
        defnam[143] = "MIRANDA";

        defcod[144] = 706;
        defnam[144] = "CORDELIA";

        defcod[145] = 707;
        defnam[145] = "OPHELIA";

        defcod[146] = 708;
        defnam[146] = "BIANCA";

        defcod[147] = 709;
        defnam[147] = "CRESSIDA";

        defcod[148] = 710;
        defnam[148] = "DESDEMONA";

        defcod[149] = 711;
        defnam[149] = "JULIET";

        defcod[150] = 712;
        defnam[150] = "PORTIA";

        defcod[151] = 713;
        defnam[151] = "ROSALIND";

        defcod[152] = 714;
        defnam[152] = "BELINDA";

        defcod[153] = 715;
        defnam[153] = "PUCK";

        defcod[154] = 716;
        defnam[154] = "CALIBAN";

        defcod[155] = 717;
        defnam[155] = "SYCORAX";

        defcod[156] = 718;
        defnam[156] = "PROSPERO";

        defcod[157] = 719;
        defnam[157] = "SETEBOS";

        defcod[158] = 720;
        defnam[158] = "STEPHANO";

        defcod[159] = 721;
        defnam[159] = "TRINCULO";

        defcod[160] = 722;
        defnam[160] = "FRANCISCO";

        defcod[161] = 723;
        defnam[161] = "MARGARET";

        defcod[162] = 724;
        defnam[162] = "FERDINAND";

        defcod[163] = 725;
        defnam[163] = "PERDITA";

        defcod[164] = 726;
        defnam[164] = "MAB";

        defcod[165] = 727;
        defnam[165] = "CUPID";

        defcod[166] = 899;
        defnam[166] = "NEPTUNE";

        defcod[167] = 801;
        defnam[167] = "TRITON";

        defcod[168] = 802;
        defnam[168] = "NEREID";

        defcod[169] = 803;
        defnam[169] = "NAIAD";

        defcod[170] = 804;
        defnam[170] = "THALASSA";

        defcod[171] = 805;
        defnam[171] = "DESPINA";

        defcod[172] = 806;
        defnam[172] = "GALATEA";

        defcod[173] = 807;
        defnam[173] = "LARISSA";

        defcod[174] = 808;
        defnam[174] = "PROTEUS";

        defcod[175] = 809;
        defnam[175] = "HALIMEDE";

        defcod[176] = 810;
        defnam[176] = "PSAMATHE";

        defcod[177] = 811;
        defnam[177] = "SAO";

        defcod[178] = 812;
        defnam[178] = "LAOMEDEIA";

        defcod[179] = 813;
        defnam[179] = "NESO";

        defcod[180] = 999;
        defnam[180] = "PLUTO";

        defcod[181] = 901;
        defnam[181] = "CHARON";

        defcod[182] = 902;
        defnam[182] = "NIX";

        defcod[183] = 903;
        defnam[183] = "HYDRA";

        defcod[184] = 904;
        defnam[184] = "KERBEROS";

        defcod[185] = 905;
        defnam[185] = "STYX";

        defcod[186] = -1;
        defnam[186] = "GEOTAIL";

        defcod[187] = -3;
        defnam[187] = "MOM";

        defcod[188] = -3;
        defnam[188] = "MARS ORBITER MISSION";

        defcod[189] = -5;
        defnam[189] = "AKATSUKI";

        defcod[190] = -5;
        defnam[190] = "VCO";

        defcod[191] = -5;
        defnam[191] = "PLC";

        defcod[192] = -5;
        defnam[192] = "PLANET-C";

        defcod[193] = -6;
        defnam[193] = "P6";

        defcod[194] = -6;
        defnam[194] = "PIONEER-6";

        defcod[195] = -7;
        defnam[195] = "P7";

        defcod[196] = -7;
        defnam[196] = "PIONEER-7";

        defcod[197] = -8;
        defnam[197] = "WIND";

        defcod[198] = -12;
        defnam[198] = "VENUS ORBITER";

        defcod[199] = -12;
        defnam[199] = "P12";

        defcod[200] = -12;
        defnam[200] = "PIONEER 12";

        defcod[201] = -12;
        defnam[201] = "LADEE";

        defcod[202] = -13;
        defnam[202] = "POLAR";

        defcod[203] = -18;
        defnam[203] = "MGN";

        defcod[204] = -18;
        defnam[204] = "MAGELLAN";

        defcod[205] = -18;
        defnam[205] = "LCROSS";

        defcod[206] = -20;
        defnam[206] = "P8";

        defcod[207] = -20;
        defnam[207] = "PIONEER-8";

        defcod[208] = -21;
        defnam[208] = "SOHO";

        defcod[209] = -23;
        defnam[209] = "P10";

        defcod[210] = -23;
        defnam[210] = "PIONEER-10";

        defcod[211] = -24;
        defnam[211] = "P11";

        defcod[212] = -24;
        defnam[212] = "PIONEER-11";

        defcod[213] = -25;
        defnam[213] = "LP";

        defcod[214] = -25;
        defnam[214] = "LUNAR PROSPECTOR";

        defcod[215] = -27;
        defnam[215] = "VK1";

        defcod[216] = -27;
        defnam[216] = "VIKING 1 ORBITER";

        defcod[217] = -28;
        defnam[217] = "JUPITER ICY MOONS EXPLORER";

        defcod[218] = -28;
        defnam[218] = "JUICE";

        defcod[219] = -29;
        defnam[219] = "STARDUST";

        defcod[220] = -29;
        defnam[220] = "SDU";

        defcod[221] = -29;
        defnam[221] = "NEXT";

        defcod[222] = -30;
        defnam[222] = "VK2";

        defcod[223] = -30;
        defnam[223] = "VIKING 2 ORBITER";

        defcod[224] = -30;
        defnam[224] = "DS-1";

        defcod[225] = -31;
        defnam[225] = "VG1";

        defcod[226] = -31;
        defnam[226] = "VOYAGER 1";

        defcod[227] = -32;
        defnam[227] = "VG2";

        defcod[228] = -32;
        defnam[228] = "VOYAGER 2";

        defcod[229] = -33;
        defnam[229] = "NEOS";

        defcod[230] = -33;
        defnam[230] = "NEO SURVEYOR";

        defcod[231] = -37;
        defnam[231] = "HYB2";

        defcod[232] = -37;
        defnam[232] = "HAYABUSA 2";

        defcod[233] = -37;
        defnam[233] = "HAYABUSA2";

        defcod[234] = -39;
        defnam[234] = "LUNAR POLAR HYDROGEN MAPPER";

        defcod[235] = -39;
        defnam[235] = "LUNAH-MAP";

        defcod[236] = -40;
        defnam[236] = "CLEMENTINE";

        defcod[237] = -41;
        defnam[237] = "MEX";

        defcod[238] = -41;
        defnam[238] = "MARS EXPRESS";

        defcod[239] = -43;
        defnam[239] = "IMAP";

        defcod[240] = -44;
        defnam[240] = "BEAGLE2";

        defcod[241] = -44;
        defnam[241] = "BEAGLE 2";

        defcod[242] = -45;
        defnam[242] = "JNSA";

        defcod[243] = -45;
        defnam[243] = "JANUS_A";

        defcod[244] = -46;
        defnam[244] = "MS-T5";

        defcod[245] = -46;
        defnam[245] = "SAKIGAKE";

        defcod[246] = -47;
        defnam[246] = "PLANET-A";

        defcod[247] = -47;
        defnam[247] = "SUISEI";

        defcod[248] = -47;
        defnam[248] = "GNS";

        defcod[249] = -47;
        defnam[249] = "GENESIS";

        defcod[250] = -48;
        defnam[250] = "HUBBLE SPACE TELESCOPE";

        defcod[251] = -48;
        defnam[251] = "HST";

        defcod[252] = -49;
        defnam[252] = "LUCY";

        defcod[253] = -53;
        defnam[253] = "MARS PATHFINDER";

        defcod[254] = -53;
        defnam[254] = "MPF";

        defcod[255] = -53;
        defnam[255] = "MARS ODYSSEY";

        defcod[256] = -53;
        defnam[256] = "MARS SURVEYOR 01 ORBITER";

        defcod[257] = -55;
        defnam[257] = "ULYSSES";

        defcod[258] = -57;
        defnam[258] = "LUNAR ICECUBE";

        defcod[259] = -58;
        defnam[259] = "VSOP";

        defcod[260] = -58;
        defnam[260] = "HALCA";

        defcod[261] = -59;
        defnam[261] = "RADIOASTRON";

        defcod[262] = -61;
        defnam[262] = "JUNO";

        defcod[263] = -62;
        defnam[263] = "EMM";

        defcod[264] = -62;
        defnam[264] = "EMIRATES MARS MISSION";

        defcod[265] = -64;
        defnam[265] = "ORX";

        defcod[266] = -64;
        defnam[266] = "OSIRIS-REX";

        defcod[267] = -65;
        defnam[267] = "MCOA";

        defcod[268] = -65;
        defnam[268] = "MARCO-A";

        defcod[269] = -66;
        defnam[269] = "VEGA 1";

        defcod[270] = -66;
        defnam[270] = "MCOB";

        defcod[271] = -66;
        defnam[271] = "MARCO-B";

        defcod[272] = -67;
        defnam[272] = "VEGA 2";

        defcod[273] = -68;
        defnam[273] = "MERCURY MAGNETOSPHERIC ORBITER";

        defcod[274] = -68;
        defnam[274] = "MMO";

        defcod[275] = -68;
        defnam[275] = "BEPICOLOMBO MMO";

        defcod[276] = -70;
        defnam[276] = "DEEP IMPACT IMPACTOR SPACECRAFT";

        defcod[277] = -72;
        defnam[277] = "JNSB";

        defcod[278] = -72;
        defnam[278] = "JANUS_B";

        defcod[279] = -74;
        defnam[279] = "MRO";

        defcod[280] = -74;
        defnam[280] = "MARS RECON ORBITER";

        defcod[281] = -76;
        defnam[281] = "CURIOSITY";

        defcod[282] = -76;
        defnam[282] = "MSL";

        defcod[283] = -76;
        defnam[283] = "MARS SCIENCE LABORATORY";

        defcod[284] = -77;
        defnam[284] = "GLL";

        defcod[285] = -77;
        defnam[285] = "GALILEO ORBITER";

        defcod[286] = -78;
        defnam[286] = "GIOTTO";

        defcod[287] = -79;
        defnam[287] = "SPITZER";

        defcod[288] = -79;
        defnam[288] = "SPACE INFRARED TELESCOPE FACILITY";

        defcod[289] = -79;
        defnam[289] = "SIRTF";

        defcod[290] = -81;
        defnam[290] = "CASSINI ITL";

        defcod[291] = -82;
        defnam[291] = "CAS";

        defcod[292] = -82;
        defnam[292] = "CASSINI";

        defcod[293] = -84;
        defnam[293] = "PHOENIX";

        defcod[294] = -85;
        defnam[294] = "LRO";

        defcod[295] = -85;
        defnam[295] = "LUNAR RECON ORBITER";

        defcod[296] = -85;
        defnam[296] = "LUNAR RECONNAISSANCE ORBITER";

        defcod[297] = -86;
        defnam[297] = "CH1";

        defcod[298] = -86;
        defnam[298] = "CHANDRAYAAN-1";

        defcod[299] = -90;
        defnam[299] = "CASSINI SIMULATION";

        defcod[300] = -93;
        defnam[300] = "NEAR EARTH ASTEROID RENDEZVOUS";

        defcod[301] = -93;
        defnam[301] = "NEAR";

        defcod[302] = -94;
        defnam[302] = "MO";

        defcod[303] = -94;
        defnam[303] = "MARS OBSERVER";

        defcod[304] = -94;
        defnam[304] = "MGS";

        defcod[305] = -94;
        defnam[305] = "MARS GLOBAL SURVEYOR";

        defcod[306] = -95;
        defnam[306] = "MGS SIMULATION";

        defcod[307] = -96;
        defnam[307] = "PARKER SOLAR PROBE";

        defcod[308] = -96;
        defnam[308] = "SPP";

        defcod[309] = -96;
        defnam[309] = "SOLAR PROBE PLUS";

        defcod[310] = -97;
        defnam[310] = "TOPEX/POSEIDON";

        defcod[311] = -98;
        defnam[311] = "NEW HORIZONS";

        defcod[312] = -107;
        defnam[312] = "TROPICAL RAINFALL MEASURING MISSION";

        defcod[313] = -107;
        defnam[313] = "TRMM";

        defcod[314] = -112;
        defnam[314] = "ICE";

        defcod[315] = -116;
        defnam[315] = "MARS POLAR LANDER";

        defcod[316] = -116;
        defnam[316] = "MPL";

        defcod[317] = -117;
        defnam[317] = "EDL DEMONSTRATOR MODULE";

        defcod[318] = -117;
        defnam[318] = "EDM";

        defcod[319] = -117;
        defnam[319] = "EXOMARS 2016 EDM";

        defcod[320] = -119;
        defnam[320] = "MARS_ORBITER_MISSION_2";

        defcod[321] = -119;
        defnam[321] = "MOM2";

        defcod[322] = -121;
        defnam[322] = "MERCURY PLANETARY ORBITER";

        defcod[323] = -121;
        defnam[323] = "MPO";

        defcod[324] = -121;
        defnam[324] = "BEPICOLOMBO MPO";

        defcod[325] = -127;
        defnam[325] = "MARS CLIMATE ORBITER";

        defcod[326] = -127;
        defnam[326] = "MCO";

        defcod[327] = -130;
        defnam[327] = "MUSES-C";

        defcod[328] = -130;
        defnam[328] = "HAYABUSA";

        defcod[329] = -131;
        defnam[329] = "SELENE";

        defcod[330] = -131;
        defnam[330] = "KAGUYA";

        defcod[331] = -135;
        defnam[331] = "DART";

        defcod[332] = -135;
        defnam[332] = "DOUBLE ASTEROID REDIRECTION TEST";

        defcod[333] = -140;
        defnam[333] = "EPOCH";

        defcod[334] = -140;
        defnam[334] = "DIXI";

        defcod[335] = -140;
        defnam[335] = "EPOXI";

        defcod[336] = -140;
        defnam[336] = "DEEP IMPACT FLYBY SPACECRAFT";

        defcod[337] = -142;
        defnam[337] = "TERRA";

        defcod[338] = -142;
        defnam[338] = "EOS-AM1";

        defcod[339] = -143;
        defnam[339] = "TRACE GAS ORBITER";

        defcod[340] = -143;
        defnam[340] = "TGO";

        defcod[341] = -143;
        defnam[341] = "EXOMARS 2016 TGO";

        defcod[342] = -144;
        defnam[342] = "SOLO";

        defcod[343] = -144;
        defnam[343] = "SOLAR ORBITER";

        defcod[344] = -146;
        defnam[344] = "LUNAR-A";

        defcod[345] = -148;
        defnam[345] = "DFLY";

        defcod[346] = -148;
        defnam[346] = "DRAGONFLY";

        defcod[347] = -150;
        defnam[347] = "CASSINI PROBE";

        defcod[348] = -150;
        defnam[348] = "HUYGENS PROBE";

        defcod[349] = -150;
        defnam[349] = "CASP";

        defcod[350] = -151;
        defnam[350] = "AXAF";

        defcod[351] = -151;
        defnam[351] = "CHANDRA";

        defcod[352] = -152;
        defnam[352] = "CH2O";

        defcod[353] = -152;
        defnam[353] = "CHANDRAYAAN-2 ORBITER";

        defcod[354] = -153;
        defnam[354] = "CH2L";

        defcod[355] = -153;
        defnam[355] = "CHANDRAYAAN-2 LANDER";

        defcod[356] = -154;
        defnam[356] = "AQUA";

        defcod[357] = -155;
        defnam[357] = "KPLO";

        defcod[358] = -155;
        defnam[358] = "KOREAN PATHFINDER LUNAR ORBITER";

        defcod[359] = -156;
        defnam[359] = "ADITYA";

        defcod[360] = -156;
        defnam[360] = "ADIT";

        defcod[361] = -159;
        defnam[361] = "EURC";

        defcod[362] = -159;
        defnam[362] = "EUROPA CLIPPER";

        defcod[363] = -164;
        defnam[363] = "LUNAR FLASHLIGHT";

        defcod[364] = -165;
        defnam[364] = "MAP";

        defcod[365] = -166;
        defnam[365] = "IMAGE";

        defcod[366] = -168;
        defnam[366] = "PERSEVERANCE";

        defcod[367] = -168;
        defnam[367] = "MARS 2020";

        defcod[368] = -168;
        defnam[368] = "MARS2020";

        defcod[369] = -168;
        defnam[369] = "M2020";

        defcod[370] = -170;
        defnam[370] = "JWST";

        defcod[371] = -170;
        defnam[371] = "JAMES WEBB SPACE TELESCOPE";

        defcod[372] = -172;
        defnam[372] = "EXM RSP SCC";

        defcod[373] = -172;
        defnam[373] = "EXM SPACECRAFT COMPOSITE";

        defcod[374] = -172;
        defnam[374] = "EXOMARS SCC";

        defcod[375] = -173;
        defnam[375] = "EXM RSP SP";

        defcod[376] = -173;
        defnam[376] = "EXM SURFACE PLATFORM";

        defcod[377] = -173;
        defnam[377] = "EXOMARS SP";

        defcod[378] = -174;
        defnam[378] = "EXM RSP RM";

        defcod[379] = -174;
        defnam[379] = "EXM ROVER";

        defcod[380] = -174;
        defnam[380] = "EXOMARS ROVER";

        defcod[381] = -177;
        defnam[381] = "GRAIL-A";

        defcod[382] = -178;
        defnam[382] = "PLANET-B";

        defcod[383] = -178;
        defnam[383] = "NOZOMI";

        defcod[384] = -181;
        defnam[384] = "GRAIL-B";

        defcod[385] = -183;
        defnam[385] = "CLUSTER 1";

        defcod[386] = -185;
        defnam[386] = "CLUSTER 2";

        defcod[387] = -188;
        defnam[387] = "MUSES-B";

        defcod[388] = -189;
        defnam[388] = "NSYT";

        defcod[389] = -189;
        defnam[389] = "INSIGHT";

        defcod[390] = -190;
        defnam[390] = "SIM";

        defcod[391] = -194;
        defnam[391] = "CLUSTER 3";

        defcod[392] = -196;
        defnam[392] = "CLUSTER 4";

        defcod[393] = -197;
        defnam[393] = "EXOMARS_LARA";

        defcod[394] = -197;
        defnam[394] = "LARA";

        defcod[395] = -198;
        defnam[395] = "INTEGRAL";

        defcod[396] = -198;
        defnam[396] = "NASA-ISRO SAR MISSION";

        defcod[397] = -198;
        defnam[397] = "NISAR";

        defcod[398] = -200;
        defnam[398] = "CONTOUR";

        defcod[399] = -202;
        defnam[399] = "MAVEN";

        defcod[400] = -203;
        defnam[400] = "DAWN";

        defcod[401] = -205;
        defnam[401] = "SOIL MOISTURE ACTIVE AND PASSIVE";

        defcod[402] = -205;
        defnam[402] = "SMAP";

        defcod[403] = -210;
        defnam[403] = "LICIA";

        defcod[404] = -210;
        defnam[404] = "LICIACUBE";

        defcod[405] = -212;
        defnam[405] = "STV51";

        defcod[406] = -213;
        defnam[406] = "STV52";

        defcod[407] = -214;
        defnam[407] = "STV53";

        defcod[408] = -226;
        defnam[408] = "ROSETTA";

        defcod[409] = -227;
        defnam[409] = "KEPLER";

        defcod[410] = -228;
        defnam[410] = "GLL PROBE";

        defcod[411] = -228;
        defnam[411] = "GALILEO PROBE";

        defcod[412] = -234;
        defnam[412] = "STEREO AHEAD";

        defcod[413] = -235;
        defnam[413] = "STEREO BEHIND";

        defcod[414] = -236;
        defnam[414] = "MESSENGER";

        defcod[415] = -238;
        defnam[415] = "SMART1";

        defcod[416] = -238;
        defnam[416] = "SM1";

        defcod[417] = -238;
        defnam[417] = "S1";

        defcod[418] = -238;
        defnam[418] = "SMART-1";

        defcod[419] = -239;
        defnam[419] = "MARTIAN MOONS EXPLORATION";

        defcod[420] = -239;
        defnam[420] = "MMX";

        defcod[421] = -240;
        defnam[421] = "SMART LANDER FOR INVESTIGATING MOON";

        defcod[422] = -240;
        defnam[422] = "SLIM";

        defcod[423] = -242;
        defnam[423] = "LUNAR TRAILBLAZER";

        defcod[424] = -243;
        defnam[424] = "VIPER";

        defcod[425] = -248;
        defnam[425] = "VEX";

        defcod[426] = -248;
        defnam[426] = "VENUS EXPRESS";

        defcod[427] = -253;
        defnam[427] = "OPPORTUNITY";

        defcod[428] = -253;
        defnam[428] = "MER-1";

        defcod[429] = -254;
        defnam[429] = "SPIRIT";

        defcod[430] = -254;
        defnam[430] = "MER-2";

        defcod[431] = -255;
        defnam[431] = "PSYC";

        defcod[432] = -301;
        defnam[432] = "HELIOS 1";

        defcod[433] = -302;
        defnam[433] = "HELIOS 2";

        defcod[434] = -362;
        defnam[434] = "RADIATION BELT STORM PROBE A";

        defcod[435] = -362;
        defnam[435] = "RBSP_A";

        defcod[436] = -363;
        defnam[436] = "RADIATION BELT STORM PROBE B";

        defcod[437] = -363;
        defnam[437] = "RBSP_B";

        defcod[438] = -500;
        defnam[438] = "RSAT";

        defcod[439] = -500;
        defnam[439] = "SELENE Relay Satellite";

        defcod[440] = -500;
        defnam[440] = "SELENE Rstar";

        defcod[441] = -500;
        defnam[441] = "Rstar";

        defcod[442] = -502;
        defnam[442] = "VSAT";

        defcod[443] = -502;
        defnam[443] = "SELENE VLBI Radio Satellite";

        defcod[444] = -502;
        defnam[444] = "SELENE VRAD Satellite";

        defcod[445] = -502;
        defnam[445] = "SELENE Vstar";

        defcod[446] = -502;
        defnam[446] = "Vstar";

        defcod[447] = -550;
        defnam[447] = "MARS-96";

        defcod[448] = -550;
        defnam[448] = "M96";

        defcod[449] = -550;
        defnam[449] = "MARS 96";

        defcod[450] = -550;
        defnam[450] = "MARS96";

        defcod[451] = -652;
        defnam[451] = "MERCURY TRANSFER MODULE";

        defcod[452] = -652;
        defnam[452] = "MTM";

        defcod[453] = -652;
        defnam[453] = "BEPICOLOMBO MTM";

        defcod[454] = -750;
        defnam[454] = "SPRINT-A";

        defcod[455] = 50000001;
        defnam[455] = "SHOEMAKER-LEVY 9-W";

        defcod[456] = 50000002;
        defnam[456] = "SHOEMAKER-LEVY 9-V";

        defcod[457] = 50000003;
        defnam[457] = "SHOEMAKER-LEVY 9-U";

        defcod[458] = 50000004;
        defnam[458] = "SHOEMAKER-LEVY 9-T";

        defcod[459] = 50000005;
        defnam[459] = "SHOEMAKER-LEVY 9-S";

        defcod[460] = 50000006;
        defnam[460] = "SHOEMAKER-LEVY 9-R";

        defcod[461] = 50000007;
        defnam[461] = "SHOEMAKER-LEVY 9-Q";

        defcod[462] = 50000008;
        defnam[462] = "SHOEMAKER-LEVY 9-P";

        defcod[463] = 50000009;
        defnam[463] = "SHOEMAKER-LEVY 9-N";

        defcod[464] = 50000010;
        defnam[464] = "SHOEMAKER-LEVY 9-M";

        defcod[465] = 50000011;
        defnam[465] = "SHOEMAKER-LEVY 9-L";

        defcod[466] = 50000012;
        defnam[466] = "SHOEMAKER-LEVY 9-K";

        defcod[467] = 50000013;
        defnam[467] = "SHOEMAKER-LEVY 9-J";

        defcod[468] = 50000014;
        defnam[468] = "SHOEMAKER-LEVY 9-H";

        defcod[469] = 50000015;
        defnam[469] = "SHOEMAKER-LEVY 9-G";

        defcod[470] = 50000016;
        defnam[470] = "SHOEMAKER-LEVY 9-F";

        defcod[471] = 50000017;
        defnam[471] = "SHOEMAKER-LEVY 9-E";

        defcod[472] = 50000018;
        defnam[472] = "SHOEMAKER-LEVY 9-D";

        defcod[473] = 50000019;
        defnam[473] = "SHOEMAKER-LEVY 9-C";

        defcod[474] = 50000020;
        defnam[474] = "SHOEMAKER-LEVY 9-B";

        defcod[475] = 50000021;
        defnam[475] = "SHOEMAKER-LEVY 9-A";

        defcod[476] = 50000022;
        defnam[476] = "SHOEMAKER-LEVY 9-Q1";

        defcod[477] = 50000023;
        defnam[477] = "SHOEMAKER-LEVY 9-P2";

        defcod[478] = 1000001;
        defnam[478] = "AREND";

        defcod[479] = 1000002;
        defnam[479] = "AREND-RIGAUX";

        defcod[480] = 1000003;
        defnam[480] = "ASHBROOK-JACKSON";

        defcod[481] = 1000004;
        defnam[481] = "BOETHIN";

        defcod[482] = 1000005;
        defnam[482] = "BORRELLY";

        defcod[483] = 1000006;
        defnam[483] = "BOWELL-SKIFF";

        defcod[484] = 1000007;
        defnam[484] = "BRADFIELD";

        defcod[485] = 1000008;
        defnam[485] = "BROOKS 2";

        defcod[486] = 1000009;
        defnam[486] = "BRORSEN-METCALF";

        defcod[487] = 1000010;
        defnam[487] = "BUS";

        defcod[488] = 1000011;
        defnam[488] = "CHERNYKH";

        defcod[489] = 1000012;
        defnam[489] = "67P/CHURYUMOV-GERASIMENKO (1969 R1)";

        defcod[490] = 1000012;
        defnam[490] = "CHURYUMOV-GERASIMENKO";

        defcod[491] = 1000013;
        defnam[491] = "CIFFREO";

        defcod[492] = 1000014;
        defnam[492] = "CLARK";

        defcod[493] = 1000015;
        defnam[493] = "COMAS SOLA";

        defcod[494] = 1000016;
        defnam[494] = "CROMMELIN";

        defcod[495] = 1000017;
        defnam[495] = "D'ARREST";

        defcod[496] = 1000018;
        defnam[496] = "DANIEL";

        defcod[497] = 1000019;
        defnam[497] = "DE VICO-SWIFT";

        defcod[498] = 1000020;
        defnam[498] = "DENNING-FUJIKAWA";

        defcod[499] = 1000021;
        defnam[499] = "DU TOIT 1";

        defcod[500] = 1000022;
        defnam[500] = "DU TOIT-HARTLEY";

        defcod[501] = 1000023;
        defnam[501] = "DUTOIT-NEUJMIN-DELPORTE";

        defcod[502] = 1000024;
        defnam[502] = "DUBIAGO";

        defcod[503] = 1000025;
        defnam[503] = "ENCKE";

        defcod[504] = 1000026;
        defnam[504] = "FAYE";

        defcod[505] = 1000027;
        defnam[505] = "FINLAY";

        defcod[506] = 1000028;
        defnam[506] = "FORBES";

        defcod[507] = 1000029;
        defnam[507] = "GEHRELS 1";

        defcod[508] = 1000030;
        defnam[508] = "GEHRELS 2";

        defcod[509] = 1000031;
        defnam[509] = "GEHRELS 3";

        defcod[510] = 1000032;
        defnam[510] = "GIACOBINI-ZINNER";

        defcod[511] = 1000033;
        defnam[511] = "GICLAS";

        defcod[512] = 1000034;
        defnam[512] = "GRIGG-SKJELLERUP";

        defcod[513] = 1000035;
        defnam[513] = "GUNN";

        defcod[514] = 1000036;
        defnam[514] = "HALLEY";

        defcod[515] = 1000037;
        defnam[515] = "HANEDA-CAMPOS";

        defcod[516] = 1000038;
        defnam[516] = "HARRINGTON";

        defcod[517] = 1000039;
        defnam[517] = "HARRINGTON-ABELL";

        defcod[518] = 1000040;
        defnam[518] = "HARTLEY 1";

        defcod[519] = 1000041;
        defnam[519] = "HARTLEY 2";

        defcod[520] = 1000042;
        defnam[520] = "HARTLEY-IRAS";

        defcod[521] = 1000043;
        defnam[521] = "HERSCHEL-RIGOLLET";

        defcod[522] = 1000044;
        defnam[522] = "HOLMES";

        defcod[523] = 1000045;
        defnam[523] = "HONDA-MRKOS-PAJDUSAKOVA";

        defcod[524] = 1000046;
        defnam[524] = "HOWELL";

        defcod[525] = 1000047;
        defnam[525] = "IRAS";

        defcod[526] = 1000048;
        defnam[526] = "JACKSON-NEUJMIN";

        defcod[527] = 1000049;
        defnam[527] = "JOHNSON";

        defcod[528] = 1000050;
        defnam[528] = "KEARNS-KWEE";

        defcod[529] = 1000051;
        defnam[529] = "KLEMOLA";

        defcod[530] = 1000052;
        defnam[530] = "KOHOUTEK";

        defcod[531] = 1000053;
        defnam[531] = "KOJIMA";

        defcod[532] = 1000054;
        defnam[532] = "KOPFF";

        defcod[533] = 1000055;
        defnam[533] = "KOWAL 1";

        defcod[534] = 1000056;
        defnam[534] = "KOWAL 2";

        defcod[535] = 1000057;
        defnam[535] = "KOWAL-MRKOS";

        defcod[536] = 1000058;
        defnam[536] = "KOWAL-VAVROVA";

        defcod[537] = 1000059;
        defnam[537] = "LONGMORE";

        defcod[538] = 1000060;
        defnam[538] = "LOVAS 1";

        defcod[539] = 1000061;
        defnam[539] = "MACHHOLZ";

        defcod[540] = 1000062;
        defnam[540] = "MAURY";

        defcod[541] = 1000063;
        defnam[541] = "NEUJMIN 1";

        defcod[542] = 1000064;
        defnam[542] = "NEUJMIN 2";

        defcod[543] = 1000065;
        defnam[543] = "NEUJMIN 3";

        defcod[544] = 1000066;
        defnam[544] = "OLBERS";

        defcod[545] = 1000067;
        defnam[545] = "PETERS-HARTLEY";

        defcod[546] = 1000068;
        defnam[546] = "PONS-BROOKS";

        defcod[547] = 1000069;
        defnam[547] = "PONS-WINNECKE";

        defcod[548] = 1000070;
        defnam[548] = "REINMUTH 1";

        defcod[549] = 1000071;
        defnam[549] = "REINMUTH 2";

        defcod[550] = 1000072;
        defnam[550] = "RUSSELL 1";

        defcod[551] = 1000073;
        defnam[551] = "RUSSELL 2";

        defcod[552] = 1000074;
        defnam[552] = "RUSSELL 3";

        defcod[553] = 1000075;
        defnam[553] = "RUSSELL 4";

        defcod[554] = 1000076;
        defnam[554] = "SANGUIN";

        defcod[555] = 1000077;
        defnam[555] = "SCHAUMASSE";

        defcod[556] = 1000078;
        defnam[556] = "SCHUSTER";

        defcod[557] = 1000079;
        defnam[557] = "SCHWASSMANN-WACHMANN 1";

        defcod[558] = 1000080;
        defnam[558] = "SCHWASSMANN-WACHMANN 2";

        defcod[559] = 1000081;
        defnam[559] = "SCHWASSMANN-WACHMANN 3";

        defcod[560] = 1000082;
        defnam[560] = "SHAJN-SCHALDACH";

        defcod[561] = 1000083;
        defnam[561] = "SHOEMAKER 1";

        defcod[562] = 1000084;
        defnam[562] = "SHOEMAKER 2";

        defcod[563] = 1000085;
        defnam[563] = "SHOEMAKER 3";

        defcod[564] = 1000086;
        defnam[564] = "SINGER-BREWSTER";

        defcod[565] = 1000087;
        defnam[565] = "SLAUGHTER-BURNHAM";

        defcod[566] = 1000088;
        defnam[566] = "SMIRNOVA-CHERNYKH";

        defcod[567] = 1000089;
        defnam[567] = "STEPHAN-OTERMA";

        defcod[568] = 1000090;
        defnam[568] = "SWIFT-GEHRELS";

        defcod[569] = 1000091;
        defnam[569] = "TAKAMIZAWA";

        defcod[570] = 1000092;
        defnam[570] = "TAYLOR";

        defcod[571] = 1000093;
        defnam[571] = "TEMPEL_1";

        defcod[572] = 1000093;
        defnam[572] = "TEMPEL 1";

        defcod[573] = 1000094;
        defnam[573] = "TEMPEL 2";

        defcod[574] = 1000095;
        defnam[574] = "TEMPEL-TUTTLE";

        defcod[575] = 1000096;
        defnam[575] = "TRITTON";

        defcod[576] = 1000097;
        defnam[576] = "TSUCHINSHAN 1";

        defcod[577] = 1000098;
        defnam[577] = "TSUCHINSHAN 2";

        defcod[578] = 1000099;
        defnam[578] = "TUTTLE";

        defcod[579] = 1000100;
        defnam[579] = "TUTTLE-GIACOBINI-KRESAK";

        defcod[580] = 1000101;
        defnam[580] = "VAISALA 1";

        defcod[581] = 1000102;
        defnam[581] = "VAN BIESBROECK";

        defcod[582] = 1000103;
        defnam[582] = "VAN HOUTEN";

        defcod[583] = 1000104;
        defnam[583] = "WEST-KOHOUTEK-IKEMURA";

        defcod[584] = 1000105;
        defnam[584] = "WHIPPLE";

        defcod[585] = 1000106;
        defnam[585] = "WILD 1";

        defcod[586] = 1000107;
        defnam[586] = "WILD 2";

        defcod[587] = 1000108;
        defnam[587] = "WILD 3";

        defcod[588] = 1000109;
        defnam[588] = "WIRTANEN";

        defcod[589] = 1000110;
        defnam[589] = "WOLF";

        defcod[590] = 1000111;
        defnam[590] = "WOLF-HARRINGTON";

        defcod[591] = 1000112;
        defnam[591] = "LOVAS 2";

        defcod[592] = 1000113;
        defnam[592] = "URATA-NIIJIMA";

        defcod[593] = 1000114;
        defnam[593] = "WISEMAN-SKIFF";

        defcod[594] = 1000115;
        defnam[594] = "HELIN";

        defcod[595] = 1000116;
        defnam[595] = "MUELLER";

        defcod[596] = 1000117;
        defnam[596] = "SHOEMAKER-HOLT 1";

        defcod[597] = 1000118;
        defnam[597] = "HELIN-ROMAN-CROCKETT";

        defcod[598] = 1000119;
        defnam[598] = "HARTLEY 3";

        defcod[599] = 1000120;
        defnam[599] = "PARKER-HARTLEY";

        defcod[600] = 1000121;
        defnam[600] = "HELIN-ROMAN-ALU 1";

        defcod[601] = 1000122;
        defnam[601] = "WILD 4";

        defcod[602] = 1000123;
        defnam[602] = "MUELLER 2";

        defcod[603] = 1000124;
        defnam[603] = "MUELLER 3";

        defcod[604] = 1000125;
        defnam[604] = "SHOEMAKER-LEVY 1";

        defcod[605] = 1000126;
        defnam[605] = "SHOEMAKER-LEVY 2";

        defcod[606] = 1000127;
        defnam[606] = "HOLT-OLMSTEAD";

        defcod[607] = 1000128;
        defnam[607] = "METCALF-BREWINGTON";

        defcod[608] = 1000129;
        defnam[608] = "LEVY";

        defcod[609] = 1000130;
        defnam[609] = "SHOEMAKER-LEVY 9";

        defcod[610] = 1000131;
        defnam[610] = "HYAKUTAKE";

        defcod[611] = 1000132;
        defnam[611] = "HALE-BOPP";

        defcod[612] = 1003228;
        defnam[612] = "C/2013 A1";

        defcod[613] = 1003228;
        defnam[613] = "SIDING SPRING";

        defcod[614] = 2000001;
        defnam[614] = "CERES";

        defcod[615] = 2000002;
        defnam[615] = "PALLAS";

        defcod[616] = 2000004;
        defnam[616] = "VESTA";

        defcod[617] = 2000016;
        defnam[617] = "PSYCHE";

        defcod[618] = 2000021;
        defnam[618] = "LUTETIA";

        defcod[619] = 2000052;
        defnam[619] = "52_EUROPA";

        defcod[620] = 2000052;
        defnam[620] = "52 EUROPA";

        defcod[621] = 2000216;
        defnam[621] = "KLEOPATRA";

        defcod[622] = 2000253;
        defnam[622] = "MATHILDE";

        defcod[623] = 2000433;
        defnam[623] = "EROS";

        defcod[624] = 2000511;
        defnam[624] = "DAVIDA";

        defcod[625] = 2002867;
        defnam[625] = "STEINS";

        defcod[626] = 2004015;
        defnam[626] = "WILSON-HARRINGTON";

        defcod[627] = 2004179;
        defnam[627] = "TOUTATIS";

        defcod[628] = 2009969;
        defnam[628] = "1992KD";

        defcod[629] = 2009969;
        defnam[629] = "BRAILLE";

        defcod[630] = 2025143;
        defnam[630] = "ITOKAWA";

        defcod[631] = 2101955;
        defnam[631] = "BENNU";

        defcod[632] = 2162173;
        defnam[632] = "RYUGU";

        defcod[633] = 2431010;
        defnam[633] = "IDA";

        defcod[634] = 2431011;
        defnam[634] = "DACTYL";

        defcod[635] = 2486958;
        defnam[635] = "ARROKOTH";

        defcod[636] = 9511010;
        defnam[636] = "GASPRA";

        defcod[637] = 20000617;
        defnam[637] = "PATROCLUS_BARYCENTER";

        defcod[638] = 20000617;
        defnam[638] = "PATROCLUS BARYCENTER";

        defcod[639] = 20003548;
        defnam[639] = "EURYBATES_BARYCENTER";

        defcod[640] = 20003548;
        defnam[640] = "EURYBATES BARYCENTER";

        defcod[641] = 20011351;
        defnam[641] = "LEUCUS";

        defcod[642] = 20015094;
        defnam[642] = "POLYMELE";

        defcod[643] = 20021900;
        defnam[643] = "ORUS";

        defcod[644] = 20052246;
        defnam[644] = "DONALDJOHANSON";

        defcod[645] = 20065803;
        defnam[645] = "DIDYMOS_BARYCENTER";

        defcod[646] = 20065803;
        defnam[646] = "DIDYMOS BARYCENTER";

        defcod[647] = 120000617;
        defnam[647] = "MENOETIUS";

        defcod[648] = 120003548;
        defnam[648] = "QUETA";

        defcod[649] = 120065803;
        defnam[649] = "DIMORPHOS";

        defcod[650] = 920000617;
        defnam[650] = "PATROCLUS";

        defcod[651] = 920003548;
        defnam[651] = "EURYBATES";

        defcod[652] = 920065803;
        defnam[652] = "DIDYMOS";

        defcod[653] = 398989;
        defnam[653] = "NOTO";

        defcod[654] = 398990;
        defnam[654] = "NEW NORCIA";

        defcod[655] = 399001;
        defnam[655] = "GOLDSTONE";

        defcod[656] = 399002;
        defnam[656] = "CANBERRA";

        defcod[657] = 399003;
        defnam[657] = "MADRID";

        defcod[658] = 399004;
        defnam[658] = "USUDA";

        defcod[659] = 399005;
        defnam[659] = "DSS-05";

        defcod[660] = 399005;
        defnam[660] = "PARKES";

        defcod[661] = 399012;
        defnam[661] = "DSS-12";

        defcod[662] = 399013;
        defnam[662] = "DSS-13";

        defcod[663] = 399014;
        defnam[663] = "DSS-14";

        defcod[664] = 399015;
        defnam[664] = "DSS-15";

        defcod[665] = 399016;
        defnam[665] = "DSS-16";

        defcod[666] = 399017;
        defnam[666] = "DSS-17";

        defcod[667] = 399023;
        defnam[667] = "DSS-23";

        defcod[668] = 399024;
        defnam[668] = "DSS-24";

        defcod[669] = 399025;
        defnam[669] = "DSS-25";

        defcod[670] = 399026;
        defnam[670] = "DSS-26";

        defcod[671] = 399027;
        defnam[671] = "DSS-27";

        defcod[672] = 399028;
        defnam[672] = "DSS-28";

        defcod[673] = 399033;
        defnam[673] = "DSS-33";

        defcod[674] = 399034;
        defnam[674] = "DSS-34";

        defcod[675] = 399035;
        defnam[675] = "DSS-35";

        defcod[676] = 399036;
        defnam[676] = "DSS-36";

        defcod[677] = 399042;
        defnam[677] = "DSS-42";

        defcod[678] = 399043;
        defnam[678] = "DSS-43";

        defcod[679] = 399045;
        defnam[679] = "DSS-45";

        defcod[680] = 399046;
        defnam[680] = "DSS-46";

        defcod[681] = 399049;
        defnam[681] = "DSS-49";

        defcod[682] = 399053;
        defnam[682] = "DSS-53";

        defcod[683] = 399054;
        defnam[683] = "DSS-54";

        defcod[684] = 399055;
        defnam[684] = "DSS-55";

        defcod[685] = 399056;
        defnam[685] = "DSS-56";

        defcod[686] = 399061;
        defnam[686] = "DSS-61";

        defcod[687] = 399063;
        defnam[687] = "DSS-63";

        defcod[688] = 399064;
        defnam[688] = "DSS-64";

        defcod[689] = 399065;
        defnam[689] = "DSS-65";

        defcod[690] = 399066;
        defnam[690] = "DSS-66";

        defcod[691] = 399069;
        defnam[691] = "DSS-69";

        defcod[692] = 2065803;
        defnam[692] = "DIDYMOS BARYCENTER";

        defcod[693] = -658030;
        defnam[693] = "DIDYMOS";

        defcod[694] = -658031;
        defnam[694] = "DIMORPHOS";

        for (int i = 0; i < NPERM; i++) {
            defnor[i] = normalize(defnam[i]);
        }

    }

    /**
     * Normalize a String by trimming any spaces at the beginning or the end and by letting no more than 1 space in a
     * row between words.
     * 
     * @param str
     *        String to normalize
     * @return the input string normalized
     */
    private static String normalize(final String str) {
        return str.replaceAll(REGEX_SPACE, "$1").toUpperCase(Locale.US);
    }

    // CHECKSTYLE: resume MagicNumber check
    //CHECKSTYLE: resume MultipleStringLiterals check
}
