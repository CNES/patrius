/**
 * 
 * Copyright 2011-2017 CNES
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
 * @history creation 15/05/2012
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * <p>
 * This class defines a "sector" field of view. This field is a min/max longitude and min/max latitude aperture on the
 * unit sphere. It is defined by three vectors : the local "north pole" vector for this sphere, the min latitude - min
 * longitude point vector (V1) and the max latitude - max longitude point vector (V2).
 * </p>
 * 
 * @concurrency immutable
 * 
 * @see IFieldOfView
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class SectorField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = -6024580242542737649L;

    /** the name of the field */
    private final String inName;

    /** the local "north pole" direction on the unit sphere */
    private final Vector3D inVectorPole;

    /** the min latitude - min longitude point vector on the unit sphere */
    private final Vector3D inVectorV1;

    /** the max latitude - max longitude point vector on the unit sphere */
    private final Vector3D inVectorV2;

    /** cross product (pole, V1) */
    private final Vector3D e1;

    /** cross product (V2, pole) */
    private final Vector3D e2;

    /** boolean true if the P, V1, V2 basis is direct */
    private final boolean direct;

    /** condition for the direction to be between the right longitudes */
    private boolean longitudeCondition;

    /** condition for the direction to be between the right latitudes */
    private boolean latitudeCondition;

    /** dot product : V1.Pole */
    private final double v1DotPole;

    /** dot product : V2.Pole */
    private final double v2DotPole;

    /** angular "minimal longitude" face */
    private final FieldAngularFace longMinFace;

    /** angular "max longitude" face */
    private final FieldAngularFace longMaxFace;

    /**
     * Constructor for the "sector" field of view.
     * The latitude of the V2 point must be greater than the latitude of the V1 point (the angle
     * from the pole vector to V1 must be greater than the angle from the pole vector to V2).
     * None of them can aligned with or opposite to the "pole" vector.
     * 
     * @param name
     *        the name of the field
     * @param vectorPole
     *        the local "north pole" direction on the unit sphere (will be normalized)
     * @param vectorV1
     *        the min latitude - min longitude point vector on the unit sphere (will be normalized)
     * @param vectorV2
     *        the max latitude - max longitude point vector on the unit sphere (will be normalized)
     */
    public SectorField(final String name, final Vector3D vectorPole, final Vector3D vectorV1,
        final Vector3D vectorV2) {

        this.inName = name;
        this.inVectorPole = vectorPole.normalize();
        this.inVectorV1 = vectorV1.normalize();
        this.inVectorV2 = vectorV2.normalize();

        final double angleV1Pole = Vector3D.angle(this.inVectorPole, this.inVectorV1);
        final double angleV2Pole = Vector3D.angle(this.inVectorPole, this.inVectorV2);

        //
        if (Comparators.lowerOrEqual(angleV1Pole, angleV2Pole, UtilsPatrius.GEOMETRY_EPSILON)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_INVALID_VECTORS_FOR_SECTOR_FIELD);
        }
        if (Comparators.equals(angleV1Pole, FastMath.PI, UtilsPatrius.GEOMETRY_EPSILON)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_INVALID_VECTORS_FOR_SECTOR_FIELD);
        }
        // comparison to zero : "<" needed
        if (angleV2Pole < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_INVALID_VECTORS_FOR_SECTOR_FIELD);
        }

        // initialisations
        // cross products
        this.e1 = Vector3D.crossProduct(this.inVectorPole, this.inVectorV1);
        this.e2 = Vector3D.crossProduct(this.inVectorV2, this.inVectorPole);

        // is the absis direct
        this.direct = (Vector3D.dotProduct(this.e1, this.inVectorV2) > 0.0);

        // dot products
        this.v1DotPole = Vector3D.dotProduct(this.inVectorV1, this.inVectorPole);
        this.v2DotPole = Vector3D.dotProduct(this.inVectorV2, this.inVectorPole);

        // field angular faces : longitudes min and max
        final Rotation rotV1 = new Rotation(this.e1, angleV2Pole - angleV1Pole);
        final Rotation rotV2 = new Rotation(this.e2, angleV2Pole - angleV1Pole);
        final Vector3D v3 = rotV1.applyTo(this.inVectorV1);
        final Vector3D v4 = rotV2.applyTo(this.inVectorV2);
        this.longMinFace = new FieldAngularFace(v3, this.inVectorV1);
        this.longMaxFace = new FieldAngularFace(v4, this.inVectorV2);
    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {

        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            return 0.0;
        } else {
            final Vector3D directionNormalised = direction.normalize();
            this.computeConditions(directionNormalised);
            final double dist;

            // minimal distance to the meridians
            final double distanceToMeridians;
            final double distanceToEstMer = this.longMaxFace.computeMinAngle(directionNormalised);
            final double distanceToWestMer = this.longMinFace.computeMinAngle(directionNormalised);
            if (MathLib.abs(distanceToEstMer) > MathLib.abs(distanceToWestMer)) {
                distanceToMeridians = distanceToWestMer;
            } else {
                distanceToMeridians = distanceToEstMer;
            }

            // if the direction is between the two right longitudes,
            // the distance is a latitude difference
            if (this.longitudeCondition) {
                final double anglePoleV1 = Vector3D.angle(this.inVectorPole, this.inVectorV1);
                final double anglePoleDirection = Vector3D.angle(this.inVectorPole, directionNormalised);
                final double anglePoleV2 = Vector3D.angle(this.inVectorPole, this.inVectorV2);

                final double distToParallel =
                    MathLib.min((anglePoleV1 - anglePoleDirection), (anglePoleDirection - anglePoleV2));

                // if the direction is inside the field, a comparison is made
                // to the "distance to the meridians"
                if (distToParallel > 0.0) {
                    dist = MathLib.min(distanceToMeridians, distToParallel);
                } else {
                    dist = distToParallel;
                }
            } else {
                // if the direction is not between the two right longitudes,
                // the distance is computed to the angular "minimal longitude" and
                // "maximal longitude" face
                dist = -MathLib.abs(distanceToMeridians);
            }
            return dist;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        final Vector3D directionNormalised = direction.normalize();
        this.computeConditions(directionNormalised);
        return this.latitudeCondition && this.longitudeCondition;
    }

    /**
     * Compute the conditions used in the getAngularDistance and isInTheField methods.
     * 
     * @param direction
     *        the tested direction vector
     */
    private void computeConditions(final Vector3D direction) {

        // latitude
        final double directionDotPole = Vector3D.dotProduct(direction, this.inVectorPole);
        this.latitudeCondition = ((directionDotPole > this.v1DotPole) && (directionDotPole < this.v2DotPole));

        // longitude
        final boolean c1 = (Vector3D.dotProduct(direction, this.e1) > 0.0);
        final boolean c2 = (Vector3D.dotProduct(direction, this.e2) > 0.0);
        final boolean subCondition1 = this.direct && (c1 && c2);
        final boolean subCondition2 = !this.direct && (c1 || c2);
        this.longitudeCondition = (subCondition1 || subCondition2);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

}
