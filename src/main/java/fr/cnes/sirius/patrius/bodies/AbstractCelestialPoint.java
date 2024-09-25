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
 * HISTORY
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-111:08/12/2023:[PATRIUS] Problemes lies à  l'utilisation des bsp
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.bodies.bsp.BSPEphemerisLoader.SpiceJ2000ConventionEnum;
import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.configuration.FramesConfiguration;
import fr.cnes.sirius.patrius.frames.transformations.EME2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.EclipticJ2000Provider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract implementation of the {@link CelestialPoint} interface.
 * <p>
 * This abstract implementation provides basic services that can be shared by most implementations of the
 * {@link CelestialPoint} interface. It holds the gravitational attraction coefficient and build some body-centered
 * frames automatically (ICRF, EME2000, etc.).
 * </p>
 *
 * @author Emmanuel Bignon
 * 
 * @since 4.13
 */
@SuppressWarnings("PMD.NullAssignment")
public abstract class AbstractCelestialPoint implements CelestialPoint {

    /** Serializable UID. */
    private static final long serialVersionUID = -4482417179048551814L;

    /** Space. */
    private static final String SPACE = " ";

    /** Name of the point. */
    private final String name;

    /** ICRF oriented, point-centered frame. */
    private CelestialBodyFrame icrfFrame;

    /** EME2000 oriented, point-centered frame. */
    private CelestialBodyFrame eme2000Frame;

    /** EclipticJ2000 oriented, point-centered frame. */
    private CelestialBodyFrame eclipticJ2000Frame;

    /** Point ephemeris. */
    private CelestialBodyEphemeris ephemeris;

    /** Gravitational parameter. */
    private double gm;

    /**
     * Constructor with parent frame.
     * 
     * @param name
     *        name
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param ephemeris
     *        ephemeris
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param convention
     *        spice convention for BSP frames
     */
    public AbstractCelestialPoint(final String name, final double gm, final CelestialBodyEphemeris ephemeris,
                                   final Frame parentFrame, final SpiceJ2000ConventionEnum convention) {
        this.name = name;
        this.gm = gm;
        this.ephemeris = ephemeris;

        // Build frames
        if (convention.equals(SpiceJ2000ConventionEnum.ICRF)) {
            // ICRF
            this.icrfFrame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame), name + SPACE
                    + ICRF_FRAME_NAME, true, this);
        } else {
            // EME2000
            this.eme2000Frame = new CelestialBodyFrame(parentFrame, new ICRFOriented(parentFrame), name + SPACE
                    + EME2000_FRAME_NAME, true, this);
        }
        
        // Build other frames
        setFrameTree();
    }

    /**
     * Constructor with icrf frame.
     * 
     * @param name
     *        name
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param ephemeris
     *        ephemeris
     * @param icrf
     *        icrf frame centered on this point
     */
    public AbstractCelestialPoint(final String name, final double gm, final CelestialBodyEphemeris ephemeris,
                                   final CelestialBodyFrame icrf) {
        this.name = name;
        this.gm = gm;
        this.ephemeris = ephemeris;
        this.icrfFrame = icrf;

        // Build frames
        setFrameTree();
    }

    /**
     * Constructor.
     *
     * @param name
     *        name of the body
     * @param gm
     *        gravitational attraction coefficient (in m<sup>3</sup>/s<sup>2</sup>)
     * @param parentFrame
     *        parent frame (usually it should be the ICRF centered on the parent body)
     * @param ephemeris ephemeris
     */
    public AbstractCelestialPoint(final String name, final double gm, final Frame parentFrame,
                                   final CelestialBodyEphemeris ephemeris) {
        this(name, gm, ephemeris, parentFrame, SpiceJ2000ConventionEnum.ICRF);
    }

    /**
     * Instantiate all the frames linked to the point.
     */
    private final void setFrameTree() {
        if (this.icrfFrame == null) {
            this.icrfFrame = new CelestialBodyFrame(this.eme2000Frame, new EME2000Provider().getTransform(null)
                .getInverse(), this.name + SPACE + ICRF_FRAME_NAME, true, this);
        }
        if (this.eme2000Frame == null) {
            this.eme2000Frame = new CelestialBodyFrame(this.icrfFrame, new EME2000Provider(), this.name + SPACE
                    + EME2000_FRAME_NAME, true, this);
        }
        this.eclipticJ2000Frame = new CelestialBodyFrame(this.icrfFrame, new EclipticJ2000Provider(), this.name + SPACE
                + ECLIPTICJ2000_FRAME_NAME, true, this);
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.icrfFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
        return this.icrfFrame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getICRF() {
        return this.icrfFrame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getEME2000() {
        return this.eme2000Frame;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyFrame getEclipticJ2000() {
        return this.eclipticJ2000Frame;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public double getGM() {
        return this.gm;
    }

    /** {@inheritDoc} */
    @Override
    public void setGM(final double gmIn) {
        this.gm = gmIn;
    }

    /** {@inheritDoc} */
    @Override
    public CelestialBodyEphemeris getEphemeris() {
        return this.ephemeris;
    }

    /** {@inheritDoc} */
    @Override
    public void setEphemeris(final CelestialBodyEphemeris ephemerisIn) {
        this.ephemeris = ephemerisIn;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        // End commentary
        final String end = "\n";
        // String builder
        final StringBuilder builder = new StringBuilder();
        // Add data
        builder.append("- Name: " + this.name + end);
        builder.append("- Corps type: " + this.getClass().getSimpleName() + " class" + end);
        builder.append("- GM: " + getGM() + end);
        // Add all frames
        builder.append("- ICRF frame: " + getICRF().toString() + end);
        builder.append("- EME2000 frame: " + getEME2000().toString() + end);
        builder.append("- Ecliptic J2000 frame: " + getEclipticJ2000().toString() + end);
        // Return builder.toString
        return builder.toString();
    }

    /**
     * Provider for ICRF oriented point-centered frame transform.
     *
     * <p>
     * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
     * derivative.
     * </p>
     * <p>
     * Frames configuration is unused.
     * </p>
     */
    private class ICRFOriented implements TransformProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = -8849993808761896559L;

        /** Parent frame (usually it should be the ICRF centered on the parent point). */
        private final Frame parentFrame;

        /**
         * Simple constructor.
         *
         * @param parentFrame
         *        parent frame (usually it should be the ICRF centered on the parent point)
         */
        public ICRFOriented(final Frame parentFrame) {
            this.parentFrame = parentFrame;
        }

        /** {@inheritDoc} */
        @Override
        public Transform getTransform(final AbsoluteDate date) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final FramesConfiguration config) throws PatriusException {
            return this.getTransform(date, config, false);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null. No analytical formula is available for spin
         * derivative.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final boolean computeSpinDerivatives) throws PatriusException {
            return this.getTransform(date, FramesFactory.getConfiguration(), computeSpinDerivatives);
        }

        /**
         * {@inheritDoc}
         * <p>
         * Spin derivative is never computed and is either 0 or null.
         * </p>
         * <p>
         * Frames configuration is unused.
         * </p>
         */
        @Override
        public Transform getTransform(final AbsoluteDate date,
                final FramesConfiguration config,
                final boolean computeSpinDerivatives) throws PatriusException {
            // compute translation from parent frame to self
            final PVCoordinates pv = getEphemeris().getPVCoordinates(date, this.parentFrame);
            return new Transform(date, pv);
        }
    }
}
