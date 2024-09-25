/**
 *
 * Copyright 2011-2022 CNES
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
 * @history creation 18/10/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.interpolation.TimeStampedInterpolableEphemeris;

/**
 * Test class for {@link PrecessionNutationCache}.
 * 
 * @author Rami Houdroge
 * @since 1.3
 * @version $Id: PrecessionNutationCacheTest.java 18088 2017-10-02 17:01:51Z bignon $
 * @deprecated since 4.13 as the precession nutation corrections cache management is deported in the
 *             {@link PrecessionNutationInterpolation} class which uses a more efficient
 *             {@link TimeStampedInterpolableEphemeris} cache system.
 */
@Deprecated
public class PrecessionNutationCacheTest {

    private final double epsN = 5e-12;

    private final double epsNd = 1e-9;

    @Test
    public void testSynchronizedToken() {

        // IERS 2010
        final IERS20032010PrecessionNutation pnD = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010);
        // IERS 2003
        final IERS20032010PrecessionNutation pnD3 = new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003);

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        // no asserts are necessary, values tested below
        pnD.getCIPCoordinates(date).getCIPMotion();
        pnD.getCIPCoordinates(date).getCIPMotionTimeDerivatives();
        pnD3.getCIPCoordinates(date).getCIPMotionTimeDerivatives();
        pnD3.getCIPCoordinates(date).getCIPMotion();
    }

    @Test
    public void testIsDirect() {
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);
        final PrecessionNutationModel pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        Assert.assertTrue(pnD.isDirect());
        Assert.assertFalse(pnN3.isDirect());
    }

    @Test
    public void testCIPInterpolations() {

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);
        final PrecessionNutationCache pnN = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003);
        final PrecessionNutationCache pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        // Date
        final AbsoluteDate date = new AbsoluteDate(2005, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        for (int i = 0; i < 43200; i += 600) {

            final AbsoluteDate currentDate = date.shiftedBy(i);
            final double[] cipD = pnD.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN = pnN.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN, cipD, this.epsN);

            final double[] cipD3 = pnD3.getCIPCoordinates(currentDate).getCIPMotion();
            final double[] cipN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotion();

            Assert.assertArrayEquals(cipN3, cipD3, this.epsN);
        }
    }

    @Test
    public void testCIPDvInperpolations() {

        // IERS 2010
        final PrecessionNutationModel pnD = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2010);
        final PrecessionNutationCache pnN = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2010));

        // IERS 2003
        final PrecessionNutationModel pnD3 = new IERS20032010PrecessionNutation(PrecessionNutationConvention.IERS2003);
        final PrecessionNutationCache pnN3 = new PrecessionNutationCache(new IERS20032010PrecessionNutation(
            PrecessionNutationConvention.IERS2003));

        // Date
        final AbsoluteDate date = new AbsoluteDate(2010, 3, 5, 0, 0, 0.0, TimeScalesFactory.getTAI());

        for (int i = 0; i < 43200; i += 600) {

            final AbsoluteDate currentDate = date.shiftedBy(i);
            final double[] cipD3 = pnD3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipN3 = pnN3.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            final double[] cipD = pnD.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();
            final double[] cipN = pnN.getCIPCoordinates(currentDate).getCIPMotionTimeDerivatives();

            Assert.assertArrayEquals(cipN, cipD, this.epsNd);
            Assert.assertArrayEquals(cipN3, cipD3, this.epsNd);
        }
    }
}
