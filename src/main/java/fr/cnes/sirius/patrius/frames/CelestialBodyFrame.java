/**
 * Copyright 2022-2022 CNES
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
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;

/**
 * Frame centered on a {@link CelestialBody}.
 * This frames contains a {@link CelestialBody}. For {@link FactoryManagedFrame} such as GCRF frame,
 * Earth celestial body is not attached, so users need to attach it using {@link #setCelestialBody(CelestialBody)}.
 * <b>Warning: this class does not check if provided celestial body is indeed
 * centered on this frame.</b>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.10
 */
public class CelestialBodyFrame extends Frame {

     /** Serializable UID. */
    private static final long serialVersionUID = 5182000151766548579L;

    /** Celestial body centered on this frame. */
    private CelestialBody celestialBody;

    /**
     * Protected constructor used only for the root frame.
     * 
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     */
    protected CelestialBodyFrame(final String name,
            final boolean pseudoInertial) {
        super(name, pseudoInertial);
    }

    /**
     * Constructor.
     * 
     * <b>Warning: this class does not check if provided celestial body is indeed
     * centered on this frame.</b>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param celestialBody
     *        celestial body centered on this frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent,
            final Transform transform,
            final String name,
            final CelestialBody celestialBody) {
        this(parent, transform, name, false, celestialBody);
    }

    /**
     * Constructor.
     * 
     * <b>Warning: this class does not check if provided celestial body is indeed
     * centered on this frame.</b>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transformProvider
     *        provider for transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param celestialBody
     *        celestial body centered on this frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent,
            final TransformProvider transformProvider,
            final String name,
            final CelestialBody celestialBody) {
        this(parent, transformProvider, name, false, celestialBody);
    }

    /**
     * Constructor.
     * 
     * <b>Warning: this class does not check if provided celestial body is indeed
     * centered on this frame.</b>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @param celestialBody
     *        celestial body centered on this frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent,
            final Transform transform,
            final String name,
            final boolean pseudoInertial,
            final CelestialBody celestialBody) {
        this(parent, new FixedTransformProvider(transform), name, pseudoInertial, celestialBody);
    }

    /**
     * Generic constructor.
     * 
     * <b>Warning: this class does not check if provided celestial body is indeed
     * centered on this frame.</b>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transformProvider
     *        provider for transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @param celestialBody
     *        celestial body centered on this frame
     * @exception IllegalArgumentException
     *            if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent,
            final TransformProvider transformProvider,
            final String name,
            final boolean pseudoInertial,
            final CelestialBody celestialBody) {
        super(parent, transformProvider, name, pseudoInertial);
        this.celestialBody = celestialBody;
    }

    /**
     * Returns the celestial body centered on this frame.
     * @return the celestial body centered on this frame
     */
    public CelestialBody getCelestialBody() {
        return celestialBody;
    }

    /**
     * Set the celestial body.
     * @param celestialBody the celestial body to set
     */
    public void setCelestialBody(final CelestialBody celestialBody) {
        this.celestialBody = celestialBody;
    }
}
