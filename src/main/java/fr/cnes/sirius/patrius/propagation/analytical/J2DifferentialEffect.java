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
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation.analytical;

import java.io.Serializable;

import fr.cnes.sirius.patrius.forces.gravity.potential.PotentialCoefficientsProvider;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.EquinoctialOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.OrbitType;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Analytical model for J2 effect.
 * <p>
 * This class computes the differential effect of J2 due to an initial orbit offset. A typical case is when an
 * inclination maneuver changes an orbit inclination at time t<sub>0</sub>. As ascending node drift rate depends on
 * inclination, the change induces a time-dependent change in ascending node for later dates.
 * </p>
 * 
 * @see fr.cnes.sirius.patrius.forces.maneuvers.SmallManeuverAnalyticalModel
 * @author Luc Maisonobe
 */
public class J2DifferentialEffect
    implements AdapterPropagator.DifferentialEffect, Serializable {

    /** Small shift. */
    private static final double SHIFT = 0.001;

    /** 1.5. */
    private static final double C15 = 1.5;

    /** 3.5. */
    private static final double C35 = 3.5;

    /** Serializable UID. */
    private static final long serialVersionUID = 3681516488729752197L;

    /** Reference date. */
    private final AbsoluteDate referenceDate;

    /** Differential drift on perigee argument. */
    private final double dPaDot;

    /** Differential drift on ascending node. */
    private final double dRaanDot;

    /** Indicator for applying effect before reference date. */
    private final boolean applyBefore;

    /**
     * Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential effect is associated with a maneuver. In
     * this case, the parameter must be set to {@code false}.
     * </p>
     * 
     * @param original
     *        original state at reference date
     * @param directEffect
     *        direct effect changing the orbit
     * @param applyBeforeIn
     *        if true, effect is applied both before and after
     *        reference date, if false it is only applied after reference date
     * @param gravityField
     *        gravity field to use
     * @exception PatriusException
     *            if gravity field does not contain J2 coefficient
     */
    public J2DifferentialEffect(final SpacecraftState original,
        final AdapterPropagator.DifferentialEffect directEffect,
        final boolean applyBeforeIn,
        final PotentialCoefficientsProvider gravityField) throws PatriusException {
        this(original, directEffect, applyBeforeIn,
            gravityField.getAe(), gravityField.getMu(), gravityField.getJ(false, 2)[2]);
    }

    /**
     * Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential effect is associated with a maneuver. In
     * this case, the parameter must be set to {@code false}.
     * </p>
     * 
     * @param orbit0
     *        original orbit at reference date
     * @param orbit1
     *        shifted orbit at reference date
     * @param applyBeforeIn
     *        if true, effect is applied both before and after
     *        reference date, if false it is only applied after reference date
     * @param gravityField
     *        gravity field to use
     * @exception PatriusException
     *            if gravity field does not contain J2 coefficient
     */
    public J2DifferentialEffect(final Orbit orbit0, final Orbit orbit1, final boolean applyBeforeIn,
        final PotentialCoefficientsProvider gravityField) throws PatriusException {
        this(orbit0, orbit1, applyBeforeIn,
            gravityField.getAe(), gravityField.getMu(), gravityField.getJ(false, 2)[2]);
    }

    /**
     * Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential effect is associated with a maneuver. In
     * this case, the parameter must be set to {@code false}.
     * </p>
     * 
     * @param original
     *        original state at reference date
     * @param directEffect
     *        direct effect changing the orbit
     * @param applyBeforeIn
     *        if true, effect is applied both before and after
     *        reference date, if false it is only applied after reference date
     * @param referenceRadius
     *        reference radius of the Earth for the potential model (m)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param j2
     *        un-normalized zonal coefficient (about +1.08e-3 for Earth)
     * @exception PatriusException
     *            if direct effect cannot be applied
     */
    public J2DifferentialEffect(final SpacecraftState original,
        final AdapterPropagator.DifferentialEffect directEffect,
        final boolean applyBeforeIn,
        final double referenceRadius, final double mu, final double j2) throws PatriusException {
        this(original.getOrbit(),
            directEffect.apply(original.shiftedBy(SHIFT)).getOrbit().shiftedBy(-SHIFT),
            applyBeforeIn, referenceRadius, mu, j2);
    }

    /**
     * Simple constructor.
     * <p>
     * The {@code applyBefore} parameter is mainly used when the differential effect is associated with a maneuver. In
     * this case, the parameter must be set to {@code false}.
     * </p>
     * 
     * @param orbit0
     *        original orbit at reference date
     * @param orbit1
     *        shifted orbit at reference date
     * @param applyBeforeIn
     *        if true, effect is applied both before and after
     *        reference date, if false it is only applied after reference date
     * @param referenceRadius
     *        reference radius of the Earth for the potential model (m)
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @param j2
     *        un-normalized zonal coefficient (about +1.08e-3 for Earth)
     */
    public J2DifferentialEffect(final Orbit orbit0, final Orbit orbit1, final boolean applyBeforeIn,
        final double referenceRadius, final double mu, final double j2) {

        this.referenceDate = orbit0.getDate();
        this.applyBefore = applyBeforeIn;

        // extract useful parameters
        final double a0 = orbit0.getA();
        final double e0 = orbit0.getE();
        final double i0 = orbit0.getI();
        final double a1 = orbit1.getA();
        final double e1 = orbit1.getE();
        final double i1 = orbit1.getI();

        // compute reference drifts
        final double oMe2 = 1 - e0 * e0;
        final double ratio = referenceRadius / (a0 * oMe2);
        final double[] sincosI0 = MathLib.sinAndCos(i0);
        final double cosI = sincosI0[1];
        final double sinI = sincosI0[0];
        final double n = MathLib.sqrt(mu / a0) / a0;
        final double c = ratio * ratio * n * j2;
        final double refPaDot = 0.75 * c * (4 - 5 * sinI * sinI);
        final double refRaanDot = -C15 * c * cosI;

        // differential model on perigee argument drift
        final double dPaDotDa = -C35 * refPaDot / a0;
        final double dPaDotDe = 4 * refPaDot * e0 / oMe2;
        final double dPaDotDi = -(4. + C35) * c * sinI * cosI;
        this.dPaDot = dPaDotDa * (a1 - a0) + dPaDotDe * (e1 - e0) + dPaDotDi * (i1 - i0);

        // differential model on ascending node drift
        final double dRaanDotDa = -C35 * refRaanDot / a0;
        final double dRaanDotDe = 4 * refRaanDot * e0 / oMe2;
        final double dRaanDotDi = -refRaanDot * MathLib.tan(i0);
        this.dRaanDot = dRaanDotDa * (a1 - a0) + dRaanDotDe * (e1 - e0) + dRaanDotDi * (i1 - i0);

    }

    /**
     * Compute the effect of the maneuver on an orbit.
     * 
     * @param orbit1
     *        original orbit at t<sub>1</sub>, without maneuver
     * @return orbit at t<sub>1</sub>, taking the maneuver
     *         into account if t<sub>1</sub> &gt; t<sub>0</sub>
     * @see #apply(SpacecraftState)
     */
    public Orbit apply(final Orbit orbit1) {

        if (orbit1.getDate().compareTo(this.referenceDate) <= 0 && !this.applyBefore) {
            // the orbit change has not occurred yet, don't change anything
            return orbit1;
        }

        return this.updateOrbit(orbit1);

    }

    /** {@inheritDoc} */
    @Override
    public SpacecraftState apply(final SpacecraftState state1) throws PatriusException {

        if (state1.getDate().compareTo(this.referenceDate) <= 0 && !this.applyBefore) {
            // the orbit change has not occurred yet, don't change anything
            return state1;
        }
        return state1.updateOrbit(this.updateOrbit(state1.getOrbit()));
    }

    /**
     * Compute the differential effect of J2 on an orbit.
     * 
     * @param orbit1
     *        original orbit at t<sub>1</sub>, without differential J2
     * @return orbit at t<sub>1</sub>, always taking the effect into account
     */
    private Orbit updateOrbit(final Orbit orbit1) {

        // convert current orbital state to equinoctial elements
        final EquinoctialOrbit original =
            (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(orbit1);

        // compute differential effect
        final AbsoluteDate date = original.getDate();
        final double dt = date.durationFrom(this.referenceDate);
        final double dPaRaan = (this.dPaDot + this.dRaanDot) * dt;
        final double[] sincosdPaRaan = MathLib.sinAndCos(dPaRaan);
        final double cPaRaan = sincosdPaRaan[1];
        final double sPaRaan = sincosdPaRaan[0];
        final double dRaan = this.dRaanDot * dt;
        final double[] sincosdRaan = MathLib.sinAndCos(dRaan);
        final double cRaan = sincosdRaan[1];
        final double sRaan = sincosdRaan[0];

        final double ex = original.getEquinoctialEx() * cPaRaan -
            original.getEquinoctialEy() * sPaRaan;
        final double ey = original.getEquinoctialEx() * sPaRaan +
            original.getEquinoctialEy() * cPaRaan;
        final double hx = original.getHx() * cRaan - original.getHy() * sRaan;
        final double hy = original.getHx() * sRaan + original.getHy() * cRaan;
        final double lambda = original.getLv() + dPaRaan;

        // build updated orbit
        final EquinoctialOrbit updated =
            new EquinoctialOrbit(original.getA(), ex, ey, hx, hy, lambda, PositionAngle.TRUE,
                original.getFrame(), date, original.getMu());

        // convert to required type
        return orbit1.getType().convertType(updated);

    }

}
