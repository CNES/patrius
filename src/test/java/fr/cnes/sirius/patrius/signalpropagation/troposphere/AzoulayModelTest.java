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
 * @history created 23/05/12
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-72:30/06/2023:[PATRIUS] Mauvaise prise en compte du MeteoConditionProvider dans les AbstractTropoFactory
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Ajout de conditions meteorologiques variables dans les modeles de troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: update due to the refactoring of the AzoulayModel class
 * VERSION::FA:345:31/10/2014: coverage
 * VERSION::DM:596:07/04/2016:add numerical non-regression checks
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import fr.cnes.sirius.patrius.ComparisonType;
import fr.cnes.sirius.patrius.Report;
import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.signalpropagation.ConstantMeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditions;
import fr.cnes.sirius.patrius.signalpropagation.MeteorologicalConditionsProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;

/**
 * Unit tests for the {@link AzoulayModel}.
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.0
 */
public class AzoulayModelTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Azoulay model
         * 
         * @featureDescription implementation of the Azoulay tropospheric correction model
         *                     to correct the elevation and distance of a propagated signal.
         * 
         * @coveredRequirements DV-MES_FILT_470
         * 
         */
        AZOULAY_MODEL
    }

    /** pressure [Pa] */
    final double pressure = 102000;

    /** temperature [K] */
    final double temperature = 20 + 273.16;

    /** moisture [percent] */
    final double humidity = 20;

    final AbsoluteDate defaultDate = new AbsoluteDate();

    final MeteorologicalConditions meteoConditions = new MeteorologicalConditions(this.pressure, this.temperature,
        this.humidity);

    final MeteorologicalConditionsProvider meteoConditionsProvider = new ConstantMeteorologicalConditionsProvider(
        this.meteoConditions);

    /** geodetic altitude [m] */
    final double altitude = 150;

    /** correction */
    final AzoulayModel correction = new AzoulayModel(this.meteoConditionsProvider, this.altitude);
    final AzoulayModel correctionFalse = new AzoulayModel(this.meteoConditionsProvider, this.altitude, false);

    /**
     * Epsilon for double comparison. Epsilon is only 3E-3 since C1 coefficient is incorrect in FDS reference.
     * Real value: 10.79574. Value from reference: 10.57974.
     */
    private final double comparisonEpsilon = 3E-3;

    /**
     * FDS results : distance and elevation corrections from measured elevations.
     * FDS geometric/apparent results are inverted.
     */
    private final double[][] fromGeometricFDS = {
        { 0.0000000000000000E+00, 7.0186137947256559E-03, 5.5399279540983365E+01 },
        { 7.8539816339744828E-02, 3.2570796758294566E-03, 2.5788257998881956E+01 },
        { 1.5707963267948966E-01, 1.7681734535243221E-03, 1.4130505880522938E+01 },
        { 2.3561944901923448E-01, 1.1879825117610513E-03, 9.6434249044240126E+00 },
        { 3.1415926535897931E-01, 8.8354969199112572E-04, 7.3329280788108600E+00 },
        { 3.9269908169872414E-01, 6.9520120355675132E-04, 5.9394733849319410E+00 },
        { 4.7123889803846897E-01, 5.6609934555378830E-04, 5.0149261265583904E+00 },
        { 5.4977871437821380E-01, 4.7116788862108772E-04, 4.3617690750007299E+00 },
        { 6.2831853071795862E-01, 3.9766426029535780E-04, 3.8798209834929827E+00 },
        { 7.0685834705770345E-01, 3.3843200779002443E-04, 3.5130043396411961E+00 },
        { 7.8539816339744828E-01, 2.8913919625673753E-04, 3.2275630764777574E+00 },
        { 8.6393797973719300E-01, 2.4700491534528779E-04, 3.0020225548857828E+00 },
        { 9.4247779607693793E-01, 2.1015613526106458E-04, 2.8221244356223636E+00 },
        { 1.0210176124166828E+00, 1.7727869277642935E-04, 2.6780840937250314E+00 },
        { 1.0995574287564276E+00, 1.4741642362839751E-04, 2.5630193040621463E+00 },
        { 1.1780972450961724E+00, 1.1984965661138330E-04, 2.4720081864577401E+00 },
        { 1.2566370614359172E+00, 9.4018404333596911E-05, 2.4015038279851049E+00 },
        { 1.3351768777756621E+00, 6.9471754649471493E-05, 2.3489608894741485E+00 },
        { 1.4137166941154069E+00, 4.5833081844277171E-05, 2.3125939773686941E+00 },
        { 1.4922565104551517E+00, 2.2774962521002370E-05, 2.2912218578814851E+00 },
        { 1.5707963267948966E+00, 1.7719712138937553E-20, 2.2841708113059385E+00 } };

    /**
     * FDS results : distance and elevation corrections from apparent elevations.
     * FDS geometric/apparent results are inverted.
     */
    private final double[][] fromApparentFDS = {
        { 0.0000000000000000E+00, 6.9190792696190283E-03, 5.4614979422360584E+01 },
        { 7.8539816339744828E-02, 3.1567685599087499E-03, 2.5000363755935268E+01 },
        { 1.5707963267948966E-01, 1.7494120249064880E-03, 1.3984510593505965E+01 },
        { 2.3561944901923448E-01, 1.1819717044698002E-03, 9.5973763266767964E+00 },
        { 3.1415926535897931E-01, 8.8094241386227714E-04, 7.3133916497710310E+00 },
        { 3.9269908169872414E-01, 6.9384985187513951E-04, 5.9296370107906915E+00 },
        { 4.7123889803846897E-01, 5.6531312058531302E-04, 5.0094068911396787E+00 },
        { 5.4977871437821380E-01, 4.7067220084371725E-04, 4.3584392214212082E+00 },
        { 6.2831853071795862E-01, 3.9733295794870254E-04, 3.8777092784544709E+00 },
        { 7.0685834705770345E-01, 3.3820071026981620E-04, 3.5116186449162869E+00 },
        { 7.8539816339744828E-01, 2.8897232506847748E-04, 3.2266334308895566E+00 },
        { 8.6393797973719300E-01, 2.4688155038034136E-04, 3.0013913253768081E+00 },
        { 9.4247779607693793E-01, 2.1006335571844406E-04, 2.8216947785752886E+00 },
        { 1.0210176124166828E+00, 1.7720820107008548E-04, 2.6777939092700378E+00 },
        { 1.0995574287564276E+00, 1.4736272787398727E-04, 2.5628272437077877E+00 },
        { 1.1780972450961724E+00, 1.1980904310979117E-04, 2.4718857358053645E+00 },
        { 1.2566370614359172E+00, 9.3988333470252546E-05, 2.4014306164917887E+00 },
        { 1.3351768777756621E+00, 6.9450495298994033E-05, 2.3489217886197968E+00 },
        { 1.4137166941154069E+00, 4.5819486691635997E-05, 2.3125772214775848E+00 },
        { 1.4922565104551517E+00, 2.2768331085547873E-05, 2.2912177586556592E+00 },
        { 1.5707963267948966E+00, 1.7719712138937553E-20, 2.2841708113059385E+00 } };

    @BeforeClass
    public static void setUpBeforeClass() {
        Report.printClassHeader(AzoulayModelTest.class.getSimpleName(), "Azoulay model");
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZOULAY_MODEL}
     * 
     * @testedMethod {@link AzoulayModel#computeElevationCorrection(double)}
     * @testedMethod {@link AzoulayModel#computeSignalDelay(double)}
     * 
     * @description tests for the Azoulay tropospheric correction model from geometric elevations
     * 
     * @input Station features (temperature, moisture, altitude, pressure) and some
     *        measured elevations, results from the identical algorithm of the FDS
     * 
     * @output elevation and distance corrections
     * 
     * @testPassCriteria the corrections are exactly the ones from the FDS
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testCorrectionsFromGeometricElevation() {

        Report.printMethodHeader("testCorrectionsFromGeometricElevation", "Azoulay model computation", "FDS",
            this.comparisonEpsilon, ComparisonType.ABSOLUTE);

        // loop on input elevations from 0 to Pi/2
        for (int i = 0; i < this.fromGeometricFDS.length; i++) {

            final double geometricElevation = this.fromGeometricFDS[i][0];
            final double[] correctionRes = new double[2];
            correctionRes[0] = this.correctionFalse.computeElevationCorrection(this.defaultDate,
                geometricElevation);
            correctionRes[1] = this.correctionFalse.computeSignalDelay(this.defaultDate, geometricElevation)
                    * Constants.SPEED_OF_LIGHT;

            Assert.assertEquals(0., (this.fromGeometricFDS[i][1] - correctionRes[0]) / this.fromGeometricFDS[i][1],
                this.comparisonEpsilon);
            Assert.assertEquals(0., (this.fromGeometricFDS[i][2] - correctionRes[1]) / this.fromGeometricFDS[i][2],
                this.comparisonEpsilon);

            if (i == 0) {
                Report.printToReport("Correction", this.fromGeometricFDS[i][1], correctionRes[0]);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZOULAY_MODEL}
     * 
     * @testedMethod {@link AzoulayModel#computeElevationCorrection(AbsoluteDate, double)}
     * @testedMethod {@link AzoulayModel#computeSignalDelay(AbsoluteDate, double)}
     * 
     * @description tests for the Azoulay tropospheric correction model from apparent elevations
     * 
     * @input Station features (temperature, moisture, altitude, pressure) and some
     *        apparent elevations, results from the identical algorithm of the FDS
     * 
     * @output elevation and distance corrections
     * 
     * @testPassCriteria the corrections are exactly the ones from the FDS
     * 
     * @referenceVersion 1.2
     * 
     * @nonRegressionVersion 1.2
     */
    @Test
    public void testCorrectionsFromApparentElevation() {

        // loop on input elevations from 0 to Pi/2
        for (final double[] element : this.fromApparentFDS) {

            final double apparentElevation = element[0];

            final double[] correctionRes = new double[2];
            correctionRes[0] = this.correction
                .computeElevationCorrection(this.defaultDate, apparentElevation);
            correctionRes[1] = this.correction.computeSignalDelay(this.defaultDate, apparentElevation)
                    * Constants.SPEED_OF_LIGHT;

            Assert.assertEquals(0., (element[1] - correctionRes[0]) / element[1], this.comparisonEpsilon);
            Assert.assertEquals(0., (element[2] - correctionRes[1]) / element[2], this.comparisonEpsilon);
        }
    }

    /**
     * 
     * @testedMethod {@link AzoulayModel#supportsParameter(Parameter)}
     * @testedMethod {@link AzoulayModel#getParameters()}
     * @testedMethod {@link AzoulayModel#derivativeValue(Parameter, double)}
     * @testedMethod {@link AzoulayModel#isDifferentiableBy(Parameter)}
     * 
     * @description tests the parameters methods
     * 
     * @testPassCriteria no parameter supported by this model
     */
    @Test
    public void testParameters() {
        Assert.assertFalse(this.correction.supportsParameter(new Parameter("param", 1.)));
        Assert.assertTrue(this.correction.getParameters().isEmpty());
        Assert.assertEquals(0., this.correction.derivativeValue(new Parameter("param", 1.), 0.01), 0.);
        Assert.assertFalse(this.correction.isDifferentiableBy(new Parameter("param", 1.)));
    }

    /**
     * @description Evaluate the correction model serialization / deserialization process.
     *
     * @testPassCriteria The correction model can be serialized and deserialized.
     */
    @Test
    public void testSerialization() {

        final AzoulayModel model = this.correction;
        final AzoulayModel deserializedModel = TestUtils.serializeAndRecover(model);

        Assert.assertEquals(model.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI),
            deserializedModel.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI), 0.);
        Assert.assertEquals(model.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI - 0.1),
            deserializedModel.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI - 0.1), 0.);
        Assert.assertEquals(model.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI - 0.2),
            deserializedModel.computeSignalDelay(this.defaultDate, MathUtils.HALF_PI - 0.2), 0.);
    }
}
