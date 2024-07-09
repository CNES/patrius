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
* VERSION:4.8:FA:FA-2998:15/11/2021:[PATRIUS] Discontinuite methode computeBearing de la classe ProjectionEllipsoidUtils
* VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
* VERSION:4.4:FA:FA-2137:04/10/2019:FA mineure Patrius V4.3
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur
* VERSION:4.3:DM:DM-2102:15/05/2019:[Patrius] Refactoring du paquet fr.cnes.sirius.patrius.bodies
* VERSION:4.3:DM:DM-2090:15/05/2019:[PATRIUS] ajout de fonctionnalites aux bibliotheques mathematiques
* VERSION:4.3:DM:DM-2091:15/05/2019:[PATRIUS] optimisation du SpacecraftState
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:86:24/10/2013:New constructors
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:364:21/11/2014:Modified contructor from state vector in order to allow null attitude provider
 * VERSION::FA:386:05/12/2014: index mutualisation for ephemeris interpolation
 * VERSION::FA:390:19/02/2015: added addAttitude method for AbstractEphemeris needs
 * VERSION::DM:290:04/03/2015: added toTransform methods
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:403:20/10/2015:Improving ergonomic
 * VERSION::FA:414:24/03/2015: proper handling of mass evolution
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::FA:449:18/12/2015:Changes in attitude handling
 * VERSION::FA:561:25/02/2016:Wiki and Javadoc corrections
 * VERSION::DM:654:04/08/2016:Add getAttitude(Frame) and getAttitude(LofType)
 * VERSION::FA:675:01/09/2016:corrected anomalies reducing the performances
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.IParamDiffFunction;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeInterpolable;
import fr.cnes.sirius.patrius.time.TimeShiftable;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * This class is the representation of a complete state holding orbit, attitude for forces and events computation and
 * additional states informations at a given date.
 * 
 * <p>
 * It contains an {@link Orbit} , plus two attitudes (for forces and events computation) and a map of additional states
 * at a current {@link AbsoluteDate}. Orbit and attitudes are guaranteed to be consistent in terms of date and reference
 * frame. The attitudes objects can be copied in the map of additional states with the additional state name
 * "ATTITUDE_FORCES", "ATTITUDE_EVENTS" or "ATTITUDE" (in numerical propagation only). The stored Attitude and the
 * additional state representing the attitude are guaranteed to be equal.
 * </p>
 * <p>
 * The user can declare a SpacecraftState with an input {@link MassProvider}. The masses from each part of the mass
 * model are automatically added to the map of additional states with a name in the form "MASS_<part name>". It is not
 * possible to get back the MassProvider.
 * </p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on a simple keplerian model for orbit, a linear
 * extrapolation for both attitudes taking the spin rate into account, no additional states changes. It is <em>not</em>
 * intended as a replacement for proper orbit and attitude propagation but should be sufficient for either small time
 * shifts or coarse accuracy.
 * </p>
 * <p>
 * The instance <code>SpacecraftState</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 */
@SuppressWarnings("PMD.NullAssignment")
public class SpacecraftState implements TimeStamped, TimeShiftable<SpacecraftState>, TimeInterpolable<SpacecraftState>,
    Serializable {

    /** Default prefix for additional state representing a mass. */
    public static final String MASS = "MASS_";

    /** First index of the additional states. */
    public static final int ORBIT_DIMENSION = 6;

    /** Serializable UID. */
    private static final long serialVersionUID = 3141803003950085500L;

    /** Orbital state. */
    private final Orbit orbit;

    /** Attitude for forces computation (attitude by default). */
    private Attitude attitude;

    /** Attitude for events computation. */
    private Attitude attitudeEvents;

    /** Attitude provider for forces computation. */
    private final AttitudeProvider attitudeProvider;

    /** Attitude provider for events computation. */
    private final AttitudeProvider attitudeProviderEvents;

    /** Additional states. */
    private final Map<String, double[]> additionalStates;

    /**
     * Build a spacecraft state from orbit only.
     * <p>
     * Attitude (for forces and events computation) are set to null values. No mass informations are added.
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     */
    public SpacecraftState(final Orbit orbitIn) {
        this.orbit = orbitIn;
        this.attitude = null;
        this.attitudeEvents = null;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
    }

    /**
     * Build a spacecraft state from orbit and a single attitude.
     * <p>
     * Attitude for forces computation is set to the input attitude. Attitude for events computation is set to null
     * value. No mass informations are added.
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attitudeIn
     *        the default attitude
     * @exception IllegalArgumentException
     *            if orbit and attitude dates or frames are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attitudeIn) {
        checkConsistency(orbitIn, attitudeIn, null);
        this.orbit = orbitIn;
        this.attitude = attitudeIn;
        this.attitudeEvents = null;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
    }

    /**
     * Create a new instance from orbit and mass provider.
     * <p>
     * Attitude (for forces and events computation) are set to null values.
     * </p>
     * <p>
     * The mass states informations contained in the mass provider are added to the additional states map with the name
     * in the form "MASS_<part name>".
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param massProvider
     *        the mass provider
     */
    public SpacecraftState(final Orbit orbitIn, final MassProvider massProvider) {
        this.orbit = orbitIn;
        this.attitude = null;
        this.attitudeEvents = null;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
        this.addMassProviderToAdditionalStates(massProvider);
    }

    /**
     * Build a spacecraft state from orbit, attitude and mass provider.
     * <p>
     * Attitude for forces computation is set to the input attitude. Attitude for events computation is set to null
     * value.
     * </p>
     * <p>
     * The mass states informations contained in the mass provider are added to the additional states list with the name
     * in the form "MASS_<part name>".
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attitudeIn
     *        the default attitude
     * @param massProvider
     *        the mass provider
     * @exception IllegalArgumentException
     *            if orbit and attitude dates or frames are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attitudeIn, final MassProvider massProvider) {
        checkConsistency(orbitIn, attitudeIn, null);
        this.orbit = orbitIn;
        this.attitude = attitudeIn;
        this.attitudeEvents = null;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
        this.addMassProviderToAdditionalStates(massProvider);
    }

    /**
     * Build a spacecraft state from orbit, attitude for forces and events computation.
     * <p>
     * No mass informations are added.
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attForces
     *        the attitude for forces computation
     * @param attEvents
     *        the attitude for events computation
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     *            if orbit and attitude dates or frames are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attForces, final Attitude attEvents) {
        checkConsistency(attForces, attEvents);
        checkConsistency(orbitIn, attForces, attEvents);
        this.orbit = orbitIn;
        this.attitude = attForces;
        this.attitudeEvents = attEvents;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
    }

    /**
     * Build a spacecraft state from orbit, attitude for forces and events computation and additional states map.
     * <p>
     * No mass informations are added.
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attForces
     *        the attitude for forces computation
     * @param attEvents
     *        the attitude for events computation
     * @param addStates
     *        the additional states map
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     *            if orbit and attitude dates or frames are not equal
     *            if attitude object and attitude in the additional states list map are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attForces, final Attitude attEvents,
        final Map<String, double[]> addStates) {
        checkConsistency(attForces, attEvents);
        checkConsistency(orbitIn, attForces, attEvents);
        checkConsistency(attForces, attEvents, addStates);
        this.orbit = orbitIn;
        this.attitude = attForces;
        this.attitudeEvents = attEvents;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
        for (final Map.Entry<String, double[]> entry : addStates.entrySet()) {
            this.additionalStates.put(entry.getKey(), entry.getValue().clone());
        }
    }

    /**
     * Build a spacecraft state from orbit, attitude for forces and events computation and mass provider.
     * <p>
     * The mass states informations contained in the mass provider are added to the additional states list with the name
     * in the form "MASS_<part name>".
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attForces
     *        the attitude for forces computation
     * @param attEvents
     *        the attitude for events computation
     * @param massProvider
     *        the mass provider
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     *            if orbit and attitude dates or frames are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attForces, final Attitude attEvents,
        final MassProvider massProvider) {
        checkConsistency(attForces, attEvents);
        checkConsistency(orbitIn, attForces, attEvents);
        this.orbit = orbitIn;
        this.attitude = attForces;
        this.attitudeEvents = attEvents;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
        this.addMassProviderToAdditionalStates(massProvider);
    }

    /**
     * Build a spacecraft state from orbit, attitude for forces and events computation, mass provider and additional
     * states map.
     * <p>
     * The mass states informations contained in the mass provider are added to the additional states list with the name
     * in the form "MASS_<part name>". If an additional state is already defined with the same name as the mass provider
     * state, the additional state is replaced with the informations from the mass provider.
     * </p>
     * 
     * @param orbitIn
     *        the orbit
     * @param attForces
     *        the attitude for forces computation
     * @param attEvents
     *        the attitude for events computation
     * @param massProvider
     *        the mass provider
     * @param additionalStatesIn
     *        the additional states
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     *            if orbit and attitude dates or frames are not equal
     *            if attitude object and attitude in the additional states list map are not equal
     */
    public SpacecraftState(final Orbit orbitIn, final Attitude attForces, final Attitude attEvents,
        final MassProvider massProvider, final Map<String, double[]> additionalStatesIn) {
        checkConsistency(attForces, attEvents);
        checkConsistency(orbitIn, attForces, attEvents);
        checkConsistency(attForces, attEvents, additionalStatesIn);
        this.orbit = orbitIn;
        this.attitude = attForces;
        this.attitudeEvents = attEvents;
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        this.additionalStates = new TreeMap<String, double[]>();
        for (final Map.Entry<String, double[]> entry : additionalStatesIn.entrySet()) {
            this.additionalStates.put(entry.getKey(), entry.getValue().clone());
        }
        this.addMassProviderToAdditionalStates(massProvider);
    }

    /**
     * Build a spacecraft from an array (a state vector) and an additional states informations map.
     * <p>
     * If the additional state informations list contains a state with the prefix "ATTITUDE_FORCES" (or
     * "ATTITUDE_EVENTS" or "ATTITUDE"), it means that the attitude for forces computation (or events computation) was
     * added to the additional states map. Then the corresponding Attitude object is initialized with the additional
     * state value. Otherwise, the {@link Attitude} is initialized using the corresponding {@link AttitudeProvider}.
     * </p>
     * 
     * @param y
     *        the state vector
     * @param orbitType
     *        the orbit type
     * @param angleType
     *        the position angle type
     * @param date
     *        the integration date
     * @param mu
     *        central attraction coefficient used for propagation (m<sup>3</sup>/s<sup>2</sup>)
     * @param frame
     *        frame in which integration is performed
     * @param addStatesInfo
     *        the additional states informations map
     * @param attProviderForces
     *        the attitude provider for forces computation
     * @param attProviderEvents
     *        the attitude provider for events computation
     */
    public SpacecraftState(final double[] y, final OrbitType orbitType, final PositionAngle angleType,
        final AbsoluteDate date, final double mu, final Frame frame,
        final Map<String, AdditionalStateInfo> addStatesInfo, final AttitudeProvider attProviderForces,
        final AttitudeProvider attProviderEvents) {

        // get additional states
        this.additionalStates = new TreeMap<String, double[]>();
        for (final Map.Entry<String, AdditionalStateInfo> entry : addStatesInfo.entrySet()) {
            final AdditionalStateInfo stateInfo = entry.getValue();
            final String name = entry.getKey();
            final int index = stateInfo.getIndex();
            final double[] state = Arrays.copyOfRange(y, index, index + stateInfo.getSize());
            this.additionalStates.put(name, state);
        }
        // get orbit
        this.orbit = orbitType.mapArrayToOrbit(y, angleType, date, mu, frame);

        this.attitudeProvider = attProviderForces;
        this.attitudeProviderEvents = attProviderEvents;
        this.attitude = null;
        this.attitudeEvents = null;

        // get attitudes for forces and events computation from additional states list
        this.updateAttitudes(date, frame);
        checkConsistency(this.attitude, this.attitudeEvents);
    }

    /**
     * Build a spacecraft from an array (a state vector) and an additional states informations map.
     * <p>
     * If the additional state informations list contains a state with the prefix "ATTITUDE_FORCES" (or
     * "ATTITUDE_EVENTS" or "ATTITUDE"), it means that the attitude for forces computation (or events computation) was
     * added to the additional states map. Then the corresponding Attitude object is initialized with the additional
     * state value. Otherwise, the {@link Attitude} is initialized using the corresponding {@link AttitudeProvider}.
     * </p>
     * 
     * @param y the state vector
     * @param orbitType the orbit type
     * @param angleType the position angle type
     * @param date the integration date
     * @param mu central attraction coefficient used for propagation (m<sup>3</sup>/s<sup>2</sup>)
     * @param frame frame in which integration is performed
     * @param addStatesInfo the additional states informations map
     * @param attForces the attitude for forces computation
     * @param attEvents the attitude for events computation
     */
    public SpacecraftState(final double[] y, final OrbitType orbitType,
        final PositionAngle angleType, final AbsoluteDate date, final double mu,
        final Frame frame, final Map<String, AdditionalStateInfo> addStatesInfo,
        final Attitude attForces, final Attitude attEvents) {

        // Get additional states
        this.additionalStates = new TreeMap<String, double[]>();
        for (final Map.Entry<String, AdditionalStateInfo> entry : addStatesInfo.entrySet()) {
            final AdditionalStateInfo stateInfo = entry.getValue();
            final String name = entry.getKey();
            final int index = stateInfo.getIndex();
            final double[] state = Arrays.copyOfRange(y, index, index + stateInfo.getSize());
            this.additionalStates.put(name, state);
        }
        // Get orbit
        this.orbit = orbitType.mapArrayToOrbit(y, angleType, date, mu, frame);

        // get attitudes for forces and events computation using the AttitudeProvider
        if (!this.additionalStates.containsKey(AttitudeType.ATTITUDE.toString())) {
            // The attitude provider could be null. In this case, the attitude will be null
            if (!this.additionalStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())) {
                if (attForces == null) {
                    this.attitude = null;
                } else {
                    this.attitude = attForces;
                }
            }
            if (!this.additionalStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString())) {
                if (attEvents == null) {
                    this.attitudeEvents = null;
                } else {
                    this.attitudeEvents = attEvents;
                }
            }
        }
        this.attitudeProvider = null;
        this.attitudeProviderEvents = null;
        // get attitudes for forces and events computation from additional states list
        this.updateAttitudes(date, frame);
        checkConsistency(this.attitude, this.attitudeEvents);
    }

    /**
     * Add the values of mass parts from MassProvider to additional states map.
     * <p>
     * In the SpacecraftState constructors, this method should be called after adding the additional states map given as
     * constructor parameter. If the additional states map previously contained a mapping for the key, the old value is
     * replaced.
     * </p>
     * 
     * @param massProvider
     *        the mass provider
     */
    private void addMassProviderToAdditionalStates(final MassProvider massProvider) {
        if (massProvider != null) {
            final List<String> partsNames = massProvider.getAllPartsNames();
            final int size = partsNames.size();
            for (int i = 0; i < size; i++) {
                final String name = partsNames.get(i);
                final double[] state = new double[] { massProvider.getMass(name) };
                this.additionalStates.put(genNameMassAddState(name), state);
            }
        }
    }

    /**
     * Update the {@link Attitude} from the additionalStates map.
     * <p>
     * The additional states representing the attitude are composed of a quaternion and a spin vector. This method
     * should be called after updating additionalStates list.
     * </p>
     * 
     * @param date
     *        the date at which attitude is defined
     * @param frame
     *        the reference frame from which attitude is defined
     */
    private void updateAttitudes(final AbsoluteDate date, final Frame frame) {
        // Check attitude consistency is already done in checkConsistency method
        if (this.additionalStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())) {
            final double[] att = this.additionalStates.get(AttitudeType.ATTITUDE_FORCES.toString());
            this.attitude = new Attitude(att, date, frame);
        }
        if (this.additionalStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString())) {
            final double[] att = this.additionalStates.get(AttitudeType.ATTITUDE_EVENTS.toString());
            this.attitudeEvents = new Attitude(att, date, frame);
        }
        // An additional state with the name "ATTITUDE" update the attitude for forces computation/
        if (this.additionalStates.containsKey(AttitudeType.ATTITUDE.toString())) {
            final double[] att = this.additionalStates.get(AttitudeType.ATTITUDE.toString());
            this.attitude = new Attitude(att, date, frame);
        }
    }

    /**
     * Generate a name in the form "MASS_<part name>" for the additional states from MassProvider.
     * 
     * @param name
     *        part name
     * @return full name of the equation
     */
    private static String genNameMassAddState(final String name) {
        return MASS + name;
    }

    /**
     * Check orbit and attitude dates are equal.
     * 
     * @param orbit
     *        the orbit
     * @param attitudeForces
     *        the attitude for forces computation
     * @param attitudeEvents
     *        the attitude for events computation
     * @exception IllegalArgumentException
     *            if orbit and attitude dates are not equal
     */
    private static void checkConsistency(final Orbit orbit, final Attitude attitudeForces,
                                         final Attitude attitudeEvents) {
        if (attitudeForces != null) {
            if (!orbit.getDate().equals(attitudeForces.getDate())) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                    orbit.getDate(), attitudeForces.getDate());
            }
            if (orbit.getFrame() != attitudeForces.getReferenceFrame()) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.FRAMES_MISMATCH,
                    orbit.getFrame().getName(), attitudeForces.getReferenceFrame().getName());
            }
        }
        if (attitudeEvents != null) {
            if (!orbit.getDate().equals(attitudeEvents.getDate())) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.ORBIT_AND_ATTITUDE_DATES_MISMATCH,
                    orbit.getDate(), attitudeEvents.getDate());
            }
            if (orbit.getFrame() != attitudeEvents.getReferenceFrame()) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.FRAMES_MISMATCH,
                    orbit.getFrame().getName(), attitudeEvents.getReferenceFrame().getName());
            }
        }
    }

    /**
     * Check attitudes registered as objects and attitudes in the additional states map are equal.
     * 
     * @param attitudeForces
     *        the attitude for forces computation
     * @param attitudeEvents
     *        the attitude for events computation
     * @param addStates
     *        the additional states
     * @exception IllegalArgumentException
     *            if attitude object and attitude in the additional states list map are not equal
     *            if additional states map contains (ATTITUDE_FORCES or ATTITUDE_EVENTS) state and ATTITUDE state
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    private static void checkConsistency(final Attitude attitudeForces, final Attitude attitudeEvents,
                                         final Map<String, double[]> addStates) {
        // CHECKSTYLE: resume CyclomaticComplexity check

        if (((addStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())) ||
            (addStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString()))) &&
            (addStates.containsKey(AttitudeType.ATTITUDE.toString()))) {
            // Exception
            throw PatriusException.createIllegalArgumentException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        if (addStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())) {
            final double[] state = addStates.get(AttitudeType.ATTITUDE_FORCES.toString());
            final double[] stateFromAttitude = attitudeForces.mapAttitudeToArray();
            for (int i = 0; i < state.length; i++) {
                if (!Precision.equals(state[i], stateFromAttitude[i], Precision.DOUBLE_COMPARISON_EPSILON)) {
                    // Exception
                    throw PatriusException.createIllegalArgumentException(PatriusMessages.ATTITUDES_MISMATCH);
                }
            }
        }
        if (addStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString())) {
            final double[] state = addStates.get(AttitudeType.ATTITUDE_EVENTS.toString());
            final double[] stateFromAttitude = attitudeEvents.mapAttitudeToArray();
            for (int i = 0; i < state.length; i++) {
                if (!Precision.equals(state[i], stateFromAttitude[i], Precision.DOUBLE_COMPARISON_EPSILON)) {
                    // Exception
                    throw PatriusException.createIllegalArgumentException(PatriusMessages.ATTITUDES_MISMATCH);
                }
            }
        }
        if (addStates.containsKey(AttitudeType.ATTITUDE.toString())) {
            final double[] stateFromAttitude;
            // The attitude contained in the additional state map is compared with the attitude for forces
            // computation.
            if (attitudeForces == null) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.NO_ATTITUDE_DEFINED);
            } else {
                stateFromAttitude = attitudeForces.mapAttitudeToArray();
            }
            final double[] state = addStates.get(AttitudeType.ATTITUDE.toString());
            for (int i = 0; i < state.length; i++) {
                if (!Precision.equals(state[i], stateFromAttitude[i], Precision.DOUBLE_COMPARISON_EPSILON)) {
                    // Exception
                    throw PatriusException.createIllegalArgumentException(PatriusMessages.ATTITUDES_MISMATCH);
                }
            }
        }
    }

    /**
     * Check if attitudeForces == null and attitudeEvents!= null.
     * 
     * @param attitudeForces
     *        the attitude for forces computation
     * @param attitudeEvents
     *        the attitude for events computation
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     */
    private static void checkConsistency(final Attitude attitudeForces, final Attitude attitudeEvents) {
        if ((attitudeForces == null) && (attitudeEvents != null)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.ATTITUDE_FORCES_NULL);
        }
    }

    /**
     * Add an additional state to the additional states map.
     * <p>
     * If the additionalStates map already contained a mapping for the name entered, the old state is replaced by the
     * specified state. It is not possible to add an additional state with "ATTITUDE_FORCES" or "ATTITUDE_EVENTS" or
     * "ATTITTUDE" name : it is a reserved name.
     * </p>
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param name
     *        the additional state name
     * @param state
     *        the additional state value
     * @return a new SpacecraftState with the added additional state.
     * @throws PatriusException
     *         if the name of the additional state to be added is "ATTITUDE_FORCES" or "ATTITUDE_EVENTS" or
     *         "ATTITUDE".
     *         if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    public SpacecraftState addAdditionalState(final String name, final double[] state) throws PatriusException {
        if (name.equals(AttitudeType.ATTITUDE_FORCES.toString()) ||
            name.equals(AttitudeType.ATTITUDE_EVENTS.toString()) ||
            name.equals(AttitudeType.ATTITUDE.toString())) {
            // this additional state name is already reserved for attitude, throw an exception:
            throw new PatriusException(PatriusMessages.ADDITIONAL_STATE_NAME_RESERVED, name);
        }
        final Map<String, double[]> addStatesTemp = this.getAdditionalStates();
        addStatesTemp.put(name, state);
        return new SpacecraftState(this.orbit, this.getAttitude(), this.getAttitudeEvents(), addStatesTemp);
    }

    /**
     * Add attitude to the additional states map.
     * <p>
     * The additional state name could be "ATTITUDE_FORCES" if {@link AttitudeType#ATTITUDE_FORCES}, "ATTITUDE_EVENTS"
     * if {@link AttitudeType#ATTITUDE_EVENTS} or "ATTITUDE" if {@link AttitudeType#ATTITUDE}. If AttitudeType#ATTITUDE,
     * the only one attitude available is added to the additional states map.
     * </p>
     * <p>
     * If an additional state with the name "ATTITUDE_FORCES", "ATTITUDE_EVENTS" or "ATTITUDE" is already in the
     * additional state map, its value will be updated with the new one. The attitude additional state contains the
     * rotation and the spin.
     * </p>
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param attitudeType
     *        one of {@link AttitudeType#ATTITUDE_FORCES} or {@link AttitudeType#ATTITUDE_EVENTS} or
     *        {@link AttitudeType#ATTITUDE}
     * @return a new SpacecraftState with the added additional state corresponding to an SpacecraftState attitude.
     * @throws PatriusException
     *         if no attitude information is defined
     *         if additional states map contains (ATTITUDE_FORCES or ATTITUDE_EVENTS) state and ATTITUDE state.
     *         if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    public SpacecraftState addAttitudeToAdditionalStates(final AttitudeType attitudeType) throws PatriusException {
        // CHECKSTYLE: resume CyclomaticComplexity check

        // Initialization
        final Map<String, double[]> addStates = this.getAdditionalStates();

        // Result depends on attitude type
        switch (attitudeType) {
            case ATTITUDE_FORCES:
                if (addStates.containsKey(AttitudeType.ATTITUDE.toString())) {
                    // Exception
                    throw new PatriusException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
                }
                if (this.getAttitude() == null) {
                    // Exception
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_DEFINED);
                } else {
                    addStates.put(attitudeType.toString(), this.getAttitude().mapAttitudeToArray());
                }
                break;
            case ATTITUDE_EVENTS:
                if (addStates.containsKey(AttitudeType.ATTITUDE.toString())) {
                    // Exception
                    throw new PatriusException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
                }
                if (this.getAttitudeEvents() == null) {
                    // Exception
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_EVENTS_DEFINED);
                } else {
                    addStates
                        .put(attitudeType.toString(), this.getAttitudeEvents().mapAttitudeToArray());
                }
                break;
            case ATTITUDE:
                if ((addStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString()))
                    || (addStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString()))) {
                    // Exception
                    throw new PatriusException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
                }
                if (this.getAttitude() == null) {
                    // Exception
                    throw new PatriusException(PatriusMessages.NO_ATTITUDE_DEFINED);
                } else {
                    addStates.put(AttitudeType.ATTITUDE.toString(), this.getAttitude()
                        .mapAttitudeToArray());
                }
                break;
            default:
                // Cannot happen
                throw new PatriusRuntimeException(PatriusMessages.UNKNOWN_PARAMETER, null);
        }

        // Return result
        return new SpacecraftState(this.orbit, this.getAttitude(), this.getAttitudeEvents(), addStates);
    }

    /**
     * Add the values of mass parts from MassProvider to additional states map.
     * <p>
     * If the additional state map already contained an additional state from the input mass provider, its value will be
     * updated with the new one.
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param massProvider
     *        the mass provider to add
     * @return a new SpacecraftState with new additional states from mass provider
     * @throws PatriusException if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    public SpacecraftState addMassProvider(final MassProvider massProvider) throws PatriusException {
        // Additional states
        final Map<String, double[]> addStates = this.getAdditionalStates();
        if (massProvider != null) {
            // update additional states with values of mass parts from massProvider
            final List<String> partsNames = massProvider.getAllPartsNames();
            final int size = partsNames.size();
            for (int i = 0; i < size; i++) {
                final String name = partsNames.get(i);
                final double[] state = new double[] { massProvider.getMass(name) };
                addStates.put(genNameMassAddState(name), state);
            }
        }
        // Create spacecraft state from updated additional states
        return new SpacecraftState(this.orbit, this.getAttitude(), this.getAttitudeEvents(), addStates);
    }

    /**
     * <p>
     * Add attitude to the additional states map. It is not possible to add an attitude for events computation if no
     * attitude for forces computation is defined.
     * </p>
     * <p>
     * Be careful, additional states associated with attitude are not updated. The user should call
     * addAttitudeToAdditionalStates to add it.
     * </p>
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param newAttitude
     *        the attitude to be added to the state
     * @param type
     *        the attitude type
     * @return a new state with added attitude (the initial state if newAttitude is null).
     * @throws PatriusException if attitude cannot be computed
     *         if attitude events cannot be computed
     * @exception IllegalArgumentException
     *            if attitudeForces == null and attitudeEvents!= null
     * @since 3.0
     */
    public SpacecraftState addAttitude(final Attitude newAttitude, final AttitudeType type) throws PatriusException {
        SpacecraftState result = this;
        if (newAttitude != null) {
            if ((type == AttitudeType.ATTITUDE) || (type == AttitudeType.ATTITUDE_FORCES)) {
                // add attitude attribute
                result = new SpacecraftState(this.orbit, newAttitude, this.getAttitudeEvents(), this.additionalStates);
            } else if (type == AttitudeType.ATTITUDE_EVENTS) {
                if (this.getAttitude() == null) {
                    throw PatriusException.createIllegalArgumentException(PatriusMessages.ATTITUDE_FORCES_NULL);
                } else {
                    // add attitude attribute
                    result = new SpacecraftState(this.orbit, this.getAttitude(), newAttitude, this.additionalStates);
                }
            }
        }
        return result;
    }

    /**
     * Update the mass of the given part.
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param partName
     *        given part
     * @param newMass
     *        new mass of the given part
     * @return a new SpacecraftState with an updated mass part
     * @throws PatriusException
     *         if no mass informations already defined for the given part
     *         if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    public SpacecraftState updateMass(final String partName, final double newMass) throws PatriusException {
        final String addStateName = genNameMassAddState(partName);
        if (this.additionalStates.containsKey(addStateName)) {
            final Map<String, double[]> addStates = this.getAdditionalStates();
            addStates.put(addStateName, new double[] { newMass });
            return new SpacecraftState(this.orbit, this.getAttitude(), this.getAttitudeEvents(), addStates);
        } else {
            throw new PatriusException(PatriusMessages.NO_MASS_INFOS_DEFINED);
        }
    }

    /**
     * Update the orbit.
     * 
     * <p>
     * <b>{@link SpacecraftState} object state being immutable, a new {@link SpacecraftState} object is returned.</b>
     * </p>
     * 
     * @param newOrbit
     *        the new orbit
     * @return a new SpacecraftState with an updated orbit
     * @throws PatriusException if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    public SpacecraftState updateOrbit(final Orbit newOrbit) throws PatriusException {
        return new SpacecraftState(newOrbit, this.getAttitude(), this.getAttitudeEvents(), this.additionalStates);
    }

    /**
     * Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on a simple keplerian model for orbit, a
     * linear extrapolation for attitude taking the spin rate into account. It is <em>not</em> intended as a replacement
     * for proper orbit and attitude propagation but should be sufficient for small time shifts or coarse accuracy.
     * </p>
     * <p>
     * WARNING : Additional states map is not changed (except for attitude objects if present) !
     * </p>
     * <p>
     * As a rough order of magnitude, the following table shows the interpolation errors obtained between this simple
     * shift method and an {@link fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator
     * Eckstein-Heschler
     * propagator} for an 800km altitude nearly circular polar Earth orbit with
     * {@link fr.cnes.sirius.patrius.attitudes.BodyCenterPointing body center pointing}. Beware that these results may
     * be different for other orbits.
     * </p>
     * <table border="1" cellpadding="5">
     * <tr bgcolor="#ccccff">
     * <th>interpolation time (s)</th>
     * <th>position error (m)</th>
     * <th>velocity error (m/s)</th>
     * <th>attitude error (&deg;)</th>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">60</td>
     * <td>20</td>
     * <td>1</td>
     * <td>0.001</td>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">120</td>
     * <td>100</td>
     * <td>2</td>
     * <td>0.002</td>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">300</td>
     * <td>600</td>
     * <td>4</td>
     * <td>0.005</td>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">600</td>
     * <td>2000</td>
     * <td>6</td>
     * <td>0.008</td>
     * </tr>
     * <tr>
     * <td bgcolor="#eeeeff">900</td>
     * <td>4000</td>
     * <td>6</td>
     * <td>0.010</td>
     * </tr>
     * </table>
     * 
     * @param dt
     *        time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     *         except for the mass which stay unchanged
     * @throws PatriusException if attitude cannot be computed
     *         if attitude events cannot be computed
     */
    @Override
    public SpacecraftState shiftedBy(final double dt) throws PatriusException {
        // Initialization
        Attitude attForces = null;
        Attitude attEvents = null;

        // Get attitude
        if (this.getAttitude() != null) {
            attForces = this.getAttitude().shiftedBy(dt);
            if (this.getAttitudeEvents() != null) {
                attEvents = this.getAttitudeEvents().shiftedBy(dt);
            }
        }
        SpacecraftState temp =
            new SpacecraftState(this.orbit.shiftedBy(dt), attForces, attEvents, this.additionalStates);
        try {
            final int enumSize = AttitudeType.values().length;
            for (int type = 0; type < enumSize; type++) {
                final String attitudeType = AttitudeType.values()[type].toString();
                if (this.additionalStates.containsKey(attitudeType)) {
                    temp = temp.addAttitudeToAdditionalStates(AttitudeType.values()[type]);
                }
            }
        } catch (final PatriusException e) {
            // Exception
            throw new IllegalArgumentException(e.getSpecifier().toString(), e);
        }

        // Return result
        return temp;
    }

    /**
     * Get an interpolated instance.
     * <p>
     * The input sample SpacecraftState should have the same additional states size and name. If no attitudes are
     * defined, an error is handled.
     * </p>
     * <p>
     * Note that the state of the current instance may not be used in the interpolation process, only its type and non
     * interpolable fields are used (for example central attraction coefficient or frame when interpolating orbits). The
     * interpolable fields taken into account are taken only from the states of the sample points. So if the state of
     * the instance must be used, the instance should be included in the sample points.
     * </p>
     * 
     * @param date
     *        interpolation date
     * @param sample
     *        sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     * @throws PatriusException
     *         if the sample points are inconsistent
     *         if no attitudes are defined
     */
    @Override
    public SpacecraftState interpolate(final AbsoluteDate date,
                                       final Collection<SpacecraftState> sample) throws PatriusException {
        final List<Orbit> orbits = new ArrayList<Orbit>(sample.size());
        final List<Attitude> attitudesForces = new ArrayList<Attitude>(sample.size());
        final List<Attitude> attitudesEvents = new ArrayList<Attitude>(sample.size());
        // One interpolator for each additional states
        final Map<String, HermiteInterpolator> interpolators = new ConcurrentHashMap<String, HermiteInterpolator>();
        // Store reference structure
        final Map<String, double[]> addStatesRef = sample.iterator().next().getAdditionalStates();
        for (final SpacecraftState state : sample) {
            orbits.add(state.getOrbit());
            attitudesForces.add(state.getAttitudeForces());
            attitudesEvents.add(state.getAttitudeEvents());
            final Map<String, double[]> addStates = state.getAdditionalStates();
            if (!equalsAddStates(addStatesRef, addStates)) {
                // check if the list of additional states contains always the same names, otherwise throw an exception
                throw new PatriusException(PatriusMessages.LISTS_OF_ADD_STATES_MISMATCH);
            }
            for (final Map.Entry<String, double[]> entry : addStates.entrySet()) {
                final String name = entry.getKey();
                // check if the name is already in the interpolators list
                if (!interpolators.containsKey(name)) {
                    final HermiteInterpolator interpolator = new HermiteInterpolator();
                    interpolators.put(name, interpolator);
                }
                interpolators.get(name).addSamplePoint(state.getDate().durationFrom(date), entry.getValue());
            }
        }
        // Attitude interpolation
        Attitude attForces = null;
        Attitude attEvents = null;
        if (this.getAttitude() != null) {
            attForces = this.getAttitude().interpolate(date, attitudesForces);
            if (this.getAttitudeEvents() != null) {
                attEvents = this.getAttitudeEvents().interpolate(date, attitudesEvents);
            }
        }

        // Additional states interpolation
        final Map<String, double[]> interpolatedAddStates = new ConcurrentHashMap<String, double[]>();
        for (final Entry<String, HermiteInterpolator> entry : interpolators.entrySet()) {
            final double[] interpolatedAddState = entry.getValue().value(0);
            interpolatedAddStates.put(entry.getKey(), interpolatedAddState);
        }
        return new SpacecraftState(this.orbit.interpolate(date, orbits), attForces, attEvents, interpolatedAddStates);
    }
    
    /**
     * Get an interpolated instance of an orbit.
     * 
     * @param date
     *        interpolation date
     * @param orbits
     *        list of orbits on which interpolation should be done
     * @return a new orbit, interpolated at specified date
     * @throws PatriusException
     *         if the sample points are inconsistent
     */
    public Orbit interpolate(final AbsoluteDate date, final List<Orbit> orbits) throws PatriusException {
        return this.orbit.interpolate(date, orbits);    
    }
    
    /**
     * Gets the current orbit.
     * 
     * @return the orbit
     */
    public Orbit getOrbit() {
        return this.orbit;
    }

    /**
     * Get the date.
     * 
     * @return date
     */
    @Override
    public AbsoluteDate getDate() {
        return this.orbit.getDate();
    }

    /**
     * Get the inertial frame.
     * 
     * @return the frame
     */
    public Frame getFrame() {
        return this.orbit.getFrame();
    }

    /**
     * Compute the transform from orbit/attitude reference frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @return transform from inertial frame (where orbit/attitude are defined) to current spacecraft frame (attitude
     *         used for forces computation is the default attitude).
     * @throws PatriusException
     *         if no attitude information is defined
     *         if attitude cannot be computed
     */
    public Transform toTransform() throws PatriusException {
        final AbsoluteDate date = this.orbit.getDate();
        if (this.getAttitude() == null) {
            throw new PatriusException(PatriusMessages.NO_ATTITUDE_DEFINED);
        } else {
            return new Transform(date,
                new Transform(date, this.orbit.getPVCoordinates()),
                new Transform(date, this.getAttitude().getOrientation()));
        }
    }

    /**
     * Compute the transform from specified frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @param frame
     *        input frame
     * @return transform from specified frame to current spacecraft frame (attitude
     *         used for forces computation is the default attitude).
     * @throws PatriusException
     *         if no attitude information is defined
     */
    public Transform toTransform(final Frame frame) throws PatriusException {
        final AbsoluteDate date = this.orbit.getDate();
        // Transform from specified frame into inertial frame (getFrame method of SpacecraftState)
        final Transform transform1 = frame.getTransformTo(this.getFrame(), date);
        // Transform from inertial frame into spacecraft frame
        final Transform transform2 = this.toTransform();
        return new Transform(date, transform1, transform2);
    }

    /**
     * Compute the transform from orbit/attitude reference frame to local orbital frame.
     * 
     * @param lofType
     *        the LOF type
     * @return transform from inertial frame (where orbit/attitude are defined) to local orbital frame.
     */
    public Transform toTransform(final LOFType lofType) {
        // Transform from the frame where position-velocity are defined to local orbital frame
        return lofType.transformFromInertial(this.getDate(), this.getPVCoordinates());
    }

    /**
     * Compute the transform from specified frame to local orbital frame.
     * 
     * @param frame
     *        input frame
     * @param lofType
     *        the LOF type
     * @return transform from from specified frame to local orbital frame
     * @throws PatriusException
     *         if some frame specific error occurs
     */
    public Transform toTransform(final Frame frame, final LOFType lofType) throws PatriusException {
        final AbsoluteDate date = this.orbit.getDate();
        // Transform from specified frame to inertial frame
        final Transform transform1 = frame.getTransformTo(this.getFrame(), date);
        // Transform from inertial frame to local orbital frame
        final Transform transform2 = lofType.transformFromInertial(date, this.getPVCoordinates());
        return new Transform(date, transform1, transform2);
    }

    /**
     * Compute the transform from orbit/attitude (for forces computation) reference frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @return transform from inertial frame (where orbit/attitude are defined) to current spacecraft frame (attitude
     *         used for forces computation is the default attitude).
     * @throws PatriusException
     *         if no attitude information is defined
     */
    public Transform toTransformForces() throws PatriusException {
        return this.toTransform();
    }

    /**
     * Compute the transform from specified frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @param frame
     *        input frame
     * @return transform from specified frame to current spacecraft frame (attitude
     *         used for forces computation is the default attitude).
     * @throws PatriusException
     *         if no attitude information is defined
     */
    public Transform toTransformForces(final Frame frame) throws PatriusException {
        return this.toTransform(frame);
    }

    /**
     * Compute the transform from orbit/attitude (for events computation) reference frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @return transform from inertial frame (where orbit/attitude are defined) to current spacecraft frame (same as
     *         toTransform if there is no specific attitude for Events).
     * @throws PatriusException
     *         if no attitude information is defined
     *         if attitude events cannot be computed
     */
    public Transform toTransformEvents() throws PatriusException {
        final AbsoluteDate date = this.orbit.getDate();
        if (this.getAttitudeEvents() == null) {
            throw new PatriusException(PatriusMessages.NO_ATTITUDE_EVENTS_DEFINED);
        } else {
            return new Transform(date,
                new Transform(date, this.orbit.getPVCoordinates()),
                new Transform(date, this.getAttitudeEvents().getOrientation()));
        }
    }

    /**
     * Compute the transform from specified reference frame to spacecraft frame.
     * <p>
     * The spacecraft frame origin is at the point defined by the orbit, and its orientation is defined by the attitude.
     * </p>
     * 
     * @param frame
     *        input frame
     * @return transform from integration frame to current spacecraft frame (same as
     *         toTransform if there is no specific attitude for Events).
     * @throws PatriusException
     *         if no attitude information is defined
     */
    public Transform toTransformEvents(final Frame frame) throws PatriusException {
        final AbsoluteDate date = this.orbit.getDate();
        // Transform from specified frame into inertial frame (getFrame method of SpacecraftState)
        final Transform transform1 = frame.getTransformTo(this.getFrame(), date);
        // Transform from inertial frame into spacecraft frame
        final Transform transform2 = this.toTransformEvents();
        return new Transform(date, transform1, transform2);
    }

    /**
     * Get the central attraction coefficient.
     * 
     * @return mu central attraction coefficient (m^3/s^2)
     */
    public double getMu() {
        return this.orbit.getMu();
    }

    /**
     * Get the keplerian period.
     * <p>
     * The keplerian period is computed directly from semi major axis and central acceleration constant.
     * </p>
     * 
     * @return keplerian period in seconds
     */
    public double getKeplerianPeriod() {
        return this.orbit.getKeplerianPeriod();
    }

    /**
     * Get the keplerian mean motion.
     * <p>
     * The keplerian mean motion is computed directly from semi major axis and central acceleration constant.
     * </p>
     * 
     * @return keplerian mean motion in radians per second
     */
    public double getKeplerianMeanMotion() {
        return this.orbit.getKeplerianMeanMotion();
    }

    /**
     * Get the semi-major axis.
     * 
     * @return semi-major axis (m)
     */
    public double getA() {
        return this.orbit.getA();
    }

    /**
     * Get the first component of the eccentricity vector (as per equinoctial parameters).
     * 
     * @return e cos(&omega; + &Omega;), first component of eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEx() {
        return this.orbit.getEquinoctialEx();
    }

    /**
     * Get the second component of the eccentricity vector (as per equinoctial parameters).
     * 
     * @return e sin(&omega; + &Omega;), second component of the eccentricity vector
     * @see #getE()
     */
    public double getEquinoctialEy() {
        return this.orbit.getEquinoctialEy();
    }

    /**
     * Get the first component of the inclination vector (as per equinoctial parameters).
     * 
     * @return tan(i/2) cos(&Omega;), first component of the inclination vector
     * @see #getI()
     */
    public double getHx() {
        return this.orbit.getHx();
    }

    /**
     * Get the second component of the inclination vector (as per equinoctial parameters).
     * 
     * @return tan(i/2) sin(&Omega;), second component of the inclination vector
     * @see #getI()
     */
    public double getHy() {
        return this.orbit.getHy();
    }

    /**
     * Get the true longitude argument (as per equinoctial parameters).
     * 
     * @return v + &omega; + &Omega; true longitude argument (rad)
     * @see #getLE()
     * @see #getLM()
     */
    public double getLv() {
        return this.orbit.getLv();
    }

    /**
     * Get the eccentric longitude argument (as per equinoctial parameters).
     * 
     * @return E + &omega; + &Omega; eccentric longitude argument (rad)
     * @see #getLv()
     * @see #getLM()
     */
    public double getLE() {
        return this.orbit.getLE();
    }

    /**
     * Get the mean longitude argument (as per equinoctial parameters).
     * 
     * @return M + &omega; + &Omega; mean longitude argument (rad)
     * @see #getLv()
     * @see #getLE()
     */
    public double getLM() {
        return this.orbit.getLM();
    }

    // Additional orbital elements

    /**
     * Get the eccentricity.
     * 
     * @return eccentricity
     * @see #getEquinoctialEx()
     * @see #getEquinoctialEy()
     */
    public double getE() {
        return this.orbit.getE();
    }

    /**
     * Get the inclination.
     * 
     * @return inclination (rad)
     * @see #getHx()
     * @see #getHy()
     */
    public double getI() {
        return this.orbit.getI();
    }

    /**
     * Get the {@link PVCoordinates} in orbit definition frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached {@link PVCoordinates}, so the caller is
     * responsible to copy it in a separate {@link PVCoordinates} if it needs to keep the value for a while.
     * 
     * @return pvCoordinates in orbit definition frame
     */
    public PVCoordinates getPVCoordinates() {
        return this.orbit.getPVCoordinates();
    }

    /**
     * Get the {@link PVCoordinates} in given output frame.
     * Compute the position and velocity of the satellite. This method caches its
     * results, and recompute them only when the method is called with a new value
     * for mu. The result is provided as a reference to the internally cached {@link PVCoordinates}, so the caller is
     * responsible to copy it in a separate {@link PVCoordinates} if it needs to keep the value for a while.
     * 
     * @param outputFrame
     *        frame in which coordinates should be defined
     * @return pvCoordinates in orbit definition frame
     * @exception PatriusException
     *            if the transformation between frames cannot be computed
     */
    public PVCoordinates getPVCoordinates(final Frame outputFrame) throws PatriusException {
        return this.orbit.getPVCoordinates(outputFrame);
    }

    /**
     * Get the default attitude : the attitude for forces computation.
     * 
     * @return the attitude for forces computation.
     * @throws PatriusException if attitude cannot be computed
     */
    public Attitude getAttitude() throws PatriusException {
        
        if (this.attitude == null) {
            if (this.attitudeProvider != null) {
                synchronized (this) {
                    // The attitude is checked a second time in case it's been initialized by an other thread while the
                    // current one was waiting
                    if (this.attitude == null) {
                        this.attitude = this.attitudeProvider.getAttitude(this.orbit);
                    }
                }
            }
        }

        return this.attitude;
    }

    /**
     * Get the default attitude : the attitude for forces computation in given output frame.
     * 
     * @param outputFrame
     *        frame in which the attitude is wanted
     * @return the attitude for force computation in the output frame
     * @throws PatriusException
     *         if conversion between reference frames fails
     *         if attitude cannot be computed
     */
    public Attitude getAttitude(final Frame outputFrame) throws PatriusException {
        final boolean spinDerivativesComputation = this.getAttitude().getRotationAcceleration() != null;
        return this.getAttitude().withReferenceFrame(outputFrame, spinDerivativesComputation);
    }

    /**
     * Get the default attitude : the attitude for forces computation in given local
     * orbital frame.
     * <p>
     * Warning: this method creates a new local orbital frame at each call. For multiple calls, prefer using
     * {@link #getAttitude(Frame)} with your own {@link LocalOrbitalFrame}.
     * </p>
     * 
     * @param lofType
     *        the LOF type
     * @return the default attitude in the local orbital frame
     * @throws PatriusException
     *         if conversion between reference frames fails
     *         if attitude cannot be computed
     */
    public Attitude getAttitude(final LOFType lofType) throws PatriusException {
        final LocalOrbitalFrame lof =
            new LocalOrbitalFrame(this.getAttitude().getReferenceFrame(), lofType, this.orbit, "LOF");
        return this.getAttitude(lof);
    }

    /**
     * Get the attitude for forces computation.
     * 
     * @return the attitude for forces computation.
     * @throws PatriusException if attitude cannot be computed
     */
    public Attitude getAttitudeForces() throws PatriusException {
        return this.getAttitude();
    }

    /**
     * Get the attitude for forces computation in given output frame.
     * 
     * @param outputFrame
     *        frame in which the attitude is wanted
     * @return the attitude for force computation in the output frame
     * @throws PatriusException
     *         if conversion between reference frames fails
     */
    public Attitude getAttitudeForces(final Frame outputFrame) throws PatriusException {
        return this.getAttitude(outputFrame);
    }

    /**
     * Get the attitude for forces computation in given local orbital frame.
     * <p>
     * Warning: this method creates a new local orbital frame at each call. For multiple calls, prefer using
     * {@link #getAttitude(Frame)} with your own {@link LocalOrbitalFrame}.
     * </p>
     * 
     * @param lofType
     *        the LOF type
     * @return the attitude for forces computation in the local orbital frame
     * @throws PatriusException
     *         if conversion between reference frames fails
     */
    public Attitude getAttitudeForces(final LOFType lofType) throws PatriusException {
        return this.getAttitude(lofType);
    }

    /**
     * Get the attitude for events computation.
     * 
     * @return the attitude for events computation (same as {@link #getAttitude()} if there is no
     *         specific attitude for Events).
     * @throws PatriusException if attitude events cannot be computed
     */
    public Attitude getAttitudeEvents() throws PatriusException {

        if (this.attitudeEvents == null) {
            synchronized (this) {
                // The attitude is checked a second time in case it's been initialized by an other thread while the
                // current one was waiting
                if (this.attitudeEvents == null) {
                    if (this.attitudeProviderEvents == null) {
                        this.attitudeEvents = this.getAttitude();
                    } else {
                        this.attitudeEvents = this.attitudeProviderEvents.getAttitude(this.orbit);
                    }
                }
            }
        }

        return this.attitudeEvents;
    }

    /**
     * Get the attitude for events computation in given output frame.
     * 
     * @param outputFrame
     *        frame in which the attitude is wanted
     * @return the attitude for events computation in the output frame
     *         (same as {@link #getAttitude(Frame)} if there is no specific attitude
     *         for Events).
     * @throws PatriusException
     *         if conversion between reference frames fails
     *         if attitude events cannot be computed
     */
    public Attitude getAttitudeEvents(final Frame outputFrame) throws PatriusException {
        if (this.getAttitudeEvents() == null) {
            return this.getAttitude(outputFrame);
        } else {
            final boolean spinDerivativesComputation = this.getAttitudeEvents().getRotationAcceleration() != null;
            return this.getAttitudeEvents().withReferenceFrame(outputFrame, spinDerivativesComputation);
        }
    }

    /**
     * Get the attitude for events computation in given local orbital frame.
     * <p>
     * Warning: this method creates a new local orbital frame at each call. For multiple calls, prefer using
     * {@link #getAttitude(Frame)} with your own {@link LocalOrbitalFrame}.
     * </p>
     * 
     * @param lofType
     *        the LOF type
     * @return the attitude for events computation in the local orbital frame
     *         (same as {@link #getAttitude(LOFType)} if there is no specific attitude
     *         for Events).
     * @throws PatriusException
     *         if conversion between reference frames fails
     *         if attitude events cannot be computed
     */
    public Attitude getAttitudeEvents(final LOFType lofType) throws PatriusException {
        if (this.getAttitudeEvents() == null) {
            return this.getAttitude(lofType);
        } else {
            final LocalOrbitalFrame lofAtttitudeEvents =
                new LocalOrbitalFrame(this.getAttitudeEvents().getReferenceFrame(),
                    lofType, this.orbit, "LOF attitude events");
            return this.getAttitude(lofAtttitudeEvents);
        }
    }

    /**
     * Get the mass of the given part.
     * 
     * @param partName
     *        given part
     * @return mass of part
     * @throws PatriusException
     *         if no mass informations defined for the given part.
     */
    public double getMass(final String partName) throws PatriusException {
        final String addStateName = genNameMassAddState(partName);
        if (this.additionalStates.containsKey(addStateName)) {
            return this.additionalStates.get(addStateName)[0];
        } else {
            throw new PatriusException(PatriusMessages.NO_MASS_INFOS_DEFINED);
        }
    }

    /**
     * Get additional states.
     * 
     * @return the additional states
     */
    public Map<String, double[]> getAdditionalStates() {
        return (Map<String, double[]>) ((TreeMap<String, double[]>) this.additionalStates).clone();
    }

    /**
     * Get additional states with the prefix "MASS_".
     * 
     * @return the additional states with the prefix "MASS_".
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<String, double[]> getAdditionalStatesMass() {
        final Map<String, double[]> map = new TreeMap<String, double[]>();
        for (final Map.Entry<String, double[]> entry : this.additionalStates.entrySet()) {
            if (entry.getKey().contains(MASS)) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }

    /**
     * Get one additional state.
     * 
     * @param name
     *        additional state name.
     * @return the additional state
     * @throws PatriusException
     *         if the input additional state name is unknown
     */
    public double[] getAdditionalState(final String name) throws PatriusException {
        if (this.additionalStates.containsKey(name)) {
            final double[] temp = this.additionalStates.get(name);
            return temp.clone();
        } else {
            throw new PatriusException(PatriusMessages.UNKNOWN_ADDITIONAL_STATE, name);
        }
    }

    /**
     * Get the additional states informations map.
     * 
     * @return the additional states informations.
     */
    @SuppressWarnings("PMD.UseConcurrentHashMap")
    public Map<String, AdditionalStateInfo> getAdditionalStatesInfos() {
        int index = ORBIT_DIMENSION;
        final Map<String, AdditionalStateInfo> result = new TreeMap<String, AdditionalStateInfo>();
        for (final Map.Entry<String, double[]> entry : this.additionalStates.entrySet()) {
            final int size = entry.getValue().length;
            result.put(entry.getKey(), new AdditionalStateInfo(size, index));
            index += size;
        }
        return result;
    }

    /**
     * Convert SpacecraftState to state vector.
     * 
     * @param orbitType
     *        the {@link Orbit orbital} parameters types
     * @param angleType
     *        the position angles type
     * @param stateVector
     *        flat array into which the state vector should be mapped
     */
    public void mapStateToArray(final OrbitType orbitType, final PositionAngle angleType, final double[] stateVector) {
        final Orbit initialOrbit = orbitType.convertType(this.getOrbit());
        orbitType.mapOrbitToArray(initialOrbit, angleType, stateVector);
        int index = ORBIT_DIMENSION;
        for (final Map.Entry<String, double[]> entry : this.additionalStates.entrySet()) {
            final double[] state = entry.getValue();
            final int size = state.length;
            System.arraycopy(state, 0, stateVector, index, size);
            index += size;
        }
    }

    /**
     * Compares additional states. This method checks additional state vector has the same keys (not same values).
     * 
     * @param addStates1
     *        the first additional states to compare
     * @param addStates2
     *        the second additional states to compare
     * @return isEqual equals true if additional states are equal.
     */
    public static boolean equalsAddStates(final Map<String, double[]> addStates1,
                                          final Map<String, double[]> addStates2) {

        // Initialization
        boolean isEqual = true;

        final Set<String> keySet1 = addStates1.keySet();
        final Set<String> keySet2 = addStates2.keySet();

        if (keySet1.size() == keySet2.size()) {
            // Loop on all states
            final Iterator<String> iterator1 = keySet1.iterator();
            final Iterator<String> iterator2 = keySet2.iterator();
            while (iterator1.hasNext()) {
                if (!iterator1.next().equals(iterator2.next())) {
                    isEqual = false;
                    break;
                }
            }
        } else {
            isEqual = false;
        }

        // Return result
        return isEqual;
    }

    /**
     * Get the state vector size.
     * 
     * @return the state vector size.
     */
    public int getStateVectorSize() {
        int size = ORBIT_DIMENSION;
        for (final Map.Entry<String, double[]> entry : this.additionalStates.entrySet()) {
            final int addStateSize = entry.getValue().length;
            size += addStateSize;
        }
        return size;
    }

    /**
     * Build a static spacecraft state which only wraps a date.
     * <p>
     * This spacecraft state shouldn't be used as a normal spacecraft state, as most of its
     * attributes aren't initialized (no orbit, no attitude, no additional states, etc).
     * </p>
     * <p>
     * <i>Usage example: to easily call the {@link IParamDiffFunction#value(SpacecraftState)} with
     * functions which only depends on dates. The performances aren't much impacted by this
     * wrapping.</i>
     * </p>
     *
     * @param date
     *        Wrapped date
     * @return the light spacecraft state
     */
    public static SpacecraftState getSpacecraftStateLight(final AbsoluteDate date) {
        /*
         * Implementation note:
         * - Uses a CartesianOrbit as it requires the least effort for initialization/transformation
         * compared to the others orbit types
         * - Uses a PVCoorinates with initialized acceleration for performance improvement in the
         * Orbit constructor
         */
        return new SpacecraftState(new CartesianOrbit(new PVCoordinates(Vector3D.ZERO,
                Vector3D.ZERO, Vector3D.PLUS_I), null, date, 0.));
    }
}
