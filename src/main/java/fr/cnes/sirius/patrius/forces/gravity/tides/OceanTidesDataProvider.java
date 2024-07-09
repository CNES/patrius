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
 *
 * @history modified 08/12/2014
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:241:08/12/2014:improved tides conception
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity.tides;

import fr.cnes.sirius.patrius.forces.gravity.tides.TidesStandards.TidesStandard;
import fr.cnes.sirius.patrius.forces.gravity.tides.coefficients.OceanTidesCoefficientsProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;
import fr.cnes.sirius.patrius.utils.exception.PatriusRuntimeException;

/**
 * <p>
 * Ocean tides parameters given by the IERS 1996, 2003 or GINS 2004 standard.
 * </p>
 * 
 * @concurrency immutable
 * 
 * @author maggioranic
 * 
 * @version $Id: OceanTidesDataProvider.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 2.3.1
 * 
 */
public class OceanTidesDataProvider implements IOceanTidesDataProvider {

    /** Serial UID. */
    private static final long serialVersionUID = 4636565365193220029L;

    /**
     * IERS 1996
     */
    private static final double[] IERS1996_DATA = new double[] { 0., -.3075, -.195, -.132, -.1032, -.0892 };
    /**
     * GINS 2004
     */
    private static final double[] GINS2004_DATA = new double[] { 0.000000, -0.305449, -0.196107, -0.133700, -0.104726,
        -0.090269, -0.082121, -0.076657, -0.072625, -0.069173, -0.066414, -0.063952, -0.061916, -0.059964,
        -0.058247, -0.056542, -0.055077, -0.053662, -0.052425, -0.051072, -0.049874, -0.048673, -0.047534,
        -0.046476, -0.045406, -0.044413, -0.043440, -0.042502, -0.041599, -0.040717, -0.039866, -0.039042,
        -0.038234, -0.037455, -0.036698, -0.035974, -0.035270, -0.034587, -0.033922, -0.033278, -0.032657,
        -0.032051, -0.031465, -0.030896, -0.030345, -0.029809, -0.029290, -0.028786, -0.028296, -0.027822,
        -0.027360, -0.026911, -0.026476, -0.026052, -0.025640, -0.025240, -0.024850, -0.024472, -0.024103,
        -0.023744, -0.023394, -0.023054, -0.022723, -0.022400, -0.022086, -0.021779, -0.021481, -0.021189,
        -0.020905, -0.020628, -0.020357, -0.020093, -0.019836, -0.019584, -0.019339, -0.019099, -0.018865,
        -0.018636, -0.018412, -0.018193, -0.017980, -0.017771, -0.017566, -0.017367, -0.017171, -0.016980,
        -0.016793, -0.016610, -0.016431, -0.016255, -0.016083, -0.015915, -0.015750, -0.015588, -0.015430,
        -0.015275, -0.015123, -0.014974, -0.014828, -0.014685, -0.014539, -0.014402, -0.014267, -0.014134,
        -0.014004, -0.013877, -0.013752, -0.013629, -0.013509, -0.013390, -0.013274, -0.013160, -0.013048,
        -0.012938, -0.012830, -0.012724, -0.012620, -0.012517, -0.012417, -0.012318, -0.012220, -0.012125,
        -0.012031, -0.011938, -0.011848, -0.011758, -0.011670, -0.011584, -0.011499, -0.011415, -0.011333,
        -0.011252, -0.011172, -0.011094, -0.011017, -0.010941, -0.010866, -0.010792, -0.010720, -0.010648,
        -0.010578, -0.010509, -0.010441, -0.010373, -0.010307, -0.010242, -0.010178, -0.010115, -0.010052,
        -0.009991, -0.009930, -0.009871, -0.009812, -0.009754, -0.009697, -0.009640, -0.009585, -0.009530,
        -0.009476, -0.009422, -0.009370, -0.009318, -0.009267, -0.009217, -0.009167, -0.009118, -0.009069,
        -0.009021, -0.008974, -0.008928, -0.008882, -0.008836, -0.008792, -0.008747, -0.008704, -0.008661,
        -0.008618, -0.008576, -0.008535, -0.008494 };

    /**
     * Ocean tides coefficients provider
     */
    private final OceanTidesCoefficientsProvider coeffProvider;

    /**
     * Tide standard
     */
    private final TidesStandard standard;

    /**
     * Simple constructor.
     * 
     * @param coefficientsProvider
     *        the ocean tide coefficients provider
     * @param tideStandard
     *        the ocean tide standard
     * 
     * @since 2.3.1
     */
    public OceanTidesDataProvider(final OceanTidesCoefficientsProvider coefficientsProvider,
        final TidesStandard tideStandard) {
        this.coeffProvider = coefficientsProvider;
        this.standard = tideStandard;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCpmSpm(final double nDoodson, final int l, final int m) {
        return this.coeffProvider.getCpmSpm(nDoodson, l, m);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getCpmEpm(final double nDoodson, final int l, final int m) {
        return this.coeffProvider.getCpmEpm(nDoodson, l, m);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getDoodsonNumbers() {
        return this.coeffProvider.getDoodsonNumbers().clone();
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxDegree(final double doodson, final int order) {
        return this.coeffProvider.getMaxDegree(doodson, order);
    }

    /** {@inheritDoc} */
    @Override
    public int getMinDegree(final double doodson, final int order) {
        return this.coeffProvider.getMinDegree(doodson, order);
    }

    /** {@inheritDoc} */
    @Override
    public int getMaxOrder(final double doodson) {
        return this.coeffProvider.getMaxOrder(doodson);
    }

    /** {@inheritDoc} */
    @Override
    public double[] getLoveNumbers() {
        final double[] loveNumbers;
        // get love number depending on standard
        switch (this.standard) {
            case IERS1996:
                loveNumbers = IERS1996_DATA.clone();
                break;
            case IERS2003:
                // Standard 2003 equal to standard 1996
                loveNumbers = IERS1996_DATA.clone();
                break;
            case GINS2004:
                loveNumbers = GINS2004_DATA.clone();
                break;
            default:
                // default case: non-covered line
                throw new PatriusRuntimeException(PatriusMessages.INTERNAL_ERROR, null);
        }
        return loveNumbers;
    }

    /** {@inheritDoc} */
    @Override
    public TidesStandard getStandard() {
        return this.standard;
    }

}
