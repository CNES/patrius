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
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au 
 *          lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC) 
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:273:20/10/2013:Minor code problems
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyPoint.BodyPointName;
import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Simple exponential atmospheric model.
 * <p>
 * This model represents a simple atmosphere with an exponential density and rigidly bound to the underlying rotating
 * body.
 * </p>
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
 * @author Fabien Maussion
 * @author Luc Maisonobe
 */
public class SimpleExponentialAtmosphere implements Atmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = 2772347498196369601L;

    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;

    /** Earth gravitation constant. */
    private static final double G0 = 9.81;

    /** Earth shape model. */
    private final EllipsoidBodyShape shape;

    /** Reference density. */
    private final double rho0;

    /** Reference altitude. */
    private final double h0;

    /** Reference altitude scale. */
    private final double hscale;

    /**
     * Create an exponential atmosphere.
     *
     * @param shapeIn
     *        body shape model
     * @param rho0In
     *        Density at the altitude h0
     * @param h0In
     *        Altitude of reference (m)
     * @param hscaleIn
     *        Scale factor (m)
     */
    public SimpleExponentialAtmosphere(final EllipsoidBodyShape shapeIn, final double rho0In, final double h0In,
                                       final double hscaleIn) {
        this.shape = shapeIn;
        this.rho0 = rho0In;
        this.h0 = h0In;
        this.hscale = hscaleIn;
    }

    /** {@inheritDoc} */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        final EllipsoidPoint gp = this.shape.buildPoint(position, frame, date, BodyPointName.DEFAULT);
        final double alt = gp.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();
        return this.rho0 * MathLib.exp((this.h0 - alt) / this.hscale);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        final Transform bodyToFrame = this.shape.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame) {
        return MathLib.sqrt(GAMMA * this.hscale * G0);
    }

    /**
     * Getter for the Body shape model.
     *
     * @return the Body shape model
     */
    public BodyShape getShape() {
        return this.shape;
    }

    /**
     * Getter for the Density at the altitude h0.
     *
     * @return the Density at the altitude h0
     */
    public double getRho0() {
        return this.rho0;
    }

    /**
     * Getter for the Altitude of reference (m).
     *
     * @return the Altitude of reference (m)
     */
    public double getH0() {
        return this.h0;
    }

    /**
     * Getter for the Scale factor (m).
     *
     * @return the Scale factor (m)
     */
    public double getHscale() {
        return this.hscale;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>shape: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        return new SimpleExponentialAtmosphere(this.shape, this.rho0, this.h0, this.hscale);
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) {
        // Nothing to do
    }
}
