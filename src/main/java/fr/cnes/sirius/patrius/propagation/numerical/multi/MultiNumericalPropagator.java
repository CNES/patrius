/**
 *
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
 *
 * @history created 18/03/2015
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-69:30/06/2023:[PATRIUS] Amélioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * VERSION:4.11:DM:DM-38:22/05/2023:[PATRIUS] Suppression de setters pour le MultiNumericalPropagator
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2888:18/05/2021:ajout des derivee des angles alpha,delta,w &amp;#224; la classe UserIAUPole
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:DM:DM-2415:27/05/2020:Gestion des PartialderivativesEquations avec MultiPropagateur 
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:300:18/03/2015:Creation multi propagator
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:426:30/10/2015: Set up orbits in non inertial frames, manage the conversion frame
 * for method propagate
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:905:04/09/2017: use of LinkedHashMap instead of HashMap
 * VERSION::FA:1520:24/04/2018: Modification of an error message
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::FA:1871:05/10/2018: Modifications FA1871 (Mass model update fix)
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::DM:1872:10/12/2018:Substitution of AttitudeProvider with MultiAttitudeProvider
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical.multi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.multi.MultiAttitudeProviderWrapper;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.AbstractIntegrator;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.SecondOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.events.EventHandler;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.CowellIntegrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.MultiPropagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NullMassDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.AdaptedMonoEventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.AdaptedMultiEventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.MultiEventDetector;
import fr.cnes.sirius.patrius.propagation.events.multi.OneSatEventDetectorWrapper;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquations;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalEquationsAndTolerances;
import fr.cnes.sirius.patrius.propagation.numerical.AdditionalStateInfo;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.numerical.TimeDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.precomputed.multi.MultiIntegratedEphemeris;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiAdaptedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.multi.MultiPatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * <p>
 * This class is copied from {@link fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator
 * NumericalPropagator} and adapted to multi propagation.
 * </p>
 * <p>
 * This class propagates N {@link SpacecraftState} using numerical integration. Each state is identified with an ID of
 * type String.
 * </p>
 * <p>
 * Multi spacecraft numerical propagation requires steps to set up the propagator to be used properly.
 * </p>
 * At least one satellite should be added to the propagator using {@link #addInitialState(SpacecraftState, String)}.
 * Then, the following configuration parameters
 * can be set for each state: </p>
 * <ul>
 * <li>the central attraction coefficient ({@link #setMu(double, String)})</li>
 * <li>the various force models ({@link #addForceModel(ForceModel, String)}, {@link #removeForceModels()})</li>
 * <li>whether {@link AdditionalEquations additional equations} (for example {@link MultiPartialDerivativesEquations
 * Jacobians}) should be propagated along with orbital state (
 * {@link #addAdditionalEquations(AdditionalEquations, String)}),
 * <li>the discrete events that should be triggered during propagation (
 * {@link #addEventDetector(EventDetector, String)}, {@link #clearEventsDetectors()})</li>
 * </ul>
 * <p>
 * The following general parameters can also be set :
 * <ul>
 * <li>the {@link OrbitType type} of orbital parameters to be used for propagation ( {@link #setOrbitType(OrbitType)}),
 * <li>the {@link PositionAngle type} of position angle to be used in orbital parameters to be used for propagation
 * where it is relevant ({@link #setPositionAngleType(PositionAngle)}),
 * <li>the discrete events that should be triggered during propagation ( {@link #addEventDetector(MultiEventDetector)},
 * {@link #clearEventsDetectors()})</li>
 * <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 * {@link #setMasterMode(double, MultiPatriusFixedStepHandler)}, {@link #setMasterMode(MultiPatriusStepHandler)},
 * {@link #setEphemerisMode()}, {@link #getGeneratedEphemeris(String)})</li>
 * </ul>
 * From these configuration parameters, only the initial state is mandatory. The default propagation settings are in
 * {@link OrbitType#EQUINOCTIAL equinoctial} parameters with {@link PositionAngle#TRUE true} longitude argument. If the
 * central attraction coefficient is not explicitly specified, the one used to define the initial orbit will be used.
 * </p>
 * <p>
 * The underlying numerical integrator set up in the constructor may also have its own configuration parameters. Typical
 * configuration parameters for adaptive stepsize integrators are the min, max and perhaps start step size as well as
 * the absolute and/or relative errors thresholds.
 * </p>
 * <p>
 * A state that is seen by the integrator is a simple six elements double array. The six first elements are either:
 * <ul>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.EquinoctialOrbit equinoctial orbit parameters} (a, e<sub>x</sub>,
 * e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, &lambda;<sub>M</sub> or &lambda;<sub>E</sub> or &lambda;<sub>v</sub>) in
 * meters and radians,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.KeplerianOrbit Keplerian orbit parameters} (a, e, i, &omega;, &Omega;, M
 * or E or v) in meters and radians,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.CircularOrbit circular orbit parameters} (a, e<sub>x</sub>,
 * e<sub>y</sub>, i, &Omega;, &alpha;<sub>M</sub> or &alpha;<sub>E</sub> or &alpha;<sub>v</sub>) in meters and rad,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.CartesianOrbit Cartesian orbit parameters} (x, y, z, v<sub>x</sub>,
 * v<sub>y</sub>, v<sub>z</sub>) in meters and meters per seconds.
 * </ul>
 * </p>
 * <p>
 * The following code snippet shows a typical setting for Low Earth Orbit propagation in equinoctial parameters and true
 * longitude argument:
 * </p>
 *
 * <pre>
 * final double dP = 0.001;
 * final double minStep = 0.001;
 * final double maxStep = 500;
 * final double initStep = 60;
 * AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep,
 *     AbsTolerance, RelTolerance);
 * integrator.setInitialStepSize(initStep);
 * propagator = new MultiNumericalPropagator(integrator);
 * </pre>
 * <p>
 * The same instance cannot be used simultaneously by different threads, the class is <em>not</em> thread-safe.
 * </p>
 *
 * @concurrency not thread-safe
 * @concurrency.comment attributes are mutable and related to propagation.
 *
 * @see SpacecraftState
 * @see ForceModel
 * @see MultiPatriusStepHandler
 * @see MultiPatriusFixedStepHandler
 * @see MultiIntegratedEphemeris
 * @see TimeDerivativesEquations
 *
 * @author maggioranic
 *
 * @version $Id$
 *
 * @since 3.0
 *
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public class MultiNumericalPropagator implements MultiPropagator, Observer, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 4034183110303195986L;

    /** Attitude providers for forces computation defined for each spacecraft. */
    private final Map<String, MultiAttitudeProvider> attitudeProvidersForces;

    /** Attitude providers for events computation defined for each spacecraft. */
    private final Map<String, MultiAttitudeProvider> attitudeProvidersEvents;

    /** Attitude providers given by default for one attitude (defined for each spacecraft). */
    private final Map<String, MultiAttitudeProvider> attitudeProvidersByDefault;

    /**
     * Force models used during the extrapolation of the Orbit, without jacobians defined for each
     * spacecraft.
     */
    private final Map<String, List<ForceModel>> forceModels;

    /** Event detectors not related to force models. */
    private final List<MultiEventDetector> multiDetectors;

    /** State vector. */
    private double[] stateVector;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Map of initial states to propagate. */
    private Map<String, SpacecraftState> initialStates;

    /** Map of current states to propagate. */
    private Map<String, SpacecraftState> currentStates;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private final FirstOrderIntegrator multiIntegrator;

    /** Counter for differential equations calls. */
    private int calls;

    /**
     * Mass model handler. If mass model equations are added to the propagator, the following
     * handler is added in order to update the mass model with the final state value
     */
    private final Map<String, MultiAdaptedStepHandler> massMultiModelHandlers;

    /** Propagator mode handler (ephemeris mode). */
    private transient MultiEphemerisModeHandler ephemerisMultiModeHandler;

    /** Propagator mode handler (master mode). */
    private transient MultiAdaptedStepHandler masterMultiModeHandler;

    /** Null mass detector defined for each state. */
    private final Map<String, NullMassDetector> nullMassDetectors;

    /** Current mode. */
    private int mode;

    /** Propagation orbit type. */
    private final OrbitType orbitType;

    /** Position angle type. */
    private final PositionAngle angleType;

    /** Additional equations and associated integrator tolerances defined for each spacecraft. */
    private final Map<String, List<AdditionalEquationsAndTolerances>> addEquationsAndTolerances;

    /** State vector informations. */
    private MultiStateVectorInfo stateVectorInfo;

    /** MU for each spacecraft to be used for integration process. */
    private final Map<String, Double> muMap;

    /** Frame for each spacecraft to be used for integration process. */
    private final Map<String, Frame> propagationFrameMap;

    /**
     * User-specified absolute tolerances for additional states. Built from the additional
     * tolerances of each additional equation.
     */
    private final Map<String, double[]> addStatesAbsoluteTolerances;

    /**
     * User-specified relative tolerances for additional states. Built from the additional
     * tolerances of each additional equation.
     */
    private final Map<String, double[]> addStatesRelativeTolerances;

    /**
     * User-specified absolute tolerances for orbit.
     */
    private final Map<String, double[]> orbitAbsoluteTolerances;

    /**
     * User-specified relative tolerances for orbit.
     */
    private final Map<String, double[]> orbitRelativeTolerances;

    /**
     * Default orbit absolute tolerance array given to the integrator.
     */
    private double[] orbitDefaultAbsoluteTolerance;

    /**
     * Default orbit relative tolerance array given to the integrator.
     */
    private double[] orbitDefaultRelativeTolerance;

    /**
     * Create a new instance of MultiNumericalPropagator. After creation, the instance is empty,
     * i.e. there are no forces at all, not even the Newtonian gravitational one. The defaults are
     * {@link OrbitType#EQUINOCTIAL} for {@link #orbitType propagation orbit type} and {@link PositionAngle#TRUE} for
     * {@link #angleType position angle type}.
     *
     * <p>
     * The new instance of MultiNumericalPropagator is declared as an {@link Observer observer} of the contained
     * integrator. As observer, it gets notified if a detector is deleted in the integrator sub-layer, to update its own
     * detectors list.
     * </p>
     *
     * @param integratorIn numerical integrator to use for propagation
     * @throws PatriusException thrown if the given propagation frames are not all pseudo-inertial
     */
    public MultiNumericalPropagator(final FirstOrderIntegrator integratorIn) throws PatriusException {
        this(integratorIn, new HashMap<String, Frame>());
    }

    /**
     * Create a new instance of MultiNumericalPropagator. After creation, the instance is empty,
     * i.e. there are no forces at all, not even the Newtonian gravitational one. The defaults are
     * {@link OrbitType#EQUINOCTIAL} for {@link #orbitType propagation orbit type} and {@link PositionAngle#TRUE} for
     * {@link #angleType position angle type}.
     *
     * <p>
     * The new instance of MultiNumericalPropagator is declared as an {@link Observer observer} of the contained
     * integrator. As observer, it gets notified if a detector is deleted in the integrator sub-layer, to update its own
     * detectors list.
     * </p>
     *
     * @param integratorIn numerical integrator to use for propagation
     * @param propagationFrameMapIn the map of the frames used for the propagation for each spacecraft
     * @throws PatriusException thrown if the given propagation frames are not all pseudo-inertial
     */
    public MultiNumericalPropagator(final FirstOrderIntegrator integratorIn,
                                    final Map<String, Frame> propagationFrameMapIn) throws PatriusException {
        this(integratorIn, propagationFrameMapIn, OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
    }

    /**
     * Create a new instance of MultiNumericalPropagator. After creation, the instance is empty,
     * i.e. there are no forces at all, not even the Newtonian gravitational one.
     *
     * <p>
     * The new instance of MultiNumericalPropagator is declared as an {@link Observer observer} of the contained
     * integrator. As observer, it gets notified if a detector is deleted in the integrator sub-layer, to update its own
     * detectors list.
     * </p>
     *
     * @param integratorIn numerical integrator to use for propagation
     * @param propagationFrameMapIn the map of the frames used for the propagation for each spacecraft
     * @param orbitTypeIn the orbital parameters type
     * @param angleTypeIn the position angle type
     * @throws PatriusException thrown if the given propagation frames are not all not pseudo-inertial
     */
    public MultiNumericalPropagator(final FirstOrderIntegrator integratorIn,
                                    final Map<String, Frame> propagationFrameMapIn,
                                    final OrbitType orbitTypeIn, final PositionAngle angleTypeIn)
        throws PatriusException {

        // Check that the propagation frames are all pseudo-inertial
        final Collection<Frame> frames = propagationFrameMapIn.values();
        boolean allPseudoInertial = true;
        for (final Frame frame : frames) {
            if (!frame.isPseudoInertial()) {
                allPseudoInertial = false;
                break;
            }
        }
        if (allPseudoInertial) {
            this.propagationFrameMap = propagationFrameMapIn;
        } else {
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
        }
        // Check if the given integrator is null
        if (integratorIn != null) {
            this.multiIntegrator = integratorIn;
        } else {
            throw new PropagationException(
                PatriusMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION);
        }
        this.forceModels = new HashMap<>();
        this.multiDetectors = new ArrayList<>();
        this.startDate = null;
        this.referenceDate = null;
        this.currentStates = new HashMap<>();
        this.addEquationsAndTolerances = new HashMap<>();
        this.attitudeProvidersForces = new HashMap<>();
        this.attitudeProvidersEvents = new HashMap<>();
        this.attitudeProvidersByDefault = new HashMap<>();
        this.stateVector = new double[SpacecraftState.ORBIT_DIMENSION];
        this.stateVector = new double[SpacecraftState.ORBIT_DIMENSION];
        // Store default tolerance array given to the integrator
        if (integratorIn instanceof AdaptiveStepsizeIntegrator) {
            this.orbitDefaultAbsoluteTolerance = storeAbsoluteDefaultTolerance(
                    (AdaptiveStepsizeIntegrator) integratorIn);
            this.orbitDefaultRelativeTolerance = storeRelativeDefaultTolerance(
                    (AdaptiveStepsizeIntegrator) integratorIn);
        }
        this.setSlaveMode();
        this.orbitType = orbitTypeIn;
        this.angleType = angleTypeIn;
        this.addStatesAbsoluteTolerances = new HashMap<>();
        this.addStatesRelativeTolerances = new HashMap<>();
        this.orbitAbsoluteTolerances = new HashMap<>();
        this.orbitRelativeTolerances = new HashMap<>();
        this.initialStates = new LinkedHashMap<>();
        this.muMap = new HashMap<>();
        this.massMultiModelHandlers = new HashMap<>();
        this.nullMassDetectors = new HashMap<>();
        if (this.multiIntegrator instanceof AbstractIntegrator) {
            ((AbstractIntegrator) this.multiIntegrator).addObserver(this);
        }
    }

    /**
     * Store absolute default orbit tolerances from the integrator.
     *
     * @param integ integrator
     * @return the object internal array
     */
    private static double[] storeAbsoluteDefaultTolerance(final AdaptiveStepsizeIntegrator integ) {
        // try to get the vector error field of the AdaptiveStepsizeIntegrator class:
        final double[] originalArray;
        if (integ.getVecAbsoluteTolerance() == null) {
            // The absolute and relative state tolerances are represented by a scalar value:
            final int originalSize = 6;
            originalArray = new double[originalSize];
            // create a 6-components array containing the tolerance scalar value
            for (int i = 0; i < originalSize; i++) {
                originalArray[i] = integ.getScalAbsoluteTolerance();
            }
        } else {
            // the absolute and relative tolerances are represented by an array:
            originalArray = integ.getVecAbsoluteTolerance();
        }
        return originalArray;
    }

    /**
     * Store relative default orbit tolerances from the integrator.
     *
     * @param integ integrator
     * @return the object internal array
     */
    private static double[] storeRelativeDefaultTolerance(final AdaptiveStepsizeIntegrator integ) {
        // try to get the vector error field of the AdaptiveStepsizeIntegrator class:
        final double[] originalArray;
        if (integ.getVecRelativeTolerance() == null) {
            // The absolute and relative state tolerances are represented by a scalar value:
            final int originalSize = 6;
            originalArray = new double[originalSize];
            // create a 6-components array containing the tolerance scalar value
            for (int i = 0; i < originalSize; i++) {
                originalArray[i] = integ.getScalRelativeTolerance();
            }
        } else {
            // the absolute and relative tolerances are represented by an array:
            originalArray = integ.getVecRelativeTolerance();
        }
        return originalArray;
    }

    /**
     * Check if the input spacecraft ID is included in initial states map.
     *
     * @param satId the spacecraft ID.
     *
     */
    private void checkSatId(final String satId) {
        if (!this.initialStates.containsKey(satId)) {
            throw PatriusException.createIllegalStateException(
                PatriusMessages.PDB_UNDEFINED_STATE_ID, satId);
        }
    }

    /**
     * Set the orbit tolerance of a defined state.
     *
     * @param absoluteTolerance the orbit absolute tolerance of the specified spacecraft.
     * @param relativeTolerance the orbit relative tolerance of the specified spacecraft.
     * @param satId the spacecraft ID.
     * @throws PatriusException The length of the input orbit tolerance is different from 6
     */
    public void setOrbitTolerance(final double[] absoluteTolerance,
                                  final double[] relativeTolerance, final String satId) throws PatriusException {
        this.checkSatId(satId);
        if ((absoluteTolerance.length != SpacecraftState.ORBIT_DIMENSION)
                || (relativeTolerance.length != SpacecraftState.ORBIT_DIMENSION)) {
            throw new PatriusException(PatriusMessages.PDB_ORBIT_TOLERENCE_LENGTH);
        }
        this.orbitAbsoluteTolerances.put(satId, absoluteTolerance);
        this.orbitRelativeTolerances.put(satId, relativeTolerance);
    }

    /**
     * Get the central attraction coefficient &mu;.
     *
     * @param satId the spacecraft ID.
     * @return mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public double getMu(final String satId) {
        this.checkSatId(satId);
        return this.initialStates.get(satId).getMu();
    }

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProvider(final String satId) {
        this.checkSatId(satId);
        MultiAttitudeProvider attProv = null;
        // If two attitude providers were provided
        if (!this.attitudeProvidersByDefault.containsKey(satId)
                || (this.attitudeProvidersByDefault.get(satId) == null)) {
            if ((this.attitudeProvidersForces.containsKey(satId))
                    || (this.attitudeProvidersForces.get(satId) != null)) {
                attProv = this.attitudeProvidersForces.get(satId);
            } else if (this.attitudeProvidersEvents.containsKey(satId)) {
                attProv = this.attitudeProvidersEvents.get(satId);
            }
        } else {
            // If single attitude provider
            attProv = this.attitudeProvidersByDefault.get(satId);
        }
        return attProv;
    }

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProviderForces(final String satId) {
        this.checkSatId(satId);
        if (this.attitudeProvidersForces.containsKey(satId)) {
            return this.attitudeProvidersForces.get(satId);
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public MultiAttitudeProvider getAttitudeProviderEvents(final String satId) {
        this.checkSatId(satId);
        if (this.attitudeProvidersEvents.containsKey(satId)) {
            return this.attitudeProvidersEvents.get(satId);
        }
        return null;
    }

    /**
     * Check if a single attitude treatment is expected.
     *
     * @param satId the spacecraft ID.
     * @throws IllegalStateException If an attitude provider or an additional equation is already
     *         defined for a specific attitude (for events or forces computation)
     */
    private void checkSingleAttitudeTreatment(final String satId) {
        this.checkSatId(satId);
        this.checkSingleAttitudeTreatmentAttProv(satId);
        this.checkSingleAttitudeTreatmentEquations(satId);
    }

    /**
     * Check if a single attitude treatment is expected in attitude provider definitions.
     *
     * @param satId the spacecraft ID.
     * @throws IllegalStateException If an attitude provider is already defined for a specific
     *         attitude (for events or forces computation)
     */
    private void checkSingleAttitudeTreatmentAttProv(final String satId) {
        final MultiAttitudeProvider attitudeProviderForces = this.attitudeProvidersForces
            .containsKey(satId) ? this.attitudeProvidersForces.get(satId) : null;
        final MultiAttitudeProvider attitudeProviderEvents = this.attitudeProvidersEvents
            .containsKey(satId) ? this.attitudeProvidersEvents.get(satId) : null;
        if ((attitudeProviderForces != null) || (attitudeProviderEvents != null)) {
            throw PatriusException.createIllegalStateException(
                PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
        }
    }

    /**
     * Check if a single attitude treatment is expected in attitude equation definitions.
     *
     * @param satId the spacecraft ID.
     * @throws IllegalStateException If an additional equation is already defined for a specific
     *         attitude (for events or forces computation)
     */
    private void checkSingleAttitudeTreatmentEquations(final String satId) {
        if (this.addEquationsAndTolerances.containsKey(satId)) {
            final List<AdditionalEquationsAndTolerances> eqs = this.addEquationsAndTolerances.get(satId);
            for (int i = 0; i < eqs.size(); i++) {
                final String name = eqs.get(i).getEquations().getName();
                if ((name.equals(AttitudeType.ATTITUDE_FORCES.toString()))
                        || (name.equals(AttitudeType.ATTITUDE_EVENTS.toString()))) {
                    throw PatriusException.createIllegalStateException(
                        PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
                }
            }
        }
    }

    /**
     * Check if a two attitude treatment is expected.
     *
     * @param satId the spacecraft ID.
     * @throws IllegalStateException If an attitude provider or an additional equation is already
     *         defined by default for a single attitude
     */
    private void checkTwoAttitudesTreatment(final String satId) {
        // Check sat ID existence
        this.checkSatId(satId);
        final MultiAttitudeProvider attitudeProviderByDefault = this.attitudeProvidersByDefault
            .containsKey(satId) ? this.attitudeProvidersByDefault.get(satId) : null;
        if (attitudeProviderByDefault != null) {
            // There is already an attitude law
            throw PatriusException.createIllegalStateException(
                PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        if (this.addEquationsAndTolerances.containsKey(satId)) {
            // Additional equations
            final List<AdditionalEquationsAndTolerances> eqs = this.addEquationsAndTolerances.get(satId);
            for (int i = 0; i < eqs.size(); i++) {
                final String name = eqs.get(i).getEquations().getName();
                if (name.equals(AttitudeType.ATTITUDE.toString())) {
                    throw PatriusException.createIllegalStateException(
                        PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
                }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider, final String satId) {
        this.setAttitudeProvider(new MultiAttitudeProviderWrapper(attitudeProvider, satId), satId);
    }

    /**
     * Set attitude provider.
     *
     * @param attitudeProvider attitude provider
     * @param satId satellite ID for attitude provider
     */
    public void setAttitudeProvider(final MultiAttitudeProvider attitudeProvider,
                                    final String satId) {
        this.checkSingleAttitudeTreatment(satId);
        if (this.addEquationsAndTolerances.containsKey(satId)) {
            final List<AdditionalEquationsAndTolerances> eqs = this.addEquationsAndTolerances.get(satId);
            for (int i = 0; i < eqs.size(); i++) {
                if (eqs.get(i).getEquations().getName().equals(AttitudeType.ATTITUDE.toString())) {
                    // An additional equation is already defined for this Attitude
                    throw PatriusException.createIllegalStateException(
                        PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
                }
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProvidersByDefault.put(satId, attitudeProvider);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attitudeProviderForces,
                                          final String satId) {
        this.setAttitudeProviderForces(new MultiAttitudeProviderWrapper(attitudeProviderForces, satId),
            satId);
    }

    /**
     * Set attitude provider for forces.
     *
     * @param attitudeProviderForces attitude provider for forces
     * @param satId satellite ID for attitude provider
     */
    public void setAttitudeProviderForces(final MultiAttitudeProvider attitudeProviderForces,
                                          final String satId) {
        this.checkTwoAttitudesTreatment(satId);
        if (this.addEquationsAndTolerances.containsKey(satId)) {
            final List<AdditionalEquationsAndTolerances> eqs = this.addEquationsAndTolerances.get(satId);
            for (int i = 0; i < eqs.size(); i++) {
                if (eqs.get(i).getEquations().getName()
                    .equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                    // An additional equation is already defined for this Attitude
                    throw PatriusException.createIllegalStateException(
                        PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
                }
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProvidersForces.put(satId, attitudeProviderForces);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attitudeProviderEvents,
                                          final String satId) {
        this.setAttitudeProviderForces(new MultiAttitudeProviderWrapper(attitudeProviderEvents, satId),
            satId);
    }

    /**
     * Set attitude provider for events.
     *
     * @param attitudeProviderEvents attitude provider for events
     * @param satId satellite ID for attitude provider
     */
    public void setAttitudeProviderEvents(final MultiAttitudeProvider attitudeProviderEvents,
                                          final String satId) {
        this.checkTwoAttitudesTreatment(satId);
        if (this.addEquationsAndTolerances.containsKey(satId)) {
            final List<AdditionalEquationsAndTolerances> eqs = this.addEquationsAndTolerances.get(satId);
            for (int i = 0; i < eqs.size(); i++) {
                if (eqs.get(i).getEquations().getName()
                    .equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                    // An additional equation is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
                }
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProvidersEvents.put(satId, attitudeProviderEvents);
    }

    /** {@inheritDoc} */
    @Override
    public void addEventDetector(final MultiEventDetector detector) {
        this.multiDetectors.add(detector);
    }

    /** {@inheritDoc} */
    @Override
    public void addEventDetector(final EventDetector detector, final String satId) {
        this.checkSatId(satId);
        final OneSatEventDetectorWrapper wrapper = new OneSatEventDetectorWrapper(detector, satId);
        this.multiDetectors.add(wrapper);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<MultiEventDetector> getEventsDetectors() {
        return Collections.unmodifiableCollection(this.multiDetectors);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventsDetectors() {
        this.multiDetectors.clear();
    }

    /**
     * Add a force model to the global model of a specific spacecraft.
     *
     * @param model {@link ForceModel} to add
     * @param satId the spacecraft ID.
     * @see #removeForceModels()
     * @see #setMu(double, String)
     */
    public void addForceModel(final ForceModel model, final String satId) {
        this.checkSatId(satId);
        if (!this.forceModels.containsKey(satId)) {
            final ArrayList<ForceModel> list = new ArrayList<>();
            this.forceModels.put(satId, list);
        }
        this.getForceModels(satId).add(model);
    }

    /**
     * Remove all force models from the global model.
     *
     * @see #addForceModel(ForceModel, String)
     */
    public void removeForceModels() {
        this.forceModels.clear();
    }

    /**
     * Get force models list.
     *
     * @param satId the spacecraft ID.
     * @return list of force models
     * @see #addForceModel(ForceModel, String)
     */
    public List<ForceModel> getForceModels(final String satId) {
        this.checkSatId(satId);
        return this.forceModels.get(satId);
    }

    /** {@inheritDoc} */
    @Override
    public int getMode() {
        return this.mode;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame(final String satId) {
        this.checkSatId(satId);
        return this.propagationFrameMap.get(satId);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #MultiNumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     *
     * @see fr.cnes.sirius.patrius.propagation.MultiPropagator#setSlaveMode()
     */
    @Override
    public void setSlaveMode() {
        this.masterMultiModeHandler = null;
        this.ephemerisMultiModeHandler = null;
        this.mode = SLAVE_MODE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #MultiNumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     *
     * @see #setMasterMode(double, MultiPatriusFixedStepHandler)
     */
    @Override
    public void setMasterMode(final double h, final MultiPatriusFixedStepHandler handler) {
        this.setMasterMode(new MultiPatriusStepNormalizer(h, handler));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #MultiNumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     *
     * @see fr.cnes.sirius.patrius.propagation.MultiPropagator #setMasterMode(MultiPatriusStepHandler)
     */
    @Override
    public void setMasterMode(final MultiPatriusStepHandler handler) {
        this.ephemerisMultiModeHandler = null;
        this.masterMultiModeHandler = new MultiAdaptedStepHandler(handler);
        this.mode = MASTER_MODE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #MultiNumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     *
     * @see fr.cnes.sirius.patrius.propagation.MultiPropagator#setEphemerisMode()
     */
    @Override
    public void setEphemerisMode() {
        this.masterMultiModeHandler = null;
        this.ephemerisMultiModeHandler = new MultiEphemerisModeHandler();
        this.mode = EPHEMERIS_GENERATION_MODE;
    }

    /**
     * Set the MassModel handlers
     *
     * <p>
     * Note that for now the massModel has to be initiate before the slave/master/ephemeris mode
     * </p>
     */
    private void setMassMultiModelHandlers() {
        if (this.massMultiModelHandlers != null) {
            final Collection<MultiAdaptedStepHandler> mashVal = this.massMultiModelHandlers.values();
            for (final MultiAdaptedStepHandler mash : mashVal) {
                this.multiIntegrator.addStepHandler(mash);
            }
        }
    }

    /**
     * Get propagation parameter type.
     *
     * @return orbit type used for propagation
     */
    public OrbitType getOrbitType() {
        return this.orbitType;
    }

    /**
     * Get propagation parameter type.
     *
     * @return angle type to use for propagation
     */
    public PositionAngle getPositionAngleType() {
        return this.angleType;
    }

    /** {@inheritDoc} */
    @Override
    public BoundedPropagator getGeneratedEphemeris(final String satId) {
        if (this.mode != EPHEMERIS_GENERATION_MODE) {
            throw PatriusException
                .createIllegalStateException(PatriusMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return this.ephemerisMultiModeHandler.getEphemeris(satId);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> getInitialStates() {
        return this.initialStates;
    }

    /**
     * Remove the initial state with the given satId from the map of the initial states.
     *
     * @param satId satellite ID of the initial state to remove
     */
    public void removeInitialState(final String satId) {
        this.initialStates.values().remove(this.initialStates.get(satId));
    }

    /** {@inheritDoc} */
    @Override
    public void addInitialState(final SpacecraftState initialState, final String satId)
        throws PatriusException {
        // Check sat ID is not null or empty
        if (satId == null || satId.isEmpty()) {
            throw new PatriusException(PatriusMessages.PDB_NULL_STATE_ID);
        }
        if (this.initialStates.isEmpty()) {
            this.startDate = initialState.getDate();
        } else {
            // Check sat ID is not already used
            if (this.initialStates.containsKey(satId)) {
                throw new PatriusException(PatriusMessages.PDB_SAT_ID_ALREADY_USED, satId);
            }
            // Check date correspondence
            if (!this.startDate.equals(initialState.getDate())) {
                throw new PatriusException(PatriusMessages.PDB_MULTI_SAT_DATE_MISMATCH,
                    initialState.getDate(), this.startDate);
            }
        }
        this.initialStates.put(satId, initialState);
        if (!this.propagationFrameMap.containsKey(satId)) {
            this.propagationFrameMap.put(satId, null);
        }
    }

    /**
     * Select additional state and equations pair in the list.
     *
     * @param addStateName name of the additional equations to select
     * @param satId the spacecraft ID.
     * @return additional state and equations pair
     * @throws PatriusException if additional equation is unknown
     */
    private AdditionalEquationsAndTolerances selectEquationsAndTolerances(final String addStateName,
                                                                          final String satId) throws PatriusException {
        this.checkSatId(satId);
        for (final AdditionalEquationsAndTolerances equAndTolerances : this.addEquationsAndTolerances
            .get(satId)) {
            if (equAndTolerances.getEquations().getName().equals(addStateName)) {
                return equAndTolerances;
            }
        }
        throw new PatriusException(PatriusMessages.UNKNOWN_ADDITIONAL_EQUATION, addStateName);
    }

    /**
     * Add a set of user-specified equations to be integrated along with the orbit propagation.
     *
     * @param addEqu additional equations
     * @param satId the spacecraft ID.
     * @see SpacecraftState#addAdditionalState(String, double[])
     */
    public void addAdditionalEquations(final AdditionalEquations addEqu, final String satId) {

        // if any additional equations are defined for this specific spacecraft, then create a new
        // additional equations list.
        if (!this.addEquationsAndTolerances.containsKey(satId)) {
            final List<AdditionalEquationsAndTolerances> newSatList = new ArrayList<>();
            this.addEquationsAndTolerances.put(satId, newSatList);
        }

        // add equation
        this.addEquationsAndTolerances.get(satId).add(new AdditionalEquationsAndTolerances(addEqu));
    }

    /**
     * Add a set of user-specified attitude equations to be integrated along with the orbit
     * propagation. If the set of attitude equations is already registered for the current attitude,
     * it is replaced by the new one.
     *
     * @param addEqu attitude additional equations
     * @param satId the spacecraft ID.
     */
    public void addAttitudeEquation(final AttitudeEquation addEqu, final String satId) {
        this.checkSatId(satId);
        final AttitudeType type = addEqu.getAttitudeType();
        final MultiAttitudeProvider attitudeProviderForces = this.attitudeProvidersForces
            .containsKey(satId) ? this.attitudeProvidersForces.get(satId) : null;
        final MultiAttitudeProvider attitudeProviderEvents = this.attitudeProvidersEvents
            .containsKey(satId) ? this.attitudeProvidersEvents.get(satId) : null;
        final MultiAttitudeProvider attitudeProviderByDefault = this.attitudeProvidersByDefault
            .containsKey(satId) ? this.attitudeProvidersByDefault.get(satId) : null;

        // check the attitude type to be integrated
        switch (type) {
            case ATTITUDE_FORCES:
                this.checkTwoAttitudesTreatment(satId);
                if (attitudeProviderForces != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE_EVENTS:
                this.checkTwoAttitudesTreatment(satId);
                if (attitudeProviderEvents != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE:
                this.checkSingleAttitudeTreatment(satId);
                if (attitudeProviderByDefault != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            default:
                throw new PatriusRuntimeException(PatriusMessages.UNKNOWN_PARAMETER, null);
        }
        // add additional equation in numerical propagator
        this.addAdditionalEquations(addEqu, satId);
    }

    /**
     * Add additional equations associated with the mass provider. A null-mass detector associated
     * with the input mass provider is automatically added.
     * <p>
     * Note that this method should be called after {@link #setSlaveMode()} or
     * {@link #setMasterMode(MultiPatriusStepHandler)} or {@link #setEphemerisMode()} since this method reset the
     * integrator step handlers list.
     * </p>
     *
     * <p>
     * <b>WARNING</b>: This method should be called only once and provided mass provider should be the same used for
     * force models.
     * </p>
     *
     * @param massProvider the mass provider
     * @param satId the spacecraft ID.
     */
    public void setMassProviderEquation(final MassProvider massProvider, final String satId) {
        this.checkSatId(satId);

        // Add mass equations
        final List<String> allPartsName = massProvider.getAllPartsNames();
        final int size = allPartsName.size();
        for (int i = 0; i < size; i++) {
            this.addAdditionalEquations(massProvider.getAdditionalEquation(allPartsName.get(i)), satId);
        }

        // Add null mass detector (in first position since it leads to stop propagation,
        // in case of event occurring at the same time)
        this.nullMassDetectors.put(satId, new NullMassDetector(massProvider));

        // Add a step handler that update mass model at the end of state
        this.massMultiModelHandlers.put(satId, new MultiAdaptedStepHandler(
            new MultiPatriusStepHandler(){

                /** Serializable UID. */
                private static final long serialVersionUID = -5030643277757352867L;

                /** {@inheritDoc} */
                @Override
                public void init(final Map<String, SpacecraftState> s0, final AbsoluteDate t) {
                    // do nothing
                }

                /** {@inheritDoc} */
                @Override
                public void handleStep(final MultiPatriusStepInterpolator interpolator,
                                       final boolean isLast) throws PropagationException {
                    // Performed only at the end of propagation.
                    // Mass provider is kept updated

                    final List<String> allPartsName = massProvider.getAllPartsNames();
                    final int size = allPartsName.size();
                    for (int i = 0; i < size; i++) {
                        final String partName = allPartsName.get(i);
                        try {
                            if (MultiNumericalPropagator.this.nullMassDetectors.get(satId).isTriggered()) {
                                // Particular case: total mass is 0, but due to numerical
                                // quality issues part mass may be a little below 0 (-1E-15 for example)
                                // Here we avoid to raise an exception
                                massProvider.updateMass(partName, 0);
                            } else {
                                // A part mass may be a little below 0 : update it to 0 in this case
                                final double additionalState = interpolator
                                    .getInterpolatedStates()
                                    .get(satId)
                                    .getAdditionalState(SpacecraftState.MASS + partName)[0];
                                if (additionalState < 0.0) {
                                    massProvider.updateMass(partName, 0.0);
                                } else {
                                    massProvider.updateMass(partName, additionalState);
                                }
                            }
                        } catch (final PatriusException e) {
                            throw new PropagationException(e,
                                PatriusMessages.NOT_POSITIVE_MASS);
                        }
                    }
                }
            }));
    }

    /**
     * Add additional state tolerances.
     *
     * @param name the additional state name
     * @param absTol absolute tolerances
     * @param relTol relative tolerances
     * @param satId the spacecraft ID.
     * @throws PatriusException if additional equation associated with the input additional state
     *         name is unknown
     */
    public void setAdditionalStateTolerance(final String name, final double[] absTol,
                                            final double[] relTol, final String satId) throws PatriusException {
        this.checkSatId(satId);
        // Set additional tolerances
        this.selectEquationsAndTolerances(name, satId).setTolerances(absTol, relTol);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> propagate(final AbsoluteDate target)
        throws PropagationException {
        try {
            if (this.initialStates.isEmpty()) {
                // Exception
                throw new PropagationException(
                    PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }
            // startDate value is already initialized
            return this.propagate(this.startDate, target);
        } catch (final PatriusException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            throw new PropagationException(oe);

        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, SpacecraftState> propagate(final AbsoluteDate start,
                                                  final AbsoluteDate target) throws PropagationException {
        try {

            if (this.initialStates.isEmpty()) {
                throw new PropagationException(
                    PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }

            if (!start.equals(this.startDate)) {
                // if propagation start date is not initial date,
                // propagate from initial to start date without event detection
                this.propagate(start, false);
                // Detectors should be entirely built next time
            }

            // propagate from start date to end date with event detection
            return this.propagate(target, true);

        } catch (final PatriusException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            throw new PropagationException(oe);

        }
    }

    /**
     * Propagation with or without event detection.
     *
     * @param tEnd target date to which orbit should be propagated
     * @param activateHandlers if true, step and event handlers should be activated
     * @return states at end of propagation
     * @exception PropagationException if orbit cannot be propagated
     */
    // CHECKSTYLE: stop CyclomaticComplexity check
    // CHECKSTYLE: stop MethodLength check
    @SuppressWarnings({ "PMD.AvoidRethrowingException", "PMD.UseConcurrentHashMap" })
    private Map<String, SpacecraftState> propagate(final AbsoluteDate tEnd,
                                                   final boolean activateHandlers) throws PropagationException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        try {
            this.manageStateFrame();
            if (this.startDate.getDate().equals(tEnd)) {
                // don't extrapolate
                return this.initialStates;
            }

            // Copy the list of step handlers into the integrator before propagation
            // Mass model handler is set in first position to ensure all other handlers will have
            // synchronized masses
            this.multiIntegrator.clearStepHandlers();
            this.setMassMultiModelHandlers();

            if (this.ephemerisMultiModeHandler != null) {
                this.multiIntegrator.addStepHandler(this.ephemerisMultiModeHandler);
                this.ephemerisMultiModeHandler.setForward(tEnd.durationFrom(this.startDate) >= 0);
            }

            if (this.masterMultiModeHandler != null) {
                this.multiIntegrator.addStepHandler(this.masterMultiModeHandler);
            }

            // space dynamics view
            this.referenceDate = this.startDate;

            this.addAttitudeToAddStatesMap();

            final Map<String, MultiAttitudeProvider> attitudeProvidersForcesLocal = new HashMap<>();
            final Map<String, MultiAttitudeProvider> attitudeProvidersEventsLocal = new HashMap<>();
            this.storeLocalAttitudeProviders(attitudeProvidersForcesLocal,
                attitudeProvidersEventsLocal);

            // Check correspondence between additional states and additional equations
            this.checkStatesEquations();

            this.currentStates = (Map<String, SpacecraftState>)
                ((LinkedHashMap<String, SpacecraftState>) this.initialStates).clone();

            // Get additional states infos
            this.stateVectorInfo = new MultiStateVectorInfo(this.currentStates, this.muMap, this.propagationFrameMap);

            // Specific handling of Cowell integrator
            if (this.multiIntegrator instanceof CowellIntegrator) {
                ((CowellIntegrator) this.multiIntegrator).setMapper(new SecondOrderMapper(
                    this.addEquationsAndTolerances, this.stateVectorInfo.getIdList()));
                if (!this.orbitType.equals(OrbitType.CARTESIAN)) {
                    // Cowell integration must be performed in cartesian coordinates
                    throw new PropagationException(PatriusMessages.COWELL_REQUIRES_CARTESIAN_COORDINATES);
                }
            }

            this.storeMuFrame();

            // initialize event handlers
            this.initializeEventHandlers(activateHandlers, attitudeProvidersForcesLocal,
                attitudeProvidersEventsLocal);

            // creating state vector
            this.stateVector = new double[this.stateVectorInfo.getStateVectorSize()];

            // mathematical view
            final double t0 = 0;
            double t1 = tEnd.preciseDurationFrom(this.startDate);

            final AbsoluteDate t = this.startDate.shiftedBy(t1);
            double dt = tEnd.preciseDurationFrom(t);
            // Avoid overshoot
            if (dt != 0.) {
                if ((t1 > 0.) ^ (dt > 0.)) {
                    t1 = MathLib.nextAfter(t1, 0.);
                }
                if (this.mode == MASTER_MODE) {
                    this.multiIntegrator.handleLastStep(false);
                }
            }

            // Map state to array
            this.stateVectorInfo.mapStatesToArray(this.currentStates, this.orbitType, this.angleType, this.stateVector);

            // Add event handlers
            // Wrap all Orekit event detector and register it to the integrator.
            this.addEventHandlers(activateHandlers, attitudeProvidersForcesLocal,
                attitudeProvidersEventsLocal);

            // mathematical integration
            if (!this.addEquationsAndTolerances.isEmpty()) {
                this.buildAdditionalTolerances();
            }
            this.expandToleranceArray();

            final DifferentialEquations diffEq = new DifferentialEquations(
                attitudeProvidersForcesLocal, attitudeProvidersEventsLocal);
            double stopTime;
            AbsoluteDate date = this.startDate;

            try {
                stopTime = this.multiIntegrator.integrate(diffEq, t0, this.stateVector, t1, this.stateVector);
                date = this.startDate.shiftedBy(stopTime);

                if (!date.equals(tEnd) && t1 == stopTime) {
                    // Propagation went to the end (no stop event) but final date is not strictly equal to
                    // expected final date (due to round-off errors)
                    // Propagate again to match exact expected date
                    // This correction is currently not supported in ephemeris mode

                    // Compute dt on which to propagate (addition order is important!)
                    dt = tEnd.preciseDurationFrom(date);

                    // Activate last step to inform user this is the last step
                    this.multiIntegrator.handleLastStep(true);

                    // 2nd initialization
                    this.referenceDate = date;
                    this.addEventHandlers(activateHandlers, attitudeProvidersForcesLocal,
                        attitudeProvidersEventsLocal);

                    if (this.masterMultiModeHandler != null) {
                        this.masterMultiModeHandler.setReference(this.referenceDate);
                    }

                    if (this.ephemerisMultiModeHandler != null) {
                        this.ephemerisMultiModeHandler.setReference(this.referenceDate);
                    }

                    if (this.massMultiModelHandlers != null) {
                        for (final Map.Entry<String, MultiAdaptedStepHandler> entry : this.massMultiModelHandlers
                            .entrySet()) {
                            entry.getValue().setReference(this.referenceDate);
                        }
                    }

                    // 2nd integration
                    stopTime = this.multiIntegrator.integrate(diffEq, t0, this.stateVector, dt, this.stateVector);
                    date = date.shiftedBy(stopTime);
                }
            } catch (final PatriusExceptionWrapper oew) {
                throw oew.getException();
            }

            this.resetToleranceArray();

            // get final state
            this.initialStates.clear();
            this.initialStates = this.stateVectorInfo.mapArrayToStates(this.stateVector, date, this.orbitType,
                this.angleType, attitudeProvidersForcesLocal, attitudeProvidersEventsLocal, this.muMap,
                this.propagationFrameMap);
            this.startDate = date;
            return this.initialStates;

        } catch (final PropagationException pe) {
            throw pe;
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        }
    }

    /**
     * Manage the state frame : the orbit to propagate is converted in the propagation frame.
     *
     * @throws PatriusException if the frame of the initial state is not inertial or pseudo-inertial
     */
    private void manageStateFrame() throws PatriusException {

        // Loop on all initial states
        for (final Entry<String, SpacecraftState> entry : this.initialStates.entrySet()) {
            final String key = entry.getKey();

            final SpacecraftState state = this.initialStates.get(key);
            final Frame propagationFrame = this.propagationFrameMap.get(key);

            if (this.propagationFrameMap.get(key) == null) {
                // Propagation frame has not been provided: frame used is orbit frame is inertial
                // or pseudo-inertial
                if (this.initialStates.get(key).getFrame().isPseudoInertial()) {
                    this.propagationFrameMap.put(key, this.initialStates.get(key).getFrame());
                } else {
                    // At least on propagation frame is not inertial
                    throw new PatriusException(PatriusMessages.PDB_NOT_INERTIAL_FRAME);
                }
            } else {
                // Propagation frame has been provided: convert initial state in propagation frame
                if (state.getFrame() != propagationFrame) {
                    final Orbit initOrbit = state.getOrbit();
                    final OrbitType type = initOrbit.getType();
                    final Orbit propagationOrbit = type.convertOrbit(initOrbit, propagationFrame);

                    // Update attitude in right frame
                    Attitude propagationAttitude = null;
                    if (state.getAttitude() != null) {
                        propagationAttitude = state.getAttitude().withReferenceFrame(
                            propagationFrame);
                    }
                    Attitude propagationAttitudeEvents = null;
                    if (state.getAttitudeEvents() != null) {
                        propagationAttitudeEvents = state.getAttitudeEvents().withReferenceFrame(
                            propagationFrame);
                    }
                    final SpacecraftState newState = new SpacecraftState(propagationOrbit, propagationAttitude,
                            propagationAttitudeEvents,
                        state.getAdditionalStates());

                    this.initialStates.put(key, newState);
                }
            }
        }
    }

    /**
     * Add attitude to additional state map in initial SpacecraftState.
     *
     * @throws PatriusException if no attitude information is defined if additional states map
     *         contains (ATTITUDE_FORCES or ATTITUDE_EVENTS) state and ATTITUDE state
     */
    private void addAttitudeToAddStatesMap() throws PatriusException {
        // Loop on all states
        for (final Entry<String, SpacecraftState> entry : this.initialStates.entrySet()) {
            final String satId = entry.getKey();
            //
            if (this.addEquationsAndTolerances.containsKey(satId)) {
                // Get additional equations associated with the current state
                final List<AdditionalEquationsAndTolerances> addEqTols = this.addEquationsAndTolerances
                    .get(satId);
                for (int i = 0; i < addEqTols.size(); i++) {
                    // Add attitude to additional state map in initial SpacecraftState
                    final String name = addEqTols.get(i).getEquations().getName();
                    this.addAttitudeToAddStatesMap(name, satId, entry.getValue());
                }
            }
        }
    }

    /**
     * Add attitude to additional state map in initial SpacecraftState.
     *
     * @param name additional state name
     * @param satId spacecraft id
     * @param state spacecraft
     * @throws PatriusException if no attitude information is defined if additional states map
     *         contains (ATTITUDE_FORCES or ATTITUDE_EVENTS) state and ATTITUDE state
     */
    private void addAttitudeToAddStatesMap(final String name, final String satId,
                                           final SpacecraftState state) throws PatriusException {
        // Add attitude to additional state map in initial SpacecraftState
        if (name.equals(AttitudeType.ATTITUDE.toString())) {
            this.initialStates.put(satId, state.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE));
        } else {
            if (name.equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                this.initialStates.put(satId,
                    state.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES));
            }
            if (name.equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                this.initialStates.put(satId,
                    state.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS));
            }
        }
    }

    /**
     * Store local attitude providers.
     *
     * @precondition
     *
     * @param attitudeProvidersForcesLocal placeholder where to put local attitude providers for
     *        forces computation
     * @param attitudeProvidersEventsLocal placeholder where to put local attitude providers for
     *        events computation
     */
    private void storeLocalAttitudeProviders(
                                             final Map<String, MultiAttitudeProvider> attitudeProvidersForcesLocal,
                                             final Map<String, MultiAttitudeProvider> attitudeProvidersEventsLocal) {
        for (final Entry<String, SpacecraftState> entry : this.initialStates.entrySet()) {
            final String satId = entry.getKey();
            final MultiAttitudeProvider attProvDefault = this.attitudeProvidersByDefault
                .containsKey(satId) ? this.attitudeProvidersByDefault.get(satId) : null;
            if (attProvDefault == null) {
                final MultiAttitudeProvider attProvForces = this.attitudeProvidersForces
                    .containsKey(satId) ? this.attitudeProvidersForces.get(satId) : null;
                final MultiAttitudeProvider attProvEvents = this.attitudeProvidersEvents
                    .containsKey(satId) ? this.attitudeProvidersEvents.get(satId) : null;
                attitudeProvidersForcesLocal.put(satId, attProvForces);
                attitudeProvidersEventsLocal.put(satId, attProvEvents);
            } else {
                attitudeProvidersForcesLocal.put(satId, attProvDefault);
            }
        }
    }

    /**
     * Store MU and Frame for integration process.
     *
     */
    private void storeMuFrame() {
        // Get states informations
        final List<String> satIdList = this.stateVectorInfo.getIdList();
        final int sizeSatIdList = satIdList.size();

        // Store mu and frame
        for (int i = 0; i < sizeSatIdList; i++) {
            this.muMap.put(satIdList.get(i), this.initialStates.get(satIdList.get(i)).getMu());
            this.propagationFrameMap.put(satIdList.get(i), this.currentStates.get(satIdList.get(i))
                .getFrame());
        }

        // Update info with last mu and frame info
        this.stateVectorInfo = new MultiStateVectorInfo(this.currentStates, this.muMap, this.propagationFrameMap);
    }

    /**
     * Check correspondence between additional states and additional equations.
     *
     * @throws PatriusException if additional states from initial state and additional equations
     *         added in the propagator does not correspond
     */
    private void checkStatesEquations() throws PatriusException {
        for (final Entry<String, SpacecraftState> entry : this.initialStates.entrySet()) {
            final String satId = entry.getKey();
            // Get additional states from initial SpacecraftState
            final Map<String, double[]> additionalStates = entry.getValue().getAdditionalStates();
            if (this.addEquationsAndTolerances.containsKey(satId)) {
                this.checkStatesEquations(satId, additionalStates);
            } else {
                // No additional equations associated with the current state
                // Check no additional states are defined for the current state
                if (!additionalStates.isEmpty()) {
                    throw new PatriusException(
                        PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
                }
            }
        }
    }

    /**
     * Private method called by previous checkStatesEquations. Check correspondence between
     * additional states and additional equations.
     *
     * @param satId spacecraft id
     * @param additionalStates additional states from initial SpacecraftState
     * @throws PatriusException if additional states from initial state and additional equations
     *         added in the propagator does not correspond
     */
    private void checkStatesEquations(final String satId,
                                      final Map<String, double[]> additionalStates) throws PatriusException {
        // Get additional equations from addEquationsAndTolerances
        final List<AdditionalEquationsAndTolerances> additionalEquation = this.addEquationsAndTolerances
            .get(satId);
        if (!additionalEquation.isEmpty()) {
            // Check correspondence between additional states numbers and additional equations
            // numbers
            final int eqsSize = additionalEquation.size();
            if ((!additionalStates.isEmpty()) && (additionalStates.size() == eqsSize)) {
                // Check names correspondence
                for (int i = 0; i < eqsSize; i++) {
                    final String eqName = additionalEquation.get(i).getEquations().getName();
                    if (!additionalStates.containsKey(eqName)) {
                        throw new PatriusException(
                            PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
                    }
                }
            } else {
                throw new PatriusException(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
            }
        }
    }

    /**
     * Build the arrays of tolerances for all additional parameters.
     *
     * @throws PatriusException if tolerances array size differs from additional states vector size
     *
     */
    private void buildAdditionalTolerances() throws PatriusException {
        if (this.multiIntegrator instanceof AdaptiveStepsizeIntegrator) {
            final List<String> satIdList = this.stateVectorInfo.getIdList();
            final int sizeSatIdList = satIdList.size();
            for (int i = 0; i < sizeSatIdList; i++) {
                final String satId = satIdList.get(i);
                // Get additional equations from addEquationsAndTolerances
                if (this.addEquationsAndTolerances.containsKey(satId)) {
                    this.buildAdditionalTolerances(satId);
                }
            }
        }

        // Additional step: store in AdditionalEquationsAndTolerances objects pos in first/second order state vector
        for (final String id : MultiNumericalPropagator.this.stateVectorInfo.getIdList()) {
            final List<AdditionalEquationsAndTolerances> list = this.addEquationsAndTolerances.get(id);
            int index = 3;
            for (final Entry<String, double[]> addState : this.initialStates.get(id).getAdditionalStates()
                .entrySet()) {
                // Find corresponding equation
                for (final AdditionalEquationsAndTolerances stateAndTol : list) {
                    if (stateAndTol.getEquations().getName().equals(addState.getKey())) {
                        // Update index
                        stateAndTol.setIndex1stOrder(this.stateVectorInfo.getAddStatesInfos(id)
                            .get(stateAndTol.getEquations().getName()).getIndex());
                        stateAndTol.setIndex2ndOrder(index);
                        index += stateAndTol.getEquations().getSecondOrderDimension();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Build the arrays of tolerances for additional parameters of a state.
     *
     * Private method created to avoid cyclomatic complexity to be higher than 10.
     *
     * @param satId spacecraftId
     * @throws PatriusException if tolerances array size differs from additional states vector size
     *
     */
    private void buildAdditionalTolerances(final String satId) throws PatriusException {

        final List<AdditionalEquationsAndTolerances> additionalEquation = this.addEquationsAndTolerances
            .get(satId);
        if (additionalEquation != null) {
            final int size = this.stateVectorInfo.getSatAddStatesSize(satId);
            final double[] addStateAbsoluteTolerance = new double[size];
            final double[] addStateRelativeTolerance = new double[size];

            for (final AdditionalEquationsAndTolerances stateAndTol : additionalEquation) {
                // At this stage, we are sure that the additional states correspond with the
                // additional
                // tolerances
                final AdditionalStateInfo addStateInfos = this.stateVectorInfo.getAddStatesInfos(satId)
                    .get(stateAndTol.getEquations().getName());
                final int sLength = addStateInfos.getSize();
                double[] absTol = stateAndTol.getAbsTol();
                double[] relTol = stateAndTol.getRelTol();

                if (absTol == null || relTol == null) {
                    // No additional tolerances for this equation,
                    // default arrays are used :
                    // absolute tolerance -> infinite
                    // default tolerance -> zero
                    absTol = new double[sLength];
                    relTol = new double[sLength];
                    Arrays.fill(absTol, Double.POSITIVE_INFINITY);
                    Arrays.fill(relTol, 0.);
                } else if (absTol.length != sLength || relTol.length != sLength) {
                    // Checks that the tolerances' sizes are the same as the additional state.
                    throw new PatriusException(
                        PatriusMessages.ADDITIONAL_STATE_WRONG_TOLERANCES_SIZE);
                }

                final int sIndex = addStateInfos.getIndex() - SpacecraftState.ORBIT_DIMENSION;

                // additional tolerance arrays filled in the order
                // matching the state vector definition
                System.arraycopy(absTol, 0, addStateAbsoluteTolerance, sIndex, sLength);
                System.arraycopy(relTol, 0, addStateRelativeTolerance, sIndex, sLength);
            }
            this.addStatesAbsoluteTolerances.put(satId, addStateAbsoluteTolerance);
            this.addStatesRelativeTolerances.put(satId, addStateRelativeTolerance);
        }
    }

    /**
     * Expand integrator tolerance array to fit compound state vector.
     */
    private void expandToleranceArray() {
        if (this.multiIntegrator instanceof AdaptiveStepsizeIntegrator) {
            ((AdaptiveStepsizeIntegrator) this.multiIntegrator).setVecAbsoluteTolerance(this.resizeArray(
                (AdaptiveStepsizeIntegrator) this.multiIntegrator, this.orbitAbsoluteTolerances,
                this.orbitDefaultAbsoluteTolerance, this.addStatesAbsoluteTolerances));
            ((AdaptiveStepsizeIntegrator) this.multiIntegrator).setVecRelativeTolerance(this.resizeArray(
                (AdaptiveStepsizeIntegrator) this.multiIntegrator, this.orbitRelativeTolerances,
                this.orbitDefaultRelativeTolerance, this.addStatesRelativeTolerances));
        }
    }

    /**
     * Resize object internal array.
     *
     * @param integ adaptive-stepsize integrator
     * @param orbitTolerancesArrays tolerances of all orbits
     * @param orbitDefaultTol orbit tolerance to be used if no orbit tolerance was defined by user
     * @param addStatesTolerancesArrays tolerances of all additional states for all states.
     * @return tolerance array
     */
    private double[] resizeArray(final AdaptiveStepsizeIntegrator integ,
                                 final Map<String, double[]> orbitTolerancesArrays, final double[] orbitDefaultTol,
                                 final Map<String, double[]> addStatesTolerancesArrays) {
        // Global tolerance array
        final double[] resizedArray = new double[this.stateVectorInfo.getStateVectorSize()];
        // Loop on all states
        final List<String> satIdList = this.stateVectorInfo.getIdList();
        final int sizeSatIdList = satIdList.size();
        for (int i = 0; i < sizeSatIdList; i++) {
            final String satId = satIdList.get(i);
            final int satRank = this.stateVectorInfo.getSatRank(satId);
            // Get orbit tolerances
            final double[] orbitTol;
            if (orbitTolerancesArrays.containsKey(satId)) {
                orbitTol = orbitTolerancesArrays.get(satId);
            } else {
                orbitTol = orbitDefaultTol;
            }

            // Add tolerances for orbit part
            System.arraycopy(orbitTol, 0, resizedArray, satRank, SpacecraftState.ORBIT_DIMENSION);

            // Get additional states tolerances
            double[] addStatesTol = null;
            if (addStatesTolerancesArrays.containsKey(satId)) {
                addStatesTol = addStatesTolerancesArrays.get(satId);
                // Add tolerances for additional states
                if (addStatesTol != null) {
                    System.arraycopy(addStatesTol, 0, resizedArray, satRank + SpacecraftState.ORBIT_DIMENSION,
                        addStatesTol.length);
                }
            }
        }

        // Return tolerance array
        return resizedArray;
    }

    /**
     * Reset integrator tolerance array to original size.
     */
    private void resetToleranceArray() {
        if (this.multiIntegrator instanceof AdaptiveStepsizeIntegrator) {
            // Create internal error.
            // Fill in the absolute tolerance array of the integrator
            ((AdaptiveStepsizeIntegrator) this.multiIntegrator)
                .setVecAbsoluteTolerance(this.orbitDefaultAbsoluteTolerance);

            // Fill in the relative tolerance array of the integrator
            ((AdaptiveStepsizeIntegrator) this.multiIntegrator)
                .setVecRelativeTolerance(this.orbitDefaultRelativeTolerance);
        }
    }

    /**
     * Initialize event handlers.
     *
     * @param activateHandlers if true, step and event handlers should be activated
     * @param localAttProvForces attitude providers for forces computation
     * @param localAttProvEvents attitude providers for events computation.
     */
    private void initializeEventHandlers(final boolean activateHandlers,
                                         final Map<String, MultiAttitudeProvider> localAttProvForces,
                                         final Map<String, MultiAttitudeProvider> localAttProvEvents) {

        if (this.masterMultiModeHandler != null) {
            this.masterMultiModeHandler.initialize(this.orbitType, this.angleType, localAttProvForces,
                localAttProvEvents, this.stateVectorInfo, activateHandlers, this.referenceDate,
                this.propagationFrameMap, this.muMap);
        }

        if (this.ephemerisMultiModeHandler != null) {
            this.ephemerisMultiModeHandler.initialize(this.orbitType, this.angleType, localAttProvForces,
                localAttProvEvents, this.stateVectorInfo, activateHandlers, this.referenceDate,
                this.propagationFrameMap, this.muMap);
        }

        if (this.massMultiModelHandlers != null) {
            // If single attitude treatment expected
            for (final Map.Entry<String, MultiAdaptedStepHandler> entry : this.massMultiModelHandlers
                .entrySet()) {
                entry.getValue().initialize(this.orbitType, this.angleType, localAttProvForces,
                    localAttProvEvents, this.stateVectorInfo, activateHandlers, this.referenceDate,
                    this.propagationFrameMap, this.muMap);
            }
        }
    }

    /**
     * Add event handlers to integrator. Wrap all Orekit event detector and register it to the
     * integrator.
     *
     * @param activateHandlers if true, step and event handlers should be activated
     * @param localAttProvForces attitude providers for forces computation
     * @param localAttProvEvents attitude providers for events computation.
     */
    protected void addEventHandlers(final boolean activateHandlers,
                                    final Map<String, MultiAttitudeProvider> localAttProvForces,
                                    final Map<String, MultiAttitudeProvider> localAttProvEvents) {

        // Detectors list has changed: (re)build it
        // Clear event handlers
        this.multiIntegrator.clearEventHandlers();

        // Add null mass detector (in first position since it leads to stop propagation,
        // in case of event occurring at the same time)
        if (!this.nullMassDetectors.isEmpty()) {
            // If single attitude treatment expected
            for (final Map.Entry<String, NullMassDetector> entry : this.nullMassDetectors.entrySet()) {
                this.setUpEventDetector(entry.getValue(), localAttProvForces, localAttProvEvents,
                    entry.getKey());
            }
        }

        // set up events added by user
        if (activateHandlers) {
            for (final MultiEventDetector detector : this.multiDetectors) {
                this.setUpEventDetector(detector, localAttProvForces, localAttProvEvents);
            }
        }

        // set up events related to force models
        final List<String> satIdList = this.stateVectorInfo.getIdList();
        final int sizeSatIdList = satIdList.size();
        // loop on all states
        for (int sat = 0; sat < sizeSatIdList; sat++) {
            final String satId = satIdList.get(sat);
            final List<ForceModel> list = this.forceModels.get(satId);

            if (list != null) {
                final int listSize = list.size();
                // loop on all force models
                for (int i = 0; i < listSize; i++) {
                    // get events associated with the current force model
                    if (list.get(i) != null) {
                        final EventDetector[] modelDetectors = list.get(i).getEventsDetectors();
                        this.addEventHandlers(satId, localAttProvForces, localAttProvEvents,
                            modelDetectors);
                    }
                }
            }
        }
    }

    /**
     * Private method Add events from force models list of a spacecraft
     *
     * @param satId spacecraft id
     * @param localAttProvForces attitude providers for forces computation
     * @param localAttProvEvents attitude providers for events computation
     * @param modelDetectors detectors associated with the current force model
     */
    private void addEventHandlers(final String satId,
                                  final Map<String, MultiAttitudeProvider> localAttProvForces,
                                  final Map<String, MultiAttitudeProvider> localAttProvEvents,
                                  final EventDetector[] modelDetectors) {
        if (modelDetectors != null) {
            for (final EventDetector detector : modelDetectors) {
                this.setUpEventDetector(detector, localAttProvForces, localAttProvEvents, satId);
            }
        }
    }

    /**
     * Wrap an Orekit event detector and register it to the integrator.
     *
     * @param osf event handler to wrap
     * @param localAttProvForces attitude providers for forces computation
     * @param localAttProvEvents attitude providers for events computation.
     * @param satId spacecraft id
     */
    protected void setUpEventDetector(final EventDetector osf,
                                      final Map<String, MultiAttitudeProvider> localAttProvForces,
                                      final Map<String, MultiAttitudeProvider> localAttProvEvents, final String satId) {
        final EventHandler handler = new AdaptedMonoEventDetector(osf, this.orbitType, this.angleType,
            localAttProvForces.get(satId), localAttProvEvents.get(satId), this.referenceDate,
            this.stateVectorInfo, satId);

        this.multiIntegrator.addEventHandler(handler, osf.getMaxCheckInterval(), osf.getThreshold(),
            osf.getMaxIterationCount());
    }

    /**
     * Wrap an Orekit multi-sat event detector and register it to the integrator.
     *
     * @param osf event handler to wrap
     * @param localAttProvForces attitude providers for forces computation
     * @param localAttProvEvents attitude providers for events computation
     */
    protected void setUpEventDetector(final MultiEventDetector osf,
                                      final Map<String, MultiAttitudeProvider> localAttProvForces,
                                      final Map<String, MultiAttitudeProvider> localAttProvEvents) {
        final EventHandler handler = new AdaptedMultiEventDetector(osf, this.orbitType, this.angleType,
            localAttProvForces, localAttProvEvents, this.referenceDate, this.muMap, this.propagationFrameMap,
            this.stateVectorInfo);
        this.multiIntegrator.addEventHandler(handler, osf.getMaxCheckInterval(), osf.getThreshold(),
            osf.getMaxIterationCount());
    }

    /**
     * Get the {@link PVCoordinates} of the body in the selected frame.
     *
     * @param date current date
     * @param frame the frame where to define the position
     * @param satId spacecraft id
     * @return position/velocity of the body (m and m/s)
     * @exception PatriusException if position/velocity cannot be computed in given frame
     */
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame,
                                          final String satId) throws PatriusException {
        return this.propagate(date).get(satId).getPVCoordinates(frame);
    }

    /**
     * Get the number of calls to the differential equations computation method.
     * <p>
     * The number of calls is reset each time the {@link #propagate(AbsoluteDate)} method is called.
     * </p>
     *
     * @return number of calls to the differential equations computation method
     */
    public int getCalls() {
        return this.calls;
    }

    /** {@inheritDoc} */
    @Override
    public void update(final Observable o, final Object eventHandlerIn) {
        final MultiEventDetector multiDetector = ((AdaptedMultiEventDetector) eventHandlerIn)
            .getMultiDetector();
        this.multiDetectors.remove(multiDetector);
    }

    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations,
        TimeDerivativesEquations, SecondOrderDifferentialEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** Reference to the derivatives array to initialize. */
        private double[] storedYDot;

        /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
        private final double[][] jacobian;

        /** Current satellite id */
        private String satId;

        /**
         * Local attitude providers for forces computation
         */
        private final Map<String, MultiAttitudeProvider> internalAttProvForces;

        /**
         * Local attitude providers for events computation
         */
        private final Map<String, MultiAttitudeProvider> internalAttProvEvents;

        /** Current velocity. */
        private double[] velocity;

        /**
         * Build a new instance.
         *
         * @param attProvidersForForces attitude providers for forces computation for all states
         * @param attProvidersForEvents attitude providers for events computation for all states
         */
        public DifferentialEquations(
                                     final Map<String, MultiAttitudeProvider> attProvidersForForces,
                                     final Map<String, MultiAttitudeProvider> attProvidersForEvents) {
            MultiNumericalPropagator.this.calls = 0;
            this.jacobian = new double[6][6];
            this.internalAttProvForces = attProvidersForForces;
            this.internalAttProvEvents = attProvidersForEvents;
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return MultiNumericalPropagator.this.stateVectorInfo.getStateVectorSize();
        }

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {

            try {
                // update space dynamics view
                final AbsoluteDate currentDate = MultiNumericalPropagator.this.referenceDate.shiftedBy(t);

                this.initDerivatives(yDot);

                // Get spacecraft id list
                final List<String> satIdList = MultiNumericalPropagator.this.stateVectorInfo.getIdList();
                final int sizeSatIdList = satIdList.size();

                // Store mu from currentStates
                // These mu are different from the local attribute muMap which stores the MU to be
                // used for integration process
                final Map<String, Double> muOrbitMap = new ConcurrentHashMap<>();
                for (int i = 0; i < sizeSatIdList; i++) {
                    final String sat = satIdList.get(i);
                    muOrbitMap.put(sat, MultiNumericalPropagator.this.currentStates.get(sat).getMu());
                }
                MultiNumericalPropagator.this.currentStates =
                    MultiNumericalPropagator.this.stateVectorInfo.mapArrayToStates(y, currentDate,
                        MultiNumericalPropagator.this.orbitType,
                        MultiNumericalPropagator.this.angleType, this.internalAttProvForces,
                        this.internalAttProvEvents, muOrbitMap,
                        MultiNumericalPropagator.this.propagationFrameMap);

                for (int id = 0; id < sizeSatIdList; id++) {
                    this.satId = satIdList.get(id);
                    final SpacecraftState state = MultiNumericalPropagator.this.currentStates.get(this.satId);

                    // initialize derivatives
                    this.initDerivatives(yDot, state.getOrbit());

                    // compute the contributions of all forces
                    final List<ForceModel> forceModelList = MultiNumericalPropagator.this.forceModels.get(this.satId);
                    if (forceModelList != null) {
                        final int size = forceModelList.size();
                        for (int i = 0; i < size; i++) {
                            if (forceModelList.get(i) != null) {
                                forceModelList.get(i).addContribution(state, this);
                            }
                        }
                    }

                    // Add contribution for additional state
                    if (MultiNumericalPropagator.this.addEquationsAndTolerances.containsKey(this.satId)) {
                        final List<AdditionalEquationsAndTolerances> addEqList =
                            MultiNumericalPropagator.this.addEquationsAndTolerances
                                .get(this.satId);

                        if (addEqList != null) {
                            final int size = addEqList.size();
                            for (int i = 0; i < size; i++) {
                                // compute additional derivatives and store it in the storedYDot
                                // vector
                                if (addEqList.get(i) != null) {
                                    addEqList.get(i).getEquations().computeDerivatives(state, this);
                                }
                            }
                        }
                    }
                }
                // increment calls counter
                ++MultiNumericalPropagator.this.calls;
            } catch (final PatriusException oe) {
                throw new PatriusExceptionWrapper(oe);
            }

        }

        /**
         * Adapted method for multi numerical propagation.
         *
         * @param yDot yDot placeholder array where to put the time derivative of the state vector
         */
        public void initDerivatives(final double[] yDot) {
            this.storedYDot = yDot;
            Arrays.fill(this.storedYDot, 0.0);
        }

        /** {@inheritDoc} */
        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit)
            throws PropagationException {
            // Retrieve additional equations
            final List<AdditionalEquationsAndTolerances> addEquationsAndTolerancesID =
                MultiNumericalPropagator.this.addEquationsAndTolerances
                    .get(this.satId);
            if (addEquationsAndTolerancesID != null) {
                for (final AdditionalEquationsAndTolerances stateAndTol : addEquationsAndTolerancesID) {
                    final AdditionalEquations equations = stateAndTol.getEquations();
                    if (equations instanceof MassEquation) {
                        // Mass derivatives to 0
                        ((MassEquation) stateAndTol.getEquations()).setMassDerivativeZero();
                    }
                }
            }

            // Compute jacobian
            currentOrbit.getJacobianWrtCartesian(MultiNumericalPropagator.this.angleType, this.jacobian);

            // Add velocity to position derivative
            this.velocity = currentOrbit.getPVCoordinates().getVelocity().toArray();
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = this.jacobian[i];
                this.storedYDot[MultiNumericalPropagator.this.stateVectorInfo.getSatRank(this.satId) + i] = jRow[0]
                        * this.velocity[0] + jRow[1] * this.velocity[1] + jRow[2] * this.velocity[2];
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = this.jacobian[i];
                this.storedYDot[MultiNumericalPropagator.this.stateVectorInfo.getSatRank(this.satId) + i] +=
                    jRow[3] * x + jRow[4] * y + jRow[5] * z;
            }

        }

        /** {@inheritDoc} */
        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws PatriusException {
            final SpacecraftState state = MultiNumericalPropagator.this.currentStates.get(this.satId);
            final Transform t = frame.getTransformTo(state.getFrame(), state.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            this.addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            // the mass flow-rate should be negative
            if ((pDot[0] > 0) && (name.contains(SpacecraftState.MASS))) {
                throw PatriusException.createIllegalArgumentException(
                    PatriusMessages.POSITIVE_FLOW_RATE, pDot[0]);
            }
            // additional state informations corresponding to the input flow rate
            final AdditionalStateInfo addStateInfo =
                MultiNumericalPropagator.this.stateVectorInfo.getAddStatesInfos(this.satId).get(
                    name);
            System.arraycopy(pDot, 0, this.storedYDot,
                addStateInfo.getIndex() + MultiNumericalPropagator.this.stateVectorInfo.getSatRank(this.satId),
                addStateInfo.getSize());
        }

        /** {@inheritDoc} */
        // CHECKSTYLE: stop CyclomaticComplexity check
        // Reason: code similar to computeDerivatives() method
        @Override
        public void computeSecondDerivatives(final double t,
                                             final double[] y,
                                             final double[] yDot,
                                             final double[] yDDot) {
            // CHECKSTYLE: resume CyclomaticComplexity check
            // Second order derivatives for integrators such as Cowell

            try {
                // At this point: y contains position, yDot contains velocity (cartesian coordinates only)

                // update space dynamics view
                final AbsoluteDate currentDate = MultiNumericalPropagator.this.referenceDate.shiftedBy(t);

                // Get spacecraft id list
                final List<String> satIdList = MultiNumericalPropagator.this.stateVectorInfo.getIdList();
                final int sizeSatIdList = satIdList.size();

                // Store mu from currentStates
                // These mu are different from the local attribute muMap which stores the MU to be
                // used for integration process
                final Map<String, Double> muOrbitMap = new ConcurrentHashMap<>();
                for (int i = 0; i < sizeSatIdList; i++) {
                    final String sat = satIdList.get(i);
                    muOrbitMap.put(sat, MultiNumericalPropagator.this.currentStates.get(sat).getMu());
                }
                final double[] x = ((CowellIntegrator) MultiNumericalPropagator.this.multiIntegrator).getMapper()
                    .buildFullState(y, yDot);
                MultiNumericalPropagator.this.currentStates =
                    MultiNumericalPropagator.this.stateVectorInfo.mapArrayToStates(x, currentDate,
                        MultiNumericalPropagator.this.orbitType,
                        MultiNumericalPropagator.this.angleType, this.internalAttProvForces,
                        this.internalAttProvEvents, muOrbitMap,
                        MultiNumericalPropagator.this.propagationFrameMap);

                // initialize derivatives
                Arrays.fill(yDDot, 0.0);

                // Loop on all satellites
                int pos = 0;
                for (int id = 0; id < sizeSatIdList; id++) {
                    this.satId = satIdList.get(id);
                    final SpacecraftState state = MultiNumericalPropagator.this.currentStates.get(this.satId);

                    final List<AdditionalEquationsAndTolerances> addEqAndTolID = MultiNumericalPropagator
                        .this.addEquationsAndTolerances.get(this.satId);
                    if (addEqAndTolID != null) {
                        for (final AdditionalEquationsAndTolerances stateAndTol : addEqAndTolID) {
                            final AdditionalEquations equations = stateAndTol.getEquations();
                            if (equations instanceof MassEquation) {
                                ((MassEquation) stateAndTol.getEquations()).setMassDerivativeZero();
                            }
                        }
                    }

                    // compute the contributions of all forces
                    final List<ForceModel> forceModelList = MultiNumericalPropagator.this.forceModels.get(this.satId);
                    if (forceModelList != null) {
                        for (final ForceModel forceModel : forceModelList) {
                            final Vector3D acc = forceModel.computeAcceleration(state);
                            yDDot[pos] += acc.getX();
                            yDDot[pos + 1] += acc.getY();
                            yDDot[pos + 2] += acc.getZ();
                        }
                    }

                    // Add contribution for additional state
                    if (MultiNumericalPropagator.this.addEquationsAndTolerances.containsKey(this.satId)) {
                        final List<AdditionalEquationsAndTolerances> addEqList =
                            MultiNumericalPropagator.this.addEquationsAndTolerances
                                .get(this.satId);

                        if (addEqList != null) {
                            for (final AdditionalEquationsAndTolerances stateAndTol : addEqList) {
                                // compute additional derivatives and store it in the storedYDot vector
                                final double[] secondDerivatives = stateAndTol.getEquations().computeSecondDerivatives(
                                    state);
                                // Put it in the right place in yDDot
                                System.arraycopy(secondDerivatives, 0, yDDot, pos + stateAndTol.getIndex2ndOrder(),
                                    secondDerivatives.length);
                            }
                        }
                    }

                    // Update next position in 2nd order state vector
                    pos += new SecondOrderMapper1Sat(MultiNumericalPropagator.this.addEquationsAndTolerances
                        .get(this.satId)).get2ndOrderDimension();
                }
                // increment calls counter
                ++MultiNumericalPropagator.this.calls;
            } catch (final PatriusException oe) {
                throw new PatriusExceptionWrapper(oe);
            }
        }
    }
}
