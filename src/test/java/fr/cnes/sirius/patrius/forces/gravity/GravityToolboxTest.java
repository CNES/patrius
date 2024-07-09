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
 * @history created 23/04/12
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:90:03/10/2013:Moved GravityToolbox to Orekit
 * VERSION::FA:306:12/11/2014:coverage
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.gravity;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.forces.gravity.potential.GRGSFormatReader;
import fr.cnes.sirius.patrius.forces.gravity.potential.GravityFieldFactory;
import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

// Règle checkstyle désactivée pour lisibilité du code de test
// CHECKSTYLE: stop MagicNumberCheck

/**
 * This class tests the tidal corrections tool box.
 * 
 * @author Gérald Mercadier, Julie Anton
 * 
 * @version $Id: GravityToolboxTest.java 18089 2017-10-02 17:02:50Z bignon $
 * 
 * @since 1.2
 */
public class GravityToolboxTest {

    /** Features description. */
    public enum features {
        /**
         * @featureTitle denormalization
         * 
         * @featureDescription denormalization of gravity coefficients
         * 
         * @coveredRequirements ??
         */
        DENORM
    }

    @Test
    public void test() throws PatriusException, IOException, ParseException {
        Utils.setDataRoot("potential");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("GRGS_EIGEN_GL04S.txt", true));
        GravityToolbox.deNormalize(GravityFieldFactory.getPotentialProvider().getC(90, 90, true));

    }

    @Test
    public void testNormalized() throws IOException, ParseException, PatriusException {

        Utils.setDataRoot("normalized");
        FramesFactory.setConfiguration(Utils.getIERS2003ConfigurationWOEOP(true));

        // c and s tables
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final int degree = 60;
        final int order = 60;

        final double[][] cN = pot.getC(degree, order, true);
        final double[][] sN = pot.getS(degree, order, true);

        final double[][] cU = pot.getC(degree, order, false);
        final double[][] sU = pot.getS(degree, order, false);

        // pv
        final Frame itrf = FramesFactory.getITRF();
        final AbsoluteDate date = new AbsoluteDate(2002, 1, 1, TimeScalesFactory.getTAI());
        final double mu = Constants.EIGEN5C_EARTH_MU;
        final double ae = Constants.EIGEN5C_EARTH_EQUATORIAL_RADIUS;

        final Vector3D pos = new Vector3D(-6590149.9269526824, 521546.44375059905, 886362.25364358397);
        final PVCoordinates pv = new PVCoordinates(pos, Vector3D.ZERO);

        // helmholtz
        final CunninghamAttractionModel model = new CunninghamAttractionModel(itrf, ae, mu, cU, sU);
        final BalminoAttractionModel model1 = new BalminoAttractionModel(itrf, ae, mu, cN, sN);

        final Vector3D cun = model.computeAcceleration(pv, date);
        final Vector3D nor = model1.computeAcceleration(pv);

        Assert.assertEquals(0, (cun.getX() - nor.getX()) / cun.getX(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getY() - nor.getY()) / cun.getY(), Precision.DOUBLE_COMPARISON_EPSILON);
        Assert.assertEquals(0, (cun.getZ() - nor.getZ()) / cun.getZ(), Precision.DOUBLE_COMPARISON_EPSILON);

    }

    @Test
    public void testReadabilityOfNewField() throws IOException, ParseException, PatriusException {
        Utils.setDataRoot("normalized");
        GravityFieldFactory.addPotentialCoefficientsReader(new GRGSFormatReader("EGNSTA02BS", true));
        final PotentialCoefficientsProvider pot = GravityFieldFactory.getPotentialProvider();

        final double[] l1 = { 71, 65, -0.19893542948347E-08, 0.68658467826988E-11 };
        final double[] l2 = { 72, 65, -0.21601038022483E-08, -0.64093000636133E-09 };
        final double[] l3 = { 160, 160, 0.26698183128708E-09, -0.60038672108981E-09 };

        Assert.assertTrue(Precision.equals(l1[2], pot.getC(71, 65, true)[71][65], 0));
        Assert.assertTrue(Precision.equals(l1[3], pot.getS(71, 65, true)[71][65], 0));

        Assert.assertTrue(Precision.equals(l2[2], pot.getC(72, 65, true)[72][65], 0));
        Assert.assertTrue(Precision.equals(l2[3], pot.getS(72, 65, true)[72][65], 0));

        Assert.assertTrue(Precision.equals(l3[2], pot.getC(160, 160, true)[160][160], 0));
        Assert.assertTrue(Precision.equals(l3[3], pot.getS(160, 160, true)[160][160], 0));
    }

    /**
     * Check that two double[][] arrays are equal
     * 
     * @param exp
     *        expected
     * @param act
     *        actual
     * @param eps
     *        threshold
     */
    public void assertArrayEquals(final double[][] exp, final double[][] act, final double eps) {
        Assert.assertEquals(exp.length, act.length);
        for (int k = 0; k < exp.length; k++) {
            Assert.assertEquals(exp[k].length, act[k].length);
            for (int j = 0; j < 1; j++) {
                Assert.assertEquals(exp[k][j], act[k][j], eps);
            }
        }
    }

    @Test
    public void testDenormalize() {
        final double[][] ref = new double[80][80];

        for (int i = 0; i < 80; i++) {
            for (int j = 0; j < 80; j++) {
                ref[i][j] = 1;
            }
        }

        final double[][] exp2 = GravityToolbox.deNormalize(ref);
        final double[][] exp1 = GravityToolbox.unNormalize(ref);

        for (int i = 0; i < 80; i++) {
            for (int j = 0; j <= i; j++) {
                if (exp1[i][j] != 0) {
                    Assert.assertEquals(0, (exp1[i][j] - exp2[i][j]) / exp1[i][j], 1e-14);
                }
            }
        }

    }
}
