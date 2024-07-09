/**
 * 
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
 * 
 * @history Created 16/10/2014
 *
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:267:24/10/2014: US76 atmosphere model added
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:378:14/04/2015:Problems with the implementation of US76 atmosphere model
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:515:02/02/2016:Add getters for temperature and pressure
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.frames.FactoryManagedFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test the {@link US76} class for the US76 Atmosphere model
 * 
 * @author Francois Toussaint
 * 
 * @version $Id$
 * 
 * @since 2.3
 * 
 */
public class US76Test {

    /**
     * Doubles comparison epsilon
     */
    private static final double EPS = 1e-15;

    /**
     * Earth equatorial radius
     */
    double ae;

    /**
     * Earth BodyShape
     */
    private OneAxisEllipsoid earth1;

    /**
     * date
     */
    private AbsoluteDate date1;

    /**
     * Frame of pos vector
     */
    private FactoryManagedFrame frame1;

    /**
     * Atmosphere model tested
     */
    private US76 atmosModel1;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle US76 tests
         * 
         * @featureDescription validate the US76 atmospheric model
         */
        US76
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(US76Test.class.getSimpleName(), "US76 atmosphere");
    }

    /**
     * @throws PatriusException
     * 
     * @testType UT
     * 
     * @testedFeature {@link features#US76}
     * 
     * @testedMethod {@link US76#getDensity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description the test computes several densities for given date, positions and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output doubles representing the density at given points
     * 
     * @testPassCriteria if densities are the same as CelestLab v3.1
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 3.0.1
     * 
     * @comments threshold is set to 10<sup>-15</sup> on a relative scale, machine precision. Reference was generated
     *           with MSLIB implementation of US76.
     * @throws PatriusException
     *         if US76 altitude range is not in range 0 to 1000 km (should not happen)
     */
    @Test
    public final void testAtmosphere() throws PatriusException {
        // This test aims to check that results agree with the reference one up to a relative error of EPS = 1e-15

        Report.printMethodHeader("testAtmosphere", "Density computation", "Celestlab 3.1.0", EPS,
            ComparisonType.RELATIVE);

        // Note1: reference values hereafter come from the MSLIB implementation of US76 (CelestLab v3.1 version)
        // Note2: a more systematic has been carried out, namely comparing the density with reference values in [0,
        // 1000] km
        // with a 10 m step. Relative error is always < EPS.

        final double alt1 = 50000.0;
        final double alt2 = 83000.0;
        final double alt3 = 88000.0;
        final double alt4 = 100000.0;
        final double alt5 = 115000.0;
        final double alt6 = 300000.0;

        final Vector3D pos1 = new Vector3D(new double[] { this.ae + alt1, 0, 0 });
        final Vector3D pos2 = new Vector3D(new double[] { this.ae + alt2, 0, 0 });
        final Vector3D pos3 = new Vector3D(new double[] { this.ae + alt3, 0, 0 });
        final Vector3D pos4 = new Vector3D(new double[] { this.ae + alt4, 0, 0 });
        final Vector3D pos5 = new Vector3D(new double[] { this.ae + alt5, 0, 0 });
        final Vector3D pos6 = new Vector3D(new double[] { this.ae + alt6, 0, 0 });

        // alt = 50km
        final double density1 = this.atmosModel1.getDensity(this.date1, pos1, this.frame1);
        final double ref1 = 1.026819651324287468E-03;

        // alt = 83km
        final double density2 = this.atmosModel1.getDensity(this.date1, pos2, this.frame1);
        final double ref2 = 1.141408788899629182E-05;

        // alt = 88km
        final double density3 = this.atmosModel1.getDensity(this.date1, pos3, this.frame1);
        final double ref3 = 4.874927458014086139E-06;

        // alt = 100km
        final double density4 = this.atmosModel1.getDensity(this.date1, pos4, this.frame1);
        final double ref4 = 5.604393691649959252E-07;

        // alt = 115km
        final double density5 = this.atmosModel1.getDensity(this.date1, pos5, this.frame1);
        final double ref5 = 4.288778336670236762E-08;

        // alt = 300km
        final double density6 = this.atmosModel1.getDensity(this.date1, pos6, this.frame1);
        final double ref6 = 1.915751770516826004E-11;

        // Source for reference values:
        // US76-MSLIB implementation

        // Comparison with reference values (altitude < 86km)
        Assert.assertEquals(0, MathLib.abs((density1 - ref1) / ref1), EPS);
        Assert.assertEquals(0, MathLib.abs((density2 - ref2) / ref2), EPS);

        // Comparison with reference values (altitude > 86km)
        Assert.assertEquals(0, MathLib.abs((density3 - ref3) / ref3), EPS);
        Assert.assertEquals(0, MathLib.abs((density4 - ref4) / ref4), EPS);
        Assert.assertEquals(0, MathLib.abs((density5 - ref5) / ref5), EPS);
        Assert.assertEquals(0, MathLib.abs((density6 - ref6) / ref6), EPS);

        Report.printToReport("Density (altitude: 50km)", ref1, density1);
        Report.printToReport("Density (altitude: 83km)", ref2, density2);
        Report.printToReport("Density (altitude: 88km)", ref3, density3);
        Report.printToReport("Density (altitude: 100km)", ref4, density4);
        Report.printToReport("Density (altitude: 115km)", ref5, density5);
        Report.printToReport("Density (altitude: 300km)", ref6, density6);
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

    /**
     * Test on the reference altitudes found at page 7 of reference US76 documentation.
     * This test is up to validate the temperature, pressure and density computed respectively
     * by methods of class {@link #US76} at each altitudes.
     * 
     * @throws PatriusException
     *         if US76 altitude range is not in range 0 to 1000 km
     * 
     * @referenceVersion 3.1
     * 
     */
    @Test
    public void testNoteTechnique() throws PatriusException {
        // Test page 7 of Technical documentation US76_NT

        // Tests' EPS reflect the number of significant digits in the reference document.

        // At least 4 significant figures in common with the reference (used for reference value that only has 5 s.d.)
        final double EPS1 = 5 * 1E-5;
        // At least 3 significant figures in common with the reference (used for reference value that only has 4 s.d.)
        final double EPS2 = 5 * 1E-4;

        Report.printMethodHeader("testNoteTechnique", "Density and temperature computation", "US76 Technical note",
            EPS2, ComparisonType.RELATIVE);

        // Altitudes 1 - 4 of the reference table
        // are geopotential altitudes => must convert to geodetic altitude before test!
        final double alt0 = this.computeZ(0);
        final double alt1 = this.computeZ(20E3);
        final double alt2 = this.computeZ(40E3);
        final double alt3 = this.computeZ(60E3);
        final double alt4 = this.computeZ(80E3);
        final double alt5 = 85000.0;
        final double alt6 = 90000.0;
        final double alt7 = 100000.0;
        final double alt8 = 120000.0;

        final GeodeticPoint gp0 = new GeodeticPoint(0, 0, alt0);
        final GeodeticPoint gp1 = new GeodeticPoint(0, 0, alt1);
        final GeodeticPoint gp2 = new GeodeticPoint(0, 0, alt2);
        final GeodeticPoint gp3 = new GeodeticPoint(0, 0, alt3);
        final GeodeticPoint gp4 = new GeodeticPoint(0, 0, alt4);
        final GeodeticPoint gp5 = new GeodeticPoint(0, 0, alt5);
        final GeodeticPoint gp6 = new GeodeticPoint(0, 0, alt6);
        final GeodeticPoint gp7 = new GeodeticPoint(0, 0, alt7);
        final GeodeticPoint gp8 = new GeodeticPoint(0, 0, alt8);

        // density at sea level
        final double density0 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp0), this.frame1);

        // pressure at sea level
        final double pressure0 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp0), this.frame1);

        // temperature at sea level
        final double temp0 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp0), this.frame1);
        final double refTemp0 = 288.15;

        // alt = 20km
        final double density1 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp1), this.frame1);
        final double pressure1 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp1), this.frame1);
        final double temp1 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp1), this.frame1);
        final double refDensity1 = 7.1865E-2;
        final double refPressure1 = 5.4032E-2;
        final double refTemp1 = 216.65;

        // alt = 40km
        final double density2 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp2), this.frame1);
        final double pressure2 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp2), this.frame1);
        final double temp2 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp2), this.frame1);
        final double refDensity2 = 3.1437E-03;
        final double refPressure2 = 2.7389E-3;
        final double refTemp2 = 251.05;

        // alt = 60km
        final double density3 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp3), this.frame1);
        final double pressure3 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp3), this.frame1);
        final double temp3 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp3), this.frame1);
        final double refDensity3 = 2.3536E-04;
        final double refPressure3 = 2.0048E-4;
        final double refTemp3 = 245.45;

        // alt = 80km
        final double density4 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp4), this.frame1);
        final double pressure4 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp4), this.frame1);
        final double temp4 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp4), this.frame1);
        final double refDensity4 = 1.2817E-05;
        final double refPressure4 = 8.7468E-6;
        final double refTemp4 = 196.65;

        // alt = 85km
        final double density5 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp5), this.frame1);
        final double pressure5 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp5), this.frame1);
        final double temp5 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp5), this.frame1);
        final double refDensity5 = 6.7098E-06;
        final double refPressure5 = 4.3985E-6;
        final double refTemp5 = 188.84;

        // alt = 90km
        final double density6 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp6), this.frame1);
        final double pressure6 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp6), this.frame1);
        final double temp6 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp6), this.frame1);
        final double refDensity6 = 2.789E-6;
        final double refPressure6 = 1.8119E-6;
        final double refTemp6 = 186.87;

        // alt = 100km
        final double density7 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp7), this.frame1);
        final double pressure7 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp7), this.frame1);
        final double temp7 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp7), this.frame1);
        final double refDensity7 = 4.575E-7;
        final double refPressure7 = 3.1593E-7;
        final double refTemp7 = 195.08;

        // alt = 120km
        final double density8 = this.atmosModel1.getDensity(this.date1, this.earth1.transform(gp8), this.frame1);
        final double pressure8 = this.atmosModel1.getPress(this.date1, this.earth1.transform(gp8), this.frame1);
        final double temp8 = this.atmosModel1.getTemp(this.date1, this.earth1.transform(gp8), this.frame1);
        final double refDensity8 = 1.814E-8;
        final double refPressure8 = 2.5050E-8;
        final double refTemp8 = 360.00;

        // Compare with table at page 7 of US76 technical documentation (CNES, 1990)

        // Densities
        Assert.assertEquals(0, MathLib.abs((refDensity1 - density1 / density0) / refDensity1), EPS1);
        Assert.assertEquals(0, MathLib.abs((refDensity2 - density2 / density0) / refDensity2), EPS1);
        Assert.assertEquals(0, MathLib.abs((refDensity3 - density3 / density0) / refDensity3), EPS1);
        Assert.assertEquals(0, MathLib.abs((refDensity4 - density4 / density0) / refDensity4), EPS1);
        Assert.assertEquals(0, MathLib.abs((refDensity5 - density5 / density0) / refDensity5), EPS1);
        Assert.assertEquals(0, MathLib.abs((refDensity6 - density6 / density0) / refDensity6), EPS2);
        Assert.assertEquals(0, MathLib.abs((refDensity7 - density7 / density0) / refDensity7), EPS2);
        Assert.assertEquals(0, MathLib.abs((refDensity8 - density8 / density0) / refDensity8), EPS2);

        // Pressures
        Assert.assertEquals(0, MathLib.abs((refPressure1 - pressure1 / pressure0) / refPressure1), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure2 - pressure2 / pressure0) / refPressure2), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure3 - pressure3 / pressure0) / refPressure3), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure4 - pressure4 / pressure0) / refPressure4), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure5 - pressure5 / pressure0) / refPressure5), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure6 - pressure6 / pressure0) / refPressure6), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure7 - pressure7 / pressure0) / refPressure7), EPS1);
        Assert.assertEquals(0, MathLib.abs((refPressure8 - pressure8 / pressure0) / refPressure8), EPS2);

        // Temperatures
        Assert.assertEquals(0, MathLib.abs((refTemp0 - temp0) / refTemp0), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp1 - temp1) / refTemp1), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp2 - temp2) / refTemp2), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp3 - temp3) / refTemp3), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp4 - temp4) / refTemp4), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp5 - temp5) / refTemp5), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp6 - temp6) / refTemp6), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp7 - temp7) / refTemp7), EPS1);
        Assert.assertEquals(0, MathLib.abs((refTemp8 - temp8) / refTemp8), EPS1);

        // Print a report for these results

        // Densities
        Report.printToReport("Density (altitude: 20km)", refDensity1, density1 / density0);
        Report.printToReport("Density (altitude: 40km)", refDensity2, density2 / density0);
        Report.printToReport("Density (altitude: 60km)", refDensity3, density3 / density0);
        Report.printToReport("Density (altitude: 80km)", refDensity4, density4 / density0);
        Report.printToReport("Density (altitude: 85km)", refDensity5, density5 / density0);
        Report.printToReport("Density (altitude: 90km)", refDensity6, density6 / density0);
        Report.printToReport("Density (altitude: 100km)", refDensity7, density7 / density0);
        Report.printToReport("Density (altitude: 120km)", refDensity8, density8 / density0);

        // Pressures
        Report.printToReport("Pressure (altitude: 20km)", refPressure1, pressure1 / pressure0);
        Report.printToReport("Pressure (altitude: 40km)", refPressure2, pressure2 / pressure0);
        Report.printToReport("Pressure (altitude: 60km)", refPressure3, pressure3 / pressure0);
        Report.printToReport("Pressure (altitude: 80km)", refPressure4, pressure4 / pressure0);
        Report.printToReport("Pressure (altitude: 85km)", refPressure5, pressure5 / pressure0);
        Report.printToReport("Pressure (altitude: 90km)", refPressure6, pressure6 / pressure0);
        Report.printToReport("Pressure (altitude: 100km)", refPressure7, pressure7 / pressure0);
        Report.printToReport("Pressure (altitude: 120km)", refPressure8, pressure8 / pressure0);

        // Temperatures
        Report.printToReport("Temperature (altitude: 0km (sea level)", refTemp0, temp0);
        Report.printToReport("Temperature (altitude: 20km)", refTemp1, temp1);
        Report.printToReport("Temperature (altitude: 40km)", refTemp2, temp2);
        Report.printToReport("Temperature (altitude: 60km)", refTemp3, temp3);
        Report.printToReport("Temperature (altitude: 80km)", refTemp4, temp4);
        Report.printToReport("Temperature (altitude: 85km)", refTemp5, temp5);
        Report.printToReport("Temperature (altitude: 90km)", refTemp6, temp6);
        Report.printToReport("Temperature (altitude: 100km)", refTemp7, temp7);
        Report.printToReport("Temperature (altitude: 120km)", refTemp8, temp8);

    }

    /**
     * Test that aims at showing the "cache" process of class {@link #US76} :
     * densities, pressures and temperatures are recomputed when one of the methods
     * {@link US76#getDensity(AbsoluteDate, Vector3D, Frame)}, {@link US76#getTemp(AbsoluteDate, Vector3D, Frame)},
     * {@link US76#getPress(AbsoluteDate, Vector3D, Frame)} is successively called with different parameters.
     * 
     * @throws PatriusException
     *         if US76 altitude range is not in range 0 to 1000 km
     * @referenceVersion 3.2
     * 
     */
    @Test
    public void testRecomputed() throws PatriusException {

        // Altitude
        final double alt1 = this.computeZ(20E3);
        final double alt2 = this.computeZ(40E3);

        // Geodetic point
        final GeodeticPoint gp1 = new GeodeticPoint(0, 0, alt1);
        final GeodeticPoint gp2 = new GeodeticPoint(0, 0, alt2);

        // Positions
        final Vector3D pos1 = this.earth1.transform(gp1);
        final Vector3D pos2 = this.earth1.transform(gp2);

        // GCRF
        final Frame gcrf = FramesFactory.getGCRF();

        // Check that the density, pressure and temperature depends on the frame used
        // so they are recomputed and lead to different results

        // When calling the following 3 methods, the parameters in cache are :
        // (date1, pos1, frame1)
        final double density1_ITRF = this.atmosModel1.getDensity(this.date1, pos1, this.frame1);
        final double press1_ITRF = this.atmosModel1.getPress(this.date1, pos1, this.frame1);
        final double temp1_ITRF = this.atmosModel1.getTemp(this.date1, pos1, this.frame1);

        // Recomputation occur here : parameters in cache are now :
        // (date1, pos1, gcrf), so results are different
        final double density1_GCRF = this.atmosModel1.getDensity(this.date1, pos1, gcrf);
        Assert.assertFalse(density1_GCRF == density1_ITRF);

        // (date1, pos1, gcrf) => different results
        final double press1_GCRF = this.atmosModel1.getPress(this.date1, pos1, gcrf);
        Assert.assertFalse(press1_GCRF == press1_ITRF);

        // (date1, pos1, gcrf) => different results
        final double temp1_GCRF = this.atmosModel1.getTemp(this.date1, pos1, gcrf);
        Assert.assertFalse(temp1_GCRF == temp1_ITRF);

        // Check also that values are recomputed if the position changes

        // (date1, pos2, frame1) are now in cache
        final double density2 = this.atmosModel1.getDensity(this.date1, pos2, this.frame1);
        final double press2 = this.atmosModel1.getPress(this.date1, pos2, this.frame1);
        final double temp2 = this.atmosModel1.getTemp(this.date1, pos2, this.frame1);

        // Values are updated so different from previous
        Assert.assertFalse(density1_ITRF == density2);
        Assert.assertFalse(press1_ITRF == press2);
        Assert.assertFalse(temp1_ITRF == temp2);

        // Finally, check that changing the dates leads to recomputation in GCRF
        // Same idea about parameters in cache
        final double density_otherDate = this.atmosModel1.getDensity(this.date1.shiftedBy(3600), pos1, gcrf);
        final double press_otherDate = this.atmosModel1.getPress(this.date1.shiftedBy(3600), pos1, gcrf);
        final double temp_otherDate = this.atmosModel1.getTemp(this.date1.shiftedBy(3600), pos1, gcrf);

        Assert.assertFalse(density1_GCRF == density_otherDate);
        Assert.assertFalse(press1_GCRF == press_otherDate);
        Assert.assertFalse(temp1_GCRF == temp_otherDate);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#US76}
     * 
     * @testedMethod {@link US76#getDensity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description Compute density for z < 0 km
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output Density value at the lowest bound of validity interval (0 km).
     * 
     * @testPassCriteria if retuned density is Rho(z = 0)
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 3.0.1
     * 
     * @comments domain of validity depends on US76 technical description
     */
    @Test
    public final void testOutOfBound1() throws PatriusException {

        // Verify that for z < z_min => rho(z) = rho(z_min)
        final GeodeticPoint gp_zmin = new GeodeticPoint(0, 0, -10);

        final GeodeticPoint gp_znegative = new GeodeticPoint(0, 0, -10);

        final Vector3D posNegative = this.earth1.transform(gp_znegative);
        final Vector3D posMin = this.earth1.transform(gp_zmin);

        final double rho1 = this.atmosModel1.getDensity(this.date1, posNegative, this.frame1);
        final double rho2 = this.atmosModel1.getDensity(this.date1, posMin, this.frame1);

        Assert.assertEquals(rho1, rho2);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#US76}
     * 
     * @testedMethod {@link US76#getDensity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description Compute density for z > 1000 km
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output Density value at the upper bound of validity interval (1000 km).
     * 
     * @testPassCriteria if retuned density is Rho(z = 1000 km)
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 3.0.1
     * 
     * @comments domain of validity depends on US76 technical description
     */
    @Test
    public final void testOutOfBound2() throws PatriusException {

        // Verify that for z > z_max => rho(z) = rho(z_max)
        final GeodeticPoint gp_zmax = new GeodeticPoint(0, 0, 1000.e3);
        final GeodeticPoint gp_outOfBound = new GeodeticPoint(0, 0, 10000.e3);

        final Vector3D posOutOfBound = this.earth1.transform(gp_outOfBound);
        final Vector3D posMax = this.earth1.transform(gp_zmax);

        final double rho1 = this.atmosModel1.getDensity(this.date1, posOutOfBound, this.frame1);
        final double rho2 = this.atmosModel1.getDensity(this.date1, posMax, this.frame1);

        Assert.assertEquals(rho1, rho2);
    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#US76}
     * 
     * @testedMethod {@link US76#getVelocity(AbsoluteDate, Vector3D, Frame)}
     * 
     * @description the test computes the velocity for given date, positions and frame
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output a Vector3D representing the velocity of atmosphere particles,
     *         expressed in the same frame as that of user
     *         given position
     * 
     * @testPassCriteria if velocity is as expected
     * 
     * @referenceVersion 2.3
     * 
     * @nonRegressionVersion 2.3
     * 
     * @comments threshold is set to 10<sup>-15</sup> on a relative scale
     */
    @Test
    public final void testGetVelocity() throws PatriusException {

        final double alt1 = 100000.0;
        final Vector3D pos1 = new Vector3D(new double[] { this.ae + alt1, 0, 0 });

        // reference velocity is the velocity in EME2000 Frame of (the static point in ITRF frame)
        // that has pos1 as position
        // in EME2000 frame
        final Transform EMEToBody = this.frame1.getTransformTo(this.earth1.getBodyFrame(), this.date1);
        final Vector3D posBody = EMEToBody.transformPosition(pos1);
        final PVCoordinates nPV = EMEToBody.getInverse().transformPVCoordinates(
            new PVCoordinates(posBody, Vector3D.ZERO));
        final Vector3D refVel = nPV.getVelocity();

        // computed velocity
        final Vector3D cmpVel = this.atmosModel1.getVelocity(this.date1, pos1, this.frame1);

        this.checkVectors(refVel, cmpVel, EPS);

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#US76}
     * 
     * @testedMethod {@link US76#getSpeedOfSound(date, position, frame)}
     * 
     * @description the test computes the speed of sound for different altitudes
     *              and compare with reference values from CelestLab v3.1
     * 
     * @input an {@link AbsoluteDate}, a {@link Vector3D} and a {@link Frame}
     * 
     * @output speed of sound
     * 
     * @testPassCriteria if speed of sound is as expected
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0.1
     * 
     * @comments threshold is set to 10<sup>-15</sup> on a relative scale
     * 
     */
    @Test
    public final void testGetSpeedOfSound() throws PatriusException {

        final double alt1 = 50000.0;
        final double alt2 = 83000.0;
        final double alt3 = 88000.0;
        final double alt4 = 100000.0;
        final double alt5 = 115000.0;
        final double alt6 = 300000.0;

        final Vector3D pos1 = new Vector3D(new double[] { this.ae + alt1, 0, 0 });
        final Vector3D pos2 = new Vector3D(new double[] { this.ae + alt2, 0, 0 });
        final Vector3D pos3 = new Vector3D(new double[] { this.ae + alt3, 0, 0 });
        final Vector3D pos4 = new Vector3D(new double[] { this.ae + alt4, 0, 0 });
        final Vector3D pos5 = new Vector3D(new double[] { this.ae + alt5, 0, 0 });
        final Vector3D pos6 = new Vector3D(new double[] { this.ae + alt6, 0, 0 });

        // alt = 50km
        final double v1 = this.atmosModel1.getSpeedOfSound(this.date1, pos1, this.frame1);
        final double ref1 = 329.7988470709885;

        // alt = 83km
        final double v2 = this.atmosModel1.getSpeedOfSound(this.date1, pos2, this.frame1);
        final double ref2 = 278.3471807932033;

        // alt = 88km
        final double v3 = this.atmosModel1.getSpeedOfSound(this.date1, pos3, this.frame1);
        final double ref3 = 274.1577199717601;

        // alt = 100km
        final double v4 = this.atmosModel1.getSpeedOfSound(this.date1, pos4, this.frame1);
        final double ref4 = 282.7882930136855;

        // alt = 115km
        final double v5 = this.atmosModel1.getSpeedOfSound(this.date1, pos5, this.frame1);
        final double ref5 = 361.7875877849136;

        // alt = 300km
        final double v6 = this.atmosModel1.getSpeedOfSound(this.date1, pos6, this.frame1);
        final double ref6 = 800.574506981326;

        // Source for reference values:
        // US76-MSLIB implementation

        // Comparison with reference values (altitude < 86km)
        Assert.assertEquals(0, MathLib.abs((v1 - ref1) / ref1), EPS);
        Assert.assertEquals(0, MathLib.abs((v2 - ref2) / ref2), EPS);

        // Comparison with reference values (altitude > 86km)
        Assert.assertEquals(0, MathLib.abs((v3 - ref3) / ref3), EPS);
        Assert.assertEquals(0, MathLib.abs((v4 - ref4) / ref4), EPS);
        Assert.assertEquals(0, MathLib.abs((v5 - ref5) / ref5), EPS);
        Assert.assertEquals(0, MathLib.abs((v6 - ref6) / ref6), EPS);
    }

    /**
     * setup method
     * 
     * @throws PatriusException
     *         if data embedded in the library cannot be read
     */
    @Before
    public void testSetup() throws PatriusException {

        Utils.setDataRoot("regular-dataCNES-2010");

        // Thu Mar 31 22:16:55 GMT 2011
        this.date1 = new AbsoluteDate(2005, 03, 31, 22, 16, 55.4778569, TimeScalesFactory.getTAI());

        // Earth
        this.frame1 = FramesFactory.getITRF();

        final double f = Constants.GRIM5C1_EARTH_FLATTENING;
        this.ae = Constants.GRIM5C1_EARTH_EQUATORIAL_RADIUS;
        this.earth1 = new OneAxisEllipsoid(this.ae, f, this.frame1);
        this.earth1.setAngularThreshold(EPS);
        this.earth1.setCloseApproachThreshold(EPS);

        this.atmosModel1 = new US76(this.earth1);

    }

    /**
     * @testType UT
     * 
     * 
     * @description Test the getters of a class.
     * 
     * @input the class parameters
     * 
     * @output the class parameters
     * 
     * @testPassCriteria the parameters of the class are the same in input and
     *                   output
     * 
     * @referenceVersion 4.1
     * 
     * @nonRegressionVersion 4.1
     */
    @Test
    public void testGetters() {
        final BodyShape earthBody = new OneAxisEllipsoid(Constants.CNES_STELA_AE,
            Constants.GRIM5C1_EARTH_FLATTENING, FramesFactory.getCIRF());
        final US76 atm = new US76(earthBody);
        final US76 atm2 = (US76) atm.copy();
        Assert.assertTrue(earthBody.equals(atm2.getEarthBody()));
    }

    /**
     * Check vectors are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    private void checkVectors(final Vector3D exp, final Vector3D act, final double eps) {
        Assert.assertEquals(exp.getX(), act.getX(), eps);
        Assert.assertEquals(exp.getY(), act.getY(), eps);
        Assert.assertEquals(exp.getZ(), act.getZ(), eps);
    }

}
