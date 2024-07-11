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
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
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
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.interval.IntervalEndpointType;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Pair;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.TimeStampedAngularCoordinates;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the RelativeTabulatedAttitudeLaw class.
 *              </p>
 * 
 * @author Thomas Galpin
 * 
 * @version $Id: RelativeTabulatedAttitudeLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.1
 */
public class RelativeTabulatedAttitudeLawTest {

    /** Constant attitude law type. */
    private final RelativeTabulatedAttitudeLaw.AroundAttitudeType CONST_ATT =
        RelativeTabulatedAttitudeLaw.AroundAttitudeType.CONSTANT_ATT;

    /** Extrapolated attitude law type. */
    private final RelativeTabulatedAttitudeLaw.AroundAttitudeType EXTRAPOL_ATT =
        RelativeTabulatedAttitudeLaw.AroundAttitudeType.EXTRAPOLATED_ATT;

    /** relative tabulated attitude leg. */
    private RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLegWithAR;

    /** relative tabulated attitude leg. */
    private RelativeTabulatedAttitudeLeg relativeTabulatedAttitudeLegWithRot;

    /** validity interval. */
    private AbsoluteDateInterval validityInterval;

    /** List of angular coordinates. */
    private List<Pair<Double, AngularCoordinates>> listAr;

    /** List of rotations. */
    private List<Pair<Double, Rotation>> listRot;

    /** reference date. */
    private AbsoluteDate refDate;

    /** frame. */
    private Frame frame;

    /** Expected attitude. */
    private Attitude attExp;

    /** Actual attitude. */
    private Attitude attActual;

/**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#RelativeTabulatedAttitudeLaw(Frame, AbsoluteDate, List,
     * fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType,
     * fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType)
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.Orbit)
     * 
     * @description test a RelativeTabulatedAttitudeLaw with constant attitude for laws
     * before and after the interval of validity of the corresponding relativeTabulatedAttitudeLeg
     * 
     * @testPassCriteria check Attitude values for interpolation dates before during and after the validity interval
     * of the corresponding relativeTabulatedAttitudeLeg
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testlawBeforeCstlawAfterCst() throws PatriusException {

        // build RelativeTabulatedAttitudeLaw
        final RelativeTabulatedAttitudeLaw relativeTabulatedAttitudeLaw =
            new RelativeTabulatedAttitudeLaw(this.refDate, this.listAr, this.frame, this.CONST_ATT, this.CONST_ATT);
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), this.refDate, Constants.EIGEN5C_EARTH_MU);

        // Check spin derivatives computation if not performed
        Assert.assertNull(relativeTabulatedAttitudeLaw.getAttitude(orbit).getRotationAcceleration());

        // Check spin derivatives computation
        this.relativeTabulatedAttitudeLegWithAR.setSpinDerivativesComputation(true);
        this.relativeTabulatedAttitudeLegWithRot.setSpinDerivativesComputation(true);
        relativeTabulatedAttitudeLaw.setSpinDerivativesComputation(true);

        // test with method RelativeTabulatedAttitudeLaw#getAttitude(Orbit)
        Assert.assertTrue(this.compareAttitudes(relativeTabulatedAttitudeLaw.getAttitude(orbit),
            relativeTabulatedAttitudeLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()), true));

        // test getAttitude() with date = start of the interval of validity
        AbsoluteDate dateTest = this.validityInterval.getLowerData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date = end of the interval of validity
        dateTest = this.validityInterval.getUpperData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date before the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(-10);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = new Attitude(this.frame, this.buildTimeStampedARNullSpin(0));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));

        // test getAttitude() with date within the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(1.5);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attActual, this.attExp, true));

        // test getAttitude() with date after the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(14);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = new Attitude(this.frame, this.buildTimeStampedARNullSpin(9));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));

        // Set back for other tests
        this.relativeTabulatedAttitudeLegWithAR.setSpinDerivativesComputation(false);
        this.relativeTabulatedAttitudeLegWithRot.setSpinDerivativesComputation(false);

        // Check rotation acceleration is null when spin derivative is deactivated
        relativeTabulatedAttitudeLaw.setSpinDerivativesComputation(false);
        Assert.assertNull(relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame)
            .getRotationAcceleration());
    }

/**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#RelativeTabulatedAttitudeLaw(Frame, AbsoluteDate, List,
     * fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType,
     * fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType)
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#getAttitude(fr.cnes.sirius.patrius.orbits.Orbit)
     * 
     * @description test a RelativeTabulatedAttitudeLaw with extrapolated attitude for laws
     * before and after the interval of validity of the corresponding relativeTabulatedAttitudeLeg
     * 
     * @testPassCriteria check Attitude values for interpolation dates before during and after the validity interval
     * of the corresponding relativeTabulatedAttitudeLeg
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testlawBeforeExtrlawAfterExtr() throws PatriusException {

        // build RelativeTabulatedAttitudeLaw
        final RelativeTabulatedAttitudeLaw relativeTabulatedAttitudeLaw =
            new RelativeTabulatedAttitudeLaw(this.refDate, this.listAr, this.frame, this.EXTRAPOL_ATT,
                this.EXTRAPOL_ATT);
        final Orbit orbit = new KeplerianOrbit(7063957.657, 0.0009269214, MathLib.toRadians(98.28647),
            MathLib.toRadians(105.332064), MathLib.toRadians(108.315415), MathLib.toRadians(89.786767),
            PositionAngle.MEAN, FramesFactory.getGCRF(), this.refDate, Constants.EIGEN5C_EARTH_MU);

        // Check spin derivatives computation if not performed
        Assert.assertNull(relativeTabulatedAttitudeLaw.getAttitude(orbit).getRotationAcceleration());

        // test getAttitude() with date = start of the interval of validity
        AbsoluteDate dateTest = this.validityInterval.getLowerData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date = end of the interval of validity
        dateTest = this.validityInterval.getUpperData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date before the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(-10);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        AngularCoordinates arExpected = this.listAr.get(0).getSecond().shiftedBy(-10);
        this.attExp = new Attitude(this.frame, new TimeStampedAngularCoordinates(dateTest,
            arExpected.getRotation(),
            arExpected.getRotationRate(),
            arExpected.getRotationAcceleration()));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));

        // test getAttitude() with date within the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(1.5);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithAR.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attActual, this.attExp, true));

        // test getAttitude() with date after the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(14);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        arExpected = this.listAr.get(this.listAr.size() - 1).getSecond().shiftedBy(14 - 9);
        this.attExp = new Attitude(this.frame, new TimeStampedAngularCoordinates(dateTest,
            arExpected.getRotation(),
            arExpected.getRotationRate(),
            arExpected.getRotationAcceleration()));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link RelativeTabulatedAttitudeLaw#RelativeTabulatedAttitudeLaw(Frame, AbsoluteDate, List, fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType, fr.cnes.sirius.patrius.attitudes.RelativeTabulatedAttitudeLaw.AroundAttitudeType)}
     * 
     * @description test for constructor with rotations
     * 
     * @testPassCriteria check Attitude values for interpolation dates before during and after the validity interval
     *                   of the corresponding relativeTabulatedAttitudeLeg
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testConstructorWithRotations() throws PatriusException {

        // build RelativeTabulatedAttitudeLaw
        final RelativeTabulatedAttitudeLaw relativeTabulatedAttitudeLaw =
            new RelativeTabulatedAttitudeLaw(this.frame, this.refDate, this.listRot, this.EXTRAPOL_ATT,
                this.EXTRAPOL_ATT);

        // test getAttitude() with date = start of the interval of validity
        AbsoluteDate dateTest = this.validityInterval.getLowerData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithRot.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date = end of the interval of validity
        dateTest = this.validityInterval.getUpperData();
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithRot.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, true));

        // test getAttitude() with date before the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(-10);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        AngularCoordinates arExpected = (new AngularCoordinates(this.listRot.get(0).getSecond(),
            Vector3D.ZERO)).shiftedBy(-10);
        this.attExp = new Attitude(this.frame, new TimeStampedAngularCoordinates(dateTest,
            arExpected.getRotation(),
            arExpected.getRotationRate(),
            arExpected.getRotationAcceleration()));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));

        // test getAttitude() with date within the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(1.5);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        this.attExp = this.relativeTabulatedAttitudeLegWithRot.getAttitude(null, dateTest, this.frame);
        Assert.assertTrue(this.compareAttitudes(this.attActual, this.attExp, true));

        // test getAttitude() with date after the interval of validity
        dateTest = this.validityInterval.getLowerData().shiftedBy(14);
        this.attActual = relativeTabulatedAttitudeLaw.getAttitude(null, dateTest, this.frame);
        arExpected = (new AngularCoordinates(this.listRot.get(this.listAr.size() - 1).getSecond(),
            Vector3D.ZERO)).shiftedBy(14 - 9);
        this.attExp = new Attitude(this.frame, new TimeStampedAngularCoordinates(dateTest,
            arExpected.getRotation(),
            arExpected.getRotationRate(),
            arExpected.getRotationAcceleration()));
        Assert.assertTrue(this.compareAttitudes(this.attExp, this.attActual, false));
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
    private TimeStampedAngularCoordinates buildTimeStampedARNullSpin(final int index) throws PatriusException {
        return new TimeStampedAngularCoordinates(this.refDate.shiftedBy(index),
            this.listAr.get(index).getSecond().getRotation(),
            Vector3D.ZERO,
            Vector3D.ZERO);
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
    private
            boolean
            compareAttitudes(final Attitude expected, final Attitude actual, final boolean compareDate)
                                                                                                       throws PatriusException {

        boolean eqDate = true;
        if (compareDate) {
            eqDate = this.eqNull(expected.getDate(), actual.getDate());
        }
        final boolean eqRefF = this.eqNull(expected.getReferenceFrame(), actual.getReferenceFrame());
        final boolean eqRot = this.eqNullRot(expected.getRotation(), actual.getRotation());
        final boolean eqSpin = this.eqNull(expected.getSpin(), actual.getSpin());
        final boolean eqAcc = this.eqNull(expected.getRotationAcceleration(), actual.getRotationAcceleration());
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
    private boolean eqNull(final Object a, final Object b) {
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
     * @return true or false
     */
    private boolean eqNullRot(final Rotation a, final Rotation b) {
        boolean rez;
        if (a == null && b == null) {
            rez = true;
        } else {
            if (a == null || b == null) {
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

    @Before
    public final void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        // date and frame
        this.refDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getTAI());
        this.frame = FramesFactory.getGCRF();

        // List of AR
        this.listAr = new ArrayList<Pair<Double, AngularCoordinates>>();
        this.listRot = new ArrayList<Pair<Double, Rotation>>();
        final AngularCoordinates ar1 = new AngularCoordinates(
            new Rotation(false, 0.48, 0.64, 0.36, 0.48), Vector3D.PLUS_I, Vector3D.PLUS_J);
        final AngularCoordinates ar2 = new AngularCoordinates(
            new Rotation(false, 0.36, 0.48, 0.48, 0.64), Vector3D.PLUS_J, Vector3D.PLUS_K);
        for (int i = 0; i < 10; i++) {
            final AngularCoordinates ar = (i % 2 == 0) ? ar1 : ar2;
            this.listAr.add(new Pair<Double, AngularCoordinates>((double) i, ar));
            this.listRot.add(new Pair<Double, Rotation>((double) i, ar.getRotation()));
        }
        this.relativeTabulatedAttitudeLegWithAR =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.frame, this.listAr);
        this.relativeTabulatedAttitudeLegWithRot =
            new RelativeTabulatedAttitudeLeg(this.refDate, this.listRot, this.frame);
        this.validityInterval = new AbsoluteDateInterval(IntervalEndpointType.CLOSED,
            this.refDate, this.refDate.shiftedBy(9), IntervalEndpointType.CLOSED);
    }
}
