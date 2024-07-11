/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import java.io.FileNotFoundException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.SolarInputs97to05;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class JB2006Test {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(JB2006Test.class.getSimpleName(), "JB2006 atmosphere");
    }

    @Test
    public void testWithOriginalTestsCases() throws PatriusException, ParseException {

        Report.printMethodHeader("testWithOriginalTestsCases", "Density computation", "Unknown", 0,
            ComparisonType.ABSOLUTE);

        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);

        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        final JB2006 atm = new JB2006(in, sun, earth);
        final JB2006 atm2 = (JB2006) atm.copy();
        double myRo;
        final double PI = 3.1415927;

        // SET SOLAR INDICES USE 1 DAY LAG FOR EUV AND F10 INFLUENCE
        final double S10 = 140;
        final double S10B = 100;
        final double F10 = 135;
        final double F10B = 95;
        // USE 5 DAY LAG FOR MG FUV INFLUENCE
        final double XM10 = 130;
        final double XM10B = 95;

        // USE 6.7 HR LAG FOR ap INFLUENCE
        final double AP = 30;
        // SET TIME OF INTEREST
        double IYR = 01;
        final double IDAY = 200;
        if (IYR < 50) {
            IYR = IYR + 100;
        }
        final double IYY = (IYR - 50) * 365 + ((IYR - 1) / 4 - 12);
        final double ID1950 = IYY + IDAY;
        final double D1950 = ID1950;
        double AMJD = D1950 + 33281;

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 400,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        double roTestCase = 0.4066e-11;
        double tzTestCase = 1137.7;
        double tinfTestCase = 1145.8;
        Assert.assertEquals(roTestCase * 1e12, MathLib.round(myRo * 1e15) / 1e3, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 400km)", roTestCase * 1e12, MathLib.round(myRo * 1e15) / 1e3);

        // alt = 90
        myRo =
            atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 90, F10,
                F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.3285e-05;
        tzTestCase = 183.0;
        tinfTestCase = 1142.8;
        Assert.assertEquals(roTestCase * 1e05, MathLib.round(myRo * 1e09) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 90km)", roTestCase * 1e05, MathLib.round(myRo * 1e09) / 1e4);

        // alt = 110
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 110,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.7587e-07;
        tzTestCase = 257.4;
        tinfTestCase = 1142.8;
        Assert.assertEquals(roTestCase * 1e07, MathLib.round(myRo * 1e11) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 110km)", roTestCase * 1e07, MathLib.round(myRo * 1e11) / 1e4);

        // alt = 180
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 180,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.5439; // *1e-9
        tzTestCase = 915.0;
        tinfTestCase = 1130.9;
        Assert.assertEquals(roTestCase, MathLib.round(myRo * 1e13) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 180km)", roTestCase, MathLib.round(myRo * 1e13) / 1e4);

        // alt = 230
        myRo = atm.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 230,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.1250e-09;
        tzTestCase = 1047.5;
        tinfTestCase = 1137.4;
        Assert.assertEquals(roTestCase * 1e09, MathLib.round(myRo * 1e13) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 230km)", roTestCase * 1e09, MathLib.round(myRo * 1e13) / 1e4);

        // alt = 270
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 270,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.4818e-10;
        tzTestCase = 1095.6;
        tinfTestCase = 1142.5;
        Assert.assertEquals(roTestCase * 1e10, MathLib.round(myRo * 1e14) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 270km)", roTestCase * 1e10, MathLib.round(myRo * 1e14) / 1e4);

        // alt = 660
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 660,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.9451e-13;
        tzTestCase = 1149.0;
        tinfTestCase = 1149.9;
        Assert.assertEquals(roTestCase * 1e13, MathLib.round(myRo * 1e17) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 660km)", roTestCase * 1e13, MathLib.round(myRo * 1e17) / 1e4);

        // alt = 890
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 890,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.8305e-14;
        tzTestCase = 1142.5;
        tinfTestCase = 1142.8;
        Assert.assertEquals(roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 890km)", roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4);

        // alt = 1320
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 1320,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.2004e-14;
        tzTestCase = 1142.7;
        tinfTestCase = 1142.8;
        Assert.assertEquals(roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 1320km)", roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4);

        // alt = 1600
        myRo = atm2.getDensity(AMJD, 90. * PI / 180., 20. * PI / 180., 90. * PI / 180., 45. * PI / 180., 1000 * 1600,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.1159e-14;
        tzTestCase = 1142.8;
        tinfTestCase = 1142.8;
        Assert.assertEquals(roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

        Report.printToReport("Density at date (altitude: 1600km)", roTestCase * 1e14, MathLib.round(myRo * 1e18) / 1e4);

        // OTHER entries
        AMJD += 50;
        myRo = atm2.getDensity(AMJD, 45. * PI / 180., 10. * PI / 180., 45. * PI / 180., -10. * PI / 180., 400 * 1000,
            F10, F10B, AP, S10, S10B, XM10, XM10B);
        roTestCase = 0.4838e-11;
        tzTestCase = 1137.4;
        tinfTestCase = 1145.4;
        Assert.assertEquals(roTestCase * 1e11, MathLib.round(myRo * 1e15) / 1e4, 0);
        Assert.assertEquals(tzTestCase, MathLib.round(atm2.getLocalTemp() * 10) / 10.0, 0);
        Assert.assertEquals(tinfTestCase, MathLib.round(atm2.getExosphericTemp() * 10) / 10.0, 0);

    }

    /** FT 268 : drag and lift implementation */
    @Test
    public final void testGetSpeedOfSound() throws PatriusException {

        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);

        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        final JB2006 atm = new JB2006(in, sun, earth);

        final AbsoluteDate date = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();

        final Vector3D position = new Vector3D(6500000., 0., 0.);
        final double speedOfSound = atm.getSpeedOfSound(date, position, gcrf);
        final double ref = 394.0444634250324;
        Assert.assertEquals(ref, speedOfSound, 1E-14);

    }

    public void testComparisonWithDTM2000() throws PatriusException, ParseException, FileNotFoundException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 01, 01),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());
        final Frame itrf = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, itrf);

        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        final JB2006 jb = new JB2006(in, sun, earth);
        final DTM2000 dtm = new DTM2000(in, sun, earth);
        // Positions

        Vector3D pos = new Vector3D(6500000.0,
            -1234567.0,
            4000000.0);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        double roJb = jb.getDensity(date, pos, FramesFactory.getEME2000());
        double roDtm = dtm.getDensity(date, pos, FramesFactory.getEME2000());

        pos = new Vector3D(3011109.360780633,
            -5889822.669411588,
            4002170.0385907636);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        roJb = jb.getDensity(date, pos, FramesFactory.getEME2000());
        roDtm = dtm.getDensity(date, pos, FramesFactory.getEME2000());

        pos = new Vector3D(-1033.4793830 * 1000,
            7901.2952754 * 1000,
            6380.3565958 * 1000);

        // COMPUTE DENSITY KG/M3 RHO

        // alt = 400
        roJb = jb.getDensity(date, pos, FramesFactory.getEME2000());
        roDtm = dtm.getDensity(date, pos, FramesFactory.getEME2000());

        GeodeticPoint point;
        for (int i = 0; i < 367; i++) {
            date = date.shiftedBy(Constants.JULIAN_DAY);
            point = new GeodeticPoint(MathLib.toRadians(40), 0, 300 * 1000);
            pos = earth.transform(point);
            roJb = jb.getDensity(date, pos, FramesFactory.getEME2000());
            roDtm = dtm.getDensity(date, pos, FramesFactory.getEME2000());
            Assert.assertEquals(roDtm, roJb, roJb);

        }

    }

    public void testSolarInputs() throws PatriusException, ParseException {

        AbsoluteDate date = new AbsoluteDate(new DateComponents(2001, 01, 14),
            TimeComponents.H00,
            TimeScalesFactory.getUTC());

        final SolarInputs97to05 in = SolarInputs97to05.getInstance();

        // 2001 14 2451924.0 176.3 164.4 180.0 180.4 163.4 169.2
        // 14 176 164 9 12 9 6 4 4 9 7
        Assert.assertEquals(176.3, in.getF10(date), 0);
        Assert.assertEquals(164.4, in.getF10B(date), 0);
        Assert.assertEquals(180.0, in.getS10(date), 0);
        Assert.assertEquals(180.4, in.getS10B(date), 0);
        Assert.assertEquals(163.4, in.getXM10(date), 0);
        Assert.assertEquals(169.2, in.getXM10B(date), 0);
        Assert.assertEquals(9, in.getAp(date), 0);

        date = date.shiftedBy(11 * 3600);
        Assert.assertEquals(6, in.getAp(date), 0);

        date = new AbsoluteDate(new DateComponents(1998, 02, 02),
            new TimeComponents(18, 00, 00),
            TimeScalesFactory.getUTC());
        // 1998 33 2450847.0 89.1 95.1 95.8 97.9 81.3 92.0 1
        // 33 89 95 4 5 4 4 2 0 0 3 98
        Assert.assertEquals(89.1, in.getF10(date), 0);
        Assert.assertEquals(95.1, in.getF10B(date), 0);
        Assert.assertEquals(95.8, in.getS10(date), 0);
        Assert.assertEquals(97.9, in.getS10B(date), 0);
        Assert.assertEquals(81.3, in.getXM10(date), 0);
        Assert.assertEquals(92.0, in.getXM10B(date), 0);
        Assert.assertEquals(0, in.getAp(date), 0);
        date = date.shiftedBy(6 * 3600 - 1);
        Assert.assertEquals(3, in.getAp(date), 0);
    }

    /**
     * Test that aims at showing the "cache" process of class {@link #JB2006} :
     * density is recomputed the method {@link JB2006#getDensity(AbsoluteDate, Vector3D, Frame)} is successively called
     * with different parameters.
     * 
     * @throws PatriusException
     *         if JB2006 altitude range is not in range 0 to 1000
     * @referenceVersion 3.2
     */
    @Test
    public void testRecomputed() throws PatriusException {

        final Frame frame = FramesFactory.getITRF();
        final PVCoordinatesProvider sun = CelestialBodyFactory.getSun();
        final OneAxisEllipsoid earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, frame);

        final SolarInputs97to05 in = SolarInputs97to05.getInstance();
        earth.setAngularThreshold(1e-10);
        final JB2006 atm = new JB2006(in, sun, earth);

        final AbsoluteDate date = new AbsoluteDate(2004, 01, 01, TimeScalesFactory.getTAI());
        final Frame gcrf = FramesFactory.getGCRF();

        this.setUp();

        // Altitude
        final double alt1 = this.computeZ(200E3);
        final double alt2 = this.computeZ(400E3);

        // Geodetic point
        final GeodeticPoint gp1 = new GeodeticPoint(0, 0, alt1);
        final GeodeticPoint gp2 = new GeodeticPoint(0, 0, alt2);

        // Positions
        final Vector3D pos1 = earth.transform(gp1);
        final Vector3D pos2 = earth.transform(gp2);

        // Check that the density depends on the frame used
        // so it is recomputed and leads to different results

        // When calling the following getDensity methods, the parameters in cache are :
        // (date, pos1, frame)
        final double density1_ITRF = atm.getDensity(date, pos1, frame);

        // Recomputation occur here : parameters in cache are now :
        // (date1, pos1, gcrf), so results are different
        final double density1_GCRF = atm.getDensity(date, pos1, gcrf);
        Assert.assertFalse(density1_GCRF == density1_ITRF);

        // Check also that values are recomputed if the position changes

        // (date1, pos2, frame) are now in cache
        final double density2 = atm.getDensity(date, pos2, frame);

        // Values are updated so different from previous
        Assert.assertFalse(density1_ITRF == density2);

        // Finally, check that changing the dates leads to recomputation in GCRF
        // Same idea about parameters in cache
        final double density_otherDate = atm.getDensity(date.shiftedBy(3600), pos1, gcrf);

        Assert.assertFalse(density1_GCRF == density_otherDate);

    }

    /**
     * Geodetic altitude from geopotential altitude
     * 
     * @param h
     *        Geopotential altitude
     * @return altitude
     */
    public double computeZ(final double h) {
        final double AE = 6356766;
        return AE * h / (AE - h);

    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }

    }

}
