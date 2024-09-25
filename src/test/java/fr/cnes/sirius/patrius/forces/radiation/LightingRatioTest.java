/**
 * Copyright 2023-2023 CNES
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
 * VERSION:4.13.1:FA:FA-176:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.13:DM:DM-101:08/12/2023:[PATRIUS] Harmonisation des eclipses pour les evenements et pour la PRS
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.radiation;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.OneAxisEllipsoid;
import fr.cnes.sirius.patrius.events.detectors.AbstractSignalPropagationDetector.PropagationDelayType;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.ConstantPVCoordinatesProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class for {@link LightingRatio}
 *
 * @author Emmanuel Bignon
 *
 * @since 4.13
 */
public class LightingRatioTest {

    /**
     * @testType UT
     *
     * @description check that lighting ratio is properly computed in various math configurations:
     *              <ul>
     *              <li>Occulted body not visible, occulted body radius = 0</li>
     *              <li>Occulted body visible, occulted body radius = 0</li>
     *              <li>Occulted body not visible, occulted body radius small</li>
     *              <li>Occulted body visible, occulted body radius small</li>
     *              <li>Occulted body behind occulting body, occulted body radius larger than projection of occulting body radius</li>
     *              <li>Occulted body partly visible</li>
     *              <li>Satellite inside occulting body apparent radius</li>
     *              <li>Occulted body is between occulting body and satellite</li>
     *              <li>Occulted body partly visible, signal propagation is taken into account</li>
     *              </ul>
     *
     * @testPassCriteria lighting ratio is as expected (reference: math, absolute threshold: 0)
     *
     * @referenceVersion 4.13
     *
     * @nonRegressionVersion 4.13
     */
    @Test
    public void testCompute() throws PatriusException {
        Utils.setDataRoot("regular-dataPBASE");
        
        // Initialization
        final CelestialBodyFrame gcrf = FramesFactory.getGCRF();
        final AbsoluteDate referenceDate = AbsoluteDate.J2000_EPOCH;
        // Occulting body in test: sphere of radius 1
        final BodyShape occultingBody = new OneAxisEllipsoid(1., 0., gcrf);

        // Case 1: occulted body not visible, occulted body radius = 0
        PVCoordinatesProvider occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10),
                gcrf);
        LightingRatio lightingRatio = new LightingRatio(occultingBody, occultedBody, 0.);
        PVCoordinatesProvider satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        Assert.assertEquals(0., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 2: occulted body visible, occulted body radius = 0
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 0.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_J.scalarMultiply(10), gcrf);
        Assert.assertEquals(1., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 3: occulted body not visible, occulted body radius small
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        Assert.assertEquals(0., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 4: occulted body visible, occulted body radius small
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_J.scalarMultiply(10), gcrf);
        Assert.assertEquals(1., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 5: occulted body behind occulting body, occulted body radius larger than projection of occulting body radius
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 10.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        Assert.assertEquals(1 - MathLib.PI * 2 * 2 / (MathLib.PI * 10 * 10), lightingRatio.compute(satellite, referenceDate), 0.005);

        // Case 6: occulted body partly visible
        occultedBody = new ConstantPVCoordinatesProvider(new Vector3D(-10, 2, 0), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        Assert.assertEquals(0.5468673338382262, lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 7: satellite inside occulting body apparent radius
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.MINUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        satellite = new ConstantPVCoordinatesProvider(new Vector3D(0.9, 0, 0), gcrf);
        Assert.assertEquals(0., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 8: occulted body is between occulting body and satellite
        occultedBody = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(20), gcrf);
        Assert.assertEquals(1., lightingRatio.compute(satellite, referenceDate), 0.);

        // Case 9: occulted body partly visible, signal propagation is taken into account
        // Check that lighting ratio is slightly different from instantaneous case
        // Also check signal propagation getters
        occultedBody = new PVCoordinatesProvider() {
            /** Serializable UID. */
            private static final long serialVersionUID = 562339513611526961L;

            @Override
            public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
                final Transform t = gcrf.getTransformTo(frame, date);
                return t.transformPVCoordinates(new PVCoordinates(new Vector3D(-10, 2, 0).add(new Vector3D(0, date
                        .durationFrom(referenceDate), 0)), Vector3D.ZERO));
            }

            @Override
            public Frame getNativeFrame(final AbsoluteDate date) {
                return gcrf;
            }
        };
        lightingRatio = new LightingRatio(occultingBody, occultedBody, 1.);
        lightingRatio.setPropagationDelayType(PropagationDelayType.LIGHT_SPEED, FramesFactory.getICRF());
        lightingRatio.setEpsilonSignalPropagation(1E-14);
        lightingRatio.setMaxIterSignalPropagation(100);
        satellite = new ConstantPVCoordinatesProvider(Vector3D.PLUS_I.scalarMultiply(10), gcrf);
        Assert.assertNotSame(0.5468673338382262, lightingRatio.compute(satellite, referenceDate));
        Assert.assertEquals(0.5468673338382262,  lightingRatio.compute(satellite, referenceDate), 1E-3);
        Assert.assertEquals(PropagationDelayType.LIGHT_SPEED, lightingRatio.getPropagationDelayType());
        Assert.assertEquals(FramesFactory.getICRF(), lightingRatio.getInertialFrame());
        Assert.assertEquals(0., lightingRatio.getEpsilonSignalPropagation(), 1e-14);
        Assert.assertEquals(100, lightingRatio.getMaxIterSignalPropagation());
    }
}
