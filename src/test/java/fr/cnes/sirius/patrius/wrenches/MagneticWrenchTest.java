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
 * @history creation 23/07/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:85:05/09/2013:Test class
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:183:17/03/2014:Added test details to javadoc
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
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
import fr.cnes.sirius.patrius.assembly.models.MagneticMoment;
import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticField;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticFieldFactory;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticFieldFactory.FieldModel;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit tests for the MagneticWrench class
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 2..1
 * 
 */
public class MagneticWrenchTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Magnetic wrench model
         * 
         * @featureDescription Magnetic wrench model
         * 
         * @coveredRequirements DV-COUPLES_10, DV-COUPLES_20, DV-COUPLES_30, DV-COUPLES_60
         */
        MAG_WRENCH,

    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(MagneticWrenchTest.class.getSimpleName(), "Magnetic wrench");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MAG_WRENCH}
     * 
     * @testedMethod {@link MagneticWrench#computeTorque(SpacecraftState)}
     * @testedMethod {@link MagneticWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input a magnetic moment (8, 5, 3) and a geomagnetic field from IGRF model (0,0000057655; -0,0000012437;
     *        0,0000101489)
     * 
     * @output the resulting magnetic moment 5.447580294862813e-005, -6.389506273145691e-005, -3.877703664391349e-005
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm
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

        Report.printMethodHeader("testComputeWrenchSpacecraftState", "Magnetic wrench computation (at center of mass)",
            "Unknown", Precision.DOUBLE_COMPARISON_EPSILON, ComparisonType.ABSOLUTE);

        final MagneticMoment moment = new MagneticMoment(new Vector3D(8., 5., 3));
        final GeoMagneticField field = GeoMagneticFieldFactory.getField(FieldModel.IGRF, 2012);

        final MagneticWrench wrench = new MagneticWrench(moment, field);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        final SpacecraftState state = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getGCRF(),
            Rotation.IDENTITY, Vector3D.ZERO));

        final Vector3D result = wrench.computeTorque(state);
        final Vector3D result1 = wrench.computeWrench(state).getTorque();
        final Vector3D expected =
            new Vector3D(5.447580294862813e-005, -6.389506273145691e-005, -3.877703664391349e-005);

        Assert.assertEquals(0, result.subtract(expected).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, result1.subtract(expected).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Torque", expected, result);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#MAG_WRENCH}
     * 
     * @testedMethod {@link MagneticWrench#computeTorque(SpacecraftState)}
     * @testedMethod {@link MagneticWrench#computeWrench(SpacecraftState)}
     * 
     * @description Test for the computeTorque and computeWrench methods
     * 
     * @input a magnetic moment (8, 5, 3) and a geomagnetic field from IGRF model (0,0000057655; -0,0000012437;
     *        0,0000101489)
     *        and an origin where to express the torque
     * 
     * @output the resulting magnetic moment 5.447580294862813e-005, -6.389506273145691e-005, -3.877703664391349e-005 is
     *         the same
     * 
     * @testPassCriteria the wrench is as expected, to 1e-14 on a relative scale for the norm
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
            "Magnetic wrench computation (at given point)", "Unknown", Precision.DOUBLE_COMPARISON_EPSILON,
            ComparisonType.ABSOLUTE);

        final MagneticMoment moment = new MagneticMoment(new Vector3D(8., 5., 3));
        final GeoMagneticField field = GeoMagneticFieldFactory.getField(FieldModel.IGRF, 2012);

        final MagneticWrench wrench = new MagneticWrench(moment, field);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final AbsoluteDate date = new AbsoluteDate(2012, 4, 2, 15, 3, 0, TimeScalesFactory.getTAI());
        final double a = 10000000;
        final Orbit orbit = new KeplerianOrbit(a, 0.00001, MathLib.toRadians(75), .5, 0, .2, PositionAngle.MEAN,
            FramesFactory.getGCRF(), date, mu);

        // spacecraft
        final SpacecraftState state = new SpacecraftState(orbit, new Attitude(date, FramesFactory.getGCRF(),
            Rotation.IDENTITY, Vector3D.ZERO));

        // combination of the position and the attitude
        final Transform rotation = new Transform(AbsoluteDate.J2000_EPOCH, state.getAttitude().getOrientation());
        final Transform translation = new Transform(AbsoluteDate.J2000_EPOCH, state.getOrbit().getPVCoordinates()
            .negate());
        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, translation, rotation);

        // main part frame
        final Frame f = new Frame(state.getFrame(), transform, "mainPartFrame");
        final Vector3D o = new Vector3D(-2, 0, 0);

        final Vector3D force = wrench.computeWrench(state, o, f).getForce();

        final Vector3D result = wrench.computeTorque(state, o, f);
        final Vector3D result1 = wrench.computeWrench(state, o, f).getTorque();

        final Vector3D expected =
            new Vector3D(5.447580294862813e-005, -6.389506273145691e-005, -3.877703664391349e-005);

        Assert.assertEquals(0, result.subtract(expected).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, result1.subtract(expected).getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);

        Report.printToReport("Torque", expected, result);

        Assert.assertEquals(0, force.getNorm(), Precision.DOUBLE_COMPARISON_EPSILON);
    }

    @Before
    public void setup() {
        Utils.setDataRoot("earth");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }
}
