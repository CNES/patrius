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
 * @history creation 19/10/2011
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.geometry.euclidean.threed;

import java.io.Serializable;

/**
 * <p>
 * This is a describing class for an ellipse in 3D space, with some algorithm to compute intersections and distances to
 * some other objects.
 * </p>
 * 
 * @useSample <p>
 *            Creation with two radiuses, and two Vector3D : Vector3D center = new Vector3D(1.0, 6.0, -2.0); Vector3D
 *            normal = new Vector3D(6.0, -3.0, -1.0); double radiusA = 2.0; double radiusB = 5.0; Ellipse ellipse = new
 *            Ellipse(center, normal, radiusA, radiusB); Intersection with a line : boolean intersects = ellipse(line);
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see Shape#distanceTo(Line)
 * @see Shape#intersects(Line)
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: Ellipse.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
public final class Ellipse extends AbstractEllipse implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 9047708229741235629L;

    /**
     * Build an ellipse in the 3D space from its center, normal vector, approximative U vector of the local frame, and
     * two radiuses.
     * 
     * @param inCenter
     *        position of the center
     * @param inNormal
     *        normal to the plane containing the ellipse
     * @param inUvector
     *        approximative U vector of the local frame : corrected to be orthogonal to the normal
     * @param inRadiusA
     *        radius on the U axis of the local frame
     * @param inRadiusB
     *        radius on the V axis of the local frame
     * @throws IllegalArgumentException
     *         if one radius is'nt strictly positive, if the normal or the uVector has a not strictly positive norm,
     *         or if they are parallel.
     */
    public Ellipse(final Vector3D inCenter, final Vector3D inNormal, final Vector3D inUvector, final double inRadiusA,
        final double inRadiusB) {
        super(inCenter, inNormal, inUvector, inRadiusA, inRadiusB);
    }

    /**
     * Get a string representation for this ellipse.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a string representation for this ellipse
     */
    @Override
    public String toString() {
        final StringBuilder res = new StringBuilder();
        final String fullClassName = this.getClass().getName();
        final String shortClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String open = "{";
        final String close = "}";
        final String comma = ",";
        res.append(shortClassName).append(open);
        // "center":
        res.append("center").append(this.getCenter().toString()).append(comma);
        // "normal":
        res.append("normal").append(this.getNormal().toString()).append(comma);
        // "radius A":
        res.append("radius A").append(open);
        res.append(this.getRadiusA()).append(close).append(comma);
        // "radius B":
        res.append("radius B").append(open);
        res.append(this.getRadiusB()).append(close).append(close);

        return res.toString();
    }

}
