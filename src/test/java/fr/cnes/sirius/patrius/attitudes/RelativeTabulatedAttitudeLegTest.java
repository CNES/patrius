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
 * @history creation 18/11/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3154:10/05/2022:[PATRIUS] Amelioration des methodes permettant l'extraction d'une sous-sequence 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2992:15/11/2021:[PATRIUS] Possibilite d'interpoler l'attitude en ignorant les rotations rate 
 * VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2105:15/05/2019:[Patrius] Ajout de la nature en entree des classes implementant l'interface Leg
 * VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:392:18/11/2015:Creation of the test class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AbstractVector3DFunction;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3DFunction;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.AngularDerivativesFilter;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the RelativeTabulatedAttitudeLeg class.
 *              </p>
 *
 * @author Thomas Galpin
 *
 * @version $Id: RelativeTabulatedAttitudeLegTest.java 17910 2017-09-11 11:58:16Z bignon $
 *
 * @since 3.1
 */
public class RelativeTabulatedAttitudeLegTest {

    /** List of angular coordinates. */
    private List<Pair<Double, AngularCoordinates>> listAr;

    /** List of angular coordinates with only one attitude. */
    private List<Pair<Double, AngularCoordinates>> listArOneAtt;

    /** List of rotations. */
    private List<Pair<Double, Rotation>> listRot;

    /** List of Attitudes. */
    private List<Attitude> listAtt;

    /** reference date. */
    private AbsoluteDate refDate;

    /** frame. */
    private Frame frame;

/**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, int, Frame)
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getAttitude(Orbit)
     *
     * @description Instantiation of an RelativeTabulatedAttitudeLeg and getting of its attitude at a date in a frame.
     *
     * @testPassCriteria the output attitudes are equal to the interpolated attitude obtained with
     * {@link TimeStampedAngularCoordinates#interpolate(AbsoluteDate, fr.cnes.sirius.patrius.utils.AngularDerivativesFilter,
     * java.util.Collection)}
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConst4() throws PatriusException {

        // with only one attitude test
        try {
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listArOneAtt, 5, this.frame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // check that when there is not enough data for Hermite interpolation, exception is raised
        // with only one attitude test
        try {
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listAr, 11, this.frame);
            Assert.fail();
        } catch (final PatriusException e) {
            // expected
        }

        // build RelativeTabulatedAttitudeLeg with 4 interpolation points
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listAr, 4, this.frame);

        // Check spin derivatives computation if not performed
        final Orbit initialOrbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), this.refDate, Constants.EIGEN5C_EARTH_MU);
        Assert.assertNull(relativeTabulatedAttitudeLeg.getAttitude(initialOrbit).getRotationAcceleration());

        // Check spin derivatives computation
        relativeTabulatedAttitudeLeg.setSpinDerivativesComputation(true);

        // check duration
        Assert.assertEquals(9., relativeTabulatedAttitudeLeg.getTimeInterval().getDuration(), 0.);

        // prepare date for testing :
        final AbsoluteDate dateLow = this.refDate.shiftedBy(-1);
        final AbsoluteDate dateHigh = this.refDate.shiftedBy(10);
        final AbsoluteDate dateFirst = this.refDate;
        final AbsoluteDate dateSecond = this.refDate.shiftedBy(1);
        final AbsoluteDate dateLast = this.refDate.shiftedBy(9);
        final AbsoluteDate dateBetween1and2 = this.refDate.shiftedBy(1.5);
        Attitude actualAtt = null;
        Attitude expectecAtt = null;

        // test on first attitude
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateFirst, this.frame);
        expectecAtt = new Attitude(this.frame, buildTimeStampedAR(0));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test on last attitude
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateLast, this.frame);
        expectecAtt = new Attitude(this.frame, buildTimeStampedAR(9));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test right on second attitude : the returned attitude should be exactly
        // the second one : interpolation must not be performed
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateSecond, this.frame);
        expectecAtt = new Attitude(this.frame, buildTimeStampedAR(1));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test between attitude 1 and attitude 2 (should be interpolated)
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateBetween1and2, this.frame);
        List<TimeStampedAngularCoordinates> sampleToInterpolate;
        sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(0));
        sampleToInterpolate.add(buildTimeStampedAR(1));
        sampleToInterpolate.add(buildTimeStampedAR(2));
        sampleToInterpolate.add(buildTimeStampedAR(3));
        expectecAtt = new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(dateBetween1and2,
            AngularDerivativesFilter.USE_RR, sampleToInterpolate, true));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test between attitude 1 and attitude 2 with different frame
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateBetween1and2, FramesFactory.getEME2000());
        sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(0));
        sampleToInterpolate.add(buildTimeStampedAR(1));
        sampleToInterpolate.add(buildTimeStampedAR(2));
        sampleToInterpolate.add(buildTimeStampedAR(3));
        final Attitude expectecAttGcrf = new Attitude(this.frame,
            TimeStampedAngularCoordinates.interpolate(dateBetween1and2,
                AngularDerivativesFilter.USE_RR, sampleToInterpolate, true));
        final Attitude expectecAttEME2000 = expectecAttGcrf.withReferenceFrame(FramesFactory.getEME2000(), true);
        Assert.assertTrue(compareAttitudes(expectecAttEME2000, actualAtt));

        // compare with TabulatedAttitude :
        final TabulatedAttitude tab = new TabulatedAttitude(this.listAtt, 4);
        tab.setSpinDerivativesComputation(true);
        for (double i = 0; i < 9; i += 0.1) {
            Assert.assertTrue(compareAttitudes(tab.getAttitude(null, this.refDate.shiftedBy(i), this.frame),
                relativeTabulatedAttitudeLeg.getAttitude(null, this.refDate.shiftedBy(i), this.frame)));
        }

        // test date low
        try {
            actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateLow, this.frame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // test date high
        try {
            actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateHigh, this.frame);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
        }

        // Test with : sample size = attitudes size :
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg2 =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listAr, 10, this.frame);
        actualAtt = relativeTabulatedAttitudeLeg2.getAttitude(null, dateBetween1and2, this.frame);
        sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(0));
        sampleToInterpolate.add(buildTimeStampedAR(1));
        sampleToInterpolate.add(buildTimeStampedAR(2));
        sampleToInterpolate.add(buildTimeStampedAR(3));
        sampleToInterpolate.add(buildTimeStampedAR(4));
        sampleToInterpolate.add(buildTimeStampedAR(5));
        sampleToInterpolate.add(buildTimeStampedAR(6));
        sampleToInterpolate.add(buildTimeStampedAR(7));
        sampleToInterpolate.add(buildTimeStampedAR(8));
        sampleToInterpolate.add(buildTimeStampedAR(9));
        expectecAtt = new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(dateBetween1and2,
            AngularDerivativesFilter.USE_RR, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test with method RelativeTabulatedAttitudeLeg#getAttitude(Orbit)
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), this.refDate, Constants.EIGEN5C_EARTH_MU);
        final Attitude att = relativeTabulatedAttitudeLeg2.getAttitude(orbit);
        final Attitude att2 = relativeTabulatedAttitudeLeg2.getAttitude(orbit, orbit.getDate(), orbit.getFrame());
        Assert.assertTrue(compareAttitudes(att, att2));

    }

/**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, Frame, List)
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Instantiation of an RelativeTabulatedAttitudeLeg and getting of its attitude at a date in a frame.
     *
     * @testPassCriteria constructor 3 calls constructor 4 with the default interpOrder.
     * Therefore check that the interpOrder is actually the default one.
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConst3() throws PatriusException {

        // build RelativeTabulatedAttitudeLeg
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.frame, this.listAr);

        final AbsoluteDate dateBetween1and2 = this.refDate.shiftedBy(1.5);

        // test between attitude 1 and attitude 2
        final Attitude actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateBetween1and2, this.frame);
        List<TimeStampedAngularCoordinates> sampleToInterpolate;
        sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(1));
        sampleToInterpolate.add(buildTimeStampedAR(2));
        final Attitude expectecAtt =
            new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(dateBetween1and2,
                AngularDerivativesFilter.USE_RR, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

    }

/**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame, int)
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description Instantiation of an RelativeTabulatedAttitudeLeg and getting of its attitude at a date in a frame.
     *
     * @testPassCriteria constructor 2 calls constructor 5 with useRotationRates = false and angular rates set to 0's.
     * Therefore check that angular rates are not used for interpolation
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConst2() throws PatriusException {

        // build RelativeTabulatedAttitudeLeg with 2 interpolation points
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg = new RelativeTabulatedAttitudeLeg(
                this.refDate, this.listRot, this.frame, 2);

        final List<TimeStampedAngularCoordinates> sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(0));
        sampleToInterpolate.add(buildTimeStampedAR(1));

        Attitude actualAtt;
        Attitude expectecAtt;

        // test at attitude 0
        final AbsoluteDate date = this.refDate;
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, date, this.frame);
        expectecAtt = new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(date,
                AngularDerivativesFilter.USE_R, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test between attitude 0 and attitude 1
        final AbsoluteDate dateBetween0and1 = this.refDate.shiftedBy(0.5);
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateBetween0and1, this.frame);
        expectecAtt = new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(
                dateBetween0and1, AngularDerivativesFilter.USE_R, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

        // test at attitude 1
        final AbsoluteDate date2 = this.refDate.shiftedBy(1);
        actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, date2, this.frame);
        expectecAtt = new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(date2,
                AngularDerivativesFilter.USE_R, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));
    }

/**
     * @throws PatriusException
     * @testType UT
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame)
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     *
     * @description constructor 1 calls constructor 2 with the default interpOrder.
     * Therefore check that the interpOrder is actually the default one.
     *
     * @testPassCriteria the output attitudes are equal to the interpolated attitude obtained with
     * {@link TimeStampedAngularCoordinates#interpolate(AbsoluteDate, fr.cnes.sirius.patrius.utils.AngularDerivativesFilter,
     * java.util.Collection)}
     *
     * @referenceVersion 3.1
     *
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testConst1() throws PatriusException {

        // build RelativeTabulatedAttitudeLeg
        final RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLeg =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listRot, this.frame);

        final AbsoluteDate dateBetween1and2 = this.refDate.shiftedBy(1.5);

        // test between attitude 1 and attitude 2
        final Attitude actualAtt = relativeTabulatedAttitudeLeg.getAttitude(null, dateBetween1and2, this.frame);
        List<TimeStampedAngularCoordinates> sampleToInterpolate;
        sampleToInterpolate = new ArrayList<>();
        sampleToInterpolate.add(buildTimeStampedAR(1));
        sampleToInterpolate.add(buildTimeStampedAR(2));
        final Attitude expectecAtt =
            new Attitude(this.frame, TimeStampedAngularCoordinates.interpolate(dateBetween1and2,
                AngularDerivativesFilter.USE_R, sampleToInterpolate));
        Assert.assertTrue(compareAttitudes(expectecAtt, actualAtt));

    }

    /**
     * private methode to build TimeStampedAngularCoordinates object from index
     *
     * @param index
     *        index
     * @return
     * @throws PatriusException
     *         if building fails
     * @since 3.1
     */
    private TimeStampedAngularCoordinates buildTimeStampedAR(final int index) throws PatriusException {
        return new TimeStampedAngularCoordinates(this.refDate.shiftedBy(index),
            this.listAr.get(index).getSecond().getRotation(),
            this.listAr.get(index).getSecond().getRotationRate(),
            this.listAr.get(index).getSecond().getRotationAcceleration());
    }

    /**
     * Compare Attitude instances. Needed because Attitude has no custom equals() method.
     *
     * @param expected
     *        expected Attitude
     * @param actual
     *        actual Attitude
     * @throws PatriusException
     */
    private static boolean compareAttitudes(final Attitude expected, final Attitude actual) throws PatriusException {

        final boolean eqDate = eqNull(expected.getDate(), actual.getDate());
        final boolean eqRefF = eqNull(expected.getReferenceFrame(), actual.getReferenceFrame());
        final boolean eqRot = eqNullRot(expected.getRotation(), actual.getRotation());
        final boolean eqSpin = eqNull(expected.getSpin(), actual.getSpin());
        final boolean eqAcc = eqNull(expected.getRotationAcceleration(), actual.getRotationAcceleration());
        final boolean fullEq = eqDate && eqRefF && eqRot && eqSpin && eqAcc;
        return fullEq;
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
        if ((a == null) && (b == null)) {
            rez = true;
        } else {
            if ((a == null) || (b == null)) {
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
     * @return true or false
     */
    private static boolean eqNullRot(final Rotation a, final Rotation b) {
        boolean rez;
        if ((a == null) && (b == null)) {
            rez = true;
        } else {
            if ((a == null) || (b == null)) {
                rez = false;
            } else {
                final boolean eqQ0 = a.getQi()[0] == b.getQi()[0];
                final boolean eqQ1 = a.getQi()[1] == b.getQi()[1];
                final boolean eqQ2 = a.getQi()[2] == b.getQi()[2];
                final boolean eqQ3 = a.getQi()[3] == b.getQi()[3];
                rez = eqQ0 && eqQ1 && eqQ2 && eqQ3;
            }
        }
        return rez;
    }

    // Test for rotation acceleration
    @Test
    public void testRotationAcceleration() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2004, 1, 1, TimeScalesFactory.getUTC());
        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0),
            FastMath.PI / 4.0, 0., PositionAngle.TRUE,
            gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit, earthPointingAtt);
        // ephemeris mode
        propagator.setEphemerisMode();

        // set up samples
        final int duration = 1600;
        final double freqSample = 2.;
        final List<Pair<Double, AngularCoordinates>> sample = new ArrayList<>();
        for (double dt = 0; dt < duration; dt += freqSample) {
            final SpacecraftState propag = propagator.propagate(initDate.shiftedBy(dt));
            final AngularCoordinates ar = new AngularCoordinates(propag.getAttitude().getRotation(),
                propag.getAttitude().getSpin(), propag.getAttitude().getRotationAcceleration());
            sample.add(new Pair<>(dt, ar));
        }
        final RelativeTabulatedAttitudeLeg relativeTab = new RelativeTabulatedAttitudeLeg(initDate, gcrf, sample);

        // Check spin derivatives computation if not performed
        Assert.assertNull(relativeTab.getAttitude(initialOrbit).getRotationAcceleration());

        // Check spin derivatives computation
        relativeTab.setSpinDerivativesComputation(true);

        // Compare acceleration obtained with interpolation and finite differences acceleration
        final Frame frameToCompute = gcrf;
        for (double i = 0.1; i < (duration - 2); i += 1) {
            final Vector3D acc = relativeTab.getAttitude(initialOrbit, initDate.shiftedBy(i), frameToCompute)
                .getRotationAcceleration();
            final Vector3D accDerivateSpin =
                getSpinFunction(relativeTab, null, frameToCompute, initDate.shiftedBy(i))
                    .nthDerivative(1).getVector3D(initDate.shiftedBy(i));
            Assert.assertEquals(acc.distance(accDerivateSpin), 0.0, 1e-11);
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        relativeTab.setSpinDerivativesComputation(false);
        Assert.assertNull(relativeTab.getAttitude(initialOrbit).getRotationAcceleration());
    }

    /**
     * Local function to provide spin function.
     *
     * @param pvProv
     *        local position-velocity provider around current date
     * @param frame
     *        reference frame from which spin function of date is computed
     * @param zeroAbscissa
     *        the date for which x=0 for spin function of date
     * @param tab
     *        tab
     * @return spin function of date relative
     */
    public Vector3DFunction getSpinFunction(final RelativeTabulatedAttitudeLeg tab, final PVCoordinatesProvider pvProv,
            final Frame frame,
            final AbsoluteDate zeroAbscissa) {
        return new AbstractVector3DFunction(zeroAbscissa){
            @Override
            public Vector3D getVector3D(final AbsoluteDate date) throws PatriusException {
                return tab.getAttitude(pvProv, date, frame).getSpin();
            }
        };
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame, int)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, Frame, List)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, int, Frame)}
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame, String)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, Frame, int, String)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, Frame, List, String)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#RelativeTabulatedAttitudeLeg(AbsoluteDate, List, int, Frame, String)}
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#getNature()}
     *
     * @description Test the new constructors which add the "nature" attribute
     *
     * @input parameters
     *
     * @output slew
     *
     * @testPassCriteria The nature attribute is well managed
     *
     * @referenceVersion 2.0
     *
     * @nonRegressionVersion 2.0
     */
    @Test
    public void FT2105() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2003");

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();

        final AbsoluteDate initDate = new AbsoluteDate(2004, 1, 1, TimeScalesFactory.getUTC());

        final Orbit initialOrbit = new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5,
            MathLib.toRadians(1.0), FastMath.PI / 4.0, 0., PositionAngle.TRUE, gcrf, initDate,
            Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        final AttitudeLaw earthPointingAtt = new BodyCenterPointing(itrf);

        // Propagator
        final KeplerianPropagator propagator = new KeplerianPropagator(initialOrbit,
            earthPointingAtt);
        // Ephemeris mode
        propagator.setEphemerisMode();

        // Set up samples
        final int duration = 1600;
        final double freqSample = 2.;

        final List<Pair<Double, AngularCoordinates>> sample = new ArrayList<>();
        for (double dt = 0; dt < duration; dt += freqSample) {
            final SpacecraftState propag = propagator.propagate(initDate.shiftedBy(dt));
            final AngularCoordinates ar = new AngularCoordinates(
                propag.getAttitude().getRotation(), propag.getAttitude().getSpin(), propag
                    .getAttitude().getRotationAcceleration());
            sample.add(new Pair<>(dt, ar));
        }

        final String DEFAULT_NATURE = "RELATIVE_TABULATED_ATTITUDE";
        final String nature = "testNature";

        // Test all the 8 constructors
        final RelativeTabulatedAttitudeLeg relativeTab1 = new RelativeTabulatedAttitudeLeg(
            initDate, this.listRot, gcrf);
        final RelativeTabulatedAttitudeLeg relativeTab2 = new RelativeTabulatedAttitudeLeg(
            initDate, this.listRot, gcrf, 4);
        final RelativeTabulatedAttitudeLeg relativeTab3 = new RelativeTabulatedAttitudeLeg(
            initDate, gcrf, sample);
        final RelativeTabulatedAttitudeLeg relativeTab4 = new RelativeTabulatedAttitudeLeg(
            initDate, sample, 4, gcrf);
        final RelativeTabulatedAttitudeLeg relativeTab5 = new RelativeTabulatedAttitudeLeg(
            initDate, this.listRot, gcrf, nature);
        final RelativeTabulatedAttitudeLeg relativeTab6 = new RelativeTabulatedAttitudeLeg(
            initDate, gcrf, sample, nature);
        final RelativeTabulatedAttitudeLeg relativeTab7 = new RelativeTabulatedAttitudeLeg(
            initDate, this.listRot, gcrf, 4, nature);
        final RelativeTabulatedAttitudeLeg relativeTab8 = new RelativeTabulatedAttitudeLeg(
            initDate, sample, 4, gcrf, nature);

        Assert.assertEquals(relativeTab1.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(relativeTab2.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(relativeTab3.getNature(), DEFAULT_NATURE);
        Assert.assertEquals(relativeTab4.getNature(), DEFAULT_NATURE);

        Assert.assertEquals(relativeTab5.getNature(), nature);
        Assert.assertEquals(relativeTab6.getNature(), nature);
        Assert.assertEquals(relativeTab7.getNature(), nature);
        Assert.assertEquals(relativeTab8.getNature(), nature);
    }

    /**
     * @testType UT
     *
     * @testedFeature none
     *
     * @testedMethod {@link RelativeTabulatedAttitudeLeg#copy(AbsoluteDateInterval)}
     *
     * @description Test the new method
     *
     * @input parameters
     *
     * @output AbsoluteDateInterval
     *
     * @testPassCriteria The method behavior is correct
     *
     * @referenceVersion 4.4
     *
     * @nonRegressionVersion 4.4
     */
    @Test
    public void testCopyMethod() throws PatriusException {

        // Constructor dates
        final AbsoluteDate referenceDate = new AbsoluteDate(2004, 1, 1, TimeScalesFactory.getUTC());
        final AbsoluteDate startDate = referenceDate.shiftedBy(this.listRot.get(0).getFirst());
        final AbsoluteDate endDate = referenceDate.shiftedBy(this.listRot.get(
            this.listRot.size() - 1).getFirst());

        final Frame gcrf = FramesFactory.getGCRF();

        // Intervals creation
        final double offset = 0.01;
        final AbsoluteDateInterval newIntervalOfValidity = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.CLOSED);
        final AbsoluteDateInterval newIntervalOfValidityOpen = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(-offset), IntervalEndpointType.OPEN);
        final AbsoluteDateInterval newIntervalOfValidityNotIncluded = new AbsoluteDateInterval(
            IntervalEndpointType.CLOSED,
            startDate.shiftedBy(offset), endDate.shiftedBy(+offset), IntervalEndpointType.CLOSED);

        // RelativeTabulatedAttitudeLeg creation
        RelativeTabulatedAttitudeLeg relativeTab1 = new RelativeTabulatedAttitudeLeg(
            referenceDate, this.listRot, gcrf);
        relativeTab1.setSpinDerivativesComputation(true);
        RelativeTabulatedAttitudeLeg relativeTab2 = new RelativeTabulatedAttitudeLeg(
            referenceDate, this.listRot, gcrf);
        final RelativeTabulatedAttitudeLeg relativeTab3 = new RelativeTabulatedAttitudeLeg(
            referenceDate, this.listRot, gcrf);

        final Attitude attitudeRef = relativeTab1.getAttitude(null, startDate.shiftedBy(1), gcrf);

        // Test case n°1 : in a standard usage, the interval stored should be updated
        final RelativeTabulatedAttitudeLeg tmp = relativeTab1.copy(newIntervalOfValidity);
        relativeTab1 = relativeTab1.copy(newIntervalOfValidity);
        Assert.assertTrue(relativeTab1.getTimeInterval().equals(newIntervalOfValidity));
        // Also check attitude in the middle of the validity interval
        final Attitude attitudeActual = relativeTab1.getAttitude(null, startDate.shiftedBy(1), gcrf);
        Assert.assertEquals(0, Rotation.distance(attitudeActual.getRotation(), attitudeRef.getRotation()), 0);
        Assert.assertEquals(0, attitudeActual.getSpin().distance(attitudeRef.getSpin()), 0);
        Assert.assertEquals(0, attitudeActual.getRotationAcceleration().distance(attitudeRef.getRotationAcceleration()), 0);

        // Test case n°2 : if we send an opened interval, it is closed before to process the truncation
        relativeTab2 = relativeTab2.copy(newIntervalOfValidityOpen);
        Assert.assertFalse(relativeTab2.getTimeInterval().equals(newIntervalOfValidityOpen));
        Assert.assertTrue(relativeTab2.getTimeInterval().equals(newIntervalOfValidity));

        // Test case n°3 : when the new interval isn't included, the method copy should throw an exception
        try {
            relativeTab3.copy(newIntervalOfValidityNotIncluded);
            Assert.fail();
        } catch (final IllegalArgumentException e) {
            // expected
            Assert.assertTrue(true);
        }
    }

    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        // date and frame
        this.refDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        this.frame = FramesFactory.getGCRF();

        // List of AR
        this.listAr = new ArrayList<>();
        this.listArOneAtt = new ArrayList<>();
        this.listRot = new ArrayList<>();
        this.listAtt = new ArrayList<>();
        final AngularCoordinates ar1 = new AngularCoordinates(
            new Rotation(false, 0.48, 0.64, 0.36, 0.48), Vector3D.PLUS_I, Vector3D.PLUS_J);
        final AngularCoordinates ar2 = new AngularCoordinates(
            new Rotation(false, 0.36, 0.48, 0.48, 0.64), Vector3D.PLUS_J, Vector3D.PLUS_K);
        for (int i = 0; i < 10; i++) {
            final AngularCoordinates ar = ((i % 2) == 0) ? ar1 : ar2;
            if (i == 0) {
                this.listArOneAtt.add(new Pair<>((double) i, ar));
            }
            this.listAr.add(new Pair<>((double) i, ar));
            this.listRot.add(new Pair<>((double) i, ar.getRotation()));
            this.listAtt.add(new Attitude(this.frame, buildTimeStampedAR(i)));
        }
    }
}
