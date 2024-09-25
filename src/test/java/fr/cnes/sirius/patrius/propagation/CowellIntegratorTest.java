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
 * HISTORY
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * VERSION:4.11:DM:DM-3306:22/05/2023:[PATRIUS] Rayon du soleil dans le calcul de la PRS
 * VERSION:4.11:DM:DM-3256:22/05/2023:[PATRIUS] Suite 3246
 * VERSION:4.11:DM:DM-38:22/05/2023:[PATRIUS] Suppression de setters pour le MultiNumericalPropagator
 * VERSION:4.11:DM:DM-40:22/05/2023:[PATRIUS] Gestion derivees par rapport au coefficient k dans les GravityModel
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration de la gestion des attractions gravitationnelles dans le propagateur
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.10:DM:DM-3244:03/11/2022:[PATRIUS] Ajout propagation du signal dans ExtremaElevationDetector
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2899:15/11/2021:[PATRIUS] Autres corps occultants que la Terre pour la SRP 
 * VERSION:4.6:DM:DM-2571:27/01/2021:[PATRIUS] Integrateur Stormer-Cowell 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.PropertyType;
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.DirectRadiativeModel;
import fr.cnes.sirius.patrius.assembly.models.MassModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.MeeusMoon;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.EventDetector;
import fr.cnes.sirius.patrius.events.detectors.ApsideDetector;
import fr.cnes.sirius.patrius.events.detectors.DateDetector;
import fr.cnes.sirius.patrius.events.maneuverandapsidedetection.ImpulseManeuver;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.forces.gravity.AbstractHarmonicGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.BalminoGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.DirectBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.GravityModel;
import fr.cnes.sirius.patrius.forces.gravity.NewtonianGravityModel;
import fr.cnes.sirius.patrius.forces.gravity.ThirdBodyAttraction;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.forces.maneuvers.ContinuousThrustManeuver;
import fr.cnes.sirius.patrius.forces.radiation.SolarRadiationPressure;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.exception.DimensionMismatchException;
import fr.cnes.sirius.patrius.math.exception.MaxCountExceededException;
import fr.cnes.sirius.patrius.math.exception.NoBracketingException;
import fr.cnes.sirius.patrius.math.exception.NumberIsTooSmallException;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.linear.Array2DRowRealMatrix;
import fr.cnes.sirius.patrius.math.ode.FirstOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.SecondOrderDifferentialEquations;
import fr.cnes.sirius.patrius.math.ode.nonstiff.DormandPrince853Integrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.CowellIntegrator;
import fr.cnes.sirius.patrius.math.ode.nonstiff.cowell.SecondOrderStateMapper;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.ApsisOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.numerical.NumericalPropagator;
import fr.cnes.sirius.patrius.propagation.numerical.PartialDerivativesEquations;
import fr.cnes.sirius.patrius.propagation.numerical.multi.MultiNumericalPropagator;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusFixedStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepHandler;
import fr.cnes.sirius.patrius.propagation.sampling.PatriusStepInterpolator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

/**
 * Test class for {@link CowellIntegrator}.
 */
public class CowellIntegratorTest {

    /** Display results flag. */
    private static final boolean DISPLAY_RES = false;

    private double maxPosError;

    private boolean isReference;

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(CowellIntegratorTest.class.getSimpleName(), "Cowell integrator");
        // Tolerances are between 0.1mm and 10mm
        Report.printMethodHeader("All Cowell validation tests", "Test all Cowell integrator features", "DOPRI 8(5,3)",
            0.01, ComparisonType.ABSOLUTE);
    }

    @Before
    public void setUp() {
        Utils.clear();
    }

    /**
     * @testType VT
     *
     * @description test Cowell pure LEO keplerian propagation. Order = 17, tol = 1E-12 (test high order).
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.0003m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPropagationKeplerian() throws PatriusException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(new DirectBodyAttraction(new NewtonianGravityModel(
            initialOrbit.getMu())));

        // Propagation and ephemeris check
        final double tolPos = 0.0003;
        propagate("LEO - Keplerian", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly,
            massModel, 17, 1E-12, true, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell LEO propagation (full force model). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.0005m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPropagationLEO() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        forceModels.add(dragMSIS2000(assembly));
        final GravityModel moonGravityModel = new MeeusMoon().getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moonGravityModel);
        final GravityModel sunGravityModel = new MeeusSun().getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ForceModel sunAttraction = new ThirdBodyAttraction(sunGravityModel);
        forceModels.add(moonAttraction);
        forceModels.add(sunAttraction);
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.00054;
        propagate("LEO", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9,
            1E-10, false, true, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell GTO propagation (full force model). Order = 9, tol = 1E-12.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.01m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPropagationGTO() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(24000E3, 0.7, 0, 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        forceModels.add(dragMSIS2000(assembly));
        final GravityModel moonGravityModel = new MeeusMoon().getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moonGravityModel);
        final GravityModel sunGravityModel = new MeeusSun().getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ForceModel sunAttraction = new ThirdBodyAttraction(sunGravityModel);
        forceModels.add(moonAttraction);
        forceModels.add(sunAttraction);
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.01;
        propagate("GTO", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9,
            1E-12, false, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell HEO propagation (full force model). Order = 9, tol = 1E-11.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.01m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPropagationHEO() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new ApsisOrbit(6700E3, 40000E3, MathLib.toRadians(15.), 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        forceModels.add(dragMSIS2000(assembly));
        final GravityModel moonGravityModel = new MeeusMoon().getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ForceModel moonAttraction = new ThirdBodyAttraction(moonGravityModel);
        final GravityModel sunGravityModel = new MeeusSun().getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ForceModel sunAttraction = new ThirdBodyAttraction(sunGravityModel);
        forceModels.add(moonAttraction);
        forceModels.add(sunAttraction);
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.011;
        propagate("HEO", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9,
            1E-11, false, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell retro-propagation (gravity model only). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.002m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testRetroPropagation() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(-86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.002;
        propagate("Retropropagation", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly,
            massModel, 9, 1E-10, false, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell master mode on 1-day propagation (gravity model only). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.001m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testMasterMode() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.001;
        propagate("Master mode", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly,
            massModel, 9, 1E-10, false, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell fixed master mode on 1-day propagation (gravity model only). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria interpolated ephemeris (every 10s) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.001m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testFixedMasterMode() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.001;
        propagate("Master mode (fixed)", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(),
            assembly, massModel, 9, 1E-10, true, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell ephemeris mode on 1-day propagation (gravity model only). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria interpolated ephemeris (every 10s) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.001m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testEphemerisMode() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation and ephemeris check
        final double tolPos = 0.001;
        propagate("Ephemeris mode", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly,
            massModel, 9, 1E-10, false, true, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell event detection. Order = 9, tol = 1E-10.
     *
     * @testPassCriteria check that events are properly detected (reference events obtained with DOPRI 853, threshold:
     *                   1E-5s, consistent with propagation accuracy)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testEventDetection() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // List of detectors: apside detector
        final List<AbsoluteDate> refEventDates = new ArrayList<>();
        final List<AbsoluteDate> actEventDates = new ArrayList<>();
        final List<EventDetector> detectors = new ArrayList<>();
        this.isReference = true;
        detectors.add(new ApsideDetector(initialOrbit, 0){
            /** Serializable UID. */
            private static final long serialVersionUID = 990040055053801096L;

            @Override
            public void init(final SpacecraftState s0, final AbsoluteDate t) {
                CowellIntegratorTest.this.isReference = !CowellIntegratorTest.this.isReference;
            }

            @Override
            public Action eventOccurred(final SpacecraftState s, final boolean increasing,
                                        final boolean forward) throws PatriusException {
                if (CowellIntegratorTest.this.isReference) {
                    refEventDates.add(s.getDate());
                } else {
                    actEventDates.add(s.getDate());
                }
                return Action.CONTINUE;
            }

        });

        // Propagation
        propagate("Event detection", initialOrbit, finalDate, forceModels, detectors, assembly, massModel, 9, 1E-10,
            false, false, true, false, false, Double.POSITIVE_INFINITY);

        // Check event dates
        Assert.assertEquals(refEventDates.size(), actEventDates.size(), 0);
        for (int i = 0; i < refEventDates.size(); i++) {
            Assert.assertEquals(0., refEventDates.get(i).durationFrom(actEventDates.get(i)), 5E-5);
        }
    }

    /**
     * @testType VT
     *
     * @description test Cowell impulse maneuvers. Order = 9, tol = 1E-10.
     *
     * @testPassCriteria interpolated ephemeris (every 10s) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.015)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testImpulseManeuver() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);

        final AbsoluteDate finalDate = initialDate.shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // List of detectors: impulse maneuver
        final List<EventDetector> detectors = new ArrayList<>();
        detectors.add(new ImpulseManeuver(new DateDetector(initialDate.shiftedBy(10000)), new Vector3D(100, 0, 0), 200,
            "Tank"));

        // Propagation
        final double tolPos = 0.015;
        propagate("Impulse maneuver", initialOrbit, finalDate, forceModels, detectors, assembly, massModel, 9, 1E-10,
            true, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell continuous maneuvers. Order = 9, tol = 1E-11.
     *
     * @testPassCriteria interpolated ephemeris (every 10s) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.025m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testContinuousManeuver() throws PatriusException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);

        final AbsoluteDate finalDate = initialDate.shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models (gravity only)
        final List<ForceModel> forceModels = new ArrayList<>();
        final PropulsiveProperty engine = (PropulsiveProperty) assembly.getMainPart().getProperty(
            PropertyType.PROPULSIVE);
        final TankProperty tank = (TankProperty) assembly.getPart("Tank").getProperty(PropertyType.TANK);
        forceModels.add(new ContinuousThrustManeuver(initialDate.shiftedBy(10000), 1000, engine, Vector3D.PLUS_I,
            massModel, tank, FramesFactory.getGCRF()));
        forceModels.add(new DirectBodyAttraction(new NewtonianGravityModel(
            initialOrbit.getMu())));

        // List of detectors: impulse maneuver
        final List<EventDetector> detectors = new ArrayList<>();

        // Propagation
        final double tolPos = 0.025;
        propagate("Continuous maneuver", initialOrbit, finalDate, forceModels, detectors, assembly, massModel, 9,
            1E-11, true, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell propagation with PRS only (reset derivatives). Order = 9, tol = 1E-11.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.02m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPropagationPRS() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(true));

        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(new SolarRadiationPressure(Constants.SEIDELMANN_UA, Constants.CONST_SOL_N_M2, new MeeusSun(),
                6.95E8, new OneAxisEllipsoid(
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(), ""),
            new DirectRadiativeModel(assembly)));
        forceModels.add(new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu())));

        // Propagation and ephemeris check
        final double tolPos = 0.05;
        propagate("PRS", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9,
            1E-11, false, false, true, false, false, tolPos);
    }

    /**
     * @testType VT
     *
     * @description test Cowell propagation on small interval: 0.1s (no exact interval) and 1E-9s (lower than Cowell
     *              first initial timestep). Order = 9, tol = 1E-10.
     *
     * @testPassCriteria final date is as expected. Propagated ephemeris is expected (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testSmallTimeInterval() throws PatriusException {

        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models
        final List<ForceModel> forceModels = new ArrayList<>();

        // Propagation on duration = 0.1s
        final double duration1 = 0.1;
        final BoundedPropagator ephemeris1 = propagate("Small interval (0.1s)", initialOrbit, initialOrbit.getDate()
            .shiftedBy(duration1), forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9, 1E-10, false,
            true, true, false, false, 0.);
        // Check final date
        Assert.assertEquals(ephemeris1.getMinDate().getOffset() + duration1, ephemeris1.getMaxDate().getOffset());

        // Propagation on duration = 1E-9s
        final double duration2 = 1E-9;
        final BoundedPropagator ephemeris2 = propagate("Small interval (1E-9s)", initialOrbit, initialOrbit.getDate()
            .shiftedBy(duration2), forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9, 1E-10, false,
            true, true, false, false, 0.);
        // Check final date
        Assert.assertEquals(ephemeris2.getMinDate().getOffset() + duration2, ephemeris2.getMaxDate().getOffset());
    }

    /**
     * @testType VT
     *
     * @description test Cowell propagation on several successive integration with same integrator. Order = 9, tol =
     *              1E-10.
     *
     * @testPassCriteria ephemeris (every time step) is same as reference ephemeris (reference: DOPRI 853 with
     *                   small time step, absolute position threshold: 0.001m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testMultipleIntegration() throws PatriusException, IOException, ParseException {
        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(3600.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(balminoAttraction());
        forceModels.add(dragMSIS2000(assembly));
        final GravityModel moonGravityModel = new MeeusMoon().getGravityModel();
        ((AbstractHarmonicGravityModel) moonGravityModel).setCentralTermContribution(false);
        final ThirdBodyAttraction moonForceModel = new ThirdBodyAttraction(moonGravityModel);
        forceModels.add(moonForceModel);
        final GravityModel sunGravityModel = new MeeusSun().getGravityModel();
        ((AbstractHarmonicGravityModel) sunGravityModel).setCentralTermContribution(false);
        final ThirdBodyAttraction sunForceModel = new ThirdBodyAttraction(sunGravityModel);
        forceModels.add(sunForceModel);
        final ForceModel newtonianAttraction = new DirectBodyAttraction(new NewtonianGravityModel(initialOrbit.getMu()));
        forceModels.add(newtonianAttraction);

        // Propagation every 1h and ephemeris check
        final double tolPos = 0.001;
        for (int i = 0; i < 24; i++) {
            final boolean display = i == 23 ? true : false;
            final BoundedPropagator ephemeris = propagate("Multiple integration", initialOrbit, finalDate, forceModels,
                new ArrayList<EventDetector>(), assembly, massModel, 9, 1E-10, false, true, display, false, false,
                tolPos);
            initialOrbit = ephemeris.propagate(finalDate).getOrbit();
            finalDate = finalDate.shiftedBy(3600.);
        }
    }

    /**
     * @testType UT
     *
     * @description test Cowell functional features (exceptions).
     *
     * @testPassCriteria exception are thrown as expected
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testFunctional() throws PatriusException {

        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final SpacecraftState initialState = new SpacecraftState(initialOrbit);

        // Cowell propagation
        final CowellIntegrator integrator = new CowellIntegrator(9, 1E-10, 1E-10);
        final NumericalPropagator propagator = new NumericalPropagator(integrator, initialState.getFrame(),
            OrbitType.KEPLERIAN, PositionAngle.TRUE);
        propagator.setInitialState(initialState);

        // Wrong orbit type
        try {
            propagator.propagate(finalDate);
            Assert.fail();
        } catch (final PropagationException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Wrong order
        try {
            new CowellIntegrator(21, 1E-10, 1E-10);
            Assert.fail();
        } catch (final PatriusRuntimeException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Wrong orbit type
        try {
            final MultiNumericalPropagator propagator2 = new MultiNumericalPropagator(integrator,
                new HashMap<String, Frame>(), OrbitType.KEPLERIAN, PositionAngle.TRUE);
            propagator2.addInitialState(initialState, "ID1");
            propagator2.propagate(finalDate);
            Assert.fail();
        } catch (final PropagationException e) {
            // Expected
            Assert.assertTrue(true);
        }

        // Wrong equations type
        try {
            integrator.integrate(new FirstOrderDifferentialEquations(){

                @Override
                public int getDimension() {
                    return 6;
                }

                @Override
                public void computeDerivatives(final double t,
                                               final double[] y,
                                               final double[] yDot) {
                    // nothing to do
                }
            }, 0, new double[6], 0, new double[6]);
            Assert.fail();
        } catch (final PatriusRuntimeException e) {
            // Expected
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType VT
     *
     * @description test Cowell externalization: a propagation is performed and ephemeris is stored in memory. This
     *              ephemeris is then read and is compared to original ephemeris.
     *              PV, mass and partial derivatives are compared
     *
     * @testPassCriteria read ephemeris (every time step) is same as reference ephemeris (reference: original ephemeris
     *                   before writing, threshold: 0.)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testExternalization() throws IOException, ClassNotFoundException, DimensionMismatchException,
        NumberIsTooSmallException, MaxCountExceededException, NoBracketingException, PatriusException, ParseException {

        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(dragMSIS2000PD(assembly));
        forceModels.add(balminoAttractionDP());

        // Propagation
        final BoundedPropagator ephemeris = propagate(
            String.format(Locale.US, "LEO (SSO) - Full      |     %d |   %1.2e", 9, 1E-10), initialOrbit, finalDate,
            forceModels, new ArrayList<EventDetector>(), assembly, massModel, 9, 1E-12, false, true, false, false,
            true, Double.POSITIVE_INFINITY);

        // Store ephemeris in memory
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(ephemeris);

        // Read ephemeris
        final ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final BoundedPropagator output = (BoundedPropagator) ois.readObject();

        // Check read ephemeris vs initial ephemeris (should be the same)
        AbsoluteDate currentDate = initialOrbit.getDate();
        while (currentDate.compareTo(finalDate) <= 0) {
            final SpacecraftState refState = ephemeris.propagate(currentDate);
            final SpacecraftState actState = output.propagate(currentDate);
            final Vector3D ref = refState.getPVCoordinates().getPosition();
            final Vector3D act = actState.getPVCoordinates().getPosition();
            Assert.assertEquals(refState.getMass("Tank"), actState.getMass("Tank"), 0.);
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 8; j++) {
                    check(refState.getAdditionalState("Partial")[i + j * 6], actState.getAdditionalState("Partial")[i
                            + j * 6], 0);
                }
            }
            Assert.assertEquals(0., ref.distance(act), 0.);
            currentDate = currentDate.shiftedBy(10.);
        }
    }

    /**
     * @testType VT
     *
     * @description test Cowell simple integration on non-orbit equations: here the harmonic system y" = -y is
     *              integrated over 100s.
     *
     * @testPassCriteria final integrated result is as expected (reference: math, absolute threshold: 1E-8)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testSimpleIntegration() {
        final CowellIntegrator integrator = new CowellIntegrator(8, 1E-10, 1E-10);
        // Equation y" = -y
        final Equations equations = new Equations();
        // Simple mapper
        final SecondOrderStateMapper mapper = new SecondOrderStateMapper(){
            @Override
            public double[] extractYDot(final double[] fullState) {
                return new double[] { fullState[1] };
            }

            @Override
            public double[] extractY(final double[] fullState) {
                return new double[] { fullState[0] };
            }

            @Override
            public double[] buildFullState(final double[] y,
                                           final double[] yDot) {
                return new double[] { y[0], yDot[0] };
            }

            @Override
            public void writeExternal(final ObjectOutput out) throws IOException {
                // Unused
            }

            @Override
            public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
                // Unused
            }
        };
        integrator.setMapper(mapper);

        // Perform integration on 100s and check y and y' with math result
        final double[] y0 = { 0, 1 };
        final double[] y = new double[2];
        integrator.integrate(equations, 0, y0, 100, y);
        Assert.assertEquals(y[0], MathLib.sin(100), 1E-8);
        Assert.assertEquals(y[1], MathLib.cos(100), 1E-8);
    }

    /**
     * @testType VT
     *
     * @description test Cowell partial derivatives propagation (full force model) including state transition matrix and
     *              sensitivity matrix. Order = 9, tol = 1E-10.
     *
     * @testPassCriteria final partial derivatives matrix is same as reference partial derivatives matrix (reference:
     *                   DOPRI 853 with
     *                   small time step, relative threshold on partial derivatives: 1E-5, absolute position threshold:
     *                   0.001m)
     *
     * @referenceVersion 4.6
     *
     * @nonRegressionVersion 4.6
     */
    @Test
    public void testPartialDerivatives() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(true));

        // Initial orbit
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final Orbit initialOrbit = new KeplerianOrbit(7000000, 0, FastMath.PI / 2., 0, 0, 0, PositionAngle.TRUE,
            FramesFactory.getGCRF(), initialDate, Constants.WGS84_EARTH_MU);
        final AbsoluteDate finalDate = initialOrbit.getDate().shiftedBy(86400.);
        final Assembly assembly = buildAssembly();
        final MassProvider massModel = new MassModel(assembly);

        // List of force models for full model
        final List<ForceModel> forceModels = new ArrayList<>();
        forceModels.add(dragMSIS2000PD(assembly));
        forceModels.add(balminoAttractionDP());

        // Propagation and ephemeris check
        final double tolPos = 0.013;
        propagate("Partial derivatives", initialOrbit, finalDate, forceModels, new ArrayList<EventDetector>(),
            assembly, massModel, 9, 1E-10, false, false, true, false, true, tolPos);
    }

    /**
     * Propagation.
     *
     * @param name name
     * @param initialOrbit initial orbit
     * @param finalDate final propagation date
     * @param forceModels list of force models
     * @param detectors list of detectors
     * @param assembly assembly
     * @param massModel mass model
     * @param order Cowell integrator order
     * @param tol Cowell integrator tolerance
     * @param isFixedMasterMode true if fixed master mode should be used
     * @param isEphemerisMode true if ephemeris master mode should be used
     * @param isTest true if output should be checked
     * @param isMC true if test is a Monte-Carlo test
     * @param isPartialDerivatives true if partial derivatives are propagated
     * @param tolPos tolerance on position
     * @return propagated ephemeris
     */
    private BoundedPropagator propagate(final String name,
                                        final Orbit initialOrbit,
                                        final AbsoluteDate finalDate,
                                        final List<ForceModel> forceModels,
                                        final List<EventDetector> detectors,
                                        final Assembly assembly,
                                        final MassProvider massModel,
                                        final int order,
                                        final double tol,
                                        final boolean isFixedMasterMode,
                                        final boolean isEphemerisMode,
                                        final boolean isTest,
                                        final boolean isMC,
                                        final boolean isPartialDerivatives,
                                        final double tolPos) throws PatriusException {
        // Initialization - 1 day propagation
        final AttitudeProvider attitudeProvider = new BodyCenterPointing();
        final Attitude attitude = attitudeProvider.getAttitude(initialOrbit);
        SpacecraftState initialState = new SpacecraftState(initialOrbit, attitude, massModel);

        // Reference propagation with Dormand-Prince (tolerances for errors < 0.1mm)
        final double[][] tolerances = NumericalPropagator.tolerances(1E-5, initialOrbit, OrbitType.EQUINOCTIAL);
        NumericalPropagator propagator1 = new NumericalPropagator(new DormandPrince853Integrator(0.01, 500,
            tolerances[0], tolerances[1]));
        for (final ForceModel forceModel : forceModels) {
            propagator1.addForceModel(forceModel);
        }

        if (isPartialDerivatives) {
            propagator1 = new NumericalPropagator(new DormandPrince853Integrator(0.01, 500,
                tolerances[0], tolerances[1]), initialState.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
            final PartialDerivativesEquations equations1 = new PartialDerivativesEquations("Partial", propagator1);
            final List<Parameter> list = new ArrayList<>();
            list.add(forceModels.get(0).getParameters().get(0));
            list.add(forceModels.get(1).getParameters().get(0));
            equations1.selectParameters(list);
            initialState = equations1.setInitialJacobians(initialState);
        }
        else {
            propagator1 = new NumericalPropagator(new DormandPrince853Integrator(0.01, 500,
                tolerances[0], tolerances[1]), initialState.getFrame(), OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        }
        for (final ForceModel forceModel : forceModels) {
            propagator1.addForceModel(forceModel);
        }
        propagator1.setInitialState(initialState);
        propagator1.setAttitudeProvider(attitudeProvider);
        for (final EventDetector detector : detectors) {
            propagator1.addEventDetector(detector);
        }
        propagator1.setMassProviderEquation(massModel);
        propagator1.setEphemerisMode();
        final double t0 = System.currentTimeMillis();
        propagator1.propagate(finalDate);
        final double tRef = (System.currentTimeMillis() - t0) / 1000.;
        final BoundedPropagator reference = propagator1.getGeneratedEphemeris();

        // Cowell propagation
        NumericalPropagator propagator2 = new NumericalPropagator(new CowellIntegrator(order, tol, tol));
        for (final ForceModel forceModel : forceModels) {
            propagator2.addForceModel(forceModel);
        }
        if (isPartialDerivatives) {
            propagator2 = new NumericalPropagator(new CowellIntegrator(order, tol, tol),
                initialState.getFrame(), OrbitType.CARTESIAN, PositionAngle.TRUE);
            final PartialDerivativesEquations equations2 = new PartialDerivativesEquations("Partial", propagator2);
            final List<Parameter> list = new ArrayList<>();
            list.add(forceModels.get(0).getParameters().get(0));
            list.add(forceModels.get(1).getParameters().get(0));
            equations2.selectParameters(list);
            initialState = equations2.setInitialJacobians(initialState);
        }
        else {
            propagator2 = new NumericalPropagator(new DormandPrince853Integrator(0.01, 500,
                tolerances[0], tolerances[1]), initialState.getFrame(), OrbitType.EQUINOCTIAL, PositionAngle.TRUE);
        }
        for (final ForceModel forceModel : forceModels) {
            propagator2.addForceModel(forceModel);
        }
        propagator2.setMassProviderEquation(massModel);
        propagator2.setInitialState(initialState);
        propagator2.setAttitudeProvider(attitudeProvider);
        for (final EventDetector detector : detectors) {
            propagator2.addEventDetector(detector);
        }
        if (isEphemerisMode) {
            // Ephemeris mode
            propagator2.setEphemerisMode();
        } else {
            this.maxPosError = 0;
            if (isFixedMasterMode) {
                // Fixed master mode
                propagator2.setMasterMode(10., new PatriusFixedStepHandler(){
                    /** Serializable UID. */
                    private static final long serialVersionUID = 4158204351163776623L;

                    @Override
                    public void init(final SpacecraftState s0, final AbsoluteDate t) {
                        checkSpacecraftState(reference, s0, isPartialDerivatives);
                    }

                    @Override
                    public void handleStep(final SpacecraftState currentState, final boolean isLast)
                        throws PropagationException {
                        checkSpacecraftState(reference, currentState, isPartialDerivatives);
                    }
                });
            } else {
                // Regular master mode
                propagator2.setMasterMode(new PatriusStepHandler(){
                    /** Serializable UID. */
                    private static final long serialVersionUID = 8794738023819352139L;

                    @Override
                    public void init(final SpacecraftState s0, final AbsoluteDate t) {
                        // nothing to do
                    }

                    @Override
                    public void handleStep(final PatriusStepInterpolator interpolator, final boolean isLast)
                        throws PropagationException {
                        try {
                            checkSpacecraftState(reference, interpolator.getInterpolatedState(), isPartialDerivatives);
                        } catch (final PatriusException e) {
                            // nothing to do
                        }
                    }
                });
            }
        }

        // Propagation
        final double t1 = System.currentTimeMillis();
        propagator2.propagate(finalDate);
        final double tAct = (System.currentTimeMillis() - t1) / 1000.;

        // Ephemeris check
        if (isEphemerisMode) {
            this.maxPosError = 0;
            final BoundedPropagator actual = propagator2.getGeneratedEphemeris();
            AbsoluteDate currentDate = initialOrbit.getDate();
            while (currentDate.compareTo(finalDate) <= 0) {
                checkSpacecraftState(reference, actual.propagate(currentDate), isPartialDerivatives);
                currentDate = currentDate.shiftedBy(10.);
            }
        }

        if (isTest) {
            // Write to report
            Report.printToReport(String.format(Locale.US, "%-22s (%d, %2.0e)", name, order, tol), 0, this.maxPosError);
            // Check max position error
            Assert.assertEquals(this.maxPosError, 0., tolPos);
        }
        if (isMC) {
            // Monte-Carlo
            System.out.println(String.format(Locale.US,
                "%s |      %2.2e |          %2.2e |         %2.2e |          %d |         %d", name,
                this.maxPosError, tAct, tRef, propagator2.getCalls(), propagator1.getCalls()));
        }

        // Return generated ephemeris
        return isEphemerisMode ? propagator2.getGeneratedEphemeris() : null;
    }

    /**
     * Check spacecraft state vs reference ephemeris.
     *
     * @param reference reference ephemeris
     * @param currentState current state to check
     * @param isPartialDerivatives true if partial are to be displayed
     */
    private void checkSpacecraftState(final BoundedPropagator reference, final SpacecraftState currentState,
                                      final boolean isPartialDerivatives) {
        try {
            // Get reference and actual positions
            final SpacecraftState refState = reference.propagate(currentState.getDate());
            final Vector3D ref = refState.getPVCoordinates().getPosition();
            final Vector3D act = currentState.getPVCoordinates().getPosition();
            // Update max position error
            this.maxPosError = MathLib.max(this.maxPosError, ref.subtract(act).getNorm());

            // Check bulletin
            Assert.assertEquals(refState.getMass("Tank"), currentState.getMass("Tank"), 1E-8);
            if (DISPLAY_RES) {
                System.out.println(currentState.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) + " "
                        + ref.subtract(act).getNorm() + " " + (currentState.getA() - 7000000) + " "
                        + currentState.getMass("Tank")
                        + " " + refState.getMass("Tank"));
            }
            // Check partial derivatives (only last bulletin)
            if (isPartialDerivatives && currentState.getDate().durationFrom(AbsoluteDate.J2000_EPOCH) == 86400) {
                final double[][] res1 = new double[6][8];
                final double[][] res2 = new double[6][8];
                for (int i = 0; i < 6; i++) {
                    for (int j = 0; j < 8; j++) {
                        res1[i][j] = refState.getAdditionalState("Partial")[i + j * 6];
                        res2[i][j] = currentState.getAdditionalState("Partial")[i + j * 6];
                        check(res1[i][j], res2[i][j], 1E-5);
                    }
                }
                if (DISPLAY_RES) {
                    System.out.println(new Array2DRowRealMatrix(res1));
                    System.out.println(new Array2DRowRealMatrix(res2));
                }
            }
        } catch (final PatriusException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check two values vs relative tolerance.
     *
     * @param value1 1st value
     * @param value2 2nd value
     * @param tol relative tolerance
     */
    private static void check(final double value1, final double value2, final double tol) {
        if (value1 == 0) {
            Assert.assertEquals(value1, value2, tol);
        } else {
            Assert.assertEquals(0., (value2 - value1) / value1, tol);
        }
    }

    /**
     * Balmino attraction 8x8.
     *
     * @return force model
     */
    private static ForceModel balminoAttraction() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(true));
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
        final double[][] c = data.getC(8, 8, true);
        final double[][] s = data.getS(8, 8, true);
        final BalminoGravityModel earthGravityModel = new BalminoGravityModel(FramesFactory.getITRF(),
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU, c, s, 0, 0);
        earthGravityModel.setCentralTermContribution(false);

        return new DirectBodyAttraction(earthGravityModel);
    }

    /**
     * Balmino attraction 8x8 with partial derivatives.
     *
     * @return force model
     */
    private static ForceModel balminoAttractionDP() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(FramesConfigurationFactory.getSimpleConfiguration(true));
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        final PotentialCoefficientsProvider data = GravityFieldFactory.getPotentialProvider();
        final double[][] c = data.getC(8, 8, true);
        final double[][] s = data.getS(8, 8, true);
        BalminoGravityModel gravityModel = new BalminoGravityModel(FramesFactory.getITRF(),
            Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU, c, s, 8, 8);
        gravityModel.setCentralTermContribution(false);
        return new DirectBodyAttraction(gravityModel);
    }

    /**
     * Drag MSIS-2000.
     *
     * @param spacecraft spacecraft
     * @return force model
     */
    private static ForceModel dragMSIS2000(final Assembly spacecraft) throws PatriusException {
        final CelestialPoint sun = new MeeusSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());
        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(new ConstantSolarActivity(140, 15));
        final Atmosphere atmosphere = new MSISE2000(data, earth, sun);
        final AeroModel aeroModel = new AeroModel(spacecraft);
        return new DragForce(atmosphere, aeroModel, false, false);
    }

    /**
     * Drag MSIS-2000 with partial derivatives.
     *
     * @param spacecraft spacecraft
     * @return force model
     */
    private static ForceModel dragMSIS2000PD(final Assembly spacecraft) throws PatriusException {
        final CelestialPoint sun = new MeeusSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF());
        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(new ConstantSolarActivity(140, 15));
        final Atmosphere atmosphere = new MSISE2000(data, earth, sun);
        final AeroModel aeroModel = new AeroModel(spacecraft, atmosphere, earth);
        return new DragForce(new Parameter("KO", 1.5), atmosphere, aeroModel, false, false);
    }

    /**
     * Build assembly. Two parts: main and tank.
     *
     * @return assembly
     */
    private static Assembly buildAssembly() throws PatriusException {

        final AssemblyBuilder builder = new AssemblyBuilder();

        // Parts
        builder.addMainPart("Main");
        builder.addPart("Tank", "Main", Transform.IDENTITY);
        builder.addPart("Solar panel 1", "Main", Transform.IDENTITY);
        builder.addPart("Solar panel 2", "Main", Transform.IDENTITY);

        // Mass properties
        builder.addProperty(new MassProperty(1000.), "Main");
        builder.addProperty(new TankProperty(100.), "Tank");
        builder.addProperty(new PropulsiveProperty(10, 200), "Main");

        // Radiative and aero properties
        builder.addProperty(new AeroSphereProperty(1., 2.2), "Main");
        builder.addProperty(new RadiativeSphereProperty(1.), "Main");
        builder.addProperty(new RadiativeProperty(0.5, 0.5, 0.5), "Main");
        builder.addProperty(new AeroFacetProperty(new Facet(Vector3D.PLUS_I, 10)), "Solar panel 1");
        builder.addProperty(new AeroFacetProperty(new Facet(Vector3D.PLUS_I, 10)), "Solar panel 2");

        builder.initMainPartFrame(new UpdatableFrame(FramesFactory.getGCRF(), Transform.IDENTITY, "Frame"));

        return builder.returnAssembly();
    }

    /**
     * Simple equation y" = -y.
     *
     */
    private class Equations implements SecondOrderDifferentialEquations, FirstOrderDifferentialEquations {

        @Override
        public void computeDerivatives(final double t,
                                       final double[] y,
                                       final double[] yDot) {
            // Unused
        }

        @Override
        public int getDimension() {
            return 2;
        }

        @Override
        public void computeSecondDerivatives(final double t,
                                             final double[] y,
                                             final double[] yDot,
                                             final double[] yDDot) {
            // y" = -y
            yDDot[0] = -y[0];
        }
    }
}
