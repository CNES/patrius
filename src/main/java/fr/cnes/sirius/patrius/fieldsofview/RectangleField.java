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
 * @history creation 16/04/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3110:10/05/2022:[PATRIUS] Bugs dans RectangleField 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2472:27/05/2020:Ajout d'un getter de sideAxis aux classes RectangleField et PyramidalField
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:217:10/03/2014:Corrected erroneous initialization of base vectors
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Matrix3D;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class describes a field of view defined by a rectangle cone, to be used in
 * "instruments" part properties. It implements the IFieldOfView interface
 * and so provides the associated services.
 * 
 * @concurrency immutable
 * 
 * @see IFieldOfView
 * @see FieldAngularFace
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.2
 * 
 */
public class RectangleField implements IFieldOfView {
    
     /** Serializable UID. */
    private static final long serialVersionUID = 3709180499264196111L;

    /** 7.0 */
    private static final double C_7 = 7.0;

    /** the name of the field */
    private final String inName;

    /** the faces of the cone */
    private final FieldAngularFace[] faces;

    /** U axis */
    private final Vector3D u;

    /** V axis */
    private final Vector3D v;

    /** W axis */
    private final Vector3D w;
    
    /** Side axis. */
    private final Vector3D[] sideAxis;

    /**
     * Constructor for a field of view defined by a rectangle cone.
     * 
     * @param name
     *        the name of the field
     * @param mainDirection
     *        the main direction of the rectangle cone
     * @param approximativeU
     *        : defines the U vector of the local frame of the rectangle cone (will be corrected
     *        to be orthogonal to the main direction vector). Can't be parallel to the main direction vector.
     * @param angularApertureU
     *        the angular aperture in U direction : must be strictly between 0 and PI
     * @param angularApertureV
     *        the angular aperture in V direction : must be strictly between 0 and PI
     */
    public RectangleField(final String name, final Vector3D mainDirection, final Vector3D approximativeU,
        final double angularApertureU, final double angularApertureV) {

        // direction norm test
        if (mainDirection.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }

        // U vector test
        if (Vector3D.crossProduct(mainDirection, approximativeU).getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }

        // angle aperture test
        if (angularApertureU < Precision.DOUBLE_COMPARISON_EPSILON
            || Comparators.greaterStrict(angularApertureU, FastMath.PI)) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }
        if (Comparators.lowerOrEqual(angularApertureU, MathUtils.HALF_PI)) {
            if (angularApertureV < Precision.DOUBLE_COMPARISON_EPSILON
                || Comparators.greaterStrict(angularApertureV, MathUtils.HALF_PI)) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
            }
        } else {
            if (Comparators.lowerStrict(angularApertureV, MathUtils.HALF_PI)
                || Comparators.greaterStrict(angularApertureV, FastMath.PI)) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
            }
        }

        // Initialisations
        this.inName = name;

        // creation of the right U, V and mainDirection vectors
        this.w = mainDirection.normalize();
        final Vector3D wcomponent = new Vector3D(Vector3D.dotProduct(this.w, approximativeU), this.w);
        this.u = approximativeU.subtract(wcomponent).normalize();
        this.v = Vector3D.crossProduct(this.w, this.u);

        // rotation matrix
        final double[][] matrixData =
        { { this.u.getX(), this.u.getY(), this.u.getZ() }, { this.v.getX(), this.v.getY(), this.v.getZ() },
            { this.w.getX(), this.w.getY(), this.w.getZ() } };
        final Matrix3D rotationMatrix = new Matrix3D(matrixData);

        // creation of the side axis..
        this.sideAxis = new Vector3D[4];
        final double tanU = MathLib.tan(angularApertureU);
        final double tanV = MathLib.tan(angularApertureV);
        final double norm = MathLib.sqrt(1 + tanU * tanU + tanV * tanV);

        // ... in the right order !!
        sideAxis[0] = rotationMatrix.transposeAndMultiply(
            new Vector3D(MathLib.divide(tanU, norm), MathLib.divide(tanV, norm),
                MathLib.divide(MathLib.signum(tanU), norm)));
        sideAxis[1] = rotationMatrix.transposeAndMultiply(
            new Vector3D(MathLib.divide(-tanU, norm), MathLib.divide(tanV, norm),
                MathLib.divide(MathLib.signum(tanU), norm)));
        sideAxis[2] = rotationMatrix.transposeAndMultiply(
            new Vector3D(MathLib.divide(-tanU, norm), MathLib.divide(-tanV, norm),
                MathLib.divide(MathLib.signum(tanU), norm)));
        sideAxis[3] = rotationMatrix.transposeAndMultiply(
            new Vector3D(MathLib.divide(tanU, norm), MathLib.divide(-tanV, norm),
                MathLib.divide(MathLib.signum(tanU), norm)));

        // faces
        this.faces = new FieldAngularFace[4];
        this.faces[0] = new FieldAngularFace(sideAxis[0], sideAxis[1]);
        this.faces[1] = new FieldAngularFace(sideAxis[1], sideAxis[2]);
        this.faces[2] = new FieldAngularFace(sideAxis[2], sideAxis[3]);
        this.faces[3] = new FieldAngularFace(sideAxis[3], sideAxis[0]);
    }

    /**
     * Get the U axis
     * 
     * @return U axis
     */
    public Vector3D getU() {
        return this.u;
    }

    /**
     * Get the V axis
     * 
     * @return V axis
     */
    public Vector3D getV() {
        return this.v;
    }

    /**
     * Get the W axis. This represents the direction of the cone
     * 
     * @return W axis
     */
    public Vector3D getW() {
        return this.w;
    }

    /**
     * Get the 4 side axis of the field of view.
     * 
     * @return the 4 side axis of the field of view
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public Vector3D[] getSideAxis() {
        return this.sideAxis;
    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {

        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            return 0.0;
        } else {

            // initialisations (the first angle found will be smallest than 7.0 and replace it)
            double angle = C_7;
            double faceAngle;

            // the result is the smallest (in absolute value!) angular distance
            // found on each face, returned with the right sign (in or out of the field)
            for (int i = 0; i < 4; i++) {
                faceAngle = this.faces[i].computeMinAngle(direction);
                if (MathLib.abs(faceAngle) < MathLib.abs(angle) 
                        || (faceAngle < 0 && MathLib.abs(faceAngle) == MathLib.abs(angle))) {
                    angle = faceAngle;
                }
            }

            return angle;
        }

    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        // comparison to 0.0 : relative comparison can't be used
        return (this.getAngularDistance(direction) > 0.0);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

}
