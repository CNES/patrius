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
 * VERSION:4.13.1:FA:FA-169:17/01/2024:[PATRIUS] Gestion du celestialPoint dans CelestialBodyFrame non rigoureuse
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.13:DM:DM-4:08/12/2023:[PATRIUS] Lien entre un repere predefini et un CelestialBody
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.frames.transformations.FixedTransformProvider;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Frame centered on a {@link CelestialPoint}.
 * <p>
 * <b>Warning: this class does not check if provided celestial body is indeed centered on this frame.</b>
 * </p>
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.10
 */
public class CelestialBodyFrame extends Frame {

     /** Serializable UID. */
    private static final long serialVersionUID = 5182000151766548579L;

    /** Celestial point centered on this frame. */
    protected CelestialPoint celestialPoint;

    /**
     * Protected constructor used only for the root frame.
     * 
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     */
    protected CelestialBodyFrame(final String name, final boolean pseudoInertial) {
        super(name, pseudoInertial);
    }

    /**
     * Constructor.
     * <p>
     * <b>Warning: this class does not check if provided celestial body is indeed centered on this frame.</b>
     * </p>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param celestialPoint
     *        celestial point centered on this frame
     * @throws IllegalArgumentException
     *         if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent, final Transform transform, final String name,
                              final CelestialPoint celestialPoint) {
        this(parent, transform, name, false, celestialPoint);
    }

    /**
     * Constructor.
     * <p>
     * <b>Warning: this class does not check if provided celestial body is indeed centered on this frame.</b>
     * </p>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transformProvider
     *        provider for transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param celestialPoint
     *        celestial point centered on this frame
     * @throws IllegalArgumentException
     *         if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent, final TransformProvider transformProvider, final String name,
                              final CelestialPoint celestialPoint) {
        this(parent, transformProvider, name, false, celestialPoint);
    }

    /**
     * Constructor.
     * <p>
     * <b>Warning: this class does not check if provided celestial body is indeed centered on this frame.</b>
     * </p>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transform
     *        transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @param celestialPoint
     *        celestial point centered on this frame
     * @throws IllegalArgumentException
     *         if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent, final Transform transform, final String name,
                              final boolean pseudoInertial, final CelestialPoint celestialPoint) {
        this(parent, new FixedTransformProvider(transform), name, pseudoInertial, celestialPoint);
    }

    /**
     * Generic constructor.
     * <p>
     * <b>Warning: this class does not check if provided celestial body is indeed centered on this frame.</b>
     * </p>
     * 
     * @param parent
     *        parent frame (must be non-null)
     * @param transformProvider
     *        provider for transform from parent frame to instance
     * @param name
     *        name of the frame
     * @param pseudoInertial
     *        true if frame is considered pseudo-inertial (i.e. suitable for propagating orbit)
     * @param celestialPoint
     *        celestial point centered on this frame
     * @throws IllegalArgumentException
     *         if the parent frame is null
     */
    public CelestialBodyFrame(final Frame parent, final TransformProvider transformProvider, final String name,
                              final boolean pseudoInertial, final CelestialPoint celestialPoint) {
        super(parent, transformProvider, name, pseudoInertial);
        this.celestialPoint = celestialPoint;
    }

    /**
     * Getter for the celestial point centered on this frame.
     * 
     * @return the celestial point centered on this frame
     * @throws PatriusException
     *         if point could not be built
     */
    public CelestialPoint getCelestialPoint() throws PatriusException {
        return this.celestialPoint;
    }
}
