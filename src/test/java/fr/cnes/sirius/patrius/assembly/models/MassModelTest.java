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
 * @history creation 25/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2460:27/05/2020:Prise en compte des temps de propagation dans les calculs evenements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:184:17/12/2013:Added test to make sure that the propagations
 * are interrupted in case the mass of a part becomes negative
 * VERSION::FA:86:22/10/2013:New mass management system
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::FA:373:12/01/2015: proper handling of mass event detection
 * VERSION::FA:381:14/01/2015:Propagator tolerances and default mass and attitude issues
 * VERSION::DM:393:16/03/2015:Constant Attitude Laws
 * VERSION::FA:673:12/09/2016: add getTotalMass(state)
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::DM:1173:24/08/2017:add propulsive and engine properties
 * VERSION::DM:976:16/11/2017:Merge continuous maneuvers classes
 * VERSION::FA:1449:15/03/2018:part can have either a Tank or a Mass property, not both
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPart;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.events.EventDetector.Action;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.detectors.NodeDetector;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.maneuvers.ImpulseManeuver;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.analysis.IDependentVariable;
import fr.cnes.sirius.patrius.math.analysis.IDependentVectorVariable;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.ode.FirstOrderIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.AdaptiveStepsizeIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.MassProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * @description
 *              <p>
 *              Test class for the simple mass model
 *              </p>
 * 
 * @see MassModel
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class MassModelTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Mass model
         * 
         * @featureDescription Model that simply adds the masses of each part of the assembly
         * 
         * @coveredRequirements DV-VEHICULE_130, DV-VEHICULE_140
         */
        MASS_MODEL
    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * 2nd part's name
     */
    private final String part2 = "part2";
    /**
     * 3rd part's name
     */
    private final String part3 = "part3";

    /**
     * 4th part's name
     */
    private final String part4 = "part4";

    /**
     * 5th part's name
     */
    private final String part5 = "part5";

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /** The first constant thrust maneuver. */
    private ContinuousThrustManeuver constant1;

    /** The second constant thrust maneuver. */
    private ContinuousThrustManeuver constant2;

    /** The first variable thrust maneuver. */
    private ContinuousThrustManeuver variable1;

    /** The second variable thrust maneuver. */
    private ContinuousThrustManeuver variable2;

    /** The second impulse maneuver. */
    private ImpulseManeuver impulse2;

    /** The third impulse maneuver. */
    private ImpulseManeuver impulse3;

    /** The fourth impulse maneuver. */
    private ImpulseManeuver impulse4;

    private final String t1 = "thruster1";
    private final String t2 = "thruster2";
    private final String t3 = "thruster3";
    private final String t4 = "thruster4";
    private final String t6 = "thruster6";
    private final String t7 = "thruster7";
    private final String t8 = "thruster8";

    private TankProperty p1;
    private TankProperty p2;
    private TankProperty p3;
    private TankProperty p4;
    private TankProperty p6;
    private TankProperty p7;
    private TankProperty p8;

    private MassProvider model;
    private double computedMass;
    private InertiaSimpleProperty i1;
    private InertiaSimpleProperty i2;
    private InertiaSimpleProperty i3;
    private InertiaSimpleProperty i4;
    private InertiaSimpleProperty i6;
    private InertiaSimpleProperty i7;
    private InertiaSimpleProperty i8;

    /** Name for the main part. */
    private final String main = "main";
    /** Name for the thruster. */
    private final String thruster = "thruster";

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getTotalMass()}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass. Three propagations
     *              are tested against propellant depletion : impulse maneuver as both an EventDetector
     *              (keplerian propagation) and an AdditionalStatesEventDetector (numerical propagation)
     *              and a constant thrust maneuver
     * 
     * @input an assembly with a main body of 10t and a tank of 1t.
     * 
     * @output an exception because the thrust maneuver requires more propellant.
     * 
     * @testPassCriteria an exception is raised
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.1.1
     * 
     * @nonRegressionVersion 2.1.1
     */
    @Test
    public void negativePartMassTest() throws PatriusException {

        /*
         * Test with an impulse maneuver as an event detector with a keplerian propagator
         */
        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        final double m0 = 10000.;

        builder.addMainPart(this.main);
        builder.addProperty(new MassProperty(m0), this.main);

        builder.addPart(this.thruster, this.main, Transform.IDENTITY);
        builder.addProperty(new MassProperty(1000.), this.thruster);

        // mass model
        final MassModel model = new MassModel(builder.returnAssembly());

        /*
         * Spacecraft state
         */
        // frame
        final Frame gcrf = FramesFactory.getGCRF();

        // dates
        final AbsoluteDate d0 = new AbsoluteDate();
        final AbsoluteDate d1 = new AbsoluteDate().shiftedBy(1000.);
        final AbsoluteDate d2 = new AbsoluteDate().shiftedBy(2000.);

        // detectors
        final DateDetector det1 = new DateDetector(d1);
        final DateDetector det2 = new DateDetector(d1);

        // orbit
        final double mu = Constants.GRIM5C1_EARTH_MU;
        final double a = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS + 400e3;
        final double e = .0001;
        final double i = 60 * 3.14 / 180;
        final double raan = 0;
        final double pa = 270 * 3.14 / 180;
        final double w = 0;

        // state
        final KeplerianOrbit orbit = new KeplerianOrbit(a, e, i, pa, raan, w, PositionAngle.TRUE, gcrf, d0, mu);
        final SpacecraftState spc = new SpacecraftState(orbit);

        // maneuvers
        final double dv = 1000;
        final double isp = 300;

        // for keplerian propagator
        final ImpulseManeuver imp1 = new ImpulseManeuver(det1, new Vector3D(dv, 0, 0), isp, model, this.thruster);

        // for numerical propatgator
        final ImpulseManeuver imp2 = new ImpulseManeuver(det2, new Vector3D(dv, 0, 0), isp, model, this.thruster);

        // for numerical propatgator
        final ContinuousThrustManeuver man1 =
            new ContinuousThrustManeuver(d1, 700, new PropulsiveProperty(5000, 300), Vector3D.PLUS_K, model,
                new TankProperty(1000.));

        // final double factor = MathLib.exp(-dv / (10 * isp));

        // required mass (m0 - m1) is > to mass of thruster
        // System.out.println(m0 - m1);

        /*
         * Propagate with three propagators
         */
        // keplerian propagator
        final KeplerianPropagator propK = new KeplerianPropagator(orbit);

        // numerical propagator
        final double[][] tol = NumericalPropagator.tolerances(1, orbit, OrbitType.CARTESIAN);
        final FirstOrderIntegrator dop = new DormandPrince853Integrator(.1, 60, tol[0], tol[1]);
        final NumericalPropagator propN1 = new NumericalPropagator(dop);

        final NumericalPropagator propN2 = new NumericalPropagator(dop);
        propN2.setInitialState(spc);
        propN2.setMassProviderEquation(model);
        propN2.addForceModel(man1);

        // configure keplerian
        propK.addEventDetector(imp1);

        // configure numerical 1
        propN1.setInitialState(spc);
        propN1.setMassProviderEquation(model);
        propN1.addEventDetector(imp2);

        // propagate keplerian
        try {
            propK.propagate(d2);
            Assert.fail();
        } catch (final PropagationException p) {
            // expected!
        }

        // propagate num1
        try {
            propN1.propagate(d2);
            Assert.fail();
        } catch (final PropagationException p) {
            // expected!
        }

        try {
            propN2.propagate(d2);
            Assert.fail();
        } catch (final PropagationException p) {
            // expected!
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getTotalMass()}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void massTest() throws PatriusException {

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // add main part
            builder.addMainPart(this.mainBody);

            // add other parts
            final Vector3D translation = new Vector3D(2.0, 1.0, 5.0);
            final Transform transform1 = new Transform(AbsoluteDate.J2000_EPOCH, translation);

            builder.addPart(this.part2, this.mainBody, transform1);
            builder.addPart(this.part3, this.mainBody, transform1);
            builder.addPart(this.part4, this.part3, transform1);
            builder.addPart(this.part5, this.part4, transform1);

            // mass properties
            final MassProperty mass1 = new MassProperty(10.0);
            final MassProperty mass2 = new MassProperty(5.0);
            final MassProperty mass4 = new MassProperty(20.0);
            final MassProperty mass5 = new MassProperty(2.0);
            builder.addProperty(mass1, this.mainBody);
            builder.addProperty(mass2, this.part2);
            builder.addProperty(mass4, this.part4);
            builder.addProperty(mass5, this.part5);

        } catch (final IllegalArgumentException e) {
            Assert.fail();
        }

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // model creation
        final MassModel model = new MassModel(assembly);

        // tests
        Assert.assertEquals(37.0, model.getTotalMass(), this.comparisonEpsilon);

        // part 2 modification
        final IPart partTwo = assembly.getPart(this.part2);
        final MassProperty partTwoMass = (MassProperty) partTwo.getProperty(PropertyType.MASS);
        partTwoMass.updateMass(8.0);

        // new mass test
        Assert.assertEquals(40.0, model.getTotalMass(), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#MassModel(Assembly)}
     * 
     * @description check that a part cannot have a tank property and a mass property at the same time
     * 
     * @inputAn assembly with part having both a tank and a mass property
     * 
     * @output exception
     * 
     * @testPassCriteria an exception is thrown as expected
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void massTankTest() throws PatriusException {

        // building the assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        // main part test
        try {
            // Add main part
            builder.addMainPart("Body");

            // Mass and Tank properties
            final MassProperty mass = new MassProperty(10.0);
            final TankProperty tank = new TankProperty(10.0);
            builder.addProperty(mass, "Body");
            builder.addProperty(tank, "Body");

            new MassModel(builder.returnAssembly());

            Assert.fail();

        } catch (final IllegalArgumentException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getAdditionalEquation(String)}
     * @testedMethod {@link MassModel#getMass(String)}
     * @testedMethod {@link MassModel#getTotalMass()}
     * @testedMethod {@link MassModel#addMassDerivative(String, double)}
     * @testedMethod {@link MassModel#registerAdditionalEquationsIn(NumericalPropagator)}
     * @testedMethod {@link MassModel#updateMass(String, double)}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void advancedMassModelSameFuelTanksForcesAndEventsTestFromInertiaModel() throws PatriusException {

        // sets the variable thrust:
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2877005283883484090L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };

        // sets the variable ISP:
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -4535695995740031898L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };

        // sets the variable direction:
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 5508913499602083945L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_K;
            }
        };

        // The initial date:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 14, 0.0),
            TimeScalesFactory.getUTC());
        final AbsoluteDate date1 = date0.shiftedBy(500);

        // One tank only
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        final Matrix3D id = new Matrix3D(new double[][] { { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } });

        AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("thruster9");
        this.p1 = new TankProperty(9000.);
        builder.addProperty(this.p1, "thruster9");
        this.model = new MassModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                this.p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, this.p1);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p1);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p1);
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1500)), Vector3D.PLUS_K.scalarMultiply(.1),
                200, this.model, "thruster9");
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1515)), Vector3D.PLUS_K.scalarMultiply(.1),
                300, this.model, "thruster9");
        this.impulse4 = new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 2, 600, 1e-6),
            Vector3D.PLUS_K.scalarMultiply(.1), 250, this.model, "thruster9");

        final double old = this.model.getMass("thruster9");
        // orbit
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new CircularOrbit(7100000, .0, .0, MathLib.toRadians(98), .0, .0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), date0, mu);

        // propagator
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.addEventDetector(this.impulse2);
        propagator.addEventDetector(this.impulse3);
        propagator.addEventDetector(this.impulse4);

        propagator.setMassProviderEquation(this.model);

        SpacecraftState finalState = propagator.propagate(date0.shiftedBy(5000));
        final double newComputedMassSameTank = finalState.getMass("thruster9");

        Assert.assertTrue(old > this.model.getMass("thruster9"));

        // Different tanks
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        builder = new AssemblyBuilder();
        builder.addMainPart("thruster10");
        builder.addPart(this.t2, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t3, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t4, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t6, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t7, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t8, "thruster10", Transform.IDENTITY);
        final double tm = 9000. / 7;
        this.p1 = new TankProperty(tm);
        this.p2 = new TankProperty(tm);
        this.p3 = new TankProperty(tm);
        this.p4 = new TankProperty(tm);
        this.p6 = new TankProperty(tm);
        this.p7 = new TankProperty(tm);
        this.p8 = new TankProperty(tm);
        builder.addProperty(this.p1, "thruster10");
        builder.addProperty(this.p2, this.t2);
        builder.addProperty(this.p3, this.t3);
        builder.addProperty(this.p4, this.t4);
        builder.addProperty(this.p6, this.t6);
        builder.addProperty(this.p7, this.t7);
        builder.addProperty(this.p8, this.t8);
        this.i1 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p1.getMassProperty());
        this.i2 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p2.getMassProperty());
        this.i3 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p3.getMassProperty());
        this.i4 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p4.getMassProperty());
        this.i6 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p6.getMassProperty());
        this.i7 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p7.getMassProperty());
        this.i8 = new InertiaSimpleProperty(Vector3D.MINUS_I, id, this.p8.getMassProperty());
        builder.addProperty(this.i1, "thruster10");
        builder.addProperty(this.i2, this.t2);
        builder.addProperty(this.i3, this.t3);
        builder.addProperty(this.i4, this.t4);
        builder.addProperty(this.i6, this.t6);
        builder.addProperty(this.i7, this.t7);
        builder.addProperty(this.i8, this.t8);
        this.model = new InertiaComputedModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                this.p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, this.p2);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p3);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p4);
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1500)), Vector3D.PLUS_K.scalarMultiply(.1),
                200, this.model, this.t6);
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1515)), Vector3D.PLUS_K.scalarMultiply(.1),
                300, this.model, this.t7);
        this.impulse4 = new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 2, 600, 1e-6),
            Vector3D.PLUS_K.scalarMultiply(.1), 250, this.model, this.t8);

        // propagator
        integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.addEventDetector(this.impulse2);
        propagator.addEventDetector(this.impulse3);
        propagator.addEventDetector(this.impulse4);
        propagator.setMassProviderEquation(this.model);

        finalState = propagator.propagate(date0.shiftedBy(5000));
        final List<String> partsNames = this.model.getAllPartsNames();
        final int size = partsNames.size();
        double newComputedMassDiffTanks = 0.;
        for (int i = 0; i < size; i++) {
            final String name = partsNames.get(i);
            newComputedMassDiffTanks += finalState.getMass(name);
        }

        Assert.assertEquals(0, (newComputedMassDiffTanks - newComputedMassSameTank) / newComputedMassDiffTanks,
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getAdditionalEquation(String)}
     * @testedMethod {@link MassModel#getMass(String)}
     * @testedMethod {@link MassModel#getTotalMass()}
     * @testedMethod {@link MassModel#addMassDerivative(String, double)}
     * @testedMethod {@link MassModel#registerAdditionalEquationsIn(NumericalPropagator)}
     * @testedMethod {@link MassModel#updateMass(String, double)}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void advancedMassModelSameFuelTanksForcesAndEventsTest() throws PatriusException {

        // sets the variable thrust:
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 9176870343080739978L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };

        // sets the variable ISP:
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 3024663143233498649L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };

        // sets the variable direction:
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 2538680229584000947L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_K;
            }
        };

        // The initial date:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 14, 0.0),
            TimeScalesFactory.getUTC());
        final AbsoluteDate date1 = date0.shiftedBy(500);

        // One tank only
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("thruster9");
        this.p1 = new TankProperty(9000.);
        builder.addProperty(this.p1, "thruster9");
        this.model = new MassModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                this.p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, this.p1);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p1);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p1);
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1500)), Vector3D.PLUS_K.scalarMultiply(.1),
                200, this.model, "thruster9");
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1515)), Vector3D.PLUS_K.scalarMultiply(.1),
                300, this.model, "thruster9");
        this.impulse4 = new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 600, 1e-6, Action.STOP,
                Action.STOP), Vector3D.PLUS_K.scalarMultiply(.1), 250, this.model, "thruster9");

        final double old = this.model.getMass("thruster9");

        // orbit
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new CircularOrbit(7100000, .0, .0, MathLib.toRadians(98), .0, .0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), date0, mu);

        // propagator
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.addEventDetector(this.impulse2);
        propagator.addEventDetector(this.impulse3);
        propagator.addEventDetector(this.impulse4);
        propagator.setMassProviderEquation(this.model);

        SpacecraftState finalState = propagator.propagate(date0.shiftedBy(5000));
        final double newComputedMassSameTank = finalState.getMass("thruster9");

        Assert.assertTrue(old > this.model.getMass("thruster9"));

        // Different tanks
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        builder = new AssemblyBuilder();
        builder.addMainPart("thruster10");
        builder.addPart(this.t2, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t3, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t4, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t6, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t7, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t8, "thruster10", Transform.IDENTITY);
        final double tm = 9000. / 7;
        this.p1 = new TankProperty(tm);
        this.p2 = new TankProperty(tm);
        this.p3 = new TankProperty(tm);
        this.p4 = new TankProperty(tm);
        this.p6 = new TankProperty(tm);
        this.p7 = new TankProperty(tm);
        this.p8 = new TankProperty(tm);
        builder.addProperty(this.p1, "thruster10");
        builder.addProperty(this.p2, this.t2);
        builder.addProperty(this.p3, this.t3);
        builder.addProperty(this.p4, this.t4);
        builder.addProperty(this.p6, this.t6);
        builder.addProperty(this.p7, this.t7);
        builder.addProperty(this.p8, this.t8);
        this.model = new MassModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                this.p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, this.p2);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p3);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p4);
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1500)), Vector3D.PLUS_K.scalarMultiply(.1),
                200, this.model, this.t6);
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date1.shiftedBy(1515)), Vector3D.PLUS_K.scalarMultiply(.1),
                300, this.model, this.t7);
        this.impulse4 = new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 2, 600, 1e-6),
            Vector3D.PLUS_K.scalarMultiply(.1), 250, this.model, this.t8);

        // propagator
        integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.addEventDetector(this.impulse2);
        propagator.addEventDetector(this.impulse3);
        propagator.addEventDetector(this.impulse4);
        propagator.setMassProviderEquation(this.model);

        finalState = propagator.propagate(date0.shiftedBy(5000));
        final List<String> partsNames = this.model.getAllPartsNames();
        final int size = partsNames.size();
        double newComputedMassDiffTanks = 0.;
        for (int i = 0; i < size; i++) {
            final String name = partsNames.get(i);
            newComputedMassDiffTanks += finalState.getMass(name);
        }

        Assert.assertEquals(0, (newComputedMassDiffTanks - newComputedMassSameTank) / newComputedMassDiffTanks,
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getAdditionalEquation(String)}
     * @testedMethod {@link MassModel#getMass(String)}
     * @testedMethod {@link MassModel#getTotalMass()}
     * @testedMethod {@link MassModel#addMassDerivative(String, double)}
     * @testedMethod {@link MassModel#registerAdditionalEquationsIn(NumericalPropagator)}
     * @testedMethod {@link MassModel#updateMass(String, double)}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void advancedMassModelSameFuelTanksForcesOnlyTest() throws PatriusException {

        // sets the variable thrust:
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 8947995742512931938L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };

        // sets the variable ISP:
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 1555347564763188114L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };

        // sets the variable direction:
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = 7615690792819529471L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_K;
            }
        };

        // The initial date:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 14, 0.0),
            TimeScalesFactory.getUTC());
        final AbsoluteDate date1 = date0.shiftedBy(500);

        // One tank only
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("thruster9");
        final TankProperty tank = new TankProperty(9000.);
        builder.addProperty(tank, "thruster9");
        this.model = new MassModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                tank);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, tank);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, tank);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, tank);

        final double old = this.model.getMass("thruster9");

        // orbit
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new CircularOrbit(7100000, .0, .0, MathLib.toRadians(98), .0, .0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), date0, mu);

        // propagator
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.setMassProviderEquation(this.model);

        SpacecraftState finalState = propagator.propagate(date0.shiftedBy(5000));
        final double newComputedMassSameTank = finalState.getMass("thruster9");

        Assert.assertTrue(old > this.model.getMass("thruster9"));

        // Different tanks
        // %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        builder = new AssemblyBuilder();
        builder.addMainPart("thruster10");
        builder.addPart(this.t2, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t3, "thruster10", Transform.IDENTITY);
        builder.addPart(this.t4, "thruster10", Transform.IDENTITY);
        final double tm = 9000. / 4;
        final TankProperty p1 = new TankProperty(tm);
        final TankProperty p2 = new TankProperty(tm);
        final TankProperty p3 = new TankProperty(tm);
        final TankProperty p4 = new TankProperty(tm);
        builder.addProperty(p1, "thruster10");
        builder.addProperty(p2, this.t2);
        builder.addProperty(p3, this.t3);
        builder.addProperty(p4, this.t4);
        this.model = new MassModel(builder.returnAssembly());

        // force models using the same tank
        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model, p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(110), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, p2);
        this.variable1 =
            new ContinuousThrustManeuver(date1.shiftedBy(250), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, p3);
        this.variable2 =
            new ContinuousThrustManeuver(date1.shiftedBy(600), 200, new PropulsiveProperty(thrust, isp), direction,
                this.model, p4);

        // propagator
        integrator = new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        propagator = new NumericalPropagator(integrator);
        propagator.setInitialState(new SpacecraftState(orbit, this.model));

        propagator.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator.addForceModel(this.constant1);
        propagator.addForceModel(this.constant2);
        propagator.addForceModel(this.variable1);
        propagator.addForceModel(this.variable2);
        propagator.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator.setMassProviderEquation(this.model);

        finalState = propagator.propagate(date0.shiftedBy(5000));
        final List<String> partsNames = this.model.getAllPartsNames();
        final int size = partsNames.size();
        double newComputedMassDiffTanks = 0.;
        for (int i = 0; i < size; i++) {
            final String name = partsNames.get(i);
            newComputedMassDiffTanks += finalState.getMass(name);
        }

        Assert.assertEquals(0, (newComputedMassDiffTanks - newComputedMassSameTank) / newComputedMassDiffTanks,
            Precision.DOUBLE_COMPARISON_EPSILON);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getAdditionalEquation(String)}
     * @testedMethod {@link MassModel#getMass(String)}
     * @testedMethod {@link MassModel#getTotalMass()}
     * @testedMethod {@link MassModel#addMassDerivative(String, double)}
     * @testedMethod {@link MassModel#registerAdditionalEquationsIn(NumericalPropagator)}
     * @testedMethod {@link MassModel#updateMass(String, double)}
     * 
     * @description creation of an assembly containing several parts with mass properties,
     *              computation, update and getting of the global mass.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * @throws PatriusException
     *         if a frame problem occurs
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void advancedMassModelSeparateFuelTanksTest() throws PatriusException {

        // The initial date:
        final AbsoluteDate date0 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 14, 0.0),
            TimeScalesFactory.getUTC());
        // The orbit:
        final double mu = CelestialBodyFactory.getEarth().getGM();
        final Orbit orbit = new CircularOrbit(7100000, .0, .0, MathLib.toRadians(98), .0, .0, PositionAngle.MEAN,
            FramesFactory.getEME2000(), date0, mu);
        // Sets the integrator:
        final double[] absTolerance = { 0.001, 1.0e-9, 1.0e-9, 1.0e-6, 1.0e-6, 1.0e-6 };
        final double[] relTolerance = { 1.0e-7, 1.0e-4, 1.0e-4, 1.0e-7, 1.0e-7, 1.0e-7 };
        final AdaptiveStepsizeIntegrator integrator =
            new DormandPrince853Integrator(0.001, 1000, absTolerance, relTolerance);

        final SpacecraftState initialState1 = new SpacecraftState(orbit, this.model);
        final NumericalPropagator propagator1 = new NumericalPropagator(integrator, initialState1.getFrame());
        propagator1.setInitialState(initialState1);

        propagator1.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator1.addForceModel(this.constant1);
        propagator1.addForceModel(this.constant2);
        propagator1.addForceModel(this.variable1);
        propagator1.addForceModel(this.variable2);
        propagator1.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator1.addEventDetector(this.impulse2);
        propagator1.addEventDetector(this.impulse3);
        propagator1.addEventDetector(this.impulse4);
        propagator1.setMassProviderEquation(this.model);

        final double o1M = this.p1.getMass();
        final double o2M = this.p2.getMass();
        final double o3M = this.p3.getMass();
        final double o4M = this.p4.getMass();
        final double o6M = this.p6.getMass();
        final double o7M = this.p7.getMass();
        final double o8M = this.p8.getMass();

        final SpacecraftState finalState1 = propagator1.propagate(date0.shiftedBy(1000000));
        final List<String> partsNames = this.model.getAllPartsNames();
        final int size = partsNames.size();
        for (int i = 0; i < size; i++) {
            final String name = partsNames.get(i);
            this.computedMass += finalState1.getMass(name);
        }

        final double v1M = o1M - this.p1.getMass();
        final double v2M = o2M - this.p2.getMass();
        final double v3M = o3M - this.p3.getMass();
        final double v4M = o4M - this.p4.getMass();
        final double v6M = o6M - this.p6.getMass();
        final double v7M = o7M - this.p7.getMass();
        final double v8M = o8M - this.p8.getMass();

        final SpacecraftState initialState2 = new SpacecraftState(orbit, this.model);
        final NumericalPropagator propagator2 = new NumericalPropagator(integrator, initialState2.getFrame());
        propagator2.setInitialState(initialState2);

        propagator2.setAttitudeProvider(new ConstantAttitudeLaw(FramesFactory.getEME2000(), Rotation.IDENTITY));
        propagator2.addForceModel(this.constant1);
        propagator2.addForceModel(this.constant2);
        propagator2.addForceModel(this.variable1);
        propagator2.addForceModel(this.variable2);
        propagator2.addForceModel(new DirectBodyAttraction(new NewtonianGravityModel(orbit.getMu())));
        propagator2.addEventDetector(this.impulse2);
        propagator2.addEventDetector(this.impulse3);
        propagator2.addEventDetector(this.impulse4);
        propagator2.setMassProviderEquation(this.model);

        final double no1M = this.p1.getMass();
        final double no2M = this.p2.getMass();
        final double no3M = this.p3.getMass();
        final double no4M = this.p4.getMass();
        final double no6M = this.p6.getMass();
        final double no7M = this.p7.getMass();
        final double no8M = this.p8.getMass();

        final SpacecraftState finalState2 = propagator2.propagate(date0.shiftedBy(1000000));

        final double nv1M = no1M - this.p1.getMass();
        final double nv2M = no2M - this.p2.getMass();
        final double nv3M = no3M - this.p3.getMass();
        final double nv4M = no4M - this.p4.getMass();
        final double nv6M = no6M - this.p6.getMass();
        final double nv7M = no7M - this.p7.getMass();
        final double nv8M = no8M - this.p8.getMass();

        Assert.assertEquals(0, (nv1M - v1M) / nv1M, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (nv2M - v2M) / nv2M, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (nv3M - v3M) / nv3M, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (nv4M - v4M) / nv4M, Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(nv6M - v6M < -Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(nv7M - v7M < -Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertTrue(nv8M - v8M < -Precision.DOUBLE_COMPARISON_EPSILON);

        double finalMass = 0.;
        for (int i = 0; i < size; i++) {
            final String name = partsNames.get(i);
            finalMass += finalState2.getMass(name);
        }

        Assert.assertTrue(this.computedMass > finalMass);
    }

    /**
     * Set up method before running the test.
     * 
     * @throws PatriusException
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        // sets the variable thrust:
        final IDependentVariable<SpacecraftState> thrust = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6079793023822896377L;

            @Override
            public double value(final SpacecraftState x) {
                return 20.0;
            }
        };

        // sets the variable ISP:
        final IDependentVariable<SpacecraftState> isp = new IDependentVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -6957311074644356457L;

            @Override
            public double value(final SpacecraftState x) {
                return 275.0;
            }
        };

        // sets the variable direction:
        final IDependentVectorVariable<SpacecraftState> direction = new IDependentVectorVariable<SpacecraftState>(){
            /** Serializable UID. */
            private static final long serialVersionUID = -1705878013382548674L;

            @Override
            public Vector3D value(final SpacecraftState x) {
                return Vector3D.PLUS_K;
            }
        };

        // sets the maneuvers with constant thrust:
        final AbsoluteDate date1 = new AbsoluteDate(new DateComponents(2004, 01, 02), new TimeComponents(02, 15, 30.0),
            TimeScalesFactory.getUTC());

        final AbsoluteDate date2 = date1.shiftedBy(260);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart(this.t1);
        builder.addPart(this.t2, this.t1, Transform.IDENTITY);
        builder.addPart(this.t3, this.t1, Transform.IDENTITY);
        builder.addPart(this.t4, this.t1, Transform.IDENTITY);
        builder.addPart(this.t6, this.t1, Transform.IDENTITY);
        builder.addPart(this.t7, this.t1, Transform.IDENTITY);
        builder.addPart(this.t8, this.t1, Transform.IDENTITY);

        this.p1 = new TankProperty(1500.);
        this.p2 = new TankProperty(1400.);
        this.p3 = new TankProperty(1300.);
        this.p4 = new TankProperty(1200.);
        this.p6 = new TankProperty(1000.);
        this.p7 = new TankProperty(0900.);
        this.p8 = new TankProperty(0800.);

        builder.addProperty(this.p1, this.t1);
        builder.addProperty(this.p2, this.t2);
        builder.addProperty(this.p3, this.t3);
        builder.addProperty(this.p4, this.t4);
        builder.addProperty(this.p6, this.t6);
        builder.addProperty(this.p7, this.t7);
        builder.addProperty(this.p8, this.t8);

        this.model = new MassModel(builder.returnAssembly());

        this.constant1 =
            new ContinuousThrustManeuver(date1, 100, new PropulsiveProperty(600, 289), Vector3D.PLUS_K, this.model,
                this.p1);
        this.constant2 =
            new ContinuousThrustManeuver(date1.shiftedBy(105), 100, new PropulsiveProperty(460, 279), Vector3D.PLUS_K,
                this.model, this.p2);
        this.variable1 =
            new ContinuousThrustManeuver(date2, 500, new PropulsiveProperty(thrust, isp), direction, this.model,
                this.p3);
        this.variable2 =
            new ContinuousThrustManeuver(date2.shiftedBy(600), 500, new PropulsiveProperty(thrust, isp), direction,
                this.model, this.p4);
        this.impulse2 =
            new ImpulseManeuver(new DateDetector(date2.shiftedBy(1500)), Vector3D.PLUS_K.scalarMultiply(.1),
                200, this.model, this.t6);
        this.impulse3 =
            new ImpulseManeuver(new DateDetector(date2.shiftedBy(1515)), Vector3D.PLUS_K.scalarMultiply(.1),
                300, this.model, this.t7);
        this.impulse4 = new ImpulseManeuver(new NodeDetector(FramesFactory.getEME2000(), 2, 600, 1e-6),
            Vector3D.PLUS_K.scalarMultiply(.1), 250, this.model, this.t8);
    }

    /**
     * Coverage test for checkProperty()
     * 
     * @throws PatriusException
     *         bcs of MassProperty
     */
    @Test(expected = IllegalArgumentException.class)
    public void wrongPartNameCheckProperty() throws PatriusException {

        Utils.setDataRoot("regular-data");

        // Assembly
        final AssemblyBuilder builder = new AssemblyBuilder();

        final double mainMass = 10000.;
        final double thrusterMass = 1000;

        builder.addMainPart(this.main);
        builder.addProperty(new MassProperty(mainMass), this.main);

        builder.addPart(this.thruster, this.main, Transform.IDENTITY);
        builder.addProperty(new MassProperty(thrusterMass), this.thruster);

        // mass model
        final MassModel massModel = new MassModel(builder.returnAssembly());
        // exception should occur
        massModel.getMass("This is not a part name");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MASS_MODEL}
     * 
     * @testedMethod {@link MassModel#getTotalMass(SpacecraftState)}
     * 
     * @description check the mass is the expected one.
     * 
     * @inputAn assembly
     * 
     * @output the global mass
     * 
     * @testPassCriteria the mass is the expected one
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public void testGetTotalMassSpacecraftState() throws PatriusException {

        // Initialization
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);

        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main2");
        builder.addPart("Part1", "Main2", Transform.IDENTITY);
        builder.addPart("Part2", "Main2", Transform.IDENTITY);
        builder.addProperty(new MassProperty(1500.), "Part1");
        builder.addProperty(new MassProperty(1400.), "Part2");
        final MassProvider massModel1 = new MassModel(builder.returnAssembly());
        builder.addMainPart("Main3");
        builder.addPart("Part1", "Main3", Transform.IDENTITY);
        builder.addPart("Part2", "Main3", Transform.IDENTITY);
        builder.addProperty(new MassProperty(1500.), "Part1");
        builder.addProperty(new MassProperty(1200.), "Part2");
        final MassProvider massModel2 = new MassModel(builder.returnAssembly());

        // Check without mass provider
        Assert.assertEquals(2900., massModel1.getTotalMass(new SpacecraftState(orbit)), 0.);

        // Check with mass provider
        Assert.assertEquals(2700., massModel1.getTotalMass(new SpacecraftState(orbit, massModel2)), 0.);
    }
}
