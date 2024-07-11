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
 *
 * @history creation 12/12/2012
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3143:10/05/2022:[PATRIUS] Nouvelle interface OrbitEventDetector et nouvelles classes
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2244:27/05/2020:Evolution de la prise en compte des fichiers EOP IERS
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:127:30/08/2013:Correct EOPC04 regular expression for IAU 1980 files
 * VERSION::FA:127:16/12/2013:Corrected bug in regular expression
 * VERSION::DM:303:02/04/2015: addition of constant outside history EOP
 * VERSION::FA:517:28/01/2016:Correct EOPC04 regular expression for IAU 2000 files
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Factory for EOPHistory.
 * O
 * <p>
 * By default, some loaders are added in the following order:<br/>
 * <ul>
 * <li>{@link RapidDataAndPredictionColumnsLoader}: IERS rapid data and prediction files in columns format 
 * (finals file). See <a href="http://maia.usno.navy.mil/ser7/readme.finals2000A">IERS rapid data and prediction files
 * </a> for more information.
 * </li>
 * <li>{@link RapidDataAndPredictionXMLLoader}: IERS rapid data and prediction files in XML format 
 * (finals file). See <a href="http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html">
 * IERS rapid data and prediction files</a> for more information.
 * </li>
 * <li>{@link EOPC04FilesLoader}: IERS EOP 05 C04 files.
 * See <a href="http://hpiers.obspm.fr/iers/eop/eopc04_05/">EOP 05 C04</a>
 * for more information.
 * </li>
 * <li>{@link BulletinBFilesLoader}: IERS bulletin B files. See <a
 * href="http://www.iers.org/IERS/EN/DataProducts/EarthOrientationData/eop.html">Bulletin B</a>
 * for more information.
 * </li>
 * </ul>
 * There is no included data in PATRIUS by default. <br/>
 * Once loaded, data are stored in static variables and are used to retrieve EOP data (used in frame conversions) 
 * through various static method.
 * </p>
 *
 * @author Rami Houdroge
 * 
 * @since 1.3
 * 
 * @version $Id: EOPHistoryFactory.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class EOPHistoryFactory {

    /**
     * Default regular expression for the Rapid Data and Prediction EOP columns files (IAU1980
     * compatibles).
     */
    public static final String RAPID_DATA_PREDICITON_COLUMNS_1980_FILENAME = "^finals\\.[^.]*$";
    /**
     * Default regular expression for the Rapid Data and Prediction EOP XML files (IAU1980
     * compatibles).
     */
    public static final String RAPID_DATA_PREDICITON_XML_1980_FILENAME = "^finals\\..*\\.xml$";
    /** Default regular expression for the EOPC04 files (IAU1980 compatibles). */
    public static final String EOPC04_1980_FILENAME = "^eopc04(.)+";
    /** Default regular expression for the BulletinB files (IAU1980 compatibles). */
    public static final String BULLETINB_1980_FILENAME = "^bulletinb((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";
    /**
     * Default regular expression for the Rapid Data and Prediction EOP columns files (IAU2000
     * compatibles).
     */
    public static final String RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME = "^finals2000A\\.[^.]*$";
    /**
     * Default regular expression for the Rapid Data and Prediction EOP XML files (IAU2000
     * compatibles).
     */
    public static final String RAPID_DATA_PREDICITON_XML_2000_FILENAME = "^finals2000A\\..*\\.xml$";
    /** Default regular expression for the EOPC04 files (IAU2000 compatibles). */
    public static final String EOPC04_2000_FILENAME = EOPC04_1980_FILENAME;
    /** Default regular expression for the BulletinB files (IAU2000 compatibles). */
    public static final String BULLETINB_2000_FILENAME = "^bulletinb_IAU2000((-\\d\\d\\d\\.txt)|(\\.\\d\\d\\d))$";

    /** EOP 1980 loaders. */
    private static final List<EOP1980HistoryLoader> EOP_1980_LOADERS = new ArrayList<EOP1980HistoryLoader>();
    /** EOP 2000 loaders. */
    private static final List<EOP2000HistoryLoader> EOP_2000_LOADERS = new ArrayList<EOP2000HistoryLoader>();

    /**
     * Private default constructor.
     */
    private EOPHistoryFactory() {
    }

    /**
     * Add a loader for EOP 1980 history.
     * 
     * @param loader custom loader to add for the EOP history
     * @see EOPHistoryFactory#addDefaultEOP1980HistoryLoaders(String, String, String, String)
     * @see EOPHistoryFactory#clearEOP1980HistoryLoaders()
     * @see EOPHistoryFactory#addEOP2000HistoryLoader(EOP2000HistoryLoader)
     */
    public static void addEOP1980HistoryLoader(final EOP1980HistoryLoader loader) {
        synchronized (EOP_1980_LOADERS) {
            EOP_1980_LOADERS.add(loader);
        }
    }

    /**
     * Add the default loaders for EOP 1980 history.
     * <p>
     * The default loaders look for IERS EOP 05 C04 and bulletins B files.
     * </p>
     * 
     * @param rapidDataColumnsSupportedNames regular expression for supported rapid data columns EOP
     *        files names (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported rapid data XML EOP files
     *        names (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP05 C04 files names (may be
     *        null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names (may
     *        be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04_05/">IERS EOP 05 C04 files</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulb/">IERS bulletins B</a>
     * @see EOPHistoryFactory#addEOP1980HistoryLoader(EOP1980HistoryLoader)
     * @see EOPHistoryFactory#clearEOP1980HistoryLoaders()
     * @see EOPHistoryFactory#addDefaultEOP2000HistoryLoaders(String, String, String, String)
     */
    public static void addDefaultEOP1980HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
        final String rapidColNames = (rapidDataColumnsSupportedNames == null) ?
            RAPID_DATA_PREDICITON_COLUMNS_1980_FILENAME
            : rapidDataColumnsSupportedNames;
        addEOP1980HistoryLoader(new RapidDataAndPredictionColumnsLoader(rapidColNames));
        final String rapidXmlNames = (rapidDataXMLSupportedNames == null) ? RAPID_DATA_PREDICITON_XML_1980_FILENAME
            : rapidDataXMLSupportedNames;
        addEOP1980HistoryLoader(new RapidDataAndPredictionXMLLoader(rapidXmlNames));
        final String eopcNames = (eopC04SupportedNames == null) ? EOPC04_1980_FILENAME
            : eopC04SupportedNames;
        addEOP1980HistoryLoader(new EOPC04FilesLoader(eopcNames));
        final String bulBNames = (bulletinBSupportedNames == null) ? BULLETINB_1980_FILENAME
            : bulletinBSupportedNames;
        addEOP1980HistoryLoader(new BulletinBFilesLoader(bulBNames));
    }

    /**
     * Clear loaders for EOP 1980 history.
     * 
     * @see EOPHistoryFactory#addEOP1980HistoryLoader
     * @see EOPHistoryFactory#addDefaultEOP1980HistoryLoaders
     * @see EOPHistoryFactory#clearEOP2000HistoryLoaders()
     */
    public static void clearEOP1980HistoryLoaders() {
        synchronized (EOP_1980_LOADERS) {
            EOP_1980_LOADERS.clear();
        }
    }

    /**
     * Get Earth Orientation Parameters history (IAU1980) data.
     * <p>
     * If no {@link EOP1980HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP1980HistoryLoader
     * addEOP1980HistoryLoader} or if {@link EOPHistoryFactory#clearEOP1980HistoryLoaders clearEOP1980HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP1980HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @return Earth Orientation Parameters history (IAU1980) data
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP1980History getEOP1980History() throws PatriusException {
        return EOPHistoryFactory.getEOP1980History(EOPInterpolators.LAGRANGE4);
    }

    /**
     * Get Earth Orientation Parameters history (IAU1980) data.
     * <p>
     * If no {@link EOP1980HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP1980HistoryLoader
     * addEOP1980HistoryLoader} or if {@link EOPHistoryFactory#clearEOP1980HistoryLoaders clearEOP1980HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP1980HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @param interpMethod : interpolation method for EOP data
     * @return Earth Orientation Parameters history (IAU1980) data
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP1980History getEOP1980History(final EOPInterpolators interpMethod)
        throws PatriusException {
        synchronized (EOP_1980_LOADERS) {
            final EOP1980History history = new EOP1980History(interpMethod);
            if (EOP_1980_LOADERS.isEmpty()) {
                addDefaultEOP1980HistoryLoaders(null, null, null, null);
            }
            for (final EOP1980HistoryLoader loader : EOP_1980_LOADERS) {
                loader.fillHistory(history);
            }
            history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
            return history;
        }
    }

    /**
     * Add a loader for EOP 2000 history.
     * 
     * @param loader custom loader to add for the EOP history
     * @see EOPHistoryFactory#addDefaultEOP2000HistoryLoaders(String, String, String, String)
     * @see EOPHistoryFactory#clearEOP2000HistoryLoaders()
     * @see EOPHistoryFactory#addEOP1980HistoryLoader
     */
    public static void addEOP2000HistoryLoader(final EOP2000HistoryLoader loader) {
        synchronized (EOP_2000_LOADERS) {
            EOP_2000_LOADERS.add(loader);
        }
    }

    /**
     * Add the default loaders for EOP 2000 history.
     * <p>
     * The default loaders look for IERS EOP 05 C04 and bulletins B files.
     * </p>
     * 
     * @param rapidDataColumnsSupportedNames regular expression for supported rapid data columns EOP
     *        files names (may be null if the default IERS file names are used)
     * @param rapidDataXMLSupportedNames regular expression for supported rapid data XML EOP files
     *        names (may be null if the default IERS file names are used)
     * @param eopC04SupportedNames regular expression for supported EOP05 C04 files names (may be
     *        null if the default IERS file names are used)
     * @param bulletinBSupportedNames regular expression for supported bulletin B files names (may
     *        be null if the default IERS file names are used)
     * @see <a href="http://hpiers.obspm.fr/eoppc/eop/eopc04_05/">IERS EOP 05 C04 files</a>
     * @see <a href="http://hpiers.obspm.fr/eoppc/bul/bulb/">IERS bulletins B</a>
     * @see EOPHistoryFactory#addEOP2000HistoryLoader
     * @see EOPHistoryFactory#clearEOP2000HistoryLoaders()
     * @see EOPHistoryFactory#addDefaultEOP1980HistoryLoaders
     */
    public static void addDefaultEOP2000HistoryLoaders(final String rapidDataColumnsSupportedNames,
                                                       final String rapidDataXMLSupportedNames,
                                                       final String eopC04SupportedNames,
                                                       final String bulletinBSupportedNames) {
        final String rapidColNames = (rapidDataColumnsSupportedNames == null) ?
            RAPID_DATA_PREDICITON_COLUMNS_2000_FILENAME
            : rapidDataColumnsSupportedNames;
        addEOP2000HistoryLoader(new RapidDataAndPredictionColumnsLoader(rapidColNames));
        final String rapidXmlNames = (rapidDataXMLSupportedNames == null) ? RAPID_DATA_PREDICITON_XML_2000_FILENAME
            : rapidDataXMLSupportedNames;
        addEOP2000HistoryLoader(new RapidDataAndPredictionXMLLoader(rapidXmlNames));
        final String eopcNames = (eopC04SupportedNames == null) ? EOPC04_2000_FILENAME
            : eopC04SupportedNames;
        addEOP2000HistoryLoader(new EOPC04FilesLoader(eopcNames));
        final String bulBNames = (bulletinBSupportedNames == null) ? BULLETINB_2000_FILENAME
            : bulletinBSupportedNames;
        addEOP2000HistoryLoader(new BulletinBFilesLoader(bulBNames));
    }

    /**
     * Clear loaders for EOP 2000 history.
     * 
     * @see EOPHistoryFactory#addEOP2000HistoryLoader
     * @see EOPHistoryFactory#addDefaultEOP2000HistoryLoaders
     * @see EOPHistoryFactory#clearEOP1980HistoryLoaders
     */
    public static void clearEOP2000HistoryLoaders() {
        synchronized (EOP_2000_LOADERS) {
            EOP_2000_LOADERS.clear();
        }
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data.
     * <p>
     * If no {@link EOP2000HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP2000HistoryLoader
     * addEOP2000HistoryLoader} or if {@link EOPHistoryFactory#clearEOP2000HistoryLoaders clearEOP2000HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP2000HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @return Earth Orientation Parameters history (IAU2000) data
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000History getEOP2000History() throws PatriusException {
        return EOPHistoryFactory.getEOP2000History(EOPInterpolators.LAGRANGE4);
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data.
     * <p>
     * If no {@link EOP2000HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP2000HistoryLoader
     * addEOP2000HistoryLoader} or if {@link EOPHistoryFactory#clearEOP2000HistoryLoaders clearEOP2000HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP2000HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @param interpMethod : interpolation method for EOP data
     * @return Earth Orientation Parameters history (IAU2000) data
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000History getEOP2000History(final EOPInterpolators interpMethod)
        throws PatriusException {
        synchronized (EOP_2000_LOADERS) {
            final EOP2000History history = new EOP2000History(interpMethod);
            if (EOP_2000_LOADERS.isEmpty()) {
                addDefaultEOP2000HistoryLoaders(null, null, null, null);
            }
            for (final EOP2000HistoryLoader loader : EOP_2000_LOADERS) {
                loader.fillHistory(history);
            }
            history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
            return history;
        }
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data using a specific loader.
     * 
     * @param interpMethod : interpolation method for EOP data
     * @param loader : EOP data loader to feed
     * @return Earth Orientation Parameters history (IAU2000) data
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000History getEOP2000History(final EOPInterpolators interpMethod,
                                                   final EOP2000HistoryLoader loader) throws PatriusException {
        final EOP2000History history = new EOP2000History(interpMethod);
        loader.fillHistory(history);
        return history;
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data.
     * <p>
     * If no {@link EOP2000HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP2000HistoryLoader
     * addEOP2000HistoryLoader} or if {@link EOPHistoryFactory#clearEOP2000HistoryLoaders clearEOP2000HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP2000HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @return Earth Orientation Parameters history (IAU2000) data. Data are extended outside the
     *         history interval: the extended values are those of the interval's bounds.
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000HistoryConstantOutsideInterval getEOP2000HistoryConstant()
        throws PatriusException {
        return EOPHistoryFactory.getEOP2000HistoryConstant(EOPInterpolators.LAGRANGE4);
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data.
     * <p>
     * If no {@link EOP2000HistoryLoader} has been added by calling {@link EOPHistoryFactory#addEOP2000HistoryLoader
     * addEOP2000HistoryLoader} or if {@link EOPHistoryFactory#clearEOP2000HistoryLoaders clearEOP2000HistoryLoaders}
     * has been called afterwards, the {@link EOPHistoryFactory#addDefaultEOP2000HistoryLoaders} method will be called
     * automatically with two null parameters (supported file names).
     * </p>
     * 
     * @param interpMethod : interpolation method for EOP data
     * @return Earth Orientation Parameters history (IAU2000) data. Data are extended outside the
     *         history interval: the extended values are those of the interval's bounds.
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000HistoryConstantOutsideInterval
        getEOP2000HistoryConstant(
                                  final EOPInterpolators interpMethod) throws PatriusException {
        synchronized (EOP_2000_LOADERS) {
            final EOP2000HistoryConstantOutsideInterval history = new EOP2000HistoryConstantOutsideInterval(
                interpMethod);
            if (EOP_2000_LOADERS.isEmpty()) {
                addDefaultEOP2000HistoryLoaders(null, null, null, null);
            }
            for (final EOP2000HistoryLoader loader : EOP_2000_LOADERS) {
                loader.fillHistory(history);
            }
            history.checkEOPContinuity(5 * Constants.JULIAN_DAY);
            return history;
        }
    }

    /**
     * Get Earth Orientation Parameters history (IAU2000) data using a specific loader.
     * 
     * @param interpMethod : interpolation method for EOP data
     * @param loader : EOP data loader to feed
     * @return Earth Orientation Parameters history (IAU2000) data. Data are extended outside the
     *         history interval: the extended values are those of the interval's bounds.
     * @exception PatriusException if the data cannot be loaded
     */
    public static EOP2000HistoryConstantOutsideInterval
        getEOP2000HistoryConstant(
                                  final EOPInterpolators interpMethod, final EOP2000HistoryLoader loader)
            throws PatriusException {
        final EOP2000HistoryConstantOutsideInterval history = new EOP2000HistoryConstantOutsideInterval(
            interpMethod);
        loader.fillHistory(history);
        return history;
    }
}
