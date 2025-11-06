/**
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
 * HISTORY
 * VERSION:4.16:OPENFD-547:25/04/2025:[PATRIUS] Mauvaise gestion de computeSpinDerivatives dans BSPEphemerisLoader
 * VERSION:4.16:OPENFD-468:25/04/2025:[PATRIUS] Renommer toutes les mentions du GeodeticPoint
 * VERSION:4.16:OPENFD-378:25/04/2025:[PATRIUS] Utiliser l'ID Spice plutot que le nom Spice pour la lecture des bsp
 * VERSION:4.15:OPENFD-428:21/11/2024:[PATRIUS] Robustesse aux bsp contenant l'échelle TDB
 * VERSION:4.14:OPENFD-:22/08/2024:
 * VERSION:4.14:OPENFD-141:22/08/2024: Isolation des algorithmes de somme et produit precis
 * VERSION:4.14:OPENFD-172:22/08/2024:[PATRIUS] Harmonisation de la gestion
 * des reperes predefinis et des corps predefinis
 * VERSION:4.14:OPENFD-258:22/08/2024:[PATRIUS] Ephemerides des barycentres planetaires
 * dans les fichiers JPL historiques
 * VERSION:4.14:OPENFD-171:22/08/2024: [PATRIUS] Lecture d'un corps celeste quelconque dans un fichier bsp
 * VERSION:4.14:OPENFD-253:22/08/2024: [PATRIUS] Problemes e l'utilisation des bsp planetaires
 * VERSION:4.14:OPENFD-179:22/08/2024: [PATRIUS] Gestion emetteur/recepteur dans les detecteurs d'evenements
 * VERSION:4.13.1:FA:FA-170:17/01/2024:[PATRIUS] Impossible d'utiliser le corps racine d'un bsp comme corps pivot
 * VERSION:4.13:FA:FA-112:08/12/2023:[PATRIUS] Probleme si Earth est utilise comme corps pivot pour mar097.bsp
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:DM:DM-49:30/06/2023:[PATRIUS] Extraction arbre des reperes SPICE et link avec CelestialBodyFactory
 * VERSION:4.11.1:DM:DM-88:30/06/2023:[PATRIUS] Complement FT 3319
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.bsp;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import fr.cnes.sirius.patrius.bodies.BSPCelestialBodyLoader;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemeris;
import fr.cnes.sirius.patrius.bodies.CelestialBodyEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.JPLEphemerisLoader;
import fr.cnes.sirius.patrius.bodies.PredefinedEphemerisType;
import fr.cnes.sirius.patrius.bodies.bsp.spice.DafHandle;
import fr.cnes.sirius.patrius.bodies.bsp.spice.DafReader;
import fr.cnes.sirius.patrius.bodies.bsp.spice.FindArraysDAF;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceBody;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceCommon;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpiceFrame;
import fr.cnes.sirius.patrius.bodies.bsp.spice.SpkReader;
import fr.cnes.sirius.patrius.data.DataLoader;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.data.PATRIUSFileInputStream;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Loader for the SPICE BSP format (type 2 and 3).
 *
 * For more details about the SPICE BSP format, please read the CSPICE documentation
 * <a href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/spk.html">here</a>
 * <p>
 * This reader implements the {@link CelestialBodyEphemerisLoader} interface and develops its own loading methods.
 * <p>
 *
 * @author Emmanuel Bignon
 *
 * @since 4.11.1
 */
public class BSPEphemerisLoader implements JPLEphemerisLoader, DataLoader {

    /** Identifier of an empty target ID in a bsp file managing TT-TDB tabulation */
    private static final int TARGET_ID_FOR_TDB_SEGMENTS = 1000000001;

    /** Identifier of an empty observer ID in a bsp file managing TT-TDB tabulation */
    private static final int OBSERVER_ID_FOR_TBD_SEGMENTS = 1000000000;

    /** Default supported files name pattern for BSP files. */
    public static final String DEFAULT_BSP_SUPPORTED_NAMES = "*\\.bsp$";

    /**
     * The mu constants are coming from
     * <a href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/spk.html">here</a>
     */

    /** Mu Sun. */
    public static final double MU_SUN = 1.3271244004127942E+20;

    /** Mu Mercury. */
    public static final double MU_MERCURY = 2.2031868551400003E+13;

    /** Mu Venus. */
    public static final double MU_VENUS = 3.2485859200000000E+14;

    /** Mu Earth Moon barycenter. */
    public static final double MU_EARTH_MOON = 4.0350323562548019E+14;

    /** Mu Earth. */
    public static final double MU_EARTH = 3.9860043550702266E14;

    /** Mu Moon. */
    public static final double MU_MOON = 4.9028001184575496E12;

    /** Mu Mars. */
    public static final double MU_MARS = 4.2828375815756102E+13;

    /** Mu Jupiter. */
    public static final double MU_JUPITER = 1.2671276409999998E+17;

    /** Mu Saturn. */
    public static final double MU_SATURN = 3.7940584841799997E+16;

    /** Mu Uranus. */
    public static final double MU_URANUS = 5.7945563999999985E+15;

    /** Mu Neptune. */
    public static final double MU_NEPTUNE = 6.8365271005803989E15;

    /** Mu Pluto. */
    public static final double MU_PLUTO = 9.7550000000000000E11;

    /**
     * The basic spatial reference system for SPICE is the J2000 system.<br>
     * This is an inertial reference frame in which the equations of motion for the solar system may be integrated.<br>
     * This reference frame is specified by the orientation of the earth's mean equator and equinox at a particular
     * epoch --- the J2000 epoch.<br>
     * This epoch is Greenwich noon on January 1, 2000 Barycentric Dynamical Time (TDB).
     */
    public static final AbsoluteDate SPICE_J2000_EPOCH = new AbsoluteDate(2000, 1, 1, 12, 0, 0.0,
        TimeScalesFactory.getTDB());

    /** Serializable UID. */
    private static final long serialVersionUID = 5353799366726936011L;

    /** Spice convention. */
    private SpiceJ2000ConventionEnum convention = SpiceJ2000ConventionEnum.ICRF;

    /** PATRIUS frame tree link (ICRF by default). */
    private Frame rootPatriusFrame = FramesFactory.getICRF();

    /** BSP body name linked to PATRIUS frame tree. */
    private String bspBodyLink = null;

    /**
     * PATRIUS frame used as Spice J2000: either EME2000 or ICRF.
     */
    public enum SpiceJ2000ConventionEnum {

        /** EME2000. */
        EME2000,

        /** ICRF. */
        ICRF;
    }

    /** BSP ephemeris (Body ID, BSPCelestialBodyEphemeris), one per segment. */
    private final Map<Integer, BSPCelestialBodyEphemeris> ephemeris;

    /** Supported file names. */
    private final String supportedNames;

    /**
     * Constructor.
     *
     * @param supportedNames
     *        supported names
     */
    public BSPEphemerisLoader(final String supportedNames) {
        super();
        this.supportedNames = supportedNames;
        this.ephemeris = new HashMap<>();
    }

    /**
     * Read all BSP segments.
     *
     * @param bspFile
     *        BSP file
     * @return a map of the segments contained in the file
     * @throws PatriusException
     *         if the file is not open or there is a problem while reading the first record
     * @throws IOException
     *         if there is a problem tempting to read the first record
     */
    private Map<Integer, BSPCelestialBodyEphemeris> readSegments(final String bspFile)
            throws PatriusException, IOException {
        // Initialization of SPK reading by getting the initial handle
        final int[] handle = new int[1];
        final boolean loaded = DafHandle.isLoaded(bspFile, handle);
        if (!loaded) {
            throw new PatriusException(PatriusMessages.PDB_SPICE_KERNEL_NOT_LOADED, bspFile);
        }

        // Reading the content of the record of the SPK File
        final int[] kernelRecord = readRecord(handle[0]);
        final int nd = kernelRecord[0];
        final int ni = kernelRecord[1];

        // Beginning forward search in the SPK kernel
        FindArraysDAF.beginForwardSearch(handle[0]);
        FindArraysDAF.selectDaf(handle[0]);

        // Now, we are going to scroll through the SPK kernel segments.
        // While scrolling the segments, we instantiate the SPK segment
        // to encapsulate each array's descriptor, filling the spkRawData.
        return readSegments(nd, ni);
    }

    /**
     * Read a record.
     *
     * @param handle
     *        handle
     * @return the record
     * @throws PatriusException
     *         if the file is not open or there is a problem while reading the first record
     * @throws IOException
     *         if there is a problem tempting to read the first record
     */
    private static int[] readRecord(final int handle) throws PatriusException, IOException {
        final String[] ifname = new String[1];
        final int[] fward = new int[1];
        final int[] bward = new int[1];
        final int[] free = new int[1];
        final int[] ndArg = new int[1];
        final int[] niArg = new int[1];
        DafReader.readFileRecord(handle, ndArg, niArg, ifname, fward, bward, free);
        return new int[] { ndArg[0], niArg[0], free[0], fward[0], bward[0] };
    }

    /**
     * Read all segments.
     *
     * @param nd
     *        kernel record size
     * @param ni
     *        kernel record size
     * @return segments
     * @throws PatriusException
     *         if reading failed
     */
    private Map<Integer, BSPCelestialBodyEphemeris> readSegments(final int nd, final int ni) throws PatriusException {

        // Segment list to return
        final Map<Integer, BSPCelestialBodyEphemeris> ephemerisList = new HashMap<>();

        // Find the next array to read its data
        boolean found = FindArraysDAF.findNextArray();

        // Counting segments and printing content
        while (found) {
            // While found is true, we are scrolling through the CK kernel array by array and getting the data of each
            // segment descriptor

            // Reading array descriptor (or summary)
            final double[] summary = FindArraysDAF.getSummaryOfArray(); // CSPICE.dafgs(recordSize);
            final double[] dc = new double[nd];
            final int[] ic = new int[ni];
            SpiceCommon.unpackSummary(summary, nd, ni, dc, ic);

            // Build segment
            final int targetID = ic[0];
            final int centerID = ic[1];
            final int frameID = ic[2];
            final DAFSegment segment = new DAFSegment(targetID, centerID, frameID);
            ephemerisList.put(targetID, new BSPCelestialBodyEphemeris(segment));

            // Testing if there is a next array and iterating if so
            found = FindArraysDAF.findNextArray();
        }

        // Link parent/children for all segments
        for (final BSPCelestialBodyEphemeris e : ephemerisList.values()) {
            // Set parent
            final int observerID = e.segment.observerID;
            for (final BSPCelestialBodyEphemeris ephemeris2 : ephemerisList.values()) {
                final int targetID2 = ephemeris2.segment.targetID;
                if (targetID2 == observerID) {
                    // Set parent
                    e.segment.parent = ephemeris2.segment;
                    break;
                }
            }

            // Set children
            final int targetID = e.segment.targetID;
            for (final BSPCelestialBodyEphemeris ephemeris2 : ephemerisList.values()) {
                final int observerID2 = ephemeris2.segment.observerID;
                if (targetID == observerID2) {
                    // Add child
                    e.segment.children.add(ephemeris2.segment);
                }
            }
        }

        return ephemerisList;
    }

    /**
     * Load celestial body ephemeris.
     *
     * @param patriusName
     *        body name known by Patrius
     * @return loaded celestial body
     * @throws PatriusException
     *         if the body, given its name, is not in the file
     */
    @Override
    public CelestialBodyEphemeris loadCelestialBodyEphemeris(final String patriusName) throws PatriusException {
        if (this.ephemeris.isEmpty()) {
            // No loaded ephemeris at the moment, try loading new ephemeris
            if (!DataProvidersManager.getInstance().feed(this.supportedNames, this)) {
                throw new PatriusException(PatriusMessages.NO_JPL_EPHEMERIDES_BINARY_FILES_FOUND);
            }
        }

        // Retrieve the bsp ID
        final String bspName = BSPCelestialBodyLoader.toSpiceName(patriusName);
        final boolean[] found = { false };
        final int bspID = SpiceBody.bodyString2Code(bspName, found);

        if (!found[0] || this.ephemeris.get(bspID) == null) {
            throw new PatriusException(PatriusMessages.BODY_NOT_AVAILABLE_IN_BSP_FILE, bspName,
                this.supportedNames);
        }

        return this.ephemeris.get(bspID);
    }


    /** {@inheritDoc} */
    @Override
    public boolean stillAcceptsData() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, PatriusException {
        // Only PATRIUSFileInputStream are handled (Spice only reads files)
        if (!(input instanceof PATRIUSFileInputStream)) {
            throw new PatriusException(PatriusMessages.WRONG_FILE_FORMAT);
        }

        // File name
        final String bspFile = ((PATRIUSFileInputStream) input).getFile().getAbsolutePath();

        // Read segments and add to ephemeris map
        // WARNING: already known objects may be overridden
        this.ephemeris.putAll(readSegments(bspFile));

        manageSpecialSegments();

        // Add root object which is not defined as a segment in BSP file
        final int rootID = getRootID();
        final DAFSegment rootSegment = new DAFSegment(rootID, rootID, 0);
        final BSPCelestialBodyEphemeris rootEphem = new BSPCelestialBodyEphemeris(rootSegment);
        this.ephemeris.put(rootID, rootEphem);
    }

    /**
     * Manages special segments within the ephemeris data.
     * <p>
     * This method iterates through the segments in the ephemeris data structure and identifies "special segments" (not
     * trajectory segments). These identified segments are subsequently removed from the ephemeris.
     * </p>
     * <p>
     * This functionality is useful for handling specialized cases (e.g.,
     * TT-TDB time correction segments) that should not be part of the primary
     * ephemeris data.
     * </p>
     *
     * @see DAFSegment
     * @see BSPCelestialBodyEphemeris
     */
    private void manageSpecialSegments() {
        // Manage special segments
        final List<Integer> keysToBeRemoved = new ArrayList<>();
        for (final Entry<Integer, BSPCelestialBodyEphemeris> ephemEntry : this.ephemeris.entrySet()) {

            final DAFSegment segment = ephemEntry.getValue().segment;
            // Special segment for TT-TDB
            if (segment.observerID == OBSERVER_ID_FOR_TBD_SEGMENTS && segment.targetID == TARGET_ID_FOR_TDB_SEGMENTS) {
                keysToBeRemoved.add(ephemEntry.getKey());
            }
        }

        // Remove special segments
        for (final Integer key : keysToBeRemoved) {
            this.ephemeris.remove(key);
        }
    }

    /**
     * Returns the root ID.
     *
     * @return the root ID
     */
    private int getRootID() {
        int res = 0;
        if (!this.ephemeris.isEmpty()) {
            // Get first segment
            DAFSegment segment = this.ephemeris.entrySet().iterator().next().getValue().segment;
            // Find root observer (should be unique)
            while (this.ephemeris.get(segment.observerID) != null) {
                segment = this.ephemeris.get(segment.observerID).segment;
            }
            res = segment.observerID;
        }
        return res;
    }

    /**
     * Get the gravitational coefficient of a body. These coefficient values are coming from
     * <a href="https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/spk.html">here</a>.
     *
     * @param body
     *        body for which the gravitational coefficient is requested
     * @return gravitational coefficient in m<sup>3</sup>/s<sup>2</sup>
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: enumeration of solar system bodies
    @Override
    public double getLoadedGravitationalCoefficient(final PredefinedEphemerisType body) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        final double mu;
        switch (body) {
            // SSB
            case SOLAR_SYSTEM_BARYCENTER:
                mu = MU_SUN + MU_MERCURY + MU_VENUS + MU_EARTH_MOON + MU_MARS + MU_JUPITER + MU_SATURN + MU_URANUS
                        + MU_NEPTUNE + MU_PLUTO;
                break;
            // SUN
            case SUN:
                mu = MU_SUN;
                break;
            // MERCURY
            case MERCURY:
                mu = MU_MERCURY;
                break;
            // VENUS
            case VENUS:
                mu = MU_VENUS;
                break;
            // EMB
            case EARTH_MOON:
                mu = MU_EARTH_MOON;
                break;
            // EARTH
            case EARTH:
                mu = MU_EARTH;
                break;
            // MOON
            case MOON:
                mu = MU_MOON;
                break;
            // MARS
            case MARS:
            case MARS_BARY:
                mu = MU_MARS;
                break;
            // JUPITER
            case JUPITER:
            case JUPITER_BARY:
                mu = MU_JUPITER;
                break;
            // SATURN
            case SATURN:
            case SATURN_BARY:
                mu = MU_SATURN;
                break;
            // URANUS
            case URANUS:
            case URANUS_BARY:
                mu = MU_URANUS;
                break;
            // NEPTUNE
            case NEPTUNE:
            case NEPTUNE_BARY:
                mu = MU_NEPTUNE;
                break;
            // PLUTO
            case PLUTO:
            case PLUTO_BARY:
                mu = MU_PLUTO;
                break;
            default:
                // Should not happen
                throw PatriusException.createInternalError(null);
        }
        // return mu in m<sup>3</sup>/s<sup>2</sup>
        return mu;
    }

    /**
     * Multiplies the input array by the input scalar and returns the result as an array of the same size.
     *
     * @param array
     *        Input array
     * @param scalar
     *        Input scalar
     * @return Input array * input scalar as an array
     */
    private static double[] scalarArrayMultiplication(final double[] array, final double scalar) {
        // Creating output array
        final double[] scaledArray = array.clone();
        for (int k = 0; k < array.length; k++) {
            scaledArray[k] *= scalar;
        }
        return scaledArray;
    }

    /**
     * Setter for the Spice J2000 convention.
     *
     * @param newConvention
     *        Spice J2000 convention
     */
    public void setSPICEJ2000Convention(final SpiceJ2000ConventionEnum newConvention) {
        this.convention = newConvention;
    }

    /**
     * Getter for the Spice J2000 convention.
     *
     * @return the Spice J2000 convention
     */
    public SpiceJ2000ConventionEnum getConvention() {
        return this.convention;
    }

    /**
     * Returns the BSP body name linked to PATRIUS frame tree.
     *
     * @return the BSP body name linked to PATRIUS frame tree
     */
    public String getBodyLink() {
        return this.bspBodyLink;
    }

    /**
     * Link a BSP IAU frame (given its name) to a PATRIUS frame.
     * <p>
     * For example, providing Mars ICRF frame and BSP name "MARS" will set BSP MARS body IAU frame as Mars ICRF frame.
     * </p>
     * <p>
     * By default, BSP ICRF and PATRIUS ICRF are linked together (they are the same frame).
     * </p>
     *
     * @param patriusFrame
     *        an existing PATRIUS frame
     * @param bspBodyName
     *        a body from BSP file, its IAU frame will become the provided PATRIUS frame
     * @throws PatriusException
     *         if body not in file
     */
    public void linkFramesTrees(final Frame patriusFrame, final String bspBodyName) throws PatriusException {
        // Try load body
        // Checks if body exists, if not an exception is thrown
        loadCelestialBodyEphemeris(bspBodyName);

        // Store PATRIUS frame tree - BSP link
        this.rootPatriusFrame = patriusFrame;
        this.bspBodyLink = bspBodyName.toUpperCase(Locale.US);
    }

    /**
     * Add complementary (body code, body name) mapping which is not included in
     * SPICE default mapping.
     * <P>
     * Warning: this method should be called before any computation using
     * {@link BSPEphemerisLoader}.
     * </p>
     * <p>
     * SPICE body code-name mapping is defined statically and is therefore
     * common to all {@link BSPEphemerisLoader}.
     * </p>
     *
     * @param bodyCodeNameMapping
     *        (body code, body name) mapping
     */
    public static void addSpiceBodyMapping(final Map<Integer, String> bodyCodeNameMapping) {
        SpiceBody.addSpiceBodyMapping(bodyCodeNameMapping);
    }

    /**
     * Clear the SPICE Body mapping
     */
    public static void clearSpiceBodyMapping() {
        SpiceBody.clearSpiceBodyMapping();
    }

    /** Local celestial body ephemeris class. */
    private static class BSPCelestialBodyEphemeris implements CelestialBodyEphemeris {

        /** Serializable UID. */
        private static final long serialVersionUID = -2941415197776129165L;

        /** DAF segment associated to ephemeris. */
        private final DAFSegment segment;

        /**
         * Constructor.
         *
         * @param segment
         *        DAF segment
         */
        public BSPCelestialBodyEphemeris(final DAFSegment segment) {
            this.segment = segment;
        }

        /** {@inheritDoc} */
        @Override
        public PVCoordinates getPVCoordinates(final AbsoluteDate date,
                                              final Frame frame)
            throws PatriusException {
            // Get PV in parent frame
            final PVCoordinates pvParentFrame = this.segment.getRawPVCoordinates(date);
            // Get PV in output frame
            if (frame.equals(this.segment.getObserverFrame())) {
                // General case, if ephemeris automatically accessed from Celestial Body
                return pvParentFrame;
            }

            final Transform t = this.segment.getObserverFrame().getTransformTo(frame, date);
            return t.transformPVCoordinates(pvParentFrame);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
            return this.segment.getObserverFrame();
        }
    }

    /**
     * DAF Segment of BSP file.<br>
     * A segment contains the ephemeris of a target, expressed in an observer/center frame.
     */
    private class DAFSegment implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = -167437426470074431L;

        /** Target identifier */
        private final int targetID;
        
        /** Target name. */
        private final String targetName;
        
        /** Observer ID. */
        private final int observerID;

        /** Observer name. */
        private final String observerName;

        /** Observer frame name. */
        private final String observerFrameName;

        /** Children segments (may be empty) - Set after construction. */
        private final List<DAFSegment> children;

        /** Observer frame. */
        private Frame observerFrame;

        /** Parent segment (null if root segment) - Set after construction. */
        private DAFSegment parent;

        /**
         * Constructor.
         *
         * @param targetID
         *        target ID
         * @param centerID
         *        center ID
         * @param frameID
         *        center frame ID
         * @throws PatriusException
         *         if conversion failed
         */
        public DAFSegment(final int targetID, final int centerID, final int frameID) throws PatriusException {
            this.targetID = targetID;
            this.targetName = SpiceBody.bodyCode2Name(targetID);
            this.observerID = centerID;
            this.observerName = SpiceBody.bodyCode2Name(centerID);
            this.observerFrameName = SpiceFrame.frameId2Name(frameID);
            this.children = new ArrayList<>();
        }

        /**
         * Getter for the PV coordinates of target of segment in center frame.
         *
         * @param date
         *        a date
         * @return returns the PV coordinates of target of segment in center frame
         * @throws PatriusException
         *         if computation failed
         */
        private PVCoordinates getRawPVCoordinates(final AbsoluteDate date) throws PatriusException {
            final double[] lt = new double[1];

            // getNbrSecAfterJ2000InTDB
            final double nbrSecAfterJ2000InTDB = date.durationFrom(SPICE_J2000_EPOCH, TimeScalesFactory.getTDB());

            // Call Spice routine
            final double[] state = SpkReader.getStateRelativeToBody(this.targetName, nbrSecAfterJ2000InTDB,
                this.observerFrameName, this.observerName, lt);

            // SPICE SPK high level state reading routines return distances in km and velocities in km/s whereas
            // PVCoordinates uses m and m/s. So we must convert those values into m and m/s before creating the
            // PVCoordinates object.
            final double[] scaledState = scalarArrayMultiplication(state, Constants.KM_TO_M);

            // Return the target state as a SPKState object
            return new PVCoordinates(scaledState[0], scaledState[1], scaledState[2], scaledState[3],
                scaledState[4], scaledState[5]);
        }

        /**
         * Getter for the segment observer frame. Built recursively on the fly (lazy initialization).
         *
         * @return the segment observer frame
         * @throws PatriusException
         *         if failed to retrieve body name from ID
         */
        private Frame getObserverFrame() throws PatriusException {
            if (this.observerFrame == null) {
                buildFrame();
            }
            return this.observerFrame;
        }

        /**
         * Setter for the observer frame. Also set frame for segments having the same parent. This avoid different
         * references.
         *
         * @param frame
         *        frame to set
         */
        private void setObserverFrame(final Frame frame) {
            this.observerFrame = frame;
            if (this.parent != null) {
                for (final DAFSegment dafSegment : this.parent.children) {
                    dafSegment.observerFrame = frame;
                }
            }
        }

        /**
         * Build full observer frame name.
         *
         * @return full observer frame name
         */
        private String buildFullObserverFrameName() {
            final String SPACE = " ";
            return this.observerName + SPACE + BSPEphemerisLoader.this.convention + SPACE
                    + BSPEphemerisLoader.this.supportedNames;
        }

        /**
         * Recursively build segment center frame on the fly.
         * If BSP body link is defined, there are 3 main cases:
         * <ul>
         * <li>Current observer is between BSP body link and SSB</li>
         * <li>Current observer is below BSP body link</li>
         * <li>Current observer is on a different branch from BSP body link - SSB</li>
         * </ul>
         * <p>
         * SSB has to be treated separately since it is not defined in BSP file (it only has observer ID = 0).
         * </p>
         *
         * @throws PatriusException
         *         if failed to retrieve body name from ID
         */
        private void buildFrame() throws PatriusException {
            final String currentFrameName = buildFullObserverFrameName();
            if (BSPEphemerisLoader.this.bspBodyLink == null) {
                // Root frame is ICRF
                // Frame if built recursively from current to SSB, regular transform
                buildFrameUpward();

            } else {
                // Root frame is a body defined in BSP file

                // Simplest case : observer body is the link body
                if (BSPEphemerisLoader.this.bspBodyLink.equals(this.observerName)) {
                    setObserverFrame(BSPEphemerisLoader.this.rootPatriusFrame);
                    return;
                }

                // Segment associated to BSP body link
                final boolean[] found = {false};
                final int bspBodyID = SpiceBody.bodyName2Code(BSPEphemerisLoader.this.bspBodyLink, found);

                final DAFSegment bspBodyLinkSegment = BSPEphemerisLoader.this.ephemeris.get(bspBodyID).segment;

                // Check if current observer name is on the ancestors path of BSP body link segment observers
                // (until ICRF if required)
                DAFSegment currentS = bspBodyLinkSegment;
                boolean isOnPathBSPLinkToICRF = false;
                final List<DAFSegment> segments = new ArrayList<>();
                while (currentS != null && !isOnPathBSPLinkToICRF) {
                    segments.add(currentS);
                    isOnPathBSPLinkToICRF = currentS.observerID == this.observerID;
                    currentS = currentS.parent;
                }

                if (isOnPathBSPLinkToICRF) {
                    // Build frames tree iteratively (not recursively) from BSP body link frame to current frame
                    // - Inverted transforms

                    // Link with BSP body link
                    Frame intermediateFrame = new CelestialBodyFrame(BSPEphemerisLoader.this.rootPatriusFrame,
                        getTransformProvider(bspBodyLinkSegment, true),
                        bspBodyLinkSegment.buildFullObserverFrameName(), null);
                    setObserverFrame(intermediateFrame);
                    // Other frames until current frame
                    for (int i = 1; i < segments.size(); i++) {
                        intermediateFrame = new CelestialBodyFrame(intermediateFrame, getTransformProvider(
                            segments.get(i), true), segments.get(i).buildFullObserverFrameName(), null);
                        setObserverFrame(intermediateFrame);
                    }
                } else {
                    // Other case - BSP body link is either:
                    // - Above current observer (i.e. is an ancestor of current observer) - Transforms are inverted
                    // - Not on the path from current observer to SSB - Regular transforms to SSB

                    // Build frames tree recursively

                    // Check if BSP body link is an ancestor of current observer
                    // Find children on path from current observer to link body starting from link body
                    final String currentTargetName = BSPEphemerisLoader.this.bspBodyLink;
                    int currentTargetID = SpiceBody.bodyName2Code(currentTargetName, found);

                    int index = childOf(currentTargetID);
                    while (index == -1) {
                        final DAFSegment currentSegment =
                                BSPEphemerisLoader.this.ephemeris.get(currentTargetID).segment;
                        if (currentSegment.parent == null) {
                            // BSP SSB has been reached
                            break;
                        }
                        currentTargetID = currentSegment.parent.targetID;
                        index = childOf(currentTargetID);
                    }

                    if (index == -1) {
                        // BSP SSB has been reached, required frame is not on the path [BSP body link to BSP SSB]
                        // Frame if build from current to SSB, regular transform (no inversion)
                        buildFrameUpward();
                    } else {
                        // BSP body link is an ancestor
                        // Build observer frame recursively, inverted transform
                        setObserverFrame(new CelestialBodyFrame(this.children.get(index).getObserverFrame(),
                            getTransformProvider(this, true), currentFrameName, null));
                    }
                }
            }
        }

        /**
         * Recursively build segment center frame on the fly in upward direct (from this to parent frame
         * until SSB if required).
         *
         * @throws PatriusException
         *         if failed to retrieve body name from ID
         */
        private void buildFrameUpward() throws PatriusException {
            if (this.observerID == 0) {
                // Root frame default case (ICRF or EME2000)
                if (BSPEphemerisLoader.this.convention == SpiceJ2000ConventionEnum.ICRF) {
                    setObserverFrame(FramesFactory.getICRF());
                } else if (BSPEphemerisLoader.this.convention == SpiceJ2000ConventionEnum.EME2000) {
                    // EME2000 convention is not handled in this case
                    throw new PatriusException(PatriusMessages.EME2000_CONVENTION_NOT_SUPPORTED);
                }
            } else {
                final DAFSegment parentSegment = BSPEphemerisLoader.this.ephemeris.get(this.observerID).segment;
                // Regular transform, no inversion
                final TransformProvider transformProvider = getTransformProvider(parentSegment, false);
                // Recursive algorithm: build parent segment observer frame if not existent
                setObserverFrame(new CelestialBodyFrame(parentSegment.getObserverFrame(), transformProvider,
                    buildFullObserverFrameName(), null));
            }
        }

        /**
         * Find position of body ID in children segments.
         *
         * @param target
         *        a body ID
         * @return position of body ID in children segments if found, -1 otherwise
         */
        private int childOf(final int target) {

            // Not found
            int child = -1;

            // Look for it
            for (int i = 0; i < this.children.size(); i++) {
                if (this.children.get(i).targetID == target) {
                    child = i;
                    break;
                }
            }

            return child;
        }

        /**
         * Build transform.
         *
         * @param segment
         *        a segment
         * @param isInverted
         *        true if transform should be inverted
         * @return transform from parent to self
         */
        private TransformProvider getTransformProvider(final DAFSegment segment, final boolean isInverted) {
            return new TransformProvider(){
                /** Serializable UID. */
                private static final long serialVersionUID = 888245954659121731L;

                /** {@inheritDoc} */
                @Override
                public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
                                              final boolean computeSpinDerivatives)
                    throws PatriusException {
                    return getTransform(date, computeSpinDerivatives);
                }

                /** {@inheritDoc} */
                @Override
                public Transform getTransform(final AbsoluteDate date, final boolean computeSpinDerivatives)
                    throws PatriusException {
                    Transform t = new Transform(date, segment.getRawPVCoordinates(date));

                    if (isInverted) {
                        t = t.getInverse(computeSpinDerivatives);
                    }

                    return t;
                }

                /** {@inheritDoc} */
                @Override
                public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config)
                    throws PatriusException {
                    return getTransform(date);
                }

                /** {@inheritDoc} */
                @Override
                public Transform getTransform(final AbsoluteDate date) throws PatriusException {
                    return getTransform(date, false);
                }
            };
        }
    }
}
