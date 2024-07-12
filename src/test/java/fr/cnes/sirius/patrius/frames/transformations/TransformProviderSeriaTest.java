/**
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
 */
/*
 * HISTORY
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-2922:15/11/2021:[PATRIUS] suppression de l'utilisation de la reflexion Java dans patrius 
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:524:25/05/2016:serialization test
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.frames.transformations;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.math.TestUtils;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.PVCoordinatesTest;

public class TransformProviderSeriaTest {

    /** Obliquity of the ecliptic. */
    private static final double EPSILON_0 = 84381.448 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in longitude. */
    private static final double D_PSI_B = -0.041775 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Bias in obliquity. */
    private static final double D_EPSILON_B = -0.0068192 * Constants.ARC_SECONDS_TO_RADIANS;

    /** Right Ascension of the 2000 equinox in ICRS frame. */
    private static final double ALPHA_0 = -0.0146 * Constants.ARC_SECONDS_TO_RADIANS;

    @Test
    public void seriaFixedTransformProviderTest() {

        final FixedTransformProvider ftp1 = new FixedTransformProvider(Transform.IDENTITY);

        final FixedTransformProvider ftp2 =
            new FixedTransformProvider(new Transform(AbsoluteDate.J2000_EPOCH, (new Rotation(
                Vector3D.PLUS_I, D_EPSILON_B).applyTo(new Rotation(Vector3D.PLUS_J, -D_PSI_B * MathLib.sin(EPSILON_0))
                .applyTo(new Rotation(Vector3D.PLUS_K, -ALPHA_0)))).revert()));

        final FixedTransformProvider[] ftps = { ftp1, ftp2 };

        for (final FixedTransformProvider ftp : ftps) {
            final FixedTransformProvider ftpRecover = TestUtils.serializeAndRecover(ftp);
            ftpEq(ftp, ftpRecover);
        }

    }

    private static void ftpEq(final FixedTransformProvider ftp1,
            final FixedTransformProvider ftp2) {
        TransformTest.assertEqualsTransform(ftp1.getTransform(null), ftp2.getTransform(null));
    }

    @Test
    public void seriaHelmertTransformationTest() {

        final HelmertTransformation ht1 = new HelmertTransformation(new AbsoluteDate(1988, 1, 1, 12, 0, 0,
            TimeScalesFactory.getTT()), 12.7, 6.5, -20.9, -0.39, 0.80, -1.14, -2.9, -0.2, -0.6, -0.11, -0.19, 0.07);
        final HelmertTransformation ht2 = new HelmertTransformation(new AbsoluteDate(1997, 1, 1, 12, 0, 0,
            TimeScalesFactory.getTT()), 6.7, 6.1, -18.5, 0.00, 0.00, 0.00, 0.0, -0.6, -1.4, 0.00, 0.00, 0.002);
        final HelmertTransformation ht3 = new HelmertTransformation(new AbsoluteDate(2000, 1, 1, 12, 0, 0,
            TimeScalesFactory.getTT()), 0.1, -0.8, -5.8, 0.000, 0.000, 0.000, -0.2, 0.1, -1.8, 0.000, 0.000, 0.000);
        final HelmertTransformation ht4 = new HelmertTransformation(new AbsoluteDate(2005, 1, 1, 12, 0, 0,
            TimeScalesFactory.getTT()), 0.5, 0.9, 4.7, 0.000, 0.000, 0.000, -0.3, 0.0, 0.0, 0.000, 0.000, 0.000);

        final HelmertTransformation[] hts = { ht1, ht2, ht3, ht4 };

        for (final HelmertTransformation ht : hts) {
            final HelmertTransformation htRecover = TestUtils.serializeAndRecover(ht);
            htEq(ht, htRecover);
        }
    }

    private static void htEq(final HelmertTransformation ht1, final HelmertTransformation ht2) {
        PVCoordinatesTest.assertEqualsPVCoordinates(ht1.getCartesian(), ht2.getCartesian());
        Assert.assertTrue((ht1.getRotationVector()).equals(ht2.getRotationVector()));
        Assert.assertTrue((ht1.getRotationRate()).equals(ht2.getRotationRate()));
        Assert.assertTrue((ht1.getEpoch()).equals(ht1.getEpoch()));
    }
}
