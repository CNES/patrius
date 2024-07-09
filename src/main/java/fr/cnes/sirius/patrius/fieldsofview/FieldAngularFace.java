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
 * @history creation 16/04/2012
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import java.io.Serializable;

import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Plane;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class represent a face of a pyramidal field of view. It can compute the
 * minimal angle between it and a given direction.
 * 
 * @concurrency immutable
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class FieldAngularFace implements Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -74833803428768332L;

    /** first direction of the face */
    private final Vector3D vector1;

    /** second direction of the face */
    private final Vector3D vector2;

    /** the cross product of the two vectors */
    private final Vector3D crossProduct;

    /** the angle bewteen the two vectors */
    private final double angle;

    /** the plane containing the two vectors */
    private final Plane plane;

    /** boolean true if V1 is the closest point of target */
    private boolean closestToV1;

    /** boolean true if V2 is the closest point of target */
    private boolean closestToV2;

    /**
     * Constructor.
     * Vectors must not have a zero norm.
     * 
     * @param firstVector
     *        the first direction of the face
     * @param secondVector
     *        the second direction of the face
     */
    public FieldAngularFace(final Vector3D firstVector, final Vector3D secondVector) {

        if (firstVector.getNorm() < UtilsPatrius.GEOMETRY_EPSILON
            || secondVector.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }

        this.vector1 = new Vector3D(1.0, firstVector);
        this.vector2 = new Vector3D(1.0, secondVector);
        this.angle = Vector3D.angle(this.vector1, this.vector2);

        this.crossProduct = Vector3D.crossProduct(this.vector1, this.vector2);

        this.plane = new Plane(Vector3D.ZERO, this.vector1, this.vector2, false);

        this.closestToV1 = false;
        this.closestToV2 = false;
    }

    /**
     * Computes the minimal angle between this and a given direction.
     * If the direction's dot product to the normal vector to the face
     * (cross product v1*v2), the angle is positive, and negative otherwise.
     * 
     * @param direction
     *        the direction vector
     * @return the signed minimal angular distance to the facet
     */
    public double computeMinAngle(final Vector3D direction) {

        // projection of the direction on the plane containing the facet.
        final Vector3D projection = this.plane.toSpace(this.plane.toSubSpace(direction));

        // angle of this projection to both vectors
        final double angleToV1 = Vector3D.angle(projection, this.vector1);
        final double angleToV2 = Vector3D.angle(projection, this.vector2);

        // if the sum of those angles is equal to the angle between the two vectors,
        // the projection belongs to the face, the angle is computed to it
        double result;
        if (MathLib.abs(this.angle - angleToV1 - angleToV2) < UtilsPatrius.GEOMETRY_EPSILON) {
            result = Vector3D.angle(direction, projection);
            this.closestToV1 = false;
            this.closestToV2 = false;
        } else if (angleToV1 < angleToV2) {
            // if the projection does not belong to the face, the angle is computed to
            // one of the vectors.
            result = Vector3D.angle(direction, this.vector1);
            this.closestToV1 = true;
            this.closestToV2 = false;
        } else {
            result = Vector3D.angle(direction, this.vector2);
            this.closestToV1 = false;
            this.closestToV2 = true;
        }

        // right sign for the angle
        if (Vector3D.dotProduct(this.crossProduct, direction) < 0.0) {
            result = -result;
        }

        return result;
    }

    /**
     * @return true if the direction vector is closest to V1
     *         once the min angle has been computed.
     */
    public boolean isCloseToVstart() {
        return this.closestToV1;
    }

    /**
     * @return true if the direction vector is closest to V2
     *         once the min angle has been computed.
     */
    public boolean isCloseToVend() {
        return this.closestToV2;
    }

}
