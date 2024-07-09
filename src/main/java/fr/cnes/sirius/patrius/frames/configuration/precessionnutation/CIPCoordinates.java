/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
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
 * @history creation 11/10/2012
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.io.Serializable;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeStamped;

/**
 * This class represents a Celestial Intermediate Pole. It contains a date and the CIP coordinates at that date.
 * 
 * @author Rami Houdroge
 * 
 * @version $Id: CIPCoordinates.java 18073 2017-10-02 16:48:07Z bignon $
 */
public final class CIPCoordinates implements TimeStamped, Serializable {

    /** Serial UID. */
    private static final long serialVersionUID = -5771171463186446472L;

    /** Date of entry. */
    private final AbsoluteDate date;

    /** X coordinate of current entry. */
    private final double x;

    /** X' coordinate of current entry. */
    private final double xP;

    /** Y coordinate of current entry. */
    private final double y;

    /** Y' coordinate of current entry. */
    private final double yP;

    /** S coordinate of current entry. */
    private final double s;

    /** S' coordinate of current entry. */
    private final double sP;

    /**
     * @param dateIn
     *        date
     * @param xIn
     *        x-coordinate of pole
     * @param xPIn
     *        x-coordinate derivative of pole
     * @param yIn
     *        y-coordinate of pole
     * @param yPIn
     *        y-coordinate derivative of pole
     * @param sIn
     *        s-coordinate of pole
     * @param sPIn
     *        s-coordinate derivative of pole
     */
    public CIPCoordinates(final AbsoluteDate dateIn, final double xIn, final double xPIn,
        final double yIn, final double yPIn, final double sIn, final double sPIn) {
        this.date = dateIn;
        this.x = xIn;
        this.xP = xPIn;
        this.y = yIn;
        this.yP = yPIn;
        this.s = sIn;
        this.sP = sPIn;
    }

    /**
     * @param dateIn
     *        date
     * @param cip
     *        coordinates of pole
     * @param cipDV
     *        coordinate derivatives of pole
     */
    public CIPCoordinates(final AbsoluteDate dateIn, final double[] cip, final double[] cipDV) {
        this.date = dateIn;
        this.x = cip[0];
        this.y = cip[1];
        this.s = cip[2];
        this.xP = cipDV[0];
        this.yP = cipDV[1];
        this.sP = cipDV[2];
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get CIP motion.
     * 
     * @return CIP motion as an array
     */
    public double[] getCIPMotion() {
        return new double[] { this.x, this.y, this.s };
    }

    /**
     * Get CIP motion time derivatives.
     * 
     * @return CIP motion time derivatives as an array
     */
    public double[] getCIPMotionTimeDerivatives() {
        return new double[] { this.xP, this.yP, this.sP };
    }

    /**
     * @return the x-coordinate
     */
    public double getX() {
        return this.x;
    }

    /**
     * @return the xP-coordinate derivative
     */
    public double getxP() {
        return this.xP;
    }

    /**
     * @return the y-coordinate
     */
    public double getY() {
        return this.y;
    }

    /**
     * @return the yP-coordinate derivative
     */
    public double getyP() {
        return this.yP;
    }

    /**
     * @return the s-coordinate
     */
    public double getS() {
        return this.s;
    }

    /**
     * @return the sP-coordinate derivative
     */
    public double getsP() {
        return this.sP;
    }

}
