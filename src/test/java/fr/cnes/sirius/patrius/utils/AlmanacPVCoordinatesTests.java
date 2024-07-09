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
 * @history creation 10/11/2015
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:497:10/11/2015:Creation
 * VERSION::FA:564:31/03/2016: Issues related with GNSS almanac and PVCoordinatesPropagator
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.CNESUtils;
import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.AlmanacPVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.AlmanacParameter;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.GPSAlmanacParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.GalileoAlmanacParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class validates almanacPVCoordinates and GPSGalileoAlmanacParameter
 * </p>
 * 
 * @author chabaudp
 * 
 * @version $Id$
 * 
 * @since 3.1
 * 
 */
public class AlmanacPVCoordinatesTests {

    /**
     * Comparison epsilon
     */
    private static final double EPSILON_POS = 1E-10;
    private static final double EPSILON_VEL = 7E-5;

    /**
     * earth angular velocity used to produce cnes reference
     */
    private static final double earthAngularVelocity = 7.2921151467E-5;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Almanac gps and galileo tools
         * 
         * @featureDescription All the classes and methods associated to almanac
         *                     position velocity coordinates provider
         * 
         */
        ALMANACH
    }

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AlmanacPVCoordinatesTests.class.getSimpleName(), "Almanac PV coordinates");
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#ALMANACH}
     * 
     * @testedMethod {@link AlmanacPVCoordinates#AlmanacPVCoordinates(AlmanacParameter, double, double, double, int)}
     * @testedMethod {@link AlmanacPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * 
     * @description Compute a pv coordinates ephemeris from a reference almanac and compare
     *              to the reference ephemeris provided.
     * 
     * @input Reference almanac GPS
     *        ******** Week 840 almanac for PRN-01 ********
     *        ID: 01
     *        Health: 000
     *        Eccentricity: 0.4714012146E-002
     *        Time of Applicability(s): 61440.0000
     *        Orbital Inclination(rad): 0.9627610967
     *        Rate of Right Ascen(r/s): -0.8183198006E-008
     *        SQRT(A) (m 1/2): 5153.604004
     *        Right Ascen at Week(rad): 0.2102607542E+001
     *        Argument of Perigee(rad): 0.480977878
     *        Mean Anom(rad): 0.2217106699E+001
     *        Af0(s): 0.9536743164E-006
     *        Af1(s/s): 0.0000000000E+000
     *        week: 840
     * 
     * 
     * @output Position velocity coordinates in WGS84 each five minutes during two days
     * 
     * @testPassCriteria Relative difference on position and velocity lower than EPSILON_POS, EPSILON_VEL
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     * @throws IOException
     *         should not happen
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetPVCoordinatesGPS() throws IOException, PatriusException {

        Report.printMethodHeader("testGetPVCoordinatesGPS", "GPS PV coordinates computation", "Math", EPSILON_VEL,
            ComparisonType.RELATIVE);

        // Read reference ephemeris file
        final URL url = AlmanacPVCoordinatesTests.class.getClassLoader().getResource("almanac/" + "EphemPV");
        final MatrixFileReader demo = new MatrixFileReader(url.getPath());
        final double[][] data = demo.getData();

        final int length = data.length;

        final PVCoordinates[] pvCoord = new PVCoordinates[length];

        for (int i = 0; i < data.length; i++) {

            final double[] pos = { 0, 0, 0 };
            final double[] vel = { 0, 0, 0 };

            // Read pos
            for (int j = 0; j < 3; j++) {
                pos[j] = data[i][j];
                vel[j] = data[i][j + 3];
            }

            pvCoord[i] = new PVCoordinates(new Vector3D(pos), new Vector3D(vel));

        }

        final AlmanacParameter almanac =
            new GPSAlmanacParameters(
                61440.0,
                840,
                2.217106699,
                0.4714012146E-2,
                5153.604004,
                2.102607542,
                0.9627610967,
                0.480977878,
                -0.8183198006E-8);
        final AlmanacPVCoordinates almanacPV =
            new AlmanacPVCoordinates(
                almanac, 1,
                Constants.GRS80_EARTH_MU,
                earthAngularVelocity);

        final AbsoluteDate startDate = new AbsoluteDate(2015, 9, 27, 17, 4, 00, TimeScalesFactory.getGPS());
        final double step = 5 * 60;
        for (int i = 0; i < length; i++) {
            final AbsoluteDate currentDate = startDate.shiftedBy(i * step);
            final PVCoordinates pvCoordActual = almanacPV.getPVCoordinates(currentDate, null);
            final PVCoordinates pvCoordExp = pvCoord[i];

            // Expected relative difference between expected and actual position lower than EPSILON_POS
            final double deltaPos =
                pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm() /
                    pvCoordActual.getPosition().getNorm();
            Assert.assertEquals(0, deltaPos, EPSILON_POS);
            // Expected relative difference between expected and actual velocity lower than EPSILON_VEL
            final double deltaVel =
                pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm() /
                    pvCoordActual.getVelocity().getNorm();
            Assert.assertEquals(0, deltaVel, EPSILON_VEL);

            if (i == 1) {
                Report.printToReport("Position", pvCoordExp.getPosition(), pvCoordActual.getPosition());
                Report.printToReport("Velocity", pvCoordExp.getVelocity(), pvCoordActual.getVelocity());
            }
        }
    }

    /**
     * @testType RVT
     * 
     * @testedFeature {@link features#ALMANACH}
     * 
     * @testedMethod {@link AlmanacPVCoordinates#AlmanacPVCoordinates(AlmanacParameter, double, double, double, int)}
     * @testedMethod {@link AlmanacPVCoordinates#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * 
     * @description Compute a pv coordinates ephemeris from a reference almanac and compare
     *              to the reference ephemeris provided.
     * 
     * @input We use GPS reference data (same as testGetPVCoordinatesGPS) to simulate Galileo data
     * 
     * @output Position velocity coordinates in WGS84 each five minutes during two days
     * 
     * @testPassCriteria Relative difference on position and velocity lower than EPSILON_POS, EPSILON_VEL
     * 
     * @referenceVersion 3.2
     * 
     * @nonRegressionVersion 3.2
     * @throws IOException
     *         should not happen
     * @throws PatriusException
     *         should not happen
     */
    @Test
    public final void testGetPVCoordinatesGalileo() throws IOException, PatriusException {

        Report.printMethodHeader("testGetPVCoordinatesGalileo", "Galileo PV coordinates computation", "Math",
            EPSILON_VEL, ComparisonType.RELATIVE);

        // Read reference ephemeris file
        final URL url = AlmanacPVCoordinatesTests.class.getClassLoader().getResource("almanac/" + "EphemPV");
        final MatrixFileReader demo = new MatrixFileReader(url.getPath());
        final double[][] data = demo.getData();

        final int length = data.length;

        final PVCoordinates[] pvCoord = new PVCoordinates[length];

        for (int i = 0; i < data.length; i++) {

            final double[] pos = { 0, 0, 0 };
            final double[] vel = { 0, 0, 0 };

            // Read pos
            for (int j = 0; j < 3; j++) {
                pos[j] = data[i][j];
                vel[j] = data[i][j + 3];
            }

            pvCoord[i] = new PVCoordinates(new Vector3D(pos), new Vector3D(vel));

        }
        // compute week of the Galileo parameters to match with GPS data
        final int weekGalileo = 840;
        final AlmanacParameter almanac =
            new GalileoAlmanacParameters(
                61440.0,
                weekGalileo,
                2.217106699,
                0.4714012146E-2,
                5153.604004,
                2.102607542,
                0.9627610967,
                0.480977878,
                -0.8183198006E-8);
        final AlmanacPVCoordinates almanacPV =
            new AlmanacPVCoordinates(
                almanac, 0,
                Constants.GRS80_EARTH_MU,
                earthAngularVelocity);

        final AbsoluteDate startDate = new AbsoluteDate(2015, 9, 27, 17, 4, 00, TimeScalesFactory.getGPS());
        final double step = 5 * 60;
        for (int i = 0; i < length; i++) {
            final AbsoluteDate currentDate = startDate.shiftedBy(i * step);
            final PVCoordinates pvCoordActual = almanacPV.getPVCoordinates(currentDate, null);
            final PVCoordinates pvCoordExp = pvCoord[i];

            // Expected relative difference between expected and actual position lower than EPSILON_POS
            final double deltaPos =
                pvCoordActual.getPosition().subtract(pvCoordExp.getPosition()).getNorm() /
                    pvCoordActual.getPosition().getNorm();
            Assert.assertEquals(0, deltaPos, EPSILON_POS);
            // Expected relative difference between expected and actual velocity lower than EPSILON_VEL
            final double deltaVel =
                pvCoordActual.getVelocity().subtract(pvCoordExp.getVelocity()).getNorm() /
                    pvCoordActual.getVelocity().getNorm();
            Assert.assertEquals(0, deltaVel, EPSILON_VEL);

            if (i == 1) {
                Report.printToReport("Position", pvCoordExp.getPosition(), pvCoordActual.getPosition());
                Report.printToReport("Velocity", pvCoordExp.getVelocity(), pvCoordActual.getVelocity());
            }
        }
    }

    /**
     * @throws PatriusException
     *         should not happen
     * @testType UT
     * 
     * @testedFeature {@link features#TODO put the enum member here}
     * 
     * @testedMethod {@link TODO put the link to the tested method here with format class#method}
     * 
     * @description Get a PVCoordinates from almanac without frame transformation. Get the same PVCoordinates in
     *              EME2000.
     *              Check if the first PVCoordinates convert in EME2000 is equal to the second PVCoordinates.
     * 
     * @input almanac
     * @input frame configuration without EOP
     * 
     * @output PVCoordinates in WGS84
     * @output PVCoordinates in EME2000
     * 
     * @testPassCriteria The first PVCoordinates transformed in EME2000 is equal to the second one
     * 
     * @referenceVersion 3.1
     * 
     * @nonRegressionVersion 3.1
     */
    @Test
    public final void testFrameConvert() throws PatriusException {

        CNESUtils.clearNewFactoriesAndCallSetDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        final AlmanacParameter almanac =
            new GPSAlmanacParameters(
                61440.0,
                840,
                2.217106699,
                0.4714012146E-2,
                5153.604004,
                2.102607542,
                0.9627610967,
                0.480977878,
                -0.8183198006E-8);
        final AlmanacPVCoordinates almanacPV =
            new AlmanacPVCoordinates(almanac, 1, Constants.GRS80_EARTH_MU, earthAngularVelocity);

        final AbsoluteDate currentDate = new AbsoluteDate(2015, 9, 27, 17, 4, 00, TimeScalesFactory.getGPS());
        final PVCoordinates pvCoordWGS84 = almanacPV.getPVCoordinates(currentDate, FramesFactory.getITRF());

        final Transform wgs84ToOutputFrame = FramesFactory.getITRF().getTransformTo(FramesFactory.getEME2000(),
            currentDate);
        final PVCoordinates pvCoordJ2000Exp = wgs84ToOutputFrame.transformPVCoordinates(pvCoordWGS84);

        final PVCoordinates pvCoordJ2000Actual = almanacPV.getPVCoordinates(currentDate, FramesFactory.getEME2000());

        // Expected relative difference between expected and actual position lower than EPSILON_POS
        final double deltaPos =
            pvCoordJ2000Actual.getPosition().subtract(pvCoordJ2000Exp.getPosition()).getNorm() /
                pvCoordJ2000Actual.getPosition().getNorm();
        Assert.assertEquals(0, deltaPos, 1E-16);
        // Expected relative difference between expected and actual velocity lower than EPSILON_VEL
        final double deltaVel =
            pvCoordJ2000Actual.getVelocity().subtract(pvCoordJ2000Exp.getVelocity()).getNorm() /
                pvCoordJ2000Actual.getVelocity().getNorm();
        Assert.assertEquals(0, deltaVel, 1E-16);

    }

    /**
     * Utility class to store data from a txt file in matrices
     */
    private class MatrixFileReader {
        private final String filePath;

        private double[][] data;

        /**
         * Renvoie la matrice contenue dans le fichier
         * data[0] contient la premiere ligne du fichier
         * data[1] contient la deuxieme ligne du fichier
         * etc...
         * 
         * @return data
         */
        public double[][] getData() {
            return this.data;
        }

        /**
         * Lecture d'un fichier texte contenant une matrice
         * (Commentaire possibles dans le fichier pour les lignes commence par #)
         * 
         * @param filePath
         * @throws IOException
         */
        public MatrixFileReader(final String filePath) throws IOException {
            super();
            this.filePath = filePath;
            this.parseFile();
        }

        private void parseFile() throws IOException {
            final BufferedReader reader = new BufferedReader(new FileReader(this.filePath));
            String line = null;
            final List<String> items = new ArrayList<String>();
            StringTokenizer splitter;
            while ((line = reader.readLine()) != null) {
                // Les lignes qui démarrent avec # ou qui ne contiennent rien ou que des espaces
                // sont ignorées.
                if (!line.startsWith("#") && !line.trim().isEmpty()) {
                    items.add(line);
                }
            }

            final int nbLig = items.size();
            int nbCol = 0;

            if (nbLig == 0) {
                this.data = new double[nbLig][nbCol];
            }
            else {

                final String firstLigne = items.get(0);
                splitter = new StringTokenizer(firstLigne, " ");

                nbCol = splitter.countTokens();

                this.data = new double[nbLig][nbCol - 2];
                int counter = 0;
                for (final String item : items) {
                    splitter = new StringTokenizer(item, " ");
                    splitter.nextElement();
                    splitter.nextElement();
                    for (int i = 0; i < nbCol - 2; i++) {
                        // Attention: Double.parseDouble ne fonctionne pas si
                        // les nombres sont écrits avec une virgule comme séparateur !!
                        this.data[counter][i] = Double.parseDouble((String) splitter.nextElement());
                    }
                    counter++;
                }
            }

            reader.close();
        }
    }
}
