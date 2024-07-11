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
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:588:29/08/2016:ISIS spin bias slew
 * VERSION::DM:832:06/02/2017:reversed ISIS spin bias slew
 * VERSION::DM:1870:05/10/2018:remove slew recomputation in getAttitude() method
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.attitudes.slew.IsisSpinBiasSlewComputer;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for the ISIS spin bias slew.
 * 
 * @author Emmanuel Bignon
 * 
 * @version $Id: IsisSpinBiasSlewTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class IsisSpinBiasSlewTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Spin bias slew tests
         * 
         * @featureDescription Tests with the spin bias slew computation
         * 
         * @coveredRequirements
         */
        ISIS_SPIN_BIAS_SLEW
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(IsisSpinBiasSlewTest.class.getSimpleName(), "ISIS spin bias slew attitude provider");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ISIS_SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getAttitude(Orbit)}
     * 
     * @description This test checks ISIS spin bias slew first case (no constant velocity phase)
     * 
     * @input {@link IsisAnalyticalSpinBiasSlew} data (both numerical and analytical)
     * 
     * @output the computed slews
     * 
     * @testPassCriteria quaternion difference on the whole ephemeris between the two slews is small (absolute
     *                   tolerance: 1E-3)
     *                   Slew duration is as expected (tolerance: 0.)
     *                   Acceleration has the expected profile (tolerance: 1E-7)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase1() throws PatriusException {

        // Initialization
        final AttitudeLaw initialLaw = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(
            1., 1., 1.), MathLib.toRadians(20.)));
        final AttitudeLaw finalLaw = new ConstantAttitudeLaw(FramesFactory.getEME2000(), new Rotation(new Vector3D(1.,
            1., 1.), MathLib.toRadians(35.)));
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final double dtSCAO = 0.250;
        final double thetaMaxAllowed = MathLib.toRadians(179);
        final double durationMax = 200;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 4.2;
        final double[][] rwMatrix = {
            { -0.6830127, -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, 0.6830127, -0.6830127, 0.6830127 },
            { -0.258819, -0.258819, 0.258819, 0.258819 }
        };
        final double tranquillisationTime = 15.;

        final IsisSpinBiasSlewComputer computer = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
                dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc,
                rwMatrix, tranquillisationTime, 25);
        final TabulatedSlew slewAnalytical = computer.computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical = computer.computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        slewAnalytical.setSpinDerivativesComputation(true);
        slewNumerical.setSpinDerivativesComputation(true);

        // Check numerical and analytical methods provide similar results
        for (int i = 0; i < 50; i++) {
            final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            Assert.assertEquals(0., Rotation.distance(attitude1.getRotation(), attitude2.getRotation()), 1E-2);
        }
        // Check last point
        final Attitude attitude1 = slewAnalytical.getAttitude(null, slewAnalytical.getTimeInterval().getUpperData(),
            FramesFactory.getGCRF());
        final Attitude attitude2 = slewNumerical.getAttitude(null, slewAnalytical.getTimeInterval().getUpperData(),
            FramesFactory.getGCRF());
        Assert.assertEquals(0., Rotation.distance(attitude1.getRotation(), attitude2.getRotation()), 1E-2);

        // Check slew duration
        Assert.assertEquals(50.568256, computer.computeDuration(null, initialLaw, initialDate, finalLaw, null), 1E-5);

        // Check acceleration is constant by phase
        for (int i = 1; i < 18; i++) {
            final double acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 3E-7);
            final double acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 4E-7);
        }
        for (int i = 22; i < 35; i++) {
            final double acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 2E-7);
            final double acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 3E-7);
        }

        // Graph for SVS: spin, rotation, rotation difference and quaternion
        // for (int i = 0; i <= 50; i++) {
        // final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Rotation rot1 = attitude1.getRotation();
        // final Rotation rot2 = attitude2.getRotation();
        // final Quaternion q2 = rot2.getQuaternion();
        // System.out.println(i + " " + FastMath.toDegrees(attitude1.getSpin().getNorm()) + " " +
        // FastMath.toDegrees(attitude2.getSpin().getNorm())
        // + " " + FastMath.toDegrees(rot1.getAngle()) + " " + FastMath.toDegrees(rot2.getAngle())
        // + " " + (FastMath.toDegrees(rot2.getAngle()) - FastMath.toDegrees(rot1.getAngle()))
        // + " " + q2.getQ0() + " " + q2.getQ1() + " " + q2.getQ2() + " " + q2.getQ3());
        // }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ISIS_SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getAttitude(Orbit)}
     * 
     * @description This test checks ISIS spin bias slew second case (with constant velocity phase)
     * 
     * @input {@link IsisAnalyticalSpinBiasSlew} data (both numerical and analytical)
     * 
     * @output the computed slews
     * 
     * @testPassCriteria quaternion difference on the whole ephemeris between the two slews is small (absolute
     *                   tolerance: 1E-3)
     *                   Slew duration is as expected (tolerance: 0.)
     *                   Acceleration has the expected profile (tolerance: 1E-7, 1E-14 between the two phases
     *                   where acceleration is null)
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase2() throws PatriusException {

        Report.printMethodHeader("testCase2", "Quaternion computation", "PATRIUS v3.3", 0., ComparisonType.RELATIVE);

        // Initialization
        final AttitudeLaw initialLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(new Vector3D(1.,
            1., 1.), MathLib.toRadians(0.)));
        final AttitudeLaw finalLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(new Vector3D(1., 1.,
            1.), MathLib.toRadians(178.)));
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final double dtSCAO = 0.250;
        final double thetaMaxAllowed = MathLib.toRadians(179);
        final double durationMax = 200;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 4.2;
        final double[][] rwMatrix = {
            { -0.6830127, -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, 0.6830127, -0.6830127, 0.6830127 },
            { -0.258819, -0.258819, 0.258819, 0.258819 }
        };
        final double tranquillisationTime = 15.;

        final TabulatedSlew slewAnalytical = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        slewAnalytical.setSpinDerivativesComputation(true);
        slewNumerical.setSpinDerivativesComputation(true);

        // Check numerical and analytical methods provide similar results
        for (int i = 0; i < 177; i++) {
            final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            Assert.assertEquals(0., Rotation.distance(attitude1.getRotation(), attitude2.getRotation()), 5E-3);
        }

        // Check slew duration
        Assert.assertEquals(177.040490, new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null), 1E-5);
        Assert.assertEquals(177.040490, new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null), 1E-5);

        // Check acceleration is constant by phase
        double acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        double acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();

        for (int i = 1; i < 27; i++) {
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 5E-7);
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 1E-6);
        }

        acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        for (int i = 135; i < 162; i++) {
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 7E-6);
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 2E-5);
        }

        // Check acceleration is 0. between the two phases
        // so the spin is constant and maximum in that case, must be equals to the one expected
        for (int i = 29; i < 134; i++) {
            final double omegaMax = MathLib.toDegrees(slewAnalytical
                .getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF()).getSpin().getNorm());
            final double acc0Ana = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            final double acc0Num = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();

            Assert.assertEquals(1.327957, omegaMax, 1E-6);
            Assert.assertEquals(0., acc0Ana, 7E-14);
            Assert.assertEquals(0., acc0Num, 7E-14);
        }

        // Graph for SVS: spin, rotation, rotation difference and quaternion
        // for (int i = 0; i < 177; i++) {
        // final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Rotation rot1 = attitude1.getRotation();
        // final Rotation rot2 = attitude2.getRotation();
        // final Quaternion q2 = rot2.getQuaternion();
        // System.out.println(i + " " + FastMath.toDegrees(attitude1.getSpin().getNorm()) + " " +
        // FastMath.toDegrees(attitude2.getSpin().getNorm())
        // + " " + FastMath.toDegrees(rot1.getAngle()) + " " + FastMath.toDegrees(rot2.getAngle())
        // + " " + (FastMath.toDegrees(rot2.getAngle()) - FastMath.toDegrees(rot1.getAngle()))
        // + " " + q2.getQ0() + " " + q2.getQ1() + " " + q2.getQ2() + " " + q2.getQ3());
        // }

        // Non-regression tests
        final Attitude act1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(10.), FramesFactory.getGCRF());
        final Attitude act2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(10.), FramesFactory.getGCRF());
        final Attitude ref1 = new Attitude(initialDate.shiftedBy(10.), FramesFactory.getGCRF(), new AngularCoordinates(
            new Rotation(false, 0.9997858879063762, 0.01194680352310178, 0.01194680352310178, 0.01194680352310178),
            new Vector3D(0.004779062499135405, 0.004779062499135405, 0.004779062499135405),
            new Vector3D(4.7790621808045983E-4, 4.7790621808045983E-4, 4.7790621808045983E-4)));
        final Attitude ref2 = new Attitude(initialDate.shiftedBy(10.), FramesFactory.getGCRF(), new AngularCoordinates(
            new Rotation(false, 0.9997750488880448, 0.012245429363755191, 0.012245429363755191,
                0.012245429363755191),
            new Vector3D(0.004779062499135403, 0.004779062499135403, 0.004779062499135403),
            new Vector3D(4.779063154475612E-4, 4.779063154475612E-4, 4.779063154475612E-4)));
        this.checkAttitudes(act1, ref1);
        this.checkAttitudes(act2, ref2);
        Report.printToReport("Analytical - Rotation", act1.getRotation(), ref1.getRotation());
        Report.printToReport("Numerical - Rotation", act2.getRotation(), ref2.getRotation());
        Report.printToReport("Analytical - Spin", act1.getSpin(), ref1.getSpin());
        Report.printToReport("Numerical - Spin", act2.getSpin(), ref2.getSpin());
        Report.printToReport("Analytical - Acceleration", act1.getRotationAcceleration(),
            ref1.getRotationAcceleration());
        Report
            .printToReport("Numerical - Acceleration", act2.getRotationAcceleration(), ref2.getRotationAcceleration());
    }

    /**
     * Check attitudes (non-regression)
     * 
     * @param attitude1
     *        attitude 1
     * @param attitude2
     *        attitude 2
     */
    private void checkAttitudes(final Attitude attitude1, final Attitude attitude2) throws PatriusException {
        final Rotation rot1 = attitude1.getRotation();
        final Rotation rot2 = attitude2.getRotation();

        final double threshold = 1e-14;
        Assert.assertEquals(rot1.getQuaternion().getQ0(), rot2.getQuaternion().getQ0(), threshold);
        Assert.assertEquals(rot1.getQuaternion().getQ1(), rot2.getQuaternion().getQ1(), threshold);
        Assert.assertEquals(rot1.getQuaternion().getQ2(), rot2.getQuaternion().getQ2(), threshold);
        Assert.assertEquals(rot1.getQuaternion().getQ3(), rot2.getQuaternion().getQ3(), threshold);

        Assert.assertEquals(0., attitude1.getSpin().subtract(attitude2.getSpin()).getNorm(), threshold);
        Assert.assertEquals(0., attitude1.getRotationAcceleration().subtract(attitude2.getRotationAcceleration())
            .getNorm(), threshold);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ISIS_SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getAttitude(Orbit)}
     * 
     * @description This test checks ISIS spin bias slew third case (with constant velocity phase)
     * 
     * @input {@link IsisAnalyticalSpinBiasSlew} data (both numerical and analytical)
     * 
     * @output the computed slews
     * 
     * @testPassCriteria quaternion difference on the whole ephemeris between the two slews is small (absolute
     *                   tolerance: 1E-3)
     *                   Slew duration is as expected (tolerance: 0.)
     *                   Acceleration has the expected profile (tolerance: 1E-7, 1E-13 between the two phases
     *                   where acceleration is null))
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testCase3() throws PatriusException {

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AttitudeLaw initialLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(new Vector3D(1.,
            1., 1.), MathLib.toRadians(20.)));
        final AttitudeLaw underlyingLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final AttitudeLaw finalLaw = new SpinStabilized(underlyingLaw, AbsoluteDate.J2000_EPOCH, new Vector3D(1., 1.,
            1.), MathLib.toRadians(1.));
        final double dtSCAO = 0.125;
        final double thetaMaxAllowed = MathLib.toRadians(179);
        final double durationMax = 200;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 5.3;
        final double[][] rwMatrix = {
            { -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, -0.6830127, 0.6830127 },
            { -0.258819, 0.258819, 0.258819 }
        };
        final double tranquillisationTime = 15.;

        final TabulatedSlew slewAnalytical = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        slewAnalytical.setSpinDerivativesComputation(true);
        slewNumerical.setSpinDerivativesComputation(true);

        // Check numerical and analytical methods provide similar results
        for (int i = 0; i < 128; i++) {
            final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            Assert.assertEquals(0., Rotation.distance(attitude1.getRotation(), attitude2.getRotation()), 3E-3);
        }

        // Check slew duration
        Assert.assertEquals(128.690518, new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null), 5E-4);
        Assert.assertEquals(128.690518, new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null), 5E-4);

        // Check acceleration is constant by phase
        double acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        double acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(1), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        for (int i = 1; i < 27; i++) {
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 2E-7);
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 3E-7);
        }

        acc01 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        acc02 = slewNumerical.getAttitude(null, initialDate.shiftedBy(22), FramesFactory.getGCRF())
            .getRotationAcceleration().getNorm();
        for (int i = 86; i < 113; i++) {
            final double acci1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc01 - acci1) / acc01, 2E-6);
            final double acci2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., (acc02 - acci2) / acc02, 3E-6);
        }

        // Check acceleration is null between these phases
        // spin is constant and maximum in that case, must be equals to the expected one
        for (int i = 36; i < 79; i++) {
            final double omegaMax = MathLib.toDegrees(slewAnalytical
                .getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF()).getSpin().getNorm());
            Assert.assertEquals(1.324382, omegaMax, 1E-6);
            final double acc0Ana = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., acc0Ana, 2E-13);
            final double acc0Num = slewNumerical.getAttitude(null, initialDate.shiftedBy(i), FramesFactory.getGCRF())
                .getRotationAcceleration().getNorm();
            Assert.assertEquals(0., acc0Num, 2E-13);
        }

        // Graph for SVS: spin, rotation, rotation difference and quaternion
        // for (int i = 0; i < 128; i++) {
        // final Attitude attitude1 = slewAnalytical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Attitude attitude2 = slewNumerical.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Rotation rot1 = attitude1.getRotation();
        // final Rotation rot2 = attitude2.getRotation();
        // final Quaternion q2 = rot2.getQuaternion();
        // System.out.println(i + " " + FastMath.toDegrees(attitude1.getSpin().getNorm()) + " " +
        // FastMath.toDegrees(attitude2.getSpin().getNorm())
        // + " " + FastMath.toDegrees(rot1.getAngle()) + " " + FastMath.toDegrees(rot2.getAngle())
        // + " " + (FastMath.toDegrees(rot2.getAngle()) - FastMath.toDegrees(rot1.getAngle()))
        // + " " + q2.getQ0() + " " + q2.getQ1() + " " + q2.getQ2() + " " + q2.getQ3());
        // }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#ISIS_SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getAttitude(Orbit)}
     * 
     * @description This test checks ISIS spin bias slew third case (with constant velocity phase) returns same
     *              ephemeris wether it is used
     *              with initial date or with final date (= initial date + slew duration) (both numerical and analytical
     *              versions)
     * 
     * @input {@link IsisAnalyticalSpinBiasSlew} data (both numerical and analytical)
     * 
     * @output the computed slews
     * 
     * @testPassCriteria attitude (rotation/spin/acceleration) difference on the whole ephemeris between the two slews
     *                   is 1E-3 (both for numerical and analytical slews)
     *                   (accuracy is driven by dtConvergenceThreshold = 0.1s value). A lower value will result in
     *                   closer ephemeris
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public final void testCase4() throws PatriusException {

        Report.printMethodHeader("testCase4", "Quaternion computation (reverse)", "PATRIUS v3.4", 0.,
            ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        final AbsoluteDate finalDate = initialDate.shiftedBy(128.6903627283653);
        final AttitudeLaw underlyingLaw1 = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final AttitudeLaw initialLaw = new SpinStabilized(underlyingLaw1, AbsoluteDate.J2000_EPOCH.shiftedBy(-30.),
            new Vector3D(1., 1., 1.), MathLib.toRadians(0.5));
        final AttitudeLaw underlyingLaw2 = new ConstantAttitudeLaw(FramesFactory.getGCRF(), Rotation.IDENTITY);
        final AttitudeLaw finalLaw = new SpinStabilized(underlyingLaw2, AbsoluteDate.J2000_EPOCH, new Vector3D(1., 1.,
            1.), MathLib.toRadians(1.));
        final double dtSCAO = 0.125;
        final double thetaMaxAllowed = MathLib.toRadians(179);
        final double durationMax = 200;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 5.3;
        final double[][] rwMatrix = {
            { -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, -0.6830127, 0.6830127 },
            { -0.258819, 0.258819, 0.258819 }
        };
        final double tranquillisationTime = 15.;

        final TabulatedSlew slewAnalyticalRef = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumericalRef = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewAnalyticalAct = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeAnalytical(null, initialLaw, null, finalLaw, finalDate);
        final TabulatedSlew slewNumericalAct = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeNumerical(null, initialLaw, null, finalLaw, finalDate);
        slewAnalyticalRef.setSpinDerivativesComputation(true);
        slewNumericalRef.setSpinDerivativesComputation(true);
        slewAnalyticalAct.setSpinDerivativesComputation(true);
        slewNumericalAct.setSpinDerivativesComputation(true);

        // Check slew duration (accuracy driven by dtConvergenceThreshold accuracy)
        Assert.assertEquals(new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null),
                new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                        inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                        tranquillisationTime).computeDuration(null, initialLaw, null, finalLaw, finalDate), 1E-1);
        Assert.assertEquals(new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                tranquillisationTime).computeDuration(null, initialLaw, initialDate, finalLaw, null),
                new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                        inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                        tranquillisationTime).computeDuration(null, initialLaw, null, finalLaw, finalDate), 1E-1);

        // Check results are identical (accuracy driven by dtConvergenceThreshold accuracy)
        for (int i = 1; i < 128; i++) {
            final Attitude attitudeAnalyticalRef = slewAnalyticalRef.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitudeNumericalRef = slewNumericalRef.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitudeAnalyticalAct = slewAnalyticalAct.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            final Attitude attitudeNumericalAct = slewNumericalAct.getAttitude(null, initialDate.shiftedBy(i),
                FramesFactory.getGCRF());
            Assert.assertEquals(0.,
                Rotation.distance(attitudeAnalyticalRef.getRotation(), attitudeAnalyticalAct.getRotation()), 2E-3);
            Assert.assertEquals(0.,
                Rotation.distance(attitudeNumericalRef.getRotation(), attitudeNumericalAct.getRotation()), 2E-3);
            Assert.assertEquals(0.,
                attitudeAnalyticalRef.getSpin().subtract(attitudeAnalyticalAct.getSpin()).getNorm(), 5E-5);
            Assert.assertEquals(0., attitudeNumericalRef.getSpin().subtract(attitudeNumericalAct.getSpin()).getNorm(),
                5E-5);
            Assert.assertEquals(
                0.,
                attitudeAnalyticalRef.getRotationAcceleration()
                    .subtract(attitudeAnalyticalAct.getRotationAcceleration()).getNorm(), 5E-4);
            Assert.assertEquals(
                0.,
                attitudeNumericalRef.getRotationAcceleration()
                    .subtract(attitudeNumericalAct.getRotationAcceleration()).getNorm(), 5E-4);
        }

        // Non-regression tests
        final Attitude act1 = slewAnalyticalAct.getAttitude(null, initialDate.shiftedBy(10.), FramesFactory.getGCRF());
        final Attitude act2 = slewNumericalAct.getAttitude(null, initialDate.shiftedBy(10.), FramesFactory.getGCRF());
        final Attitude ref1 = new Attitude(initialDate.shiftedBy(10.), FramesFactory.getGCRF(), new AngularCoordinates(
            new Rotation(false, 0.9818209514705973, 0.10958652480022502, 0.10958652480022502, 0.10958652480022502),
            new Vector3D(0.003749521231830011, 0.003749521231830011, 0.003749521231830011),
            new Vector3D(3.7769851116348837E-4, 3.7769851116348837E-4, 3.7769851116348837E-4)));
        final Attitude ref2 = new Attitude(initialDate.shiftedBy(10.), FramesFactory.getGCRF(), new AngularCoordinates(
            new Rotation(false, 0.9817824289638493, 0.10970150740052718, 0.10970150740052718, 0.10970150740052718),
            new Vector3D(0.003783984927412917, 0.003783984927412917, 0.003783984927412917),
            new Vector3D(5.628588204722161E-4, 5.628588204722161E-4, 5.628588204722161E-4)));
        this.checkAttitudes(act1, ref1);
        this.checkAttitudes(act2, ref2);
        Report.printToReport("Analytical - Rotation", act1.getRotation(), ref1.getRotation());
        Report.printToReport("Numerical - Rotation", act2.getRotation(), ref2.getRotation());
        Report.printToReport("Analytical - Spin", act1.getSpin(), ref1.getSpin());
        Report.printToReport("Numerical - Spin", act2.getSpin(), ref2.getSpin());
        Report.printToReport("Analytical - Acceleration", act1.getRotationAcceleration(),
            ref1.getRotationAcceleration());
        Report
            .printToReport("Numerical - Acceleration", act2.getRotationAcceleration(), ref2.getRotationAcceleration());

        // Graph for SVS: spin, rotation, rotation difference and quaternion
        // for (int i = 1; i < 128; i++) {
        // final Attitude attitude1 = slewAnalyticalAct.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Attitude attitude2 = slewNumericalAct.getAttitude(null, initialDate.shiftedBy(i),
        // FramesFactory.getGCRF());
        // final Rotation rot1 = attitude1.getRotation();
        // final Rotation rot2 = attitude2.getRotation();
        // final Quaternion q2 = rot2.getQuaternion();
        // System.out.println(i + " " + FastMath.toDegrees(attitude1.getSpin().getNorm()) + " " +
        // FastMath.toDegrees(attitude2.getSpin().getNorm())
        // + " " + FastMath.toDegrees(rot1.getAngle()) + " " + FastMath.toDegrees(rot2.getAngle())
        // + " " + (FastMath.toDegrees(rot2.getAngle()) - FastMath.toDegrees(rot1.getAngle()))
        // + " " + q2.getQ0() + " " + q2.getQ1() + " " + q2.getQ2() + " " + q2.getQ3());
        // }
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ISIS_SPIN_BIAS_SLEW}
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getAttitude(Orbit)}
     * 
     * @description This test checks an exception is raised if required angular rate is too high
     * 
     * @input {@link IsisAnalyticalSpinBiasSlew} data
     * 
     * @output exception
     * 
     * @testPassCriteria an exception is thrown
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     */
    @Test
    public final void testException() throws PatriusException {

        // Initialization
        final AttitudeLaw initialLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(new Vector3D(1.,
            1., 1.), MathLib.toRadians(20.)));
        final AttitudeLaw finalLaw = new ConstantAttitudeLaw(FramesFactory.getGCRF(), new Rotation(new Vector3D(1., 1.,
            1.), MathLib.toRadians(35.)));
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH;
        final double dtSCAO = 0.250;
        final double thetaMaxAllowed = 0.01;
        final double durationMax = 180;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 4.2;
        final double[][] rwMatrix = {
            { -0.6830127, -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, 0.6830127, -0.6830127, 0.6830127 },
            { -0.258819, -0.258819, 0.258819, 0.258819 }
        };
        final double tranquillisationTime = 15.;

        // Slew too fast
        try {
            final Slew slewAnalytical = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax, dtConvergenceThreshold,
                    inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
                    tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
            slewAnalytical.getAttitude(null, initialDate, FramesFactory.getGCRF());
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Date out of bounds
        final Slew slew2 = new IsisSpinBiasSlewComputer(
            dtSCAO, MathLib.toRadians(179), durationMax, dtConvergenceThreshold,
            inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel, rwDeltaMomentumAlloc, rwMatrix,
            tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        try {
            slew2.getAttitude(null, initialDate.shiftedBy(-10), FramesFactory.getGCRF());
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature none
     * 
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#IsisAnalyticalSpinBiasSlew(AttitudeProvider, AttitudeProvider, AbsoluteDate, TypeOfDate, double, double, double, double, double[][], double, double, double, double[][], double)}
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#IsisAnalyticalSpinBiasSlew(AttitudeProvider, AttitudeProvider, AbsoluteDate, TypeOfDate, double, double, double, double, double[][], double, double, double, double[][], double, int)}
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#IsisAnalyticalSpinBiasSlew(AttitudeProvider, AttitudeProvider, AbsoluteDate, TypeOfDate, double, double, double, double, double[][], double, double, double, double[][], double, String)}
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#IsisAnalyticalSpinBiasSlew(AttitudeProvider, AttitudeProvider, AbsoluteDate, TypeOfDate, double, double, double, double, double[][], double, double, double, double[][], double, int, String)}
     * @testedMethod {@link IsisAnalyticalSpinBiasSlew#getNature()}
     * 
     * @description Test the new constructors which add the "nature" attribute
     * 
     * @input parameters
     * 
     * @output AbstractIsisSpinBiasSlew
     * 
     * @testPassCriteria The nature attribute is well managed
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void FT2105() throws PatriusException {

        Report.printMethodHeader("testCase4", "Quaternion computation (reverse)", "PATRIUS v3.4",
            0., ComparisonType.RELATIVE);

        // Initialization
        final AbsoluteDate initialDate = AbsoluteDate.J2000_EPOCH.shiftedBy(10.);
        initialDate.shiftedBy(128.6903627283653);
        final AttitudeLaw underlyingLaw1 = new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY);
        final AttitudeLaw initialLaw = new SpinStabilized(underlyingLaw1,
            AbsoluteDate.J2000_EPOCH.shiftedBy(-30.), new Vector3D(1., 1., 1.),
            MathLib.toRadians(0.5));
        final AttitudeLaw underlyingLaw2 = new ConstantAttitudeLaw(FramesFactory.getGCRF(),
            Rotation.IDENTITY);
        final AttitudeLaw finalLaw = new SpinStabilized(underlyingLaw2, AbsoluteDate.J2000_EPOCH,
            new Vector3D(1., 1., 1.), MathLib.toRadians(1.));
        final double dtSCAO = 0.125;
        final double thetaMaxAllowed = MathLib.toRadians(179);
        final double durationMax = 200;
        final double dtConvergenceThreshold = 0.1;
        final double[][] inertiaMatrix = { { 305, 0, 0 }, { 0, 315, 0 }, { 0, 0, 90 } };
        final double rwTorqueAllocAccel = 0.15;
        final double rwTorqueAllocDecel = 0.15;
        final double rwDeltaMomentumAlloc = 5.3;
        final double[][] rwMatrix = { { -0.6830127, -0.6830127, -0.6830127 },
            { -0.6830127, -0.6830127, 0.6830127 }, { -0.258819, 0.258819, 0.258819 } };
        final double tranquillisationTime = 15.;
        final int maxIterationsNumberIn = 25;

        final String DEFAULT_NATURE1 = "ATTITUDE_ISIS_SPIN_BIAS_SLEW";
        final String DEFAULT_NATURE2 = "ATTITUDE_ISIS_SPIN_BIAS_SLEW";
        final String nature = "testNature";

        // Test all the 4 constructors of IsisAnalyticalSpinBiasSlew class
        final TabulatedSlew slewAnalytical1 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewAnalytical2 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, maxIterationsNumberIn).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewAnalytical3 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, nature).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewAnalytical4 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, maxIterationsNumberIn, nature).computeAnalytical(null, initialLaw, initialDate, finalLaw, null);

        Assert.assertEquals(slewAnalytical1.getNature(), DEFAULT_NATURE1);
        Assert.assertEquals(slewAnalytical2.getNature(), DEFAULT_NATURE1);
        Assert.assertEquals(slewAnalytical3.getNature(), nature);
        Assert.assertEquals(slewAnalytical4.getNature(), nature);

        // Test all the 4 constructors of IsisNumericalSpinBiasSlew class
        final TabulatedSlew slewNumerical1 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical2 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, maxIterationsNumberIn).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical3 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, nature).computeNumerical(null, initialLaw, initialDate, finalLaw, null);
        final TabulatedSlew slewNumerical4 = new IsisSpinBiasSlewComputer(dtSCAO, thetaMaxAllowed, durationMax,
            dtConvergenceThreshold, inertiaMatrix, rwTorqueAllocAccel, rwTorqueAllocDecel,
            rwDeltaMomentumAlloc, rwMatrix, tranquillisationTime, maxIterationsNumberIn, nature).computeNumerical(null, initialLaw, initialDate, finalLaw, null);

        Assert.assertEquals(slewNumerical1.getNature(), DEFAULT_NATURE2);
        Assert.assertEquals(slewNumerical2.getNature(), DEFAULT_NATURE2);
        Assert.assertEquals(slewNumerical3.getNature(), nature);
        Assert.assertEquals(slewNumerical4.getNature(), nature);
    }
}
