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
 * @history 30/08/2016 Creation of the class
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1451:09/03/2018: Normalization of xSun
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.directions.ConstantVectorDirection;
import fr.cnes.sirius.patrius.attitudes.directions.IDirection;
import fr.cnes.sirius.patrius.attitudes.directions.ToCelestialBodyCenterDirection;
import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.MeeusSun;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link IsisSunPointing}.
 * 
 * @author rodriguest
 * 
 * @version $Id: IsisSunPointingTest.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 3.3
 * 
 */
public class IsisSunPointingTest {

    /** Sun body. */
    private static CelestialBody sun;

    /** Sn gravitational constant. */
    private final double mu = Constants.EGM96_EARTH_MU;

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link IsisSunPointing#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Considering a given elliptic orbit at given date, this test ensures that the axis of the satellite
     *              following
     *              a ISIS Sun pointing law correspond the Sun computed axis (Xsun, Ysun, Zsun).
     *              It is verified that Zsat is aligned Zsun, the latter being the SatSun vector, also that Xsat is
     *              aligned with Xsun.
     *              The condition in Xsun computation in the algorithm is covered : continuity conditions are verified
     *              at a date
     *              where the discontinuity occured.
     * 
     * @input an elliptic orbit
     * 
     * @output differences between actual and expected axis
     * 
     * @testPassCriteria As Zsat is opposite to Zsun, the norm of Zsat + Zsun must be equal to 0. at threshold epsilon.
     *                   The norm of the difference Xsat - Xsun must be equal to 0. at given threshold.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     * 
     * @throws PatriusException
     */
    @Test
    public void sunPointingTest() throws PatriusException {

        Report.printMethodHeader("sunPointingTest", "Rotation computation", "Math", 1E-15, ComparisonType.RELATIVE);

        // The Sun
        sun = CelestialBodyFactory.getSun();

        // Define an elliptic orbit
        final AbsoluteDate date = new AbsoluteDate(2016, 8, 30, 14, 40, 0, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(10000000, 0.95, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            gcrf, date, this.mu);

        // ISIS Sun pointing law : the K satellite axis should be aligned with the sun-satellite direction
        final IsisSunPointing law = new IsisSunPointing(sun);

        // The expected Zsat axis
        final PVCoordinates satPv = orbit.getPVCoordinates();
        final Vector3D satPos = satPv.getPosition();
        final Vector3D expectedZsat = satPos.subtract(sun.getPVCoordinates(date, gcrf).getPosition()).normalize();

        // The expected Xsat axis
        final Vector3D satVel = satPv.getVelocity();
        final Vector3D orbitalMomentum = satPos.crossProduct(satVel).negate();
        final Vector3D expectedXsat = orbitalMomentum.crossProduct(expectedZsat).normalize();

        // The actual computed axis
        final Attitude actualAtt = law.getAttitude(orbit);
        final Vector3D actualZsat = actualAtt.getRotation().applyTo(Vector3D.PLUS_K);
        final Vector3D actualXsat = actualAtt.getRotation().applyTo(Vector3D.PLUS_I);

        // Perform the comparisons
        final Vector3D expectedNullVector = actualZsat.subtract(expectedZsat);
        Assert.assertEquals(expectedNullVector.getNorm(), 0., 1e-15);
        Assert.assertEquals(actualXsat.subtract(expectedXsat).getNorm(), 0., 1e-15);
        Report.printToReport("X axis", expectedXsat, actualXsat);
        Report.printToReport("Z axis", expectedZsat, actualZsat);

        // Find a date for which the condition on Xsat in the algorithm is covered
        final AbsoluteDate dateEventOnXsat = date.shiftedBy(113 * 86400 + 3600 + 18 * 60 + 23);
        final Orbit currentOrbit = orbit.shiftedBy(dateEventOnXsat.durationFrom(date));

        // Perform comparisons on X axis computed just before/after the discontinuity :
        // check that the sign of x component changes, sign on z component is continuous
        // also ensure that the "two" X axis are nearly collinear before the event, opposite after
        final Attitude attBeforeEvent1 = law.getAttitude(currentOrbit);
        final Attitude attBeforeEvent2 = law.getAttitude(currentOrbit.shiftedBy(0.404));
        final Attitude attAfterEvent = law.getAttitude(currentOrbit.shiftedBy(0.405));
        final Vector3D xBefore1 = attBeforeEvent1.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D xBefore2 = attBeforeEvent2.getRotation().applyTo(Vector3D.PLUS_I);
        final Vector3D xAfter = attAfterEvent.getRotation().applyTo(Vector3D.PLUS_I);

        Assert.assertEquals(xBefore1.add(xBefore2).getNorm(), 2., 1.0E-14);
        Assert.assertEquals(xBefore2.add(xAfter).getNorm(), 0., 4.0E-10);
        Assert.assertTrue(xBefore2.getX() > 0.);
        Assert.assertTrue(xAfter.getX() < 0.);
        Assert.assertTrue(xBefore2.getZ() < 0.);
        Assert.assertTrue(xAfter.getZ() < 0.);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link IsisSunPointing#IsisSunPointing(CelestialBody)}
     *               {@link IsisSunPointing#IsisSunPointing(IDirection)}
     * 
     * @description Considering a given elliptic orbit at given date, it is verified that the two constructors of
     *              the class are consistant with respect to attitude restitution.
     * 
     * @input an elliptic orbit
     * 
     * @output differences between actual and expected rotations associated to output attitudes
     * 
     * @testPassCriteria the distance between the rotations should be 0. at given threshold
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     * 
     * @throws PatriusException
     */
    @Test
    public void constructorTest() throws PatriusException {

        // Sun direction : used Meeus model
        final CelestialBody sunBody = new MeeusSun();
        final IDirection sunDirection = new ToCelestialBodyCenterDirection(sunBody);

        // Define an elliptic orbit
        final AbsoluteDate date = new AbsoluteDate(2016, 9, 19, 14, 20, 0, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();
        final Orbit orbit = new KeplerianOrbit(10000000, 0.95, MathLib.toRadians(75), 0, 0, 0, PositionAngle.MEAN,
            gcrf, date, this.mu);

        // ISIS Sun pointing law :
        final IsisSunPointing law1 = new IsisSunPointing(sunBody);
        final IsisSunPointing law2 = new IsisSunPointing(sunDirection);

        // The attitude computed in each case
        final Attitude actualAtt1 = law1.getAttitude(orbit);
        final Rotation rot1 = actualAtt1.getRotation();
        final Attitude actualAtt2 = law2.getAttitude(orbit);
        final Rotation rot2 = actualAtt2.getRotation();
        // Perform the comparisons
        Assert.assertEquals(Rotation.distance(rot1, rot2), 0., 1e-15);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedMethod {@link IsisSunPointing#getAttitude(org.orekit.utils.PVCoordinatesProvider, AbsoluteDate, Frame)}
     * 
     * @description Simple coverage test to verify that an exception is well raised in the case where the ISIS Sun
     *              pointing law could not be defined : the case where the Sun is orthogonal to the orbital plane.
     * 
     * @input an elliptic orbit
     * 
     * @output an Orekit exception
     * 
     * @testPassCriteria the expected exception must be caught.
     * 
     * @referenceVersion 3.3
     * 
     * @nonRegressionVersion 3.3
     * 
     * @throws PatriusException
     */
    @Test(expected = PatriusException.class)
    public void sunPointingException() throws PatriusException {

        // Sun direction : constant direction for this coverage test
        final Frame gcrf = FramesFactory.getGCRF();
        final ConstantVectorDirection sunDir = new ConstantVectorDirection(Vector3D.PLUS_K, gcrf);

        // Build an orbit which angular momentum is aligned with Sun direction
        final AbsoluteDate date = new AbsoluteDate(2016, 8, 30, 14, 40, 0, TimeScalesFactory.getTAI());
        final IsisSunPointing law = new IsisSunPointing(sunDir);
        final PVCoordinates pvCoord = new PVCoordinates(Vector3D.PLUS_I, Vector3D.PLUS_J);
        final Orbit orbit = new KeplerianOrbit(pvCoord, gcrf, date, this.mu);

        law.getAttitude(orbit);
    }

    /**
     * Set-up before test.
     * 
     * @throws PatriusException
     */
    @BeforeClass
    public static void setUp() throws PatriusException {
        Report.printClassHeader(IsisSunPointingTest.class.getSimpleName(), "ISIS Sun pointing");
        Utils.setDataRoot("regular-dataCNES-2003");
    }
}
