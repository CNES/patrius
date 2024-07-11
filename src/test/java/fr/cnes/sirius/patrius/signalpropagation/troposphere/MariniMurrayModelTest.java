/**
 * Copyright 2021-2021 CNES
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
 * HISTORY
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.8:DM:DM-2928:15/11/2021:[PATRIUS] Modele tropospherique Marini-Murray 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * This class includes the tests concerning the Marini-Murray model for the laser range tracking
 * correction at elevation values above 10°. The reference class for this application is
 * MariniMurrayModel.
 * 
 * @author Natale N. - GMV
 */

public class MariniMurrayModelTest {

    /**
     * This array contains a set of results of the tropospheric range correction obtained in scilab
     * by means of the Marini-Murray model formulation. This is the reference data for the
     * comparison performed in the test "testSignalDelay"
     */
    private final double[] scilabRefDeltaR = { 3.89365646599918, 3.89562994098040, 3.89619316984512, 3.90110837956300,
            4.92953271102573, 4.93150618454149, 4.93196752845152, 4.93688273444197, 3.89486544596138, 3.89683953370640,
            3.89740293745362, 3.90231967334366, 4.93106333046345, 4.93303741674254, 4.93349890389981, 4.93841563606126,
            3.88674585029647, 3.88871586972060, 3.88927311546640, 3.89417971852277, 4.92078458122775, 4.92275459918420,
            4.92320881455931, 4.92811541388182, 3.88795057105983, 3.88992120110358, 3.89047861957113, 3.89538674345928,
            4.92230980865073, 4.92428043722635, 4.92473479338831, 4.92964291354143, 2.07878766042983, 2.07983774604468,
            2.08034418184684, 2.08295956696105, 2.63197912652628, 2.63302921207682, 2.63353344207737, 2.63614882702794,
            2.07943312380059, 2.08048353546693, 2.08099012851735, 2.08360632570839, 2.63279635579462, 2.63384676739663,
            2.63435115396054, 2.63696735098789, 2.07514629979307, 2.07619454700191, 2.07669998869813, 2.07931079499957,
            2.62736877652999, 2.62841702367442, 2.62892023496772, 2.63153104110523, 2.07578950414207, 2.07683807626158,
            2.07734367462255, 2.07995529015946, 2.62818314562949, 2.62923171768456, 2.62973508495130, 2.63234670032421,
            3.76253492468005, 3.76444194154317, 3.76498620329111, 3.76973588996408, 4.76352732952968, 4.76543434497668,
            4.76588015281539, 4.77062983588645, 3.76370319141614, 3.76561080040782, 3.76615523114898, 3.77090639259921,
            4.76500640431224, 4.76691401188735, 4.76735995814935, 4.77211111599655, 3.75585702868190, 3.75776070635616,
            3.75829918647081, 3.76304055631747, 4.75507379897865, 4.75697747523466, 4.75741639459673, 4.76215776083527,
            3.75702117965058, 3.75892544738144, 3.75946409440131, 3.76420693386469, 4.75654766332625, 4.75845192963842,
            4.75889098504624, 4.76363382090038, 2.00878306590769, 2.00979778917214, 2.01028717040270, 2.01281448063658,
            2.54334543148833, 2.54436015469062, 2.54484740440146, 2.54737471447720, 2.00940679285852, 2.01042183119444,
            2.01091136437782, 2.01343945934129, 2.54413514000280, 2.54515017827655, 2.54563757927837, 2.54816567408365,
            2.00526433057786, 2.00627727734587, 2.00676569794769, 2.00928858356344, 2.53889033817758, 2.53990328488334,
            2.54038955019259, 2.54291243564993, 2.00588587458102, 2.00689913531811, 2.00738770730889, 2.00991137490855,
            2.53967728284134, 2.54069054351616, 2.54117695954633, 2.54370062698752 };

    /**
     * @testedMethod {@link MariniMurrayModel#computeSignalDelay(double)}
     * @testedMethod {@link MariniMurrayModel#computeElevationCorrection(double)}
     *
     * @description
     * This test is an application of the Marini-Murray model for the laser range tracking
     * correction. In the test numerical results provided by MariniMurrayModel class are compared
     * with results obtained in Scilab reported in the array "scilabRefDeltaR". Input data for these
     * results are obtained with all the possible combinations of two elements for each entry of the
     * model. The options considered for each entry of the model are stocked in a set of arrays (for
     * wavelength entry for example, wavelengthsValues array is created).
     * 
     * @testPassCriteria signal delay is as expected (reference: Scilab, relative threshold: 0), elevation correction is 0
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testSignalDelay() {

        // Definition of dataset for each input of the Marini-Murray model
        final double[] wavelengthsValues = new double[] { 500, 700 };//
        final double[] elevationValues = new double[] { 30.0, 70.0 };
        final double[] latitudeValues = new double[] { -30.0, 50.0 };
        final double[] altitudeValues = new double[] { 0.0, 1.0e3 };
        final double[] pressureValues = new double[] { 800.0e2, 1013.0e2 };
        final double[] temperatureValues = new double[] { 288.0, 303.0 };
        final double[] humidityValues = new double[] { 40.0, 80.0 };

        int rowsCount = 0; // counter for the reference results array
        // Start of a series of "for" cycles for filling in the array input of the Marini-Murray
        // model. The cycles are built on values defined in the arrays above.
        for (final double lambda : wavelengthsValues) { // cycle on wavelength

            for (final double elevation : elevationValues) { // cycle on elevation
                for (final double latitude : latitudeValues) { // cycle on latitude
                    for (final double altitude : altitudeValues) { // cycle on altitude
                        for (final double pressure : pressureValues) { // cycle on pressure
                            for (final double temperature : temperatureValues) { // cycle on temperature
                                for (final double humidity : humidityValues) { // cycle on humidity

                                    // Creation of the Marini-Murray model with the given input data
                                    final MariniMurrayModel model = new MariniMurrayModel(pressure,
                                            MathLib.toRadians(latitude), humidity, temperature, lambda, altitude);

                                    // Comparison of the results obtained with the reference
                                    final double tropoCorrection = Constants.SPEED_OF_LIGHT
                                            * model.computeSignalDelay(MathLib.toRadians(elevation));
                                    Assert.assertEquals(scilabRefDeltaR[rowsCount], tropoCorrection, 1e-12);

                                    // Increase the rows counter for the reference array
                                    rowsCount++;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * @testedMethod {@link MariniMurrayModel#equals(Object)}
     * @testedMethod {@link MariniMurrayModel#hashCode()}
     *
     * @description test equals() and hashcode() methods for different cases
     * 
     * @testPassCriteria output is as expected
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public void testEquals() {
        // Models creation
        final MariniMurrayModel model1 = new MariniMurrayModel(800.0e2,
                MathLib.toRadians(-30.0), 40.0, 288.0, 500, 0.0);
        final MariniMurrayModel model2 = new MariniMurrayModel(800.0e2,
                MathLib.toRadians(-31.0), 40.0, 288.0, 500, 0.0);
        final MariniMurrayModel model3 = new MariniMurrayModel(800.0e2,
                MathLib.toRadians(-30.0), 40.0, 288.0, 500, 0.0);
        
        // Checks equals
        Assert.assertTrue(model1.equals(model1));
        Assert.assertFalse(model1.equals(null));
        Assert.assertFalse(model1.equals(model2));
        Assert.assertFalse(model1.equals(new Double(3)));
        Assert.assertTrue(model1.equals(model3));
        
        // Check hashcode
        Assert.assertEquals(model1.hashCode(), model1.hashCode());
        Assert.assertEquals(model1.hashCode(), model3.hashCode());
        Assert.assertFalse(model1.hashCode() == model2.hashCode());
    }

}
