/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::FA:1777:04/10/2018:correct ICRF parent frame
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.CelestialBodyFactory;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class HarrisPriesterTest {

    // Sun
    private PVCoordinatesProvider sun;

    // Earth
    private OneAxisEllipsoid earth;

    // Frame
    private Frame itrf;

    // Time Scale
    private TimeScale utc;

    // Date
    private AbsoluteDate date;

    @Test
    public void testStandard() throws PatriusException {

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth);
        final HarrisPriester atm2 = (HarrisPriester) hp.copy();

        // Position at 500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 500000.);
        final Vector3D pos = this.earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = atm2.getDensity(this.date, pos, this.itrf);

        Assert.assertEquals(3.9236341626253185E-13, rho, 1.0e-21);

    }

    /** FT 268 : drag and lift implementation */
    @Test(expected = PatriusException.class)
    public final void testGetSpeedOfSound() throws PatriusException {

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth);
        final GeodeticPoint point = new GeodeticPoint(0, 0, 500000.);
        final Vector3D pos = this.earth.transform(point);
        hp.getSpeedOfSound(this.date, pos, this.itrf);
    }

    @Test
    public void testParameterN() throws PatriusException {

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth);

        // Position at 500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 500000.);
        final Vector3D pos = this.earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho4 = hp.getDensity(this.date, pos, this.itrf);

        final HarrisPriester hp2 = new HarrisPriester(this.sun, this.earth, 2);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho2 = hp2.getDensity(this.date, pos, this.itrf);

        final HarrisPriester hp6 = new HarrisPriester(this.sun, this.earth, 6);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho6 = hp6.getDensity(this.date, pos, this.itrf);

        final double c2Psi2 = 2.150731005787848e-2;

        Assert.assertEquals(c2Psi2, (rho6 - rho2) / (rho4 - rho2) - 1., 1.e-7);

    }

    @Test
    public void testMaxAlt() throws PatriusException {

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth);

        // Position at 1500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 1500000.);
        final Vector3D pos = this.earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = hp.getDensity(this.date, pos, this.itrf);

        Assert.assertEquals(0.0, rho, 0.0);
    }

    @Test
    public void testUserTab() throws PatriusException {

        final double[][] userTab = {
            { 100000., 4.974e+02, 4.974e+02 },
            { 110000., 7.800e+01, 7.800e+01 },
            { 120000., 2.490e+01, 2.400e+01 },
            { 130000., 8.377e+00, 8.710e+00 },
            { 140000., 3.899e+00, 4.059e+00 },
            { 150000., 2.122e+00, 2.215e+00 },
            { 160000., 1.263e+00, 1.344e+00 },
            { 170000., 8.008e-01, 8.758e-01 },
            { 180000., 5.283e-01, 6.010e-01 },
            { 190000., 3.618e-01, 4.297e-01 },
            { 200000., 2.557e-01, 3.162e-01 },
            { 210000., 1.839e-01, 2.396e-01 },
            { 220000., 1.341e-01, 1.853e-01 },
            { 230000., 9.949e-02, 1.455e-01 },
            { 240000., 7.488e-02, 1.157e-01 },
            { 250000., 5.709e-02, 9.308e-02 },
            { 260000., 4.403e-02, 7.555e-02 },
            { 270000., 3.430e-02, 6.182e-02 },
            { 280000., 2.697e-02, 5.095e-02 },
            { 290000., 2.139e-02, 4.226e-02 },
            { 300000., 1.708e-02, 3.526e-02 },
            { 320000., 1.099e-02, 2.511e-02 },
            { 340000., 7.214e-03, 1.819e-02 },
            { 360000., 4.824e-03, 1.337e-02 },
            { 380000., 3.274e-03, 9.955e-03 },
            { 400000., 2.249e-03, 7.492e-03 },
            { 420000., 1.558e-03, 5.684e-03 },
            { 440000., 1.091e-03, 4.355e-03 },
            { 460000., 7.701e-04, 3.362e-03 },
            { 480000., 5.474e-04, 2.612e-03 },
            { 500000., 3.916e-04, 2.042e-03 },
            { 520000., 2.819e-04, 1.605e-03 },
            { 540000., 2.042e-04, 1.267e-03 },
            { 560000., 1.488e-04, 1.005e-03 },
            { 580000., 1.092e-04, 7.997e-04 },
            { 600000., 8.070e-05, 6.390e-04 },
            { 620000., 6.012e-05, 5.123e-04 },
            { 640000., 4.519e-05, 4.121e-04 },
            { 660000., 3.430e-05, 3.325e-04 },
            { 680000., 2.632e-05, 2.691e-04 },
            { 700000., 2.043e-05, 2.185e-04 },
            { 720000., 1.607e-05, 1.779e-04 },
            { 740000., 1.281e-05, 1.452e-04 },
            { 760000., 1.036e-05, 1.190e-04 },
            { 780000., 8.496e-06, 9.776e-05 },
            { 800000., 7.069e-06, 8.059e-05 },
            { 850000., 4.800e-06, 5.500e-05 },
            { 900000., 3.300e-06, 3.700e-05 },
            { 950000., 2.450e-06, 2.400e-05 },
            { 1000000., 1.900e-06, 1.700e-05 },
            { 1100000., 1.180e-06, 8.700e-06 },
            { 1200000., 7.500e-07, 4.800e-06 },
            { 1300000., 5.300e-07, 3.200e-06 },
            { 1400000., 4.100e-07, 2.000e-06 },
            { 1500000., 2.900e-07, 1.350e-06 },
            { 1600000., 2.000e-07, 9.500e-07 },
            { 1700000., 1.600e-07, 7.700e-07 },
            { 1800000., 1.200e-07, 6.300e-07 },
            { 1900000., 9.600e-08, 5.200e-07 },
            { 2000000., 7.300e-08, 4.400e-07 }
        };

        // Position at 1500 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 1500000.);
        final Vector3D pos = this.earth.transform(point);

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth, userTab);

        // COMPUTE DENSITY KG/M3 RHO
        final double rho = hp.getDensity(this.date, pos, this.itrf);

        // New reference since PATRIUS 4.2 and use of GCRF as reference frame instead of EME2000
        Assert.assertEquals(2.9049031871974194E-7, rho, 1.0e-17);

        final HarrisPriester hp6 = new HarrisPriester(this.sun, this.earth, userTab, 6);
        final double rho6 = hp6.getDensity(this.date, pos, this.itrf);

        final HarrisPriester hp2 = new HarrisPriester(this.sun, this.earth, userTab, 2);
        final double rho2 = hp2.getDensity(this.date, pos, this.itrf);

        final double c2Psi2 = 2.150731005787848e-2;

        Assert.assertEquals(c2Psi2, (rho6 - rho2) / (rho - rho2) - 1., 1.e-7);

    }

    @Test(expected = PatriusException.class)
    public void testOutOfRange() throws PatriusException {

        final HarrisPriester hp = new HarrisPriester(this.sun, this.earth);

        // Position at 50 km height
        final GeodeticPoint point = new GeodeticPoint(0, 0, 50000.);
        final Vector3D pos = this.earth.transform(point);

        // COMPUTE DENSITY KG/M3 RHO
        hp.getDensity(this.date, pos, this.itrf);
    }

    @Before
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");
            FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));

            this.sun = CelestialBodyFactory.getSun();

            this.itrf = FramesFactory.getITRF();
            this.earth = new OneAxisEllipsoid(6378136.460, 1.0 / 298.257222101, this.itrf);
            this.earth.setAngularThreshold(1.e-10);

            // Equinoxe 21 mars 2003 à 1h00m
            this.utc = TimeScalesFactory.getUTC();
            this.date = new AbsoluteDate(new DateComponents(2003, 03, 21), new TimeComponents(1, 0, 0.), this.utc);

        } catch (final PatriusException oe) {
            Assert.fail(oe.getMessage());
        }
    }

    @After
    public void tearDown() {
        this.utc = null;
    }

}
