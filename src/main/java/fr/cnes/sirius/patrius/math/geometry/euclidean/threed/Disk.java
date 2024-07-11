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
 * @history creation 12/10/11
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
 * Implements a representation of a disk.
 * <p>
 * The disk is defined by a center, a normal to the plane containing the disk, and a radius.<br>
 * This class implements the SolidShape interface.
 * </p>
 * 
 * @useSample <p>
 *            <code>
 * // normal to the disk<br>
 * final Vector3D normPlane = new Vector3D(...);<br>
 * // center of the disk<br>
 * final Vector3D centerDisk = new Vector3D(...);<br>
 * // Radius of the disk<br>
 * final double diskRadius = 6.55957;<br>
 * // We create the disk<br>
 * final Disk myDisk = new Disk(centerDisk, normPlane, diskRadius);<br>
 * final double rez = capDisk.distanceTo(something);
 * </code>
 *            </p>
 * 
 * @concurrency immutable
 * 
 * @see SolidShape
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: Disk.java 17583 2017-05-10 13:05:10Z bignon $
 * 
 * @since 1.0
 * 
 */
@SuppressWarnings("PMD.ShortClassName")
public final class Disk extends AbstractEllipse implements Serializable {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 9040808229741425729L;

    /**
     * Constructs the disk.
     * 
     * @param center
     *        the vector for the center
     * @param normal
     *        the vector for the normal to the disk's plane
     * @param radius
     *        the radius of the disk - has to be positive
     */
    public Disk(final Vector3D center, final Vector3D normal, final double radius) {
        super(center, normal, normal.orthogonal(), radius, radius);
    }

    /**
     * Get a string representation for this disk.
     * The given parameters are in the same order as
     * in the constructor.
     * 
     * @return a string representation for this disk
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
        // "radius":
        res.append("radius").append(open).append(this.getRadiusA()).append(close);
        res.append(close);

        return res.toString();
    }

}
