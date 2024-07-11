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
 * @history Created 24/11/2014
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-3040:15/11/2021:[PATRIUS]Reversement des evolutions de la branche patrius-for-lotus 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:490:17/11/2015:Mise à jour testJacobianList() au vu de la nouvelle implémentation de DragForce
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:596:12/04/2016:Improve test coherence
 * VERSION::DM:834:04/04/2017:create vehicle object
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.IPartProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroGlobalProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.US76;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Sphere;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.CartesianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link DragLiftModel} class for the aero drag and lift model
 * 
 * @author Francois Toussaint
 * 
 * @version $Id$
 * 
 * @since 3.0
 * 
 */
public class DragLiftModelTest {

    /**
     * Doubles comparison epsilon
     */
    private static final double EPS = 1e-14;

    /** An Assembly for the tests. */
    private Assembly testAssembly;

    /** Body name. */
    private static final String MAIN_BODY = "mainBody";

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Drag lift model
         * 
         * @featureDescription Aerodynamic drag and lift model
         */
        DRAG_LIFT_MODEL
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(DragLiftModelTest.class.getSimpleName(), "Drag lift model");
    }

    /**
     * @throws PatriusException
     * @throws IOException
     * @throws ParseException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragForce#getParameters()}
     * 
     * @description Test for the DragLiftModel parameters
     * 
     * @input nothing
     * 
     * @output parameter list
     * 
     * @testPassCriteria two elements
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testJacobianList() throws PatriusException, IOException, ParseException {

        // Configure data management accordingly
        Utils.setDataRoot("potentialPartialDerivatives");

        // create the assembly
        this.createValidTestAssembly();

        // create an AeroModel for the assembly
        final DragLiftModel dragLiftModel = new DragLiftModel(this.testAssembly);
        final DragForce att = new DragForce(null, dragLiftModel);

        // TRO
        // Assert.assertEquals(5, att.getParameters().size());
        Assert.assertEquals(3, att.getParameters().size());

        final String n = "C_X";
        final String t = "C_Z";

        boolean nb = false;
        boolean tb = false;

        for (final Parameter p : att.getParameters()) {
            if (p.getName().contains(n)) {
                nb = true;
            }
            if (p.getName().contains(t)) {
                tb = true;
            }
        }

        Assert.assertTrue(nb && tb);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragLiftModel#DragLiftModel(Assembly)}
     * 
     * @description Test for the DragLiftModel constructor
     * 
     * @input an Assembly
     * 
     * @output an DragLiftModel instance
     * 
     * @testPassCriteria the instance creation succeeds
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public final void testDragLiftModel() throws PatriusException {

        // create the assembly
        this.createValidTestAssembly();

        // create an DragLiftModel for the assembly
        final DragLiftModel dragLiftModel = new DragLiftModel(this.testAssembly);

        // Should never fail
        Assert.assertNotNull(dragLiftModel);
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragLiftModel#DragLiftModel(Assembly)}
     * 
     * @description Error test for the DragLiftModel constructor : missing properties for a valid drag lift model
     * 
     * @input an Assembly
     * 
     * @output none
     * 
     * @testPassCriteria an IllegalArgumentException is raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testAeroModelError1() throws PatriusException {

        // create the invalid assembly 1
        this.createInvalidTestAssembly1();

        // create an DragLiftModel for the assembly
        final DragLiftModel dragLiftModel1 = new DragLiftModel(this.testAssembly);

        // Should never reach here
        Assert.fail(dragLiftModel1.toString() + " is valid but should not be");
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragLiftModel#DragLiftModel(Assembly)}
     * 
     * @description Error test for the DragLiftModel constructor : missing properties for a valid drag lift model
     * 
     * @input an Assembly
     * 
     * @output none
     * 
     * @testPassCriteria an IllegalArgumentException is raised
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test(expected = IllegalArgumentException.class)
    public final void testAeroModelError2() throws PatriusException {

        // create the invalid assembly 2
        this.createInvalidTestAssembly2();

        // create an DragLiftModel for the assembly
        final DragLiftModel dragLiftModel2 = new DragLiftModel(this.testAssembly);

        // Should never reach here
        Assert.fail(dragLiftModel2.toString() + " is valid but should not be");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragLiftModel#dragAcceleration(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragAcceleration method
     * 
     * @input an Assembly
     * 
     * @output an DragLiftModel instance and an acceleration vector
     * 
     * @testPassCriteria the acceleration vector is as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testDragAcceleration() throws PatriusException {

        Report.printMethodHeader("testDragAcceleration", "Acceleration", "Unknwown", EPS, ComparisonType.ABSOLUTE);

        // spacecraft state
        final double mu = 0.39860043770442e+15;
        final Vector3D position = new Vector3D(7.E+06, 0., 0.);
        final Vector3D velocity = new Vector3D(1.E+03, 1.E+03, 1.E+03);
        final PVCoordinates pvs = new PVCoordinates(position, velocity);
        final AbsoluteDate date = new AbsoluteDate();
        final Orbit orbit = new CartesianOrbit(pvs, FramesFactory.getGCRF(), date, mu);
        final Attitude attitude = new LofOffset(orbit.getFrame(), LOFType.LVLH).getAttitude(orbit, orbit.getDate(),
            orbit.getFrame());
        final SpacecraftState state = new SpacecraftState(orbit, attitude);

        // Earth
        final double f = 0.0;
        final double ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(ae, f, FramesFactory.getITRF());
        earth.setAngularThreshold(EPS);
        earth.setCloseApproachThreshold(EPS);

        // atmosphere
        final US76 atmosModel = new US76(earth);
        final Vector3D relativeVelocity = atmosModel.getVelocity(date, position, FramesFactory.getGCRF());
        final double density = atmosModel.getDensity(date, position, FramesFactory.getGCRF());

        // create the assembly
        this.createValidTestAssembly();

        this.testAssembly.initMainPartFrame(state);
        // create an DragLiftModel for the assembly
        final DragLiftModel dragLiftModel = new DragLiftModel(this.testAssembly);

        final Vector3D acc = dragLiftModel.dragAcceleration(state, density, relativeVelocity);

        // non regression reference
        final Vector3D act = new Vector3D(-1.4152459717340624E-10, 1.3063808964730143E-10, 3.658483354115038E-15);

        this.checkVectors(acc, act, EPS);
        Report.printToReport("Acceleration", acc, act);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#DRAG_LIFT_MODEL}
     * 
     * @testedMethod {@link DragLiftModel#addDDragAccDParam(SpacecraftState, Parameter, double, Vector3D, double[])}
     * @testedMethod {@link DragLiftModel#addDDragAccDState(SpacecraftState, double[][], double[][], double, Vector3D, Vector3D, boolean, boolean)}
     * 
     * @description Test that an exception is raised when using not implemented method addDDragAccDParam and
     *              addDDragAccDState
     * 
     * @input an Assembly
     * 
     * @output an DragLiftModel instance and an acceleration vector
     * 
     * @testPassCriteria the exception is raised
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     */
    @Test
    public final void testNotImplementedMethod() throws PatriusException {

        // create an DragLiftModel for the assembly
        try {
            this.createValidTestAssembly();
        } catch (final PatriusException e1) {
            e1.printStackTrace();
        }
        final DragLiftModel dragLiftModel = new DragLiftModel(this.testAssembly);

        // Call not implemented methods : an exception should raise
        final double[] dAccdParam = new double[3];
        final double[][] dAccdPos = null;
        final double[][] dAccdVel = null;
        final Vector3D acceleration = new Vector3D(1.0, 0., 0.);
        // Method addDDragAccDParam
        try {
            dragLiftModel.addDDragAccDParam(null, null, 0.0, Vector3D.MINUS_I, dAccdParam);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
        // Method addDDragAccDState
        try {
            dragLiftModel.addDDragAccDState(null, dAccdPos, dAccdVel, 0.0, acceleration, Vector3D.MINUS_I, true, true);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }

    }

    /**
     * General set up method.
     * 
     * @throws PatriusException
     *         if an Orekit error occurs
     */
    @Before
    public void setUp() throws PatriusException {
        // Orekit data initialization
        fr.cnes.sirius.patrius.Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

    /**
     * Check vectors are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    private void checkVectors(final Vector3D exp, final Vector3D act, final double eps) {
        Assert.assertEquals(exp.getX(), act.getX(), eps);
        Assert.assertEquals(exp.getY(), act.getY(), eps);
        Assert.assertEquals(exp.getZ(), act.getZ(), eps);
    }

    /**
     * Creates a new Assembly and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createValidTestAssembly() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part
        builder.addMainPart(MAIN_BODY);

        // adding aero properties
        final double dragCoef = 1.2;
        final double liftCoef = 1.3;
        final IPartProperty aeroGlobalProp = new AeroGlobalProperty(dragCoef, liftCoef, new Sphere(Vector3D.ZERO,
            1. / MathLib.sqrt(FastMath.PI)));
        builder.addProperty(aeroGlobalProp, MAIN_BODY);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new invalid Assembly and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createInvalidTestAssembly1() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part
        builder.addMainPart(MAIN_BODY);

        // adding mass properties
        final IPartProperty massMainProp = new MassProperty(100.);
        builder.addProperty(massMainProp, MAIN_BODY);

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

    /**
     * Creates a new invalid Assembly and sets testAssembly with it.
     * 
     * @throws PatriusException
     */
    private void createInvalidTestAssembly2() throws PatriusException {
        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part
        builder.addMainPart(MAIN_BODY);

        // adding aero properties
        final double dragCoef = 1.2;
        final double liftCoef = 1.3;
        final IPartProperty aeroGlobalProp = new AeroGlobalProperty(dragCoef, liftCoef, new Sphere(Vector3D.ZERO,
            1. / MathLib.sqrt(FastMath.PI)));
        builder.addProperty(aeroGlobalProp, MAIN_BODY);

        // assembly creation
        this.testAssembly = builder.returnAssembly();
    }

}
