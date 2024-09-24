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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: new test class added
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela.orbits;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * This class tests the CIRD to MOD conversions with Stela software references to characterise the differences
 * </p>
 * 
 * @see STELA LBN_ICRF_MOD_ICRF test
 * 
 * @author Cedric Dental
 * 
 * @version $Id$
 * 
 * @since 2.0
 * 
 */
public class FrameConverterTest {

    /** Features description. */
    public enum features {

        /**
         * @featureTitle STELA Patrius frame conversions
         * 
         * @featureDescription STELA ICRF to MOD to ICRF
         * 
         * @coveredRequirements
         */
        STELA_PATRIUS_FRAME_CONVERSIONS,

    }

    /**
     * @throws PatriusException
     * @testType UT
     * 
     * @testedFeature {@link features#STELA_PATRIUS_FRAME_CONVERSIONS}
     * 
     * @testedMethod {@link JacobianConverter#computeEquinoctialToCartesianJacobian(StelaEquinoctialOrbit)}
     * 
     * @description test STELA frame conversions : CIRF to MOD to CIRF.
     * 
     * @input a StelaEquinoctialOrbit
     * 
     * @output a Jacobian
     * 
     * @testPassCriteria the jacobian is equal to a Stela reference result ("MyStela (Scilab), 30/11/2012")
     * 
     * @referenceVersion 1.3
     * 
     * @nonRegressionVersion 1.3
     * 
     */
    @Test
    public void LBN_LBN_CIRF_MOD_CIRF_TV_Test() throws PatriusException {

        Utils.setDataRoot("regular-dataPBASE");
        FramesFactory.setConfiguration(Utils.getIERS2010Configuration());

        // Final state of test
        boolean globalStatus = true;

        final double[] tol = { 0, 1E-10, 1E-10, 1E-10, 1E-10, 1E-10, 1E-10 };

        final AbsoluteDate sameDate = new AbsoluteDate(2005, 6, 27, 15, 34, 28.164, TimeScalesFactory.getTAI())
            .shiftedBy(34);
        // TT - UT1 is equal to 64.184s

        final PVCoordinates posVelGCRF = new PVCoordinates(new Vector3D(9.9139548581650E+05, 4.8868428878198E+05,
            7.1097216050246E+06), new Vector3D(1.9635744745694E+03,
            -7.1741401462190E+03, 2.1869492101677E+02));

        final PVCoordinates posVelMeanCelestialOfDate = new PVCoordinates(new Vector3D(9.8700474157875E+05,
            4.8989836488501E+05, 7.1102489321076E+06), new Vector3D(
            1.9722594643749E+03, -7.1717253217158E+03, 2.1974425656651E+02));

        // GCRF to MOD
        final Transform transformG2M = FramesFactory.getGCRF().getTransformTo(FramesFactory.getMOD(false), sameDate);
        final PVCoordinates posVelTM = transformG2M.transformPVCoordinates(posVelGCRF);

        globalStatus &= this.comparePV(posVelMeanCelestialOfDate, posVelTM, tol);

        // MOD back to GCRF
        final Transform transformM2G = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getGCRF(), sameDate);
        final PVCoordinates posVelTG = transformM2G.transformPVCoordinates(posVelTM);

        globalStatus &= this.comparePV(posVelGCRF, posVelTG, tol);

        // MOD to GCRF
        final PVCoordinates posVelTG2 = transformM2G.transformPVCoordinates(posVelMeanCelestialOfDate);

        globalStatus &= this.comparePV(posVelGCRF, posVelTG2, tol);

        // GCRF to MOD
        final PVCoordinates posVelTM2 = transformG2M.transformPVCoordinates(posVelTG2);

        globalStatus &= this.comparePV(posVelMeanCelestialOfDate, posVelTM2, tol);

        Assert.assertTrue(globalStatus);
    }

    private boolean comparePV(final PVCoordinates expected, final PVCoordinates actual, final double[] tol) {

        boolean stat = true;

        for (int j = 0; j < 3; j++) {
            stat &= this.relative(expected.getPosition().toArray()[j], actual.getPosition().toArray()[j], tol[j + 1]);
            stat &=
                this.relative(expected.getVelocity().toArray()[j], actual.getVelocity().toArray()[j], tol[j + 3 + 1]);
        }

        return stat;

    }

    private boolean relative(final double expected, final double actual, final double tol) {

        boolean stat = false;

        final double form = MathLib.abs((expected - actual) / expected);

        stat = form < tol;

        return stat;
    }

}
