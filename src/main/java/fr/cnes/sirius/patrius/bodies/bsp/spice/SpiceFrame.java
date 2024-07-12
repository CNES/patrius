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
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp.spice;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

//CHECKSTYLE: stop MagicNumber check
//Reason: Spice frame list

/**
 * This class contains the methods necessaries for manipulating different 
 * reference frames.
 * It is build as an adaptation of the framex.for file from the SPICE
 * library.
 * <p>
 *
 * @author
 *
 * @since 4.11
 */
public final class SpiceFrame {

    /**
     * Number of inertial frames
     */
    public static final int NINERT = 21;
    // Different type of frames defined in frmtyp.inc of SPICE library
    /**
     * Inertial frame type
     */
    public static final int INERTL = 1;
    /**
     * PCK frame type
     */
    public static final int PCK = INERTL + 1;
    /**
     * CK frame type
     */
    public static final int CK = PCK + 1;
    /**
     * TK frame type
     */
    public static final int TK = CK + 1;
    
    /**
     * Boolean indicating if it's the first call to any
     * method of the class
     */
    private static boolean first = true;

    /**
     * J2000 frame name
     */
    private static final String J2000 = "J2000";
    /**
     *  Size of kernem frame lists
     */
    private static final int MAXKFR = 5209;
    /**
     * Number of non-inertial frames
     */
    private static final int NNINERT = 124;
    
    /**
     * Total number of frames
     */
    private static final int NCOUNT = NINERT + NNINERT;
    
    /**
     * Pool state counter to the user value
     */
    private static CounterArray pulctr;
    
    /**
     * Kernel pool Id-based frame list.
     */
    private static List<Integer> kernelIdList;
    /**
     * Kernel pool name-based frame list.
     */
    private static List<String> kernelNameList;
    
    /**
     * Built-in Id-based frames list.
     */
    private static List<Integer> builtIdList = new ArrayList<Integer>(NCOUNT);
    /**
     * Built-in name-based frames list.
     */
    private static List<String> builtNameList = new ArrayList<String>(NCOUNT);
    
    /**
     * Array of the official SPICE names for the recognized
     * frames (both inertial and non-inertial)
     */
    private static String[] name = new String[NCOUNT];
    /**
     * Array of the official SPICE names for the recognized
     * frames (both inertial and non-inertial) (read in kernel)
     */
    private static String[] kname = new String[MAXKFR];
    
    /**
     * is an array parallel to NAME of SPICE ID codes for
     * the various frames
     */
    private static int[] idcode = new int[NCOUNT];
    /**
     * is an array parallel to NAME of SPICE ID codes for
     * the various frames (read in kernel)
     */
    private static int[] kidcode = new int[MAXKFR];
    
    /**
     * is an array parallel to NAME of body ID codes for
     * the centers of frames
     */
    private static int[] center = new int[NCOUNT];
    /**
     * is an array parallel to NAME of body ID codes for
     * the centers of frames (read in kernel)
     */
    private static int[] kcenter = new int[MAXKFR];
    
    /**
     * is an array parallel to NAME of inertial frame types
     * for the various frames. These include INERTL, PCK,
     * CK, etc.
     */
    private static int[] type = new int[NCOUNT];
    /**
     * is an array parallel to NAME of inertial frame types
     * for the various frames. These include INERTL, PCK,
     * CK, etc. (read in kernel)
     */
    private static int[] ktype = new int[MAXKFR];
    
    /**
     * is an array parallel to NAME of the ID code for the
     * frame within the TYPE of the frame. Once the class
     * of the frame has been identified by TYPE, TYPID is
     * used to access the information specific about this
     * frame.
     */
    private static int[] typid = new int[NCOUNT];
    /**
     * is an array parallel to NAME of the ID code for the
     * frame within the TYPE of the frame. Once the class
     * of the frame has been identified by TYPE, TYPID is
     * used to access the information specific about this
     * frame. (read in kernel)
     */ 
    private static int[] ktypid = new int[MAXKFR];
    
    /**
     * Constructor
     */
    private SpiceFrame() {
        // Nothing to do
    }
    
    /**
     * Initialization
     * @throws PatriusException if there is a problem initializing the counter array
     */
    private static void init() throws PatriusException {
        // Initialize POOL state counter to the user value.
        pulctr = new CounterArray("USER");
        
        // Initialize kernel POOL frame lists
        kernelIdList = new ArrayList<Integer>(MAXKFR);
        kernelNameList = new ArrayList<String>(MAXKFR);
        
        // Initialize built-in frame tables and lists
        initFrames();
        
        first = false;
    }
    
    /**
     * Retrieve the name of a reference frame associated with a SPICE ID
     * code.
     * This routine is a translation of FRMNAM from the SPICE library
     * @param code an integer code for a reference frame
     * @return the name associated with the reference frame.
     * @throws PatriusException if there is a problem with the counter array or with the pool
     */
    public static String frameId2Name(final int code) throws PatriusException {     
        // For efficiency, J2000 deserves special treatment
        if (code == 1) {
            return J2000;
        }
        // Perform any needed first pass initializations.
        if (first) {
            init();
        }
        
        final int index = builtIdList.indexOf(Integer.valueOf(code));
        String frame = "";
        if (index >= 0) {
            frame = name[index];
        } else {
            // See if this frame is in the kernel pool frame ID-based hash.
            // First reset the hash if POOL has changed.           
            if (PoolSpice.checkPoolStateCounter(pulctr)) {
                kernelIdList = new ArrayList<Integer>(MAXKFR);
                kernelNameList = new ArrayList<String>(MAXKFR);
            }
            
            // Check if this ID is in the list.
            final int kindex = kernelIdList.indexOf(Integer.valueOf(code));
            
            if (kindex >= 0) {
                frame = kname[kindex];
            } else {
                // The ID wasn't in the list, see if we can find this frame in
                // the kernel pool.
                StringBuffer pname = new StringBuffer("FRAME_#_NAME");
                final int hashIndex = pname.indexOf("#");
                pname = pname.replace(hashIndex, hashIndex + 1, String.valueOf(code));
                final List<String> line = PoolSpice.getStrVals(pname.toString());
                
                if (line.size() == 1) {
                    frame = line.get(0).trim();
                    
                    //Note that since we did not collect all needed
                    // information about this frame, we will not try to add it
                    // to the list.
                }
            }
        }
        
        return frame;
    }
    
    /**
     * Look up the frame ID code associated with a string.
     * 
     * @param frame The name of some reference frame
     * @return The SPICE ID code of the frame.
     * @throws PatriusException if there is a problem with the counter array or with the pool
     */
    public static int frameName2Id(final String frame) throws PatriusException {      
        // For efficiency, J2000 deserves special treatment.
        if (J2000.equalsIgnoreCase(frame.trim())) {
            return 1;
        }
        
        // Perform any needed first pass initializations.
        if (first) {
            init();
        }
        
        // Determine the location of the requested item in the array
        // of names.
        String pname = frame.trim().toUpperCase(Locale.US);
        
        final int item = builtNameList.indexOf(pname);
        
        // If the name is in our list, we can just look up its ID code in
        // the parallel array.
        int id = 0;
        if (item >= 0) {
            id = idcode[item];
        } else {
            // See if this frame is in the kernel pool frame ID-based hash.
            // First reset the hash if POOL has changed.           
            if (PoolSpice.checkPoolStateCounter(pulctr)) {
                kernelIdList = new ArrayList<Integer>(MAXKFR);
                kernelNameList = new ArrayList<String>(MAXKFR);
            }
            
            // Check if this ID is in the list.
            final int kindex = kernelNameList.indexOf(pname);
            
            if (kindex >= 0) {
                id = kidcode[kindex];
            } else {
                // The name wasn't in the list, see if we can find this frame
                // in the kernel pool
                final StringBuffer sbpname = new StringBuffer(pname);
                pname = sbpname.insert(0, "FRAME_").toString();
                
                final List<Integer> values = PoolSpice.getIntVals(pname);
                
                if (values.size() == 1){
                    id = values.get(0).intValue();
                    
                    // If we made it to this point, we successfully mapped the
                    // kernel frame name to its ID. Add this pair to the
                    // name-based list.
                    kernelNameList.add(pname);
                    kernelIdList.add(Integer.valueOf(id));
                } else {
                    id = 0;
                }
            }
        }
        
        return id;
    }
    
    /**
     * Retrieve the minimal attributes associated with a frame
     * needed for converting transformations to and from it.
     * This method is based on the FRINFO routine of the SPICE library.
     * @param frCode the ID code for some frame
     * @param cent (out) the center of the frame
     * @param frClass (out) the class (type) of the frame
     * @param classId (out) the ID code for the frame within its class.
     * @return TRUE if the requested information is available.
     * @throws PatriusException if there is an initialization problem
     */
    public static boolean frameInfo(final int frCode, 
                                    final int[] cent, 
                                    final int[] frClass, 
                                    final int[] classId) throws PatriusException {
        // For efficiency, J2000 deserves special treatment.
        if (frCode == 1) {
            cent[0] = 0;
            frClass[0] = INERTL;
            classId[0] = 1;
            return true;
        }
        
        // Perform any needed first pass initializations.
        if (first) {
            init();
        }
        
        // Determine the location of the requested item in the array of ID codes.
        final int item = builtIdList.indexOf(Integer.valueOf(frCode));
        
        // If the Id is in the list, we can just look up for the rest in the parallel array
        if (item >= 0) {
            cent[0] = center[item];
            frClass[0] = type[item];
            classId[0] = typid[item];
            return true;
        } else {
            // We do not perform any kernel pool verfication. Any non-SPK kernel has been loaded TODO            
            throw new PatriusException(PatriusMessages.PDB_IMPOSSIBLE_TO_GET_HERE_SPICE);
        }
    }
    
    /**
     * Return the frame name, frame ID, and center associated with a given frame class and class ID.
     * Method based on the CCIFRM routine from the SPICE library.
     * @param frClass Class of frame.
     * @param classId Class ID of frame.
     * @param frCode (out) ID code of the frame identified by FRCLSS, CLSSID.
     * @param frName (out) Name of the frame identified by FRCLSS, CLSSID. 
     * @param cent (out) Center of the frame identified by FRCLSS, CLSSID.
     * @return TRUE if the requested information is available.
     * @throws PatriusException if there is an initialization problem.
     */
    public static boolean getFrameFromClass(final int frClass, 
                                            final int classId, 
                                            final int[] frCode, 
                                            final String[] frName, 
                                            final int[] cent) throws PatriusException {
        // Perform any needed first pass initializations.
        if (first) {
            init();
        }
        
        // First try to look up from the built-in list the frame associated with
        // the input class and class ID. Unfortunately, this is a linear search.
        for (int i = 0; i < NCOUNT; i++) {
            if ( type[i] == frClass && typid[i] == classId ) {
                // We have a match. Assign the output arguments and return.
                frName[0] = name[i];
                frCode[0] = idcode[i];
                cent[0] = center[i];
                
                return true;
            }
        }
        
        // Unfortunately we did not find a frame associated with the input
        // class and class ID in the built-in list. We need to look for this
        // frame in the kernel POOL. Since neither of these input values
        // appears in a kernel variable name, we may have to look at all of
        // the frame specifications in the kernel pool. Start out by looking
        // the frame class assignments from any loaded frame specifications.

        // Kernel pool search isn't useful because any no-spk kernel has been loaded TODO       
        return false;
    }
    
    /**
     * Translate a string containing a frame name to its ID code, but
     * bypass calling frameName2Id and return saved value provided by the
     * caller if the name is the same as the saved name and the POOL
     * state did not change.
     * Based on the ZZNAMFRM routine of the SPICE library.
     * 
     * @param usrctr POOL state counter saved by the caller.
     * @param savnam Frame name saved by the caller.
     * @param savcde Frame ID code saved by the caller.
     * @param nameF Frame name
     * @return Frame ID code.
     * @throws PatriusException in case there is a problem checking a counter update or translating the name to Id.
     */
    public static int frameName2IdBypass(final CounterArray usrctr, 
                                         final String[] savnam, 
                                         final int[] savcde, 
                                         final String nameF) throws PatriusException {
        // Check/update POOL state counter.
        final boolean update = PoolSpice.checkPoolStateCounter(usrctr);
        
        int code = 0;
        // Check update flag, saved ID, and saved name against the input.
        if ( !update && savcde[0] != 0 && savnam[0].equals(nameF)) {
            // No change in the POOL state, the saved name was successfully
            // resolved earlier, and input and saved names are the same.
            // Return saved ID.
            code = savcde[0];
        } else {
            // POOL state changed, or the saved name was never successfully
            // resolved earlier, or input and saved names are different. Call
            // NAMFRM to look up ID and reset saved values.
            code = frameName2Id(nameF);
            savnam[0] = nameF;
            savcde[0] = code;
        }
        
        return code;
    }
    
    /**
     * Determine if a frame ID code corresponds to an inertial frame code
     * @param code frame SPICE ID code
     * @return a boolean indicating if the frame is an inertial or non-inertial frame.
     */
    public static boolean isInertial(final int code) {
        return code > 0 && code <= NINERT;
    }
    
    /**
     * Initialize the necessary arrays
     */
    private static void initFrames(){
        // List of inertial frames considered in SPICE
        final String[] inert = {J2000, "B1950", "FK4", "DE-118", "DE-96", "DE-102", "DE-108",
                                "DE-111", "DE-114", "DE-122", "DE-125", "DE-130", "GALACTIC", 
                                "DE-200", "DE-202", "MARSIAU", "ECLIPJ2000", "ECLIPB1950", "DE-140",
                                "DE-142", "DE-143"};
        
        // Inertial Frames Section
        for (int i = 0; i < NINERT; i++) {
            idcode[i] = i + 1;
            center[i] = 0;
            type[i] = INERTL;
            typid[i] = i + 1;
            name[i] = inert[i];
        }
        
        // Non-intertial frame section
        name   [ NINERT +   0 ] =  "IAU_MERCURY_BARYCENTER";
        idcode [ NINERT +   0 ] =  10001;
        center [ NINERT +   0 ] =  1;
        typid  [ NINERT +   0 ] =  1;
        type   [ NINERT +   0 ] =  PCK;

        name   [ NINERT +   1 ] =  "IAU_VENUS_BARYCENTER";
        idcode [ NINERT +   1 ] =  10002;
        center [ NINERT +   1 ] =  2;
        typid  [ NINERT +   1 ] =  2;
        type   [ NINERT +   1 ] =  PCK;

        name   [ NINERT +   2 ] =  "IAU_EARTH_BARYCENTER";
        idcode [ NINERT +   2 ] =  10003;
        center [ NINERT +   2 ] =  3;
        typid  [ NINERT +   2 ] =  3;
        type   [ NINERT +   2 ] =  PCK;

        name   [ NINERT +   3 ] =  "IAU_MARS_BARYCENTER";
        idcode [ NINERT +   3 ] =  10004;
        center [ NINERT +   3 ] =  4;
        typid  [ NINERT +   3 ] =  4;
        type   [ NINERT +   3 ] =  PCK;

        name   [ NINERT +   4 ] =  "IAU_JUPITER_BARYCENTER";
        idcode [ NINERT +   4 ] =  10005;
        center [ NINERT +   4 ] =  5;
        typid  [ NINERT +   4 ] =  5;
        type   [ NINERT +   4 ] =  PCK;

        name   [ NINERT +   5 ] =  "IAU_SATURN_BARYCENTER";
        idcode [ NINERT +   5 ] =  10006;
        center [ NINERT +   5 ] =  6;
        typid  [ NINERT +   5 ] =  6;
        type   [ NINERT +   5 ] =  PCK;

        name   [ NINERT +   6 ] =  "IAU_URANUS_BARYCENTER";
        idcode [ NINERT +   6 ] =  10007;
        center [ NINERT +   6 ] =  7;
        typid  [ NINERT +   6 ] =  7;
        type   [ NINERT +   6 ] =  PCK;

        name   [ NINERT +   7 ] =  "IAU_NEPTUNE_BARYCENTER";
        idcode [ NINERT +   7 ] =  10008;
        center [ NINERT +   7 ] =  8;
        typid  [ NINERT +   7 ] =  8;
        type   [ NINERT +   7 ] =  PCK;

        name   [ NINERT +   8 ] =  "IAU_PLUTO_BARYCENTER";
        idcode [ NINERT +   8 ] =  10009;
        center [ NINERT +   8 ] =  9;
        typid  [ NINERT +   8 ] =  9;
        type   [ NINERT +   8 ] =  PCK;

        name   [ NINERT +   9 ] =  "IAU_SUN";
        idcode [ NINERT +   9 ] =  10010;
        center [ NINERT +   9 ] =  10;
        typid  [ NINERT +   9 ] =  10;
        type   [ NINERT +   9 ] =  PCK;

        name   [ NINERT +  10 ] =  "IAU_MERCURY";
        idcode [ NINERT +  10 ] =  10011;
        center [ NINERT +  10 ] =  199;
        typid  [ NINERT +  10 ] =  199;
        type   [ NINERT +  10 ] =  PCK;

        name   [ NINERT +  11 ] =  "IAU_VENUS";
        idcode [ NINERT +  11 ] =  10012;
        center [ NINERT +  11 ] =  299;
        typid  [ NINERT +  11 ] =  299;
        type   [ NINERT +  11 ] =  PCK;

        name   [ NINERT +  12 ] =  "IAU_EARTH";
        idcode [ NINERT +  12 ] =  10013;
        center [ NINERT +  12 ] =  399;
        typid  [ NINERT +  12 ] =  399;
        type   [ NINERT +  12 ] =  PCK;

        name   [ NINERT +  13 ] =  "IAU_MARS";
        idcode [ NINERT +  13 ] =  10014;
        center [ NINERT +  13 ] =  499;
        typid  [ NINERT +  13 ] =  499;
        type   [ NINERT +  13 ] =  PCK;

        name   [ NINERT +  14 ] =  "IAU_JUPITER";
        idcode [ NINERT +  14 ] =  10015;
        center [ NINERT +  14 ] =  599;
        typid  [ NINERT +  14 ] =  599;
        type   [ NINERT +  14 ] =  PCK;

        name   [ NINERT +  15 ] =  "IAU_SATURN";
        idcode [ NINERT +  15 ] =  10016;
        center [ NINERT +  15 ] =  699;
        typid  [ NINERT +  15 ] =  699;
        type   [ NINERT +  15 ] =  PCK;

        name   [ NINERT +  16 ] =  "IAU_URANUS";
        idcode [ NINERT +  16 ] =  10017;
        center [ NINERT +  16 ] =  799;
        typid  [ NINERT +  16 ] =  799;
        type   [ NINERT +  16 ] =  PCK;

        name   [ NINERT +  17 ] =  "IAU_NEPTUNE";
        idcode [ NINERT +  17 ] =  10018;
        center [ NINERT +  17 ] =  899;
        typid  [ NINERT +  17 ] =  899;
        type   [ NINERT +  17 ] =  PCK;

        name   [ NINERT +  18 ] =  "IAU_PLUTO";
        idcode [ NINERT +  18 ] =  10019;
        center [ NINERT +  18 ] =  999;
        typid  [ NINERT +  18 ] =  999;
        type   [ NINERT +  18 ] =  PCK;

        name   [ NINERT +  19 ] =  "IAU_MOON";
        idcode [ NINERT +  19 ] =  10020;
        center [ NINERT +  19 ] =  301;
        typid  [ NINERT +  19 ] =  301;
        type   [ NINERT +  19 ] =  PCK;

        name   [ NINERT +  20 ] =  "IAU_PHOBOS";
        idcode [ NINERT +  20 ] =  10021;
        center [ NINERT +  20 ] =  401;
        typid  [ NINERT +  20 ] =  401;
        type   [ NINERT +  20 ] =  PCK;

        name   [ NINERT +  21 ] =  "IAU_DEIMOS";
        idcode [ NINERT +  21 ] =  10022;
        center [ NINERT +  21 ] =  402;
        typid  [ NINERT +  21 ] =  402;
        type   [ NINERT +  21 ] =  PCK;

        name   [ NINERT +  22 ] =  "IAU_IO";
        idcode [ NINERT +  22 ] =  10023;
        center [ NINERT +  22 ] =  501;
        typid  [ NINERT +  22 ] =  501;
        type   [ NINERT +  22 ] =  PCK;

        name   [ NINERT +  23 ] =  "IAU_EUROPA";
        idcode [ NINERT +  23 ] =  10024;
        center [ NINERT +  23 ] =  502;
        typid  [ NINERT +  23 ] =  502;
        type   [ NINERT +  23 ] =  PCK;

        name   [ NINERT +  24 ] =  "IAU_GANYMEDE";
        idcode [ NINERT +  24 ] =  10025;
        center [ NINERT +  24 ] =  503;
        typid  [ NINERT +  24 ] =  503;
        type   [ NINERT +  24 ] =  PCK;

        name   [ NINERT +  25 ] =  "IAU_CALLISTO";
        idcode [ NINERT +  25 ] =  10026;
        center [ NINERT +  25 ] =  504;
        typid  [ NINERT +  25 ] =  504;
        type   [ NINERT +  25 ] =  PCK;

        name   [ NINERT +  26 ] =  "IAU_AMALTHEA";
        idcode [ NINERT +  26 ] =  10027;
        center [ NINERT +  26 ] =  505;
        typid  [ NINERT +  26 ] =  505;
        type   [ NINERT +  26 ] =  PCK;

        name   [ NINERT +  27 ] =  "IAU_HIMALIA";
        idcode [ NINERT +  27 ] =  10028;
        center [ NINERT +  27 ] =  506;
        typid  [ NINERT +  27 ] =  506;
        type   [ NINERT +  27 ] =  PCK;

        name   [ NINERT +  28 ] =  "IAU_ELARA";
        idcode [ NINERT +  28 ] =  10029;
        center [ NINERT +  28 ] =  507;
        typid  [ NINERT +  28 ] =  507;
        type   [ NINERT +  28 ] =  PCK;

        name   [ NINERT +  29 ] =  "IAU_PASIPHAE";
        idcode [ NINERT +  29 ] =  10030;
        center [ NINERT +  29 ] =  508;
        typid  [ NINERT +  29 ] =  508;
        type   [ NINERT +  29 ] =  PCK;

        name   [ NINERT +  30 ] =  "IAU_SINOPE";
        idcode [ NINERT +  30 ] =  10031;
        center [ NINERT +  30 ] =  509;
        typid  [ NINERT +  30 ] =  509;
        type   [ NINERT +  30 ] =  PCK;

        name   [ NINERT +  31 ] =  "IAU_LYSITHEA";
        idcode [ NINERT +  31 ] =  10032;
        center [ NINERT +  31 ] =  510;
        typid  [ NINERT +  31 ] =  510;
        type   [ NINERT +  31 ] =  PCK;

        name   [ NINERT +  32 ] =  "IAU_CARME";
        idcode [ NINERT +  32 ] =  10033;
        center [ NINERT +  32 ] =  511;
        typid  [ NINERT +  32 ] =  511;
        type   [ NINERT +  32 ] =  PCK;

        name   [ NINERT +  33 ] =  "IAU_ANANKE";
        idcode [ NINERT +  33 ] =  10034;
        center [ NINERT +  33 ] =  512;
        typid  [ NINERT +  33 ] =  512;
        type   [ NINERT +  33 ] =  PCK;

        name   [ NINERT +  34 ] =  "IAU_LEDA";
        idcode [ NINERT +  34 ] =  10035;
        center [ NINERT +  34 ] =  513;
        typid  [ NINERT +  34 ] =  513;
        type   [ NINERT +  34 ] =  PCK;

        name   [ NINERT +  35 ] =  "IAU_THEBE";
        idcode [ NINERT +  35 ] =  10036;
        center [ NINERT +  35 ] =  514;
        typid  [ NINERT +  35 ] =  514;
        type   [ NINERT +  35 ] =  PCK;

        name   [ NINERT +  36 ] =  "IAU_ADRASTEA";
        idcode [ NINERT +  36 ] =  10037;
        center [ NINERT +  36 ] =  515;
        typid  [ NINERT +  36 ] =  515;
        type   [ NINERT +  36 ] =  PCK;

        name   [ NINERT +  37 ] =  "IAU_METIS";
        idcode [ NINERT +  37 ] =  10038;
        center [ NINERT +  37 ] =  516;
        typid  [ NINERT +  37 ] =  516;
        type   [ NINERT +  37 ] =  PCK;

        name   [ NINERT +  38 ] =  "IAU_MIMAS";
        idcode [ NINERT +  38 ] =  10039;
        center [ NINERT +  38 ] =  601;
        typid  [ NINERT +  38 ] =  601;
        type   [ NINERT +  38 ] =  PCK;

        name   [ NINERT +  39 ] =  "IAU_ENCELADUS";
        idcode [ NINERT +  39 ] =  10040;
        center [ NINERT +  39 ] =  602;
        typid  [ NINERT +  39 ] =  602;
        type   [ NINERT +  39 ] =  PCK;

        name   [ NINERT +  40 ] =  "IAU_TETHYS";
        idcode [ NINERT +  40 ] =  10041;
        center [ NINERT +  40 ] =  603;
        typid  [ NINERT +  40 ] =  603;
        type   [ NINERT +  40 ] =  PCK;

        name   [ NINERT +  41 ] =  "IAU_DIONE";
        idcode [ NINERT +  41 ] =  10042;
        center [ NINERT +  41 ] =  604;
        typid  [ NINERT +  41 ] =  604;
        type   [ NINERT +  41 ] =  PCK;

        name   [ NINERT +  42 ] =  "IAU_RHEA";
        idcode [ NINERT +  42 ] =  10043;
        center [ NINERT +  42 ] =  605;
        typid  [ NINERT +  42 ] =  605;
        type   [ NINERT +  42 ] =  PCK;

        name   [ NINERT +  43 ] =  "IAU_TITAN";
        idcode [ NINERT +  43 ] =  10044;
        center [ NINERT +  43 ] =  606;
        typid  [ NINERT +  43 ] =  606;
        type   [ NINERT +  43 ] =  PCK;

        name   [ NINERT +  44 ] =  "IAU_HYPERION";
        idcode [ NINERT +  44 ] =  10045;
        center [ NINERT +  44 ] =  607;
        typid  [ NINERT +  44 ] =  607;
        type   [ NINERT +  44 ] =  PCK;

        name   [ NINERT +  45 ] =  "IAU_IAPETUS";
        idcode [ NINERT +  45 ] =  10046;
        center [ NINERT +  45 ] =  608;
        typid  [ NINERT +  45 ] =  608;
        type   [ NINERT +  45 ] =  PCK;

        name   [ NINERT +  46 ] =  "IAU_PHOEBE";
        idcode [ NINERT +  46 ] =  10047;
        center [ NINERT +  46 ] =  609;
        typid  [ NINERT +  46 ] =  609;
        type   [ NINERT +  46 ] =  PCK;

        name   [ NINERT +  47 ] =  "IAU_JANUS";
        idcode [ NINERT +  47 ] =  10048;
        center [ NINERT +  47 ] =  610;
        typid  [ NINERT +  47 ] =  610;
        type   [ NINERT +  47 ] =  PCK;

        name   [ NINERT +  48 ] =  "IAU_EPIMETHEUS";
        idcode [ NINERT +  48 ] =  10049;
        center [ NINERT +  48 ] =  611;
        typid  [ NINERT +  48 ] =  611;
        type   [ NINERT +  48 ] =  PCK;

        name   [ NINERT +  49 ] =  "IAU_HELENE";
        idcode [ NINERT +  49 ] =  10050;
        center [ NINERT +  49 ] =  612;
        typid  [ NINERT +  49 ] =  612;
        type   [ NINERT +  49 ] =  PCK;

        name   [ NINERT +  50 ] =  "IAU_TELESTO";
        idcode [ NINERT +  50 ] =  10051;
        center [ NINERT +  50 ] =  613;
        typid  [ NINERT +  50 ] =  613;
        type   [ NINERT +  50 ] =  PCK;

        name   [ NINERT +  51 ] =  "IAU_CALYPSO";
        idcode [ NINERT +  51 ] =  10052;
        center [ NINERT +  51 ] =  614;
        typid  [ NINERT +  51 ] =  614;
        type   [ NINERT +  51 ] =  PCK;

        name   [ NINERT +  52 ] =  "IAU_ATLAS";
        idcode [ NINERT +  52 ] =  10053;
        center [ NINERT +  52 ] =  615;
        typid  [ NINERT +  52 ] =  615;
        type   [ NINERT +  52 ] =  PCK;

        name   [ NINERT +  53 ] =  "IAU_PROMETHEUS";
        idcode [ NINERT +  53 ] =  10054;
        center [ NINERT +  53 ] =  616;
        typid  [ NINERT +  53 ] =  616;
        type   [ NINERT +  53 ] =  PCK;

        name   [ NINERT +  54 ] =  "IAU_PANDORA";
        idcode [ NINERT +  54 ] =  10055;
        center [ NINERT +  54 ] =  617;
        typid  [ NINERT +  54 ] =  617;
        type   [ NINERT +  54 ] =  PCK;

        name   [ NINERT +  55 ] =  "IAU_ARIEL";
        idcode [ NINERT +  55 ] =  10056;
        center [ NINERT +  55 ] =  701;
        typid  [ NINERT +  55 ] =  701;
        type   [ NINERT +  55 ] =  PCK;

        name   [ NINERT +  56 ] =  "IAU_UMBRIEL";
        idcode [ NINERT +  56 ] =  10057;
        center [ NINERT +  56 ] =  702;
        typid  [ NINERT +  56 ] =  702;
        type   [ NINERT +  56 ] =  PCK;

        name   [ NINERT +  57 ] =  "IAU_TITANIA";
        idcode [ NINERT +  57 ] =  10058;
        center [ NINERT +  57 ] =  703;
        typid  [ NINERT +  57 ] =  703;
        type   [ NINERT +  57 ] =  PCK;

        name   [ NINERT +  58 ] =  "IAU_OBERON";
        idcode [ NINERT +  58 ] =  10059;
        center [ NINERT +  58 ] =  704;
        typid  [ NINERT +  58 ] =  704;
        type   [ NINERT +  58 ] =  PCK;

        name   [ NINERT +  59 ] =  "IAU_MIRANDA";
        idcode [ NINERT +  59 ] =  10060;
        center [ NINERT +  59 ] =  705;
        typid  [ NINERT +  59 ] =  705;
        type   [ NINERT +  59 ] =  PCK;

        name   [ NINERT +  60 ] =  "IAU_CORDELIA";
        idcode [ NINERT +  60 ] =  10061;
        center [ NINERT +  60 ] =  706;
        typid  [ NINERT +  60 ] =  706;
        type   [ NINERT +  60 ] =  PCK;

        name   [ NINERT +  61 ] =  "IAU_OPHELIA";
        idcode [ NINERT +  61 ] =  10062;
        center [ NINERT +  61 ] =  707;
        typid  [ NINERT +  61 ] =  707;
        type   [ NINERT +  61 ] =  PCK;

        name   [ NINERT +  62 ] =  "IAU_BIANCA";
        idcode [ NINERT +  62 ] =  10063;
        center [ NINERT +  62 ] =  708;
        typid  [ NINERT +  62 ] =  708;
        type   [ NINERT +  62 ] =  PCK;

        name   [ NINERT +  63 ] =  "IAU_CRESSIDA";
        idcode [ NINERT +  63 ] =  10064;
        center [ NINERT +  63 ] =  709;
        typid  [ NINERT +  63 ] =  709;
        type   [ NINERT +  63 ] =  PCK;

        name   [ NINERT +  64 ] =  "IAU_DESDEMONA";
        idcode [ NINERT +  64 ] =  10065;
        center [ NINERT +  64 ] =  710;
        typid  [ NINERT +  64 ] =  710;
        type   [ NINERT +  64 ] =  PCK;

        name   [ NINERT +  65 ] =  "IAU_JULIET";
        idcode [ NINERT +  65 ] =  10066;
        center [ NINERT +  65 ] =  711;
        typid  [ NINERT +  65 ] =  711;
        type   [ NINERT +  65 ] =  PCK;

        name   [ NINERT +  66 ] =  "IAU_PORTIA";
        idcode [ NINERT +  66 ] =  10067;
        center [ NINERT +  66 ] =  712;
        typid  [ NINERT +  66 ] =  712;
        type   [ NINERT +  66 ] =  PCK;

        name   [ NINERT +  67 ] =  "IAU_ROSALIND";
        idcode [ NINERT +  67 ] =  10068;
        center [ NINERT +  67 ] =  713;
        typid  [ NINERT +  67 ] =  713;
        type   [ NINERT +  67 ] =  PCK;

        name   [ NINERT +  68 ] =  "IAU_BELINDA";
        idcode [ NINERT +  68 ] =  10069;
        center [ NINERT +  68 ] =  714;
        typid  [ NINERT +  68 ] =  714;
        type   [ NINERT +  68 ] =  PCK;

        name   [ NINERT +  69 ] =  "IAU_PUCK";
        idcode [ NINERT +  69 ] =  10070;
        center [ NINERT +  69 ] =  715;
        typid  [ NINERT +  69 ] =  715;
        type   [ NINERT +  69 ] =  PCK;

        name   [ NINERT +  70 ] =  "IAU_TRITON";
        idcode [ NINERT +  70 ] =  10071;
        center [ NINERT +  70 ] =  801;
        typid  [ NINERT +  70 ] =  801;
        type   [ NINERT +  70 ] =  PCK;

        name   [ NINERT +  71 ] =  "IAU_NEREID";
        idcode [ NINERT +  71 ] =  10072;
        center [ NINERT +  71 ] =  802;
        typid  [ NINERT +  71 ] =  802;
        type   [ NINERT +  71 ] =  PCK;

        name   [ NINERT +  72 ] =  "IAU_NAIAD";
        idcode [ NINERT +  72 ] =  10073;
        center [ NINERT +  72 ] =  803;
        typid  [ NINERT +  72 ] =  803;
        type   [ NINERT +  72 ] =  PCK;

        name   [ NINERT +  73 ] =  "IAU_THALASSA";
        idcode [ NINERT +  73 ] =  10074;
        center [ NINERT +  73 ] =  804;
        typid  [ NINERT +  73 ] =  804;
        type   [ NINERT +  73 ] =  PCK;

        name   [ NINERT +  74 ] =  "IAU_DESPINA";
        idcode [ NINERT +  74 ] =  10075;
        center [ NINERT +  74 ] =  805;
        typid  [ NINERT +  74 ] =  805;
        type   [ NINERT +  74 ] =  PCK;

        name   [ NINERT +  75 ] =  "IAU_GALATEA";
        idcode [ NINERT +  75 ] =  10076;
        center [ NINERT +  75 ] =  806;
        typid  [ NINERT +  75 ] =  806;
        type   [ NINERT +  75 ] =  PCK;

        name   [ NINERT +  76 ] =  "IAU_LARISSA";
        idcode [ NINERT +  76 ] =  10077;
        center [ NINERT +  76 ] =  807;
        typid  [ NINERT +  76 ] =  807;
        type   [ NINERT +  76 ] =  PCK;

        name   [ NINERT +  77 ] =  "IAU_PROTEUS";
        idcode [ NINERT +  77 ] =  10078;
        center [ NINERT +  77 ] =  808;
        typid  [ NINERT +  77 ] =  808;
        type   [ NINERT +  77 ] =  PCK;

        name   [ NINERT +  78 ] =  "IAU_CHARON";
        idcode [ NINERT +  78 ] =  10079;
        center [ NINERT +  78 ] =  901;
        typid  [ NINERT +  78 ] =  901;
        type   [ NINERT +  78 ] =  PCK;

        name   [ NINERT +  79 ] =  "ITRF93";
        idcode [ NINERT +  79 ] =  13000;
        center [ NINERT +  79 ] =  399;
        typid  [ NINERT +  79 ] =  3000;
        type   [ NINERT +  79 ] =  PCK;

        name   [ NINERT +  80 ] =  "EARTH_FIXED";
        idcode [ NINERT +  80 ] =  10081;
        center [ NINERT +  80 ] =  399;
        typid  [ NINERT +  80 ] =  10081;
        type   [ NINERT +  80 ] =  TK;

        name   [ NINERT +  81 ] =  "IAU_PAN";
        idcode [ NINERT +  81 ] =  10082;
        center [ NINERT +  81 ] =  618;
        typid  [ NINERT +  81 ] =  618;
        type   [ NINERT +  81 ] =  PCK;

        name   [ NINERT +  82 ] =  "IAU_GASPRA";
        idcode [ NINERT +  82 ] =  10083;
        center [ NINERT +  82 ] =  9511010;
        typid  [ NINERT +  82 ] =  9511010;
        type   [ NINERT +  82 ] =  PCK;

        name   [ NINERT +  83 ] =  "IAU_IDA";
        idcode [ NINERT +  83 ] =  10084;
        center [ NINERT +  83 ] =  2431010;
        typid  [ NINERT +  83 ] =  2431010;
        type   [ NINERT +  83 ] =  PCK;

        name   [ NINERT +  84 ] =  "IAU_EROS";
        idcode [ NINERT +  84 ] =  10085;
        center [ NINERT +  84 ] =  2000433;
        typid  [ NINERT +  84 ] =  2000433;
        type   [ NINERT +  84 ] =  PCK;

        name   [ NINERT +  85 ] =  "IAU_CALLIRRHOE";
        idcode [ NINERT +  85 ] =  10086;
        center [ NINERT +  85 ] =  517;
        typid  [ NINERT +  85 ] =  517;
        type   [ NINERT +  85 ] =  PCK;

        name   [ NINERT +  86 ] =  "IAU_THEMISTO";
        idcode [ NINERT +  86 ] =  10087;
        center [ NINERT +  86 ] =  518;
        typid  [ NINERT +  86 ] =  518;
        type   [ NINERT +  86 ] =  PCK;

        name   [ NINERT +  87 ] =  "IAU_MEGACLITE";
        idcode [ NINERT +  87 ] =  10088;
        center [ NINERT +  87 ] =  519;
        typid  [ NINERT +  87 ] =  519;
        type   [ NINERT +  87 ] =  PCK;

        name   [ NINERT +  88 ] =  "IAU_TAYGETE";
        idcode [ NINERT +  88 ] =  10089;
        center [ NINERT +  88 ] =  520;
        typid  [ NINERT +  88 ] =  520;
        type   [ NINERT +  88 ] =  PCK;

        name   [ NINERT +  89 ] =  "IAU_CHALDENE";
        idcode [ NINERT +  89 ] =  10090;
        center [ NINERT +  89 ] =  521;
        typid  [ NINERT +  89 ] =  521;
        type   [ NINERT +  89 ] =  PCK;

        name   [ NINERT +  90 ] =  "IAU_HARPALYKE";
        idcode [ NINERT +  90 ] =  10091;
        center [ NINERT +  90 ] =  522;
        typid  [ NINERT +  90 ] =  522;
        type   [ NINERT +  90 ] =  PCK;

        name   [ NINERT +  91 ] =  "IAU_KALYKE";
        idcode [ NINERT +  91 ] =  10092;
        center [ NINERT +  91 ] =  523;
        typid  [ NINERT +  91 ] =  523;
        type   [ NINERT +  91 ] =  PCK;

        name   [ NINERT +  92 ] =  "IAU_IOCASTE";
        idcode [ NINERT +  92 ] =  10093;
        center [ NINERT +  92 ] =  524;
        typid  [ NINERT +  92 ] =  524;
        type   [ NINERT +  92 ] =  PCK;

        name   [ NINERT +  93 ] =  "IAU_ERINOME";
        idcode [ NINERT +  93 ] =  10094;
        center [ NINERT +  93 ] =  525;
        typid  [ NINERT +  93 ] =  525;
        type   [ NINERT +  93 ] =  PCK;

        name   [ NINERT +  94 ] =  "IAU_ISONOE";
        idcode [ NINERT +  94 ] =  10095;
        center [ NINERT +  94 ] =  526;
        typid  [ NINERT +  94 ] =  526;
        type   [ NINERT +  94 ] =  PCK;

        name   [ NINERT +  95 ] =  "IAU_PRAXIDIKE";
        idcode [ NINERT +  95 ] =  10096;
        center [ NINERT +  95 ] =  527;
        typid  [ NINERT +  95 ] =  527;
        type   [ NINERT +  95 ] =  PCK;

        name   [ NINERT +  96 ] =  "IAU_BORRELLY";
        idcode [ NINERT +  96 ] =  10097;
        center [ NINERT +  96 ] =  1000005;
        typid  [ NINERT +  96 ] =  1000005;
        type   [ NINERT +  96 ] =  PCK;

        name   [ NINERT +  97 ] =  "IAU_TEMPEL_1";
        idcode [ NINERT +  97 ] =  10098;
        center [ NINERT +  97 ] =  1000093;
        typid  [ NINERT +  97 ] =  1000093;
        type   [ NINERT +  97 ] =  PCK;

        name   [ NINERT +  98 ] =  "IAU_VESTA";
        idcode [ NINERT +  98 ] =  10099;
        center [ NINERT +  98 ] =  2000004;
        typid  [ NINERT +  98 ] =  2000004;
        type   [ NINERT +  98 ] =  PCK;

        name   [ NINERT +  99 ] =  "IAU_ITOKAWA";
        idcode [ NINERT +  99 ] =  10100;
        center [ NINERT +  99 ] =  2025143;
        typid  [ NINERT +  99 ] =  2025143;
        type   [ NINERT +  99 ] =  PCK;

        name   [ NINERT + 100 ] =  "IAU_CERES";
        idcode [ NINERT + 100 ] =  10101;
        center [ NINERT + 100 ] =  2000001;
        typid  [ NINERT + 100 ] =  2000001;
        type   [ NINERT + 100 ] =  PCK;

        name   [ NINERT + 101 ] =  "IAU_PALLAS";
        idcode [ NINERT + 101 ] =  10102;
        center [ NINERT + 101 ] =  2000002;
        typid  [ NINERT + 101 ] =  2000002;
        type   [ NINERT + 101 ] =  PCK;

        name   [ NINERT + 102 ] =  "IAU_LUTETIA";
        idcode [ NINERT + 102 ] =  10103;
        center [ NINERT + 102 ] =  2000021;
        typid  [ NINERT + 102 ] =  2000021;
        type   [ NINERT + 102 ] =  PCK;

        name   [ NINERT + 103 ] =  "IAU_DAVIDA";
        idcode [ NINERT + 103 ] =  10104;
        center [ NINERT + 103 ] =  2000511;
        typid  [ NINERT + 103 ] =  2000511;
        type   [ NINERT + 103 ] =  PCK;

        name   [ NINERT + 104 ] =  "IAU_STEINS";
        idcode [ NINERT + 104 ] =  10105;
        center [ NINERT + 104 ] =  2002867;
        typid  [ NINERT + 104 ] =  2002867;
        type   [ NINERT + 104 ] =  PCK;

        name   [ NINERT + 105 ] =  "IAU_BENNU";
        idcode [ NINERT + 105 ] =  10106;
        center [ NINERT + 105 ] =  2101955;
        typid  [ NINERT + 105 ] =  2101955;
        type   [ NINERT + 105 ] =  PCK;

        name   [ NINERT + 106 ] =  "IAU_52_EUROPA";
        idcode [ NINERT + 106 ] =  10107;
        center [ NINERT + 106 ] =  2000052;
        typid  [ NINERT + 106 ] =  2000052;
        type   [ NINERT + 106 ] =  PCK;

        name   [ NINERT + 107 ] =  "IAU_NIX";
        idcode [ NINERT + 107 ] =  10108;
        center [ NINERT + 107 ] =  902;
        typid  [ NINERT + 107 ] =  902;
        type   [ NINERT + 107 ] =  PCK;

        name   [ NINERT + 108 ] =  "IAU_HYDRA";
        idcode [ NINERT + 108 ] =  10109;
        center [ NINERT + 108 ] =  903;
        typid  [ NINERT + 108 ] =  903;
        type   [ NINERT + 108 ] =  PCK;

        name   [ NINERT + 109 ] =  "IAU_RYUGU";
        idcode [ NINERT + 109 ] =  10110;
        center [ NINERT + 109 ] =  2162173;
        typid  [ NINERT + 109 ] =  2162173;
        type   [ NINERT + 109 ] =  PCK;

        name   [ NINERT + 110 ] =  "IAU_ARROKOTH";
        idcode [ NINERT + 110 ] =  10111;
        center [ NINERT + 110 ] =  2486958;
        typid  [ NINERT + 110 ] =  2486958;
        type   [ NINERT + 110 ] =  PCK;

        name   [ NINERT + 111 ] =  "IAU_DIDYMOS_BARYCENTER";
        idcode [ NINERT + 111 ] =  10112;
        center [ NINERT + 111 ] =  20065803;
        typid  [ NINERT + 111 ] =  20065803;
        type   [ NINERT + 111 ] =  PCK;

        name   [ NINERT + 112 ] =  "IAU_DIDYMOS";
        idcode [ NINERT + 112 ] =  10113;
        center [ NINERT + 112 ] =  920065803;
        typid  [ NINERT + 112 ] =  920065803;
        type   [ NINERT + 112 ] =  PCK;

        name   [ NINERT + 113 ] =  "IAU_DIMORPHOS";
        idcode [ NINERT + 113 ] =  10114;
        center [ NINERT + 113 ] =  120065803;
        typid  [ NINERT + 113 ] =  120065803;
        type   [ NINERT + 113 ] =  PCK;

        name   [ NINERT + 114 ] =  "IAU_DONALDJOHANSON";
        idcode [ NINERT + 114 ] =  10115;
        center [ NINERT + 114 ] =  20052246;
        typid  [ NINERT + 114 ] =  20052246;
        type   [ NINERT + 114 ] =  PCK;

        name   [ NINERT + 115 ] =  "IAU_EURYBATES";
        idcode [ NINERT + 115 ] =  10116;
        center [ NINERT + 115 ] =  920003548;
        typid  [ NINERT + 115 ] =  920003548;
        type   [ NINERT + 115 ] =  PCK;

        name   [ NINERT + 116 ] =  "IAU_EURYBATES_BARYCENTER";
        idcode [ NINERT + 116 ] =  10117;
        center [ NINERT + 116 ] =  20003548;
        typid  [ NINERT + 116 ] =  20003548;
        type   [ NINERT + 116 ] =  PCK;

        name   [ NINERT + 117 ] =  "IAU_QUETA";
        idcode [ NINERT + 117 ] =  10118;
        center [ NINERT + 117 ] =  120003548;
        typid  [ NINERT + 117 ] =  120003548;
        type   [ NINERT + 117 ] =  PCK;

        name   [ NINERT + 118 ] =  "IAU_POLYMELE";
        idcode [ NINERT + 118 ] =  10119;
        center [ NINERT + 118 ] =  20015094;
        typid  [ NINERT + 118 ] =  20015094;
        type   [ NINERT + 118 ] =  PCK;

        name   [ NINERT + 119 ] =  "IAU_LEUCUS";
        idcode [ NINERT + 119 ] =  10120;
        center [ NINERT + 119 ] =  20011351;
        typid  [ NINERT + 119 ] =  20011351;
        type   [ NINERT + 119 ] =  PCK;

        name   [ NINERT + 120 ] =  "IAU_ORUS";
        idcode [ NINERT + 120 ] =  10121;
        center [ NINERT + 120 ] =  20021900;
        typid  [ NINERT + 120 ] =  20021900;
        type   [ NINERT + 120 ] =  PCK;

        name   [ NINERT + 121 ] =  "IAU_PATROCLUS_BARYCENTER";
        idcode [ NINERT + 121 ] =  10122;
        center [ NINERT + 121 ] =  20000617;
        typid  [ NINERT + 121 ] =  20000617;
        type   [ NINERT + 121 ] =  PCK;

        name   [ NINERT + 122 ] =  "IAU_PATROCLUS";
        idcode [ NINERT + 122 ] =  10123;
        center [ NINERT + 122 ] =  920000617;
        typid  [ NINERT + 122 ] =  920000617;
        type   [ NINERT + 122 ] =  PCK;

        name   [ NINERT + 123 ] =  "IAU_MENOETIUS";
        idcode [ NINERT + 123 ] =  10124;
        center [ NINERT + 123 ] =  120000617;
        typid  [ NINERT + 123 ] =  120000617;
        type   [ NINERT + 123 ] =  PCK;

        // Populate the lists
        for (int i = 0; i < NCOUNT; i++ ) {
            builtIdList.add(Integer.valueOf(idcode[i]));
            builtNameList.add(name[i]);
        }
        
    }
    
    //CHECKSTYLE: resume MagicNumber check
}
