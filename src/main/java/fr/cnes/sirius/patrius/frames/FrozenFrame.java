/**
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3197:22/05/2023:[PATRIUS] Deplacement dans PATRIUS de classes façade ALGO DV SIRUS 
 * VERSION:4.11:FA:FA-3277:22/05/2023:[PATRIUS] Ellipsoïde ajuste sur un FacetBodyShape
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Provide a wrapper frame between a reference and a coordinate frame, so that it has no angular velocity
 *
 * @author GMV
 */
public final class FrozenFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 1218815380486390553L;

    /**
     * Build a non-inertial frame that has zero linear/angular velocity to a given reference frame.<br>
     * Represents a frame that returns always as a frozenFrame (so that function actually has no use for this
     * frame).<br>
     * Note that frame is internally declared inertial if reference is so as to allow computation of "osculating"
     * keplerian elements, but this frame CANNOT be used for propagation (or any time-variable usage).<br>
     *
     * @param coordinate
     *            represented frame
     * @param reference
     *            (inertial) frame from which the coordinate frame is frozen
     * @param name
     *            name of the frame (eg coordinate.getName()+".Frozen")
     * @exception IllegalArgumentException
     *                if the parent frame is null
     */
    public FrozenFrame(final Frame coordinate, final Frame reference, final String name)
        throws IllegalArgumentException {
        super(reference, new LocalProvider(reference, coordinate), name, reference.isPseudoInertial());
    }

    /** Local provider for transforms. */
    private static final class LocalProvider implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8095046440436339636L;

        /** Reference frame. */
        private final Frame reference;

        /** Reference frame. */
        private final Frame coordinate;

        /**
         * Simple constructor.
         *
         * @param reference
         *            reference frame
         * @param coordinate
         *            coordinate frame
         */
        public LocalProvider(final Frame reference, final Frame coordinate) {
            this.coordinate = coordinate;
            this.reference = reference;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return getTransform(date, FramesFactory.getConfiguration(), false);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date, final boolean flag) throws PatriusException {
            return getTransform(date, FramesFactory.getConfiguration(), flag);
        }

        /** {@inheritDoc} */
        @Override
        public final Transform getTransform(final AbsoluteDate date, final FramesConfiguration config)
            throws PatriusException {
            return getTransform(date, config, false);
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date, final FramesConfiguration config,
            final boolean computeSpinDerivatives) throws PatriusException {
            return this.reference.getTransformTo(this.coordinate, date, computeSpinDerivatives).freeze();
        }
    }
}
