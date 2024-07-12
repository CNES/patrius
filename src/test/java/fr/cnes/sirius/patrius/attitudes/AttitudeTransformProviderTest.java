/**
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:DM:DM-3157:10/05/2022:[PATRIUS] Construction d'un AttitudeFrame a partir d'un AttitudeProvider 
 * END-HISTORY
 */
/*
 */
/*
 */
/*
 */
/*
 */
/*
 */
package fr.cnes.sirius.patrius.attitudes;

import junit.framework.Assert;

import org.junit.Test;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfigurationFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.AngularCoordinates;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Test class needed to test the methods of the class {@link AttitudeTransformProvider}.
 * 
 * @author anardi
 *
 */
public class AttitudeTransformProviderTest {
    /**
     * Test needed to validate the constructor of the {@link AttitudeTransformProvider} class and its resulting
     * transform.
     * 
     * @throws PatriusException if transform cannot be computed at given date
     * 
     * @testedMethod {@link AttitudeTransformProvider#AttitudeTransformProvider(AttitudeProvider,
     *               PVCoordinatesProvider, Frame)}
     * @testedMethod {@link AttitudeTransformProvider#getTransform(AbsoluteDate, FramesConfiguration, boolean)}
     */
    @Test
    public void attitudeTransformProviderTest() throws PatriusException {
        // Define the frame
        final Frame frame = FramesFactory.getGCRF();
        // Define the orbit
        final Orbit orbit = new KeplerianOrbit(7000000, 0, 0, 0, 0, 0, PositionAngle.TRUE, frame,
            AbsoluteDate.J2000_EPOCH, Constants.EGM96_EARTH_MU);
        // Create the attitude provider
        final AttitudeProvider attitudeProvider = new AttitudeProvider(){
             /** Serializable UID. */
            private static final long serialVersionUID = 1L;

            /**
             * Set the spin derivatives computations.
             */
            @Override
            public void setSpinDerivativesComputation(final boolean computeSpinDeriv) {
                // Nothing needs to be done here
            }

            /**
             * Get the attitude.
             */
            @Override
            public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame)
                throws PatriusException {
                final AngularCoordinates angularCoord = new AngularCoordinates(Rotation.IDENTITY, Vector3D.PLUS_I,
                    Vector3D.PLUS_K);
                return new Attitude(AbsoluteDate.J2000_EPOCH, FramesFactory.getGCRF(), angularCoord);
            }
        };
        // Build the attitude frame
        final AttitudeTransformProvider attTransfProv = new AttitudeTransformProvider(attitudeProvider, orbit, frame);
        // Check that the attitude transform provider has been correctly built
        Assert.assertNotNull(attTransfProv);
        // Build a transform from the attitude trasform provider
        final Transform transform = attTransfProv.getTransform(new AbsoluteDate(),
            FramesConfigurationFactory.getIERS2010Configuration(), true);
        // Check that the transform built from the attitude transform provider is not null
        Assert.assertNotNull(transform);
    }
}
