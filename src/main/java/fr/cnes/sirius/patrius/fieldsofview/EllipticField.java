/**
 * 
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
 * 
 * @history created 16/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import java.util.Locale;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.InfiniteEllipticCone;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class describes an elliptic field of view to be used in "instruments" part properties. It
 * implements the IFieldOfView interface and provides the associated services.
 * 
 * @concurrency immutable
 * 
 * @see IFieldOfView
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public final class EllipticField implements IFieldOfView {

     /** Serializable UID. */
    private static final long serialVersionUID = -2411394755445476577L;

    /** the name of the field */
    private final String inName;

    /** Elliptic cone */
    private final InfiniteEllipticCone cone;

    /** Inverted cone */
    private boolean inverted;

    /** Main direction */
    private final Vector3D mainDir;

    /** Semi major axis direction */
    private final Vector3D uAxisDir;

    /** Semi major angle */
    private final double semiA;

    /** Semi minor angle */
    private final double semiB;

    /** Center of cone */
    private final Vector3D center;

    /**
     * Constructor for an elliptic field of view.
     * 
     * @param name
     *        the name of the field
     * @param origin
     *        origin of the cone
     * @param mainDirection
     *        the direction defining the center of the field
     * @param majorSemiAxisDirection
     *        the direction defining the semi major axis of the field
     * @param angleA
     *        the angular aperture along semi major axis (in rad)
     * @param angleB
     *        the angular aperture along semi minor axis (in rad)
     * @throws IllegalArgumentException
     *         if the origin is not correct
     */
    public EllipticField(final String name, final Vector3D origin, final Vector3D mainDirection,
        final Vector3D majorSemiAxisDirection, final double angleA, final double angleB) {

        if (origin == Vector3D.NaN || origin == Vector3D.NEGATIVE_INFINITY || origin == Vector3D.POSITIVE_INFINITY) {
            throw new IllegalArgumentException(
                PatriusMessages.NULL_NOT_ALLOWED.getLocalizedString(Locale.getDefault()));
        }

        sanityCheck(angleA, angleB);

        /*
         * If the cone is obtuse, the complementary acute cone is considered and the ouput of the getAngularDistance and
         * isInTheField methods is negated.
         */
        if (this.inversionCheck(angleA)) {
            this.cone =
                new InfiniteEllipticCone(Vector3D.ZERO, mainDirection.negate(), majorSemiAxisDirection, FastMath.PI
                    - angleA, FastMath.PI - angleB);
        } else {
            this.cone = new InfiniteEllipticCone(Vector3D.ZERO, mainDirection, majorSemiAxisDirection, angleA, angleB);
        }

        // storage for toString purposes
        this.center = origin;
        this.mainDir = mainDirection.normalize();
        this.uAxisDir =
            majorSemiAxisDirection.subtract(
                this.mainDir.scalarMultiply(majorSemiAxisDirection.dotProduct(this.mainDir)))
                .normalize();
        this.semiA = angleA;
        this.semiB = angleB;

        // storage of name
        this.inName = name;
    }

    /**
     * Check that correct angles have been specified
     * 
     * @param angleA
     *        the angular aperture along semi major axis
     * @param angleB
     *        the angular aperture along semi minor axis
     */
    private static void sanityCheck(final double angleA, final double angleB) {
        // angle aperture test
        if (angleA < Precision.DOUBLE_COMPARISON_EPSILON || Comparators.greaterOrEqual(angleA, FastMath.PI)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        // angle aperture test
        if (angleB < Precision.DOUBLE_COMPARISON_EPSILON || Comparators.greaterOrEqual(angleB, FastMath.PI)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        // make sure the angles definition makes a cone
        if (angleB > Precision.DOUBLE_COMPARISON_EPSILON && angleB <= FastMath.PI / 2 && angleA >= FastMath.PI / 2
            && angleA <= FastMath.PI) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        sanityCheck2(angleA, angleB);
    }

    /**
     * Check that correct angles have been specified
     * 
     * @param angleA
     *        the angular aperture along semi major axis
     * @param angleB
     *        the angular aperture along semi minor axis
     */
    private static void sanityCheck2(final double angleA, final double angleB) {

        // make sure the angles definition makes a cone
        if (Comparators.equals(angleA, FastMath.PI / 2, Precision.DOUBLE_COMPARISON_EPSILON)
            || Comparators.equals(angleB, FastMath.PI / 2, Precision.DOUBLE_COMPARISON_EPSILON)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        if (angleA > Precision.DOUBLE_COMPARISON_EPSILON && angleA <= FastMath.PI / 2 && angleB >= FastMath.PI / 2
            && angleB <= FastMath.PI) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
    }

    /**
     * 
     * Check if the cone is obtuse
     * 
     * @param angleA
     *        the angular aperture along semi major axis
     * 
     * @return true if cone is obtuse, false otherwise
     */
    private boolean inversionCheck(final double angleA) {
        /*
         * The cone is inverted if it is obtuse
         */
        this.inverted = angleA > FastMath.PI / 2;
        return this.inverted;
    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {
        return this.inverted ? -this.getTrueAngularDistance(direction) : this.getTrueAngularDistance(direction);

    }

    /**
     * Computes the angular distance between a vector and the border of the field. The result is positive if the
     * direction is in the field, negative otherwise.
     * 
     * @param direction
     *        the direction vector (expressed in the tropocentric coordinate system of the object)
     * @return the angular distance
     */
    private double getTrueAngularDistance(final Vector3D direction) {

        final double exit;
        /*
         * We assume the direction defined by the origin and closest point on the cone is a good approximation of the
         * closest direction on the cone
         */
        final Vector3D closestPoint = this.cone.closestPointTo(direction);

        if (closestPoint.getX() == this.cone.getOrigin().getX() && closestPoint.getY() == this.cone.getOrigin().getY()
            && closestPoint.getZ() == this.cone.getOrigin().getZ()) {
            /*
             * If the closest point is the apex, return a default value. Continuity of the getTrueAngularDistance
             * function is assured.
             */
            exit = -FastMath.PI / 2;
        } else {
            /*
             * Otherwise, compute the closest point on the cone and return the angular separation
             */
            double sign = -1;
            if (this.cone.isStrictlyInside(direction)) {
                sign = 1;
            }
            exit = sign * Vector3D.angle(closestPoint, direction);
        }
        return exit;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        final boolean exit;
        if (this.inverted) {
            exit = !this.cone.isStrictlyInside(direction);
        } else {
            exit = this.cone.isStrictlyInside(direction);
        }
        return exit;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

    /**
     * Get a representation for this infinite oblique circular cone. The given parameters are in the same order as in
     * the constructor.
     * 
     * @return a representation for this infinite oblique circular cone
     */
    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        // fr.cnes.sirius.patrius.fieldsofview.EllipticField
        final String fullClassName = this.getClass().getName();
        // EllipticField
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        // center of cone coordinates
        res.append("Origin");
        res.append(this.center.toString());
        res.append(comma);
        // main direction vector
        res.append("Direction");
        res.append(this.mainDir.toString());
        res.append(comma);
        // Semi major axis vector
        res.append("U vector");
        res.append(this.uAxisDir.toString());
        res.append(comma);
        // Semi major angle cordinates
        res.append("Angle on U").append(open);
        res.append(this.semiA).append(close);
        res.append(comma);
        // Semi minor angle value
        res.append("Angle on V").append(open);
        res.append(this.semiB).append(close);
        res.append(close);

        return res.toString();
    }

}
