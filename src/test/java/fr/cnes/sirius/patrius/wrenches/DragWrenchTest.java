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
 * @history Created 27/04/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:106:16/07/2013:Account of massless parts with aero props. Account of parts with mass and without aero properties.
 * VERSION::DM:85:25/07/2013:Created the Drag wrench model
 * VERSION::FA:183:17/03/2014:Added test details to javadoc
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:1177:06/09/2017:add Cook model validation test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.wrenches;

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
import fr.cnes.sirius.patrius.assembly.models.AeroModel;
import fr.cnes.sirius.patrius.assembly.models.AeroWrenchModel;
import fr.cnes.sirius.patrius.assembly.properties.AeroApplicationPoint;
import fr.cnes.sirius.patrius.assembly.properties.AeroFacetProperty;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.features.Facet;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.DTM2000;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.ConstantSolarActivity;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.specialized.DTMSolarData;
import fr.cnes.sirius.patrius.forces.drag.DragForce;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the AeroModel class.
 * 
 * @author cardosop
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class DragWrenchTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Aero wrench model
         * 
         * @featureDescription Aerodynamic wrench model for an assembly
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_40
         */
        AERO_WRENCH_MODEL,

    }

    /**
     * Main part's name
     */
    private final String mainBody = "mainBody";
    /**
     * other part's name
     */
    private final String part2 = "part2";

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(DragWrenchTest.class.getSimpleName(), "Drag wrench");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AERO_WRENCH_MODEL}
     * 
     * @testedMethod {@link AeroWrenchModel#dragWrench(SpacecraftState, double, Vector3D)}
     * 
     * @description Test for the dragWrench method (model with a sphere only)
     * 
     * @input an Assembly made of two parts : a main body and another part.
     *        main body : sphere with a radius of 5m
     *        other part : facet with n = (-1, 0, 2) and area of 25*pi
     *        application point : (-3, 0, 0) and (0, -2, 0) for the sphere and facet respectively
     *        inertia : mass center at (1, 0, 0) and I3 for inertia matrix. total mass = 1500 kg
     * 
     * @output a wrench instance calculated at mass center and another displaced by 3. the torques expected are
     *         (1.141420322460713e-007, -3.056514393840981e-007,
     *         1.622102707575510e-007) and (1.141420322460713e-007, -5.440836832607007e-007, 2.047775629161278e-007)
     *         respectively
     * 
     * @testPassCriteria the results are as expected, to 1e-14 on a relative scale for the norm
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testDragWrench() throws PatriusException {

        Report.printMethodHeader("testDragWrench", "Drag wrench computation", "Unknown",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // -------------------- ASSEMBLY RADIATIVE WRENCH MODEL ---------------- //

        final AssemblyBuilder builder = new AssemblyBuilder();

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // one facet
        final Vector3D normal = new Vector3D(-1., 0, 2.0);
        final Facet facet = new Facet(normal, 25 * FastMath.PI);

        final IPartProperty aeroSphereProp = new AeroSphereProperty(5.);
        builder.addProperty(aeroSphereProp, this.mainBody);
        final IPartProperty aeroFacetProp = new AeroFacetProperty(facet);
        builder.addProperty(aeroFacetProp, this.part2);

        // application points
        final AeroApplicationPoint apS = new AeroApplicationPoint(Vector3D.MINUS_I.scalarMultiply(3));
        builder.addProperty(apS, this.mainBody);
        final AeroApplicationPoint apF = new AeroApplicationPoint(Vector3D.PLUS_J.scalarMultiply(-2));
        builder.addProperty(apF, this.part2);

        // inertia simple prop
        final MassProperty mp = new MassProperty(1500);
        final InertiaSimpleProperty prop = new InertiaSimpleProperty(Vector3D.PLUS_I, new Matrix3D(new double[][] {
            { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }), mp);
        builder.addProperty(prop, this.mainBody);

        // assembly creation
        final Assembly assembly = builder.returnAssembly();

        // -------------------- ASSEMBLY RADIATIVE WRENCH MODEL ---------------- //

        // add main part (one sphere) and part2 (one facet)
        builder.addMainPart(this.mainBody);
        builder.addPart(this.part2, this.mainBody, Transform.IDENTITY);

        // one facet
        builder.addProperty(aeroSphereProp, this.mainBody);
        builder.addProperty(aeroFacetProp, this.part2);

        // application points
        builder.addProperty(apS, this.mainBody);
        builder.addProperty(apF, this.part2);

        // inertia simple prop
        builder.addProperty(mp, this.mainBody);

        // assembly creation
        final Assembly assemblyRef = builder.returnAssembly();

        // --------------------------------- A SPACECRAFT STATE ---------------------- //

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        final SpacecraftState state = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getGCRF(),
            Rotation.IDENTITY, Vector3D.ZERO));
        assemblyRef.initMainPartFrame(state);

        // --------------------------------- ATMOSPHERE DATA ---------------------- //

        // create an Wrench model for the assembly
        final AeroWrenchModel aeroModel = new AeroWrenchModel(assembly);
        final SolarActivityDataProvider solAct = new ConstantSolarActivity(11, 15.);
        final Atmosphere atm = new DTM2000(new DTMSolarData(solAct),
            CelestialBodyFactory.getSun(), new OneAxisEllipsoid(Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS,
                Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getITRF()));

        final DragWrench wreModel = new DragWrench(atm, aeroModel);
        final DragForce refModel = new DragForce(atm, new AeroModel(assemblyRef));

        // --------------------------------- ACCELERATION VALIDATION ---------------------- //
        assembly.initMainPartFrame(state);
        final Vector3D expectedF = refModel.computeAcceleration(state).scalarMultiply(mp.getMass());
        final Vector3D computedF = wreModel.computeWrench(state).getForce();

        // Expected : acceleration is ZERO
        Assert.assertEquals(0, expectedF.subtract(computedF).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        // --------------------------------- ACCELERATION --------------------------------- //

        // --------------------------------- WRENCH CHECK ---------------------- //

        // tx 1.141420322460713e-007
        // ty -3.056514393840981e-007
        // tz 1.622102707575510e-007
        Vector3D expectedTorque = new Vector3D(1.141420322460713e-007, -3.056514393840981e-007,
            1.622102707575510e-007);
        Vector3D actualTorque = wreModel.computeTorque(state);
        Assert.assertEquals(0, expectedTorque.subtract(actualTorque).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        actualTorque = wreModel.computeWrench(state).getTorque();
        Assert.assertEquals(0, expectedTorque.subtract(actualTorque).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        // displaced
        // tx 1.141420322460713e-007
        // ty -5.440836832607007e-007
        // tz 2.047775629161278e-007
        expectedTorque = new Vector3D(1.141420322460713e-007, -5.440836832607007e-007,
            2.047775629161278e-007);

        actualTorque = wreModel.computeTorque(state, Vector3D.PLUS_I.scalarMultiply(3),
            assembly.getMainPart().getFrame());
        Assert.assertEquals(0, expectedTorque.subtract(actualTorque).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        actualTorque = wreModel.computeWrench(state, Vector3D.PLUS_I.scalarMultiply(3),
            assembly.getMainPart().getFrame()).getTorque();
        Assert.assertEquals(0, expectedTorque.subtract(actualTorque).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Torque", expectedTorque, actualTorque);
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
}
