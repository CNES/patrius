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
 * HISTORY
 * VERSION:4.13:DM:DM-32:08/12/2023:[PATRIUS] Ajout d'un ThreeAxisEllipsoid
 * VERSION:4.13:DM:DM-44:08/12/2023:[PATRIUS] Organisation des classes de detecteurs d'evenements
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:DM:DM-37:08/12/2023:[PATRIUS] Date d'evenement et propagation du signal
 * VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite
 * de convertir les sorties de VacuumSignalPropagation
 * VERSION:4.13:FA:FA-144:08/12/2023:[PATRIUS] la methode BodyShape.getBodyFrame devrait
 * retourner un CelestialBodyFrame
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies;

import fr.cnes.sirius.patrius.frames.CelestialBodyFrame;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Abstract class for a body shape to mutualize parameters and features.
 *
 * @see BodyShape
 *
 * @author Thibaut BONIT
 *
 * @since 4.12
 */
public abstract class AbstractBodyShape implements BodyShape {

    /** Default epsilon (s) for signal propagation computation. */
    public static final double DEFAULT_EPSILON_SIGNAL_PROPAGATION = 1e-14;

    /** Serializable UID. */
    private static final long serialVersionUID = -5698984389969206821L;

    /** LLH coordinates system. */
    protected LLHCoordinatesSystem lLHCoordinatesSystem;

    /**
     * Altitude epsilon below which the altitude coordinate is neglected: below this value,the method
     * {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate)} will be automatically used instead of
     * {@link #getIntersectionPoint(Line, Vector3D, Frame, AbsoluteDate, double)}. This epsilon altitude is also used to
     * assess if a body point is on the shape surface or not (method {@link BodyPoint#isOnShapeSurface(double)}.
     */
    protected double distanceEpsilon = DEFAULT_DISTANCE_EPSILON;

    /** Epsilon for signal propagation computation. */
    private double epsSignalPropagation = DEFAULT_EPSILON_SIGNAL_PROPAGATION;

    /** Body name. */
    private final String name;

    /** Body frame. */
    private final CelestialBodyFrame bodyFrame;

    /**
     * Constructor.
     *
     * @param name
     *        body name
     * @param bodyFrame
     *        frame in which celestial body coordinates are defined
     */
    public AbstractBodyShape(final String name, final CelestialBodyFrame bodyFrame) {
        this.name = name;
        this.bodyFrame = bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public final String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public final CelestialBodyFrame getBodyFrame() {
        return this.bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public PVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) throws PatriusException {
        return this.bodyFrame.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Frame getNativeFrame(final AbsoluteDate date) {
        return this.bodyFrame;
    }

    /** {@inheritDoc} */
    @Override
    public final LLHCoordinatesSystem getLLHCoordinatesSystem() {
        return this.lLHCoordinatesSystem;
    }

    /** {@inheritDoc} */
    @Override
    public final void setLLHCoordinatesSystem(final LLHCoordinatesSystem coordSystem) {
        this.lLHCoordinatesSystem = coordSystem;
    }

    /** {@inheritDoc} */
    @Override
    public double getDistanceEpsilon() {
        return this.distanceEpsilon;
    }

    /** {@inheritDoc} */
    @Override
    public void setDistanceEpsilon(final double epsilon) {
        this.distanceEpsilon = epsilon;
    }

    /** {@inheritDoc} */
    @Override
    public double getEpsilonSignalPropagation() {
        return this.epsSignalPropagation;
    }

    /** {@inheritDoc} */
    @Override
    public void setEpsilonSignalPropagation(final double epsilon) {
        this.epsSignalPropagation = epsilon;
    }
}
