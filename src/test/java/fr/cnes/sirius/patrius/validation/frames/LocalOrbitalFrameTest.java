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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.frames;

import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.LocalOrbitalFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.validation.propagation.DummyPropagator;

/**
 * @description additional validation tests for LocalOrbitalFrame
 * 
 * @see fr.cnes.sirius.patrius.frames#LocalOrbitalFrame
 * 
 * @author Sylvain VRESK
 * 
 * @version $Id: LocalOrbitalFrameTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.0
 * 
 */
public class LocalOrbitalFrameTest {

    /** validation tool */
    static Validate validate;

    /**
     * TNW ROL
     */
    private static final String TNW = "TNW";

    /**
     * QSW ROL
     */
    private static final String QSW = "QSW";

    /** Epsilon used for distance comparison. */
    private final double epsilonDistance = Utils.epsilonTest;

    /** Epsilon used for double comparisons. */
    private final double epsilonComparison = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Non regression Epsilon */
    private final double epsilonNonReg = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle LocalOrbitalFrame validation test improvement for QSW frame
         * 
         * @featureDescription improve orekit LocalOrbitalFrame class code validation and coverage. Tests results are
         *                     checked with MSLIB provided ones.
         * 
         * @coveredRequirements DV-REPERES_70, DV-REPERES_80, DV-REPERES_90
         */
        VALIDATION_QSW,
        /**
         * @featureTitle LocalOrbitalFrame validation test improvement for TNW frame
         * 
         * @featureDescription improve orekit LocalOrbitalFrame class code validation and coverage. Tests results are
         *                     checked with MSLIB provided ones.
         * 
         * @coveredRequirements DV-REPERES_70, DV-REPERES_80, DV-REPERES_90
         */
        VALIDATION_TNW
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#VALIDATION_QSW}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.frames.LocalOrbitalFrame#LocalOrbitalFrame(Frame, LOFType, org.orekit.utils.PVCoordinatesProvider, String)}
     * 
     * @description checks results with MSLIB90 V6.13 ones with an absolute epsilon of 1e-12 for distances comparison
     *              (100 times the epsilon for double comparison because it is a physical measure which is compared with
     *              a reference which comes from another software.
     * 
     * @input Vector3D in IJK frame
     * 
     * @output Vector3D in QSW frame
     * 
     * @testPassCriteria no exception arise, the frames are as expected and the position is equal to the reference with
     *                   an epsilon of 1e-12 (epsilon for distances comparison). The epsilon of non regression is 1e-14.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     *         if fails
     * 
     */
    @Test
    public void testQSW() throws PatriusException {

        final Frame veis1950 = FramesFactory.getVeis1950();
        final Frame gcrf = FramesFactory.getGCRF();
        final TimeScale utc = TimeScalesFactory.getUTC();

        final AbsoluteDate initialDate = new AbsoluteDate(2000, 1, 1, 0, 0, 0., utc);

        // Case #1...
        final Vector3D pos1 = new Vector3D(1., Vector3D.PLUS_I);
        final Vector3D vel1 = new Vector3D(1., Vector3D.PLUS_J);
        final PVCoordinates pvc1 = new PVCoordinates(pos1, vel1);
        final DummyPropagator propagator1 = new DummyPropagator(pvc1, initialDate, gcrf);
        final LocalOrbitalFrame qsw1 = new LocalOrbitalFrame(gcrf, LOFType.QSW, propagator1, QSW);

        final Transform to = gcrf.getTransformTo(qsw1, initialDate);
        final PVCoordinates pvco = to.transformPVCoordinates(pvc1);
        final Vector3D po = pvco.getPosition();
        final Vector3D vo = pvco.getVelocity();
        final Vector3D xDirectiono = to.transformVector(Vector3D.PLUS_I);
        final Vector3D yDirectiono = to.transformVector(Vector3D.PLUS_J);
        // final Vector3D zDirectiono = to.transformVector(Vector3D.PLUS_K);

        Assert.assertEquals(po.dotProduct(xDirectiono), 0., this.epsilonComparison);
        Assert.assertEquals(vo.dotProduct(yDirectiono), 0., this.epsilonComparison);
        Assert.assertEquals(po.getNorm() * vo.getNorm(), po.dotProduct(vo), this.epsilonComparison);

        final Transform ti = qsw1.getTransformTo(gcrf, initialDate);
        final PVCoordinates pvci = ti.transformPVCoordinates(pvco);
        final Vector3D pi = pvci.getPosition();
        final Vector3D vi = pvci.getVelocity();
        final Vector3D xDirectioni = ti.transformVector(xDirectiono);
        // final Vector3D yDirectioni = ti.transformVector(yDirectiono);
        // final Vector3D zDirectioni = ti.transformVector(zDirectiono);

        Assert.assertEquals(pi.dotProduct(xDirectioni), 1., this.epsilonComparison);
        Assert.assertEquals(pi.dotProduct(vi), 0., this.epsilonComparison);

        Assert.assertEquals(Vector3D.PLUS_I.getX(), pi.getX(), this.epsilonComparison);
        Assert.assertEquals(Vector3D.PLUS_I.getY(), pi.getY(), this.epsilonComparison);
        Assert.assertEquals(Vector3D.PLUS_I.getZ(), pi.getZ(), this.epsilonComparison);

        // Case #2...
        final Vector3D pos2 = new Vector3D(1., 0., 2.);
        final Vector3D vel2 = new Vector3D(2., -1., 1.);
        final PVCoordinates pvc2 = new PVCoordinates(pos2, vel2);
        final DummyPropagator propagator2 = new DummyPropagator(pvc2, initialDate, veis1950);
        final LocalOrbitalFrame qsw2 = new LocalOrbitalFrame(veis1950, LOFType.QSW, propagator2, QSW);

        final Transform t2 = qsw2.getTransformTo(veis1950, initialDate);

        final Vector3D xDirection2 = t2.transformVector(Vector3D.PLUS_I);
        final Vector3D yDirection2 = t2.transformVector(Vector3D.PLUS_J);
        final Vector3D zDirection2 = t2.transformVector(Vector3D.PLUS_K);

        double expected_X;
        double expected_Y;
        double expected_Z;
        expected_X = 0.447213595499958;
        expected_Y = 0.;
        expected_Z = 0.894427190999916;
        validate.assertEquals(xDirection2.getX(), 0.44721359549995787, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "xDirection2_X");
        validate.assertEquals(xDirection2.getY(), 0.0, this.epsilonNonReg, expected_Y, this.epsilonDistance,
            "xDirection2_Y");
        validate.assertEquals(xDirection2.getZ(), 0.894427190999916, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "xDirection2_Z");

        expected_X = 0.717137165600636;
        expected_Y = -0.597614304667197;
        expected_Z = -0.358568582800318;
        validate.assertEquals(yDirection2.getX(), 0.7171371656006361, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "yDirection2_X");
        validate.assertEquals(yDirection2.getY(), -0.597614304667197, this.epsilonNonReg, expected_Y,
            this.epsilonDistance,
            "yDirection2_Y");
        validate.assertEquals(yDirection2.getZ(), -0.358568582800318, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "yDirection2_Z");

        expected_X = 0.534522483824849;
        expected_Y = 0.80178372573727;
        expected_Z = -0.267261241912424;
        validate.assertEquals(zDirection2.getX(), 0.534522483824849, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "zDirection2_X");
        validate.assertEquals(zDirection2.getY(), 0.8017837257372731, this.epsilonNonReg, expected_Y,
            this.epsilonDistance,
            "zDirection2_Y");
        validate.assertEquals(zDirection2.getZ(), -0.2672612419124245, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "zDirection2_Z");

        // case #3...
        final Vector3D pos3 = new Vector3D(1., 1., 1.);
        final Vector3D vel3 = new Vector3D(1., 1., 0.);
        final PVCoordinates pvc3 = new PVCoordinates(pos3, vel3);
        final DummyPropagator propagator3 = new DummyPropagator(pvc3, initialDate, veis1950);
        final LocalOrbitalFrame qsw3 = new LocalOrbitalFrame(veis1950, LOFType.QSW, propagator3, QSW);

        final Transform t3 = veis1950.getTransformTo(qsw3, initialDate);

        final Vector3D v_out = t3.transformVector(new Vector3D(1., 1., 1.));
        Assert.assertEquals(MathLib.sqrt(3.), v_out.getX(), this.epsilonComparison);
        Assert.assertEquals(0., v_out.getY(), this.epsilonComparison);
        Assert.assertEquals(0., v_out.getZ(), this.epsilonComparison);
    }

    /**
     * @testType TVT
     * 
     * @testedFeature {@link features#VALIDATION_TNW}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.frames.LocalOrbitalFrame#LocalOrbitalFrame(Frame, LOFType, org.orekit.utils.PVCoordinatesProvider, String)}
     * 
     * @description checks results with MSLIB90 V6.13 ones with an absolute epsilon of 1e-12 for distances comparison
     *              (100 times the epsilon for double comparison because it is a physical measure which is compared with
     *              a reference which comes from another software.
     * 
     * @input Vector3D in IJK frame
     * 
     * @output Vector3D in TNW frame
     * 
     * @testPassCriteria no exception arise, the frames are as expected and the position is equal to the reference with
     *                   an epsilon of 1e-12 (epsilon for distances comparison). The epsilon of non regression is 1e-14.
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     * 
     * @throws PatriusException
     *         if fails
     * 
     */
    @Test
    public void testTNW() throws PatriusException {

        final Frame veis1950 = FramesFactory.getVeis1950();
        final Frame gcrf = FramesFactory.getGCRF();
        final TimeScale utc = TimeScalesFactory.getUTC();

        final AbsoluteDate initialDate = new AbsoluteDate(2000, 1, 1, 0, 0, 0., utc);

        // Case #1...
        final Vector3D pos1 = new Vector3D(1., Vector3D.PLUS_J);
        final Vector3D vel1 = new Vector3D(1., Vector3D.PLUS_I);
        final PVCoordinates pvc1 = new PVCoordinates(pos1, vel1);
        final DummyPropagator propagator1 = new DummyPropagator(pvc1, initialDate, gcrf);
        final LocalOrbitalFrame tnw1 = new LocalOrbitalFrame(gcrf, LOFType.TNW, propagator1, TNW);

        final Transform to = gcrf.getTransformTo(tnw1, initialDate);
        final PVCoordinates pvco = to.transformPVCoordinates(pvc1);
        final Vector3D po = pvco.getPosition();
        final Vector3D vo = pvco.getVelocity();
        final Vector3D xDirectiono = to.transformVector(Vector3D.PLUS_I);
        final Vector3D yDirectiono = to.transformVector(Vector3D.PLUS_J);
        // final Vector3D zDirectiono = to.transformVector(Vector3D.PLUS_K);

        Assert.assertEquals(0., po.dotProduct(xDirectiono), this.epsilonComparison);
        Assert.assertEquals(0., vo.dotProduct(yDirectiono), this.epsilonComparison);
        Assert.assertEquals(po.getNorm() * vo.getNorm(), po.dotProduct(vo), this.epsilonComparison);

        final Transform ti = tnw1.getTransformTo(gcrf, initialDate);
        final PVCoordinates pvci = ti.transformPVCoordinates(pvco);
        final Vector3D pi = pvci.getPosition();
        final Vector3D vi = pvci.getVelocity();
        final Vector3D xDirectioni = ti.transformVector(xDirectiono);
        // final Vector3D yDirectioni = ti.transformVector(yDirectiono);
        // final Vector3D zDirectioni = ti.transformVector(zDirectiono);

        Assert.assertEquals(0., pi.dotProduct(xDirectioni), this.epsilonComparison);
        Assert.assertEquals(0., pi.dotProduct(vi), this.epsilonComparison);
        Assert.assertEquals(Vector3D.PLUS_I.getX(), vi.getX(), this.epsilonComparison);
        Assert.assertEquals(Vector3D.PLUS_I.getY(), vi.getY(), this.epsilonComparison);
        Assert.assertEquals(Vector3D.PLUS_I.getZ(), vi.getZ(), this.epsilonComparison);

        // Case #2...
        final Vector3D pos2 = new Vector3D(1., 0., 2.);
        final Vector3D vel2 = new Vector3D(2., -1., 1.);
        final PVCoordinates pvc2 = new PVCoordinates(pos2, vel2);
        final DummyPropagator propagator2 = new DummyPropagator(pvc2, initialDate, veis1950);
        final LocalOrbitalFrame tnw2 = new LocalOrbitalFrame(veis1950, LOFType.TNW, propagator2, TNW);

        final Transform t2 = tnw2.getTransformTo(veis1950, initialDate);

        final Vector3D xDirection1 = t2.transformVector(Vector3D.PLUS_I);
        final Vector3D yDirection1 = t2.transformVector(Vector3D.PLUS_J);
        final Vector3D zDirection1 = t2.transformVector(Vector3D.PLUS_K);

        double expected_X;
        double expected_Y;
        double expected_Z;
        expected_X = 0.816496580927726;
        expected_Y = -0.408248290463863;
        expected_Z = 0.408248290463863;
        validate.assertEquals(xDirection1.getX(), 0.8164965809277196, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "xDirection1_X");
        validate.assertEquals(xDirection1.getY(), -0.4082482904638618, this.epsilonNonReg, expected_Y,
            this.epsilonDistance,
            "xDirection1_Y");
        validate.assertEquals(xDirection1.getZ(), 0.40824829046385935, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "xDirection1_Z");

        expected_X = 0.218217890235992;
        expected_Y = -0.436435780471985;
        expected_Z = -0.872871560943970;
        validate.assertEquals(yDirection1.getX(), 0.21821789023599503, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "yDirection1_X");
        validate.assertEquals(yDirection1.getY(), -0.4364357804719782, this.epsilonNonReg, expected_Y,
            this.epsilonDistance,
            "yDirection1_Y");
        validate.assertEquals(yDirection1.getZ(), -0.8728715609439696, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "yDirection1_Z");

        expected_X = 0.534522483824849;
        expected_Y = 0.801783725737273;
        expected_Z = -0.267261241912424;
        validate.assertEquals(zDirection1.getX(), 0.5345224838248439, this.epsilonNonReg, expected_X,
            this.epsilonDistance,
            "zDirection1_X");
        validate.assertEquals(zDirection1.getY(), 0.8017837257372745, this.epsilonNonReg, expected_Y,
            this.epsilonDistance,
            "zDirection1_Y");
        validate.assertEquals(zDirection1.getZ(), -0.26726124191241896, this.epsilonNonReg, expected_Z,
            this.epsilonDistance,
            "zDirection1_Z");

        // Case #3...
        final Vector3D pos3 = new Vector3D(1., 1., 1.);
        final Vector3D vel3 = new Vector3D(1., 1., 0.);
        final PVCoordinates pvc3 = new PVCoordinates(pos3, vel3);
        final DummyPropagator propagator3 = new DummyPropagator(pvc3, initialDate, veis1950);
        final LocalOrbitalFrame tnw3 = new LocalOrbitalFrame(veis1950, LOFType.TNW, propagator3, TNW);

        final Transform t3 = veis1950.getTransformTo(tnw3, initialDate);

        final Vector3D v_out = t3.transformVector(new Vector3D(1., 1., 0.));
        Assert.assertEquals(MathLib.sqrt(2.), v_out.getX(), this.epsilonComparison);
        Assert.assertEquals(0., v_out.getY(), this.epsilonComparison);
        Assert.assertEquals(0., v_out.getZ(), this.epsilonComparison);
    }

    /**
     * @throws PatriusException
     * @description start ot the test
     * 
     * @since 1.0
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");
        FramesFactory.setConfiguration(fr.cnes.sirius.patrius.Utils.getIERS2003Configuration(true));

        try {
            validate = new Validate(LocalOrbitalFrameTest.class);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @description end of the test
     * 
     * @since 1.0
     */
    @AfterClass
    public static void tearDown() {
        try {
            validate.produceLog();
        } catch (final IOException e) {
            e.printStackTrace();
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
