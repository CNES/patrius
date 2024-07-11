/**
 * Copyright 2011-2020 CNES
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
 * VERSION:4.9:DM:DM-3159:10/05/2022:[PATRIUS] Implementation de l'interface GeometricBodyShape par CelestialBody
 * VERSION:4.9:FA:FA-3174:10/05/2022:[PATRIUS] Corriger les differences de convention entre toutes les methodes de calcul ...
 * VERSION:4.9:DM:DM-3169:10/05/2022:[PATRIUS] Precision de l'hypothese de propagation instantanee de la lumiere
 * VERSION:4.7:FA:FA-2821:18/05/2021:Refus injustifié de calcul de l incidence solaire lorsqu elle dépasse 90 degres 
 * VERSION:4.7:DM:DM-2767:18/05/2021:Evolutions et corrections diverses 
 * VERSION:4.6:DM:DM-2544:27/01/2021:Ajouter la definition d'un corps celeste a partir d'un modele de forme 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * VERSION:4.6:DM:DM-2591:27/01/2021:[PATRIUS] Intigration et validation JOptimizer
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.bodies.mesh;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Line;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Data container for {@link FacetBodyShape} surface data. A surface data is a {@link FacetBodyShape} surface
 * data derived from a date and a body point (incidence, solar incidence, distance, etc.). The light propagation is
 * considered as instantaneous to compute the solar incidence.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.6
 */
public class SurfacePointedData {

    /** Date. */
    private final AbsoluteDate date;

    /** Intersection between the line of sight and the body. */
    private final Intersection intersection;

    /** Incidence. */
    private final double incidence;

    /** Solar incidence. */
    private final double solarIncidence;

    /** Phase angle. */
    private double phaseAngle;

    /** Distance from viewer to target point. */
    private final double distance;

    /** Resolution. */
    private final double resolution;

    /**
     * Constructor. The light propagation is considered as instantaneous.
     * @param body facet 
     * @param date date
     * @param position satellite position in frame
     * @param direction line of sight direction in frame
     * @param frame frame in which position and direction are expressed
     * @param sun sun
     * @param pixelFOV aperture of field of view for one pixel 
     * @throws PatriusException thrown if intersection is null or Sun frame conversion failed
     */
    public SurfacePointedData(final FacetBodyShape body,
            final AbsoluteDate date,
            final Vector3D position,
            final Vector3D direction,
            final Frame frame,
            final PVCoordinatesProvider sun,
            final double pixelFOV) throws PatriusException {
        this.date = date;

        // Compute intersection point and triangle
        final Line line = new Line(position, position.add(direction));
        // Transform the position vector in body frame
        final Transform toBodyFrame = frame.getTransformTo(body.getBodyFrame(), date);
        final Vector3D posInBodyFrame = toBodyFrame.transformPosition(position);
        
        this.intersection = body.getIntersection(line, posInBodyFrame, frame, date);
        if (this.intersection == null) {
            // Surface not visible from sensor
            throw new PatriusException(PatriusMessages.MNT_SURFACE_NOT_VISIBLE_FROM_SENSOR);
        }
        final Triangle triangle = intersection.getTriangle();

        // Compute parameters
        this.incidence = Vector3D.angle(triangle.getNormal().negate(), direction);
        if (this.incidence >= MathLib.PI / 2.) {
            // Surface not visible from sensor
            throw new PatriusException(PatriusMessages.MNT_SURFACE_NOT_VISIBLE_FROM_SENSOR);
        }
        final Vector3D intersectionPoint = intersection.getPoint();
        final Vector3D sunPos = sun.getPVCoordinates(date, frame).getPosition();
        final Vector3D sunVector = intersectionPoint.subtract(sunPos);
        this.solarIncidence = Vector3D.angle(triangle.getNormal().negate(), sunVector);
        this.phaseAngle = Vector3D.angle(sunVector, direction);
        if (this.solarIncidence >= MathLib.PI / 2.) {
            this.phaseAngle = Double.NaN;
        }
        this.distance = Vector3D.distance(position, intersectionPoint);

        // Compute resolution
        this.resolution = distance * MathLib.cos(incidence)
                * (MathLib.tan(incidence + pixelFOV / 2) - MathLib.tan(incidence - pixelFOV / 2));
    }

    /**
     * Returns the date.
     * @return the date
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /**
     * Returns the intersection between the line of sight and the body in body frame.
     * @return the intersection between the line of sight and the body in body frame
     */
    public Intersection getIntersection() {
        return intersection;
    }

    /**
     * Returns the incidence which is the angle between the normal of the intersection triangle and the sight direction.
     * @return the incidence
     */
    public double getIncidence() {
        return incidence;
    }

    /**
     * Returns the solar incidence which is the angle between the normal of the intersection triangle and the Sun
     * direction.
     * @return the solar incidence
     */
    public double getSolarIncidence() {
        return solarIncidence;
    }

    /**
     * Returns the phase angle which is the angle between the sight direction and the Sun direction.
     * @return the phase angle
     */
    public double getPhaseAngle() {
        return phaseAngle;
    }

    /**
     * Returns the resolution.
     * @return the resolution
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * Returns the distance from viewer to target point.
     * @return the distance from viewer to target point
     */
    public double getDistance() {
        return distance;
    }
}
