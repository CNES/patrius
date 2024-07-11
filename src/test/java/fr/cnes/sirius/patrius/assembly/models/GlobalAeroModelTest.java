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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2082:15/05/2019:Modifications mineures d'api
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:599:13/09/2016: new tabulated aero model
 * VERSION::FA:705:07/12/2016: corrected anomaly in dragAcceleration()
 * VERSION::DM:711:07/12/2016: change signature of method getCoefficients()
 * VERSION::DM:849:20/03/2017:Implementation of DragCoefficientProvider with file reader
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.SortedMap;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.models.GlobalDragCoefficientProvider.INTERP;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaConstant;
import fr.cnes.sirius.patrius.assembly.models.cook.AlphaCookModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantAttitudeLaw;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.ContinuousMSISE2000SolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.UTCTAILoader;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the {@link GlobalAeroModel} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id$
 * 
 * @since 3.3
 */
public class GlobalAeroModelTest {

    /** Path to aero coefficients file. */
    private static String pathToAeroData;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test Tabulated aero model
         * 
         * @featureDescription test Tabulated aero
         * 
         * @coveredRequirements DM-599
         */
        GLOBAL_AERO_MODEL
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        // Root resource
        final String aeroFolder = "coeffaero/";

        // 1) Read aero file without header
        pathToAeroData = GlobalAeroModelTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalModel.txt").getFile();
        Report.printClassHeader(GlobalAeroModelTest.class.getSimpleName(), "Global aero model");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check the acceleration computed by the global aero model
     * 
     * @input data
     * 
     * @output acceleration
     * 
     * @testPassCriteria Results is the same as reference (Math case, relative threshold: 1E-13)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testMath() throws PatriusException {

        // Initialization
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(1000.), "Main");
        builder.addProperty(new AeroProperty(0.4, 17, new AlphaConstant(0.2)), "Main");

        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new MyDragCoefficient(),
            new MyAtmosphere());

        // Computation and check
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY).getAttitude(orbit));

        final Vector3D actual = model.dragAcceleration(state, 13, new Vector3D(14, 15, 16));
        final Vector3D expected = new Vector3D(149.45787794256, 174.191496113057, 198.925114283553);
        Assert.assertEquals(0., actual.subtract(expected).getNorm(), 7E-12);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check the SCx/y/z coefficients computed by the global aero model (case 1)
     * 
     * @input data
     * 
     * @output SCx/y/z coefficients / acceleration
     * 
     * @testPassCriteria Results is the same as reference (Scilab, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase1() throws PatriusException, IOException {

        Report.printMethodHeader("testCase1", "Compute SC", "Scilab", 1E-15, ComparisonType.RELATIVE);

        // SCx/y/z coefficient is checked.
        // Since output is acceleration, all terms in the drag formula (m, rho, etc. are set to 1.)

        // Initialization
        final double mass = 1.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");
        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData), new MyAtmosphere2());

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY).getAttitude(orbit));

        final double azimut = MathLib.toRadians(0.);
        final double elevation = MathLib.toRadians(-90);
        final double s = 4;

        final double rho = 1.;
        final double molarMass = 12.;
        final double vrelnorm = MathLib.sqrt(s * s * 1255 * 2 * Constants.PERFECT_GAS_CONSTANT / molarMass);
        final Vector3D vrel = new Vector3D(vrelnorm, new Vector3D(azimut, elevation));
        final Vector3D actual = model.dragAcceleration(state, rho, vrel.negate());
        final Vector3D actualSC = new Vector3D(1. / (0.5 * vrelnorm * vrelnorm * rho / mass), actual);

        // Check
        final double scx = -4.1817680780250541e-03;
        final double scy = 6.2839645637912016e-03;
        final double scz = 2.5409644751591585e+00;
        final Vector3D expected = new Vector3D(scx, scy, scz);
        Report.printToReport("SC", expected, actualSC);
        Assert.assertEquals(0, (expected.getX() - actualSC.getX()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getY() - actualSC.getY()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getZ() - actualSC.getZ()) / expected.getNorm(), 1E-14);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check the SCx/y/z coefficients computed by the global aero model (case 2)
     * 
     * @input data
     * 
     * @output SCx/y/z coefficients / acceleration
     * 
     * @testPassCriteria Results is the same as reference (Scilab, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase2() throws PatriusException, IOException {

        Report.printMethodHeader("testCase2", "Compute SC", "Scilab", 1E-15, ComparisonType.RELATIVE);

        // SCx/y/z coefficient is checked.
        // Since output is acceleration, all terms in the drag formula (m, rho, etc. are set to 1.)

        // Initialization
        final double mass = 1.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaCookModel(new MyAtmosphere2(), 4., 16.)), "Main");
        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData), new MyAtmosphere2());

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY).getAttitude(orbit));

        final double azimut = MathLib.toRadians(0.);
        final double elevation = MathLib.toRadians(-90);
        final double s = 4;

        final double rho = 1.;
        final double molarMass = 12.;
        final double vrelnorm = MathLib.sqrt(s * s * 1255 * 2 * Constants.PERFECT_GAS_CONSTANT / molarMass);
        final Vector3D vrel = new Vector3D(vrelnorm, new Vector3D(azimut, elevation));

        final Vector3D actual = model.dragAcceleration(state, rho, vrel.negate());
        final Vector3D actualSC = new Vector3D(1. / (0.5 * vrelnorm * vrelnorm * rho / mass), actual);

        // Check
        final double scx = -2.3014001010535169e-02;
        final double scy = -3.5624037680427358e-03;
        final double scz = 2.6991525052792156e+00;
        final Vector3D expected = new Vector3D(scx, scy, scz);
        Report.printToReport("SC", expected, actualSC);
        Assert.assertEquals(0, (expected.getX() - actualSC.getX()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getY() - actualSC.getY()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getZ() - actualSC.getZ()) / expected.getNorm(), 7E-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check the SCx/y/z coefficients computed by the global aero model (case 3)
     * 
     * @input data
     * 
     * @output SCx/y/z coefficients / acceleration
     * 
     * @testPassCriteria Results is the same as reference (Scilab, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase3() throws PatriusException, IOException {

        Report.printMethodHeader("testCase3", "Compute SC", "Scilab", 1E-2, ComparisonType.RELATIVE);

        // SCx/y/z coefficient is checked.
        // Since output is acceleration, all terms in the drag formula (m, rho, etc. are set to 1.)

        // Initialization
        final double mass = 1.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");
        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData), new MyAtmosphere2());

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY).getAttitude(orbit));

        final double azimut = MathLib.toRadians(46.3);
        final double elevation = MathLib.toRadians(8.2);
        final double s = 6.4;

        final double rho = 1.;
        final double molarMass = 12.;
        final double vrelnorm = MathLib.sqrt(s * s * 1255 * 2 * Constants.PERFECT_GAS_CONSTANT / molarMass);
        final Vector3D vrel = new Vector3D(vrelnorm, new Vector3D(azimut, elevation));

        final Vector3D actual = model.dragAcceleration(state, rho, vrel.negate());
        final Vector3D actualSC = new Vector3D(1. / (0.5 * vrelnorm * vrelnorm * rho / mass), actual);

        // Check
        // Accuracy is only 1E-2 since expected results are generated with order 4 spline interpolator
        final double scx = -2.11480699242508639e+00;
        final double scy = -2.0200148429533562e+00;
        final double scz = -3.9196914029286584e-01;
        final Vector3D expected = new Vector3D(scx, scy, scz);
        Report.printToReport("SC", expected, actualSC);
        Assert.assertEquals(0, (expected.getX() - actualSC.getX()) / expected.getNorm(), 1E-2);
        Assert.assertEquals(0, (expected.getY() - actualSC.getY()) / expected.getNorm(), 1E-2);
        Assert.assertEquals(0, (expected.getZ() - actualSC.getZ()) / expected.getNorm(), 1E-2);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check the SCx/y/z coefficients computed by the global aero model (case 3)
     *              This case is similar to case 3 but with no required interpolation (azimut, elevation and s match a
     *              grid point)
     * 
     * @input data
     * 
     * @output SCx/y/z coefficients / acceleration
     * 
     * @testPassCriteria Results is the same as reference (Scilab, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase3bis() throws PatriusException, IOException {

        Report.printMethodHeader("testCase3bis", "Compute SC", "Scilab", 1E-15, ComparisonType.RELATIVE);

        // SCx/y/z coefficient is checked.
        // Since output is acceleration, all terms in the drag formula (m, rho, etc. are set to 1.)

        // Initialization
        final double mass = 1.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");
        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData), new MyAtmosphere2());

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getEME2000(),
            Rotation.IDENTITY).getAttitude(orbit));

        final double azimut = MathLib.toRadians(8.);
        final double elevation = MathLib.toRadians(-82.);
        final double s = 6.;

        final double rho = 1.;
        final double molarMass = 12.;
        final double vrelnorm = MathLib.sqrt(s * s * 1255 * 2 * Constants.PERFECT_GAS_CONSTANT / molarMass);
        final Vector3D vrel = new Vector3D(vrelnorm, new Vector3D(azimut, elevation));

        final Vector3D actual = model.dragAcceleration(state, rho, vrel.negate());
        final Vector3D actualSC = new Vector3D(1. / (0.5 * vrelnorm * vrelnorm * rho / mass), actual);

        // Check
        final double scx = -3.4106940411185355e-01;
        final double scy = -5.3472681852359136e-02;
        final double scz = 2.3492867878072499e+00;
        final Vector3D expected = new Vector3D(scx, scy, scz);
        Report.printToReport("SC", expected, actualSC);
        Assert.assertEquals(0, (expected.getX() - actualSC.getX()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getY() - actualSC.getY()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getZ() - actualSC.getZ()) / expected.getNorm(), 3E-15);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D, Vector3D, boolean, boolean)}
     * 
     * @description check partial derivatives computation
     * 
     * @input data
     * 
     * @output partial derivatives dAcc/dX, dAcc/dV
     * 
     * @testPassCriteria Results is the same as reference (PATRIUS v3.4: partial derivatives computed via drag force
     *                   centered-finites differences, threshold: 1E-5)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testDerivatives() throws PatriusException, IOException {

        Report.printMethodHeader("testDerivatives", "Compute partial derivatives", "PATRIUS v3.4", 0,
            ComparisonType.RELATIVE);

        // Add UTC-TAI
        TimeScalesFactory.clearUTCTAILoaders();
        TimeScalesFactory.addUTCTAILoader(new UTCTAILoader(){

            @Override
            public boolean stillAcceptsData() {
                return false;
            }

            @Override
            public void loadData(final InputStream input, final String name)
                                                                            throws IOException, ParseException,
                                                                            PatriusException {
            }

            @Override
            public SortedMap<DateComponents, Integer> loadTimeSteps() {
                final SortedMap<DateComponents, Integer> entries = new TreeMap<DateComponents, Integer>();
                for (int i = 1990; i < 2010; i++) {
                    entries.put(new DateComponents(i, 1, 1), -35);
                }
                return entries;
            }

            @Override
            public String getSupportedNames() {
                return "";
            }
        });

        // Initialization
        final double mass = 1.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");

        final ExtendedAtmosphere atmosphere = new MyAtmosphere3();
        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData), atmosphere);

        // Computation
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;

        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getMOD(false),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(FramesFactory.getMOD(false),
            Rotation.IDENTITY).getAttitude(orbit));

        final double rho = atmosphere.getDensity(state.getDate(), state.getPVCoordinates().getPosition(), state.getFrame());
        final Vector3D vAtm =
                atmosphere.getVelocity(state.getDate(), state.getPVCoordinates().getPosition(), state.getFrame());
            final Vector3D relvel = vAtm.subtract(state.getPVCoordinates().getVelocity());

        // Actual data
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        model.addDDragAccDState(state, dAccdPos, dAccdVel, rho, Vector3D.PLUS_I, relvel, true, true);

        // Expected data
        final double hPos = 1.;
        final double hVel = state.getMu()
            * hPos
            / (state.getPVCoordinates().getVelocity().getNorm() * state.getPVCoordinates().getPosition()
                .getNormSq());
        final DragForce drag = new DragForce(atmosphere, model);
        final Vector3D accMX =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(-hPos, 0, 0), Vector3D.ZERO));
        final Vector3D accPX =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(hPos, 0, 0), Vector3D.ZERO));
        final Vector3D accMY =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(0, -hPos, 0), Vector3D.ZERO));
        final Vector3D accPY =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(0, hPos, 0), Vector3D.ZERO));
        final Vector3D accMZ =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(0, 0, -hPos), Vector3D.ZERO));
        final Vector3D accPZ =
            drag.computeAcceleration(this.shiftState(state, new Vector3D(0, 0, hPos), Vector3D.ZERO));

        final Vector3D accMVX =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(-hVel, 0, 0)));
        final Vector3D accPVX =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(hVel, 0, 0)));
        final Vector3D accMVY =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(0, -hVel, 0)));
        final Vector3D accPVY =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(0, hVel, 0)));
        final Vector3D accMVZ =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(0, 0, -hVel)));
        final Vector3D accPVZ =
            drag.computeAcceleration(this.shiftState(state, Vector3D.ZERO, new Vector3D(0, 0, hVel)));

        final double[][] dAccdPosExp = {
            { (accPX.getX() - accMX.getX()) / (2. * hPos), (accPY.getX() - accMY.getX()) / (2. * hPos),
                (accPZ.getX() - accMZ.getX()) / (2. * hPos) },
            { (accPX.getY() - accMX.getY()) / (2. * hPos), (accPY.getY() - accMY.getY()) / (2. * hPos),
                (accPZ.getY() - accMZ.getY()) / (2. * hPos) },
            { (accPX.getZ() - accMX.getZ()) / (2. * hPos), (accPY.getZ() - accMY.getZ()) / (2. * hPos),
                (accPZ.getZ() - accMZ.getZ()) / (2. * hPos) },
        };

        final double[][] dAccdVelExp = {
            { (accPVX.getX() - accMVX.getX()) / (2. * hVel), (accPVY.getX() - accMVY.getX()) / (2. * hVel),
                (accPVZ.getX() - accMVZ.getX()) / (2. * hVel) },
            { (accPVX.getY() - accMVX.getY()) / (2. * hVel), (accPVY.getY() - accMVY.getY()) / (2. * hVel),
                (accPVZ.getY() - accMVZ.getY()) / (2. * hVel) },
            { (accPVX.getZ() - accMVX.getZ()) / (2. * hVel), (accPVY.getZ() - accMVY.getZ()) / (2. * hVel),
                (accPVZ.getZ() - accMVZ.getZ()) / (2. * hVel) },
        };

        // Check
        Report.printToReport("dAccdPos", dAccdPosExp, dAccdPos);
        Report.printToReport("dAccdVel", dAccdVelExp, dAccdVel);
        for (int i = 0; i < dAccdVelExp.length; i++) {
            for (int j = 0; j < dAccdVelExp[i].length; j++) {
                // Accuracy limited because reference is computed by finite differences
                Assert.assertEquals(dAccdVelExp[i][j], dAccdVel[i][j], 1E-5);
                Assert.assertEquals(dAccdPosExp[i][j], dAccdPos[i][j], 0);
            }
        }
    }

    /**
     * Shift provided spacecraft state position and velocity with provided increments.
     * 
     * @param s spacecraft state
     * @param dp position increment
     * @param dv velocity increment
     * @return shifted spacecraft state
     * @throws PatriusException if attitude cannot be computed
     */
    private SpacecraftState
            shiftState(final SpacecraftState s, final Vector3D dp, final Vector3D dv)
                                                                                     throws PatriusException {
        final PVCoordinates pv = s.getPVCoordinates();
        final PVCoordinates pvNew = new PVCoordinates(pv.getPosition().add(dp), pv.getVelocity().add(dv));
        final Orbit orbit = new CartesianOrbit(pvNew, s.getFrame(), s.getDate(), s.getMu());
        return new SpacecraftState(orbit, s.getAttitudeForces(), s.getAttitudeEvents(), s.getAdditionalStates());
    }

    /**
     * @throws IOException
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check exceptions are properly thrown
     * 
     * @input wrong data
     * 
     * @output exception
     * 
     * @testPassCriteria exceptions are thrown as expected
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testException() throws PatriusException, IOException {

        // Main part without TabulatedDragProperty
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addPart("Part", "Main", Transform.IDENTITY);
        builder.addProperty(new MassProperty(1000.), "Main");
        builder.addProperty(new AeroProperty(0.4, 17, new AlphaConstant(0.2)), "Part");
        try {
            new GlobalAeroModel(builder.returnAssembly(), new MyDragCoefficient(), new MyAtmosphere());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Assembly without mass property
        final AssemblyBuilder builder2 = new AssemblyBuilder();
        builder2.addMainPart("Main");
        builder2.addProperty(new AeroProperty(0.4, 17, new AlphaConstant(0.2)), "Main");
        try {
            new GlobalAeroModel(builder2.returnAssembly(), new MyDragCoefficient(), new MyAtmosphere());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        // Derivative with respect to parameter
        final double mass = 1.;
        final AssemblyBuilder builder3 = new AssemblyBuilder();
        builder3.addMainPart("Main");
        builder3.addProperty(new MassProperty(mass), "Main");
        builder3.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");

        final CelestialBody sun = new MeeusSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
            Constants.WGS84_EARTH_FLATTENING, FramesFactory.getMOD(false));
        final MSISE2000InputParameters data = new ContinuousMSISE2000SolarData(new ConstantSolarActivity(140, 15));
        final ExtendedAtmosphere atmosphere = new MSISE2000(data, earth, sun);
        final GlobalAeroModel model = new GlobalAeroModel(builder3.returnAssembly(), new GlobalDragCoefficientProvider(
            INTERP.LINEAR, pathToAeroData),
            atmosphere);

        try {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
            final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE,
                FramesFactory.getMOD(false), date, Constants.EGM96_EARTH_MU);
            final SpacecraftState state = new SpacecraftState(orbit, new ConstantAttitudeLaw(
                FramesFactory.getMOD(false), Rotation.IDENTITY).getAttitude(orbit));
            model.addDDragAccDParam(state, new Parameter("param", 0), 12., Vector3D.PLUS_I, new double[3]);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check acceleration computed by the global aero model is consistent with attitude change
     * 
     * @input data
     * 
     * @output acceleration
     * 
     * @testPassCriteria Results is the same as reference (Math, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testWithAttitude() throws PatriusException, IOException {

        Report.printMethodHeader("testWithAttitude", "Compute SC depending on attitude", "Math", 1E-15,
            ComparisonType.RELATIVE);

        // Initialization
        final double mass = 2.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");

        final DragCoefficientProvider provider = new DragCoefficientProvider(){
            @Override
            public DragCoefficient getCoefficients(final Vector3D relativeVelocity, final AtmosphereData data,
                                                   final Assembly assembly) {
                return new DragCoefficient(new Vector3D(1, 0, 0), Vector3D.ZERO, Vector3D.ZERO, Vector3D.ZERO);
            }
        };
        final ExtendedAtmosphere atmosphere = new ExtendedAtmosphere(){
            @Override
            public
                    Vector3D
                    getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
                return new Vector3D(10, 0, 0);
            }

            @Override
            public
                    double
                    getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                        throws PatriusException {
                return 0;
            }

            @Override
            public
                    double
                    getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                   throws PatriusException {
                return 2;
            }

            @Override
            public AtmosphereData
                    getData(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
                return new AtmosphereData(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1);
            }

            @Override
            public Atmosphere copy() {
                return null;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do
            }
        };

        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), provider, atmosphere);

        // Initial case: x(sat) is directed toward orbital velocity hence y(GCRF)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new BodyCenterPointing().getAttitude(orbit));

        final double rho = 2.;
        final Vector3D vrel = Vector3D.PLUS_J;
        final Vector3D actual = model.dragAcceleration(state, rho, vrel);

        final Vector3D scExp = new Vector3D(0, 1, 0);
        final Vector3D expected = scExp.scalarMultiply(0.5 * rho * vrel.getNormSq() / mass);

        // Check
        Report.printToReport("Acceleration", expected, actual);
        Assert.assertEquals(0, (expected.getX() - actual.getX()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getY() - actual.getY()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getZ() - actual.getZ()) / expected.getNorm(), 1E-15);

        // Case after period / 4 (anomaly has shifted by 90deg): x(sat) is directed toward orbital velocity hence
        // -x(GCRF)
        final Orbit orbit2 = orbit.shiftedBy(orbit.getKeplerianPeriod() / 4.);
        final SpacecraftState state2 = new SpacecraftState(orbit2, new BodyCenterPointing().getAttitude(orbit2));

        final double rho2 = 2.;
        final Vector3D vrel2 = Vector3D.MINUS_I;
        final Vector3D actual2 = model.dragAcceleration(state2, rho2, vrel2);

        final Vector3D scExp2 = new Vector3D(-1, 0, 0);
        final Vector3D expected2 = scExp2.scalarMultiply(0.5 * rho2 * vrel2.getNormSq() / mass);

        // Check
        Report.printToReport("Acceleration", expected2, actual2);
        Assert.assertEquals(0, (expected2.getX() - actual2.getX()) / expected2.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected2.getY() - actual2.getY()) / expected2.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected2.getZ() - actual2.getZ()) / expected2.getNorm(), 1E-15);

    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GLOBAL_AERO_MODEL}
     * 
     * @testedMethod {@link GlobalAeroModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description check acceleration computed by the global aero model is consistent with attitude change :
     *              unlike {@link #testWithAttitude()}, the used DragCoefficientProvider depends on the relative
     *              velocity in
     *              the satellite frame. It aims at validating the process of the conversions performed in
     *              {@link GlobalAeroModel #dragAcceleration(SpacecraftState, double, Vector3D)} : the first one to
     *              express the velocity in the satellite
     *              frame, the second to convert back vector SC in the orbit inertial frame.
     * 
     * @input data
     * 
     * @output acceleration
     * 
     * @testPassCriteria Results is the same as reference (Math, relative threshold: 1E-15)
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testWithAttitude2() throws PatriusException, IOException {

        Report.printMethodHeader("testWithAttitude2", "Compute SC depending on attitude", "Math", 1E-15,
            ComparisonType.RELATIVE);

        // Initialization
        final double mass = 2.;
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart("Main");
        builder.addProperty(new MassProperty(mass), "Main");
        builder.addProperty(new AeroProperty(0.11, 300, new AlphaConstant(1.)), "Main");

        // Build a DragCoefficientProvider depending on the relativeVelocity
        final DragCoefficientProvider provider = new DragCoefficientProvider(){
            @Override
            public DragCoefficient getCoefficients(final Vector3D relativeVelocity, final AtmosphereData data,
                                                   final Assembly assembly) {
                // Relative velocity is here [2, 0, 0] since it is in spacecraft frame
                return new DragCoefficient(relativeVelocity.scalarMultiply(2.), Vector3D.ZERO, Vector3D.ZERO,
                    Vector3D.ZERO);
            }
        };
        final ExtendedAtmosphere atmosphere = new ExtendedAtmosphere(){
            @Override
            public
                    Vector3D
                    getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
                return new Vector3D(10, 0, 0);
            }

            @Override
            public
                    double
                    getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                        throws PatriusException {
                return 0;
            }

            @Override
            public
                    double
                    getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                   throws PatriusException {
                return 2;
            }

            @Override
            public AtmosphereData
                    getData(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
                return new AtmosphereData(1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1);
            }

            @Override
            public Atmosphere copy() {
                return null;
            }
            
            /** {@inheritDoc} */
            @Override
            public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
                // Nothing to do
            }
        };

        final GlobalAeroModel model = new GlobalAeroModel(builder.returnAssembly(), provider, atmosphere);

        // Initial case: x(sat) is directed toward orbital velocity hence y(GCRF)
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, FramesFactory.getEME2000(),
            date, Constants.EGM96_EARTH_MU);
        final SpacecraftState state = new SpacecraftState(orbit, new BodyCenterPointing().getAttitude(orbit));

        final double rho = 2.;
        final Vector3D vrel = Vector3D.PLUS_J;
        final Vector3D actual = model.dragAcceleration(state, rho, vrel);

        final Vector3D scExp = new Vector3D(0, 2, 0);
        final Vector3D expected = scExp.scalarMultiply(0.5 * rho * vrel.getNormSq() / mass);

        // Check
        Report.printToReport("Acceleration", expected, actual);
        Assert.assertEquals(0, (expected.getX() - actual.getX()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getY() - actual.getY()) / expected.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected.getZ() - actual.getZ()) / expected.getNorm(), 1E-15);

        // Case after period / 4 (anomaly has shifted by 90deg): x(sat) is directed toward orbital velocity hence
        // -x(GCRF)
        final Orbit orbit2 = orbit.shiftedBy(orbit.getKeplerianPeriod() / 4.);
        final SpacecraftState state2 = new SpacecraftState(orbit2, new BodyCenterPointing().getAttitude(orbit2));

        final double rho2 = 2.;
        final Vector3D vrel2 = Vector3D.MINUS_I;
        final Vector3D actual2 = model.dragAcceleration(state2, rho2, vrel2);

        final Vector3D scExp2 = new Vector3D(-2, 0, 0);
        final Vector3D expected2 = scExp2.scalarMultiply(0.5 * rho2 * vrel2.getNormSq() / mass);

        // Check
        Report.printToReport("Acceleration", expected2, actual2);
        Assert.assertEquals(0, (expected2.getX() - actual2.getX()) / expected2.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected2.getY() - actual2.getY()) / expected2.getNorm(), 1E-15);
        Assert.assertEquals(0, (expected2.getZ() - actual2.getZ()) / expected2.getNorm(), 1E-15);
    }

    /**
     * Local drag coefficient for tests.
     */
    private class MyDragCoefficient implements DragCoefficientProvider {

        @Override
        public DragCoefficient getCoefficients(final Vector3D relativeVelocity, final AtmosphereData data,
                                               final Assembly assembly) {
            return new DragCoefficient(new Vector3D(1, 2, 3), new Vector3D(4, 5, 6), new Vector3D(7, 8, 9),
                new Vector3D(10, 11, 12));
        }
    }

    /**
     * Local atmosphere for tests.
     */
    private class MyAtmosphere implements ExtendedAtmosphere {

        @Override
        public double
                getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public Vector3D
                getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
            // Unused
            return Vector3D.ZERO;
        }

        @Override
        public
                double
                getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public AtmosphereData
                getData(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // 0 set for unused data
            // Values have been chosen to result in a molar mass of 0.015kg
            final double c = Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
            return new AtmosphereData(1E-3, 14, 0, 0, 0, 0, 0, 1, 0, 0, (40. - 15. / c) / (15. / c - 16.));
        }

        @Override
        public Atmosphere copy() {
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }

    /**
     * Local atmosphere for tests.
     */
    private class MyAtmosphere2 implements ExtendedAtmosphere {

        @Override
        public double
                getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public Vector3D
                getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
            // Unused
            return Vector3D.ZERO;
        }

        @Override
        public
                double
                getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public AtmosphereData
                getData(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // 0 set for unused data
            // Values have been chosen to result in a molar mass of 0.012kg
            final double c = Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
            return new AtmosphereData(1E-3, 1255, 0, 0, 0, 0, 0, 1, 0, 0, (40. - 12. / c) / (12. / c - 16.));
        }

        @Override
        public Atmosphere copy() {
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }

    /**
     * Local atmosphere for tests.
     */
    private class MyAtmosphere3 implements ExtendedAtmosphere {

        @Override
        public double
                getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // Unused
            return position.getNorm() / 1E10;
        }

        @Override
        public Vector3D
                getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                throws PatriusException {
            // Unused
            return position.scalarMultiply(0.001);
        }

        @Override
        public
                double
                getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                                    throws PatriusException {
            // Unused
            return 0;
        }

        @Override
        public AtmosphereData
                getData(final AbsoluteDate date, final Vector3D position, final Frame frame) throws PatriusException {
            // 0 set for unused data
            // Values have been chosen to result in a molar mass of 0.015kg
            final double c = Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
            return new AtmosphereData(position.getNorm() * 1E-3, 14, 0, 0, 0, 0, 0, 1, 0, 0, (40. - 15. / c)
                / (15. / c - 16.));
        }

        @Override
        public Atmosphere copy() {
            return null;
        }
        
        /** {@inheritDoc} */
        @Override
        public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
            // Nothing to do
        }
    }
}
