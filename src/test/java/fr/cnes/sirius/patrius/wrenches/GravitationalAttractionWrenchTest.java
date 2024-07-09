/**
 * 
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
 * 
 * @history Created on 18/07/2013
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:85:09/09/2013:Created the attraction wrench model
 * VERSION::FA:183:17/03/2014:Added test details to javadoc
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 * 
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
import fr.cnes.sirius.patrius.assembly.models.InertiaComputedModel;
import fr.cnes.sirius.patrius.assembly.properties.InertiaSimpleProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
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
 * Unit tests
 * 
 * @author Rami Houdroge
 * @since 2.1
 * @version $$
 */
public class GravitationalAttractionWrenchTest {

    private SpacecraftState s;
    private final String mainBody = "main";
    private InertiaComputedModel model;
    private double mu;
    private GravitationalAttractionWrench wrenchModel;
    private final double eps = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Gravitational wrench model
         * 
         * @featureDescription Gravitational wrench model for an assembly
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_70
         */
        GRAVITATIONAL_WRENCH,

    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GravitationalAttractionWrenchTest.class.getSimpleName(),
            "Gravitational attraction wrench");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITATIONAL_WRENCH}
     * 
     * @testedMethod {@link GravitationalAttractionWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input an inertia model (1, 0, 0) I3 and 1500 kg and a gravitational parameter
     * 
     * @output the resulting wrench at the given orbital position
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm, with respect
     *                   to an analytical reference calculated in the test
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testComputeWrenchSpacecraftState() throws PatriusException {

        Report.printMethodHeader("testComputeWrenchSpacecraftState",
            "Gravitational attraction wrench computation (at center of mass)", "Math",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        // Unit vector
        final Vector3D unit = this.s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = 3 * this.mu / MathLib.pow(this.s.getPVCoordinates().getPosition().getNorm(), 3);
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(this.s.getFrame(), this.s.getDate()).multiply(unit);

        final Wrench exp = new Wrench(this.model.getMassCenter(this.s.getFrame(), this.s.getDate()), force,
            force.crossProduct(inertia));
        final Wrench act = this.wrenchModel.computeWrench(this.s);

        this.checkV(exp.getOrigin(), act.getOrigin(), this.eps);
        this.checkV(exp.getTorque(), act.getTorque(), this.eps);
        this.checkV(exp.getForce(), act.getForce(), this.eps);

        Report.printToReport("Torque", exp.getTorque(), act.getTorque());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITATIONAL_WRENCH}
     * 
     * @testedMethod {@link GravitationalAttractionWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input an inertia model (1, 0, 0) I3 and 1500 kg and a gravitational parameter. A point in which to express the
     *        wrench is also given (0, 5, 0)
     * 
     * @output the resulting wrench at the given user position
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm, with respect
     *                   to an analytical reference calculated in the desired location
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testComputeWrenchSpacecraftStateVector3DFrame() throws PatriusException {

        Report.printMethodHeader("testComputeWrenchSpacecraftStateVector3DFrame",
            "Gravitational attraction wrench computation (at given point)", "Math",
            Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        final Frame f = FramesFactory.getCIRF();
        final Vector3D o = Vector3D.PLUS_J.scalarMultiply(5);

        // Unit vector
        final Vector3D unit = this.s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = 3 * this.mu / MathLib.pow(this.s.getPVCoordinates().getPosition().getNorm(), 3);
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(f, this.s.getDate(), o).multiply(unit);

        final Wrench exp = new Wrench(o, force,
            force.crossProduct(inertia));
        final Wrench act = this.wrenchModel.computeWrench(this.s, o, f);

        this.checkV(exp.getOrigin(), act.getOrigin(), this.eps);
        this.checkV(exp.getTorque(), act.getTorque(), this.eps);
        this.checkV(exp.getForce(), act.getForce(), this.eps);

        Report.printToReport("Torque", exp.getTorque(), act.getTorque());
    }

    /**
     * For coverage (same as testComputeWrenchSpacecraftState).
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITATIONAL_WRENCH}
     * 
     * @testedMethod {@link GravitationalAttractionWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input an inertia model (1, 0, 0) I3 and 1500 kg and a gravitational parameter
     * 
     * @output the resulting wrench at the given orbital position
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm, with respect
     *                   to an analytical reference calculated in the test
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testComputeTorqueSpacecraftState() throws PatriusException {

        // Unit vector
        final Vector3D unit = this.s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = 3 * this.mu / MathLib.pow(this.s.getPVCoordinates().getPosition().getNorm(), 3);
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(this.s.getFrame(), this.s.getDate()).multiply(unit);

        final Vector3D exp = new Wrench(this.model.getMassCenter(this.s.getFrame(), this.s.getDate()), force,
            force.crossProduct(inertia)).getTorque();
        final Vector3D act = this.wrenchModel.computeTorque(this.s);

        this.checkV(exp, act, this.eps);
    }

    /**
     * For coverage (same as testComputeWrenchSpacecraftStateVector3DFrame).
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#GRAVITATIONAL_WRENCH}
     * 
     * @testedMethod {@link GravitationalAttractionWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input an inertia model (1, 0, 0) I3 and 1500 kg and a gravitational parameter. A point in which to express the
     *        wrench is also given (0, 5, 0)
     * 
     * @output the resulting wrench at the given user position
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm, with respect
     *                   to an analytical reference calculated in the desired location
     * 
     * @referenceVersion 2.1
     * 
     * @nonRegressionVersion 2.1
     * 
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public void testComputeTorqueSpacecraftStateVector3DFrame() throws PatriusException {
        final Frame f = FramesFactory.getCIRF();
        final Vector3D o = Vector3D.PLUS_J.scalarMultiply(5);

        // Unit vector
        final Vector3D unit = this.s.getPVCoordinates().getPosition().normalize();

        // gravitational attraction
        final double tmtr3 = 3 * this.mu / MathLib.pow(this.s.getPVCoordinates().getPosition().getNorm(), 3);
        final Vector3D force = unit.scalarMultiply(tmtr3);

        // inertia model
        final Vector3D inertia = this.model.getInertiaMatrix(f, this.s.getDate(), o).multiply(unit);

        final Vector3D exp = new Wrench(o, force,
            force.crossProduct(inertia)).getTorque();
        final Vector3D act = this.wrenchModel.computeTorque(this.s, o, f);

        this.checkV(exp, act, this.eps);

    }

    @Before
    public void setup() throws PatriusException {

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // orbit
        this.mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, this.mu);

        // spacecraft
        this.s = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getGCRF(),
            Rotation.IDENTITY, Vector3D.ZERO));

        // inertia simple prop
        final AssemblyBuilder builder = new AssemblyBuilder();
        builder.addMainPart(this.mainBody);
        final MassProperty mp = new MassProperty(1500);
        final InertiaSimpleProperty prop = new InertiaSimpleProperty(Vector3D.PLUS_I, new Matrix3D(new double[][] {
            { 1, 0, 0 }, { 0, 1, 0 }, { 0, 0, 1 } }), mp);
        builder.addProperty(prop, this.mainBody);
        builder.initMainPartFrame(this.s);
        final Assembly assembly = builder.returnAssembly();
        this.model = new InertiaComputedModel(assembly);

        this.wrenchModel = new GravitationalAttractionWrench(this.model, this.mu);
    }

    private void checkV(final Vector3D exp, final Vector3D act, final double e) {
        Assert.assertEquals(exp.getX(), act.getX(), e);
        Assert.assertEquals(exp.getY(), act.getY(), e);
        Assert.assertEquals(exp.getZ(), act.getZ(), e);
    }
}
