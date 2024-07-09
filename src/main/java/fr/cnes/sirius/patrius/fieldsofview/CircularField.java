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
 * @history creation 16/04/2012
 *
 * HISTORY
* VERSION:4.6:FA:FA-2542:27/01/2021:[PATRIUS] Definition d'un champ de vue avec demi-angle de 180° 
 * VERSION:4.5.1:FA:FA-2540:04/08/2020:Evolutions et corrections dans le package fr.cnes.sirius.patrius.math.linear
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:1193:30/08/2017: Javadoc correction
 * VERSION:4.1.1:FA:1797:07/09/2018: Add getter
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.fieldsofview;

import fr.cnes.sirius.patrius.math.Comparators;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.utils.UtilsPatrius;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This class describes a right circular field of view to be used in "instruments" part properties.
 * It implements the IFieldOfView interface and so provides the associated services.
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
public final class CircularField implements IFieldOfView {

    /** Serial UID. */
    private static final long serialVersionUID = -3306669984166993698L;

    /** Name of the field. */
    private final String inName;

    /** Half-angular aperture. */
    private final double inHalfAngularAperture;

    /** Direction defining the center of the field. */
    private final Vector3D inMainDirection;

    /**
     * Constructor for a circular field of view.
     * 
     * @param name the name of the field
     * @param halfAngularAperture the half angular aperture : must be strictly between 0 and PI
     * @param mainDirection the direction defining the center of the field
     */
    public CircularField(final String name, final double halfAngularAperture,
        final Vector3D mainDirection) {

        // direction norm test
        if (mainDirection.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_ZERO_NORM);
        }

        // angle aperture test
        if (halfAngularAperture < Precision.DOUBLE_COMPARISON_EPSILON
            || Comparators.greaterStrict(halfAngularAperture, FastMath.PI)) {
            throw PatriusException
                .createIllegalArgumentException(PatriusMessages.PDB_ANGLE_OUTSIDE_INTERVAL);
        }

        // initializations
        this.inName = name;
        this.inHalfAngularAperture = halfAngularAperture;
        this.inMainDirection = new Vector3D(1.0, mainDirection);
    }

    /** {@inheritDoc} */
    @Override
    public double getAngularDistance(final Vector3D direction) {
        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            return 0.0;
        } else {
            return this.inHalfAngularAperture - Vector3D.angle(direction, this.inMainDirection);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInTheField(final Vector3D direction) {
        if (direction.getNorm() < UtilsPatrius.GEOMETRY_EPSILON) {
            // Norm too small: angle to main direction cannot be computed
            return false;
        } else {
            if (this.inHalfAngularAperture == MathLib.PI) {
                // Specific case of omni-directional field
                return true;
            } else {
                // Generic case
                return (Comparators.lowerStrict(Vector3D.angle(direction, this.inMainDirection),
                        this.inHalfAngularAperture));
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.inName;
    }

    /**
     * Returns the half-aperture.
     * 
     * @return the half-aperture
     */
    public double getHalfAngularAperture() {
        return this.inHalfAngularAperture;
    }
}
