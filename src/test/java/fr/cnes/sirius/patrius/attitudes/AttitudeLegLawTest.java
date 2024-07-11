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
 * @history creation 10/11/2015
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2465:27/05/2020:Refactoring calculs de ralliements
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:392:10/11/2015:Creation of the test class
 * VERSION::FA:565:03/03/2016:corrections on attitude requirements
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.EphemerisType;
import fr.cnes.sirius.patrius.bodies.JPLCelestialBodyLoader;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Tests for the AttitudeLegLaw class.
 *              </p>
 * 
 * @author Thomas Galpin
 * 
 * @version $Id: AttitudeLegLawTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.1
 */
public class AttitudeLegLawTest {

    /** Attitude law before. */
    private AttitudeLaw lawBefore;

    /** Attitude leg. */
    private AttitudeLeg leg;

    /** Attitude law after. */
    private AttitudeLaw lawAfter;

    /** PV coordinates provider. */
    Orbit orbit;

    /** Features description. */
    public enum features {

        /**
         * @featureTitle Attitude leg law
         * 
         * @featureDescription object describing an attitude leg law in a
         *                     time interval and also attitude laws outside its interval
         * 
         */
        ATTITUDE_LEG_LAW
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#ATTITUDE_LEG_LAW}
     * 
     * @testedMethod {@link AttitudeLegLaw#getAttitude(PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Instantiation of an AttitudeLegLaw and getting of its attitude at a date in a frame.
     * 
     * @input the output date and frame
     * 
     * @output Attitude
     * 
     * @testPassCriteria the output attitudes are the one used to create the basic attitude provider objects.
     *                   They depend on if - the date in contained in the interval of validity of the attitudeLeg
     *                   - the date is before the interval of validity of the attitudeLeg
     *                   - the date is after the interval of validity of the attitudeLeg
     *                   The data must be equal and no complex computation is involved : the epsilon is the double
     *                   comparison epsilon.
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public void testGetAttitude() throws PatriusException {

        // law
        final AttitudeLegLaw attitudeLeglaw = new AttitudeLegLaw(this.lawBefore, this.leg, this.lawAfter);
        attitudeLeglaw.setSpinDerivativesComputation(true);

        // test constructors
        final Attitude att = attitudeLeglaw.getAttitude(this.orbit);
        final Attitude att2 = attitudeLeglaw.getAttitude(this.orbit, this.orbit.getDate(), this.orbit.getFrame());
        Assert.assertEquals(att.getDate(), att2.getDate());
        Assert.assertEquals(att.getReferenceFrame(), att2.getReferenceFrame());
        Assert.assertTrue(att.getRotation().isEqualTo(att2.getRotation()));
        Assert.assertEquals(0, att.getSpin().getNorm(), att2.getSpin().getNorm());
        Assert.assertTrue(this.compareAttitudes(att, att2));

        // check for each points that the obtained attitude is the correct one
        final AbsoluteDate initDate = this.leg.getTimeInterval().getLowerData().shiftedBy(-120);
        for (int i = 0; i < 360; i++) {
            final AbsoluteDate date = initDate.shiftedBy(i);
            final Frame frame = FramesFactory.getGCRF();

            final Attitude attLawBefore = this.lawBefore.getAttitude(this.orbit, date, frame);
            final Attitude attLawAfter = this.lawAfter.getAttitude(this.orbit, date, frame);
            final Attitude attLeglaw = attitudeLeglaw.getAttitude(this.orbit, date, frame);

            if (i < 120) {
                // law before should be used
                Assert.assertTrue(this.compareAttitudes(attLawBefore, attLeglaw));
            }
            if (i >= 120 && i <= 240) {
                // leg should be used
                Assert.assertTrue(this.compareAttitudes(this.leg.getAttitude(this.orbit, date, frame), attLeglaw));
            }
            if (i > 240) {
                // law before should be used
                Assert.assertTrue(this.compareAttitudes(attLawAfter, attLeglaw));
            }
        }

        // Check rotation acceleration is null when spin derivative is deactivated
        attitudeLeglaw.setSpinDerivativesComputation(false);
        Assert.assertNull(attitudeLeglaw.getAttitude(this.orbit).getRotationAcceleration());
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
    private boolean compareAttitudes(final Attitude expected, final Attitude actual) throws PatriusException {

        final boolean eqDate = this.eqNull(expected.getDate(), actual.getDate());
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

    /**
     * Build the two attitude laws outside the interval of validity and the attitude Leg in the interval of validity
     * - law before : earth pointing
     * - law after : sun pointing
     * - leg : constant spin slew
     * 
     * @throws PatriusException
     *         orekit exception
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-dataCNES-2003");

        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
        final JPLCelestialBodyLoader loaderSun = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SUN);

        final JPLCelestialBodyLoader loaderEMB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.EARTH_MOON);
        final JPLCelestialBodyLoader loaderSSB = new JPLCelestialBodyLoader("unxp2000.405",
            EphemerisType.SOLAR_SYSTEM_BARYCENTER);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.EARTH_MOON, loaderEMB);
        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SOLAR_SYSTEM_BARYCENTER, loaderSSB);

        CelestialBodyFactory.addCelestialBodyLoader(CelestialBodyFactory.SUN, loaderSun);

        Assert.assertTrue(true);

        // Orbite initiale, propagateur sat1
        final Frame gcrf = FramesFactory.getGCRF();
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate initDate = new AbsoluteDate(2008, 1, 1, TimeScalesFactory.getUTC());
        this.orbit =
            new KeplerianOrbit(6700.e3, 0.01, FastMath.PI / 2.5, MathLib.toRadians(1.0), FastMath.PI / 4.0, 0.,
                PositionAngle.TRUE,
                gcrf, initDate, Constants.WGS84_EARTH_MU);

        // Attitude en Pointage Terre
        this.lawBefore = new BodyCenterPointing(itrf);
        // Attitude en Pointage Soleil
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        this.lawAfter = new CelestialBodyPointed(gcrf, sun, Vector3D.PLUS_K, Vector3D.MINUS_K, Vector3D.PLUS_I);

        // Constant spin slew
        final TimeScale tai = TimeScalesFactory.getTAI();
        final AbsoluteDate dateShiftEarthPointingSlew = new AbsoluteDate("2008-01-01T00:29:26.709", tai);
        final ConstantSpinSlew cstSpinSlew =
            new ConstantSpinSlew(this.lawBefore.getAttitude(orbit, dateShiftEarthPointingSlew, gcrf),
                    this.lawAfter.getAttitude(orbit, dateShiftEarthPointingSlew.shiftedBy(120.), gcrf));
        this.leg = cstSpinSlew;
    }
}
