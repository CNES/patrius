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
 *
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.13:DM:DM-5:08/12/2023:[PATRIUS] Orientation d'un corps celeste sous forme de quaternions
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

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

    /** Zero CIP coordinates. */
    public static final CIPCoordinates ZERO = new CIPCoordinates(AbsoluteDate.J2000_EPOCH, 0., 0., 0., 0., 0., 0.);

     /** Serializable UID. */
    private static final long serialVersionUID = -5771171463186446472L;

    /** Date of entry. */
    private final AbsoluteDate date;

    /** X-coordinate of pole. */
    private final double x;

    /** X-coordinate derivative of pole. */
    private final double xP;

    /** Y-coordinate of pole. */
    private final double y;

    /** Y-coordinate derivative of pole. */
    private final double yP;

    /** S-coordinate of pole. */
    private final double s;

    /** S-coordinate derivative of pole. */
    private final double sP;

    /**
     * Constructor for the CIP pole coordinates.
     * 
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
     * Constructor for the CIP pole coordinates.
     * 
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
     * Indicate if the coordinates of pole are zero.
     *
     *@return true if the coordinates of pole are zero
     */
    public boolean isCIPMotionZero() {
        return this.x == 0. && this.y == 0. && this.s == 0.;
    }

    /**
     * Indicate if the coordinates derivatives of pole are zero.
     *
     *@return true if the coordinates derivatives of pole are zero
     */
    public boolean isCIPMotionTimeDerivativesZero() {
        return this.xP == 0 && this.yP == 0 && this.sP == 0;
    }

    /**
     * Get for the CIP motion.
     *
     * @return the CIP motion as an array
     */
    public double[] getCIPMotion() {
        return new double[] { this.x, this.y, this.s };
    }

    /**
     * Getter for the CIP motion time derivatives.
     *
     * @return the CIP motion time derivatives as an array
     */
    public double[] getCIPMotionTimeDerivatives() {
        return new double[] { this.xP, this.yP, this.sP };
    }

    /**
     * Getter for the x-coordinate of pole.
     * 
     * @return the x-coordinate of pole
     */
    public double getX() {
        return this.x;
    }

    /**
     * Getter for the x-coordinate derivative of pole.
     * 
     * @return the x-coordinate derivative of pole
     */
    public double getxP() {
        return this.xP;
    }

    /**
     * Getter for the y-coordinate of pole.
     * 
     * @return the y-coordinate of pole
     */
    public double getY() {
        return this.y;
    }

    /**
     * Getter for the y-coordinate derivative of pole.
     * 
     * @return the y-coordinate derivative of pole
     */
    public double getyP() {
        return this.yP;
    }

    /**
     * Getter for the s-coordinate of pole.
     * 
     * @return the s-coordinate of pole
     */
    public double getS() {
        return this.s;
    }

    /**
     * Getter for the s-coordinate derivative of pole.
     * 
     * @return the s-coordinate derivative of pole
     */
    public double getsP() {
        return this.sP;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(final Object object) {
        boolean isEqual = false;

        if (object == this) {
            // Identity
            isEqual = true;
        } else if ((object != null) && (object.getClass() == this.getClass())) {
            // Same object type: check all attributes
            final CIPCoordinates other = (CIPCoordinates) object;

            // Evaluate the attitudes components
            isEqual = Objects.equals(this.date, other.date)
                    && Double.doubleToLongBits(this.x) == Double.doubleToLongBits(other.x)
                    && Double.doubleToLongBits(this.xP) == Double.doubleToLongBits(other.xP)
                    && Double.doubleToLongBits(this.y) == Double.doubleToLongBits(other.y)
                    && Double.doubleToLongBits(this.yP) == Double.doubleToLongBits(other.yP)
                    && Double.doubleToLongBits(this.s) == Double.doubleToLongBits(other.s)
                    && Double.doubleToLongBits(this.sP) == Double.doubleToLongBits(other.sP);
        }

        return isEqual;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return Objects.hash(this.date, this.x, this.xP, this.y, this.yP, this.s, this.sP);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.format(Locale.US, "CIPCoordinates{%s: x=%f, y=%f, s=%f, x'=%f, y'=%f, s'=%f}",
            this.date.toString(), this.x, this.y, this.s, this.xP, this.yP, this.sP);
    }
}
