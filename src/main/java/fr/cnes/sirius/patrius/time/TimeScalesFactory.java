/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2022 CNES
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:DM:DM-2649:18/05/2021: ajout d un getter parametrable TimeScalesFactory
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Removed dependency to UTC-TAI in UT1Scale
 * VERSION::FA:209:10/03/2014:Removed getGMST(EOPInterpolator) method because
 * the parameter wasn't used anywhere
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Factory for predefined time scales.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link UTCTAIHistoryFilesLoader}: UTC-TAI file in IERS format (file UTC-TAI.history). See
 * <a href="http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history">IERS UTC-TAI.history file</a> for more information
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used for UTC time scale through {@link #getUTC()} method.
 * </p>
 * 
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NonThreadSafeSingleton")
public final class TimeScalesFactory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -2063625014942931917L;

    /** Universal Time Coordinate scale. */
    private static UTCScale utc = null;

    /** Universal Time 1 scale. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile UT1Scale ut1 = null;

    /** Greenwich Mean Sidereal Time scale. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile GMSTScale gmst = null;

    /** UTCTAI offsets loaders. */
    private static List<UTCTAILoader> loaders = new ArrayList<UTCTAILoader>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private TimeScalesFactory() {
    }

    /**
     * Add a loader for UTC-TAI offsets history files.
     * 
     * @param loader custom loader to add
     * @see #getUTC()
     * @see #clearUTCTAILoaders()
     */
    public static void addUTCTAILoader(final UTCTAILoader loader) {
        loaders.add(loader);
    }

    /**
     * Add the default loader for UTC-TAI offsets history files.
     * <p>
     * The default loader looks for a file named {@code UTC-TAI.history} that must be in the IERS format.
     * </p>
     * 
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulc/UTC-TAI.history">IERS UTC-TAI.history file</a>
     * @see #getUTC()
     * @see #clearUTCTAILoaders()
     */
    public static void addDefaultUTCTAILoader() {
        addUTCTAILoader(new UTCTAIHistoryFilesLoader());
    }

    /**
     * Clear loaders for UTC-TAI offsets history files.
     * 
     * @see #getUTC()
     * @see #addUTCTAILoader(UTCTAILoader)
     * @see #addDefaultUTCTAILoader()
     */
    public static void clearUTCTAILoaders() {
        loaders.clear();
    }

    /**
     * Get the International Atomic Time scale.
     * 
     * @return International Atomic Time scale
     */
    public static TAIScale getTAI() {
        return TAILazyHolder.INSTANCE;
    }

    /**
     * Get the Universal Time Coordinate scale.
     * <p>
     * If no {@link UTCTAILoader} has been added by calling {@link #addUTCTAILoader(UTCTAILoader)
     * addUTCTAILoader} or if {@link #clearUTCTAILoaders() clearUTCTAILoaders} has been called afterwards, the
     * {@link #addDefaultUTCTAILoader() addDefaultUTCTAILoader} method will be called automatically.
     * </p>
     * 
     * @return Universal Time Coordinate scale
     * @exception PatriusException if some data can't be read or some file content is corrupted
     * @see #addUTCTAILoader(UTCTAILoader)
     * @see #clearUTCTAILoaders()
     * @see #addDefaultUTCTAILoader()
     */
    public static UTCScale getUTC() throws PatriusException {

        /** Use of Double-Check locking */
        if (utc == null) {
            synchronized (TimeScalesFactory.class) {
                if (utc == null) {
                    SortedMap<DateComponents, Integer> entries = new TreeMap<DateComponents, Integer>();
                    boolean loaded = false;
                    if (loaders.isEmpty()) {
                        // Add default loaders
                        addDefaultUTCTAILoader();
                    }
                    for (final UTCTAILoader loader : loaders) {
                        DataProvidersManager.getInstance().feed(loader.getSupportedNames(), loader);
                        if (!loader.stillAcceptsData()) {
                            // Add data
                            entries = loader.loadTimeSteps();
                            loaded = true;
                        }
                    }
                    if (!loaded) {
                        // Exception
                        throw new PatriusException(
                            PatriusMessages.NO_IERS_UTC_TAI_HISTORY_DATA_LOADED);
                    }
                    utc = new UTCScale(entries);
                }
            }
        }

        // Return result
        return utc;
    }

    /**
     * Get the Universal Time 1 scale.
     * <p>
     * UT1 scale depends on both UTC scale and Earth Orientation Parameters, so this method loads these data sets. See
     * the {@link #getUTC() TimeScalesFactory.getUTC()} and
     * {@link fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory#getEOP2000History()
     * EOPHistoryFactory.getEOP2000History()} methods for an explanation of how the corresponding data loaders can be
     * configured.
     * </p>
     * 
     * @return Universal Time 1 scale
     * @exception PatriusException if some data can't be read or some file content is corrupted
     * @see #getUTC()
     */
    public static UT1Scale getUT1() throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (ut1 == null) {
            synchronized (TimeScalesFactory.class) {
                if (ut1 == null) {
                    ut1 = new UT1Scale();
                }
            }
        }
        return ut1;
    }

    /**
     * Get the Terrestrial Time scale.
     * 
     * @return Terrestrial Time scale
     */
    public static TTScale getTT() {
        return TTLazyHolder.INSTANCE;
    }

    /**
     * Get the Galileo System Time scale.
     * 
     * @return Galileo System Time scale
     */
    public static GalileoScale getGST() {
        return GSTLazyHolder.INSTANCE;
    }

    /**
     * Get the Global Positioning System scale.
     * 
     * @return Global Positioning System scale
     */
    public static GPSScale getGPS() {
        return GPSLazyHolder.INSTANCE;
    }

    /**
     * Get the Geocentric Coordinate Time scale.
     * 
     * @return Geocentric Coordinate Time scale
     */
    public static TCGScale getTCG() {
        return TCGLazyHolder.INSTANCE;
    }

    /**
     * Get the Barycentric Dynamic Time scale.
     * 
     * @return Barycentric Dynamic Time scale
     */
    public static TDBScale getTDB() {
        return TDBLazyHolder.INSTANCE;
    }

    /**
     * Get the Barycentric Coordinate Time scale.
     * 
     * @return Barycentric Coordinate Time scale
     */
    public static TCBScale getTCB() {
        return TCBLazyHolder.INSTANCE;
    }

    /**
     * Get the Greenwich Mean Sidereal Time scale.
     * 
     * @return Greenwich Mean Sidereal Time scale
     * @exception PatriusException if some data can't be read or some file content is corrupted
     */
    public static GMSTScale getGMST() throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (gmst == null) {
            synchronized (TimeScalesFactory.class) {
                if (gmst == null) {
                    gmst = new GMSTScale(getUT1());
                }
            }
        }
        return gmst;
    }

    /**
     * Returns the {@link TimeScale} corresponding to the string representation of time scale.
     * For instance:
     * <ul>
     * <li>Calling get("UTC") will return {@link UTCScale} and is equivalent to calling
     * {@link TimeScalesFactory#getUTC()},</li>
     * <li>Calling get("TAI") will return {@link TAIScale} and is equivalent to calling
     * {@link TimeScalesFactory#getTAI()}.</li>
     * </ul>
     * @param timescale time scale as string
     * @return time scale as {@link TimeScale}
     * @throws PatriusException thrown if string matches no known time scale
     */
    public static TimeScale get(final String timescale) throws PatriusException {
        // Initialization
        TimeScale res = null;
        // Switch on all available time scales
        switch (timescale) {
            case "TAI":
                // TAI
                res = getTAI();
                break;
            case "UTC":
                // UTC
                res = getUTC();
                break;
            case "UT1":
                // UT1
                res = getUT1();
                break;
            case "TT":
                // TT
                res = getTT();
                break;
            case "GST":
                // FST
                res = getGST();
                break;
            case "GPS":
                // GPS
                res = getGPS();
                break;
            case "TCG":
                // TCG
                res = getTCG();
                break;
            case "TDB":
                // TDB
                res = getTDB();
                break;
            case "TCB":
                // TCB
                res = getTCB();
                break;
            case "GMST":
                // GMST
                res = getGMST();
                break;
            default:
                // Unknown time scale
                throw new PatriusException(PatriusMessages.UNKNOWN_TIMESCALE, timescale);
        }
        // Return result
        return res;
    }

    /**
     * Clear the time scales (UTC, UT1 and GMST).
     */
    @SuppressWarnings("PMD.NullAssignment")
    // Reason: code kept as such
    public static void clearTimeScales() {
        utc = null;
        ut1 = null;
        gmst = null;
    }

    
    /** Lasy holder for International Atomic Time scale. */
    private static class TAILazyHolder {
        /** International Atomic Time scale. */
        private static final TAIScale INSTANCE = new TAIScale();
    }

    /** Lasy holder for Terrestrial Time scale. */
    private static class TTLazyHolder {
        /** Terrestrial Time scale. */
        private static final TTScale INSTANCE = new TTScale();
    }

    /** Lasy holder for Galileo System Time scale. */
    private static class GSTLazyHolder {
        /** Galileo System Time scale. */
        private static final GalileoScale INSTANCE = new GalileoScale();
    }

    /** Lasy holder for Global Positioning System scale. */
    private static class GPSLazyHolder {
        /** Global Positioning System scale. */
        private static final GPSScale INSTANCE = new GPSScale();
    }

    /** Lasy holder for Geocentric Coordinate Time scale. */
    private static class TCGLazyHolder {
        /** Geocentric Coordinate Time scale. */
        private static final TCGScale INSTANCE = new TCGScale();
    }

    /** Lasy holder for Barycentric Dynamic Time scale. */
    private static class TDBLazyHolder {
        /** Barycentric Dynamic Time scale. */
        private static final TDBScale INSTANCE = new TDBScale();
    }

    /** Lasy holder for Barycentric Dynamic Time scale. */
    private static class TCBLazyHolder {
        /** Barycentric Dynamic Time scale. */
        private static final TCBScale INSTANCE = new TCBScale(getTDB());
    }
}
