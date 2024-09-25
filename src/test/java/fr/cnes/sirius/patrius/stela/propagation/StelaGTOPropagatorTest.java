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
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.11:DM:DM-3287:22/05/2023:[PATRIUS] Ajout des courtes periodes dues a la traînee atmospherique et a la pression de radiation solaire dans STELA
 * VERSION:4.11:DM:DM-3300:22/05/2023:[PATRIUS] Nouvelle approche pour le calcul de la position relative de 2 corps celestes 
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:64:30/05/2013:update with renamed classes
 * VERSION::DM:91:26/07/2013:test modification
 * VERSION::DM:131:28/10/2013:Changed ConstanSolarActivity class
 * VERSION::DM:190:29/07/2014: Modified maneuvers in retro-propagation case
 * (added forward parameter to eventOccurred signature)
 * VERSION::DM:208:05/08/2014: one shot event detector
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::FA:406:20/02/2015:Checkstyle corrections (nb cyclomatic) + couverture
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * VERSION::DM:393:12/03/2015: Constant Attitude Laws
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * VERSION::FA:359:31/03/2015: Proper management of reentry case with step size control
 * VERSION::FA:391:13/04/2015: system to retrieve STELA dE/dt
 * VERSION::DM:426:30/10/2015: Tests the new functionalities on orbit definition and orbit propagation
 * VERSION::DM:484:25/09/2015:Get additional state from an AbsoluteDate
 * VERSION::FA:463:09/11/2015:Minor changes on STELA features
 * VERSION::DM:454:24/11/2015:Class test updated according the new implementation for detectors
 * VERSION::DM:605:30/09/2016:gathered Meeus models
 * VERSION::FA:1286:05/09/2017:correct osculating orbit propagation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.propagation;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.MeeusSun.MODEL;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger;
import fr.cnes.sirius.patrius.events.postprocessing.EventsLogger.LoggedEvent;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ACSOLFormatReader;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataFactory;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ClassicalMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsReader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.exception.util.DummyLocalizable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.EulerIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.RungeKutta6Integrator;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.StelaEquinoctialParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.AdditionalStateProvider;
import fr.cnes.sirius.patrius.propagation.BoundedPropagator;
import fr.cnes.sirius.patrius.propagation.Propagator;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.numerical.AttitudeEquation.AttitudeType;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandlerMultiplexer;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.PerigeeAltitudeDetector;
import fr.cnes.sirius.patrius.stela.bodies.MeeusMoonStela;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaGaussContribution;
import fr.cnes.sirius.patrius.stela.forces.AbstractStelaLagrangeContribution;
import fr.cnes.sirius.patrius.stela.forces.StelaForceModel;
import fr.cnes.sirius.patrius.stela.forces.atmospheres.MSIS00Adapter;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAeroModel;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaAtmosphericDrag;
import fr.cnes.sirius.patrius.stela.forces.drag.StelaCd;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaTesseralAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaThirdBodyAttraction;
import fr.cnes.sirius.patrius.stela.forces.gravity.StelaZonalAttraction;
import fr.cnes.sirius.patrius.stela.forces.noninertial.NonInertialContribution;
import fr.cnes.sirius.patrius.stela.forces.radiation.StelaSRPSquaring;
import fr.cnes.sirius.patrius.stela.orbits.OrbitNatureConverter;
import fr.cnes.sirius.patrius.stela.orbits.StelaEquinoctialOrbit;
import fr.cnes.sirius.patrius.stela.propagation.data.TimeDerivativeData;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for the class StelaGTOPropagator.
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class StelaGTOPropagatorTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Stela GTO propagator is able to propagate an orbit
         * 
         * @featureDescription test the Stela GTO propagator is able to propagate an orbit
         * 
         * @coveredRequirements
         */
        STELA_GTO_PROPAGATION,

        /**
         * @featureTitle Stela GTO propagator partial derivatives computation
         * 
         * @featureDescription test the Stela GTO propagator is able to compute partial derivatives during propagation
         * 
         * @coveredRequirements
         */
        STELA_GTO_PARTIAL_DERIVATIVES_COMPUTATION,

        /**
         * @featureTitle Stela GTO propagator compatibility with Orekit propagation functionalities
         * 
         * @featureDescription test the Stela GTO propagator is consistent with the Orekit propagation functionalities
         * 
         * @coveredRequirements
         */
        COMPATIBILITY_WITH_OREKIT_PROPAGATION_FUNCTIONALITIES,

        /**
         * @featureTitle Stela GTO propagator can be used with different fixed or variable step integrator
         * 
         * @featureDescription test the Stela GTO propagator can be used with different fixed or variable step
         *                     integrator
         * 
         * @coveredRequirements
         */
        STELA_GTO_COMPATIBLE_WITH_COMMONS_MATH_INTEGRATORS
    }

    /** A Stela GTO propagator. */
    private StelaGTOPropagator propagator;

    /** An integrator. */
    private FirstOrderIntegrator integrator;
    /** Initial Date. */
    private AbsoluteDate initDate;
    /** Orbit. */
    private Orbit orbit;

    /** A spacecraft state to be used as the propagator initial state. */
    private SpacecraftState initialState;
    /** Atmosphere */
    private Atmosphere atmosphere;
    /** Propagators */
    StelaGTOPropagator propagator42;
    StelaGTOPropagator propagatorT;
    /** The sun */
    CelestialPoint sun = null;

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#StelaGTOPropagator(FirstOrderIntegrator, StelaEquinoctialOrbit, boolean, AttitudeProvider, double)}
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test a simple keplerian propagation
     * 
     * @input a Stela GTO propagator
     * 
     * @output the final state of the propagation
     * 
     * @testPassCriteria check the final state is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testKepler() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setDOPIntegrator();
        this.setStelaProp2();

        // creating forces that will be removed afterward
        final StelaThirdBodyAttraction sol = new StelaThirdBodyAttraction(new MeeusSun(MODEL.STELA), 3, 2, 2);
        final StelaSRPSquaring prs = new StelaSRPSquaring(1000, 10, 1.5, 11, new MeeusSun(MODEL.STELA));

        this.propagator.addForceModel(sol);
        this.propagator.addForceModel(prs);

        Assert.assertEquals(1, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(1, this.propagator.getLagrangeForceModels().size());

        // Test the removal of forces
        this.propagator.removeForceModels();
        Assert.assertEquals(0, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(0, this.propagator.getLagrangeForceModels().size());

        // Add null Non inertial contribution
        this.propagator.addForceModel(new NonInertialContribution(7, FramesFactory.getCIRF()));

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = this.propagator.propagate(initDate.shiftedBy(dt));
        // Check results
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA()) / this.initialState.getA();
        Assert.assertEquals(0, (this.initialState.getA() - finalState.getA()) / finalState.getA(), 1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEx() - finalState.getEquinoctialEx()) / finalState.getEquinoctialEx(),
            1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEy() - finalState.getEquinoctialEy()) / finalState.getEquinoctialEy(),
            1e-12);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1E-12);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1E-12);
        Assert.assertEquals(0,
            JavaMathAdapter.mod(this.initialState.getLM() + n * dt - finalState.getLM(), 2 * FastMath.PI), 1.e-14);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#StelaGTOPropagator(FirstOrderIntegrator, StelaEquinoctialOrbit, boolean, AttitudeProvider, AttitudeProvider, double)}
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test a simple keplerian propagation with two attitude providers given to the constructor
     * 
     * @input a Stela GTO propagator with two attitude providers
     * 
     * @output the final state of the propagation
     * 
     * @testPassCriteria check the final state is the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testKeplerTwoAttProvider() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setDOPIntegrator();
        this.setStelaProp3();

        // creating forces that will be removed afterward
        final StelaThirdBodyAttraction sol = new StelaThirdBodyAttraction(new MeeusSun(MODEL.STELA), 3, 2, 2);
        final StelaSRPSquaring prs = new StelaSRPSquaring(1000, 10, 1.5, 11, new MeeusSun(MODEL.STELA));

        this.propagator.addForceModel(sol);
        this.propagator.addForceModel(prs);

        Assert.assertEquals(1, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(1, this.propagator.getLagrangeForceModels().size());

        // Test the removal of forces
        this.propagator.removeForceModels();
        Assert.assertEquals(0, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(0, this.propagator.getLagrangeForceModels().size());

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = this.propagator.propagate(initDate.shiftedBy(dt));
        // Check results
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA()) / this.initialState.getA();
        Assert.assertEquals(0, (this.initialState.getA() - finalState.getA()) / finalState.getA(), 1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEx() - finalState.getEquinoctialEx()) / finalState.getEquinoctialEx(),
            1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEy() - finalState.getEquinoctialEy()) / finalState.getEquinoctialEy(),
            1e-12);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1E-12);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1E-12);
        Assert.assertEquals(0,
            JavaMathAdapter.mod(this.initialState.getLM() + n * dt - finalState.getLM(), 2 * FastMath.PI), 1.e-14);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#StelaGTOPropagator(FirstOrderIntegrator, StelaEquinoctialOrbit, boolean, AttitudeProvider, AttitudeProvider, double)}
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test a simple keplerian propagation with two attitude equations given to the constructor
     * 
     * @input a Stela GTO propagator with two attitude equation
     * 
     * @output the final state of the propagation
     * 
     * @testPassCriteria check the final state is the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testKeplerTwoAttEqu() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK4Integrator();
        this.setStelaProp4();

        // creating forces that will be removed afterward
        final StelaThirdBodyAttraction sol = new StelaThirdBodyAttraction(new MeeusSun(MODEL.STELA), 3, 2, 2);
        final StelaSRPSquaring prs = new StelaSRPSquaring(1000, 10, 1.5, 11, new MeeusSun(MODEL.STELA)
            );

        this.propagator.addForceModel(sol);
        this.propagator.addForceModel(prs);

        Assert.assertEquals(1, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(1, this.propagator.getLagrangeForceModels().size());

        // Test the removal of forces
        this.propagator.removeForceModels();
        Assert.assertEquals(0, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(0, this.propagator.getLagrangeForceModels().size());

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = this.propagator.propagate(initDate.shiftedBy(dt));
        MathLib.sqrt(this.initialState.getMu() / this.initialState.getA());
        this.initialState.getA();
        Assert.assertEquals(0, (this.initialState.getA() - finalState.getA()) / finalState.getA(), 1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEx() - finalState.getEquinoctialEx()) / finalState.getEquinoctialEx(),
            1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEy() - finalState.getEquinoctialEy()) / finalState.getEquinoctialEy(),
            1e-12);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1E-12);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1E-12);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#StelaGTOPropagator(FirstOrderIntegrator, StelaEquinoctialOrbit, boolean, AttitudeProvider, AttitudeProvider, double)}
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test a simple keplerian propagation with a single equations given to the constructor
     * 
     * @input a Stela GTO propagator with one attitude equation
     * 
     * @output the final state of the propagation
     * 
     * @testPassCriteria check the final state is the expected one
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testKeplerOneAttEqu() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK4Integrator();
        this.setStelaProp5();

        // creating forces that will be removed afterward
        final StelaThirdBodyAttraction sol = new StelaThirdBodyAttraction(new MeeusSun(MODEL.STELA), 3, 2, 2);
        final StelaSRPSquaring prs = new StelaSRPSquaring(1000, 10, 1.5, 11, new MeeusSun(MODEL.STELA));

        this.propagator.addForceModel(sol);
        this.propagator.addForceModel(prs);

        Assert.assertEquals(1, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(1, this.propagator.getLagrangeForceModels().size());

        // Test the removal of forces
        this.propagator.removeForceModels();
        Assert.assertEquals(0, this.propagator.getGaussForceModels().size());
        Assert.assertEquals(0, this.propagator.getLagrangeForceModels().size());

        // Propagation of the initial state at t + dt
        final double dt = 3200.;
        final SpacecraftState finalState = this.propagator.propagate(initDate.shiftedBy(dt));
        MathLib.sqrt(this.initialState.getMu() / this.initialState.getA());
        this.initialState.getA();
        Assert.assertEquals(0, (this.initialState.getA() - finalState.getA()) / finalState.getA(), 1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEx() - finalState.getEquinoctialEx()) / finalState.getEquinoctialEx(),
            1E-12);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEy() - finalState.getEquinoctialEy()) / finalState.getEquinoctialEy(),
            1e-12);
        Assert.assertEquals(this.initialState.getHx(), finalState.getHx(), 1E-12);
        Assert.assertEquals(this.initialState.getHy(), finalState.getHy(), 1E-12);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COMPATIBILITY_WITH_OREKIT_PROPAGATION_FUNCTIONALITIES}
     * 
     * @testedMethod {@link StelaGTOPropagator#addEventDetector(EventDetector)}
     * 
     * @description test the Orekit events detection mechanism on the Stela GTO propagator
     * 
     * @input a Stela GTO propagator and a date detector
     * 
     * @output the final state of the propagation and the detected event
     * 
     * @testPassCriteria the propagation is successful and the detected event is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testEventDetection() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setDOPIntegrator();
        this.setStelaProp();
        final DateDetector dateDetector = new DateDetector(initDate.shiftedBy(300.0)){
            /** Serializable UID. */
            private static final long serialVersionUID = -529447487008441942L;

            @Override
            public Action
                eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                    throws PatriusException {
                return Action.CONTINUE;
            }
        };
        // Use an events logger to register the detected event:
        final EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(dateDetector));
        // Propagation of the initial state at t + dt
        final double dt = 5000.;
        final SpacecraftState finalState = this.propagator.propagate(initDate.shiftedBy(dt));

        // Check results
        final List<LoggedEvent> events = logger.getLoggedEvents();
        Assert.assertEquals(1, events.size());
        Assert.assertEquals(300., events.get(0).getState().getDate().durationFrom(initDate), 0.0);
        Assert.assertEquals(5000., finalState.getDate().durationFrom(initDate), 0.0);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COMPATIBILITY_WITH_OREKIT_PROPAGATION_FUNCTIONALITIES}
     * 
     * @testedMethod {@link StelaGTOPropagator#addEventDetector(EventDetector)}
     * 
     * @description test the Orekit events detection mechanism on the Stela GTO propagator
     * 
     * @input a Stela GTO propagator and a custom date detector which should detect only the first date
     * 
     * @output the final state of the propagation and the detected event
     * 
     * @testPassCriteria the propagation is successful and the custom event occured just one time.
     *                   The event detector is removed after the first detection.
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testRemoveDetector() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setDOPIntegrator();
        this.setStelaProp();

        final double dt = 1000.;

        // this date should always be detected
        final AbsoluteDate d1 = initDate.shiftedBy(dt);

        // this date should be detected if eventOccured returns CONTINUE
        final AbsoluteDate d2 = initDate.shiftedBy(dt * 2.0);

        final MyDoubleDateDetector detectorA = new MyDoubleDateDetector(d1, d2, Action.CONTINUE, false);
        final MyDoubleDateDetector detectorB = new MyDoubleDateDetector(d1, d2, Action.CONTINUE, true);

        // Use an events logger to register the detected event:
        EventsLogger logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(detectorA));
        // Propagation of the initial state at t + dt
        final double dt_propagation = 10000.;
        this.propagator.propagate(initDate.shiftedBy(dt_propagation));

        // Check results
        Assert.assertEquals(2, detectorA.getCount());

        this.setDOPIntegrator();
        this.setStelaProp();

        // Use an events logger to register the detected event:
        logger = new EventsLogger();
        this.propagator.addEventDetector(logger.monitorDetector(detectorB));
        this.propagator.propagate(initDate.shiftedBy(dt_propagation));

        // Check results
        Assert.assertEquals(1, detectorB.getCount());
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COMPATIBILITY_WITH_OREKIT_PROPAGATION_FUNCTIONALITIES}
     * 
     * @testedMethod {@link StelaGTOPropagator#setEphemerisMode()}
     * @testedMethod {@link StelaGTOPropagator#getGeneratedEphemeris()}
     * 
     * @description test the Orekit "Ephemeris" mode on the Stela GTO propagator
     * 
     * @input a Stela GTO propagator
     * 
     * @output the BoundedPropagator
     * 
     * @testPassCriteria the BoundedPropagator is properly created and is able to perform a
     *                   bounded propagation
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testEphemerisMode() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK6Integrator();
        this.setStelaProp();
        this.propagator.setEphemerisMode();
        // Propagation of the initial state at t + 10 days
        final double dt = 2. * Constants.JULIAN_DAY;
        this.propagator.propagate(initDate.shiftedBy(5. * dt));

        // Get ephemeris
        final BoundedPropagator ephem = this.propagator.getGeneratedEphemeris();

        // Propagation of the initial state with ephemeris at t + 2 days
        final SpacecraftState s = ephem.propagate(initDate.shiftedBy(dt));

        // Check results
        final double epsilon = 2e-12;
        final double n = MathLib.sqrt(this.initialState.getMu() / this.initialState.getA()) / this.initialState.getA();

        Assert.assertEquals(0, (this.initialState.getA() - s.getA()) / this.initialState.getA(), epsilon);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEx() - s.getEquinoctialEx()) / this.initialState.getEquinoctialEx(),
            epsilon);
        Assert.assertEquals(0,
            (this.initialState.getEquinoctialEy() - s.getEquinoctialEy()) / this.initialState.getEquinoctialEy(),
            epsilon);
        Assert.assertEquals(0, (this.initialState.getHx() - s.getHx()) / this.initialState.getHx(), epsilon);
        Assert.assertEquals(0, (this.initialState.getHy() - s.getHy()) / this.initialState.getHy(), epsilon);
        Assert
            .assertEquals(0,
                JavaMathAdapter.mod((this.initialState.getLM() + n * dt - s.getLM()), 2 * FastMath.PI) / s.getLM(),
                epsilon);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#COMPATIBILITY_WITH_OREKIT_PROPAGATION_FUNCTIONALITIES}
     * 
     * @testedMethod {@link StelaGTOPropagator#setMasterMode(double, PatriusFixedStepHandler)}
     * @testedMethod {@link StelaGTOPropagator#setMasterMode(PatriusStepHandler)}
     * 
     * @description test the Orekit "Master" mode on the Stela GTO propagator
     * 
     * @input a Stela GTO propagator and a StepHandler that records the date at every step.
     * 
     * @output the dates recorded by the StepHandler
     * 
     * @testPassCriteria the recorded dates are the expected one (the StepHandler worked properly)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testMasterMode() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK6Integrator();
        this.setStelaProp();
        // First test: fixed-step handler:
        final List<AbsoluteDate> dates = new ArrayList<>();
        final PatriusFixedStepHandler handler1 = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1299794489221596253L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                // record the current date:
                dates.add(currentState.getDate());
            }
        };
        final double step = 100.0;
        this.propagator.setMasterMode(step, handler1);
        // Propagation of the initial state:
        this.propagator.propagate(initDate.shiftedBy(1015.));
        // There should be 11 elements in the dates list (10 fixed step during propagation):
        Assert.assertEquals(11, dates.size());
        for (int i = 0; i < dates.size(); i++) {
            Assert.assertEquals(initDate.shiftedBy(step * i), dates.get(i));
        }

        // Second test: step handler:
        this.propagator.resetInitialState(this.initialState);
        final PatriusStepHandler handler2 = new PatriusStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6292177639127627183L;
            private AbsoluteDate previous;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if ((this.previous != null) && !isLast) {
                    Assert.assertEquals(StelaGTOPropagatorTest.this.initialState.getKeplerianPeriod() / 100,
                        interpolator.getCurrentDate().durationFrom(this.previous), 1.0e-10);
                }
                this.previous = interpolator.getCurrentDate();
            }
        };
        this.propagator.setMasterMode(handler2);
        // Propagation of the initial state:
        this.propagator.propagate(initDate.shiftedBy(1005.));
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PARTIAL_DERIVATIVES_COMPUTATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#propagate(AbsoluteDate, AbsoluteDate)}
     * 
     * @description test computation mechanism of the the partial derivatives of the state in the
     *              Stela GTO propagator. This test uses a mock force model and a step handler to check the partial
     *              derivatives
     *              value every 100 seconds.
     * 
     * @input a Stela GTO propagator, a mock force model, and a step handler that checks the value of the
     *        computed partial derivatives.
     * 
     * @output the computed partial derivatives
     * 
     * @testPassCriteria the partial derivatives are properly computed during the propagation and their
     *                   value is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    // @Test
    public void testPartialDerivativesComputation() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK6Integrator();
        final DummyForceModel force = new DummyForceModel();
        this.setStelaPropWithPD(force);
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -2900898172123219256L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                double[] addState;
                try {
                    addState = currentState.getAdditionalState("PARTIAL_DERIVATIVES");
                    final double t = currentState.getDate().durationFrom(initDate);

                    Assert.assertEquals(0.2 * t + 1, addState[0], 1E-10);
                    Assert.assertEquals(0.2 * t, addState[6], 1E-10);

                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        };
        this.propagator.setMasterMode(100, handler);
        // Propagation of the initial state:
        final double dt = 0.25 * Constants.JULIAN_DAY;
        this.propagator.propagate(initDate.shiftedBy(dt));
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PARTIAL_DERIVATIVES_COMPUTATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#propagate(AbsoluteDate, AbsoluteDate)}
     * 
     * @description test computation mechanism of the the partial derivatives of the state in the
     *              Stela GTO propagator when the partial derivatives are a function of time.
     * 
     * @input a Stela GTO propagator, a mock force model, and a step handler that checks the value of the
     *        computed partial derivatives.
     * 
     * @output the computed partial derivatives
     * 
     * @testPassCriteria the partial derivatives are properly computed during the propagation and their
     *                   value is the expected one
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    // @Test
    public void testPartialDerivativesComputation2() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK4Integrator();
        final DummyForceModel2 force = new DummyForceModel2();
        this.setStelaPropWithPD(force);
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4001619640047705019L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                double[] addState;
                try {
                    addState = currentState.getAdditionalState("PARTIAL_DERIVATIVES");
                    final double t = currentState.getDate().durationFrom(initDate);
                    Assert.assertEquals(t * t / 2, addState[1], 1E-10);

                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
            }
        };
        this.propagator.setMasterMode(8, handler);
        // Propagation of the initial state:
        final double dt = 0.25 * Constants.JULIAN_DAY;
        this.propagator.propagate(initDate.shiftedBy(dt));
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_COMPATIBLE_WITH_COMMONS_MATH_INTEGRATORS}
     * 
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test a simple keplerian propagation with different integrators
     * 
     * @input a Stela GTO propagator and three different integrators
     * 
     * @output the final state of the propagation using the three different integrators
     * 
     * @testPassCriteria check the final state is the same with the three different integrators
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     */
    @Test
    public void testDifferentIntegrators() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        final double dt = 10000.;
        // RK4:
        this.setRK4Integrator();
        this.setStelaProp();
        // Propagation of the initial state at t + dt
        final SpacecraftState finalStateRK4 = this.propagator.propagate(initDate.shiftedBy(dt));
        // RK6:
        this.setRK6Integrator();
        this.setStelaProp();
        // Propagation of the initial state at t + dt
        final SpacecraftState finalStateRK6 = this.propagator.propagate(initDate.shiftedBy(dt));
        // DOP:
        this.setDOPIntegrator();
        this.setStelaProp();
        // Propagation of the initial state at t + dt
        final SpacecraftState finalStateDOP = this.propagator.propagate(initDate.shiftedBy(dt));
        // Check results
        Assert.assertEquals(finalStateRK4.getLM(), finalStateRK6.getLM(), 1E-10);
        Assert.assertEquals(finalStateRK4.getLM(), finalStateDOP.getLM(), 1E-10);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#setStartDate(AbsoluteDate)}
     * @testedMethod {@link StelaGTOPropagator#setIntegrator(FirstOrderIntegrator)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProvider(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#getPvProvider()}
     * @testedMethod {@link StelaGTOPropagator#getEventsDetectors()}
     * @testedMethod {@link StelaGTOPropagator#clearEventsDetectors()}
     * @testedMethod {@link StelaGTOPropagator#getMode()}
     * @testedMethod {@link StelaGTOPropagator#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description test for additional small features and code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testMisc() throws PatriusException {
        final AbsoluteDate initDate = this.initialState.getDate();
        this.setRK6Integrator(0.01);
        this.setStelaProp();
        // Next line is superfluous, added for code coverage
        this.propagator.setStartDate(initDate);
        // Next line is superfluous, added for code coverage
        this.propagator.setIntegrator(this.integrator);
        // Attitude provider
        final AttitudeProvider ap = new BodyCenterPointing(FramesFactory.getITRF());
        this.propagator.setAttitudeProvider(ap);
        Assert.assertEquals(ap, this.propagator.getAttitudeProvider());
        // PV Provider
        final PVCoordinatesProvider pvp = this.propagator.getPvProvider();
        Assert.assertNotNull(pvp);
        // Event detectors (none)
        Collection<EventDetector> colEvents = this.propagator.getEventsDetectors();
        Assert.assertTrue(colEvents.isEmpty());
        this.propagator.clearEventsDetectors();
        colEvents = this.propagator.getEventsDetectors();
        Assert.assertTrue(colEvents.isEmpty());
        // Fixed-step handler:
        final List<AbsoluteDate> dates = new ArrayList<>();
        final PatriusFixedStepHandler handler1 = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8573094585592598752L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                // record the current date:
                dates.add(currentState.getDate());
            }
        };
        final double step = 100.0;
        this.propagator.setMasterMode(step, handler1);
        // Mode
        Assert.assertEquals(Propagator.MASTER_MODE, this.propagator.getMode());
        // Propagation of the initial state:
        final SpacecraftState fState = this.propagator.propagate(initDate.shiftedBy(step + 1.));
        // There should be 2 elements in the dates list
        Assert.assertEquals(2, dates.size());
        for (int i = 0; i < dates.size(); i++) {
            Assert.assertEquals(initDate.shiftedBy(step * i), dates.get(i));
        }
        // PVCoordinates
        final PVCoordinates pPv = this.propagator.getPVCoordinates(initDate.shiftedBy(step + 1.), fState.getFrame());
        Assert.assertArrayEquals(fState.getPVCoordinates().getPosition().toArray(),
            pPv.getPosition().toArray(), 0.);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test for additional small features and code coverage
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testMisc2() throws PatriusException {
        this.setRK6Integrator();

        // tests constructor
        final StelaGTOPropagator sgeteo = new StelaGTOPropagator(this.integrator, 0, 100);

        // tests isRecomputeDrag
        Assert.assertTrue(sgeteo.isRecomputeDrag());

        // tests initialOrbitTest
        final double reentry_alt = 80 * 1000;
        final double earthRadius = 6378136.46;
        final double maxCheck = 2.5;
        final double threshold = 1;
        final OrbitNatureConverter orbConv = new OrbitNatureConverter(sgeteo.getForceModels());
        final PerigeeAltitudeDetector perDet = new PerigeeAltitudeDetector(maxCheck, threshold, reentry_alt,
            earthRadius, orbConv);
        sgeteo.addEventDetector(perDet);
        final AbsoluteDate initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.),
            TimeScalesFactory.getTAI());
        final SpacecraftState initialState2 = new SpacecraftState(new ApsisOrbit(6385000, 25422200, 0, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getEME2000(), initDate, Constants.EGM96_EARTH_MU));

        try {
            // Check result
            sgeteo.initialOrbitTest(initialState2);
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
        final SpacecraftState initialState3 = new SpacecraftState(new ApsisOrbit(7000000, 25422200, 0, 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getCIRF(), initDate, Constants.EGM96_EARTH_MU));

        sgeteo.setInitialState(initialState3, 1000, true);
        final Orbit or = sgeteo.propagate(initDate.shiftedBy(0)).getOrbit();
        Assert.assertEquals(initialState3.getOrbit().getLM(), or.getLM(), 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test exception throwing when reentering
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void testNoReentryException() throws PatriusException {

        final double stepSize = 86400;
        final StelaGTOPropagator prop = new StelaGTOPropagator(this.integrator, 5, 100){
            /** Serializable UID. */
            private static final long serialVersionUID = 910519404358686009L;

            @Override
            public SpacecraftState propagationManagement(final SpacecraftState state, final double stepSize,
                                                         final double dt,
                                                         final AbsoluteDate target) throws PatriusException {

                this.wasException = true;
                this.maxDate = state.getDate().shiftedBy(5 * stepSize);
                return state;
            }
        };

        prop.resetInitialState(this.initialState);

        try {
            prop.propagationManagement(this.initialState, stepSize, 0, null);
            prop.propagateSpacecraftState(this.initialState.getDate().shiftedBy(6 * stepSize));
            Assert.assertTrue(false);

        } catch (final Exception e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test drag recomputing
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testDragrecomputing() throws PatriusException {

        final StelaGTOPropagator propDrag = new StelaGTOPropagator(this.integrator, 5, 100){
            /** Serializable UID. */
            private static final long serialVersionUID = -8993556566612276779L;

            @Override
            protected SpacecraftState propagateSpacecraftState(final AbsoluteDate date) throws PatriusException {

                this.stepCounter++;
                return null;
            }
        };

        final int n = 2;
        this.dragSetUp();
        final StelaAeroModel sp = new StelaAeroModel(1000, new StelaCd(2.2), 10, this.atmosphere, 50);
        final StelaAtmosphericDrag atmosphericDrag =
            new StelaAtmosphericDrag(sp, this.atmosphere, 33, 6378000, 2500000,
                n);
        propDrag.addForceModel(atmosphericDrag);

        // Check result
        propDrag.dragComputation();

        Assert.assertTrue(propDrag.isRecomputeDrag());
        propDrag.propagateSpacecraftState(null);
        propDrag.dragComputation();
        Assert.assertFalse(propDrag.isRecomputeDrag());

        final double[] in = { 1, 2, 3 };
        propDrag.setdDragdt(in);
        final double[] out = propDrag.getdDragdt();

        Assert.assertArrayEquals(in, out, 0);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test PropagationManagement and Loop
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testPropagationManagement() throws PatriusException {

        final StelaGTOPropagator propDrag = new StelaGTOPropagator(this.integrator, 5, 100){
            /** Serializable UID. */
            private static final long serialVersionUID = 9021185375689005305L;

            @Override
            public SpacecraftState
                goAhead(final double stepSize, final double dt, final AbsoluteDate target)
                    throws PropagationException {
                new StelaEquinoctialOrbit(new StelaEquinoctialParameters(455000, 1.2, 1.2, 0.2, 0.55, 0.55, 36512547,
                    true), FramesFactory.getEME2000(),
                    new AbsoluteDate());
                return null;
            }
        };
        try {
            propDrag.propagationManagement(this.initialState, 86400, 1, this.initialState.getDate().shiftedBy(86400));
            Assert.assertTrue(false);
        } catch (final Exception e) {
            Assert.assertTrue(true);
        }

        final StelaGTOPropagator propDrag2 = new StelaGTOPropagator(this.integrator, 5, 100){
            /** Serializable UID. */
            private static final long serialVersionUID = 6156918974532573965L;

            @Override
            public SpacecraftState
                goAhead(final double stepSize, final double dt, final AbsoluteDate target)
                    throws PropagationException {
                return null;
            }
        };

        // Check result
        propDrag2.propagationManagementLoop(86400, null, 0, "");

        Assert.assertTrue(propDrag2.propagationManagement(this.initialState, 86400, 1,
            this.initialState.getDate().shiftedBy(86400)) == null);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test the abstract propagator
     * 
     * @input misc
     * 
     * @output misc
     * 
     * @testPassCriteria small features behave as expected
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testAbstractPropagator() throws PatriusException {
        this.setRK6Integrator();

        // tests constructor
        final StelaGTOPropagator sgeteo = new StelaGTOPropagator(this.integrator, 0, 100);

        final AdditionalStateProvider prov = new AdditionalStateProvider(){

            /** Serializable UID. */
            private static final long serialVersionUID = 2162112900647548678L;

            @Override
            public String getName() {
                return "hello";
            }

            @Override
            public double[] getAdditionalState(final AbsoluteDate date) throws PropagationException {
                return null;
            }
        };
        sgeteo.addAdditionalStateProvider(prov);

        // EventDectector
        final double reentry_alt = 80 * 1000;
        final double earthRadius = 6378136.46;
        final PerigeeAltitudeDetector per = new PerigeeAltitudeDetector(reentry_alt, earthRadius);
        sgeteo.addEventDetector(per);

        final Collection<EventDetector> c = sgeteo.getEventsDetectors();

        Assert.assertTrue(c.contains(per));

        // BoundedPropagatorView
        final Orbit orb1 =
            new KeplerianOrbit(45250000, 0.5, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getEME2000(),
                new AbsoluteDate(new DateComponents(2011, 254), TimeScalesFactory.getTAI()), 39865425.85784);
        final Orbit orb2 =
            new KeplerianOrbit(35250000, 0.6, 0.3, 0, 0, 0, PositionAngle.MEAN, FramesFactory.getEME2000(),
                new AbsoluteDate(new DateComponents(2011, 54), TimeScalesFactory.getTAI()), 39865425.85784);

        sgeteo.resetInitialState(new SpacecraftState(orb1));

        final BoundedPropagator bpro = sgeteo.getGeneratedEphemeris();

        bpro.resetInitialState(new SpacecraftState(orb2));

        final Orbit stB = sgeteo.getInitialState().getOrbit();

        Assert.assertTrue(stB.getDate().durationFrom(orb2.getDate()) == 0);

        Assert.assertTrue(stB.getA() == orb2.getA());

        final PVCoordinates pv = bpro.getPVCoordinates(stB.getDate(), stB.getFrame());

        Assert.assertEquals(stB.getPVCoordinates().getPosition().getX(), pv.getPosition().getX(), 0);
        Assert.assertEquals(stB.getPVCoordinates().getPosition().getZ(), pv.getPosition().getZ(), 0);
        Assert.assertEquals(stB.getPVCoordinates().getVelocity().getY(), pv.getVelocity().getY(), 0);

        // retropropagation for Bounded
        final StelaGTOPropagator sgeteo2 = new StelaGTOPropagator(this.integrator, 0, 100){

            /** Serializable UID. */
            private static final long serialVersionUID = -3051131604370845834L;

            @Override
            protected SpacecraftState basicPropagate(final AbsoluteDate date) throws PropagationException {
                Orbit orbit = null;
                SpacecraftState sp = null;
                try {
                    orbit = new StelaEquinoctialOrbit(this.getInitialState().getPVCoordinates(), this.getInitialState()
                        .getFrame(), date, this.getInitialState().getMu());
                } catch (final PatriusException e) {
                    e.printStackTrace();
                }
                sp = new SpacecraftState(orbit);
                return sp;
            }

        };
        sgeteo2.resetInitialState(new SpacecraftState(orb1));
        sgeteo2.propagate(sgeteo2.getInitialState().getDate().shiftedBy(-3600));
        final BoundedPropagator bpro2 = sgeteo2.getGeneratedEphemeris();

        Assert.assertEquals(0, bpro2.getMinDate().compareTo(sgeteo2.getInitialState().getDate()), 0);
        Assert.assertEquals(0, bpro2.getMaxDate().compareTo(sgeteo2.getInitialState().getDate().shiftedBy(3600)), 0);
    }

    /**
     * @throws IllegalArgumentException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test short propagation
     * 
     * @testPassCriteria small differences with reference (Stela) EPS 5.10^-14 (usual value used in reference Stela)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     * @throws PatriusException
     *         should not happen
     * @throws ParseException
     * @throws IOException
     */
    @Test
    public void zonalPropagationTest() throws IllegalArgumentException, PatriusException, IOException,
        ParseException {

        // Orekit data initialization
        final String OREKIT_DIRECTORY = "stela";
        Utils.setDataRoot(OREKIT_DIRECTORY);
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
        final AbsoluteDate start = new AbsoluteDate(new DateComponents(2013, 04, 11),
            new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        final AbsoluteDate end = new AbsoluteDate(new DateComponents(2113, 04, 25),
            new TimeComponents(0, 0, 35.), TimeScalesFactory.getTAI());
        final double dt = Constants.JULIAN_DAY;

        final Orbit orbit2 = new ApsisOrbit(84400 + 6378000, 36000000 + 6378000, MathLib.toRadians(10),
            MathLib.toRadians(30), MathLib.toRadians(20), MathLib.toRadians(45), PositionAngle.MEAN,
            FramesFactory.getCIRF(), start, 398600441449820.0);

        final RungeKutta6Integrator rk62 = new RungeKutta6Integrator(dt);

        final MyGRGSFormatReader reader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        this.propagatorT = new StelaGTOPropagator(rk62, 5, 10000);
        this.propagatorT.setInitialState(new SpacecraftState(orbit2), 1000, false);
        this.propagatorT.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        final StelaZonalAttraction zonaux = new StelaZonalAttraction(provider, 7, true, 2, 0, false);
        this.propagatorT.addForceModel(zonaux);

        // reentry altitude
        final double reentry_alt = 80 * 1000;

        final double maxCheck = dt;
        final double threshold = 0.5;
        final OrbitNatureConverter orbConv = new OrbitNatureConverter(this.propagatorT.getForceModels());
        final PerigeeAltitudeDetector perDet = new PerigeeAltitudeDetector(maxCheck, threshold, reentry_alt,
            6378 * 1000,
            orbConv);
        this.propagatorT.addEventDetector(perDet);

        final List<SpacecraftState> ephemeris = new ArrayList<>();
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = 5913810144333556904L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                ephemeris.add(currentState);
                StelaGTOPropagatorTest.this.propagatorT.resetInitialState(currentState);

            }
        };
        this.propagatorT.setMasterMode(dt, handler);

        this.propagatorT.propagate(start, end);

        // results file

        final double[][] expectedStela = {
            { 24420.2 * 1000, 0.47268455365758405, 0.5633235149852166, 0.08189960831908934, 0.029809019626209157,
                MathLib.toRadians(94.99999999999999) },
            { 24420.2 * 1000, 0.4686858486204683, 0.5666558159454558, 0.0821157508644028, 0.029197610014574497,
                MathLib.toRadians(194.67956175374786) },
            { 24420.2 * 1000, 0.4646636882760391, 0.569959697826537, 0.0823273675764153, 0.028584586801522455,
                MathLib.toRadians(294.35912037711034) },
            { 24420.2 * 1000, 0.4606182745649791, 0.5732349957198378, 0.08253444697922209, 0.027969983751239505,
                MathLib.toRadians(34.03867588005541) },
            { 24420.2 * 1000, 0.4565498105719714, 0.5764815461615451, 0.08273697784665823, 0.027353834717238866,
                MathLib.toRadians(133.71822827375755) },
            { 24420.2 * 1000, 0.4524585005152021, 0.5796991871403572, 0.08293494920283076, 0.026736173640477767,
                MathLib.toRadians(233.3977775705977) },
            { 24420.2 * 1000, 0.44834454973582244, 0.5828877581051015, 0.0831283503226378, 0.026117034547469224,
                MathLib.toRadians(333.07732378416233) } };

        for (int i = 0; i < expectedStela.length; i++) {
            final double[] ephem2 = (new StelaEquinoctialOrbit(ephemeris.get(i).getOrbit())).mapOrbitToArray();
            final double[] ephem = { ephem2[0], ephem2[2], ephem2[3], ephem2[4], ephem2[5],
                JavaMathAdapter.mod(ephem2[1], 2 * FastMath.PI) };

            for (int j = 0; j < expectedStela[0].length; j++) {
                Assert.assertEquals(0, MathLib.abs((expectedStela[i][j] - ephem[j]) / expectedStela[i][j]), 1e-10);

            }
        }

        Assert.assertTrue(ephemeris.get(ephemeris.size() - 1).getDate().durationFrom(orbit2.getDate()) > 0);
        Assert
            .assertTrue(ephemeris.get(ephemeris.size() - 1).getDate().durationFrom(orbit2.getDate()) < 9 * 24 * 60 * 60);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test exceptions (from KeplerianPropagator tests)
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     */
    @Test(expected = PropagationException.class)
    public void testException() throws PatriusException {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(7.8e6, 0.032, 0.4, 0.1, 0.2, 0.3, PositionAngle.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, 3.986004415e14);
        final StelaGTOPropagator propagatorA = new StelaGTOPropagator(new RungeKutta6Integrator(2.5), 0, 100);
        propagatorA.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagatorA.setInitialState(new SpacecraftState(orbit), 1000, false);
        final PatriusStepHandlerMultiplexer multiplexer = new PatriusStepHandlerMultiplexer();
        propagatorA.setMasterMode(multiplexer);
        multiplexer.add(new PatriusStepHandler(){
            private static final long serialVersionUID = 8183822352839222377L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if (isLast) {
                    throw new PropagationException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });
        propagatorA.setMasterMode(new PatriusStepHandler(){
            private static final long serialVersionUID = 8183822352839222377L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final PatriusStepInterpolator interpolator,
                                   final boolean isLast) throws PropagationException {
                if (isLast) {
                    throw new PropagationException((Throwable) null, new DummyLocalizable("dummy error"));
                }
            }
        });

        propagatorA.propagate(orbit.getDate().shiftedBy(-3600));
    }

    /**
     * 
     * Test StelaGTOPropagator initialized with two attitudes
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderForces(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderEvents(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProvider(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderForces()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderEvents()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProvider()}
     * 
     * @description Test
     * 
     * @input a {@link StelaGTOPropagator}
     * 
     * @output an {@link AttitudeProvider}
     * 
     * @testPassCriteria if the attitude provider is the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testGetSetTWOAttitudeProvider() throws PatriusException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), (new Rotation(true, 0.1,
            0.2, 0.4, 0.1)));

        // Extrapolator definition
        // -----------------------
        this.setDOPIntegrator();
        this.setStelaProp3();
        this.propagator.setAttitudeProviderForces(bodyCenterPointing);
        Assert.assertEquals(bodyCenterPointing, this.propagator.getAttitudeProviderForces());
        Assert.assertEquals(bodyCenterPointing, this.propagator.getAttitudeProvider());
        this.propagator.setAttitudeProviderEvents(inertial);
        Assert.assertEquals(inertial, this.propagator.getAttitudeProviderEvents());

        boolean testOk = false;
        // The test should fail because a two attitudes treatment is expected
        try {
            this.propagator.setAttitudeProvider(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Coverage getAttitudeProvider in case only the attitude provider for events computation is defined
        this.setDOPIntegrator();
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);
        this.propagator.setAttitudeProviderEvents(inertial);
        Assert.assertEquals(inertial, this.propagator.getAttitudeProvider());
    }

    /**
     * 
     * Test StelaGTOPropagator initialized with a single attitude
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderForces(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderEvents(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProvider(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderForces()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderEvents()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProvider()}
     * 
     * @description Test
     * 
     * @input a {@link StelaGTOPropagator}
     * 
     * @output an {@link AttitudeProvider}
     * 
     * @testPassCriteria if the attitude provider is the expected one
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testGetSetONEAttitudeProvider() throws PatriusException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());

        // Extrapolator definition
        // -----------------------
        this.setDOPIntegrator();
        this.setStelaProp2();
        this.propagator.setAttitudeProvider(bodyCenterPointing);
        Assert.assertEquals(bodyCenterPointing, this.propagator.getAttitudeProvider());

        boolean testOk = false;
        // The test should fail because a single attitude treatment is expected
        try {
            this.propagator.setAttitudeProviderForces(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // The test should fail because a single attitude treatment is expected
        try {
            this.propagator.setAttitudeProviderEvents(null);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#setInitialState(SpacecraftState, double, boolean)}
     * 
     * @description Test
     * 
     * @input initial state in a frame different from the integration frame
     * 
     * @output initial state frame
     * 
     * @testPassCriteria initial state frame equal to integration frame
     * 
     * @comments Test for coverage purpose
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testSetInitialState() throws PatriusException {
        // Integrator definition
        // -----------------------
        this.setDOPIntegrator();

        // SpacecraftState definition
        // -----------------------
        final Frame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH.shiftedBy(584.);
        final double mu1 = Constants.JPL_SSD_EARTH_GM;

        // equinoctial orbit
        final double a = 7208669.8179538045;
        final double ex = -1.2567210190877305E-4;
        final double ey = 6.429616630050415E-5;
        final double hx = -1.1052690138668437;
        final double hy = 0.2723243780097966;
        final double l = -1.3663515257676748;

        // orbits
        final EquinoctialOrbit orbit = new EquinoctialOrbit(a, ex, ey, hx, hy, l, PositionAngle.TRUE, gcrf, initDate,
            mu1);
        final SpacecraftState initState = new SpacecraftState(orbit);

        // Extrapolator definition
        // -----------------------
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);

        // reference frame of the StelaGTOPropagator : MOD
        final Frame integrationFrame = FramesFactory.getCIRF();
        this.propagator =
            new StelaGTOPropagator(this.integrator, inertial, bodyCenterPointing, new StelaBasicInterpolator(),
                0, 0);
        this.propagator.setInitialState(initState, 1000, false);

        Assert.assertEquals(gcrf, initState.getFrame());
        Assert.assertEquals(integrationFrame, this.propagator.getFrame());
        Assert.assertEquals(integrationFrame, this.propagator.getInitialState().getFrame());

        this.propagator = new StelaGTOPropagator(this.integrator, 0, 0);
        this.propagator.setInitialState(initState, 1000, false);

        Assert.assertEquals(gcrf, initState.getFrame());
        Assert.assertEquals(integrationFrame, this.propagator.getFrame());
        Assert.assertEquals(integrationFrame, this.propagator.getInitialState().getFrame());
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderForces(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProviderEvents(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#setAttitudeProvider(AttitudeProvider)}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderForces()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProviderEvents()}
     * @testedMethod {@link StelaGTOPropagator#getAttitudeProvider()}
     * @testedMethod {@link StelaGTOPropagator#addAttitudeEquation(StelaAttitudeAdditionalEquations)}
     * 
     * @description Tests for coverage purpose
     * 
     * @testPassCriteria exception raised as expected
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     */
    @Test
    public void testSetGetAttitudeProvider() throws PatriusException {
        final AttitudeProvider provider1 = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(true, 0.1,
            0.1, 0.5, 0.3));
        final StelaAttitudeAdditionalEquations eqsProviderForces = new StelaAttitudeAdditionalEquations(
            AttitudeType.ATTITUDE_FORCES){
            /** Serializable UID. */
            private static final long serialVersionUID = -8103437774916460457L;

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return state;
            }

            @Override
            public int getEquationsDimension() {
                return 7;
            }
        };
        final StelaAttitudeAdditionalEquations eqsProviderDefault = new StelaAttitudeAdditionalEquations(
            AttitudeType.ATTITUDE){
            /** Serializable UID. */
            private static final long serialVersionUID = -966157208248314556L;

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return state;
            }

            @Override
            public int getEquationsDimension() {
                return 7;
            }
        };
        final StelaAttitudeAdditionalEquations eqsProviderEvents = new StelaAttitudeAdditionalEquations(
            AttitudeType.ATTITUDE_EVENTS){
            /** Serializable UID. */
            private static final long serialVersionUID = -7490484098187055481L;

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return state;
            }

            @Override
            public int getEquationsDimension() {
                return 7;
            }
        };

        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);
        Assert.assertEquals(this.propagator.getMass(this.initDate), 1000., Precision.DOUBLE_COMPARISON_EPSILON);
        /*
         * TEST 1 : add an attitude provider for forces computation
         * -> A - Try to add an additional equation representing the attitude for forces computation
         * -> B - Try to add an additional equation representing the attitude by default
         * -> C - Try to add an attitude provider by default
         */
        this.propagator.setAttitudeProviderForces(provider1);
        boolean testOk = false;
        // 1-A the test should fail because a force attitude provider is already defined in the propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderForces);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
        final AttitudeProvider forcesProvider = this.propagator.getAttitudeProviderForces();
        Assert.assertNotNull(forcesProvider);

        testOk = false;
        // 1-B the test should fail because a two attitudes treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderDefault);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // 1-C the test should fail because a two attitudes treatment is expected
        try {
            this.propagator.setAttitudeProvider(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST 2 : add an attitude provider for events computation
         * -> A - Try to add an additional equation representing the attitude for events computation
         */

        this.propagator.setAttitudeProviderEvents(provider1);
        testOk = false;
        // the test should fail because an events attitude provider is already defined in the propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderEvents);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
        final AttitudeProvider eventsProvider = this.propagator.getAttitudeProviderEvents();
        Assert.assertNotNull(eventsProvider);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);

        /*
         * TEST 3 : add an attitude provider by default
         * -> A - Try to add an additional equation representing the attitude by default
         */

        this.propagator.setAttitudeProvider(provider1);
        testOk = false;
        // the test should fail because an attitude provider by defaultis already defined in the propagator:
        try {
            this.propagator.addAttitudeEquation(eqsProviderDefault);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_PROVIDER_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);

        /*
         * TEST 4 : add additional equation representing the attitude for forces computation
         * -> A - Try to add an additional equation representing the attitude for forces computation
         * -> B - Try to add an AttitudeProvider by default
         */
        this.propagator.addAttitudeEquation(eqsProviderForces);

        testOk = false;
        // the test should fail because the force attitude equation is already defined in the propagator:
        try {
            this.propagator.setAttitudeProviderForces(forcesProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // the test should fail because the force attitude equation is already defined in the propagator:
        try {
            this.propagator.setAttitudeProvider(forcesProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.TWO_ATTITUDES_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST 5 : add additional equation representing the attitude for events computation
         * -> A - Try to add an additional equation representing the attitude for events computation
         */
        this.propagator.addAttitudeEquation(eqsProviderEvents);
        testOk = false;
        // the test should fail because the events attitude equation is already defined in the propagator:
        try {
            this.propagator.setAttitudeProviderEvents(eventsProvider);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // Create a clean propagator for another series of exception tests:
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);

        /*
         * TEST 6 : add additional equation representing the attitude by default
         * -> A - Try to add an attitude provider representing the attitude for forces computation
         * -> B - Try to add an attitude provider representing the attitude for events computation
         * -> C - Try to add an attitude provider representing the attitude by default
         * -> D - Try to add an additional equation representing the attitude for forces computation
         * -> E - Try to add an additional equation representing the attitude for events computation
         */
        this.propagator.addAttitudeEquation(eqsProviderDefault);
        testOk = false;
        // A - the test should fail because a single attitude treatment is expected:
        try {
            this.propagator.setAttitudeProviderForces(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        testOk = false;
        // B - the test should fail because a single attitude treatment is expected:
        try {
            this.propagator.setAttitudeProviderEvents(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // C - the test should fail because an additional equation representing the attitude by default is already
        // defined
        try {
            this.propagator.setAttitudeProvider(provider1);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.ATTITUDE_ADD_EQ_ALREADY_DEFINED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // D - the test should fail because a single attitude treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderForces);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // E - the test should fail because a single attitude treatment is expected
        try {
            this.propagator.addAttitudeEquation(eqsProviderEvents);
            Assert.fail();
        } catch (final IllegalStateException e) {
            testOk = true;
            Assert.assertEquals(PatriusMessages.SINGLE_ATTITUDE_TREATMENT_EXPECTED.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        /*
         * TEST X : Other tests
         */
        // Create a clean propagator for another series of exception tests:
        this.propagator = new StelaGTOPropagator(this.integrator);
        final Attitude attitude =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH).getAttitude(this.orbit, this.orbit.getDate(),
                this.orbit.getFrame());
        this.initialState = new SpacecraftState(this.orbit, attitude, attitude);
        this.propagator.setInitialState(this.initialState, 1000, false);

        this.initialState = this.initialState.addAdditionalState("New State", new double[] { 0.0, 0.0 });
        this.propagator.resetInitialState(this.initialState);
        testOk = false;
        // the test should fail because the additional states or the additional equations are empty:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
        this.propagator.addAdditionalEquations(eqsProviderForces);
        testOk = false;
        // the test should fail because the additional states number does not correspond to the additional equations
        // number:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
        this.propagator.addAdditionalEquations(new StelaAdditionalEquations(){
            /** Serializable UID. */
            private static final long serialVersionUID = -7888562276346029589L;

            @Override
            public String getName() {
                return "New Equation";
            }

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return null;
            }

            @Override
            public int getEquationsDimension() {
                return 1;
            }
        });
        testOk = false;
        // the test should fail because the additional states names do not correspond to the additional equations names:
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);

        // the test should fail because the additional states size do not correspond to the additional equations size:
        // Create a clean propagator for another series of exception tests:
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.initialState = new SpacecraftState(this.orbit);
        this.initialState = this.initialState.addAdditionalState("New Equation", new double[] { 0.0, 0.0 });
        this.propagator.resetInitialState(this.initialState);
        this.propagator.addAdditionalEquations(new StelaAdditionalEquations(){
            /** Serializable UID. */
            private static final long serialVersionUID = -9011178647921442697L;

            @Override
            public String getName() {
                return "New Equation";
            }

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return null;
            }

            @Override
            public int getEquationsDimension() {
                return 1;
            }
        });
        try {
            this.propagator.propagate(this.initDate, this.initDate.shiftedBy(200.0));
            Assert.fail();
        } catch (final PatriusException e) {
            testOk = true;
            Assert
                .assertEquals(PatriusMessages.WRONG_CORRESPONDENCE_STATES_EQUATIONS.getSourceString(), e.getMessage());
        }
        Assert.assertTrue(testOk);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @testedMethod {@link StelaGTOPropagator#propagateOrbit(AbsoluteDate)}
     * 
     * @description test propagations with negative cd, cr, Sd or Sr
     * 
     * @input a Stela GTO propagator
     * 
     * @output the final state of the propagation
     * 
     * @testPassCriteria final state is coherent
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testNegativeFeatures() throws PatriusException, IOException, ParseException {

        // Positive Cd, positive Sd = Negative Cd, negative Sd
        final SpacecraftState state1 = this.performPropagation(1000, 10, 1, 10, 2.);
        final SpacecraftState state2 = this.performPropagation(1000, 10, 1, -10, -2.);
        checkFinalStates(state1, state2);

        // Positive Cd, negative Sd = Negative Cd, positive Sd
        final SpacecraftState state3 = this.performPropagation(1000, 10, 1, 10, -2.);
        final SpacecraftState state4 = this.performPropagation(1000, 10, 1, -10, 2.);
        checkFinalStates(state3, state4);

        // Positive Cr, positive Sr = Negative Cr, negative Sr
        final SpacecraftState state5 = this.performPropagation(1000, 10, 1, 10, 2.);
        final SpacecraftState state6 = this.performPropagation(1000, -10, -1, 10, 2.);
        checkFinalStates(state5, state6);

        // Positive Cr, negative Sr = Negative Cr, positive Sr
        final SpacecraftState state7 = this.performPropagation(1000, 10, -1, 10, 2.);
        final SpacecraftState state8 = this.performPropagation(1000, -10, 1, 10, 2.);
        checkFinalStates(state7, state8);
    }

    /**
     * Check provided states are equals
     * 
     * @param state1
     *        a state
     * @param state2
     *        another state
     */
    private static void checkFinalStates(final SpacecraftState state1, final SpacecraftState state2) {
        Assert.assertEquals(state1.getDate().durationFrom(state2.getDate()), 0, 0);
        Assert.assertEquals(state1.getA(), state2.getA(), 0);
        Assert.assertEquals(state1.getEquinoctialEx(), state2.getEquinoctialEx(), 0);
        Assert.assertEquals(state1.getEquinoctialEy(), state2.getEquinoctialEy(), 0);
        Assert.assertEquals(state1.getI(), state2.getI(), 0);
        Assert.assertEquals(state1.getLM(), state2.getLM(), 0);
    }

    /**
     * Perform 10 days propagation.
     * 
     * @param mass
     *        mass
     * @param refArea
     *        reflective area
     * @param refCoef
     *        reflective coefficient
     * @param dragArea
     *        drag area
     * @param dragCd
     *        drag coefficient
     * @return propagated state
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    private SpacecraftState performPropagation(final double mass, final double refArea, final double refCoef,
                                               final double dragArea, final double dragCd)
        throws PatriusException,
        IOException, ParseException {
        // Propagator
        final StelaGTOPropagator propagator = new StelaGTOPropagator(new ClassicalRungeKuttaIntegrator(86400));

        // Add forces
        final MyGRGSFormatReader reader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(reader);
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();

        propagator.addForceModel(new StelaZonalAttraction(provider, 7, true, 2, 0, false));
        propagator.addForceModel(new StelaTesseralAttraction(provider, 7, 2, 86400, 5));
        final MeeusSun sun = new MeeusSun(MODEL.STELA);
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);
        final MSIS00Adapter atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), 6378136.46,
            1 / 0.29825765000000E+03, sun);
        final StelaAeroModel sp = new StelaAeroModel(mass, new StelaCd(dragCd), dragArea, atmosphere, 50.);
        propagator.addForceModel(new StelaAtmosphericDrag(sp, atmosphere, 33, 6378000, 2500000, 1));
        propagator.addForceModel(new StelaThirdBodyAttraction(sun, 4, 2, 0));
        propagator.addForceModel(new StelaThirdBodyAttraction(new MeeusMoonStela(6378136.46), 4, 2, 0));
        propagator.addForceModel(new StelaSRPSquaring(mass, refArea, refCoef, 11, sun));

        // Initial state
        propagator.setInitialState(this.initialState, mass, false);

        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5296377916683744242L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                propagator.resetInitialState(currentState);

            }
        };
        propagator.setMasterMode(86400, handler);

        // Propagation (over 10 days)
        return propagator.propagate(this.initialState.getDate().shiftedBy(86400. * 10));
    }

    /**
     * Set a Dormand-Prince 853 integrator.
     */
    private void setDOPIntegrator() {
        final double minStep = 100;
        final double maxStep = 864.;
        final double[] absTOL = { 1e-5, 1e-5, 1e-5, 1e-8, 1e-8, 1e-8 };
        final double[] relTOL = { 1e-10, 1e-10, 1e-10, 1e-10, 1e-10, 1e-10 };
        this.integrator = new DormandPrince853Integrator(minStep, maxStep, absTOL, relTOL);
        ((AdaptiveStepsizeIntegrator) this.integrator).setInitialStepSize(minStep);
    }

    /**
     * Set a RK4 integrator.
     */
    private void setRK4Integrator() {
        this.integrator = new ClassicalRungeKuttaIntegrator(2.5);
    }

    /**
     * Set a RK6 integrator.
     */
    private void setRK6Integrator() {
        this.integrator = new RungeKutta6Integrator(2.5);
    }

    /**
     * Set a RK6 integrator with step.
     * 
     * @param step
     *        step
     */
    private void setRK6Integrator(final double step) {
        this.integrator = new RungeKutta6Integrator(step);
    }

    /**
     * Set a Stela GTO propagator without partial derivatives computation.
     */
    private void setStelaProp() throws PatriusException {
        this.propagator = new StelaGTOPropagator(this.integrator);
        this.propagator.setInitialState(this.initialState, 1000, false);
        this.propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
    }

    /**
     * Set a Stela GTO propagator without partial derivatives computation.
     */
    private void setStelaProp2() throws PatriusException {
        this.propagator = new StelaGTOPropagator(this.integrator, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY), new StelaBasicInterpolator(),
            0, 0);
        this.propagator.setInitialState(this.initialState, 1000, false);
    }

    /**
     * Set a Stela GTO propagator without partial derivatives computation and with two attitude providers (for forces
     * and events computation)
     */
    private void setStelaProp3() throws PatriusException {
        final AttitudeProvider bodyCenterPointing = new BodyCenterPointing(FramesFactory.getGCRF());
        final AttitudeProvider inertial = new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY);
        this.propagator =
            new StelaGTOPropagator(this.integrator, inertial, bodyCenterPointing, new StelaBasicInterpolator(),
                0, 0);
        this.propagator.setInitialState(this.initialState, 1000, false);
    }

    /**
     * Set a Stela GTO propagator without partial derivatives computation and with two attitude equations (for forces
     * and events computation)
     */
    private void setStelaProp4() throws PatriusException {
        final StelaAttitudeAdditionalEquations eqsProviderForces = new StelaAttitudeAdditionalEquations(
            AttitudeType.ATTITUDE_FORCES){
            /** Serializable UID. */
            private static final long serialVersionUID = -5272312428818388681L;

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return state.addAttitudeToAdditionalStates(this.getAttitudeType());
            }

            @Override
            public int getEquationsDimension() {
                return 7;
            }
        };
        final StelaAttitudeAdditionalEquations eqsProviderEvents = new StelaAttitudeAdditionalEquations(
            AttitudeType.ATTITUDE_EVENTS){
            /** Serializable UID. */
            private static final long serialVersionUID = -1043251819956970873L;

            @Override
            public void
                computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                    throws PatriusException {
                // nothing to do
            }

            @Override
            public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                return state.addAttitudeToAdditionalStates(this.getAttitudeType());
            }

            @Override
            public int getEquationsDimension() {
                return 7;
            }
        };

        this.propagator = new StelaGTOPropagator(this.integrator, 0, 0);
        final Attitude attitude =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH).getAttitude(this.orbit, this.orbit.getDate(),
                this.orbit.getFrame());
        this.initialState = new SpacecraftState(this.orbit, attitude, attitude);
        this.propagator.setInitialState(this.initialState, 1000, false);
        this.propagator.addAttitudeEquation(eqsProviderEvents);
        this.propagator.addAttitudeEquation(eqsProviderForces);
    }

    /**
     * Set a Stela GTO propagator without partial derivatives computation and with a single attitude equation
     */
    private void setStelaProp5() throws PatriusException {
        final StelaAttitudeAdditionalEquations eqsProvider =
            new StelaAttitudeAdditionalEquations(AttitudeType.ATTITUDE){
                /** Serializable UID. */
                private static final long serialVersionUID = -4826705435071210277L;

                @Override
                public void
                    computeDerivatives(final StelaEquinoctialOrbit o, final double[] p, final double[] pDot)
                        throws PatriusException {
                    // nothing to do
                }

                @Override
                public SpacecraftState addInitialAdditionalState(final SpacecraftState state) throws PatriusException {
                    return state.addAttitudeToAdditionalStates(this.getAttitudeType());
                }

                @Override
                public int getEquationsDimension() {
                    return 7;
                }
            };

        this.propagator = new StelaGTOPropagator(this.integrator, 0, 0);
        final Attitude attitude =
            new LofOffset(this.orbit.getFrame(), LOFType.LVLH).getAttitude(this.orbit, this.orbit.getDate(),
                this.orbit.getFrame());
        this.initialState = new SpacecraftState(this.orbit, attitude, attitude);
        this.propagator.setInitialState(this.initialState, 1000, false);
        this.propagator.addAttitudeEquation(eqsProvider);
    }

    /**
     * Set a Stela GTO propagator with partial derivatives computation.
     * 
     * @param force
     *        the force to add to the propagator
     */
    private void setStelaPropWithPD(final StelaForceModel force) throws PatriusException {

        this.propagator = new StelaGTOPropagator(this.integrator);
        // propagator.setMass(1000);
        this.propagator.setInitialState(this.initialState, 1000, false);

        this.propagator.addForceModel(force);
        // Add the partial derivatives equations:
        final StelaPartialDerivativesEquations pd = new StelaPartialDerivativesEquations(
            this.propagator.getGaussForceModels(), this.propagator.getLagrangeForceModels(), 1, this.propagator);
        this.propagator.addAdditionalEquations(pd);
    }

    /**
     * Test from FA-359: required stepsize is about 5.27s.
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test reentry with step size control
     * 
     * @testPassCriteria exceptions thrown as expected (if min stepsize > 5.27s)
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testReentry() {

        // Min step size is set to 1s: propagation should end properly
        try {
            final SpacecraftState finalState = this.propagation(1.);
            Assert.assertNotNull(finalState);
        } catch (final Exception e) {
            Assert.fail();
        }

        // Min step size is set to 10s: propagation should fail since required step size is about 5.27s
        try {
            this.propagation(8.);
            Assert.fail();
        } catch (final Exception e) {
            Assert.assertEquals(PatriusMessages.STELA_INTEGRATION_FAILED.getSourceString(), e.getMessage());
            Assert.assertTrue(true);
        }
    }

    /**
     * Propagation method.
     * 
     * @param minStepSize
     *        min step size allowed (s)
     * @return propagated spacecraft state
     */
    private SpacecraftState propagation(final double minStepSize) throws PatriusException, IOException, ParseException {

        // Init data
        Utils.setDataRoot("stela/ft359/");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());
        TimeScalesFactory.addUTCTAILoader(new MyUTCTAILoader());

        // Initial State
        final AbsoluteDate initialDate = new AbsoluteDate(2007, 11, 17, 13, 59, 46.620, TimeScalesFactory.getUTC());
        final StelaEquinoctialOrbit initMeanSV = new StelaEquinoctialOrbit(6826108.902851214, 0.007489961550459905,
            0.01658439576130266,
            0.7527184670182551, 0.11140936641140112, 0.17578433186628573, FramesFactory.getMOD(false), initialDate,
            Constants.CNES_STELA_MU, false);
        final SpacecraftState initialMeanState = new SpacecraftState(initMeanSV);
        final double mass = 1.0;
        final double smRef = 0.08404948005082934;

        // Potential reader
        final MyGRGSFormatReader potReader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(potReader);
        final PotentialCoefficientsProvider potProvider = GravityFieldFactory.getPotentialProvider();

        // Initialization of the propagator
        final StelaGTOPropagator patriusProp = new StelaGTOPropagator(new RungeKutta6Integrator(Constants.JULIAN_DAY),
            5, minStepSize);

        final OrbitNatureConverter orbConv = new OrbitNatureConverter(patriusProp.getForceModels());
        patriusProp.setInitialState(initialMeanState, mass, false);

        // Adding the maximum altitude detector
        patriusProp.addEventDetector(new PerigeeAltitudeDetector(Constants.JULIAN_DAY, 0.5, 80000.0,
            Constants.CNES_STELA_AE, orbConv));

        // Forces
        final StelaZonalAttraction zonalsForce = new StelaZonalAttraction(potProvider, 7, true, 2, 0, false);

        // Solar activity
        SolarActivityDataFactory.addSolarActivityDataReader(new ACSOLFormatReader("stela_solar_activity_provided"));
        final SolarActivityDataProvider solarActivity = SolarActivityDataFactory.getSolarActivityDataProvider();
        final MSIS00Adapter atmosModel = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity),
            Constants.CNES_STELA_AE,
            Constants.UAI1994_EARTH_FLATTENING, new MeeusSun(MODEL.STELA));

        // Initializing the Drag model
        final StelaCd dragCoeff = initDragCoefficient();
        final StelaAeroModel aeroModel = new StelaAeroModel(mass, dragCoeff, smRef, atmosModel, 50);
        final StelaAtmosphericDrag atmosDragModel = new StelaAtmosphericDrag(aeroModel, atmosModel, 33,
            Constants.CNES_STELA_AE, 2500000.0, 1);

        // Forces
        patriusProp.addForceModel(zonalsForce);
        patriusProp.addForceModel(atmosDragModel);

        // Adding the step handler
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){

            /** Serializable UID. */
            private static final long serialVersionUID = 590182838561580249L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                patriusProp.resetInitialState(currentState);
            }
        };

        patriusProp.setMasterMode(Constants.JULIAN_DAY, handler);

        // Do not include SRP and drag in nature conversion (non-regression with PATRIUS 4.10)
        final List<StelaForceModel> forceModelsNature = new ArrayList<StelaForceModel>();
        forceModelsNature.add(zonalsForce);
        patriusProp.setNatureConverter(forceModelsNature);

        // Propagation
        final AbsoluteDate targetDate = initialDate.shiftedBy(5.0 * 365.0 * Constants.JULIAN_DAY);
        return patriusProp.propagate(initialDate, targetDate);
    }

    /**
     * @throws PatriusException
     * @throws ParseException
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_GTO_PROPAGATION}
     * 
     * @description test time derivatives
     * 
     * @testPassCriteria propagation with Euler 1st order scheme gives the same result as step by step time derivatives
     *                   integration, extracted state, mean motion and derivative are good and time derivatives are
     *                   sampled once every step
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testTimeDerivatives() throws PatriusException, IOException, ParseException {

        // ========================= Propagation on 10 days with Euler integrator ========================= //

        // Potential reader
        final MyGRGSFormatReader potReader = new MyGRGSFormatReader("stelaCoefficients", true);
        GravityFieldFactory.addPotentialCoefficientsReader(potReader);
        final PotentialCoefficientsProvider potProvider = GravityFieldFactory.getPotentialProvider();

        // Initializing the Drag model
        final MSIS00Adapter atmosModel = new MSIS00Adapter(new ClassicalMSISE2000SolarData(new ConstantSolarActivity(
            140., 15.)), Constants.CNES_STELA_AE,
            Constants.UAI1994_EARTH_FLATTENING, new MeeusSun(MODEL.STELA));
        final StelaAeroModel aeroModel = new StelaAeroModel(1., new StelaCd(2.2), 1., atmosModel, 50);

        // Forces
        final List<StelaForceModel> forceModels = new ArrayList<>();
        forceModels.add(new StelaZonalAttraction(potProvider, 7, true, 2, 0, false));
        forceModels.add(new StelaTesseralAttraction(potProvider));
        forceModels.add(new StelaAtmosphericDrag(aeroModel, atmosModel, 33, Constants.CNES_STELA_AE, 2500000.0, 1));
        final AbstractStelaGaussContribution force = new StelaSRPSquaring(1., 1., 1.5, 11, new MeeusSun(MODEL.STELA));
        forceModels.add(force);
        forceModels.add(new NonInertialContribution(11, FramesFactory.getGCRF()));

        // Propagator
        final StelaGTOPropagator propagator = buildPropagatorTimeDerivatives(forceModels, new EulerIntegrator(
            Constants.JULIAN_DAY), 1);

        // Propagation on 10 days
        propagator.setStoreTimeDerivatives(true);
        final Orbit initialOrbit = propagator.getInitialState().getOrbit();
        final AbsoluteDate initialDate = propagator.getInitialState().getDate();
        final SpacecraftState finalState = propagator.propagate(initialDate,
            initialDate.shiftedBy(10. * Constants.JULIAN_DAY));
        final List<TimeDerivativeData> list = propagator.getTimeDerivativesList();

        // ========================= Propagation on 10 days using time derivatives ========================= //

        final double[] state = new double[] { 7000000, 0., 0., 0., 0., 0. };
        final double[][] stm = new double[][] {
            { 1., 0., 0., 0., 0., 0., 0., 0. },
            { 0., 1., 0., 0., 0., 0., 0., 0. },
            { 0., 0., 1., 0., 0., 0., 0., 0. },
            { 0., 0., 0., 1., 0., 0., 0., 0. },
            { 0., 0., 0., 0., 1., 0., 0., 0. },
            { 0., 0., 0., 0., 0., 1., 0., 0. },
        };
        for (int i = 0; i < 10; i++) {
            final double[] contrib = list.get(i).getTotalContribution();
            final double[][] contribSTM = list.get(i).getTotalContributionSTM();
            for (int j = 0; j < contrib.length; j++) {
                state[j] += contrib[j] * Constants.JULIAN_DAY;
                for (int k = 0; k < contribSTM[j].length; k++) {
                    stm[j][k] += contribSTM[j][k] * Constants.JULIAN_DAY;
                }
            }
        }
        final double[] stmVect = new double[6 * 8];
        JavaMathAdapter.matrixToVector(stm, stmVect, 0);

        // Check final state
        final StelaEquinoctialParameters expected = finalState.getOrbit().getParameters()
            .getStelaEquinoctialParameters();
        final double[] expectedSTM = finalState.getAdditionalState("PARTIAL_DERIVATIVES");
        Assert.assertEquals((expected.getA() - state[0]) / state[0], 0, 1E-15);
        Assert.assertEquals((expected.getEquinoctialEx() - state[2]) / state[2], 0, 1E-15);
        Assert.assertEquals((expected.getEquinoctialEy() - state[3]) / state[3], 0, 1E-15);
        Assert.assertEquals((expected.getIx() - state[4]) / state[4], 0, 1E-15);
        Assert.assertEquals((expected.getIy() - state[5]) / state[5], 0, 1E-15);
        Assert.assertEquals((expected.getLM() - state[1]) / state[1], 0, 1E-15);
        for (int i = 0; i < expectedSTM.length; i++) {
            if (stmVect[i] != 0) {
                Assert.assertEquals((expectedSTM[i] - stmVect[i]) / stmVect[i], 0, 1E-15);
            } else {
                Assert.assertEquals(expectedSTM[i], stmVect[i], 1E-15);
            }
        }

        // Check available force models
        Assert.assertEquals(list.get(0).getAvailableForceModels().size(), 5);
        Assert.assertEquals(list.get(0).getAvailableForceModelsSTM().size(), 5);

        // Check initial state
        final double[] expectedInitialState = new double[] { 7000000, 0., 0., 0., 0., 0. };
        final StelaEquinoctialParameters initialParams = list.get(0).getOrbit().getParameters()
            .getStelaEquinoctialParameters();
        final double[] actualInitialState = new double[] { initialParams.getA(), initialParams.getLM(),
            initialParams.getEquinoctialEx(),
            initialParams.getEquinoctialEy(), initialParams.getIx(), initialParams.getIy() };
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(expectedInitialState[i], actualInitialState[i], 0);
        }

        // Check mean motion
        final double[] expectedMeanMotion = new double[6];
        final double[][] expectedMeanMotionSTM = new double[6][8];
        expectedMeanMotionSTM[1][0] = -3.0
                * MathLib.sqrt(initialOrbit.getMu()
                        / (initialOrbit.getA() * initialOrbit.getA() * initialOrbit.getA()))
                / (2.0 * initialOrbit.getA());
        ((StelaEquinoctialOrbit) list.get(0).getOrbit()).addKeplerContribution(PositionAngle.MEAN,
            Constants.CNES_STELA_MU, expectedMeanMotion);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(expectedMeanMotion[i], list.get(0).getMeanMotion()[i], 0);
            for (int j = 0; j < expectedMeanMotionSTM[i].length; j++) {
                Assert.assertEquals(expectedMeanMotionSTM[i][j], list.get(0).getMeanMotionSTM()[i][j], 0);
            }
        }

        // Check derivative (SRP)
        final double[] expectedDerivative = { -3.729652240478421E-6, 4.912262413926257E-10, 8.760975783806423E-10,
            1.756180192119048E-10, 1.022333051130011E-11, -5.119652856700734E-11 };
        final double[][] expectedDerivativeSTM = {
            { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, -3.801444810898808E-10, 1.9037999959700484E-9, 0.0, 0.0, 0.0, 0.0 },
            { 9.065714266524039E-17, 0.0, 0.0, 0.0, 1.1004596668591072E-9, 0.0, 0.0, 1.2691999973133657E-9 },
            { 1.8102118147137176E-17, 0.0, 0.0, 0.0, 0.0, 1.1004596668591072E-9, 0.0, 2.5342965405992054E-10 },
            { 0.0, 0.0, -2.751149167147768E-10, 0.0, 0.0, 0.0, 0.0, 0.0 },
            { 0.0, 0.0, 0.0, -2.751149167147768E-10, 0.0, 0.0, 0.0, 0.0 }
        };
        final double[] actualDerivative = list.get(0).getDerivatives(force);
        final double[][] actualDerivativeSTM = list.get(0).getDerivativesSTM(force);
        for (int i = 0; i < 6; i++) {
            Assert.assertEquals(expectedDerivative[i], actualDerivative[i], 0);
            for (int j = 0; j < expectedDerivativeSTM[i].length; j++) {
                Assert.assertEquals(expectedDerivativeSTM[i][j], actualDerivativeSTM[i][j], 0);
            }
        }

        // ========================= Propagation with RK ========================= //

        // Propagator
        final StelaGTOPropagator propagator2 =
            buildPropagatorTimeDerivatives(forceModels, new RungeKutta6Integrator(Constants.JULIAN_DAY), 2);

        // Propagation on 10 days
        propagator2.setStoreTimeDerivatives(true);
        propagator2.propagate(initialDate, initialDate.shiftedBy(10. * Constants.JULIAN_DAY));
        final List<TimeDerivativeData> list2 = propagator2.getTimeDerivativesList();

        // Check there is one time derivative every step and not every sub-step
        Assert.assertEquals(list2.get(1).getOrbit().getDate().durationFrom(list2.get(0).getOrbit().getDate()), 86400.,
            0);
        Assert.assertEquals(list2.size(), 10);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link NumericalPropagator#setOrbitFrame(Frame)}
     * 
     * @description This test aims at verifying that an exception is risen if a non
     *              pseudo-inertial or non inertial frame is provided for propagation
     * 
     * @input NumericalPropagator
     * @input Frame provided for propagation : TIRF
     * 
     * @output Expected an OrekitException
     * @throws PatriusException
     * @testPassCriteria An OrekitException must be caught
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test(expected = PatriusException.class)
    public void testSetNonInertialFrame() throws PatriusException {

        // Initial state
        final AbsoluteDate initDate = AbsoluteDate.J2000_EPOCH;
        final KeplerianOrbit orbit = new KeplerianOrbit(8000E3, 0, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getITRF(), initDate, Constants.EGM96_EARTH_MU);

        // Propagator
        final StelaGTOPropagator prop = new StelaGTOPropagator(new ClassicalRungeKuttaIntegrator(30.));

        prop.setInitialState(new SpacecraftState(orbit), 1000, false);
        final Frame from = FramesFactory.getTIRF();

        // An exception should occur here !
        prop.setOrbitFrame(from);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link StelaGTOPropagator#propagate(AbsoluteDate)}
     * 
     * @description This test checks that a propagation with an initial mean orbit returns exactly the same result than
     *              with an initial osculating orbit.
     * 
     * @input mean orbit and its osculating counter part
     * 
     * @output same result
     * 
     * @testPassCriteria both propagations returns the exact same result
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testMeanOsculatingPropagation() throws PatriusException, IOException, ParseException {

        // Initialization
        final AbsoluteDate initialDate = new AbsoluteDate("2010-01-01T00:00:00.000", TimeScalesFactory.getUTC());
        final KeplerianOrbit initialOrbit = new KeplerianOrbit(42168.e3, 0.001, MathLib.toRadians(0.1), 0, 0, 0,
            PositionAngle.MEAN, FramesFactory.getCIRF(),
            initialDate, Constants.WGS84_EARTH_MU);
        final StelaGTOPropagator propagator = new StelaGTOPropagator(new RungeKutta6Integrator(86400.), 5., 1.);
        final PotentialCoefficientsProvider provider = GravityFieldFactory.getPotentialProvider();
        propagator.addForceModel(new StelaZonalAttraction(provider, 4, true, 2, 0, false));
        propagator.addForceModel(new StelaTesseralAttraction(provider, 4, 5, 86400., 2));
        propagator.addForceModel(new StelaSRPSquaring(1000, 60, 1.0, 11, new MeeusSun(MODEL.STELA)));
        propagator.addForceModel(new StelaThirdBodyAttraction(new MeeusSun(MODEL.STELA), 4, 2, 2));
        propagator.addForceModel(new StelaThirdBodyAttraction(new MeeusMoonStela(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS), 4, 2, 2));

        // Propagation from both mean/osculating elements
        final OrbitNatureConverter orbitNatureConverter = propagator.getOrbitNatureConverter();

        final StelaEquinoctialOrbit oscInitSEOrb = new StelaEquinoctialOrbit(initialOrbit);
        propagator.setInitialState(new SpacecraftState(oscInitSEOrb), 1000, true);
        final SpacecraftState finalStateOsc = propagator.propagate(initialDate.shiftedBy(1));
        final StelaEquinoctialOrbit finalOrbitOsc = new StelaEquinoctialOrbit(finalStateOsc.getOrbit());

        final StelaEquinoctialOrbit meanInitSEOrb = orbitNatureConverter.toMean(oscInitSEOrb);
        propagator.setInitialState(new SpacecraftState(meanInitSEOrb), 1000, false);
        final SpacecraftState finalStateMean = propagator.propagate(initialDate.shiftedBy(1));
        final StelaEquinoctialOrbit finalOrbitMean = new StelaEquinoctialOrbit(finalStateMean.getOrbit());

        // Check
        Assert.assertEquals(finalOrbitMean.getA(), finalOrbitOsc.getA(), 0);
        Assert.assertEquals(finalOrbitMean.getEquinoctialEx(), finalOrbitOsc.getEquinoctialEx(), 0);
        Assert.assertEquals(finalOrbitMean.getEquinoctialEy(), finalOrbitOsc.getEquinoctialEy(), 0);
        Assert.assertEquals(finalOrbitMean.getIx(), finalOrbitOsc.getIx(), 0);
        Assert.assertEquals(finalOrbitMean.getIy(), finalOrbitOsc.getIy(), 0);
        Assert.assertEquals(finalOrbitMean.getLM(), finalOrbitOsc.getLM(), 0);

        // check the method getSpacecraftState
        final SpacecraftState state = propagator.getSpacecraftState(initialDate.shiftedBy(1));
        Assert.assertEquals(finalStateMean.getPVCoordinates().getPosition().getNorm(),
            state.getPVCoordinates().getPosition().getNorm(), Utils.epsilonTest);
    }

    /**
     * Propagation method.
     * 
     * @param forceModels
     *        force models
     * @param integrator
     *        integrator
     * @param recomputeStep
     *        recompute step
     * @return propagated spacecraft state
     */
    private static StelaGTOPropagator buildPropagatorTimeDerivatives(final List<StelaForceModel> forceModels,
                                                                     final FirstOrderIntegrator integrator,
                                                                     final int recomputeStep)
        throws PatriusException {

        // Initial State
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final StelaEquinoctialOrbit initialOrbit = new StelaEquinoctialOrbit(7000000, 0., 0., 0., 0., 0.,
            FramesFactory.getCIRF(),
            initialDate, Constants.CNES_STELA_MU, false);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Initialization of the propagator
        final StelaGTOPropagator propagator = new StelaGTOPropagator(integrator, 5, 1.);

        // Forces
        for (final StelaForceModel stelaForceModel : forceModels) {
            propagator.addForceModel(stelaForceModel);
        }

        // Adding the step handler
        final PatriusFixedStepHandler handler = new PatriusFixedStepHandler(){
            /** Serializable UID. */
            private static final long serialVersionUID = -5728500984690470921L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                // nothing to do
            }

            @Override
            public void handleStep(final SpacecraftState currentState, final boolean isLast)
                throws PropagationException {
                propagator.resetInitialState(currentState);
            }
        };

        propagator.setMasterMode(Constants.JULIAN_DAY, handler);

        // Add partial derivatives
        final StelaPartialDerivativesEquations eq = new StelaPartialDerivativesEquations(
            propagator.getGaussForceModels(),
            propagator.getLagrangeForceModels(), recomputeStep, propagator);
        propagator.addAdditionalEquations(eq);
        propagator.setInitialState(eq.addInitialAdditionalState(initialState), 1., false);

        return propagator;
    }

    /**
     * Initialize drag coefficient.
     * 
     * @return drag coefficient
     */
    private static StelaCd initDragCoefficient() throws IOException {

        // Drag coef
        final Map<Double, Double> map = new TreeMap<>();
        final FileReader fileReader = new FileReader(ClassLoader.getSystemResource(
            "stela/ft359/stela_drag_coefficients").getFile());
        final BufferedReader reader = new BufferedReader(fileReader);

        // Skip header
        for (int i = 0; i < 6; i++) {
            reader.readLine();
        }

        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            final String[] tab = line.trim().split("\\s+");
            map.put(Double.parseDouble(tab[0]), Double.parseDouble(tab[1]));
        }
        reader.close();
        return new StelaCd(map, Constants.CNES_STELA_AE, Constants.UAI1994_EARTH_FLATTENING);
    }

    /**
     * Constant UTC-TAI loader.
     */
    private class MyUTCTAILoader implements UTCTAILoader {

        public MyUTCTAILoader() {
        }

        @Override
        public boolean stillAcceptsData() {
            return false;
        }

        @Override
        public void loadData(final InputStream input, final String name)
            throws IOException, ParseException,
            PatriusException {
            // nothing to do
        }

        @Override
        public SortedMap<DateComponents, Integer> loadTimeSteps() {
            final SortedMap<DateComponents, Integer> entries = new TreeMap<>();
            entries.put(DateComponents.J2000_EPOCH, 35);
            entries.put(new DateComponents(2200, 1, 1), 35);
            return entries;
        }

        @Override
        public String getSupportedNames() {
            return "";
        }
    }

    /**
     * Setup method.
     * 
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     */
    @Before
    public void setUp() throws PatriusException, IOException, ParseException {
        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getStelaConfiguration());

        this.initDate = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.),
            TimeScalesFactory.getTAI());
        final double rt = 6378 * 1000;
        this.orbit = new ApsisOrbit(180000.0 + rt, 36000000 + rt, MathLib.toRadians(10.),
            MathLib.toRadians(30.), MathLib.toRadians(20.), MathLib.toRadians(45.),
            PositionAngle.MEAN, FramesFactory.getCIRF(), this.initDate, Constants.EGM96_EARTH_MU);
        this.initialState = new SpacecraftState(this.orbit);
    }

    /**
     * Custom event detector.
     * This date detector should detect two dates.
     * The returned Action of eventOccured method is entered as a parameter of the constructor.
     * If action = Action.CONTINUE : eventOccurred should be called two times.
     */
    class MyDoubleDateDetector extends DateDetector {

        /** Serializable UID. */
        private static final long serialVersionUID = 4945455545781779512L;
        private int count = 0;
        private Action action;

        public MyDoubleDateDetector(final AbsoluteDate target1, final AbsoluteDate target2, final Action action,
                                    final boolean remove) {
            super(target1, 10., 10.e-10, action, remove);
            this.addEventDate(target2);
        }

        @Override
        public Action
            eventOccurred(final SpacecraftState s, final boolean increasing, final boolean forward)
                throws PatriusException {
            this.count++;
            return this.action;
        }

        public int getCount() {
            return this.count;
        }
    }

    /**
     * Set up test for drag perturbation
     * 
     * @param atmosphere
     * 
     * @throws PatriusException
     */
    public void dragSetUp() throws PatriusException {

        // UTC-TAI leap seconds:
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name) throws IOException, ParseException,
                PatriusException {
                // nothing to do
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> map = new TreeMap<>();
                for (int i = 2000; i < 2112; i++) {
                    // constant value:
                    map.put(new DateComponents(i, 11, 13), 35);
                }
                return map;
            }

            @Override
            public String getSupportedNames() {
                return "No name";
            }
        });

        // earth - stela values
        final double f = 0.29825765000000E+03;
        final double ae = 6378136.46;
        // earth - stela values

        // Celestial bodies:
        this.sun = new MeeusSun(MODEL.STELA);

        // Constant solar activity:
        final ConstantSolarActivity solarActivity = new ConstantSolarActivity(140.00, 15.);

        // Atmosphere:
        this.atmosphere = new MSIS00Adapter(new ClassicalMSISE2000SolarData(solarActivity), ae, 1 / f, this.sun);

        // trying exponential implementation, as used in test class
        // final Frame mod = FramesFactory.getMOD(false);
        // atmosphere = new SimpleExponentialAtmosphere(new OneAxisEllipsoid(Utils.ae, 1.0 / 298.257222101, mod),
        // 0.0004, 42000.0, 7500.0);
    }

    /**
     * Dummy StelaForceModel for test purposes.
     */
    class DummyForceModel extends AbstractStelaLagrangeContribution {

        /** Serializable UID. */
        private static final long serialVersionUID = 3747910104824649979L;

        @Override
        public double[] computePerturbation(final StelaEquinoctialOrbit orbit) {
            final double t = orbit.getDate().durationFrom(StelaGTOPropagatorTest.this.initialState.getDate());
            final double[] rez = { 0., 0., 0., 0., 0., t };
            return rez;
        }

        @Override
        public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
                final OrbitNatureConverter converter) {
            return new double[6];
        }

        @Override
        public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
            final double[][] rez = new double[6][6];
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {

                    rez[i][j] = 0.2;
                }
            }
            return rez;
        }
    }

    /**
     * Dummy StelaForceModel 2 for test purposes.
     */
    class DummyForceModel2 extends AbstractStelaLagrangeContribution {
        /** Serializable UID. */
        private static final long serialVersionUID = 4907813395041876706L;

        @Override
        public double[] computePerturbation(final StelaEquinoctialOrbit orbit) {
            final double t = orbit.getDate().durationFrom(StelaGTOPropagatorTest.this.initialState.getDate());
            final double[] rez = { 0., 0., 0., 0., 0., t };
            return rez;
        }

        @Override
        public double[] computeShortPeriods(final StelaEquinoctialOrbit orbit,
                final OrbitNatureConverter converter) {
            return new double[6];
        }

        @Override
        public double[][] computePartialDerivatives(final StelaEquinoctialOrbit orbit) {
            final double t = orbit.getDate().durationFrom(StelaGTOPropagatorTest.this.initialState.getDate());
            // the partial derivatives are function of the time:
            final double[][] rez = new double[6][6];
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 6; j++) {

                    rez[i][j] = t;
                }
            }
            return rez;
        }
    }
}

/*
 * NOTE
 * The GRGS reader, and abstract reader below extend the original classes because zonal and tesseral coefficients test
 * values are unnormalized, whereas the original implementations require normalized coefficients. The intent of theses
 * classes is to override the getC and getS method to return the coefficients as read, whitout any further operations.
 */

class MyGRGSFormatReader extends MyPotentialCoefficientsReader {

    /** Serializable UID. */
    private static final long serialVersionUID = 85386992419831956L;
    /** Patterns for lines (the last pattern is repeated for all data lines). */
    private static final Pattern[] LINES;

    static {

        // sub-patterns
        final String real = "[-+]?\\d?\\.\\d+[eEdD][-+]\\d\\d";
        final String sep = ")\\s*(";

        // regular expression for header lines
        final String[] header = { "^\\s*FIELD - .*$", "^\\s+AE\\s+1/F\\s+GM\\s+OMEGA\\s*$",
            "^\\s*(" + real + sep + real + sep + real + sep + real + ")\\s*$",
            "^\\s*REFERENCE\\s+DATE\\s+:\\s+\\d.*$", "^\\s*MAXIMAL\\s+DEGREE\\s+:\\s+(\\d+)\\s.*$",
            // case insensitive for the next line
            "(?i)^\\s*L\\s+M\\s+DOT\\s+CBAR\\s+SBAR\\s+SIGMA C\\s+SIGMA S(\\s+LIB)?\\s*$" };

        // regular expression for data lines
        final String data = "^([ 0-9]{3})([ 0-9]{3})(   |DOT)\\s*(" + real + sep + real + sep + real + sep + real
                + ")(\\s+[0-9]+)?\\s*$";

        // compile the regular expressions
        LINES = new Pattern[header.length + 1];
        for (int i = 0; i < header.length; ++i) {
            LINES[i] = Pattern.compile(header[i]);
        }
        LINES[LINES.length - 1] = Pattern.compile(data);

    }

    /**
     * Simple constructor.
     * 
     * @param supportedNames
     *        regular expression for supported files names
     * @param missingCoefficientsAllowed
     *        if true, allows missing coefficients in the input data
     */
    public MyGRGSFormatReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    /**
     * {@inheritDoc@Override
}
     */
    @Override
    public void loadData(final InputStream input, final String name) throws IOException, ParseException,
        PatriusException {

        // FIELD - GRIM5, VERSION : C1, november 1999
        // AE 1/F GM OMEGA
        // 0.63781364600000E+070.29825765000000E+030.39860044150000E+150.72921150000000E-04
        // REFERENCE DATE : 1997.00
        // MAXIMAL DEGREE : 120 Sigmas calibration factor : .5000E+01 (applied)
        // L M DOT CBAR SBAR SIGMA C SIGMA S
        // 2 0DOT 0.13637590952454E-10 0.00000000000000E+00 .143968E-11 .000000E+00
        // 3 0DOT 0.28175700027753E-11 0.00000000000000E+00 .496704E-12 .000000E+00
        // 4 0DOT 0.12249148508277E-10 0.00000000000000E+00 .129977E-11 .000000E+00
        // 0 0 .99999999988600E+00 .00000000000000E+00 .153900E-09 .000000E+00
        // 2 0 -0.48416511550920E-03 0.00000000000000E+00 .204904E-10 .000000E+00

        final BufferedReader r = new BufferedReader(new InputStreamReader(input));
        boolean okConstants = false;
        boolean okMaxDegree = false;
        boolean okCoeffs = false;
        int lineNumber = 0;
        for (String line = r.readLine(); line != null; line = r.readLine()) {

            ++lineNumber;

            // match current header or data line
            final Matcher matcher = LINES[MathLib.min(LINES.length, lineNumber) - 1].matcher(line);
            if (!matcher.matches()) {
                throw PatriusException.createParseException(PatriusMessages.UNABLE_TO_PARSE_LINE_IN_FILE, lineNumber,
                    name, line);
            }

            if (lineNumber == 3) {
                // header line defining ae, 1/f, GM and Omega
                this.ae = Double.parseDouble(matcher.group(1).replace('D', 'E'));
                this.mu = Double.parseDouble(matcher.group(3).replace('D', 'E'));
                okConstants = true;
            } else if (lineNumber == 5) {
                // header line defining max degree
                final int maxDegree = Integer.parseInt(matcher.group(1));
                this.normalizedC = new double[maxDegree + 1][];
                this.normalizedS = new double[maxDegree + 1][];
                for (int k = 0; k < this.normalizedC.length; k++) {
                    this.normalizedC[k] = new double[k + 1];
                    this.normalizedS[k] = new double[k + 1];
                    if (!this.missingCoefficientsAllowed()) {
                        Arrays.fill(this.normalizedC[k], Double.NaN);
                        Arrays.fill(this.normalizedS[k], Double.NaN);
                    }
                }
                if (this.missingCoefficientsAllowed()) {
                    // set the default value for the only expected non-zero coefficient
                    this.normalizedC[0][0] = 1.0;
                }
                okMaxDegree = true;
            } else if (lineNumber > 6) {
                // data line
                if ("".equals(matcher.group(3).trim())) {
                    // non-dot data line
                    final int i = Integer.parseInt(matcher.group(1).trim());
                    final int j = Integer.parseInt(matcher.group(2).trim());
                    this.normalizedC[i][j] = Double.parseDouble(matcher.group(4).replace('D', 'E'));
                    this.normalizedS[i][j] = Double.parseDouble(matcher.group(5).replace('D', 'E'));
                    okCoeffs = true;
                }
            }

        }

        for (int k = 0; okCoeffs && k < this.normalizedC.length; k++) {
            final double[] cK = this.normalizedC[k];
            final double[] sK = this.normalizedS[k];
            for (int i = 0; okCoeffs && i < cK.length; ++i) {
                if (Double.isNaN(cK[i])) {
                    okCoeffs = false;
                }
            }
            for (int i = 0; okCoeffs && i < sK.length; ++i) {
                if (Double.isNaN(sK[i])) {
                    okCoeffs = false;
                }
            }
        }

        if (!(okConstants && okMaxDegree && okCoeffs)) {
            String loaderName = this.getClass().getName();
            loaderName = loaderName.substring(loaderName.lastIndexOf('.') + 1);
            throw new PatriusException(PatriusMessages.UNEXPECTED_FILE_FORMAT_ERROR_FOR_LOADER, name, loaderName);
        }

        // normalizedC = normalize(normalizedC);
        // normalizedS = normalize(normalizedS);
        this.readCompleted = true;
    }
}

abstract class MyPotentialCoefficientsReader extends PotentialCoefficientsReader {

    /** Serializable UID. */
    private static final long serialVersionUID = 6951674850445373540L;

    protected MyPotentialCoefficientsReader(final String supportedNames, final boolean missingCoefficientsAllowed) {
        super(supportedNames, missingCoefficientsAllowed);
    }

    @Override
    public double[] getJ(final boolean normalized, final int n) throws PatriusException {
        return super.getJ(true, n);
    }

    @Override
    public double[][] getC(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, super.getC(n, m, true));
    }

    @Override
    public double[][] getS(final int n, final int m, final boolean normalized) throws PatriusException {
        return truncateArray(n, m, super.getS(n, m, true));
    }

    private static double[][] truncateArray(final int n, final int m, final double[][] complete)
        throws PatriusException {

        // safety checks
        if (n >= complete.length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_DEGREE_FOR_GRAVITY_FIELD, n, complete.length - 1);
        }
        if (m >= complete[complete.length - 1].length) {
            throw new PatriusException(PatriusMessages.TOO_LARGE_ORDER_FOR_GRAVITY_FIELD, m,
                complete[complete.length - 1].length - 1);
        }

        // truncate each array row in turn
        final double[][] result = new double[n + 1][];
        for (int i = 0; i <= n; i++) {
            final double[] ri = new double[MathLib.min(i, m) + 1];
            System.arraycopy(complete[i], 0, ri, 0, ri.length);
            result[i] = ri;
        }

        return result;
    }
}
