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
 * @history 05/03/2013
 * 
 * HISTORY
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:94:30/09/2013:2D propagator update
 * VERSION::DM:211:08/04/2014:Modified analytical 2D propagator
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:266:29/04/2015:add various centered analytical models
 * VERSION::FA:1301:06/09/2017:Generalized EOP history
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.validation.propagation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.attitudes.AttitudeProvider;
import fr.cnes.sirius.patrius.attitudes.LofOffset;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.LOFType;
import fr.cnes.sirius.patrius.frames.configuration.DiurnalRotation;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationBuilder;
import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistoryFactory;
import fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory;
import fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.CircularOrbit;
import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DOrbitModel;
import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DParameterModel;
import fr.cnes.sirius.patrius.propagation.analytical.twod.Analytical2DPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.tools.validationTool.Validate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Validation test for {@link Analytical2DPropagator}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: Analytical2DPropagatorTest.java 17911 2017-09-11 12:02:31Z bignon $
 * 
 */
public class Analytical2DPropagatorTest {

    /** files */
    private final String dir = "analytical2Dvalidation" + File.separator;
    /** files */
    private final String loc = this.dir + "input" + File.separator;
    /** files */
    private final String locR = this.dir + "ref" + File.separator;
    /** files */
    private final String locNR = this.dir + "nreg" + File.separator;

    /** files */
    private final String exF1Dpso = "test_1Dpso_coef_ex.txt";
    /** files */
    private final String eyF1Dpso = "test_1Dpso_coef_ey.txt";
    /** files */
    private final String aF1Dpso = "test_1Dpso_coef_sma.txt";
    /** files */
    private final String iF1Dpso = "test_1Dpso_coef_inc.txt";
    /** files */
    private final String rF1Dpso = "test_1Dpso_coef_lna.txt";
    /** files */
    private final String lF1Dpso = "test_1Dpso_coef_psoM.txt";

    /** files */
    private final String exFR1Dpso = "test_1Dpso_mode_lna_res_ex.txt";
    /** files */
    private final String eyFR1Dpso = "test_1Dpso_mode_lna_res_ey.txt";
    /** files */
    private final String aFR1Dpso = "test_1Dpso_mode_lna_res_sma.txt";
    /** files */
    private final String iFR1Dpso = "test_1Dpso_mode_lna_res_inc.txt";
    /** files */
    private final String rFR1Dpso = "test_1Dpso_mode_lna_res_raan.txt";
    /** files */
    private final String lFR1Dpso = "test_1Dpso_mode_lna_res_psoM.txt";

    /** files */
    private final String exF1Dlna = "test_1Dlna_coef_ex.txt";
    /** files */
    private final String eyF1Dlna = "test_1Dlna_coef_ey.txt";
    /** files */
    private final String aF1Dlna = "test_1Dlna_coef_sma.txt";
    /** files */
    private final String iF1Dlna = "test_1Dlna_coef_inc.txt";
    /** files */
    private final String rF1Dlna = "test_1Dlna_coef_lna.txt";
    /** files */
    private final String lF1Dlna = "test_1Dlna_coef_psoM.txt";

    /** files */
    private final String exFR1Dlna = "test_1Dlna_mode_lna_res_ex.txt";
    /** files */
    private final String eyFR1Dlna = "test_1Dlna_mode_lna_res_ey.txt";
    /** files */
    private final String aFR1Dlna = "test_1Dlna_mode_lna_res_sma.txt";
    /** files */
    private final String iFR1Dlna = "test_1Dlna_mode_lna_res_inc.txt";
    /** files */
    private final String rFR1Dlna = "test_1Dlna_mode_lna_res_raan.txt";
    /** files */
    private final String lFR1Dlna = "test_1Dlna_mode_lna_res_psoM.txt";

    /** files */
    private final String exF2D = "test_2D_coef_ex.txt";
    /** files */
    private final String eyF2D = "test_2D_coef_ey.txt";
    /** files */
    private final String aF2D = "test_2D_coef_sma.txt";
    /** files */
    private final String iF2D = "test_2D_coef_inc.txt";
    /** files */
    private final String rF2D = "test_2D_coef_lna.txt";
    /** files */
    private final String lF2D = "test_2D_coef_psoM.txt";

    /** files */
    private final String exFR2D = "test_2D_mode_lna_res_ex.txt";
    /** files */
    private final String eyFR2D = "test_2D_mode_lna_res_ey.txt";
    /** files */
    private final String aFR2D = "test_2D_mode_lna_res_sma.txt";
    /** files */
    private final String iFR2D = "test_2D_mode_lna_res_inc.txt";
    /** files */
    private final String rFR2D = "test_2D_mode_lna_res_raan.txt";
    /** files */
    private final String lFR2D = "test_2D_mode_lna_res_psoM.txt";

    /** files */
    private final String exRe2D = "test_2D_linear_re_coef_ex.txt";
    /** files */
    private final String eyRe2D = "test_2D_linear_re_coef_ey.txt";
    /** files */
    private final String aRe2D = "test_2D_linear_re_coef_sma.txt";
    /** files */
    private final String iRe2D = "test_2D_linear_re_coef_inc.txt";
    /** files */
    private final String rRe2D = "test_2D_linear_re_coef_lna.txt";
    /** files */
    private final String lRe2D = "test_2D_linear_re_coef_psoM.txt";

    /** files */
    private final String refRe2D = "test_2D_linear_re.txt";

    /** files */
    private final String exId2D = "test_2D_linear_id_coef_ex.txt";
    /** files */
    private final String eyId2D = "test_2D_linear_id_coef_ey.txt";
    /** files */
    private final String aId2D = "test_2D_linear_id_coef_sma.txt";
    /** files */
    private final String iId2D = "test_2D_linear_id_coef_inc.txt";
    /** files */
    private final String rId2D = "test_2D_linear_id_coef_lna.txt";
    /** files */
    private final String lId2D = "test_2D_linear_id_coef_psoM.txt";

    /** files */
    private final String refId2D = "test_2D_linear_id.txt";

    /** files */
    private final ParameterModelReader aReader = new ParameterModelReader();
    /** files */
    private final ParameterModelReader exReader = new ParameterModelReader();
    /** files */
    private final ParameterModelReader eyReader = new ParameterModelReader();
    /** files */
    private final ParameterModelReader iReader = new ParameterModelReader();
    /** files */
    private final ParameterModelReader rReader = new ParameterModelReader();
    /** files */
    private final ParameterModelReader lReader = new ParameterModelReader();

    /** files */
    private final RefReader aReaderR = new RefReader();
    /** files */
    private final RefReader exReaderR = new RefReader();
    /** files */
    private final RefReader eyReaderR = new RefReader();
    /** files */
    private final RefReader iReaderR = new RefReader();
    /** files */
    private final RefReader rReaderR = new RefReader();
    /** files */
    private final RefReader lReaderR = new RefReader();
    /** files */
    private final NRegReader nReg = new NRegReader();

    private Analytical2DParameterModel aModel;
    private Analytical2DParameterModel exModel;
    private Analytical2DParameterModel eyModel;
    private Analytical2DParameterModel iModel;
    private Analytical2DParameterModel rModel;
    private Analytical2DParameterModel lModel;

    /** Features description. */
    public enum features {
        /**
         * @featureTitle Validate the analytical 2D propagator
         * 
         * @featureDescription Validate the propagator
         * 
         * @coveredRequirements DV-PROPAG_10, DV-PROPAG_20
         */
        ANALYTICAL_2D_PROPAGATOR
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * 
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * 
     * @description test the propagator
     * 
     * @input polynomial, trigonometric and common parameters coefficients
     * 
     * @output circular orbits
     * 
     * @testPassCriteria the resulting orbits are the same as the reference ones, to the absolute thresholds :
     * 
     *                   <pre>
     * {@code   param      a      i      M      M+w    ex     ey}
     * {@code   abs dev    2e-7, 7e-15, 2e-15, 1e-11, 2e-14, 2e-14}
     * </pre>
     * 
     *                   The non regression threshold is set to the machine epsilon
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testPropagator2D() throws URISyntaxException, IOException, PatriusException {

        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        this.aReader.readData(this.loc + this.aF2D);
        this.iReader.readData(this.loc + this.iF2D);
        this.exReader.readData(this.loc + this.exF2D);
        this.eyReader.readData(this.loc + this.eyF2D);
        this.rReader.readData(this.loc + this.rF2D);
        this.lReader.readData(this.loc + this.lF2D);

        this.aModel = this.aReader.getModel();
        this.exModel = this.exReader.getModel();
        this.eyModel = this.eyReader.getModel();
        this.iModel = this.iReader.getModel();
        this.rModel = this.rReader.getModel();
        this.lModel = this.lReader.getModel();

        // ref data
        this.aReaderR.readData(this.locR + this.aFR2D);
        this.iReaderR.readData(this.locR + this.iFR2D);
        this.exReaderR.readData(this.locR + this.exFR2D);
        this.eyReaderR.readData(this.locR + this.eyFR2D);
        this.rReaderR.readData(this.locR + this.rFR2D);
        this.lReaderR.readData(this.locR + this.lFR2D);

        // nreg
        this.nReg.readData(this.locNR + "params_2D");

        final AbsoluteDate refDate = new AbsoluteDate(this.aReader.getDate(), TimeScalesFactory.getTAI());

        final Analytical2DOrbitModel params =
            new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel, this.iModel, this.rModel,
                this.lModel, Constants.EGM96_EARTH_MU);

        final AttitudeProvider attprov = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        final Analytical2DPropagator prop = new Analytical2DPropagator(attprov, params, refDate);

        final double[] refValues = new double[6];
        final double[] actValues = new double[6];
        final String[] param = new String[] { "a ", "i ", "ra", "al", "ex", "ey" };
        final Validate val = new Validate(this.getClass());

        for (final Double time : this.aReaderR.getDates()) {
            final CircularOrbit c = (CircularOrbit) prop.propagate(refDate.shiftedBy(time)).getOrbit();

            refValues[0] = this.aReaderR.getValue(time);
            refValues[1] = this.iReaderR.getValue(time);
            refValues[2] = this.rReaderR.getValue(time);
            refValues[3] = this.lReaderR.getValue(time);
            refValues[4] = this.exReaderR.getValue(time);
            refValues[5] = this.eyReaderR.getValue(time);

            actValues[0] = c.getA();
            actValues[1] = c.getI();
            actValues[2] = c.getRightAscensionOfAscendingNode();
            actValues[3] = c.getAlphaM();
            actValues[4] = c.getCircularEx();
            actValues[5] = c.getCircularEy();

            final Double[] data = this.nReg.getValues(time);

            for (int i = 0; i < 6; i++) {
                final double regEps = Precision.EPSILON * MathLib.abs(refValues[i]);
                final double refEps = Precision.DOUBLE_COMPARISON_EPSILON * 2 * MathLib.abs(refValues[i]);
                val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps, param[i]);
            }
        }
        // val.produceLog();
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * 
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * 
     * @description test the propagator
     * 
     * @input polynomial, trigonometric and common parameters coefficients
     * 
     * @output circular orbits
     * 
     * @testPassCriteria the resulting orbits are the same as the reference ones, to the absolute thresholds :
     * 
     *                   <pre>
     * {@code   param      a      i      M      M+w    ex     ey}
     * {@code   abs dev    3e-9, 3e-15, 5e-16, 7e-12, 4e-19, 4e-18}
     * </pre>
     * 
     *                   The non regression threshold is set to the machine epsilon
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testPropagator1Dlna() throws URISyntaxException, IOException, PatriusException {

        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        this.aReader.readData(this.loc + this.aF1Dlna);
        this.iReader.readData(this.loc + this.iF1Dlna);
        this.exReader.readData(this.loc + this.exF1Dlna);
        this.eyReader.readData(this.loc + this.eyF1Dlna);
        this.rReader.readData(this.loc + this.rF1Dlna);
        this.lReader.readData(this.loc + this.lF1Dlna);

        this.aModel = this.aReader.getModel();
        this.exModel = this.exReader.getModel();
        this.eyModel = this.eyReader.getModel();
        this.iModel = this.iReader.getModel();
        this.rModel = this.rReader.getModel();
        this.lModel = this.lReader.getModel();

        // ref data
        this.aReaderR.readData(this.locR + this.aFR1Dlna);
        this.iReaderR.readData(this.locR + this.iFR1Dlna);
        this.exReaderR.readData(this.locR + this.exFR1Dlna);
        this.eyReaderR.readData(this.locR + this.eyFR1Dlna);
        this.rReaderR.readData(this.locR + this.rFR1Dlna);
        this.lReaderR.readData(this.locR + this.lFR1Dlna);

        // nreg
        this.nReg.readData(this.locNR + "params_1Dlna");

        final AbsoluteDate refDate = new AbsoluteDate(this.aReader.getDate(), TimeScalesFactory.getTAI());

        final Analytical2DOrbitModel params =
            new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel, this.iModel, this.rModel,
                this.lModel, Constants.EGM96_EARTH_MU);

        final AttitudeProvider attprov = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        final Analytical2DPropagator prop = new Analytical2DPropagator(attprov, params, refDate);

        final double[] refValues = new double[6];
        final double[] actValues = new double[6];
        final String[] param = new String[] { "a ", "i ", "ra", "al", "ex", "ey" };
        final Validate val = new Validate(this.getClass());

        for (final Double time : this.aReaderR.getDates()) {
            final CircularOrbit c = (CircularOrbit) prop.propagate(refDate.shiftedBy(time)).getOrbit();

            refValues[0] = this.aReaderR.getValue(time);
            refValues[1] = this.iReaderR.getValue(time);
            refValues[2] = this.rReaderR.getValue(time);
            refValues[3] = this.lReaderR.getValue(time);
            refValues[4] = this.exReaderR.getValue(time);
            refValues[5] = this.eyReaderR.getValue(time);

            actValues[0] = c.getA();
            actValues[1] = c.getI();
            actValues[2] = c.getRightAscensionOfAscendingNode();
            actValues[3] = c.getAlphaM();
            actValues[4] = c.getCircularEx();
            actValues[5] = c.getCircularEy();

            final Double[] data = this.nReg.getValues(time);

            for (int i = 0; i < 6; i++) {
                final double regEps = Precision.EPSILON * MathLib.abs(refValues[i]);
                final double refEps = Precision.DOUBLE_COMPARISON_EPSILON * MathLib.abs(refValues[i]);
                val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps, param[i]);
            }
        }

        // val.produceLog();
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * 
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * 
     * @description test the propagator
     * 
     * @input polynomial, trigonometric and common parameters coefficients
     * 
     * @output circular orbits
     * 
     * @testPassCriteria the resulting orbits are the same as the reference ones, to the absolute thresholds :
     * 
     *                   <pre>
     * {@code   param      a      i      M      M+w    ex     ey}
     * {@code   abs dev    2e-7, 3e-15, 2e-15, 7e-12, 2e-14, 2e-14}
     * </pre>
     * 
     *                   The non regression threshold is set to the machine epsilon
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 2.2
     */
    @Test
    public void testPropagator1Dpso() throws URISyntaxException, IOException, PatriusException {

        Utils.setDataRoot("analytical2Dvalidation");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        this.aReader.readData(this.loc + this.aF1Dpso);
        this.iReader.readData(this.loc + this.iF1Dpso);
        this.exReader.readData(this.loc + this.exF1Dpso);
        this.eyReader.readData(this.loc + this.eyF1Dpso);
        this.rReader.readData(this.loc + this.rF1Dpso);
        this.lReader.readData(this.loc + this.lF1Dpso);

        this.aModel = this.aReader.getModel();
        this.exModel = this.exReader.getModel();
        this.eyModel = this.eyReader.getModel();
        this.iModel = this.iReader.getModel();
        this.rModel = this.rReader.getModel();
        this.lModel = this.lReader.getModel();

        // ref data
        this.aReaderR.readData(this.locR + this.aFR1Dpso);
        this.iReaderR.readData(this.locR + this.iFR1Dpso);
        this.exReaderR.readData(this.locR + this.exFR1Dpso);
        this.eyReaderR.readData(this.locR + this.eyFR1Dpso);
        this.rReaderR.readData(this.locR + this.rFR1Dpso);
        this.lReaderR.readData(this.locR + this.lFR1Dpso);

        // nreg
        this.nReg.readData(this.locNR + "params_1Dpso");

        final AbsoluteDate refDate = new AbsoluteDate(this.aReader.getDate(), TimeScalesFactory.getTAI());

        final Analytical2DOrbitModel params =
            new Analytical2DOrbitModel(this.aModel, this.exModel, this.eyModel, this.iModel, this.rModel,
                this.lModel, Constants.EGM96_EARTH_MU);

        final AttitudeProvider attprov = new LofOffset(FramesFactory.getGCRF(), LOFType.TNW);
        final Analytical2DPropagator prop = new Analytical2DPropagator(attprov, params, refDate);

        final double[] refValues = new double[6];
        final double[] actValues = new double[6];
        final String[] param = new String[] { "a ", "i ", "ra", "al", "ex", "ey" };
        final Validate val = new Validate(this.getClass());

        for (final Double time : this.aReaderR.getDates()) {
            final CircularOrbit c = (CircularOrbit) prop.propagate(refDate.shiftedBy(time)).getOrbit();

            refValues[0] = this.aReaderR.getValue(time);
            refValues[1] = this.iReaderR.getValue(time);
            refValues[2] = this.rReaderR.getValue(time);
            refValues[3] = this.lReaderR.getValue(time);
            refValues[4] = this.exReaderR.getValue(time);
            refValues[5] = this.eyReaderR.getValue(time);

            actValues[0] = c.getA();
            actValues[1] = c.getI();
            actValues[2] = c.getRightAscensionOfAscendingNode();
            actValues[3] = c.getAlphaM();
            actValues[4] = c.getCircularEx();
            actValues[5] = c.getCircularEy();

            final Double[] data = this.nReg.getValues(time);

            for (int i = 0; i < 6; i++) {
                final double regEps = Precision.EPSILON * MathLib.abs(refValues[i]);
                final double refEps = Precision.DOUBLE_COMPARISON_EPSILON * 3 * MathLib.abs(refValues[i]);
                val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps, param[i]);
            }
        }

        // val.produceLog();
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * 
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * 
     * @description test the propagator
     * 
     * @input linear piecewise functions for centered part, trigonometric and common parameters coefficients
     * 
     * @output circular orbits
     * 
     * @testPassCriteria the resulting orbits are the same as the reference ones, to the absolute thresholds :
     * 
     *                   <pre>
     * {@code   param      a      i      M      M+w    ex     ey}
     * {@code   abs dev    2e-7, 7e-15, 2e-15, 1e-11, 2e-14, 2e-14}
     * </pre>
     * 
     *                   The non regression threshold is set to the machine epsilon
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPropagator2DRe() throws URISyntaxException, IOException, PatriusException {

        // Initialization
        Utils.setDataRoot("analytical2Dvalidation");

        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        builder.setEOPHistory(EOPHistoryFactory.getEOP2000HistoryConstant());
        builder.setDiurnalRotation(new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
            LibrationCorrectionModelFactory.NO_LIBRATION));
        FramesFactory.setConfiguration(builder.getConfiguration());

        final AbsoluteDate refDate = AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(58423 * Constants.JULIAN_DAY)
            .shiftedBy(49136.50897127987 + 32.184);

        // Input data
        final Analytical2DParameterModel aModel = new ParameterModelReader2().readData(this.loc + this.aRe2D, false);
        final Analytical2DParameterModel exModel = new ParameterModelReader2().readData(this.loc + this.exRe2D, false);
        final Analytical2DParameterModel eyModel = new ParameterModelReader2().readData(this.loc + this.eyRe2D, false);
        final Analytical2DParameterModel iModel = new ParameterModelReader2().readData(this.loc + this.iRe2D, false);
        final Analytical2DParameterModel rModel = new ParameterModelReader2().readData(this.loc + this.rRe2D, true);
        final Analytical2DParameterModel lModel = new ParameterModelReader2().readData(this.loc + this.lRe2D, true);

        // ref data
        final RefReader2 refReader = new RefReader2();
        refReader.readData(this.locR + this.refRe2D);

        // nreg
        this.nReg.readData(this.locNR + "params_2D_re");

        final Analytical2DOrbitModel params = new Analytical2DOrbitModel(aModel, exModel, eyModel, iModel, rModel,
            lModel, Constants.EGM96_EARTH_MU);

        final Analytical2DPropagator prop = new Analytical2DPropagator(params, refDate);

        final String[] param = new String[] { "a ", "ex", "ey", "i ", "ra", "al" };
        final Validate val = new Validate(this.getClass());

        for (final Double time : refReader.getDates()) {
            // Actual
            final CircularOrbit c = (CircularOrbit) prop.propagate(refDate.shiftedBy(time)).getOrbit();
            final double[] actValues = { c.getA(), c.getCircularEx(), c.getCircularEy(), c.getI(),
                c.getRightAscensionOfAscendingNode(), c.getAlphaM() };
            // Reference
            final double[] refValues = refReader.getValue(time);

            final Double[] data = this.nReg.getValues(time);

            for (int i = 0; i < 6; i++) {
                final double regEps = Precision.EPSILON * MathLib.abs(refValues[i]);
                final double refEps = Precision.DOUBLE_COMPARISON_EPSILON * 2 * MathLib.abs(refValues[i]);
                if (i != 1 && i != 2) {
                    val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps, param[i]);
                } else {
                    val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps * 1E5, param[i]);
                }
            }

        }

        // val.produceLog();
    }

    /**
     * @throws URISyntaxException
     *         if fails
     * @throws IOException
     *         if fails
     * @throws PatriusException
     *         if fails
     * @testType TVT
     * 
     * @testedFeature {@link features#ANALYTICAL_2D_PROPAGATOR}
     * 
     * @testedMethod {@link Analytical2DPropagator#propagate(AbsoluteDate)}
     * 
     * @description test the propagator
     * 
     * @input linear piecewise functions for centered part, trigonometric and common parameters coefficients
     * 
     * @output circular orbits
     * 
     * @testPassCriteria the resulting orbits are the same as the reference ones, to the absolute thresholds :
     * 
     *                   <pre>
     * {@code   param      a      i      M      M+w    ex     ey}
     * {@code   abs dev    2e-7, 7e-15, 2e-15, 1e-11, 2e-14, 2e-14}
     * </pre>
     * 
     *                   The non regression threshold is set to the machine epsilon
     * 
     * @referenceVersion 3.0
     * 
     * @nonRegressionVersion 3.0
     */
    @Test
    public void testPropagator2DId() throws URISyntaxException, IOException, PatriusException {

        // Initialization
        Utils.setDataRoot("analytical2Dvalidation");

        final FramesConfigurationBuilder builder = new FramesConfigurationBuilder();
        builder.setEOPHistory(EOPHistoryFactory.getEOP2000HistoryConstant());
        builder.setDiurnalRotation(new DiurnalRotation(TidalCorrectionModelFactory.NO_TIDE,
            LibrationCorrectionModelFactory.NO_LIBRATION));
        FramesFactory.setConfiguration(builder.getConfiguration());

        final AbsoluteDate refDate = AbsoluteDate.MODIFIED_JULIAN_EPOCH.shiftedBy(58423 * Constants.JULIAN_DAY)
            .shiftedBy(50170.653635397815 + 32.184);

        // Input data
        final Analytical2DParameterModel aModel = new ParameterModelReader2().readData(this.loc + this.aId2D, false);
        final Analytical2DParameterModel exModel = new ParameterModelReader2().readData(this.loc + this.exId2D, false);
        final Analytical2DParameterModel eyModel = new ParameterModelReader2().readData(this.loc + this.eyId2D, false);
        final Analytical2DParameterModel iModel = new ParameterModelReader2().readData(this.loc + this.iId2D, false);
        final Analytical2DParameterModel rModel = new ParameterModelReader2().readData(this.loc + this.rId2D, true);
        final Analytical2DParameterModel lModel = new ParameterModelReader2().readData(this.loc + this.lId2D, true);

        // ref data
        final RefReader2 refReader = new RefReader2();
        refReader.readData(this.locR + this.refId2D);

        // nreg
        this.nReg.readData(this.locNR + "params_2D_id");

        final Analytical2DOrbitModel params = new Analytical2DOrbitModel(aModel, exModel, eyModel, iModel, rModel,
            lModel, Constants.EGM96_EARTH_MU);

        final Analytical2DPropagator prop = new Analytical2DPropagator(params, refDate);

        final String[] param = new String[] { "a ", "ex", "ey", "i ", "ra", "al" };
        final Validate val = new Validate(this.getClass());

        for (final Double time : refReader.getDates()) {
            // Actual
            final CircularOrbit c = (CircularOrbit) prop.propagate(refDate.shiftedBy(time)).getOrbit();
            final double[] actValues = { c.getA(), c.getCircularEx(), c.getCircularEy(), c.getI(),
                c.getRightAscensionOfAscendingNode(), c.getAlphaM() };
            // Reference
            final double[] refValues = refReader.getValue(time);

            final Double[] data = this.nReg.getValues(time);

            for (int i = 0; i < 6; i++) {
                final double regEps = Precision.EPSILON * MathLib.abs(refValues[i]);
                final double refEps = Precision.DOUBLE_COMPARISON_EPSILON * 2 * MathLib.abs(refValues[i]);
                // if (i == 1) {
                // System.out.println((actValues[i] - refValues[i]) / refValues[i]);
                // }
                if (i != 1 && i != 2) {
                    val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps, param[i]);
                } else {
                    val.assertEquals(actValues[i], data[i], regEps, refValues[i], refEps * 1E5, param[i]);
                }
            }

            // /* GENERATE NON REG FILE */
            // System.out.print(time + " ");
            // for (double d : actValues) {
            // System.out.print(d + " ");
            // }
            // System.out.println();
        }

        // val.produceLog();
    }
}
