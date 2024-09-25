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
 * VERSION:4.13.1:FA:FA-176:17/01/2024:[PATRIUS] Reliquat OPENFD
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3129:10/05/2022:[PATRIUS] Commentaires TODO ou FIXME 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2499:27/01/2021:[PATRIUS] Anomalie dans la gestion des panneaux solaires de la classe Vehicle 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:93:31/03/2014:changed API for partial derivatives
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:534:10/02/2016:Parametrization of force models
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.forces.drag.DragSensitive;
import fr.cnes.sirius.patrius.forces.radiation.RadiationSensitive;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * experimental class representing the features of a classical satellite
 * with a convex body shape and rotating flat solar arrays.
 * <p>
 * As of 5.0, this class is still considered experimental, so use it with care.
 * </p>
 * <p>
 * The body can be either a simple parallelepipedic box aligned with spacecraft axes or a set of facets defined by their
 * area and normal vector. This should handle accurately most spacecraft shapes.
 * </p>
 * <p>
 * The solar array rotation with respect to satellite body can be either the best lighting orientation (i.e. Sun exactly
 * in solar array meridian plane defined by solar array rotation axis and solar array normal vector) or a rotation
 * evolving linearly according to a start position and an angular rate (which can be set to 0 for non-rotating panels,
 * which may occur in special modes or during contingencies).
 * </p>
 * <p>
 * This model does not take cast shadow between body and solar array into account.
 * </p>
 * <p>
 * Instances of this class are guaranteed to be immutable.
 * </p>
 * 
 * @see SphericalSpacecraft
 * @author Luc Maisonobe
 * @author Pascal Parraud
 */
public class BoxAndSolarArraySpacecraft implements RadiationSensitive, DragSensitive {

    /** Serializable UID. */
    private static final long serialVersionUID = -4426844682371384944L;

    /** Surface vectors for body facets. */
    private final List<Facet> facets;

    /** Solar array area (m<sup>2</sup>). */
    private final double solarArrayArea;

    /** Reference date for linear rotation angle (may be null). */
    private final AbsoluteDate referenceDate;

    /** Rotation rate for linear rotation angle. */
    private final double rotationRate;

    /** Solar array reference axis in spacecraft frame (may be null). */
    private final Vector3D saX;

    /** Solar array third axis in spacecraft frame (may be null). */
    private final Vector3D saY;

    /** Solar array rotation axis in spacecraft frame. */
    private final Vector3D saZ;

    /** Drag coefficient. */
    private double dragCoeff;

    /** Absorption coefficient. */
    private double absorptionCoeff;

    /** Specular reflection coefficient. */
    private double specularReflectionCoeff;

    /** Diffuse reflection coefficient. */
    private double diffuseReflectionCoeff;

    /** Sun model. */
    private final PVCoordinatesProvider sun;

    /** Part name. */
    private final String partName;

    /**
     * Build a spacecraft model with best lighting of solar array.
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction will always be in the solar array
     * meridian plane defined by solar array rotation axis and solar array normal vector.
     * </p>
     * 
     * @param xLength
     *        length of the body along its X axis (m)
     * @param yLength
     *        length of the body along its Y axis (m)
     * @param zLength
     *        length of the body along its Z axis (m)
     * @param sun
     *        sun model
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @param dragCoeff
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param specularReflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseReflectionCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure
     * @param partName
     *        name of the spacecraft part concerned
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
        final double zLength,
        final PVCoordinatesProvider sun, final double solarArrayArea,
        final Vector3D solarArrayAxis,
        final double dragCoeff,
        final double absorptionCoeff,
        final double specularReflectionCoeff,
        final double diffuseReflectionCoeff,
        final String partName) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
            dragCoeff, absorptionCoeff, specularReflectionCoeff, diffuseReflectionCoeff, partName);
    }

    /**
     * Build a spacecraft model with best lighting of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of the body is describe by a vector
     * normal to the facet (pointing outward of the spacecraft) and whose norm is the surface area in m<sup>2</sup>.
     * </p>
     * <p>
     * Solar arrays orientation will be such that at each time the Sun direction will always be in the solar array
     * meridian plane defined by solar array rotation axis and solar array normal vector.
     * </p>
     * 
     * @param facets
     *        body facets (only the facets with strictly positive area will be stored)
     * @param sun
     *        sun model
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @param dragCoeff
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param specularReflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseReflectionCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure
     * @param partName
     *        name of the spacecraft part concerned
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
        final PVCoordinatesProvider sun, final double solarArrayArea,
        final Vector3D solarArrayAxis,
        final double dragCoeff,
        final double absorptionCoeff,
        final double specularReflectionCoeff,
        final double diffuseReflectionCoeff,
        final String partName) {

        this.facets = filter(facets);

        this.sun = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate = null;
        this.rotationRate = 0;

        this.saZ = solarArrayAxis.normalize();
        this.saY = null;
        this.saX = null;

        this.dragCoeff = dragCoeff;
        this.absorptionCoeff = absorptionCoeff;
        this.specularReflectionCoeff = specularReflectionCoeff;
        this.diffuseReflectionCoeff = diffuseReflectionCoeff;
        this.partName = partName;
    }

    /**
     * Build a spacecraft model with linear rotation of solar array.
     * <p>
     * Solar arrays orientation will be a regular rotation from the reference orientation at reference date and using a
     * constant rotation rate.
     * </p>
     * 
     * @param xLength
     *        length of the body along its X axis (m)
     * @param yLength
     *        length of the body along its Y axis (m)
     * @param zLength
     *        length of the body along its Z axis (m)
     * @param sun
     *        sun model
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @param referenceDate
     *        reference date for the solar array rotation
     * @param referenceNormal
     *        direction of the solar array normal at reference date
     *        in spacecraft frame
     * @param rotationRate
     *        rotation rate of the solar array, may be 0 (rad/s)
     * @param dragCoeff
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param reflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseReflectionCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure
     * @param partName
     *        name of the spacecraft part concerned
     */
    public BoxAndSolarArraySpacecraft(final double xLength, final double yLength,
        final double zLength,
        final PVCoordinatesProvider sun, final double solarArrayArea,
        final Vector3D solarArrayAxis,
        final AbsoluteDate referenceDate,
        final Vector3D referenceNormal,
        final double rotationRate,
        final double dragCoeff,
        final double absorptionCoeff,
        final double reflectionCoeff,
        final double diffuseReflectionCoeff,
        final String partName) {
        this(simpleBoxFacets(xLength, yLength, zLength), sun, solarArrayArea, solarArrayAxis,
            referenceDate, referenceNormal, rotationRate,
            dragCoeff, absorptionCoeff, reflectionCoeff, diffuseReflectionCoeff, partName);
    }

    /**
     * Build a spacecraft model with linear rotation of solar array.
     * <p>
     * The spacecraft body is described by an array of surface vectors. Each facet of the body is describe by a vector
     * normal to the facet (pointing outward of the spacecraft) and whose norm is the surface area in m<sup>2</sup>.
     * </p>
     * <p>
     * Solar arrays orientation will be a regular rotation from the reference orientation at reference date and using a
     * constant rotation rate.
     * </p>
     * 
     * @param facets
     *        body facets (only the facets with strictly positive area will be stored)
     * @param sun
     *        sun model
     * @param solarArrayArea
     *        area of the solar array (m<sup>2</sup>)
     * @param solarArrayAxis
     *        solar array rotation axis in satellite frame
     * @param referenceDate
     *        reference date for the solar array rotation
     * @param referenceNormal
     *        direction of the solar array normal at reference date
     *        in spacecraft frame
     * @param rotationRate
     *        rotation rate of the solar array, may be 0 (rad/s)
     * @param dragCoeff
     *        drag coefficient (used only for drag)
     * @param absorptionCoeff
     *        absorption coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param reflectionCoeff
     *        specular reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure)
     * @param diffuseReflectionCoeff
     *        diffuse reflection coefficient between 0.0 an 1.0
     *        (used only for radiation pressure
     * @param partName
     *        name of the spacecraft part concerned
     */
    public BoxAndSolarArraySpacecraft(final Facet[] facets,
        final PVCoordinatesProvider sun, final double solarArrayArea,
        final Vector3D solarArrayAxis,
        final AbsoluteDate referenceDate,
        final Vector3D referenceNormal,
        final double rotationRate,
        final double dragCoeff,
        final double absorptionCoeff,
        final double reflectionCoeff,
        final double diffuseReflectionCoeff,
        final String partName) {

        this.facets = filter(facets.clone());

        this.sun = sun;
        this.solarArrayArea = solarArrayArea;
        this.referenceDate = referenceDate;
        this.rotationRate = rotationRate;

        this.saZ = solarArrayAxis.normalize();
        this.saY = Vector3D.crossProduct(this.saZ, referenceNormal).normalize();
        this.saX = Vector3D.crossProduct(this.saY, this.saZ);

        this.dragCoeff = dragCoeff;
        this.absorptionCoeff = absorptionCoeff;
        this.specularReflectionCoeff = reflectionCoeff;
        this.diffuseReflectionCoeff = diffuseReflectionCoeff;
        this.partName = partName;

    }

    /**
     * Get solar array normal in spacecraft frame.
     * 
     * @param state
     *        current state information: date, kinematics, attitude
     * @return solar array normal in spacecraft frame
     * @exception PatriusException
     *            if sun direction cannot be computed in best lighting
     *            configuration
     */
    public synchronized Vector3D getNormal(final SpacecraftState state)
                                                                       throws PatriusException {

        final AbsoluteDate date = state.getDate();

        if (this.referenceDate != null) {
            // use a simple rotation at fixed rate
            final double alpha = this.rotationRate * date.durationFrom(this.referenceDate);
            return new Vector3D(MathLib.cos(alpha), this.saX, MathLib.sin(alpha), this.saY);
        }

        // compute orientation for best lighting
        final Frame frame = state.getFrame();
        final Vector3D sunInert = this.sun.getPVCoordinates(date, frame).getPosition().normalize();
        final Vector3D sunSpacecraft = state.getAttitude().getRotation().applyInverseTo(sunInert);
        final double d = Vector3D.dotProduct(sunSpacecraft, this.saZ);
        final double f = 1 - d * d;
        if (f < Precision.EPSILON) {
            // extremely rare case: the sun is along solar array rotation axis
            // (there will not be much output power ...)
            // we set up an arbitrary normal
            return this.saZ.orthogonal();
        }

        final double s = 1.0 / MathLib.sqrt(f);
        return new Vector3D(s, sunSpacecraft, -s * d, this.saZ);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D dragAcceleration(final SpacecraftState state, final double density,
                                     final Vector3D relativeVelocity)
                                                                     throws PatriusException {

        // relative velocity in spacecraft frame
        final Vector3D v = state.getAttitude().getRotation().applyInverseTo(relativeVelocity);

        // solar array contribution
        final Vector3D solarArrayFacet = new Vector3D(this.solarArrayArea, this.getNormal(state));
        double sv = MathLib.abs(Vector3D.dotProduct(solarArrayFacet, v));

        // body facets contribution
        for (final Facet facet : this.facets) {
            final double dot = Vector3D.dotProduct(facet.getNormal(), v);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                sv -= facet.getArea() * dot;
            }
        }

        return new Vector3D(density * sv * this.dragCoeff / (2.0 * state.getMass(this.partName)), relativeVelocity);

    }

    /** {@inheritDoc} */
    @Override
    public Vector3D
            radiationPressureAcceleration(final SpacecraftState state, final Vector3D flux)
                                                                                           throws PatriusException {

        // radiation flux in spacecraft frame
        final Rotation r = state.getAttitude().getRotation();
        final Vector3D fluxSat = r.applyInverseTo(flux);

        // solar array contribution
        Facet facet = new Facet(this.getNormal(state), this.solarArrayArea);
        double dot = Vector3D.dotProduct(facet.getNormal(), fluxSat);
        if (dot > 0) {
            // the solar array is illuminated backward,
            // fix signs to compute contribution correctly
            dot = -dot;
            facet = new Facet(facet.getNormal().negate(), this.solarArrayArea);
        }
        Vector3D force = this.facetRadiationAcceleration(facet, fluxSat, dot);

        // body facets contribution
        for (final Facet bodyFacet : this.facets) {
            dot = Vector3D.dotProduct(bodyFacet.getNormal(), fluxSat);
            if (dot < 0) {
                // the facet intercepts the incoming flux
                force = force.add(this.facetRadiationAcceleration(bodyFacet, fluxSat, dot));
            }
        }

        // convert to inertial frame
        return r.applyTo(new Vector3D(1.0 / state.getMass(this.partName), force));

    }

    /**
     * Compute contribution of one facet to force.
     * The computation of the radiative force of one facet has been modified
     * (see https://www.orekit.org/forge/issues/92) to implement the correct
     * Vallado's formulation.
     * <p>
     * This method implements equation 8-44 from David A. Vallado's Fundamentals of Astrodynamics and Applications,
     * third edition, 2007, Microcosm Press.
     * </p>
     * 
     * @param facet
     *        facet definition
     * @param fluxSat
     *        radiation pressure flux in spacecraft frame
     * @param dot
     *        dot product of facet and fluxSat (must be negative)
     * @return contribution of the facet to force in spacecraft frame
     */
    private Vector3D facetRadiationAcceleration(final Facet facet,
                                                final Vector3D fluxSat, final double dot) {
        final double area = facet.getArea();
        final double psr = fluxSat.getNorm();

        // Vallado's equation 8-44 uses different parameters which are related
        // to our parameters as:
        // cos (phi) = -dot / (psr * area)
        // n = facet / area
        // s = -fluxSat / psr
        final double cN = 2 * area * dot *
            (this.diffuseReflectionCoeff / 3 - this.specularReflectionCoeff * dot / psr);
        final double cS = (area * dot / psr) * (this.specularReflectionCoeff - 1);

        return new Vector3D(cN, facet.getNormal(), cS, fluxSat);
    }

    /**
     * Class representing a single facet of a convex spacecraft body.
     * <p>
     * Instance of this class are guaranteed to be immutable.
     * </p>
     * 
     * @author Luc Maisonobe
     */
    public static class Facet implements Serializable {

        /** Serializble UID. */
        private static final long serialVersionUID = -1743508315029520059L;

        /** Unit Normal vector, pointing outward. */
        private final Vector3D normal;

        /** Area in m<sup>2</sup>. */
        private final double area;

        /**
         * Simple constructor.
         * 
         * @param normal
         *        vector normal to the facet, pointing outward (will be normalized)
         * @param area
         *        facet area in m<sup>2</sup>
         */
        public Facet(final Vector3D normal, final double area) {
            this.normal = normal.normalize();
            this.area = area;
        }

        /**
         * Get unit normal vector.
         * 
         * @return unit normal vector
         */
        public Vector3D getNormal() {
            return this.normal;
        }

        /**
         * Get facet area.
         * 
         * @return facet area
         */
        public double getArea() {
            return this.area;
        }

    }

    /**
     * Build the surface vectors for body facets of a simple parallelepipedic box.
     * 
     * @param xLength
     *        length of the body along its X axis (m)
     * @param yLength
     *        length of the body along its Y axis (m)
     * @param zLength
     *        length of the body along its Z axis (m)
     * @return surface vectors array
     */
    private static Facet[] simpleBoxFacets(final double xLength, final double yLength, final double zLength) {
        return new Facet[] {
            new Facet(Vector3D.MINUS_I, yLength * zLength),
            new Facet(Vector3D.PLUS_I, yLength * zLength),
            new Facet(Vector3D.MINUS_J, xLength * zLength),
            new Facet(Vector3D.PLUS_J, xLength * zLength),
            new Facet(Vector3D.MINUS_K, xLength * yLength),
            new Facet(Vector3D.PLUS_K, xLength * yLength)
        };
    }

    /**
     * Filter out zero area facets.
     * 
     * @param facets
     *        original facets (may include zero area facets)
     * @return filtered array
     */
    private static List<Facet> filter(final Facet[] facets) {
        final List<Facet> filtered = new ArrayList<>(facets.length);
        for (final Facet facet : facets) {
            if (facet.getArea() > 0) {
                filtered.add(facet);
            }
        }
        return filtered;
    }

    /**
     * Set the absorption coefficient.
     * 
     * @param value
     *        the absorption coefficient to set
     */
    public void setAbsorptionCoefficient(final double value) {
        this.absorptionCoeff = value;
        this.diffuseReflectionCoeff = 1 - (this.absorptionCoeff + this.specularReflectionCoeff);
    }

    /**
     * Get the absorption coefficient.
     * 
     * @return the reflection coefficient
     */
    public double getAbsorptionCoefficient() {
        return this.absorptionCoeff;
    }

    /**
     * Set the reflection coefficient.
     * 
     * @param value
     *        the reflection coefficient to set
     */
    public void setReflectionCoefficient(final double value) {
        this.specularReflectionCoeff = value;
        this.diffuseReflectionCoeff = 1 - (this.absorptionCoeff + this.specularReflectionCoeff);
    }

    /**
     * Get the reflection coefficient.
     * 
     * @return the reflection coefficient
     */
    public double getReflectionCoefficient() {
        return this.specularReflectionCoeff;
    }

    /**
     * Set the drag coefficient.
     * 
     * @param value
     *        the drag coefficient to set
     */
    public void setDragCoefficient(final double value) {
        this.dragCoeff = value;
    }

    /**
     * Get the drag coefficient.
     * 
     * @return the drag coefficient
     */
    public double getDragCoefficient() {
        return this.dragCoeff;
    }

    /**
     * Set the diffusion coefficient.
     * 
     * @param value
     *        the diffusion coefficient to set
     */
    public void setDiffusionCoefficient(final double value) {
        this.diffuseReflectionCoeff = value;
    }

    /**
     * Get the diffusion coefficient.
     * 
     * @return the diffusion coefficient
     */
    public double getDiffusionCoefficient() {
        return this.diffuseReflectionCoeff;
    }

    /** {@inheritDoc} */
    public void addDDragAccDParam(final SpacecraftState s, final String paramName, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDDragAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                  final double density, final Vector3D acceleration, final Vector3D relativeVelocity,
                                  final boolean computeGradientPosition, final boolean computeGradientVelocity) {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDParam(final SpacecraftState s, final Parameter paramName, final double[] dAccdParam,
                                 final Vector3D satSunVector) {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public void addDSRPAccDState(final SpacecraftState s, final double[][] dAccdPos, final double[][] dAccdVel,
                                 final Vector3D satSunVector) {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getJacobianParameters() {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);
    }

    /**
     * @see fr.cnes.sirius.patrius.forces.drag.DragSensitive#addDDragAccDParam(fr.cnes.sirius.patrius.propagation.SpacecraftState,
     *      fr.cnes.sirius.patrius.math.parameter.Parameter, double,
     *      fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D, double[])
     */
    @Override
    public void addDDragAccDParam(final SpacecraftState s, final Parameter param, final double density,
                                  final Vector3D relativeVelocity, final double[] dAccdParam) {
        // this model does not support partial derivatives computation yet:
        throw PatriusException.createInternalError(null);

    }

    /** {@inheritDoc} */
    @Override
    public DragSensitive copy(final Assembly assembly) {
        // Unused
        return null;
    }
}
