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
 * @history creation 20/03/2017
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:849:20/03/2017:Implementation of DragCoefficientProvider with file reader
 * VERSION::DM:1175:29/06/2017:add validation test aero vs global aero
 * VERSION::FA:1176:28/11/2017:add error message
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.assembly.models.GlobalDragCoefficientProvider.INTERP;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TricubicSplineInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TrivariateGridInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link InterpolatedDragReader}.
 * The robustness of the reader for aero coefficients file is tested.
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class InterpolatedDragReaderTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Test reader for aero coefficients file
         * 
         * @featureDescription test reader robustness
         * 
         * @coveredRequirements DM-849
         */
        INTERPOLATED_DRAG_READER
    }

    /**
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATED_DRAG_READER}
     * 
     * @testedMethod {@link InterpolatedDragReader#getData()}
     * 
     * @description The reader robustness is tested in the following cases :
     *              - the file has no header
     *              - the file contains spaces or tabulations on several columns
     *              - the file has a larger number of columns than the standard file for GlobalAeroModel
     * 
     * @input aero files
     * 
     * @output retrieved data from files
     * 
     * @testPassCriteria reading must occur without trouble, data must be as expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testReaderRobustness() throws IOException, PatriusException {

        // Root resource
        final String aeroFolder = "coeffaero/";

        // 1) Read aero file without header
        // This file also has wrong number of column for last element
        final String fileNoHeader = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalNoHeader.txt").getFile();
        InterpolatedDragReader reader1 = null;
        try {
            reader1 = new InterpolatedDragReader();
            reader1.readFile(fileNoHeader);
        } catch (final IOException e) {
            // Should not happen !
            Assert.fail();
        } catch (final PatriusException e) {
            // Should happen ! (last element has wrong number of columns)
            Assert.assertTrue(true);
        }

        // 2) Read aero file containing random spaces and tabulations
        final String fileSpaceTab = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalSpacesTab.txt").getFile();
        InterpolatedDragReader reader2 = null;
        try {
            reader2 = new InterpolatedDragReader();
            reader2.readFile(fileSpaceTab);
        } catch (final IOException e) {
            // Should not happen !
            Assert.fail();
        }

        // Get data : 27 columns
        final double[][] data2 = reader2.readFile(fileSpaceTab);
        Assert.assertEquals(data2[0].length, 27, 0);

        // 2) Read aero file containing a larger number of columns
        final String fileMoreCol = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalMultipleCol.txt").getFile();
        InterpolatedDragReader reader3 = null;
        try {
            reader3 = new InterpolatedDragReader();
            reader3.readFile(fileMoreCol);
        } catch (final IOException e) {
            // Should not happen !
            Assert.fail();
        }

        // Get data : 36 columns
        final double[][] data3 = reader3.readFile(fileMoreCol);
        Assert.assertEquals(data3[0].length, 36, 0);

        // Check interpolation fails with some data are missing in the file
        final String fileMissingLines = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalModelMissing.txt").getFile();
        try {
            new GlobalDragCoefficientProvider(INTERP.SPLINE, fileMissingLines);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }

        // Check wrong number of columns
        final String fileWrongColumns = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalMultipleCol.txt").getFile();
        try {
            new GlobalDragCoefficientProvider(INTERP.SPLINE, fileWrongColumns);
            Assert.fail();
        } catch (final PatriusException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * @throws IOException
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATED_DRAG_READER}
     * 
     * @testedMethod {@link InterpolatedDragReader#getData()}
     * 
     * @description This test is up to ensure that the given file is well read : lines must have
     *              the expected values on each columns.
     * 
     * @input aero files
     * 
     * @output retrieved data from files
     * 
     * @testPassCriteria The values are the one expected
     * 
     * @referenceVersion 3.4
     * 
     * @nonRegressionVersion 3.4
     */
    @Test
    public void testLineReading() throws IOException, PatriusException {

        // Expected data
        final double[] expectedLine5 = new double[] { -90.0, 0.0, 8.0, -1.839393557097472E-4, 5.956375704942251E-4,
            1.9317756096202259,
            -0.019561353760158613, 0.01715804602257643, 1.2705098245110689, -0.012851146604036166,
            -0.006094166313808657, 0.17717851764514078,
            0.011513763577920002, 0.007675686459960495, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
            0.0 };

        final double[] expectedLastLine = new double[] { 90.0, 360.0, 8.0, -1.7682736426001723E-4,
            8.679126766160673E-4, -1.9223470526411934,
            -0.018227517347116737, 0.018463771417432, -1.3040124670205768, -0.012168967173842346,
            0.003530582155178884, -0.17850151100192482,
            0.010917669827920001, -0.001615270449219209, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.,
            0.0, 0.0, 0.0, 0.,
            0.0, 0.0, 0.0, 0. };

        // Root resource
        final String aeroFolder = "coeffaero/";
        final String fileAero = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource(aeroFolder + "CoeffAeroGlobalModel.txt").getFile();
        final InterpolatedDragReader reader = new InterpolatedDragReader();

        // Get data
        final double[][] actualData = reader.readFile(fileAero);

        // Comparison
        for (int i = 0; i < actualData[0].length; i++) {
            Assert.assertEquals(expectedLine5[i], actualData[4][i], 0.);
            Assert.assertEquals(expectedLastLine[i], actualData[actualData.length - 1][i], 0.);
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#INTERPOLATED_DRAG_READER}
     * 
     * @testedMethod {@link GlobalDragCoefficientProvider#getCoefficients(Vector3D, fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData, fr.cnes.sirius.patrius.assembly.Assembly)}
     * 
     * @description check spline interpolation is properly performed. Reference values are manually computed using
     *              extracted data from coefficient file.
     *              Input values (relative velocity, azimut, elevation, s, etc.) are extracted from GlobalAeroModelTest
     *              testCase3().
     * 
     * @input drag coefficient file
     * 
     * @output drag coefficients
     * 
     * @testPassCriteria Results is the same as reference (Math, relative threshold: 2E-4, threshold is not zero since
     *                   reference has been
     *                   computed on a subset of the all points, results is however better than linear interpolation
     *                   result which has an error of 0.003)
     * 
     * @referenceVersion 4.0
     * 
     * @nonRegressionVersion 4.0
     */
    @Test
    public void testSpline() throws PatriusException, IOException {

        // Initialization
        final String fileAero = InterpolatedDragReaderTest.class.getClassLoader()
            .getResource("coeffaero/CoeffAeroGlobalModel.txt").getFile();
        final GlobalDragCoefficientProvider provider = new GlobalDragCoefficientProvider(INTERP.SPLINE, fileAero);

        // Actual value
        final Vector3D relativeVelocity = new Vector3D(-182.50904268459894, -190.98480374787437, -38.06719405967798);
        // Values have been chosen to result in a molar mass of 0.012kg
        final double c = Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
        final AtmosphereData atmosData = new AtmosphereData(1E-3, 1255, 0, 0, 0, 0, 0, 1, 0, 0, (40. - 12. / c)
            / (12. / c - 16.));
        final DragCoefficient actualCoef = provider.getCoefficients(relativeVelocity, atmosData, null);
        final double actual = actualCoef.getScAbs().getX();

        // Expected value
        final double elevation = 8.2;
        final double azimut = 46.3;
        final double s = 6.4;

        final TrivariateGridInterpolator interpolator = new TricubicSplineInterpolator();
        final double[] x = { 2, 6, 10, 14 };
        final double[] y = { 40, 44, 48, 52 };
        final double[] z = { 5, 6, 7, 8 };
        final double[][][] res = {
            {
            { -2.214158623507655, -2.1823435397528996, -2.161378518309268, -2.1466745864532637 },
            { -2.0323732329892334, -2.001838814258554, -1.981736257818508, -1.96764761069968 },
            { -1.8137422529773224, -1.7847145924030796, -1.7656169111084672, -1.752239704917943 },
            { -1.6110805739445848, -1.5834864571764844, -1.565359317347387, -1.5526772716387747 },
        },
            {
            { -2.2335334856315967, -2.2060593753154425, -2.1891831755456415, -2.1782731372263746 },
            { -2.0541179812445747, -2.027656801545101, -2.011407479709542, -2.00090220458124 },
            { -1.8398988325465149, -1.8146324347335727, -1.7991206898792278, -1.7890918994349025 },
            { -1.6405018001139804, -1.6163898990469279, -1.6015932856042534, -1.5920262411177695 },
        },
            {
            { -2.26614446321084, -2.244310830705067, -2.231916426431277, -2.224443831550702 },
            { -2.084000537175206, -2.062874583496598, -2.050869018371874, -2.043620249894022 },
            { -1.8721675576805996, -1.8518925021697021, -1.8403592350742004, -1.8333865267269773 },
            { -1.6672096426084855, -1.6477178360637565, -1.6366119580179974, -1.6298831880417164 },
        },
            {
            { -2.2894962192504553, -2.271672990299712, -2.2617036686829928, -2.255587504685879 },
            { -2.109456012354668, -2.0920895993316995, -2.0823586172168236, -2.0763804901340563 },
            { -1.8974408382634937, -1.8806536058373886, -1.8712303881806358, -1.8654336089579249 },
            { -1.6932140919573695, -1.6769383226531234, -1.6677781001332335, -1.6621320156533677 },
        },
        };
        final TrivariateFunction f = interpolator.interpolate(x, y, z, res);

        final double expected = f.value(elevation, azimut, s);

        // Check
        Assert.assertEquals(0, (actual - expected) / expected, 2E-4);
    }
}
