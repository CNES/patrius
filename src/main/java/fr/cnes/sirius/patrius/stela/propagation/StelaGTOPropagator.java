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
 * @history created 18/01/2013
 *
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Courtes periodes traînee atmospherique et prs
 * VERSION:4.11:DM:DM-3235:22/05/2023:[PATRIUS][TEMPS_CALCUL] Attitude spacecraft state lazy
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.8:FA:FA-3009:15/11/2021:[PATRIUS] IllegalArgumentException SolarActivityToolbox
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:71:25/05/2013:Access modifiers updates.
 * VERSION::FA:64:31/05/2013:modification to perform osculating to mean
 * conversion only once, at the beginning of the propagation
 * VERSION::DM:91:26/07/2013:transform used only when needed when setting
 * initial parameters (cancelation problems)
 * VERSION::FA:183:14/03/2014:Corrected javadoc for the setdDragdt methods
 * VERSION::FA:180:18/03/2014:Added FA 71 history
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:406:20/02/2015:Checkstyle corrections (nb cyclomatic)
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * VERSION::FA:359:31/03/2015: Proper management of reentry case with step size control
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:403:20/10/2015:Improving ergonomics
 * VERSION::FA:463:09/11/2015:Minor changes on STELA features
 * VERSION::FA:1286:05/09/2017:correct osculating orbit propagation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.events.EventState;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.PerigeeAltitudeDetector;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.noninertial.NonInertialContribution;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * STELA propagator for Geostationnary Transfert Orbits (GTO).<br>
 *
 * @concurrency not thread-safe
 *
 * @concurrency.comment use of mutable attributes
 * @serial this class isn't fully serializable, in part because the integrator parameter (
 *         {@link FirstOrderIntegrator} doesn't extends {@link Serializable} for now)
 *
 * @author Tiziana Sabatini
 * @author Cedric Dental
 *
 * @version $Id$
 *
 * @since 1.3
 *
 */
@SuppressWarnings({ "PMD.NullAssignment", "PMD.ConstructorCallsOverridableMethod" })
public class StelaGTOPropagator extends StelaAbstractPropagator {

    /** Serializable UID. */
    private static final long serialVersionUID = 6976438319827286237L;

    /** Size of the state vector. */
    private static final int STATE_SIZE = 6;

    /** Was the exception mecanism triggered already */
    protected boolean wasException;

    /** Max date after which simulation has to fail if exception mecanism was triggered */
    protected AbsoluteDate maxDate;

    /** Step counter for drag computation. */
    protected int stepCounter;

    /** Frame to be used during the extrapolation (CIRF). */
    private final Frame integrationFrame;

    /** Gauss force models used during the extrapolation of the orbit. */
    private final List<AbstractStelaGaussContribution> gaussForces;

    /** Lagrange force models used during the extrapolation of the orbit. */
    private final List<AbstractStelaLagrangeContribution> lagrangeForces;

    /** Reference date. */
    private AbsoluteDate referenceDate;

    /** Current &mu;. */
    private double mu;

    /** The mass of the spacecraft. */
    private double mass;

    /** Additional equations and associated data. */
    private final List<StelaAdditionalEquations> addEquationsList;

    /** The mean / osculating orbit converter. */
    private OrbitNatureConverter converter;

    /** Drag re-computation parameter */
    private boolean recomputeDrag;

    /** Stored drag contribution. */
    private double[] dDragdt;

    /** Maximum number of original steps after which propagation is stopped */
    private final double maxShift;

    /** Minimum step size. */
    private final double minStepSize;

    /** Step handler for forces requiring update every step, not every substep. */
    private ForcesStepHandler forcesStepHandler;

    /**
     * Build a StelaGTOPropagator.
     * <p>
     * After creation, there is no orbit, method {@link #setInitialState setInitialState} must be
     * called. This constructor uses a Default Law as AttitudeProvider. For a specific Attitude
     * Provider please use the other constructor. After creation, there are no perturbing forces at
     * all. This means that if {@link #addForceModel
     * addForceModel} is not called after creation, the integrated orbit will follow a simple
     * Keplerian evolution.
     * </p>
     * <p>
     * During reentry, no correction is performed if some results are not physical. As a result, an
     * exception will be thrown if obtained results are not physical.
     * </p>
     *
     * @param integr
     *        the first order integrator to use for extrapolation
     *
     * @throws PatriusException
     *         exception in reset initial state
     */
    public StelaGTOPropagator(final FirstOrderIntegrator integr) throws PatriusException {
        this(integr, 0, 0);
    }

    /**
     * Build a StelaGTOPropagator.
     * <p>
     * After creation, there is no orbit, method {@link #setInitialState setInitialState} must be
     * called. This constructor uses a the AttitudeProvider given by the user. After creation, there
     * are no perturbing forces at all. This means that if {@link #addForceModel addForceModel} is
     * not called after creation, the integrated orbit will follow a simple Keplerian evolution.
     * </p>
     * <p>
     * No attitude provider is defined. The user should call
     * {@link #setAttitudeProvider(AttitudeProvider)} or
     * {@link #setAttitudeProviderEvents(AttitudeProvider)} or
     * {@link #setAttitudeProviderForces(AttitudeProvider)}
     * </p>
     *
     * @param integr
     *        the first order integrator to use for extrapolation
     * @param maxShiftIn
     *        Maximum number of integration steps STELA will try to correct to get some physical
     *        results
     *        (mostly during reentry since integration step is too large). If 0, not correction is
     *        performed.
     *        If more corrections are required, an exception will be thrown
     * @param minStepSizeIn
     *        Smallest integration stepsize allowed. STELA will try to reduce stepsize during
     *        reentry to try to
     *        get some physical results. If equals to integrator step size, not correction is done.
     *        If a smaller stepsize than provided value is required, an exception will be thrown
     *
     * @throws PatriusException
     *         exception in reset initial state
     */
    public StelaGTOPropagator(final FirstOrderIntegrator integr, final double maxShiftIn,
            final double minStepSizeIn) throws PatriusException {
        this(integr, null, new StelaBasicInterpolator(), maxShiftIn, minStepSizeIn);
    }

    /**
     * Build a StelaGTOPropagator.
     * <p>
     * After creation, there is no orbit, method {@link #setInitialState setInitialState} must be
     * called. This constructor uses a single AttitudeProvider given by the user. After creation,
     * there are no perturbing forces at all. This means that if {@link #addForceModel
     * addForceModel} is not called after creation, the integrated orbit will follow a simple
     * Keplerian evolution.
     * </p>
     *
     * @param integr
     *        the first order integrator to use for extrapolation
     * @param inAttitudeProvider
     *        attitude provider
     * @param inInter
     *        the interpolator
     * @param maxShiftIn
     *        Maximum number of integration steps STELA will try to correct to get some physical
     *        results
     *        (mostly during reentry since integration step is too large). If 0, not correction is
     *        performed.
     *        If more corrections are required, an exception will be thrown
     * @param minStepSizeIn
     *        Smallest integration stepsize allowed. STELA will try to reduce stepsize during
     *        reentry to try to
     *        get some physical results. If equals to integrator step size, not correction is done.
     *        If a smaller stepsize than provided value is required, an exception will be thrown
     *
     * @throws PatriusException
     *         exception in reset initial state
     */
    public StelaGTOPropagator(final FirstOrderIntegrator integr,
            final AttitudeProvider inAttitudeProvider, final StelaBasicInterpolator inInter,
            final double maxShiftIn, final double minStepSizeIn) throws PatriusException {
        // a single attitude provider is defined.
        super(inAttitudeProvider, inInter);

        this.gaussForces = new ArrayList<>();
        this.lagrangeForces = new ArrayList<>();
        this.addEquationsList = new ArrayList<>();
        this.referenceDate = null;
        this.mu = Double.NaN;
        this.mass = Double.NaN;
        this.integrationFrame = FramesFactory.getCIRF();
        this.recomputeDrag = true;
        this.stepCounter = 0;
        this.maxShift = maxShiftIn;
        this.minStepSize = minStepSizeIn;
        this.wasException = false;

        this.setIntegrator(integr);
        this.converter = new OrbitNatureConverter(this.getForceModels());
    }

    /**
     * Build a StelaGTOPropagator.
     * <p>
     * After creation, there is no orbit, method {@link #setInitialState setInitialState} must be
     * called. This constructor uses AttitudeProvider for forces and events computations given by
     * the user. After creation, there are no perturbing forces at all. This means that if
     * {@link #addForceModel addForceModel} is not called after creation, the integrated orbit will
     * follow a simple Keplerian evolution.
     * </p>
     *
     * @param integr
     *        the first order integrator to use for extrapolation
     * @param inAttitudeProviderForces
     *        attitude provider for forces computation
     * @param inAttitudeProviderEvents
     *        attitude provider for events computation
     * @param inInter
     *        the interpolator
     * @param maxShiftIn
     *        Maximum number of integration steps STELA will try to correct to get some physical
     *        results
     *        (mostly during reentry since integration step is too large). If 0, not correction is
     *        performed.
     *        If more corrections are required, an exception will be thrown
     * @param minStepSizeIn
     *        Smallest integration stepsize allowed. STELA will try to reduce stepsize during
     *        reentry to try to
     *        get some physical results. If equals to integrator step size, not correction is done.
     *        If a smaller stepsize than provided value is required, an exception will be thrown
     *
     * @throws PatriusException
     *         exception in reset initial state
     */
    public StelaGTOPropagator(final FirstOrderIntegrator integr,
            final AttitudeProvider inAttitudeProviderForces,
            final AttitudeProvider inAttitudeProviderEvents, final StelaBasicInterpolator inInter,
            final double maxShiftIn, final double minStepSizeIn) throws PatriusException {
        // if not specified, attitudeProvider is the default law.
        super(inAttitudeProviderForces, inAttitudeProviderEvents, inInter);

        this.gaussForces = new ArrayList<>();
        this.lagrangeForces = new ArrayList<>();
        this.addEquationsList = new ArrayList<>();
        this.referenceDate = null;
        this.mu = Double.NaN;
        this.mass = Double.NaN;
        this.integrationFrame = FramesFactory.getCIRF();
        this.recomputeDrag = true;
        this.stepCounter = 0;
        this.maxShift = maxShiftIn;
        this.minStepSize = minStepSizeIn;
        this.wasException = false;

        this.setIntegrator(integr);
        this.converter = new OrbitNatureConverter(this.getForceModels());
    }

    /**
     * @return the recomputeDrag
     */
    protected boolean isRecomputeDrag() {
        return this.recomputeDrag;
    }

    /**
     * Set the initial state.
     *
     * @param initialState
     *        initial state (no mass information should be provided)
     * @param massIn
     *        the initial mass
     * @param isOsculatingIn
     *        true if initial state is osculating
     * @throws PatriusException
     *         OrekitException
     * @see #propagate(AbsoluteDate)
     */
    public void setInitialState(final SpacecraftState initialState, final double massIn,
            final boolean isOsculatingIn) throws PatriusException {

        // Set start date
        this.setStartDate(initialState.getDate());

        // Convert to mean elements if necessary and get equinoctial orbit
        SpacecraftState initialState2 = initialState;
        if (isOsculatingIn) {
            // Reset transform
            MeeusSun.resetTransform();
            MeeusMoonStela.resetTransform();

            // Convert to mean elements
            final StelaEquinoctialOrbit equiOrbit = new StelaEquinoctialOrbit(
                    initialState.getOrbit());
            final StelaEquinoctialOrbit meanOrbit = this.converter.toMean(equiOrbit);
            initialState2 = initialState.updateOrbit(meanOrbit);
        }

        this.mu = initialState2.getOrbit().getMu();
        this.referenceDate = initialState2.getOrbit().getDate();
        this.mass = massIn;
        if (this.integrationFrame.getName().equals(initialState2.getFrame().getName())) {

            this.resetInitialState(initialState2);

        } else {
            // Frame tranformation
            final Transform transform = initialState2.getOrbit().getFrame()
                    .getTransformTo(this.integrationFrame, initialState2.getDate());

            final StelaEquinoctialOrbit initStateTransform = new StelaEquinoctialOrbit(
                    transform.transformPVCoordinates(initialState2.getPVCoordinates()),
                    this.integrationFrame, this.referenceDate, this.mu);

            SpacecraftState iniStateTransf = null;

            // Build the initial state with the correct attitude providers
            AttitudeProvider attProvForces = null;
            AttitudeProvider attProvEvents = null;
            if (this.attitudeProviderByDefault != null) {
                attProvForces = this.attitudeProviderByDefault;
            } else {
                attProvForces = this.attitudeProviderForces;
                attProvEvents = this.attitudeProviderEvents;
            }

            // Build initial state
            iniStateTransf = new SpacecraftState(attProvForces, attProvEvents, initStateTransform,
                    initialState2.getAdditionalStates());
            this.resetInitialState(iniStateTransf);
        }
    }

    /**
     * Add a force model to the global perturbation model.
     * <p>
     * If this method is not called at all, the integrated orbit will follow a simple keplerian
     * evolution.
     * </p>
     *
     * @param forceModel
     *        perturbing {@link StelaForceModel} to add
     * @see #removeForceModels()
     */
    public void addForceModel(final StelaForceModel forceModel) {
        if ("GAUSS".equals(forceModel.getType())) {
            // Gauss force model:
            this.gaussForces.add((AbstractStelaGaussContribution) forceModel);
        } else {
            // Lagrange force model:
            this.lagrangeForces.add((AbstractStelaLagrangeContribution) forceModel);
        }
        // Update nature converter
        this.converter = new OrbitNatureConverter(this.getForceModels());
    }

    /**
     * Set the force models to be used in the mean <=> osculating converter.
     * By default, all force models are included.
     * <p>Beware to call this method after the {@link #addForceModel(StelaForceModel)} method
     * since the latter overrides the nature converter.</p>
     * @param forceModels force models to be used in the mean <=> osculating converter
     */
    public void setNatureConverter(final List<StelaForceModel> forceModels) {
        this.converter = new OrbitNatureConverter(forceModels);
    }

    /**
     * Remove all perturbing force models from the global perturbation model.
     * <p>
     * Once all perturbing forces have been removed (and as long as no new force model is added),
     * the integrated orbit will follow a simple keplerian evolution.
     * </p>
     *
     * @see #addForceModel(StelaForceModel)
     */
    public void removeForceModels() {
        this.gaussForces.clear();
        this.lagrangeForces.clear();
        this.converter = new OrbitNatureConverter(this.getForceModels());
    }

    /**
     * Get perturbing force models list.
     *
     * @return list of all perturbing force models
     * @see #addForceModel(StelaForceModel)
     * @see #removeForceModels()
     */
    public List<StelaForceModel> getForceModels() {
        final List<StelaForceModel> forces = new ArrayList<>();
        forces.addAll(this.gaussForces);
        forces.addAll(this.lagrangeForces);
        return forces;
    }

    /**
     * Get perturbing Gauss force models list.
     *
     * @return list of Gauss perturbing force models
     * @see #addForceModel(StelaForceModel)
     * @see #removeForceModels()
     */
    public List<AbstractStelaGaussContribution> getGaussForceModels() {
        return this.gaussForces;
    }

    /**
     * Get perturbing Lagrange force models list.
     *
     * @return list of Lagrange perturbing force models
     * @see #addForceModel(StelaForceModel)
     * @see #removeForceModels()
     */
    public List<AbstractStelaLagrangeContribution> getLagrangeForceModels() {
        return this.lagrangeForces;
    }

    /**
     * Add a set of user-specified additional equations to be integrated along with the orbit
     * propagation.
     *
     * @param addEquations
     *        additional equations
     * @see NumericalPropagator
     */
    public void addAdditionalEquations(final StelaAdditionalEquations addEquations) {
        this.addEquationsList.add(addEquations);
    }

    /**
     * Add a set of user-specified attitude equations to be integrated along with the orbit
     * propagation.
     * If the set of attitude equations is already registered for the current attitude, it is
     * replaced by the new one.
     *
     * @param addEqu
     *        attitude additional equations for Stela GTO propagator
     */
    public void addAttitudeEquation(final StelaAttitudeAdditionalEquations addEqu) {
        final AttitudeType type = addEqu.getAttitudeType();
        // check the attitude type to be integrated
        switch (type) {
            case ATTITUDE_FORCES:
                this.checkTwoAttitudesTreatment();
                if ((this.attitudeProviderForces != null)) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                            .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE_EVENTS:
                this.checkTwoAttitudesTreatment();
                if ((this.attitudeProviderEvents != null)) {
                    // An AttitudeProvider is already defined for this Attitude
                    throw PatriusException
                            .createIllegalStateException(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED);
                }
                break;
            case ATTITUDE:
                this.checkSingleAttitudeTreatment();
                if ((this.attitudeProviderByDefault != null)) {
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

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProvider(final AttitudeProvider attitudeProvider) {
        this.checkSingleAttitudeTreatment();
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            if (this.addEquationsList.get(i).getName().equals(AttitudeType.ATTITUDE.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }

        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        super.setAttitudeProvider(attitudeProvider);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderForces(final AttitudeProvider attitudeForcesProvider) {
        this.checkTwoAttitudesTreatment();
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            if (this.addEquationsList.get(i).getName()
                    .equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }

        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        super.setAttitudeProviderForces(attitudeForcesProvider);
    }

    /** {@inheritDoc} */
    @Override
    public void setAttitudeProviderEvents(final AttitudeProvider attitudeEventsProvider) {
        this.checkTwoAttitudesTreatment();
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            if (this.addEquationsList.get(i).getName()
                    .equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                // An additional equation is already defined for this Attitude
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED);
            }
        }

        // If an AttitudeProvider was already defined by the user or by default, it is replaced by
        // the new one
        super.setAttitudeProviderEvents(attitudeEventsProvider);
    }

    /**
     * Check if a single attitude treatment is expected
     *
     * @since 2.3
     *
     * @throws IllegalStateException
     *         If an attitude provider or an additional equation is already defined for a specific
     *         attitude (for
     *         events or forces computation)
     */
    private void checkSingleAttitudeTreatment() {
        if ((this.attitudeProviderForces != null) || (this.attitudeProviderEvents != null)) {
            throw PatriusException
                    .createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
        }
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            final String name = this.addEquationsList.get(i).getName();
            if ((name.equals(AttitudeType.ATTITUDE_FORCES.toString()))
                    || (name.equals(AttitudeType.ATTITUDE_EVENTS.toString()))) {
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED);
            }
        }
    }

    /**
     * Check if a two attitude treatment is expected
     *
     * @since 2.3
     *
     * @throws IllegalStateException
     *         If an attitude provider or an additional equation is already defined by default for a
     *         single attitude
     */
    private void checkTwoAttitudesTreatment() {
        if (this.attitudeProviderByDefault != null) {
            throw PatriusException
                    .createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
        }
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            final String name = this.addEquationsList.get(i).getName();
            if (name.equals(AttitudeType.ATTITUDE.toString())) {
                throw PatriusException
                        .createIllegalStateException(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState propagate(final AbsoluteDate start, final AbsoluteDate target)
            throws PropagationException {
        return super.propagate(start, target);
    }

    /** {@inheritDoc} */
    @Override
    protected List<PatriusStepHandler> getStepHandlers() {
        final List<PatriusStepHandler> stepHandlers = new ArrayList<>();

        // Check if some forces have to be handled via step handler
        // i.e. forces requiring to be updated every step and not every substep
        NonInertialContribution nonInertialContribution = null;
        for (int i = 0; i < this.gaussForces.size(); i++) {
            if (this.gaussForces.get(i).getClass().equals(NonInertialContribution.class)) {
                nonInertialContribution = (NonInertialContribution) this.gaussForces.get(i);
            }
        }

        // Build and return list
        this.forcesStepHandler = new ForcesStepHandler(this.converter, nonInertialContribution);
        stepHandlers.add(this.forcesStepHandler);
        return stepHandlers;
    }

    /**
     *
     * tests whether the initial orbit is under Perigee Altitude
     *
     * @param initialState2
     *        the initial state
     * @throws PatriusException
     *         the orekit exception
     *
     * @since 1.3
     */
    protected void initialOrbitTest(final SpacecraftState initialState2) throws PatriusException {
        // tests whether the initial orbit is under Perigee Altitude
        for (final EventState state : this.eventsStates) {
            if (state.getEventDetector().getClass().equals(PerigeeAltitudeDetector.class)) {
                final double g = state.getEventDetector().g(initialState2);

                if (g < 0) {
                    throw PatriusException.createIllegalArgumentException(
                            PatriusMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, -g,
                            ((PerigeeAltitudeDetector) (state.getEventDetector())).getAltitude());
                }
            }
        }

    }

    /**
     * {@inheritDoc} Note that mean elements are returned.
     */
    @Override
    protected SpacecraftState propagateSpacecraftState(final AbsoluteDate date)
            throws PatriusException {

        // deals with date greater than the authorized one.
        if (this.wasException && (date.compareTo(this.maxDate) > 0)) {
            throw new PropagationException(PatriusMessages.PDB_PROPAGATION_NO_REENTRY,
                    this.maxShift);
        }

        // get current initial state and date
        final StelaEquinoctialOrbit equiOrbit = new StelaEquinoctialOrbit(this.getInitialState()
                .getOrbit());
        SpacecraftState initState = this.getInitialState().updateOrbit(equiOrbit);
        final AbsoluteDate initialDate = initState.getDate();

        if (initialDate.equals(date)) {
            // tests whether the initial orbit is under Perigee Altitude
            this.initialOrbitTest(initState);
            // don't extrapolate, return current SpacecraftState
            return initState;
        }

        // Add attitude to additional state map in initial SpacecraftState
        for (int i = 0; i < this.addEquationsList.size(); i++) {
            final String name = this.addEquationsList.get(i).getName();
            if (name.equals(AttitudeType.ATTITUDE.toString())) {
                initState = initState.addAttitudeToAdditionalStates(AttitudeType.ATTITUDE);
            } else {
                if (name.equals(AttitudeType.ATTITUDE_FORCES.toString())) {
                    initState = initState
                            .addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_FORCES);
                }
                if (name.equals(AttitudeType.ATTITUDE_EVENTS.toString())) {
                    initState = initState
                            .addAttitudeToAdditionalStates(AttitudeType.ATTITUDE_EVENTS);
                }
            }
        }

        // Check correspondence between additional states and additional equations
        this.checkStatesEquations(initState);

        // creating state vector
        final double[] stateVector = this.mapStelaStateToArray(initState);

        // Initialize differential equations system
        this.dragComputation();
        final StelaDifferentialEquations diffEquations = new StelaDifferentialEquations(this);

        // Propagate mean elements
        final double t0 = initialDate.durationFrom(this.referenceDate);
        final double t1 = date.durationFrom(this.referenceDate);

        this.integrator.integrate(diffEquations, t0, stateVector, t1, stateVector);

        // update the step counter
        this.stepCounter++;

        final SpacecraftState newSpacecraftState = this.mapArrayToStelaState(stateVector, date);

        this.resetInitialState(newSpacecraftState);
        return newSpacecraftState;
    }

    /**
     * Check correspondence between additional states and additional equations.
     *
     * @param state
     *        state
     *
     * @throws PatriusException
     *         if additional states from initial state and additional equations added in the
     *         propagator does not
     *         correspond
     */
    private void checkStatesEquations(final SpacecraftState state) throws PatriusException {
        // Get additional states from initial SpacecraftState
        final Map<String, double[]> additionalStates = state.getAdditionalStates();

        if (!additionalStates.isEmpty() && !this.addEquationsList.isEmpty()) {
            // Check correspondence between additional states numbers and additional equations
            // numbers
            final int eqsSize = this.addEquationsList.size();
            if (additionalStates.size() == eqsSize) {
                for (int i = 0; i < eqsSize; i++) {
                    final StelaAdditionalEquations addEquation = this.addEquationsList.get(i);
                    // Check names correspondence
                    final String eqName = addEquation.getName();
                    if (!additionalStates.containsKey(eqName)) {
                        throw new PatriusException(
                                PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
                    }
                    // Check size correspondence
                    if (addEquation.getEquationsDimension() != additionalStates.get(eqName).length) {
                        throw new PatriusException(
                                PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
                    }
                }
            } else {
                throw new PatriusException(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
            }
        }
        if (additionalStates.isEmpty() ^ this.addEquationsList.isEmpty()) {
            // Either additional states from SpacecraftStare are empty or additional equations are
            // empty
            throw new PatriusException(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS);
        }
    }

    /**
     * Map Stela state to array.
     *
     * @param state
     *        the state
     * @return the state vector
     * @throws PatriusException
     *         raised during SpacecraftState.getAdditionalState()
     *
     * @since 3.0
     */
    private double[] mapStelaStateToArray(final SpacecraftState state) throws PatriusException {
        // creating state vector
        final double[] stateVector = new double[this.computeDimension()];

        // Conversions of initial state
        final StelaEquinoctialOrbit orbit = new StelaEquinoctialOrbit(state.getOrbit());
        final double[] initialStateParams = orbit.mapOrbitToArray();

        System.arraycopy(initialStateParams, 0, stateVector, 0, STATE_SIZE);

        // Additional states:
        int index = STATE_SIZE;
        for (final StelaAdditionalEquations eq : this.addEquationsList) {
            final double[] addState = state.getAdditionalState(eq.getName());
            // addState.length = Equ.getEquationsDimension()
            System.arraycopy(addState, 0, stateVector, index, addState.length);
            // Incrementing index
            index += addState.length;
        }
        return stateVector;
    }

    /**
     * Convert the state vector into a SpacecraftState.
     *
     * @param stateVector
     *        the state vector
     * @param date
     *        the date
     * @return the state
     * @throws PatriusException
     *         raised during state computation.
     *
     * @since 3.0
     */
    private SpacecraftState
            mapArrayToStelaState(final double[] stateVector, final AbsoluteDate date)
                    throws PatriusException {
        // Get the additional state data from state vector
        int index = STATE_SIZE;
        final Map<String, double[]> addStates = new ConcurrentHashMap<>();
        for (final StelaAdditionalEquations eq : this.addEquationsList) {
            final String name = eq.getName();
            final int addStateLength = eq.getEquationsDimension();
            final double[] addState = new double[addStateLength];
            System.arraycopy(stateVector, index, addState, 0, addStateLength);
            addStates.put(name, addState);
            index += addStateLength;

            // for StelaPartialDerivatives update step counter
            if (eq.getClass().equals(StelaPartialDerivativesEquations.class)) {
                ((StelaPartialDerivativesEquations) eq).updateStepCounter();
            }
        }

        // Get Stela orbit
        final StelaEquinoctialOrbit newOrbit = new StelaEquinoctialOrbit(stateVector[0],
                stateVector[2], stateVector[3], stateVector[4], stateVector[5], stateVector[1],
                this.integrationFrame, date, this.mu);

        // Try to compute osculating orbit at perigee.
        // If computation fails, integration step must be reduced
        // Warning: this may look like dead code but conversion to osculating is performed
        // on purpose: il fails, an exception is thrown and is later caught which means
        // timestep has to be reduced
        final double ksiAnomZero = MathLib.atan2(newOrbit.getEquinoctialEy(),
                newOrbit.getEquinoctialEx());
        final StelaEquinoctialOrbit newOrbit2 = new StelaEquinoctialOrbit(newOrbit.getA(),
                newOrbit.getEquinoctialEx(), newOrbit.getEquinoctialEy(), newOrbit.getIx(),
                newOrbit.getIy(), ksiAnomZero, newOrbit.getFrame(), newOrbit.getDate(),
                newOrbit.getMu());
        this.converter.toOsculating(newOrbit2);

        // Compute the attitude
        final Attitude[] attitude = this.mapAttitude(addStates, newOrbit);

        return new SpacecraftState(newOrbit, attitude[0], attitude[1], addStates);
    }

    /**
     * Compute attitude from additional states or AttitudeProvider.
     *
     * @param addStates
     *        the additional states map
     * @param newOrbit
     *        the Stela orbit
     * @return the attitude for forces and events computation
     * @throws PatriusException
     *         raised during attitude computation.
     *
     * @since 3.0
     */
    private Attitude[] mapAttitude(final Map<String, double[]> addStates,
            final StelaEquinoctialOrbit newOrbit) throws PatriusException {
        // Compute the attitude
        Attitude attForces = null;
        Attitude attEvents = null;

        // get attitudes for forces and events computation using the AttitudeProvider
        if (!addStates.containsKey(AttitudeType.ATTITUDE.toString())) {
            if (!addStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())
                    && (this.attitudeProviderForces != null)) {
                attForces = this.attitudeProviderForces.getAttitude(newOrbit);
            }
            if (!addStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString())
                    && (this.attitudeProviderEvents != null)) {
                attEvents = this.attitudeProviderEvents.getAttitude(newOrbit);
            }
        }

        // get attitudes for forces and events computation from additional states map
        if (addStates.containsKey(AttitudeType.ATTITUDE.toString())) {
            // One attitude
            attForces = new Attitude(addStates.get(AttitudeType.ATTITUDE.toString()),
                    newOrbit.getDate(), newOrbit.getFrame());
        } else {
            // Two attitudes
            if (addStates.containsKey(AttitudeType.ATTITUDE_FORCES.toString())) {
                attForces = new Attitude(addStates.get(AttitudeType.ATTITUDE_FORCES.toString()),
                        newOrbit.getDate(), newOrbit.getFrame());
            }
            if (addStates.containsKey(AttitudeType.ATTITUDE_EVENTS.toString())) {
                attEvents = new Attitude(addStates.get(AttitudeType.ATTITUDE_EVENTS.toString()),
                        newOrbit.getDate(), newOrbit.getFrame());
            }
        }
        return new Attitude[] { attForces, attEvents };
    }

    /**
     *
     * update drag computation need before creating differential equations
     *
     * @since 1.3
     */
    protected void dragComputation() {
        // update drag computation need before creating differential equations
        for (final StelaForceModel gauss : this.gaussForces) {

            if (gauss.getClass().equals(StelaAtmosphericDrag.class)) {
                final int dragStep = ((StelaAtmosphericDrag) gauss).getDragRecomputeStep();
                this.updateDragComputationNeed(dragStep);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("PMD.PreserveStackTrace")
    protected SpacecraftState propagationManagement(final SpacecraftState state,
            final double stepSize, final double dt, final AbsoluteDate target)
            throws PatriusException {

        if (this.maxShift == 0 || this.minStepSize >= stepSize / 2.) {
            // User did not allow for propagation failures, classic method is used
            return super.propagationManagement(state, stepSize, dt, target);
        }

        SpacecraftState finalState = null;
        final double stepSizeOld = stepSize;
        try {
            finalState = this.goAhead(stepSize, dt, target);

        } catch (final IllegalArgumentException e) {
            // create illegal argument exception
            final IllegalArgumentException exception = PatriusException
                    .createIllegalArgumentException(
                            PatriusMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                            StelaEquinoctialParameters.class.getName());
            final String message = exception.getMessage();

            if (e.getMessage().equalsIgnoreCase(message)) {
                // Mecanism to deal with last step propagation reducing the stepsize

                if (!this.wasException) {
                    this.maxDate = state.getDate().shiftedBy(this.maxShift * stepSize);
                }
                this.wasException = true;
                finalState = this.propagationManagementLoop(stepSizeOld, target, dt, message);

                if (finalState == null) {
                    // Throw exception
                    throw PatriusException.createIllegalArgumentException(
                            PatriusMessages.STELA_INTEGRATION_FAILED,
                            StelaEquinoctialParameters.class.getName());
                }
            }
        }
        // the final spacecraft state
        return finalState;
    }

    /**
     * Reduce step size until propagation over next step returns physical values.
     *
     * @param currentStepSize
     *        the initial stepSize
     * @param target
     *        the target date
     * @param dt
     *        the dt time
     * @param message
     *        the exception message
     * @return finalstate
     *         the final state
     *
     * @throws PropagationException
     *         a propagation exception
     * @since 1.3
     */
    protected SpacecraftState propagationManagementLoop(final double currentStepSize,
            final AbsoluteDate target, final double dt, final String message)
            throws PropagationException {

        // Initialization: start with step size / 2
        double stepSize = currentStepSize / 2.;

        // Reduce step size until propagation over next step returns physical values
        while (stepSize > this.minStepSize) {
            try {
                // Define fixed-step integrator with reduced step size
                this.setMasterMode(stepSize, this.oldStepHandler);
                this.setIntegrator(new RungeKutta6Integrator(stepSize));
                // Go one step ahead
                return this.goAhead(stepSize, dt, target);

            } catch (final IllegalArgumentException e) {
                if (e.getMessage().equalsIgnoreCase(message)) {
                    // Step size still too large, divide it by 2
                    stepSize /= 2;
                }
            }
        }
        return null;
    }

    /**
     *
     * Update drag computation need
     *
     * @param dragStep
     *        the drag recomputation step
     *
     */
    private void updateDragComputationNeed(final int dragStep) {

        final double mod = JavaMathAdapter.mod(this.stepCounter, dragStep);
        if (mod == 0) {
            this.recomputeDrag = true;
        } else {
            this.recomputeDrag = false;
        }
    }

    /** {@inheritDoc} */
    @Override
    protected double getMass(final AbsoluteDate date) throws PropagationException {
        return this.mass;
    }

    /**
     * Compute complete state vector dimension.
     *
     * @return state vector dimension
     */
    private int computeDimension() {
        int sum = STATE_SIZE;
        for (final StelaAdditionalEquations eq : this.addEquationsList) {
            sum += eq.getEquationsDimension();
        }
        return sum;
    }

    /**
     * Get the reference date
     *
     * @return the reference date.
     */
    public AbsoluteDate getReferenceDate() {
        return this.referenceDate;
    }

    /**
     * Get the additional equations.
     *
     * @return the additional equations.
     */
    public List<StelaAdditionalEquations> getAddEquations() {
        return this.addEquationsList;
    }

    /**
     * Get the mean/osculating orbit converter.
     *
     * @return the mean/osculating orbit converter.
     */
    public OrbitNatureConverter getOrbitNatureConverter() {
        return this.converter;
    }

    /**
     * Specify the drag values, only used for n steps drag computation purposes
     *
     * @param indDragdt
     *        the drag values to set
     */
    protected void setdDragdt(final double[] indDragdt) {
        this.dDragdt = indDragdt.clone();
    }

    /**
     * Method that returns the drag value in memory, only used for n steps drag computation purposes
     *
     * @return the dDragdt
     */
    protected double[] getdDragdt() {
        return this.dDragdt.clone();
    }

    /**
     * Getter for forces step handler.
     *
     * @return forces step handler
     */
    public ForcesStepHandler getForcesStepHandler() {
        return this.forcesStepHandler;
    }
}
