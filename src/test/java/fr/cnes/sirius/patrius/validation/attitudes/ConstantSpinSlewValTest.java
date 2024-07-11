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
 * @history created 10/02/12
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.7:DM:DM-2653:18/05/2021:generalisation des sequences et correction/refonte des sequences de segments 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:210:12/03/2014:Modified slerp algorithm to take shortest path
 * VERSION::FA:271:05/09/2014:Definitions anomalies LVLH and VVLH
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::FA:1870:05/10/2018:remove slew recomputation in getAttitude() method
 * VERSION::DM:1948:14/11/2018:new attitude leg sequence design
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.validation.attitudes;

import static fr.cnes.sirius.patrius.math.util.MathLib.sqrt;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.attitudes.Attitude;
import fr.cnes.sirius.patrius.attitudes.AttitudeLaw;
import fr.cnes.sirius.patrius.attitudes.AttitudeLawLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeLeg;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.BodyCenterPointing;
import fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew;
import fr.cnes.sirius.patrius.attitudes.FixedRate;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.attitudes.Slew;
import fr.cnes.sirius.patrius.attitudes.StrictAttitudeLegsSequence;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.math.complex.Quaternion;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.RotationOrder;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class validates the constant spin slew.
 * The tests are based on quaternion and attitude comparison, not suitable for use
 * with the Validate class - which is therefore not used.
 * 
 * @see ConstantSpinSlew
 * 
 * @author Julie Anton
 * 
 * @version $Id: ConstantSpinSlewValidationTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 * @since 1.1
 * 
 */
public class ConstantSpinSlewValTest {

    /** Epsilon comparison */
    private static final double EPS = Precision.DOUBLE_COMPARISON_EPSILON;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle constant spin slew validation
         * 
         * @featureDescription analytical validation for the constant spin slew
         * 
         * @coveredRequirements DV-ATT_290, DV-ATT_291
         */
        ANALYTICAL_VALIDATION,
        /**
         * @featureTitle constant spin slew validation
         * 
         * @featureDescription validation of the constant spin slew inside a sequence
         * 
         * @coveredRequirements DV-ATT_290, DV-ATT_291
         */
        SEQUENCE
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_VALIDATION}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, Frame, int)}
     * 
     * @description this is a test which validates the constant spin slew computations on a simple case.
     * 
     * @input the initial quaternion is (0,1,0,0), the final one is (0,0,1,0) and is the result of the rotation of angle
     *        90°.
     * 
     * @output intermediate quaternions : the first one is the result of the rotation of angle 30°, the second one is
     *         the result of another rotation of angle 30°.
     * 
     * @testPassCriteria the first intermediate quaternion should be equal to (0, -sqrt(3)/2, +1/2, 0) ; the second
     *                   intermediate quaternion should be equal to (0, -1/2, +sqrt(3)/2, 0). The minus signs are due
     *                   to the fact that the final quaternion considered for the rotation is opposed (-q1 instead
     *                   of q1) in order to go from q0 to q1 following the short way.
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSimpleCase() throws PatriusException {

        // reference date
        final AbsoluteDate refDate = new AbsoluteDate(2012, 2, 10, TimeScalesFactory.getTAI());
        // reference frame
        final Frame refFrame = FramesFactory.getEME2000();

        // initial attitude
        final Quaternion qInitial = new Quaternion(0, 1, 0, 0);
        final Attitude attInitial = new Attitude(refDate, refFrame,
            new Rotation(false, qInitial), Vector3D.ZERO);
        final AttitudeProvider iLaw = new FixedRate(attInitial);

        // final attitude
        final Quaternion qFinal = new Quaternion(0, 0, 1, 0);
        final Attitude attFinal = new Attitude(refDate.shiftedBy(60.), refFrame, new Rotation(false,
            qFinal), Vector3D.ZERO);
        final AttitudeProvider fLaw = new FixedRate(attFinal);
        
        // Satellite position
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit circOrbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.),
                MathLib.toRadians(270.), MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(),
                refDate, mu);

        // Slew
        final Slew slerp = new ConstantSpinSlew(iLaw.getAttitude(circOrbit, refDate, refFrame), fLaw.getAttitude(circOrbit, refDate.shiftedBy(60), refFrame));

        Quaternion intermediateRot;
        Quaternion intermediateRefRot;
        boolean isEqual;

        // first intermediate rotation of 30°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(20.), refFrame)
            .getRotation().getQuaternion();
        intermediateRefRot = new Quaternion(0, MathLib.sqrt(3) / 2., -1 / 2., 0);
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        Assert.assertTrue(isEqual);

        // second intermediate rotation of 30°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(40.), refFrame)
            .getRotation().getQuaternion();
        intermediateRefRot = new Quaternion(0, 1 / 2., -MathLib.sqrt(3) / 2., 0);
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        Assert.assertTrue(isEqual);

        // third and last intermediate rotation of 30°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(60.), refFrame)
            .getRotation().getQuaternion();
        intermediateRefRot = new Quaternion(0, 0, 1, 0);
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        Assert.assertTrue(isEqual);
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_VALIDATION}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame, int)}
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description this is a test which validates the constant spin slew computations on a general case.
     * 
     * @input the initial quaternion is (0.25, -sqrt(3)/4, 0, sqrt(3)/2), the final one is (-sqrt(3)/8, (3 + 2
     *        sqrt(3))/8, 0.125, (sqrt(3) - 6)/8) and is the result of the rotation of angle 150°.
     * 
     * @output 4 intermediate quaternions all given by successive rotations of angle 30° (25° for the last)
     * 
     * @testPassCriteria the intermediate quaternions should be equal to references calculated with an alternative
     *                   formula based on rotations (q_0 \( q_0^{-1} q_1) ^ t \)) to a 1e-14 absolute threshold for
     *                   quaternion components
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testGeneralCase() throws PatriusException {

        final Quaternion q0 = new Quaternion(0.25, -sqrt(3) / 4, 0, sqrt(3) / 2);
        final Quaternion q1 = new Quaternion(-sqrt(3) / 8, (3 + 2 * sqrt(3)) / 8, 0.125, (sqrt(3) - 6) / 8);

        final Rotation r0 = new Rotation(false, q0);
        final Rotation r1 = new Rotation(false, q1);

        // reference date
        final AbsoluteDate refDate = new AbsoluteDate(2012, 2, 10, TimeScalesFactory.getTAI());
        // refernece frame
        final Frame refFrame = FramesFactory.getEME2000();

        // initial attitude
        final Attitude attInitial = new Attitude(refDate, refFrame,
            r0, Vector3D.ZERO);
        final AttitudeProvider iLaw = new FixedRate(attInitial);

        // final attitude
        final Attitude attFinal = new Attitude(refDate.shiftedBy(120.), refFrame, r1, Vector3D.ZERO);
        final AttitudeProvider fLaw = new FixedRate(attFinal);

        // maneuver profile
        final Slew slerp = new ConstantSpinSlew(attInitial, attFinal);

        double t;
        final Rotation r = r0.applyInverseTo(r1);

        Quaternion intermediateRot;
        Quaternion intermediateRefRot;
        boolean isEqual;

        // Satellite position
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit circOrbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.),
            MathLib.toRadians(270.), MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(),
            refDate, mu);

        /*
         * first intermediate rotation of 30°
         */
        // computed quaternion
        intermediateRot = slerp.getAttitude(circOrbit, refDate.shiftedBy(24.), refFrame).getRotation().getQuaternion();

        // reference quaternion
        t = .2;
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        // assertion
        Assert.assertTrue(isEqual);

        /*
         * second intermediate rotation of 60°
         */
        // computed quaternion
        intermediateRot = slerp.getAttitude(circOrbit, refDate.shiftedBy(48.), refFrame).getRotation().getQuaternion();

        // reference quaternion
        t = .4;
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        // assertion
        Assert.assertTrue(isEqual);

        /*
         * third intermediate rotation of 90°
         */
        // computed quaternion
        intermediateRot = slerp.getAttitude(circOrbit, refDate.shiftedBy(72.), refFrame).getRotation().getQuaternion();

        // reference quaternion
        t = .6;
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        // assertion
        Assert.assertTrue(isEqual);

        /*
         * fourth intermediate rotation of 120°
         */
        // computed quaternion
        intermediateRot = slerp.getAttitude(circOrbit, refDate.shiftedBy(96.), refFrame).getRotation().getQuaternion();

        // reference quaternion
        t = .8;
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        // assertion
        Assert.assertTrue(isEqual);

        /*
         * fifth intermediate rotation of 145°
         */
        // computed quaternion
        intermediateRot = slerp.getAttitude(circOrbit, refDate.shiftedBy(145 / 150. * 120), refFrame).getRotation()
            .getQuaternion();

        // reference quaternion
        t = 145 / 150.;
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();
        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        // assertion
        Assert.assertTrue(isEqual);
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_VALIDATION}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, Frame, int)}
     * 
     * @description this is a test which validates the constant spin slew computations on a limit case : small angle.
     * 
     * @input the initial quaternion is (0.25, -sqrt(3)/4, 0, sqrt(3)/2), the final one is (-sqrt(3)/8, (3 + 2
     *        sqrt(3))/8, 0.125, (sqrt(3) - 6)/8) and is the result of the rotation of angle 150°.
     * 
     * @output 2 intermediate quaternions : the first one given by the rotation of angle 0.1°, the second one given buy
     *         the rotation of angle 1°.
     * 
     * @testPassCriteria the constant spin slew profile should gives the expected results.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testSmallAngleCase() throws PatriusException {

        // reference date
        final AbsoluteDate refDate = new AbsoluteDate(2012, 2, 10, TimeScalesFactory.getTAI());
        // refernece frame
        final Frame refFrame = FramesFactory.getEME2000();

        // initial attitude
        final Quaternion qInitial = new Quaternion(1 / 4., -MathLib.sqrt(3) / 4., 0., MathLib.sqrt(3) / 2.);
        final Attitude attInitial = new Attitude(refDate, refFrame,
            new Rotation(false, qInitial), Vector3D.ZERO);
        final AttitudeProvider iLaw = new FixedRate(attInitial);

        // final attitude
        final Quaternion qFinal = new Quaternion(MathLib.sqrt(3) / 8., (-3. + 2 * MathLib.sqrt(3)) / 8., 1. / 8.,
            (MathLib.sqrt(3) + 6) / 8.);
        final Attitude attFinal = new Attitude(refDate.shiftedBy(300.), refFrame, new Rotation(false,
            qFinal), Vector3D.ZERO);
        final AttitudeProvider fLaw = new FixedRate(attFinal);

        // maneuver profile
        final Slew slerp = new ConstantSpinSlew(attInitial, attFinal);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit circOrbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.),
            MathLib.toRadians(270.), MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(),
            refDate, mu);

        // small angle : 0.1°
        final double theta = MathLib.toRadians(0.1);
        final Quaternion qRot = new Quaternion(MathLib.cos(theta), 0, MathLib.sin(theta), 0);

        Rotation intermediateRot;
        Rotation intermediateRefRot;
        Quaternion intermediateRefQuat;
        double isEqual;

        // intermediate rotation of 0.1°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(1.), refFrame)
            .getRotation();
        intermediateRefRot = new Rotation(false, Quaternion.multiply(qRot, qInitial));
        isEqual = Rotation.distance(intermediateRefRot, intermediateRot);

        Assert.assertEquals(0., isEqual, EPS);

        // intermediate rotation of 1°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(10.), refFrame)
            .getRotation();
        intermediateRefQuat = intermediateRefRot.getQuaternion();
        for (int i = 1; i < 10; i++) {
            intermediateRefQuat = Quaternion.multiply(qRot, intermediateRefQuat);
        }
        intermediateRefRot = new Rotation(false, intermediateRefQuat);
        isEqual = Rotation.distance(intermediateRefRot, intermediateRot);

        Assert.assertEquals(0., isEqual, EPS);
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_VALIDATION}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(AbsoluteDate, Frame, int)}
     * 
     * @description this is a test which validates the constant spin slew computations on a limit case : large angle.
     * 
     * @input the initial quaternion is (0.25, -sqrt(3)/4, 0, sqrt(3)/2), the final one is the result of the rotation of
     *        angle 179°.
     * 
     * @output intermediate quaternion given by the rotation of angle 178°.
     * 
     * @testPassCriteria the intermediate quaternion should be equal to reference calculated with an alternative
     *                   formula based on rotations (q_0 \( q_0^{-1} q_1) ^ t \)) to a 1e-14 absolute threshold for
     *                   quaternion components
     * 
     * @referenceVersion 2.2
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testLargeAngleCase() throws PatriusException {

        // reference date
        final AbsoluteDate refDate = new AbsoluteDate(2012, 2, 10, TimeScalesFactory.getTAI());
        // reference frame
        final Frame refFrame = FramesFactory.getEME2000();

        // initial attitude
        final Quaternion qInitial = new Quaternion(1 / 4., -MathLib.sqrt(3) / 4., 0., MathLib.sqrt(3) / 2.);
        final Rotation r0 = new Rotation(false, qInitial);
        final Attitude attInitial = new Attitude(refDate, refFrame, r0, Vector3D.ZERO);
        final AttitudeProvider iLaw = new FixedRate(attInitial);

        // large angle : 179° (near to PI)
        final double theta = MathLib.toRadians(179);
        final Quaternion qRot = new Quaternion(MathLib.cos(theta), 0, MathLib.sin(theta), 0);

        // first intermediate rotation of 0.1°
        final Quaternion qFinal = Quaternion.multiply(qRot, qInitial);
        final Rotation r1 = new Rotation(false, qFinal);
        final Attitude attFinal = new Attitude(refDate.shiftedBy(44.75), refFrame, r1, Vector3D.ZERO);
        final AttitudeProvider fLaw = new FixedRate(attFinal);

        // maneuver profile
        final Slew slerp = new ConstantSpinSlew(attInitial, attFinal);

        // orbit
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit circOrbit = new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, MathLib.toRadians(50.),
            MathLib.toRadians(270.), MathLib.toRadians(5.300), PositionAngle.MEAN, FramesFactory.getEME2000(),
            refDate, mu);

        Quaternion intermediateRot;
        Quaternion intermediateRefRot;
        boolean isEqual;

        // intermediate rotation of 178°
        intermediateRot = slerp.getAttitude(refDate.shiftedBy(44.5), refFrame)
            .getRotation().getQuaternion();

        // reference
        final double t = 178 / 179.;
        final Rotation r = r0.applyInverseTo(r1);
        intermediateRefRot = r0.applyTo(new Rotation(r.getAxis(), r.getAngle() * t)).getQuaternion();

        isEqual = intermediateRefRot.equals(intermediateRot, EPS);

        Assert.assertTrue(isEqual);
    }

    /**
     * @throws PatriusException
     * @testType TVT
     * 
     * @testedFeature {@link features#SEQUENCE}
     * 
     * @testedMethod {@link fr.cnes.sirius.patrius.attitudes.ConstantSpinSlew#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link StrictAttitudeLegsSequence#add(AttitudeLeg)}
     * @testedMethod {@link StrictAttitudeLegsSequence#getAttitude(fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description tests that the constant spin slew between two laws is correctly added to the sequence.
     * 
     * @input two laws and a constant spin slew in between
     * 
     * @output Attitude
     * 
     * @testPassCriteria the computed attitude at the junction between the law and the constant spin slew should be the
     *                   same either
     *                   if one calls getAttitude() on the law, on the constant spin slew or on the sequence.
     * 
     * @referenceVersion 1.1
     * 
     * @nonRegressionVersion 1.1
     */
    @Test
    public void testSequence() throws PatriusException {
        final Frame gcrf = FramesFactory.getGCRF();

        // attitude laws
        final AttitudeLaw law1 = new BodyCenterPointing(gcrf);
        final AttitudeLaw law2 = new LofOffset(gcrf, LOFType.LVLH, RotationOrder.ZXY, 0, MathLib.toRadians(20), 0);

        // orbit
        final AbsoluteDate date = new AbsoluteDate(2012, 3, 7, 12, 02, 0.0, TimeScalesFactory.getTT());
        final double mu = Constants.EGM96_EARTH_MU;
        final Orbit leo = new KeplerianOrbit(7200000, 0.001, MathLib.toRadians(40), 0, 0, 0, PositionAngle.MEAN, gcrf,
            date, mu);

        // constant spin slew
        final Slew slerp = new ConstantSpinSlew(law1.getAttitude(leo, date.shiftedBy(3 * 3600.), gcrf),
                law2.getAttitude(leo, date.shiftedBy(3 * 3600. + 300.), gcrf));

        // constant spin slew interval of validity (util to create the law legs of the sequence)
        final AbsoluteDateInterval slerpInterval = slerp.getTimeInterval();

        // first law leg
        final AttitudeLeg lawLeg1 = new AttitudeLawLeg(law1, date, slerpInterval.getLowerData());

        // second law leg
        final AttitudeLeg lawLeg2 = new AttitudeLawLeg(law2, slerpInterval.getUpperData(), slerpInterval.getUpperData()
            .shiftedBy(3600.));

        // attitude laws sequence
        final StrictAttitudeLegsSequence<AttitudeLeg> sequence = new StrictAttitudeLegsSequence<>();

        sequence.add(lawLeg1);
        sequence.add(slerp);
        sequence.add(lawLeg2);

        // attitude at the beginning of the constant spin slew
        final Attitude att1 = law1.getAttitude(leo, slerp.getTimeInterval().getLowerData(), gcrf);
        // attitude at the end of the constant spin slew
        final Attitude att2 = law2.getAttitude(leo, slerp.getTimeInterval().getUpperData(), gcrf);

        // the attitude at the beginning of the constant spin slew given by the constant spin slew and the first law are
        // equal with an allowed
        // error of 1e-14 due to the computation errors
        compareAttitudes(att1, slerp.getAttitude(slerp.getTimeInterval().getLowerData(), gcrf), EPS);
        // the attitude at the end of the constant spin slew given by the constant spin slew and the second law are
        // equal with an allowed error of
        // 1e-14 due to the computation errors
        compareAttitudes(att2, slerp.getAttitude(slerp.getTimeInterval().getUpperData(), gcrf), EPS);

        // the attitude at the beginning of the constant spin slew given by the constant spin slew and the sequence are
        // equal with an allowed error of
        // 1e-14 due to the computation errors
        compareAttitudes(slerp.getAttitude(slerp.getTimeInterval().getLowerData(), gcrf),
            sequence.getAttitude(leo, slerp.getTimeInterval().getLowerData(), gcrf), EPS);
        // the attitude at the end of the constant spin slew given by the constant spin slew and the sequence are equal
        // with an allowed error of
        // 1e-14 due to the computation errors
        compareAttitudes(slerp.getAttitude(slerp.getTimeInterval().getUpperData(), gcrf),
            sequence.getAttitude(leo, slerp.getTimeInterval().getUpperData(), gcrf), EPS);
    }

    /**
     * Compare Attitude instances. Needed because Attitude has no custom equals() method.
     * 
     * @param expected
     *        expected Attitude
     * @param actual
     *        actual Attitude
     * @param threshold
     *        : threshold
     */
    private static void compareAttitudes(final Attitude expected, final Attitude actual, final double threshold) {

        final boolean eqDate = eqNull(expected.getDate(), actual.getDate());
        final boolean eqRefF = eqNull(expected.getReferenceFrame(), actual.getReferenceFrame());
        final boolean eqRot = eqNullRot(expected.getRotation(), actual.getRotation(), threshold);

        final boolean fullEq = eqDate && eqRefF && eqRot;

        if (!fullEq) {
            Assert.fail("Attitude instances differ.");
        }
    }

    /**
     * Like equals, but managing null.
     * 
     * @param a
     *        object a
     * @param b
     *        object b
     * @return true or false
     */
    private static boolean eqNull(final Object a, final Object b) {
        boolean rez;
        if (a == null && b == null) {
            rez = true;
        } else {
            if (a == null || b == null) {
                rez = false;
            } else {
                rez = a.equals(b);
            }
        }
        return rez;
    }

    /**
     * Like equals, but managing null, for Rotation.
     * 
     * @param a
     *        object a
     * @param b
     *        object b
     * @param threshold
     *        : threshold
     * @return true or false
     */
    private static boolean eqNullRot(final Rotation a,
                              final Rotation b, final double threshold) {
        boolean rez;
        if (a == null && b == null) {
            rez = true;
        } else {
            if (a == null || b == null) {
                rez = false;
            } else {
                final Quaternion qa = a.getQuaternion();
                final Quaternion qb = b.getQuaternion();
                final boolean eq = qa.equals(qb, threshold);
                rez = eq;
            }
        }
        return rez;
    }
}
