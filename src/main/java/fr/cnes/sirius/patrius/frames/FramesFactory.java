/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:272:09/10/2014:added H0 - n frame
 * VERSION::FA:479:26/11/2015:Anomalie dans la classe FramesFactory lors de l’usage de multi thread
 * VERSION::DM:489:12/01/2016:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:590:05/04/2016:correction to freeze H0 - n frame
 * VERSION::DM:661:01/02/2017:add H0MinusNFrame class
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.CIRFProvider;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.EODProvider;
import fr.cnes.sirius.patrius.frames.transformations.GTODProvider;
import fr.cnes.sirius.patrius.frames.transformations.ITRFEquinoxProvider;
import fr.cnes.sirius.patrius.frames.transformations.ITRFProvider;
import fr.cnes.sirius.patrius.frames.transformations.InterpolatingTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.MODProvider;
import fr.cnes.sirius.patrius.frames.transformations.TEMEProvider;
import fr.cnes.sirius.patrius.frames.transformations.TIRFProvider;
import fr.cnes.sirius.patrius.frames.transformations.TODProvider;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.VEISProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PatriusConfiguration;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Factory for predefined reference frames.
 * 
 * <h5>FramesFactory Presentation</h5>
 * <p>
 * Several predefined reference frames are implemented in OREKIT. They are linked together in a tree with the
 * <i>Geocentric Celestial Reference Frame</i> (GCRF) as the root of the tree. The IERS frames require a
 * FramesConfiguration. If no configuration is specified by the user, a default one is used. The user can create a
 * configuration with the {@link fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder}, and pass it to
 * the {@link FramesFactory} with the {@link #setConfiguration(FramesConfiguration)} method.
 * </p>
 * <h5>Reference Frames</h5>
 * <p>
 * The user can retrieve those reference frames using various static methods (
 * {@link FramesFactory#getFrame(Predefined)}, {@link #getGCRF()}, {@link #getCIRF()}, {@link #getTIRF()},
 * {@link #getITRF()}, {@link #getEME2000()}, {@link #getMOD(boolean)}, {@link #getTOD(boolean)},
 * {@link #getGTOD(boolean)}, {@link #getITRFEquinox()}, {@link #getTEME()} and {@link #getVeis1950()}).
 * </p>
 * <h5>International Earth Rotation Service Frames</h5>
 * <p>
 * The frames defined by the IERS are available, and are described in the <a
 * href="ftp://tai.bipm.org/iers/conv2010/tn36.pdf">IERS conventions (2010)</a>. The are fully configurable. Using the
 * {@link fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder}, one can specify all models pertaining
 * to the transformations between the IERS frames.
 * </p>
 * <p >
 * This frame is used to define position on solid Earth. It rotates with the Earth and includes the pole motion with
 * respect to Earth crust as provided by the {@link FramesConfiguration frames
 * configuration}. Its pole axis is the IERS Reference Pole (IRP).
 * </p>
 * <h5>Classical paradigm: equinox-based transformations</h5>
 * <p>
 * The classical paradigm used prior to IERS conventions 2003 is equinox based and uses more intermediate frames. Only
 * some of these frames are supported in Orekit.
 * </p>
 * <p>
 * Here is a schematic representation of the predefined reference frames tree:
 * </p>
 * 
 * <pre>
 *                                                     GCRF
 *                                                       │
 *                                              ┌────────┴────┬────────────────────┐
 *                                              │             │     Frame bias     │
 *                                              │             │                 EME2000
 *                                              │             │                    │
 *                                              │             │ Precession effects │
 *        Bias, Precession and Nutation effects │             │                    │
 *          with or w/o EOP nutation correction │            MOD                  MOD  (Mean equator Of Date)
 *                                              │             │             w/o EOP corrections
 *                                              │     ┌───────┤  Nutation effects  ├───────────────────────────┐
 *    (Celestial Intermediate Reference Frame) CIRF   │       │                    │                           │
 *                                              │     │      TOD                  TOD  (True equator Of Date)  │
 *                       Earth natural rotation │     │       │             w/o EOP corrections                │
 *                                              │     │       │    Sidereal Time   │                           │
 *                                              │     │       │                    │                           │
 *  (Terrestrial Intermediate Reference Frame) TIRF  EOD     GTOD                 GTOD  (Green. True Of Date) EOD
 *                                              │                           w/o EOP corrections
 *                                  Pole motion │                                  │
 *                                              │                                  ├────────────┐
 *                                              │                                  │            │
 * (International Terrestrial Reference Frame) ITRF                               ITRF        VEIS1950
 *                                                                           equinox-based
 * 
 * </pre>
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * 
 * @author Guylaine Prat
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public final class FramesFactory implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 1720647682459923909L;
    
    /** 24. */
    private static final int TWENTY_FOUR = 24;
    /** 8. */
    private static final int EIGHT = 8;

    /** Predefined frames. */
    private static transient Map<Predefined, FactoryManagedFrame> frames =
        new ConcurrentHashMap<Predefined, FactoryManagedFrame>();

    /** Frames configuration. */
    private static final AtomicReference<FramesConfiguration> CONFIG_REF =
        new AtomicReference<FramesConfiguration>();

    /**
     * Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a default constructor. This private
     * constructor prevents the compiler from generating one automatically.
     * </p>
     */
    private FramesFactory() {
    }

    /**
     * Get one of the predefined frames.
     * 
     * @param factoryKey key of the frame within the factory
     * @return the predefined frame
     * @exception PatriusException if frame cannot be built due to missing data
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public static Frame getFrame(final Predefined factoryKey) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialisation
        final Frame res;

        switch (factoryKey) {
            case GCRF:
                // Case GCRF
                res = getGCRF();
                break;
            case ICRF:
                // Case ICRF
                res = getICRF();
                break;
            case EME2000:
                // Case EME2000
                res = getEME2000();
                break;
            case ITRF:
                // Case ITRF
                res = getITRF();
                break;
            case ITRF_EQUINOX:
                // Case ITRF Equinox
                res = getITRFEquinox();
                break;
            case TIRF:
                // Case TIRF
                res = getTIRF();
                break;
            case CIRF:
                // Case CIRF
                res = getCIRF();
                break;
            case VEIS_1950:
                // Case Veis 1950
                res = getVeis1950();
                break;
            case GTOD_WITHOUT_EOP_CORRECTIONS:
                // Case GTOD without EOP corrections
                res = getGTOD(false);
                break;
            case GTOD_WITH_EOP_CORRECTIONS:
                // Case GTOD with EOP corrections
                res = getGTOD(true);
                break;
            case TOD_WITHOUT_EOP_CORRECTIONS:
                // Case TOD without EOP corrections
                res = getTOD(false);
                break;
            case TOD_WITH_EOP_CORRECTIONS:
                // Case TOD with EOP corrections
                res = getTOD(true);
                break;
            case MOD_WITHOUT_EOP_CORRECTIONS:
                // Case MOD without EOP corrections
                res = getMOD(false);
                break;
            case MOD_WITH_EOP_CORRECTIONS:
                // Case MOD with EOP corrections
                res = getMOD(true);
                break;
            case TEME:
                // Case TEME
                res = getTEME();
                break;
            case EOD_WITH_EOP_CORRECTIONS:
                // Case EOD with EOP corrections
                res = getEODFrame(true);
                break;
            case EOD_WITHOUT_EOP_CORRECTIONS:
                // EOD without EOP corrections
                res = getEODFrame(false);
                break;
            default:
                // Cannot happen
                throw PatriusException.createInternalError(null);
        }

        // Return frame
        //
        return res;
    }

    /**
     * Get the unique GCRF frame.
     * <p>
     * The GCRF frame is the root frame in the frame tree.
     * </p>
     * 
     * @return the unique instance of the GCRF frame
     */
    public static Frame getGCRF() {
        return Frame.getRoot();
    }

    /**
     * Get the unique ICRF frame.
     * <p>
     * The ICRF frame is centered at solar system barycenter and aligned with GCRF.
     * </p>
     * 
     * @return the unique instance of the ICRF frame
     * @exception PatriusException if solar system ephemerides cannot be loaded
     */
    public static Frame getICRF() throws PatriusException {
        return CelestialBodyFactory.getSolarSystemBarycenter().getInertiallyOrientedFrame();
    }

    /**
     * Get the CIRF reference frame.
     * 
     * @return the selected reference frame singleton.
     */
    public static FactoryManagedFrame getCIRF() {

        // try to find an already built frame
        final Predefined factoryKey = Predefined.CIRF;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            frame = new FactoryManagedFrame(getGCRF(), new CIRFProvider(), true, factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the TIRF reference frame.
     * 
     * @return the selected reference frame singleton.
     * @exception PatriusException if the precession-nutation model data embedded in the library
     *            cannot be read.
     */
    public static FactoryManagedFrame getTIRF() throws PatriusException {
        // try to find an already built frame
        final Predefined factoryKey = Predefined.TIRF;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getCIRF(), new TIRFProvider(), false, factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;
    }

    /**
     * Get the ITRF reference frame.
     * 
     * @return the selected reference frame singleton.
     * @exception PatriusException if the precession-nutation model data embedded in the library
     *            cannot be read.
     */
    public static FactoryManagedFrame getITRF() throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = Predefined.ITRF;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            final Frame tirfFrame = getTIRF();
            frame = new FactoryManagedFrame(tirfFrame, new ITRFProvider(), false, factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the unique EME2000 frame.
     * <p>
     * The EME2000 frame is also called the J2000 frame. The former denomination is preferred in Orekit.
     * </p>
     * 
     * @return the unique instance of the EME2000 frame
     */
    public static FactoryManagedFrame getEME2000() {

        // try to find an already built frame
        FactoryManagedFrame frame = frames.get(Predefined.EME2000);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getGCRF(), new EME2000Provider(), true,
                Predefined.EME2000);
            frames.put(Predefined.EME2000, frame);
        }

        return frame;

    }

    /**
     * Get the VEIS 1950 reference frame.
     * <p>
     * Its parent frame is the GTOD frame without EOP corrections.
     * <p>
     * 
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getVeis1950() throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = Predefined.VEIS_1950;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(FramesFactory.getGTOD(false), new VEISProvider(), true,
                factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the equinox-based ITRF reference frame.
     * 
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getITRFEquinox() throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = Predefined.ITRF_EQUINOX;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getGTOD(true), new ITRFEquinoxProvider(), false,
                factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the GTOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for consistency with legacy software that
     * don't handle EOP correction parameters. Beware that setting this parameter to {@code false} leads to crude
     * accuracy (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * 
     * @param applyEOPCorr if true, EOP corrections are applied (here, lod)
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getGTOD(final boolean applyEOPCorr) throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = applyEOPCorr ? Predefined.GTOD_WITH_EOP_CORRECTIONS
            : Predefined.GTOD_WITHOUT_EOP_CORRECTIONS;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getTOD(applyEOPCorr), new GTODProvider(), false,
                factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the TOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for consistency with legacy software that
     * don't handle EOP correction parameters. Beware that setting this parameter to {@code false} leads to crude
     * accuracy (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * 
     * @param applyEOPCorr if true, EOP corrections are applied (here, nutation)
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTOD(final boolean applyEOPCorr) throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey;
        final int interpolationPoints;
        final int pointsPerDay;
        if (applyEOPCorr) {
            // Apply EOP correction
            factoryKey = Predefined.TOD_WITH_EOP_CORRECTIONS;
            interpolationPoints = 6;
            pointsPerDay = TWENTY_FOUR;
        } else {
            // Do not apply EOP correction
            factoryKey = Predefined.TOD_WITHOUT_EOP_CORRECTIONS;
            interpolationPoints = 6;
            pointsPerDay = EIGHT;
        }
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            final TransformProvider interpolating = new InterpolatingTransformProvider(
                new TODProvider(applyEOPCorr), true, false, AbsoluteDate.PAST_INFINITY,
                AbsoluteDate.FUTURE_INFINITY, interpolationPoints, Constants.JULIAN_DAY
                    / pointsPerDay, PatriusConfiguration.getCacheSlotsNumber(),
                Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);
            frame = new FactoryManagedFrame(getMOD(applyEOPCorr), interpolating, true, factoryKey);
            frames.put(factoryKey, frame);
        }

        // Return frame
        return frame;

    }

    /**
     * Get the MOD reference frame.
     * <p>
     * The applyEOPCorr parameter is available mainly for testing purposes or for consistency with legacy software that
     * don't handle EOP correction parameters. Beware that setting this parameter to {@code false} leads to crude
     * accuracy (order of magnitudes for errors might be above 1m in LEO and 10m in GEO).
     * </p>
     * 
     * @param applyEOPCorr if true, EOP corrections are applied (EME2000/GCRF bias compensation)
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getMOD(final boolean applyEOPCorr) throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = applyEOPCorr ? Predefined.MOD_WITH_EOP_CORRECTIONS
            : Predefined.MOD_WITHOUT_EOP_CORRECTIONS;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(applyEOPCorr ? FramesFactory.getGCRF()
                : FramesFactory.getEME2000(), new MODProvider(), true, factoryKey);
            frames.put(factoryKey, frame);
        }

        return frame;

    }

    /**
     * Get the TEME reference frame.
     * <p>
     * The TEME frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em> official definition and
     * there are some ambiguities about whether it should be used as "of date" or "of epoch". This frame should
     * therefore be used <em>only</em> for TLE propagation and not for anything else, as recommended by the CCSDS Orbit
     * Data Message blue book.
     * </p>
     * 
     * @return the selected reference frame singleton.
     * @exception PatriusException if data embedded in the library cannot be read
     */
    public static FactoryManagedFrame getTEME() throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = Predefined.TEME;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getTOD(false), new TEMEProvider(), true, factoryKey);
            frames.put(factoryKey, frame);
        }
        return frame;

    }

    /**
     * <p>
     * This class implements the EOD frame (mean ecliptic and equinox of the epoch).
     * </p>
     * See "Astronomical Algorithms", chapter 24 "Solar Coordinates", Jean Meeus, 1991.
     * 
     * @param applyEOPCorr true to take into account EOP corrections
     * @return the EOD frame
     * @throws PatriusException if data embedded in the library cannot be read
     * */
    public static FactoryManagedFrame getEODFrame(final boolean applyEOPCorr)
                                                                             throws PatriusException {

        // try to find an already built frame
        final Predefined factoryKey = applyEOPCorr ? Predefined.EOD_WITH_EOP_CORRECTIONS
            : Predefined.EOD_WITHOUT_EOP_CORRECTIONS;
        FactoryManagedFrame frame = frames.get(factoryKey);

        if (frame == null) {
            // it's the first time we need this frame, build it and store it
            frame = new FactoryManagedFrame(getMOD(applyEOPCorr), new EODProvider(), true,
                factoryKey);
            frames.put(factoryKey, frame);
        }
        return frame;

    }

    /**
     * Get the "H0 - n" reference frame. The "H0 - n" frame is a pseudo-inertial frame, built from
     * the GCRF-ITRF transformation at the date H0 - n; this transformation is "frozen" in time, and
     * it is combined to a rotation of an angle "longitude" around the Z axis of the ITRF frame.
     * 
     * @param name name of the frame.
     * @param h0 the H0 date.
     * @param n the offset for the date (date = H0 - n).
     * @param longitude the rotation angle around the ITRF Z axis (rad).
     * @return the selected reference frame.
     * @throws PatriusException when the ITRF-GCRF transformation cannot be computed
     */
    public static Frame getH0MinusN(final String name, final AbsoluteDate h0, final double n,
                                    final double longitude) throws PatriusException {
        return new H0MinusNFrame(name, h0, n, longitude);
    }

    /**
     * Get the "H0 - n" reference frame. The "H0 - n" frame is a pseudo-inertial frame, built from
     * the GCRF-ITRF transformation at the date H0 - n; this transformation is "frozen" in time, and
     * it is combined to a rotation of an angle "longitude" around the Z axis of the ITRF frame.
     * 
     * @param name name of the frame.
     * @param h0MinusN the H0 - n date.
     * @param longitude the rotation angle around the ITRF Z axis (rad).
     * @return the selected reference frame.
     * @throws PatriusException when the ITRF-GCRF transformation cannot be computed
     */
    public static Frame getH0MinusN(final String name, final AbsoluteDate h0MinusN,
                                    final double longitude) throws PatriusException {
        // In that case, n is not given independently, it is set to 0
        return new H0MinusNFrame(name, h0MinusN, 0., longitude);
    }

    /**
     * Sets a new configuration. Replaces the current instance of the configuration by the provided
     * parameter.
     * 
     * @param newCfg the new configuration.
     */
    public static void setConfiguration(final FramesConfiguration newCfg) {
        CONFIG_REF.set(newCfg);
    }

    /**
     * Sets the default configuration as the current configuration.
     */
    private static void initConfiguration() {
        setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
    }

    /**
     * Getter for the current configuration.
     * 
     * @return configuration the current configuration
     */
    public static FramesConfiguration getConfiguration() {
        if (CONFIG_REF.get() == null) {
            // Builds a default configuration
            initConfiguration();
        }
        return CONFIG_REF.get();
    }

    /**
     * Clear the frames tree.
     */
    public static void clear() {
        frames.clear();
    }
}
