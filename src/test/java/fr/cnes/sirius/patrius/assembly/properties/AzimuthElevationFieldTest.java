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
 * @history creation 30/05/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6.1:DM:DM-2871:15/03/2021:Changement du sens des Azimuts (Annulation de SIRIUS-FT-2558)
 * VERSION:4.6:DM:DM-2558:27/01/2021:Changement du sens des Azimuts 
 * VERSION:4.4:DM:DM-2153:04/10/2019:[PATRIUS] PVCoordinatePropagator
 * VERSION:4.3.1:FA:FA-2141:11/07/2019:[PATRIUS] IndexOutOfBoundsException lors qu'on definit un masque physique sans point avec azimut egal a 0
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1783:12/11/2018:Global improvement of azimuth elevation field
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.properties;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.fieldsofview.AzimuthElevationField;
import fr.cnes.sirius.patrius.fieldsofview.IFieldOfView;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.AzimuthElevationCalculator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;

/**
 * <p>
 * Test class for the azimuth-elevation field of view.
 * </p>
 * 
 * @see AzimuthElevationField
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class AzimuthElevationFieldTest {

    /** Features description. */
    enum features {
        /**
         * @featureTitle Azimuth-elevation field of view.
         * 
         * @featureDescription Azimuth-elevation field of view to be used
         *                     in sensors description
         * 
         * @coveredRequirements DV-VEHICULE_190, DV-VEHICULE_200, DV-VEHICULE_220,
         *                      DV-VEHICULE_230, DV-VEHICULE_240, DV-VEHICULE_250
         */
        AZIMUTH_ELEVATION_FIELD
    }

    /** Epsilon for double comparison. */
    private final double comparisonEpsilon = Precision.DOUBLE_COMPARISON_EPSILON;

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH_ELEVATION_FIELD}
     * 
     * @testedMethod {@link AzimuthElevationField#getAngularDistance(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#isInTheField(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#getName()}
     * 
     * @description test of the basic methods of a azimuth-elevation field of view
     * 
     * @input an azimuth-elevation field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 2.0
     * 
     * @nonRegressionVersion 2.0
     */
    @Test
    public void elevationMaskField() {

        // Initialization
        final String name = "azimuthElevationField";
        final double[][] mask = { { MathLib.toRadians(0), MathLib.toRadians(5) },
            { MathLib.toRadians(180), MathLib.toRadians(3) },
            { MathLib.toRadians(-90), MathLib.toRadians(4) } };

        // First check (math)
        IFieldOfView field = new AzimuthElevationField(mask, 0.0, name);
        Vector3D direction = new Vector3D(0.0, 1.0, 1.0);
        double elevation = field.getAngularDistance(direction);
        Assert.assertEquals(FastMath.PI / 4.0 - MathLib.toRadians(4), elevation, this.comparisonEpsilon);
        Assert.assertTrue(field.isInTheField(direction));

        // Second check (math)
        field = new AzimuthElevationField(mask, -4.0 * FastMath.PI, name);
        direction = new Vector3D(0.0, 1.0, 1.0);
        elevation = field.getAngularDistance(direction);
        Assert.assertEquals(FastMath.PI / 4.0 - MathLib.toRadians(4), elevation, this.comparisonEpsilon);

        // Other checks
        Assert.assertEquals(name, field.getName());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH_ELEVATION_FIELD}
     * 
     * @testedMethod {@link AzimuthElevationField#getAngularDistance(Vector3D)}
     * 
     * @description check with a full mask that angular distances
     *              (in the middle, on the borders of the mask and on double points) are properly retrieved
     * 
     * @input an azimuth-elevation field of view, some vectors
     * 
     * @output angular distance
     * 
     * @testPassCriteria the angular distance are as expected
     * 
     * @referenceVersion 4.2
     * 
     * @nonRegressionVersion 4.2
     */
    @Test
    public void elevationMaskFieldFull() {

        // Initialization: build mask (linear mask with double point in the middle)
        final double[][] mask = new double[12][2];
        for (int i = 0; i < mask.length; i++) {
            if (i <= 6) {
                mask[i][0] = 2. * i * FastMath.PI / 12;
            } else {
                mask[i][0] = 2. * (i - 1) * FastMath.PI / 12;
            }
            mask[i][1] = i / 12.;
        }
        final IFieldOfView field = new AzimuthElevationField(mask, 0.0, "");

        // Check

        // First point/last point of mask
        final Vector3D d1 = new Vector3D(1.0, 0.0, 1.0);
        Assert.assertEquals(FastMath.PI / 4. - 0., field.getAngularDistance(d1), this.comparisonEpsilon);

        // Regular point of mask
        final Vector3D d2 = new Vector3D(0.0, 1.0, 1.0);
        Assert.assertEquals(FastMath.PI / 4. - 10. / 12., field.getAngularDistance(d2), this.comparisonEpsilon);

        // Regular point of mask on middle of extended part
        final double x = FastMath.PI / 6.;
        final Vector3D d3 = new Vector3D(MathLib.cos(x), -MathLib.sin(x), 1.0);
        Assert.assertEquals(FastMath.PI / 4. - 1. / 12., field.getAngularDistance(d3), this.comparisonEpsilon);

        // Double point of mask: return highest elevation (smallest angular distance)
        final Vector3D d4 = new Vector3D(-1.0, 0.0, 1.0);
        Assert.assertEquals(FastMath.PI / 4. - 7. / 12., field.getAngularDistance(d4), this.comparisonEpsilon);

        // Same check (on double point only) with orientation of Pi / 2.
        final IFieldOfView field2 = new AzimuthElevationField(mask, FastMath.PI / 2., "");
        final Vector3D d5 = new Vector3D(0.0, -1.0, 1.0);
        Assert.assertEquals(FastMath.PI / 4., field2.getAngularDistance(d5), this.comparisonEpsilon);
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH_ELEVATION_FIELD}
     * 
     * @testedMethod {@link AzimuthElevationField#getElevation(double)}
     * 
     * @description check the class behavior with the specific case when we doesn't initialize a azimuth mask with 0.rad
     *              azimuth and we call a value between 0. and the first azimuth value. In general, the method
     *              (getElevation) should work on all the [0; 2PI] interval.
     * 
     * @testPassCriteria no exception is threw and the result is well computed
     * 
     * @referenceVersion 4.2.2
     */
    @Test
    public void evaluationGetElevationMethod() {

        /*
         * The mask is precisely built to cover all the following tests case :
         * - We don't want to have a 0rad first value, to show up a specific case
         * - We would like to avoid 180° and 360° values just to make sure there isn't any weird behavior when the
         * method needs to extrapolate its mask to match those values.
         */
        final double[][] mask = new double[12][2];

        // We define azimuth array at 30°, 60°, 90° ... 300°, 330°.
        for (int i = 0; i < mask.length; i++) {

            if (i < 6) {
                mask[i][0] = 2. * (i + 1) * FastMath.PI / 12; // 30°
            } else {
                mask[i][0] = 2. * i * FastMath.PI / 12; // 30°
            }
            mask[i][1] = i / 12. + 0.05;
        }

        // We want to avoid 180°
        mask[5][0] = 2 * 5.5 * FastMath.PI / 12; // 165°
        mask[6][0] = 2 * 6.5 * FastMath.PI / 12; // 195°

        final AzimuthElevationField field = new AzimuthElevationField(mask, 0.0, "");

        double res = 0;
        final double dAzi = FastMath.toRadians(0.5);
        final double twoPi = 2. * FastMath.PI;

        // Method evaluation on [0; 2PI[rad interval using 0.5° step
        for (double target = 0.; target < twoPi; target += dAzi) {

            // The method shouldn't throw any exception
            try {
                res = field.getElevation(target);
            } catch (IndexOutOfBoundsException e) {
                // Shouldn't occur with the fix
                Assert.fail();
            }

            // Check the specific case result calculation using linear extrapolation for target values [0; mask[0][0]]
            if (target <= mask[0][0]) {
                final double el0 = field.linear(0., mask[mask.length - 1][0] - MathUtils.TWO_PI,
                    mask[0][0], mask[mask.length - 1][1], mask[0][1]);
                final double expectedRes = field.linear(target, 0., mask[0][0], el0, mask[0][1]);

                Assert.assertEquals(expectedRes, res, 0.);
            }
        }
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH_ELEVATION_FIELD}
     * 
     * @testedMethod {@link AzimuthElevationField#getAngularDistance(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#isInTheField(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#getName()}
     * 
     * @description Define mask with specificities on cardinal points (N, E, S, W) 
     *              define a Frame whose x axis points to North 
     *              For each cardinal points, check with a vector which is above, below or equals to the mask limit
     * 
     * @input an azimuth-elevation field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 4.6.1
     * 
     * @nonRegressionVersion 4.6.1
     */
    @Test
    public void elevationMaskFieldCardinalPoints() {

        // Initialization
        final String maskName = "azimuthElevationField";
        
        // azimuth defined from North, clockwise
        final double[][] azimuthElevMask = { 
            { MathLib.toRadians(0), MathLib.toRadians(0) },
            { MathLib.toRadians(40), MathLib.toRadians(0) },
            { MathLib.toRadians(40), MathLib.toRadians(30) },
            { MathLib.toRadians(100), MathLib.toRadians(30) },
            { MathLib.toRadians(100), MathLib.toRadians(0) },
            { MathLib.toRadians(190), MathLib.toRadians(0) },
            { MathLib.toRadians(190), MathLib.toRadians(30) },
            { MathLib.toRadians(280), MathLib.toRadians(30) },
            { MathLib.toRadians(280), MathLib.toRadians(0) },
            };

        // First check (math)
        // Frame x axis is aligned with North (frameOrientaation = 0.0)
        IFieldOfView fov = new AzimuthElevationField(azimuthElevMask, 0.0, maskName);

        //
        // Azimuth North
        //
        
        // Azimuth = North, elevation = 45°, mask = 0° => Delta = 45 > 0 => In the Field
        Vector3D direction = new Vector3D(1.0, 0.0, 1.0);
        double direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        //
        // Azimuth East
        //
        
        // Azimuth = East, elevation = 0°, mask = 30° => Delta = -30 <= 0 => Not In the Field
        direction = new Vector3D(0.0, -1.0, 0.0);
        direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(-30), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        // Azimuth = East, elevation = 45°, mask = 30° => Delta = 45-30 > 0  => In field
        direction = new Vector3D(0.0, -1.0, 1.0);
        direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45-30), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        //
        // Azimuth South
        //

        // Azimuth = South, elevation = 0°, mask = 0° => Delta = 0 <= 0 => Not in field (because equals to the limit)
        direction = new Vector3D(-1.0, 0.0, 0);
        direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(0), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        // Azimuth = South, elevation = 0.1°, mask = 0° => Delta > 0 => In the Field
        direction = new Vector3D(-1.0, 0.0, 0.1);
        Assert.assertTrue(fov.isInTheField(direction));

        //
        // Azimuth West
        //
       
        // Azimuth = West, elevation = 0°, mask = 30° => Delta = - 30 <= 0 => Not in the Field
        direction = new Vector3D(0.0, 1.0, 0.0);
        direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(-30), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        // Azimuth = West, elevation = 45°, mask = 30°  => Delta = 45 > 0 => In the Field
        direction = new Vector3D(0.0, 1.0, 1.0);
        direction2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45-30), direction2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        
        // Other checks
        Assert.assertEquals(maskName, fov.getName());
    }

    /**
     * @testType UT
     * 
     * @testedFeature {@link features#AZIMUTH_ELEVATION_FIELD}
     * 
     * @testedMethod {@link AzimuthElevationField#getAngularDistance(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#isInTheField(Vector3D)}
     * @testedMethod {@link AzimuthElevationField#getName()}
     * 
     * @description Define mask with specificities on cardinal points (N, E, S, W)
     *              define a Frame whose x axis points to North-East 
     *              For each cardinal points, check with a vector which is above, below or equals to the mask limit
     * 
     * @input an azimuth-elevation field of view, some vectors
     * 
     * @output angular distances
     * 
     * @testPassCriteria the angular distances are right, with the expected signs (positive
     *                   if the vector is n the field)
     * 
     * @referenceVersion 4.6.1
     * 
     * @nonRegressionVersion 4.6.1
     */
    @Test
    public void elevationMaskFieldFrameXaxisNorthEast() {

        // Initialization
        final String maskName = "azimuthElevationField";
        
        // azimuth defined from North, clockwise
        final double[][] azimuthElevMask = { 
            { MathLib.toRadians(0), MathLib.toRadians(0) },
            { MathLib.toRadians(40), MathLib.toRadians(0) },
            { MathLib.toRadians(40), MathLib.toRadians(30) },
            { MathLib.toRadians(100), MathLib.toRadians(30) },
            { MathLib.toRadians(100), MathLib.toRadians(0) },
            { MathLib.toRadians(190), MathLib.toRadians(0) },
            { MathLib.toRadians(190), MathLib.toRadians(30) },
            { MathLib.toRadians(280), MathLib.toRadians(30) },
            { MathLib.toRadians(280), MathLib.toRadians(0) },
            };

        // First check (math)
        // frameOrientation = -45° (x aligned to North-East, y aligned to North-West)
        final double northToFrameXAxisAngle = MathLib.toRadians(-45);
        AzimuthElevationField fov = new AzimuthElevationField(azimuthElevMask, northToFrameXAxisAngle, maskName);
        AzimuthElevationCalculator azimuthElevationCalculator = fov.getAzimuthElevationCalculator();

        //
        // Azimuth 45°
        //
        
        // direction azimuth = 45°, elevation = 45°, mask = 30° => Delta = 45-30 > 0 => In the Field
        Vector3D direction = new Vector3D(1.0, 0.0, 1.0);
        
        double dirAzimuth = azimuthElevationCalculator.getAzimuth(direction);
        Assert.assertEquals(MathLib.toRadians(45), dirAzimuth, this.comparisonEpsilon);
        
        double dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45-30), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        //
        // Azimuth 135°
        //
        
        // direction Azimuth = 135°, elevation = 0°, mask = 0° => Delta = 0-0 <= 0 => Not In the Field
        direction = new Vector3D(0.0, -1.0, 0.0);
        
        dirAzimuth = azimuthElevationCalculator.getAzimuth(direction);
        Assert.assertEquals(MathLib.toRadians(135), dirAzimuth, this.comparisonEpsilon);
        
        dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(0), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        // direction Azimuth = 135°, elevation = 45°, mask = 0° => Delta = 45-0 > 0  => In field
        direction = new Vector3D(0.0, -1.0, 1.0);
        
        dirAzimuth = azimuthElevationCalculator.getAzimuth(direction);
        Assert.assertEquals(MathLib.toRadians(135), dirAzimuth, this.comparisonEpsilon);
        
        dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        //
        // Azimuth 225°
        //

        // direction Azimuth = 225°, elevation = 0°, mask = 30° => Delta = 0-30 <= 0 => Not in field (because equals to the limit)
        direction = new Vector3D(-1.0, 0.0, 0);
        
        dirAzimuth = azimuthElevationCalculator.getAzimuth(direction);
        Assert.assertEquals(MathLib.toRadians(225), dirAzimuth, this.comparisonEpsilon);
        
        dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(-30), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        //
        // Azimuth 315°
        //
       
        // direction Azimuth = 315°, elevation = 0°, mask = 0° => Delta = 0 <= 0 => Not in the Field
        direction = new Vector3D(0.0, 1.0, 0.0);
        
        dirAzimuth = azimuthElevationCalculator.getAzimuth(direction);
        Assert.assertEquals(MathLib.toRadians(315), dirAzimuth, this.comparisonEpsilon);
        
        dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(0), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertFalse(fov.isInTheField(direction));

        // Azimuth = 315°, elevation = 45°, mask = 0°  => Delta = 45-0 > 0 => In the Field
        direction = new Vector3D(0.0, 1.0, 1.0);
        
        dir2fovAngle = fov.getAngularDistance(direction);
        Assert.assertEquals(MathLib.toRadians(45), dir2fovAngle, this.comparisonEpsilon);
        Assert.assertTrue(fov.isInTheField(direction));

        
        // Other checks
        Assert.assertEquals(maskName, fov.getName());
    }
}
