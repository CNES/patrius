/**
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
/* Copyright 2011-2012 Space Applications Services
 * 
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:92:17/05/2013:Added tests for coverage
 * VERSION::FA:306:12/11/2014:coverage
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.models.earth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.NumberFormat;
import java.util.StringTokenizer;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.models.earth.GeoMagneticFieldFactory.FieldModel;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

public class GeoMagneticFieldTest {

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(GeoMagneticFieldTest.class.getSimpleName(), "Geomagnetic field");
    }

    @Test
    public void testWMM() throws Exception {

        final double eps = 1e-1;
        Report.printMethodHeader("testWMM", "WMM model computation", "WMM2010 Report", eps, ComparisonType.ABSOLUTE);

        // test values from sample coordinate file
        this.runSampleFile(FieldModel.WMM, "sample_coords.txt", "sample_out_WMM2010.txt");

        // test results for test values provided as part of the WMM2010 Report
        final double[][] testValues = {
            // Date Alt Lat Lon X Y Z H F I D
            // km deg deg nT nT nT nT nT deg deg
            { 2010.0, 0, 80, 0, 6649.5, -714.6, 54346.2, 6687.8, 54756.2, 82.98, -6.13 },
            { 2010.0, 0, 0, 120, 39428.8, 664.9, -11683.8, 39434.5, 41128.9, -16.50, 0.97 },
            { 2010.0, 0, -80, 240, 5657.7, 15727.3, -53407.5, 16714.0, 55961.8, -72.62, 70.21 },
            { 2010.0, 100, 80, 0, 6332.2, -729.1, 52194.9, 6374.0, 52582.6, 83.04, -6.57 },
            { 2010.0, 100, 0, 120, 37452.0, 611.9, -11180.8, 37457.0, 39090.1, -16.62, 0.94 },
            { 2010.0, 100, -80, 240, 5484.3, 14762.8, -50834.8, 15748.6, 53218.3, -72.79, 69.62 },
            { 2012.5, 0, 80, 0, 6658.0, -606.7, 54420.4, 6685.5, 54829.5, 83.00, -5.21 },
            { 2012.5, 0, 0, 120, 39423.9, 608.1, -11540.5, 39428.6, 41082.8, -16.31, 0.88 },
            { 2012.5, 0, -80, 240, 5713.6, 15731.8, -53184.3, 16737.2, 55755.7, -72.53, 70.04 },
            { 2012.5, 100, 80, 0, 6340.9, -625.1, 52261.9, 6371.6, 52648.9, 83.05, -5.63 },
            { 2012.5, 100, 0, 120, 37448.1, 559.7, -11044.2, 37452.2, 39046.7, -16.43, 0.86 },
            { 2012.5, 100, -80, 240, 5535.5, 14765.4, -50625.9, 15768.9, 53024.9, -72.70, 69.45 }
        };

        for (int i = 0; i < testValues.length; i++) {
            final GeoMagneticField model = GeoMagneticFieldFactory.getWMM(testValues[i][0]);
            final GeoMagneticElements result = model.calculateField(testValues[i][2],
                testValues[i][3],
                testValues[i][1]);

            // X
            Assert.assertEquals(testValues[i][4], result.getFieldVector().getX(), eps);
            // Y
            Assert.assertEquals(testValues[i][5], result.getFieldVector().getY(), eps);
            // Z
            Assert.assertEquals(testValues[i][6], result.getFieldVector().getZ(), eps);
            // H
            Assert.assertEquals(testValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(testValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(testValues[i][9], result.getInclination(), eps);
            // declination
            Assert.assertEquals(testValues[i][10], result.getDeclination(), eps);

            Report.printToReport("Field vector (case " + i + ")", new Vector3D(testValues[i][4], testValues[i][5],
                testValues[i][6]), result.getFieldVector());
            Report.printToReport("Horizontal intensity (case " + i + ")", testValues[i][7],
                result.getHorizontalIntensity());
            Report.printToReport("Total intensity (case " + i + ")", testValues[i][8], result.getTotalIntensity());
            Report.printToReport("Inclination (case " + i + ")", testValues[i][9], result.getInclination());
            Report.printToReport("Declination (case " + i + ")", testValues[i][10], result.getDeclination());
        }
    }

    @Test
    public void testIGRF() throws Exception {

        final double eps = 1e0;
        Report.printMethodHeader("testIGRF", "IGRF model computation", "WMM2010 Report", eps, ComparisonType.ABSOLUTE);

        // test values from sample coordinate file
        this.runSampleFile(FieldModel.IGRF, "sample_coords.txt", "sample_out_IGRF11.txt");

        // test results for test values provided as part of the WMM2010 Report
        // the results for the IGRF11 model have been obtained from the NOAA
        // online calculator:
        // http://www.ngdc.noaa.gov/geomagmodels/IGRFWMM.jsp

        final double[][] testValues = {
            // Date Alt Lat Lon X Y Z H F I D
            // km deg deg nT nT nT nT nT deg deg
            { 2010.0, 0, 80, 0, 6657.7, -720.7, 54354.1, 6696.6, 54765.1, 82.983, -6.183 },
            { 2010.0, 0, 0, 120, 39427.4, 660.4, -11679.4, 39433.0, 41126.2, -16.500, 0.966 },
            { 2010.0, 100, 80, 0, 6338.8, -734.0, 52201.4, 6381.2, 52590.0, 83.033, -6.600 },
            { 2010.0, 100, 0, 120, 37450.6, 608.7, -11177.2, 37455.6, 39087.7, -16.617, 0.933 },
            { 2012.5, 0, 80, 0, 6657.2, -609.6, 54407.0, 6685.1, 54816.2, 83.000, -5.233 },
            { 2012.5, 0, 0, 120, 39431.8, 589.4, -11520.2, 39436.2, 41084.4, -16.283, 0.850 },
            { 2012.5, 100, 80, 0, 6339.7, -626.9, 52248.7, 6370.6, 52635.6, 83.050, -5.650 },
            { 2012.5, 100, 0, 120, 37455.4, 543.0, -11025.5, 37459.4, 39048.3, -16.400, 0.833 }
        };

        for (int i = 0; i < testValues.length; i++) {
            final GeoMagneticField model = GeoMagneticFieldFactory.getIGRF(testValues[i][0]);
            final GeoMagneticElements result = model.calculateField(testValues[i][2],
                testValues[i][3],
                testValues[i][1]);

            final Vector3D b = result.getFieldVector();

            // X
            Assert.assertEquals(testValues[i][4], b.getX(), eps);
            // Y
            Assert.assertEquals(testValues[i][5], b.getY(), eps);
            // Z
            Assert.assertEquals(testValues[i][6], b.getZ(), eps);
            // H
            Assert.assertEquals(testValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(testValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(testValues[i][9], result.getInclination(), eps);
            // declination
            Assert.assertEquals(testValues[i][10], result.getDeclination(), eps);

            Report.printToReport("Field vector (case " + i + ")", new Vector3D(testValues[i][4], testValues[i][5],
                testValues[i][6]), result.getFieldVector());
            Report.printToReport("Horizontal intensity (case " + i + ")", testValues[i][7],
                result.getHorizontalIntensity());
            Report.printToReport("Total intensity (case " + i + ")", testValues[i][8], result.getTotalIntensity());
            Report.printToReport("Inclination (case " + i + ")", testValues[i][9], result.getInclination());
            Report.printToReport("Declination (case " + i + ")", testValues[i][10], result.getDeclination());

            // toString for coverage
            if (i == 0) {
                final String s = result.toString();
                final NumberFormat f = NumberFormat.getInstance();
                final String B = result.getFieldVector().toString(f);
                final String H = f.format(result.getHorizontalIntensity());
                final String F = f.format(result.getTotalIntensity());
                final String I = f.format(result.getInclination());
                final String D = f.format(result.getDeclination());

                Assert.assertEquals(s,
                    "MagneticField[B=" + B + ",H=" + H + ",F=" + F + ",I=" + I + ",D=" + D + "]");
            }
        }
    }

    @Test
    public void testCartesianPoint() throws Exception {

        final double eps = 1e-6;

        final OneAxisEllipsoid body =
            new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING,
                FramesFactory.getITRF());
        final Vector3D point =
            body.transform(new GeodeticPoint(MathLib.toRadians(-89.99), MathLib.toRadians(-180.0), 10. * 1000.));

        final Transform t = FramesFactory.getITRF()
            .getTransformTo(FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH);

        // IGRF model
        final GeoMagneticField model = GeoMagneticFieldFactory.getIGRF(2010.0);
        final GeoMagneticElements resultIGRF = model.calculateField(t.transformVector(point),
            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH);
        final GeoMagneticElements refIGRF = model.transformModel(
            GeoMagneticField.getDecimalYear(AbsoluteDate.J2000_EPOCH)).calculateField(-89.99, -180.0, 10.);

        Assert.assertEquals(resultIGRF.getFieldVector().getX(), refIGRF.getFieldVector().getX(), eps);
        Assert.assertEquals(resultIGRF.getFieldVector().getY(), refIGRF.getFieldVector().getY(), eps);
        Assert.assertEquals(resultIGRF.getFieldVector().getZ(), refIGRF.getFieldVector().getZ(), eps);

        // To cover code, need to clean the factory with this line
        Utils.setDataRoot("earth");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AbsoluteDate d = AbsoluteDate.J2000_EPOCH.shiftedBy(1000000000);
        final GeoMagneticField model1 = GeoMagneticFieldFactory.getIGRF(d);
        final GeoMagneticElements result1 = model1.calculateField(t.transformVector(point), FramesFactory.getEME2000(),
            AbsoluteDate.J2000_EPOCH);

        Assert.assertEquals(result1.getFieldVector().getX(), refIGRF.getFieldVector().getX(), eps);
        Assert.assertEquals(result1.getFieldVector().getY(), refIGRF.getFieldVector().getY(), eps);
        Assert.assertEquals(result1.getFieldVector().getZ(), refIGRF.getFieldVector().getZ(), eps);

        final GeoMagneticField modelIGRF1 = GeoMagneticFieldFactory
            .getField(GeoMagneticFieldFactory.FieldModel.IGRF, d);
        final GeoMagneticElements resultIGRF1 = modelIGRF1.calculateField(t.transformVector(point),
            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH);
        final GeoMagneticElements refIGRF1 = modelIGRF1.transformModel(
            GeoMagneticField.getDecimalYear(AbsoluteDate.J2000_EPOCH)).calculateField(-89.99, -180.0, 10.);

        Assert.assertEquals(resultIGRF1.getFieldVector().getX(), refIGRF1.getFieldVector().getX(), eps);
        Assert.assertEquals(resultIGRF1.getFieldVector().getY(), refIGRF1.getFieldVector().getY(), eps);
        Assert.assertEquals(resultIGRF1.getFieldVector().getZ(), refIGRF1.getFieldVector().getZ(), eps);

        // To cover code, need to clean the factory with this line
        Utils.setDataRoot("earth");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // WMM model with different date
        final GeoMagneticField modelWMM = GeoMagneticFieldFactory.getWMM(d);
        final GeoMagneticElements resultWMM = modelWMM.calculateField(t.transformVector(point),
            FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH);
        final GeoMagneticElements refWMM = modelWMM.transformModel(
            GeoMagneticField.getDecimalYear(AbsoluteDate.J2000_EPOCH)).calculateField(-89.99, -180.0, 10.);

        Assert.assertEquals(resultWMM.getFieldVector().getX(), refWMM.getFieldVector().getX(), eps);
        Assert.assertEquals(resultWMM.getFieldVector().getY(), refWMM.getFieldVector().getY(), eps);
        Assert.assertEquals(resultWMM.getFieldVector().getZ(), refWMM.getFieldVector().getZ(), eps);

        // WMM model with same date
        final GeoMagneticField modelWMM1 = GeoMagneticFieldFactory.getField(GeoMagneticFieldFactory.FieldModel.WMM, d);
        final Transform t1 = FramesFactory.getITRF().getTransformTo(FramesFactory.getEME2000(), d);
        final GeoMagneticElements resultWMM1 = modelWMM1.calculateField(t1.transformVector(point),
            FramesFactory.getEME2000(), d);
        final GeoMagneticElements refWMM1 = modelWMM1.calculateField(-89.99, -180.0, 10.);

        Assert.assertEquals(resultWMM1.getFieldVector().getX(), refWMM1.getFieldVector().getX(), eps);
        Assert.assertEquals(resultWMM1.getFieldVector().getY(), refWMM1.getFieldVector().getY(), eps);
        Assert.assertEquals(resultWMM1.getFieldVector().getZ(), refWMM1.getFieldVector().getZ(), eps);
    }

    @Test(expected = PatriusException.class)
    public void testUnsupportedTransform() throws Exception {
        final GeoMagneticField model = GeoMagneticFieldFactory.getIGRF(1910);

        // the IGRF model of 1910 does not have secular variation, thus time transformation is not supported
        model.transformModel(1950);
    }

    @Test(expected = PatriusException.class)
    public void testOutsideValidityTransform() throws Exception {
        final GeoMagneticField model1 = GeoMagneticFieldFactory.getIGRF(2005);
        final GeoMagneticField model2 = GeoMagneticFieldFactory.getIGRF(2010);

        // the interpolation transformation is only allowed between 2005 and 2010
        model1.transformModel(model2, 2012);
    }

    @Test
    public void testValidTransform() throws Exception {
        final GeoMagneticField model = GeoMagneticFieldFactory.getWMM(2010);

        Assert.assertTrue(model.supportsTimeTransform());

        final GeoMagneticField transformedModel = model.transformModel(2012);

        Assert.assertEquals(2010, transformedModel.validFrom(), 1e0);
        Assert.assertEquals(2015, transformedModel.validTo(), 1e0);
        Assert.assertEquals(2012, transformedModel.getEpoch(), 1e0);

        final GeoMagneticField otherModel = GeoMagneticFieldFactory.getIGRF(2012);
        final GeoMagneticField transformedOtherModel = model.transformModel(otherModel, 2014);
        Assert.assertEquals(2010, transformedOtherModel.validFrom(), 1e0);
        Assert.assertEquals(2015, transformedOtherModel.validTo(), 1e0);
        Assert.assertEquals(2014, transformedOtherModel.getEpoch(), 1e0);

        final GeoMagneticField transformedOtherModelInverse = otherModel.transformModel(model, 2014);
        Assert.assertEquals(2010, transformedOtherModelInverse.validFrom(), 1e0);
        Assert.assertEquals(2015, transformedOtherModelInverse.validTo(), 1e0);
        Assert.assertEquals(2014, transformedOtherModelInverse.getEpoch(), 1e0);
    }

    // Test for coverage purposes.
    @Test
    public void testCoverage() throws Exception {
        GeoMagneticFieldFactory.clearModels();
        // test the add/clear reader features:
        final GeoMagneticField model1 = GeoMagneticFieldFactory.getWMM(2010);
        // clear the readers and add new readers (they are the same readers as before):
        GeoMagneticFieldFactory.clearGeoMagneticModelReaders();
        final GeoMagneticModelReader testReader = new COFFileFormatReader("\\.COF$");
        GeoMagneticFieldFactory.addGeoMagneticModelReader(FieldModel.IGRF, testReader);
        GeoMagneticFieldFactory.addGeoMagneticModelReader(FieldModel.WMM, testReader);
        final GeoMagneticField model2 = GeoMagneticFieldFactory.getWMM(2010);
        final GeodeticPoint point = new GeodeticPoint(MathLib.toRadians(50.00), MathLib.toRadians(-180.0),
            10. * 1000.);
        // test the two models are the same
        Assert.assertEquals(model1.calculateField(point).getDeclination(), model2.calculateField(point)
            .getDeclination(), 0.0);
        Assert.assertEquals(model1.calculateField(point).getTotalIntensity(), model2.calculateField(point)
            .getTotalIntensity(), 0.0);
        Assert.assertEquals(model1.calculateField(point).getInclination(), model2.calculateField(point)
            .getInclination(), 0.0);

        // test an exception is thrown when the date is not valid (IGRF model not loaded yet):
        boolean rez = false;
        try {
            GeoMagneticFieldFactory.getIGRF(1950);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);

        // test an exception is thrown when the date is not valid (WMM model already loaded):
        rez = false;
        try {
            GeoMagneticFieldFactory.getWMM(1950);
        } catch (final PatriusException e) {
            rez = true;
        }
        Assert.assertTrue(rez);
    }

    public void runSampleFile(final FieldModel type, final String inputFile, final String outputFile)
        throws Exception {

        final BufferedReader inReader = new BufferedReader(new InputStreamReader(this.getResource(inputFile)));
        final BufferedReader outReader = new BufferedReader(new InputStreamReader(this.getResource(outputFile)));

        // read header line
        outReader.readLine();

        String line = null;
        while ((line = inReader.readLine()) != null) {
            if (line.trim().length() == 0) {
                break;
            }

            final StringTokenizer st = new StringTokenizer(line);

            final double year = this.getYear(st.nextToken());
            final String heightType = st.nextToken();
            final String heightStr = st.nextToken();
            final double lat = this.getLatLon(st.nextToken());
            final double lon = this.getLatLon(st.nextToken());

            final GeoMagneticField field = GeoMagneticFieldFactory.getField(type, year);

            double height = Double.valueOf(heightStr.substring(1));
            if (heightStr.startsWith("M")) {
                // convert from m to km
                height /= 1000d;
            } else if (heightStr.startsWith("F")) {
                // convert from feet to km
                height *= 3.048e-4;
            }

            final GeoMagneticElements ge = field.calculateField(lat, lon, height);
            final String validateLine = outReader.readLine();
            // geocentric altitude is not yet supported, ignore by now
            if (!heightType.startsWith("C")) {
                this.validate(ge, validateLine);
            }
        }
    }

    private double getYear(final String yearStr) {

        if (yearStr.contains(",")) {
            final StringTokenizer st = new StringTokenizer(yearStr, ",");
            final String y = st.nextToken();
            final String m = st.nextToken();
            final String d = st.nextToken();
            return GeoMagneticField.getDecimalYear(Integer.valueOf(d), Integer.valueOf(m), Integer.valueOf(y));
        } else {
            return Double.valueOf(yearStr);
        }
    }

    private double getLatLon(final String str) {

        if (str.contains(",")) {
            final StringTokenizer st = new StringTokenizer(str, ",");
            final int d = Integer.valueOf(st.nextToken());
            final int m = Integer.valueOf(st.nextToken());
            int s = 0;
            if (st.hasMoreTokens()) {
                s = Integer.valueOf(st.nextToken());
            }
            double deg = MathLib.abs(d) + m / 60d + s / 3600d;
            if (d < 0) {
                deg = -deg;
            }
            return deg;
        } else {
            return Double.valueOf(str);
        }
    }

    private double getDegree(final String degree, final String minute) {
        double result = Double.valueOf(degree.substring(0, degree.length() - 1));
        final double min = Double.valueOf(minute.substring(0, minute.length() - 1)) / 60d;
        result += (result < 0) ? -min : min;
        return result;
    }

    @SuppressWarnings("unused")
    private void validate(final GeoMagneticElements ge, final String outputLine)
        throws Exception {

        final StringTokenizer st = new StringTokenizer(outputLine);

        this.getYear(st.nextToken());

        st.nextToken();
        st.nextToken();
        st.nextToken();
        st.nextToken();

        final double dec = this.getDegree(st.nextToken(), st.nextToken());
        final double inc = this.getDegree(st.nextToken(), st.nextToken());

        final double h = Double.valueOf(st.nextToken());
        final double x = Double.valueOf(st.nextToken());
        final double y = Double.valueOf(st.nextToken());
        final double z = Double.valueOf(st.nextToken());
        final double f = Double.valueOf(st.nextToken());

        final double eps = 1e-1;
        Assert.assertEquals(h, ge.getHorizontalIntensity(), eps);
        Assert.assertEquals(f, ge.getTotalIntensity(), eps);
        Assert.assertEquals(x, ge.getFieldVector().getX(), eps);
        Assert.assertEquals(y, ge.getFieldVector().getY(), eps);
        Assert.assertEquals(z, ge.getFieldVector().getZ(), eps);
        Assert.assertEquals(dec, ge.getDeclination(), eps);
        Assert.assertEquals(inc, ge.getInclination(), eps);
    }

    private InputStream getResource(final String name) throws FileNotFoundException {
        final String dataPath = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH);
        return new FileInputStream(new File(dataPath, name));
    }

    @Before
    public void setUp() throws Exception {
        Utils.setDataRoot("earth");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));
    }

}
