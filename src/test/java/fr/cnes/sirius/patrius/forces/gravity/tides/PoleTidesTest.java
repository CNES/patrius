/**
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
 * HISTORY
* VERSION:4.8:DM:DM-2962:15/11/2021:[PATRIUS] Precision numerique lors du ShiftedBy avec TimeScale 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.6:DM:DM-2622:27/01/2021:Modelisation de la maree polaire dans Patrius 
* VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testng.Assert;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.ForceModel;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.parameter.StandardFieldDescriptors;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Unit testing for {@link PoleTides} class.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 * 
 */
public class PoleTidesTest {

    /** IERS graphical reference. */
    private static final double[][] refIERS = {
        { 3.2017680532893146E-11, -2.5404922968286357E-11 },
        { 3.787656737695896E-11, -2.16415300996611E-11 },
        { 4.123105045383471E-11, -1.7822155771407177E-11 },
        { 4.344827910183955E-11, -1.3103753473291836E-11 },
        { 4.639284032883799E-11, -8.398847328773612E-12 },
        { 4.690536728637334E-11, -3.423122766623649E-12 },
        { 4.6586341101866936E-11, 1.5793991990510833E-12 },
        { 4.7468577303827166E-11, 6.531525384858305E-12 },
        { 4.666372309219465E-11, 1.1889239118921773E-11 },
        { 4.249855134003552E-11, 1.6891836695220928E-11 },
        { 3.826944186120301E-11, 2.2112085676215252E-11 },
        { 3.3992127353658625E-11, 2.6153089009544098E-11 },
        { 2.8200708347125493E-11, 2.9670346045327163E-11 },
        { 2.2036338906385503E-11, 3.307165532461827E-11 },
        { 1.725698942902978E-11, 3.528551207687108E-11 },
        { 1.0422799361218445E-11, 3.655073225685718E-11 },
        { 3.2676736703591925E-12, 3.713369456940105E-11 },
        { -5.251437321180208E-12, 3.67887200880242E-11 },
        { -1.381556742664025E-11, 3.611259166934666E-11 },
        { -2.1265089746123982E-11, 3.3962827852495055E-11 },
        { -2.740304301031465E-11, 3.137248936844251E-11 },
        { -3.286237300474466E-11, 2.727844298943112E-11 },
        { -3.74331324491659E-11, 2.2984136245472475E-11 },
        { -4.094742672437616E-11, 1.865576174375285E-11 },
        { -4.3557538074158593E-11, 1.3648333377230855E-11 },
        { -4.430995111543387E-11, 8.109500762032128E-12 },
        { -4.408016373492392E-11, 2.713860794872249E-12 },
        { -4.310822017595825E-11, -2.8335629439715832E-12 },
        { -4.115896175850969E-11, -7.445237411517687E-12 },
        { -3.685408525340132E-11, -1.2680385733065357E-11 },
        { -3.368719297792704E-11, -1.683625638947416E-11 },
        { -2.974619358396689E-11, -2.0968542193114828E-11 },
        { -2.4645811594185605E-11, -2.497574182636995E-11 },
        { -1.7626159304782856E-11, -2.7374155181335932E-11 },
        { -1.0645842547200626E-11, -2.9234437687616626E-11 },
        { -3.941106156775507E-12, -3.0218485340586814E-11 },
        { 3.0679651865943693E-12, -3.107499199514249E-11 },
        { 8.909569084727735E-12, -3.1061940560118116E-11 },
        { 1.4139887542825338E-11, -2.986909391730366E-11 },
        { 1.9382428067647193E-11, -2.7774276211841066E-11 },
        { 2.4951148534537284E-11, -2.561330570804548E-11 },
        { 3.0478916520477844E-11, -2.2809358601325607E-11 },
        { 3.5324477679267736E-11, -1.9091252581308718E-11 },
        { 3.9236836554957755E-11, -1.5345141433593014E-11 },
        { 4.126325028650181E-11, -1.1687864177922587E-11 },
        { 4.2748966171682433E-11, -6.653631198410937E-12 },
        { 4.427062174960805E-11, -1.8331287698543768E-12 },
        { 4.375074996573126E-11, 3.380973383024135E-12 },
        { 4.2506397726512026E-11, 8.286138948780413E-12 },
        { 3.90175373182191E-11, 1.3040588217056401E-11 },
        { 3.5922066030590826E-11, 1.7548820216114432E-11 },
        { 3.403731195830937E-11, 2.0119574942625746E-11 },
        { 3.0877973238748746E-11, 2.2805228109678564E-11 },
        { 2.710289249786633E-11, 2.545214289830347E-11 },
        { 2.0305824275615067E-11, 2.8238296685014612E-11 },
        { 1.408828852878547E-11, 3.0116609270869425E-11 },
        { 8.398775376026334E-12, 3.1383158897799464E-11 },
        { 2.6687470777191376E-12, 3.152057668454314E-11 },
        { -2.8061149723139738E-12, 3.0982193747120016E-11 },
        { -8.959671407928235E-12, 3.073806838877272E-11 },
        { -1.4108375689619177E-11, 2.9890711785145494E-11 },
        { -1.9461154197651987E-11, 2.759099144425312E-11 },
        { -2.433257125102655E-11, 2.4070350168210366E-11 },
        { -2.7720731426192527E-11, 2.0154561239657406E-11 },
        { -2.920918272237466E-11, 1.657151158517036E-11 },
        { -3.0368670706055246E-11, 1.2447048273514037E-11 },
        { -3.1008105295850655E-11, 8.58482380866061E-12 },
        { -3.124256199936243E-11, 4.8886279947428205E-12 },
        { -3.151317503386019E-11, 1.024105525568108E-12 },
        { -3.1101392738892844E-11, -3.0070829201574292E-12 },
        { -2.91956492347394E-11, -6.920073790251329E-12 },
        { -2.5565066981537478E-11, -1.0630070868277595E-11 },
        { -2.2583303769313743E-11, -1.3853054959674441E-11 } };
    
    /** Previous frame configuration. */
    private static FramesConfiguration configuration;
    
    @BeforeClass
    public static void setUpBeforeClass() {
        Utils.setDataRoot("regular-dataPBASE");
        configuration = FramesFactory.getConfiguration();
        FramesFactory.setConfiguration(FramesConfigurationFactory.getIERS2010Configuration());
    }
    
    @AfterClass
    public static void tearDownAfterClass() {
        FramesFactory.setConfiguration(configuration);
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link PoleTides#computeAcceleration(SpacecraftState)}
     * 
     * @description check the value of the polar tide contribution to acceleration
     * 
     * @testPassCriteria the value is as expected (reference: PATRIUS 4.8, threshold: 0). Values have been graphically
     *                   checked vs NT IERS 2010 beforehand
     * 
     * @referenceVersion 4.8
     * 
     * @nonRegressionVersion 4.8
     */
    @Test
    public final void testCoefficients() throws PatriusException {

        final PoleTides polarTides = new PoleTides(FramesFactory.getTIRF(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, false, true);
        for (int i = 0; i < 2 * 365; i += 10) {
            final AbsoluteDate date = AbsoluteDate.J2000_EPOCH.shiftedBy(3 * 365.25 * 86400 + i * 86400);
            polarTides.updateCoefficientsCandS(date);
            final double[][] resC = polarTides.coefficientsC;
            final double[][] resS = polarTides.coefficientsS;
            Assert.assertEquals(resC[2][1], refIERS[i / 10][0], 0.);
            Assert.assertEquals(resS[2][1], refIERS[i / 10][1], 0.);
        }
    }

    /**
     * @testType UT
     * 
     * @testedMethod {@link PoleTides#addDAccDState(SpacecraftState, double[][], double[][])}
     * 
     * @description check that partial derivatives C and S coefficients are properly computed
     * 
     * @testPassCriteria partial derivatives coefficients C and S are exactly the same as base coefficients if partial derivatives are computed, 0 otherwise
     * 
     * @referenceVersion 4.6
     * 
     * @nonRegressionVersion 4.6
     */
    @Test
    public final void testCoefficientsPD() throws PatriusException {
        
        // Initialization
        final SpacecraftState state = new SpacecraftState(new KeplerianOrbit(7000000, 0, 0, 0, 0, 0,
                PositionAngle.TRUE, FramesFactory.getGCRF(), AbsoluteDate.J2000_EPOCH.shiftedBy(3 * 86400 * 365.25), Constants.WGS84_EARTH_MU));
        
        // No partials
        final PoleTides polarTides = new PoleTides(FramesFactory.getTIRF(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, true, true, false);
        final double[][] dAccdPos = new double[3][3];
        final double[][] dAccdVel = new double[3][3];
        polarTides.addDAccDState(state, dAccdPos, dAccdVel);
        
        // Empty array
        for (int i = 0; i < polarTides.coefficientsCPD.length; i++) {
            for (int j = 0; j < polarTides.coefficientsCPD[0].length; j++) {
                Assert.assertEquals(0, polarTides.coefficientsCPD[i][j], 0.);
                Assert.assertEquals(0, polarTides.coefficientsSPD[i][j], 0.);
            }
        }
        
        // Partials
        final PoleTides polarTides2 = new PoleTides(FramesFactory.getTIRF(), Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                Constants.WGS84_EARTH_MU, true, true, true);
        polarTides2.addDAccDState(state, dAccdPos, dAccdVel);
        
        // Coefficients for partials are same as coefficients for acceleration computation
        for (int i = 0; i < polarTides2.coefficientsC.length; i++) {
            for (int j = 0; j < polarTides2.coefficientsC[0].length; j++) {
                Assert.assertEquals(polarTides2.coefficientsC[i][j], polarTides2.coefficientsCPD[i][j], 0.);
                Assert.assertEquals(polarTides2.coefficientsS[i][j], polarTides2.coefficientsSPD[i][j], 0.);
            }
        }
    }

    /**
     * @throws PatriusException if the precession-nutation model data embedded in the library cannot
     *         be read
     * @testType UT
     * 
     * @testedMethod {@link ForceModel#enrichParameterDescriptors()}
     * 
     * @description check that the parameters of this force model are well enriched with the
     *              {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor.
     * 
     * @testPassCriteria the {@link StandardFieldDescriptors#FORCE_MODEL FORCE_MODEL} descriptor is
     *                   well contained in each parameter of the force model
     */
    @Test
    public void testEnrichParameterDescriptors() throws PatriusException {
        final PoleTides forceModel = new PoleTides(FramesFactory.getTIRF(),
                Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_MU, false, true);

        // Check that the force model has some parameters (otherwise this test isn't needed and the
        // enrichParameterDescriptors method shouldn't be called in the force model)
        Assert.assertTrue(forceModel.getParameters().size() > 0);

        // Check that each parameter is well enriched
        for (final Parameter p : forceModel.getParameters()) {
            Assert.assertTrue(p.getDescriptor().contains(StandardFieldDescriptors.FORCE_MODEL));
        }
    }
}
