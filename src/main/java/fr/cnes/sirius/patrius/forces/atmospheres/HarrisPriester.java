/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */

/*
 *
 * HISTORY
* VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * This atmosphere model is the realization of the Modified Harris-Priester model.
 * <p>
 * This model is a static one that takes into account the diurnal density bulge. It doesn't need any space weather data
 * but a density vs. altitude table, which depends on solar activity.
 * </p>
 * <p>
 * The implementation relies on the book:<br>
 * <b>Satellite Orbits</b><br>
 * <i>Oliver Montenbruck, Eberhard Gill</i><br>
 * Springer 2005
 * </p>
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
 * @author Pascal Parraud
 */
public class HarrisPriester implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = 2772347498196369601L;

    /** 0.5. */
    private static final double HALF = 0.5;

    // Constants :

    /** Lag angle in longitude. */
    private static final double LAG = MathLib.toRadians(30.0);

    /**
     * Harris-Priester min-max density (kg/m3) vs. altitude (m) table. These data are valid for a mean solar activity.
     */
    private static final double[][] ALT_RHO = {
        { 100000.0, 4.974e-07, 4.974e-07 },
        { 120000.0, 2.490e-08, 2.490e-08 },
        { 130000.0, 8.377e-09, 8.710e-09 },
        { 140000.0, 3.899e-09, 4.059e-09 },
        { 150000.0, 2.122e-09, 2.215e-09 },
        { 160000.0, 1.263e-09, 1.344e-09 },
        { 170000.0, 8.008e-10, 8.758e-10 },
        { 180000.0, 5.283e-10, 6.010e-10 },
        { 190000.0, 3.617e-10, 4.297e-10 },
        { 200000.0, 2.557e-10, 3.162e-10 },
        { 210000.0, 1.839e-10, 2.396e-10 },
        { 220000.0, 1.341e-10, 1.853e-10 },
        { 230000.0, 9.949e-11, 1.455e-10 },
        { 240000.0, 7.488e-11, 1.157e-10 },
        { 250000.0, 5.709e-11, 9.308e-11 },
        { 260000.0, 4.403e-11, 7.555e-11 },
        { 270000.0, 3.430e-11, 6.182e-11 },
        { 280000.0, 2.697e-11, 5.095e-11 },
        { 290000.0, 2.139e-11, 4.226e-11 },
        { 300000.0, 1.708e-11, 3.526e-11 },
        { 320000.0, 1.099e-11, 2.511e-11 },
        { 340000.0, 7.214e-12, 1.819e-11 },
        { 360000.0, 4.824e-12, 1.337e-11 },
        { 380000.0, 3.274e-12, 9.955e-12 },
        { 400000.0, 2.249e-12, 7.492e-12 },
        { 420000.0, 1.558e-12, 5.684e-12 },
        { 440000.0, 1.091e-12, 4.355e-12 },
        { 460000.0, 7.701e-13, 3.362e-12 },
        { 480000.0, 5.474e-13, 2.612e-12 },
        { 500000.0, 3.916e-13, 2.042e-12 },
        { 520000.0, 2.819e-13, 1.605e-12 },
        { 540000.0, 2.042e-13, 1.267e-12 },
        { 560000.0, 1.488e-13, 1.005e-12 },
        { 580000.0, 1.092e-13, 7.997e-13 },
        { 600000.0, 8.070e-14, 6.390e-13 },
        { 620000.0, 6.012e-14, 5.123e-13 },
        { 640000.0, 4.519e-14, 4.121e-13 },
        { 660000.0, 3.430e-14, 3.325e-13 },
        { 680000.0, 2.632e-14, 2.691e-13 },
        { 700000.0, 2.043e-14, 2.185e-13 },
        { 720000.0, 1.607e-14, 1.779e-13 },
        { 740000.0, 1.281e-14, 1.452e-13 },
        { 760000.0, 1.036e-14, 1.190e-13 },
        { 780000.0, 8.496e-15, 9.776e-14 },
        { 800000.0, 7.069e-15, 8.059e-14 },
        { 840000.0, 4.680e-15, 5.741e-14 },
        { 880000.0, 3.200e-15, 4.210e-14 },
        { 920000.0, 2.210e-15, 3.130e-14 },
        { 960000.0, 1.560e-15, 2.360e-14 },
        { 1000000.0, 1.150e-15, 1.810e-14 }
    };

    /** Cosine exponent from 2 to 6 according to inclination. */
    private double n;

    /** Sun position. */
    private final PVCoordinatesProvider sun;

    /** Earth body shape. */
    private final EllipsoidBodyShape earth;

    /** Density table. */
    private double[][] tabAltRho;

    /**
     * Simple constructor for Modified Harris-Priester atmosphere model.
     * <p>
     * The cosine exponent value is set to 4 by default.
     * </p>
     * <p>
     * The default embedded density table is the one given in the referenced book from Montenbruck & Gill. It is given
     * for mean solar activity and spreads over 100 to 1000 km.
     * </p>
     *
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     */
    public HarrisPriester(final PVCoordinatesProvider sunIn, final EllipsoidBodyShape earthIn) {
        this.sun = sunIn;
        this.earth = earthIn;
        this.tabAltRho = ALT_RHO;
        setN(4);
    }

    /**
     * Constructor for Modified Harris-Priester atmosphere model.
     * <p>
     * Recommanded values for the cosine exponent spread over the range 2, for low inclination orbits, to 6, for polar
     * orbits.
     * </p>
     * <p>
     * The default embedded density table is the one given in the referenced book from Montenbruck & Gill. It is given
     * for mean solar activity and spreads over 100 to 1000 km.
     * </p>
     *
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @param nIn
     *        the cosine exponent
     */
    public HarrisPriester(final PVCoordinatesProvider sunIn, final EllipsoidBodyShape earthIn, final double nIn) {
        this.sun = sunIn;
        this.earth = earthIn;
        this.tabAltRho = ALT_RHO;
        setN(nIn);
    }

    /**
     * Constructor for Modified Harris-Priester atmosphere model.
     * <p>
     * The provided density table must be an array such as:
     * <ul>
     * <li>tabAltRho[][0] = altitude (m)</li>
     * <li>tabAltRho[][1] = min density (kg/m<sup>3</sup>)</li>
     * <li>tabAltRho[][2] = max density (kg/m<sup>3</sup>)</li>
     * </ul>
     * The altitude must be increasing without limitation in range.<br>
     * The internal density table is a copy of the provided one.
     * </p>
     * <p>
     * The cosine exponent value is set to 4 by default.
     * </p>
     *
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @param tabAltRhoIn
     *        the density table
     */
    public HarrisPriester(final PVCoordinatesProvider sunIn, final EllipsoidBodyShape earthIn,
                          final double[][] tabAltRhoIn) {
        this.sun = sunIn;
        this.earth = earthIn;
        setN(4);
        setTabDensity(tabAltRhoIn);
    }

    /**
     * Constructor for Modified Harris-Priester atmosphere model.
     * <p>
     * Recommended values for the cosine exponent spread over the range 2, for low inclination orbits, to 6, for polar
     * orbits.
     * </p>
     * <p>
     * The provided density table must be an array such as:
     * <ul>
     * <li>tabAltRho[][0] = altitude (m)</li>
     * <li>tabAltRho[][1] = min density (kg/m<sup>3</sup>)</li>
     * <li>tabAltRho[][2] = max density (kg/m<sup>3</sup>)</li>
     * </ul>
     * The altitude must be increasing without limitation in range.<br>
     * The internal density table is a copy of the provided one.
     * </p>
     *
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @param tabAltRhoIn
     *        the density table
     * @param nIn
     *        the cosine exponent
     */
    public HarrisPriester(final PVCoordinatesProvider sunIn, final EllipsoidBodyShape earthIn,
                          final double[][] tabAltRhoIn, final double nIn) {
        this.sun = sunIn;
        this.earth = earthIn;
        setN(nIn);
        setTabDensity(tabAltRhoIn);
    }

    /**
     * Setter for the parameter N, the cosine exponent.
     *
     * @param nIn
     *        the cosine exponent
     */
    private void setN(final double nIn) {
        this.n = nIn;
    }

    /**
     * Setter for the user defined density table to deal with different solar activities.
     *
     * @param tab
     *        density vs. altitude table
     */
    private void setTabDensity(final double[][] tab) {
        this.tabAltRho = new double[tab.length][];
        for (int i = 0; i < tab.length; i++) {
            this.tabAltRho[i] = tab[i].clone();
        }
    }

    /**
     * Getter for the current density table.
     * <p>
     * The density table is an array such as:
     * <ul>
     * <li>tabAltRho[][0] = altitude (m)</li>
     * <li>tabAltRho[][1] = min density (kg/m<sup>3</sup>)</li>
     * <li>tabAltRho[][2] = max density (kg/m<sup>3</sup>)</li>
     * </ul>
     * The altitude must be increasing without limitation in range.
     * </p>
     * <p>
     * The returned density table is a copy of the current one.
     * </p>
     *
     * @return density vs. altitude table
     */
    public double[][] getTabDensity() {
        final double[][] copy = new double[this.tabAltRho.length][];
        for (int i = 0; i < this.tabAltRho.length; i++) {
            copy[i] = this.tabAltRho[i].clone();
        }
        return copy;
    }

    /**
     * Getter for the minimal altitude for the model.
     * <p>
     * No computation is possible below this altitude.
     * </p>
     *
     * @return the minimal altitude (m)
     */
    public double getMinAlt() {
        return this.tabAltRho[0][0];
    }

    /**
     * Getter for the maximal altitude for the model.
     * <p>
     * Above this altitude, density is assumed to be zero.
     * </p>
     *
     * @return the maximal altitude (m)
     */
    public double getMaxAlt() {
        return this.tabAltRho[this.tabAltRho.length - 1][0];
    }

    /**
     * Getter for the local density.
     *
     * @param sunRAsc
     *        Right Ascension of Sun (radians)
     * @param sunDecl
     *        Declination of Sun (radians)
     * @param satPos
     *        position of s/c in earth frame(m)
     * @param satAlt
     *        height of s/c (m)
     * @return the local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *            if altitude is below the model minimal altitude
     */
    public double getDensity(final double sunRAsc, final double sunDecl, final Vector3D satPos, final double satAlt)
        throws PatriusException {
        // Check for height boundaries
        if (satAlt < getMinAlt()) {
            throw new PatriusException(PatriusMessages.ALTITUDE_BELOW_ALLOWED_THRESHOLD, satAlt, getMinAlt());
        }
        if (satAlt > getMaxAlt()) {
            return 0.;
        }

        // Diurnal bulge apex direction
        final double[] sincosDec = MathLib.sinAndCos(sunDecl);
        final double sinDec = sincosDec[0];
        final double cosDec = sincosDec[1];
        final double[] sincosAsc = MathLib.sinAndCos(sunRAsc + LAG);
        final double sinAsc = sincosAsc[0];
        final double cosAsc = sincosAsc[1];
        final Vector3D dDBA = new Vector3D(cosDec * cosAsc, cosDec * sinAsc, sinDec);

        // Cosine of angle Psi between the diurnal bulge apex and the satellite
        final double cosPsi = dDBA.normalize().dotProduct(satPos.normalize());
        // (1 + cos(Psi))/2 = cos2(Psi/2)
        final double c2Psi2 = HALF + HALF * cosPsi;

        // Search altitude index in density table
        int ia = 0;
        while (ia < this.tabAltRho.length - 2 && satAlt > this.tabAltRho[ia][0]) {
            ia++;
        }

        // Exponential density interpolation
        final double altMin = (this.tabAltRho[ia][0] - this.tabAltRho[ia + 1][0])
                / MathLib.log(this.tabAltRho[ia + 1][1] / this.tabAltRho[ia][1]);
        final double altMax = (this.tabAltRho[ia][0] - this.tabAltRho[ia + 1][0])
                / MathLib.log(this.tabAltRho[ia + 1][2] / this.tabAltRho[ia][2]);

        final double rhoMin = this.tabAltRho[ia][1] * MathLib.exp((this.tabAltRho[ia][0] - satAlt) / altMin);
        final double rhoMax = this.tabAltRho[ia][2] * MathLib.exp((this.tabAltRho[ia][0] - satAlt) / altMax);

        return rhoMin + (rhoMax - rhoMin) * MathLib.pow(c2Psi2, this.n / 2);
    }

    /**
     * Getter for the local density.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return local density (kg/m<sup>3</sup>)
     * @throws PatriusException
     *            if some frame conversion cannot be performed or if altitude is below the model minimal altitude
     */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        // compute sun geodetic position
        final EllipsoidPoint sunInBody = this.earth.buildPoint(this.sun.getPVCoordinates(date, frame).getPosition(),
            frame, date, "sunPoint");
        final double sunRAAN = sunInBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
        final double sunDecl = sunInBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();

        // compute s/c position in earth frame
        final EllipsoidPoint satInBody = this.earth.buildPoint(position, frame, date, "satPoint");
        final double satAlt = satInBody.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();
        final Vector3D posInBody = satInBody.getPosition();

        return this.getDensity(sunRAAN, sunDecl, posInBody, satAlt);
    }

    /**
     * Getter for the inertial velocity of atmosphere molecules.
     * <p>
     * Here the case is simplified : atmosphere is supposed to have a null velocity in earth frame.
     * </p>
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @return velocity (m/s) (defined in the same frame as the position)
     * @throws PatriusException
     *            if some frame conversion cannot be performed
     */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        throw new PatriusException(PatriusMessages.UNEXPECTED_ATMOSPHERE_MODEL);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        final double[][] tabAltRhoNew = new double[this.tabAltRho.length][];
        for (int i = 0; i < this.tabAltRho.length; i++) {
            tabAltRhoNew[i] = this.tabAltRho[i].clone();
        }
        return new HarrisPriester(this.sun, this.earth, tabAltRhoNew, this.n);
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
        // Nothing to do
    }
}
