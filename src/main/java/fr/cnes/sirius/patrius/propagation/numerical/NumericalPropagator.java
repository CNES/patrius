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
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3150:10/05/2022:[PATRIUS] Absence d'attitude lors de l'utilisation du mode Ephemeris du prop.
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.7:DM:DM-2859:18/05/2021:Optimisation du code ; SpacecraftState 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.4:DM:DM-2126:04/10/2019:[PATRIUS] Calcul du DeltaV realise
 * VERSION:4.3:FA:FA-2079:15/05/2019:Non suppression de detecteurs d'evenements en fin de propagation
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:187:16/12/2013:Deactivated event detection in 'simple propagation' for t0 to tStart
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:280:08/10/2014:propagator modified in order to use the mu of gravitational forces
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:372:27/11/2014:Newtonian attraction bug
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:284:06/01/2014:New architecture for parameterizable Parameters
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:418:12/03/2015:Corrected problem with setAttitudeProvider
 * VERSION::DM:300:18/03/2015:Creation multi propagator (Replaced AdditionalStateData by AdditionalStateInfo)
 * VERSION::FA:325:02/04/2015: problem with end date that does not match required end date
 * VERSION::FA:476:06/10/2015:Propagation until final date in ephemeris mode
 * VERSION::DM:426:30/10/2015: Possibility to set up orbits in non inertial frames
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::FA:706:13/12/2016: synchronisation problem with the Assemby mass
 * VERSION::FA:1520:24/04/2018: Modification of an error message
 * VERSION::FA:1653:23/10/2018: correct handling of detectors in several propagations
 * VERSION::FA:1852:05/10/2018: move mass flow rate test in MassEquation
 * VERSION::FA:1868:31/10/2018: handle proper end of integration
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * VERSION::FA:XXXX:29/01/2019: Implement the Observer interface and the update method
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.numerical;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;

import fr.cnes.sirius.patrius.assembly.properties.MassEquation;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.gravity.AbstractAttractionModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianAttractionModel;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.MathIllegalArgumentException;
import fr.cnes.sirius.patrius.math.exception.MathIllegalStateException;
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
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.AdaptedEventDetector;
import fr.cnes.sirius.patrius.propagation.events.EventDetector;
import fr.cnes.sirius.patrius.propagation.events.NullMassDetector;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.sampling.AdaptedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepNormalizer;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * This class propagates {@link SpacecraftState} using numerical integration.
 * <p>
 * Numerical propagation is much more accurate than analytical propagation like for example
 * {@link fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator keplerian} or
 * {@link fr.cnes.sirius.patrius.propagation.analytical.EcksteinHechlerPropagator Eckstein-Hechler},
 * but requires a few more steps to set up to be used properly. Whereas analytical propagators are
 * configured only thanks to their various constructors and can be used immediately after
 * construction, numerical propagators configuration involve setting several parameters between
 * construction time and propagation time.
 * </p>
 * <p>
 * The configuration parameters that can be set are:
 * </p>
 * <ul>
 * <li>the initial spacecraft state ({@link #setInitialState(SpacecraftState)})</li>
 * <li>the central attraction coefficient ({@link #setMu(double)})</li>
 * <li>the various force models ({@link #addForceModel(ForceModel)}, {@link #removeForceModels()})</li>
 * <li>the {@link OrbitType type} of orbital parameters to be used for propagation (
 * {@link #setOrbitType(OrbitType)}),
 * <li>the {@link PositionAngle type} of position angle to be used in orbital parameters to be used
 * for propagation where it is relevant ({@link #setPositionAngleType(PositionAngle)}),
 * <li>whether {@link AdditionalEquations additional equations} (for example
 * {@link PartialDerivativesEquations
 * Jacobians}) should be propagated along with orbital state (
 * {@link #addAdditionalEquations(AdditionalEquations)}),
 * <li>the discrete events that should be triggered during propagation (
 * {@link #addEventDetector(EventDetector)}, {@link #clearEventsDetectors()})</li>
 * <li>the binding logic with the rest of the application ({@link #setSlaveMode()},
 * {@link #setMasterMode(double, PatriusFixedStepHandler)},
 * {@link #setMasterMode(PatriusStepHandler)}, {@link #setEphemerisMode()},
 * {@link #getGeneratedEphemeris()})</li>
 * </ul>
 * <p>
 * From these configuration parameters, only the initial state is mandatory. The default propagation
 * settings are in {@link OrbitType#EQUINOCTIAL equinoctial} parameters with
 * {@link PositionAngle#TRUE true} longitude argument. If the central attraction coefficient is not
 * explicitly specified, the one used to define the initial orbit will be used. However, specifying
 * only the initial state and perhaps the central attraction coefficient would mean the propagator
 * would use only keplerian forces. In this case, the simpler
 * {@link fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator KeplerianPropagator}
 * class would perhaps be more effective.
 * </p>
 * <p>
 * The underlying numerical integrator set up in the constructor may also have its own configuration
 * parameters. Typical configuration parameters for adaptive stepsize integrators are the min, max
 * and perhaps start step size as well as the absolute and/or relative errors thresholds.
 * </p>
 * <p>
 * The state that is seen by the integrator is a simple six elements double array. The six first
 * elements are either:
 * <ul>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.EquinoctialOrbit equinoctial orbit parameters} (a,
 * e<sub>x</sub>, e<sub>y</sub>, h<sub>x</sub>, h<sub>y</sub>, &lambda;<sub>M</sub> or
 * &lambda;<sub>E</sub> or &lambda;<sub>v</sub>) in meters and radians,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.KeplerianOrbit Keplerian orbit parameters} (a, e, i,
 * &omega;, &Omega;, M or E or v) in meters and radians,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.CircularOrbit circular orbit parameters} (a,
 * e<sub>x</sub>, e<sub>y</sub>, i, &Omega;, &alpha;<sub>M</sub> or &alpha;<sub>E</sub> or
 * &alpha;<sub>v</sub>) in meters and radians,</li>
 * <li>the {@link fr.cnes.sirius.patrius.orbits.CartesianOrbit Cartesian orbit parameters} (x, y, z,
 * v<sub>x</sub>, v<sub>y</sub>, v<sub>z</sub>) in meters and meters per seconds.
 * </ul>
 * </p>
 * <p>
 * The following code snippet shows a typical setting for Low Earth Orbit propagation in equinoctial
 * parameters and true longitude argument:
 * </p>
 * 
 * <pre>
 * final double dP = 0.001;
 * final double minStep = 0.001;
 * final double maxStep = 500;
 * final double initStep = 60;
 * AdaptiveStepsizeIntegrator integrator = new DormandPrince853Integrator(minStep, maxStep,
 *         AbsTolerance, RelTolerance);
 * integrator.setInitialStepSize(initStep);
 * propagator = new NumericalPropagator(integrator);
 * </pre>
 * <p>
 * The same propagator can be reused for several state extrapolations, by resetting the initial
 * state without modifying the other configuration parameters. However, the same instance cannot be
 * used simultaneously by different threads, the class is <em>not</em> thread-safe.
 * </p>
 * <b> Warning </b> : when using a fixed step handler (method
 * {@link #setMasterMode(double, PatriusFixedStepHandler)},
 * with an Assembly, users must access to
 * the additional states (such as mass) by the spacecraft AND NOT using the Assembly since Assembly
 * is synchronized only once per integration step. In any other case (using an
 * {@link PatriusStepHandler} ) for
 * instance), both assembly and spacecraft can be used to retrieve
 * additional states.
 * 
 * @serial the serialization support of this class is not guarantee
 * 
 * @see SpacecraftState
 * @see ForceModel
 * @see PatriusStepHandler
 * @see PatriusFixedStepHandler
 * @see fr.cnes.sirius.patrius.propagation.precomputed.IntegratedEphemeris
 * @see TimeDerivativesEquations
 * 
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Pierre Cardoso
 */
@SuppressWarnings({"PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod"})
public class NumericalPropagator implements Propagator, Observer {

    /** Serializable UID. */
    private static final long serialVersionUID = 3022312736994891425L;

    /** Attitude provider for forces computation. */
    private AttitudeProvider attitudeProviderForces;

    /** Attitude provider for events computation. */
    private AttitudeProvider attitudeProviderEvents;

    /** Attitude provider given by default for one attitude. */
    private AttitudeProvider attitudeProviderByDefault;

    /** Central body attraction. */
    private NewtonianAttractionModel newtonianAttraction;

    /** Force models used during the extrapolation of the Orbit, without jacobians. */
    private final List<ForceModel> forceModels;

    /** Event detectors not related to force models. */
    private final List<EventDetector> detectors;

    /** State vector. */
    private double[] stateVector;

    /** Start date. */
    private AbsoluteDate startDate;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Initial state to propagate. */
    private SpacecraftState initialState;

    /** Current state to propagate. */
    private SpacecraftState currentState;

    /** Integrator selected by the user for the orbital extrapolation process. */
    private transient FirstOrderIntegrator integrator;

    /** Counter for differential equations calls. */
    private int calls;

    /** Propagator mode handler (ephemeris mode). */
    private transient EphemerisModeHandler modeEphemerisHandler;

    /** Propagator mode handler (master mode). */
    private transient AdaptedStepHandler modeMasterHandler;

    /**
     * Mass model handler. If mass model equations are added to the propagator, the following
     * handler is added in order to update the mass model with the final state value
     */
    private AdaptedStepHandler massModelHandler;

    /** Null mass detector. */
    private NullMassDetector nullMassDetector;

    /** Current mode. */
    private int mode;

    /** Propagation orbit type. */
    private OrbitType orbitType;

    /** Position angle type. */
    private PositionAngle angleType;

    /** Additional equations and associated integrator tolerances. */
    private final List<AdditionalEquationsAndTolerances> addEquationsAndTolerances;

    /** Additional states informations. */
    private Map<String, AdditionalStateInfo> addStateInfos;

    /** Frame used for propagation. */
    private Frame propagationFrame;

    /**
     * User-specified absolute tolerances for additional states. Built from the additional
     * tolerances of each additional equation.
     */
    private double[] addStatesAbsoluteTolerances;

    /**
     * User-specified relative tolerances for additional states. Built from the additional
     * tolerances of each additional equation.
     */
    private double[] addStatesRelativeTolerances;

    /** Newtonian attraction disabling flag. */
    private boolean disableNewtonianAttractionFlag = false;
    
    /**
     * Create a new instance of NumericalPropagator, based on orbit definition mu. After creation,
     * the instance is empty, i.e. there are no perturbing forces at all. This means that if {@link #addForceModel
     * addForceModel} is not called after creation, the integrated orbit will
     * follow a keplerian evolution only. The defaults are {@link OrbitType#EQUINOCTIAL} for
     * {@link #setOrbitType(OrbitType) propagation orbit type} and {@link PositionAngle#TRUE} for
     * {@link #setPositionAngleType(PositionAngle) position angle type}.
     * 
     * <p>
     * The new instance of NumericalPropagator is declared as an {@link Observer observer} of the contained integrator.
     * As observer, it gets notified if a detector is deleted in the integrator sub-layer, to update its own detectors
     * list.
     * </p>
     * 
     * @param integratorIn numerical integrator to use for propagation.
     */
    public NumericalPropagator(final FirstOrderIntegrator integratorIn) {
        this.forceModels = new ArrayList<>();
        this.detectors = new ArrayList<>();
        this.startDate = null;
        this.referenceDate = null;
        this.currentState = null;
        this.addEquationsAndTolerances = new ArrayList<>();
        this.attitudeProviderForces = null;
        this.attitudeProviderEvents = null;
        this.attitudeProviderByDefault = null;
        this.stateVector = new double[SpacecraftState.ORBIT_DIMENSION];
        this.setMu(Double.NaN);
        this.setIntegrator(integratorIn);
        this.setSlaveMode();
        this.setOrbitType(OrbitType.EQUINOCTIAL);
        this.setPositionAngleType(PositionAngle.TRUE);
        this.addStatesAbsoluteTolerances = null;
        this.addStatesRelativeTolerances = null;
        this.massModelHandler = null;
        this.nullMassDetector = null;
        this.propagationFrame = null;

        if (this.integrator instanceof AbstractIntegrator) {
            ((AbstractIntegrator) this.integrator).addObserver(this);
        }
    }

    /**
     * Set the integrator and declare the NumericalPropagator object as observer of the specified
     * integrator.
     * 
     * @param integratorIn numerical integrator to use for propagation.
     */
    public void setIntegrator(final FirstOrderIntegrator integratorIn) {
        this.integrator = integratorIn;

        if (this.integrator instanceof AbstractIntegrator) {
            ((AbstractIntegrator) this.integrator).addObserver(this);
        }
    }

    /**
     * Set the central attraction coefficient &mu;.<br>
     * The Newtonian attraction from the central body force model will be updated with the new
     * coefficient.
     * 
     * @param mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @see #getMu()
     * @see #addForceModel(ForceModel)
     */
    public void setMu(final double mu) {
        this.newtonianAttraction = new NewtonianAttractionModel(this.propagationFrame, mu);
    }

    /**
     * Get the central attraction coefficient &mu;.
     * 
     * @return mu central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @see #setMu(double)
     */
    public double getMu() {
        return this.newtonianAttraction.getMu();
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProvider() {
        final AttitudeProvider res;
        if (this.attitudeProviderByDefault == null) {
            if (this.attitudeProviderForces == null) {
                res = this.attitudeProviderEvents;
            } else {
                res = this.attitudeProviderForces;
            }
        } else {
            res = this.attitudeProviderByDefault;
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProviderForces() {
        return this.attitudeProviderForces;
    }

    /** {@inheritDoc} */
    @Override
    public AttitudeProvider getAttitudeProviderEvents() {
        return this.attitudeProviderEvents;
    }

    /**
     * Check if a single attitude treatment is expected.
     * 
     * @since 2.3.1
     * 
     * @throws IllegalStateException If an attitude provider or an additional equation is already
     *         defined for a specific attitude (for events or forces computation)
     */
    private void checkSingleAttitudeTreatment() {
        if ((this.attitudeProviderForces != null) || (this.attitudeProviderEvents != null)) {
            throw PatriusException
                .createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
        }
        for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
            final String name = this.addEquationsAndTolerances.get(i).getEquations().getName();
            if ((name.equals(AttitudeType.ATTITUDE_FORCES.toString()))
                || (name.equals(AttitudeType.ATTITUDE_EVENTS.toString()))) {
                throw PatriusException
                    .createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
            }
        }
    }

    /**
     * Check if a two attitude treatment is expected.
     * 
     * @since 2.3.1
     * 
     * @throws IllegalStateException If an attitude provider or an additional equation is already
     *         defined by default for a single attitude
     */
    private void checkTwoAttitudesTreatment() {
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException
                .createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
            final String name = this.addEquationsAndTolerances.get(i).getEquations().getName();
            if (name.equals(AttitudeType.ATTITUDE.toString())) {
                throw PatriusException
                    .createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.checkSingleAttitudeTreatment();
        for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
            if (this.addEquationsAndTolerances.get(i).getEquations().getName()
                .equals(AttitudeType.ATTITUDE.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                    .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProviderByDefault = attitudeProvider;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attitudeForcesProvider) {
        this.checkTwoAttitudesTreatment();
        for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
            if (this.addEquationsAndTolerances.get(i).getEquations().getName()
                .equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                    .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProviderForces = attitudeForcesProvider;
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attitudeEventsProvider) {
        this.checkTwoAttitudesTreatment();
        for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
            if (this.addEquationsAndTolerances.get(i).getEquations().getName()
                .equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                    .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }
        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        this.attitudeProviderEvents = attitudeEventsProvider;
    }

    /** {@inheritDoc} */
    @Override
    public void addEventDetector(final EventDetector detector) {
        this.detectors.add(detector);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<EventDetector> getEventsDetectors() {
        return Collections.unmodifiableCollection(this.detectors);
    }

    /** {@inheritDoc} */
    @Override
    public void clearEventsDetectors() {
        this.detectors.clear();
    }

    /**
     * Add a force model to the global perturbation model.
     * <p>
     * If the force is an attraction model, the central attraction coefficient of this force will be used during the
     * propagation.
     * </p>
     * <p>
     * If the force is a Newtonian attraction model, the central attraction coefficient will be updated but the force
     * will not be added to the model (already present).
     * </p>
     * <p>
     * If this method is not called at all, the integrated orbit will follow a keplerian evolution only (using the
     * central attraction coefficient of the orbit).
     * </p>
     * 
     * <p>
     * Advice: in order to minimize absorption effects leading to reduced accuracy, add force models from least
     * perturbing force to highest perturbing force. Example: for LEO orbits, drag should be added in last.
     * </p>
     * 
     * @param model perturbing {@link ForceModel} to add
     * @see #removeForceModels()
     * @see #setMu(double)
     */
    public void addForceModel(final ForceModel model) {

//        if (model instanceof AttractionModel && !(model instanceof ThirdBodyAttraction)) {
        if (model instanceof AbstractAttractionModel) {
            // If the force is a gravitational attraction model, use the
            // central attraction coefficient of this force for the propagation.
            this.setMu(((AbstractAttractionModel) model).getMu());
            if (model instanceof NewtonianAttractionModel) {
                // The Newtonian attraction is already in the list of forces,
                // it should not be added again.
                return;
            }
        }
        this.forceModels.add(model);
    }

    /**
     * Remove all perturbing force models from the global perturbation model.
     * <p>
     * Once all perturbing forces have been removed (and as long as no new force model is added), the integrated orbit
     * will follow a keplerian evolution only.
     * </p>
     * 
     * @see #addForceModel(ForceModel)
     */
    public void removeForceModels() {
        this.forceModels.clear();
    }

    /**
     * Get perturbing force models list.
     * 
     * @return list of perturbing force models
     * @see #addForceModel(ForceModel)
     * @see #getNewtonianAttractionForceModel()
     */
    public List<ForceModel> getForceModels() {
        return this.forceModels;
    }

    /**
     * Get the Newtonian attraction from the central body force model.
     * 
     * @return Newtonian attraction force model
     * @see #setMu(double)
     * @see #getForceModels()
     */
    public NewtonianAttractionModel getNewtonianAttractionForceModel() {
        return this.newtonianAttraction;
    }

    /** {@inheritDoc} */
    @Override
    public int getMode() {
        return this.mode;
    }

    /** {@inheritDoc} */
    @Override
    public Frame getFrame() {
        return this.propagationFrame != null ? this.propagationFrame : (this.initialState != null
            && this.initialState.getFrame().isPseudoInertial() ? this.initialState.getFrame() : null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #NumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been callled.
     * </p>
     */
    @Override
    public void setSlaveMode() {
        this.modeMasterHandler = null;
        this.modeEphemerisHandler = null;
        this.mode = SLAVE_MODE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #NumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     */
    @Override
    public void setMasterMode(final double h, final PatriusFixedStepHandler handler) {
        this.setMasterMode(new PatriusStepNormalizer(h, handler));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #NumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been callled.
     * </p>
     */
    @Override
    public void setMasterMode(final PatriusStepHandler handler) {
        this.modeMasterHandler = new AdaptedStepHandler(handler);
        this.modeEphemerisHandler = null;
        this.mode = MASTER_MODE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method has the side effect of replacing the step handlers of the underlying integrator set up in
     * the {@link #NumericalPropagator(FirstOrderIntegrator) constructor} or the
     * {@link #setIntegrator(FirstOrderIntegrator) setIntegrator} method. So if a specific step handler is needed, it
     * should be added after this method has been called.
     * </p>
     */
    @Override
    public void setEphemerisMode() {
        this.modeMasterHandler = null;
        this.modeEphemerisHandler = new EphemerisModeHandler();
        this.mode = EPHEMERIS_GENERATION_MODE;
    }

    /**
     * Set propagation orbit type.
     * 
     * @param orbitTypeIn orbit type to use for propagation
     */
    public void setOrbitType(final OrbitType orbitTypeIn) {
        this.orbitType = orbitTypeIn;
    }

    /** {@inheritDoc} */
    @Override
    public void setOrbitFrame(final Frame frame) throws PatriusException {
        if (frame.isPseudoInertial()) {
            this.propagationFrame = frame;
            this.newtonianAttraction = new NewtonianAttractionModel(this.propagationFrame,
                this.newtonianAttraction.getMuParameter());
        } else {
            throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
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
     * Set position angle type.
     * <p>
     * The position parameter type is meaningful only if {@link #getOrbitType() propagation orbit
     * type} support it. As an example, it is not meaningful for propagation in {@link OrbitType#CARTESIAN Cartesian}
     * parameters.
     * </p>
     * 
     * @param positionAngleType angle type to use for propagation
     */
    public void setPositionAngleType(final PositionAngle positionAngleType) {
        this.angleType = positionAngleType;
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
    public BoundedPropagator getGeneratedEphemeris() {
        if (this.mode != EPHEMERIS_GENERATION_MODE) {
            throw PatriusException.createIllegalStateException(
                PatriusMessages.PROPAGATOR_NOT_IN_EPHEMERIS_GENERATION_MODE);
        }
        return this.modeEphemerisHandler.getEphemeris(this.attitudeProviderByDefault);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState getInitialState() {
        return this.initialState;
    }

    /**
     * Set the initial state.
     * 
     * @param initialStateIn initial state
     * @see #propagate(AbsoluteDate)
     */
    public void setInitialState(final SpacecraftState initialStateIn) {
        this.resetInitialState(initialStateIn);
    }

    /** {@inheritDoc} */
    @Override
    public void resetInitialState(final SpacecraftState state) {
        if (this.newtonianAttraction == null) {
            this.setMu(state.getMu());
        }
        this.initialState = state;
        this.startDate = null;
    }

    /**
     * Select additional state and equations pair in the list.
     * 
     * @param name name of the additional equations to select
     * @return additional state and equations pair
     * @throws PatriusException if additional equation is unknown
     */
    private AdditionalEquationsAndTolerances selectEquationsAndTolerances(final String name)
                                                                                            throws PatriusException {
        for (final AdditionalEquationsAndTolerances equAndTolerances : this.addEquationsAndTolerances) {
            if (equAndTolerances.getEquations().getName().equals(name)) {
                return equAndTolerances;
            }
        }
        throw new PatriusException(PatriusMessages.UNKNOWN_ADDITIONAL_EQUATION, name);
    }

    /**
     * Add a set of user-specified equations to be integrated along with the orbit propagation. If
     * the set of equations is already registered, it is replaced by the new one.
     * 
     * @param addEqu additional equations
     * @see SpacecraftState#addAdditionalState(String, double[])
     */
    public void addAdditionalEquations(final AdditionalEquations addEqu) {
        // this is really a new set of equations, add it
        this.addEquationsAndTolerances.add(new AdditionalEquationsAndTolerances(addEqu));
    }

    /**
     * Add a set of user-specified attitude equations to be integrated along with the orbit
     * propagation. If the set of attitude equations is already registered for the current attitude,
     * it is replaced by the new one.
     * 
     * @param addEqu attitude additional equations
     */
    public void addAttitudeEquation(final AttitudeEquation addEqu) {
        final AttitudeType type = addEqu.getAttitudeType();
        // check the attitude type to be integrated
        switch (type) {
            case ATTITUDE_FORCES:
                this.checkTwoAttitudesTreatment();
                if (this.attitudeProviderForces != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE_EVENTS:
                this.checkTwoAttitudesTreatment();
                if (this.attitudeProviderEvents != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE:
                this.checkSingleAttitudeTreatment();
                if (this.attitudeProviderByDefault != null) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            default:
                throw new PatriusRuntimeException(PatriusMessages.UNKNOWN_PARAMETER, null);
        }
        // add additional equation in numerical propagator
        this.addAdditionalEquations(addEqu);
    }

    /**
     * Add additional equations associated with the mass provider. A null-mass detector associated
     * with the input mass provider is automatically added.
     * <p>
     * Note that this method should be called after {@link #setSlaveMode()} or
     * {@link #setMasterMode(PatriusStepHandler)} or {@link #setEphemerisMode()} since this method reset the integrator
     * step handlers list.
     * </p>
     * 
     * <p>
     * <b>WARNING</b>: This method should be called only once and provided mass provider should be the same used for
     * force models.
     * </p>
     * 
     * @param massProvider the mass provider
     */
    public void setMassProviderEquation(final MassProvider massProvider) {

        // Add mass equations
        final List<String> allPartsName = massProvider.getAllPartsNames();
        final int size = allPartsName.size();
        for (int i = 0; i < size; i++) {
            this.addAdditionalEquations(massProvider.getAdditionalEquation(allPartsName.get(i)));
        }
        // Add null mass detector (in first position since it leads to stop propagation, in case
        // of event occurring
        // at the same time)
        this.nullMassDetector = new NullMassDetector(massProvider);

        // Add a step handler that update mass model at the end of state
        this.massModelHandler = new AdaptedStepHandler(new PatriusStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = 7479991641528003467L;

            /** {@inheritDoc} */
            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // do nothing
            }

            /** {@inheritDoc} */
            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                    final boolean isLast) throws PropagationException {
                // Performed at each step of propagation.
                // Mass provider is kept updated
                final List<String> allPartsName = massProvider.getAllPartsNames();
                final int size = allPartsName.size();
                for (int i = 0; i < size; i++) {
                    final String partName = allPartsName.get(i);
                    try {
                        if (NumericalPropagator.this.nullMassDetector.isTriggered()) {
                            // Particular case: total mass is 0, but due to numerical quality
                            // issues
                            // part mass may be a little below 0 (-1E-15 for example)
                            // Here we avoid to raise an exception
                            massProvider.updateMass(partName, 0);
                        } else {
                            // A part mass may be a little below 0 : update it to 0 in this case
                            final double additionalState =
                                    interpolator.getInterpolatedState().getAdditionalState(
                                            SpacecraftState.MASS + partName)[0];
                            if (additionalState < 0.0) {
                                massProvider.updateMass(partName, 0.0);
                            } else {
                                massProvider.updateMass(partName, additionalState);
                            }
                        }
                    } catch (final PatriusException e) {
                        throw new PropagationException(e, PatriusMessages.NOT_POSITIVE_MASS);
                    }
                }
            }
        });
    }

    /**
     * Add additional state tolerances.
     * 
     * @param name the additional state name
     * @param absTol absolute tolerances
     * @param relTol relative tolerances
     * @throws PatriusException if additional equation associated with the input additional state
     *         name is unknown
     */
    public void setAdditionalStateTolerance(final String name, final double[] absTol,
                                            final double[] relTol) throws PatriusException {
        // Check that tolerances' sizes are the same as the additional state is done in
        // buildAdditionalTolerances
        // method.
        // Set additional tolerances
        this.selectEquationsAndTolerances(name).setTolerances(absTol, relTol);
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate target) throws PropagationException {
        try {
            if (this.startDate == null) {
                if (this.initialState == null) {
                    // No provided initial state
                    throw new PropagationException(
                        PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
                }
                this.startDate = this.initialState.getDate();
            }
            // Propagation
            return this.propagate(this.startDate, target);
        } catch (final PatriusException oe) {

            // recover a possible embedded PropagationException
            for (Throwable t = oe; t != null; t = t.getCause()) {
                if (t instanceof PropagationException) {
                    throw (PropagationException) t;
                }
            }
            // Throw exception
            throw new PropagationException(oe);

        }
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate tStart, final AbsoluteDate tEnd)
                                                                                        throws PropagationException {
        try {

            if (this.initialState == null) {
                throw new PropagationException(
                    PatriusMessages.INITIAL_STATE_NOT_SPECIFIED_FOR_ORBIT_PROPAGATION);
            }

            if (!tStart.equals(this.initialState.getDate())) {
                // if propagation start date is not initial date,
                // propagate from initial to start date without event detection
                this.propagate(tStart, false);
                // Detectors should be entirely built next time
            }

            // propagate from start date to end date with event detection
            return this.propagate(tEnd, true);

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
     * @return state at end of propagation
     * @exception PropagationException if orbit cannot be propagated
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CyclomaticComplexity check
    // Reason: Orekit code kept as such
    @SuppressWarnings("PMD.AvoidRethrowingException")
    private SpacecraftState propagate(final AbsoluteDate tEnd,
            final boolean activateHandlers) throws PropagationException {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CyclomaticComplexity check

        try {
            this.manageStateFrame();
            if (this.initialState.getDate().equals(tEnd)) {
                // don't extrapolate
                return this.initialState;
            }
            if (this.integrator == null) {
                throw new PropagationException(
                    PatriusMessages.ODE_INTEGRATOR_NOT_SET_FOR_ORBIT_PROPAGATION);
            }

            // Specific handling of Cowell integrator
            if (this.integrator instanceof CowellIntegrator) {
                ((CowellIntegrator) this.integrator).setMapper(new SecondOrderMapper(
                        this.addEquationsAndTolerances));
                if (!this.orbitType.equals(OrbitType.CARTESIAN)) {
                    // Cowell integration must be performed in cartesian coordinates
                    throw new PropagationException(PatriusMessages.COWELL_REQUIRES_CARTESIAN_COORDINATES);
                }
            }

            // Copy the list of step handlers into the integrator before propagation
            // Mass model handler is set in first position to ensure all other handlers will have
            // synchronized masses
            this.integrator.clearStepHandlers();
            if (this.massModelHandler != null) {
                this.integrator.addStepHandler(this.massModelHandler);
            }
            if (this.modeEphemerisHandler != null) {
                this.integrator.addStepHandler(this.modeEphemerisHandler);
            }
            if (this.modeMasterHandler != null) {
                this.integrator.addStepHandler(this.modeMasterHandler);
            }

            // space dynamics view
            this.referenceDate = this.initialState.getDate();

            // set Mu
            if (Double.isNaN(this.getMu())) {
                this.setMu(this.initialState.getOrbit().getMu());
            }
            // Add attitude to additional state map in initial SpacecraftState
            for (int i = 0; i < this.addEquationsAndTolerances.size(); i++) {
                final String name = this.addEquationsAndTolerances.get(i).getEquations().getName();
                if (name.equals(AttitudeType.ATTITUDE.toString())) {
                    this.initialState =
                        this.initialState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);
                } else {
                    if (name.equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                        this.initialState =
                            this.initialState
                                .addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES);
                    }
                    if (name.equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                        this.initialState =
                            this.initialState
                                .addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);
                    }
                }
            }

            // Check correspondence between additional states and additional equations
            this.checkStatesEquations();

            this.currentState = this.initialState;

            // Get additional states infos
            this.addStateInfos = this.currentState.getAdditionalStatesInfos();

            /*
             * Loop on every force models. If the force model implement the interface StepHandler (for now, only
             * ContinuousThrustManeuver does it), then it's added as a step handler to the integrator.
             */
            for (final ForceModel forceModel : this.forceModels) {
                if (forceModel instanceof PatriusStepHandler) {
                    final AdaptedStepHandler stepHandler = new AdaptedStepHandler((PatriusStepHandler) forceModel);
                    stepHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderByDefault,
                        null, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                    this.integrator.addStepHandler(stepHandler);
                }
            }
            
            // initialize mode handler
            switch (this.mode) {
                case MASTER_MODE:
                    break;
                case EPHEMERIS_GENERATION_MODE:
                    break;
                default:
                    // this should be slave mode
                    break;
            }
            if (this.modeMasterHandler != null) {
                // If single attitude treatment expected
                if (this.attitudeProviderByDefault == null) {
                    this.modeMasterHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderForces,
                        this.attitudeProviderEvents, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                } else {
                    this.modeMasterHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderByDefault,
                        null, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                }
            }
            if (this.modeEphemerisHandler != null) {
                final double duration = tEnd.durationFrom(this.initialState.getDate());
                // If single attitude treatment expected
                if (this.attitudeProviderByDefault == null) {
                    this.modeEphemerisHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderForces,
                        this.attitudeProviderEvents, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                    this.modeEphemerisHandler.setForward(duration >= 0);
                } else {
                    this.modeEphemerisHandler.initialize(this.orbitType, this.angleType,
                        this.attitudeProviderByDefault, null, this.addStateInfos, activateHandlers,
                        this.referenceDate, this.initialState.getFrame(), this.newtonianAttraction.getMu());
                    this.modeEphemerisHandler.setForward(duration >= 0);
                }
            }
            if (this.massModelHandler != null) {
                // If single attitude treatment expected
                if (this.attitudeProviderByDefault == null) {
                    this.massModelHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderForces,
                        this.attitudeProviderEvents, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                } else {
                    this.massModelHandler.initialize(this.orbitType, this.angleType, this.attitudeProviderByDefault,
                        null, this.addStateInfos, activateHandlers, this.referenceDate,
                        this.initialState.getFrame(), this.newtonianAttraction.getMu());
                }
            }

            // Check all force models have proper data on propagation interval
            for (final ForceModel forceModel : this.forceModels) {
                forceModel.checkData(this.initialState.getDate(), tEnd);
            }

            // creating state vector
            this.stateVector = new double[this.getDimension()];

            final AbsoluteDate initialDate = this.initialState.getDate();
            // mathematical view
            final double t0 = 0;
            double t1 = tEnd.preciseDurationFrom(initialDate);

            final AbsoluteDate t = initialDate.shiftedBy(t1);
            double dt = tEnd.preciseDurationFrom(t);
            // Avoid overshoot
            if (dt != 0.) {
                if ((t1 > 0.) ^ (dt > 0.)) {
                    t1 = MathLib.nextAfter(t1, 0.);
                }
                if (this.mode == MASTER_MODE) {
                    this.integrator.handleLastStep(false);
                }
            }

            // Map state to array
            this.currentState.mapStateToArray(this.orbitType, this.angleType, this.stateVector);

            // Add event handlers
            this.addEventHandlers(activateHandlers);

            // mathematical integration
            if (!this.addEquationsAndTolerances.isEmpty()) {
                this.buildAdditionalTolerances();
            }
            this.expandToleranceArray();

            final DifferentialEquations diffEq = new DifferentialEquations();
            double stopTime;
            AbsoluteDate date = initialDate;
            try {
                stopTime = this.integrator.integrate(diffEq, t0, this.stateVector, t1, this.stateVector);
                date = initialDate.shiftedBy(stopTime);

                if (!date.equals(tEnd) && t1 == stopTime) {
                    // Propagation went to the end (no stop event)
                    // but final date is not strictly equal to expected final date (due to round-off
                    // errors)
                    // Propagate again to match exact expected date

                    // Compute dt on which to propagate
                    dt = tEnd.preciseDurationFrom(date);

                    // Activate last step to inform user this is the last step
                    this.integrator.handleLastStep(true);

                    // 2nd initialization
                    this.referenceDate = date;
                    this.addEventHandlers(activateHandlers);
                    if (this.modeMasterHandler != null) {
                        this.modeMasterHandler.setReference(this.referenceDate);
                    }
                    if (this.modeEphemerisHandler != null) {
                        this.modeEphemerisHandler.setReference(this.referenceDate);
                    }
                    if (this.massModelHandler != null) {
                        this.massModelHandler.setReference(this.referenceDate);
                    }

                    // 2nd integration
                    stopTime = this.integrator.integrate(diffEq, t0, this.stateVector, dt, this.stateVector);
                    date = date.shiftedBy(stopTime);
                }

            } catch (final PatriusExceptionWrapper oew) {
                throw oew.getException();
            }

            if (!this.addEquationsAndTolerances.isEmpty()) {
                this.resetToleranceArray();
            }

            // get final state
            // If single attitude treatment expected
            if (this.attitudeProviderByDefault == null) {
                this.initialState =
                    new SpacecraftState(this.stateVector, this.orbitType, this.angleType, date, this.getMu(),
                        this.initialState.getFrame(), this.addStateInfos, this.attitudeProviderForces,
                        this.attitudeProviderEvents);
            } else {
                this.initialState =
                    new SpacecraftState(this.stateVector, this.orbitType, this.angleType, date, this.getMu(),
                        this.initialState.getFrame(), this.addStateInfos, this.attitudeProviderByDefault,
                        null);
            }

            this.startDate = date;
            return this.initialState;

        } catch (final PropagationException pe) {
            throw pe;
        } catch (final PatriusException oe) {
            throw new PropagationException(oe);
        } catch (final MathIllegalArgumentException miae) {
            throw PropagationException.unwrap(miae);
        } catch (final MathIllegalStateException mise) {
            throw PropagationException.unwrap(mise);
        }
    }

    /**
     * Manage the state frame : the orbit to propagate is converted in the propagation frame.
     * 
     * @throws PatriusException if the frame of the initial state is not inertial or pseudo-inertial
     */
    private void manageStateFrame() throws PatriusException {

        if (this.propagationFrame == null) {
            // Propagation frame has not been provided: frame used is orbit frame is inertial or
            // pseudo-inertial
            if (this.getInitialState().getFrame().isPseudoInertial()) {
                this.propagationFrame = this.getInitialState().getFrame();
                this.newtonianAttraction = new NewtonianAttractionModel(this.propagationFrame,
                    newtonianAttraction.getMuParameter());
            } else {
                throw new PatriusException(PatriusMessages.NOT_INERTIAL_FRAME);
            }
        } else {
            // Propagation frame has been provided: convert initial state in propagation frame
            if (this.getInitialState().getFrame() != this.propagationFrame) {
                final Orbit initOrbit = this.getInitialState().getOrbit();
                final OrbitType type = initOrbit.getType();
                final Orbit propagationOrbit = type.convertOrbit(initOrbit, this.propagationFrame);

                // Update attitude in right frame
                Attitude propagationAttitude = null;
                if (this.getInitialState().getAttitude() != null) {
                    propagationAttitude =
                        this.getInitialState().getAttitude().withReferenceFrame(this.propagationFrame);
                }
                Attitude propagationAttitudeEvents = null;
                if (this.getInitialState().getAttitudeEvents() != null) {
                    propagationAttitudeEvents =
                        this.getInitialState().getAttitudeEvents().withReferenceFrame(
                            this.propagationFrame);
                }

                this.initialState =
                    new SpacecraftState(propagationOrbit, propagationAttitude,
                        propagationAttitudeEvents, this.getInitialState().getAdditionalStates());
            }
        }
    }

    /**
     * Check correspondence between additional states and additional equations.
     * 
     * @throws PatriusException if additional states from initial state and additional equations
     *         added in the propagator does not correspond
     */
    private void checkStatesEquations() throws PatriusException {
        // Get additional states from initial SpacecraftState
        final Map<String, double[]> additionalStates = this.initialState.getAdditionalStates();

        if (!additionalStates.isEmpty() && !this.addEquationsAndTolerances.isEmpty()) {
            // Check correspondence between additional states numbers and additional equations
            // numbers
            final int eqsSize = this.addEquationsAndTolerances.size();
            if (additionalStates.size() == eqsSize) {
                // Check names correspondence
                for (int i = 0; i < eqsSize; i++) {
                    final String eqName = this.addEquationsAndTolerances.get(i).getEquations().getName();
                    if (!additionalStates.containsKey(eqName)) {
                        throw new PatriusException(
                            PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
                    }
                }
            } else {
                throw new PatriusException(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
            }
        } else if (additionalStates.isEmpty() ^ this.addEquationsAndTolerances.isEmpty()) {
            // Either additional states from SpacecraftStare are empty or additional equations are
            // empty (XOR)
            throw new PatriusException(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
        }
    }

    /**
     * Build the arrays of tolerances for additional parameters.
     * 
     * @throws PatriusException if tolerances array size differs from additional states vector size
     * 
     */
    private void buildAdditionalTolerances() throws PatriusException {
        if (this.integrator instanceof AdaptiveStepsizeIntegrator) {
            final int dim = this.getDimension() - this.getBasicDimension();
            this.addStatesAbsoluteTolerances = new double[dim];
            this.addStatesRelativeTolerances = new double[dim];
            int idxAbs = 0;
            int idxRel = 0;
            for (final AdditionalEquationsAndTolerances stateAndTol : this.addEquationsAndTolerances) {
                // At this stage, we are sure that the additional states correspond with the
                // additional tolerances
                final int sLength =
                    this.initialState.getAdditionalStates()
                        .get(stateAndTol.getEquations().getName()).length;
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

                // additional tolerance arrays filled in the order
                // matching the state vector definition
                for (final double elt : absTol) {
                    this.addStatesAbsoluteTolerances[idxAbs] = elt;
                    idxAbs++;
                }
                for (final double elt : relTol) {
                    this.addStatesRelativeTolerances[idxRel] = elt;
                    idxRel++;
                }
            }
        }
        
        // Additional step: store in AdditionalEquationsAndTolerances objects pos in first/second order state vector
        int index = 3;
        for (final Entry<String, double[]> addState : this.initialState.getAdditionalStates().entrySet()) {
            // Find corresponding equation
            for (final AdditionalEquationsAndTolerances stateAndTol : this.addEquationsAndTolerances) {
                if (stateAndTol.getEquations().getName().equals(addState.getKey())) {
                    // Update index
                    stateAndTol.setIndex1stOrder(NumericalPropagator.this.addStateInfos.get(
                            stateAndTol.getEquations().getName()).getIndex());
                    stateAndTol.setIndex2ndOrder(index);
                    index += stateAndTol.getEquations().getSecondOrderDimension();
                    break;
                }
            }
        }
    }

    /**
     * Expand integrator tolerance array to fit compound state vector.
     */
    private void expandToleranceArray() {
        if (this.integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = this.getDimension();
            resizeArray((AdaptiveStepsizeIntegrator) this.integrator, n,
                    this.addStatesAbsoluteTolerances, this.addStatesRelativeTolerances);
        }
    }

    /**
     * Reset integrator tolerance array to original size.
     */
    private void resetToleranceArray() {
        if (this.integrator instanceof AdaptiveStepsizeIntegrator) {
            final int n = this.stateVector.length;
            resizeArray((AdaptiveStepsizeIntegrator) this.integrator, n, null, null);
        }
    }

    /**
     * Resize object internal array.
     * 
     * @param integ adaptive-stepsize integrator
     * @param newSize new problem dimension
     * @param newTolerancesAbs absolute tolerance values array, supersedes filler, may be null
     * @param newTolerancesRel relative tolerance values array, supersedes filler, may be null
     */
    private static void resizeArray(final AdaptiveStepsizeIntegrator integ, final int newSize,
                             final double[] newTolerancesAbs, final double[] newTolerancesRel) {
        // try to get the vector error field of the AdaptiveStepsizeIntegrator class:
        final double[] resizedArrayAbs = new double[newSize];
        final double[] resizedArrayRel = new double[newSize];
        final double[] originalArrayAbs;
        final double[] originalArrayRel;
        if (integ.getVecAbsoluteTolerance() == null) {
            // The absolute and relative state tolerances are represented by a scalar value:
            final int originalSize = 6;
            originalArrayAbs = new double[originalSize];
            originalArrayRel = new double[originalSize];
            // create a 6-components array containing the tolerance scalar value
            for (int i = 0; i < originalSize; i++) {
                originalArrayAbs[i] = integ.getScalAbsoluteTolerance();
                originalArrayRel[i] = integ.getScalRelativeTolerance();
            }
            if (newSize > originalSize) {
                // expand array
                System.arraycopy(originalArrayAbs, 0, resizedArrayAbs, 0, originalSize);
                System.arraycopy(originalArrayRel, 0, resizedArrayRel, 0, originalSize);
                // Complement with newTolerances
                System.arraycopy(newTolerancesAbs, 0, resizedArrayAbs, originalSize, newTolerancesAbs.length);
                System.arraycopy(newTolerancesRel, 0, resizedArrayRel, originalSize, newTolerancesRel.length);
            } else {
                // shrink array
                System.arraycopy(originalArrayAbs, 0, resizedArrayAbs, 0, newSize);
                System.arraycopy(originalArrayRel, 0, resizedArrayRel, 0, newSize);
            }
            // store the new resized array in the AdaptiveStepsizeIntegrator tolerance array:
            integ.setVecAbsoluteTolerance(resizedArrayAbs);
            integ.setVecRelativeTolerance(resizedArrayRel);
        } else {
            // the absolute and relative tolerances are represented by an array:
            originalArrayAbs = integ.getVecAbsoluteTolerance();
            originalArrayRel = integ.getVecRelativeTolerance();
            final int originalSize = originalArrayAbs.length;

            if (newSize > originalSize) {
                // expand array
                System.arraycopy(originalArrayAbs, 0, resizedArrayAbs, 0, originalSize);
                System.arraycopy(originalArrayRel, 0, resizedArrayRel, 0, originalSize);
                // Complement with newTolerances
                System.arraycopy(newTolerancesAbs, 0, resizedArrayAbs, originalSize, newTolerancesAbs.length);
                System.arraycopy(newTolerancesRel, 0, resizedArrayRel, originalSize, newTolerancesRel.length);
            } else {
                // shrink array
                System.arraycopy(originalArrayAbs, 0, resizedArrayAbs, 0, newSize);
                System.arraycopy(originalArrayRel, 0, resizedArrayRel, 0, newSize);
            }
            integ.setVecAbsoluteTolerance(resizedArrayAbs);
            integ.setVecRelativeTolerance(resizedArrayRel);
        }
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame)
                                                                                     throws PatriusException {
        return this.propagate(date).getPVCoordinates(frame);
    }
    
    /** {@inheritDoc} */
    @Override
    public SpacecraftState getSpacecraftState(final AbsoluteDate date) throws PropagationException {
        return this.propagate(date);
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

    /**
     * Add event handlers to integrator.
     * 
     * @param activateHandlers if true, step and event handlers should be activated
     */
    private void addEventHandlers(final boolean activateHandlers) {

        // Clear event handlers
        this.integrator.clearEventHandlers();

        // Add null mass detector (in first position since it leads to stop propagation,
        // in case of event occurring
        // at the same time)
        if (this.nullMassDetector != null) {
            this.setUpEventDetector(this.nullMassDetector);
        }

        // set up events added by user
        if (activateHandlers) {
            for (final EventDetector detector : this.detectors) {
                this.setUpEventDetector(detector);
            }
        }

        // set up events related to force models
        for (final ForceModel forceModel : this.forceModels) {
            final EventDetector[] modelDetectors = forceModel.getEventsDetectors();
            if (modelDetectors != null) {
                for (final EventDetector detector : modelDetectors) {
                    this.setUpEventDetector(detector);
                }
            }
        }
    }

    /**
     * Wrap an Orekit event detector and register it to the integrator.
     * 
     * @param osf event handler to wrap
     */
    protected void setUpEventDetector(final EventDetector osf) {
        final EventHandler handler;
        if (this.attitudeProviderByDefault == null) {
            handler =
                new AdaptedEventDetector(osf, this.addStateInfos, this.orbitType, this.angleType,
                    this.attitudeProviderForces, this.attitudeProviderEvents, this.referenceDate,
                    this.newtonianAttraction.getMu(), this.initialState.getFrame());
        } else {
            handler =
                new AdaptedEventDetector(osf, this.addStateInfos, this.orbitType, this.angleType,
                    this.attitudeProviderByDefault, null, this.referenceDate,
                    this.newtonianAttraction.getMu(), this.initialState.getFrame());
        }

        this.integrator.addEventHandler(handler, osf.getMaxCheckInterval(), osf.getThreshold(),
            osf.getMaxIterationCount());
    }

    /**
     * Get state vector dimension without additional parameters.
     * 
     * @return state vector dimension without additional parameters.
     */
    public int getBasicDimension() {
        return SpacecraftState.ORBIT_DIMENSION;

    }

    /**
     * Compute complete state vector dimension.
     * 
     * @return state vector dimension
     */
    public int getDimension() {
        int sum = this.getBasicDimension();
        for (final Entry<String, AdditionalStateInfo> entry : this.addStateInfos.entrySet()) {
            final AdditionalStateInfo stateInfo = entry.getValue();
            sum += stateInfo.getSize();
        }
        return sum;
    }

    /**
     * Estimate tolerance vectors for integrators.
     * <p>
     * The errors are estimated from partial derivatives properties of orbits, starting from a scalar position error
     * specified by the user. Considering the energy conservation equation V = sqrt(mu (2/r - 1/a)), we get at constant
     * energy (i.e. on a Keplerian trajectory):
     * 
     * <pre>
     * V<sup>2</sup> r |dV| = mu |dr|
     * </pre>
     * 
     * So we deduce a scalar velocity error consistent with the position error. From here, we apply orbits Jacobians
     * matrices to get consistent errors on orbital parameters.
     * </p>
     * <p>
     * The tolerances are only <em>orders of magnitude</em>, and integrator tolerances are only local estimates, not
     * global ones. So some care must be taken when using these tolerances. Setting 1mm as a position error does NOT
     * mean the tolerances will guarantee a 1mm error position after several orbits integration.
     * </p>
     * 
     * @param dP user specified position error
     * @param orbit reference orbit
     * @param type propagation type for the meaning of the tolerance vectors elements (it may be
     *        different from {@code orbit.getType()})
     * @return a two rows array, row 0 being the absolute tolerance error and row 1 being the
     *         relative tolerance error
     */
    public static double[][] tolerances(final double dP, final Orbit orbit, final OrbitType type) {

        // estimate the scalar velocity error
        final PVCoordinates pv = orbit.getPVCoordinates();
        final double r2 = pv.getPosition().getNormSq();
        final double v = pv.getVelocity().getNorm();
        final double dV = orbit.getMu() * dP / (v * r2);

        // Initialization
        final double[] absTol = new double[6];
        final double[] relTol = new double[6];

        if (type == OrbitType.CARTESIAN) {
            // Cartesian case: direct
            absTol[0] = dP;
            absTol[1] = dP;
            absTol[2] = dP;
            absTol[3] = dV;
            absTol[4] = dV;
            absTol[5] = dV;
        } else {

            // convert the orbit to the desired type
            final double[][] jacobian = new double[6][6];
            final Orbit converted = type.convertType(orbit);
            converted.getJacobianWrtCartesian(PositionAngle.TRUE, jacobian);

            for (int i = 0; i < 6; ++i) {
                final double[] row = jacobian[i];
                absTol[i] =
                    MathLib.abs(row[0]) * dP + MathLib.abs(row[1]) * dP
                        + MathLib.abs(row[2]) * dP + MathLib.abs(row[3]) * dV
                        + MathLib.abs(row[4]) * dV + MathLib.abs(row[5]) * dV;
            }

        }

        Arrays.fill(relTol, dP / MathLib.sqrt(r2));

        // Return tolerances array
        return new double[][] { absTol, relTol };

    }

    /** {@inheritDoc} */
    @Override
    public void update(final Observable o, final Object eventHandlerIn) {
        final EventDetector detector = ((AdaptedEventDetector) eventHandlerIn).getDetector();
        this.detectors.remove(detector);
    }

    /**
     * Method used to disable Newtonian attraction which is active by default.
     */
    public void disableNewtonianAttraction() {
        this.disableNewtonianAttractionFlag = true;
    }
    
    /** Internal class for differential equations representation. */
    private class DifferentialEquations implements FirstOrderDifferentialEquations, SecondOrderDifferentialEquations,
            TimeDerivativesEquations {

        /** Serializable UID. */
        private static final long serialVersionUID = -1927530118454989452L;

        /** Reference to the derivatives array to initialize. */
        private double[] storedYDot;

        /** Jacobian of the orbital parameters with respect to the cartesian parameters. */
        private final double[][] jacobian;

        /** Build a new instance. */
        public DifferentialEquations() {
            NumericalPropagator.this.calls = 0;
            this.jacobian = new double[6][6];
        }

        /** {@inheritDoc} */
        @Override
        public int getDimension() {
            return NumericalPropagator.this.getDimension();
        }

        /** {@inheritDoc} */
        @Override
        public void computeDerivatives(final double t, final double[] y, final double[] yDot) {

            try {
                // update space dynamics view
                final AbsoluteDate currentDate = NumericalPropagator.this.referenceDate.shiftedBy(t);
                if (NumericalPropagator.this.attitudeProviderByDefault == null) {
                    NumericalPropagator.this.currentState =
                        new SpacecraftState(y, NumericalPropagator.this.orbitType, NumericalPropagator.this.angleType,
                            currentDate,
                            NumericalPropagator.this.currentState.getMu(),
                            NumericalPropagator.this.currentState.getFrame(), NumericalPropagator.this.addStateInfos,
                            NumericalPropagator.this.attitudeProviderForces,
                            NumericalPropagator.this.attitudeProviderEvents);
                } else {
                    NumericalPropagator.this.currentState =
                        new SpacecraftState(y, NumericalPropagator.this.orbitType, NumericalPropagator.this.angleType,
                            currentDate,
                            NumericalPropagator.this.currentState.getMu(),
                            NumericalPropagator.this.currentState.getFrame(), NumericalPropagator.this.addStateInfos,
                            NumericalPropagator.this.attitudeProviderByDefault, null);
                }

                // initialize derivatives
                this.initDerivatives(yDot, NumericalPropagator.this.currentState.getOrbit());

                // compute the contributions of all perturbing forces
                for (final ForceModel forceModel : NumericalPropagator.this.forceModels) {
                    forceModel.addContribution(NumericalPropagator.this.currentState, this);
                }

                // finalize derivatives by adding the Kepler contribution
                if (!NumericalPropagator.this.disableNewtonianAttractionFlag) {
                    NumericalPropagator.this.newtonianAttraction.addContribution(NumericalPropagator.this.currentState,
                            this);
                } else {
                    this.storedYDot[0] = y[3];
                    this.storedYDot[1] = y[4];
                    this.storedYDot[2] = y[5];
                }

                // Add contribution for additional state
                for (final AdditionalEquationsAndTolerances stateAndTol : 
                    NumericalPropagator.this.addEquationsAndTolerances) {

                    // compute additional derivatives and store it in the storedYDot vector
                    stateAndTol.getEquations().computeDerivatives(NumericalPropagator.this.currentState, this);
                }

                // increment calls counter
                ++NumericalPropagator.this.calls;

            } catch (final PatriusException oe) {
                throw new PatriusExceptionWrapper(oe);
            }

        }

        /** {@inheritDoc} */
        @Override
        public void initDerivatives(final double[] yDot, final Orbit currentOrbit)
                                                                                  throws PropagationException {
            for (final AdditionalEquationsAndTolerances stateAndTol : 
                NumericalPropagator.this.addEquationsAndTolerances) {
                final AdditionalEquations equations = stateAndTol.getEquations();
                if (equations instanceof MassEquation) {
                    ((MassEquation) stateAndTol.getEquations()).setMassDerivativeZero();
                }
            }
            this.storedYDot = yDot;
            Arrays.fill(this.storedYDot, 0.0);
            currentOrbit.getJacobianWrtCartesian(NumericalPropagator.this.angleType, this.jacobian);
        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final double mu) {
            NumericalPropagator.this.currentState.getOrbit().addKeplerContribution(NumericalPropagator.this.angleType,
                mu, this.storedYDot);
        }

        /** {@inheritDoc} */
        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            for (int i = 0; i < 6; ++i) {
                final double[] jRow = this.jacobian[i];
                this.storedYDot[i] += jRow[3] * x + jRow[4] * y + jRow[5] * z;
            }
        }

        /** {@inheritDoc} */
        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame)
                                                                            throws PatriusException {
            final Transform t =
                frame.getTransformTo(NumericalPropagator.this.currentState.getFrame(),
                    NumericalPropagator.this.currentState.getDate());
            final Vector3D gammInRefFrame = t.transformVector(gamma);
            this.addXYZAcceleration(gammInRefFrame.getX(), gammInRefFrame.getY(), gammInRefFrame.getZ());
        }

        /** {@inheritDoc} */
        @Override
        public void addAdditionalStateDerivative(final String name, final double[] pDot) {
            final AdditionalStateInfo addStateInfo = NumericalPropagator.this.addStateInfos.get(name);
            System.arraycopy(pDot, 0, this.storedYDot, addStateInfo.getIndex(), addStateInfo.getSize());
        }

        /** {@inheritDoc} */
        @Override
        public void computeSecondDerivatives(final double t,
                final double[] y,
                final double[] yDot,
                final double[] yDDot) {
            // Second order derivatives for integrators such as Cowell

            try {
                // At this point: y contains position, yDot contains velocity (cartesian coordinates only)

                // update space dynamics view
                final AbsoluteDate currentDate = NumericalPropagator.this.referenceDate.shiftedBy(t);
                // Build state
                final double[] x = ((CowellIntegrator) NumericalPropagator.this.integrator)
                        .getMapper().buildFullState(y, yDot);
                if (NumericalPropagator.this.attitudeProviderByDefault == null) {
                    NumericalPropagator.this.currentState =
                        new SpacecraftState(x, OrbitType.CARTESIAN, NumericalPropagator.this.angleType,
                            currentDate,
                            NumericalPropagator.this.currentState.getMu(),
                            NumericalPropagator.this.currentState.getFrame(), NumericalPropagator.this.addStateInfos,
                            NumericalPropagator.this.attitudeProviderForces,
                            NumericalPropagator.this.attitudeProviderEvents);
                } else {
                    NumericalPropagator.this.currentState =
                        new SpacecraftState(x, OrbitType.CARTESIAN, NumericalPropagator.this.angleType,
                            currentDate,
                            NumericalPropagator.this.currentState.getMu(),
                            NumericalPropagator.this.currentState.getFrame(), NumericalPropagator.this.addStateInfos,
                            NumericalPropagator.this.attitudeProviderByDefault, null);
                }
                
                // initialize derivatives
                Arrays.fill(yDDot, 0.0);
                for (final AdditionalEquationsAndTolerances stateAndTol : 
                    NumericalPropagator.this.addEquationsAndTolerances) {
                    final AdditionalEquations equations = stateAndTol.getEquations();
                    if (equations instanceof MassEquation) {
                        ((MassEquation) stateAndTol.getEquations()).setMassDerivativeZero();
                    }
                }

                // compute the contributions of all perturbing forces
                for (final ForceModel forceModel : NumericalPropagator.this.forceModels) {
                    final Vector3D acc = forceModel.computeAcceleration(NumericalPropagator.this.currentState);
                    yDDot[0] += acc.getX();
                    yDDot[1] += acc.getY();
                    yDDot[2] += acc.getZ();
                }

                // finalize derivatives by adding the Kepler contribution
                if (!NumericalPropagator.this.disableNewtonianAttractionFlag) {
                    final Vector3D acc = NumericalPropagator.this.newtonianAttraction
                            .computeAcceleration(NumericalPropagator.this.currentState);
                    yDDot[0] += acc.getX();
                    yDDot[1] += acc.getY();
                    yDDot[2] += acc.getZ();
                }

                // Add contribution for additional state
                for (final AdditionalEquationsAndTolerances stateAndTol : 
                    NumericalPropagator.this.addEquationsAndTolerances) {
                    // compute additional derivatives and store it in the storedYDot vector
                    final double[] secondDerivatives = stateAndTol.getEquations().computeSecondDerivatives(
                            NumericalPropagator.this.currentState);
                    // Put it in the right place in yDDot
                    System.arraycopy(secondDerivatives, 0, yDDot, stateAndTol.getIndex2ndOrder(),
                            secondDerivatives.length);
                }

                // increment calls counter
                ++NumericalPropagator.this.calls;

            } catch (final PatriusException oe) {
                throw new PatriusExceptionWrapper(oe);
            }
        }
    }
}
